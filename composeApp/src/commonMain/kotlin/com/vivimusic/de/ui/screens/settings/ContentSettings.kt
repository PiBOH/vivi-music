package com.vivimusic.de.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.vivimusic.de.data.writeSetting
import com.vivimusic.de.i18n.customAppLocale
import com.vivimusic.de.i18n.supportedLanguages
import com.vivimusic.de.resources.*
import org.jetbrains.compose.resources.stringResource

private const val LANGUAGE_KEY = "app.language"

/** Content settings: currently the app language selection. */
@Composable
fun ContentSettings(onBack: () -> Unit) {
    var showLanguageDialog by remember { mutableStateOf(false) }

    val selected = customAppLocale?.let { code ->
        supportedLanguages.firstOrNull { it.code == code }
    }
    val currentLabel = selected?.nameInOwnLanguage ?: stringResource(Res.string.system_language)

    SettingsPage(
        title = stringResource(Res.string.content),
        onBack = onBack,
    ) {
        SettingsGroup(title = stringResource(Res.string.settings_language)) {
            SettingsItem(
                title = stringResource(Res.string.settings_language),
                trailingText = currentLabel,
                onClick = { showLanguageDialog = true },
            )
        }
    }

    if (showLanguageDialog) {
        ChoiceDialog(
            title = stringResource(Res.string.settings_language),
            options = listOf("" to stringResource(Res.string.system_language)) +
                supportedLanguages.map { it.code to it.nameInOwnLanguage },
            selectedValue = customAppLocale ?: "",
            onSelect = { code ->
                customAppLocale = if (code.isEmpty()) null else code
                writeSetting(LANGUAGE_KEY, code)
            },
            onDismiss = { showLanguageDialog = false },
        )
    }
}
