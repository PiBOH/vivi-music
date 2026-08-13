package com.vivimusic.de.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.vivimusic.de.domain.Song
import com.vivimusic.de.resources.*
import com.vivimusic.de.ui.theme.groupedItemShape
import com.vivimusic.de.ui.theme.listItemColors
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SongRow(
    song: Song,
    viewModel: AppViewModel,
    shape: Shape,
    queue: List<Song>? = null,
) {
    val isFavorite by viewModel.isFavorite(song.id).collectAsState(initial = false)
    ListItem(
        headlineContent = { Text(song.title) },
        supportingContent = {
            Text(song.artist.ifBlank { stringResource(Res.string.unknown_artist) })
        },
        trailingContent = {
            TextButton(onClick = { viewModel.toggleFavorite(song) }) {
                Text(
                    if (isFavorite) stringResource(Res.string.favorite_remove)
                    else stringResource(Res.string.favorite_add)
                )
            }
        },
        colors = listItemColors(),
        modifier = Modifier.clip(shape).clickable {
            if (queue != null) viewModel.playInQueue(song, queue) else viewModel.play(song)
        },
    )
}

@Composable
fun SongList(
    songs: List<Song>,
    viewModel: AppViewModel,
    emptyText: StringResource,
    modifier: Modifier = Modifier,
) {
    if (songs.isEmpty()) {
        EmptyState(emptyText, modifier)
        return
    }
    LazyColumn(modifier.fillMaxSize()) {
        itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
            SongRow(song, viewModel, shape = groupedItemShape(index, songs.size), queue = songs)
        }
    }
}

@Composable
fun EmptyState(text: StringResource, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(text),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

/**
 * Horizontally scrolling row of filter chips, ported 1:1 from ViVi Music's
 * `ChipsRow`: 35dp-tall `FilterChip`s, a spring-animated corner radius
 * (20dp selected / 8dp unselected) and a check leading icon when selected.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <E> ChipsRow(
    chips: List<Pair<E, String>>,
    currentValue: E?,
    onValueUpdate: (E) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.width(12.dp))

        chips.forEach { (value, label) ->
            val isSelected = currentValue == value
            val cornerRadius by animateDpAsState(
                targetValue = if (isSelected) 20.dp else 8.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
                label = "corner_radius",
            )

            FilterChip(
                label = { Text(label) },
                selected = isSelected,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = containerColor,
                ),
                onClick = { onValueUpdate(value) },
                leadingIcon = if (isSelected) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Done,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                        )
                    }
                } else {
                    null
                },
                shape = RoundedCornerShape(cornerRadius),
                border = null,
                modifier = Modifier.height(35.dp),
            )

            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}
