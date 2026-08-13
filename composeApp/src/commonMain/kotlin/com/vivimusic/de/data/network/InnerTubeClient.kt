package com.vivimusic.de.data.network

import com.vivimusic.de.domain.Album
import com.vivimusic.de.domain.Song
import io.ktor.client.HttpClient
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
 * original ViVi Music project. The response shapes parsed here match the
 * ANDROID_MUSIC InnerTube client.
 */
class InnerTubeClient(
    private val httpClient: HttpClient,
    private val json: Json = sharedJson,
) {
    private val baseUrl = "https://music.youtube.com/youtubei/v1"
    private val apiKey = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30"
    private val clientVersion = "7.16.53"

    private fun context(): JsonObject = buildJsonObject {
        put(
            "context",
            buildJsonObject {
                put(
                    "client",
                    buildJsonObject {
                        put("clientName", "ANDROID_MUSIC")
                        put("clientVersion", clientVersion)
                        put("androidSdkVersion", 30)
                        put("hl", "en")
                        put("gl", "US")
                    }
                )
            }
        )
    }

    private fun bodyWith(vararg pairs: Pair<String, JsonElement>): JsonObject = buildJsonObject {
        put("context", context())
        pairs.forEach { (key, value) -> put(key, value) }
    }

    private suspend fun call(endpoint: String, body: JsonObject): JsonObject {
        val response = httpClient.post("$baseUrl/$endpoint?key=$apiKey") {
            contentType(ContentType.Application.Json)
            setBody(body.toString())
        }
        return json.parseToJsonElement(response.bodyAsText()).jsonObject
    }

    suspend fun search(query: String): List<Song> {
        val response = call("search", bodyWith("query" to JsonPrimitive(query)))
        val sections = response.sectionListRendererContents() ?: return emptyList()
        return sections.mapNotNull { section ->
            val shelf = (section as? JsonObject)?.get("musicShelfRenderer") as? JsonObject
                ?: return@mapNotNull null
            extractListItemSongs(shelf)
        }.flatten()
    }

    suspend fun getHome(): List<Song> {
        val response = call("browse", bodyWith("browseId" to JsonPrimitive("FEmusic_home")))
        val sections = response.sectionListRendererContents() ?: return emptyList()
        return sections.mapNotNull { section ->
            val obj = section as? JsonObject ?: return@mapNotNull null
            val shelf = obj["musicCarouselShelfRenderer"] as? JsonObject
                ?: obj["musicShelfRenderer"] as? JsonObject
                ?: return@mapNotNull null
            val items = shelf["contents"] as? JsonArray ?: return@mapNotNull emptyList()
            items.mapNotNull { item ->
                ((item as? JsonObject)?.get("musicTwoRowItemRenderer") as? JsonObject)?.toSongFromTwoRow()
            }
        }.flatten().distinctBy { it.id }
    }

    suspend fun getAlbumOrPlaylist(browseId: String): Album {
        val response = call("browse", bodyWith("browseId" to JsonPrimitive(browseId)))
        val sections = response.sectionListRendererContents() ?: emptyList()
        val songs = sections.mapNotNull { section ->
            val obj = section as? JsonObject ?: return@mapNotNull null
            val shelf = obj["musicPlaylistShelfRenderer"] as? JsonObject
                ?: obj["musicShelfRenderer"] as? JsonObject
                ?: return@mapNotNull null
            extractListItemSongs(shelf)
        }.flatten()

        val header = (response["header"] as? JsonObject)?.get("musicDetailHeaderRenderer") as? JsonObject
        val title = header?.get("title")?.let { (it as? JsonObject)?.firstText() } ?: ""
        val artist = header?.get("subtitle")?.let { (it as? JsonObject)?.joinedText() } ?: ""
        val thumbnailUrl = header?.thumbnailUrl()

        return Album(
            id = browseId,
            title = title,
            artist = artist,
            thumbnailUrl = thumbnailUrl,
            songs = songs,
        )
    }

    suspend fun getSong(videoId: String): Song? {
        val response = call("player", bodyWith("videoId" to JsonPrimitive(videoId)))
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
        return formats
            .mapNotNull { it as? JsonObject }
            .filter { it.str("mimeType")?.startsWith("audio/") == true }
            .maxByOrNull { it.str("bitrate")?.toLongOrNull() ?: 0L }
            ?.str("url")
    }

    // ----- response traversal helpers -----

    private fun JsonObject.sectionListRendererContents(): JsonArray? =
        (this["contents"] as? JsonObject)
            ?.get("singleColumnBrowseResultsRenderer")?.let { singleColumnOrTabs(it) }
            ?: (this["contents"] as? JsonObject)?.get("tabbedSearchResultsRenderer")?.let { singleColumnOrTabs(it) }

    private fun singleColumnOrTabs(renderer: JsonElement): JsonArray? {
        val rendererObj = renderer as? JsonObject ?: return null
        val tabs = rendererObj["tabs"] as? JsonArray ?: return null
        val tab = tabs.firstOrNull() as? JsonObject ?: return null
        val content = (tab["tabRenderer"] as? JsonObject)?.get("content") as? JsonObject ?: return null
        return (content["sectionListRenderer"] as? JsonObject)?.get("contents") as? JsonArray
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

    private fun JsonObject.textRuns(): List<String> =
        ((this["text"] as? JsonObject)?.get("runs") as? JsonArray)
            ?.mapNotNull { (it as? JsonObject)?.str("text") }
            ?: emptyList()

    private fun JsonObject.firstText(): String? = textRuns().firstOrNull()

    private fun JsonObject.joinedText(): String = textRuns().joinToString(separator = "")

    private fun JsonObject.str(key: String): String? =
        (this[key] as? JsonPrimitive)?.let { if (it.isString) it.content else null }
}
