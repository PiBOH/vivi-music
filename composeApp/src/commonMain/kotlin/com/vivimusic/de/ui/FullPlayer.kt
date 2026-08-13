package com.vivimusic.de.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.vivimusic.de.resources.*
import com.vivimusic.de.ui.player.LyricsScreen
import com.vivimusic.de.ui.player.QueueScreen
import org.jetbrains.compose.resources.stringResource

private enum class PlayerPage { Player, Queue, Lyrics }

/**
 * Expanded player, ported visually from ViVi Music's `BottomSheetPlayer`, with
 * a desktop page switcher for the player, the queue and the lyrics. The seek
 * bar, time labels and the playback controls (shuffle/previous/play/next/
 * repeat) are wired to the audio engine and the play queue.
 */
@Composable
fun FullPlayer(
    viewModel: AppViewModel,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var page by remember { mutableStateOf(PlayerPage.Player) }

    val title = when (page) {
        PlayerPage.Player -> stringResource(Res.string.playing_now)
        PlayerPage.Queue -> stringResource(Res.string.queue)
        PlayerPage.Lyrics -> stringResource(Res.string.lyrics)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            IconButton(onClick = onCollapse) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null)
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            FilledTonalIconButton(
                onClick = { page = PlayerPage.Queue },
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = stringResource(Res.string.queue),
                    tint = if (page == PlayerPage.Queue) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            FilledTonalIconButton(
                onClick = { page = PlayerPage.Lyrics },
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Lyrics,
                    contentDescription = stringResource(Res.string.lyrics),
                    tint = if (page == PlayerPage.Lyrics) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }

        when (page) {
            PlayerPage.Player -> PlayerContent(viewModel)
            PlayerPage.Queue -> QueueScreen(viewModel)
            PlayerPage.Lyrics -> LyricsScreen(viewModel)
        }
    }
}

@Composable
private fun ColumnScope.PlayerContent(viewModel: AppViewModel) {
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val shuffleEnabled by viewModel.shuffleEnabled.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val song = currentSong ?: return

    Box(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        if (song.thumbnailUrl.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        } else {
            AsyncImage(
                model = song.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(280.dp)
                    .clip(RoundedCornerShape(16.dp)),
            )
        }
    }

    Text(
        text = song.title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    Text(
        text = song.artist.ifBlank { stringResource(Res.string.unknown_artist) },
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )

    val durationMs = playbackState.durationMs.coerceAtLeast(1L)
    val positionMs = playbackState.positionMs.coerceIn(0L, durationMs)
    Slider(
        value = positionMs.toFloat(),
        onValueChange = { viewModel.seekTo(it.toLong()) },
        valueRange = 0f..durationMs.toFloat(),
    )
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(formatDuration(playbackState.positionMs), style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.weight(1f))
        Text(formatDuration(playbackState.durationMs), style = MaterialTheme.typography.labelSmall)
    }

    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
    ) {
        IconButton(onClick = viewModel::toggleShuffle) {
            Icon(
                imageVector = Icons.Filled.Shuffle,
                contentDescription = stringResource(Res.string.shuffle),
                tint = if (shuffleEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        IconButton(onClick = viewModel::playPrevious) {
            Icon(Icons.Filled.SkipPrevious, contentDescription = null)
        }
        FilledIconButton(onClick = viewModel::togglePlayPause, modifier = Modifier.size(64.dp)) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
            )
        }
        IconButton(onClick = viewModel::playNext) {
            Icon(Icons.Filled.SkipNext, contentDescription = null)
        }
        IconButton(onClick = viewModel::cycleRepeatMode) {
            Icon(
                imageVector = if (repeatMode == RepeatMode.One) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                contentDescription = stringResource(Res.string.repeat),
                tint = if (repeatMode != RepeatMode.Off) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

private fun formatDuration(ms: Long?): String {
    if (ms == null || ms <= 0) return "--:--"
    val totalSec = ms / 1000
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
