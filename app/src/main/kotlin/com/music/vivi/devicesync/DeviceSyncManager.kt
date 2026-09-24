/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.devicesync

import android.content.Context
import android.os.Build
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.music.vivi.constants.AppLanguageKey
import com.music.vivi.constants.AppLanguagePeerDeviceKey
import com.music.vivi.constants.AppLanguagePeerSeqKey
import com.music.vivi.constants.AppLanguageSeqKey
import com.music.vivi.constants.AudioNormalizationKey
import com.music.vivi.constants.AudioQuality
import com.music.vivi.constants.AudioQualityKey
import com.music.vivi.constants.ContentCountryKey
import com.music.vivi.constants.ContentLanguageKey
import com.music.vivi.constants.CrossfadeDurationKey
import com.music.vivi.constants.CrossfadeEnabledKey
import com.music.vivi.constants.DarkModeKey
import com.music.vivi.constants.DeviceSyncDeviceIdKey
import com.music.vivi.constants.DeviceSyncEnabledKey
import com.music.vivi.constants.DeviceSyncPairIdKey
import com.music.vivi.constants.DeviceSyncServerUrlKey
import com.music.vivi.constants.DynamicThemeKey
import com.music.vivi.constants.EnableDiscordRPCKey
import com.music.vivi.constants.EnableKugouKey
import com.music.vivi.constants.EnableLastFMScrobblingKey
import com.music.vivi.constants.EnableListenTogetherKey
import com.music.vivi.constants.EnableLrcLibKey
import com.music.vivi.constants.EnableMusixmatchKey
import com.music.vivi.constants.EnablePaxsenixKey
import com.music.vivi.constants.EnableUnisonKey
import com.music.vivi.constants.EnableYouLyPlusKey
import com.music.vivi.constants.LyricsRomanizeChineseKey
import com.music.vivi.constants.LyricsRomanizeJapaneseKey
import com.music.vivi.constants.LyricsRomanizeKoreanKey
import com.music.vivi.constants.PreferredLyricsProvider
import com.music.vivi.constants.PreferredLyricsProviderKey
import com.music.vivi.constants.PureBlackKey
import com.music.vivi.constants.SYSTEM_DEFAULT
import com.music.vivi.constants.SaavnAudioQuality
import com.music.vivi.constants.SaavnAudioQualityKey
import com.music.vivi.constants.SelectedFontKey
import com.music.vivi.constants.SelectedThemeColorKey
import com.music.vivi.constants.SkipSilenceKey
import com.music.vivi.constants.SuggestionRegionKey
import com.music.vivi.constants.SyncViviVolumeKey
import com.music.vivi.constants.TranslateLanguageKey
import com.music.vivi.constants.TranslateLyricsKey
import com.music.vivi.db.MusicDatabase
import com.music.vivi.db.entities.Playlist
import com.music.vivi.db.entities.PlaylistEntity
import com.music.vivi.db.entities.PlaylistSongMap
import com.music.vivi.db.entities.Song
import com.music.vivi.models.MediaMetadata
import com.music.vivi.sync.LibrarySnapshot
import com.music.vivi.sync.PlaybackSnapshot
import com.music.vivi.sync.SyncClient
import com.music.vivi.sync.SyncConnectionState
import com.music.vivi.sync.SyncEvent
import com.music.vivi.sync.SyncServer
import com.music.vivi.sync.SyncSnapshot
import com.music.vivi.sync.SyncedPlaylist
import com.music.vivi.sync.SyncedSong
import com.music.vivi.utils.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridges the shared `sync` module with the Android app.
 *
 * - Observes the shared preferences subset and pushes a snapshot when it changes.
 * - Applies incoming snapshots (settings + a pending playback resume).
 * - Exposes pairing helpers (`createPairingCode` / `joinPair`) for the UI.
 *
 * Playback (queue + position) is captured via [pushPlayback], called from the
 * player layer; the incoming resume is exposed through [pendingPlayback].
 */
