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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider

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
        // Smart placement: below-right of the cursor by default; if there is
        // not enough room it flips above and clamps to the window edges. It is
        // always offset away from the pointer, so it never covers the click
        // target under the cursor.
        tooltipPlacement = SmartTooltipPlacement(),
        content = content,
    )
}

/**
 * Tooltip placement that keeps the hint away from the pointer:
 * - default: below the cursor, slightly to the right;
 * - if there is no room below, it appears above;
 * - it always clamps to the window so it is never cut off.
 */
@OptIn(ExperimentalFoundationApi::class)
private class SmartTooltipPlacement : TooltipPlacement {
    @Composable
    override fun positionProvider(cursorPosition: Offset): PopupPositionProvider {
        val density = LocalDensity.current
        val gapPx = with(density) { 8.dp.roundToPx() }
        val sidePx = with(density) { 12.dp.roundToPx() }
        // cursorPosition is relative to the anchored component; the popup
        // position must be in window coordinates, so the component's own
        // offset (anchorBounds.left/top) is added when positioning.
        return object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val popup = popupContentSize
                val cursorX = anchorBounds.left + cursorPosition.x.toInt()
                val cursorY = anchorBounds.top + cursorPosition.y.toInt()

                // Preferred: below the cursor, pushed slightly right so it does
                // not sit exactly where the click happens.
                var x = (cursorX + sidePx).coerceAtLeast(0)
                var y = cursorY + gapPx

                // Not enough room below -> flip above.
                if (y + popup.height > windowSize.height) {
                    y = cursorY - popup.height - gapPx
                }

                // Clamp horizontally (keep a small margin from the window edge).
                if (x + popup.width > windowSize.width) {
                    x = (windowSize.width - popup.width - gapPx).coerceAtLeast(0)
                }

                // Final vertical safety clamp (tiny windows).
                y = y.coerceIn(0, (windowSize.height - popup.height).coerceAtLeast(0))

                return IntOffset(x, y)
            }
        }
    }
}
