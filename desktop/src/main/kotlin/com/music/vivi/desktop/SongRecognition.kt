package com.music.vivi.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.music.shazamkit.Shazam
import com.music.shazamkit.models.RecognitionResult
import kotlinx.serialization.Serializable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64
import java.util.zip.CRC32
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sin

// ---------------------------------------------------------------------------
// Pure-JVM Shazam fingerprint (ported from the mobile ShazamSignatureGenerator,
// replacing android.util.Base64 with java.util.Base64).
// ---------------------------------------------------------------------------

object ShazamFingerprint {
    private const val SAMPLE_RATE = 16_000
    private const val FFT_SIZE = 2048
    private const val FFT_OUTPUT_SIZE = FFT_SIZE / 2 + 1
    private const val MAX_PEAKS = 255
    private const val MAX_TIME_SECONDS = 12.0
    private const val RING_BUF_SIZE = 256

    private const val BAND_250_520 = 0
    private const val BAND_520_1450 = 1
    private const val BAND_1450_3500 = 2
    private const val BAND_3500_5500 = 3

    private val HANNING = DoubleArray(FFT_SIZE) { i ->
        0.5 * (1.0 - cos(2.0 * PI * (i + 1).toDouble() / 2049.0))
    }

    fun fromI16(samples: ByteArray): String {
        val pcm = ShortArray(samples.size / 2)
        ByteBuffer.wrap(samples).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(pcm)
        return State().process(pcm)
    }

    private class State {
        private val samplesRing = IntArray(FFT_SIZE)
        private var samplesPos = 0
        private val fftOutputs = Array(RING_BUF_SIZE) { DoubleArray(FFT_OUTPUT_SIZE) }
        private var fftPos = 0
        private var fftNumWritten = 0
        private val spreadFfts = Array(RING_BUF_SIZE) { DoubleArray(FFT_OUTPUT_SIZE) }
        private var spreadPos = 0
        private var spreadNumWritten = 0
        private var numSamples = 0
        private val bandPeaks = Array(4) { mutableListOf<Peak>() }
        private var totalPeaks = 0

        fun process(pcm: ShortArray): String {
            var offset = 0
            while (offset + 128 <= pcm.size) {
                val elapsedSec = numSamples.toDouble() / SAMPLE_RATE
                if (elapsedSec >= MAX_TIME_SECONDS && totalPeaks >= MAX_PEAKS) break
                numSamples += 128
                for (k in offset until offset + 128) {
                    samplesRing[samplesPos] = pcm[k].toInt()
                    samplesPos = (samplesPos + 1) % FFT_SIZE
                }
                doFFT()
                doPeakSpreading()
                if (spreadNumWritten >= 47) doPeakRecognition()
                offset += 128
            }
            return encodeSignature()
        }

        private fun doFFT() {
            val windowed = DoubleArray(FFT_SIZE) { i ->
                samplesRing[(samplesPos + i) % FFT_SIZE].toDouble() * HANNING[i]
            }
            val result = computeRfft(windowed)
            result.copyInto(fftOutputs[fftPos])
            fftPos = (fftPos + 1) % RING_BUF_SIZE
            fftNumWritten++
        }

        private fun doPeakSpreading() {
            val lastFftIdx = (fftPos - 1 + RING_BUF_SIZE) % RING_BUF_SIZE
            val spread = fftOutputs[lastFftIdx].copyOf()
            for (pos in 0 until FFT_OUTPUT_SIZE - 2) {
                spread[pos] = maxOf(spread[pos], spread[pos + 1], spread[pos + 2])
            }
            for (pos in 0 until FFT_OUTPUT_SIZE) {
                var maxVal = spread[pos]
                for (off in intArrayOf(-1, -3, -6)) {
                    val idx = ((spreadPos + off) % RING_BUF_SIZE + RING_BUF_SIZE) % RING_BUF_SIZE
                    val oldVal = spreadFfts[idx][pos]
                    if (oldVal > maxVal) maxVal = oldVal
                    spreadFfts[idx][pos] = maxVal
                }
            }
            spread.copyInto(spreadFfts[spreadPos])
            spreadPos = (spreadPos + 1) % RING_BUF_SIZE
            spreadNumWritten++
        }

