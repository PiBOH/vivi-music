package com.vivimusic.de.data.sync

import com.vivimusic.de.data.db.AppDatabase
import com.vivimusic.de.data.db.FavoriteEntity
import com.vivimusic.de.data.db.HistoryEntity
import com.vivimusic.de.data.db.PlaylistEntity
import com.vivimusic.de.data.db.PlaylistSongEntity
import com.vivimusic.de.data.db.SongEntity
import com.vivimusic.de.data.db.SyncStateEntity
import com.vivimusic.de.data.network.sharedJson
import com.vivimusic.de.data.nowEpochMillis
import io.github.jan.supabase.realtime.PostgresAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

enum class SyncStatus {
    Disabled,
    Offline,
    Syncing,
    Synced,
    Error,
}

/**
 * Orchestrates bidirectional sync between the local Room database and Supabase.
 *
 * Pull: fetch remote rows updated after the last sync timestamp and upsert them
 * locally. Push: upload every local row (this first version pushes the whole
 * local dataset instead of tracking a per-row dirty flag, which keeps the
 * initial implementation simple and correct for small libraries).
 *
 * Realtime: subscribes to Postgres changes for the current user and applies
 * inserts/updates/deletes to the local database as they happen.
 */
class SyncManager(
    private val db: AppDatabase,
    private val syncClient: SupabaseSyncClient?,
    private val scope: CoroutineScope,
    private val json: Json = sharedJson,
) {
    private val _status = MutableStateFlow(SyncStatus.Disabled)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    val isEnabled: Boolean get() = syncClient != null

    /** User-facing switch (persisted under `sync.enabled`), defaults to on. */
    @Volatile
    var enabled: Boolean = true

    init {
        _status.value = if (syncClient != null) SyncStatus.Offline else SyncStatus.Disabled
    }

    /** Turns synchronization on/off and reflects the change in the status flow. */
    fun applyEnabled(value: Boolean) {
        enabled = value
        _status.value = if (!value || syncClient == null) {
            SyncStatus.Disabled
        } else {
            SyncStatus.Offline
        }
    }

    // ----- auth (delegated to Supabase) -----

    /** True when a Supabase backend is configured (auth + sync available). */
    val isConfigured: Boolean get() = syncClient != null

    suspend fun signIn(email: String, password: String) {
        val client = syncClient ?: throw IllegalStateException("Synchronization is not configured")
        client.signIn(email, password)
    }

    suspend fun signUp(email: String, password: String) {
        val client = syncClient ?: throw IllegalStateException("Synchronization is not configured")
        client.signUp(email, password)
    }

    suspend fun signOut() {
        syncClient?.signOut()
    }

    suspend fun currentUserEmail(): String? = syncClient?.currentEmail()

    /** Runs a full pull/push cycle for the signed-in user. */
    suspend fun fullSync() {
        val client = syncClient ?: return
        if (!enabled) {
            _status.value = SyncStatus.Disabled
            return
        }
        _status.value = SyncStatus.Syncing
        try {
            val userId = client.currentUserId() ?: run {
                _status.value = SyncStatus.Offline
                return
            }
            syncPlaylists(client, userId)
            syncFavorites(client, userId)
            syncHistory(client, userId)
            _status.value = SyncStatus.Synced
        } catch (t: Throwable) {
            _status.value = SyncStatus.Error
        }
    }

    /** Called after a local mutation to propagate the change to the server. */
    suspend fun afterLocalChange() {
        if (!enabled) return
        if (syncClient == null) return
        fullSync()
    }

    private suspend fun syncPlaylists(client: SupabaseSyncClient, userId: String) {
        val lastSynced = lastSyncedAt("playlists")
        val remote = client.pullPlaylists(userId, lastSynced)
        db.playlistDao().upsertAll(remote.map { it.toEntity() })
        db.playlistDao().getAllPlaylists().forEach { local ->
            client.pushPlaylist(local.toDto(userId))
        }

        val remoteSongs = client.pullPlaylistSongs(userId, lastSyncedAt("playlist_songs"))
        remoteSongs.forEach { it.applyToDb() }
        db.playlistSongDao().getAll().forEach { local ->
            client.pushPlaylistSong(local.toDto(userId))
        }
        markSynced("playlists")
        markSynced("playlist_songs")
    }

    private suspend fun syncFavorites(client: SupabaseSyncClient, userId: String) {
        val lastSynced = lastSyncedAt("favorites")
        val remote = client.pullFavorites(userId, lastSynced)
        remote.forEach { it.applyToDb() }
        db.favoriteDao().getAll().forEach { local ->
            client.pushFavorite(local.toDto(userId))
        }
        markSynced("favorites")
    }

    private suspend fun syncHistory(client: SupabaseSyncClient, userId: String) {
        val lastSynced = lastSyncedAt("history")
        val remote = client.pullHistory(userId, lastSynced)
        remote.forEach { it.applyToDb() }
        db.historyDao().getAll().forEach { local ->
            client.pushHistory(local.toDto(userId))
        }
        markSynced("history")
    }

    /** Subscribes to remote changes and mirrors them into the local database. */
    fun startRealtime() {
        val client = syncClient ?: return
        scope.launch {
            val userId = client.currentUserId() ?: return@launch
            listOf("playlists", "playlist_songs", "favorites", "history").forEach { table ->
                client.observeTable(table, userId).onEach { action ->
                    applyRemoteAction(table, action)
                }.launchIn(scope)
            }
        }
    }

    private suspend fun applyRemoteAction(table: String, action: PostgresAction) {
        when (action) {
            is PostgresAction.Insert -> applyInsert(table, action.record)
            is PostgresAction.Update -> applyInsert(table, action.record)
            is PostgresAction.Delete -> applyDelete(table, action.oldRecord)
            else -> Unit
        }
    }

    private suspend fun applyInsert(table: String, record: JsonElement) {
        when (table) {
            "playlists" -> db.playlistDao().upsert(json.decodeFromJsonElement<PlaylistDto>(record).toEntity())
            "playlist_songs" -> json.decodeFromJsonElement<PlaylistSongDto>(record).applyToDb()
            "favorites" -> json.decodeFromJsonElement<FavoriteDto>(record).applyToDb()
            "history" -> json.decodeFromJsonElement<HistoryDto>(record).applyToDb()
        }
    }

    private suspend fun applyDelete(table: String, record: JsonElement?) {
        record ?: return
        when (table) {
            "playlists" -> {
                val dto = json.decodeFromJsonElement<PlaylistDto>(record)
                db.playlistDao().deletePlaylist(dto.id)
                db.playlistSongDao().deleteByPlaylist(dto.id)
            }
            "playlist_songs" -> {
                val dto = json.decodeFromJsonElement<PlaylistSongDto>(record)
                db.playlistSongDao().deleteById(dto.id)
            }
            "favorites" -> {
                val dto = json.decodeFromJsonElement<FavoriteDto>(record)
                db.favoriteDao().delete(dto.songId)
            }
            "history" -> {
                val dto = json.decodeFromJsonElement<HistoryDto>(record)
                db.historyDao().delete(dto.songId)
            }
            else -> Unit
        }
    }

    private suspend fun lastSyncedAt(entityType: String): Long =
        db.syncStateDao().get(entityType)?.lastSyncedAtEpochMs ?: 0L

    private suspend fun markSynced(entityType: String) {
        db.syncStateDao().upsert(SyncStateEntity(entityType = entityType, lastSyncedAtEpochMs = nowEpochMillis()))
    }

    // ----- mapping helpers -----

    private suspend fun songMeta(songId: String): SongEntity? = db.songDao().getSong(songId)

    private fun PlaylistDto.toEntity() = PlaylistEntity(
        id = id,
        name = name,
        description = description,
        thumbnailUrl = thumbnailUrl,
        createdAtEpochMs = createdAt,
        updatedAtEpochMs = updatedAt,
        userId = userId,
    )

    private fun PlaylistEntity.toDto(userId: String) = PlaylistDto(
        id = id,
        userId = userId,
        name = name,
        description = description,
        thumbnailUrl = thumbnailUrl,
        createdAt = createdAtEpochMs,
        updatedAt = updatedAtEpochMs,
    )

    private suspend fun PlaylistSongEntity.toDto(userId: String): PlaylistSongDto {
        val song = songMeta(songId)
        return PlaylistSongDto(
            id = id,
            userId = userId,
            playlistId = playlistId,
            songId = songId,
            title = song?.title ?: "",
            artist = song?.artist ?: "",
            album = song?.album ?: "",
            thumbnailUrl = song?.thumbnailUrl,
            durationMs = song?.durationMs,
            position = position,
            updatedAt = nowEpochMillis(),
        )
    }

    private suspend fun PlaylistSongDto.applyToDb() {
        db.songDao().upsert(
            SongEntity(
                id = songId,
                title = title,
                artist = artist,
                album = album,
                thumbnailUrl = thumbnailUrl,
                durationMs = durationMs,
            )
        )
        db.playlistSongDao().upsert(PlaylistSongEntity(id = id, playlistId = playlistId, songId = songId, position = position))
    }

    private suspend fun FavoriteDto.applyToDb() {
        db.songDao().upsert(
            SongEntity(
                id = songId,
                title = title,
                artist = artist,
                album = album,
                thumbnailUrl = thumbnailUrl,
                durationMs = durationMs,
            )
        )
        db.favoriteDao().upsert(FavoriteEntity(songId = songId, addedAtEpochMs = addedAt))
    }

    private suspend fun FavoriteEntity.toDto(userId: String): FavoriteDto {
        val song = songMeta(songId)
        return FavoriteDto(
            id = "$userId:$songId",
            userId = userId,
            songId = songId,
            title = song?.title ?: "",
            artist = song?.artist ?: "",
            album = song?.album ?: "",
            thumbnailUrl = song?.thumbnailUrl,
            durationMs = song?.durationMs,
            addedAt = addedAtEpochMs,
        )
    }

    private suspend fun HistoryDto.applyToDb() {
        db.songDao().upsert(
            SongEntity(
                id = songId,
                title = title,
                artist = artist,
                album = album,
                thumbnailUrl = thumbnailUrl,
                durationMs = durationMs,
            )
        )
        db.historyDao().upsert(HistoryEntity(songId = songId, playedAtEpochMs = playedAt))
    }

    private suspend fun HistoryEntity.toDto(userId: String): HistoryDto {
        val song = songMeta(songId)
        return HistoryDto(
            id = "$userId:$songId",
            userId = userId,
            songId = songId,
            title = song?.title ?: "",
            artist = song?.artist ?: "",
            album = song?.album ?: "",
            thumbnailUrl = song?.thumbnailUrl,
            durationMs = song?.durationMs,
            playedAt = playedAtEpochMs,
        )
    }
}
