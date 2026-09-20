package com.vivimusic.de.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.vivimusic.de.data.sync.SyncStatus
import com.vivimusic.de.resources.*
import com.vivimusic.de.ui.AppViewModel
import org.jetbrains.compose.resources.stringResource

/** Privacy settings: data synchronization control and status. */
@Composable
fun PrivacySettings(viewModel: AppViewModel, onBack: () -> Unit) {
    val syncEnabled by viewModel.syncEnabled.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()

    SettingsPage(
        title = stringResource(Res.string.privacy),
        onBack = onBack,
    ) {
        SettingsGroup(title = stringResource(Res.string.privacy_sync)) {
            SettingsItem(
                title = stringResource(Res.string.privacy_sync),
                description = stringResource(Res.string.privacy_sync_desc),
                checked = syncEnabled,
                onCheckedChange = viewModel::setSyncEnabled,
            )
            SettingsDivider()
            SettingsItem(
                title = stringResource(Res.string.sync_status),
                trailingText = syncStatusLabel(syncStatus),
            )
            SettingsDivider()
            SettingsItem(
                title = stringResource(Res.string.sync_now),
                onClick = viewModel::syncNow,
            )
        }
    }
}

@Composable
private fun syncStatusLabel(status: SyncStatus): String = when (status) {
    SyncStatus.Disabled -> stringResource(Res.string.sync_disabled)
    SyncStatus.Offline -> stringResource(Res.string.sync_offline)
    SyncStatus.Syncing -> stringResource(Res.string.sync_syncing)
    SyncStatus.Synced -> stringResource(Res.string.sync_synced)
    SyncStatus.Error -> stringResource(Res.string.sync_error)
}