        private fun doPeakRecognition() {
            val fftMinus46 = fftOutputs[(fftPos - 46 + RING_BUF_SIZE * 2) % RING_BUF_SIZE]
            val spreadMinus49 = spreadFfts[(spreadPos - 49 + RING_BUF_SIZE * 2) % RING_BUF_SIZE]
            val otherOffsets = intArrayOf(-53, -45, 165, 172, 179, 186, 193, 200, 214, 221, 228, 235, 242, 249)

            for (binPos in 10 until FFT_OUTPUT_SIZE - 8) {
                val fftVal = fftMinus46[binPos]
                if (fftVal < 1.0 / 64.0 || fftVal < spreadMinus49[binPos]) continue
                var maxNeighborSpread49 = 0.0
                for (off in intArrayOf(-10, -7, -4, -3, 1, 2, 5, 8)) {
                    val v = spreadMinus49[binPos + off]
                    if (v > maxNeighborSpread49) maxNeighborSpread49 = v
                }
                if (fftVal <= maxNeighborSpread49) continue
                var maxNeighborOther = maxNeighborSpread49
                for (off in otherOffsets) {
                    val spreadIdx = ((spreadPos + off) % RING_BUF_SIZE + RING_BUF_SIZE) % RING_BUF_SIZE
                    val v = spreadFfts[spreadIdx][binPos - 1]
                    if (v > maxNeighborOther) maxNeighborOther = v
                }
                if (fftVal <= maxNeighborOther) continue

                val fftNumber = spreadNumWritten - 46
                val peakMag = ln(max(1.0 / 64.0, fftVal)) * 1477.3 + 6144
                val peakMagBefore = ln(max(1.0 / 64.0, fftMinus46[binPos - 1])) * 1477.3 + 6144
                val peakMagAfter = ln(max(1.0 / 64.0, fftMinus46[binPos + 1])) * 1477.3 + 6144
                val peakVariation1 = peakMag * 2 - peakMagBefore - peakMagAfter
                val peakVariation2 = (peakMagAfter - peakMagBefore) * 32 / peakVariation1
                val correctedBin = binPos * 64.0 + peakVariation2
                val frequencyHz = correctedBin * (16000.0 / 2.0 / 1024.0 / 64.0)
                val band = when {
                    frequencyHz < 250.0 -> continue
                    frequencyHz < 520.0 -> BAND_250_520
                    frequencyHz < 1450.0 -> BAND_520_1450
                    frequencyHz < 3500.0 -> BAND_1450_3500
                    frequencyHz <= 5500.0 -> BAND_3500_5500
                    else -> continue
                }
                bandPeaks[band].add(Peak(fftNumber, peakMag.toInt(), correctedBin.toInt()))
                totalPeaks++
            }
        }

        private fun encodeSignature(): String {
            val contentsStream = ByteArrayOutputStream()
            for (bandId in 0..3) {
                val peaks = bandPeaks[bandId]
                if (peaks.isEmpty()) continue
                val peakBuf = ByteArrayOutputStream()
                var prevFftPassNumber = 0
                for (peak in peaks) {
                    val diff = peak.fftPassNumber - prevFftPassNumber
                    if (diff >= 255) {
                        peakBuf.write(0xFF)
                        writeLE32(peakBuf, peak.fftPassNumber)
                        prevFftPassNumber = peak.fftPassNumber
                    }
                    peakBuf.write(peak.fftPassNumber - prevFftPassNumber)
                    writeLE16(peakBuf, peak.peakMagnitude)
                    writeLE16(peakBuf, peak.correctedPeakFrequencyBin)
                    prevFftPassNumber = peak.fftPassNumber
                }
                val peakBytes = peakBuf.toByteArray()
                writeLE32(contentsStream, 0x60030040 + bandId)
                writeLE32(contentsStream, peakBytes.size)
                contentsStream.write(peakBytes)
                val padBytes = (4 - peakBytes.size % 4) % 4
                repeat(padBytes) { contentsStream.write(0) }
            }
            val contents = contentsStream.toByteArray()
            val sizeMinusHeader = contents.size + 8
            val samplesAndOffset = (numSamples + SAMPLE_RATE * 0.24).toInt()
            val headerBytes = ByteBuffer.allocate(48).order(ByteOrder.LITTLE_ENDIAN).apply {
                putInt(0xcafe2580.toInt())
                putInt(0)
                putInt(sizeMinusHeader)
                putInt(0x94119c00.toInt())
                putInt(0); putInt(0); putInt(0)
                putInt(3 shl 27)
                putInt(0); putInt(0)
                putInt(samplesAndOffset)
                putInt((15 shl 19) + 0x40000)
            }.array()
            val fullBuf = ByteArrayOutputStream(56 + contents.size)
            fullBuf.write(headerBytes)
            writeLE32(fullBuf, 0x40000000)
            writeLE32(fullBuf, contents.size + 8)
            fullBuf.write(contents)
            val fullBytes = fullBuf.toByteArray()
            val crc = CRC32()
            crc.update(fullBytes, 8, fullBytes.size - 8)
            val crcValue = crc.value.toInt()
            fullBytes[4] = (crcValue and 0xFF).toByte()
            fullBytes[5] = ((crcValue shr 8) and 0xFF).toByte()
            fullBytes[6] = ((crcValue shr 16) and 0xFF).toByte()
            fullBytes[7] = ((crcValue shr 24) and 0xFF).toByte()
            return "data:audio/vnd.shazam.sig;base64," + Base64.getEncoder().encodeToString(fullBytes)
        }

