package com.music.vivi.desktop

import com.music.innertube.YouTube
import com.music.innertube.models.WatchEndpoint
import com.music.kugou.KuGou
import com.music.lrclib.LrcLib
import com.music.lyrics.TextFolding
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
 *    track that no community server covers still shows its correct text (the
 *    video's own *timed captions* would be even better, but the mobile app's
 *    `YouTubeSubtitle` source cannot be used here: `get_transcript` answers
 *    `400 FAILED_PRECONDITION` to the synthesised params for EVERY video,
 *    captioned ones included — verified with direct requests — so that
 *    provider is dead weight in the mobile chain too);
 *  - logs which provider answered (and every failure) through [AppLog] so a
 *    bad case is diagnosable from an exported log.
 */
object DesktopLyrics {
    /** Per-provider ceiling; a hung provider is skipped, never blocks lyrics. */
    private const val PROVIDER_TIMEOUT_MS = 6_000L

    /**
     * Every provider the resolver knows, in the built-in order. The Content
     * screen shows this list so the user can reorder it or turn entries off;
     * the names are the providers' own (brand) names, so they are not
     * translated.
     */
    val PROVIDER_ORDER: List<String> = listOf(
        "LrcLib", "BetterLyrics", "YouLyPlus", "KuGou", "Musixmatch", "Paxsenix", "Unison",
    )

    /**
     * How far a synced lyric file may legitimately run past the track's own
     * duration: intros/outros and radio edits differ by seconds, not minutes.
     */
    private const val LYRICS_TAIL_ALLOWANCE_MS = 60_000L

    /** LRC line timestamp, e.g. `[01:23.45]` / `[1:23]`. */
    private val lrcTime = Regex("""\[\d{1,2}:\d{1,2}(?:[.:]\d{1,3})?]""")

    /** Rich-sync word timestamp, e.g. `<01:23.45>`. */
    private val richTime = Regex("""<\d{1,2}:\d{2}(?:[.:]\d{1,3})?>""")

    /** True when the text carries timestamps (line-level LRC or word-level). */
    private fun looksSynced(text: String) = lrcTime.containsMatchIn(text) || richTime.containsMatchIn(text)

    /**
     * Timestamp of the LAST lyric line, in milliseconds, or null when the text
     * has no line timestamps.
     *
     * Used as a sanity check: lyrics belonging to another song (or to a much
     * longer album version) keep "singing" well past the end of the track that
     * is playing, which is measurable even when the provider's matching looked
     * plausible.
     */
    private fun lastTimestampMs(text: String): Long? {
        var last: Long? = null
        for (match in lrcTime.findAll(text)) {
            val parts = match.value.trim('[', ']').split(':', '.')
            if (parts.size < 2) continue
            val minutes = parts[0].toLongOrNull() ?: continue
            val seconds = parts[1].toLongOrNull() ?: continue
            val frac = parts.getOrNull(2)?.toLongOrNull() ?: 0L
            // Two-digit fractions are centiseconds, three-digit ones ms.
            val fracMs = when ((parts.getOrNull(2)?.length ?: 0)) {
                1 -> frac * 100
                2 -> frac * 10
                else -> frac
            }
            last = minutes * 60_000 + seconds * 1_000 + fracMs
        }
        return last
    }

