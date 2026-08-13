package com.vivimusic.de.ui.screens.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vivimusic.de.resources.*
import com.vivimusic.de.data.update.UpdateDownloadState
import com.vivimusic.de.ui.AppViewModel
import org.jetbrains.compose.resources.stringResource

/** Update settings: pre-release opt-in plus manual check and download. */
@Composable
fun UpdateSettings(viewModel: AppViewModel, onBack: () -> Unit) {
    val checkPrereleases by viewModel.checkPrereleases.collectAsState()
    val updateStatus by viewModel.updateStatus.collectAsState()
    val downloadState by viewModel.updateDownloadState.collectAsState()

    SettingsPage(
        title = stringResource(Res.string.update_settings),
        onBack = onBack,
    ) {
        SettingsGroup {
            SettingsItem(
                title = stringResource(Res.string.update_check_prereleases),
                checked = checkPrereleases,
                onCheckedChange = viewModel::setCheckPrereleases,
            )
            SettingsDivider()
            SettingsItem(
                title = stringResource(Res.string.update_check_now),
                onClick = viewModel::checkForUpdates,
            )
        }

        val status = updateStatus
        if (status != null) {
            Text(
                text = if (status.updateAvailable) {
                    "${stringResource(Res.string.update_available)}: ${status.latest?.tagName ?: ""}"
                } else {
                    stringResource(Res.string.update_up_to_date)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            )
            if (status.updateAvailable) {
                val isDownloading = downloadState is UpdateDownloadState.Downloading
                val error = (downloadState as? UpdateDownloadState.Error)?.message
                if (error != null) {
                    Text(
                        text = "${stringResource(Res.string.update_download_error)} $error",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { status.latest?.let(viewModel::downloadAndInstallUpdate) },
                    enabled = !isDownloading,
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    if (isDownloading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(Res.string.update_downloading))
                        }
                    } else {
                        Text(
                            if (error != null) stringResource(Res.string.retry)
                            else stringResource(Res.string.update_download),
                        )
                    }
                }
                if (downloadState is UpdateDownloadState.Launched) {
                    Text(
                        text = stringResource(Res.string.update_launched),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
            }
        }
    }
}
