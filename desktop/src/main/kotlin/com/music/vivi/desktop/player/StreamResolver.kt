package com.music.vivi.desktop.player

import com.music.vivi.desktop.DesktopSettings
import com.music.vivi.desktop.GuestSession
import com.music.innertube.NewPipeExtractor
import com.music.innertube.YouTube
import com.music.innertube.YouTubeExtractor
import com.music.innertube.models.YouTubeClient
import com.music.innertube.models.response.PlayerResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Resolves a playable AAC (`audio/mp4`, itag 140) stream URL for a YouTube
 * video id. AAC is used because the desktop player decodes it with the
 * pure-Java `jaad` decoder; Opus would need a WebM demuxer we do not have.
 *
 * This mirrors the mobile app's `YTPlayerUtils.resolvePlaybackData` client
 * chain (main client + fallback clients + n-param deobfuscation + URL
 * validation), minus the Android-only PoToken / connectivity logic. The single
 * ANDROID_VR attempt previously used here was not enough: YouTube often
 * answers it with `LOGIN_REQUIRED` as a bot-detection signal, and without a
 * fallback the resolver returned null ("could not resolve the audio stream").
 */
object StreamResolver {

    /**
     * Audio quality: picks the preferred AAC-LC itag. `139` (HE-AAC) and `251`
     * (Opus) are deliberately excluded because the JAAD decoder cannot decode
     * them. `AUTO`/`HIGH` prefer 256 kbps (itag 141), `LOW` prefers 128 kbps
     * (itag 140).
     */
    enum class AudioQuality(val preferredItags: List<Int>) {
        AUTO(listOf(141, 140)),
        HIGH(listOf(141, 140)),
        LOW(listOf(140, 141));

        companion object {
            fun from(key: String?): AudioQuality =
                entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: AUTO
        }
    }

    /** A resolved stream URL plus the User-Agent required to download it, and
     *  the authoritative track length (from `videoDetails.lengthSeconds`) when
     *  known — used to give the seek slider a correct range immediately. */
    data class ResolvedStream(val url: String, val userAgent: String, val durationMs: Long? = null)

    /** Fast, PoToken-free main client (same as the mobile app). */
    private val MAIN_CLIENT: YouTubeClient = YouTubeClient.ANDROID_VR_1_43_32

    /**
     * Content-aware client ordering, ported from the mobile app (upstream
     * 6.0.6 `ContentAwareFallbackStrategy`): the fallback chain is picked from
     * the track's content hints instead of one static list.
     */
    private val fallbackStrategy = com.music.innertube.strategy.ContentAwareFallbackStrategy()

    /**
     * Clients that stream without a PoToken are the only ones usable on
     * desktop (we cannot generate one). WEB_REMIX is allowed as a last
     * resort: its URLs are n-deobfuscated and validated by the downloader,
     * exactly like the mobile app does for web clients.
     */
    private val FALLBACK_CLIENTS: List<YouTubeClient> = listOf(
        YouTubeClient.VISIONOS,
        YouTubeClient.ANDROID_VR_1_65_10,
        YouTubeClient.ANDROID_VR_1_43_32,
        YouTubeClient.TVHTML5,
        YouTubeClient.TVHTML5_SIMPLY_EMBEDDED_PLAYER,
        YouTubeClient.ANDROID_CREATOR,
        YouTubeClient.WEB_CREATOR,
    )

    /**
     * Short-lived in-memory cache of resolved stream URLs, so starting the
     * same track again (or retrying it) does not re-run the whole resolution
     * chain (NewPipe signature + player client chain + URL validation) for as
     * long as the URLs are still valid. googlevideo URLs are single-use and
     * expire quickly, hence the 10-minute TTL.
     */
    private class Cached(val streams: List<ResolvedStream>, val expiresAt: Long)

    private val cache = java.util.concurrent.ConcurrentHashMap<String, Cached>()
    private const val CACHE_MAX_ENTRIES = 32

