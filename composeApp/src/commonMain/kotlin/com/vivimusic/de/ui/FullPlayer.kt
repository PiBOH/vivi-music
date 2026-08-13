package com.vivimusic.de.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
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
import com.vivimusic.de.resources.*
import org.jetbrains.compose.resources.stringResource

/**
 * Expanded player, ported visually from ViVi Music's `BottomSheetPlayer`:
 * artwork, title/artist, a seek bar and the playback controls.
 *
 * The seek bar and time labels are wired to the audio engine;
 * shuffle/previous/next/repeat are placeholders until the play queue exists.
 */
@Composable
fun FullPlayer(
    viewModel: AppViewModel,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val song = currentSong ?: return

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
                text = stringResource(Res.string.playing_now),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

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
            IconButton(onClick = { /* shuffle */ }) {
                Icon(Icons.Filled.Shuffle, contentDescription = null)
            }
            IconButton(onClick = { /* previous */ }) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = null)
            }
            FilledIconButton(onClick = viewModel::togglePlayPause, modifier = Modifier.size(64.dp)) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                )
            }
            IconButton(onClick = { /* next */ }) {
                Icon(Icons.Filled.SkipNext, contentDescription = null)
            }
            IconButton(onClick = { /* repeat */ }) {
                Icon(Icons.Filled.Repeat, contentDescription = null)
            }
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
