package com.music.vivi.desktop

import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Small persistent cache for fetched lyrics, mirroring the audio file cache:
 * lyrics are kept in memory for the session and written to disk so they survive
 * restarts (matching the "stream cache forever" expectation). The Lyrics screen
 * and the look-ahead prefetcher both read/write through here.
 */
object LyricsCache {
    private val dir = File(System.getProperty("user.home"), ".vivimusic/cache/lyrics").apply { mkdirs() }
    private val mem = ConcurrentHashMap<String, String>()

    fun get(videoId: String): String? {
        mem[videoId]?.let { return it }
        val f = file(videoId)
        return if (f.exists()) {
            runCatching { f.readText() }.getOrNull()?.also { mem[videoId] = it }
        } else {
            null
        }
    }

    fun put(videoId: String, lyrics: String) {
        mem[videoId] = lyrics
        runCatching { file(videoId).writeText(lyrics) }
    }

    private fun file(videoId: String): File {
        val safe = videoId.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return File(dir, "$safe.txt")
    }
}
