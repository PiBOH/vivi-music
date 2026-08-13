package com.vivimusic.de.data

import com.vivimusic.de.data.db.AppDatabase
import com.vivimusic.de.data.db.FavoriteEntity
import com.vivimusic.de.data.db.HistoryEntity
import com.vivimusic.de.data.db.SearchHistoryEntity
import com.vivimusic.de.data.db.SongEntity
import com.vivimusic.de.data.lyrics.LyricsClient
import com.vivimusic.de.data.lyrics.LyricsLine
import com.vivimusic.de.data.network.InnerTubeClient
import com.vivimusic.de.data.sync.SyncManager
import com.vivimusic.de.domain.Album
import com.vivimusic.de.domain.Artist
import com.vivimusic.de.domain.HomeSection
import com.vivimusic.de.domain.Playlist
import com.vivimusic.de.domain.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * The single entry point the UI uses to read and mutate music data. It sits on
 * top of the local database, the InnerTube client and the sync manager.
 */
class MusicRepository(
    private val db: AppDatabase,
    private val innerTube: InnerTubeClient,
    private val lyricsClient: LyricsClient,
    private val syncManager: SyncManager,
    private val scope: CoroutineScope,
) {
    // ----- remote catalog -----

    suspend fun search(query: String): List<Song> = innerTube.search(query)

    suspend fun searchSuggestions(query: String): List<String> = innerTube.getSearchSuggestions(query)

    suspend fun home(): List<HomeSection> = innerTube.getHome()

    suspend fun getAlbumOrPlaylist(browseId: String): Album = innerTube.getAlbumOrPlaylist(browseId)

    suspend fun getArtist(browseId: String): Artist = innerTube.getArtist(browseId)

    suspend fun getSong(videoId: String): Song? = innerTube.getSong(videoId)

    // ----- lyrics -----

    suspend fun getLyrics(song: Song): List<LyricsLine> =
        lyricsClient.getLyrics(song.title, song.artist, song.durationMs?.div(1_000))

    // ----- favorites -----

    fun observeFavorites(): Flow<List<Song>> =
        db.favoriteDao().observeFavorites().map { list -> list.map { it.toDomain() } }

    fun isFavorite(songId: String): Flow<Boolean> =
        db.favoriteDao().observeFavorites().map { list -> list.any { it.id == songId } }

    fun toggleFavorite(song: Song) {
        scope.launch {
            val wasFavorite = db.favoriteDao().getFavoriteIds().contains(song.id)
            if (wasFavorite) {
                db.favoriteDao().delete(song.id)
            } else {
                db.songDao().upsert(song.toEntity())
                db.favoriteDao().upsert(FavoriteEntity(songId = song.id, addedAtEpochMs = nowEpochMillis()))
            }
            syncManager.afterLocalChange()
        }
    }

    // ----- history -----

    fun observeHistory(): Flow<List<Song>> =
        db.historyDao().observeHistory().map { list -> list.map { it.toDomain() } }

    fun recordPlay(song: Song) {
        scope.launch {
            db.songDao().upsert(song.toEntity())
            db.historyDao().upsert(HistoryEntity(songId = song.id, playedAtEpochMs = nowEpochMillis()))
            syncManager.afterLocalChange()
        }
    }

    // ----- playlists -----

    fun observePlaylists(): Flow<List<Playlist>> =
        db.playlistDao().observePlaylists().map { list -> list.map { it.toDomain() } }

    fun createPlaylist(name: String) {
        scope.launch {
            val id = "pl-${nowEpochMillis()}-${name.hashCode()}"
            db.playlistDao().upsert(
                com.vivimusic.de.data.db.PlaylistEntity(
                    id = id,
                    name = name,
                    description = null,
                    thumbnailUrl = null,
                    createdAtEpochMs = nowEpochMillis(),
                    updatedAtEpochMs = nowEpochMillis(),
                    userId = null,
                )
            )
            syncManager.afterLocalChange()
        }
    }

    fun deletePlaylist(id: String) {
        scope.launch {
            db.playlistDao().deletePlaylist(id)
            db.playlistSongDao().deleteByPlaylist(id)
            syncManager.afterLocalChange()
        }
    }

    fun observePlaylistSongs(playlistId: String): Flow<List<Song>> =
        db.playlistSongDao().observeSongs(playlistId).map { list -> list.map { it.toDomain() } }

    fun addSongToPlaylist(playlistId: String, song: Song) {
        scope.launch {
            db.songDao().upsert(song.toEntity())
            db.playlistSongDao().upsert(
                com.vivimusic.de.data.db.PlaylistSongEntity(
                    id = "$playlistId-${song.id}",
                    playlistId = playlistId,
                    songId = song.id,
                    position = nowEpochMillis().toInt(),
                )
            )
            syncManager.afterLocalChange()
        }
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        scope.launch {
            db.playlistSongDao().deleteById("$playlistId-$songId")
            syncManager.afterLocalChange()
        }
    }

    // ----- search history -----

    fun observeSearchHistory(): Flow<List<String>> =
        db.searchHistoryDao().observeSearchHistory().map { list -> list.map { it.query } }

    fun addSearchHistory(query: String) {
        scope.launch {
            db.searchHistoryDao().insert(
                SearchHistoryEntity(query = query, searchedAtEpochMs = nowEpochMillis())
            )
        }
    }

    fun deleteSearchHistory(query: String) {
        scope.launch {
            db.searchHistoryDao().delete(query)
        }
    }

    fun clearSearchHistory() {
        scope.launch {
            db.searchHistoryDao().clearAll()
        }
    }

    fun clearHistory() {
        scope.launch {
            db.historyDao().clearAll()
            syncManager.afterLocalChange()
        }
    }

    // ----- sync -----

    fun syncNow() {
        scope.launch { syncManager.fullSync() }
    }

    // ----- mapping -----

    private fun SongEntity.toDomain() = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        thumbnailUrl = thumbnailUrl,
        durationMs = durationMs,
    )

    private fun Song.toEntity() = SongEntity(
        id = id,
        title = title,
        artist = artist,
        album = album,
        thumbnailUrl = thumbnailUrl,
        durationMs = durationMs,
    )

    private fun com.vivimusic.de.data.db.PlaylistEntity.toDomain() = Playlist(
        id = id,
        name = name,
        description = description,
        thumbnailUrl = thumbnailUrl,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs,
    )
}
