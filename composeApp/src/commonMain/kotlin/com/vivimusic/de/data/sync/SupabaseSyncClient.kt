package com.vivimusic.de.data.sync

import com.vivimusic.de.data.AppConfig
import com.vivimusic.de.data.clearSessionJson
import com.vivimusic.de.data.loadSessionJson
import com.vivimusic.de.data.network.sharedJson
import com.vivimusic.de.data.saveSessionJson
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Thin wrapper around supabase-kt (PostgREST + Auth + Realtime) scoped to the
 * sync needs of the application: playlists, playlist songs, favorites and
 * history, all filtered by the authenticated user.
 */
class SupabaseSyncClient private constructor(
    private val client: SupabaseClient,
    private val json: Json,
) {
    companion object {
        fun create(json: Json = sharedJson): SupabaseSyncClient {
            val client = createSupabaseClient(
                supabaseUrl = AppConfig.supabaseUrl,
                supabaseKey = AppConfig.supabaseAnonKey,
            ) {
                install(Auth) {
                    // Persist the session to disk so the user stays signed in
                    // across restarts: auto-saved on sign-in/refresh, restored
                    // on startup, cleared on sign-out.
                    sessionManager = FileSessionManager(json)
                    autoLoadFromStorage = true
                    autoSaveToStorage = true
                }
                install(Postgrest)
                install(Realtime)
            }
            return SupabaseSyncClient(client, json)
        }
    }

    // ----- auth -----

    suspend fun signIn(mail: String, password: String) {
        client.auth.signInWith(Email) {
            email = mail
            this.password = password
        }
    }

    suspend fun signUp(mail: String, password: String) {
        client.auth.signUpWith(Email) {
            email = mail
            this.password = password
        }
    }

    suspend fun signOut() {
        client.auth.signOut()
    }

    suspend fun currentUserId(): String? {
        awaitInitialized()
        return client.auth.currentUserOrNull()?.id
    }

    suspend fun currentEmail(): String? {
        awaitInitialized()
        return client.auth.currentUserOrNull()?.email
    }

    /** Waits for the Auth plugin to finish restoring the stored session. */
    private suspend fun awaitInitialized() {
        if (client.auth.sessionStatus.value == SessionStatus.Initializing) {
            client.auth.sessionStatus.first { it != SessionStatus.Initializing }
        }
    }

    // ----- pull -----

    suspend fun pullPlaylists(userId: String, since: Long): List<PlaylistDto> =
        client.from("playlists").select {
            filter {
                eq("user_id", userId)
                gte("updated_at", since)
            }
        }.decodeList<PlaylistDto>()

    suspend fun pullPlaylistSongs(userId: String, since: Long): List<PlaylistSongDto> =
        client.from("playlist_songs").select {
            filter {
                eq("user_id", userId)
                gte("updated_at", since)
            }
        }.decodeList<PlaylistSongDto>()

    suspend fun pullFavorites(userId: String, since: Long): List<FavoriteDto> =
        client.from("favorites").select {
            filter {
                eq("user_id", userId)
                gte("added_at", since)
            }
        }.decodeList<FavoriteDto>()

    suspend fun pullHistory(userId: String, since: Long): List<HistoryDto> =
        client.from("history").select {
            filter {
                eq("user_id", userId)
                gte("played_at", since)
            }
        }.decodeList<HistoryDto>()

    // ----- push -----

    suspend fun pushPlaylist(row: PlaylistDto) {
        client.from("playlists").upsert(row) { select() }.decodeSingleOrNull<PlaylistDto>()
    }

    suspend fun pushPlaylistSong(row: PlaylistSongDto) {
        client.from("playlist_songs").upsert(row) { select() }.decodeSingleOrNull<PlaylistSongDto>()
    }

    suspend fun pushFavorite(row: FavoriteDto) {
        client.from("favorites").upsert(row) { select() }.decodeSingleOrNull<FavoriteDto>()
    }

    suspend fun pushHistory(row: HistoryDto) {
        client.from("history").upsert(row) { select() }.decodeSingleOrNull<HistoryDto>()
    }

    suspend fun deletePlaylist(userId: String, id: String) {
        client.from("playlists").delete {
            filter {
                eq("user_id", userId)
                eq("id", id)
            }
        }
    }

    suspend fun deleteFavorite(userId: String, songId: String) {
        client.from("favorites").delete {
            filter {
                eq("user_id", userId)
                eq("song_id", songId)
            }
        }
    }

    // ----- realtime -----

    suspend fun observeTable(table: String, userId: String): Flow<PostgresAction> {
        val channel = client.channel("sync:$table:$userId")
        channel.subscribe()
        return channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            this.table = table
            filter("user_id", FilterOperator.EQ, userId)
        }
    }
}

/**
 * Persists the auth session as JSON on disk (see the `SessionStore`
 * expect/actual). The Auth plugin drives it automatically: save on
 * sign-in/refresh, load on startup, delete on sign-out.
 */
private class FileSessionManager(private val json: Json) : SessionManager {
    override suspend fun loadSession(): UserSession {
        val stored = loadSessionJson()
            ?: throw IllegalStateException("No stored session")
        return json.decodeFromString<UserSession>(stored)
    }

    override suspend fun saveSession(session: UserSession) {
        saveSessionJson(json.encodeToString(session))
    }

    override suspend fun deleteSession() {
        clearSessionJson()
    }
}
