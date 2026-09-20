package com.vivimusic.de.data.sync

import kotlinx.serialization.Serializable

/**
 * Remote representations of the user data stored in Supabase. Field names use
 * camelCase: supabase-kt converts them to snake_case columns automatically
 * (CAMEL_CASE_TO_SNAKE_CASE is the default conversion method).
 *
 * Favorites, history and playlist songs embed the track metadata so that a
 * synced row is immediately displayable on any device without an extra network
 * round-trip.
 */
@Serializable
data class PlaylistDto(
    val id: String,
    val userId: String,
    val name: String,
    val description: String? = null,
    val thumbnailUrl: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

@Serializable
data class PlaylistSongDto(
    val id: String,
    val userId: String,
    val playlistId: String,
    val songId: String,
    val title: String,
    val artist: String = "",
    val album: String = "",
    val thumbnailUrl: String? = null,
    val durationMs: Long? = null,
    val position: Int,
    val updatedAt: Long = 0L,
)

@Serializable
data class FavoriteDto(
    val id: String,
    val userId: String,
    val songId: String,
    val title: String,
    val artist: String = "",
    val album: String = "",
    val thumbnailUrl: String? = null,
    val durationMs: Long? = null,
    val addedAt: Long = 0L,
)

@Serializable
data class HistoryDto(
    val id: String,
    val userId: String,
    val songId: String,
    val title: String,
    val artist: String = "",
    val album: String = "",
    val thumbnailUrl: String? = null,
    val durationMs: Long? = null,
    val playedAt: Long = 0L,
)
