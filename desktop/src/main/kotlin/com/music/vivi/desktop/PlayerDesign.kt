package com.music.vivi.desktop

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/** Player layout variant (ported from the mobile player-design toggles). */
enum class PlayerDesign(val key: String) {
    CLASSIC("classic"),
    NEW("new"),
    V2("v2"),
    EXPRESSIVE("expressive");

    companion object {
        fun from(key: String?): PlayerDesign = entries.firstOrNull { it.key == key } ?: CLASSIC
    }
}

/** Background style behind the full player (the mobile has these variants). */
enum class PlayerBackgroundStyle(val key: String) {
    CANVAS("canvas"),
    GRADIENT("gradient"),
    BLUR("blur"),
    GLOW("glow"),
    APPLE_MUSIC("apple_music"),
    LIVE_MESH("live_mesh"),
    /** Audio-reactive equalizer bars that follow the real decoded PCM level. */
    VISUALIZER("visualizer");

    companion object {
        fun from(key: String?): PlayerBackgroundStyle = entries.firstOrNull { it.key == key } ?: CANVAS
    }
}

/** Mini-player design layout variant (mirrors Android app's 3 miniplayer designs). */
enum class MiniPlayerDesign(val key: String) {
    CLASSIC("classic"),
    NEW("new"),
    APPLE("apple");

    companion object {
        fun from(key: String?): MiniPlayerDesign = when (key) {
            "classic", "mini_player_classic" -> CLASSIC
            "new", "mini_player_new" -> NEW
            "apple", "mini_player_apple" -> APPLE
            // Unknown/missing key: fall back to the enum's declared default.
            else -> CLASSIC
        }
    }
}

/** Mini-player dynamic background style (mirrors Android app's 5 background styles). */
enum class MiniPlayerBackgroundStyle(val key: String) {
    FOLLOW_THEME("follow_theme"),
    GRADIENT("gradient"),
    BLUR("blur"),
    GLOW_MOTION("glow_motion"),
    LIVE_MESH("live_mesh");

    companion object {
        fun from(key: String?): MiniPlayerBackgroundStyle = when (key) {
            "gradient" -> GRADIENT
            "blur" -> BLUR
            "glow", "glow_motion" -> GLOW_MOTION
            "live_mesh", "mesh" -> LIVE_MESH
            "follow_theme" -> FOLLOW_THEME
            // Unknown/missing key: fall back to the enum's declared default.
            else -> FOLLOW_THEME
        }
    }
}

/** Legacy mini-player style enum maintained for compatibility. */
enum class MiniPlayerStyle(val key: String) {
    STANDARD("standard"),
    APPLE("apple"),
    OUTLINE("outline"),
    PURE_BLACK("pure_black");

    companion object {
        fun from(key: String?): MiniPlayerStyle = entries.firstOrNull { it.key == key } ?: STANDARD
    }
}

/** Resolves the art size + title-overlay for a [PlayerDesign] variant. */
data class PlayerDesignMetrics(val artSize: Dp, val overlayTitle: Boolean, val artCorner: Dp)

fun PlayerDesign.metrics(): PlayerDesignMetrics = when (this) {
    PlayerDesign.CLASSIC -> PlayerDesignMetrics(360.dp, false, 11.dp)
    PlayerDesign.NEW -> PlayerDesignMetrics(400.dp, false, 11.dp)
    PlayerDesign.V2 -> PlayerDesignMetrics(440.dp, true, 11.dp)
    PlayerDesign.EXPRESSIVE -> PlayerDesignMetrics(521.dp, true, 11.dp)
}

/**
 * Full-screen background behind the Player, honoring the selected
 * [PlayerBackgroundStyle] (canvas / gradient / blur / glow / apple music /
 * live mesh).
 */
@Composable
fun PlayerBackground(
    style: PlayerBackgroundStyle,
    bgUrl: String?,
    accent: Color,
    modifier: Modifier = Modifier,
    /**
     * Current audio level (0..1) driving the VISUALIZER style; ignored by the
     * other styles. Supplied by the caller so it stays decoupled from the
     * audio pipeline.
     */
    audioLevel: Float = 0f,
    /** True while audio is actually playing (drives the visualizer decay). */
    isPlaying: Boolean = true,
) {
    Box(modifier.fillMaxSize().clipToBounds()) {
        when (style) {
            PlayerBackgroundStyle.CANVAS -> CanvasBackground(bgUrl, Modifier.fillMaxSize())
            PlayerBackgroundStyle.GRADIENT -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to accent.copy(alpha = 0.45f),
                                0.6f to accent.copy(alpha = 0.15f),
                                1f to Color.Transparent,
                            )
                        )
                )
            }
            PlayerBackgroundStyle.BLUR -> {
                if (bgUrl != null) {
                    AsyncImage(
                        model = bgUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().blur(48.dp),
                    )
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                )
            }
            PlayerBackgroundStyle.GLOW -> {
                val transition = rememberInfiniteTransition(label = "glow")
                val pulse by transition.animateFloat(
                    initialValue = 0.25f,
                    targetValue = 0.6f,
                    animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Reverse),
                    label = "glowPulse",
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    accent.copy(alpha = pulse),
                                    Color.Transparent,
                                    Color.Transparent,
                                ),
                                center = Offset(0.5f, 0.4f),
                                radius = 1400f,
                            )
                        )
                )
            }
            PlayerBackgroundStyle.APPLE_MUSIC -> {
                if (bgUrl != null) {
                    AsyncImage(
                        model = bgUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().blur(36.dp).graphicsLayer { scaleX = 1.15f; scaleY = 1.15f },
                    )
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = 0.35f),
                                1f to Color.Black.copy(alpha = 0.8f),
                            )
                        )
                )
            }
            PlayerBackgroundStyle.LIVE_MESH -> LiveMeshBackground(accent)
            PlayerBackgroundStyle.VISUALIZER -> VisualizerBackground(
                level = if (isPlaying) audioLevel else 0f,
                accent = accent,
            )
        }
    }
}

