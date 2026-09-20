package com.vivimusic.de.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec
import com.vivimusic.de.data.readSetting
import com.vivimusic.de.data.writeSetting

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
    readSetting("appearance.accent")?.toLongOrNull()?.let { Color(it.toULong()) } ?: DefaultThemeColor,
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

// ----- color-scheme cache -----
//
// materialKolor's `dynamicColorScheme` builds the whole Material 3 scheme from
// a seed via HCT/CAM16 conversions. That is deterministic per (seed, isDark),
// so the generated `ColorScheme` is cached in memory and on disk to avoid
// recomputing it on every launch (and every theme toggle). The disk copy is
// purely an optimization: if it is missing, stale or unreadable, the scheme is
// recomputed and the cache refreshed.

private const val SCHEME_CACHE_SEED = "appearance.schemeCache.seed"
private const val SCHEME_CACHE_LIGHT = "appearance.schemeCache.light"
private const val SCHEME_CACHE_DARK = "appearance.schemeCache.dark"
private const val SCHEME_COLOR_COUNT = 48

private val schemeMemoryCache = mutableMapOf<Long, MutableMap<Boolean, ColorScheme>>()

private fun cachedScheme(seed: Color, isDark: Boolean): ColorScheme {
    val seedArgb = seed.value.toLong()
    schemeMemoryCache[seedArgb]?.get(isDark)?.let { return it }

    val diskKey = if (isDark) SCHEME_CACHE_DARK else SCHEME_CACHE_LIGHT
    val disk = if (readSetting(SCHEME_CACHE_SEED)?.toLongOrNull() == seedArgb) {
        decodeScheme(readSetting(diskKey))
    } else {
        null
    }

    val scheme = disk ?: computeScheme(seed, isDark).also { computed ->
        // Persist the freshly computed scheme (best effort; never let a cache
        // write break rendering).
        runCatching {
            writeSetting(SCHEME_CACHE_SEED, seedArgb.toString())
            writeSetting(diskKey, encodeScheme(computed))
        }
    }

    schemeMemoryCache.getOrPut(seedArgb) { mutableMapOf() }[isDark] = scheme
    return scheme
}

private fun computeScheme(seed: Color, isDark: Boolean): ColorScheme =
    dynamicColorScheme(
        seedColor = seed,
        isDark = isDark,
        specVersion = ColorSpec.SpecVersion.SPEC_2025,
        style = PaletteStyle.TonalSpot,
    )

private fun encodeScheme(scheme: ColorScheme): String = listOf(
    scheme.primary, scheme.onPrimary, scheme.primaryContainer, scheme.onPrimaryContainer,
    scheme.inversePrimary, scheme.secondary, scheme.onSecondary, scheme.secondaryContainer,
    scheme.onSecondaryContainer, scheme.tertiary, scheme.onTertiary, scheme.tertiaryContainer,
    scheme.onTertiaryContainer, scheme.background, scheme.onBackground, scheme.surface,
    scheme.onSurface, scheme.surfaceVariant, scheme.onSurfaceVariant, scheme.surfaceTint,
    scheme.inverseSurface, scheme.inverseOnSurface, scheme.error, scheme.onError,
    scheme.errorContainer, scheme.onErrorContainer, scheme.outline, scheme.outlineVariant,
    scheme.scrim, scheme.surfaceBright, scheme.surfaceDim, scheme.surfaceContainer,
    scheme.surfaceContainerHigh, scheme.surfaceContainerHighest, scheme.surfaceContainerLow,
    scheme.surfaceContainerLowest, scheme.primaryFixed, scheme.primaryFixedDim,
    scheme.onPrimaryFixed, scheme.onPrimaryFixedVariant, scheme.secondaryFixed,
    scheme.secondaryFixedDim, scheme.onSecondaryFixed, scheme.onSecondaryFixedVariant,
    scheme.tertiaryFixed, scheme.tertiaryFixedDim, scheme.onTertiaryFixed,
    scheme.onTertiaryFixedVariant,
).joinToString(",") { it.value.toLong().toString() }

private fun decodeScheme(encoded: String?): ColorScheme? {
    if (encoded.isNullOrBlank()) return null
    val longs = encoded.split(",").mapNotNull { it.toLongOrNull() }
    if (longs.size != SCHEME_COLOR_COUNT) return null
    val c = longs.map { Color(it.toULong()) }
    return ColorScheme(
        primary = c[0], onPrimary = c[1], primaryContainer = c[2], onPrimaryContainer = c[3],
        inversePrimary = c[4], secondary = c[5], onSecondary = c[6], secondaryContainer = c[7],
        onSecondaryContainer = c[8], tertiary = c[9], onTertiary = c[10], tertiaryContainer = c[11],
        onTertiaryContainer = c[12], background = c[13], onBackground = c[14], surface = c[15],
        onSurface = c[16], surfaceVariant = c[17], onSurfaceVariant = c[18], surfaceTint = c[19],
        inverseSurface = c[20], inverseOnSurface = c[21], error = c[22], onError = c[23],
        errorContainer = c[24], onErrorContainer = c[25], outline = c[26], outlineVariant = c[27],
        scrim = c[28], surfaceBright = c[29], surfaceDim = c[30], surfaceContainer = c[31],
        surfaceContainerHigh = c[32], surfaceContainerHighest = c[33], surfaceContainerLow = c[34],
        surfaceContainerLowest = c[35], primaryFixed = c[36], primaryFixedDim = c[37],
        onPrimaryFixed = c[38], onPrimaryFixedVariant = c[39], secondaryFixed = c[40],
        secondaryFixedDim = c[41], onSecondaryFixed = c[42], onSecondaryFixedVariant = c[43],
        tertiaryFixed = c[44], tertiaryFixedDim = c[45], onTertiaryFixed = c[46],
        onTertiaryFixedVariant = c[47],
    )
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
    val colorScheme = remember(appThemeColor, darkTheme) {
        cachedScheme(appThemeColor, darkTheme)
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = appTypography(),
    ) {
        // Material3 components normally provide their own content color, but
        // plain Text/Icon calls inherit LocalContentColor. Set the app-wide
        // default explicitly so unspecified content never falls back to the
        // platform's black color in dark mode.
        CompositionLocalProvider(
            LocalContentColor provides colorScheme.onBackground,
            content = content,
        )
    }
}
