package com.music.vivi.desktop

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Surface
import org.jetbrains.skia.svg.SVGDOM
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

/**
 * The bundled brand logo.
 *
 * The mark ships twice: as an SVG and as a 1024x1024 PNG (both master files in
 * `desktop/icons/`, shipped under `resources/images/`), and this object is the
 * single place the UI asks for it.
 *
 * The vector is preferred, and it is what the tray icon and the sidebar
 * actually draw. Compose has no SVG loader of its own, but the Skia that Compose
 * Desktop is built on does ([SVGDOM]), so the mark is rasterized at exactly the
 * size the caller is about to draw it — 26 dp of sidebar on a 200% display is a
 * 52 px raster of the real artwork, not a 1024 px bitmap squeezed down to 52.
 *
 * The PNG stays as the fallback, and it is not a placeholder: if the vector
 * cannot be read or the SVG backend is unavailable (a stripped native build),
 * everything still works, just resampled. That is what [highQualityScaled] is
 * for — it halves the source repeatedly (each pass averages whole 2x2 blocks, so
 * no detail is skipped and no ringing is introduced) until the next halving
 * would undershoot the target, and only then finishes with one high-quality
 * interpolated step. A single 1024 -> 26 bilinear step is what made the small
 * sizes look cheap in the first place, and the report that came out of it is
 * "the tray and sidebar icons are very low quality".
 */
object BrandLogo {

    /**
     * The size the SVG is authored at: its `viewBox` and the units of every
     * coordinate in the file. The canvas is scaled to the target size instead of
     * asking Skia to re-lay-out the document at it — see [renderVector].
     */
    private const val SVG_INTRINSIC_PX = 1024f

    /** The vector master, read once. */
    private val svgBytes: ByteArray? by lazy { readVector() }

    /** Raw `logo_vmde.png`, read once. Used only when the vector cannot be. */
    private val source: BufferedImage? by lazy { readSource() }

    /**
     * Rasters of the vector, keyed by target size. The sidebar asks for the same
     * size on every recomposition, and the tray re-reads its icon on every track
     * change; neither should re-rasterize the SVG.
     */
    private val vectorCache = HashMap<Int, BufferedImage>()

    /** Compose-ready bitmaps, keyed by target size. */
    private val composeCache = HashMap<Int, ImageBitmap>()

    /** True when the mark could be read at all (false only in a broken build). */
    val available: Boolean get() = svgBytes != null || source != null

    /**
     * The logo resampled so that a caller drawing it at [sizeDp] can also bump
     * it for a HiDPI display ([scale] = 2 on a 200% monitor).
     */
    @Synchronized
    fun composeBitmap(sizePx: Int, scale: Float = 1f): ImageBitmap? {
        val target = (sizePx * scale).toInt().coerceIn(16, 1024)
        composeCache[target]?.let { return it }
        val bitmap = raster(target)
            ?.toComposeImageBitmap()
            ?: source?.let { highQualityScaled(it, target).toComposeImageBitmap() }
            ?: return null
        composeCache[target] = bitmap
        return bitmap
    }

    /** AWT image at [size] px, for the tray icon. */
    fun awtImage(size: Int): BufferedImage? {
        val target = size.coerceIn(1, 1024)
        raster(target)?.let { return it }
        val png = source ?: return null
        return highQualityScaled(png, target)
    }

    /** The mark at [size]x[size] px: the vector when it renders, else null. */
    @Synchronized
    private fun raster(size: Int): BufferedImage? {
        vectorCache[size]?.let { return it }
        val bytes = svgBytes ?: return null
        val image = renderVector(bytes, size) ?: return null
        vectorCache[size] = image
        return image
    }