    /** Monotonic per-track generation, ported from upstream `StreamUrlCache`:
     *  bumped on every invalidation so a still-running resolution cannot store
     *  a stale result under an already-invalidated track (URL-invalidation race). */
    private val generations = java.util.concurrent.ConcurrentHashMap<String, Long>()

    private fun generation(videoId: String): Long = generations[videoId] ?: 0L

    /** Cache lifetime in ms, read from the user setting (10–60 minutes floor,
     *  issue #26); 0 (or any non-positive value) means the cache never expires.
     *  Values below the 10-minute floor (legacy 1–9) are clamped to 10. */
    private fun cacheTtlMs(): Long {
        val minutes = DesktopSettings.load().streamCacheMinutes
        return when {
            minutes <= 0 -> Long.MAX_VALUE
            minutes in 1..9 -> 10 * 60_000L
            else -> minutes.coerceAtMost(60) * 60_000L
        }
    }

    /**
     * Evicts a cached resolution so the next resolve fetches a fresh URL.
     * googlevideo URLs are single-use / expire, so a cached "forever" URL must
     * not be returned again after it has failed with a 403. Also bumps the
     * generation so in-flight resolutions are discarded instead of committed.
     */
    fun invalidate(videoId: String) {
        cache.remove(videoId)
        generations[videoId] = generation(videoId) + 1
    }

    /**
     * Returns a direct HTTP URL to an AAC audio stream, or null if it cannot be
     * resolved. Callers should invoke this from a background coroutine: the
     * NewPipe path is blocking and the player path performs network I/O.
     */
    /**
     * Resolves an ordered list of candidate stream URLs (each with the
     * User-Agent required to download it). The player tries them in order and
     * falls through on failure, so a bot-blocked URL from one source never
     * prevents playback when another source works.
     */
    suspend fun resolveAacStream(videoId: String, quality: AudioQuality = AudioQuality.AUTO): List<ResolvedStream> {
        // Serve from the in-memory cache first: the URLs are only valid for a
        // few minutes anyway, so re-resolving is pure waste when we still have
        // a working candidate.
        val now = System.currentTimeMillis()
        cache[videoId]?.let { hit ->
            if (hit.expiresAt > now && hit.streams.isNotEmpty()) return hit.streams
            cache.remove(videoId)
        }
        GuestSession.ensure()
        var resolution = resolveOnce(videoId, quality)
        // Bot detection: when no candidate URL was found at all, YouTube likely
        // flagged the guest identity — rotate it and retry once (mirrors the
        // Android BotDetectionMitigator).
        if (!resolution.anyFound) {
            GuestSession.rotate()
            resolution = resolveOnce(videoId, quality)
        }
        // Last resort: transient failures can leave us with no candidates at all;
        // retry a couple of times with a short backoff before reporting failure.
        var attempts = 0
        while (resolution.streams.isEmpty() && attempts < 2) {
            delay(750L)
            resolution = resolveOnce(videoId, quality)
            attempts++
        }
        if (resolution.streams.isNotEmpty()) {
            // Generation check (upstream StreamUrlCache semantics): commit only
            // if the track was not invalidated while we were resolving.
            val genAtStart = resolution.generation
            if (generation(videoId) == genAtStart) {
                val ttl = cacheTtlMs()
                val expiresAt = if (ttl == Long.MAX_VALUE) Long.MAX_VALUE else System.currentTimeMillis() + ttl
                cache[videoId] = Cached(resolution.streams, expiresAt)
                while (cache.size > CACHE_MAX_ENTRIES) {
                    val oldest = cache.entries.minByOrNull { it.value.expiresAt }?.key ?: break
                    cache.remove(oldest)
                }
            }
        }
        return resolution.streams
    }

    private data class Resolution(val streams: List<ResolvedStream>, val anyFound: Boolean, val generation: Long)

