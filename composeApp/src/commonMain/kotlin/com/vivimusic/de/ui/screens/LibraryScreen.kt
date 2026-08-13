package com.vivimusic.de.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vivimusic.de.domain.Playlist
import com.vivimusic.de.resources.*
import com.vivimusic.de.ui.AppViewModel
import com.vivimusic.de.ui.SongList
import org.jetbrains.compose.resources.stringResource

@Composable
fun LibraryScreen(viewModel: AppViewModel) {
    val favorites by viewModel.favorites.collectAsState()
    val history by viewModel.history.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    var tab by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(Res.string.nav_favorites)) })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(Res.string.nav_history)) })
            Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text(stringResource(Res.string.nav_playlists)) })
        }
        when (tab) {
            0 -> SongList(favorites, viewModel, Res.string.empty_favorites)
            1 -> SongList(history, viewModel, Res.string.empty_history)
            2 -> PlaylistsSection(playlists, viewModel)
        }
    }
}

@Composable
private fun PlaylistsSection(playlists: List<Playlist>, viewModel: AppViewModel) {
    var name by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text(stringResource(Res.string.new_playlist)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    viewModel.createPlaylist(name.trim())
                    name = ""
                },
            ) {
                Text(stringResource(Res.string.new_playlist))
            }
        }
        if (playlists.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(Res.string.empty_playlists))
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(playlists, key = { it.id }) { playlist ->
                    ListItem(
                        headlineContent = { Text(playlist.name) },
                        trailingContent = {
                            TextButton(onClick = { viewModel.deletePlaylist(playlist.id) }) {
                                Text(stringResource(Res.string.delete))
                            }
                        },
                    )
                }
            }
        }
    }
}
