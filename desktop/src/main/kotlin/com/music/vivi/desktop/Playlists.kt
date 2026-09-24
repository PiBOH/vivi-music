package com.music.vivi.desktop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.music.vivi.sync.SyncedPlaylist
import com.music.vivi.sync.SyncedSong
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

private const val MIRROR_PREFIX = "yt-"

/**
 * The account's playlist id behind a local playlist, in whichever of the two
 * forms it is stored: a playlist mirrored **from** the account carries it in its
 * id (`yt-<id>`), one created here and pushed up carries it in
 * [SyncedPlaylist.remoteId]. This — not the local id — is what identifies the
 * same playlist on two devices.
 */
private fun SyncedPlaylist.accountIdentity(): String? =
    remoteId?.takeIf { it.isNotBlank() }
        ?: id.removePrefix(MIRROR_PREFIX).takeIf { id.startsWith(MIRROR_PREFIX) }

/**
 * The account's *special* playlists: the liked songs (`LM`) and the ones saved for
 * later (`SE`). They are not playlists the paired device should import — the
 * phone already has its own Liked list and its own saved-for-later row, and
 * carrying the desktop's mirror of them over as an ordinary playlist is what gave
 * it a second entry for each of them (the mobile app filters exactly these two out
 * of its own account sync).
 */
private val SPECIAL_ACCOUNT_PLAYLISTS = setOf("LM", "SE")

/**
 * True when two entries are the same playlist even though their ids do not say so:
 * the same name, and one song list contained in the other.
 *
 * This is what recognises the copies a pairing leaves behind when there is no id to
 * match on — the peer's *own* row for a playlist that was later created on YouTube
 * Music here (its copy carries no account id at all), and the fact that the two
 * devices each created the account copy of the same playlist (two account ids, same
 * name, and the same songs, or the ones of one contained in those of the other).
 *
 * **Constraint:** the merge is local and the songs are unioned, so nothing is lost
 * from the app's point of view and nothing at all is deleted on YouTube Music — but
 * two playlists of the same name whose songs are in a containment relation *are*
 * treated as one. That is deliberately the price of the copies this pairing creates:
 * they are indistinguishable by anything else, and a stale copy of a playlist (one
 * song less, one song more) is exactly the shape they come in.
 */
internal fun samePlaylist(a: SyncedPlaylist, b: SyncedPlaylist): Boolean {
    if (a.deleted || b.deleted) return false
    val name = a.name.trim()
    if (name.isEmpty() || !name.equals(b.name.trim(), ignoreCase = true)) return false
    val x = a.songs.map { it.id }.toSet()
    val y = b.songs.map { it.id }.toSet()
    if (x.isEmpty() || y.isEmpty()) return false
    return x == y || x.containsAll(y) || y.containsAll(x)
}

/**
 * JSON-backed store for the desktop's local playlists.
 *
 * Playlists are the same [SyncedPlaylist] objects that travel over the sync
 * protocol, so there is no mapping between a "local" and a "wire" type. A
 * deleted playlist stays in the store as a tombstone (`deleted = true`) until
 * it is pruned, so deletions can propagate to the paired phone.
 */
object PlaylistStore {
    private val json = sharedJsonPretty

    private val file = File(System.getProperty("user.home"), ".vivimusic/playlists.json").apply {
        parentFile?.mkdirs()
    }

    private val _all = MutableStateFlow(load())
    val all: StateFlow<List<SyncedPlaylist>> = _all.asStateFlow()

    init {
        // The copies a pairing created before the account id travelled with the
        // playlist (E1034) are collapsed on every start: it only ever merges
        // entries that are the same account playlist, so it is a no-op once the
        // store is clean.
        repairDuplicates()
    }

    /** Active (non-deleted) playlists, most recently updated first. */
    val active: List<SyncedPlaylist>
        get() = _all.value.filter { !it.deleted }.sortedByDescending { it.updatedAt }

    fun get(id: String): SyncedPlaylist? = _all.value.firstOrNull { it.id == id && !it.deleted }

    /**
     * A new local playlist. [remoteId] links it to the account's copy when the
     * user asked for it at creation time (the mobile create dialog's
     * "Sync playlist" switch); otherwise it is local-only and
     * [PlaylistSync.uploadMissing] is what can create the account copy later.
     */
    fun create(name: String, remoteId: String? = null): SyncedPlaylist {
        val p = SyncedPlaylist(
            id = newId(),
            name = name.trim(),
            updatedAt = System.currentTimeMillis(),
            remoteId = remoteId,
        )
        _all.value = _all.value + p
        persist()
        return p
    }

