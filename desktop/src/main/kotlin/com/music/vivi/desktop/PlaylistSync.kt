package com.music.vivi.desktop

import com.music.innertube.YouTube
import com.music.innertube.models.PlaylistItem
import com.music.vivi.sync.SyncedPlaylist
import com.music.vivi.sync.SyncedSong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Keeps the desktop's playlists in step with the signed-in YouTube Music
 * account — the mobile app's *Auto sync with account*.
 *
 * The account's playlists are mirrored into [PlaylistStore] under a stable id
 * (`yt-<playlistId>`), so a second sync updates the same local playlist instead
 * of creating a duplicate. A mirrored playlist is only written when something
 * actually changed, so a sync that finds nothing new does not bump
 * `updatedAt` — that timestamp is the last-write-wins key of the device sync,
 * and bumping it for free would make an untouched playlist win against a
 * genuine edit made on the paired phone.
 *
 * Two directions, and the user asks for the second one explicitly: [sync] pulls
 * the account's playlists down, while [pushPlaylist] / [uploadMissing] create a
 * local playlist's copy **on the account** and upload its songs (the mobile
 * create dialog's *Sync playlist* switch, and the Account screen's
 * *Create on YouTube Music* action). A playlist that lives on both sides keeps
 * them together: a song added here is pushed ([songsAdded]), a rename is
 * propagated ([renamed]) and a deletion too ([deleted]) — the last one is
 * irreversible on the account, so it happens only from the delete the user
 * performs here, never from a tombstone that arrived over the device sync.
 */
object PlaylistSync {
    enum class Phase { IDLE, RUNNING, DONE, FAILED }

    /** Result of the last run, for the account screen's status line. */
    data class Status(
        val phase: Phase = Phase.IDLE,
        val playlists: Int = 0,
        val songs: Int = 0,
        val message: String = "",
        /** How many playlists were CREATED on the account by this run. */
        val created: Int = 0,
        // ---- live progress of the run in flight ----------------------------
        // A forced sync of a whole library is minutes of network work, so a
        // spinner alone cannot tell the user whether it is moving or stuck.
        // These four fields are updated as the run advances and describe what
        // it is doing right now:
        /** Playlists already processed by this run. */
        val done: Int = 0,
        /** Playlists this run has to process (0 = not known yet). */
        val total: Int = 0,
        /** Songs already moved (uploaded or downloaded) by this run. */
        val songsDone: Int = 0,
        /** Songs this run has to move (0 = not known yet). */
        val songsTotal: Int = 0,
        /** Name of the playlist being worked on right now (empty = none). */
        val current: String = "",
        /** Which half of the run is in flight. */
        val uploading: Boolean = false,
    ) {
        /**
         * One-line, language-neutral progress description (numbers + the
         * already-translated labels, so no new string keys are needed):
         * `Uploading 3/12 · 128/540 songs — 'Feste'`.
         */
        fun progressText(prefix: String): String {
            if (phase != Phase.RUNNING) return prefix
            val parts = mutableListOf(prefix)
            if (total > 0) parts += "$done/$total"
            if (songsTotal > 0) parts += "$songsDone/$songsTotal"
            val head = parts.joinToString(" · ")
            return if (current.isNotBlank()) "$head — '$current'" else head
        }
    }

    private val _status = MutableStateFlow(Status())
    val status: StateFlow<Status> = _status.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inFlight = AtomicBoolean(false)

    /** Playlist ids this app mirrors from the account (for the UI label). */
    fun isMirrored(playlistId: String): Boolean = playlistId.startsWith(PREFIX)

    /** The account playlist id behind a mirrored local playlist, or null. */
    fun remoteId(localId: String): String? =
        if (localId.startsWith(PREFIX)) localId.removePrefix(PREFIX) else null

    private const val PREFIX = "yt-"

    /** True when the user asked for the sync and there is an account to sync. */
    fun isEnabled(): Boolean = DesktopSettings.load().syncPlaylistsWithYoutube && LoginManager.isLoggedIn()

    /**
     * The account's playlist id behind a local playlist, whichever of the two
     * forms it is stored in (see `SyncedPlaylist.remoteId`): a playlist mirrored
     * from the account has it in its id (`yt-…`), one created here and pushed up
     * has it in the field.
     */
    fun SyncedPlaylist.accountPlaylistId(): String? = remoteId ?: remoteId(id)

