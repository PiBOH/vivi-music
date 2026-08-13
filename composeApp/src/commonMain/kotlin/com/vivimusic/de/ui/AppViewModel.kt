package com.vivimusic.de.ui

import com.vivimusic.de.data.MusicRepository
import com.vivimusic.de.data.sync.SyncManager
import com.vivimusic.de.data.sync.SyncStatus
import com.vivimusic.de.domain.Album
import com.vivimusic.de.domain.HomeSection
import com.vivimusic.de.domain.Playlist
import com.vivimusic.de.domain.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Holds the UI state and exposes the repository/sync operations to the Compose
 * screens. Kept intentionally simple: no DI or ViewModel framework.
 */
class AppViewModel(
    private val repository: MusicRepository,
    syncManager: SyncManager,
    private val scope: CoroutineScope,
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

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _album = MutableStateFlow<Album?>(null)
    val album: StateFlow<Album?> = _album.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init {
        loadHome()
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
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        scope.launch {
            _loading.value = true
            _searchResults.value = repository.search(query)
            _loading.value = false
        }
    }

    fun play(song: Song) {
        scope.launch {
            _currentSong.value = repository.getSong(song.id) ?: song
            repository.recordPlay(song)
        }
    }

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
