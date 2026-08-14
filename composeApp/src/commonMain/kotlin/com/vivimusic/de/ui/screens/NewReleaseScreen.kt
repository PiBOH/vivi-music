package com.vivimusic.de.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vivimusic.de.resources.Res
import com.vivimusic.de.resources.back
import com.vivimusic.de.resources.empty_albums
import com.vivimusic.de.resources.new_release_albums
import com.vivimusic.de.ui.AlbumCard
import com.vivimusic.de.ui.AppViewModel
import com.vivimusic.de.ui.EmptyState
import org.jetbrains.compose.resources.stringResource

/**
 * New releases screen, ported from ViVi Music's `NewReleaseScreen`: an adaptive
 * grid of new release album cards.
 */
@Composable
fun NewReleaseScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onOpenAlbum: (String) -> Unit,
) {
    val albums by viewModel.newReleaseAlbums.collectAsState()
    val loading by viewModel.exploreLoading.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadNewReleases() }

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
                text = stringResource(Res.string.new_release_albums),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        if (loading && albums.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        if (albums.isEmpty()) {
            EmptyState(Res.string.empty_albums)
            return@Column
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 152.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(albums, key = { it.id }) { album ->
                AlbumCard(
                    album = album,
                    onClick = { onOpenAlbum(album.id) },
                    modifier = Modifier.fillMaxWidth(),
                    thumbnailSize = 0.dp,
                )
            }
        }
    }
}
