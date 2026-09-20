package com.vivimusic.de.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Grouped list-item shapes, ported 1:1 from ViVi Music
 * (`com.music.vivi.utils.ShapesCurve`). Items inside a group share a 4dp
 * connected corner; the first and last items use a 16dp end corner.
 */
private const val ConnectedCornerRadius = 4
private const val EndCornerRadius = 16

fun CornerBasedShape.top(): CornerBasedShape =
    copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp))

@Composable
fun listItemColors(): ListItemColors =
    ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)

fun leadingItemShape(): RoundedCornerShape = RoundedCornerShape(
    topStart = EndCornerRadius.dp,
    topEnd = EndCornerRadius.dp,
    bottomStart = ConnectedCornerRadius.dp,
    bottomEnd = ConnectedCornerRadius.dp,
)

fun middleItemShape(): RoundedCornerShape = RoundedCornerShape(ConnectedCornerRadius.dp)

fun endItemShape(): RoundedCornerShape = RoundedCornerShape(
    topStart = ConnectedCornerRadius.dp,
    topEnd = ConnectedCornerRadius.dp,
    bottomStart = EndCornerRadius.dp,
    bottomEnd = EndCornerRadius.dp,
)

fun groupedItemShape(index: Int, count: Int): Shape = when {
    index == 0 -> leadingItemShape()
    index == count - 1 -> endItemShape()
    else -> middleItemShape()
}
