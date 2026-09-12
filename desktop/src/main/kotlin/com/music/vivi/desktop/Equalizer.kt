package com.music.vivi.desktop

import kotlinx.serialization.Serializable
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Desktop port of the mobile parametric equalizer (`app/.../eq`).
 *
 * The model types are `@Serializable` so they can be persisted in
 * [DesktopSettings]; the parser reads AutoEQ `ParametricEQ.txt` files; the
 * [EqualizerProcessor] applies the active profile to the 16-bit PCM bytes the
 * desktop `AudioPlayer` writes to the output line. When no profile is active
 * the player keeps its current pass-through path untouched.
 */

/** Filter types supported by the biquad implementation (AutoEQ naming). */
@Serializable
enum class FilterType { PK, LSC, HSC, LPQ, HPQ }

/** A single parametric EQ band. */
@Serializable
data class ParametricEQBand(
    val frequency: Double,                      // Center frequency in Hz
    val gain: Double,                           // Gain in dB
    val q: Double = 1.41,                       // Q factor (bandwidth) - default sqrt(2)
    val filterType: FilterType = FilterType.PK,
    val enabled: Boolean = true,
)

/** A complete parametric EQ configuration (parsed from an AutoEQ preset). */
@Serializable
data class ParametricEQ(
    val preamp: Double,
    val bands: List<ParametricEQBand>,
    val metadata: Map<String, String> = emptyMap(),
) {
    companion object {
        const val MAX_BANDS = 20
    }
}

/** Saved EQ profile with metadata (mirrors the mobile `SavedEQProfile`). */
@Serializable
data class SavedEQProfile(
    val id: String,
    val name: String,
    val deviceModel: String,
    val bands: List<ParametricEQBand>,
    val preamp: Double = 0.0,
    val isCustom: Boolean = false,
    val isActive: Boolean = false,
    val addedTimestamp: Long = System.currentTimeMillis(),
)

/**
 * Parser for AutoEq `ParametricEQ.txt` files:
 *   Preamp: -5.2 dB
 *   Filter 1: ON LSC Fc 105 Hz Gain 8.8 dB Q 0.70
 *   Filter 2: ON PK Fc 70 Hz Gain -6.7 dB Q 0.29
 */
object ParametricEQParser {

    fun parseText(content: String): ParametricEQ {
        var preamp = 0.0
        val bands = mutableListOf<ParametricEQBand>()
        val metadata = mutableMapOf<String, String>()

        for (line in content.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            when {
                trimmed.startsWith("Preamp:", ignoreCase = true) -> {
                    preamp = parsePreamp(trimmed)
                }
                trimmed.startsWith("Filter", ignoreCase = true) -> {
                    parseFilterLine(trimmed)?.let { bands.add(it) }
                }
                else -> {
                    val parts = trimmed.split(":", limit = 2)
                    if (parts.size == 2) metadata[parts[0].trim()] = parts[1].trim()
                }
            }
        }
        return ParametricEQ(preamp = preamp, bands = bands, metadata = metadata)
    }

    private fun parsePreamp(line: String): Double {
        val regex = Regex("""Preamp:\s*([-+]?\d+\.?\d*)\s*dB""", RegexOption.IGNORE_CASE)
        return regex.find(line)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
    }

    private fun parseFilterLine(line: String): ParametricEQBand? = try {
        if (!line.contains("ON", ignoreCase = true)) return null
        val filterType = parseFilterType(line) ?: return null
        val frequency = parseValue(line, "Fc", "Hz") ?: return null
        val gain = parseValue(line, "Gain", "dB") ?: return null
        val q = parseValue(line, "Q", null) ?: return null
        ParametricEQBand(filterType = filterType, frequency = frequency, gain = gain, q = q)
    } catch (_: Exception) {
        null
    }

    private fun parseFilterType(line: String): FilterType? = when {
        line.contains("LSC", ignoreCase = true) -> FilterType.LSC
        line.contains("HSC", ignoreCase = true) -> FilterType.HSC
        line.contains("PK", ignoreCase = true) -> FilterType.PK
        line.contains("LPQ", ignoreCase = true) -> FilterType.LPQ
        line.contains("HPQ", ignoreCase = true) -> FilterType.HPQ
        else -> null
    }

    private fun parseValue(line: String, keyword: String, unit: String?): Double? {
        val unitPattern = if (unit != null) "\\s*$unit" else ""
        val regex = Regex("""$keyword\s+([-+]?\d+\.?\d*)$unitPattern""", RegexOption.IGNORE_CASE)
        return regex.find(line)?.groupValues?.get(1)?.toDoubleOrNull()
    }
}

