package com.vivimusic.de.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.vivimusic.de.domain.Song
import com.vivimusic.de.resources.Res
import com.vivimusic.de.resources.delete
import com.vivimusic.de.resources.empty_playlists
import com.vivimusic.de.resources.unknown_artist
import com.vivimusic.de.ui.AppViewModel
import com.vivimusic.de.ui.EmptyState
import com.vivimusic.de.ui.theme.groupedItemShape
import com.vivimusic.de.ui.theme.listItemColors
import androidx.compose.material3.ListItem
import org.jetbrains.compose.resources.stringResource

/**
 * Local playlist detail, ported from ViVi Music's `LocalPlaylistScreen`: a
 * square thumbnail header with the playlist name and the list of its songs
 * (with a remove button on each row).
 */
@Composable
fun PlaylistScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val playlist by viewModel.playlist.collectAsState()
    val songs by viewModel.playlistSongs.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
        }

        val current = playlist ?: return@Column

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item(key = "playlist_header") {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp, vertical = 8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp)),
                    ) {
                        if (current.thumbnailUrl.isNullOrBlank()) {
                            Box(
                                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            AsyncImage(
                                model = current.thumbnailUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = current.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (songs.isEmpty()) {
                item(key = "empty") {
                    EmptyState(Res.string.empty_playlists)
                }
            } else {
                itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                    PlaylistSongRow(
                        song = song,
                        viewModel = viewModel,
                        shape = groupedItemShape(index, songs.size),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistSongRow(song: Song, viewModel: AppViewModel, shape: androidx.compose.ui.graphics.Shape) {
    ListItem(
        headlineContent = { Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(
                song.artist.ifBlank { stringResource(Res.string.unknown_artist) },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = {
            TextButton(onClick = { viewModel.removeFromPlaylist(song) }) {
                Text(stringResource(Res.string.delete))
            }
        },
        colors = listItemColors(),
        modifier = Modifier
            .clip(shape)
            .clickable { viewModel.play(song) },
    )
}
