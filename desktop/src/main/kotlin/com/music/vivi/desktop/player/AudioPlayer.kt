package com.music.vivi.desktop.player

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
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

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

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
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

    /** Incremented on every (re)start; stale threads ignore their callbacks. */
    private var generation = 0

    @Volatile private var paused = false
    @Volatile private var stopped = false
    @Volatile private var volume = 1f
    private val lock = Object()

    private companion object {
        /** Min interval between decoded-position reports to the UI (ms). */
        const val POSITION_REPORT_INTERVAL_MS = 100L

        /** Bytes that must be on disk before the MP4 demuxer is created: the
         *  `moov` box (decoder setup) lives at the head of the file, before the
         *  first audio fragment. Audio-only moovs are only a few KB, so this is
         *  normally satisfied after the first network round-trip; the sample
         *  walker skips incomplete trailing atoms, so starting with just the
         *  first fragment (instead of waiting for a second one) is safe. */
        const val MIN_START_BYTES = 32 * 1024L

        /** Poll interval while waiting for the download to catch up (ms). */
        const val DOWNLOAD_POLL_MS = 30L
    }

    @Volatile private var line: SourceDataLine? = null

    /**
     * Optional parametric-EQ processor applied to the decoded 16-bit PCM just
     * before it is written to the output line. Default null = pass-through,
     * byte-identical to the previous behaviour; the UI sets it from the active
     * EQ profile (Settings → Player & audio → Equalizer).
     */
    @Volatile var equalizer: EqualizerProcessor? = null

    private var onPosition: ((Long) -> Unit)? = null
    private var onDuration: ((Long) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null
    private var onComplete: (() -> Unit)? = null

    /**
     * Called with the instantaneous PCM level (0..1, RMS-normalized) once per
     * decoded frame (~43/s) while audio is being written to the output line.
     * Powers the audio-reactive "Visualizer" player background. When paused
     * or stopped, no callback fires (the UI decays the level to 0 itself).
     */
    @Volatile
    var onLevel: ((Float) -> Unit)? = null

    private var currentStreams: List<StreamResolver.ResolvedStream>? = null
    private var currentCacheKey: String? = null

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
        onPosition: (Long) -> Unit,
        onDuration: (Long) -> Unit,
        onError: (String) -> Unit,
        onComplete: () -> Unit,
    ) {
        this.onPosition = onPosition
        this.onDuration = onDuration
        this.onError = onError
        this.onComplete = onComplete
        startDecode(streams, cacheKey, startAtMs, startPaused)
    }

    /** Seeks to [ms] by restarting decode from the cached file, preserving the
     *  current pause state (seeking while paused must stay paused). */
    fun seekTo(ms: Long) {
        val streams = currentStreams ?: return
        val key = currentCacheKey ?: return
        startDecode(streams, key, ms.coerceAtLeast(0L), startPaused = paused)
    }

    /** Sets playback volume in the 0f..1f range. */
    fun setVolume(v: Float) {
        volume = v.coerceIn(0f, 1f)
    }

    fun pause() {
        paused = true
        line?.stop()
    }

    fun resume() {
        paused = false
        synchronized(lock) { lock.notifyAll() }
        line?.start()
    }

    fun stop() {
        stopped = true
        paused = false
        synchronized(lock) { lock.notifyAll() }
        runCatching { line?.stop() }
        runCatching { line?.close() }
        line = null
    }

    private fun startDecode(streams: List<StreamResolver.ResolvedStream>, cacheKey: String, startAtMs: Long, startPaused: Boolean) {
        // Invalidate any running thread and reset the play flags.
        val gen = ++generation
        stopped = true
        synchronized(lock) { lock.notifyAll() }
        runCatching { line?.stop() }
        runCatching { line?.close() }
        line = null
        paused = startPaused
        stopped = false
        currentStreams = streams
        currentCacheKey = cacheKey

        // Authoritative duration from the player response, used by
        // [decodeAndPlay] for the seek range and the truncation guard. It is
        // deliberately NOT reported through [onDuration] here: the UI state
        // already carries the track duration, and firing it immediately would
        // clear the "downloading" phase before any audio is actually ready.
        val knownDurationMs = streams.firstNotNullOfOrNull { it.durationMs }?.takeIf { it > 0 } ?: 0L

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
                decodeAndPlay(handle, gen, startAtMs, knownDurationMs)
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
     * Walks the root atoms of a possibly still-growing fragmented MP4, from
     * [from] until [until], appending the AAC sample table of every complete
     * `moof` box to [samples]. Returns the offset just past the last atom
     * walked, so scanning can resume as the download grows. Trailing atoms that
     * are not fully downloaded yet are skipped.
     */
    private fun walkAtoms(
        channel: SeekableByteChannel,
        trackId: Int,
        from: Long,
        until: Long,
        samples: MutableList<Pair<Long, Int>>,
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
                collectMoofSamples(channel, trackId, pos, size, samples)
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

    private fun decodeAndPlay(handle: DownloadHandle, gen: Int, startAtMs: Long, knownDurationMs: Long = 0L) {
        NIOUtils.readableChannel(handle.file).use { channel ->
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
            var scannedTo = 0L

            /** Scans [scannedTo..downloadedBytes) for complete `moof` boxes and
             *  appends their AAC samples; advances [scannedTo] past them. */
            fun scanMore() {
                val until = handle.downloadedBytes
                if (scannedTo < until) {
                    scannedTo = walkAtoms(channel, trackId, scannedTo, until, samples)
                }
            }

            // Wait until the first audio fragment is fully downloaded.
            scanMore()
            while (samples.isEmpty() && !handle.complete && !handle.failed) {
                Thread.sleep(DOWNLOAD_POLL_MS)
                scanMore()
            }
            if (samples.isEmpty()) {
                throw if (handle.failed) IOException(handle.failure ?: "Audio download failed")
                else IOException("No audio frames to decode")
            }

            /** Blocks until the sample at [index] is fully on disk (or the
             *  download finished/failed). */
            fun awaitSample(index: Int) {
                val (offset, size) = samples[index]
                while (offset + size > handle.downloadedBytes && !handle.complete && !handle.failed) {
                    Thread.sleep(DOWNLOAD_POLL_MS)
                    scanMore()
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
            val derivedDurationMs = (firstFrameSeconds * samples.size * 1000).toLong()
            // Prefer the player-response duration (accurate); the AAC derivation
            // is only a fallback for streams that didn't carry a lengthSeconds.
            val durationMs = if (knownDurationMs > 0) knownDurationMs
                else maxOf(derivedDurationMs, metaDurationMs)
            if (gen == generation) onDuration?.invoke(durationMs)

            // Truncated-cache guard for files already fully on disk when playback
            // started (a stale/interrupted cache holds only a fraction of the
            // track: it would play a few seconds then "end", auto-skipping to the
            // next track). Skipped while the download is still growing — a
            // partial scan at this point is expected, and the end-of-track check
            // below covers that case.
            if (handle.complete && scannedTo >= handle.downloadedBytes && knownDurationMs > 0 &&
                derivedDurationMs < knownDurationMs * 0.6
            ) {
                throw IOException(
                    "Cached audio is truncated (only ${derivedDurationMs / 1000}s of ${knownDurationMs / 1000}s); re-downloading"
                )
            }

            val format = AudioFormat(
                buffer.sampleRate.toFloat(),
                buffer.bitsPerSample,
                buffer.channels,
                true,
                buffer.isBigEndian,
            )
            val out = AudioSystem.getSourceDataLine(format)
                ?: throw IOException("No audio output device supports $format")
            line = out
            // A larger output buffer smooths over scheduler/GC hiccups — the
            // old 8 KB buffer underran easily on macOS (audible glitches),
            // especially while the animated canvas competes for CPU. Fall back
            // to the small buffer only if the line rejects the bigger one.
            runCatching { out.open(format, 16384) }.getOrElse {
                out.open(format, 8192)
            }
            out.start()

            val bigEndian = buffer.isBigEndian
            val bitsPerSample = buffer.bitsPerSample
            val targetSeconds = startAtMs / 1000.0
            // Position reports are throttled so the UI (seek slider, lyrics) does
            // not recompose once per decoded frame (~43/s). Reporting ~10/s keeps
            // the slider smooth and draggable while staying accurate to ~100 ms.
            var lastReportMs = -POSITION_REPORT_INTERVAL_MS
            // Level callbacks are decimated (every other decoded frame, ~20/s)
            // so the audio-reactive visualizer drives about half the UI
            // recompositions of before: the frame-rate UI load was starving the
            // audio scheduler on macOS, causing micro pauses/skips that
            // coincided with small UI hitches.
            var levelTick = false

            // Jump straight to the AAC frame that contains the requested
            // position instead of decoding (and discarding) every frame from
            // the start: seeking into a long track used to visibly re-scan the
            // whole seek bar from zero and could only land once the download
            // reached the target, so it never felt precise. AAC-LC frames are
            // independent and ~constant-size, so frame N begins at
            // N × frameDuration.
            val frameSeconds = buffer.length.coerceAtLeast(0.0)
            val skipIndex = if (frameSeconds > 0.0 && startAtMs > 0L) {
                (startAtMs / 1000.0 / frameSeconds).toInt()
                    .coerceIn(0, (samples.size - 1).coerceAtLeast(0))
            } else 0
            var index = skipIndex
            var elapsedSeconds = index * frameSeconds
            // When jumping forward, replace the calibration frame (0) that is
            // already in the buffer with the frame at the seek target.
            if (index > 0) {
                awaitSample(index)
                decodeAt(index)
            }

            fun reportPosition() {
                if (gen != generation) return
                var posMs = ((elapsedSeconds + buffer.length) * 1000).toLong()
                // Never report past the end of the track, so the seek slider can't
                // get stuck at the end while playing (or push a past-end position
                // to the synced device).
                if (durationMs > 0) posMs = posMs.coerceAtMost(durationMs)
                if (posMs - lastReportMs >= POSITION_REPORT_INTERVAL_MS) {
                    lastReportMs = posMs
                    onPosition?.invoke(posMs)
                }
            }

            fun emit() {
                // Write the current frame to the output line (skipped while
                // paused) and report the decoded position.
                if (elapsedSeconds + buffer.length >= targetSeconds) {
                    if (!paused) {
                        val data = if (volume < 0.999f && bitsPerSample == 16) {
                            scale16(buffer.data, volume, bigEndian)
                        } else {
                            buffer.data
                        }
                        // Optional EQ: applied to the final PCM buffer (after the
                        // volume scale) so it stays a pure add-on — null default
                        // keeps the audio path identical to before.
                        val outData = equalizer?.let { eq ->
                            if (bitsPerSample == 16) eq.process(data, bigEndian, buffer.sampleRate, buffer.channels)
                            else data
                        } ?: data
                        var written = 0
                        while (written < outData.size) {
                            val n = out.write(outData, written, outData.size - written)
                            if (n <= 0) break
                            written += n
                        }
                        if (bitsPerSample == 16) {
                            levelTick = !levelTick
                            if (levelTick) onLevel?.invoke(rms16(outData, bigEndian))
                        }
                    }
                }
                reportPosition()
                elapsedSeconds += buffer.length
            }
            emit()

            while (true) {
                synchronized(lock) {
                    while (paused && !stopped) lock.wait()
                }
                if (stopped || gen != generation) break

                // Grow the sample table as new fragments arrive.
                scanMore()
                if (index + 1 >= samples.size) {
                    if (handle.failed) throw IOException(handle.failure ?: "Audio download failed")
                    if (!handle.complete) {
                        Thread.sleep(DOWNLOAD_POLL_MS)
                        continue
                    }
                    break // download complete and samples exhausted → end of track
                }
                awaitSample(index + 1)
                index++
                decodeAt(index)
                emit()
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

            out.drain()
            out.stop()
            out.close()
            if (line === out) line = null
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
