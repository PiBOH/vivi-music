package com.music.vivi.desktop.player

import com.music.vivi.desktop.AppLog
import com.music.vivi.desktop.DesktopSettings
import com.music.vivi.desktop.EqualizerProcessor
import com.music.vivi.desktop.GuestSession
import com.music.vivi.desktop.NowPlaying
import com.music.vivi.desktop.ParametricEQ
import com.music.vivi.desktop.SavedEQProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

enum class RepeatMode { OFF, ALL, ONE }

/** Loading state shown in the player while a track is being resolved/downloaded. */
enum class LoadPhase { NONE, RESOLVING, DOWNLOADING }

data class PlayerState(
    val queue: List<NowPlaying> = emptyList(),
    val index: Int = -1,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val volume: Float = 1f,
    val isShuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    /** Localization key shown when stream resolution fails. */
    val errorKey: String? = null,
    /** Human-readable technical detail for playback failures. */
    val errorDetail: String? = null,
    /** Current load phase (resolving / downloading / none). */
    val loadPhase: LoadPhase = LoadPhase.NONE,
    /** True while the stream is being resolved/downloaded and audio hasn't started. */
    val isResolving: Boolean = false,
) {
    val current: NowPlaying? get() = queue.getOrNull(index)
    val isLoading: Boolean get() = loadPhase != LoadPhase.NONE
}

/**
 * Owns the [AudioPlayer] and exposes UI-facing playback state, including a
 * full queue (add/remove/next/previous/skip/auto-advance), shuffle, repeat,
 * volume and seeking. The current track is resolved to an AAC stream and
 * played on a background coroutine.
 */
