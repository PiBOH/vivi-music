package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ViewSidebar
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Brightness1
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.roundToInt

/**
 * Appearance hub: rows mirroring the Android app's Appearance sub-menu.
 * Single-choice options (font, canvas source, density, transitions, player
 * design) are inline dropdowns — no extra sub-screen, matching mobile.
 */
@Composable
fun AppearanceSection(
    language: String,
    selectedFont: AppFont,
    onFontChange: (AppFont) -> Unit,
    customFontPath: String = "",
    onImportFont: () -> Unit = {},
    canvasEnabled: Boolean = true,
    onCanvasEnabledChange: (Boolean) -> Unit = {},
    canvasSource: CanvasSource = CanvasSource.AUTO,
    onCanvasSourceChange: (CanvasSource) -> Unit = {},
    densityScale: Float = 1f,
    onDensityScaleChange: (Float) -> Unit = {},
    gridItemSize: Int = 160,
    onGridItemSizeChange: (Int) -> Unit = {},
    screenTransition: String = "fade",
    onScreenTransitionChange: (String) -> Unit = {},
    playerDesign: PlayerDesign = PlayerDesign.CLASSIC,
    onPlayerDesignChange: (PlayerDesign) -> Unit = {},
    playerBackground: PlayerBackgroundStyle = PlayerBackgroundStyle.CANVAS,
    onPlayerBackgroundChange: (PlayerBackgroundStyle) -> Unit = {},
    rotatingThumbnail: Boolean = false,
    onRotatingThumbnailChange: (Boolean) -> Unit = {},
    miniPlayerDesign: MiniPlayerDesign = MiniPlayerDesign.CLASSIC,
    onMiniPlayerDesignChange: (MiniPlayerDesign) -> Unit = {},
    miniPlayerBackgroundStyle: MiniPlayerBackgroundStyle = MiniPlayerBackgroundStyle.FOLLOW_THEME,
    onMiniPlayerBackgroundStyleChange: (MiniPlayerBackgroundStyle) -> Unit = {},
    pureBlackMiniPlayer: Boolean = false,
    onPureBlackMiniPlayerChange: (Boolean) -> Unit = {},
    onOpenTheme: () -> Unit,
    onOpenIntro: () -> Unit,
    nativeTitleBar: Boolean = false,
    onNativeTitleBarChange: (Boolean) -> Unit = {},
    showRightSidebar: Boolean = true,
    onShowRightSidebarChange: (Boolean) -> Unit = {},
    onRestart: () -> Unit = {},
) {
    var showRestartDialog by remember { mutableStateOf(false) }

    val fontOptions = buildList {
        AppFont.entries.filter { it != AppFont.CUSTOM }.forEach { f ->
            add(f to Localization.get(language, when (f) {
                AppFont.SYSTEM -> "font_system"
                AppFont.GOOGLE_SANS -> "font_google_sans"
                AppFont.SANS_FLEX -> "font_sans_flex"
                AppFont.OUTFIT -> "font_outfit"
                AppFont.PLUS_JAKARTA_SANS -> "font_plus_jakarta_sans"
                AppFont.CUSTOM -> "custom_font"
            }))
        }
        if (customFontPath.isNotBlank()) {
            add(AppFont.CUSTOM to Localization.get(language, "custom_font"))
        }
    }

    Text(Localization.get(language, "appearance"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))

    M3SettingsGroup(
        items = listOf(
            M3SettingsItem(
                icon = Icons.Filled.Palette,
                title = { Text(Localization.get(language, "theme_colors")) },
                trailing = { SettingsChevron() },
                onClick = onOpenTheme,
            ),
        ),
    )

    M3SettingsDropdownItem(
        icon = Icons.Filled.FontDownload,
        title = Localization.get(language, "app_font"),
        value = Localization.get(language, when (selectedFont) {
            AppFont.SYSTEM -> "font_system"
            AppFont.GOOGLE_SANS -> "font_google_sans"
            AppFont.SANS_FLEX -> "font_sans_flex"
            AppFont.OUTFIT -> "font_outfit"
            AppFont.PLUS_JAKARTA_SANS -> "font_plus_jakarta_sans"
            AppFont.CUSTOM -> "custom_font"
        }),
        options = fontOptions.map { (f, label) -> f.value to label },
        onSelect = { v -> onFontChange(AppFont.fromValue(v)) },
    )

    // Import a custom .ttf/.otf font file (shown when one is installed).
    M3SettingsGroup(
        items = listOf(
            M3SettingsItem(
                icon = Icons.Filled.FontDownload,
                title = { Text(Localization.get(language, "import_font")) },
                description = customFontPath.takeIf { it.isNotBlank() }?.let {
                    { Text(it.substringAfterLast("\\").substringAfterLast("/")) }
                },
                trailing = { SettingsChevron() },
                onClick = onImportFont,
            ),
        ),
    )

    M3SettingsGroup(
        items = listOf(
            M3SettingsItem(
                icon = Icons.Filled.Movie,
                title = { Text(Localization.get(language, "use_canvas")) },
                description = { Text(Localization.get(language, "vivimusic_canvas_desc")) },
                trailing = {
                    Switch(checked = canvasEnabled, onCheckedChange = onCanvasEnabledChange)
                },
                onClick = { onCanvasEnabledChange(!canvasEnabled) },
            ),
        ),
    )

    M3SettingsDropdownItem(
        icon = Icons.Filled.Movie,
        title = Localization.get(language, "canvas_source"),
        value = Localization.get(language, when (canvasSource) {
            CanvasSource.AUTO -> "canvas_source_auto"
            CanvasSource.APPLE_MUSIC -> "canvas_source_apple_music"
            CanvasSource.VIVIMUSIC -> "canvas_source_vivimusic"
            CanvasSource.TIDAL -> "canvas_source_tidal"
        }),
        options = CanvasSource.entries.map { s ->
            s.key to Localization.get(language, when (s) {
                CanvasSource.AUTO -> "canvas_source_auto"
                CanvasSource.APPLE_MUSIC -> "canvas_source_apple_music"
                CanvasSource.VIVIMUSIC -> "canvas_source_vivimusic"
                CanvasSource.TIDAL -> "canvas_source_tidal"
            })
        },
        onSelect = { key -> onCanvasSourceChange(CanvasSource.from(key)) },
    )

    M3SettingsDropdownItem(
        icon = Icons.Filled.SettingsBrightness,
        title = Localization.get(language, "density_and_grid"),
        value = densityLabel(densityScale),
        options = DENSITY_PRESETS.map { it.toString() to densityLabel(it) },
        onSelect = { s -> onDensityScaleChange(s.toFloatOrNull() ?: densityScale) },
    )

    M3SettingsDropdownItem(
        icon = Icons.Filled.ViewAgenda,
        title = Localization.get(language, "grid_item_size"),
        value = Localization.get(language, when (gridItemSize) {
            140 -> "grid_small"
            200 -> "grid_large"
            240 -> "grid_xlarge"
            else -> "grid_medium"
        }),
        options = GRID_PRESETS.map { (size, key) -> size.toString() to Localization.get(language, key) },
        onSelect = { s -> onGridItemSizeChange(s.toIntOrNull() ?: gridItemSize) },
    )

    M3SettingsDropdownItem(
        icon = Icons.Filled.MusicNote,
        title = Localization.get(language, "player_design"),
        value = Localization.get(language, when (playerDesign) {
            PlayerDesign.CLASSIC -> "player_design_classic"
            PlayerDesign.NEW -> "player_design_new"
            PlayerDesign.V2 -> "player_design_v2"
            PlayerDesign.EXPRESSIVE -> "player_design_expressive"
        }),
        options = PlayerDesign.entries.map { d ->
            d.key to Localization.get(language, when (d) {
                PlayerDesign.CLASSIC -> "player_design_classic"
                PlayerDesign.NEW -> "player_design_new"
                PlayerDesign.V2 -> "player_design_v2"
                PlayerDesign.EXPRESSIVE -> "player_design_expressive"
            })
        },
        onSelect = { key -> onPlayerDesignChange(PlayerDesign.from(key)) },
    )

    M3SettingsDropdownItem(
        icon = Icons.Filled.Wallpaper,
        title = Localization.get(language, "player_background"),
        value = Localization.get(language, when (playerBackground) {
            PlayerBackgroundStyle.CANVAS -> "canvas"
            PlayerBackgroundStyle.GRADIENT -> "player_background_gradient"
            PlayerBackgroundStyle.BLUR -> "player_background_blur"
            PlayerBackgroundStyle.GLOW -> "player_background_glow"
            PlayerBackgroundStyle.APPLE_MUSIC -> "player_background_apple"
            PlayerBackgroundStyle.LIVE_MESH -> "player_background_mesh"
            PlayerBackgroundStyle.VISUALIZER -> "player_background_visualizer"
        }),
        options = PlayerBackgroundStyle.entries.map { b ->
            b.key to Localization.get(language, when (b) {
                PlayerBackgroundStyle.CANVAS -> "canvas"
                PlayerBackgroundStyle.GRADIENT -> "player_background_gradient"
                PlayerBackgroundStyle.BLUR -> "player_background_blur"
                PlayerBackgroundStyle.GLOW -> "player_background_glow"
                PlayerBackgroundStyle.APPLE_MUSIC -> "player_background_apple"
                PlayerBackgroundStyle.LIVE_MESH -> "player_background_mesh"
                PlayerBackgroundStyle.VISUALIZER -> "player_background_visualizer"
            })
        },
        onSelect = { key -> onPlayerBackgroundChange(PlayerBackgroundStyle.from(key)) },
    )

    M3SettingsGroup(
        items = listOf(
            M3SettingsItem(
                icon = Icons.Filled.Album,
                title = { Text(Localization.get(language, "rotating_thumbnail")) },
                description = { Text(Localization.get(language, "rotating_thumbnail_desc")) },
                trailing = {
                    Switch(checked = rotatingThumbnail, onCheckedChange = onRotatingThumbnailChange)
                },
                onClick = { onRotatingThumbnailChange(!rotatingThumbnail) },
            ),
        ),
    )

    M3SettingsDropdownItem(
        icon = Icons.Filled.Devices,
        title = Localization.get(language, "mini_player_design"),
        value = Localization.get(language, when (miniPlayerDesign) {
            MiniPlayerDesign.CLASSIC -> "mini_player_classic"
            MiniPlayerDesign.NEW -> "mini_player_new"
            MiniPlayerDesign.APPLE -> "mini_player_apple"
        }),
        options = MiniPlayerDesign.entries.map { d ->
            d.key to Localization.get(language, when (d) {
                MiniPlayerDesign.CLASSIC -> "mini_player_classic"
                MiniPlayerDesign.NEW -> "mini_player_new"
                MiniPlayerDesign.APPLE -> "mini_player_apple"
            })
        },
        onSelect = { key -> onMiniPlayerDesignChange(MiniPlayerDesign.from(key)) },
    )

    M3SettingsDropdownItem(
        icon = Icons.Filled.Wallpaper,
        title = Localization.get(language, "mini_player_background"),
        value = Localization.get(language, when (miniPlayerBackgroundStyle) {
            MiniPlayerBackgroundStyle.FOLLOW_THEME -> "mini_player_bg_follow_theme"
            MiniPlayerBackgroundStyle.GRADIENT -> "mini_player_bg_gradient"
            MiniPlayerBackgroundStyle.BLUR -> "mini_player_bg_blur"
            MiniPlayerBackgroundStyle.GLOW_MOTION -> "mini_player_bg_glow_motion"
            MiniPlayerBackgroundStyle.LIVE_MESH -> "mini_player_bg_live_mesh"
        }),
        options = MiniPlayerBackgroundStyle.entries.map { b ->
            b.key to Localization.get(language, when (b) {
                MiniPlayerBackgroundStyle.FOLLOW_THEME -> "mini_player_bg_follow_theme"
                MiniPlayerBackgroundStyle.GRADIENT -> "mini_player_bg_gradient"
                MiniPlayerBackgroundStyle.BLUR -> "mini_player_bg_blur"
                MiniPlayerBackgroundStyle.GLOW_MOTION -> "mini_player_bg_glow_motion"
                MiniPlayerBackgroundStyle.LIVE_MESH -> "mini_player_bg_live_mesh"
            })
        },
        onSelect = { key -> onMiniPlayerBackgroundStyleChange(MiniPlayerBackgroundStyle.from(key)) },
    )

    M3SettingsGroup(
        items = listOf(
            M3SettingsItem(
                icon = Icons.Filled.Brightness1,
                title = { Text(Localization.get(language, "pure_black_mini")) },
                description = { Text(Localization.get(language, "pure_black_mini_desc")) },
                trailing = {
                    Switch(checked = pureBlackMiniPlayer, onCheckedChange = onPureBlackMiniPlayerChange)
                },
                onClick = { onPureBlackMiniPlayerChange(!pureBlackMiniPlayer) },
            ),
        ),
    )

    M3SettingsDropdownItem(
        icon = Icons.Filled.MotionPhotosOn,
        title = Localization.get(language, "screen_transitions"),
        value = Localization.get(language, when (screenTransition) {
            "slide" -> "transition_slide"
            "off" -> "transition_off"
            else -> "transition_fade"
        }),
        options = listOf(
            "off" to Localization.get(language, "transition_off"),
            "fade" to Localization.get(language, "transition_fade"),
            "slide" to Localization.get(language, "transition_slide"),
        ),
        onSelect = onScreenTransitionChange,
    )

    M3SettingsGroup(
        items = listOf(
            M3SettingsItem(
                icon = Icons.Filled.Movie,
                title = { Text(Localization.get(language, "intro")) },
                description = { Text(Localization.get(language, "show_intro_on_startup")) },
                trailing = { SettingsChevron() },
                onClick = onOpenIntro,
            ),
        ),
    )

    // Native system title bar vs VIVI's custom one. The window chrome is fixed
    // at creation, so the change is applied on the next launch.
    M3SettingsGroup(
        items = listOf(
            M3SettingsItem(
                icon = Icons.Filled.DesktopWindows,
                title = { Text(Localization.get(language, "native_title_bar")) },
                description = { Text(Localization.get(language, "native_title_bar_desc")) },
                trailing = {
                    Switch(
                        checked = nativeTitleBar,
                        onCheckedChange = { v ->
                            onNativeTitleBarChange(v)
                            showRestartDialog = true
                        },
                    )
                },
                onClick = {
                    onNativeTitleBarChange(!nativeTitleBar)
                    showRestartDialog = true
                },
            ),
        ),
    )

    // Right Now Playing panel (the Spotify-style right sidebar in the player).
    M3SettingsGroup(
        items = listOf(
            M3SettingsItem(
                icon = Icons.AutoMirrored.Filled.ViewSidebar,
                title = { Text(Localization.get(language, "right_panel")) },
                description = { Text(Localization.get(language, "right_panel_desc")) },
                trailing = {
                    Switch(
                        checked = showRightSidebar,
                        onCheckedChange = onShowRightSidebarChange,
                    )
                },
                onClick = { onShowRightSidebarChange(!showRightSidebar) },
            ),
        ),
    )

    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { showRestartDialog = false },
            title = { Text(Localization.get(language, "restart_required_title")) },
            text = { Text(Localization.get(language, "restart_required")) },
            confirmButton = {
                Button(onClick = { onRestart() }) {
                    Text(Localization.get(language, "restart_now"))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRestartDialog = false }) {
                    Text(Localization.get(language, "later"))
                }
            },
        )
    }
}