/**
 * Biquad filter (Robert Bristow-Johnson's Audio EQ Cookbook), ported verbatim
 * from the mobile implementation. Supports peaking (PK), low-shelf (LSC) and
 * high-shelf (HSC); the pass-through fallback keeps unknown types as peaking.
 */
class BiquadFilter(
    val sampleRate: Int,
    val frequency: Double,
    var gain: Double,
    val q: Double = 1.41,
    val filterType: FilterType = FilterType.PK,
) {
    var lastOutputLeft = 0.0
        private set
    var lastOutputRight = 0.0
        private set

    private var a0 = 0.0
    private var a1 = 0.0
    private var a2 = 0.0
    private var b0 = 0.0
    private var b1 = 0.0
    private var b2 = 0.0

    private var x1L = 0.0
    private var x2L = 0.0
    private var y1L = 0.0
    private var y2L = 0.0
    private var x1R = 0.0
    private var x2R = 0.0
    private var y1R = 0.0
    private var y2R = 0.0

    init {
        calculateCoefficients()
    }

    private fun calculateCoefficients() {
        when (filterType) {
            FilterType.PK -> calculatePeaking()
            FilterType.LSC -> calculateLowShelf()
            FilterType.HSC -> calculateHighShelf()
            else -> calculatePeaking()
        }
    }

    private fun calculatePeaking() {
        val A = 10.0.pow(gain / 40.0)
        val omega = 2.0 * PI * frequency / sampleRate
        val sinOmega = sin(omega)
        val cosOmega = cos(omega)
        val alpha = sinOmega / (2.0 * q)

        b0 = 1.0 + alpha * A
        b1 = -2.0 * cosOmega
        b2 = 1.0 - alpha * A
        a0 = 1.0 + alpha / A
        a1 = -2.0 * cosOmega
        a2 = 1.0 - alpha / A

        b0 /= a0; b1 /= a0; b2 /= a0; a1 /= a0; a2 /= a0; a0 = 1.0
    }

    private fun calculateLowShelf() {
        val A = sqrt(10.0.pow(gain / 20.0))
        val omega = 2.0 * PI * frequency / sampleRate
        val sinOmega = sin(omega)
        val cosOmega = cos(omega)
        val S = 1.0
        val alpha = sinOmega / 2.0 * sqrt((A + 1.0 / A) * (1.0 / S - 1.0) + 2.0)
        val sqrtA = sqrt(A)
        val aPlusOne = A + 1.0
        val aMinusOne = A - 1.0
        val twoSqrtAAlpha = 2.0 * sqrtA * alpha

        b0 = A * (aPlusOne - aMinusOne * cosOmega + twoSqrtAAlpha)
        b1 = 2.0 * A * (aMinusOne - aPlusOne * cosOmega)
        b2 = A * (aPlusOne - aMinusOne * cosOmega - twoSqrtAAlpha)
        a0 = aPlusOne + aMinusOne * cosOmega + twoSqrtAAlpha
        a1 = -2.0 * (aMinusOne + aPlusOne * cosOmega)
        a2 = aPlusOne + aMinusOne * cosOmega - twoSqrtAAlpha

        b0 /= a0; b1 /= a0; b2 /= a0; a1 /= a0; a2 /= a0; a0 = 1.0
    }

    private fun calculateHighShelf() {
        val A = sqrt(10.0.pow(gain / 20.0))
        val omega = 2.0 * PI * frequency / sampleRate
        val sinOmega = sin(omega)
        val cosOmega = cos(omega)
        val S = 1.0
        val alpha = sinOmega / 2.0 * sqrt((A + 1.0 / A) * (1.0 / S - 1.0) + 2.0)
        val sqrtA = sqrt(A)
        val aPlusOne = A + 1.0
        val aMinusOne = A - 1.0
        val twoSqrtAAlpha = 2.0 * sqrtA * alpha

        b0 = A * (aPlusOne + aMinusOne * cosOmega + twoSqrtAAlpha)
        b1 = -2.0 * A * (aMinusOne + aPlusOne * cosOmega)
        b2 = A * (aPlusOne + aMinusOne * cosOmega - twoSqrtAAlpha)
        a0 = aPlusOne - aMinusOne * cosOmega + twoSqrtAAlpha
        a1 = 2.0 * (aMinusOne - aPlusOne * cosOmega)
        a2 = aPlusOne - aMinusOne * cosOmega - twoSqrtAAlpha

        b0 /= a0; b1 /= a0; b2 /= a0; a1 /= a0; a2 /= a0; a0 = 1.0
    }

    fun processSample(input: Double): Double {
        val output = b0 * input + b1 * x1L + b2 * x2L - a1 * y1L - a2 * y2L
        x2L = x1L; x1L = input; y2L = y1L; y1L = output
        return output
    }

    fun processStereo(inputLeft: Double, inputRight: Double) {
        val outputLeft = b0 * inputLeft + b1 * x1L + b2 * x2L - a1 * y1L - a2 * y2L
        x2L = x1L; x1L = inputLeft; y2L = y1L; y1L = outputLeft
        lastOutputLeft = outputLeft

        val outputRight = b0 * inputRight + b1 * x1R + b2 * x2R - a1 * y1R - a2 * y2R
        x2R = x1R; x1R = inputRight; y2R = y1R; y1R = outputRight
        lastOutputRight = outputRight
    }

    fun updateGain(newGain: Double) {
        if (this.gain == newGain) return
        this.gain = newGain
        calculateCoefficients()
    }

    fun reset() {
        x1L = 0.0; x2L = 0.0; y1L = 0.0; y2L = 0.0
        x1R = 0.0; x2R = 0.0; y1R = 0.0; y2R = 0.0
    }
}

