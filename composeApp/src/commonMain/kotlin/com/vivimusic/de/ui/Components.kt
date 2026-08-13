package com.vivimusic.de.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.vivimusic.de.domain.Song
import com.vivimusic.de.resources.*
import com.vivimusic.de.ui.theme.groupedItemShape
import com.vivimusic.de.ui.theme.listItemColors
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SongRow(song: Song, viewModel: AppViewModel, shape: Shape) {
    val isFavorite by viewModel.isFavorite(song.id).collectAsState(initial = false)
    ListItem(
        headlineContent = { Text(song.title) },
        supportingContent = {
            Text(song.artist.ifBlank { stringResource(Res.string.unknown_artist) })
        },
        trailingContent = {
            TextButton(onClick = { viewModel.toggleFavorite(song) }) {
                Text(
                    if (isFavorite) stringResource(Res.string.favorite_remove)
                    else stringResource(Res.string.favorite_add)
                )
            }
        },
        colors = listItemColors(),
        modifier = Modifier.clip(shape).clickable { viewModel.play(song) },
    )
}

@Composable
fun SongList(
    songs: List<Song>,
    viewModel: AppViewModel,
    emptyText: StringResource,
    modifier: Modifier = Modifier,
) {
    if (songs.isEmpty()) {
        EmptyState(emptyText, modifier)
        return
    }
    LazyColumn(modifier.fillMaxSize()) {
        itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
            SongRow(song, viewModel, shape = groupedItemShape(index, songs.size))
        }
    }
}

@Composable
fun EmptyState(text: StringResource, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(text),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
