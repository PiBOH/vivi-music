package com.music.kugou

import com.music.kugou.models.DownloadLyricsResponse
import com.music.kugou.models.Keyword
import com.music.kugou.models.SearchLyricsResponse
import com.music.kugou.models.SearchSongResponse
import com.music.lyrics.TextFolding
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.encodeURLParameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.json.Json
import java.lang.Integer.min
import kotlin.math.abs

@OptIn(ExperimentalSerializationApi::class, ExperimentalEncodingApi::class)
private val client = HttpClient {
    expectSuccess = true

    install(ContentNegotiation) {
        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = true
        }
        json(json)
        json(json, ContentType.Text.Html)
        json(json, ContentType.Text.Plain)
    }

    install(ContentEncoding) {
        gzip()
        deflate()
    }
}

private const val PAGE_SIZE = 8
private const val HEAD_CUT_LIMIT = 30

/**
 * KuGou Lyrics Library
 * Modified from [ViMusic](https://github.com/vfsfitvnm/ViMusic)
 */
object KuGou {
    var useTraditionalChinese: Boolean = false

    suspend fun getLyrics(title: String, artist: String, duration: Int, album: String? = null): Result<String> =
        runCatching {
            val keyword = generateKeyword(title, artist, album)
            getLyricsCandidate(keyword, duration)?.let { candidate ->
                Base64.Default.decode(downloadLyrics(candidate.id, candidate.accesskey).content).decodeToString()
                    .normalize()
            } ?: throw IllegalStateException("No matching lyrics candidate for '${keyword.title} - ${keyword.artist}'")
        }

    suspend fun getAllPossibleLyricsOptions(
        title: String, artist: String, duration: Int, album: String? = null, callback: (String) -> Unit
    ) {
        val keyword = generateKeyword(title, artist, album)
        searchSongs(keyword).data.info.forEach {
            if (durationMatches(it.duration, duration) && matchesRequest(it.songname, it.singername, keyword)) {
                searchLyricsByHash(it.hash).candidates.firstOrNull()?.let { candidate ->
                    Base64.Default.decode(downloadLyrics(candidate.id, candidate.accesskey).content).decodeToString()
                        .normalize().let(callback)
                }
            }
        }
        searchLyricsByKeyword(keyword, duration).candidates.forEach { candidate ->
            if (durationMatches(candidate.duration.toInt(), duration) &&
                matchesRequest(candidate.song, candidate.singer, keyword)
            ) {
                Base64.Default.decode(downloadLyrics(candidate.id, candidate.accesskey).content).decodeToString()
                    .normalize().let(callback)
            }
        }
    }

    suspend fun getLyricsCandidate(
        keyword: Keyword, duration: Int
    ): SearchLyricsResponse.Candidate? {
        searchSongs(keyword).data.info.forEach { song ->
            // The duration filter is NOT an identity: KuGou answers a search
            // with fuzzy results, so "that other song by the same artist that
            // happens to be ~8 s long alike" used to be accepted and its
            // lyrics shown for a completely different track. Both the name of
            // the song and the artist must agree with the request.
            if (durationMatches(song.duration, duration) &&
                matchesRequest(song.songname, song.singername, keyword)
            ) {
                val candidate = searchLyricsByHash(song.hash).candidates.firstOrNull()
                if (candidate != null) return candidate
            }
        }
        return searchLyricsByKeyword(keyword, duration).candidates
            .firstOrNull { candidate ->
                durationMatches(candidate.duration.toInt(), duration) &&
                    matchesRequest(candidate.song, candidate.singer, keyword)
            }
    }

    suspend fun searchSongs(keyword: Keyword) =
        client.get("https://mobileservice.kugou.com/api/v3/search/song") {
            parameter("version", 9108)
            parameter("plat", 0)
            parameter("pagesize", PAGE_SIZE)
            parameter("showtype", 0)
            val searchQuery = buildString {
                append(keyword.title)
                append(" - ")
                append(keyword.artist)
                if (!keyword.album.isNullOrBlank()) {
                    append(" ")
                    append(keyword.album)
                }
            }
            url.encodedParameters.append(
                "keyword",
                searchQuery.encodeURLParameter(spaceToPlus = false)
            )
        }.body<SearchSongResponse>()

    private suspend fun searchLyricsByKeyword(keyword: Keyword, duration: Int) =
        client.get("https://lyrics.kugou.com/search") {
            parameter("ver", 1)
            parameter("man", "yes")
            parameter("client", "pc")
            parameter(
                "duration", duration.takeIf { it != -1 }?.times(1000)
            ) // if duration == -1, we don't care duration
            val searchQuery = buildString {
                append(keyword.title)
                append(" - ")
                append(keyword.artist)
                if (!keyword.album.isNullOrBlank()) {
                    append(" ")
                    append(keyword.album)
                }
            }
            url.encodedParameters.append(
                "keyword",
                searchQuery.encodeURLParameter(spaceToPlus = false)
            )
        }.body<SearchLyricsResponse>()

    private suspend fun searchLyricsByHash(hash: String) =
        client.get("https://lyrics.kugou.com/search") {
            parameter("ver", 1)
            parameter("man", "yes")
            parameter("client", "pc")
            parameter("hash", hash)
        }.body<SearchLyricsResponse>()

    private suspend fun downloadLyrics(id: Long, accessKey: String) =
        client.get("https://lyrics.kugou.com/download") {
            parameter("fmt", "lrc")
            parameter("charset", "utf8")
            parameter("client", "pc")
            parameter("ver", 1)
            parameter("id", id)
            parameter("accesskey", accessKey)
        }.body<DownloadLyricsResponse>()