class PlayerController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val player = AudioPlayer()

    /** Whether to automatically play the next queued track when one ends. */
    @Volatile var autoPlayNext: Boolean = true

    private companion object {
        /** Total resolution/playback attempts before an error is surfaced. */
        const val MAX_PLAY_ATTEMPTS = 3
    }

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    /** Most recently started tracks (newest first), used as seeds for the Home
     *  "Recommended" section (port of the mobile Daily-Discover mechanism). */
    private val _recentTracks = MutableStateFlow<List<NowPlaying>>(emptyList())
    val recentTracks: StateFlow<List<NowPlaying>> = _recentTracks.asStateFlow()

    private fun noteTrackStarted(track: NowPlaying) {
        _recentTracks.value =
            (listOf(track) + _recentTracks.value).distinctBy { it.videoId }.take(12)
    }

    /** User-initiated seeks (emitted so the sync layer can push them instantly). */
    private val _seekEvents = MutableSharedFlow<Long>(extraBufferCapacity = 16)
    val seekEvents: SharedFlow<Long> = _seekEvents.asSharedFlow()

    /**
     * Instantaneous audio level (0..1) of the decoded PCM stream, driven by
     * [AudioPlayer.onLevel]. Used by the "Visualizer" player background.
     * Updated ~20x/s (every other decoded frame); callers smooth it when
     * drawing. The decimation halves the UI recomposition load that could
     * otherwise starve the audio scheduler (macOS micro pauses/skips).
     */
    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    /**
     * Buffered fraction (0..1) of the current track: how much of the stream has
     * been downloaded/decoded so far, driven by [AudioPlayer.onBufferedFraction].
     * 1f when fully cached / not streaming (the UI then hides the secondary
     * "buffered" segment on the seek bars). Reset to 1f whenever no stream is
     * loaded and to 0f while a fresh download starts.
     */
    private val _bufferedFraction = MutableStateFlow(1f)
    val bufferedFraction: StateFlow<Float> = _bufferedFraction.asStateFlow()

    /**
     * Fraction (0..1) of the current track the user scrubbed to while its
     * duration was still unknown (track loaded from the restored queue but
     * never played). Keeps the seek bar thumb at the scrubbed point so the
     * seek is visible; playback starts from it via [pendingStartFraction].
     * Cleared when playback begins, the track changes, or the queue resets.
     */
    private val _pendingSeekFraction = MutableStateFlow<Float?>(null)
    val pendingSeekFraction: StateFlow<Float?> = _pendingSeekFraction.asStateFlow()

    init {
        // Restore the saved shuffle/repeat state when "remember" is enabled.
        val s = DesktopSettings.load()
        if (s.rememberShuffleRepeat) {
            _state.value = PlayerState(
                isShuffle = s.isShuffle,
                repeatMode = repeatModeFromKey(s.repeatModeKey),
            )
        }
        // Restore the in-app (VIVI) volume: it used to reset to 100% on every
        // launch because the volume was never persisted.
        if (s.playerVolume in 0f..1f) {
            player.setVolume(s.playerVolume)
            _state.value = _state.value.copy(volume = s.playerVolume)
        }
        // Feed the audio-reactive visualizer from the decoded PCM stream.
        player.onLevel = { level -> _audioLevel.value = level }
        // Feed the secondary "buffered" segment of the seek bars (YouTube-style).
        player.onBufferedFraction = { frac -> _bufferedFraction.value = frac }
    }

    /** Resets the visualizer level to silence (e.g. on pause/stop). */
    fun resetAudioLevel() {
        _audioLevel.value = 0f
    }

    /** Monotonic token identifying the active play session. */
    private var playToken = 0

    /**
     * videoId currently loaded (or being resolved/loaded) in the [AudioPlayer].
     * A track restored from the persistent queue has no stream loaded, so
     * pressing play must trigger a real load instead of a no-op `resume()`.
     */
    @Volatile
    private var loadedVideoId: String? = null

    /**
     * Wall-clock time of the latest LOCAL user play/navigation command (toggle,
     * next, previous, skip, play). The sync layer uses it so a freshly pressed
     * play wins over a peer snapshot that reflects the pre-action state — the
     * peer keeps pushing its own "paused" echo until it processes our play, and
     * applying that echo pauses the track the user just started (the "must press
     * play twice when paired" bug). Remote-applied snapshots never touch this.
     */
    @Volatile
    var lastLocalPlayIntentAt: Long = 0L

    /** Back-navigation history used by "previous" in shuffle mode. */
    private val previousStack = ArrayDeque<Int>()

    /**
     * Start position chosen while the track's duration was still unknown (a
     * 0..1 fraction of the track). A loaded-but-never-played track can be
     * scrubbed before its length is known; the fraction is applied to the real
     * duration the moment playback starts. Consumed by the next [playAtAttempt].
     */
    private var pendingStartFraction: Float? = null

    fun play(track: NowPlaying) {
        AppLog.log("playback", "play: '${track.title}' [${track.videoId}]")
        lastLocalPlayIntentAt = System.currentTimeMillis()
        playAt(listOf(track), 0)
    }

    fun playAll(tracks: List<NowPlaying>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        AppLog.log("playback", "playAll: ${tracks.size} tracks, start at $startIndex ('${tracks[startIndex].title}') ")
        lastLocalPlayIntentAt = System.currentTimeMillis()
        playAt(tracks, startIndex.coerceIn(0, tracks.lastIndex))
    }

    /** Appends a track to the queue; if nothing is playing, starts it. */
    fun addToQueue(track: NowPlaying) {
        val s = _state.value
        AppLog.log("queue", "addToQueue: '${track.title}' [${track.videoId}]")
        if (s.current == null) {
            play(track)
        } else {
            _state.update { it.copy(queue = it.queue + track) }
        }
    }

    /** Appends a list of tracks to the queue; if nothing is playing, starts them. */
    fun addAllToQueue(tracks: List<NowPlaying>) {
        val s = _state.value
        AppLog.log("queue", "addAllToQueue: ${tracks.size} tracks")
        if (s.current == null) {
            playAll(tracks)
        } else {
            _state.update { it.copy(queue = it.queue + tracks) }
        }
    }

    /**
     * Inserts [track] right after the currently playing item ("play next",
     * used by Listen Together suggestions and QUEUE_ADD insert_next). If the
     * queue is empty, starts the track instead.
     */
    fun insertNext(track: NowPlaying) {
        val s = _state.value
        if (s.current == null) {
            play(track)
            return
        }
        val idx = (s.index + 1).coerceAtMost(s.queue.size)
        _state.update { it.copy(queue = it.queue.toMutableList().apply { add(idx, track) }) }
    }

    /**
     * Replaces the whole queue WITHOUT restarting the current track: if the
     * currently playing [videoId] is still present, the index is remapped and
     * playback/position are preserved (used by Listen Together SYNC_QUEUE so a
     * guest doesn't hear a glitch when the host adds/removes songs). Falls back
     * to a full [applyRemotePlayback] when the current track is gone.
     */
    fun replaceQueuePreservingCurrent(newQueue: List<NowPlaying>, positionMs: Long, isPlaying: Boolean) {
        if (newQueue.isEmpty()) {
            clearQueue()
            return
        }
        val s = _state.value
        val currentId = s.current?.videoId
        if (currentId == null) {
            restoreQueue(newQueue, 0)
            return
        }
        val newIndex = newQueue.indexOfFirst { it.videoId == currentId }
        if (newIndex < 0) {
            applyRemotePlayback(newQueue, 0, positionMs, isPlaying)
        } else {
            val wasPlaying = s.isPlaying
            _state.update {
                it.copy(queue = newQueue, index = newIndex, isPlaying = wasPlaying || isPlaying)
            }
            if (!wasPlaying && isPlaying) {
                // Current track still loaded: just resume instead of reloading.
                player.resume()
                _state.update { st -> st.copy(isPlaying = true) }
            }
        }
    }

    fun next() {
        val s = _state.value
        if (s.queue.isEmpty()) return
        AppLog.log("playback", "next (index ${s.index + 1} of ${s.queue.size})")
        // At the end of the queue "next" wraps back to the first track, so the
        // user never hits a dead button (repeat mode only affects auto-advance).
        val nextIndex = when {
            s.queue.size == 1 -> 0
            s.isShuffle -> randomIndexExcluding(s.queue.size, s.index)
            s.index < s.queue.lastIndex -> s.index + 1
            else -> 0
        }
        lastLocalPlayIntentAt = System.currentTimeMillis()
        previousStack.addLast(s.index)
        playAt(s.queue, nextIndex)
    }

    fun previous() {
        val s = _state.value
        if (s.queue.isEmpty()) return
        AppLog.log("playback", "previous (index ${s.index - 1})")
        val prevIndex = when {
            s.isShuffle -> previousStack.removeLastOrNull() ?: randomIndexExcluding(s.queue.size, s.index)
            s.index > 0 -> s.index - 1
            else -> s.queue.lastIndex
        }
        lastLocalPlayIntentAt = System.currentTimeMillis()
        playAt(s.queue, prevIndex)
    }

    fun skipTo(index: Int) {
        val s = _state.value
        if (index in s.queue.indices) {
            AppLog.log("queue", "skipTo: index $index")
            lastLocalPlayIntentAt = System.currentTimeMillis()
            previousStack.addLast(s.index)
            playAt(s.queue, index)
        }
    }

    fun removeAt(index: Int) {
        val s = _state.value
        if (index !in s.queue.indices) return
        AppLog.log("queue", "removeAt: index $index")
        if (index !in s.queue.indices) return
        val newQueue = s.queue.toMutableList().apply { removeAt(index) }
        when {
            newQueue.isEmpty() -> {
                playToken++
                player.stop()
                loadedVideoId = null
                _bufferedFraction.value = 1f
                _pendingSeekFraction.value = null
                _state.value = PlayerState(volume = s.volume, isShuffle = s.isShuffle, repeatMode = s.repeatMode)
            }
            index < s.index -> _state.update { it.copy(queue = newQueue, index = it.index - 1) }
            index == s.index -> playAt(newQueue, s.index.coerceAtMost(newQueue.lastIndex))
            else -> _state.update { it.copy(queue = newQueue) }
        }
    }

    fun clearQueue() {
        val s = _state.value
        AppLog.log("queue", "clearQueue (${s.queue.size} tracks)")
        playToken++
        player.stop()
        loadedVideoId = null
        _bufferedFraction.value = 1f
        _pendingSeekFraction.value = null
        _state.value = PlayerState(volume = s.volume, isShuffle = s.isShuffle, repeatMode = s.repeatMode)
    }

    /**
     * Applies a new ordering of the same queue items (drag-to-reorder),
     * keeping the currently playing track selected.
     */
    fun reorder(newQueue: List<NowPlaying>) {
        val s = _state.value
        if (newQueue.size != s.queue.size) return
        val currentId = s.current?.videoId
        val newIndex = newQueue.indexOfFirst { it.videoId == currentId }.takeIf { it != -1 } ?: s.index
        _state.update { it.copy(queue = newQueue, index = newIndex) }
    }

    fun toggle() {
        val s = _state.value
        if (s.current == null) return
        AppLog.log("playback", if (s.isPlaying) "pause" else "play toggle")
        lastLocalPlayIntentAt = System.currentTimeMillis()
        if (s.isPlaying) {
            player.pause()
            _state.update { it.copy(isPlaying = false) }
        } else {
            startCurrent(s)
        }
    }

    /**
     * Starts the current track. If its stream isn't loaded yet (e.g. it was
     * restored from the persistent queue, or a previous load failed), trigger a
     * real resolution + load instead of a no-op `resume()`.
     */
    private fun startCurrent(s: PlayerState) {
        if (loadedVideoId != s.current?.videoId) {
            // A track that already finished keeps its end position: pressing
            // play again must restart it from the beginning, not from the end
            // (restarting at the end instantly "completes" and stops again,
            // which looked like the play button not working).
            val startAt = if (s.durationMs > 0 && s.positionMs >= s.durationMs) 0L else s.positionMs
            playAt(s.queue, s.index, startAtMs = startAt, startPaused = false)
        } else {
            player.resume()
            _state.update { it.copy(isPlaying = true) }
        }
    }

    fun stop() {
        playToken++
        player.stop()
        loadedVideoId = null
        _bufferedFraction.value = 1f
        _pendingSeekFraction.value = null
        _state.update { it.copy(isPlaying = false, positionMs = 0L) }
    }

    fun seekTo(ms: Long) {
        AppLog.log("playback", "seek to ${ms}ms")
        seekInternal(ms, startStream = true)?.let { _seekEvents.tryEmit(it) }
    }

    /**
     * Applies a remote seek in place (same track) without emitting a seek event
     * and without restarting the stream, then matches the peer's play/pause.
     *
     * When [toleranceMs] > 0 and the requested position is already within that
     * tolerance while playing, the seek is skipped (only play/pause is matched)
     * so periodic re-sync ticks don't cause audible seek glitches.
     */
    fun seekRemote(positionMs: Long, isPlaying: Boolean, toleranceMs: Long = 0L) {
        if (_state.value.current == null) return
        if (toleranceMs > 0 && isPlaying &&
            abs(positionMs - _state.value.positionMs) <= toleranceMs
        ) {
            setPlaying(isPlaying)
            return
        }
        seekInternal(positionMs)
        setPlaying(isPlaying)
    }

    /**
     * Applies a periodic drift-tic correction: matches play/pause, and only
     * seeks FORWARD (catch up) when the remote position is ahead by more than
     * [toleranceMs]. It never seeks backward, so a device that is ahead (the
     * leader) isn't dragged back by the follower's slightly-stale position.
     */
    fun seekRemoteCatchUp(positionMs: Long, isPlaying: Boolean, toleranceMs: Long) {
        val s = _state.value
        if (s.current == null) {
            setPlaying(isPlaying)
            return
        }
        if (isPlaying && positionMs - s.positionMs > toleranceMs) {
            seekInternal(positionMs)
        }
        setPlaying(isPlaying)
    }

    /**
     * Applies a seek. [startStream] is true only for local user scrubs: when
     * the track's stream isn't loaded yet, the scrub itself kicks off the
     * resolution/load (YouTube behavior — scrubbing an unloaded video starts
     * buffering it) and playback begins from the scrubbed position. Remote
     * seeks keep the old remember-only path and let [setPlaying] decide.
     */
    private fun seekInternal(ms: Long, startStream: Boolean = false): Long? {
        val s = _state.value
        if (s.current == null) return null
        // Duration unknown (track loaded but never resolved): the seek bar has
        // no real time range, so the value is a 0..1000 encoding of the desired
        // START FRACTION. It is remembered and applied to the stream duration
        // when playback actually begins — pressing play then starts from the
        // scrubbed point.
        // The fraction encoding is only produced by the local seek bars (whose
        // range is 0..1000 while the duration is unknown); a remote seek in real
        // milliseconds is much larger and must not be reinterpreted.
        if (s.durationMs <= 0L && ms in 0..1000L) {
            val fraction = (ms / 1000f).coerceIn(0f, 1f)
            pendingStartFraction = fraction
            _pendingSeekFraction.value = fraction
            _state.update { it.copy(positionMs = 0L) }
            // Scrubbing a never-resolved track starts its stream right away;
            // [playAtAttempt] applies the pending fraction to the real duration
            // the moment it becomes known.
            if (startStream && !s.isResolving && loadedVideoId != s.current?.videoId) {
                playAt(s.queue, s.index, startAtMs = 0L, startPaused = false)
            }
            return null
        }
        // A real (time-based) seek means the duration is known: drop any
        // pending fraction so a stale thumb can't linger on a later track.
        _pendingSeekFraction.value = null
        val target = ms.coerceIn(0L, s.durationMs)
        if (loadedVideoId == s.current?.videoId && player.hasLoadedStream()) {
            player.seekTo(target)
        } else if (startStream && !s.isResolving) {
            // Stream not loaded yet (restored queue / never started): the scrub
            // kicks off the resolution so the position becomes real and playback
            // starts from it. While a resolution is already in flight the target
            // is just remembered — [playAtAttempt] honors it (`st.positionMs`)
            // when the stream is ready, so dragging during resolution works too.
            _state.update { it.copy(positionMs = target) }
            playAt(s.queue, s.index, startAtMs = target, startPaused = false)
            return null
        }
        _state.update { it.copy(positionMs = target) }
        return target
    }

    private fun setPlaying(playing: Boolean) {
        val s = _state.value
        if (s.isPlaying == playing) return
        if (playing) {
            startCurrent(s)
        } else {
            player.pause()
            _state.update { it.copy(isPlaying = false) }
        }
    }

    fun setVolume(v: Float) {
        val clamped = v.coerceIn(0f, 1f)
        AppLog.log("volume", "set to ${(clamped * 100).roundToInt()}%")
        player.setVolume(clamped)
        _state.update { it.copy(volume = clamped) }
        // Persist the volume so it survives restarts instead of resetting to
        // the default maximum (also covers volumes applied by device sync).
        runCatching { DesktopSettings.update { it.copy(playerVolume = clamped) } }
    }

    /**
     * Applies an EQ profile to the PCM stream (null = equalization off). The
     * processor is a pure add-on to the output write path: when null the audio
     * path is byte-identical to before, so the frozen playback core is untouched.
     */
    fun setEqualizer(profile: SavedEQProfile?) {
        player.equalizer = EqualizerProcessor().apply {
            setProfile(profile?.let {
                ParametricEQ(preamp = it.preamp, bands = it.bands)
            })
        }
    }

    /** True when [videoId] already has a valid on-disk cache file (prefetch check). */
    fun isCached(videoId: String): Boolean = player.isCached(videoId)

    /** Downloads [streams] for [videoId] without playing (look-ahead prefetch). */
    fun prefetch(streams: List<StreamResolver.ResolvedStream>, videoId: String) =
        player.prefetch(streams, videoId)

    fun toggleShuffle() {
        val s = _state.value
        val newShuffle = !s.isShuffle
        AppLog.log("playback", "shuffle ${if (newShuffle) "on" else "off"}")
        if (!newShuffle) previousStack.clear()
        _state.update { it.copy(isShuffle = newShuffle) }
        persistShuffleRepeat()
    }

    fun cycleRepeatMode() {
        val s = _state.value
        val next = when (s.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        AppLog.log("playback", "repeat mode → $next")
        _state.update { it.copy(repeatMode = next) }
        persistShuffleRepeat()
    }

    /** Sets the shuffle state (used when applying a remote device-sync snapshot). */
    fun setShuffle(enabled: Boolean) {
        val s = _state.value
        if (s.isShuffle == enabled) return
        if (!enabled) previousStack.clear()
        _state.update { it.copy(isShuffle = enabled) }
        persistShuffleRepeat()
    }

    /** Sets the repeat mode (used when applying a remote device-sync snapshot). */
    fun setRepeatMode(mode: RepeatMode) {
        val s = _state.value
        if (s.repeatMode == mode) return
        _state.update { it.copy(repeatMode = mode) }
        persistShuffleRepeat()
    }

    /** Restores a saved queue without starting playback (persistent queue). */
    fun restoreQueue(tracks: List<NowPlaying>, index: Int) {
        if (tracks.isEmpty()) return
        playToken++
        player.stop()
        loadedVideoId = null
        _bufferedFraction.value = 1f
        _pendingSeekFraction.value = null
        val idx = index.coerceIn(0, tracks.lastIndex)
        _state.update {
            it.copy(
                queue = tracks,
                index = idx,
                isPlaying = false,
                positionMs = 0L,
                // Report the saved duration so the seek slider is usable
                // immediately, even before the stream is resolved.
                durationMs = tracks[idx].durationMs,
            )
        }
    }

    /**
     * Applies a remote playback snapshot (from device sync): replaces the
     * queue, jumps to the given index and position, and starts/pauses.
     */
    fun applyRemotePlayback(
        tracks: List<NowPlaying>,
        index: Int,
        positionMs: Long,
        isPlaying: Boolean,
        isResolving: Boolean = false,
    ) {
        if (tracks.isEmpty()) return
        val idx = index.coerceIn(0, tracks.lastIndex)
        // Hold while the peer is still resolving its stream (symmetric with the
        // mobile, which also holds on `isResolving`). `resumeWhenReady` records
        // the peer's ultimate intent: when it wants to play we only hold until
        // our own stream is ready, then auto-start instead of emitting a
        // transient isPlaying=false snapshot that would pause the peer.
        val startPaused = !isPlaying || isResolving
        playAt(
            tracks,
            idx,
            startAtMs = positionMs.coerceAtLeast(0L),
            startPaused = startPaused,
            resumeWhenReady = isPlaying,
        )
    }

    private fun playAt(
        tracks: List<NowPlaying>,
        index: Int,
        startAtMs: Long = 0L,
        startPaused: Boolean = false,
        resumeWhenReady: Boolean = !startPaused,
    ) = playAtAttempt(tracks, index, startAtMs, startPaused, resumeWhenReady, attempt = 0)

    private fun playAtAttempt(
        tracks: List<NowPlaying>,
        index: Int,
        startAtMs: Long,
        startPaused: Boolean,
        resumeWhenReady: Boolean,
        attempt: Int,
    ) {
        val track = tracks[index]
        val token = ++playToken
        loadedVideoId = track.videoId
        noteTrackStarted(track)
        scope.launch {
            player.stop()
            _state.value = PlayerState(
                queue = tracks,
                index = index,
                isPlaying = !startPaused,
                positionMs = startAtMs,
                // Report the known duration immediately so the seek slider has
                // a correct range before the stream resolves (otherwise it shows
                // as disabled / stuck at the end while positionMs > 0).
                durationMs = track.durationMs,
                volume = _state.value.volume,
                isShuffle = _state.value.isShuffle,
                repeatMode = _state.value.repeatMode,
                loadPhase = LoadPhase.RESOLVING,
                isResolving = true,
            )

            // A track that was already downloaded in full (audio cache) plays
            // straight from disk — no network resolution, no "resolving"
            // spinner, and no re-download on every restart with "cache
            // forever". Only tracks without a valid cache file need a stream
            // URL to fetch.
            val alreadyCached = player.isCached(track.videoId)
            AppLog.log("playback", "resolving '${track.title}' [${track.videoId}] (attempt ${attempt + 1}/${MAX_PLAY_ATTEMPTS}, cached=$alreadyCached)")
            val streams = if (alreadyCached) {
                emptyList()
            } else {
                StreamResolver.resolveAacStream(
                    track.videoId,
                    StreamResolver.AudioQuality.from(DesktopSettings.load().audioQuality),
                )
            }
            if (streams.isEmpty() && !alreadyCached) {
                if (attempt + 1 < MAX_PLAY_ATTEMPTS) {
                    // Bot detection / transient resolution failure: rotate the
                    // guest identity and try a fresh resolution.
                    AppLog.log("playback", "resolution failed, rotating guest and retrying")
                    GuestSession.rotate()
                    playAtAttempt(tracks, index, startAtMs, startPaused, resumeWhenReady, attempt + 1)
                } else {
                    loadedVideoId = null
                    _bufferedFraction.value = 1f
                    AppLog.log("playback", "resolution failed after $MAX_PLAY_ATTEMPTS attempts — surfacing stream_error")
                    _state.update { it.copy(isPlaying = false, errorKey = "stream_error", errorDetail = null, loadPhase = LoadPhase.NONE, isResolving = false) }
                }
                return@launch
            }
            AppLog.log("playback", "stream ready for '${track.title}' (${if (alreadyCached) "cache" else "network"})")
            _state.update {
                it.copy(
                    errorKey = null,
                    errorDetail = null,
                    // Cached tracks keep the RESOLVING phase only until the
                    // first position report flips it (the resumeWhenReady
                    // logic relies on that transition), so no spinner is
                    // actually shown for them.
                    loadPhase = if (alreadyCached) it.loadPhase else LoadPhase.DOWNLOADING,
                )
            }

            // If the seek bar was scrubbed while this track was still being
            // resolved/downloaded (no stream loaded yet), honor that position
            // instead of the original startAtMs — otherwise the seek is lost
            // the moment the stream starts from its planned offset. A scrub
            // done while the duration was still unknown is a fraction: apply it
            // to the metadata duration when available, otherwise forward it to
            // the player which resolves it against the stream duration.
            val st = _state.value
            val pendingFraction = pendingStartFraction
            pendingStartFraction = null
            // Playback is starting: the seek bar switches to the real timeline,
            // so the pending scrub indicator is no longer needed.
            _pendingSeekFraction.value = null
            val sameTrack = st.index == index && st.current?.videoId == track.videoId
            var startFraction: Float? = null
            val effectiveStartAt = when {
                pendingFraction != null && pendingFraction > 0f && sameTrack -> {
                    val knownDur = track.durationMs.takeIf { it > 0 }
                        ?: st.durationMs.takeIf { it > 0 } ?: 0L
                    if (knownDur > 0) {
                        (pendingFraction * knownDur).toLong()
                    } else {
                        startFraction = pendingFraction
                        0L
                    }
                }
                sameTrack && st.positionMs > 0L -> st.positionMs.coerceAtLeast(0L)
                else -> startAtMs
            }
            _bufferedFraction.value = if (alreadyCached) 1f else 0f

            player.play(
                streams = streams,
                cacheKey = track.videoId,
                startAtMs = effectiveStartAt,
                startAtFraction = startFraction,
                startPaused = startPaused,
                onError = { msg ->
                    AppLog.log("playback", "playback error: $msg")
                    // Evict the cached resolution: a stale, single-use
                    // googlevideo URL must not be returned again by the retry.
                    StreamResolver.invalidate(track.videoId)
                    // Also evict the audio cache file: a truncated/interrupted
                    // download plays a fragment and "ends" early, which showed
                    // up as tracks stopping after a few seconds and skipping by
                    // themselves. The retry below re-downloads a clean copy.
                    player.evictCache(track.videoId)
                    if (attempt + 1 < MAX_PLAY_ATTEMPTS) {
                        // Download/decode failure (e.g. stale googlevideo 403):
                        // rotate the guest identity and re-resolve, then retry.
                        scope.launch {
                            GuestSession.rotate()
                            playAtAttempt(tracks, index, startAtMs, startPaused, resumeWhenReady, attempt + 1)
                        }
                    } else {
                        loadedVideoId = null
                        _bufferedFraction.value = 1f
                        AppLog.log("playback", "giving up after $MAX_PLAY_ATTEMPTS attempts: $msg")
                        _state.update { s ->
                            if (s.index == index && s.queue.getOrNull(index)?.videoId == track.videoId) {
                                s.copy(isPlaying = false, errorDetail = msg, loadPhase = LoadPhase.NONE, isResolving = false)
                            } else s
                        }
                    }
                },
                onPosition = { pos ->
                    // First position report means audio is actually ready. When
                    // we were held only because the peer was still resolving
                    // (resumeWhenReady), resume now so the paired device never
                    // sees a transient isResolving=false/isPlaying=false pause.
                    if (resumeWhenReady && _state.value.isResolving) player.resume()
                    _state.update { s ->
                        if (s.index == index && s.queue.getOrNull(index)?.videoId == track.videoId) {
                            if (s.isResolving) {
                                // Only on the resolving→ready transition apply the
                                // held-back intent. After that, keep the latest
                                // user/peer play-pause choice: a stale
                                // resumeWhenReady must not overwrite a manual
                                // toggle (that left the button stuck on "play"
                                // while audio kept playing).
                                s.copy(positionMs = pos, isResolving = false, isPlaying = resumeWhenReady)
                            } else {
                                s.copy(positionMs = pos)
                            }
                        } else s
                    }
                },
                onDuration = { dur ->
                    _state.update { s ->
                        if (s.index == index && s.queue.getOrNull(index)?.videoId == track.videoId) {
                            // Backfill the real duration into the queue item too:
                            // the persistent queue then keeps durations, so a
                            // track restored on the next launch already has a
                            // correct seek range (no more 0..1 "dead" bar).
                            val queue = s.queue.toMutableList()
                            queue[index] = queue[index].copy(durationMs = dur)
                            s.copy(queue = queue, durationMs = dur, loadPhase = LoadPhase.NONE)
                        } else s
                    }
                },
                onComplete = {
                    if (token != playToken) return@play
                    val s = _state.value
                    if (s.index == index && s.queue.getOrNull(index)?.videoId == track.videoId) {
                        handleTrackEnd(s, index, token)
                    }
                },
            )
        }
    }

    private fun handleTrackEnd(s: PlayerState, index: Int, token: Int) {
        AppLog.log("playback", "track ended (repeat=${s.repeatMode}, autoPlayNext=$autoPlayNext)")
        when {
            s.repeatMode == RepeatMode.ONE -> {
                if (token == playToken) playAt(s.queue, index)
            }
            autoPlayNext -> {
                val nextIndex = when {
                    s.queue.size == 1 && s.repeatMode != RepeatMode.ALL -> -1
                    s.isShuffle -> randomIndexExcluding(s.queue.size, s.index)
                    s.index < s.queue.lastIndex -> s.index + 1
                    s.repeatMode == RepeatMode.ALL -> 0
                    else -> -1
                }
                if (nextIndex >= 0) {
                    // Auto-advance is also a local play intent: the peer's
                    // pre-advance "paused" echo must not pause the new track.
                    AppLog.log("playback", "auto-advancing to index $nextIndex")
                    lastLocalPlayIntentAt = System.currentTimeMillis()
                    previousStack.addLast(s.index)
                    playAt(s.queue, nextIndex)
                } else {
                    loadedVideoId = null
                    _state.update { it.copy(isPlaying = false) }
                }
            }
            else -> {
                loadedVideoId = null
                _state.update { it.copy(isPlaying = false) }
            }
        }
    }

    private fun randomIndexExcluding(size: Int, exclude: Int): Int {
        if (size <= 1) return 0
        var idx = Random.nextInt(size)
        while (idx == exclude) idx = Random.nextInt(size)
        return idx
    }

    private fun persistShuffleRepeat() {
        val s = DesktopSettings.load()
        if (s.rememberShuffleRepeat) {
            DesktopSettings.update {
                it.copy(
                    isShuffle = _state.value.isShuffle,
                    repeatModeKey = _state.value.repeatMode.name,
                )
            }
        }
    }

    private fun repeatModeFromKey(key: String): RepeatMode =
        runCatching { RepeatMode.valueOf(key) }.getOrDefault(RepeatMode.OFF)
}