    /**
     * Tries the providers in order and returns the first usable text.
     * [durationMs] may be 0/unknown — providers then fall back to their own
     * title/artist matching (same as before).
     *
     * When [preferSynced] is true (the "Synced lyrics" option), a TIMED
     * result is preferred across the whole chain: once a provider answers with
     * plain text, the search keeps going through the remaining sources in the
     * hope of finding a synced version of the same song, and only falls back
     * to that plain text when no source has timestamps. When false, the first
     * usable answer wins (previous behavior).
     */
    suspend fun fetch(
        videoId: String,
        title: String,
        artist: String,
        durationMs: Long,
        album: String? = null,
        preferSynced: Boolean = true,
    ): Result<String> {
        val durationSec = if (durationMs > 0) (durationMs / 1000L).toInt() else -1

        // Order: community synced sources first (LrcLib keeps its previous
        // priority so songs that already worked keep working), official
        // YouTube Music lyrics last as an exact-text guarantee. Album is
        // passed through like the mobile app does — it helps the providers
        // pick the right recording (radio edit vs original, live vs studio).
        // Providers match against plain-text catalogues, while YouTube Music
        // titles/artists are full of decorative Unicode (`ＭＩＧＵＥＬ 𝑷𝒉𝒐𝒏𝒌`,
        // `𝗖𝗥𝗢𝗪𝗡 𝗕𝗘𝗔𝗥`): the folded text (`MIGUEL Phonk`, `CROWN BEAR`) is
        // what every provider is queried with, while the original stays in the
        // log so the user still recognises the track. Without this, those tracks
        // matched nothing anywhere and ended up with no lyrics at all.
        val queryTitle = TextFolding.fold(title)
        val queryArtist = TextFolding.fold(artist)
        val queryAlbum = album?.let { TextFolding.fold(it) }

        // The Content screen can reorder these and turn some of them off
        // (port of the mobile "Lyrics provider priority" list): the stored
        // order is the order they are asked in, and a provider that is not in
        // it is skipped. An empty list keeps the built-in order with all on.
        val chain: List<Pair<String, suspend () -> Result<String>>> = listOf(
            "LrcLib" to { LrcLib.getLyrics(queryTitle, queryArtist, durationSec, queryAlbum) },
            "BetterLyrics" to { BetterLyrics.getLyrics(queryTitle, queryArtist, durationSec, queryAlbum) },
            "YouLyPlus" to { YouLyPlus.getLyrics(queryTitle, queryArtist, durationSec, queryAlbum, id = videoId) },
            "KuGou" to { KuGou.getLyrics(queryTitle, queryArtist, durationSec, queryAlbum) },
            "Musixmatch" to { Musixmatch.getLyrics(queryTitle, queryArtist, durationSec, queryAlbum) },
            "Paxsenix" to { Paxsenix.getLyrics(queryTitle, queryArtist, durationSec, queryAlbum) },
            "Unison" to { Unison.getLyrics(queryTitle, queryArtist, durationSec, queryAlbum, videoId = videoId) },
        )

        val priority = DesktopSettings.load().lyricsProviderPriority
        val providers = if (priority.isEmpty()) {
            chain
        } else {
            priority.mapNotNull { name -> chain.firstOrNull { it.first == name } }
        }

        AppLog.log(
            "lyrics",
            "fetch lyrics for '$title' [$videoId] (duration=${if (durationSec > 0) "${durationSec}s" else "unknown"}, preferSynced=$preferSynced, providers=${providers.joinToString("/") { it.first }})",
        )
        var plainFallback: String? = null
        for ((name, call) in providers) {
            val result = withTimeoutOrNull(PROVIDER_TIMEOUT_MS) { call() }
            if (result == null) {
                AppLog.log("lyrics", "  $name: timed out after ${PROVIDER_TIMEOUT_MS / 1000}s for '$title'")
                continue
            }
            result
                .onSuccess { text ->
                    if (text.isBlank()) {
                        AppLog.log("lyrics", "  $name: returned blank lyrics for '$title'")
                        return@onSuccess
                    }
                    val synced = looksSynced(text)
                    AppLog.log(
                        "lyrics",
                        "  $name: got ${text.length} chars (${if (synced) "synced" else "plain"}) for '$title'",
                    )
                    // A synced result that runs far past the end of the track
                    // is not this song: keep looking instead of showing wrong
                    // lyrics (an answer that is merely a different edit of the
                    // same song stays within the allowance).
                    if (synced && durationMs > 0) {
                        val last = lastTimestampMs(text)
                        if (last != null && last > durationMs + LYRICS_TAIL_ALLOWANCE_MS) {
                            AppLog.log(
                                "lyrics",
                                "  $name: lyrics run ${(last - durationMs) / 1000}s past the " +
                                    "${durationMs / 1000}s track — discarding (wrong song or version)",
                            )
                            return@onSuccess
                        }
                    }
                    // Synced result (or the option off / any answer): done.
                    if (synced || !preferSynced) return Result.success(text)
                    // Plain answer while syncing is wanted: remember it as a
                    // fallback but keep looking for a timed version.
                    if (plainFallback == null) {
                        plainFallback = text
                        AppLog.log("lyrics", "  $name: plain fallback kept — hunting for synced lyrics")
                    }
                }
                .onFailure { AppLog.log("lyrics", "  $name: ${it.message} for '$title'") }
        }

        // No source had timestamps: use the best plain text we found.
        plainFallback?.let {
            AppLog.log("lyrics", "no synced lyrics found — using the plain fallback (${it.length} chars) for '$title'")
            return Result.success(it)
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
