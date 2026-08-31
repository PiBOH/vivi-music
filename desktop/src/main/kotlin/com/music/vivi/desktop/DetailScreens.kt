package com.music.vivi.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.music.innertube.YouTube
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.BrowseEndpoint
import com.music.innertube.models.SongItem
import com.music.innertube.pages.AlbumPage
import com.music.innertube.pages.ArtistItemsPage
import com.music.innertube.pages.ArtistPage
import com.music.innertube.pages.HistoryPage
import com.music.innertube.pages.PlaylistPage

@Composable
fun AlbumScreen(
    browseId: String,
    language: String,
    onBack: () -> Unit,
    onOpenArtist: (String) -> Unit,
    onPlaySong: (SongItem) -> Unit,
    onAddToQueue: (SongItem) -> Unit,
    onAddToPlaylist: (SongItem) -> Unit,
    onPlayAll: (List<SongItem>) -> Unit,
    onShuffleAll: (List<SongItem>) -> Unit,
) {
    var page by remember { mutableStateOf<AlbumPage?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(browseId) {
        YouTube.album(browseId).fold(
            onSuccess = { page = it },
            onFailure = { error = it.message },
        )
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(language, onBack)
        when {
            error != null -> ErrorBox(language, error)
            page == null -> LoadingBox(language)
            else -> {
                val album = page!!.album
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Thumbnail(album.thumbnail, Modifier.size(128.dp))
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            album.title,
                            style = MaterialTheme.typography.headlineMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val artists = album.artists?.joinToString(", ") { it.name }.orEmpty()
                        if (artists.isNotBlank()) {
                            Text(
                                artists,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        album.year?.let { Text(it.toString(), style = MaterialTheme.typography.bodyMedium) }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(Localization.get(language, "songs"), style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.weight(1f))
                    Button(onClick = { onPlayAll(page!!.songs) }) { Text(Localization.get(language, "play_all")) }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { onShuffleAll(page!!.songs) }) {
                        Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(Localization.get(language, "shuffle_all"))
                    }
                }
                LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                    items(page!!.songs, key = { it.id }) { song ->
                        SongRow(song, language, { onPlaySong(song) }, onAddToQueue = { onAddToQueue(song) }, onAddToPlaylist = { onAddToPlaylist(song) })
                    }
                }
            }
        }
    }
}

