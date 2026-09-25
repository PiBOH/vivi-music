package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.OutlinedButton
import com.music.innertube.YouTube
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.YTItem
import com.music.innertube.pages.LibraryPage
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

/** The account's saved-artists page (a library corpus, not a normal browse). */
const val ARTISTS_BROWSE_ID = "FEmusic_library_corpus_artists"

/**
 * "The library changed" — a counter the Library screen watches.
 *
 * The screen loads a page and then sits on it: liking a song (from the player,
 * a list row or the paired phone), removing one, or editing a playlist changed
 * the account but nothing told the screen, so the new song only appeared after
 * leaving and reopening it. [bump] is called by the places that change the
 * library and the screen re-fetches the page it is showing.
 */
object LibraryRefresh {
    private val _revision = MutableStateFlow(0)
    val revision: StateFlow<Int> = _revision.asStateFlow()

    fun bump() {
        _revision.value += 1
    }
}

/**
 * The desktop's artist list, cached on disk.
 *
 * This is the desktop counterpart of the artists table of the mobile database:
 * mobile's Artists screen does **not** read a server list, it reads the artists
 * its database collected from the songs it has seen (the *Library* filter) or
 * the account's followed ones (the *Liked* filter). The desktop has no such
 * table, so the list it builds is kept here: it survives a restart, it is there
 * before the first network answer and it is what is shown when the account has
 * nothing saved.
 */
object ArtistsStore {
    private val json = sharedJsonPretty

    private val file = File(System.getProperty("user.home"), ".vivimusic/artists.json").apply {
        parentFile?.mkdirs()
    }

    private val _all = MutableStateFlow(load())

    val all: StateFlow<List<ArtistItem>> = _all.asStateFlow()

    /** Merges [artists] into the cache (one entry per channel id) and persists. */
    fun cache(artists: List<ArtistItem>) {
        if (artists.isEmpty()) return
        _all.value = (_all.value + artists)
            .associateBy { it.id }
            .values
            .sortedBy { it.title.lowercase() }
        persist()
    }

    private fun load(): List<ArtistItem> = try {
        if (file.exists()) {
            json.decodeFromString<List<CachedArtist>>(file.readText()).map { it.toItem() }
        } else {
            emptyList()
        }
    } catch (_: Exception) {
        emptyList()
    }

    private fun persist() {
        try {
            file.writeText(json.encodeToString(_all.value.map { CachedArtist(it.id, it.title, it.thumbnail) }))
        } catch (_: Exception) {
            // best-effort
        }
    }

    /** One cached artist: id, name and picture are all the grid needs to draw. */
    @Serializable
    private data class CachedArtist(
        val id: String,
        val title: String,
        val thumbnail: String? = null,
    ) {
        fun toItem(): ArtistItem = ArtistItem(
            id = id,
            title = title,
            thumbnail = thumbnail,
            shuffleEndpoint = null,
            radioEndpoint = null,
        )
    }
}

/**
 * One library page, ready to render.
 *
 * The saved-artists corpus is the one page YouTube Music does not always fill:
 * an account that has saved songs but has never followed an artist gets an
 * **empty** page back — the request succeeds, the response carries no items, and
 * the screen has nothing to draw (which is exactly how the Artists screen
 * stayed empty, while `browse.log` only ever said "1 section(s), 0 item(s)").
 * The artists page is then answered by [libraryArtists] instead.
 *
 * The shape of every response is logged, so a support zip can tell "the account
 * has nothing saved" from "the parser did not understand the page".
 */
