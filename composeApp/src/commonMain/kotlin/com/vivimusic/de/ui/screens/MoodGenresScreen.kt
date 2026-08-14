package com.vivimusic.de.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.vivimusic.de.resources.empty_mood_genres
import com.vivimusic.de.resources.mood_and_genres
import com.vivimusic.de.ui.AppViewModel
import com.vivimusic.de.ui.EmptyState
import com.vivimusic.de.ui.NavigationTitle
import org.jetbrains.compose.resources.stringResource

private val MoodButtonHeight = 48.dp

/**
 * Moods & genres screen, ported from ViVi Music's `MoodAndGenresScreen`:
 * sections of mood/genre tiles (two per row, adapted from the mobile layout).
 */
@Composable
fun MoodGenresScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onOpenBrowse: (String, String?) -> Unit,
) {
    val sections by viewModel.moodGenres.collectAsState()
    val loading by viewModel.exploreLoading.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadMoodGenres() }

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
                text = stringResource(Res.string.mood_and_genres),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        if (loading && sections.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        if (sections.isEmpty()) {
            EmptyState(Res.string.empty_mood_genres)
            return@Column
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            sections.forEach { section ->
                item(key = "title-${section.title}") {
                    NavigationTitle(title = section.title)
                }
                itemsIndexed(section.items, key = { _, item -> "${section.title}-${item.browseId}" }) { _, mood ->
                    MoodTile(
                        title = mood.title,
                        onClick = { onOpenBrowse(mood.browseId, mood.params) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MoodTile(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .fillMaxWidth()
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