/**
 * Applies a parametric EQ profile to 16-bit PCM bytes. Filters are created
 * lazily from the profile + current sample rate/channels and rebuilt when the
 * stream format changes (mirrors the mobile `CustomEqualizerAudioProcessor`
 * pending-profile behaviour). Thread-safe: the audio thread calls [process]
 * while the UI thread may swap profiles via [setProfile].
 */
class EqualizerProcessor {
    @Volatile private var profile: ParametricEQ? = null
    @Volatile private var preampGain: Double = 1.0
    @Volatile private var filters: List<BiquadFilter> = emptyList()
    private val lock = Any()
    @Volatile private var sampleRate = 0
    @Volatile private var channels = 0

    fun setProfile(eq: ParametricEQ?) {
        synchronized(lock) {
            profile = eq
            if (eq == null) {
                filters = emptyList()
                preampGain = 1.0
            } else {
                preampGain = 10.0.pow(eq.preamp / 20.0)
                if (sampleRate > 0) {
                    filters = eq.bands
                        .filter { it.enabled && it.frequency < sampleRate / 2.0 }
                        .map { BiquadFilter(sampleRate, it.frequency, it.gain, it.q, it.filterType) }
                } else {
                    filters = emptyList()
                }
            }
        }
    }

    fun isActive(): Boolean = synchronized(lock) { profile != null }

    /**
     * Processes [data] (16-bit PCM, [channels] interleaved, byte order
     * [bigEndian]) and returns a new array with the EQ applied. Returns the
     * input unchanged when no profile is active.
     */
    fun process(data: ByteArray, bigEndian: Boolean, rate: Int, channelCount: Int): ByteArray {
        if (rate != sampleRate || channelCount != channels) {
            synchronized(lock) {
                if (rate != sampleRate || channelCount != channels) {
                    sampleRate = rate
                    channels = channelCount
                    profile?.let { setProfile(it) }
                }
            }
        }
        val active = synchronized(lock) { if (profile == null) null else filters }
        if (active == null || active.isEmpty()) return data
        val gain = preampGain

        val sampleCount = data.size / 2
        val out = ByteArray(data.size)
        val frameCount = sampleCount / channelCount

        fun readShort(index: Int): Int {
            val i = index * 2
            return if (bigEndian) {
                (data[i].toInt() shl 8) or (data[i + 1].toInt() and 0xFF)
            } else {
                (data[i + 1].toInt() shl 8) or (data[i].toInt() and 0xFF)
            }
        }
        fun writeShort(index: Int, value: Int) {
            val i = index * 2
            val v = value.coerceIn(-32768, 32767)
            if (bigEndian) {
                out[i] = (v shr 8).toByte()
                out[i + 1] = v.toByte()
            } else {
                out[i] = v.toByte()
                out[i + 1] = (v shr 8).toByte()
            }
        }

        var sample = 0
        repeat(frameCount) {
            when (channelCount) {
                1 -> {
                    val s = readShort(sample).toDouble() / 32768.0
                    var processed = s
                    for (f in active) processed = f.processSample(processed)
                    writeShort(sample, (processed * gain * 32768.0).toInt())
                    sample++
                }
                else -> {
                    val left = readShort(sample).toDouble() / 32768.0
                    val right = readShort(sample + 1).toDouble() / 32768.0
                    var pL = left
                    var pR = right
                    for (f in active) {
                        f.processStereo(pL, pR)
                        pL = f.lastOutputLeft
                        pR = f.lastOutputRight
                    }
                    writeShort(sample, (pL * gain * 32768.0).toInt())
                    writeShort(sample + 1, (pR * gain * 32768.0).toInt())
                    sample += 2
                }
            }
        }
        return out
    }
}
