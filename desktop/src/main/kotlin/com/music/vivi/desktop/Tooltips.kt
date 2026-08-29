package com.music.vivi.desktop

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Wraps a composable (usually a button) with a small hover tooltip — the
 * "name" hint that appears when the pointer rests on the element, like the
 * native tooltips on websites.
 *
 * Usage:
 *   Tooltip("Play") {
 *       IconButton(onClick = ...) { Icon(...) }
 *   }
 *
 * Pass `null` to disable the tooltip.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Tooltip(text: String?, content: @Composable () -> Unit) {
    if (text.isNullOrBlank()) {
        content()
        return
    }
    TooltipArea(
        tooltip = {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.inverseSurface,
                shadowElevation = 4.dp,
            ) {
                Text(
                    text,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        },
        tooltipPlacement = TooltipPlacement.CursorPoint(),
        content = content,
    )
}