    /**
     * A song was added to a playlist that also lives on the account: push it,
     * the way the mobile app does for a playlist with a browse id.
     *
     * Fire-and-forget on purpose — the local write has already succeeded and the
     * user must not wait on a network round trip — but every push is logged, so
     * a failure is visible in `playlists.log` rather than silent.
     */
    fun songsAdded(playlist: SyncedPlaylist, added: List<SyncedSong>) {
        val remote = playlist.accountPlaylistId() ?: return
        if (added.isEmpty()) return
        if (!LoginManager.isLoggedIn()) return
        scope.launch {
            val pushed = added.count { song ->
                val ok = YouTube.addToPlaylist(remote, song.id).isSuccess
                if (!ok) AppLog.log("playlists", "  '${playlist.name}': '${song.title}' was not added to the account copy")
                ok
            }
            AppLog.log("playlists", "'${playlist.name}': $pushed of ${added.size} song(s) pushed to the account copy")
        }
    }

    /**
     * The account's copy of a playlist the user deleted here is deleted with it,
     * as it is in the mobile app (a playlist with a browse id is removed from
     * both). Only this path propagates a deletion: a tombstone arriving over the
     * device sync removes the playlist here, but it never reaches into the
     * account from there.
     *
     * YouTube keeps no trash for a playlist, so this is irreversible — which is
     * why an unsigned session or an unlinked playlist simply does nothing.
     */
    fun deleted(playlist: SyncedPlaylist) {
        val remote = playlist.accountPlaylistId() ?: return
        if (!LoginManager.isLoggedIn()) return
        scope.launch {
            val ok = YouTube.deletePlaylist(remote).isSuccess
            AppLog.log(
                "playlists",
                "'${playlist.name}': account copy ${if (ok) "deleted" else "NOT deleted (it is still on YouTube Music)"}",
            )
        }
    }

    /** The account's copy of a renamed local playlist is renamed with it. */
    fun renamed(playlist: SyncedPlaylist) {
        val remote = playlist.accountPlaylistId() ?: return
        if (!LoginManager.isLoggedIn()) return
        if (playlist.name.isBlank()) return
        scope.launch {
            val ok = YouTube.renamePlaylist(remote, playlist.name).isSuccess
            AppLog.log(
                "playlists",
                "'${playlist.name}': account copy ${if (ok) "renamed" else "NOT renamed (the name there stays as it was)"}",
            )
        }
    }

    /**
     * Runs one pull: the account's playlists are mirrored into the local store.
     * Concurrent calls are ignored (the sidebar opens, the login lands and the
     * account screen opens within the same second).
     */
    fun sync(trigger: String = "manual") {
        if (!isEnabled()) {
            _status.value = Status(Phase.IDLE, message = "sync disabled or not signed in")
            return
        }
        run(trigger) { pull(trigger) }
    }

    /**
     * Creates the account's copy of one local playlist (when it does not have
     * one yet) and uploads its songs — what the create dialog's *Sync playlist*
     * switch asks for. Reads the playlist inside the coroutine, so songs added
     * in the same turn are uploaded too.
     */
    fun pushPlaylist(localId: String, trigger: String = "create") {
        if (!LoginManager.isLoggedIn()) {
            AppLog.log("playlists", "upload ($trigger): not signed in — the playlist stays local")
            return
        }
        // Deliberately NOT behind [run]'s in-flight guard: a user creating two
        // playlists one after the other must not have the second one dropped
        // because the first is still uploading. Each of these touches its own
        // playlist, so they cannot race on the same one.
        scope.launch {
            try {
                val created = if (pushOne(localId, trigger)) 1 else 0
                _status.value = Status(Phase.DONE, created = created)
            } catch (t: Throwable) {
                AppLog.log("playlists", "upload ($trigger) failed: ${t.message}")
                _status.value = Status(Phase.FAILED, message = t.message ?: "failed")
            }
        }
    }

