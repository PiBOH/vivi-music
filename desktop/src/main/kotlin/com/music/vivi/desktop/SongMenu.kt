package com.music.vivi.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.LibraryAddCheck
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.vivi.sync.SyncedSong
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

/** Global non-invasive toast/snackbar for one-shot feedback (copy, …). */
object DesktopSnackbar {
    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val events = _events.asSharedFlow()
    fun show(message: String) { _events.tryEmit(message) }
}

/**
 * In-memory like / library state for the current session. The desktop has no
 * Room database like the Android app, so the toggled state lives here for the
 * session; the authoritative state stays on the YouTube account.
 *
 * The liked part is also this desktop's side of the **device-sync liked-song
 * ledger**. Each id maps to a [SyncedSong] entry — a like carries the song's
 * metadata (so the phone can store a song it has never seen), an unlike a
 * `deleted` tombstone — and `updatedAt` is the last-write-wins key against the
 * paired device's own entry for the same id. Entries learned from the peer are
 * held here too (that is what keeps the hearts right for the songs the phone
 * liked), but they are **not** sent back: the phone already has them.
 */
object SongActions {
    private val liked = mutableStateMapOf<String, SyncedSong>()
    private val inLibrary = mutableStateMapOf<String, Boolean>()

    /**
     * Incremented on every *local* like/unlike, so the sync layer knows when to
     * push the ledger. An entry learned from the peer does not bump it: a remote
     * change must never be sent back (the echo would ping-pong).
     */
    private val _likedVersion = MutableStateFlow(0)
    val likedVersion: StateFlow<Int> = _likedVersion.asStateFlow()

    /**
     * Fired when a song is liked ("Auto download on like" hooks here):
     * receives the song id and title. Registered once by the main window.
     * Only a like the **user** makes fires it, never one learned from the peer.
     */
    @Volatile
    var onSongLiked: ((id: String, title: String) -> Unit)? = null

    fun isLiked(id: String): Boolean = liked[id]?.let { !it.deleted } ?: false

    fun isInLibrary(song: SongItem): Boolean =
        inLibrary[song.id] ?: (song.libraryRemoveToken != null)

    /**
     * Applies a like/unlike the **user** made here: stamps the ledger entry with
     * the current time and marks it for the next library push.
     */
    fun setLiked(
        id: String,
        value: Boolean,
        title: String? = null,
        artist: String? = null,
        thumbnail: String? = null,
    ) {
        val previous = liked[id]
        liked[id] = SyncedSong(
            id = id,
            title = title ?: previous?.title.orEmpty(),
            artist = artist ?: previous?.artist.orEmpty(),
            thumbnail = thumbnail ?: previous?.thumbnail,
            updatedAt = System.currentTimeMillis(),
            deleted = !value,
        )
        _likedVersion.value += 1
        // "Songs" in the Library screen is the liked list: it has to reload, or
        // the heart toggles and the list behind it does not move.
        LibraryRefresh.bump()
        if (value && title != null) {
            onSongLiked?.invoke(id, title)
        }
    }

    fun setInLibrary(id: String, value: Boolean) { inLibrary[id] = value }

    /** The songs liked right now (the flat id list of the wire protocol). */
    fun likedIds(): List<String> = liked.values.filterNot { it.deleted }.map { it.id }

    /**
     * The ledger as this desktop sends it: the entries **this** session edited
     * (a like with metadata, an unlike as a tombstone) plus the tombstones of
     * the ones it was told about and must not forget. Entries simply learned
     * from the peer are left out — the phone already has them, and re-sending a
     * whole liked library on every push (a snapshot goes out on every queue
     * change) is bandwidth nothing needs.
     */
    fun toSyncedLiked(now: Long = System.currentTimeMillis()): List<SyncedSong> =
        liked.values.filter { it.updatedAt > 0L && !(it.deleted && now - it.updatedAt > TOMBSTONE_TTL_MS) }