/**
 * Theme sub-screen: 4 mode circles (System / Light / Dark / Pure black) +
 * the full 21-color accent palette + a live preview card. Mirrors the Android
 * `ThemeScreen` (pixel-perfect mode selection).
 */
@Composable
fun ThemeSection(
    language: String,
    mode: ThemeMode,
    accent: Color,
    onModeChange: (ThemeMode) -> Unit,
    onAccentChange: (Color) -> Unit,
    accentIntensity: Float = 1f,
    onAccentIntensityChange: (Float) -> Unit = {},
    pureBlack: Boolean,
    onPureBlackChange: (Boolean) -> Unit,
    customAccents: List<Int> = emptyList(),
    onAddCustomAccent: (Int) -> Unit = {},
    onRemoveCustomAccent: (Int) -> Unit = {},
) {
    Column(Modifier.fillMaxWidth()) {
        Text(Localization.get(language, "theme_colors"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))

        // Live preview card (uses the currently applied theme).
        ThemePreviewCard(Modifier.fillMaxWidth().padding(top = 16.dp).height(140.dp))

        Spacer(Modifier.height(24.dp))

        Text(Localization.get(language, "theme_mode"), style = MaterialTheme.typography.titleMedium)
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ThemeModeCircle(
                language = language,
                label = Localization.get(language, "theme_system"),
                selected = mode == ThemeMode.SYSTEM && !pureBlack,
                previewDark = false,
                pureBlack = false,
                isAuto = true,
                onClick = { onModeChange(ThemeMode.SYSTEM); onPureBlackChange(false) },
            )
            ThemeModeCircle(
                language = language,
                label = Localization.get(language, "theme_light"),
                selected = mode == ThemeMode.LIGHT && !pureBlack,
                previewDark = false,
                pureBlack = false,
                isAuto = false,
                onClick = { onModeChange(ThemeMode.LIGHT); onPureBlackChange(false) },
            )
            ThemeModeCircle(
                language = language,
                label = Localization.get(language, "theme_dark"),
                selected = mode == ThemeMode.DARK && !pureBlack,
                previewDark = true,
                pureBlack = false,
                isAuto = false,
                onClick = { onModeChange(ThemeMode.DARK); onPureBlackChange(false) },
            )
            ThemeModeCircle(
                language = language,
                label = Localization.get(language, "pure_black"),
                selected = mode == ThemeMode.DARK && pureBlack,
                previewDark = true,
                pureBlack = true,
                isAuto = false,
                onClick = { onModeChange(ThemeMode.DARK); onPureBlackChange(true) },
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(Localization.get(language, "color_palette"), style = MaterialTheme.typography.titleMedium)
        // Palette swatches (wrap via FlowRow-like manual chunking: show in rows of 7).
        AccentPalette.colors.chunked(7).forEach { row ->
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { entry ->
                    val isSelected = if (entry.color == Color.Transparent) {
                        accent == Color.Transparent
                    } else {
                        accent == entry.color
                    }
                    val colorName = Localization.get(language, "accent_${entry.key}")
                    Tooltip(colorName) {
                        AccentSwatch(
                            color = entry.color,
                            selected = isSelected,
                            onClick = {
                                if (entry.color == Color.Transparent) {
                                    AccentPalette.refreshSystemAccent()
                                    onAccentChange(Color.Transparent)
                                } else {
                                    onAccentChange(entry.color)
                                }
                            },
                        )
                    }
                }
            }
        }

        if (customAccents.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text(Localization.get(language, "custom_colors"), style = MaterialTheme.typography.titleMedium)
            customAccents.chunked(7).forEach { row ->
                Row(
                    Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    row.forEach { argb ->
                        val c = argbIntToColor(argb)
                        val isSelected = accent == c
                        Tooltip(Localization.get(language, "remove_custom_color")) {
                            Box {
                                AccentSwatch(
                                    color = c,
                                    selected = isSelected,
                                    onClick = { onAccentChange(c) },
                                )
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface)
                                        .clickable { onRemoveCustomAccent(argb) }
                                        .padding(2.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(Localization.get(language, "accent_intensity"), style = MaterialTheme.typography.titleMedium)
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Slider(
                value = accentIntensity,
                onValueChange = onAccentIntensityChange,
                valueRange = 0f..1f,
                steps = 18,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${(accentIntensity * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 14.dp).widthIn(min = 44.dp),
                textAlign = TextAlign.End,
            )
        }
        // Small live preview of the currently adjusted accent.
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(100, 75, 50, 25, 0).forEach { pct ->
                val c = adjustAccentIntensity(accent, pct / 100f)
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(c)
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), CircleShape),
                )
            }
        }

        // --- Custom color picker (HSV gradient bars) ---
        Spacer(Modifier.height(28.dp))
        Text(Localization.get(language, "custom_color"), style = MaterialTheme.typography.titleMedium)

        val initialHsv = remember(accent) { colorToHsv(accent) }
        var hue by remember { mutableStateOf(initialHsv[0]) }
        var saturation by remember { mutableStateOf(initialHsv[1]) }
        var brightness by remember { mutableStateOf(initialHsv[2]) }
        val customColor = hsvToColor(hue, saturation, brightness)
        val alreadySaved = remember(customAccents, customColor) {
            customAccents.any { argbIntToColor(it) == customColor }
        }

        Spacer(Modifier.height(14.dp))
        Text(
            Localization.get(language, "hue"),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        GradientBar(
            gradient = Brush.horizontalGradient(
                listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red),
            ),
            fraction = hue / 360f,
            onFractionChange = { hue = it * 360f },
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            Localization.get(language, "saturation"),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        GradientBar(
            gradient = Brush.horizontalGradient(
                listOf(hsvToColor(hue, 0f, brightness), hsvToColor(hue, 1f, brightness)),
            ),
            fraction = saturation,
            onFractionChange = { saturation = it },
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            Localization.get(language, "brightness"),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        GradientBar(
            gradient = Brush.horizontalGradient(
                listOf(hsvToColor(hue, saturation, 0f), hsvToColor(hue, saturation, 1f)),
            ),
            fraction = brightness,
            onFractionChange = { brightness = it },
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        )

        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(customColor)
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), CircleShape),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "#%06X".format(java.util.Locale.US, colorToArgbInt(customColor) and 0xFFFFFF),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { onAddCustomAccent(colorToArgbInt(customColor)) },
                enabled = !alreadySaved,
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(Localization.get(language, "add_to_palette"))
            }
        }

        Spacer(Modifier.height(36.dp))
    }
}