    /**
     * Records the account's playlist id on an existing local playlist, so its
     * later edits can reach the account (see [PlaylistSync.songAdded]).
     */
    fun link(id: String, remoteId: String) {
        _all.value = _all.value.map { p ->
            if (p.id == id && !p.deleted && p.remoteId != remoteId) {
                p.copy(remoteId = remoteId, updatedAt = System.currentTimeMillis())
            } else {
                p
            }
        }
        // A playlist that has just learned its account id may now be the same
        // playlist as an entry that already knew it (the mirror of the account's
        // copy, or the peer's row for it) — the copies collapse here, at the very
        // moment the link makes them recognisable.
        repairDuplicates()
        persist()
    }

    fun rename(id: String, name: String) {
        var renamed: SyncedPlaylist? = null
        _all.value = _all.value.map { p ->
            if (p.id == id && !p.deleted) {
                p.copy(name = name.trim(), updatedAt = System.currentTimeMillis()).also { renamed = it }
            } else {
                p
            }
        }
        persist()
        // The account's copy is renamed with it, so the two do not drift apart
        // (deleting it there is deliberately NOT propagated: it would remove a
        // playlist from the user's YouTube account, which is not reversible).
        renamed?.let { PlaylistSync.renamed(it) }
    }

    /**
     * Deletes a playlist here, and — when it also lives on the account — there
     * too, exactly like the mobile app does for a playlist with a browse id.
     */
    fun delete(id: String) {
        var removed: SyncedPlaylist? = null
        _all.value = _all.value.map { p ->
            if (p.id == id) {
                p.copy(deleted = true, updatedAt = System.currentTimeMillis()).also { removed = it }
            } else {
                p
            }
        }
        persist()
        removed?.let { PlaylistSync.deleted(it) }
    }

    fun addSongs(id: String, songs: List<SyncedSong>) {
        var addedTo: SyncedPlaylist? = null
        var added: List<SyncedSong> = emptyList()
        _all.value = _all.value.map { p ->
            if (p.id == id && !p.deleted) {
                val existing = p.songs.map { it.id }.toSet()
                added = songs.filter { it.id !in existing }
                if (added.isEmpty()) p
                else p.copy(songs = p.songs + added, updatedAt = System.currentTimeMillis())
                    .also { addedTo = it }
            } else p
        }
        persist()
        // A playlist that lives on the account too: the song was written here, so
        // it has to reach YouTube as well, exactly like the mobile app does when
        // a song is added to a playlist that carries a browse id.
        val updated = addedTo ?: return
        PlaylistSync.songsAdded(updated, added)
    }

    fun removeSong(id: String, songId: String) {
        _all.value = _all.value.map { p ->
            if (p.id == id && !p.deleted) {
                p.copy(songs = p.songs.filter { it.id != songId }, updatedAt = System.currentTimeMillis())
            } else p
        }
        persist()
    }

    fun reorderSongs(id: String, newOrder: List<SyncedSong>) {
        _all.value = _all.value.map { p ->
            if (p.id == id && !p.deleted) {
                p.copy(songs = newOrder, updatedAt = System.currentTimeMillis())
            } else p
        }
        persist()
    }

    /**
     * Merges a remote playlist list into the store with last-write-wins.
     *
     * The identity of a playlist across two devices is its **account** playlist
     * id, never the local row id: both apps generate their own (`LP` + 8
     * characters), so the very same account playlist is `LPAAAA…` here and
     * `LPBBBB…` on the phone. Matching on the local id (what this used to do) is
     * what imported a second copy of every account playlist as soon as the two
     * were paired (E1034). A remote playlist is therefore merged into the local
     * playlist that already mirrors the same account playlist — or into the one
     * whose own id *is* that account id, the form this app keeps for its own
     * mirrors — and only added as a new local playlist when nothing matches.
     *
     * Local playlists missing from [remote] are kept (they will be pushed back).
     */
    fun applyRemote(remote: List<SyncedPlaylist>) {
        if (remote.isEmpty()) return
        val merged = _all.value.associateBy { it.id }.toMutableMap()
        for (r in remote) {
            // The account's special lists are not playlists either side should
            // import (see SPECIAL_ACCOUNT_PLAYLISTS).
            if (r.accountIdentity() in SPECIAL_ACCOUNT_PLAYLISTS) continue
            val target = findTarget(merged.values, r)
            if (target == null) {
                merged[r.id] = r
                continue
            }
            val winner = if (r.updatedAt > target.updatedAt) r else target
            // The local id survives (the sidebar, the routes and the peer all
            // point at it) and the account id is adopted from whichever side
            // carries it, so a playlist imported before this fix gains its link
            // instead of being mirrored a second time later.
            merged[target.id] = winner.copy(
                id = target.id,
                remoteId = target.remoteId ?: r.remoteId,
            )
        }
        _all.value = merged.values.toList()
        repairDuplicates()
        persist()
    }

