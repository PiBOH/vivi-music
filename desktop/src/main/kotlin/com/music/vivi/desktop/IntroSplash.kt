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
import kotlinx.coroutines.delay
import javax.imageio.ImageIO
import javax.imageio.ImageReader
import javax.imageio.metadata.IIOMetadata
import javax.imageio.metadata.IIOMetadataNode
import javax.imageio.stream.ImageInputStream

/**
 * Full-screen animated intro played once at startup (Settings → System → Intro).
 * Clicking anywhere skips it; when the animation ends it calls [onFinished].
 */
@Composable
fun IntroSplash(language: String, onFinished: () -> Unit) {
    var frame by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(Unit) {
        playIntroGif { bmp ->
            frame = bmp
        }
        // Hold the last frame briefly so the ending isn't abrupt.
        delay(120)
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

/**
 * Decodes and plays the bundled `intro.gif` once, invoking [onFrame] for each
 * decoded frame. Frames are read lazily so only one bitmap is held in memory at
 * a time. Returns immediately (no frames) if the resource is missing.
 */
private suspend fun playIntroGif(onFrame: (ImageBitmap) -> Unit) {
    val stream = AppInfo::class.java.getResourceAsStream("/images/intro.gif") ?: return
    stream.use { s ->
        val input: ImageInputStream = ImageIO.createImageInputStream(s) ?: return
        val readers = ImageIO.getImageReaders(input)
        val reader: ImageReader = readers.asSequence()
            .firstOrNull { it.formatName.equals("gif", ignoreCase = true) }
            ?: return
        reader.setInput(input)
        try {
            val count = reader.getNumImages(true)
            for (i in 0 until count) {
                val delayMs = gifFrameDelayMs(reader.getImageMetadata(i))
                val buffered = reader.read(i) ?: continue
                onFrame(buffered.toComposeImageBitmap())
                delay(delayMs.toLong())
            }
        } catch (_: Exception) {
            // Never let a broken/intro GIF block startup.
        } finally {
            runCatching { reader.dispose() }
        }
    }
}

/** Frame delay in milliseconds, read from the GIF Graphic Control Extension. */
private fun gifFrameDelayMs(meta: IIOMetadata?): Int {
    if (meta == null) return 100
    return try {
        val root = meta.getAsTree("javax_imageio_gif_image_1.0") as? IIOMetadataNode
        val gce = findChild(root, "GraphicControlExtension")
        val hundredths = gce?.getAttribute("delayTime")?.toIntOrNull() ?: 0
        (if (hundredths <= 0) 10 else hundredths) * 10
    } catch (_: Exception) {
        100
    }
}

private fun findChild(node: IIOMetadataNode?, name: String): IIOMetadataNode? {
    if (node == null) return null
    for (i in 0 until node.length) {
        val child = node.item(i) as? IIOMetadataNode ?: continue
        if (child.nodeName == name) return child
        findChild(child, name)?.let { return it }
    }
    return null
}