    /**
     * Merges the peer's liked entries into the ledger, last-write-wins per id.
     *
     * A remote entry only wins on a **strictly newer** edit time, so an echo of
     * our own like (same timestamp) cannot flip it back, and an unlike is never
     * invented: an entry with no time at all (`updatedAt == 0`, an older peer, or
     * the flat [ids] list) can only *add* a like, and only when this desktop has
     * no entry for that song yet.
     *
     * Nothing here is marked as a local edit and [onSongLiked] is not fired: the
     * peer's like is not this desktop's like ("Auto download on like" must not
     * start downloading the phone's likes), and a remote change must not travel
     * back.
     */
    fun applyRemoteLiked(entries: List<SyncedSong>, ids: List<String> = emptyList()) {
        val pending = LinkedHashMap<String, SyncedSong>()
        for (r in entries) {
            if (r.id.isBlank()) continue
            val local = liked[r.id]
            val wins = when {
                local == null -> !r.deleted && r.updatedAt >= 0L
                r.updatedAt > 0L -> r.updatedAt > local.updatedAt
                else -> false
            }
            if (wins) pending[r.id] = r
        }
        for (id in ids) {
            if (id.isBlank() || liked.containsKey(id) || pending.containsKey(id)) continue
            pending[id] = SyncedSong(id = id, deleted = false)
        }
        if (pending.isEmpty()) return
        pending.forEach { (id, r) ->
            liked[id] = SyncedSong(
                id = id,
                title = r.title.ifBlank { liked[id]?.title.orEmpty() },
                artist = r.artist.ifBlank { liked[id]?.artist.orEmpty() },
                thumbnail = r.thumbnail ?: liked[id]?.thumbnail,
                updatedAt = r.updatedAt,
                deleted = r.deleted,
            )
        }
        // A like/unlike made on the paired phone lands in the same list.
        LibraryRefresh.bump()
    }

    /**
     * How long a tombstone is kept. It is what lets the peer apply an unlike made
     * while it was offline; a whole quarter is far more than a pairing needs, and
     * it keeps the ledger from growing forever.
     */
    private const val TOMBSTONE_TTL_MS = 90L * 24 * 60 * 60 * 1000
}

fun copyToClipboard(text: String) {
    runCatching {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
    }
}

/** "⋮" context menu for a song: like, library, add-to-playlist, queue and share. */
@Composable
fun SongMenu(
    song: SongItem,
    language: String,
    onAddToPlaylist: (() -> Unit)?,
    /** Adds the song to the play queue. It used to be a bare "＋" glyph at the
     *  end of the row: no icon, no tooltip, no label, so it read as a mystery
     *  button that did nothing (the row never showed whether the song landed in
     *  the queue). The action lives here now, where it is named. */
    onAddToQueue: (() -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val liked = SongActions.isLiked(song.id)
    val inLibrary = SongActions.isInLibrary(song)

    Box {
        Tooltip(Localization.get(language, "tooltip_more")) {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = Localization.get(language, "more"),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(Localization.get(language, if (liked) "unlike" else "like")) },
                leadingIcon = {
                    Icon(
                        if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = if (liked) MaterialTheme.colorScheme.error else LocalContentColor.current,
                    )
                },
                onClick = {
                    expanded = false
                    val next = !liked
                    // The metadata travels with the like: it is what lets the
                    // paired phone store a song it has never seen (see
                    // SongActions / LibrarySnapshot.likedSongs).
                    SongActions.setLiked(
                        song.id,
                        next,
                        title = song.title,
                        artist = song.artists.joinToString(", ") { it.name },
                        thumbnail = song.thumbnail,
                    )
                    scope.launch { YouTube.likeVideo(song.id, next) }
                },
            )
            DropdownMenuItem(
                text = { Text(Localization.get(language, if (inLibrary) "remove_from_library" else "add_to_library")) },
                leadingIcon = {
                    Icon(
                        if (inLibrary) Icons.Filled.LibraryAddCheck else Icons.Filled.LibraryAdd,
                        contentDescription = null,
                    )
                },
                onClick = {
                    expanded = false
                    val next = !inLibrary
                    SongActions.setInLibrary(song.id, next)
                    scope.launch { YouTube.toggleSongLibrary(song.id, addToLibrary = next) }
                },
            )
            if (onAddToPlaylist != null) {
                DropdownMenuItem(
                    text = { Text(Localization.get(language, "add_to_playlist")) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onAddToPlaylist()
                    },
                )
            }
            if (onAddToQueue != null) {
                DropdownMenuItem(
                    text = { Text(Localization.get(language, "add_to_queue")) },
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null)
                    },
                    onClick = {
                        expanded = false
                        onAddToQueue()
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(Localization.get(language, "share")) },
                leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                onClick = {
                    expanded = false
                    copyToClipboard(song.shareLink)
                    DesktopSnackbar.show(Localization.get(language, "copied_to_clipboard"))
                },
            )
        }
    }
}
