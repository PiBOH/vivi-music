package com.vivimusic.de.ui

import com.vivimusic.de.data.MusicRepository
import com.vivimusic.de.data.lyrics.LyricsLine
import com.vivimusic.de.data.playback.AudioEngine
import com.vivimusic.de.data.playback.PlaybackState
import com.vivimusic.de.data.readSetting
import com.vivimusic.de.data.sync.SyncManager
import com.vivimusic.de.data.sync.SyncStatus
import com.vivimusic.de.data.update.UpdateChecker
import com.vivimusic.de.data.update.UpdateStatus
import com.vivimusic.de.data.writeSetting
import com.vivimusic.de.domain.Album
import com.vivimusic.de.domain.Artist
import com.vivimusic.de.domain.HomeSection
import com.vivimusic.de.domain.Playlist
import com.vivimusic.de.domain.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

private const val UPDATE_CHECK_PRERELEASES_KEY = "update.check_prereleases"
private const val SYNC_ENABLED_KEY = "sync.enabled"

enum class RepeatMode { Off, All, One }

sealed interface AuthState {
    data object Checking : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val email: String) : AuthState
    data class Error(val message: String) : AuthState
}

data class ListeningStats(
    val totalPlayTimeMs: Long = 0L,
    val uniqueSongs: Int = 0,
    val uniqueArtists: Int = 0,
    val uniqueAlbums: Int = 0,
)

/**
 * Holds the UI state and exposes the repository/sync operations to the Compose
 * screens. Kept intentionally simple: no DI or ViewModel framework.
 */
