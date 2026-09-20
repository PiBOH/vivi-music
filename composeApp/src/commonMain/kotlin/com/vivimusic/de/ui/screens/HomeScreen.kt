package com.vivimusic.de.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.vivimusic.de.domain.Song
import com.vivimusic.de.resources.*
import com.vivimusic.de.ui.AppViewModel
import com.vivimusic.de.ui.EmptyState
import com.vivimusic.de.ui.NavigationTitle
import org.jetbrains.compose.resources.stringResource

private val ThumbnailCornerRadius = 6.dp
private val CardThumbnailSize = 128.dp

@Composable
fun HomeScreen(viewModel: AppViewModel) {
    val sections by viewModel.homeSections.collectAsState()
    val loading by viewModel.loading.collectAsState()

    if (loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (sections.isEmpty()) {
        EmptyState(Res.string.empty_home)
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        sections.forEach { section ->
            item(key = "title-${section.title}") {
                NavigationTitle(
                    title = section.title,
                    onPlayAllClick = {
                        section.songs.firstOrNull()?.let(viewModel::play)
                    },
                )
            }
            item(key = "list-${section.title}") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(section.songs, key = { it.id }) { song ->
                        SongCard(song = song, onClick = { viewModel.play(song) })
                    }
                }
            }
        }
    }
}

/**
 * Square-thumbnail song card, matching the grid cards used in ViVi Music's home
 * carousels.
 */
@Composable
private fun SongCard(song: Song, onClick: () -> Unit) {
    Column(modifier = Modifier.width(CardThumbnailSize).clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .size(CardThumbnailSize)
                .clip(RoundedCornerShape(ThumbnailCornerRadius)),
        ) {
            if (song.thumbnailUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
            } else {
                AsyncImage(
                    model = song.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Text(
            text = song.title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = song.artist.ifBlank { stringResource(Res.string.unknown_artist) },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
