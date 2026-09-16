package com.music.vivi.desktop

import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Small persistent cache for fetched lyrics, mirroring the audio file cache:
 * lyrics are kept in memory for the session and written to disk so they survive
 * restarts (matching the "stream cache forever" expectation). The Lyrics screen
 * reads/writes through here.
 *
 * The cache is keyed by BOTH the video id and the fetch mode ([preferSynced]):
 * a plain (non-timed) result produced by a "first answer wins" fetch must never
 * satisfy a later "synced lyrics" request (and vice versa), otherwise the
 * resolver fixes appear to do nothing because the stale entry is returned from
 * disk forever.
 */
object LyricsCache {
    private val dir = File(System.getProperty("user.home"), ".vivimusic/cache/lyrics").apply { mkdirs() }
    private val mem = ConcurrentHashMap<String, String>()

    fun get(videoId: String, preferSynced: Boolean): String? {
        val key = memKey(videoId, preferSynced)
        mem[key]?.let { return it }
        val f = file(videoId, preferSynced)
        return if (f.exists()) {
            runCatching { f.readText() }.getOrNull()?.also { mem[key] = it }
        } else {
            null
        }
    }

    fun put(videoId: String, lyrics: String, preferSynced: Boolean) {
        val key = memKey(videoId, preferSynced)
        mem[key] = lyrics
        runCatching { file(videoId, preferSynced).writeText(lyrics) }
    }

    /**
     * The file name carries BOTH a version suffix and the fetch mode
     * (`-s` = synced-first, `-p` = plain/first-answer), so:
     *  - entries written by an older resolver are ignored and re-fetched once;
     *  - toggling "Synced lyrics" never reuses the other mode's cached text.
     *
     * **Any change to how a provider is picked or validated MUST bump this
     * version.** A cached answer is served without asking any provider again, so
     * without a bump a wrong association survives the fix forever: the reported
     * `Blu Da Ba Dee` kept showing `Move Your Body - Eiffel 65` (the exact
     * mismatch fixed by the KuGou matching in 1.50.67) because that entry was
     * still on disk in `68ugkg9RePc.s.v4.txt`. v5 invalidates it; v4 separated
     * the fetch modes; v3 invalidated first-answer-wins entries; v2 invalidated
     * single-provider, no-duration lookups.
     */
    private fun file(videoId: String, preferSynced: Boolean): File {
        val safe = videoId.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val mode = if (preferSynced) "s" else "p"
        return File(dir, "$safe.$mode.v$CACHE_VERSION.txt")
    }

    /** Cache format version — see [file] for when and why to bump it. */
    private const val CACHE_VERSION = 5

    private fun memKey(videoId: String, preferSynced: Boolean) = "$videoId|${if (preferSynced) "s" else "p"}"
}