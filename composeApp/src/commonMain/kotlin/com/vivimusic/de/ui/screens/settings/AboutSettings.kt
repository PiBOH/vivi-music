package com.vivimusic.de.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vivimusic.de.data.AppConfig
import com.vivimusic.de.data.update.openUrl
import com.vivimusic.de.resources.*
import com.vivimusic.de.ui.AppViewModel
import com.vivimusic.de.ui.AxolotlMascot
import org.jetbrains.compose.resources.stringResource

private const val REPO_URL = "https://github.com/PiBOH/vivi-music-de"

/** About screen: mascot, version, credits, changelog and source link. */
@Composable
fun AboutSettings(viewModel: AppViewModel, onBack: () -> Unit) {
    var showChangelog by remember { mutableStateOf(false) }

    if (showChangelog) {
        ChangelogScreen(viewModel, onBack = { showChangelog = false })
        return
    }

    SettingsPage(
        title = stringResource(Res.string.about_title),
        onBack = onBack,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        ) {
            AxolotlMascot(modifier = Modifier.size(112.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "${stringResource(Res.string.app_name)} ${AppConfig.appVersion}",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.about_credits),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        SettingsGroup {
            SettingsItem(
                title = stringResource(Res.string.about_changelog),
                onClick = { showChangelog = true },
            )
            SettingsDivider()
            SettingsItem(
                title = stringResource(Res.string.about_source),
                description = stringResource(Res.string.about_source_desc),
                onClick = { openUrl(REPO_URL) },
            )
        }
    }
}
