package com.vivimusic.de.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.vivimusic.de.resources.Res
import com.vivimusic.de.resources.nav_history
import com.vivimusic.de.resources.empty_history
import com.vivimusic.de.ui.AppViewModel
import com.vivimusic.de.ui.EmptyState
import com.vivimusic.de.ui.NavigationTitle
import com.vivimusic.de.ui.SongRow
import com.vivimusic.de.ui.theme.groupedItemShape
import org.jetbrains.compose.resources.stringResource

/**
 * Listening history, ported from ViVi Music's `HistoryScreen`: a title and the
 * flat list of recently played songs.
 */
@Composable
fun HistoryScreen(viewModel: AppViewModel) {
    val history by viewModel.history.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        NavigationTitle(title = stringResource(Res.string.nav_history))
        if (history.isEmpty()) {
            EmptyState(Res.string.empty_history)
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(history, key = { _, song -> song.id }) { index, song ->
                    SongRow(
                        song = song,
                        viewModel = viewModel,
                        shape = groupedItemShape(index, history.size),
                    )
                }
            }
        }
    }
}