    private suspend fun resolveOnce(videoId: String, quality: AudioQuality): Resolution {
        // 1) NewPipe — handles the signature cipher and returns already-playable
        //    stream URLs when its extractor is not bot-blocked. These URLs are
        //    served to NewPipe's Firefox UA, so keep that UA for the download.
        //    When NewPipe succeeds, return immediately: the URL is playable as-is
        //    and the player falls through to the next candidate (or the retry
        //    path in resolveAacStream) if the download later fails — no need to
        //    wait for the whole client chain + HEAD validation on every play.
        val newPipeUrl = withContext(Dispatchers.IO) {
            runCatching {
                val urls = YouTube.getNewPipeStreamUrls(videoId)
                quality.preferredItags.firstNotNullOfOrNull { tag -> urls.firstOrNull { it.first == tag }?.second }
            }.getOrNull()
        }
        if (!newPipeUrl.isNullOrBlank()) {
            return Resolution(listOf(ResolvedStream(newPipeUrl, YouTubeClient.USER_AGENT_WEB)), anyFound = true, generation = generation(videoId))
        }

        // 2) Client chain — only reached when NewPipe was bot-blocked/failed.
        //    Collect a couple of playable AAC URLs; the download itself is the
        //    validation (the player tries candidates in order and falls through
        //    on failure), so no HEAD round-trip is spent per candidate.
        val collected = mutableListOf<ResolvedStream>()
        var signatureTimestamp: Int? = null
        var signatureFetched = false

        for (ytClient in listOf(MAIN_CLIENT) + FALLBACK_CLIENTS) {
            if (ytClient.useSignatureTimestamp && !signatureFetched) {
                signatureTimestamp = withContext(Dispatchers.IO) {
                    runCatching { NewPipeExtractor.getSignatureTimestamp(videoId).getOrNull() }.getOrNull()
                }
                signatureFetched = true
            }
            val ts = if (ytClient.useSignatureTimestamp) signatureTimestamp else null

            val response = YouTube.player(videoId, null, ytClient, ts, null).getOrNull()
                ?: continue
            if (response.playabilityStatus.status != "OK") continue

            val url = resolveFromResponse(response, quality.preferredItags, ytClient) ?: continue
            val durationMs = response.videoDetails?.lengthSeconds?.toDoubleOrNull()?.times(1000)?.toLong()
            val stream = ResolvedStream(url, ytClient.userAgent, durationMs)
            if (collected.any { it.url == url }) continue
            collected += stream
            // Two independent candidates are enough for the download fall-through;
            // stop here instead of running the whole fallback chain sequentially.
            if (collected.size >= 2) break
        }

        return Resolution(collected, anyFound = collected.isNotEmpty(), generation = generation(videoId))
    }

    /** Picks the best AAC format from a successful player response and resolves its URL. */
    private fun resolveFromResponse(response: PlayerResponse, preferredItags: List<Int>, ytClient: YouTubeClient): String? {
        val adaptive = response.streamingData?.adaptiveFormats ?: return null
        // Only AAC-LC (codec mp4a.40.2, itags 140/141): the JAAD decoder handles
        // AAC-LC, but fails on HE-AAC/SBR (mp4a.40.5, e.g. itag 139) with a
        // "FIL element overread" error. Order by the requested quality's itags.
        val aacLc = adaptive.filter { it.isAudio && it.isOriginal && it.mimeType.contains("mp4a.40.2") }
        val format = preferredItags.firstNotNullOfOrNull { tag -> aacLc.firstOrNull { it.itag == tag } }
            ?: aacLc.firstOrNull()
            ?: return null

        val raw = when {
            !format.url.isNullOrEmpty() -> format.url
            else -> format.signatureCipher ?: format.cipher
        } ?: return null

        val url = if (raw.startsWith("http")) raw else YouTubeExtractor.decryptUrl(raw)
        if (url.isNullOrBlank()) return null

        // Apply the n-parameter transform only for web clients, matching the
        // mobile app. Transforming Android/iOS/VisionOS URLs with the web
        // player's throttle deobfuscator corrupts their `n` param and googlevideo
        // then answers 403.
        return if (ytClient.useWebPoTokens) {
            runCatching { YouTubeExtractor.deobfuscateUrlNParam(url) }.getOrDefault(url)
        } else {
            url
        }
    }


}
