package com.music.vivi.desktop

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Full-screen animated intro played once at startup (Settings → System → Intro,
 * or Settings → Appearance → Intro).
 *
 * This is a *native* Compose animation — no video, GIF or frame sequence. The
 * VIVI Music DE logo fades/scales in with a gentle breathing pulse and fades
 * back out. Clicking anywhere (or the animation ending) calls [onFinished].
 *
 * Two dimensions are user-selectable in Settings:
 *  - [style]: what is shown — logo only / logo + app name / logo + name + version.
 *  - [background]: the backdrop — animated gradient / glow / dark.
 */
@Composable
fun IntroSplash(
    language: String,
    style: String,
    background: String,
    onFinished: () -> Unit,
) {
    val logo = remember { loadLogo() }
    val accent = MaterialTheme.colorScheme.primary

    // Entrance progress drives the fade + scale of the whole content column.
    val enter = remember { Animatable(0f) }
    val pulse = rememberInfiniteTransition(label = "intro_pulse")
    val breathe by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "breathe",
    )

    LaunchedEffect(Unit) {
        enter.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        delay(1700)
        onFinished()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(introBackgroundBrush(background, accent))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onFinished() },
        contentAlignment = Alignment.Center,
    ) {
        // Soft glow behind the logo (hidden on the flat "dark" background).
        if (background != "dark") {
            val glowRadius = with(LocalDensity.current) { 200.dp.toPx() }
            Box(
                Modifier
                    .size(400.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                accent.copy(alpha = (0.28f * breathe).coerceIn(0f, 0.34f)),
                                Color.Transparent,
                            ),
                            radius = glowRadius,
                        ),
                    ),
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                val s = enter.value * breathe
                scaleX = s
                scaleY = s
                alpha = enter.value
            },
        ) {
            if (logo != null) {
                Image(
                    bitmap = logo,
                    contentDescription = null,
                    modifier = Modifier.size(220.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            if (style != "logo") {
                Spacer(Modifier.height(20.dp))
                Text(
                    "VIVI Music DE",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                )
            }
            if (style == "logo_tagline") {
                Spacer(Modifier.height(6.dp))
                Text(
                    AppInfo.FULL_VERSION,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f),
                )
            }
        }

        Text(
            Localization.get(language, "click_to_skip"),
            color = Color.White.copy(alpha = 0.5f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
        )
    }
}

/** Builds the backdrop brush for the requested background variant. */
private fun introBackgroundBrush(background: String, accent: Color): Brush = when (background) {
    "dark" -> Brush.verticalGradient(listOf(Color(0xFF151519), Color(0xFF0A0A0D)))
    "glow" -> Brush.verticalGradient(
        listOf(accent.copy(alpha = 0.18f), Color(0xFF100D14), Color(0xFF0A0A0D)),
    )
    else -> Brush.linearGradient(
        listOf(
            accent.copy(alpha = 0.32f),
            Color(0xFF0A0A0D),
            Color(0xFF0A0A0D),
        ),
    )
}

/** Loads the bundled official VIVI Music DE logo (`/images/logo_vmde.png`). */
internal fun loadLogo(): ImageBitmap? = runCatching {
    val stream = AppInfo::class.java.getResourceAsStream("/images/logo_vmde.png") ?: return null
    stream.use { s -> javax.imageio.ImageIO.read(s)?.toComposeImageBitmap() }
}.getOrNull()