    /**
     * Rasterizes the SVG at exactly [size]x[size] px.
     *
     * Returns null (never throws) when the SVG backend is missing, the file does
     * not parse, or the render comes out empty — which is what keeps the PNG
     * fallback reachable: a logo is not worth a crash, and an invisible icon is
     * worse than a resampled one.
     *
     * **The container size is the SVG's, not the target's.** Setting it to the
     * target size (`setContainerSize(size, size)`) looks right and is wrong:
     * measured with the bundled Skia (0.9.37.3), every render below 256 px comes
     * back **fully transparent** — 0 opaque pixels at 16, 26, 32, 48, 64 and
     * 128 px — and at 256 px only 20 964 of the expected 51 489. The mark was
     * shipped that way in 1.53.21, which is why the sidebar drew nothing and the
     * tray icon never appeared: an all-transparent tray image is an empty slot.
     * The container is now the mark's own 1024 (what its `viewBox` says) and the
     * *canvas* is scaled to the target, which renders correctly at every size
     * (26 px: 537 opaque pixels of 676, the expected 79 %).
     *
     * The decoded image is also copied into `TYPE_INT_ARGB`: Skia's PNG comes
     * back from ImageIO as `TYPE_4BYTE_ABGR`, and `toComposeImageBitmap()` only
     * accepts ARGB — the sidebar would fail on the type even with a good render.
     */
    private fun renderVector(bytes: ByteArray, size: Int): BufferedImage? = runCatching {
        if (!SkiaVectorLogo.available()) return null
        val data = org.jetbrains.skia.Data.makeFromBytes(bytes)
        val dom = SVGDOM(data)
        try {
            val surface = Surface.makeRasterN32Premul(size, size)
            try {
                dom.setContainerSize(SVG_INTRINSIC_PX, SVG_INTRINSIC_PX)
                val scale = size / SVG_INTRINSIC_PX
                surface.canvas.scale(scale, scale)
                dom.render(surface.canvas)
                val png = surface.makeImageSnapshot().encodeToData(EncodedImageFormat.PNG)
                    ?: return null
                val decoded = ImageIO.read(ByteArrayInputStream(png.bytes)) ?: return null
                toArgb(decoded).takeIf { it.hasVisiblePixels() }
            } finally {
                surface.close()
            }
        } finally {
            dom.close()
            data.close()
        }
    }.getOrNull()

    /** A `TYPE_INT_ARGB` copy of [image] (`toComposeImageBitmap` needs ARGB). */
    private fun toArgb(image: BufferedImage): BufferedImage {
        if (image.type == BufferedImage.TYPE_INT_ARGB) return image
        val out = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB)
        val g = out.createGraphics()
        try {
            g.drawImage(image, 0, 0, null)
        } finally {
            g.dispose()
        }
        return out
    }

    /**
     * True when anything in [image] is actually opaque. A fully transparent
     * raster is a failure, not a logo: it is what a mis-sized SVG render
     * produces, and it must send the caller to the PNG instead of to nothing.
     */
    private fun BufferedImage.hasVisiblePixels(): Boolean {
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (((getRGB(x, y) ushr 24) and 0xFF) > 8) return true
            }
        }
        return false
    }

    private fun readVector(): ByteArray? = runCatching {
        BrandLogo::class.java.getResourceAsStream("/images/logo_vmde.svg")?.use { it.readBytes() }
    }.getOrNull()

    private fun readSource(): BufferedImage? = runCatching {
        BrandLogo::class.java.getResourceAsStream("/images/logo_vmde.png")?.use { stream ->
            ImageIO.read(stream)
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

/**
 * True when the running Skia was built with the SVG module.
 *
 * Checked by name rather than by catching a `NoClassDefFoundError` on the class
 * itself: this file references [SVGDOM] directly, so a build without the module
 * would fail the class load of [BrandLogo] entirely — with the check, the
 * fallback path is still reachable. The result is memoized by the JVM's own
 * class loading, so this is a one-off lookup.
 */
private object SkiaVectorLogo {
    fun available(): Boolean = runCatching {
        Class.forName("org.jetbrains.skia.svg.SVGDOM", false, BrandLogo::class.java.classLoader)
        true
    }.getOrDefault(false)
}
