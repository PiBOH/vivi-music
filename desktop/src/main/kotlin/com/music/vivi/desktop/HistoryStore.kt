package com.music.vivi.desktop

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import java.io.File

/** One locally remembered playback, newest first in the file. */
@Serializable
data class HistoryEntry(
    val videoId: String,
    val title: String,
    val artist: String,
    val thumbnail: String? = null,
    val durationMs: Long = 0L,
    val album: String? = null,
    /** Wall-clock time the track started, used to sort and to age entries out. */
    val playedAt: Long = 0L,
    /** How many times the track was started (the list keeps one row per track). */
    val plays: Int = 1,
)

/** Search terms the user actually acted on, newest first. */
@Serializable
data class SearchEntry(
    val term: String,
    val at: Long = 0L,
    val uses: Int = 1,
)

@Serializable
private data class HistoryFile(
    val tracks: List<HistoryEntry> = emptyList(),
    val searches: List<SearchEntry> = emptyList(),
)

/**
 * JSON-backed store for the local history: `~/.vivimusic/history.json`.
 *
 * Before this store existed the history lived in memory only, so it was empty
 * again on every launch ("the history does not really remember what I search")
 * and the Home recommendation seeds were lost with it. The file keeps the
 * 200 most recent tracks and the 50 most recent searches; the same entries ride
 * along in the log-export settings dump so a tester's history can be inspected.
 *
 * Both lists are written by the *debounced* writers below: playback can start
 * several tracks in a row when the user skips, and rewriting the file on every
 * skip would add pointless disk traffic to the audio path.
 */
object HistoryStore {
    private const val MAX_TRACKS = 200
    private const val MAX_SEARCHES = 50

    private val json = sharedJsonPretty

    private val file = File(System.getProperty("user.home"), ".vivimusic/history.json").apply {
        parentFile?.mkdirs()
    }

    private var state: HistoryFile = load()

    private val _tracks = MutableStateFlow(state.tracks)
    /** Newest-first locally played tracks. */
    val tracks: StateFlow<List<HistoryEntry>> = _tracks.asStateFlow()

    private val _searches = MutableStateFlow(state.searches)
    /** Newest-first search terms the user acted on. */
    val searches: StateFlow<List<SearchEntry>> = _searches.asStateFlow()

    private val writeLock = Any()
    private var pendingWrite = false

    /** Records a started track, collapsing repeats of the same video id. */
    fun recordTrack(videoId: String, title: String, artist: String, thumbnail: String?, durationMs: Long, album: String?) {
        if (videoId.isBlank()) return
        val now = System.currentTimeMillis()
        val existing = state.tracks.firstOrNull { it.videoId == videoId }
        val entry = HistoryEntry(
            videoId = videoId,
            title = title,
            artist = artist,
            thumbnail = thumbnail,
            durationMs = durationMs,
            album = album,
            playedAt = now,
            plays = (existing?.plays ?: 0) + 1,
        )
        val updated = (listOf(entry) + state.tracks.filterNot { it.videoId == videoId }).take(MAX_TRACKS)
        state = state.copy(tracks = updated)
        _tracks.value = updated
        scheduleWrite()
    }

    fun recordSearch(term: String) {
        val clean = term.trim()
        if (clean.isBlank()) return
        val existing = state.searches.firstOrNull { it.term.equals(clean, ignoreCase = true) }
        val entry = SearchEntry(
            term = clean,
            at = System.currentTimeMillis(),
            uses = (existing?.uses ?: 0) + 1,
        )
        val updated = (listOf(entry) + state.searches.filterNot { it.term.equals(clean, ignoreCase = true) })
            .take(MAX_SEARCHES)
        state = state.copy(searches = updated)
        _searches.value = updated
        scheduleWrite()
    }

    fun clearTracks() {
        state = state.copy(tracks = emptyList())
        _tracks.value = emptyList()
        write()
    }

    fun clearSearches() {
        state = state.copy(searches = emptyList())
        _searches.value = emptyList()
        write()
    }

    fun replaceAll(tracks: List<HistoryEntry>, searches: List<SearchEntry>) {
        state = HistoryFile(tracks = tracks.take(MAX_TRACKS), searches = searches.take(MAX_SEARCHES))
        _tracks.value = state.tracks
        _searches.value = state.searches
        write()
    }

    /**
     * Coalesces the writes: skipping tracks quickly used to rewrite the file on
     * every start. The flag is reset by the debounce thread, so playback never
     * blocks on the disk and a burst of skips costs one write.
     */
    private fun scheduleWrite() {
        synchronized(writeLock) {
            if (pendingWrite) return
            pendingWrite = true
        }
        Thread {
            try {
                Thread.sleep(1500)
            } catch (_: InterruptedException) {
                // Write anyway: the process is going away.
            }
            synchronized(writeLock) { pendingWrite = false }
            write()
        }.apply {
            isDaemon = true
            name = "vivi-history-writer"
        }.start()
    }

    /** Writes immediately; called at shutdown so a fast exit cannot lose a play. */
    fun flush() {
        synchronized(writeLock) { pendingWrite = false }
        write()
    }

    private fun load(): HistoryFile = try {
        if (file.exists()) json.decodeFromString(file.readText()) else HistoryFile()
    } catch (_: Exception) {
        HistoryFile()
    }

    private fun write() {
        try {
            file.writeText(json.encodeToString(state))
        } catch (e: Exception) {
            AppLog.log("history", "could not save history: ${e.message}")
        }
    }
}
