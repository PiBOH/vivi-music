package com.music.vivi.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.imageio.ImageIO

/** Number of frames pre-extracted from `desktop/icons/Vivi DE intro.mp4`. */
private const val INTRO_FRAME_COUNT = 139

/** Playback rate of the source video (30 fps → ~33 ms per frame). */
private const val INTRO_FPS = 30

/**
 * Full-screen animated intro played once at startup (Settings → System → Intro).
 *
 * The intro is a sequence of full-color JPEG frames extracted from the MP4 in
 * `desktop/icons/` (see `scripts/ExtractIntroFrames.java`). JPEG is used instead
 * of the original GIF because GIF is limited to 256 colors and showed visible
 * banding on gradients. Clicking anywhere skips it; when the animation ends it
 * calls [onFinished].
 */
@Composable
fun IntroSplash(language: String, onFinished: () -> Unit) {
    var frame by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(Unit) {
        val frameDelay = (1000L / INTRO_FPS)
        var index = 0
        while (index < INTRO_FRAME_COUNT) {
            val bmp = withContext(Dispatchers.IO) { loadIntroFrame(index) }
            if (bmp == null) {
                // Asset missing — never block startup on a broken intro.
                onFinished()
                return@LaunchedEffect
            }
            frame = bmp
            index++
            if (index < INTRO_FRAME_COUNT) delay(frameDelay)
        }
        onFinished()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onFinished() },
    ) {
        val current = frame
        if (current != null) {
            Image(
                bitmap = current,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
        Text(
            Localization.get(language, "click_to_skip"),
            color = Color.White.copy(alpha = 0.55f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
        )
    }
}

/** Loads one intro frame (`/images/intro/frame_NNN.jpg`) from the bundled resources. */
private fun loadIntroFrame(index: Int): ImageBitmap? {
    val name = "frame_" + index.toString().padStart(3, '0') + ".jpg"
    val stream = AppInfo::class.java.getResourceAsStream("/images/intro/$name") ?: return null
    return stream.use { s -> ImageIO.read(s)?.toComposeImageBitmap() }
}