    /**
     * The explicit *create them on YouTube Music* action: every local playlist
     * that is not on the account yet is created there and its songs uploaded;
     * the account's playlists are then pulled down in the same run, so both
     * sides end up mirroring each other. An explicit request, so it also runs
     * with *Auto sync with account* switched off — but never without a session.
     */
    fun uploadMissing(trigger: String = "upload") {
        if (!LoginManager.isLoggedIn()) {
            _status.value = Status(Phase.FAILED, message = "not signed in")
            return
        }
        val pending = PlaylistStore.active.filter { it.accountPlaylistId() == null }
        if (pending.isEmpty()) {
            AppLog.log("playlists", "upload ($trigger): every local playlist is already on the account")
            run(trigger) { pull(trigger) }
            return
        }
        run(trigger) {
            // Total work known up front: the upload half creates one playlist
            // per item and pushes every song it holds, the pull half then
            // mirrors the account's list. Reporting both halves keeps the
            // progress line honest for the whole run.
            val pendingSongs = pending.sumOf { it.songs.size }
            _status.value = Status(
                Phase.RUNNING,
                total = pending.size,
                songsTotal = pendingSongs,
                uploading = true,
            )
            var created = 0
            var doneCount = 0
            var songsUploaded = 0
            for (playlist in pending) {
                _status.value = _status.value.copy(current = playlist.name)
                val songsInThisOne = playlist.songs.size
                val ok = pushOne(playlist.id, trigger) { pushed, _ ->
                    // Songs of THIS playlist reported live as they land on the
                    // account, on top of the playlists already finished.
                    _status.value = _status.value.copy(
                        songsDone = songsUploaded + pushed,
                        songsTotal = pendingSongs,
                    )
                }
                if (ok) created++
                songsUploaded += songsInThisOne
                doneCount++
                _status.value = _status.value.copy(done = doneCount, created = created, songsDone = songsUploaded)
            }
            _status.value = _status.value.copy(uploading = false, current = "")
            pull(trigger).copy(created = created, done = pending.size, total = pending.size, songsDone = songsUploaded)
        }
    }

    /** One serialized run, with the shared in-flight guard and status bookkeeping. */
    private fun run(trigger: String, block: suspend () -> Status) {
        if (!inFlight.compareAndSet(false, true)) {
            AppLog.log("playlists", "$trigger: another sync is already running — skipped")
            return
        }
        _status.value = Status(Phase.RUNNING)
        scope.launch {
            try {
                _status.value = block()
            } catch (t: Throwable) {
                AppLog.log("playlists", "youtube sync ($trigger) failed: ${t.message}")
                _status.value = Status(Phase.FAILED, message = t.message ?: "failed")
            } finally {
                inFlight.set(false)
            }
        }
    }

    /**
     * Creates [localId]'s account copy and uploads the songs it holds right now.
     * Returns whether the copy was created.
     *
     * Sequential on purpose: one request per playlist, and the bulk action is
     * dozens of them — firing them together is what gets a client rate-limited.
     */
    private suspend fun pushOne(localId: String, trigger: String): Boolean = pushOne(localId, trigger, null)

    /**
     * Creates [localId]'s account copy and uploads the songs it holds right now.
     * Returns whether the copy was created.
     *
     * [onSong] is called after every song with `(uploaded, total)`, so the
     * caller can report progress while the upload runs.
     */
    private suspend fun pushOne(
        localId: String,
        trigger: String,
        onSong: ((Int, Int) -> Unit)?,
    ): Boolean {
        val playlist = PlaylistStore.get(localId) ?: return false
        if (playlist.accountPlaylistId() != null) return false
        // The account may already hold this very playlist: the two devices each
        // have their own row for it and the account copy was created from the other
        // one (the phone's "Sync playlist", or *Create on YouTube Music* here).
        // Creating a second copy is exactly the E1034 duplicate, so this row adopts
        // the account id of the copy that exists and only the songs the account does
        // not have are pushed.
        val twin = PlaylistStore.active.firstOrNull { other ->
            other.id != playlist.id && other.accountPlaylistId() != null && samePlaylist(other, playlist)
        }
        if (twin != null) {
            val account = twin.accountPlaylistId()!!
            PlaylistStore.link(playlist.id, account)
            val alreadyThere = twin.songs.map { it.id }.toSet()
            val missing = playlist.songs.filterNot { it.id in alreadyThere }
            var pushed = 0
            missing.forEach { song ->
                if (YouTube.addToPlaylist(account, song.id).isSuccess) pushed++
                onSong?.invoke(pushed, missing.size)
            }
            AppLog.log(
                "playlists",
                "upload ($trigger): '${playlist.name}' already lives on the account ($account) — " +
                    "linked instead of creating a second copy, $pushed of ${missing.size} missing song(s) pushed",
            )
            return false
        }
        val remote = runCatching { YouTube.createPlaylist(playlist.name) }.getOrNull()
        if (remote.isNullOrBlank()) {
            AppLog.log("playlists", "upload ($trigger): '${playlist.name}' was NOT created on the account")
            return false
        }
        // Linked first: a song added from now on is pushed by [songsAdded].
        PlaylistStore.link(playlist.id, remote)
        var pushed = 0
        playlist.songs.forEach { song ->
            val ok = YouTube.addToPlaylist(remote, song.id).isSuccess
            if (ok) {
                pushed++
            } else {
                AppLog.log("playlists", "  '${playlist.name}': '${song.title}' was not uploaded")
            }
            onSong?.invoke(pushed, playlist.songs.size)
        }
        AppLog.log(
            "playlists",
            "upload ($trigger): '${playlist.name}' created on the account ($remote), $pushed of " +
                "${playlist.songs.size} song(s) uploaded",
        )
        return true
    }

