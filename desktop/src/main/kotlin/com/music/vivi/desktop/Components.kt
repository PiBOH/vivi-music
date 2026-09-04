package com.music.vivi.desktop

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.sin
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.YTItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Renders a heavily blurred, opaque backdrop derived from an artwork URL,
 * cached per artwork URL to ensure zero performance hit on recomposition/window resizing.
 */
@Composable
fun CachedBlurBackdrop(
    artworkUrl: String?,
    modifier: Modifier = Modifier,
    blurRadiusPx: Float = 80f,
    scrimColor: Color = Color.Black.copy(alpha = 0.55f),
    fallbackColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable BoxScope.() -> Unit
) {
    var cachedBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var loadedUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(artworkUrl) {
        if (artworkUrl.isNullOrBlank()) {
            cachedBitmap = null
            loadedUrl = null
            return@LaunchedEffect
        }
        if (loadedUrl == artworkUrl && cachedBitmap != null) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            try {
                val url = java.net.URI(artworkUrl).toURL()
                val bytes = url.readBytes()
                val skiaImage = org.jetbrains.skia.Image.makeFromEncoded(bytes)
                val targetW = (skiaImage.width / 4).coerceAtLeast(64)
                val targetH = (skiaImage.height / 4).coerceAtLeast(64)
                val surface = org.jetbrains.skia.Surface.makeRasterN32Premul(targetW, targetH)
                val canvas = surface.canvas
                val blurFilter = org.jetbrains.skia.ImageFilter.makeBlur(
                    blurRadiusPx / 4f,
                    blurRadiusPx / 4f,
                    org.jetbrains.skia.FilterTileMode.CLAMP
                )
                val paint = org.jetbrains.skia.Paint().apply { imageFilter = blurFilter }
                canvas.drawImageRect(
                    skiaImage,
                    org.jetbrains.skia.Rect.makeWH(skiaImage.width.toFloat(), skiaImage.height.toFloat()),
                    org.jetbrains.skia.Rect.makeWH(targetW.toFloat(), targetH.toFloat()),
                    paint
                )
                val snapshot = surface.makeImageSnapshot()
                val composeBitmap = snapshot.toComposeImageBitmap()
                withContext(Dispatchers.Main) {
                    cachedBitmap = composeBitmap
                    loadedUrl = artworkUrl
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    cachedBitmap = null
                    loadedUrl = artworkUrl
                }
            }
        }
    }

    Box(modifier = modifier) {
        val bitmap = cachedBitmap
        if (bitmap != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawImage(
                    image = bitmap,
                    dstSize = IntSize(size.width.toInt(), size.height.toInt())
                )
                drawRect(color = scrimColor)
            }
        } else {
            Box(Modifier.fillMaxSize().background(fallbackColor)) {
                Box(Modifier.fillMaxSize().background(scrimColor))
            }
        }
        content()
    }
}

/**
 * Playback context for list rows: which track is currently loaded, whether it
 * is playing, and the live audio level (0..1) for the now-playing indicator.
 * Provided by the app root; rows read it via [LocalPlayback].
 */
data class PlaybackContext(
    val videoId: String? = null,
    val isPlaying: Boolean = false,
    val audioLevel: StateFlow<Float>? = null,
    /** Buffered fraction (0..1) of the current track (1f = fully cached / not
     *  streaming). Powers the YouTube-style secondary segment on seek bars. */
    val bufferedFraction: StateFlow<Float>? = null,
    /** Fraction (0..1) of the current track scrubbed while its duration was
     *  still unknown (loaded from the restored queue but never played); null
     *  when the duration is known or nothing was scrubbed. Keeps the seek bar
     *  thumb at the scrubbed point until playback actually begins. */
    val pendingSeekFraction: StateFlow<Float?>? = null,
)

/** Composition local exposing the current playback to list rows (SongRow etc.). */
val LocalPlayback = compositionLocalOf { PlaybackContext() }

/**
 * Current buffered fraction for the seek bars, collected from [LocalPlayback].
 * Returns 1f when the app root doesn't provide one (no stream loaded or the
 * composable is rendered outside the provider): a full buffer means the
 * secondary "buffered" segment stays hidden.
 */
@Composable
fun playbackBufferedFraction(): Float {
    val flow = LocalPlayback.current.bufferedFraction ?: return 1f
    return flow.collectAsState().value
}

/**
 * Current pending scrub fraction (0..1) for the seek bars, collected from
 * [LocalPlayback]. Returns null when the duration is known (real seek range)
 * or nothing was scrubbed yet.
 */
