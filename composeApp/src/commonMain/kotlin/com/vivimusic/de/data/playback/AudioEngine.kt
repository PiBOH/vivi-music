package com.vivimusic.de.data.playback

import kotlinx.coroutines.flow.StateFlow

/**
 * Snapshot of the current playback state, published by an [AudioEngine].
 */
data class PlaybackState(
    val songId: String? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val error: String? = null,
)

/**
 * Desktop audio playback abstraction. Implementations decode and play a remote
 * audio stream (resolved by the InnerTube client) and publish the current
 * state through [state].
 */
interface AudioEngine {
    val state: StateFlow<PlaybackState>

    fun play(songId: String, url: String, durationMs: Long)
    fun toggle()
    fun pause()
    fun resume()
    fun seekTo(positionMs: Long)
    fun stop()
    fun release()
}

expect fun createAudioEngine(): AudioEngine