    /** One pull: the account's playlists into the local store. */
    private suspend fun pull(trigger: String): Status {
        _status.value = _status.value.copy(uploading = false, current = "")
        val page = YouTube.library("FEmusic_liked_playlists").getOrElse { error ->
            val why = error.message ?: error.javaClass.simpleName
            AppLog.log("playlists", "youtube sync ($trigger): playlist list failed — $why")
            return Status(Phase.FAILED, message = why)
        }
        // A playlist that is already a local one — mirrored as `yt-<id>`, or
        // created here and pushed to the account ([SyncedPlaylist.remoteId]) —
        // is NOT mirrored a second time: the account's copy of such a playlist
        // was filled BY this app, so the local playlist is its source of truth
        // (mirroring it would create the duplicate the sync exists to avoid,
        // and would overwrite local edits with the order the account holds).
        val linkedRemoteIds = PlaylistStore.active.mapNotNull { it.remoteId }.toSet()
        val remoteItems = page.items.filterIsInstance<PlaylistItem>().filterNot { item ->
            val linked = item.id in linkedRemoteIds
            if (linked) {
                AppLog.log("playlists", "  '${item.title}': already a local playlist — not mirrored again")
            }
            linked
        }
        AppLog.log("playlists", "youtube sync ($trigger): ${remoteItems.size} playlist(s) in the account")

        // Songs are fetched with a small concurrency cap: a large library is
        // dozens of requests, and firing them all at once is what gets a client
        // rate-limited.
        val gate = Semaphore(4)
        val started = _status.value
        _status.value = started.copy(
            Phase.RUNNING,
            playlists = 0,
            songs = 0,
            created = started.created,
            // A new phase: the totals now describe the account's list, not the
            // upload half that just finished.
            done = 0,
            total = remoteItems.size,
            songsDone = 0,
            songsTotal = 0,
            current = "",
            uploading = false,
        )
        var pulledSongs = 0
        val mirrored = remoteItems.map { item ->
            scope.async {
                gate.withPermit { mirrorOne(item) }
            }
        }.mapIndexed { index, deferred ->
            // Awaited in list order: the progress line then advances one
            // playlist at a time even though four are in flight.
            val result = deferred.await()
            pulledSongs += result?.songs?.size ?: 0
            _status.value = _status.value.copy(
                done = index + 1,
                current = remoteItems.getOrNull(index)?.title.orEmpty(),
                songsDone = pulledSongs,
                songsTotal = 0,
            )
            result
        }.filterNotNull()

        if (mirrored.isNotEmpty()) PlaylistStore.applyRemote(mirrored)
        val songs = mirrored.sumOf { it.songs.size }
        AppLog.log(
            "playlists",
            "youtube sync ($trigger): ${mirrored.size} of ${remoteItems.size} playlist(s) updated, $songs song(s)",
        )
        return Status(Phase.DONE, playlists = mirrored.size, songs = songs)
    }

    /**
     * Reads one account playlist and returns the local copy to write, or null
     * when the stored copy is already identical (nothing to bump).
     */
    private suspend fun mirrorOne(item: PlaylistItem): SyncedPlaylist? {
        val localId = PREFIX + item.id
        val page = YouTube.playlist(item.id).getOrElse { error ->
            AppLog.log("playlists", "  '${item.title}': songs failed — ${error.message ?: "error"}")
            return null
        }
        val songs = page.songs.map { song ->
            SyncedSong(
                id = song.id,
                title = song.title,
                artist = song.artists.joinToString(", ") { it.name },
                thumbnail = song.thumbnail,
            )
        }
        val name = item.title
        val existing = PlaylistStore.get(localId)
        if (existing != null &&
            existing.name == name &&
            existing.songs.map { it.id } == songs.map { it.id }
        ) {
            return null
        }
        AppLog.log(
            "playlists",
            "  '${item.title}': ${songs.size} song(s) mirrored" + if (existing == null) " (new)" else " (changed)",
        )
        return SyncedPlaylist(
            id = localId,
            name = name,
            songs = songs,
            updatedAt = System.currentTimeMillis(),
        )
    }
}
