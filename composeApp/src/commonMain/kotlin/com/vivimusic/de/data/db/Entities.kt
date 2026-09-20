package com.vivimusic.de.data.db

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val thumbnailUrl: String?,
    val durationMs: Long?,
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
    val thumbnailUrl: String?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val userId: String?,
)

@Entity(tableName = "playlist_songs")
data class PlaylistSongEntity(
    @PrimaryKey val id: String,
    val playlistId: String,
    val songId: String,
    val position: Int,
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val songId: String,
    val addedAtEpochMs: Long,
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val songId: String,
    val playedAtEpochMs: Long,
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val searchedAtEpochMs: Long,
)

@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val entityType: String,
    val lastSyncedAtEpochMs: Long,
)
