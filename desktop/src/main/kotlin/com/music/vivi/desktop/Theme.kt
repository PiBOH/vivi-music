package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Light / dark / follow-system theme mode, persisted in [DesktopSettings]. */
enum class ThemeMode(val key: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun from(key: String?): ThemeMode = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

data class AccentColor(val name: String, val color: Color, val key: String = name.lowercase().replace(' ', '_'))

/**
 * Accent color palette mirroring the Android app's theme colors. The first
 * entry is the "dynamic/system" sentinel (transparent) which selects the
 * default accent color.
 */
object AccentPalette {
    val default: Color = Color(0xFFED5564)

    @Volatile private var cachedSystemAccent: Color? = null

    /**
     * Best-effort "Material You" detection of the OS accent color:
     * Windows DWM accent (registry), macOS accent (defaults), GNOME accent
     * (gsettings). Returns null when the platform accent can't be read, in
     * which case the default accent is used.
     */
    fun systemAccent(): Color? {
        cachedSystemAccent?.let { return it }
        val detected = detectSystemAccent()
        cachedSystemAccent = detected
        return detected
    }

    /** Forget the cached system accent so it is re-detected on next use. */
    fun refreshSystemAccent() {
        cachedSystemAccent = null
    }

    private fun detectSystemAccent(): Color? {
        val os = System.getProperty("os.name", "").lowercase()
        return try {
            when {
                os.contains("win") -> windowsAccent()
                os.contains("mac") -> macAccent()
                os.contains("linux") -> linuxAccent()
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun runCmd(vararg cmd: String): String = try {
        ProcessBuilder(*cmd).redirectErrorStream(true).start().apply {
            waitFor(3, java.util.concurrent.TimeUnit.SECONDS)
        }.inputStream.bufferedReader().readText()
    } catch (_: Exception) {
        ""
    }

    private fun windowsAccent(): Color? {
        val out = runCmd("reg", "query", "HKCU\\Software\\Microsoft\\Windows\\DWM", "/v", "AccentColor")
        val m = Regex("0x([0-9A-Fa-f]{8})").find(out) ?: return null
        val abgr = m.groupValues[1].toLong(16)
        // DWM AccentColor is stored as 0xAABBGGRR.
        val r = (abgr and 0xFF).toInt()
        val g = ((abgr shr 8) and 0xFF).toInt()
        val b = ((abgr shr 16) and 0xFF).toInt()
        return Color(r / 255f, g / 255f, b / 255f)
    }

    private fun macAccent(): Color? {
        // AppleAccentColor: -1 graphite, 0 blue, 1 purple, 2 pink, 3 red,
        // 4 orange, 5 yellow, 6 green, 7 teal.
        val v = runCmd("defaults", "read", "-g", "AppleAccentColor").trim().toIntOrNull() ?: return null
        return when (v) {
            -1 -> Color(0xFF8E8E93)
            0 -> Color(0xFF0A84FF)
            1 -> Color(0xFFBF5AF2)
            2 -> Color(0xFFFF2D55)
            3 -> Color(0xFFFF453A)
            4 -> Color(0xFFFF9F0A)
            5 -> Color(0xFFFFD60A)
            6 -> Color(0xFF30D158)
            7 -> Color(0xFF64D2FF)
            else -> null
        }
    }

    private fun linuxAccent(): Color? {
        val out = runCmd("gsettings", "get", "org.gnome.desktop.interface", "accent-color")
        val rgb = Regex("rgba?\\((\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)").find(out)
        if (rgb != null) {
            val r = rgb.groupValues[1].toIntOrNull() ?: return null
            val g = rgb.groupValues[2].toIntOrNull() ?: return null
            val b = rgb.groupValues[3].toIntOrNull() ?: return null
            return Color(r / 255f, g / 255f, b / 255f)
        }
        val hex = Regex("'([0-9A-Fa-f]{8})'").find(out)
        if (hex != null) {
            val v = hex.groupValues[1].toLong(16)
            val r = ((v shr 24) and 0xFF).toInt()
            val g = ((v shr 16) and 0xFF).toInt()
            val b = ((v shr 8) and 0xFF).toInt()
            return Color(r / 255f, g / 255f, b / 255f)
        }
        return null
    }

    /** Resolve a sentinel (dynamic) swatch to the OS accent (or the default). */
    fun effective(color: Color): Color = if (color == Color.Transparent) (systemAccent() ?: default) else color

    val colors: List<AccentColor> = listOf(
        AccentColor("Dynamic", Color.Transparent),
        AccentColor("Crimson", Color(0xFFEC5464)),
        AccentColor("Rose", Color(0xFFD81B60)),
        AccentColor("Purple", Color(0xFF8E24AA)),
        AccentColor("Monochrome", Color(0xFF000000)),
        AccentColor("Deep Purple", Color(0xFF5E35B1)),
        AccentColor("Indigo", Color(0xFF3949AB)),
        AccentColor("Blue", Color(0xFF1E88E5)),
        AccentColor("Sky Blue", Color(0xFF039BE5)),
        AccentColor("Cyan", Color(0xFF00ACC1)),
        AccentColor("Teal", Color(0xFF00897B)),
        AccentColor("Green", Color(0xFF43A047)),
        AccentColor("Spotify", Color(0xFF1DB954)),
        AccentColor("Light Green", Color(0xFF7CB342)),
        AccentColor("Lime", Color(0xFFC0CA33)),
        AccentColor("Yellow", Color(0xFFFDD835)),
        AccentColor("Amber", Color(0xFFFFB300)),
        AccentColor("Orange", Color(0xFFFB8C00)),
        AccentColor("Deep Orange", Color(0xFFF4511E)),
        AccentColor("Brown", Color(0xFF6D4C41)),
        AccentColor("Grey", Color(0xFF757575)),
        AccentColor("Blue Grey", Color(0xFF546E7A)),
        AccentColor("Magenta", Color(0xFFC2185B)),
        AccentColor("Turquoise", Color(0xFF00BFA5)),
        AccentColor("Coral", Color(0xFFFF7043)),
        AccentColor("Lavender", Color(0xFF9575CD)),
        AccentColor("Gold", Color(0xFFFFC107)),
        AccentColor("Navy", Color(0xFF283593)),
    )
}

/** Converts HSV (hue 0..360, saturation/value 0..1) to an opaque RGB [Color]. */
fun hsvToColor(h: Float, s: Float, v: Float): Color {
    val hh = ((h % 360f) + 360f) % 360f
    val c = v * s
    val x = c * (1f - kotlin.math.abs((hh / 60f) % 2f - 1f))
    val m = v - c
    val (r, g, b) = when {
        hh < 60f -> Triple(c, x, 0f)
        hh < 120f -> Triple(x, c, 0f)
        hh < 180f -> Triple(0f, c, x)
        hh < 240f -> Triple(0f, x, c)
        hh < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(r + m, g + m, b + m)
}

/** Converts an RGB [Color] to HSV (hue 0..360, saturation/value 0..1). */
fun colorToHsv(color: Color): FloatArray {
    val r = color.red
    val g = color.green
    val b = color.blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val d = max - min
    val v = max
    val s = if (max == 0f) 0f else d / max
    var h = 0f
    if (d != 0f) {
        h = when (max) {
            r -> ((g - b) / d) % 6f
            g -> (b - r) / d + 2f
            else -> (r - g) / d + 4f
        } * 60f
        if (h < 0f) h += 360f
    }
    return floatArrayOf(h, s, v)
}

/** Packs a [Color] into an opaque ARGB [Int] (cross-platform, no Android API). */
fun colorToArgbInt(color: Color): Int {
    val a = (color.alpha * 255 + 0.5f).toInt()
    val r = (color.red * 255 + 0.5f).toInt()
    val g = (color.green * 255 + 0.5f).toInt()
    val b = (color.blue * 255 + 0.5f).toInt()
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}

/** Reconstructs a [Color] from an ARGB [Int] (cross-platform, no Android API). */
fun argbIntToColor(argb: Int): Color = Color(
    red = ((argb shr 16) and 0xFF) / 255f,
    green = ((argb shr 8) and 0xFF) / 255f,
    blue = (argb and 0xFF) / 255f,
    alpha = ((argb ushr 24) and 0xFF) / 255f,
)

/**
 * Scales the saturation (vividness) of [color] by [intensity] (0..1).
 * 1 keeps the color unchanged; 0 yields a desaturated (grey) version of the
 * same lightness. Used so the user can tune how strong the accent appears
 * without changing which hue it is.
 */
fun adjustAccentIntensity(color: Color, intensity: Float): Color {
    val i = intensity.coerceIn(0f, 1f)
    if (i >= 1f) return color
    val r = color.red
    val g = color.green
    val b = color.blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val l = (max + min) / 2f
    val d = max - min
    var s = if (d == 0f) 0f else if (l > 0.5f) d / (2f - max - min) else d / (max + min)
    s = (s * i).coerceIn(0f, 1f)
    var h = 0f
    if (d != 0f) {
        h = when (max) {
            r -> ((g - b) / d) % 6f
            g -> (b - r) / d + 2f
            else -> (r - g) / d + 4f
        } * 60f
        if (h < 0f) h += 360f
    }
    val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
    val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
    val m = l - c / 2f
    val (rr, gg, bb) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(red = rr + m, green = gg + m, blue = bb + m, alpha = color.alpha)
}

/**
 * Rotates the hue of [color] by [degrees] (0..360), keeping its saturation and
 * lightness. Used to derive expressive accent-based palettes (Spotify/Apple
 * style gradients) without leaving the Material theme's accent hue family.
 */
fun rotateHue(color: Color, degrees: Float): Color {
    val d = ((degrees % 360f) + 360f) % 360f
    if (d == 0f) return color
    val r = color.red
    val g = color.green
    val b = color.blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val l = (max + min) / 2f
    val diff = max - min
    if (diff == 0f) return color // greys have no hue to rotate
    var h = if (l > 0.5f) diff / (2f - max - min) else diff / (max + min)
    h = when (max) {
        r -> ((g - b) / diff) % 6f
        g -> (b - r) / diff + 2f
        else -> (r - g) / diff + 4f
    }
    h = (h * 60f + d) % 360f
    if (h < 0f) h += 360f
    val s = if (l > 0.5f) diff / (2f - max - min) else diff / (max + min)
    val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
    val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
    val m = l - c / 2f
    val (rr, gg, bb) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(red = rr + m, green = gg + m, blue = bb + m, alpha = color.alpha)
}

/**
 * Flat Spotify-style palette: fixed surfaces, no tonal Material 3 variation.
 * Dark: bg `#121212`, cards/panels `#181818`, hover `#282828`, secondary text
 * `#B3B3B3`. Light: bg `#FFFFFF`, panels `#F6F6F6`, text `#191414`.
 * [pureBlack] forces a true-black background in dark mode (like Spotify's
 * sidebar). The accent color stays user-selectable (it is used as the flat
 * primary, e.g. the "Spotify" green `#1DB954` when that swatch is picked).
 */
private fun spotifyScheme(isDark: Boolean, pureBlack: Boolean, accent: Color): ColorScheme {
    // The sentinel (dynamic) accent resolves to the OS accent / default.
    val seed = AccentPalette.effective(accent)
    val onAccent = if (seed.luminance() > 0.5f) Color(0xFF000000) else Color(0xFFFFFFFF)
    return if (isDark) {
        val bg = if (pureBlack) Color(0xFF000000) else Color(0xFF121212)
        darkColorScheme(
            primary = seed,
            onPrimary = onAccent,
            primaryContainer = seed,
            onPrimaryContainer = onAccent,
            secondary = Color(0xFFB3B3B3),
            onSecondary = Color(0xFF121212),
            secondaryContainer = Color(0xFF282828),
            onSecondaryContainer = Color(0xFFFFFFFF),
            background = bg,
            onBackground = Color(0xFFFFFFFF),
            surface = bg,
            onSurface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFF282828),
            onSurfaceVariant = Color(0xFFB3B3B3),
            surfaceContainerLowest = Color(0xFF0A0A0A),
            surfaceContainerLow = Color(0xFF181818),
            surfaceContainer = Color(0xFF181818),
            surfaceContainerHigh = Color(0xFF242424),
            surfaceContainerHighest = Color(0xFF282828),
            surfaceBright = Color(0xFF282828),
            surfaceDim = Color(0xFF121212),
            outline = Color(0xFF7A7A7A),
            outlineVariant = Color(0xFF282828),
            error = Color(0xFFF15E6C),
            onError = Color(0xFF000000),
            errorContainer = Color(0xFF4C0A12),
            onErrorContainer = Color(0xFFFFB4AB),
            scrim = Color(0xFF000000),
            inverseSurface = Color(0xFFFFFFFF),
            inverseOnSurface = Color(0xFF121212),
            inversePrimary = seed,
            surfaceTint = seed,
        )
    } else {
        lightColorScheme(
            primary = seed,
            onPrimary = onAccent,
            primaryContainer = seed,
            onPrimaryContainer = onAccent,
            secondary = Color(0xFF616161),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFECECEC),
            onSecondaryContainer = Color(0xFF191414),
            background = Color(0xFFFFFFFF),
            onBackground = Color(0xFF191414),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF191414),
            surfaceVariant = Color(0xFFECECEC),
            onSurfaceVariant = Color(0xFF616161),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFF6F6F6),
            surfaceContainer = Color(0xFFF6F6F6),
            surfaceContainerHigh = Color(0xFFECECEC),
            surfaceContainerHighest = Color(0xFFE4E4E4),
            surfaceBright = Color(0xFFFFFFFF),
            surfaceDim = Color(0xFFE8E8E8),
            outline = Color(0xFFA6A6A6),
            outlineVariant = Color(0xFFE4E4E4),
            error = Color(0xFFE2211A),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002),
            scrim = Color(0xFF000000),
            inverseSurface = Color(0xFF191414),
            inverseOnSurface = Color(0xFFFFFFFF),
            inversePrimary = seed,
            surfaceTint = seed,
        )
    }
}

