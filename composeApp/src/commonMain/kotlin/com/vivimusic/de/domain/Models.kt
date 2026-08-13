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