    /**
     * The local playlist a remote [r] stands for, or null when it is new here.
     *
     * Three ways to recognise it, in order of certainty: the same row id (both
     * sides have the same playlist under the same id), the account id it carries
     * ([SyncedPlaylist.remoteId], what a patched peer sends), or a remote id that
     * is an account playlist we already mirror.
     */
    private fun findTarget(local: Collection<SyncedPlaylist>, r: SyncedPlaylist): SyncedPlaylist? {
        local.firstOrNull { it.id == r.id }?.let { return it }
        r.remoteId?.takeIf { it.isNotBlank() }
            ?.let { account -> local.firstOrNull { it.accountIdentity() == account } }
            ?.let { return it }
        return local.firstOrNull { it.accountIdentity() == r.id }
    }

    /**
     * Collapses the copies of one playlist into a single entry.
     *
     * A pairing can leave the same playlist in the store more than once, in two
     * different ways:
     *
     *  1. **The same account id under two rows** (E1034): the copy arriving from the
     *     paired device for a playlist that is already here. Both apps generate
     *     their own local row id (`LP` + 8 characters), so the copy can only be
     *     recognised by the account id it carries.
     *  2. **Nothing in common but the name and the songs**: the peer's *own* row for
     *     a playlist that was afterwards created on YouTube Music **here** (its copy
     *     carries no account id at all), and the fact that the two devices each
     *     created the account copy of the same playlist (two account ids, same name,
     *     same songs). See [samePlaylist].
     *
     * The kept entry is the one that already knows its account id — so the link to
     * YouTube Music survives — and the others are tombstoned **locally**. Nothing
     * here ever touches the account: only the delete the user performs does, so this
     * cleanup can never remove a playlist from YouTube Music. Runs at startup and
     * after every merge or link, and it does nothing when there is nothing to merge.
     */
    fun repairDuplicates() {
        val active = _all.value.filterNot { it.deleted }
        if (active.size < 2) return
        val now = System.currentTimeMillis()

        // The entry that survives a group of copies, and the copies collapsing into it.
        class Group(val keep: SyncedPlaylist, val members: MutableList<SyncedPlaylist>)

        val groups = mutableListOf<Group>()
        val grouped = mutableSetOf<String>()

        // (1) One account playlist, several local rows.
        active.filter { it.accountIdentity() != null }
            .groupBy { it.accountIdentity()!! }
            .filterValues { it.size > 1 }
            .forEach { (_, share) ->
                val keep = share.firstOrNull { it.remoteId != null }
                    ?: share.firstOrNull { it.id.startsWith(MIRROR_PREFIX) }
                    ?: share.minByOrNull { it.updatedAt }
                    ?: return@forEach
                val members = share.filter { it.id != keep.id }.toMutableList()
                groups += Group(keep, members)
                grouped += keep.id
                grouped += members.map { it.id }
            }

        // (2) The same playlist by name and songs, with no id in common. The order
        //     decides what is kept: a row that knows its account id first, then a
        //     mirror, then a local row, oldest first — the entry whose id the user
        //     is most likely already looking at survives.
        val ranked = active
            .filterNot { it.id in grouped }
            .sortedWith(
                compareBy(
                    { when {
                        it.remoteId != null -> 0
                        it.id.startsWith(MIRROR_PREFIX) -> 1
                        else -> 2
                    } },
                    { it.updatedAt },
                ),
            )
        for (p in ranked) {
            if (p.id in grouped) continue
            val twin = groups.firstOrNull { samePlaylist(it.keep, p) }
            if (twin != null) {
                twin.members += p
                grouped += p.id
                continue
            }
            val later = ranked.filter { it.id !in grouped && it.id != p.id && samePlaylist(p, it) }
            if (later.isEmpty()) continue
            groups += Group(p, later.toMutableList())
            grouped += p.id
            grouped += later.map { it.id }
        }
        val merging = groups.filter { it.members.isNotEmpty() }
        if (merging.isEmpty()) return

        var all = _all.value
        for (group in merging) {
            val keep = group.keep
            val members = group.members
            val account = keep.accountIdentity() ?: members.firstNotNullOfOrNull { it.accountIdentity() }
            // Every copy may hold songs the others do not: the kept order comes
            // first, the extras follow, and a song that is in several stays once.
            val songs = LinkedHashMap<String, SyncedSong>()
            (listOf(keep) + members).forEach { copy ->
                copy.songs.forEach { song -> songs.putIfAbsent(song.id, song) }
            }
            all = all.map { p ->
                when {
                    p.id == keep.id -> p.copy(
                        remoteId = p.remoteId ?: account,
                        songs = songs.values.toList(),
                        updatedAt = maxOf(keep.updatedAt, members.maxOfOrNull { it.updatedAt } ?: 0L, now),
                    )
                    members.any { it.id == p.id } -> p.copy(deleted = true, updatedAt = now)
                    else -> p
                }
            }
            AppLog.log(
                "playlists",
                "repair: '${keep.name}' existed ${members.size + 1} times (the same playlist, " +
                    "account id ${account ?: "none"}) — kept ${keep.id}, " +
                    "removed ${members.joinToString { it.id }} LOCALLY ONLY, " +
                    "nothing was deleted on YouTube Music",
            )
        }
        _all.value = all
        persist()
    }

