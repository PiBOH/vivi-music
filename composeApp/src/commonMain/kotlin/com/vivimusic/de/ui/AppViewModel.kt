package com.vivimusic.de.ui

import com.vivimusic.de.data.MusicRepository
import com.vivimusic.de.data.playback.AudioEngine
import com.vivimusic.de.data.playback.PlaybackState
import com.vivimusic.de.data.readSetting
import com.vivimusic.de.data.sync.SyncManager
import com.vivimusic.de.data.sync.SyncStatus
import com.vivimusic.de.data.update.UpdateChecker
import com.vivimusic.de.data.update.UpdateStatus
import com.vivimusic.de.data.writeSetting
import com.vivimusic.de.domain.Album
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

private const val UPDATE_CHECK_PRERELEASES_KEY = "update.check_prereleases"

/**
 * Holds the UI state and exposes the repository/sync operations to the Compose
 * screens. Kept intentionally simple: no DI or ViewModel framework.
 */
class AppViewModel(
    private val repository: MusicRepository,
    syncManager: SyncManager,
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

    private val _album = MutableStateFlow<Album?>(null)
    val album: StateFlow<Album?> = _album.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _checkPrereleases = MutableStateFlow(readSetting(UPDATE_CHECK_PRERELEASES_KEY) == "true")
    val checkPrereleases: StateFlow<Boolean> = _checkPrereleases.asStateFlow()

    private val _updateStatus = MutableStateFlow<UpdateStatus?>(null)
    val updateStatus: StateFlow<UpdateStatus?> = _updateStatus.asStateFlow()

    init {
        loadHome()
        checkForUpdates()
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

    fun play(song: Song) {
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

    fun closeAlbum() {
        _album.value = null
    }

    fun toggleFavorite(song: Song) = repository.toggleFavorite(song)

    fun isFavorite(songId: String): Flow<Boolean> = repository.isFavorite(songId)

    fun createPlaylist(name: String) = repository.createPlaylist(name)

    fun deletePlaylist(id: String) = repository.deletePlaylist(id)

    fun syncNow() = repository.syncNow()
}
