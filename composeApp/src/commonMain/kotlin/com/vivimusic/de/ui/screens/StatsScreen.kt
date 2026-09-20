package com.vivimusic.de.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vivimusic.de.resources.Res
import com.vivimusic.de.resources.back
import com.vivimusic.de.resources.empty_artists
import com.vivimusic.de.resources.empty_history
import com.vivimusic.de.resources.stats
import com.vivimusic.de.resources.top_artists
import com.vivimusic.de.resources.top_songs
import com.vivimusic.de.resources.total_listening_time
import com.vivimusic.de.resources.unique_albums
import com.vivimusic.de.resources.unique_songs
import com.vivimusic.de.resources.unique_artists
import com.vivimusic.de.ui.AppViewModel
import com.vivimusic.de.ui.NavigationTitle
import com.vivimusic.de.ui.SongRow
import com.vivimusic.de.ui.theme.groupedItemShape
import org.jetbrains.compose.resources.stringResource

/**
 * Statistics screen, ported from ViVi Music's `StatsScreen` and adapted for
 * desktop: listening totals plus the most played songs and artists. Play
 * counts are derived from the local history (one entry per song/artist), so the
 * ranking reflects the listening history currently stored on this device.
 */
@Composable
fun StatsScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val stats by viewModel.listeningStats.collectAsState()
    val history by viewModel.history.collectAsState()
    val topArtists by viewModel.topArtists.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(Res.string.back),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                text = stringResource(Res.string.stats),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item(key = "totals") {
                StatsSummary(stats)
            }

            item(key = "top_songs_title") {
                NavigationTitle(title = stringResource(Res.string.top_songs))
            }
            if (history.isEmpty()) {
                item(key = "top_songs_empty") {
                    Text(
                        text = stringResource(Res.string.empty_history),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            } else {
                items(history, key = { it.id }) { song ->
                    SongRow(
                        song = song,
                        viewModel = viewModel,
                        shape = groupedItemShape(history.indexOf(song), history.size),
                        queue = history,
                    )
                }
            }

            item(key = "top_artists_title") {
                NavigationTitle(title = stringResource(Res.string.top_artists))
            }
            if (topArtists.isEmpty()) {
                item(key = "top_artists_empty") {
                    Text(
                        text = stringResource(Res.string.empty_artists),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            } else {
                items(topArtists, key = { it.first }) { (name, count) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsSummary(stats: com.vivimusic.de.ui.ListeningStats) {
    Column(modifier = Modifier.padding(16.dp)) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth(),
    ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
            ) {
                Column {
                    Text(
                        text = stringResource(Res.string.total_listening_time),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatDuration(stats.totalPlayTimeMs),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCell(
                icon = Icons.Filled.MusicNote,
                value = stats.uniqueSongs,
                label = stringResource(Res.string.unique_songs),
                modifier = Modifier.weight(1f),
            )
            StatCell(
                icon = Icons.Filled.Person,
                value = stats.uniqueArtists,
                label = stringResource(Res.string.unique_artists),
                modifier = Modifier.weight(1f),
            )
            StatCell(
                icon = Icons.Filled.Album,
                value = stats.uniqueAlbums,
                label = stringResource(Res.string.unique_albums),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatCell(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: Int,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 16.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "0m"
    val totalMinutes = ms / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}
