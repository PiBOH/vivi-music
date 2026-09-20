package com.vivimusic.de.ui.screens.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vivimusic.de.resources.*
import com.vivimusic.de.ui.AppViewModel
import org.jetbrains.compose.resources.stringResource

/** Storage settings: clear local search/playback data. */
@Composable
fun StorageSettings(viewModel: AppViewModel, onBack: () -> Unit) {
    var confirmSearch by remember { mutableStateOf(false) }
    var confirmHistory by remember { mutableStateOf(false) }

    SettingsPage(
        title = stringResource(Res.string.storage),
        onBack = onBack,
    ) {
        SettingsGroup {
            SettingsItem(
                title = stringResource(Res.string.storage_clear_search),
                onClick = { confirmSearch = true },
            )
            SettingsDivider()
            SettingsItem(
                title = stringResource(Res.string.storage_clear_history),
                onClick = { confirmHistory = true },
            )
        }

        Text(
            text = stringResource(Res.string.storage_location),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
    }

    if (confirmSearch) {
        ConfirmDialog(
            title = stringResource(Res.string.storage_clear_search),
            message = stringResource(Res.string.storage_clear_search_message),
            confirmLabel = stringResource(Res.string.clear),
            onConfirm = viewModel::clearSearchHistory,
            onDismiss = { confirmSearch = false },
        )
    }

    if (confirmHistory) {
        ConfirmDialog(
            title = stringResource(Res.string.storage_clear_history),
            message = stringResource(Res.string.storage_clear_history_message),
            confirmLabel = stringResource(Res.string.clear),
            onConfirm = viewModel::clearHistory,
            onDismiss = { confirmHistory = false },
        )
    }
}
