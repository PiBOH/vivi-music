package com.vivimusic.de.data.playback

import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioReference
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

actual fun createAudioEngine(): AudioEngine = DesktopAudioEngine()

/**
 * Desktop audio engine backed by LavaPlayer (streaming + decoding) and Java
 * Sound (audio output). It plays the direct audio stream URL resolved by the
 * InnerTube client and publishes position/duration/state via a StateFlow.
 */
class DesktopAudioEngine : AudioEngine {

    private val manager: AudioPlayerManager = DefaultAudioPlayerManager().apply {
        configuration.outputFormat = StandardAudioDataFormats.COMMON_PCM_S16_LE
        AudioSourceManagers.registerRemoteSources(this)
    }

    private val player: AudioPlayer = manager.createPlayer()

    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    @Volatile
    private var running = false

    private var decodeThread: Thread? = null

    init {
        player.addListener(object : AudioEventAdapter() {
            override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
                stopPlayback()
                _state.value = _state.value.copy(
                    isPlaying = false,
                    isBuffering = false,
                    positionMs = _state.value.durationMs,
                )
            }

            override fun onTrackException(player: AudioPlayer, track: AudioTrack, exception: FriendlyException) {
                _state.value = _state.value.copy(
                    isPlaying = false,
                    isBuffering = false,
                    error = exception.message,
                )
            }
        })
    }

    override fun play(songId: String, url: String, durationMs: Long) {
        stop()
        _state.value = PlaybackState(songId = songId, durationMs = durationMs, isBuffering = true)
        manager.loadItem(AudioReference(url, null), object : AudioLoadResultHandler {
            override fun trackLoaded(track: AudioTrack) = start(track)

            override fun playlistLoaded(playlist: AudioPlaylist) {
                val track = playlist.selectedTrack ?: playlist.tracks.firstOrNull()
                if (track != null) start(track) else fail("No playable track found")
            }

            override fun noMatches() = fail("No playable stream found")

            override fun loadFailed(exception: FriendlyException) = fail(exception.message)
        })
    }

    private fun start(track: AudioTrack) {
        player.playTrack(track)
        _state.value = _state.value.copy(isPlaying = true, isBuffering = false)
        startDecodeLoop()
    }

    private fun fail(message: String?) {
        _state.value = _state.value.copy(isPlaying = false, isBuffering = false, error = message)
    }

    override fun toggle() {
        if (player.isPaused) resume() else pause()
    }

    override fun pause() {
        player.isPaused = true
        _state.value = _state.value.copy(isPlaying = false)
    }

    override fun resume() {
        player.isPaused = false
        _state.value = _state.value.copy(isPlaying = true)
    }

    override fun seekTo(positionMs: Long) {
        player.playingTrack?.position = positionMs
        _state.value = _state.value.copy(positionMs = positionMs)
    }

    override fun stop() {
        stopPlayback()
        _state.value = PlaybackState()
    }

    override fun release() {
        stop()
        player.destroy()
        manager.shutdown()
    }

    private fun stopPlayback() {
        running = false
        decodeThread?.interrupt()
        decodeThread = null
        player.stopTrack()
    }

    private fun startDecodeLoop() {
        running = true
        decodeThread = Thread {
            var line: SourceDataLine? = null
            try {
                line = openLine()
                if (line == null) {
                    _state.value = _state.value.copy(isPlaying = false, error = "No audio output device available")
                    return@Thread
                }
                var lastPositionUpdate = 0L
                while (running) {
                    val frame = player.provide()
                    if (frame == null || frame.isTerminator) {
                        try {
                            Thread.sleep(10)
                        } catch (e: InterruptedException) {
                            break
                        }
                    } else {
                        val data = frame.data
                        if (data.isNotEmpty()) {
                            line.write(data, 0, data.size)
                        }
                    }
                    val now = System.currentTimeMillis()
                    if (now - lastPositionUpdate >= 200) {
                        lastPositionUpdate = now
                        val position = player.playingTrack?.position ?: 0L
                        _state.value = _state.value.copy(positionMs = position)
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isPlaying = false, error = e.message)
            } finally {
                try { line?.drain() } catch (_: Exception) {}
                try { line?.stop() } catch (_: Exception) {}
                try { line?.close() } catch (_: Exception) {}
            }
        }.apply {
            isDaemon = true
            start()
        }
    }

    private fun openLine(): SourceDataLine? = try {
        val format = AudioFormat(48_000f, 16, 2, true, false)
        val line = AudioSystem.getSourceDataLine(format)
        line.open(format)
        line.start()
        line
    } catch (e: Exception) {
        null
    }
}
