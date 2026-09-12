package com.music.vivi.desktop.player

import com.music.vivi.desktop.DesktopSettings
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine

/**
 * Real audio output device selection (port of the mobile "audio output device"
 * picker).
 *
 * The desktop engine writes PCM to a Java Sound [SourceDataLine]. By default it
 * asks the OS for the default line; here the user can pin a specific
 * [javax.sound.sampled.Mixer] (the OS audio endpoint, e.g. "Speakers",
 * "Headphones", a virtual loopback device…). The chosen mixer name is persisted
 * in [DesktopSettings.outputDeviceName] and read back by [AudioPlayer] whenever
 * it opens a new line, so a change applies from the next track/seek.
 *
 * Every call is best-effort: a device that disappeared since it was saved falls
 * back to the system default instead of breaking playback.
 */
object AudioOutput {

    data class Device(val name: String, val description: String) {
        /** Human label: "Description — name" unless they are the same. */
        val label: String
            get() = when {
                description.isBlank() || description == name -> name
                else -> "$description — $name"
            }
    }

    /** PCM format used only to probe which mixers can play audio at all. */
    private val probeFormat = AudioFormat(
        AudioFormat.Encoding.PCM_SIGNED,
        44100f,
        16,
        2,
        4,
        44100f,
        false,
    )

    /** Mixer name of the selected device ("" = system default). */
    @Volatile
    var selectedName: String = ""

    /** Reloads the persisted selection (called once at startup). */
    fun load() {
        selectedName = DesktopSettings.load().outputDeviceName
    }

    /**
     * All mixers that can play PCM through a [SourceDataLine], in OS order.
     *
     * `Mixer.sourceLineInfo` (the mixer's own playback lines) is the primary
     * probe: asking with a fully-specified format makes the default OS endpoint
     * answer "no" — it only negotiates the format later, when the line is opened
     * — which used to leave the picker with nothing but "System default".
     */
    fun devices(): List<Device> = runCatching {
        val typed = DataLine.Info(SourceDataLine::class.java, probeFormat)
        AudioSystem.getMixerInfo().mapNotNull { mixerInfo ->
            val mixer = runCatching { AudioSystem.getMixer(mixerInfo) }.getOrNull()
                ?: return@mapNotNull null
            val supported = runCatching {
                mixer.sourceLineInfo.isNotEmpty() || mixer.isLineSupported(typed)
            }.getOrDefault(false)
            if (supported) Device(mixerInfo.name, mixerInfo.description) else null
        }
    }.getOrDefault(emptyList())

    /** Persists [name] as the preferred device ("" = system default). */
    fun apply(name: String) {
        selectedName = name
        DesktopSettings.update { it.copy(outputDeviceName = name) }
    }

    /**
     * Opens a [SourceDataLine] for [format] on the selected device. Returns null
     * when no line could be opened (the caller reports the failure); when the
     * saved device is gone or does not accept the format, the system default is
     * used instead.
     */
    fun openLine(format: AudioFormat): SourceDataLine? {
        val name = selectedName
        if (name.isNotBlank()) {
            val mixerInfo = AudioSystem.getMixerInfo().firstOrNull { it.name == name }
            if (mixerInfo != null) {
                val mixer = runCatching { AudioSystem.getMixer(mixerInfo) }.getOrNull()
                if (mixer != null) {
                    val line = runCatching {
                        mixer.getLine(DataLine.Info(SourceDataLine::class.java, format))
                    }.getOrNull() as? SourceDataLine
                    if (line != null) return line
                }
            }
        }
        return runCatching { AudioSystem.getSourceDataLine(format) }.getOrNull()
    }
}
