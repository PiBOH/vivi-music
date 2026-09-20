package com.vivimusic.de.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vivimusic.de.data.lyrics.LyricsLine
import com.vivimusic.de.resources.*
import com.vivimusic.de.ui.AppViewModel
import org.jetbrains.compose.resources.stringResource

/**
 * Synchronized lyrics, ported from ViVi Music's lyrics panel: the current line
 * is highlighted (primary color + larger weight) and the list auto-scrolls to
 * follow playback. Plain (unsynced) lyrics are shown statically.
 */
@Composable
fun LyricsScreen(viewModel: AppViewModel) {
    val lyrics by viewModel.lyrics.collectAsState()
    val loading by viewModel.lyricsLoading.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        val song = currentSong
        if (song != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = song.artist.ifBlank { stringResource(Res.string.unknown_artist) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        when {
            loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            lyrics.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(Res.string.no_lyrics),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> SyncedLyrics(lyrics, playbackState.positionMs)
        }
    }
}

@Composable
private fun SyncedLyrics(lyrics: List<LyricsLine>, positionMs: Long) {
    val listState = rememberLazyListState()

    val currentIndex = if (lyrics.any { it.timeMs > 0 }) {
        lyrics.indexOfLast { it.timeMs <= positionMs }
    } else {
        -1
    }

    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0) {
            listState.animateScrollToItem(currentIndex.coerceAtLeast(0))
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            top = 16.dp,
            bottom = 96.dp,
        ),
    ) {
        itemsIndexed(lyrics, key = { index, line -> "${line.timeMs}-$index" }) { index, line ->
            val active = index == currentIndex
            Text(
                text = line.text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                color = if (active) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 10.dp),
            )
        }
    }
}
