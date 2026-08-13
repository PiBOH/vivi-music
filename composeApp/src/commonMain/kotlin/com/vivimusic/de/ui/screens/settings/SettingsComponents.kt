package com.vivimusic.de.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vivimusic.de.data.readSetting
import com.vivimusic.de.data.writeSetting
import com.vivimusic.de.resources.Res
import com.vivimusic.de.resources.back
import com.vivimusic.de.resources.cancel
import com.vivimusic.de.resources.close
import org.jetbrains.compose.resources.stringResource

/** Maximum readable width for the settings content on wide desktop windows. */
private val MaxContentWidth = 760.dp

/**
 * Scrollable settings page scaffold, desktop-adapted: content is centered and
 * capped to [MaxContentWidth] so the list does not stretch across the window.
 * An optional back arrow turns the page into a nested settings screen.
 */
@Composable
fun SettingsPage(
    title: String,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.back),
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = MaxContentWidth)
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            content = content,
        )
    }
}

/**
 * A grouped settings section, mirroring ViVi Music's `Material3SettingsGroup`:
 * an optional section title followed by a rounded card containing the items,
 * separated by dividers.
 */
@Composable
fun SettingsGroup(
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
            )
        }
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(content = content)
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

/**
 * A single settings row: optional leading icon, title + optional description,
 * and a trailing control (switch, value text or navigation chevron).
 */
@Composable
fun SettingsItem(
    title: String,
    description: String? = null,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    checked: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    trailingText: String? = null,
    highlighted: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (highlighted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        when {
            checked != null && onCheckedChange != null -> Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
            trailingText != null -> Text(
                text = trailingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            onClick != null -> Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** A divider between two [SettingsItem]s inside a [SettingsGroup]. */
@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 20.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}

/** A radio-list selection dialog, ported from ViVi Music's `EnumDialog`. */
@Composable
fun ChoiceDialog(
    title: String,
    options: List<Pair<String, String>>,
    selectedValue: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onSelect(value)
                                onDismiss()
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        RadioButton(
                            selected = value == selectedValue,
                            onClick = {
                                onSelect(value)
                                onDismiss()
                            },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.close))
            }
        },
    )
}

/** A destructive-action confirmation dialog. */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}

/** A row of selectable accent-color swatches (a ring marks the selection). */
@Composable
fun ColorSwatches(
    colors: List<Color>,
    selected: Color,
    onSelect: (Color) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        colors.forEach { color ->
            val isSelected = color == selected
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color)
                    .let {
                        if (isSelected) {
                            it.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        } else {
                            it
                        }
                    }
                    .clickable { onSelect(color) },
            )
        }
    }
}

/** Persisted boolean setting (read once, written on change). */
@Composable
fun rememberBoolSetting(key: String, default: Boolean): Pair<Boolean, (Boolean) -> Unit> {
    var value by remember(key) { mutableStateOf(readSetting(key)?.toBoolean() ?: default) }
    val onValueChange: (Boolean) -> Unit = { new ->
        value = new
        writeSetting(key, new.toString())
    }
    return value to onValueChange
}

/** Persisted string setting (read once, written on change). */
@Composable
fun rememberStringSetting(key: String, default: String): Pair<String, (String) -> Unit> {
    var value by remember(key) { mutableStateOf(readSetting(key) ?: default) }
    val onValueChange: (String) -> Unit = { new ->
        value = new
        writeSetting(key, new)
    }
    return value to onValueChange
}