@Composable
fun playbackPendingSeekFraction(): Float? {
    return LocalPlayback.current.pendingSeekFraction?.collectAsState()?.value
}

/** Square-ish artwork with a neutral placeholder behind it while loading. */
@Composable
fun Thumbnail(url: String?, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(8.dp)
    Box(modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceVariant)) {
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

fun formatDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun subtitleOf(item: YTItem): String = when (item) {
    is SongItem -> item.artists.joinToString(", ") { it.name }
    is AlbumItem -> buildString {
        item.artists?.joinToString(", ") { it.name }?.takeIf { it.isNotBlank() }?.let { append(it) }
        if (item.year != null) {
            if (isNotEmpty()) append(" • ")
            append(item.year)
        }
    }
    is ArtistItem -> "Artist"
    is PlaylistItem -> item.author?.name ?: item.songCountText.orEmpty()
}

/** Compact tile used in the Home/Search/Artist carousels and Browse grid. */
@Composable
fun YtItemCard(item: YTItem, onClick: () -> Unit, width: Dp? = 140.dp, modifier: Modifier = Modifier) {
    val root = if (width != null) Modifier.width(width) else Modifier.fillMaxWidth()
    Column(root.then(modifier).clickable(onClick = onClick)) {
        Thumbnail(item.thumbnail, Modifier.fillMaxWidth().aspectRatio(1f))
        Text(
            item.title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        val subtitle = subtitleOf(item)
        if (subtitle.isNotBlank()) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** One song in a vertical list (album / playlist / search songs). */
@Composable
fun SongRow(
    song: SongItem,
    language: String,
    onClick: () -> Unit,
    onAddToQueue: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
) {
    val playback = LocalPlayback.current
    val isCurrent = song.id == playback.videoId
    val accent = MaterialTheme.colorScheme.primary
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp)
            .let { if (isCurrent) it.background(accent.copy(alpha = 0.07f), RoundedCornerShape(8.dp)) else it }
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Thumbnail(song.thumbnail, Modifier.size(48.dp))
        if (isCurrent) {
            Spacer(Modifier.width(8.dp))
            NowPlayingBars(
                audioLevel = playback.audioLevel,
                isPlaying = playback.isPlaying,
                color = accent,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isCurrent) accent else Color.Unspecified,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                song.artists.joinToString(", ") { it.name },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onAddToPlaylist != null) {
            Tooltip(Localization.get(language, "add_to_playlist")) {
                IconButton(onClick = onAddToPlaylist) {
                    Icon(
                        Icons.AutoMirrored.Filled.PlaylistAdd,
                        contentDescription = Localization.get(language, "add_to_playlist"),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        SongMenu(song = song, language = language, onAddToPlaylist = onAddToPlaylist)
        if (onAddToQueue != null) {
            Text(
                "＋",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { onAddToQueue() }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        song.duration?.let {
            Text(it.let(::formatDuration), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Small animated equalizer bars marking the currently playing row. When
 * [audioLevel] is available and the track is playing, the bars move with the
 * real decoded audio; otherwise they fall back to a gentle idle pulse. Only
 * composed for the single current row, so the ~43 Hz level updates stay cheap.
 */
@Composable
private fun NowPlayingBars(
    audioLevel: StateFlow<Float>?,
    isPlaying: Boolean,
    color: Color,
) {
    val level by (audioLevel ?: remember { kotlinx.coroutines.flow.MutableStateFlow(0.5f) }).collectAsState()
    val target = if (isPlaying) level.coerceIn(0.15f, 1f) else 0.08f
    val smooth by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 140, easing = LinearEasing),
        label = "npBarsLevel",
    )
    val transition = rememberInfiniteTransition(label = "npBars")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "npBarsPhase",
    )
    Canvas(Modifier.size(width = 16.dp, height = 14.dp)) {
        val barWidth = size.width / 5f
        val gap = barWidth * 0.8f
        val maxH = size.height
        for (i in 0 until 3) {
            val wobble = 0.5f + 0.5f * sin((phase * 2 * Math.PI + i * 1.4).toFloat())
            val h = (smooth * maxH * (0.45f + 0.55f * wobble)).coerceAtLeast(3f)
            drawRoundRect(
                color = color,
                topLeft = Offset(i * (barWidth + gap), (maxH - h) / 2f),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }
    }
}

@Composable
fun LoadingBox(language: String) {
    Box(Modifier.fillMaxSize().padding(16.dp)) { Text(Localization.get(language, "loading")) }
}

@Composable
fun ErrorBox(language: String, message: String?) {
    Box(Modifier.fillMaxSize().padding(16.dp)) {
        // Selectable so every network/load error can be copied for a bug report.
        SelectionContainer {
            Text("${Localization.get(language, "error")}: $message", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun BackButton(language: String, onClick: () -> Unit) {
    Text(
        "‹ ${Localization.get(language, "back")}",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clickable(onClick = onClick).padding(vertical = 8.dp),
    )
}

/** Route a tap on any [YTItem] to the matching screen/action. */
fun onItemClick(
    item: YTItem,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onPlaySong: (SongItem) -> Unit,
) {
    when (item) {
        is AlbumItem -> onOpenAlbum(item.browseId)
        is ArtistItem -> onOpenArtist(item.id)
        is PlaylistItem -> onOpenPlaylist(item.id)
        is SongItem -> onPlaySong(item)
    }
}

/**
 * Section header in the style of the Android app's `NavigationTitle`: an
 * optional label above a bold, primary-coloured title, with an optional
 * "Play all" button and a chevron when the whole header is clickable.
 */
@Composable
fun SectionHeader(
    title: String,
    language: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    onClick: (() -> Unit)? = null,
    onPlayAll: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            if (!label.isNullOrBlank()) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (onPlayAll != null) {
            OutlinedButton(
                onClick = onPlayAll,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp),
            ) {
                Text(Localization.get(language, "play_all"), style = MaterialTheme.typography.labelSmall)
            }
        }

        if (onClick != null) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** A single Mood & genres chip, styled like the Android app's button with M3 Expressive geometry. */
@Composable
fun MoodAndGenresButton(title: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 8.dp, bottomEnd = 18.dp, bottomStart = 8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ---------------------------------------------------------------------------
// ViviSlider: slim / squiggly / wavy slider styles (ported from mobile)
// ---------------------------------------------------------------------------

/** Slider visual style (matches the mobile slider-style setting). */
enum class ViviSliderStyle(val key: String) {
    SLIM("slim"),
    EXPRESSIVE("expressive"),
    SQUIGGLY("squiggly"),
    WAVY("wavy");

    companion object {
        fun from(key: String?): ViviSliderStyle = entries.firstOrNull { it.key == key } ?: SLIM
    }
}

/**
 * Custom slider with track styles:
 * - SLIM: thin straight track (default Material look)
 * - EXPRESSIVE: thick rounded capsule track (Apple Music / M3 Expressive look)
 * - SQUIGGLY: tight zig-zag bumps along the track
 * - WAVY: smooth sine wave along the track
 */
@Composable
fun ViviSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    style: ViviSliderStyle = ViviSliderStyle.SLIM,
    enabled: Boolean = true,
    /** Optional buffered fraction (0..1) drawn as a fainter secondary segment
     *  behind the played portion (YouTube-style). Null hides it entirely;
     *  a full buffer (1f) draws it full-width behind the played fill, like a
     *  fully loaded YouTube video. */
    bufferedFraction: Float? = null,
    modifier: Modifier = Modifier,
) {
    val range = valueRange.endInclusive - valueRange.start
    val fraction = if (range == 0f) 0f else ((value - valueRange.start) / range).coerceIn(0f, 1f)
    // The buffer segment stays visible like on YouTube: it grows while the
    // stream downloads and remains full-width once fully loaded/cached (it is
    // only hidden when the buffered fraction is null, i.e. no stream info).
    val buffer = (bufferedFraction ?: 1f).coerceIn(0f, 1f)
    val showBuffer = bufferedFraction != null && buffer > fraction + 0.002f
    val bufferColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    val primary = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val thumbColor = if (enabled) primary else MaterialTheme.colorScheme.outline

    Box(
        modifier
            .height(28.dp)
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    if (enabled) {
                        val f = (offset.x / size.width).coerceIn(0f, 1f)
                        onValueChange(valueRange.start + f * range)
                        onValueChangeFinished?.invoke()
                    }
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { if (enabled) onValueChangeFinished?.invoke() },
                    onDragCancel = { if (enabled) onValueChangeFinished?.invoke() },
                ) { change, _ ->
                    if (enabled) {
                        val f = (change.position.x / size.width).coerceIn(0f, 1f)
                        onValueChange(valueRange.start + f * range)
                        change.consume()
                    }
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val trackHeight = when (style) {
            ViviSliderStyle.SLIM -> 3.dp
            ViviSliderStyle.EXPRESSIVE -> 6.dp
            ViviSliderStyle.SQUIGGLY -> 6.dp
            ViviSliderStyle.WAVY -> 6.dp
        }
        Canvas(Modifier.fillMaxWidth().height(28.dp)) {
            val cy = size.height / 2f
            val amp = when (style) {
                ViviSliderStyle.SLIM, ViviSliderStyle.EXPRESSIVE -> 0f
                ViviSliderStyle.SQUIGGLY -> 3.5f
                ViviSliderStyle.WAVY -> 6f
            }
            val freq = when (style) {
                ViviSliderStyle.SLIM, ViviSliderStyle.EXPRESSIVE -> 0f
                ViviSliderStyle.SQUIGGLY -> 14f
                ViviSliderStyle.WAVY -> 3f
            }
            fun waveY(x: Float): Float = cy + if (freq > 0f) {
                kotlin.math.sin((x / size.width) * freq * 2f * kotlin.math.PI.toFloat()) * amp
            } else 0f

            fun buildPath(toX: Float): Path {
                val p = Path()
                p.moveTo(0f, waveY(0f))
                var x = 0f
                while (x <= toX) {
                    p.lineTo(x, waveY(x))
                    x += 4f
                }
                return p
            }

            if (style == ViviSliderStyle.SLIM) {
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(0f, cy - 1.5f),
                    size = androidx.compose.ui.geometry.Size(size.width, 3f),
                    cornerRadius = CornerRadius(1.5f, 1.5f),
                )
                if (showBuffer) {
                    drawRoundRect(
                        color = bufferColor,
                        topLeft = Offset(0f, cy - 1.5f),
                        size = androidx.compose.ui.geometry.Size(size.width * buffer, 3f),
                        cornerRadius = CornerRadius(1.5f, 1.5f),
                    )
                }
                drawRoundRect(
                    color = primary,
                    topLeft = Offset(0f, cy - 1.5f),
                    size = androidx.compose.ui.geometry.Size(size.width * fraction, 3f),
                    cornerRadius = CornerRadius(1.5f, 1.5f),
                )
            } else if (style == ViviSliderStyle.EXPRESSIVE) {
                val h = 6.dp.toPx()
                // Inactive track (thick capsule)
                drawRoundRect(
                    color = trackColor.copy(alpha = 0.6f),
                    topLeft = Offset(0f, cy - h / 2f),
                    size = androidx.compose.ui.geometry.Size(size.width, h),
                    cornerRadius = CornerRadius(h / 2f, h / 2f),
                )
                // Buffered portion (fainter, behind the played fill)
                if (showBuffer) {
                    drawRoundRect(
                        color = bufferColor,
                        topLeft = Offset(0f, cy - h / 2f),
                        size = androidx.compose.ui.geometry.Size(size.width * buffer, h),
                        cornerRadius = CornerRadius(h / 2f, h / 2f),
                    )
                }
                // Active track (thick capsule fill)
                if (fraction > 0f) {
                    drawRoundRect(
                        color = primary,
                        topLeft = Offset(0f, cy - h / 2f),
                        size = androidx.compose.ui.geometry.Size(size.width * fraction, h),
                        cornerRadius = CornerRadius(h / 2f, h / 2f),
                    )
                }
            } else {
                val stroke = Stroke(width = trackHeight.toPx(), cap = StrokeCap.Round)
                drawPath(buildPath(size.width), color = trackColor, style = stroke)
                if (showBuffer) {
                    drawPath(buildPath(size.width * buffer), color = bufferColor, style = stroke)
                }
                if (fraction > 0.001f) {
                    drawPath(buildPath(size.width * fraction), color = primary, style = stroke)
                }
            }

            // Thumb
            val thumbX = size.width * fraction
            val thumbRadius = when (style) {
                ViviSliderStyle.SLIM -> 6f
                ViviSliderStyle.EXPRESSIVE -> 7f
                else -> 7f
            }
            drawCircle(
                color = thumbColor,
                radius = thumbRadius,
                center = Offset(thumbX, waveY(thumbX)),
            )
            // Subtle ring so the thumb is visible on any background.
            drawCircle(
                color = Color.White.copy(alpha = 0.35f),
                radius = thumbRadius,
                center = Offset(thumbX, waveY(thumbX)),
                style = Stroke(width = 1.5f),
            )
        }
    }
}