suspend fun loadLibraryPage(browseId: String): Result<LibraryPage> {
    val result = YouTube.library(browseId)
    val page = result.getOrNull()
    AppLog.log(
        "browse",
        "library $browseId → ${page?.items?.size ?: 0} item(s), shape=${page?.shape ?: "-"}" +
            (result.exceptionOrNull()?.let { " — ${it.message}" } ?: ""),
    )
    // Every other library page is what the server returned, filtered.
    if (browseId != ARTISTS_BROWSE_ID) return result.map { it.copy(items = it.items.filteredContent()) }

    // The artists tab answers from the account's songs and playlists even when
    // the corpus request itself failed: a request that comes back empty (or
    // errors) used to leave the tab empty or on the error box while the rest of
    // the library was browsable.
    val artists = libraryArtists(page?.items.orEmpty())
    if (artists.isEmpty()) return result.map { it.copy(items = it.items.filteredContent()) }
    return Result.success(
        LibraryPage(items = artists, continuation = null, shape = "artists:${artists.size}"),
    )
}

/**
 * The artists to draw, in the mobile app's *Library* sense: the ones the account
 * follows, or — when it follows none — the ones heard in its songs.
 *
 * The corpus page is the *Liked* filter of mobile's Artists screen and it is
 * genuinely empty for an account that follows no artist (the user's does: the
 * log says `library FEmusic_library_corpus_artists → 0 item(s)`). Mobile still
 * has a list in that case because its screen reads the *artists table* of its
 * database, which is filled from the songs it has seen — the account's saved
 * songs **and** its playlists. The desktop has no such table, so the equivalent
 * is derived from every song list it can reach and cached by [ArtistsStore]
 * (which is also what is shown offline).
 *
 * The old derivation read the liked-songs page only. For an account whose liked
 * list is empty (or whose request comes back empty) that page yields nothing, so
 * the screen fell back to whatever the cache happened to hold — one artist, from
 * a single lucky page in an older session ("the Artists screen always shows only
 * one artist"). The account's **playlists** are the reliable source: they hold
 * the songs the user actually keeps, and every song carries its artists' channel
 * ids, so those artists open like any other.
 */
private suspend fun libraryArtists(corpusItems: List<YTItem>): List<ArtistItem> {
    val corpus = corpusItems.filterIsInstance<ArtistItem>()
    if (corpus.isNotEmpty()) {
        ArtistsStore.cache(corpus)
        return corpus
    }
    val derived = LinkedHashMap<String, ArtistItem>()
    artistsFromSavedSongs().forEach { derived.putIfAbsent(it.id, it) }
    val fromLibrary = derived.size
    artistsFromPlaylists(derived)
    val fromPlaylists = derived.size - fromLibrary
    if (derived.isNotEmpty()) {
        ArtistsStore.cache(derived.values.toList())
        AppLog.log(
            "browse",
            "artists corpus empty — derived ${derived.size} artist(s) " +
                "($fromLibrary from the saved songs, $fromPlaylists from the account's playlists)",
        )
        return derived.values.toList()
    }
    val cached = ArtistsStore.all.value
    if (cached.isNotEmpty()) {
        AppLog.log("browse", "artists corpus empty and no saved songs — ${cached.size} artist(s) from the cache")
    }
    return cached
}

/**
 * Adds the artists of the account's playlists' songs to [into].
 *
 * The playlists are walked a few at a time (the screen only needs enough to fill
 * a grid, and every artist found is cached for good, so the next visit already
 * knows them); a playlist that cannot be read is skipped instead of failing the
 * whole tab.
 */
private suspend fun artistsFromPlaylists(
    into: LinkedHashMap<String, ArtistItem>,
    maxPlaylists: Int = 8,
) {
    val page = YouTube.library("FEmusic_liked_playlists").getOrNull() ?: return
    val playlists = page.items.filterIsInstance<PlaylistItem>().take(maxPlaylists)
    for (playlist in playlists) {
        val songs = YouTube.playlist(playlist.id).getOrNull()?.songs ?: continue
        songs.forEach { song ->
            song.artists.forEach artistLoop@{ artist ->
                val id = artist.id?.takeIf { it.isNotBlank() } ?: return@artistLoop
                into.putIfAbsent(
                    id,
                    ArtistItem(
                        id = id,
                        title = artist.name,
                        thumbnail = song.thumbnail,
                        shuffleEndpoint = null,
                        radioEndpoint = null,
                    ),
                )
            }
        }
    }
    AppLog.log(
        "browse",
        "artists from playlists: ${playlists.size} playlist(s) read → ${into.size} artist(s) so far",
    )
}