        private fun writeLE32(out: ByteArrayOutputStream, value: Int) {
            out.write(value and 0xFF)
            out.write((value ushr 8) and 0xFF)
            out.write((value ushr 16) and 0xFF)
            out.write((value ushr 24) and 0xFF)
        }

        private fun writeLE16(out: ByteArrayOutputStream, value: Int) {
            out.write(value and 0xFF)
            out.write((value ushr 8) and 0xFF)
        }
    }

    private data class Peak(val fftPassNumber: Int, val peakMagnitude: Int, val correctedPeakFrequencyBin: Int)

    private fun computeRfft(windowed: DoubleArray): DoubleArray {
        val n = windowed.size
        val re = windowed.copyOf()
        val im = DoubleArray(n)
        var j = 0
        for (i in 1 until n) {
            var bit = n ushr 1
            while (j and bit != 0) { j = j xor bit; bit = bit ushr 1 }
            j = j xor bit
            if (i < j) {
                var tmp = re[i]; re[i] = re[j]; re[j] = tmp
                tmp = im[i]; im[i] = im[j]; im[j] = tmp
            }
        }
        var len = 2
        while (len <= n) {
            val halfLen = len ushr 1
            val ang = -PI / halfLen
            val wBaseRe = cos(ang)
            val wBaseIm = kotlin.math.sin(ang)
            var i = 0
            while (i < n) {
                var wRe = 1.0
                var wIm = 0.0
                for (k in 0 until halfLen) {
                    val u = i + k
                    val v = u + halfLen
                    val evenRe = re[u]; val evenIm = im[u]
                    val oddRe = re[v] * wRe - im[v] * wIm
                    val oddIm = re[v] * wIm + im[v] * wRe
                    re[u] = evenRe + oddRe; im[u] = evenIm + oddIm
                    re[v] = evenRe - oddRe; im[v] = evenIm - oddIm
                    val newWRe = wRe * wBaseRe - wIm * wBaseIm
                    wIm = wRe * wBaseIm + wIm * wBaseRe
                    wRe = newWRe
                }
                i += len
            }
            len = len shl 1
        }
        val scaleFactor = 1.0 / (1 shl 17)
        val minVal = 1e-10
        return DoubleArray(FFT_OUTPUT_SIZE) { idx ->
            val r = re[idx]; val img = im[idx]
            val mag = (r * r + img * img) * scaleFactor
            if (mag < minVal) minVal else mag
        }
    }
}

// ---------------------------------------------------------------------------
// Desktop recognition: record mic, resample to 16kHz mono, fingerprint, query.
// ---------------------------------------------------------------------------

object DesktopRecognition {
    private const val RECORD_DURATION_MS = 10_000L
    private const val REQUIRED_SAMPLE_RATE = 16_000

    private data class Capture(
        val data: ByteArray,
        val sampleRate: Float,
        val channels: Int,
        val bigEndian: Boolean,
    )

    suspend fun recognize(): RecognitionResult = withContext(Dispatchers.IO) {
        val captured = record()
        val mono = toMonoLittleEndian(captured)
        val resampled = resample(mono, captured.sampleRate.toInt(), REQUIRED_SAMPLE_RATE)
        val signature = ShazamFingerprint.fromI16(resampled)
        val sampleDurationMs = (resampled.size / 2) * 1000L / REQUIRED_SAMPLE_RATE
        Shazam.recognize(signature, sampleDurationMs).getOrThrow()
    }

    /**
     * Records ~10s from the default microphone. Tries a few well-supported rates
     * and reads back the *actual* negotiated line format, so a driver that only
     * supports 48 kHz (or stereo) is handled correctly instead of producing a
     * time-scaled / interleaved fingerprint that never matches.
     */
    private fun record(): Capture {
        val candidates = floatArrayOf(48_000f, 44_100f, 16_000f, 22_050f)
        var lastError: Throwable? = null
        for (rate in candidates) {
            try {
                val format = AudioFormat(rate, 16, 1, true, false)
                val info = DataLine.Info(TargetDataLine::class.java, format)
                if (!AudioSystem.isLineSupported(info)) continue
                val line = AudioSystem.getLine(info) as TargetDataLine
                try {
                    line.open(format)
                    line.start()
                    val out = ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    val start = System.currentTimeMillis()
                    while (System.currentTimeMillis() - start < RECORD_DURATION_MS) {
                        val read = line.read(buffer, 0, buffer.size)
                        if (read > 0) out.write(buffer, 0, read)
                    }
                    val actual = line.format
                    return Capture(out.toByteArray(), actual.sampleRate, actual.channels, actual.isBigEndian)
                } finally {
                    runCatching { line.stop() }
                    runCatching { line.close() }
                }
            } catch (t: Throwable) {
                lastError = t
            }
        }
        throw IllegalStateException("No microphone available", lastError)
    }

