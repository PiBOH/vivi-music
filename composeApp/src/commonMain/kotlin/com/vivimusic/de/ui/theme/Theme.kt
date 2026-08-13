package com.vivimusic.de.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.vivimusic.de.data.readSetting

/**
 * Default seed color, ported 1:1 from ViVi Music
 * (`com.music.vivi.ui.theme.DefaultThemeColor = 0xFFED5564`).
 */
val DefaultThemeColor = Color(0xFFED5564)

/**
 * User-selected theme mode, persisted under `appearance.theme`. One of
 * "system", "light" or "dark" (defaults to "system").
 */
var appThemeMode by mutableStateOf(readSetting("appearance.theme") ?: "system")
    private set

/** User-selected accent color, persisted under `appearance.accent` (ARGB). */
var appThemeColor by mutableStateOf(
    readSetting("appearance.accent")?.toLongOrNull()?.let { Color(it) } ?: DefaultThemeColor,
)
    private set

/** Applies a persisted theme mode and accent color. */
fun applyThemeMode(mode: String) {
    appThemeMode = mode
}

/** Applies a persisted accent color. */
fun applyAccentColor(color: Color) {
    appThemeColor = color
}

/**
 * App theme, ported from ViVi Music's `vivimusicTheme`.
 *
 * The mobile app wraps everything in `MaterialExpressiveTheme` with
 * `MotionScheme.expressive()`. Compose Multiplatform's `material3` currently
 * marks those two symbols as `internal`, so this port applies the same color
 * scheme (materialKolor, SPEC 2025, TonalSpot) and typography through the
 * public `MaterialTheme` entry point and relies on the expressive components
 * (`NavigationBar`, `SecondaryTabRow`, ...) for the expressive look. The
 * motion scheme can be switched on once CMP exposes `MotionScheme` publicly.
 */
@Composable
fun ViviTheme(
    content: @Composable () -> Unit,
) {
    val darkTheme = when (appThemeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    val colorScheme = rememberDynamicColorScheme(
        seedColor = appThemeColor,
        isDark = darkTheme,
        specVersion = ColorSpec.SpecVersion.SPEC_2025,
        style = PaletteStyle.TonalSpot,
    )
    MaterialTheme(
        colorScheme = colorScheme,
        typography = appTypography(),
        content = content,
    )
}
