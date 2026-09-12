package com.music.vivi.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/**
 * Dedicated real-time log viewer opened from the Developer options ("Open"
 * next to the Live monitor card). Shows [AppLog] lines as they are emitted,
 * auto-scrolled to the newest entry, with a Clear action and selectable text
 * so any line can be copied for a bug report. The window title is a native
 * title (hardcoded English), matching the existing DevTools window.
 */
@Composable
fun LiveLogWindowContent(language: String, onClose: () -> Unit) {
    val lines by AppLog.lines.collectAsState()
    val listState = rememberLazyListState()

    // Keep the view pinned to the newest line while the log grows.
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) listState.scrollToItem(lines.lastIndex)
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "VIVI Music DE — Live log",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { AppLog.clear() }) {
                    Text(Localization.get(language, "tooltip_clear"))
                }
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = onClose) {
                    Text(Localization.get(language, "close"))
                }
            }
            Spacer(Modifier.padding(top = 4.dp))
            SelectionContainer {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    items(lines, key = { it }) { line ->
                        Text(
                            line,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                            ),
                        )
                    }
                }
            }
        }
    }
}