    /** Downmixes to mono and normalizes byte order to little-endian 16-bit PCM. */
    private fun toMonoLittleEndian(c: Capture): ByteArray {
        val srcOrder = if (c.bigEndian) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN
        if (c.channels <= 1) {
            if (!c.bigEndian) return c.data
            val shorts = ShortArray(c.data.size / 2)
            ByteBuffer.wrap(c.data).order(srcOrder).asShortBuffer().get(shorts)
            val out = ByteArray(c.data.size)
            ByteBuffer.wrap(out).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shorts)
            return out
        }
        val frameSize = c.channels * 2
        val frames = c.data.size / frameSize
        val out = ByteArray(frames * 2)
        val src = ByteBuffer.wrap(c.data).order(srcOrder)
        val dst = ByteBuffer.wrap(out).order(ByteOrder.LITTLE_ENDIAN)
        for (f in 0 until frames) {
            var sum = 0
            for (ch in 0 until c.channels) sum += src.short.toInt()
            dst.putShort((sum / c.channels).toShort())
        }
        return out
    }

    /**
     * Anti-aliased resampler (band-limited sinc + Hann window). Replaces the old
     * linear-interpolation resampler, whose aliasing corrupted the 4–5.5 kHz band
     * Shazam relies on and caused persistent "no match".
     */
    private fun resample(inputMonoLE: ByteArray, fromRate: Int, toRate: Int): ByteArray {
        val inShorts = ShortArray(inputMonoLE.size / 2)
        ByteBuffer.wrap(inputMonoLE).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(inShorts)
        if (fromRate == toRate) return inputMonoLE

        val ratio = fromRate.toDouble() / toRate.toDouble()
        val outLen = (inShorts.size / ratio).toInt()
        val outShorts = ShortArray(outLen)
        val cutoff = 0.5 * minOf(1.0, toRate.toDouble() / fromRate.toDouble())
        val taps = 48

        for (i in outShorts.indices) {
            val t = i * ratio
            val t0 = t.toInt()
            val frac = t - t0
            var acc = 0.0
            var norm = 0.0
            for (j in -taps..taps) {
                val idx = t0 + j
                if (idx < 0 || idx >= inShorts.size) continue
                val x = frac - j
                val s = if (x == 0.0) 2.0 * cutoff
                    else 2.0 * cutoff * sin(2.0 * PI * cutoff * x) / (2.0 * PI * cutoff * x)
                val w = 0.5 + 0.5 * cos(PI * x / taps)
                val h = s * w
                acc += inShorts[idx].toDouble() * h
                norm += h
            }
            if (norm != 0.0) acc /= norm
            outShorts[i] = acc.coerceIn(-32768.0, 32767.0).toInt().toShort()
        }

        val out = ByteArray(outShorts.size * 2)
        ByteBuffer.wrap(out).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(outShorts)
        return out
    }
}

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

@Serializable
data class RecognitionHistoryItem(
    val title: String,
    val artist: String,
    val coverArtUrl: String?,
    val timestamp: Long,
)

@Composable
fun SongRecognitionScreen(
    language: String,
    onBack: () -> Unit,
    history: List<RecognitionHistoryItem>,
    onHistoryChange: (List<RecognitionHistoryItem>) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) } // null = idle, else status text
    var recognizing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<RecognitionResult?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(language, onBack)
        Text(Localization.get(language, "song_recognition"), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))

        Button(
            enabled = !recognizing,
            onClick = {
                recognizing = true
                status = Localization.get(language, "listening")
                result = null
                scope.launch {
                    runCatching { DesktopRecognition.recognize() }
                        .onSuccess { r ->
                            result = r
                            status = null
                            val item = RecognitionHistoryItem(
                                title = r.title,
                                artist = r.artist,
                                coverArtUrl = r.coverArtHqUrl ?: r.coverArtUrl,
                                timestamp = System.currentTimeMillis(),
                            )
                            onHistoryChange((listOf(item) + history.filter { it.title != item.title }).take(50))
                        }
                        .onFailure { e ->
                            status = e.message ?: Localization.get(language, "recognition_failed")
                        }
                    recognizing = false
                }
            },
        ) {
            if (recognizing) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text(Localization.get(language, "recognize"))
        }

        status?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }

        result?.let { r ->
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Thumbnail(r.coverArtHqUrl ?: r.coverArtUrl, Modifier.size(96.dp))
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(r.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(r.artist, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    r.album?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }

        if (history.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(Localization.get(language, "recognition_history"), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                items(history, key = { "${it.timestamp}-${it.title}" }) { item ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Thumbnail(item.coverArtUrl, Modifier.size(40.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(item.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}
