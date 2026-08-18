package com.music.vivi.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.innertube.models.YTItem
import com.music.innertube.pages.ChartsPage
import com.music.innertube.pages.MoodAndGenres

/** New Release albums screen (grid of the latest albums). */
@Composable
fun NewReleasesScreen(
    language: String,
    gridItemSize: Int,
    onBack: () -> Unit,
    onOpenAlbum: (String) -> Unit,
) {
    var albums by remember { mutableStateOf<List<YTItem>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        YouTube.newReleaseAlbums().fold(
            onSuccess = { albums = it.distinctBy { a -> a.id } },
            onFailure = { error = it.message },
        )
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(language, onBack)
        Text(Localization.get(language, "new_release_albums"), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        when {
            error != null -> ErrorBox(language, error)
            albums == null -> LoadingBox(language)
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(gridItemSize.dp),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 8.dp),
            ) {
                gridItems(albums!!, key = { it.id }) { item ->
                    YtItemCard(
                        item = item,
                        width = null,
                        onClick = { onItemClick(item, onOpenAlbum, {}, {}, {}) },
                    )
                }
            }
        }
    }
}

/** Charts screen (trending + top songs/videos). */
@Composable
fun ChartsScreen(
    language: String,
    onBack: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onPlaySong: (SongItem) -> Unit,
    onAddToQueue: (SongItem) -> Unit,
    onAddToPlaylist: (SongItem) -> Unit,
) {
    var page by remember { mutableStateOf<ChartsPage?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        YouTube.getChartsPage().fold(
            onSuccess = { page = it },
            onFailure = { error = it.message },
        )
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(language, onBack)
        Text(Localization.get(language, "charts"), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        when {
            error != null -> ErrorBox(language, error)
            page == null -> LoadingBox(language)
            else -> LazyColumn(Modifier.fillMaxSize()) {
                page!!.sections.filter { it.title != "Top music videos" }.forEach { section ->
                    item(key = "header-${section.title}") {
                        SectionHeader(
                            title = when (section.title) {
                                "Trending" -> Localization.get(language, "trending")
                                else -> section.title
                            },
                            language = language,
                        )
                    }
                    item(key = "content-${section.title}") {
                        val songs = section.items.filterIsInstance<SongItem>()
                        if (songs.size == section.items.size) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp),
                            ) {
                                items(songs.distinctBy { it.id }, key = { it.id }) { song ->
                                    Box(Modifier.width(320.dp)) {
                                        SongRow(song, language, { onPlaySong(song) }, onAddToQueue = { onAddToQueue(song) }, onAddToPlaylist = { onAddToPlaylist(song) })
                                    }
                                }
                            }
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp),
                            ) {
                                items(section.items.distinctBy { it.id }, key = { it.id }) { item ->
                                    YtItemCard(item = item, onClick = { onItemClick(item, onOpenAlbum, onOpenArtist, onOpenPlaylist, onPlaySong) })
                                }
                            }
                        }
                    }
                }
                val topVideos = page!!.sections.find { it.title == "Top music videos" }
                if (topVideos != null) {
                    item(key = "top_videos_header") {
                        SectionHeader(title = Localization.get(language, "top_music_videos"), language = language)
                    }
                    item(key = "top_videos_content") {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                        ) {
                            items(topVideos.items.filterIsInstance<SongItem>().distinctBy { it.id }, key = { it.id }) { video ->
                                YtItemCard(item = video, onClick = { onPlaySong(video) })
                            }
                        }
                    }
                }
                item(key = "charts_spacer") { Box(Modifier.height(16.dp)) }
            }
        }
    }
}

/** Dedicated Mood & genres screen (the full list, not the Home preview). */
@Composable
fun MoodGenresScreen(
    language: String,
    onBack: () -> Unit,
    onOpenBrowse: (String, String?) -> Unit,
) {
    var sections by remember { mutableStateOf<List<MoodAndGenres>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        YouTube.moodAndGenres().fold(
            onSuccess = { sections = it },
            onFailure = { error = it.message },
        )
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(language, onBack)
        Text(Localization.get(language, "mood_and_genres"), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        when {
            error != null -> ErrorBox(language, error)
            sections == null -> LoadingBox(language)
            else -> LazyColumn(Modifier.fillMaxSize()) {
                sections!!.forEach { section ->
                    item(key = "header-${section.title}") {
                        Text(
                            section.title,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                        )
                    }
                    item(key = "grid-${section.title}") {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(180.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            userScrollEnabled = false,
                        ) {
                            gridItems(section.items, key = { it.endpoint.browseId + it.title }) { item ->
                                MoodAndGenresButton(
                                    title = item.title,
                                    onClick = { onOpenBrowse(item.endpoint.browseId, item.endpoint.params) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dedicated auto-playlist screen (Liked / Top / etc.). Loads the same
 * browse-id the Library tabs use, but as its own pushed screen with the
 * chosen title, so every auto-playlist has a real detail page.
 */
@Composable
fun AutoPlaylistScreen(
    browseId: String,
    title: String,
    language: String,
    gridItemSize: Int,
    onBack: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onPlaySong: (SongItem) -> Unit,
    onAddToQueue: (SongItem) -> Unit,
    onAddToPlaylist: (SongItem) -> Unit,
) {
    var page by remember { mutableStateOf<com.music.innertube.pages.LibraryPage?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(browseId) {
        YouTube.library(browseId).fold(
            onSuccess = { page = it },
            onFailure = { error = it.message },
        )
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(language, onBack)
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        when {
            error != null -> ErrorBox(language, error)
            page == null -> LoadingBox(language)
            else -> {
                val items = page!!.items
                val songs = items.filterIsInstance<SongItem>()
                if (songs.size == items.size && songs.isNotEmpty()) {
                    LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp)) {
                        items(songs, key = { it.id }) { song ->
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
                        gridItems(items, key = { it.id }) { item ->
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