@Composable
fun ArtistScreen(
    browseId: String,
    language: String,
    onBack: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onPlaySong: (SongItem) -> Unit,
    onAddToQueue: (SongItem) -> Unit,
    onAddToPlaylist: (SongItem) -> Unit,
) {
    var page by remember { mutableStateOf<ArtistPage?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableStateOf(0) } // 0 = Songs, 1 = Albums, 2 = Items
    var itemsPage by remember { mutableStateOf<ArtistItemsPage?>(null) }
    var itemsEndpoint by remember { mutableStateOf<BrowseEndpoint?>(null) }

    LaunchedEffect(browseId) {
        YouTube.artist(browseId).fold(
            onSuccess = { p ->
                page = p
                // Prefer the first section with a "See all" endpoint for the Items tab.
                itemsEndpoint = p.sections.firstNotNullOfOrNull { it.moreEndpoint }
            },
            onFailure = { error = it.message },
        )
    }

    LaunchedEffect(tab, itemsEndpoint) {
        if (tab == 2 && itemsEndpoint != null) {
            YouTube.artistItems(itemsEndpoint!!).fold(
                onSuccess = { itemsPage = it },
                onFailure = { itemsPage = null },
            )
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(language, onBack)
        when {
            error != null -> ErrorBox(language, error)
            page == null -> LoadingBox(language)
            else -> {
                val artist = page!!.artist
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Thumbnail(artist.thumbnail, Modifier.size(128.dp))
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            artist.title,
                            style = MaterialTheme.typography.headlineMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        page!!.subscriberCountText?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                val tabs = listOf(Localization.get(language, "songs"), Localization.get(language, "albums"), Localization.get(language, "items"))
                TabRow(selectedTabIndex = tab) {
                    tabs.forEachIndexed { i, title ->
                        Tab(selected = tab == i, onClick = { tab = i }, text = { Text(title) })
                    }
                }
                Spacer(Modifier.height(8.dp))

                if (tab == 2) {
                    val items = itemsPage?.items.orEmpty()
                    if (items.isEmpty()) {
                        LoadingBox(language)
                    } else if (items.all { it is SongItem }) {
                        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                            items(items.filterIsInstance<SongItem>(), key = { it.id }) { song ->
                                SongRow(song, language, { onPlaySong(song) }, onAddToQueue = { onAddToQueue(song) }, onAddToPlaylist = { onAddToPlaylist(song) })
                            }
                        }
                    } else {
                        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                            items(items, key = { it.id }) { item ->
                                YtItemCard(item = item, width = null, onClick = { onItemClick(item, onOpenAlbum, onOpenArtist, onOpenPlaylist, onPlaySong) })
                            }
                        }
                    }
                } else {
                    LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                        page!!.sections.forEach { section ->
                            val songs = section.items.filterIsInstance<SongItem>()
                            val albums = section.items.filterIsInstance<AlbumItem>()
                            val others = section.items.filterNot { it is SongItem || it is AlbumItem }
                            val visible = when (tab) {
                                0 -> songs
                                1 -> albums
                                else -> emptyList()
                            }
                            val mixed = tab == 1 && (albums.isNotEmpty() || others.isNotEmpty())

                            if (visible.isNotEmpty()) {
                                item(key = "header-${tab}-${section.title}") {
                                    Text(section.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                                }
                                if (tab == 0) {
                                    items(visible.filterIsInstance<SongItem>().distinctBy { it.id }, key = { "song-${it.id}" }) { song ->
                                        SongRow(song, language, { onPlaySong(song) }, onAddToQueue = { onAddToQueue(song) }, onAddToPlaylist = { onAddToPlaylist(song) })
                                    }
                                } else {
                                    item(key = "grid-${tab}-${section.title}") {
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            items((albums + others).distinctBy { it.id }, key = { it.id }) { item ->
                                                YtItemCard(item = item, onClick = { onItemClick(item, onOpenAlbum, onOpenArtist, onOpenPlaylist, onPlaySong) })
                                            }
                                        }
                                    }
                                }
                            } else if (mixed && albums.isNotEmpty()) {
                                item(key = "header-${tab}-${section.title}") {
                                    Text(section.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                                }
                                item(key = "grid-${tab}-${section.title}") {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        items((albums + others).distinctBy { it.id }, key = { it.id }) { item ->
                                            YtItemCard(item = item, onClick = { onItemClick(item, onOpenAlbum, onOpenArtist, onOpenPlaylist, onPlaySong) })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistScreen(
    playlistId: String,
    language: String,
    onBack: () -> Unit,
    onOpenArtist: (String) -> Unit,
    onPlaySong: (SongItem) -> Unit,
    onAddToQueue: (SongItem) -> Unit,
    onAddToPlaylist: (SongItem) -> Unit,
    onPlayAll: (List<SongItem>) -> Unit,
    onShuffleAll: (List<SongItem>) -> Unit,
) {
    var page by remember { mutableStateOf<PlaylistPage?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(playlistId) {
        YouTube.playlist(playlistId).fold(
            onSuccess = { page = it },
            onFailure = { error = it.message },
        )
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(language, onBack)
        when {
            error != null -> ErrorBox(language, error)
            page == null -> LoadingBox(language)
            else -> {
                val playlist = page!!.playlist
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Thumbnail(playlist.thumbnail, Modifier.size(128.dp))
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            playlist.title,
                            style = MaterialTheme.typography.headlineMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        playlist.author?.let {
                            Text(
                                it.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        playlist.songCountText?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { onPlayAll(page!!.songs) }) { Text(Localization.get(language, "play_all")) }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { onShuffleAll(page!!.songs) }) {
                        Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(Localization.get(language, "shuffle_all"))
                    }
                }
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                    items(page!!.songs, key = { it.id }) { song ->
                        SongRow(song, language, { onPlaySong(song) }, onAddToQueue = { onAddToQueue(song) }, onAddToPlaylist = { onAddToPlaylist(song) })
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(
    language: String,
    onBack: () -> Unit,
    onPlaySong: (SongItem) -> Unit,
    onAddToQueue: (SongItem) -> Unit,
    onAddToPlaylist: (SongItem) -> Unit,
) {
    var page by remember { mutableStateOf<HistoryPage?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        YouTube.musicHistory().fold(
            onSuccess = { page = it },
            onFailure = { error = it.message },
        )
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(language, onBack)
        Text(Localization.get(language, "history"), style = MaterialTheme.typography.headlineMedium)
        when {
            error != null -> ErrorBox(language, error)
            page == null -> LoadingBox(language)
            else -> {
                val sections = page!!.sections.orEmpty()
                if (sections.isEmpty()) {
                    Text(
                        Localization.get(language, "history_empty"),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                } else {
                    LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                        sections.forEach { section ->
                            item(key = "header-${section.title}") {
                                Text(
                                    section.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                                )
                            }
                            items(section.songs, key = { "song-${it.id}" }) { song ->
                                SongRow(song, language, { onPlaySong(song) }, onAddToQueue = { onAddToQueue(song) }, onAddToPlaylist = { onAddToPlaylist(song) })
                            }
                        }
                    }
                }
            }
        }
    }
}
