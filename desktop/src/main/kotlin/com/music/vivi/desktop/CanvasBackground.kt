package com.music.vivi.desktop

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/**
 * Blurred, slowly-zooming artwork background behind the Player (Apple
 * Music–style). Animated GIF/WebP URLs animate via Coil; everything else shows
 * a static image with a subtle Ken Burns zoom for a "live" feel.
 *
 * Two things keep this cheap enough to sit under the whole player:
 *  - the blur is rasterized on a fixed [LAYER_DP] layer and only *scaled* to
 *    cover the window. Blurring a window-sized bitmap again on every frame of a
 *    frame-by-frame animated transform is what made the player heavy (the same
 *    mistake the blurred artwork backdrop had);
 *  - the zoom loop only runs while [animate] is true — a paused player has
 *    nothing to move, and an endless animation keeps the whole window redrawing
 *    at 60 fps for as long as the screen is open.
 */
@Composable
fun CanvasBackground(url: String?, modifier: Modifier = Modifier, animate: Boolean = true) {
    val dark = isAppInDarkTheme()
    // The Ken Burns zoom is a slow 14 s loop.
    val scale = if (animate) {
        val transition = rememberInfiniteTransition(label = "canvas")
        val animated by transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.18f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 14000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "canvasScale",
        )
        animated
    } else {
        1.09f
    }

    BoxWithConstraints(modifier.clipToBounds().background(MaterialTheme.colorScheme.surfaceVariant)) {
        if (!url.isNullOrBlank()) {
            // Scale needed for the fixed layer to cover the window whatever its
            // aspect ratio (the image is cropped inside the layer).
            val cover = maxOf(maxWidth, maxHeight) / LAYER_DP
            Box(
                Modifier.matchParentSize(),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    // The canvas layer is blurred/rasterized at a fixed size: a
                    // high-resolution variant is pointless here, but the
                    // provider's tiny `w120-h120` crop is visibly soft. 544 px
                    // is the tier the mobile app uses for it.
                    model = adjustedThumbnailUrl(url, 544, DesktopSettings.load().dataSaver),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    // Order matters: the transform sits OUTSIDE the blur, so the
                    // blurred layer is rasterized once and only blitted bigger.
                    modifier = Modifier
                        .size(LAYER_DP)
                        .graphicsLayer {
                            scaleX = cover.toFloat() * scale
                            scaleY = cover.toFloat() * scale
                        }
                        .blur(28.dp),
                )
            }
            // Scrim for contrast with the overlaid text/controls. Without artwork
            // the surface is the plain theme background (so the player follows
            // Light/Dark like the rest of the UI); with artwork the scrim is
            // stronger in dark mode and lighter in light mode.
            Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = if (dark) 0.35f else 0.2f)))
        }
    }
}

/** Side of the blurred layer that gets scaled up (kept in the fixed-size family). */
private val LAYER_DP = 384.dp
