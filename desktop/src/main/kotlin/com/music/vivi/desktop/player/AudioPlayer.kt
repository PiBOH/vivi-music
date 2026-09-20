package com.music.vivi.desktop.player

import com.music.vivi.desktop.AppLog
import com.music.vivi.desktop.EqualizerProcessor
import net.sourceforge.jaad.aac.Decoder
import net.sourceforge.jaad.aac.SampleBuffer
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.io.SeekableByteChannel
import org.jcodec.containers.mp4.MP4Util
import org.jcodec.containers.mp4.boxes.Header
import org.jcodec.containers.mp4.boxes.MovieFragmentBox
import org.jcodec.containers.mp4.boxes.NodeBox
import org.jcodec.containers.mp4.boxes.TrackFragmentHeaderBox
import org.jcodec.containers.mp4.boxes.TrunBox
import org.jcodec.containers.mp4.demuxer.AbstractMP4DemuxerTrack
import org.jcodec.containers.mp4.demuxer.MP4Demuxer
import org.jcodec.containers.mp4.demuxer.MP4DemuxerTrackMeta
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine
import kotlin.math.roundToInt

/**
 * Self-contained AAC player: downloads the MP4 stream to a local cache file,
 * demuxes the (fragmented/DASH) MP4 container with `jcodec`, decodes the raw
 * AAC frames to PCM with the bundled `jaad` decoder, and plays them through
 * Java Sound. No native libraries or external binaries are required.
 *
 * Playback is **progressive**: the stream is downloaded to a unique `.part`
 * file in the background while the decoder starts as soon as the first audio
 * fragment is on disk, so a track begins in seconds instead of after the whole
 * file has downloaded. The sample table is grown incrementally as new `moof`
 * fragments arrive, and the download is shared between a look-ahead prefetch
 * and a user-initiated play of the same track.
 *
 * YouTube serves its `audio/mp4` streams as *fragmented* MP4 (fMP4, `ftyp`
 * brand "dash"): the `moov` sample table is empty and the real samples live in
 * `moof`/`trun` boxes, which `jaad`'s own `MP4Container` demuxer does not
 * understand. This player walks the `moof` fragments directly.
 *
 * Every failure stage reports a human-readable message through [onError]
 * instead of failing silently, so playback problems are visible in the UI.
 */
class AudioPlayer {

    // Playback & network stack speed: a shared connection pool (the CDN hosts
    // are reused across tracks and prefetches, so TLS handshakes are not paid
    // again for every song) plus a dispatcher that allows the current track and
    // the surrounding prefetches to download in parallel.
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .connectionPool(ConnectionPool(12, 5, TimeUnit.MINUTES))
        .dispatcher(Dispatcher().apply {
            maxRequests = 64
            maxRequestsPerHost = 16
        })
        .build()

    private val cacheDir =
        File(System.getProperty("user.home"), ".vivimusic/cache/audio").apply { mkdirs() }

    /** In-flight downloads keyed by cacheKey: a prefetch and a user play of the
     *  same track join the SAME download instead of racing on one `.part` path
     *  and corrupting each other's file. */
    private val activeDownloads = ConcurrentHashMap<String, DownloadHandle>()

    /** Unique partial-download suffix (see [beginDownload]). */
    private val nextPartId = java.util.concurrent.atomic.AtomicLong(0)

    private var thread: Thread? = null

    /**
     * Probe for the playback cushion (issue #3): decoded PCM still queued plus
     * source already downloaded but not yet decoded, in seconds. Set by the
     * running decode session (see [playbackCushionSeconds]).
     */
    @Volatile private var cushionProbe: (() -> Double)? = null

    /** Incremented on every (re)start; stale threads ignore their callbacks. */
    private var generation = 0

    @Volatile private var paused = false

    /** True once the current output line has been started. While it is false
     *  the line is stopped and its writes are buffered, so the device never
     *  begins consuming from an empty ring (see [LINE_PRIME_SECONDS]). */
    @Volatile private var lineStarted = false
    @Volatile private var stopped = false
    @Volatile private var volume = 1f

    /**
     * Device-side gain, when the sound card exposes one. The fallback (scaling
     * the PCM we hand to the line) can only affect audio that has NOT reached
     * the device yet, and the device ring holds up to a full second — so the
     * slider used to be heard only after that already-queued second had played
     * out. The driver's own gain is applied while the ring is consumed, which
     * makes the change audible immediately.
     */
    @Volatile private var deviceGain: javax.sound.sampled.FloatControl? = null
    @Volatile private var deviceMute: javax.sound.sampled.BooleanControl? = null
    private val lock = Object()

    private companion object {
        /** Min interval between decoded-position reports to the UI (ms).
         *  50 ms (~20 reports/s) instead of 100 ms: the seekbar used to lag the
         *  audio by up to ~50 ms because the UI only ever saw every second
         *  decoded tick. Still throttled well below one report per frame. */
        const val POSITION_REPORT_INTERVAL_MS = 50L

        /**
         * Level callbacks are decimated (every third decoded frame, ~14/s) so
         * the audio-reactive visualizer drives about a third of the UI
         * recompositions of a per-frame feed: the frame-rate UI load was
         * starving the audio scheduler on slower machines, causing micro
         * pauses/skips that coincided with small UI hitches. The visualizer
         * tween (120 ms) is far longer than the gap between updates, so it
         * still looks continuous.
         */
        const val LEVEL_DECIMATION = 3

        /** Bytes that must be on disk before the MP4 demuxer is created: the
         *  `moov` box (decoder setup) lives at the head of the file, before the
         *  first audio fragment. Audio-only moovs are only a few KB, so this is
         *  normally satisfied after the first network round-trip; the sample
         *  walker skips incomplete trailing atoms, so starting with just the
         *  first fragment (instead of waiting for a second one) is safe. */
        const val MIN_START_BYTES = 32 * 1024L

        /** Poll interval while waiting for the download to catch up (ms).
         *  15 ms halves the worst-case "wait for buffer" latency at track start
         *  and after a seek; the wait is a cheap file-length check. */
        const val DOWNLOAD_POLL_MS = 15L

        /** Interval between buffered-fraction reports to the UI (ms). Also used
         *  as the paused-state poll so the download keeps filling the cache and
         *  the secondary buffer bar keeps advancing while audio is paused. */
        const val BUFFERED_POLL_MS = 250L

        /** Max bytes of newly arrived fragments scanned in one pass: bounding
         *  the atom walk keeps a huge network burst from stalling the decode
         *  thread in a single giant scan (see [decodeAndPlay]). */
        const val SCAN_WINDOW_BYTES = 256 * 1024L

        /** How often the writer samples what the sound card actually played
         *  (see the device-health check in `flushPending`). */
        const val DEVICE_CHECK_INTERVAL_MS = 10_000L

        /** Slack allowed in that sample before it counts as a device stall: a
         *  line may legitimately be a little behind real time without anything
         *  being audible. */
        const val DEVICE_STALL_TOLERANCE_MS = 500L

        /** RMS level below which a decoded frame counts as silence (~-62 dBFS). */
        const val SILENCE_RMS_LEVEL = 0.0008f

        /** Minimum consecutive silent frames before a run is skipped (~150 ms
         *  at ~43 frames/s): short gaps, breaths and quiet attacks stay intact.
         *  With "Instantly skip silence" the wait is reduced to 2 frames. */
        const val MIN_SILENCE_RUN_FRAMES = 7

        /**
         * Seconds of already-decoded PCM the decode thread may run ahead of the
         * sound card (issue #3). This is the real jitter headroom: the writer
         * thread never blocks on anything but the queue, so a decode hiccup
         * (GC, disk scan, network wait) stays inaudible until the queue drains.
         * 8 s ≈ 1.4 MB at 44.1 kHz stereo 16-bit.
         */
        const val AUDIO_QUEUE_SECONDS = 8.0

        /**
         * Seconds of audio we ASK the output line to buffer. The device decides
         * what it grants: the granted size is what actually absorbs jitter, so
         * it is logged at every track start (issue #3 — the real cushion has to
         * be visible in the exported log, otherwise "it still skips" cannot be
         * told apart from "the device kept the buffer small").
         */
        const val LINE_BUFFER_SECONDS = 1.0

        /**
         * Seconds of audio that must already sit in the device ring BEFORE the
         * line starts consuming (issue #3). A line started while its ring is
         * still empty goes dry at once — the reporting log shows `the device
         * ran dry here` 70 ms after a track had started — and that underrun is
         * the click/skip heard at the very beginning of a track. Writes are
         * buffered while the line is stopped, so the writer fills the ring
         * first and starts the line once this much audio is queued in it (or
         * as soon as the producer has nothing more to hand over).
         */
        const val LINE_PRIME_SECONDS = 0.3

        /**
         * Seconds of audio that must already be on disk before the output line
         * is opened (issue #3). Playback used to start with only the first
         * fragment (~2 s) downloaded, so the PCM queue could never fill and the
         * whole pipeline ran pinned to the download frontier: every pause in the
         * delivery — and every "skip silence" cut, which consumes source WITHOUT
         * producing output — reached the sound card as a gap. A few seconds of
         * source first turn those into inaudible stalls.
         */
        const val PREBUFFER_SECONDS = 8.0

        /**
         * Wall-clock cap on the pre-buffer wait (ms): progressive playback must
         * still start in seconds on a slow link, so after this the track starts
         * with whatever the network managed to deliver.
         */
        const val PREBUFFER_MAX_WAIT_MS = 3_000L

        /**
         * Source seconds (downloaded, not yet decoded) a silence cut requires
         * before it is allowed (issue #3). A cut runs the decoder forward
         * without producing output, so making one while the download is close
         * behind is exactly what starves the sound card; when the cushion is
         * thin the silence is played instead (a natural pause beats a dropout)
         * and the download catches back up.
         */
        const val CUT_MIN_CUSHION_SECONDS = 3.0

        /**
         * PCM handed to the line in ONE `SourceDataLine.write` call (~120 ms).
         * One write per AAC frame (~23 ms) means ~43 wakeups per second, each of
         * which has to be scheduled in time to keep the device ring fed; bigger
         * blocks mean ~8 wakeups per second and a steadier ring fill, with no
         * extra latency (the audio is written ahead anyway).
         */
        const val WRITE_CHUNK_SECONDS = 0.12

        /**
         * Unplayed audio left in the device ring below which an audible gap is
         * possible (issue #3). This is the cushion the user actually hears: the
         * software PCM queue can hold seconds of audio, but if the ring empties
         * there is nothing left to play, so crossing this line is the definitive
         * evidence of a real (audible) underrun — and it is logged even when the
         * decode queue is full, which is what no diagnostic could see before.
         */
        const val CUSHION_WARN_MS = 50.0

        /** How long the closing track waits for the writer to play out the
         *  queued tail before the line is force-closed. */
        const val WRITER_JOIN_MS = 15_000L

        /**
         * PCM the decode thread keeps queued while playback is PAUSED (ms).
         *
         * The producer used to stop feeding the queue completely on pause, so
         * a resume began with an EMPTY queue and an empty device ring: the
         * sound card stayed silent for the first decode round-trip and the log
         * recorded it as `audio output starved: queue empty waiting for decode`
         * right after a resume (heard as a hiccup on every pause/play). A small
         * pre-roll removes the gap without decoding the whole track ahead while
         * paused: the download keeps filling the cache as before.
         */
        const val PAUSE_PREROLL_MS = 400
    }

