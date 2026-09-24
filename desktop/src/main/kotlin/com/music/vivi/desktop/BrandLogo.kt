package com.music.vivi.desktop

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage

/**
 * The bundled brand logo, resampled properly.
 *
 * The logo ships as a 1024x1024 PNG (gradients, transparency and the white
 * rings of the mark — see `desktop/icons/logo_vmde.png`). Everything that draws
 * it is small: the sidebar shows it at 26-30 dp and the tray icon at 16-64 px.
 * Resampling 1024 px down to 26 px in **one** pass is what made both look cheap:
 * a single bilinear/box step drops most of the artwork's detail, which is
 * exactly the "the tray and sidebar icons are very low quality" report. There is
 * also nothing to gain from shipping a hand-traced SVG here — the mark's
 * gradients and soft edges survive a proper downscale, and a trace of it (the
 * `logo_vmde.svg` that was lying in `desktop/icons/`) comes out flat black and
 * white, which would be a downgrade.
 *
 * [highQualityScaled] therefore halves the image repeatedly (each pass averages
 * whole 2x2 blocks, so no detail is skipped and no ringing is introduced) until
 * the next halving would undershoot the target, and only then finishes with one
 * high-quality interpolated step.
 */
object BrandLogo {

    /** Raw `logo_vmde.png`, read once. */
    private val source: BufferedImage? by lazy { readSource() }

    /**
     * The logo resampled so that a caller drawing it at [sizeDp] can also bump
     * it for a HiDPI display ([scale] = 2 on a 200% monitor).
     */
    private val composeCache = HashMap<Int, ImageBitmap>()

    /** True when the bundled logo could be read (false only in a broken build). */
    val available: Boolean get() = source != null

    @Synchronized
    fun composeBitmap(sizePx: Int, scale: Float = 1f): ImageBitmap? {
        val target = (sizePx * scale).toInt().coerceIn(16, 1024)
        composeCache[target]?.let { return it }
        val bitmap = source?.let { highQualityScaled(it, target).toComposeImageBitmap() } ?: return null
        // The sidebar asks for the same size on every recomposition; the cache
        // keeps this from re-rasterizing the 1024 px source each time.
        composeCache[target] = bitmap
        return bitmap
    }

    /** AWT image at [size] px, for the tray icon. */
    fun awtImage(size: Int): BufferedImage? {
        val source = source ?: return null
        return highQualityScaled(source, size)
    }

    private fun readSource(): BufferedImage? = runCatching {
        BrandLogo::class.java.getResourceAsStream("/images/logo_vmde.png")?.use { stream ->
            javax.imageio.ImageIO.read(stream)
        }
    }.getOrNull()

    /**
     * Scales [image] to a square of [size] px, keeping the alpha channel.
     *
     * Repeated halving is the point: it is the difference between "a small logo"
     * and "a small logo that still looks like the logo".
     */
    fun highQualityScaled(image: BufferedImage, size: Int): BufferedImage {
        val target = size.coerceAtLeast(1)
        var current = image
        while (current.width / 2 >= target && current.height / 2 >= target) {
            current = halve(current)
        }
        if (current.width == target && current.height == target) return current
        val out = BufferedImage(target, target, BufferedImage.TYPE_INT_ARGB)
        val g: Graphics2D = out.createGraphics()
        try {
            g.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC,
            )
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.setRenderingHint(
                RenderingHints.KEY_ALPHA_INTERPOLATION,
                RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY,
            )
            g.drawImage(current, 0, 0, target, target, null)
        } finally {
            g.dispose()
        }
        return out
    }

    /** Averages every 2x2 block — an anti-aliased downscale with no detail loss. */
    private fun halve(image: BufferedImage): BufferedImage {
        val w = (image.width / 2).coerceAtLeast(1)
        val h = (image.height / 2).coerceAtLeast(1)
        val out = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until h) {
            for (x in 0 until w) {
                var a = 0L
                var r = 0L
                var g = 0L
                var b = 0L
                for (dy in 0 until 2) {
                    for (dx in 0 until 2) {
                        val argb = image.getRGB(
                            (x * 2 + dx).coerceAtMost(image.width - 1),
                            (y * 2 + dy).coerceAtMost(image.height - 1),
                        )
                        val alpha = (argb ushr 24) and 0xFF
                        // Weight the colour by alpha: averaging a transparent
                        // pixel's black into the edge would darken the mark's
                        // outline instead of softening it.
                        a += alpha.toLong()
                        r += ((argb shr 16) and 0xFF).toLong() * alpha
                        g += ((argb shr 8) and 0xFF).toLong() * alpha
                        b += (argb and 0xFF).toLong() * alpha
                    }
                }
                val alpha = (a / 4).toInt().coerceIn(0, 255)
                val weight = a.coerceAtLeast(1L)
                val rr = (r / weight).toInt().coerceIn(0, 255)
                val gg = (g / weight).toInt().coerceIn(0, 255)
                val bb = (b / weight).toInt().coerceIn(0, 255)
                out.setRGB(x, y, (alpha shl 24) or (rr shl 16) or (gg shl 8) or bb)
            }
        }
        return out
    }
}
