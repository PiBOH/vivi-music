package com.vivimusic.de.domain

import kotlinx.serialization.Serializable

/**
 * A single track. [id] is the YouTube Music video id used across the app,
 * the local database and the remote sync layer.
 */
@Serializable
data class Song(
    val id: String,
    val title: String,
    val artist: String = "",
    val album: String = "",
    val thumbnailUrl: String? = null,
    val durationMs: Long? = null,
    val streamUrl: String? = null,
)

@Serializable
data class Album(
    val id: String,
    val title: String,
    val artist: String = "",
    val year: String? = null,
    val description: String? = null,
    val thumbnailUrl: String? = null,
    val songs: List<Song> = emptyList(),
)

@Serializable
data class Artist(
    val id: String,
    val name: String,
    val thumbnailUrl: String? = null,
    val description: String? = null,
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
)

@Serializable
data class Playlist(
    val id: String,
    val name: String,
    val description: String? = null,
    val thumbnailUrl: String? = null,
    val createdAtEpochMs: Long = 0L,
    val updatedAtEpochMs: Long = 0L,
    val songs: List<Song> = emptyList(),
)

/**
 * A titled group of songs shown on the Home screen (e.g. "Quick Picks",
 * "Keep listening"), parsed from a single InnerTube home shelf.
 */
@Serializable
data class HomeSection(
    val title: String,
    val songs: List<Song>,
)

/**
 * One mood or genre tile shown on the Explore screen and on the dedicated
 * Moods & genres screen. [browseId] and [params] drive the InnerTube browse
 * request used to open the tile's content.
 */
@Serializable
data class MoodGenre(
    val title: String,
    val browseId: String,
    val params: String? = null,
)

/** A titled group of mood/genre tiles, parsed from the moods & genres browse page. */
@Serializable
data class MoodGenreSection(
    val title: String,
    val items: List<MoodGenre>,
)

/** The Explore screen payload: new release albums plus the mood/genre tiles. */
@Serializable
data class ExploreData(
    val newReleaseAlbums: List<Album>,
    val moodGenres: List<MoodGenre>,
)

/** A single entry in a charts section: either a playable song or an album. */
sealed interface ChartItem {
    val id: String
    val title: String
    val thumbnailUrl: String?

    data class SongItem(
        override val id: String,
        override val title: String,
        val artist: String,
        override val thumbnailUrl: String?,
    ) : ChartItem

    data class AlbumItem(
        override val id: String,
        override val title: String,
        val artist: String,
        val year: String?,
        override val thumbnailUrl: String?,
    ) : ChartItem
}

/** A titled section of the charts page (e.g. "Trending", "Top music videos"). */
@Serializable
data class ChartSection(
    val title: String,
    val items: List<ChartItem>,
)

/** A generic browse results page, used when opening a mood/genre tile. */
@Serializable
data class BrowseData(
    val title: String,
    val items: List<ChartItem>,
)

/**
 * The signed-in YouTube Music account profile, fetched from the
 * `account/account_menu` InnerTube endpoint.
 */
@Serializable
data class AccountInfo(
    val name: String,
    val email: String? = null,
    val channelHandle: String? = null,
    val thumbnailUrl: String? = null,
)

/**
 * A single entry in the signed-in account library (a liked playlist, album or
 * artist), parsed from the InnerTube `library` endpoint.
 */
sealed interface LibraryItem {
    val id: String
    val title: String
    val subtitle: String
    val thumbnailUrl: String?

    data class Playlist(
        override val id: String,
        override val title: String,
        override val subtitle: String,
        override val thumbnailUrl: String?,
    ) : LibraryItem

    data class Album(
        override val id: String,
        override val title: String,
        override val subtitle: String,
        override val thumbnailUrl: String?,
    ) : LibraryItem

    data class Artist(
        override val id: String,
        override val title: String,
        override val subtitle: String,
        override val thumbnailUrl: String?,
    ) : LibraryItem
}