    private fun normalizeTitle(title: String) =
        title.replace("\\(.*\\)".toRegex(), "").replace("（.*）".toRegex(), "")
            .replace("「.*」".toRegex(), "").replace("『.*』".toRegex(), "")
            .replace("<.*>".toRegex(), "").replace("《.*》".toRegex(), "")
            .replace("〈.*〉".toRegex(), "").replace("＜.*＞".toRegex(), "")

    private fun normalizeArtist(artist: String) =
        artist.replace(", ", "、").replace(" & ", "、").replace(".", "").replace("和", "、")
            .replace("\\(.*\\)".toRegex(), "").replace("（.*）".toRegex(), "")

    fun generateKeyword(title: String, artist: String, album: String? = null) =
        Keyword(normalizeTitle(title), normalizeArtist(artist), album)

    /**
     * True when a candidate's runtime is compatible with the requested one.
     * Both values are seconds; <= 0 means unknown and matches anything. Values
     * in the tens of thousands are milliseconds (some endpoints answer that
     * way) and are converted first.
     */
    private fun durationMatches(candidate: Int, requested: Int): Boolean {
        if (requested <= 0 || candidate <= 0) return true
        val candidateSec = if (candidate > 10_000) candidate / 1000 else candidate
        return abs(candidateSec - requested) <= DURATION_TOLERANCE
    }

    /** Words that carry no identity in a track or artist name. */
    private val NOISE_TOKENS = setOf(
        "feat", "ft", "featuring", "the", "a", "an", "and", "with", "vs",
        "official", "lyric", "lyrics", "audio", "video", "hd", "hq", "mv",
        "remaster", "remastered", "remastering", "version", "edit", "mix",
        "prod", "produced", "by", "from", "deluxe", "album", "single",
    )

    private val NON_ALPHANUMERIC = Regex("[^\\p{L}\\p{Nd}]+")

    /**
     * Tokens of [text], folded to plain Unicode first: a stylized title
     * (`ＭＩＧＵＥＬ 𝑷𝒉𝒐𝒏𝒌`) and a catalogue entry (`Miguel Phonk`) must produce
     * the same tokens, otherwise the title/artist check below rejects the
     * correct candidate and those tracks never get lyrics at all.
     */
    private fun tokens(text: String?): Set<String> =
        TextFolding.fold(text.orEmpty())
            .lowercase()
            .split(NON_ALPHANUMERIC)
            .asSequence()
            .filter { it.length > 1 || it.any(Char::isDigit) }
            .filterNot { it in NOISE_TOKENS }
            .toSet()

    /** Portion of the shorter token set that also appears in the other one. */
    private fun overlap(a: Set<String>, b: Set<String>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val shared = a.count { it in b }
        return shared.toDouble() / min(a.size, b.size)
    }

    /**
     * True when [candidateSong]/[candidateSinger] really describe [keyword].
     *
     * KuGou's fuzzy search makes the first result often a different track by
     * the same artist, and the endpoint sometimes swaps the two fields, so
     * both readings of the pair are accepted. When the request carries no
     * usable title there is nothing to check against: the match is allowed.
     */
    private fun matchesRequest(candidateSong: String?, candidateSinger: String?, keyword: Keyword): Boolean {
        val wantedTitle = tokens(keyword.title)
        if (wantedTitle.isEmpty()) return true
        val wantedArtist = tokens(keyword.artist)
        val gotSong = tokens(candidateSong)
        val gotSinger = tokens(candidateSinger)
        if (gotSong.isEmpty() && gotSinger.isEmpty()) return false

        fun pairMatches(title: Set<String>, artist: Set<String>): Boolean {
            if (overlap(wantedTitle, title) < TEXT_MATCH_THRESHOLD) return false
            if (wantedArtist.isEmpty() || artist.isEmpty()) return true
            return overlap(wantedArtist, artist) >= TEXT_MATCH_THRESHOLD
        }

        return pairMatches(gotSong, gotSinger) || pairMatches(gotSinger, gotSong)
    }

    private fun String.normalize(): String =
        lines().filter { line -> line.matches(ACCEPTED_REGEX) }
            .let { lines ->
                // Remove useless information such as singer, writer, composer, guitar, etc.
                var headCutLine = 0
                for (i in min(HEAD_CUT_LIMIT, lines.lastIndex) downTo 0) {
                    if (lines[i].matches(BANNED_REGEX)) {
                        headCutLine = i + 1
                        break
                    }
                }
                val filteredLines = lines.drop(headCutLine)

                var tailCutLine = 0
                for (i in min(lines.size - HEAD_CUT_LIMIT, lines.lastIndex) downTo 0) {
                    if (lines[lines.lastIndex - i].matches(BANNED_REGEX)) {
                        tailCutLine = i + 1
                        break
                    }
                }
                val finalLines = filteredLines.dropLast(tailCutLine)

                return@let finalLines.joinToString("\n")
            }

    @Suppress("RegExpRedundantEscape")
    private val ACCEPTED_REGEX = "\\[(\\d\\d):(\\d\\d)\\.(\\d{2,3})\\].*".toRegex()
    private val BANNED_REGEX = ".+].+[:：].+".toRegex()

    private const val DURATION_TOLERANCE = 8

    /** Share of the name tokens that must agree for a candidate to be trusted. */
    private const val TEXT_MATCH_THRESHOLD = 0.6
}
