package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.music.vivi.desktop.player.PlayerController
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import kotlin.math.roundToInt

private const val WIDGET_WIDTH_DP = 340
private const val WIDGET_HEIGHT_DP = 92

/**
 * Union of every attached screen, in window coordinates, so the widget can be
 * clamped on multi-monitor setups too. Falls back to a 1080p box if AWT cannot
 * report any display.
 */
private fun virtualScreenBounds(): Rectangle =
    GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices
        .map { it.defaultConfiguration.bounds }
        .fold<Rectangle, Rectangle?>(null) { acc, b -> acc?.union(b) ?: Rectangle(b) }
        ?: Rectangle(0, 0, 1920, 1080)

/** Keeps the widget fully inside [bounds], never letting it be dragged away (issue #64). */
private fun clampToScreen(x: Int, y: Int, w: Int, h: Int, bounds: Rectangle): Pair<Int, Int> {
    val maxX = (bounds.x + bounds.width - w).coerceAtLeast(bounds.x)
    val maxY = (bounds.y + bounds.height - h).coerceAtLeast(bounds.y)
    return x.coerceIn(bounds.x, maxX) to y.coerceIn(bounds.y, maxY)
}

/**
 * Cider-style floating \"Now Playing\" widget: a small always-on-top, draggable
 * window that stays above every other app and shows the current track with
 * transport controls. Its position persists across restarts. Dragging is done
 * through [WindowDraggableArea] on the whole surface.
 */
@Composable
fun NowPlayingWidgetWindow(
    player: PlayerController,
    language: String,
    themeMode: ThemeMode,
    accent: Color,
    pureBlack: Boolean,
    font: AppFont,
    onClose: () -> Unit,
) {
    val state by player.state.collectAsState()
    val np = state.current
    val isPlaying = state.isPlaying

    val saved = remember { DesktopSettings.load() }
    val density = LocalDensity.current
    val screenBounds = remember { virtualScreenBounds() }
    val winState = rememberWindowState(
        placement = WindowPlacement.Floating,
        position = if (saved.widgetX >= 0 && saved.widgetY >= 0) {
            // A position saved on a screen that is gone (monitor unplugged,
            // different resolution) must not throw the widget off screen.
            val w = with(density) { WIDGET_WIDTH_DP.dp.roundToPx() }
            val h = with(density) { WIDGET_HEIGHT_DP.dp.roundToPx() }
            val (cx, cy) = clampToScreen(saved.widgetX, saved.widgetY, w, h, screenBounds)
            WindowPosition(cx.dp, cy.dp)
        } else {
            WindowPosition(Alignment.TopEnd)
        },
        width = WIDGET_WIDTH_DP.dp,
        height = WIDGET_HEIGHT_DP.dp,
    )

    // Persist the widget position (debounced) so a drag doesn't hammer the
    // settings file with one write per pixel.
    @OptIn(FlowPreview::class)
    LaunchedEffect(Unit) {
        snapshotFlow { winState.position }
            .debounce(500)
            .collect { pos ->
                // Until the widget is dragged for the first time it sits at an
                // *aligned* position (WindowPosition(Alignment.TopEnd)), whose x/y
                // are unspecified (NaN). roundToInt() throws on NaN, so persisting
                // it crashed the whole app ~0.5s after the widget appeared, on
                // every launch (issue #63).
                if (!pos.isSpecified) return@collect
                val w = with(density) { WIDGET_WIDTH_DP.dp.roundToPx() }
                val h = with(density) { WIDGET_HEIGHT_DP.dp.roundToPx() }
                val px = with(density) { pos.x.toPx().roundToInt() }
                val py = with(density) { pos.y.toPx().roundToInt() }
                val (cx, cy) = clampToScreen(px, py, w, h, screenBounds)
                // The window is drag-droppable: if it is released partly off
                // screen, snap it back so it can never be lost (issue #64).
                if (cx != px || cy != py) {
                    winState.position = WindowPosition(cx.dp, cy.dp)
                }
                DesktopSettings.update { it.copy(widgetX = cx, widgetY = cy) }
            }
    }

    Window(
        onCloseRequest = onClose,
        state = winState,
        undecorated = true,
        transparent = true,
        alwaysOnTop = true,
        resizable = false,
        title = "VIVI Music — Now Playing",
    ) {
        AppTheme(mode = themeMode, accent = accent, pureBlack = pureBlack, font = font) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            tonalElevation = 3.dp,
        ) {
            WindowDraggableArea(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(
                    Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        Modifier
                            .size(58.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Thumbnail(np?.thumbnail, Modifier.fillMaxSize())
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            np?.title ?: "VIVI Music",
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            np?.artist ?: Localization.get(language, "nothing_playing"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Tooltip(Localization.get(language, "tooltip_previous")) {
                        IconButton(onClick = { player.previous() }, modifier = Modifier.size(30.dp)) {
                            Icon(
                                Icons.Filled.SkipPrevious,
                                contentDescription = Localization.get(language, "previous"),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    Tooltip(Localization.get(language, if (isPlaying) "pause" else "play")) {
                        IconButton(
                            onClick = { player.toggle() },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(17.dp))
                                .background(MaterialTheme.colorScheme.primary),
                        ) {
                            Icon(
                                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = Localization.get(language, if (isPlaying) "pause" else "play"),
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    Tooltip(Localization.get(language, "tooltip_next")) {
                        IconButton(onClick = { player.next() }, modifier = Modifier.size(30.dp)) {
                            Icon(
                                Icons.Filled.SkipNext,
                                contentDescription = Localization.get(language, "next"),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(2.dp))
                    Tooltip(Localization.get(language, "close")) {
                        IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = Localization.get(language, "close"),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }
        }
    }
}
