package com.vivimusic.de.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.vivimusic.de.data.writeSetting
import com.vivimusic.de.resources.*
import com.vivimusic.de.ui.theme.DefaultThemeColor
import com.vivimusic.de.ui.theme.appThemeColor
import com.vivimusic.de.ui.theme.appThemeMode
import com.vivimusic.de.ui.theme.applyAccentColor
import com.vivimusic.de.ui.theme.applyThemeMode
import org.jetbrains.compose.resources.stringResource

private val AccentPresets = listOf(
    DefaultThemeColor,
    Color(0xFFF44336),
    Color(0xFFE91E63),
    Color(0xFF9C27B0),
    Color(0xFF673AB7),
    Color(0xFF3F51B5),
    Color(0xFF2196F3),
    Color(0xFF009688),
    Color(0xFF4CAF50),
    Color(0xFFFF9800),
)

/** Appearance settings: theme mode (system/light/dark) and accent color. */
@Composable
fun AppearanceSettings(onBack: () -> Unit) {
    var showThemeDialog by remember { mutableStateOf(false) }

    val themeLabel = when (appThemeMode) {
        "light" -> stringResource(Res.string.theme_light)
        "dark" -> stringResource(Res.string.theme_dark)
        else -> stringResource(Res.string.theme_system)
    }

    SettingsPage(
        title = stringResource(Res.string.appearance),
        onBack = onBack,
    ) {
        SettingsGroup(title = stringResource(Res.string.appearance_theme)) {
            SettingsItem(
                title = stringResource(Res.string.appearance_theme),
                trailingText = themeLabel,
                onClick = { showThemeDialog = true },
            )
        }

        SettingsGroup(title = stringResource(Res.string.appearance_accent)) {
            ColorSwatches(
                colors = AccentPresets,
                selected = appThemeColor,
                onSelect = { color ->
                    applyAccentColor(color)
                    writeSetting("appearance.accent", color.value.toLong().toString())
                },
            )
        }
    }

    if (showThemeDialog) {
        ChoiceDialog(
            title = stringResource(Res.string.appearance_theme),
            options = listOf(
                "system" to stringResource(Res.string.theme_system),
                "light" to stringResource(Res.string.theme_light),
                "dark" to stringResource(Res.string.theme_dark),
            ),
            selectedValue = appThemeMode,
            onSelect = { mode ->
                applyThemeMode(mode)
                writeSetting("appearance.theme", mode)
            },
            onDismiss = { showThemeDialog = false },
        )
    }
}
