package com.vivimusic.de.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Translate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.vivimusic.de.resources.*
import com.vivimusic.de.ui.AppViewModel
import com.vivimusic.de.ui.screens.settings.AboutSettings
import com.vivimusic.de.ui.screens.settings.AppearanceSettings
import com.vivimusic.de.ui.screens.settings.ContentSettings
import com.vivimusic.de.ui.screens.settings.PlayerSettings
import com.vivimusic.de.ui.screens.settings.PrivacySettings
import com.vivimusic.de.ui.screens.settings.SettingsDivider
import com.vivimusic.de.ui.screens.settings.SettingsGroup
import com.vivimusic.de.ui.screens.settings.SettingsItem
import com.vivimusic.de.ui.screens.settings.SettingsPage
import com.vivimusic.de.ui.screens.settings.StorageSettings
import com.vivimusic.de.ui.screens.settings.UpdateSettings
import org.jetbrains.compose.resources.stringResource

private enum class SettingsDestination {
    Update,
    Appearance,
    Player,
    Content,
    Privacy,
    Storage,
    About,
}

/**
 * Settings entry point, ported from ViVi Music's `SettingsScreen`: a grouped
 * list of destinations that open nested settings pages. Desktop-adapted: no
 * Android-specific entries (account, cast, data saver, backup) and a wider,
 * centered content column.
 */
@Composable
fun SettingsScreen(viewModel: AppViewModel) {
    var destination by remember { mutableStateOf<SettingsDestination?>(null) }

    when (destination) {
        null -> SettingsHub(viewModel, onOpen = { destination = it })
        SettingsDestination.Update -> UpdateSettings(viewModel, onBack = { destination = null })
        SettingsDestination.Appearance -> AppearanceSettings(onBack = { destination = null })
        SettingsDestination.Player -> PlayerSettings(onBack = { destination = null })
        SettingsDestination.Content -> ContentSettings(onBack = { destination = null })
        SettingsDestination.Privacy -> PrivacySettings(viewModel, onBack = { destination = null })
        SettingsDestination.Storage -> StorageSettings(viewModel, onBack = { destination = null })
        SettingsDestination.About -> AboutSettings(viewModel = viewModel, onBack = { destination = null })
    }
}

@Composable
private fun SettingsHub(viewModel: AppViewModel, onOpen: (SettingsDestination) -> Unit) {
    val updateStatus by viewModel.updateStatus.collectAsState()
    val updateLabel = if (updateStatus?.updateAvailable == true) {
        stringResource(Res.string.update_available)
    } else {
        stringResource(Res.string.update_up_to_date)
    }

    SettingsPage(title = stringResource(Res.string.nav_settings)) {
        SettingsGroup {
            SettingsItem(
                title = stringResource(Res.string.update_settings),
                description = updateLabel,
                icon = Icons.Filled.Refresh,
                highlighted = updateStatus?.updateAvailable == true,
                onClick = { onOpen(SettingsDestination.Update) },
            )
            SettingsDivider()
            SettingsItem(
                title = stringResource(Res.string.appearance),
                icon = Icons.Filled.Palette,
                onClick = { onOpen(SettingsDestination.Appearance) },
            )
            SettingsDivider()
            SettingsItem(
                title = stringResource(Res.string.player_and_audio),
                icon = Icons.Filled.MusicNote,
                onClick = { onOpen(SettingsDestination.Player) },
            )
            SettingsDivider()
            SettingsItem(
                title = stringResource(Res.string.content),
                icon = Icons.Filled.Translate,
                onClick = { onOpen(SettingsDestination.Content) },
            )
            SettingsDivider()
            SettingsItem(
                title = stringResource(Res.string.privacy),
                icon = Icons.Filled.Lock,
                onClick = { onOpen(SettingsDestination.Privacy) },
            )
            SettingsDivider()
            SettingsItem(
                title = stringResource(Res.string.storage),
                icon = Icons.Filled.Storage,
                onClick = { onOpen(SettingsDestination.Storage) },
            )
            SettingsDivider()
            SettingsItem(
                title = stringResource(Res.string.about_title),
                icon = Icons.Filled.Info,
                onClick = { onOpen(SettingsDestination.About) },
            )
        }
    }
}
