package com.vivimusic.de.data.network

import com.vivimusic.de.domain.Album
import com.vivimusic.de.domain.Artist
import com.vivimusic.de.domain.HomeSection
import com.vivimusic.de.domain.Song
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * A focused InnerTube (YouTube Music web API) client implemented on top of
 * Ktor and kotlinx.serialization. It covers the main read flows used by the
 * application: search, home feed, album/playlist browsing and track stream
 * resolution.
 *
 * Note: this is a working subset of the full inner-tube module available in the
 * original ViVi Music project. Read endpoints (search, browse, suggestions)
 * use the WEB_REMIX client, which returns the classic renderer format; the
 * `player` endpoint uses ANDROID_MUSIC for stream URL resolution.
 */
class InnerTubeClient(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val json: Json = sharedJson,
) {
    private val baseUrl = "https://music.youtube.com/youtubei/v1"

    /**
     * InnerTube clients. WEB_REMIX is used for read endpoints (search, browse,
     * suggestions) because it returns the classic renderer format.
     * ANDROID_MUSIC is kept for the `player` endpoint (direct stream URL
     * resolution).
     */
    private enum class Client(
        val clientName: String,
        val version: String,
        val id: String,
        val userAgent: String,
    ) {
        WEB_REMIX(
            clientName = "WEB_REMIX",
            version = "1.20260213.01.00",
            id = "67",
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0",
        ),
        ANDROID_MUSIC(
            clientName = "ANDROID_MUSIC",
            version = "6.37.50",
            id = "73",
            userAgent = "com.google.android.apps.youtube.music/6.37.50 (Linux; U; Android 14) gzip",
        ),
    }

    private fun context(client: Client): JsonObject = buildJsonObject {
        put(
            "context",
            buildJsonObject {
                put(
                    "client",
                    buildJsonObject {
                        put("clientName", client.clientName)
                        put("clientVersion", client.version)
                        put("hl", "en")
                        put("gl", "US")
                    }
                )
            }
        )
    }

    private fun bodyWith(client: Client, vararg pairs: Pair<String, JsonElement>): JsonObject = buildJsonObject {
        put("context", context(client))
        pairs.forEach { (key, value) -> put(key, value) }
    }

    private suspend fun call(endpoint: String, body: JsonObject, client: Client = Client.WEB_REMIX): JsonObject {
        val response = httpClient.post("$baseUrl/$endpoint?key=$apiKey") {
            contentType(ContentType.Application.Json)
            header("X-Goog-Api-Format-Version", "1")
            header("X-YouTube-Client-Name", client.id)
            header("X-YouTube-Client-Version", client.version)
            header("X-Origin", "https://music.youtube.com")
            header("Referer", "https://music.youtube.com/")
            header("User-Agent", client.userAgent)
            setBody(body.toString())
        }
        return json.parseToJsonElement(response.bodyAsText()).jsonObject
    }

    suspend fun search(query: String): List<Song> {
        val response = call("search", bodyWith(Client.WEB_REMIX, "query" to JsonPrimitive(query)))
        val sections = response.sectionListRendererContents() ?: return emptyList()
        return sections.mapNotNull { section ->
            val obj = section as? JsonObject ?: return@mapNotNull null
            val shelf = obj["musicShelfRenderer"] as? JsonObject
                ?: obj["itemSectionRenderer"] as? JsonObject
                ?: return@mapNotNull null
            extractListItemSongs(shelf)
        }.flatten()
    }

    /**
     * Live autocomplete suggestions from the `music/get_search_suggestions`
     * endpoint, returned as plain query strings.
     */
    suspend fun getSearchSuggestions(query: String): List<String> {
        val response = call("music/get_search_suggestions", bodyWith(Client.WEB_REMIX, "input" to JsonPrimitive(query)))
        val contents = response["contents"] as? JsonArray ?: return emptyList()
        val sectionRenderer = (contents.firstOrNull() as? JsonObject)
            ?.get("searchSuggestionsSectionRenderer") as? JsonObject
            ?: return emptyList()
        val items = sectionRenderer["contents"] as? JsonArray ?: return emptyList()
        return items.mapNotNull { item ->
            val renderer = (item as? JsonObject)?.get("searchSuggestionRenderer") as? JsonObject
                ?: return@mapNotNull null
            val suggestion = renderer["suggestion"] as? JsonObject ?: return@mapNotNull null
            val text = ((suggestion["runs"] as? JsonArray)
                ?.mapNotNull { (it as? JsonObject)?.str("text") }
                ?: emptyList()).joinToString(separator = "")
            if (text.isBlank()) null else text
        }
    }

    suspend fun getHome(): List<HomeSection> {
        val response = call("browse", bodyWith(Client.WEB_REMIX, "browseId" to JsonPrimitive("FEmusic_home")))
        val sections = response.sectionListRendererContents() ?: return emptyList()
        return sections.mapNotNull { section ->
            val obj = section as? JsonObject ?: return@mapNotNull null
            val shelf = obj["musicCarouselShelfRenderer"] as? JsonObject
                ?: obj["musicShelfRenderer"] as? JsonObject
                ?: return@mapNotNull null
            val songs = (shelf["contents"] as? JsonArray).orEmpty()
                .mapNotNull { item ->
                    val renderer = item as? JsonObject ?: return@mapNotNull null
                    (renderer["musicTwoRowItemRenderer"] as? JsonObject)?.toSongFromTwoRow()
                        ?: (renderer["musicResponsiveListItemRenderer"] as? JsonObject)?.toSongFromListItem()
                }
                .distinctBy { it.id }
            if (songs.isEmpty()) return@mapNotNull null
            HomeSection(title = shelf.shelfTitle().orEmpty(), songs = songs)
        }
    }

    suspend fun getAlbumOrPlaylist(browseId: String): Album {
        val response = call("browse", bodyWith(Client.WEB_REMIX, "browseId" to JsonPrimitive(browseId)))
        // Albums/playlists use twoColumnBrowseResultsRenderer: the header sits
        // in the first tab's section list, the songs in secondaryContents.
        val twoCol = (response["contents"] as? JsonObject)?.get("twoColumnBrowseResultsRenderer") as? JsonObject
        val header = twoCol?.tabSections()
            ?.mapNotNull { (it as? JsonObject)?.get("musicResponsiveHeaderRenderer") as? JsonObject }
            ?.firstOrNull()
        val songs = twoCol?.secondarySongs().orEmpty()

        // Fallback for older renderer shapes (musicDetailHeaderRenderer +
        // musicPlaylistShelfRenderer in a single-column browse).
        val legacySongs = if (songs.isEmpty()) {
            response.sectionListRendererContents()
                .orEmpty()
                .mapNotNull { section ->
                    val obj = section as? JsonObject ?: return@mapNotNull null
                    val shelf = obj["musicPlaylistShelfRenderer"] as? JsonObject
                        ?: obj["musicShelfRenderer"] as? JsonObject
                        ?: return@mapNotNull null
                    extractListItemSongs(shelf)
                }
                .flatten()
        } else {
            emptyList()
        }

        val microformat = (response["microformat"] as? JsonObject)?.get("microformatDataRenderer") as? JsonObject
        val title = header?.get("title")?.let { (it as? JsonObject)?.firstText() }
            ?: microformat?.str("title")
            ?: ""
        val artist = microformat?.str("description")
            ?.substringAfter("\u2022")
            ?.trim()
            ?.takeUnless { it == "Album" || it == "Playlist" }
            .orEmpty()
        val year = header?.get("subtitle")?.let { (it as? JsonObject)?.textRuns() }
            ?.firstOrNull { it.length == 4 && it.all(Char::isDigit) }
            ?: microformat?.str("year")
        val thumbnailUrl = header?.thumbnailUrl() ?: microformat?.microformatThumbnailUrl()

        return Album(
            id = browseId,
            title = title,
            artist = artist,
            year = year,
            description = header?.get("description")?.let { (it as? JsonObject)?.firstText() },
            thumbnailUrl = thumbnailUrl,
            songs = songs.ifEmpty { legacySongs },
        )
    }

    /**
     * Loads an artist page: the immersive header (name, thumbnail) plus the
     * "Top songs" shelf and the album carousels.
     */
    suspend fun getArtist(browseId: String): Artist {
        val response = call("browse", bodyWith(Client.WEB_REMIX, "browseId" to JsonPrimitive(browseId)))
        val header = (response["header"] as? JsonObject)?.get("musicImmersiveHeaderRenderer") as? JsonObject
        val name = header?.get("title")?.let { (it as? JsonObject)?.firstText() } ?: ""
        val description = header?.get("description")?.let { (it as? JsonObject)?.firstText() }
        val thumbnailUrl = header?.thumbnailUrl()

        val sections = response.sectionListRendererContents().orEmpty()
            .mapNotNull { it as? JsonObject }

        val songs = sections
            .mapNotNull { it["musicShelfRenderer"] as? JsonObject }
            .flatMap { extractListItemSongs(it) }

        val albums = sections
            .mapNotNull { it["musicCarouselShelfRenderer"] as? JsonObject }
            .flatMap { shelf ->
                (shelf["contents"] as? JsonArray).orEmpty().mapNotNull { item ->
                    ((item as? JsonObject)?.get("musicTwoRowItemRenderer") as? JsonObject)?.toAlbumFromTwoRow()
                }
            }
            .distinctBy { it.id }

        return Artist(
            id = browseId,
            name = name,
            thumbnailUrl = thumbnailUrl,
            description = description,
            songs = songs,
            albums = albums,
        )
    }

    suspend fun getSong(videoId: String): Song? {
        val response = call("player", bodyWith(Client.ANDROID_MUSIC, "videoId" to JsonPrimitive(videoId)), client = Client.ANDROID_MUSIC)
        val details = response["videoDetails"] as? JsonObject ?: return null
        val title = details.str("title") ?: return null
        val artist = details.str("author") ?: ""
        val lengthSeconds = details.str("lengthSeconds")?.toLongOrNull()
        val thumbnails = (details["thumbnail"] as? JsonObject)?.get("thumbnails") as? JsonArray
        val thumbnailUrl = (thumbnails?.lastOrNull() as? JsonObject)?.str("url")
        return Song(
            id = videoId,
            title = title,
            artist = artist,
            thumbnailUrl = thumbnailUrl,
            durationMs = lengthSeconds?.times(1_000L),
            streamUrl = extractAudioStreamUrl(response),
        )
    }

    private fun extractAudioStreamUrl(response: JsonObject): String? {
        val streaming = response["streamingData"] as? JsonObject ?: return null
        val formats = streaming["adaptiveFormats"] as? JsonArray ?: return null
        val audioFormats = formats
            .mapNotNull { it as? JsonObject }
            .filter { it.str("mimeType")?.startsWith("audio/") == true }
        // Prefer Opus (audio/webm), which is decoded by the bundled native
        // decoder; AAC (audio/mp4) would need an extra codec module not
        // included in the core. Fall back to the highest bitrate audio format.
        val opus = audioFormats.firstOrNull { it.str("mimeType")?.startsWith("audio/webm") == true }
        val chosen = opus ?: audioFormats.maxByOrNull { it.str("bitrate")?.toLongOrNull() ?: 0L }
        return chosen?.str("url")
    }

    // ----- response traversal helpers -----

    private fun JsonObject.sectionListRendererContents(): JsonArray? =
        (this["contents"] as? JsonObject)
            ?.get("singleColumnBrowseResultsRenderer")?.let { singleColumnOrTabs(it) }
            ?: (this["contents"] as? JsonObject)?.get("tabbedSearchResultsRenderer")?.let { singleColumnOrTabs(it) }
            ?: (this["contents"] as? JsonObject)?.get("twoColumnBrowseResultsRenderer")?.let { twoColumnTabs(it) }

    private fun singleColumnOrTabs(renderer: JsonElement): JsonArray? {
        val rendererObj = renderer as? JsonObject ?: return null
        val tabs = rendererObj["tabs"] as? JsonArray ?: return null
        val tab = tabs.firstOrNull() as? JsonObject ?: return null
        val content = (tab["tabRenderer"] as? JsonObject)?.get("content") as? JsonObject ?: return null
        return (content["sectionListRenderer"] as? JsonObject)?.get("contents") as? JsonArray
    }

    private fun twoColumnTabs(renderer: JsonElement): JsonArray? {
        val rendererObj = renderer as? JsonObject ?: return null
        val tabs = rendererObj["tabs"] as? JsonArray ?: return null
        val tab = tabs.firstOrNull() as? JsonObject ?: return null
        val content = (tab["tabRenderer"] as? JsonObject)?.get("content") as? JsonObject ?: return null
        return (content["sectionListRenderer"] as? JsonObject)?.get("contents") as? JsonArray
    }

    /** Sections from a twoColumnBrowseResultsRenderer's first tab. */
    private fun JsonObject.tabSections(): JsonArray? {
        val tabs = this["tabs"] as? JsonArray ?: return null
        val tab = tabs.firstOrNull() as? JsonObject ?: return null
        val content = (tab["tabRenderer"] as? JsonObject)?.get("content") as? JsonObject ?: return null
        return (content["sectionListRenderer"] as? JsonObject)?.get("contents") as? JsonArray
    }

    /** Songs from a twoColumnBrowseResultsRenderer's secondaryContents. */
    private fun JsonObject.secondarySongs(): List<Song> {
        val secondary = (this["secondaryContents"] as? JsonObject)?.get("sectionListRenderer") as? JsonObject
            ?: return emptyList()
        val sections = secondary["contents"] as? JsonArray ?: return emptyList()
        return sections.mapNotNull { section ->
            val obj = section as? JsonObject ?: return@mapNotNull null
            val shelf = obj["musicShelfRenderer"] as? JsonObject
                ?: obj["musicPlaylistShelfRenderer"] as? JsonObject
                ?: return@mapNotNull null
            extractListItemSongs(shelf)
        }.flatten()
    }

    private fun extractListItemSongs(shelf: JsonObject): List<Song> {
        val items = shelf["contents"] as? JsonArray ?: return emptyList()
        return items.mapNotNull { item ->
            ((item as? JsonObject)?.get("musicResponsiveListItemRenderer") as? JsonObject)?.toSongFromListItem()
        }
    }

    private fun JsonObject.toSongFromListItem(): Song? {
        val id = videoId() ?: return null
        val flexColumns = this["flexColumns"] as? JsonArray ?: return null
        val title = flexColumns.getOrNull(0)
            ?.let { (it as? JsonObject)?.get("musicResponsiveListItemFlexColumnRenderer") as? JsonObject }
            ?.firstText() ?: ""
        val subtitle = flexColumns.getOrNull(1)
            ?.let { (it as? JsonObject)?.get("musicResponsiveListItemFlexColumnRenderer") as? JsonObject }
            ?.joinedText() ?: ""
        return Song(id = id, title = title, artist = subtitle, thumbnailUrl = thumbnailUrl())
    }

    private fun JsonObject.toSongFromTwoRow(): Song? {
        val id = watchVideoId() ?: return null
        val title = (this["title"] as? JsonObject)?.firstText() ?: ""
        val subtitle = (this["subtitle"] as? JsonObject)?.joinedText() ?: ""
        return Song(id = id, title = title, artist = subtitle, thumbnailUrl = thumbnailUrl())
    }

    private fun JsonObject.toAlbumFromTwoRow(): Album? {
        val id = browseId() ?: return null
        val title = (this["title"] as? JsonObject)?.firstText() ?: ""
        val year = (this["subtitle"] as? JsonObject)?.firstText()
        return Album(id = id, title = title, year = year, thumbnailUrl = thumbnailUrl())
    }

    private fun JsonObject.browseId(): String? =
        ((this["navigationEndpoint"] as? JsonObject)?.get("browseEndpoint") as? JsonObject)?.str("browseId")

    private fun JsonObject.videoId(): String? {
        str("videoId")?.let { return it }
        val overlay = this["overlay"] as? JsonObject ?: return null
        val content = (overlay["musicItemThumbnailOverlayRenderer"] as? JsonObject)?.get("content") as? JsonObject
        val playButton = content?.get("musicPlayButtonRenderer") as? JsonObject
        val watch = (playButton?.get("playNavigationEndpoint") as? JsonObject)?.get("watchEndpoint") as? JsonObject
        return watch?.str("videoId")
    }

    private fun JsonObject.watchVideoId(): String? =
        ((this["navigationEndpoint"] as? JsonObject)?.get("watchEndpoint") as? JsonObject)?.str("videoId")

    private fun JsonObject.thumbnailUrl(): String? {
        val renderer = (this["thumbnail"] as? JsonObject) ?: (this["thumbnailRenderer"] as? JsonObject) ?: return null
        val musicThumb = (renderer["musicThumbnailRenderer"] as? JsonObject) ?: renderer
        val thumbnails = ((musicThumb["thumbnail"] as? JsonObject)?.get("thumbnails") as? JsonArray) ?: return null
        return (thumbnails.lastOrNull() as? JsonObject)?.str("url")
    }

    /** Microformat thumbnails are a bare `thumbnails` array (no wrapper). */
    private fun JsonObject.microformatThumbnailUrl(): String? {
        val thumbnails = (this["thumbnail"] as? JsonObject)?.get("thumbnails") as? JsonArray ?: return null
        return (thumbnails.lastOrNull() as? JsonObject)?.str("url")
    }

    private fun JsonObject.textRuns(): List<String> =
        ((this["text"] as? JsonObject)?.get("runs") as? JsonArray)
            ?.mapNotNull { (it as? JsonObject)?.str("text") }
            ?: emptyList()

    private fun JsonObject.firstText(): String? = textRuns().firstOrNull()

    private fun JsonObject.joinedText(): String = textRuns().joinToString(separator = "")

    private fun JsonObject.str(key: String): String? =
        (this[key] as? JsonPrimitive)?.let { if (it.isString) it.content else null }

    /** Extracts the title of a home shelf from its header or title runs. */
    private fun JsonObject.shelfTitle(): String? {
        val header = this["header"] as? JsonObject
        if (header != null) {
            val basic = header["musicCarouselShelfBasicHeaderRenderer"] as? JsonObject
            return (basic?.get("title") as? JsonObject)?.firstText()
        }
        return (this["title"] as? JsonObject)?.firstText()
    }
}
