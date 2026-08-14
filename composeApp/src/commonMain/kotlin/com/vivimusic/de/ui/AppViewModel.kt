package com.vivimusic.de.ui

import com.vivimusic.de.data.MusicRepository
import com.vivimusic.de.data.YTM_COOKIE_KEY
import com.vivimusic.de.data.YTM_EMAIL_KEY
import com.vivimusic.de.data.YTM_HANDLE_KEY
import com.vivimusic.de.data.YTM_NAME_KEY
import com.vivimusic.de.data.YTM_THUMB_KEY
import com.vivimusic.de.data.lyrics.LyricsLine
import com.vivimusic.de.data.network.extractYtCookie
import com.vivimusic.de.data.network.hasSapisidCookie
import com.vivimusic.de.data.network.validateYtCookie
import com.vivimusic.de.data.playback.AudioEngine
import com.vivimusic.de.data.playback.PlaybackState
import com.vivimusic.de.data.readSetting
import com.vivimusic.de.data.sync.SyncManager
import com.vivimusic.de.data.sync.SyncStatus
import com.vivimusic.de.data.update.AppRelease
import com.vivimusic.de.data.update.UpdateChecker
import com.vivimusic.de.data.update.UpdateCleanupState
import com.vivimusic.de.data.update.UpdateDownloadState
import com.vivimusic.de.data.update.UpdateStatus
import com.vivimusic.de.data.update.cleanupDownloadedUpdates as cleanupUpdateFiles
import com.vivimusic.de.data.writeSetting
import com.vivimusic.de.domain.AccountInfo
import com.vivimusic.de.domain.Album
import com.vivimusic.de.domain.Artist
import com.vivimusic.de.domain.BrowseData
import com.vivimusic.de.domain.ChartSection
import com.vivimusic.de.domain.ExploreData
import com.vivimusic.de.domain.HomeSection
import com.vivimusic.de.domain.LibraryItem
import com.vivimusic.de.domain.MoodGenreSection
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

    // ----- Explore / Charts / New releases / Moods & genres / Browse -----

    private val _explore = MutableStateFlow<ExploreData?>(null)
    val explore: StateFlow<ExploreData?> = _explore.asStateFlow()

    private val _charts = MutableStateFlow<List<ChartSection>>(emptyList())
    val charts: StateFlow<List<ChartSection>> = _charts.asStateFlow()

    private val _newReleaseAlbums = MutableStateFlow<List<Album>>(emptyList())
    val newReleaseAlbums: StateFlow<List<Album>> = _newReleaseAlbums.asStateFlow()

    private val _moodGenres = MutableStateFlow<List<MoodGenreSection>>(emptyList())
    val moodGenres: StateFlow<List<MoodGenreSection>> = _moodGenres.asStateFlow()

    private val _browse = MutableStateFlow<BrowseData?>(null)
    val browse: StateFlow<BrowseData?> = _browse.asStateFlow()

    private val _exploreLoading = MutableStateFlow(false)
    val exploreLoading: StateFlow<Boolean> = _exploreLoading.asStateFlow()

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

    // YouTube Music account (signed-in library), driven by the InnerTube cookie.
    private val _ytAccountInfo = MutableStateFlow(loadYtAccount())
    val ytAccountInfo: StateFlow<AccountInfo?> = _ytAccountInfo.asStateFlow()

    private val _ytSignedIn = MutableStateFlow(cookieHasSapisid(readSetting(YTM_COOKIE_KEY)))
    val ytSignedIn: StateFlow<Boolean> = _ytSignedIn.asStateFlow()

    private val _ytLibrary = MutableStateFlow<List<LibraryItem>>(emptyList())
    val ytLibrary: StateFlow<List<LibraryItem>> = _ytLibrary.asStateFlow()

    private val _ytLibraryLoading = MutableStateFlow(false)
    val ytLibraryLoading: StateFlow<Boolean> = _ytLibraryLoading.asStateFlow()

    // Listening statistics derived from the local history.
    val listeningStats: StateFlow<ListeningStats> = history.map { songs ->
        ListeningStats(
            totalPlayTimeMs = songs.sumOf { it.durationMs ?: 0L },
            uniqueSongs = songs.distinctBy { it.id }.size,
            uniqueArtists = songs.map { it.artist }.filter { it.isNotBlank() }.distinct().size,
            uniqueAlbums = songs.map { it.album }.filter { it.isNotBlank() }.distinct().size,
        )
    }.stateIn(scope, SharingStarted.Eagerly, ListeningStats())

    /** Most listened artists, ranked by how many history songs they appear in. */
    val topArtists: StateFlow<List<Pair<String, Int>>> = history.map { songs ->
        songs.filter { it.artist.isNotBlank() }
            .groupingBy { it.artist }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .map { it.key to it.value }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

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

    private val _checkingForUpdates = MutableStateFlow(false)
    val checkingForUpdates: StateFlow<Boolean> = _checkingForUpdates.asStateFlow()

    private val _updateDownloadState = MutableStateFlow<UpdateDownloadState>(UpdateDownloadState.Idle)
    val updateDownloadState: StateFlow<UpdateDownloadState> = _updateDownloadState.asStateFlow()

    private val _updateCleanupState = MutableStateFlow<UpdateCleanupState>(UpdateCleanupState.Idle)
    val updateCleanupState: StateFlow<UpdateCleanupState> = _updateCleanupState.asStateFlow()

    /**
     * Kicks off the initial (network) work. Called after the first frame is
     * composed so the window opens instantly instead of competing with these
     * coroutines for the first frame.
     */
    fun loadInitialData() {
        loadHome()
        checkForUpdates()
        refreshAuthState()
        if (_ytSignedIn.value) loadYtLibrary()
    }

    fun setCheckPrereleases(enabled: Boolean) {
        _checkPrereleases.value = enabled
        writeSetting(UPDATE_CHECK_PRERELEASES_KEY, enabled.toString())
        checkForUpdates()
    }

    fun checkForUpdates() {
        if (_checkingForUpdates.value) return
        scope.launch {
            _checkingForUpdates.value = true
            try {
                _updateStatus.value = updateChecker.check(includePrereleases = _checkPrereleases.value)
            } finally {
                _checkingForUpdates.value = false
            }
        }
    }

    fun downloadAndInstallUpdate(release: AppRelease) {
        if (_updateDownloadState.value is UpdateDownloadState.Downloading) return
        scope.launch {
            _updateDownloadState.value = UpdateDownloadState.Downloading()
            _updateDownloadState.value = try {
                UpdateDownloadState.Launched(
                    updateChecker.downloadAndLaunch(release) { progress ->
                        _updateDownloadState.value = UpdateDownloadState.Downloading(progress)
                    },
                )
            } catch (t: Throwable) {
                UpdateDownloadState.Error(fullErrorDetails("Update download failed", t))
            }
        }
    }

    fun resetUpdateDownloadState() {
        _updateDownloadState.value = UpdateDownloadState.Idle
    }

    fun cleanupDownloadedUpdates() {
        if (_updateCleanupState.value is UpdateCleanupState.Cleaning) return
        scope.launch {
            _updateCleanupState.value = UpdateCleanupState.Cleaning
            _updateCleanupState.value = try {
                UpdateCleanupState.Completed(cleanupUpdateFiles())
            } catch (t: Throwable) {
                UpdateCleanupState.Error(fullErrorDetails("Update cleanup failed", t))
            }
        }
    }

    /** Fetches the raw Keep-a-Changelog markdown from the repository. */
    suspend fun fetchChangelogMarkdown(): String = updateChecker.fetchChangelogMarkdown()

    fun loadHome() {
        scope.launch {
            _loading.value = true
            _homeSections.value = repository.home()
            _loading.value = false
        }
    }

    /** Loads the Explore screen payload once (new releases + mood/genre tiles). */
    fun loadExplore() {
        if (_explore.value != null) return
        scope.launch {
            _exploreLoading.value = true
            _explore.value = repository.explore()
            _exploreLoading.value = false
        }
    }

    fun loadCharts() {
        if (_charts.value.isNotEmpty()) return
        scope.launch {
            _exploreLoading.value = true
            _charts.value = repository.charts()
            _exploreLoading.value = false
        }
    }

    fun loadNewReleases() {
        if (_newReleaseAlbums.value.isNotEmpty()) return
        scope.launch {
            _exploreLoading.value = true
            _newReleaseAlbums.value = repository.newReleaseAlbums()
            _exploreLoading.value = false
        }
    }

    fun loadMoodGenres() {
        if (_moodGenres.value.isNotEmpty()) return
        scope.launch {
            _exploreLoading.value = true
            _moodGenres.value = repository.moodGenres()
            _exploreLoading.value = false
        }
    }

    fun openBrowse(browseId: String, params: String?) {
        scope.launch {
            _exploreLoading.value = true
            _browse.value = repository.browse(browseId, params)
            _exploreLoading.value = false
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
                AuthState.Error(fullErrorDetails("Sign in failed", t))
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
                AuthState.Error(fullErrorDetails("Sign up failed", t))
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

    // ----- YouTube Music account (cookie-based) -----

    /**
     * Validates a pasted cookie against the account endpoint, persists it and
     * loads the account library. [onResult] receives null on success or an
     * error message on failure.
     */
    fun signInYt(cookieInput: String, onResult: (String?) -> Unit) {
        scope.launch {
            try {
                val cookie = extractYtCookie(cookieInput)
                validateYtCookie(cookieInput)?.let { validationError ->
                    onResult("Invalid YouTube Music cookie: $validationError")
                    return@launch
                }
                repository.setYtCookie(cookie)
                val info = repository.accountInfo()
                if (info == null) {
                    repository.setYtCookie(null)
                    onResult("Invalid cookie")
                    return@launch
                }
                writeSetting(YTM_COOKIE_KEY, cookie)
                writeSetting(YTM_NAME_KEY, info.name)
                writeSetting(YTM_EMAIL_KEY, info.email.orEmpty())
                writeSetting(YTM_HANDLE_KEY, info.channelHandle.orEmpty())
                writeSetting(YTM_THUMB_KEY, info.thumbnailUrl.orEmpty())
                _ytAccountInfo.value = info
                _ytSignedIn.value = true
                onResult(null)
                loadYtLibrary()
            } catch (t: Throwable) {
                repository.setYtCookie(null)
                onResult(fullErrorDetails("YouTube Music sign in failed", t))
            }
        }
    }

    fun signOutYt() {
        repository.setYtCookie(null)
        _ytAccountInfo.value = null
        _ytSignedIn.value = false
        _ytLibrary.value = emptyList()
        writeSetting(YTM_COOKIE_KEY, "")
        writeSetting(YTM_NAME_KEY, "")
        writeSetting(YTM_EMAIL_KEY, "")
        writeSetting(YTM_HANDLE_KEY, "")
        writeSetting(YTM_THUMB_KEY, "")
    }

    /** Loads the liked playlists, albums and artists for the signed-in account. */
    fun loadYtLibrary() {
        scope.launch {
            _ytLibraryLoading.value = true
            try {
                val playlists = repository.libraryPlaylists()
                val albums = repository.libraryAlbums()
                val artists = repository.libraryArtists()
                _ytLibrary.value = playlists + albums + artists
            } catch (t: Throwable) {
                // The library is best-effort: keep whatever we already had.
            } finally {
                _ytLibraryLoading.value = false
            }
        }
    }
}

private fun fullErrorDetails(context: String, throwable: Throwable): String = buildString {
    append(context)
    append(": ")
    append(throwable.message ?: "No error message")
    appendLine()
    appendLine()
    append(throwable.stackTraceToString())
}

private fun cookieHasSapisid(cookie: String?): Boolean =
    cookie?.let { hasSapisidCookie(extractYtCookie(it)) } == true

private fun loadYtAccount(): AccountInfo? {
    val name = readSetting(YTM_NAME_KEY)?.takeIf { it.isNotBlank() } ?: return null
    return AccountInfo(
        name = name,
        email = readSetting(YTM_EMAIL_KEY)?.takeIf { it.isNotBlank() },
        channelHandle = readSetting(YTM_HANDLE_KEY)?.takeIf { it.isNotBlank() },
        thumbnailUrl = readSetting(YTM_THUMB_KEY)?.takeIf { it.isNotBlank() },
    )
}