    @Volatile private var line: SourceDataLine? = null

    /**
     * Optional parametric-EQ processor applied to the decoded 16-bit PCM just
     * before it is written to the output line. Default null = pass-through,
     * byte-identical to the previous behaviour; the UI sets it from the active
     * EQ profile (Settings → Player & audio → Equalizer).
     */
    @Volatile var equalizer: EqualizerProcessor? = null

    /**
     * "Skip silence": silent runs are dropped from the output while a track
     * plays, so it fast-forwards through them. Default false = the audio path
     * is byte-identical to before (no silence detection runs).
     */
    @Volatile var skipSilence: Boolean = false

    /**
     * "Instantly skip silence": on top of [skipSilence], leading silence at
     * the start of a track/seek is cut right away and mid-track silent runs
     * are jumped as soon as they are detected (2 frames instead of ~150 ms).
     */
    @Volatile var skipSilenceInstant: Boolean = false

    @Volatile private var onPosition: ((Long) -> Unit)? = null
    @Volatile private var onDuration: ((Long) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null
    private var onComplete: (() -> Unit)? = null

    /**
     * Single thread that delivers the UI-facing callbacks (issue #3), so no
     * application code ever runs on the decode/writer threads. See
     * [CallbackPump].
     */
    private val pump = CallbackPump(
        position = { onPosition?.invoke(it) },
        level = { onLevel?.invoke(it) },
        buffered = { onBufferedFraction?.invoke(it) },
        duration = { onDuration?.invoke(it) },
    )

    /**
     * Called with the instantaneous PCM level (0..1, RMS-normalized) once per
     * decoded frame (~43/s) while audio is being written to the output line.
     * Powers the audio-reactive "Visualizer" player background. When paused
     * or stopped, no callback fires (the UI decays the level to 0 itself).
     */
    @Volatile
    var onLevel: ((Float) -> Unit)? = null

    @Volatile private var currentStreams: List<StreamResolver.ResolvedStream>? = null
    @Volatile private var currentCacheKey: String? = null
    /** Metadata duration of the track currently loaded (see [play]). Kept
     *  across [seekTo] so a seek preserves the authoritative length. */
    @Volatile private var currentFallbackDurationMs: Long = 0L

    /**
     * Called with the buffered fraction (0..1) of the current track: how much
     * of the stream has been downloaded/decoded so far, reported while playing
     * AND while paused (the cache keeps filling). 1f means the whole track is
     * on disk (fully cached or download finished) — callers then hide the
     * secondary "buffered" segment, mirroring a fully buffered YouTube video.
     */
    @Volatile
    var onBufferedFraction: ((Float) -> Unit)? = null

    init {
        // Sweep stale partial downloads left behind by a crash: a `.part` is
        // never a valid cache file (the final `$key.m4a` is only written after
        // a complete download), so leftover parts are pure garbage.
        cacheDir.listFiles { f -> f.name.endsWith(".part") }?.forEach { it.delete() }
    }

    /**
     * Starts playing [streams] on a background thread. [cacheKey] names the
     * local cache file (use a stable id such as the videoId so repeats/seeks
     * don't re-download). [onPosition] reports decoded position, [onDuration]
     * the total track length, [onError] a human-readable failure reason, and
     * [onComplete] fires when the stream ends or is stopped.
     */
    fun play(
        streams: List<StreamResolver.ResolvedStream>,
        cacheKey: String,
        startAtMs: Long = 0L,
        startPaused: Boolean = false,
        /** Desired start as a 0..1 fraction of the track, used only when the
         *  total duration is not known yet (a track loaded from the queue whose
         *  metadata carried no length). Resolved against the stream duration
         *  inside the decode thread, where it becomes available. */
        startAtFraction: Float? = null,
        /** Track length from the caller's metadata (queue/search/browse/LT
         *  always carry it). Used as the authoritative duration when the
         *  resolved stream did not (the NewPipe fast path and the cache path
         *  return URLs without a length), so the seek range and the truncation
         *  guard stay correct instead of falling back to the ~19 s first
         *  fragment. */
        fallbackDurationMs: Long = 0L,
        onPosition: (Long) -> Unit,
        onDuration: (Long) -> Unit,
        onError: (String) -> Unit,
        onComplete: () -> Unit,
    ) {
        this.onPosition = onPosition
        this.onDuration = onDuration
        this.onError = onError
        this.onComplete = onComplete
        // Records the stalls that reach the audio path — measured at the
        // writer's own thread priority, with heap/GC/CPU context — in
        // playback.log from the first track on (issue #3).
        AudioPriorityWatchdog.ensureRunning()
        startDecode(streams, cacheKey, startAtMs, startPaused, startAtFraction, fallbackDurationMs)
    }

    /** Seeks to [ms] by restarting decode from the cached file, preserving the
     *  current pause state (seeking while paused must stay paused). */
    fun seekTo(ms: Long) {
        val streams = currentStreams ?: return
        val key = currentCacheKey ?: return
        startDecode(streams, key, ms.coerceAtLeast(0L), startPaused = paused, fallbackDurationMs = currentFallbackDurationMs)
    }

    /** Sets playback volume in the 0f..1f range. */
    fun setVolume(v: Float) {
        volume = v.coerceIn(0f, 1f)
        applyDeviceVolume()
    }

    /**
     * Pushes [volume] to the device gain when the line has one (instant, and it
     * applies to what is already buffered); without one the writer keeps
     * scaling the PCM it hands over, one block at a time.
     */
    private fun applyDeviceVolume() {
        val gain = deviceGain ?: return
        val v = volume
        runCatching {
            if (v <= 0.0005f) {
                deviceMute?.value = true
                gain.value = gain.minimum
            } else {
                deviceMute?.value = false
                val db = (20.0 * kotlin.math.log10(v.toDouble())).toFloat()
                gain.value = db.coerceIn(gain.minimum, gain.maximum)
            }
        }
    }

    fun pause() {
        paused = true
        line?.stop()
    }

    fun resume() {
        paused = false
        synchronized(lock) { lock.notifyAll() }
        // Only re-start a line that was already playing: when playback was
        // paused before the first block was handed over, the writer owns the
        // start so the ring can be primed first (issue #3).
        if (lineStarted) line?.start()
    }

    fun stop() {
        stopped = true
        paused = false
        synchronized(lock) { lock.notifyAll() }
        runCatching { line?.stop() }
        runCatching { line?.close() }
        line = null
        deviceGain = null
        deviceMute = null
    }

    private fun startDecode(
        streams: List<StreamResolver.ResolvedStream>,
        cacheKey: String,
        startAtMs: Long,
        startPaused: Boolean,
        startAtFraction: Float? = null,
        fallbackDurationMs: Long = 0L,
    ) {
        // Invalidate any running thread and reset the play flags.
        val gen = ++generation
        stopped = true
        synchronized(lock) { lock.notifyAll() }
        runCatching { line?.stop() }
        runCatching { line?.close() }
        line = null
        // The gain control belongs to the line that was just closed.
        deviceGain = null
        deviceMute = null
        paused = startPaused
        stopped = false
        currentStreams = streams
        currentCacheKey = cacheKey
        currentFallbackDurationMs = fallbackDurationMs

        // Authoritative duration, used by [decodeAndPlay] for the seek range
        // and the truncation guard. Priority: the player response's
        // `lengthSeconds` when the resolution carried one, then the caller's
        // metadata duration (queue/search/browse/LT always know the real
        // length), then 0 = derive it from the decoded sample count. It is
        // deliberately NOT reported through [onDuration] here: the UI state
        // already carries the track duration, and firing it immediately would
        // clear the "downloading" phase before any audio is actually ready.
        val knownDurationMs = streams.firstNotNullOfOrNull { it.durationMs }?.takeIf { it > 0 }
            ?: fallbackDurationMs.takeIf { it > 0 } ?: 0L

        thread = Thread {
            var failed = false
            try {
                val safe = cacheKey.replace(Regex("[^A-Za-z0-9._-]"), "_")
                val cached = File(cacheDir, "$safe.m4a")
                // A complete, valid cache file plays directly; otherwise the
                // stream is downloaded progressively and playback starts as
                // soon as the first fragment arrives.
                val handle = if (cached.exists() && cached.length() > 0 && isValidMp4(cached)) {
                    DownloadHandle(cached, cached.length(), complete = true)
                } else {
                    beginDownload(streams, cacheKey)
                }
                decodeAndPlay(handle, gen, startAtMs, knownDurationMs, startAtFraction)
            } catch (e: Exception) {
                failed = true
                if (gen == generation) {
                    onError?.invoke(e.message ?: e::class.simpleName ?: "Unknown playback error")
                }
            } finally {
                // `onComplete` means the track *finished normally*: it must NOT
                // fire after an error, otherwise a failed track (e.g. a 403) is
                // treated as "ended" and auto-advances to the next one, looping
                // through the whole queue.
                if (gen == generation && !failed) onComplete?.invoke()
            }
        }.apply {
            isDaemon = true
            name = "vivimusic-audio"
            // The decode thread refills the sound buffer on a deadline; if the
            // scheduler preempts it too long the output line underruns (the
            // audible micro-pause/skip reported on macOS). Keep it at the
            // highest priority so UI/GC work can't starve it.
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    /** True while a stream has been loaded (even if paused/ended) — i.e. a
     *  [seekTo] would restart a real decode instead of being a no-op. Before
     *  the first play of a restored track there is no loaded stream, so seeks
     *  only move the UI position and are applied when playback starts. */
    fun hasLoadedStream(): Boolean = currentStreams != null

    /** True when [cacheKey] already has a valid, non-truncated local cache file. */
    fun isCached(cacheKey: String): Boolean {
        val safe = cacheKey.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val file = File(cacheDir, "$safe.m4a")
        return file.exists() && file.length() > 0 && isValidMp4(file)
    }

    /**
     * Deletes every cache file for [cacheKey] (the final file plus any partial
     * downloads), so the next play re-downloads a clean copy. Used when a
     * truncated or corrupt download is detected (see [decodeAndPlay]).
     */
    fun evictCache(cacheKey: String) {
        val safe = cacheKey.replace(Regex("[^A-Za-z0-9._-]"), "_")
        File(cacheDir, "$safe.m4a").delete()
        cacheDir.listFiles { f -> f.name.startsWith("$safe.m4a") && f.name.endsWith(".part") }
            ?.forEach { it.delete() }
    }

    /**
     * Seconds of headroom the playback pipeline already holds (decoded PCM in
     * the queue + source on disk not yet decoded). Used to keep the look-ahead
     * prefetch from competing with the track the user is listening to.
     * [Double.MAX_VALUE] when nothing is playing, so an idle or paused player
     * never blocks caching.
     */
    fun playbackCushionSeconds(): Double =
        runCatching { cushionProbe?.invoke() ?: Double.MAX_VALUE }.getOrDefault(Double.MAX_VALUE)

    /** Downloads [streams] for [cacheKey] without playing (look-ahead prefetch).
     *  Joins an in-flight download if one already exists, so a play that starts
     *  while the prefetch is running never races it. */
    fun prefetch(streams: List<StreamResolver.ResolvedStream>, cacheKey: String) {
        if (isCached(cacheKey)) return
        beginDownload(streams, cacheKey)
    }

    /** Shared state of an in-flight (or already complete) audio download. */
    private class DownloadHandle(
        /** The file the decoder reads from (a unique `.part` while downloading). */
        val file: File,
        /** Bytes of [file] written so far; grows until [complete]. */
        @Volatile var downloadedBytes: Long,
        /** True once the download finished (successfully or not). */
        @Volatile var complete: Boolean,
        /** True when every candidate URL failed. */
        @Volatile var failed: Boolean = false,
        /** Human-readable reason when [failed] (the last candidate's error). */
        @Volatile var failure: String? = null,
    )

    /**
     * Starts (or joins) a background download of [streams] to a unique `.part`
     * file and returns immediately: the decoder consumes the file progressively
     * while it downloads, so playback begins as soon as the first fragment
     * arrives instead of after the whole track is on disk.
     */
    private fun beginDownload(streams: List<StreamResolver.ResolvedStream>, cacheKey: String): DownloadHandle {
        // The audio cache folder can disappear at runtime: the in-app "clear
        // cache" (Settings → Storage / Privacy) deletes every subfolder of
        // ~/.vivimusic/cache, including audio/, and AudioPlayer only created it
        // once at construction. Without recreating it here every download
        // started after the cleanup fails with "path not found", which showed
        // up as tracks resolving over and over and never starting (the player
        // retried, rotated the guest identity, and failed instantly each time).
        runCatching { cacheDir.mkdirs() }
        activeDownloads[cacheKey]?.let { return it }
        val safe = cacheKey.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val file = File(cacheDir, "$safe.m4a")
        // A stale/truncated final file (interrupted download or a leftover from
        // an older buggy build) would play a fragment and "end" early — delete
        // it so the fresh download writes a clean copy.
        if (file.exists()) file.delete()
        val part = File(cacheDir, "$safe.m4a.${nextPartId.incrementAndGet()}.part")
        val handle = DownloadHandle(part, 0L, complete = false)
        val existing = activeDownloads.putIfAbsent(cacheKey, handle)
        if (existing != null) return existing
        Thread {
            try {
                var lastError: IOException? = null
                for (stream in streams) {
                    try {
                        if (part.exists()) part.delete()
                        download(stream, part, handle)
                        val total = part.length()
                        if (total <= 0) throw IOException("Downloaded audio file is empty")
                        handle.downloadedBytes = total
                        // Promote to the final cache name (best-effort: while
                        // the decoder holds the `.part` open — Windows — a
                        // rename fails, so fall back to a copy; the decoder
                        // keeps reading the `.part` either way).
                        if (!part.renameTo(file)) {
                            runCatching { part.copyTo(file, overwrite = true) }
                        }
                        handle.complete = true
                        return@Thread
                    } catch (e: IOException) {
                        lastError = e
                    }
                }
                handle.failure = lastError?.message ?: "No stream URL available"
                handle.failed = true
                handle.complete = true
            } catch (e: Exception) {
                handle.failure = e.message
                handle.failed = true
                handle.complete = true
            } finally {
                activeDownloads.remove(cacheKey)
            }
        }.apply {
            isDaemon = true
            name = "vivimusic-download"
            start()
        }
        return handle
    }

    private fun download(stream: StreamResolver.ResolvedStream, part: File, handle: DownloadHandle) {
        // googlevideo ties a stream URL to the client that requested it, so the
        // download MUST use the same User-Agent (otherwise it answers 403). Some
        // endpoints want a Range header (ExoPlayer-style) while others reject it,
        // so retry without it when the first attempt is refused.
        val base = Request.Builder()
            .url(stream.url)
            .header("User-Agent", stream.userAgent)
            .header("Accept", "*/*")
            .header("Accept-Encoding", "identity")
            .header("Accept-Language", "en-US,en;q=0.9")

        var response = client.newCall(base.build().newBuilder().header("Range", "bytes=0-").build()).execute()
        if (!response.isSuccessful && response.code == 403) {
            response.close()
            response = client.newCall(base.build()).execute()
        }
        response.use { r ->
            if (!r.isSuccessful) throw IOException("HTTP ${r.code} downloading audio (${stream.url})")
            val body = r.body ?: throw IOException("Empty audio response body")
            var written = 0L
            part.outputStream().use { out ->
                body.byteStream().use { input ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        written += n
                        handle.downloadedBytes = written
                    }
                }
            }
        }
    }

    /**
     * Integrity check: a valid fragmented MP4 starts with an `ftyp` box AND
     * contains at least one `moof` (where the audio samples live). Checking only
     * `ftyp` let a truncated/interrupted cache file pass and then "end" after a
     * few seconds, auto-skipping to the next track. A parse error here means the
     * file is corrupt and must be re-downloaded.
     */
    private fun isValidMp4(file: File): Boolean = runCatching {
        if (file.length() < 16) return@runCatching false
        NIOUtils.readableChannel(file).use { channel ->
            val atoms = MP4Util.getRootAtoms(channel)
            atoms.isNotEmpty() &&
                atoms.first().header.fourcc == "ftyp" &&
                atoms.any { it.header.fourcc == "moof" }
        }
    }.getOrDefault(false)

    /**
     * Opens [line] with the biggest device buffer it will accept and returns the
     * granted size in bytes (0 when nothing worked) — see the call site in
     * [decodeAndPlay] for why this matters to issue #3.
     *
     * The candidates descend from 4× the wanted size to the legacy 8 KB: the
     * first accepted one is the largest the device offers, and the granted size
     * is read back with `getBufferSize()` because a backend is free to keep its
     * own smaller ring even after accepting the request (the exported log then
     * shows the real cushion instead of the wish).
     */
    private fun openWithLargestBuffer(line: SourceDataLine, format: AudioFormat, bytesPerSecond: Double): Int {
        val target = (bytesPerSecond * LINE_BUFFER_SECONDS).toInt().coerceAtLeast(32768)
        val candidates = intArrayOf(target * 4, target * 2, target, target / 2, target / 4, 16384, 8192)
        for (size in candidates) {
            if (size <= 0) continue
            if (runCatching { line.open(format, size) }.isSuccess) {
                return runCatching { line.bufferSize }.getOrDefault(size)
            }
        }
        return 0
    }

    /**
     * Walks the root atoms of a possibly still-growing fragmented MP4, from
     * [from] until [until], appending the AAC sample table of every complete
     * `moof` box to [samples], recording in [fragmentStarts] where each
     * fragment's samples begin. Returns the offset just past the last atom
     * walked, so scanning can resume as the download grows. Trailing atoms that
     * are not fully downloaded yet are skipped.
     */
    private fun walkAtoms(
        channel: SeekableByteChannel,
        trackId: Int,
        from: Long,
        until: Long,
        samples: MutableList<Pair<Long, Int>>,
        fragmentStarts: MutableList<Int>,
    ): Long {
        var pos = from
        val header = ByteBuffer.allocate(16)
        while (pos + 8 <= until) {
            channel.setPosition(pos)
            header.clear()
            if (!readFully(channel, header, 8)) break
            header.rewind()
            val size32 = header.int.toLong() and 0xFFFFFFFFL
            val fourcc = String(header.array(), 4, 4, Charsets.ISO_8859_1)
            var size = size32
            if (size == 1L) {
                // 64-bit extended size: 8 more bytes after the type.
                header.clear()
                if (!readFully(channel, header, 8)) break
                header.rewind()
                size = header.long
            } else if (size == 0L) {
                size = until - pos // runs to the end of the file
            }
            if (size <= 0 || size > until - pos) break // incomplete trailing atom
            if (fourcc == "moof") {
                val appendedFrom = samples.size
                collectMoofSamples(channel, trackId, pos, size, samples)
                if (samples.size > appendedFrom) fragmentStarts.add(appendedFrom)
            }
            pos += size
        }
        return pos
    }

    /**
     * Appends the AAC samples of one complete `moof` box (at [moofOffset],
     * [moofSize] bytes long) to [samples], in decode order. YouTube fMP4 sets
     * `trun.data_offset` relative to the start of the enclosing `moof`, and
     * stores the samples contiguously, so each sample's offset is
     * `moofOffset + dataOffset + sum(previous sizes)`.
     */
    private fun collectMoofSamples(
        channel: SeekableByteChannel,
        trackId: Int,
        moofOffset: Long,
        moofSize: Long,
        samples: MutableList<Pair<Long, Int>>,
    ) {
        runCatching {
            val moof = MP4Util.Atom(Header.createHeader("moof", moofSize), moofOffset).parseBox(channel)
                as? MovieFragmentBox ?: return
            for (traf in moof.tracks) {
                val tfhd = NodeBox.findFirst(traf, TrackFragmentHeaderBox::class.java, "tfhd") ?: continue
                if (tfhd.trackId != trackId) continue
                val trun = NodeBox.findFirst(traf, TrunBox::class.java, "trun") ?: continue
                val base = moofOffset + (if (trun.isDataOffsetAvailable) trun.dataOffset.toLong() else 0L)
                var offset = base
                for (size in trun.sampleSizes) {
                    samples.add(offset to size)
                    offset += size
                }
            }
        }
    }

    /** Reads exactly [len] bytes into [bb]; returns false at end of file. */
    private fun readFully(channel: SeekableByteChannel, bb: ByteBuffer, len: Int): Boolean {
        var total = 0
        while (total < len) {
            val n = channel.read(bb)
            if (n < 0) return false
            total += n
        }
        return true
    }

    private fun decodeAndPlay(
        handle: DownloadHandle,
        gen: Int,
        startAtMs: Long,
        knownDurationMs: Long = 0L,
        startAtFraction: Float? = null,
    ) {
        // The download thread creates the `.part` file asynchronously (and can
        // delete/recreate it between stream candidates), so opening the channel
        // the instant play() returns raced it and threw FileNotFoundException
        // ("cannot find the file specified") right after "stream ready" — the
        // failed attempt then retried and wiped the just-built up-next queue.
        // Wait until the file actually exists before opening the channel.
        var opened: SeekableByteChannel? = null
        while (opened == null) {
            opened = runCatching { NIOUtils.readableChannel(handle.file) }.getOrNull()
            if (opened != null) break
            if (handle.failed || handle.complete || stopped || gen != generation) break
            Thread.sleep(DOWNLOAD_POLL_MS)
        }
        val channel = opened ?: throw IOException(handle.failure ?: "Audio file is not available")
        channel.use { channel ->
            // The download may still be in flight: wait until the head of the
            // file (`ftyp` + `moov`) is on disk before parsing the container.
            while (handle.downloadedBytes < MIN_START_BYTES && !handle.complete && !handle.failed) {
                Thread.sleep(DOWNLOAD_POLL_MS)
            }
            if (handle.failed) throw IOException(handle.failure ?: "Audio download failed")
            if (stopped || gen != generation) return@use

            val demuxer = MP4Demuxer.createMP4Demuxer(channel)
            val track = demuxer.audioTracks.firstOrNull() as? AbstractMP4DemuxerTrack
                ?: throw IOException("No audio track found in stream")
            val dsi = MP4DemuxerTrackMeta.getCodecPrivate(track)
                ?: throw IOException("No AAC decoder info found in stream")
            val decoder = Decoder(NIOUtils.toArray(dsi))
            val buffer = SampleBuffer()

            val trackId = track.box.trackHeader.trackId
            // Sample table, grown incrementally: while the download is still
            // running, new `moof` fragments (and their samples) keep arriving.
            val samples = mutableListOf<Pair<Long, Int>>()
            /** Index in [samples] where each `moof` fragment's samples begin: the
             *  samples of one fragment are contiguous BY CONSTRUCTION (their
             *  offsets are the sum of the sample sizes), so only the boundaries
             *  between fragments can carry a real problem. */
            val fragmentStarts = mutableListOf<Int>()
            var scannedTo = 0L
            /** Fragments whose boundary has been checked (see
             *  [validateNewFragments]); the total is reported at the end. */
            var validatedFragments = 0
            /** Overlaps or backwards jumps seen between fragments (see
             *  [validateNewFragments]); reported as a total at the end. */
            var boundaryIssueCount = 0

            /** Scans [scannedTo..downloadedBytes) for complete `moof` boxes and
             *  appends their AAC samples; advances [scannedTo] past them. The
             *  walk is bounded to a window per call: after a big network burst
             *  one unbounded scan over hundreds of new atoms would stall the
             *  decode thread longer than the output buffer covers. */
            fun scanMore() {
                val until = handle.downloadedBytes
                if (scannedTo < until) {
                    scannedTo = walkAtoms(
                        channel, trackId, scannedTo,
                        minOf(until, scannedTo + SCAN_WINDOW_BYTES), samples, fragmentStarts,
                    )
                }
            }

            /**
             * Verifies the boundaries between the `moof` fragments just appended
             * (issue #3).
             *
             * The samples of one fragment are contiguous by construction: their
             * offsets are computed by adding up the sample sizes. A gap can
             * therefore only be *between* fragments — and there a gap is normal,
             * because YouTube's fMP4 puts the next `moof` box (the fragment's
             * metadata, ~1.8 KB of `trun` entries) between two `mdat` payloads:
             * a gap in BYTES, not in AUDIO. Comparing "previous sample end" with
             * "next sample start" across fragments therefore reported a skip on
             * every single track (`delta 1824 bytes`) while the device check
             * showed 99-100% of the wall time played — 52 tracks out of 52 in
             * the exported log, i.e. a diagnostic that cried wolf.
             *
             * What is worth checking, and what a mis-parsed container would
             * actually produce, is that the fragments ADVANCE: a fragment that
             * starts inside the previous one, or before it, means the `moof`
             * walk lost its place. Reported once per problem, plus a total.
             */
            fun validateNewFragments() {
                while (validatedFragments + 1 < fragmentStarts.size) {
                    val nextIndex = fragmentStarts[validatedFragments + 1]
                    val previousLast = samples[nextIndex - 1]
                    val previousEnd = previousLast.first + previousLast.second
                    val nextStart = samples[nextIndex].first
                    validatedFragments++
                    if (nextStart >= previousEnd) continue
                    boundaryIssueCount++
                    if (boundaryIssueCount <= 10) {
                        AppLog.log(
                            "playback",
                            "sample table overlap at frame $nextIndex: this fragment starts at " +
                                "$nextStart but the previous one ends at $previousEnd " +
                                "(delta ${nextStart - previousEnd} bytes) — the fragment walk lost its place",
                        )
                    }
                }
            }

            /** [scanMore] plus the fragment-boundary check (the sample table only
             *  ever grows, so a new fragment is where a problem can appear). */
            fun scanStep() {
                scanMore()
                validateNewFragments()
            }

            // Wait until the first audio fragment is fully downloaded.
            scanStep()
            while (samples.isEmpty() && !handle.complete && !handle.failed) {
                Thread.sleep(DOWNLOAD_POLL_MS)
                scanStep()
            }
            // The whole file is already on disk: scan it ALL before deriving the
            // duration or judging truncation. Otherwise the sample table only
            // holds the first ~256 KB window (~19 s), so a complete cached track
            // is misjudged as "truncated" (thrown away and re-downloaded on
            // every play) and a genuinely truncated cache file plays its first
            // ~19 s and "ends" — the seek bar never moving past ~19 s.
            if (handle.complete) {
                while (scannedTo < handle.downloadedBytes) {
                    scannedTo = walkAtoms(
                        channel, trackId, scannedTo,
                        minOf(handle.downloadedBytes, scannedTo + SCAN_WINDOW_BYTES), samples,
                        fragmentStarts,
                    )
                    validateNewFragments()
                }
            }
            if (samples.isEmpty()) {
                throw if (handle.failed) IOException(handle.failure ?: "Audio download failed")
                else IOException("No audio frames to decode")
            }

            /**
             * Throttled underrun diagnostics (issue #3). A gap is audible
             * exactly when the decode thread has to wait for the download while
             * the output line is nearly empty: recording the wait and the line's
             * remaining headroom turns "it lags sometimes" into something
             * checkable in the exported playback log.
             */
            var lastStallLogMs = 0L
            // Declared here (not next to its first assignment below) so the
            // stall diagnostics can report the current playback time.
            var elapsedSeconds = 0.0
            // SOURCE (compressed) bytes per second, measured from the sample
            // walk and assigned once the table exists: with it the stall
            // diagnostics turn "waited 424 ms for data" into "the download was
            // N s behind", which is what separates a slow network from a slow
            // decoder. The PCM byte rate must NOT be used here — the stream is
            // compressed, so dividing AAC bytes by the PCM rate under-reports
            // by an order of magnitude.
            var sourceBytesPerSecond: () -> Double = { 0.0 }
            // Same reason: the PCM queue and the output line are built further
            // down (they need the decoded format), so the stall diagnostics ask
            // this provider for the queued audio instead of referencing them
            // directly (-1 = not created yet).
            var queuedPcmMs: () -> Int = { -1 }
            fun logStall(waitMs: Long, missingBytes: Long) {
                if (waitMs < 40) return
                val now = System.currentTimeMillis()
                if (now - lastStallLogMs < 2_000L) return
                lastStallLogMs = now
                val freeMs = runCatching {
                    val l = line ?: return@runCatching -1.0
                    val f = l.format
                    val bytesPerSec = f.sampleRate * f.channels * (f.sampleSizeInBits / 8)
                    if (bytesPerSec <= 0f) -1.0
                    else l.available().toDouble() / bytesPerSec * 1000.0
                }.getOrDefault(-1.0)
                // With the PCM queue in front of the writer this wait is only
                // audible when the queue ALSO ran dry, so log what was still
                // queued: it separates "decoder was slow but covered" from a
                // real output gap.
                val queuedMs = queuedPcmMs()
                // The missing source in SECONDS, measured the moment the wait
                // started: "waited X ms for Y s of audio" says how far behind
                // the download was and at what speed it caught up (issue #3).
                val sourceRate = sourceBytesPerSecond()
                val missingText = if (sourceRate > 0.0) {
                    "%.1fs behind".format(java.util.Locale.US, missingBytes / sourceRate)
                } else {
                    "lag unknown"
                }
                AppLog.log(
                    "playback",
                    "audio stall: waited ${waitMs}ms for data at ~${elapsedSeconds.toInt()}s " +
                        "(line headroom ${freeMs.toInt()}ms, queued ${queuedMs}ms, " +
                        "download $missingText)",
                )
            }

            /** Blocks until the sample at [index] is fully on disk (or the
             *  download finished/failed). */
            fun awaitSample(index: Int) {
                val (offset, size) = samples[index]
                val waitStart = System.currentTimeMillis()
                // How much source the decoder was still missing when it started
                // to wait: reported as seconds by [logStall] (issue #3).
                val missingAtStart = (offset + size - handle.downloadedBytes).coerceAtLeast(0L)
                while (offset + size > handle.downloadedBytes && !handle.complete && !handle.failed) {
                    Thread.sleep(DOWNLOAD_POLL_MS)
                    scanStep()
                }
                // Only a wait that RESOLVED (the sample did arrive) is a stall:
                // the other exit path throws right below.
                if (offset + size <= handle.downloadedBytes) {
                    logStall(System.currentTimeMillis() - waitStart, missingAtStart)
                }
                if (handle.failed) throw IOException(handle.failure ?: "Audio download failed")
                if (offset + size > handle.downloadedBytes) {
                    throw IOException("Audio stream ended before the track finished downloading")
                }
            }

            fun decodeAt(index: Int) {
                val (offset, size) = samples[index]
                channel.setPosition(offset)
                val raw = ByteArray(size)
                val bb = ByteBuffer.wrap(raw)
                while (bb.hasRemaining()) {
                    if (channel.read(bb) < 0) break
                }
                decoder.decodeFrame(raw, buffer)
            }

            awaitSample(0)
            decodeAt(0)

            // Total duration: YouTube's fragmented MP4 has an empty mdhd (jcodec
            // reports totalDuration == 0), so derive it from the sample count x
            // per-frame duration (AAC-LC frames are constant-size). Without this
            // the seek slider gets a 0..1 range and can only land on the start
            // or the end.
            val firstFrameSeconds = buffer.length.coerceAtLeast(0.0)
            val metaDurationMs = runCatching { track.meta.totalDuration }
                .getOrNull()?.takeIf { it > 0 }?.let { (it * 1000).toLong() } ?: 0L
            // The AAC derivation is a fallback for streams that carried no
            // duration (NewPipe fast path, cached files, LT guest tracks): it is
            // computed from the sample table, which holds only the first
            // ~256 KB scan window when playback starts (~19 s of audio). It must
            // therefore GROW as fragments arrive instead of freezing at that
            // first-window value — otherwise every track looks ~19 s long, the
            // position clamps there and (with crossfade on) the next track
            // starts after ~19 s.
            fun currentDerivedDurationMs(): Long =
                (firstFrameSeconds * samples.size * 1000).toLong()
            fun currentDurationMs(): Long =
                if (knownDurationMs > 0) knownDurationMs
                else maxOf(currentDerivedDurationMs(), metaDurationMs)
            // Report the duration as soon as it is known, and re-report when it
            // grows (more fragments scanned), so the seek range follows the real
            // track length instead of the first fragment.
            var reportedDurationMs = 0L
            fun reportDuration() {
                val dur = currentDurationMs()
                if (dur > reportedDurationMs && gen == generation) {
                    reportedDurationMs = dur
                    pump.publishDuration(dur)
                }
            }
            reportDuration()

            // Truncated-cache guard for files already fully on disk when playback
            // started (a stale/interrupted cache holds only a fraction of the
            // track: it would play a few seconds then "end", auto-skipping to the
            // next track). Skipped while the download is still growing — a
            // partial scan at this point is expected, and the end-of-track check
            // below covers that case.
            if (handle.complete && scannedTo >= handle.downloadedBytes && knownDurationMs > 0 &&
                currentDerivedDurationMs() < knownDurationMs * 0.6
            ) {
                throw IOException(
                    "Cached audio is truncated (only ${currentDerivedDurationMs() / 1000}s of ${knownDurationMs / 1000}s); re-downloading"
                )
            }

            // The compressed source rate, from what the sample walk has measured
            // so far (scanned bytes / scanned seconds); used by the stall
            // diagnostics only, and recomputed on every read because both sides
            // grow as the download advances.
            sourceBytesPerSecond = {
                val scannedSeconds = samples.size * firstFrameSeconds
                if (scannedTo > 0L && scannedSeconds > 0.0) scannedTo / scannedSeconds else 0.0
            }

            // Source pre-buffer (issue #3). With only the starting fragment on
            // disk the PCM queue could never fill, so the whole pipeline ran
            // pinned to the download frontier and every delivery pause (or
            // silence cut) was audible. Waiting for [PREBUFFER_SECONDS] of source
            // first — bounded by [PREBUFFER_MAX_WAIT_MS] so a slow link still
            // starts in seconds, and skipped entirely for a complete file —
            // gives the queue something to absorb those hiccups with.
            if (!handle.complete && firstFrameSeconds > 0.0) {
                val prebufferDeadline = System.currentTimeMillis() + PREBUFFER_MAX_WAIT_MS
                val neededSeconds = PREBUFFER_SECONDS
                while (!stopped && gen == generation && !handle.failed &&
                    firstFrameSeconds * samples.size < neededSeconds &&
                    System.currentTimeMillis() < prebufferDeadline
                ) {
                    Thread.sleep(DOWNLOAD_POLL_MS)
                    scanMore()
                    reportDuration()
                }
                AppLog.log(
                    "playback",
                    "pre-buffered ${("%.1f".format(java.util.Locale.US, firstFrameSeconds * samples.size))}s " +
                        "of source before starting the output " +
                        "(wanted ${PREBUFFER_SECONDS.toInt()}s)",
                )
                if (handle.failed) throw IOException(handle.failure ?: "Audio download failed")
                if (stopped || gen != generation) return@use
            }

            val format = AudioFormat(
                buffer.sampleRate.toFloat(),
                buffer.bitsPerSample,
                buffer.channels,
                true,
                buffer.isBigEndian,
            )
            val out = AudioOutput.openLine(format)
                ?: throw IOException("No audio output device supports $format")
            line = out
            // The device buffer is the cushion the user actually hears: the PCM
            // queue in front of the writer covers decoder stalls (seconds), but
            // when the writer is frozen (JVM/GC pause) or simply scheduled late,
            // the only audio left to play is what the device ring already holds.
            // So: ask for more than we need, let the device decide, and LOG what
            // it granted — the real cushion has to be visible in the exported log
            // (issue #3), otherwise "it still skips" cannot be told apart from
            // "the device kept the buffer small". A too-large request is refused
            // rather than clamped by most backends, so the candidates descend:
            // the first accepted size is the biggest one available, and the
            // legacy 8-16 KB sizes (~45-90 ms, the ones that made issue #3
            // audible) stay as the very last resort.
            val bytesPerSecond = format.sampleRate.toDouble() * format.channels *
                (format.sampleSizeInBits / 8)
            val grantedBytes = openWithLargestBuffer(out, format, bytesPerSecond)
            if (grantedBytes <= 0) throw IOException("Could not open the audio output device")
            // Deliberately NOT out.start() here: the device would start pulling
            // from an empty ring and underrun in its first milliseconds, which
            // is the click heard at the beginning of a track (issue #3). Writes
            // are buffered while the line is stopped and the writer starts it
            // once the ring is primed — see flushPending().
            lineStarted = false
            // Instant volume: hand the level to the driver's own gain when the
            // backend has one (see [deviceGain]); otherwise the writer keeps
            // scaling the PCM blocks it writes.
            deviceGain = AudioOutput.masterGain(out)
            deviceMute = AudioOutput.mute(out)
            applyDeviceVolume()
            val lineBufferMs = (grantedBytes * 1000.0 / bytesPerSecond).toInt()
            val silenceMode = when {
                skipSilenceInstant -> "instant"
                skipSilence -> "on"
                else -> "off"
            }
            AppLog.log(
                "playback",
                "audio output: ${format.sampleRate.toInt()}Hz ${format.sampleSizeInBits}bit " +
                    "${format.channels}ch, device buffer ${lineBufferMs}ms (asked " +
                    "${(LINE_BUFFER_SECONDS * 1000).toInt()}ms), pcm queue " +
                    "${AUDIO_QUEUE_SECONDS.toInt()}s, writes of " +
                    "${(WRITE_CHUNK_SECONDS * 1000).toInt()}ms, volume ${(volume * 100).toInt()}% " +
                    "(${if (deviceGain != null) "device gain" else "pcm scaling"}), " +
                    "skip silence $silenceMode, equalizer ${if (equalizer != null) "on" else "off"}",
            )

            val bigEndian = buffer.isBigEndian
            val bitsPerSample = buffer.bitsPerSample
            // A fraction-based start (duration unknown when the user scrubbed)
            // is resolved against the stream duration here, where it is known.
            val effectiveStartMs = if (startAtFraction != null && knownDurationMs > 0) {
                (startAtFraction * knownDurationMs).toLong()
            } else {
                startAtMs
            }
            val targetSeconds = effectiveStartMs / 1000.0
            // Position reports are throttled so the UI (seek slider, lyrics) does
            // not recompose once per decoded frame (~43/s). Reporting ~10/s keeps
            // the slider smooth and draggable while staying accurate to ~100 ms.
            var lastReportMs = -POSITION_REPORT_INTERVAL_MS
            // Level callbacks are decimated (see [LEVEL_DECIMATION]).
            var levelTick = 0

            // Jump straight to the AAC frame that contains the requested
            // position instead of decoding (and discarding) every frame from
            // the start: seeking into a long track used to visibly re-scan the
            // whole seek bar from zero and could only land once the download
            // reached the target, so it never felt precise. AAC-LC frames are
            // independent and ~constant-size, so frame N begins at
            // N × frameDuration.
            val frameSeconds = buffer.length.coerceAtLeast(0.0)
            val skipIndex = if (frameSeconds > 0.0 && effectiveStartMs > 0L) {
                (effectiveStartMs / 1000.0 / frameSeconds).toInt()
                    .coerceIn(0, (samples.size - 1).coerceAtLeast(0))
            } else 0
            var index = skipIndex
            elapsedSeconds = index * frameSeconds
            // When jumping forward, replace the calibration frame (0) that is
            // already in the buffer with the frame at the seek target.
            if (index > 0) {
                awaitSample(index)
                decodeAt(index)
            }

            /**
             * Output decoupling (issue #3). Before, ONE thread decoded the AAC,
             * walked the sample table, waited on the network and called the
             * blocking `SourceDataLine.write` on the same deadline: any pause of
             * that thread (GC, a 256 KB atom scan, a network wait while
             * streaming, UI contention) went straight to the sound card as the
             * random micro-pause/skip macOS users reported. Now the decode
             * thread only renders PCM into this queue and a dedicated writer
             * thread owns the line, so a decoder hiccup stays inaudible as long
             * as the queue still holds audio.
             */
            val queueSlots = if (frameSeconds > 0.0) {
                (AUDIO_QUEUE_SECONDS / frameSeconds).toInt().coerceIn(16, 1024)
            } else {
                64
            }
            val pcmQueue = ArrayBlockingQueue<ByteArray>(queueSlots)
            /** Set when the producer is finished (end of track, error, stop):
             *  tells the writer that an empty queue is final, so it exits. */
            val producerDone = AtomicBoolean(false)
            // Diagnostics for [logStall]: "how much audio is still queued for
            // the writer", i.e. the jitter headroom left when the decoder had
            // to wait for the download.
            queuedPcmMs = {
                runCatching {
                    val bytes = pcmQueue.sumOf { it.size }
                    val bytesPerSec = format.sampleRate * format.channels *
                        (format.sampleSizeInBits / 8)
                    if (bytesPerSec <= 0f) -1
                    else (bytes * 1000.0 / bytesPerSec).toInt()
                }.getOrDefault(-1)
            }

            /** Hands one rendered PCM frame to the writer, blocking while the
             *  queue is full (that backpressure IS the look-ahead bound). */
            fun enqueue(chunk: ByteArray) {
                while (!stopped && gen == generation) {
                    val ok = runCatching {
                        pcmQueue.offer(chunk, 50L, TimeUnit.MILLISECONDS)
                    }.getOrDefault(false)
                    if (ok) return
                }
            }

            fun reportPosition() {
                if (gen != generation) return
                // Report the REAL playhead (frames the line has actually output)
                // offset by the seek start, not the decoded-ahead time: the
                // decode thread runs ahead of the sound by the output buffer, so
                // decoded time would make the seek slider / lyrics lead the
                // audio by the whole buffer (worse now that it is ~250 ms).
                val lineFrames = runCatching { out.getLongFramePosition() }.getOrDefault(0L)
                var posMs = effectiveStartMs
                if (format.sampleRate > 0f) {
                    posMs += (lineFrames * 1000L / format.sampleRate.toLong())
                }
                // Never report past the end of the track, so the seek slider can't
                // get stuck at the end while playing (or push a past-end position
                // to the synced device). Uses the CURRENT (growing) duration so a
                // derived length doesn't freeze the playhead at the first
                // fragment.
                val dur = currentDurationMs()
                if (dur > 0) posMs = posMs.coerceAtMost(dur)
                if (posMs - lastReportMs >= POSITION_REPORT_INTERVAL_MS) {
                    lastReportMs = posMs
                    // Published to the callback pump: the app code behind this
                    // (seek bar, lyrics, crossfade, history) must never run on
                    // the thread that feeds the sound card (issue #3).
                    pump.publishPosition(posMs)
                }
            }

            /**
             * The writer thread: it does NOTHING but hand pre-rendered PCM to
             * the sound card, and it is the only thread that may be late. It
             * also reports the playhead (the line's own frame counter, i.e. the
             * audio actually played), so position reporting keeps following the
             * real output instead of the decode look-ahead.
             */
            var lastStarveLogMs = 0L
            var lastCushionLogMs = 0L
            // One write per ~WRITE_CHUNK_SECONDS of audio instead of one per
            // decoded frame (~23 ms): ~43 wakeups per second each have to be
            // scheduled in time to keep the device fed, ~8 are far easier to
            // honour, and the ring is refilled in bigger, steadier steps.
            val writeChunkBytes = (bytesPerSecond * WRITE_CHUNK_SECONDS).toInt().coerceAtLeast(8192)
            val writeBuffer = ByteArray(writeChunkBytes + 8192)
            var pendingBytes = 0
            var handedOverBytes = 0L
            /** Wall time of the last block actually handed to the device: the
             *  cushion warning is only meaningful while this line is being fed. */
            var lastWriteWallMs = 0L
            /** Last gain actually used for a write (logged when it changes). */
            var appliedVolume = -1f
            // Device health sampling (issue #3). Every other diagnostic assumes
            // the sound card keeps consuming what it was given; a device that
            // stops (or plays back slower than real time) is audible and used to
            // leave no trace at all, which is exactly the case where the logs
            // looked perfectly clean while the user heard a jump.
            var lastDeviceCheckMs = 0L
            var lastDeviceCheckPlayedMs = 0L
            var deviceStallLogged = 0
            /** Audio the line reports as actually played, in ms. */
            fun playedAudioMs(): Long =
                if (format.sampleRate > 0f) {
                    runCatching { out.longFramePosition * 1000L / format.sampleRate.toLong() }
                        .getOrDefault(0L)
                } else {
                    0L
                }

            /** Audio already given to the device minus the audio it played: the
             *  cushion that absorbs a late writer. Computed from the line's own
             *  frame counter (verified to be the PLAYED position), so it does
             *  not depend on our bookkeeping. */
            fun cushionMs(): Double = runCatching {
                val playedBytes = out.longFramePosition * format.frameSize.toLong()
                (handedOverBytes - playedBytes) * 1000.0 / bytesPerSecond
            }.getOrDefault(-1.0)

            /** Hands the accumulated PCM to the line (blocking while the device
             *  ring is full — that backpressure is the natural pacing). */
            fun flushPending() {
                if (pendingBytes <= 0) return
                // The volume is applied HERE, on the block that is about to be
                // handed to the device — not while the frames are decoded. The
                // producer renders up to AUDIO_QUEUE_SECONDS ahead, so scaling
                // at decode time made a volume change audible only after the
                // whole queue had played out: on a cached track that is ~8 s of
                // "the slider moved but nothing happened". Here the delay is one
                // write block (~120 ms), and the value the output actually used
                // is logged.
                val gain = volume
                val data = if (deviceGain == null && gain < 0.999f && bitsPerSample == 16) {
                    scale16(writeBuffer, gain, bigEndian)
                } else {
                    writeBuffer
                }
                if (gain != appliedVolume) {
                    appliedVolume = gain
                    AppLog.log(
                        "volume",
                        "output gain now ${(gain * 100).roundToInt()}%",
                    )
                }
                var done = 0
                while (done < pendingBytes) {
                    val n = out.write(data, done, pendingBytes - done)
                    if (n <= 0) break
                    done += n
                }
                handedOverBytes += done.toLong()
                pendingBytes = 0
                lastWriteWallMs = System.currentTimeMillis()
                // Prime the device before it starts consuming (issue #3): a
                // line started with an empty ring goes dry right away — the
                // reporting log shows "the device ran dry here" 70 ms after a
                // track had started, and that underrun is the click/skip heard
                // at the beginning of a track. The ring is filled while the
                // line is stopped and the start happens once
                // [LINE_PRIME_SECONDS] of audio is queued in it, or as soon as
                // the producer has nothing more to hand over (a slow decode
                // must not delay the start for ever).
                if (!lineStarted) {
                    val primed = cushionMs()
                    if (primed >= LINE_PRIME_SECONDS * 1000.0 ||
                        pcmQueue.isEmpty() || producerDone.get()
                    ) {
                        lineStarted = true
                        runCatching { out.start() }
                        AppLog.log(
                            "playback",
                            "audio output primed: device started with ${primed.toInt()}ms " +
                                "already queued in its buffer (ring ~${lineBufferMs}ms)",
                        )
                    }
                }
                // The only situation that can be audible: the device ring is
                // about to run dry. Previously this was invisible, because the
                // PCM queue in front of the writer can be seconds long while
                // the ring empties (issue #3). Only meaningful once the device
                // is actually playing: before the start there is nothing to
                // run dry.
                val cushion = cushionMs()
                // Only while this line is actually being fed: at a track change
                // the ring of the OUTGOING line is empty by design while the PCM
                // queue already holds the NEXT track, which is how the log came
                // to report "the output can run dry here (queued for the writer:
                // 7987ms)" without a single audible gap.
                val fedRecently = System.currentTimeMillis() - lastWriteWallMs <= 500L
                if (lineStarted && fedRecently && cushion >= 0.0 && cushion <= CUSHION_WARN_MS) {
                    val now = System.currentTimeMillis()
                    if (now - lastCushionLogMs >= 3_000L) {
                        lastCushionLogMs = now
                        AppLog.log(
                            "playback",
                            "audio cushion low: only ${cushion.toInt()}ms of audio left in the device " +
                                "buffer (~${lineBufferMs}ms total) — the output can run dry here " +
                                "(queued for the writer: ${queuedPcmMs()}ms)",
                        )
                    }
                }
                // Device health check: how much audio the line actually played
                // in the last window, measured against the wall time it took.
                // Real-time playback gives ~100%; a figure far below that is the
                // device itself stalling (or dropping frames), which the
                // starvation/cushion checks cannot see because the writer keeps
                // handing over audio happily.
                if (lineStarted) {
                    val playedMs = playedAudioMs()
                    val now = System.currentTimeMillis()
                    if (lastDeviceCheckMs == 0L) {
                        lastDeviceCheckMs = now
                        lastDeviceCheckPlayedMs = playedMs
                    } else if (now - lastDeviceCheckMs >= DEVICE_CHECK_INTERVAL_MS) {
                        val windowMs = now - lastDeviceCheckMs
                        val played = playedMs - lastDeviceCheckPlayedMs
                        val ratio = if (windowMs > 0) played * 100 / windowMs else 100
                        if (played < windowMs - DEVICE_STALL_TOLERANCE_MS && deviceStallLogged < 20) {
                            deviceStallLogged++
                            AppLog.log(
                                "playback",
                                "audio device stall: the sound card played only ${played}ms of audio in " +
                                    "${windowMs}ms of wall time (${ratio}%) — the gap is BELOW our buffers " +
                                    "(cushion ${cushionMs().toInt()}ms, handed over " +
                                    "${(handedOverBytes * 1000L / bytesPerSecond.toLong())}ms so far)",
                            )
                        } else {
                            AppLog.log(
                                "playback",
                                "audio device check: played ${played}ms of ${windowMs}ms wall (${ratio}%), " +
                                    "cushion ${cushionMs().toInt()}ms, handed over " +
                                    "${(handedOverBytes * 1000L / bytesPerSecond.toLong())}ms",
                            )
                        }
                        lastDeviceCheckMs = now
                        lastDeviceCheckPlayedMs = playedMs
                    }
                }
                reportPosition()
            }

            val writer = Thread {
                try {
                    while (!stopped && gen == generation) {
                        // Honour pause without writing a byte: the line is
                        // stopped by pause() and the tile/position stay frozen.
                        synchronized(lock) {
                            while (paused && !stopped && gen == generation &&
                                !producerDone.get()
                            ) {
                                lock.wait(25L)
                            }
                        }
                        if (stopped || gen != generation) break
                        if (paused && producerDone.get()) break

                        val chunk = pcmQueue.poll(50L, TimeUnit.MILLISECONDS)
                        if (chunk == null) {
                            // Producer behind. Usually harmless (the line buffer
                            // is still full), but it is the ONLY situation that
                            // can be audible, so record it with the line's
                            // remaining headroom for the exported log.
                            if (producerDone.get()) break
                            val now = System.currentTimeMillis()
                            if (now - lastStarveLogMs >= 2_000L) {
                                lastStarveLogMs = now
                                val freeMs = runCatching {
                                    val f = out.format
                                    val bytesPerSec = f.sampleRate * f.channels *
                                        (f.sampleSizeInBits / 8)
                                    if (bytesPerSec <= 0f) -1.0
                                    else out.available().toDouble() / bytesPerSec * 1000.0
                                }.getOrDefault(-1.0)
                                // The cushion is the only number that decides
                                // whether this wait is AUDIBLE (issue #3): with
                                // nothing unplayed left the device has gone
                                // silent, and this is the one state the
                                // "cushion low" check cannot see, because it
                                // only runs when there is something to write.
                                val cushion = cushionMs()
                                val ranDry = cushion >= 0.0 && cushion <= CUSHION_WARN_MS
                                val dryNote = when {
                                    !ranDry -> ""
                                    handedOverBytes == 0L ->
                                        " — nothing had been handed to the device yet"
                                    else -> " — the device ran dry here"
                                }
                                AppLog.log(
                                    "playback",
                                    "audio output starved: queue empty waiting for decode " +
                                        "(line headroom ${freeMs.toInt()}ms, unplayed " +
                                        "${cushion.toInt()}ms)$dryNote",
                                )
                            }
                            continue
                        }
                        // Bigger blocks: accumulate until the target size (or
                        // until the incoming chunk would not fit).
                        if (pendingBytes + chunk.size > writeBuffer.size) flushPending()
                        System.arraycopy(chunk, 0, writeBuffer, pendingBytes, chunk.size)
                        pendingBytes += chunk.size
                        if (pendingBytes >= writeChunkBytes) flushPending()
                    }
                } catch (_: Throwable) {
                    // stop()/seek closes the line under a blocked write: exit.
                } finally {
                    // Normal end of track: play out what is still held locally
                    // (a few dozen ms) before draining the device ring. On a
                    // stop/seek/pause the audio is dropped instead.
                    if (producerDone.get() && !stopped && gen == generation && !paused) {
                        runCatching { flushPending() }
                    }
                    runCatching { out.drain() }
                    runCatching { out.stop() }
                    runCatching { out.close() }
                    if (line === out) line = null
                }
            }.apply {
                isDaemon = true
                name = "vivimusic-audio-out"
                // Only constraint of the whole pipeline: never be scheduled
                // late. The decode thread may stall, this one must not.
                priority = Thread.MAX_PRIORITY
                start()
            }

            // Playback cushion probe (issue #3): what the look-ahead prefetch
            // checks before it takes bandwidth (see [playbackCushionSeconds]).
            // MAX_VALUE means "no playback to protect", which keeps an idle or
            // paused player from blocking the cache pass.
            cushionProbe = {
                if (gen != generation || stopped) {
                    Double.MAX_VALUE
                } else {
                    val queuedSeconds = queuedPcmMs().coerceAtLeast(0) / 1000.0
                    // Source already scanned (therefore fully on disk) that has
                    // not been consumed yet — measured in frames so the
                    // compressed size of the file cannot skew it.
                    val frontierSeconds = samples.size * firstFrameSeconds
                    queuedSeconds + (frontierSeconds - elapsedSeconds).coerceAtLeast(0.0)
                }
            }

            // Throttled buffered-fraction reports: decoded time available (samples
            // scanned so far x per-frame duration) over the track duration. A fully
            // cached file or a finished download reports 1f, so the UI hides the
            // secondary buffer segment (nothing is "still buffering").
            var lastBufferedReportAt = -BUFFERED_POLL_MS
            fun reportBuffered() {
                if (gen != generation) return
                val dur = currentDurationMs()
                val frac = if (handle.complete) {
                    1f
                } else if (dur > 0 && frameSeconds > 0.0) {
                    val decodedMs = samples.size * frameSeconds * 1000
                    (decodedMs / dur.toDouble()).toFloat().coerceIn(0f, 1f)
                } else {
                    1f // unknown duration: nothing meaningful to show
                }
                val now = System.currentTimeMillis()
                if (now - lastBufferedReportAt >= BUFFERED_POLL_MS) {
                    lastBufferedReportAt = now
                    pump.publishBuffered(frac)
                }
            }

            // "Skip silence" state. [leading] is true until the first audible
            // frame of this decode session has actually been written, so a
            // silent intro (or the silence after a seek) is cut without waiting
            // for the run-length detection. [silentRunFrames] counts consecutive
            // silent frames; once it crosses the threshold the run is dropped.
            val silenceEnabled = skipSilence || skipSilenceInstant
            var leading = true
            var silentRunFrames = 0
            var suppressing = false
            // "Skip silence" deliberately drops audio, so it can be HEARD as a
            // short skip: it must be visible in the log (issue #3 — a user
            // hearing gaps must not be left wondering whether the app was
            // supposed to cut that bit of the song).
            var skippedFrames = 0
            var skipLogs = 0
            var cutHoldLogs = 0
            /**
             * Source seconds that are scanned (therefore fully on disk) but not
             * consumed yet: the headroom a silence cut consumes from (issue #3).
             * It is thin exactly when the download is at the frontier, which is
             * when a cut must not happen. Measured in FRAMES on purpose: the
             * first version converted the downloaded bytes with the PCM byte
             * rate, and since the stream is compressed that under-reported the
             * cushion ~10x — it even held cuts back on fully cached tracks.
             */
            fun sourceCushionSeconds(): Double =
                samples.size * firstFrameSeconds - elapsedSeconds

            fun emit() {
                // Render the current frame into the PCM queue for the writer
                // thread (skipped while paused); the playhead itself is reported
                // by the writer, from the frames the line has actually played.
                val doWrite = !paused && elapsedSeconds + buffer.length >= targetSeconds
                var suppressed = false
                if (doWrite && bitsPerSample == 16 && silenceEnabled) {
                    val silent = rms16(buffer.data, bigEndian) <= SILENCE_RMS_LEVEL
                    if (silent) {
                        // Nothing audible has been written yet: cut immediately.
                        // Otherwise count the run; "instantly" skips as soon as
                        // the run is clearly detected (2 frames), the normal
                        // mode after the longer minimum (~150 ms) so breaths
                        // and quiet attacks stay intact.
                        val minRun = if (skipSilenceInstant) 2 else MIN_SILENCE_RUN_FRAMES
                        // Count every frame of the run (also while suppressing)
                        // so the log can report the real length of what was cut.
                        silentRunFrames++
                        if (!suppressing) {
                            if (leading || silentRunFrames >= minRun) {
                                // A cut runs the decoder forward WITHOUT producing
                                // output, so it may only consume headroom that
                                // exists (issue #3): with the download close
                                // behind, skipping the silence would starve the
                                // sound card, while playing it costs nothing and
                                // lets the download catch back up.
                                val cushion = sourceCushionSeconds()
                                if (cushion >= CUT_MIN_CUSHION_SECONDS) {
                                    suppressing = true
                                } else if (cutHoldLogs < 3) {
                                    cutHoldLogs++
                                    AppLog.log(
                                        "playback",
                                        "skip silence: held back at ~${elapsedSeconds.toInt()}s — only " +
                                            "${"%.1f".format(java.util.Locale.US, cushion.coerceAtLeast(0.0))}s " +
                                            "of source buffered (cutting here would starve the output)",
                                    )
                                }
                            }
                        }
                        if (suppressing) suppressed = true
                    } else {
                        // An audible frame ends any suppression window: report the
                        // run that was just cut (few lines per track, so this can
                        // never flood the log).
                        if (suppressing) {
                            skippedFrames += silentRunFrames
                            if (skipLogs < 5) {
                                skipLogs++
                                AppLog.log(
                                    "playback",
                                    "skip silence: cut ${(silentRunFrames * buffer.length * 1000).toInt()}ms " +
                                        "of silence at ~${elapsedSeconds.toInt()}s",
                                )
                            }
                        }
                        suppressing = false
                        silentRunFrames = 0
                    }
                }
                if (doWrite && !suppressed) {
                    leading = false
                    // Gain is applied by the writer, not here: see flushPending
                    // (scaling at decode time delayed every volume change by the
                    // whole PCM queue).
                    val data = buffer.data
                    // Optional EQ: applied to the decoded PCM as a pure add-on —
                    // null default keeps the audio path identical to before.
                    // (The gain is applied later, by the writer; a scalar gain
                    // and a linear equalizer commute, so the result is the
                    // same.)
                    val outData = equalizer?.let { eq ->
                        if (bitsPerSample == 16) eq.process(data, bigEndian, buffer.sampleRate, buffer.channels)
                        else data
                    } ?: data
                    // Hand the frame to the writer instead of writing it here.
                    // jaad reuses `buffer.data` for the next decode, so the
                    // chunk is copied before being queued (the writer still owns
                    // it after the following decodeFrame call).
                    enqueue(outData.copyOf())
                    if (bitsPerSample == 16) {
                        levelTick = (levelTick + 1) % LEVEL_DECIMATION
                        // The level keeps the volume in its scale so the
                        // visualizer still follows the slider, even though the
                        // gain itself is applied later, by the writer.
                        if (levelTick == 0) pump.publishLevel(rms16(outData, bigEndian) * volume)
                    }
                }
                elapsedSeconds += buffer.length
            }
            emit()
            reportBuffered()

            /**
             * Producer loop: decode frames into the PCM queue. It runs AHEAD of
             * the writer (up to the queue capacity), so disk scans, network
             * waits and GC pauses no longer have to fit between two writes to
             * the sound card (issue #3).
             */
            fun produceFrames() {
                while (true) {
                    synchronized(lock) {
                        // Paused: hold a [PAUSE_PREROLL_MS] pre-roll in the queue
                        // (so a resume has audio to write immediately instead of
                        // starting on an empty queue — see PAUSE_PREROLL_MS) and
                        // only then idle. While idling the download keeps filling
                        // the cache: wait in short slices so newly arrived
                        // fragments are scanned and the buffered fraction is
                        // refreshed, instead of sleeping until resume.
                        while (paused && !stopped && queuedPcmMs() < PAUSE_PREROLL_MS) {
                            lock.wait(BUFFERED_POLL_MS)
                            scanStep()
                            reportBuffered()
                            reportDuration()
                        }
                    }
                    if (stopped || gen != generation) return

                    if (index + 1 < samples.size) {
                        awaitSample(index + 1)
                        index++
                        decodeAt(index)
                        emit()
                        scanStep()
                        reportBuffered()
                        reportDuration()
                    } else {
                        // No next sample yet: grow the sample table as fragments
                        // arrive, then poll until the download catches up.
                        scanStep()
                        reportBuffered()
                        reportDuration()
                        if (index + 1 < samples.size) continue
                        if (handle.failed) throw IOException(handle.failure ?: "Audio download failed")
                        if (!handle.complete) {
                            Thread.sleep(DOWNLOAD_POLL_MS)
                            continue
                        }
                        return // download complete and samples exhausted → end of track
                    }
                }
            }

            var endedNormally = false
            try {
                produceFrames()
                endedNormally = true
            } finally {
                if (skippedFrames > 0) {
                    AppLog.log(
                        "playback",
                        "skip silence: ${(skippedFrames * frameSeconds).toInt()}s of silence cut in " +
                            "this track (${skippedFrames} frames) — " +
                            "turn the option off if those jumps are unwanted",
                    )
                }
                // One line per track that states whether the SOURCE audio was
                // ever discontinuous (see [validateNewFragments]): "0" plus a
                // clean device check rules the whole pipeline out for that
                // track, which is what makes a "still lags" report actionable
                // instead of a guess (issue #3).
                AppLog.log(
                    "playback",
                    "audio integrity: ${boundaryIssueCount} sample-table overlaps, " +
                        "${deviceStallLogged} device stalls, ${samples.size} frames scanned " +
                        "in ${fragmentStarts.size} fragments",
                )
                // Never leave the writer behind: on a normal end it plays out
                // the queued tail first (so the last seconds are not cut), on a
                // stop/seek/error the queued audio is dropped and the line is
                // closed, which also unblocks a writer sitting inside write().
                producerDone.set(true)
                if (endedNormally && !stopped && gen == generation) {
                    runCatching { writer.join(WRITER_JOIN_MS) }
                }
                if (stopped || gen != generation || !endedNormally || writer.isAlive) {
                    pcmQueue.clear()
                    runCatching { out.stop() }
                    runCatching { out.close() }
                    if (line === out) line = null
                }
            }

            // End-of-track truncation guard: a download that "completed" but only
            // delivered a fraction of the known duration (e.g. a URL cut short)
            // must be treated as truncated so the caller evicts and retries with
            // a clean download instead of silently ending early.
            if (gen == generation && !stopped && handle.complete && knownDurationMs > 0) {
                val totalDerived = (firstFrameSeconds * samples.size * 1000).toLong()
                if (totalDerived < knownDurationMs * 0.6) {
                    throw IOException(
                        "Cached audio is truncated (only ${totalDerived / 1000}s of ${knownDurationMs / 1000}s); re-downloading"
                    )
                }
            }
        }
    }

    /**
     * Normalized RMS level (0..1) of 16-bit PCM samples. Computed on a
     * decimated subset (every 4th sample) so the ~43 Hz frame rate stays cheap.
     */
    private fun rms16(data: ByteArray, bigEndian: Boolean): Float {
        if (data.size < 2) return 0f
        var sum = 0.0
        var count = 0
        var i = 0
        while (i + 1 < data.size) {
            val hi: Int
            val lo: Int
            if (bigEndian) {
                hi = data[i].toInt() and 0xFF
                lo = data[i + 1].toInt() and 0xFF
            } else {
                lo = data[i].toInt() and 0xFF
                hi = data[i + 1].toInt() and 0xFF
            }
            var s = (hi shl 8) or lo
            if (s >= 0x8000) s -= 0x10000 // sign-extend to signed 16-bit
            val f = s / 32768f
            sum += (f * f).toDouble()
            count++
            i += 8 // every 4th sample (2 bytes each)
        }
        if (count == 0) return 0f
        return kotlin.math.sqrt((sum / count).toFloat()).coerceIn(0f, 1f)
    }

    /**
     * Delivers the playback callbacks (position/level/buffered/duration) to the
     * app from ONE dedicated thread instead of the audio threads themselves
     * (issue #3).
     *
     * The seek bar, the synced lyrics, the crossfade scheduling and the listen
     * history all hang off these callbacks, i.e. off application code that can
     * log to disk, hit the database or wake up the whole Compose tree. Running
     * that on the thread that hands PCM to the sound card is a stall waiting to
     * happen. Here the audio threads only publish the latest value (a couple of
     * field writes) and this thread — which nothing waits on — invokes the
     * callbacks at ~50 Hz, dropping intermediate values instead of queueing
     * them (the UI only ever wants the newest position).
     */
    private class CallbackPump(
        private val position: (Long) -> Unit,
        private val level: (Float) -> Unit,
        private val buffered: (Float) -> Unit,
        private val duration: (Long) -> Unit,
    ) : Thread("vivimusic-ui-pump") {

        private val lock = Any()
        private var pendingPosition: Long? = null
        private var pendingLevel: Float? = null
        private var pendingBuffered: Float? = null
        private var pendingDuration: Long? = null

        init {
            isDaemon = true
            priority = NORM_PRIORITY
            start()
        }

        fun publishPosition(ms: Long) = synchronized(lock) { pendingPosition = ms }
        fun publishLevel(value: Float) = synchronized(lock) { pendingLevel = value }
        fun publishBuffered(fraction: Float) = synchronized(lock) { pendingBuffered = fraction }
        fun publishDuration(ms: Long) = synchronized(lock) { pendingDuration = ms }

        override fun run() {
            while (true) {
                var pos: Long? = null
                var lvl: Float? = null
                var buf: Float? = null
                var dur: Long? = null
                synchronized(lock) {
                    pos = pendingPosition; pendingPosition = null
                    lvl = pendingLevel; pendingLevel = null
                    buf = pendingBuffered; pendingBuffered = null
                    dur = pendingDuration; pendingDuration = null
                }
                // Duration first: the seek range must exist before a position
                // can be interpreted against it.
                dur?.let { runCatching { duration(it) } }
                buf?.let { runCatching { buffered(it) } }
                pos?.let { runCatching { position(it) } }
                lvl?.let { runCatching { level(it) } }
                try {
                    Thread.sleep(20L)
                } catch (_: InterruptedException) {
                    return
                }
            }
        }
    }

    /**
     * Logs the stalls that can actually reach the sound card (issue #3).
     *
     * This probe sleeps 50 ms at the SAME thread priority as the writer thread
     * that feeds the output line, so a late return cannot be blamed on thread
     * starvation of a low-priority helper: the audio path was held up the same
     * way. The heap, GC and process-CPU figures next to it tell whether it was
     * the JVM (a stop-the-world pause shows GC counters moving) or the machine
     * (everything small, CPU pegged).
     *
     * The first version ran this probe at MIN_PRIORITY, which measured the
     * opposite thing: on a busy machine the process starves a lowest-priority
     * thread by hundreds of milliseconds whatever the audio is doing, so it
     * reported "frozen" for delays the writer never saw.
     */
    private object AudioPriorityWatchdog {

        private val started = AtomicBoolean(false)

        fun ensureRunning() {
            if (!started.compareAndSet(false, true)) return
            Thread {
                val gcBeans = runCatching {
                    java.lang.management.ManagementFactory.getGarbageCollectorMXBeans()
                }.getOrDefault(emptyList())
                val memory = runCatching {
                    java.lang.management.ManagementFactory.getMemoryMXBean()
                }.getOrNull()
                var lastLogMs = 0L
                while (true) {
                    val start = System.currentTimeMillis()
                    try {
                        Thread.sleep(50L)
                    } catch (_: InterruptedException) {
                        return@Thread
                    }
                    val late = System.currentTimeMillis() - start - 50L
                    if (late < 120L) continue
                    val now = System.currentTimeMillis()
                    if (now - lastLogMs < 1_000L) continue
                    lastLogMs = now
                    val heap = memory?.heapMemoryUsage
                    val used = (heap?.used ?: 0L) / 1024 / 1024
                    val max = (heap?.max ?: 0L) / 1024 / 1024
                    val gc = gcBeans.joinToString(" ") { b -> "${b.name}:${b.collectionCount}/${b.collectionTime}ms" }
                    // Process/system CPU disambiguates the two possible causes
                    // without another measurement round: a frozen JVM shows GC
                    // counters moving, a saturated machine shows high CPU here.
                    // Not every platform reports these (NaN when unavailable).
                    val os = runCatching {
                        java.lang.management.ManagementFactory.getOperatingSystemMXBean()
                    }.getOrNull() as? com.sun.management.OperatingSystemMXBean
                    val cpu = os?.let { bean ->
                        val proc = runCatching { bean.processCpuLoad }.getOrDefault(-1.0)
                        val all = runCatching { bean.cpuLoad }.getOrDefault(-1.0)
                        when {
                            proc < 0.0 && all < 0.0 -> ""
                            proc >= 0.0 && all >= 0.0 ->
                                ", cpu ${(proc * 100).roundToInt()}% of ${(all * 100).roundToInt()}% busy"
                            proc >= 0.0 -> ", cpu ${(proc * 100).roundToInt()}% of the process"
                            else -> ", system cpu ${(all * 100).roundToInt()}%"
                        }
                    } ?: ""
                    runCatching {
                        AppLog.log(
                            "playback",
                            "audio priority stall: 50ms sleep returned ${late + 50}ms late " +
                                "(= ${late}ms held up at the writer's priority; heap " +
                                "${used}/${max}MB, gc $gc$cpu)",
                        )
                    }
                }
            }.apply {
                isDaemon = true
                name = "vivimusic-audio-watchdog"
                // Same priority as the audio writer: what this measures must be
                // representative of the thread that feeds the sound card.
                priority = Thread.MAX_PRIORITY
                start()
            }
        }
    }

    /** Scales 16-bit PCM samples by [gain] (0..1), honoring [bigEndian] order. */
    private fun scale16(data: ByteArray, gain: Float, bigEndian: Boolean): ByteArray {
        if (gain >= 0.999f) return data
        val n = data.size / 2
        val out = ByteArray(data.size)
        for (i in 0 until n) {
            val hi: Int
            val lo: Int
            if (bigEndian) {
                hi = data[2 * i].toInt() and 0xFF
                lo = data[2 * i + 1].toInt() and 0xFF
            } else {
                lo = data[2 * i].toInt() and 0xFF
                hi = data[2 * i + 1].toInt() and 0xFF
            }
            var s = (hi shl 8) or lo
            if (s >= 0x8000) s -= 0x10000 // sign-extend to signed 16-bit
            s = (s * gain).toInt().coerceIn(-32768, 32767)
            val u = s and 0xFFFF
            if (bigEndian) {
                out[2 * i] = (u shr 8).toByte()
                out[2 * i + 1] = u.toByte()
            } else {
                out[2 * i] = u.toByte()
                out[2 * i + 1] = (u shr 8).toByte()
            }
        }
        return out
    }
}
