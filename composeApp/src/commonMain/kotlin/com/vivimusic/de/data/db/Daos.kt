package com.vivimusic.de.data.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY updatedAtEpochMs DESC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists ORDER BY updatedAtEpochMs DESC")
    suspend fun getAllPlaylists(): List<PlaylistEntity>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylist(id: String): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(playlist: PlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(playlists: List<PlaylistEntity>)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: String)
}

@Dao
interface SongDao {
    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getSong(id: String): SongEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(song: SongEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(songs: List<SongEntity>)
}

@Dao
interface PlaylistSongDao {
    @Query("SELECT s.* FROM playlist_songs ps JOIN songs s ON s.id = ps.songId WHERE ps.playlistId = :playlistId ORDER BY ps.position ASC")
    fun observeSongs(playlistId: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM playlist_songs")
    suspend fun getAll(): List<PlaylistSongEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: PlaylistSongEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<PlaylistSongEntity>)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun deleteByPlaylist(playlistId: String)

    @Query("DELETE FROM playlist_songs WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface FavoriteDao {
    @Query("SELECT s.* FROM favorites f JOIN songs s ON s.id = f.songId ORDER BY f.addedAtEpochMs DESC")
    fun observeFavorites(): Flow<List<SongEntity>>

    @Query("SELECT * FROM favorites")
    suspend fun getAll(): List<FavoriteEntity>

    @Query("SELECT songId FROM favorites")
    suspend fun getFavoriteIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE songId = :songId")
    suspend fun delete(songId: String)
}

@Dao
interface HistoryDao {
    @Query("SELECT s.* FROM history h JOIN songs s ON s.id = h.songId ORDER BY h.playedAtEpochMs DESC")
    fun observeHistory(): Flow<List<SongEntity>>

    @Query("SELECT * FROM history")
    suspend fun getAll(): List<HistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: HistoryEntity)

    @Query("DELETE FROM history WHERE songId = :songId")
    suspend fun delete(songId: String)
}

@Dao
interface SyncStateDao {
    @Query("SELECT * FROM sync_state")
    suspend fun getAll(): List<SyncStateEntity>

    @Query("SELECT * FROM sync_state WHERE entityType = :entityType")
    suspend fun get(entityType: String): SyncStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: SyncStateEntity)
}