    /** Full state (active + recent tombstones) for the sync snapshot. */
    fun toSynced(): List<SyncedPlaylist> {
        // Prune tombstones older than 30 days so the wire list stays bounded.
        val cutoff = System.currentTimeMillis() - 30L * 24 * 3600 * 1000
        return _all.value
            .filterNot { it.accountIdentity() in SPECIAL_ACCOUNT_PLAYLISTS }
            .filterNot { it.deleted && it.updatedAt < cutoff }
            // A playlist mirrored from the account carries its account id inside
            // its own id (`yt-<id>`). It travels in [SyncedPlaylist.remoteId] too,
            // so the peer recognises it as the account playlist instead of
            // importing yet another copy of it (E1034).
            .map { p ->
                if (p.remoteId == null) {
                    p.accountIdentity()?.let { account -> p.copy(remoteId = account) } ?: p
                } else {
                    p
                }
            }
    }

    /** Replaces the whole store (used when restoring a backup) and persists. */
    fun replaceAll(playlists: List<SyncedPlaylist>) {
        _all.value = playlists
        persist()
    }

    private fun newId(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val suffix = (1..8).map { chars.random() }.joinToString("")
        return "LP$suffix"
    }

    private fun load(): List<SyncedPlaylist> = try {
        if (file.exists()) json.decodeFromString(file.readText()) else emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    private fun persist() {
        try {
            file.writeText(json.encodeToString(_all.value))
        } catch (_: Exception) {
            // best-effort
        }
    }
}

/** List of the user's local playlists with create / rename / delete actions. */
@Composable
fun LocalPlaylistsScreen(
    language: String,
    onBack: () -> Unit,
    onOpenPlaylist: (String) -> Unit,
) {
    val playlists by PlaylistStore.all.collectAsState()
    val active = playlists.filter { !it.deleted }.sortedByDescending { it.updatedAt }

    var showCreate by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<SyncedPlaylist?>(null) }
    var deleteTarget by remember { mutableStateOf<SyncedPlaylist?>(null) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    // Defer the actual deletion until the dialog has been dismissed. Deleting a
    // row from the list in the same frame as closing the dialog reflows the
    // LazyColumn while the dialog window is still being torn down, which trips
    // the Compose "layouts are not part of the same hierarchy" crash.
    LaunchedEffect(deleteTarget) {
        val id = pendingDeleteId
        if (deleteTarget == null && id != null) {
            pendingDeleteId = null
            PlaylistStore.delete(id)
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(language, onBack)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Localization.get(language, "playlists"), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.weight(1f))
            // The same action as the one in the Account screen: the playlist
            // list is where a user looks for it, so it lives in both places.
            if (LoginManager.isLoggedIn()) {
                val pendingUpload = active.count { it.remoteId == null && !PlaylistSync.isMirrored(it.id) }
                val syncStatus by PlaylistSync.status.collectAsState()
                var confirmUpload by remember { mutableStateOf(false) }
                Tooltip(
                    if (pendingUpload > 0) Localization.get(language, "playlists_upload")
                    else Localization.get(language, "playlists_upload_none"),
                ) {
                    OutlinedButton(
                        onClick = { confirmUpload = true },
                        enabled = pendingUpload > 0 && syncStatus.phase != PlaylistSync.Phase.RUNNING,
                    ) {
                        Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(Localization.get(language, "playlists_upload"))
                    }
                }
                if (syncStatus.phase == PlaylistSync.Phase.RUNNING) {
                    Spacer(Modifier.width(8.dp))
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    // The run can take minutes: show where it actually is.
                    Text(
                        syncStatus.progressText(
                            Localization.get(
                                language,
                                if (syncStatus.uploading) "playlists_upload" else "sync_in_progress",
                            ),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(8.dp))
                if (confirmUpload) {
                    AlertDialog(
                        onDismissRequest = { confirmUpload = false },
                        title = { Text(Localization.get(language, "playlists_upload")) },
                        text = {
                            Text(
                                Localization.get(language, "playlists_upload_confirm")
                                    .replace("%d", pendingUpload.toString()),
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                confirmUpload = false
                                PlaylistSync.uploadMissing("manual")
                            }) { Text(Localization.get(language, "ok")) }
                        },
                        dismissButton = {
                            TextButton(onClick = { confirmUpload = false }) {
                                Text(Localization.get(language, "cancel"))
                            }
                        },
                    )
                }
            }
            Button(onClick = { showCreate = true }) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(Localization.get(language, "new_playlist"))
            }
        }

        if (active.isEmpty()) {
            Text(
                Localization.get(language, "no_playlists"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp)) {
                items(active, key = { it.id }) { p ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onOpenPlaylist(p.id) }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(p.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                Localization.get(language, "song_count").replace("%d", p.songs.size.toString()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Tooltip(Localization.get(language, "rename")) {
                            IconButton(onClick = { renameTarget = p }) {
                                Icon(Icons.Filled.Edit, contentDescription = Localization.get(language, "rename"))
                            }
                        }
                        Tooltip(Localization.get(language, "delete")) {
                            IconButton(onClick = { deleteTarget = p }) {
                                Icon(Icons.Filled.Delete, contentDescription = Localization.get(language, "delete"), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        PlaylistNameDialog(
            language = language,
            initialName = "",
            confirmLabel = Localization.get(language, "create"),
            onConfirm = { name -> PlaylistStore.create(name) },
            onDismiss = { showCreate = false },
            allowSyncing = true,
            onConfirmSynced = { name ->
                // Created here first, so the playlist is on screen at once; the
                // account's copy is created in the background (and the songs
                // added to it are pushed as they come).
                PlaylistSync.pushPlaylist(PlaylistStore.create(name).id)
            },
        )
    }
    renameTarget?.let { target ->
        PlaylistNameDialog(
            language = language,
            initialName = target.name,
            confirmLabel = Localization.get(language, "save"),
            onConfirm = { name -> PlaylistStore.rename(target.id, name) },
            onDismiss = { renameTarget = null },
        )
    }
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(Localization.get(language, "delete_playlist")) },
            text = { Text(Localization.get(language, "delete_playlist_confirm")) },
            confirmButton = {
                TextButton(onClick = {
                    pendingDeleteId = target.id
                    deleteTarget = null
                }) { Text(Localization.get(language, "delete")) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(Localization.get(language, "cancel")) }
            },
        )
    }
}

/** A single local playlist: playable song list with per-song remove. */
@Composable
fun LocalPlaylistScreen(
    playlistId: String,
    language: String,
    onBack: () -> Unit,
    onPlay: (SyncedSong) -> Unit,
    onPlayAll: (List<SyncedSong>) -> Unit,
) {
    val playlists by PlaylistStore.all.collectAsState()
    val playlist = playlists.firstOrNull { it.id == playlistId && !it.deleted }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(language, onBack)
        if (playlist == null) {
            Text(
                Localization.get(language, "playlist_not_found"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
            return@Column
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(playlist.name, style = MaterialTheme.typography.headlineMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            if (playlist.songs.isNotEmpty()) {
                OutlinedButton(onClick = { onPlayAll(playlist.songs) }) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(Localization.get(language, "play_all"))
                }
            }
        }

        if (playlist.songs.isEmpty()) {
            Text(
                Localization.get(language, "empty_playlist"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
        } else {
            key(playlistId) {
                val lazyListState = rememberLazyListState()
                val localSongs = remember { mutableStateListOf<SyncedSong>() }
                var hasDragged by remember { mutableStateOf(false) }
                val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
                    localSongs.add(to.index, localSongs.removeAt(from.index))
                    hasDragged = true
                }

                // Keep the local copy in sync with the stored playlist (skip while dragging).
                LaunchedEffect(playlist.songs) {
                    if (!reorderableState.isAnyItemDragging) {
                        localSongs.clear()
                        localSongs.addAll(playlist.songs)
                    }
                }

                // Commit the new order once the drag ends.
                LaunchedEffect(reorderableState.isAnyItemDragging) {
                    if (!reorderableState.isAnyItemDragging && hasDragged) {
                        PlaylistStore.reorderSongs(playlist.id, localSongs.toList())
                        hasDragged = false
                    }
                }

                Text(
                    Localization.get(language, "drag_to_reorder"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                LazyColumn(state = lazyListState, modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                    itemsIndexed(localSongs, key = { _, song -> song.id }) { _, song ->
                        ReorderableItem(state = reorderableState, key = song.id) {
                            Row(
                                Modifier.fillMaxWidth().clickable { onPlay(song) }.padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "⠿",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .draggableHandle()
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                )
                                Thumbnail(song.thumbnail, Modifier.size(44.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(song.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        song.artist,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Text(
                                    "✕",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .clickable { PlaylistStore.removeSong(playlist.id, song.id) }
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Dialog to add [song] to one of the existing playlists (or create a new one). */
@Composable
fun AddToPlaylistDialog(
    language: String,
    song: SyncedSong,
    onDismiss: () -> Unit,
) {
    val playlists by PlaylistStore.all.collectAsState()
    val active = playlists.filter { !it.deleted }.sortedByDescending { it.updatedAt }
    var showCreate by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Localization.get(language, "add_to_playlist")) },
        text = {
            Column {
                TextButton(onClick = { showCreate = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(Localization.get(language, "new_playlist"))
                }
                LazyColumn {
                    items(active, key = { it.id }) { p ->
                        val contains = p.songs.any { it.id == song.id }
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                PlaylistStore.addSongs(p.id, listOf(song))
                                onDismiss()
                            }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                p.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (contains) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            if (contains) {
                                Text("✓", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(Localization.get(language, "cancel")) }
        },
    )

    if (showCreate) {
        PlaylistNameDialog(
            language = language,
            initialName = "",
            confirmLabel = Localization.get(language, "create"),
            onConfirm = { name ->
                PlaylistStore.addSongs(PlaylistStore.create(name).id, listOf(song))
                showCreate = false
                onDismiss()
            },
            onDismiss = { showCreate = false },
            allowSyncing = true,
            onConfirmSynced = { name ->
                // The song goes to the local playlist first, and the account copy
                // is created straight after: [PlaylistSync.pushPlaylist] reads
                // the playlist when it runs, so this song is uploaded with it
                // instead of waiting for the next one.
                val created = PlaylistStore.create(name)
                PlaylistStore.addSongs(created.id, listOf(song))
                PlaylistSync.pushPlaylist(created.id)
                showCreate = false
                onDismiss()
            },
        )
    }
}

/**
 * Names a new (or renamed) playlist.
 *
 * When [allowSyncing] is set — creating, never renaming — it carries the mobile
 * create dialog's *Sync playlist* switch: with it on, the playlist is created on
 * YouTube Music as well, and the songs land in the account copy as they are
 * added ([PlaylistSync.songsAdded]). The account copy is created in the
 * background, so the screen never waits on the network.
 */
@Composable
fun PlaylistNameDialog(
    language: String,
    initialName: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    allowSyncing: Boolean = false,
    /** Used instead of [onConfirm] when the sync switch is on. */
    onConfirmSynced: ((String) -> Unit)? = null,
) {
    var name by remember { mutableStateOf(initialName) }
    var wantsSync by remember { mutableStateOf(false) }
    val signedIn = LoginManager.isLoggedIn()
    val showSyncRow = allowSyncing && onConfirmSynced != null
    val syncing = showSyncRow && wantsSync && signedIn
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Localization.get(language, "playlist_name")) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text(Localization.get(language, "playlist_name")) },
                )
                if (showSyncRow) {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                Localization.get(language, "sync_playlist"),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                Localization.get(language, "sync_playlist_desc"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Switch(checked = wantsSync, onCheckedChange = { wantsSync = it })
                    }
                    if (wantsSync && !signedIn) {
                        Text(
                            Localization.get(language, "not_logged_in_youtube"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    if (name.isNotBlank()) {
                        if (syncing) onConfirmSynced!!(name) else onConfirm(name)
                        onDismiss()
                    }
                },
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(Localization.get(language, "cancel")) }
        },
    )
}

