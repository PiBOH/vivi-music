package com.vivimusic.de.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val colorScheme = darkColorScheme(
    primary = Color(0xFF8AB4F8),
    onPrimary = Color(0xFF0F1B2D),
    primaryContainer = Color(0xFF23324B),
    secondary = Color(0xFF7DD6C3),
    background = Color(0xFF0F1318),
    onBackground = Color(0xFFE1E6EC),
    surface = Color(0xFF161B22),
    onSurface = Color(0xFFE1E6EC),
    surfaceVariant = Color(0xFF20262F),
    error = Color(0xFFF2B8B5),
)

@Composable
fun ViviTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colorScheme, content = content)
}