/**
 * The artists of the account's saved songs, one entry each, alphabetically.
 *
 * A few pages are walked (an account can have thousands of saved songs and each
 * page is another request): the point is to fill the screen with the artists the
 * user actually listens to, not to mirror the whole library in one go — every
 * visit extends the cache with what it saw.
 *
 * The picture is the one of the artist's first song: YouTube Music does not send
 * artist artwork with a song, and a card with no image at all reads as a broken
 * entry rather than as one of the user's artists.
 */
private suspend fun artistsFromSavedSongs(maxPages: Int = 4): List<ArtistItem> {
    val byId = LinkedHashMap<String, ArtistItem>()

    fun collect(items: List<YTItem>) {
        items.filterIsInstance<SongItem>().forEach { song ->
            song.artists.forEach artistLoop@{ artist ->
                val id = artist.id?.takeIf { it.isNotBlank() } ?: return@artistLoop
                byId.putIfAbsent(
                    id,
                    ArtistItem(
                        id = id,
                        title = artist.name,
                        thumbnail = song.thumbnail,
                        shuffleEndpoint = null,
                        radioEndpoint = null,
                    ),
                )
            }
        }
    }

    val first = YouTube.library("FEmusic_liked_videos").getOrNull() ?: return emptyList()
    collect(first.items)
    var token = first.continuation
    var pages = 0
    while (!token.isNullOrBlank() && pages < maxPages) {
        val next = YouTube.libraryContinuation(token).getOrNull() ?: break
        collect(next.items)
        token = next.continuation
        pages++
    }
    AppLog.log("browse", "derived artists: ${pages + 1} page(s) → ${byId.size} artist(s)")
    return byId.values.sortedBy { it.title.lowercase() }
}

/**
 * Library with tabs for the signed-in user's liked songs, albums, artists and
 * playlists. Prompts for login when there is no session cookie.
 */
