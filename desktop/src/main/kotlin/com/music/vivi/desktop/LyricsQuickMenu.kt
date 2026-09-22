package com.music.vivi.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Subject
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * The mobile app's lyrics menu, opened from the player itself: the same options
 * the Lyrics settings screen offers (style, position, glow, blur, tap-to-seek,
 * auto-scroll, size, spacing) applied live, plus the shortcuts to the lyrics
 * screens and to the full settings page.
 *
 * Mobile keeps this menu inside the player because a listener wants to change
 * the style without leaving the song; the desktop required a trip to
 * Settings → Lyrics, so the options are repeated here instead of only linked.
 *
 * The menu reads the settings through [settingsFileRevision], so a change made
 * elsewhere is reflected immediately, and every change is written straight to
 * the settings file so it survives the restart.
 */
@Composable
fun LyricsQuickMenu(
    language: String,
    onDismiss: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenLyricsFocus: (() -> Unit)?,
    onOpenLyricsSettings: (() -> Unit)?,
) {
    val revision = settingsFileRevision()
    val state = remember(revision) { DesktopSettings.load() }
    val options = remember(revision) { lyricsDisplayOptionsFrom(state) }

    fun apply(new: LyricsDisplayOptions) {
        DesktopSettings.update { it.withLyricsDisplayOptions(new) }
        AppLog.log(
            "lyrics",
            "options changed: style=${new.style.id} position=${new.position} glow=${new.glowEffect} " +
                "appleBlur=${new.appleMusicBlur} blur=${new.standardBlur} " +
                "tapToSeek=${new.clickToSeek} autoScroll=${new.autoScroll}",
        )
    }

    fun applySize(size: Float) {
        DesktopSettings.update { it.copy(lyricsTextSize = size) }
        AppLog.log("lyrics", "text size: ${size.toInt()}sp")
    }

    fun applySpacing(spacing: Float) {
        DesktopSettings.update { it.copy(lyricsLineSpacing = spacing) }
        AppLog.log("lyrics", "line spacing: ${"%.2f".format(spacing)}")
    }

    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss,
        // The list is ~25 entries once the styles are expanded: it must scroll
        // on a short window instead of running off the screen.
        modifier = Modifier.verticalScroll(rememberScrollState()),
    ) {
        DropdownMenuItem(
            text = { Text(Localization.get(language, "lyrics")) },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Subject, contentDescription = null) },
            onClick = { onDismiss(); onOpenLyrics() },
        )
        if (onOpenLyricsFocus != null) {
            DropdownMenuItem(
                text = { Text(Localization.get(language, "lyrics_focus")) },
                leadingIcon = { Icon(Icons.Filled.Lyrics, contentDescription = null) },
                onClick = { onDismiss(); onOpenLyricsFocus() },
            )
        }
        HorizontalDivider()

        // --- Style -------------------------------------------------------
        MenuHeader(Localization.get(language, "lyrics_animation_style"))
        LyricsAnimationStyle.entries.forEach { style ->
            DropdownMenuItem(
                text = {
                    Text(
                        lyricsStyleLabel(language, style),
                        fontWeight = if (style == options.style) FontWeight.SemiBold else null,
                    )
                },
                leadingIcon = { CheckSlot(style == options.style) },
                onClick = { apply(options.copy(style = style)) },
            )
        }
        HorizontalDivider()

        // --- Position ----------------------------------------------------
        MenuHeader(Localization.get(language, "lyrics_text_position"))
        LyricsPosition.entries.forEach { position ->
            DropdownMenuItem(
                text = { Text(lyricsPositionLabel(language, position)) },
                leadingIcon = { CheckSlot(position == options.position) },
                onClick = { apply(options.copy(position = position)) },
            )
        }
        HorizontalDivider()

        // --- Toggles -----------------------------------------------------
        MenuToggle(
            label = Localization.get(language, "lyrics_glow_effect"),
            checked = options.glowEffect,
            onToggle = { apply(options.copy(glowEffect = it)) },
        )
        // Apple Music blur only reaches the VIVI Music style, so the entry is
        // offered only while that style is selected (same rule as the settings).
        if (options.style == LyricsAnimationStyle.VIVIMUSIC_1) {
            MenuToggle(
                label = Localization.get(language, "lyrics_apple_blur"),
                checked = options.appleMusicBlur,
                onToggle = { apply(options.copy(appleMusicBlur = it)) },
            )
        }
        MenuToggle(
            label = Localization.get(language, "lyrics_standard_blur"),
            checked = options.standardBlur,
            onToggle = { apply(options.copy(standardBlur = it)) },
        )
        MenuToggle(
            label = Localization.get(language, "lyrics_click_to_seek"),
            checked = options.clickToSeek,
            onToggle = { apply(options.copy(clickToSeek = it)) },
        )
        MenuToggle(
            label = Localization.get(language, "lyrics_auto_scroll"),
            checked = options.autoScroll,
            onToggle = { apply(options.copy(autoScroll = it)) },
        )
        MenuToggle(
            label = Localization.get(language, "lyrics_thumbnail_play_pause"),
            checked = state.lyricsThumbnailPlayPause,
            onToggle = {
                DesktopSettings.update { s -> s.copy(lyricsThumbnailPlayPause = it) }
                AppLog.log("lyrics", "thumbnail play/pause: $it")
            },
        )
        HorizontalDivider()

        // --- Size / spacing (steppers, so the menu never grows a slider) ---
        MenuStepper(
            label = Localization.get(language, "lyrics_text_size"),
            value = "${options.textSizeSp.toInt()} sp",
            onMinus = { applySize((options.textSizeSp - 1f).coerceIn(12f, 32f)) },
            onPlus = { applySize((options.textSizeSp + 1f).coerceIn(12f, 32f)) },
        )
        MenuStepper(
            label = Localization.get(language, "lyrics_line_spacing"),
            value = "%.2f".format(options.lineSpacing),
            onMinus = { applySpacing((options.lineSpacing - 0.05f).coerceIn(1f, 2f)) },
            onPlus = { applySpacing((options.lineSpacing + 0.05f).coerceIn(1f, 2f)) },
        )

        if (onOpenLyricsSettings != null) {
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(Localization.get(language, "settings")) },
                onClick = { onDismiss(); onOpenLyricsSettings() },
            )
        }
    }
}

/** Section header inside the menu (not clickable). */
@Composable
private fun MenuHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

/** Fixed-width slot so every check mark lines up whether it is drawn or not. */
@Composable
private fun CheckSlot(checked: Boolean) {
    Box(Modifier.width(24.dp), contentAlignment = Alignment.CenterStart) {
        if (checked) Icon(Icons.Filled.Check, contentDescription = null)
    }
}

/** A switching entry: the check mark shows the state instead of a switch row. */
@Composable
private fun MenuToggle(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = { CheckSlot(checked) },
        onClick = { onToggle(!checked) },
    )
}

/** A numeric entry with -/+ buttons (size, spacing). */
@Composable
private fun MenuStepper(
    label: String,
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(label)
                Text(
                    value,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        trailingIcon = {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                StepperButton("−", onMinus)
                StepperButton("+", onPlus)
            }
        },
        // Clicking the row itself steps up: the -/+ buttons are the fine control.
        onClick = onPlus,
    )
}

@Composable
private fun StepperButton(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.size(26.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}