@Composable
private fun GradientBar(
    gradient: Brush,
    fraction: Float,
    onFractionChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var barWidthPx by remember { mutableStateOf(1f) }
    BoxWithConstraints(
        modifier = modifier
            .height(24.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(gradient)
            .onSizeChanged { barWidthPx = it.width.toFloat() }
            .pointerInput(barWidthPx) {
                fun pick(x: Float) = onFractionChange((x / barWidthPx).coerceIn(0f, 1f))
                detectTapGestures { pick(it.x) }
                detectDragGestures(
                    onDragStart = { pick(it.x) },
                    onDrag = { change, _ ->
                        change.consume()
                        pick(change.position.x)
                    },
                )
            },
    ) {
        val thumbX = (fraction * maxWidth.value).dp.coerceIn(0.dp, maxWidth)
        Box(
            Modifier
                .offset(x = thumbX - 6.dp)
                .width(12.dp)
                .fillMaxHeight()
                .border(2.dp, Color.White, RoundedCornerShape(3.dp))
                .shadow(2.dp, RoundedCornerShape(3.dp)),
        )
    }
}

@Composable
private fun ThemeModeCircle(
    language: String,
    label: String,
    selected: Boolean,
    previewDark: Boolean,
    pureBlack: Boolean,
    isAuto: Boolean,
    onClick: () -> Unit,
) {
    val innerColor = when {
        pureBlack -> Color.Black
        previewDark -> MaterialTheme.colorScheme.surface
        else -> Color.White
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    if (selected) MaterialTheme.colorScheme.surfaceVariant
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                )
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(18.dp),
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(innerColor)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (isAuto) {
                    Icon(
                        Icons.Filled.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp),
                    )
                } else if (selected) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = if (pureBlack) Color.White else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun AccentSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .then(
                if (color == Color.Transparent) {
                    Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                } else {
                    Modifier.background(color)
                },
            )
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = if (color == Color.Transparent) MaterialTheme.colorScheme.onSurface else Color.White,
                modifier = Modifier.size(16.dp),
            )
        } else if (color == Color.Transparent) {
            Icon(
                Icons.Filled.Palette,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ThemePreviewCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                )
                Spacer(Modifier.width(12.dp))
                Box(
                    Modifier
                        .size(28.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                )
            }
            Row(
                Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    Modifier
                        .weight(2f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary),
                )
                Column(
                    Modifier.weight(1f).fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondary),
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.tertiary),
                    )
                }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                )
            }
        }
    }
}

/** Human-readable density label (e.g. "110%") for a scale value. */
private fun densityLabel(scale: Float): String = "${(scale * 100).roundToInt()}%"

/** Density scale presets (fractional; 1f = 100%). */
private val DENSITY_PRESETS = listOf(
    2.0f, 1.8f, 1.5f, 1.4f, 1.3f, 1.25f, 1.2f, 1.1f,
    1f, 0.85f, 0.75f, 0.65f, 0.55f,
)

/** Grid cell width presets in dp (small / medium / large). */
private val GRID_PRESETS = listOf(140 to "grid_small", 160 to "grid_medium", 200 to "grid_large", 240 to "grid_xlarge")
