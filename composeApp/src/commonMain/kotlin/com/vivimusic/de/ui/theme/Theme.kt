package com.vivimusic.de.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme

/**
 * Default seed color, ported 1:1 from ViVi Music
 * (`com.music.vivi.ui.theme.DefaultThemeColor = 0xFFED5564`).
 */
val DefaultThemeColor = Color(0xFFED5564)

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
    darkTheme: Boolean = true,
    themeColor: Color = DefaultThemeColor,
    content: @Composable () -> Unit,
) {
    val colorScheme = rememberDynamicColorScheme(
        seedColor = themeColor,
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
