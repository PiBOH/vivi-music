package com.vivimusic.de.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vivimusic.de.resources.Res
import com.vivimusic.de.resources.back
import com.vivimusic.de.resources.charts
import com.vivimusic.de.resources.explore
import com.vivimusic.de.resources.mood_and_genres
import com.vivimusic.de.resources.new_release_albums
import com.vivimusic.de.resources.open_charts
import com.vivimusic.de.ui.AlbumCard
import com.vivimusic.de.ui.AppViewModel
import com.vivimusic.de.ui.NavigationTitle
import org.jetbrains.compose.resources.stringResource

private val MoodButtonHeight = 48.dp

/**
 * Explore screen, ported from ViVi Music's `ExploreScreen`: a "New release
 * albums" carousel and the mood/genre tiles. Tapping the section headers opens
 * the dedicated New releases / Moods & genres screens, and each tile opens the
 * generic browse screen.
 */
@Composable
fun ExploreScreen(
    viewModel: AppViewModel,
    onBack: (() -> Unit)? = null,
    onOpenCharts: () -> Unit,
    onOpenNewReleases: () -> Unit,
    onOpenMoodGenres: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenBrowse: (String, String?) -> Unit,
) {
    val explore by viewModel.explore.collectAsState()
    val loading by viewModel.exploreLoading.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadExplore() }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.back),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
            Text(
                text = stringResource(Res.string.explore),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        if (loading && explore == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        val data = explore
        if (data == null) {
            return@Column
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (data.newReleaseAlbums.isNotEmpty()) {
                item(key = "new_releases_title") {
                    NavigationTitle(
                        title = stringResource(Res.string.new_release_albums),
                    )
                }
                item(key = "new_releases_content") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(data.newReleaseAlbums, key = { it.id }) { album ->
                            AlbumCard(album = album, onClick = { onOpenAlbum(album.id) })
                        }
                    }
                }
            }

            if (data.moodGenres.isNotEmpty()) {
                item(key = "mood_genres_title") {
                    NavigationTitle(title = stringResource(Res.string.mood_and_genres))
                }
                item(key = "mood_genres_content") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(data.moodGenres, key = { it.browseId }) { mood ->
                            MoodGenreButton(
                                title = mood.title,
                                onClick = { onOpenBrowse(mood.browseId, mood.params) },
                            )
                        }
                    }
                }
            }

            item(key = "charts_entry") {
                NavigationTitle(title = stringResource(Res.string.charts))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .clickable(onClick = onOpenCharts)
                        .padding(16.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.open_charts),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

/** A mood/genre tile, matching ViVi Music's `MoodAndGenresButton`. */
@Composable
private fun MoodGenreButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .width(180.dp)
            .height(MoodButtonHeight)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
