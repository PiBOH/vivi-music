package com.vivimusic.de.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vivimusic.de.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun PlayerBar(viewModel: AppViewModel) {
    val currentSong by viewModel.currentSong.collectAsState()
    val song = currentSong ?: return
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.playing_now),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(text = song.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = song.artist.ifBlank { stringResource(Res.string.unknown_artist) },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            TextButton(onClick = { viewModel.play(song) }) {
                Text(stringResource(Res.string.play))
            }
        }
    }
}
