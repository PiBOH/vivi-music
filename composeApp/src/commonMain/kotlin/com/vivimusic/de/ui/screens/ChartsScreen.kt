package com.vivimusic.de.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.vivimusic.de.resources.charts
import com.vivimusic.de.resources.empty_charts
import com.vivimusic.de.ui.AppViewModel
import com.vivimusic.de.ui.ChartItemRow
import com.vivimusic.de.ui.EmptyState
import com.vivimusic.de.ui.NavigationTitle
import com.vivimusic.de.ui.theme.groupedItemShape
import org.jetbrains.compose.resources.stringResource

/**
 * Charts screen, ported from ViVi Music's `ChartsScreen`: each chart section
 * (trending, top songs, top music videos, genres) rendered as a title plus its
 * song/album rows. Tapping a row plays it or opens the album.
 */
@Composable
fun ChartsScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onOpenAlbum: (String) -> Unit,
) {
    val charts by viewModel.charts.collectAsState()
    val loading by viewModel.exploreLoading.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadCharts() }

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
                text = stringResource(Res.string.charts),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        if (loading && charts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        if (charts.isEmpty()) {
            EmptyState(Res.string.empty_charts)
            return@Column
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            charts.forEach { section ->
                item(key = "title-${section.title}") {
                    NavigationTitle(title = section.title)
                }
                itemsIndexed(section.items, key = { _, item -> "${section.title}-${item.id}" }) { index, item ->
                    ChartItemRow(
                        item = item,
                        viewModel = viewModel,
                        onOpenAlbum = onOpenAlbum,
                        shape = groupedItemShape(index, section.items.size),
                    )
                }
            }
        }
    }
}