/** Flat 8dp corners everywhere (Spotify uses small, consistent radii). */
private val spotifyShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(8.dp),
)

/**
 * Applies the selected light/dark mode (resolving "system" against the OS)
 * and accent color to the whole app via [MaterialTheme]. When [spotify] is
 * true the app uses the flat Spotify palette (fixed green accent, 8dp shapes,
 * bolder typography) instead of the tonal Material 3 scheme.
 */
@Composable
fun AppTheme(
    mode: ThemeMode,
    accent: Color,
    pureBlack: Boolean = false,
    font: AppFont = AppFont.SYSTEM,
    spotify: Boolean = false,
    accentIntensity: Float = 1f,
    customFontPath: String? = null,
    content: @Composable () -> Unit,
) {
    val useDark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    // Resolve the sentinel (dynamic) accent, then apply the user-set intensity
    // (saturation) so it seeds both the flat Spotify primary and the tonal
    // Material scheme.
    val appliedAccent = adjustAccentIntensity(AccentPalette.effective(accent), accentIntensity)
    val colorScheme = if (spotify) {
        // Flat Spotify palette — no tonal engine; the accent is the flat primary.
        spotifyScheme(useDark, pureBlack && useDark, appliedAccent)
    } else {
        // Same seed-based tonal palette as the Android app (TonalSpot +
        // SPEC_2025), so the desktop colors match the mobile app pixel-perfectly.
        rememberDynamicColorScheme(
            seedColor = appliedAccent,
            isDark = useDark,
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
            style = PaletteStyle.TonalSpot,
        )
    }
    // "Pure black" replaces the tonal dark surfaces with a true black background.
    val effective = if (!spotify && pureBlack && useDark) {
        colorScheme.copy(background = Color.Black, surface = Color.Black)
    } else {
        colorScheme
    }
    // Apply the selected app font to every text style (headlines down to labels);
    // in Spotify mode the titles/labels are bolder, like the Spotify UI.
    val typography = remember(font, spotify, customFontPath) {
        val family = AppFonts.familyFor(font, customFontPath)
        val base = Typography()
        val applied = Typography(
            displayLarge = base.displayLarge.copy(fontFamily = family),
            displayMedium = base.displayMedium.copy(fontFamily = family),
            displaySmall = base.displaySmall.copy(fontFamily = family),
            headlineLarge = base.headlineLarge.copy(fontFamily = family),
            headlineMedium = base.headlineMedium.copy(fontFamily = family),
            headlineSmall = base.headlineSmall.copy(fontFamily = family),
            titleLarge = base.titleLarge.copy(fontFamily = family),
            titleMedium = base.titleMedium.copy(fontFamily = family),
            titleSmall = base.titleSmall.copy(fontFamily = family),
            bodyLarge = base.bodyLarge.copy(fontFamily = family),
            bodyMedium = base.bodyMedium.copy(fontFamily = family),
            bodySmall = base.bodySmall.copy(fontFamily = family),
            labelLarge = base.labelLarge.copy(fontFamily = family),
            labelMedium = base.labelMedium.copy(fontFamily = family),
            labelSmall = base.labelSmall.copy(fontFamily = family),
        )
        if (!spotify) {
            applied
        } else {
            applied.copy(
                displayLarge = applied.displayLarge.copy(fontWeight = FontWeight.Bold),
                displayMedium = applied.displayMedium.copy(fontWeight = FontWeight.Bold),
                displaySmall = applied.displaySmall.copy(fontWeight = FontWeight.Bold),
                headlineLarge = applied.headlineLarge.copy(fontWeight = FontWeight.Bold),
                headlineMedium = applied.headlineMedium.copy(fontWeight = FontWeight.Bold),
                headlineSmall = applied.headlineSmall.copy(fontWeight = FontWeight.Bold),
                titleLarge = applied.titleLarge.copy(fontWeight = FontWeight.Bold),
                titleMedium = applied.titleMedium.copy(fontWeight = FontWeight.Bold),
                titleSmall = applied.titleSmall.copy(fontWeight = FontWeight.Bold),
                labelLarge = applied.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                labelMedium = applied.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                labelSmall = applied.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            )
        }
    }
    MaterialTheme(
        colorScheme = effective,
        typography = typography,
        shapes = if (spotify) spotifyShapes else Shapes(),
    ) {
        // Material3's MaterialTheme does NOT set LocalContentColor, so any Text
        // without an explicit color would fall back to the default (black) and
        // never adapt to the theme. Provide it explicitly so text follows the
        // onBackground color, then paint the whole window with the theme
        // background (otherwise the native light window shows through in dark
        // mode).
        CompositionLocalProvider(LocalContentColor provides effective.onBackground) {
            Box(Modifier.fillMaxSize().background(effective.background)) {
                content()
            }
        }
    }
}