/**
 * Audio-reactive equalizer bars. The bars pulse with the real decoded audio
 * level (smoothly attacked) while each bar keeps its own gentle phase offset,
 * so the wall of bars reads as "music" even at low volume. The level decays
 * to silence when paused.
 */
@Composable
private fun VisualizerBackground(level: Float, accent: Color) {
    val smooth by animateFloatAsState(
        targetValue = level,
        animationSpec = tween(durationMillis = 120, easing = LinearEasing),
        label = "vizLevel",
    )
    val transition = rememberInfiniteTransition(label = "vizWobble")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "vizPhase",
    )
    val barCount = 56
    Canvas(Modifier.fillMaxSize()) {
        val barWidth = size.width / barCount
        val gap = barWidth * 0.28f
        val centerY = size.height * 0.5f
        val maxHalf = size.height * 0.42f
        val base = 0.08f + smooth * 0.8f
        for (i in 0 until barCount) {
            val wobble = 0.5f + 0.5f * kotlin.math.sin((phase * 2 * Math.PI + i * 0.55).toFloat())
            val height = (base * (0.35f + 0.65f * wobble) * maxHalf).coerceAtLeast(2f)
            val x = i * barWidth + gap / 2
            drawRoundRect(
                color = accent.copy(alpha = 0.28f + 0.55f * base * (0.4f + 0.6f * wobble)),
                topLeft = Offset(x, centerY - height),
                size = Size(barWidth - gap, height * 2f),
                cornerRadius = CornerRadius(barWidth * 0.3f, barWidth * 0.3f),
            )
        }
        // Dark scrim so the artwork/title above stays readable.
        drawRect(color = Color.Black.copy(alpha = 0.35f))
    }
}

/** Animated multi-blob gradient (live mesh). */
@Composable
private fun BoxScope.LiveMeshBackground(accent: Color) {
    val transition = rememberInfiniteTransition(label = "mesh")
    val dx by transition.animateFloat(
        initialValue = -0.4f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse),
        label = "meshDx",
    )
    val dy by transition.animateFloat(
        initialValue = -0.3f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Reverse),
        label = "meshDy",
    )
    val dz by transition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Reverse),
        label = "meshDz",
    )
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = size.width * dx; translationY = size.height * dy }
                .background(
                    Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.5f), Color.Transparent),
                        radius = 1200f,
                    )
                )
        )
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = size.width * -dx * 0.8f; translationY = size.height * dz }
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF6A4BFF).copy(alpha = 0.4f),
                            Color.Transparent,
                        ),
                        radius = 1000f,
                    )
                )
        )
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = size.width * dy; translationY = size.height * -dx * 0.7f }
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF00BFA5).copy(alpha = 0.35f),
                            Color.Transparent,
                        ),
                        radius = 900f,
                    )
                )
        )
    }
}

/**
 * Album artwork that slowly rotates when [rotating] is enabled (the mobile
 * "rotating thumbnail" option).
 */
@Composable
fun PlayerThumbnail(
    url: String?,
    size: Dp,
    corner: Dp,
    rotating: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!rotating) {
        Box(
            modifier
                .size(size)
                .clip(RoundedCornerShape(corner))
                .background(Color.Black.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Thumbnail(url, Modifier.fillMaxSize())
        }
        return
    }
    val transition = rememberInfiniteTransition(label = "rotatingArt")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(24_000, easing = LinearEasing), RepeatMode.Restart),
        label = "rotation",
    )
    Box(
        modifier
            .size(size)
            .graphicsLayer {
                rotationZ = angle
                // Slight breathing scale while rotating.
                scaleX = 1f + 0.02f * kotlin.math.sin(angle * kotlin.math.PI / 180.0).toFloat()
                scaleY = 1f + 0.02f * kotlin.math.sin(angle * kotlin.math.PI / 180.0).toFloat()
            }
            .clip(RoundedCornerShape(corner))
            .background(Color.Black.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Thumbnail(url, Modifier.fillMaxSize())
    }
}
