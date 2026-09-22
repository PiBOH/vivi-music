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
 * Reads only: creating, renaming or deleting a playlist still happens locally
 * (and reaches the account's copy through its own actions when the user makes
 * them from the online screens).
 */
object PlaylistSync {
    enum class Phase { IDLE, RUNNING, DONE, FAILED }

    /** Result of the last run, for the account screen's status line. */
    data class Status(
        val phase: Phase = Phase.IDLE,
        val playlists: Int = 0,
        val songs: Int = 0,
        val message: String = "",
    )

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
     * Runs one sync. Concurrent calls are ignored (the sidebar opens, the
     * login lands and the account screen opens within the same second).
     */
    fun sync(trigger: String = "manual") {
        if (!isEnabled()) {
            _status.value = Status(Phase.IDLE, message = "sync disabled or not signed in")
            return
        }
        if (!inFlight.compareAndSet(false, true)) return
        _status.value = Status(Phase.RUNNING)
        scope.launch {
            try {
                val page = YouTube.library("FEmusic_liked_playlists").getOrElse { error ->
                    val why = error.message ?: error.javaClass.simpleName
                    AppLog.log("playlists", "youtube sync ($trigger): playlist list failed — $why")
                    _status.value = Status(Phase.FAILED, message = why)
                    return@launch
                }
                val remoteItems = page.items.filterIsInstance<PlaylistItem>()
                AppLog.log("playlists", "youtube sync ($trigger): ${remoteItems.size} playlist(s) in the account")

                // Songs are fetched with a small concurrency cap: a large
                // library is dozens of requests, and firing them all at once
                // is what gets a client rate-limited.
                val gate = Semaphore(4)
                val mirrored = remoteItems.map { item ->
                    scope.async {
                        gate.withPermit { mirrorOne(item) }
                    }
                }.awaitAll().filterNotNull()

                if (mirrored.isNotEmpty()) PlaylistStore.applyRemote(mirrored)
                val songs = mirrored.sumOf { it.songs.size }
                AppLog.log(
                    "playlists",
                    "youtube sync ($trigger): ${mirrored.size} of ${remoteItems.size} playlist(s) updated, $songs song(s)",
                )
                _status.value = Status(Phase.DONE, playlists = mirrored.size, songs = songs)
            } catch (t: Throwable) {
                AppLog.log("playlists", "youtube sync ($trigger) failed: ${t.message}")
                _status.value = Status(Phase.FAILED, message = t.message ?: "failed")
            } finally {
                inFlight.set(false)
            }
        }
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
