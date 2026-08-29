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
import kotlin.math.roundToInt

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
    val winState = rememberWindowState(
        placement = WindowPlacement.Floating,
        position = if (saved.widgetX >= 0 && saved.widgetY >= 0) {
            WindowPosition(saved.widgetX.dp, saved.widgetY.dp)
        } else {
            WindowPosition(Alignment.TopEnd)
        },
        width = 340.dp,
        height = 92.dp,
    )

    // Persist the widget position (debounced) so a drag doesn't hammer the
    // settings file with one write per pixel.
    @OptIn(FlowPreview::class)
    LaunchedEffect(winState.position) {
        snapshotFlow { winState.position }
            .debounce(500)
            .collect { pos ->
                val px = with(density) { pos.x.toPx().roundToInt() }
                val py = with(density) { pos.y.toPx().roundToInt() }
                DesktopSettings.update { it.copy(widgetX = px, widgetY = py) }
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
                    Tooltip(Localization.get(language, "previous")) {
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
                    Tooltip(Localization.get(language, "next")) {
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
