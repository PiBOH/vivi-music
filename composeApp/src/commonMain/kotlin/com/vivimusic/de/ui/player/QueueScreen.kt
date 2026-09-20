package com.vivimusic.de.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.vivimusic.de.domain.Song
import com.vivimusic.de.resources.*
import com.vivimusic.de.ui.AppViewModel
import com.vivimusic.de.ui.EmptyState
import com.vivimusic.de.ui.RepeatMode
import com.vivimusic.de.ui.theme.groupedItemShape
import org.jetbrains.compose.resources.stringResource

/**
 * Play queue, ported from ViVi Music's `Queue` (V2): shuffle/repeat pills, a
 * header with the track count and a clear action, and the list of upcoming
 * songs with the currently playing one highlighted.
 */
@Composable
fun QueueScreen(viewModel: AppViewModel) {
    val queue by viewModel.queue.collectAsState()
    val currentIndex by viewModel.queueIndex.collectAsState()
    val shuffleEnabled by viewModel.shuffleEnabled.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.queue),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(Res.string.up_next),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (queue.isNotEmpty()) {
                TextButton(onClick = viewModel::clearQueue) {
                    Text(stringResource(Res.string.clear))
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            QueueTogglePill(
                icon = { Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp)) },
                label = stringResource(Res.string.shuffle),
                active = shuffleEnabled,
                onClick = viewModel::toggleShuffle,
                modifier = Modifier.weight(1f),
            )
            QueueTogglePill(
                icon = {
                    Icon(
                        imageVector = if (repeatMode == RepeatMode.One) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
                label = stringResource(Res.string.repeat),
                active = repeatMode != RepeatMode.Off,
                onClick = viewModel::cycleRepeatMode,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (queue.isEmpty()) {
            EmptyState(Res.string.queue_empty)
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(queue, key = { _, song -> song.id }) { index, song ->
                    QueueRow(
                        song = song,
                        isCurrent = index == currentIndex,
                        isPlaying = isPlaying,
                        shape = groupedItemShape(index, queue.size),
                        onClick = { viewModel.playAt(index) },
                        onRemove = { viewModel.removeFromQueue(index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueTogglePill(
    icon: @Composable () -> Unit,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = if (active) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val content = if (active) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(container)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
    ) {
        icon()
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun QueueRow(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    shape: androidx.compose.ui.graphics.Shape,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    val titleColor = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (isCurrent) {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                } else {
                    Color.Transparent
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        if (isCurrent && isPlaying) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        QueueThumbnail(song.thumbnailUrl)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = titleColor,
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
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(Res.string.delete),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun QueueThumbnail(url: String?) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
