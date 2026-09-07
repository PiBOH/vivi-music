package com.music.vivi.desktop

import com.music.innertube.YouTube
import com.music.innertube.models.WatchEndpoint
import com.music.kugou.KuGou
import com.music.lrclib.LrcLib
import com.music.musixmatch.Musixmatch
import com.music.paxsenix.Paxsenix
import com.music.unison.Unison
import com.music.vivi.betterlyrics.BetterLyrics
import com.music.youlyplus.YouLyPlus
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Desktop lyrics resolver.
 *
 * The mobile app asks a whole chain of lyric providers (community + official)
 * and picks the first one that answers; the desktop edition used to ask a
 * single community server (LrcLib) with no real duration, which produced
 * missing / wrong-version / un-synced lyrics and cached the mistake forever.
 *
 * This mirror of the mobile flow:
 *  - passes the REAL track duration (when known) to every provider so the
 *    correct recording is matched (± a few seconds), not just the same title;
 *  - falls back across providers in a fixed order until one answers (synced
 *    LRC preferred by each provider when it has one);
 *  - ends with the official YouTube Music lyrics for the exact video, so a
 *    track that no community server covers still shows its correct text;
 *  - logs which provider answered (and every failure) through [AppLog] so a
 *    bad case is diagnosable from an exported log.
 */
object DesktopLyrics {
    /** Per-provider ceiling; a hung provider is skipped, never blocks lyrics. */
    private const val PROVIDER_TIMEOUT_MS = 6_000L

    /**
     * Tries the providers in order and returns the first usable text.
     * [durationMs] may be 0/unknown — providers then fall back to their own
     * title/artist matching (same as before).
     */
    suspend fun fetch(
        videoId: String,
        title: String,
        artist: String,
        durationMs: Long,
    ): Result<String> {
        val durationSec = if (durationMs > 0) (durationMs / 1000L).toInt() else -1

        // Order: community synced sources first (LrcLib keeps its previous
        // priority so songs that already worked keep working), official
        // YouTube Music lyrics last as an exact-text guarantee.
        val providers: List<Pair<String, suspend () -> Result<String>>> = listOf(
            "LrcLib" to { LrcLib.getLyrics(title, artist, durationSec) },
            "BetterLyrics" to { BetterLyrics.getLyrics(title, artist, durationSec) },
            "YouLyPlus" to { YouLyPlus.getLyrics(title, artist, durationSec, id = videoId) },
            "KuGou" to { KuGou.getLyrics(title, artist, durationSec) },
            "Musixmatch" to { Musixmatch.getLyrics(title, artist, durationSec) },
            "Paxsenix" to { Paxsenix.getLyrics(title, artist, durationSec) },
            "Unison" to { Unison.getLyrics(title, artist, durationSec, videoId = videoId) },
        )

        AppLog.log(
            "lyrics",
            "fetch lyrics for '$title' [$videoId] (duration=${if (durationSec > 0) "${durationSec}s" else "unknown"})",
        )
        for ((name, call) in providers) {
            val result = withTimeoutOrNull(PROVIDER_TIMEOUT_MS) { call() }
            if (result == null) {
                AppLog.log("lyrics", "  $name: timed out after ${PROVIDER_TIMEOUT_MS / 1000}s for '$title'")
                continue
            }
            result
                .onSuccess { text ->
                    if (text.isNotBlank()) {
                        AppLog.log("lyrics", "  $name: got ${text.length} chars for '$title'")
                        return Result.success(text)
                    }
                    AppLog.log("lyrics", "  $name: returned blank lyrics for '$title'")
                }
                .onFailure { AppLog.log("lyrics", "  $name: ${it.message} for '$title'") }
        }

        // Last resort: the official lyrics for this exact video (never a wrong
        // version; plain text when the video has no timed captions).
        val official = withTimeoutOrNull(PROVIDER_TIMEOUT_MS) { youTubeMusicOfficial(videoId) }
        if (official != null) {
            AppLog.log("lyrics", "  YouTube Music: got ${official.length} chars (official) for '$title'")
            return Result.success(official)
        }
        AppLog.log("lyrics", "no lyrics from any provider for '$title' [$videoId]")
        return Result.failure(IllegalStateException("No lyrics found from any provider"))
    }

    private suspend fun youTubeMusicOfficial(videoId: String): String? {
        // YouTube.next already returns a Result (no extra runCatching: it
        // would nest Result<Result<...>> and hide the members below).
        val next = YouTube.next(WatchEndpoint(videoId = videoId)).getOrNull() ?: return null
        val endpoint = next.lyricsEndpoint ?: return null
        return YouTube.lyrics(endpoint)
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }
}