class AppViewModel(
    private val repository: MusicRepository,
    private val syncManager: SyncManager,
    private val scope: CoroutineScope,
    private val audioEngine: AudioEngine,
    private val updateChecker: UpdateChecker,
) {
    val favorites: StateFlow<List<Song>> =
        repository.observeFavorites().stateIn(scope, SharingStarted.Eagerly, emptyList())

    val history: StateFlow<List<Song>> =
        repository.observeHistory().stateIn(scope, SharingStarted.Eagerly, emptyList())

    val playlists: StateFlow<List<Playlist>> =
        repository.observePlaylists().stateIn(scope, SharingStarted.Eagerly, emptyList())

    val syncStatus: StateFlow<SyncStatus> = syncManager.status

    /** True when a Supabase backend is configured (auth + sync available). */
    val syncConfigured: Boolean get() = syncManager.isConfigured

    private val _syncEnabled = MutableStateFlow(readSetting(SYNC_ENABLED_KEY) != "false")
    val syncEnabled: StateFlow<Boolean> = _syncEnabled.asStateFlow()

    init {
        syncManager.enabled = _syncEnabled.value
    }

    private val _homeSections = MutableStateFlow<List<HomeSection>>(emptyList())
    val homeSections: StateFlow<List<HomeSection>> = _homeSections.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Song>>(emptyList())
    val searchResults: StateFlow<List<Song>> = _searchResults.asStateFlow()

    private val _searchSuggestions = MutableStateFlow<List<String>>(emptyList())
    val searchSuggestions: StateFlow<List<String>> = _searchSuggestions.asStateFlow()

    val searchHistory: StateFlow<List<String>> =
        repository.observeSearchHistory().stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var searchJob: Job? = null

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    // Playback state, driven by the real audio engine.
    val playbackState: StateFlow<PlaybackState> = audioEngine.state

    val isPlaying: StateFlow<Boolean> =
        playbackState.map { it.isPlaying }.stateIn(scope, SharingStarted.Eagerly, false)

    // Play queue state.
    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _queueIndex = MutableStateFlow(-1)
    val queueIndex: StateFlow<Int> = _queueIndex.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.Off)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    // Lyrics for the current song.
    private val _lyrics = MutableStateFlow<List<LyricsLine>>(emptyList())
    val lyrics: StateFlow<List<LyricsLine>> = _lyrics.asStateFlow()

    private val _lyricsLoading = MutableStateFlow(false)
    val lyricsLoading: StateFlow<Boolean> = _lyricsLoading.asStateFlow()

    // Authentication state, driven by the Supabase sync backend.
    private val _authState = MutableStateFlow<AuthState>(AuthState.Checking)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // Listening statistics derived from the local history.
    val listeningStats: StateFlow<ListeningStats> = history.map { songs ->
        ListeningStats(
            totalPlayTimeMs = songs.sumOf { it.durationMs ?: 0L },
            uniqueSongs = songs.distinctBy { it.id }.size,
            uniqueArtists = songs.map { it.artist }.filter { it.isNotBlank() }.distinct().size,
            uniqueAlbums = songs.map { it.album }.filter { it.isNotBlank() }.distinct().size,
        )
    }.stateIn(scope, SharingStarted.Eagerly, ListeningStats())

    private val _album = MutableStateFlow<Album?>(null)
    val album: StateFlow<Album?> = _album.asStateFlow()

    private val _artist = MutableStateFlow<Artist?>(null)
    val artist: StateFlow<Artist?> = _artist.asStateFlow()

    private val _playlist = MutableStateFlow<Playlist?>(null)
    val playlist: StateFlow<Playlist?> = _playlist.asStateFlow()

    private val _playlistSongs = MutableStateFlow<List<Song>>(emptyList())
    val playlistSongs: StateFlow<List<Song>> = _playlistSongs.asStateFlow()

    private var playlistSongsJob: Job? = null

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _checkPrereleases = MutableStateFlow(readSetting(UPDATE_CHECK_PRERELEASES_KEY) == "true")
    val checkPrereleases: StateFlow<Boolean> = _checkPrereleases.asStateFlow()

    private val _updateStatus = MutableStateFlow<UpdateStatus?>(null)
    val updateStatus: StateFlow<UpdateStatus?> = _updateStatus.asStateFlow()

    init {
        loadHome()
        checkForUpdates()
        refreshAuthState()
    }

    fun setCheckPrereleases(enabled: Boolean) {
        _checkPrereleases.value = enabled
        writeSetting(UPDATE_CHECK_PRERELEASES_KEY, enabled.toString())
        checkForUpdates()
    }

    fun checkForUpdates() {
        scope.launch {
            _updateStatus.value = updateChecker.check(includePrereleases = _checkPrereleases.value)
        }
    }

    fun loadHome() {
        scope.launch {
            _loading.value = true
            _homeSections.value = repository.home()
            _loading.value = false
        }
    }

    fun onQueryChange(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _searchSuggestions.value = emptyList()
            return
        }
        searchJob = scope.launch {
            // Debounce live suggestions/results while the user is typing.
            delay(300)
            _loading.value = true
            _searchSuggestions.value = repository.searchSuggestions(query)
            _searchResults.value = repository.search(query)
            _loading.value = false
        }
    }

    /** Submits a search (records history and runs the full search). */
    fun submitSearch(query: String) {
        if (query.isBlank()) return
        _searchQuery.value = query
        repository.addSearchHistory(query)
        searchJob?.cancel()
        searchJob = scope.launch {
            _loading.value = true
            _searchResults.value = repository.search(query)
            _loading.value = false
        }
    }

    fun deleteSearchHistory(query: String) = repository.deleteSearchHistory(query)

    /** Plays a single song, resetting the queue to just that song. */
    fun play(song: Song) {
        _queue.value = listOf(song)
        _queueIndex.value = 0
        playInternal(song)
    }

    /** Replaces the queue with [songs] and plays [startIndex]. */
    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        _queue.value = songs
        val index = startIndex.coerceIn(0, songs.lastIndex)
        _queueIndex.value = index
        playInternal(songs[index])
    }

    /** Plays [song] in the context of [songs] (used when tapping a list row). */
    fun playInQueue(song: Song, songs: List<Song>) {
        if (songs.isEmpty()) {
            play(song)
            return
        }
        _queue.value = songs
        val index = songs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        _queueIndex.value = index
        playInternal(songs[index])
    }

    fun enqueue(song: Song) {
        _queue.value = _queue.value + song
    }

    fun playAt(index: Int) {
        val songs = _queue.value
        if (index !in songs.indices) return
        _queueIndex.value = index
        playInternal(songs[index])
    }

    fun playNext() {
        val songs = _queue.value
        if (songs.isEmpty()) return
        val index = when {
            _repeatMode.value == RepeatMode.One -> _queueIndex.value.coerceIn(0, songs.lastIndex)
            _shuffleEnabled.value -> Random.nextInt(songs.size)
            _queueIndex.value < songs.lastIndex -> _queueIndex.value + 1
            _repeatMode.value == RepeatMode.All -> 0
            else -> _queueIndex.value.coerceIn(0, songs.lastIndex)
        }
        _queueIndex.value = index
        playInternal(songs[index])
    }

    fun playPrevious() {
        val songs = _queue.value
        if (songs.isEmpty()) return
        val index = (_queueIndex.value - 1).coerceAtLeast(0)
        _queueIndex.value = index
        playInternal(songs[index])
    }

    fun removeFromQueue(index: Int) {
        val songs = _queue.value.toMutableList()
        if (index !in songs.indices) return
        songs.removeAt(index)
        _queue.value = songs
        _queueIndex.value = when {
            songs.isEmpty() -> -1
            index < _queueIndex.value -> _queueIndex.value - 1
            index == _queueIndex.value -> index.coerceIn(0, songs.lastIndex)
            else -> _queueIndex.value
        }
    }

    fun clearQueue() {
        _queue.value = emptyList()
        _queueIndex.value = -1
    }

    fun toggleShuffle() {
        _shuffleEnabled.value = !_shuffleEnabled.value
    }

    fun cycleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.Off -> RepeatMode.All
            RepeatMode.All -> RepeatMode.One
            RepeatMode.One -> RepeatMode.Off
        }
    }

    fun loadLyrics(song: Song) {
        _lyricsLoading.value = true
        _lyrics.value = emptyList()
        scope.launch {
            _lyrics.value = repository.getLyrics(song)
            _lyricsLoading.value = false
        }
    }

    private fun playInternal(song: Song) {
        scope.launch {
            val full = repository.getSong(song.id) ?: song
            _currentSong.value = full
            repository.recordPlay(full)
            val url = full.streamUrl
            if (url.isNullOrBlank()) {
                audioEngine.stop()
            } else {
                audioEngine.play(songId = full.id, url = url, durationMs = full.durationMs ?: 0L)
            }
            loadLyrics(full)
        }
    }

    fun togglePlayPause() {
        if (_currentSong.value != null) {
            audioEngine.toggle()
        }
    }

    fun seekTo(positionMs: Long) = audioEngine.seekTo(positionMs)

    fun openAlbum(browseId: String) {
        scope.launch {
            _album.value = repository.getAlbumOrPlaylist(browseId)
        }
    }

    /** Shows a locally derived album (from favorites) without a remote fetch. */
    fun showLocalAlbum(album: Album) {
        _album.value = album
    }

    fun closeAlbum() {
        _album.value = null
    }

    fun openArtist(browseId: String) {
        scope.launch {
            _artist.value = repository.getArtist(browseId)
        }
    }

    /** Shows a locally derived artist (from favorites) without a remote fetch. */
    fun showLocalArtist(artist: Artist) {
        _artist.value = artist
    }

    fun closeArtist() {
        _artist.value = null
    }

    fun openPlaylist(playlist: Playlist) {
        _playlist.value = playlist
        _playlistSongs.value = emptyList()
        playlistSongsJob?.cancel()
        playlistSongsJob = scope.launch {
            repository.observePlaylistSongs(playlist.id).collect { songs ->
                _playlistSongs.value = songs
            }
        }
    }

    fun closePlaylist() {
        _playlist.value = null
        _playlistSongs.value = emptyList()
    }

    fun addToPlaylist(song: Song) {
        val playlist = _playlist.value ?: return
        repository.addSongToPlaylist(playlist.id, song)
    }

    fun removeFromPlaylist(song: Song) {
        val playlist = _playlist.value ?: return
        repository.removeSongFromPlaylist(playlist.id, song.id)
    }

    fun toggleFavorite(song: Song) = repository.toggleFavorite(song)

    fun isFavorite(songId: String): Flow<Boolean> = repository.isFavorite(songId)

    fun createPlaylist(name: String) = repository.createPlaylist(name)

    fun deletePlaylist(id: String) = repository.deletePlaylist(id)

    fun setSyncEnabled(enabled: Boolean) {
        _syncEnabled.value = enabled
        syncManager.applyEnabled(enabled)
        writeSetting(SYNC_ENABLED_KEY, enabled.toString())
    }

    fun clearSearchHistory() = repository.clearSearchHistory()

    fun clearHistory() = repository.clearHistory()

    fun syncNow() = repository.syncNow()

    // ----- account / auth -----

    fun refreshAuthState() {
        _authState.value = AuthState.Checking
        scope.launch {
            _authState.value = try {
                val email = syncManager.currentUserEmail()
                if (email != null) AuthState.SignedIn(email) else AuthState.SignedOut
            } catch (t: Throwable) {
                AuthState.SignedOut
            }
        }
    }

    fun signIn(email: String, password: String) {
        if (!syncManager.isConfigured) {
            _authState.value = AuthState.Error("Synchronization is not configured")
            return
        }
        _authState.value = AuthState.Checking
        scope.launch {
            _authState.value = try {
                syncManager.signIn(email, password)
                val user = syncManager.currentUserEmail() ?: email
                AuthState.SignedIn(user)
            } catch (t: Throwable) {
                AuthState.Error(t.message ?: "Sign in failed")
            }
        }
    }

    fun signUp(email: String, password: String) {
        if (!syncManager.isConfigured) {
            _authState.value = AuthState.Error("Synchronization is not configured")
            return
        }
        _authState.value = AuthState.Checking
        scope.launch {
            _authState.value = try {
                syncManager.signUp(email, password)
                val user = syncManager.currentUserEmail() ?: email
                AuthState.SignedIn(user)
            } catch (t: Throwable) {
                AuthState.Error(t.message ?: "Sign up failed")
            }
        }
    }

    fun signOut() {
        scope.launch {
            try {
                syncManager.signOut()
            } finally {
                _authState.value = AuthState.SignedOut
            }
        }
    }
}