@Singleton
class DeviceSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var client: SyncClient? = null
    private var started = false

    /** Set while applying a remote snapshot, to avoid echoing it back. */
    @Volatile
    private var applyingRemote = false

    /** While set, playback pushes are suppressed (avoids echoing a snapshot back). */
    @Volatile
    private var suppressPlaybackPushUntil = 0L

    /** (deviceId, seq) language markers of the snapshot being applied. */
    private var pendingLangPeerId: String = ""
    private var pendingLangPeerSeq: Long = 0L

    /** Resolving state of the last snapshot we actually sent (for forcing the
     *  resolving/ready transition past the echo-suppression window). */
    private var lastPushedResolving: Boolean? = null

    /** While set, library pushes are suppressed (avoids echoing an applied snapshot). */
    @Volatile
    private var suppressLibraryPushUntil = 0L

    /**
     * Liked-song ledger of this session: the unlike **tombstones** (id -> the time
     * of the unlike) that have to travel to the desktop, plus the liked ids of the
     * last library read, which is how an unlike made here is noticed at all (a song
     * that was in the library and is not any more). The like state itself lives in
     * the database, so only the tombstones need this side memory.
     */
    private val likeTombstones = mutableMapOf<String, Long>()
    private var lastLikedIds: Set<String> = emptySet()

    private var lastPlayback: PlaybackSnapshot? = null

    private var lastLibrary: LibrarySnapshot? = null

    /** Queue fingerprint + last-write-wins timestamp (shared relay-time frame). */
    private var lastQueueFingerprint = ""
    private var queueUpdatedAt = 0L

    private val _paired = MutableStateFlow(false)
    val paired: StateFlow<Boolean> = _paired.asStateFlow()

    private val _status = MutableStateFlow("")
    val status: StateFlow<String> = _status.asStateFlow()

    private val _peerDeviceName = MutableStateFlow("")
    val peerDeviceName: StateFlow<String> = _peerDeviceName.asStateFlow()

    private val _pendingPlayback = MutableStateFlow<PlaybackSnapshot?>(null)
    val pendingPlayback: StateFlow<PlaybackSnapshot?> = _pendingPlayback.asStateFlow()

    private val _syncedLibrary = MutableStateFlow<LibrarySnapshot?>(null)
    val syncedLibrary: StateFlow<LibrarySnapshot?> = _syncedLibrary.asStateFlow()

    fun start() {
        if (started) return
        started = true
        scope.launch { observeLifecycle() }
        scope.launch { observeSettingsAndPush() }
        scope.launch { observeLibraryAndPush() }
    }

    // ---------------------------------------------------------------------
    // Public pairing / sync API (for the settings UI)
    // ---------------------------------------------------------------------

    fun createPairingCode() {
        scope.launch {
            ensureClient()
            client?.requestPairingCode()
        }
    }

    fun joinPair(code: String) {
        scope.launch {
            ensureClient()
            client?.joinPair(code)
        }
    }

    /**
     * A desktop QR was scanned: persist its relay address and join with its code
     * in one ordered operation.
     *
     * The QR already carries both ("vivimusic://pair?addr=…&code=…"), so the
     * user should not have to review the field and tap Pair afterwards — that
     * extra step is what made a successful scan look like the code had not been
     * recognized. The address is written first and [ensureClient] builds the
     * client for THAT address before the code is sent, so the join cannot race
     * against the URL change.
     */
    fun joinPairFromScan(serverUrl: String?, code: String) {
        val trimmed = code.trim()
        if (trimmed.isEmpty()) return
        scope.launch {
            if (!serverUrl.isNullOrBlank()) {
                context.dataStore.edit { it[DeviceSyncServerUrlKey] = serverUrl }
            }
            ensureClient()
            client?.joinPair(trimmed)
        }
    }

    fun unpair() {
        scope.launch {
            client?.unpair()
            context.dataStore.edit {
                it.remove(DeviceSyncPairIdKey)
                it[DeviceSyncEnabledKey] = false
            }
            _paired.value = false
            _peerDeviceName.value = ""
        }
    }

    /**
     * Persist the relay server URL with the app-lifetime scope so the value
     * survives leaving the Devices screen (a screen-scoped coroutine is
     * cancelled on exit, which made a cleared/changed URL snap back to the
     * last-used one). A blank value is stored as-is: unpaired users then see
     * the default cloud relay on the next visit.
     */
    fun saveServerUrl(value: String) {
        scope.launch {
            context.dataStore.edit { it[DeviceSyncServerUrlKey] = value }
        }
    }

    /**
     * Capture the current queue + position from the player and sync it.
     *
     * @return true if the snapshot will actually be sent (not suppressed by the
     * echo window and the client is connected); false when it was dropped, so
     * callers that care (the volume poll) can retry.
     */
    fun pushPlayback(playback: PlaybackSnapshot): Boolean {
        // Only stamp the shared-clock timestamp once the relay clock offset is
        // measured. Stamping a raw local clock (before the first PONG / with an
        // older relay) makes the peer extrapolate by the clock skew and causes
        // the two players to keep seeking each other back and forth.
        val stamp = playback.positionAtMs == 0L && client?.hasServerOffset == true
        var snap = if (stamp) {
            playback.copy(positionAtMs = serverNowMs())
        } else {
            playback
        }
        val fp = queueFingerprint(snap)
        if (fp.isNotEmpty() && fp != lastQueueFingerprint) {
            // The queue/index changed locally: stamp a fresh LWW timestamp.
            lastQueueFingerprint = fp
            queueUpdatedAt = serverNowMs()
        }
        lastPlayback = snap.copy(queueUpdatedAt = queueUpdatedAt)
        // A resolving transition is new, asymmetric information (this device
        // needs time to buffer while the peer may already be playing). Force it
        // past the echo-suppression window so the peer learns to hold/start.
        val resolving = snap.isResolving
        if (!(resolving != lastPushedResolving) &&
            System.currentTimeMillis() < suppressPlaybackPushUntil
        ) {
            return false
        }
        val c = client ?: return false
        if (c.connectionState.value != SyncConnectionState.CONNECTED) return false
        scope.launch { pushCurrentSnapshot() }
        lastPushedResolving = resolving
        return true
    }

    /** Last-write-wins timestamp of the local queue (relay frame); 0 = none. */
    fun queueUpdatedAt(): Long = queueUpdatedAt

    /** Adopts the remote queue's fingerprint + timestamp right after applying it. */
    fun noteQueueApplied(snapshot: PlaybackSnapshot) {
        val fp = queueFingerprint(snapshot)
        if (fp.isNotEmpty()) {
            lastQueueFingerprint = fp
            if (snapshot.queueUpdatedAt > 0L) queueUpdatedAt = snapshot.queueUpdatedAt
        }
    }

    private fun queueFingerprint(p: PlaybackSnapshot): String =
        if (p.queue.isEmpty()) "" else p.queue.joinToString("|") { it.id } + "@" + p.queueIndex

    /** Estimated clock offset to the relay server (see [SyncClient.serverOffsetMs]). */
    val serverOffsetMs: Long get() = client?.serverOffsetMs ?: 0L

    /** Current epoch millis in the shared relay-time reference frame. */
    private fun serverNowMs(): Long = System.currentTimeMillis() + serverOffsetMs

    /**
     * Live position of a received snapshot: extrapolates `positionMs + elapsed`
     * while the peer is playing, using the timestamp and the shared clock
     * reference frame (falls back to the raw position otherwise).
     */
    fun effectivePosition(snapshot: PlaybackSnapshot): Long {
        val base = snapshot.positionMs.coerceAtLeast(0L)
        val at = snapshot.positionAtMs
        // While the peer is resolving its stream the position is frozen, so
        // extrapolating would race ahead of the peer's actual playback.
        if (at <= 0L || !snapshot.isPlaying || snapshot.isResolving) return base
        val elapsed = (serverNowMs() - at).coerceAtLeast(0L)
        return (base + elapsed).coerceAtLeast(0L)
    }

    // ---------------------------------------------------------------------
    // Internals
    // ---------------------------------------------------------------------

    private suspend fun observeLifecycle() {
        context.dataStore.data
            .map { prefs ->
                val enabled = prefs[DeviceSyncEnabledKey] ?: false
                val pairId = prefs[DeviceSyncPairIdKey].orEmpty()
                enabled to pairId
            }
            .distinctUntilChanged()
            .collect { (enabled, pairId) ->
                _paired.value = enabled && pairId.isNotEmpty()
                if (enabled && pairId.isNotEmpty()) {
                    ensureClient()
                } else if (!enabled) {
                    teardownClient()
                }
            }
    }

    private suspend fun observeSettingsAndPush() {
        context.dataStore.data
            .map { readSettings(it) }
            .distinctUntilChanged()
            .collect { settings ->
                if (applyingRemote || !_paired.value) return@collect
                pushCurrentSnapshot(settings)
            }
    }

    /** Observe the local library (liked songs / albums / artists / playlists) and push it. */
    private suspend fun observeLibraryAndPush() {
        combine(
            database.likedSongsByCreateDateAsc(),
            database.albumsLikedByCreateDateAsc(),
            database.artistsBookmarkedByCreateDateAsc(),
            database.playlistsByCreateDateAsc(),
        ) { songs, albums, artists, playlists ->
            PlaylistLibraryInput(
                songs = songs,
                albumIds = albums.map { it.album.id },
                artistIds = artists.map { it.artist.id },
                playlists = playlists,
            )
        }
            .distinctUntilChanged()
            .collect { input ->
                // Resolve each playlist's ordered songs (the combine transform is
                // non-suspend, so this happens here in the suspend collector).
                val syncedPlaylists = input.playlists.map { p ->
                    val songs = database.playlistSongs(p.playlist.id).first()
                    SyncedPlaylist(
                        id = p.playlist.id,
                        name = p.playlist.name,
                        songs = songs.map { ps ->
                            SyncedSong(
                                id = ps.song.song.id,
                                title = ps.song.song.title,
                                artist = ps.song.artists.joinToString(", ") { it.name },
                                thumbnail = ps.song.song.thumbnailUrl,
                            )
                        },
                        updatedAt = p.playlist.lastUpdateTime?.toInstant(ZoneOffset.UTC)?.toEpochMilli() ?: 0L,
                        // The account's playlist id, when this playlist lives on
                        // YouTube Music. It is the only identity the two devices
                        // share: the local row ids are generated per device
                        // ("LP" + 8 characters), so without this the peer cannot
                        // tell the arriving copy from the playlist it already has
                        // and imports a second one (E1034).
                        remoteId = p.playlist.browseId,
                    )
                }
                // ---- liked songs (the phone side of the liked-song ledger) ----
                val now = System.currentTimeMillis()
                val currentIds = input.songs.map { it.song.id }
                // An unlike made here is a song that was in the library at the
                // previous read and is not any more. It becomes a tombstone with
                // its own time, because that time is the only thing that lets the
                // desktop tell "the user unliked it here" from "the desktop's copy
                // of a re-like is older". A tombstone that arrived *from* the
                // desktop is already recorded with the desktop's own timestamp,
                // so `putIfAbsent` never re-stamps it with this device's clock.
                for (id in lastLikedIds - currentIds.toSet()) {
                    likeTombstones.putIfAbsent(id, now)
                }
                // A song liked again after an unlike drops its tombstone: the new
                // like must not be beaten by the older unlike on the desktop.
                for (song in input.songs) {
                    val tombstone = likeTombstones[song.song.id] ?: continue
                    val likedAt = song.song.likedDate
                        ?.toInstant(ZoneOffset.UTC)?.toEpochMilli() ?: 0L
                    if (likedAt > tombstone) likeTombstones.remove(song.song.id)
                }
                lastLikedIds = currentIds.toSet()
                val likedEntries = input.songs.map { song ->
                    SyncedSong(
                        id = song.song.id,
                        // No title/artist/thumbnail: the desktop renders its own
                        // songs and only needs "is this one liked" — the metadata
                        // is what a *desktop* like carries, for the phone that
                        // may have never seen the song (see applyRemoteLikedSongs).
                        updatedAt = song.song.likedDate
                            ?.toInstant(ZoneOffset.UTC)?.toEpochMilli() ?: 0L,
                    )
                } + likeTombstones.map { (id, at) ->
                    SyncedSong(id = id, updatedAt = at, deleted = true)
                }

                lastLibrary = LibrarySnapshot(
                    songIds = input.songs.map { it.song.id },
                    albumIds = input.albumIds,
                    artistIds = input.artistIds,
                    playlistIds = input.playlists.map { it.playlist.id },
                    playlists = syncedPlaylists,
                    likedSongs = likedEntries,
                )
                if (!applyingRemote && _paired.value && System.currentTimeMillis() >= suppressLibraryPushUntil) {
                    pushCurrentSnapshot()
                }
            }
    }

    private suspend fun ensureClient() {
        val prefs = context.dataStore.data.first()
        // A cleared (blank) URL means "not set": fall back to the default cloud
        // relay instead of trying to connect to an empty address.
        val url = prefs[DeviceSyncServerUrlKey]?.takeIf { it.isNotBlank() } ?: SyncServer.DEFAULT_URL
        val deviceId = resolveDeviceId(prefs)

        val existing = client
        if (existing != null && existing.serverUrl == url && existing.deviceId == deviceId) {
            if (existing.connectionState.value != SyncConnectionState.CONNECTED) existing.connect()
            return
        }

        teardownClient()
        val created = SyncClient(url, deviceId, defaultDeviceName())
        client = created
        scope.launch { created.events.collect { handleEvent(it) } }
        created.connect()
    }

    private fun teardownClient() {
        client?.disconnect()
        client = null
    }

    private suspend fun resolveDeviceId(prefs: Preferences): String {
        prefs[DeviceSyncDeviceIdKey]?.takeIf { it.isNotEmpty() }?.let { return it }
        val id = UUID.randomUUID().toString()
        context.dataStore.edit { it[DeviceSyncDeviceIdKey] = id }
        return id
    }

    private fun defaultDeviceName(): String {
        val name = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
        return name.ifBlank { "Android" }
    }

    private suspend fun handleEvent(event: SyncEvent) {
        when (event) {
            is SyncEvent.Connected -> {
                _status.value = "Connected"
                client?.pullSnapshot()
            }
            is SyncEvent.PairCode -> _status.value = event.code
            is SyncEvent.Paired -> {
                _paired.value = true
                _peerDeviceName.value = event.peerDeviceName
                context.dataStore.edit {
                    it[DeviceSyncPairIdKey] = event.pairId
                    it[DeviceSyncEnabledKey] = true
                }
                _status.value = "Paired with ${event.peerDeviceName}"
                pushCurrentSnapshot()
            }
            is SyncEvent.SnapshotReceived -> applySnapshot(event.snapshot)
            is SyncEvent.Disconnected -> _status.value = "Disconnected"
            is SyncEvent.NoSnapshot -> Unit
            is SyncEvent.Error -> {
                _status.value = event.message
                // The peer unpaired us, or the relay no longer knows about this
                // pair (e.g. the desktop's LAN relay was stopped/restarted). Drop
                // the local pairing so the UI stops claiming we are paired.
                if (event.message.contains("unpaired", ignoreCase = true) ||
                    event.message.contains("not paired", ignoreCase = true)
                ) {
                    context.dataStore.edit {
                        it.remove(DeviceSyncPairIdKey)
                        it[DeviceSyncEnabledKey] = false
                    }
                    _paired.value = false
                    _peerDeviceName.value = ""
                }
            }
        }
    }

    private suspend fun applySnapshot(snapshot: SyncSnapshot) {
        val current = client ?: return
        if (snapshot.deviceId == current.deviceId) return

        applyingRemote = true
        // Language markers carried by the sender (see applySetting for why the
        // phone never blindly adopts a language pushed at pair time).
        pendingLangPeerId = snapshot.settings["languageDeviceId"].orEmpty()
        pendingLangPeerSeq = snapshot.settings["languageSeq"]?.toLongOrNull() ?: 0L
        try {
            snapshot.settings.forEach { (key, value) -> applySetting(key, value) }
            snapshot.deviceName.takeIf { it.isNotBlank() }?.let { _peerDeviceName.value = it }
            if (snapshot.playback != null) {
                suppressPlaybackPushUntil = System.currentTimeMillis() + 1500L
                _pendingPlayback.value = snapshot.playback
            }
            snapshot.library?.let { lib ->
                _syncedLibrary.value = lib
                if (lib.playlists.isNotEmpty()) applyRemotePlaylists(lib.playlists)
                applyRemoteLikedSongs(lib)
                suppressLibraryPushUntil = System.currentTimeMillis() + 2000L
            }
        } catch (e: Exception) {
            Timber.e(e, "DeviceSync: failed to apply snapshot")
        } finally {
            applyingRemote = false
        }
    }

    /**
     * Applies the peer's playlist list to the local library, per-playlist
     * last-write-wins by [SyncedPlaylist.updatedAt]. Deletion tombstones remove
     * the local playlist only when they are newer than the local edit.
     */
    private suspend fun applyRemotePlaylists(remote: List<SyncedPlaylist>) {
        val now = LocalDateTime.now()
        for (r in remote) {
            // The same playlist is recognised by its *account* id, not by the
            // local row id: this app stores the account's playlists under its own
            // generated id (see [PlaylistEntity.generatePlaylistId]) while the
            // desktop has another one, so matching on the id alone imported a
            // second copy of every account playlist at pairing (E1034).
            val accountId = r.remoteId?.takeIf { it.isNotBlank() }
            if (accountId != null && SPECIAL_ACCOUNT_PLAYLIST_IDS.contains(accountId)) {
                dropSpecialPlaylist(accountId)
                continue
            }
            val local = database.playlist(r.id).first()
                ?: accountId?.let { database.playlistByBrowseId(it).first() }
                ?: sameContentPlaylist(r)
            val localUpdatedAt = local?.playlist?.lastUpdateTime
                ?.toInstant(ZoneOffset.UTC)?.toEpochMilli() ?: 0L

            if (r.deleted) {
                // Only the local row is removed: a playlist is never deleted from
                // YouTube Music from here, whichever way it arrived — only the
                // delete the user performs does that.
                if (local != null && r.updatedAt > localUpdatedAt) {
                    database.delete(local.playlist)
                }
                continue
            }
            if (r.updatedAt <= localUpdatedAt) continue // local is newer/equal: keep it

            // Preserve the peer's edit timestamp instead of stamping "now":
            // last-write-wins compares `updatedAt` against the stored
            // `lastUpdateTime`, so overwriting it with the local clock made the
            // next remote rename/delete look "older" and get silently dropped.
            val remoteUpdateTime = if (r.updatedAt > 0L) {
                LocalDateTime.ofInstant(Instant.ofEpochMilli(r.updatedAt), ZoneOffset.UTC)
            } else now

            val playlistId = local?.playlist?.id ?: r.id.ifBlank { PlaylistEntity.generatePlaylistId() }
            if (local == null) {
                database.insert(
                    PlaylistEntity(
                        id = playlistId,
                        name = r.name,
                        // Kept so this row is known to live on the account: the
                        // playlist is not treated as a local-only one, and a later
                        // rename/delete from the desktop lands on it.
                        browseId = accountId,
                        bookmarkedAt = now,
                        lastUpdateTime = remoteUpdateTime,
                    )
                )
            } else {
                database.update(
                    local.playlist.copy(
                        name = r.name,
                        browseId = local.playlist.browseId ?: accountId,
                        lastUpdateTime = remoteUpdateTime,
                    )
                )
            }

            // Replace the playlist's songs with the remote order (under the id the
            // playlist is stored with here, which may be the local one).
            database.clearPlaylist(playlistId)
            r.songs.forEachIndexed { index, s ->
                // Ensure the song + artist rows exist so the playlist renders.
                database.insert(
                    MediaMetadata(
                        id = s.id,
                        title = s.title,
                        artists = listOf(MediaMetadata.Artist(null, s.artist)),
                        duration = -1,
                        thumbnailUrl = s.thumbnail,
                    )
                )
                database.insert(
                    PlaylistSongMap(
                        playlistId = playlistId,
                        songId = s.id,
                        position = index,
                    )
                )
            }
        }
    }

    /**
     * A local playlist the arriving [r] stands for although neither id matches.
     *
     * What is left to recognise it by is the name and the songs: the peer imported
     * this phone's **own** playlist (so the row here carries no account id at all), or
     * the two devices each created the account copy of the same playlist (two account
     * ids, same name, and the songs of one contained in those of the other). It is the
     * same rule the desktop applies on its side (`PlaylistStore.samePlaylist`), and it
     * is what stops the pairing from importing a second copy of a playlist this app
     * already has (E1034).
     */
    private suspend fun sameContentPlaylist(r: SyncedPlaylist): Playlist? {
        if (r.songs.isEmpty()) return null
        val name = r.name.trim()
        if (name.isEmpty()) return null
        val songs = r.songs.map { it.id }.toSet()
        return database.playlistsByNameAsc().first().firstOrNull { candidate ->
            if (!candidate.playlist.name.trim().equals(name, ignoreCase = true)) return@firstOrNull false
            val local = database.playlistSongs(candidate.playlist.id).first()
                .map { it.song.song.id }.toSet()
            if (local.isEmpty()) return@firstOrNull false
            local == songs || local.containsAll(songs) || songs.containsAll(local)
        }
    }

    /**
     * Removes a local row this app holds for one of the account's **special**
     * playlists (liked songs, saved for later). They are not playlists: the liked
     * songs are this app's own Liked list and the saved-for-later ones its own row,
     * so a mirror of them imported from the desktop showed up as a second entry. The
     * playlists themselves are untouched on YouTube Music (nothing is deleted there).
     */
    private suspend fun dropSpecialPlaylist(accountId: String) {
        val rows = database.playlistsByNameAsc().first()
            .filter { it.playlist.browseId == accountId }
        for (row in rows) {
            Timber.d("DeviceSync: dropping the local copy of the account's '$accountId' list (${row.playlist.name})")
            database.delete(row.playlist)
        }
    }

    /**
     * Applies the peer's liked songs to the local library, last-write-wins per
     * song id by the entry's `updatedAt` (the same rule the playlists use).
     *
     * Two things this deliberately does **not** do:
     *
     *  - it never calls `YouTube.likeVideo`: the account belongs to the device
     *    the user actually tapped on (that side already wrote to it), and a
     *    snapshot must not like or unlike anything on YouTube Music;
     *  - an entry with no edit time (an older peer, or a plain
     *    [LibrarySnapshot.songIds] id) can only *add* a like — a removal is
     *    destructive and is never invented from data that carries no time.
     *
     * A like that arrives for a song this app has never seen is stored with the
     * metadata the desktop sent (title/artist/thumbnail), which is what makes it
     * show up in the Liked list instead of being a dangling id.
     */
    private suspend fun applyRemoteLikedSongs(lib: LibrarySnapshot) {
        // The entries are the authoritative form; the flat list is what an older
        // peer sends, and it is merged in without any time at all.
        val remote = LinkedHashMap<String, SyncedSong>()
        for (entry in lib.likedSongs) {
            if (entry.id.isNotBlank()) remote[entry.id] = entry
        }
        for (id in lib.songIds) {
            if (id.isNotBlank()) remote.putIfAbsent(id, SyncedSong(id = id))
        }
        if (remote.isEmpty()) return

        val now = LocalDateTime.now()
        for ((id, entry) in remote) {
            val local = database.getSongById(id)
            val likedAt = local?.song?.likedDate
                ?.toInstant(ZoneOffset.UTC)?.toEpochMilli() ?: 0L
            val tombstoneAt = likeTombstones[id] ?: 0L

            if (entry.deleted) {
                // Only a tombstone that is newer than both the local like and the
                // tombstone already held is a real removal.
                if (entry.updatedAt <= 0L) continue
                if (entry.updatedAt <= likedAt) continue
                if (entry.updatedAt <= tombstoneAt) continue
                likeTombstones[id] = entry.updatedAt
                if (local?.song?.liked == true) {
                    database.update(local.song.copy(liked = false, likedDate = null))
                }
                continue
            }

            val wins = if (entry.updatedAt > 0L) {
                entry.updatedAt > maxOf(likedAt, tombstoneAt)
            } else {
                local?.song?.liked != true && tombstoneAt == 0L
            }
            if (!wins) continue
            likeTombstones.remove(id)
            val likedDate = if (entry.updatedAt > 0L) {
                LocalDateTime.ofInstant(Instant.ofEpochMilli(entry.updatedAt), ZoneOffset.UTC)
            } else {
                now
            }
            if (local == null) {
                database.insert(
                    MediaMetadata(
                        id = id,
                        title = entry.title.ifBlank { id },
                        artists = entry.artist.takeIf { it.isNotBlank() }
                            ?.let { listOf(MediaMetadata.Artist(null, it)) }
                            .orEmpty(),
                        duration = -1,
                        thumbnailUrl = entry.thumbnail,
                    )
                ) { song ->
                    song.copy(liked = true, likedDate = likedDate)
                }
            } else {
                database.update(local.song.copy(liked = true, likedDate = likedDate))
            }
        }
    }

    private suspend fun pushCurrentSnapshot(settings: Map<String, String>? = null) {
        val current = client ?: return
        if (current.connectionState.value != SyncConnectionState.CONNECTED) return

        val prefs = context.dataStore.data.first()
        current.pushSnapshot(
            SyncSnapshot(
                deviceId = current.deviceId,
                deviceName = defaultDeviceName(),
                updatedAt = System.currentTimeMillis(),
                settings = settings ?: readSettings(prefs),
                playback = lastPlayback,
                library = lastLibrary,
            )
        )
    }

    // ---------------------------------------------------------------------
    // Shared settings mapping (key name <-> typed preference)
    // ---------------------------------------------------------------------

    private fun readSettings(prefs: Preferences): Map<String, String> = buildMap {
        // Theme
        put(DynamicThemeKey.name, (prefs[DynamicThemeKey] ?: true).toString())
        put(SelectedThemeColorKey.name, (prefs[SelectedThemeColorKey] ?: 0).toString())
        put(DarkModeKey.name, prefs[DarkModeKey] ?: "AUTO")
        put(PureBlackKey.name, (prefs[PureBlackKey] ?: false).toString())
        put(SelectedFontKey.name, prefs[SelectedFontKey] ?: "system")
        // Language / content
        put(AppLanguageKey.name, prefs[AppLanguageKey] ?: SYSTEM_DEFAULT)
        // Source markers for bidirectional language sync: the desktop peer only
        // applies a language whose (deviceId, seq) proves it is a newer manual
        // change made on the phone — never an echo or a stale pair-time push.
        put("languageDeviceId", prefs[DeviceSyncDeviceIdKey].orEmpty())
        put("languageSeq", (prefs[AppLanguageSeqKey] ?: 0L).toString())
        put(ContentLanguageKey.name, prefs[ContentLanguageKey] ?: SYSTEM_DEFAULT)
        put(ContentCountryKey.name, prefs[ContentCountryKey] ?: SYSTEM_DEFAULT)
        put(SuggestionRegionKey.name, prefs[SuggestionRegionKey] ?: "system")
        // Audio quality
        put(AudioQualityKey.name, prefs[AudioQualityKey] ?: AudioQuality.AUTO.name)
        put(AudioNormalizationKey.name, (prefs[AudioNormalizationKey] ?: false).toString())
        put(SaavnAudioQualityKey.name, prefs[SaavnAudioQualityKey] ?: SaavnAudioQuality.QUALITY_320.name)
        put(CrossfadeEnabledKey.name, (prefs[CrossfadeEnabledKey] ?: false).toString())
        put(CrossfadeDurationKey.name, (prefs[CrossfadeDurationKey] ?: 0f).toString())
        put(SkipSilenceKey.name, (prefs[SkipSilenceKey] ?: false).toString())
        put(SyncViviVolumeKey.name, (prefs[SyncViviVolumeKey] ?: true).toString())
        // Lyrics
        put(PreferredLyricsProviderKey.name, prefs[PreferredLyricsProviderKey] ?: PreferredLyricsProvider.LRCLIB.name)
        put(TranslateLyricsKey.name, (prefs[TranslateLyricsKey] ?: false).toString())
        put(TranslateLanguageKey.name, prefs[TranslateLanguageKey] ?: "en")
        put(LyricsRomanizeJapaneseKey.name, (prefs[LyricsRomanizeJapaneseKey] ?: false).toString())
        put(LyricsRomanizeKoreanKey.name, (prefs[LyricsRomanizeKoreanKey] ?: false).toString())
        put(LyricsRomanizeChineseKey.name, (prefs[LyricsRomanizeChineseKey] ?: false).toString())
        put(EnableKugouKey.name, (prefs[EnableKugouKey] ?: false).toString())
        put(EnableLrcLibKey.name, (prefs[EnableLrcLibKey] ?: false).toString())
        put(EnableMusixmatchKey.name, (prefs[EnableMusixmatchKey] ?: false).toString())
        put(EnableUnisonKey.name, (prefs[EnableUnisonKey] ?: false).toString())
        put(EnableYouLyPlusKey.name, (prefs[EnableYouLyPlusKey] ?: false).toString())
        put(EnablePaxsenixKey.name, (prefs[EnablePaxsenixKey] ?: false).toString())
        // Integrations
        put(EnableLastFMScrobblingKey.name, (prefs[EnableLastFMScrobblingKey] ?: false).toString())
        put(EnableDiscordRPCKey.name, (prefs[EnableDiscordRPCKey] ?: false).toString())
        put(EnableListenTogetherKey.name, (prefs[EnableListenTogetherKey] ?: false).toString())
    }

    private suspend fun applySetting(key: String, value: String) {
        context.dataStore.edit { prefs ->
            when (key) {
                DynamicThemeKey.name -> prefs[DynamicThemeKey] = value.toBooleanStrictOrNull() ?: return@edit
                SelectedThemeColorKey.name -> prefs[SelectedThemeColorKey] = value.toIntOrNull() ?: return@edit
                DarkModeKey.name -> prefs[DarkModeKey] = value
                PureBlackKey.name -> prefs[PureBlackKey] = value.toBooleanStrictOrNull() ?: return@edit
                SelectedFontKey.name -> prefs[SelectedFontKey] = value
                AppLanguageKey.name -> {
                    val ownId = prefs[DeviceSyncDeviceIdKey].orEmpty()
                    val legacy = pendingLangPeerId.isEmpty() || pendingLangPeerSeq <= 0L
                    val myEcho = !legacy && pendingLangPeerId == ownId
                    val lastPeerId = prefs[AppLanguagePeerDeviceKey].orEmpty()
                    val lastPeerSeq = prefs[AppLanguagePeerSeqKey] ?: 0L
                    val stale = !legacy && pendingLangPeerId == lastPeerId && pendingLangPeerSeq <= lastPeerSeq
                    if (legacy) {
                        // Old desktop build that sends no markers: mirror its
                        // explicit language only until a manual choice exists on
                        // the phone (then the phone's own push wins on the DE).
                        if ((prefs[AppLanguageSeqKey] ?: 0L) == 0L) prefs[AppLanguageKey] = value
                        return@edit
                    }
                    if (myEcho || stale) return@edit
                    prefs[AppLanguageKey] = value
                    prefs[AppLanguagePeerDeviceKey] = pendingLangPeerId
                    prefs[AppLanguagePeerSeqKey] = pendingLangPeerSeq
                }
                ContentLanguageKey.name -> prefs[ContentLanguageKey] = value
                ContentCountryKey.name -> prefs[ContentCountryKey] = value
                SuggestionRegionKey.name -> prefs[SuggestionRegionKey] = value
                AudioQualityKey.name -> prefs[AudioQualityKey] = value
                AudioNormalizationKey.name -> prefs[AudioNormalizationKey] = value.toBooleanStrictOrNull() ?: return@edit
                SaavnAudioQualityKey.name -> prefs[SaavnAudioQualityKey] = value
                CrossfadeEnabledKey.name -> prefs[CrossfadeEnabledKey] = value.toBooleanStrictOrNull() ?: return@edit
                CrossfadeDurationKey.name -> prefs[CrossfadeDurationKey] = value.toFloatOrNull() ?: return@edit
                SkipSilenceKey.name -> prefs[SkipSilenceKey] = value.toBooleanStrictOrNull() ?: return@edit
                SyncViviVolumeKey.name -> prefs[SyncViviVolumeKey] = value.toBooleanStrictOrNull() ?: return@edit
                PreferredLyricsProviderKey.name -> prefs[PreferredLyricsProviderKey] = value
                TranslateLyricsKey.name -> prefs[TranslateLyricsKey] = value.toBooleanStrictOrNull() ?: return@edit
                TranslateLanguageKey.name -> prefs[TranslateLanguageKey] = value
                LyricsRomanizeJapaneseKey.name -> prefs[LyricsRomanizeJapaneseKey] = value.toBooleanStrictOrNull() ?: return@edit
                LyricsRomanizeKoreanKey.name -> prefs[LyricsRomanizeKoreanKey] = value.toBooleanStrictOrNull() ?: return@edit
                LyricsRomanizeChineseKey.name -> prefs[LyricsRomanizeChineseKey] = value.toBooleanStrictOrNull() ?: return@edit
                EnableKugouKey.name -> prefs[EnableKugouKey] = value.toBooleanStrictOrNull() ?: return@edit
                EnableLrcLibKey.name -> prefs[EnableLrcLibKey] = value.toBooleanStrictOrNull() ?: return@edit
                EnableMusixmatchKey.name -> prefs[EnableMusixmatchKey] = value.toBooleanStrictOrNull() ?: return@edit
                EnableUnisonKey.name -> prefs[EnableUnisonKey] = value.toBooleanStrictOrNull() ?: return@edit
                EnableYouLyPlusKey.name -> prefs[EnableYouLyPlusKey] = value.toBooleanStrictOrNull() ?: return@edit
                EnablePaxsenixKey.name -> prefs[EnablePaxsenixKey] = value.toBooleanStrictOrNull() ?: return@edit
                EnableLastFMScrobblingKey.name -> prefs[EnableLastFMScrobblingKey] = value.toBooleanStrictOrNull() ?: return@edit
                EnableDiscordRPCKey.name -> prefs[EnableDiscordRPCKey] = value.toBooleanStrictOrNull() ?: return@edit
                EnableListenTogetherKey.name -> prefs[EnableListenTogetherKey] = value.toBooleanStrictOrNull() ?: return@edit
            }
        }
    }
}

/**
 * The account playlists that are not playlists: the liked songs and the ones saved
 * for later. The mobile app already filters exactly these two out of its own account
 * sync, and the desktop must not mirror them over as ordinary playlists either.
 */
private val SPECIAL_ACCOUNT_PLAYLIST_IDS = setOf("LM", "SE")

/** Intermediate library state used to hand the flows over to the suspend collector. */
private data class PlaylistLibraryInput(
    // The whole rows, not just the ids: the liked entries the desktop receives
    // need each song's own `likedDate` as their edit time.
    val songs: List<Song>,
    val albumIds: List<String>,
    val artistIds: List<String>,
    val playlists: List<Playlist>,
)
