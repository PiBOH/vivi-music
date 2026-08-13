package com.vivimusic.de.data.lyrics

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.math.abs

/**
 * A single timed lyric line, parsed from an LRC file.
 */
data class LyricsLine(
    val timeMs: Long,
    val text: String,
)

@Serializable
data class LrcLibTrack(
    val id: Int = 0,
    val trackName: String = "",
    val artistName: String = "",
    val duration: Double = 0.0,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null,
)

/**
 * Minimal LrcLib (https://lrclib.net) client used to fetch lyrics for a track.
 *
 * This is a lightweight port of ViVi Music's `LrcLib` provider: it searches by
 * cleaned title/artist, prefers synced (LRC) lyrics and parses them into timed
 * [LyricsLine]s. Plain (unsynced) lyrics are returned with time 0 so the UI can
 * still show them statically.
 */
class LyricsClient(
    private val httpClient: HttpClient,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    },
) {
    private val lrcTagRegex = Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")

    suspend fun getLyrics(title: String, artist: String, durationSeconds: Long?): List<LyricsLine> {
        if (title.isBlank()) return emptyList()
        val cleanedTitle = cleanTitle(title)
        val cleanedArtist = cleanArtist(artist)
        val tracks = search(cleanedTitle, cleanedArtist)
            .filter { it.syncedLyrics != null || it.plainLyrics != null }
        val track = pickBest(tracks, durationSeconds) ?: return emptyList()
        val raw = track.syncedLyrics ?: track.plainLyrics ?: return emptyList()
        return if (track.syncedLyrics != null) parseLrc(raw) else plainToLines(raw)
    }

    private suspend fun search(trackName: String, artistName: String): List<LrcLibTrack> = try {
        val text = httpClient.get("https://lrclib.net/api/search") {
            parameter("track_name", trackName)
            parameter("artist_name", artistName)
        }.bodyAsText()
        json.decodeFromString(ListSerializer(LrcLibTrack.serializer()), text)
    } catch (t: Throwable) {
        emptyList()
    }

    private fun pickBest(tracks: List<LrcLibTrack>, durationSeconds: Long?): LrcLibTrack? {
        if (tracks.isEmpty()) return null
        val synced = tracks.filter { it.syncedLyrics != null }
        val pool = synced.ifEmpty { tracks }
        if (durationSeconds != null && durationSeconds > 0) {
            val closest = pool.minByOrNull { abs(it.duration - durationSeconds) }
            if (closest != null && abs(closest.duration - durationSeconds) <= 10) return closest
        }
        return pool.firstOrNull()
    }

    private fun parseLrc(lrc: String): List<LyricsLine> {
        val lines = mutableListOf<LyricsLine>()
        for (raw in lrc.lines()) {
            val matches = lrcTagRegex.findAll(raw).toList()
            if (matches.isEmpty()) continue
            val text = raw.substringAfterLast(']').trim()
            if (text.isBlank()) continue
            for (match in matches) {
                val minutes = match.groupValues[1].toLongOrNull() ?: continue
                val seconds = match.groupValues[2].toLongOrNull() ?: continue
                val fraction = match.groupValues[3]
                    .takeIf { it.isNotEmpty() }
                    ?.padEnd(3, '0')
                    ?.take(3)
                    ?.toLongOrNull() ?: 0L
                lines.add(LyricsLine(minutes * 60_000 + seconds * 1_000 + fraction, text))
            }
        }
        return lines.sortedBy { it.timeMs }
    }

    private fun plainToLines(plain: String): List<LyricsLine> =
        plain.lines().mapNotNull { line ->
            val text = line.trim()
            if (text.isBlank()) null else LyricsLine(0L, text)
        }

    private val titleCleanupPatterns = listOf(
        Regex("""\s*\(.*?(official|video|audio|lyrics|lyric|visualizer|hd|hq|4k|remaster|remix|live|acoustic|version|edit|extended|radio|clean|explicit).*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*\[.*?(official|video|audio|lyrics|lyric|visualizer|hd|hq|4k|remaster|remix|live|acoustic|version|edit|extended|radio|clean|explicit).*?]""", RegexOption.IGNORE_CASE),
        Regex("""\s*\(feat\..*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*\(ft\..*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*feat\..*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*ft\..*$""", RegexOption.IGNORE_CASE),
    )

    private val artistSeparators = listOf(
        " & ", " and ", ", ", " x ", " X ", " feat. ", " feat ", " ft. ", " ft ", " featuring ", " with ",
    )

    private fun cleanTitle(title: String): String {
        var cleaned = title.trim()
        for (pattern in titleCleanupPatterns) {
            cleaned = cleaned.replace(pattern, "")
        }
        return cleaned.trim()
    }

    private fun cleanArtist(artist: String): String {
        var cleaned = artist.trim()
        for (separator in artistSeparators) {
            if (cleaned.contains(separator, ignoreCase = true)) {
                cleaned = cleaned.split(separator, ignoreCase = true, limit = 2)[0]
                break
            }
        }
        return cleaned.trim()
    }
}
