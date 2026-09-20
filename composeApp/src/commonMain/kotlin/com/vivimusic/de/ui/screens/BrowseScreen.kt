package com.vivimusic.de.ui.screens

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
import com.vivimusic.de.resources.browse
import com.vivimusic.de.resources.empty_browse
import com.vivimusic.de.ui.AppViewModel
import com.vivimusic.de.ui.ChartItemRow
import com.vivimusic.de.ui.EmptyState
import com.vivimusic.de.ui.theme.groupedItemShape
import org.jetbrains.compose.resources.stringResource

/**
 * Generic browse results screen, ported from ViVi Music's `BrowseScreen` /
 * `YouTubeBrowseScreen`: shows the album/playlist/artist grid returned for a
 * mood/genre tile.
 */
@Composable
fun BrowseScreen(
    viewModel: AppViewModel,
    browseId: String,
    params: String?,
    onBack: () -> Unit,
    onOpenAlbum: (String) -> Unit,
) {
    val browse by viewModel.browse.collectAsState()
    val loading by viewModel.exploreLoading.collectAsState()

    LaunchedEffect(browseId, params) { viewModel.openBrowse(browseId, params) }

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
                text = browse?.title?.takeIf { it.isNotBlank() } ?: stringResource(Res.string.browse),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        if (loading && browse == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        val items = browse?.items.orEmpty()
        if (items.isEmpty()) {
            EmptyState(Res.string.empty_browse)
            return@Column
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                ChartItemRow(
                    item = item,
                    viewModel = viewModel,
                    onOpenAlbum = onOpenAlbum,
                    shape = groupedItemShape(index, items.size),
                )
            }
        }
    }
}
