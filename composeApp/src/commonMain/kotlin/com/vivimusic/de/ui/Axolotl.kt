package com.vivimusic.de.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

/**
 * Original pixel-art mascot: a blue axolotl in a Minecraft-like style.
 *
 * The art is a grid of characters where each character maps to a color below
 * and '.' is transparent. This keeps a single source of truth shared between
 * the in-app rendering and the icon generation script
 * (`tools/generate_icons.py`); keep the two grids in sync.
 */
val AXOLOTL_PIXELS: List<String> = listOf(
    ".PPP........PPP.",
    ".PPPP......PPPP.",
    ".PPPPBBBBBBPPPP.",
    ".PBBBBBBBBBBBBP.",
    ".PBBEEBBBBEEBBP.",
    ".PBBEEBBBBEEBBP.",
    ".PBBBBBBBBBBBBP.",
    "..BBBBBBBBBBBB..",
    "..BBBBBMMBBBBB..",
    "..BBBBBBBBBBBB..",
    "..BBBBBBBBBBBB..",
    "...BBBBBBBBBB...",
    "....BBBBBBBB....",
    ".....DBBBBD.....",
    "......DBBD......",
    ".......DD.......",
)

private val AXOLOTL_COLORS: Map<Char, Color> = mapOf(
    'B' to Color(0xFF54C9F0),
    'D' to Color(0xFF2E6E8E),
    'P' to Color(0xFFFF8FB5),
    'E' to Color(0xFF14202E),
    'M' to Color(0xFF2E6E8E),
)

/**
 * Renders the mascot as hard-edged squares (no anti-aliasing) so it keeps the
 * pixelated look at any size. The canvas is square.
 */
@Composable
fun AxolotlMascot(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.aspectRatio(1f)) {
        val cols = AXOLOTL_PIXELS.firstOrNull()?.length ?: return@Canvas
        val rows = AXOLOTL_PIXELS.size
        val cell = minOf(size.width / cols, size.height / rows)
        val offsetX = (size.width - cell * cols) / 2f
        val offsetY = (size.height - cell * rows) / 2f
        AXOLOTL_PIXELS.forEachIndexed { y, row ->
            row.forEachIndexed { x, ch ->
                val color = AXOLOTL_COLORS[ch] ?: return@forEachIndexed
                drawRect(
                    color = color,
                    topLeft = Offset(offsetX + x * cell, offsetY + y * cell),
                    size = Size(cell, cell),
                )
            }
        }
    }
}