@Composable
fun LibraryScreen(
    language: String,
    isLoggedIn: Boolean,
    gridItemSize: Int,
    onLoggedIn: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onPlaySong: (SongItem) -> Unit,
    onAddToQueue: (SongItem) -> Unit,
    onAddToPlaylist: (SongItem) -> Unit,
    onShuffleAll: (List<SongItem>) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(Localization.get(language, "library"), style = MaterialTheme.typography.headlineMedium)

        if (!isLoggedIn) {
            Text(
                Localization.get(language, "library_login_prompt"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
            // Show the sign-in options directly here instead of a "Log in" button
            // that opens a second screen: the user picks Google or manual cookies
            // without an extra navigation step.
            LoginContent(language = language, onLoggedIn = onLoggedIn)
            return@Column
        }

        val tabs = listOf("songs", "albums", "artists", "playlists")
        val browseIds = listOf(
            "FEmusic_liked_videos",
            "FEmusic_liked_albums",
            ARTISTS_BROWSE_ID,
            "FEmusic_liked_playlists",
        )
        var selectedTab by remember { mutableStateOf(0) }
        var page by remember { mutableStateOf<LibraryPage?>(null) }
        var loading by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        var sortAsc by remember { mutableStateOf(true) }
        var sortByArtist by remember { mutableStateOf(false) }
        // Manual retry: a transient failure (expired session, 401, network
        // hiccup) used to leave an empty list with no way to reload without
        // leaving and reopening the screen.
        var reloadKey by remember { mutableStateOf(0) }
        // Live refresh: liking/unliking a song, or a change made on the paired
        // device, reloads the tab that is on screen (see [LibraryRefresh]).
        val libraryRevision by LibraryRefresh.revision.collectAsState()
        var loadedTab by remember { mutableStateOf(-1) }

        LaunchedEffect(selectedTab, reloadKey, libraryRevision) {
            // A different tab has nothing to keep: it shows the spinner. A
            // refresh of the SAME tab keeps the list it already has on screen
            // while the new page is fetched — clearing it made every like/unlike
            // flash the whole tab through the loading state.
            if (loadedTab != selectedTab) {
                page = null
                loading = true
            }
            error = null
            // Through the shared loader: the artists tab has the derived-artists
            // fallback (see [loadLibraryPage]).
            loadLibraryPage(browseIds[selectedTab]).fold(
                onSuccess = {
                    page = it
                    loadedTab = selectedTab
                    loading = false
                },
                onFailure = { error = it.message; loading = false },
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { i, key ->
                FilterChip(
                    selected = i == selectedTab,
                    onClick = { selectedTab = i },
                    label = { Text(Localization.get(language, key)) },
                )
                if (i != tabs.lastIndex) Spacer(Modifier.width(8.dp))
            }
            Spacer(Modifier.weight(1f))
            if (selectedTab == 0 && page?.items?.isNotEmpty() == true) {
                OutlinedButton(onClick = { onShuffleAll(page!!.items.filterIsInstance<SongItem>()) }) {
                    Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(Localization.get(language, "shuffle_all"))
                }
            }
        }

        // Sort chips (A-Z / Z-A, plus "by artist" for the songs tab).
        if (page?.items?.isNotEmpty() == true) {
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = sortAsc,
                    onClick = { sortAsc = true },
                    label = { Text(Localization.get(language, "sort_az")) },
                )
                FilterChip(
                    selected = !sortAsc,
                    onClick = { sortAsc = false },
                    label = { Text(Localization.get(language, "sort_za")) },
                )
                if (selectedTab == 0) {
                    FilterChip(
                        selected = sortByArtist,
                        onClick = { sortByArtist = !sortByArtist },
                        label = { Text(Localization.get(language, "sort_artist")) },
                    )
                }
            }
        }

        when {
            error != null -> ErrorBox(language, error)
            // The artists tab is the one that has to be *derived* when the
            // account has no artist list (see [loadLibraryPage]): it walks the
            // songs and builds the list from them, which takes seconds rather
            // than a moment, and a bare spinner for that long reads as a hang.
            loading || page == null -> LoadingBox(
                language,
                hint = if (selectedTab == 2) Localization.get(language, "artists_loading_hint") else null,
            )
            else -> {
                val rawItems = page!!.items
                val items = run {
                    val base = if (selectedTab == 0) {
                        val songs = rawItems.filterIsInstance<SongItem>()
                        if (sortByArtist) {
                            songs.sortedWith(compareBy({ it.artists.firstOrNull()?.name.orEmpty().lowercase() }, { it.title.lowercase() }))
                        } else {
                            songs.sortedBy { it.title.lowercase() }
                        }
                    } else {
                        rawItems.sortedBy { it.title.lowercase() }
                    }
                    if (sortAsc) base else base.reversed()
                }
                if (items.isEmpty()) {
                    Column(Modifier.padding(top = 16.dp)) {
                        Text(
                            Localization.get(language, "library_empty"),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.size(12.dp))
                        OutlinedButton(onClick = { reloadKey++ }) {
                            Text(Localization.get(language, "refresh"))
                        }
                    }
                } else if (selectedTab == 0) {
                    LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp)) {
                        items(items.filterIsInstance<SongItem>(), key = { it.id }) { song ->
                            SongRow(song, language, { onPlaySong(song) }, onAddToQueue = { onAddToQueue(song) }, onAddToPlaylist = { onAddToPlaylist(song) })
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(gridItemSize.dp),
                        modifier = Modifier.fillMaxSize().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(items, key = { it.id }) { item ->
                            Box(Modifier.fillMaxWidth()) {
                                YtItemCard(
                                    item = item,
                                    width = null,
                                    onClick = { onItemClick(item, onOpenAlbum, onOpenArtist, onOpenPlaylist, onPlaySong) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
