package com.vivimusic.de.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vivimusic.de.data.AppConfig
import com.vivimusic.de.data.sync.SyncStatus
import com.vivimusic.de.data.writeSetting
import com.vivimusic.de.i18n.customAppLocale
import com.vivimusic.de.i18n.supportedLanguages
import com.vivimusic.de.resources.*
import com.vivimusic.de.ui.AppViewModel
import com.vivimusic.de.ui.AxolotlMascot
import org.jetbrains.compose.resources.stringResource

private const val LANGUAGE_KEY = "app.language"

@Composable
fun SettingsScreen(viewModel: AppViewModel) {
    val syncStatus by viewModel.syncStatus.collectAsState()
    var langMenuExpanded by remember { mutableStateOf(false) }

    val selectedLanguage = customAppLocale?.let { code ->
        supportedLanguages.firstOrNull { it.code == code }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    ) {
        Text(
            text = stringResource(Res.string.settings_language),
            style = MaterialTheme.typography.titleMedium,
        )
        Box {
            OutlinedButton(onClick = { langMenuExpanded = true }) {
                Text(selectedLanguage?.nativeName ?: stringResource(Res.string.system_language))
            }
            DropdownMenu(expanded = langMenuExpanded, onDismissRequest = { langMenuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.system_language)) },
                    onClick = {
                        customAppLocale = null
                        writeSetting(LANGUAGE_KEY, "")
                        langMenuExpanded = false
                    },
                )
                supportedLanguages.forEach { language ->
                    DropdownMenuItem(
                        text = { Text(language.nativeName) },
                        onClick = {
                            customAppLocale = language.code
                            writeSetting(LANGUAGE_KEY, language.code)
                            langMenuExpanded = false
                        },
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text(
            text = stringResource(Res.string.sync_status),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = syncStatusLabel(syncStatus),
            style = MaterialTheme.typography.bodyMedium,
        )
        TextButton(onClick = { viewModel.syncNow() }) {
            Text(stringResource(Res.string.retry))
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text(
            text = stringResource(Res.string.about_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            AxolotlMascot(modifier = Modifier.size(96.dp))
        }
        Text(
            text = "${stringResource(Res.string.about_version)} ${AppConfig.appVersion}",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = stringResource(Res.string.about_credits),
            style = MaterialTheme.typography.bodySmall,
        )
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
