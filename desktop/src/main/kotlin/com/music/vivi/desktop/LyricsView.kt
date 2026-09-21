package com.music.vivi.desktop

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.lyrics.LyricLine
import com.music.lyrics.LyricsParser
import com.music.lyrics.LyricsRomanizer
import com.music.lyrics.WordTimestamp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

/**
 * Animated lyrics view — the desktop port of the mobile app's lyrics renderer.
 *
 * Every entry of [LyricsAnimationStyle] reproduces the mobile recipe of the same
 * name (the word fills, the halo strengths, the alpha falloff of the line styled
 * with separate word composables). [LyricsAnimationStyle.ALPHA] is the only
 * desktop-only entry: it is the plain line list the desktop drew before the
 * mobile look was ported.
 *
 * Every style reads the playback position through a `State` inside the item
 * lambda, so the ~40 position updates per second recompose only the visible
 * lines instead of the whole panel.
 */
enum class LyricsAnimationStyle(val id: String) {
    NONE("NONE"),
    FADE("FADE"),
    GLOW("GLOW"),
    SLIDE("SLIDE"),
    KARAOKE("KARAOKE"),
    APPLE("APPLE"),
    APPLE_V2("APPLE_V2"),
    VIVIMUSIC_1("VIVIMUSIC_1"),
    LYRICS_V2("LYRICS_V2"),
    METRO_LYRICS("METRO_LYRICS"),

    /** Desktop-only: the pre-port look (one plain text per line). */
    ALPHA("ALPHA"),
    ;

    companion object {
        /** Tolerant lookup: an unknown/removed id falls back to the default. */
        fun from(id: String?): LyricsAnimationStyle =
            entries.firstOrNull { it.id.equals(id?.trim(), ignoreCase = true) } ?: VIVIMUSIC_1
    }
}

/** Horizontal alignment of the lyric block (mobile's "Text position"). */
enum class LyricsPosition {
    LEFT, CENTER, RIGHT;

    companion object {
        fun from(id: String?): LyricsPosition =
            entries.firstOrNull { it.name.equals(id?.trim(), ignoreCase = true) } ?: CENTER
    }
}

/**
 * How much faster/slower than real time the lyrics run, from the global
 * "Animation speed" preference (Appearance → Animation speed).
 *
 * One scale factor for the whole renderer: every style positions its words on
 * the timeline, so stretching the timeline there makes the setting apply to all
 * of them without a second copy of the maths in each one.
 */
private val LYRICS_SPEED: Float
    get() = when (Animations.speed) {
        "fast" -> 1.6f
        "slow" -> 0.62f
        else -> 1f
    }

/** Playback position on the (possibly stretched) lyric timeline of one line. */
private fun lyricPosition(position: Long, lineStartMs: Long): Long =
    if (LYRICS_SPEED == 1f) {
        position
    } else {
        lineStartMs + ((position - lineStartMs) * LYRICS_SPEED).toLong()
    }

/** Smoothstep of a linear 0..1 progress (the mobile renderer's easing). */
private fun smoothstep(linear: Float): Float {
    val t = linear.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/** Everything the renderer needs besides the lines and the position. */
data class LyricsDisplayOptions(
    val style: LyricsAnimationStyle = LyricsAnimationStyle.VIVIMUSIC_1,
    val glowEffect: Boolean = true,
    val appleMusicBlur: Boolean = false,
    val standardBlur: Boolean = false,
    val clickToSeek: Boolean = true,
    val autoScroll: Boolean = true,
    val position: LyricsPosition = LyricsPosition.CENTER,
    val textSizeSp: Float = 18f,
    val lineSpacing: Float = 1.35f,
    /** Null when romanization is off; otherwise the enabled scripts. */
    val romanize: LyricsRomanizer.Options? = null,
    val romanizeAsMain: Boolean = false,
    /** One translated line per lyric line, or null when off/unavailable. */
    val translated: List<String?>? = null,
)

/**
 * Reads the persisted settings into the renderer's option object.
 *
 * Romanization is "on" only when at least one script is enabled; that keeps a
 * single source of truth (the master switch in the settings screen) instead of
 * a second boolean that can disagree with the per-script flags.
 */
fun lyricsDisplayOptionsFrom(state: DesktopSyncState): LyricsDisplayOptions {
    val scripts = LyricsRomanizer.Options(
        japanese = state.lyricsRomanizeJapanese,
        korean = state.lyricsRomanizeKorean,
        chinese = state.lyricsRomanizeChinese,
        russian = state.lyricsRomanizeRussian,
        ukrainian = state.lyricsRomanizeUkrainian,
        serbian = state.lyricsRomanizeSerbian,
        bulgarian = state.lyricsRomanizeBulgarian,
        belarusian = state.lyricsRomanizeBelarusian,
        kyrgyz = state.lyricsRomanizeKyrgyz,
        macedonian = state.lyricsRomanizeMacedonian,
        hindi = state.lyricsRomanizeHindi,
        punjabi = state.lyricsRomanizePunjabi,
    )
    val anyScript = with(scripts) {
        japanese || korean || chinese || russian || ukrainian || serbian ||
            bulgarian || belarusian || kyrgyz || macedonian || hindi || punjabi
    }
    return LyricsDisplayOptions(
        style = LyricsAnimationStyle.from(state.lyricsAnimationStyle),
        glowEffect = state.lyricsGlowEffect,
        appleMusicBlur = state.lyricsAppleMusicBlur,
        standardBlur = state.lyricsStandardBlur,
        clickToSeek = state.lyricsClickToSeek,
        autoScroll = state.lyricsAutoScroll,
        position = LyricsPosition.from(state.lyricsTextPosition),
        textSizeSp = state.lyricsTextSize,
        lineSpacing = state.lyricsLineSpacing,
        romanize = if (anyScript) scripts else null,
        romanizeAsMain = state.lyricsRomanizeAsMain,
    )
}

/** Writes [options] back into the persisted settings. */
fun DesktopSyncState.withLyricsDisplayOptions(options: LyricsDisplayOptions): DesktopSyncState {
    val scripts = options.romanize ?: LyricsRomanizer.Options(
        japanese = false, korean = false, chinese = false, russian = false,
        ukrainian = false, serbian = false, bulgarian = false, belarusian = false,
        kyrgyz = false, macedonian = false, hindi = false, punjabi = false,
    )
    return copy(
        lyricsAnimationStyle = options.style.id,
        lyricsGlowEffect = options.glowEffect,
        lyricsAppleMusicBlur = options.appleMusicBlur,
        lyricsStandardBlur = options.standardBlur,
        lyricsClickToSeek = options.clickToSeek,
        lyricsAutoScroll = options.autoScroll,
        lyricsTextPosition = options.position.name,
        lyricsRomanizeJapanese = scripts.japanese,
        lyricsRomanizeKorean = scripts.korean,
        lyricsRomanizeChinese = scripts.chinese,
        lyricsRomanizeRussian = scripts.russian,
        lyricsRomanizeUkrainian = scripts.ukrainian,
        lyricsRomanizeSerbian = scripts.serbian,
        lyricsRomanizeBulgarian = scripts.bulgarian,
        lyricsRomanizeBelarusian = scripts.belarusian,
        lyricsRomanizeKyrgyz = scripts.kyrgyz,
        lyricsRomanizeMacedonian = scripts.macedonian,
        lyricsRomanizeHindi = scripts.hindi,
        lyricsRomanizePunjabi = scripts.punjabi,
        lyricsRomanizeAsMain = options.romanizeAsMain,
    )
}

/** The AI translation settings, or null when the option is off/unconfigured. */
fun lyricsTranslationConfig(state: DesktopSyncState, enabled: Boolean): LyricsTranslator.Config? {
    if (!enabled) return null
    return LyricsTranslator.Config(
        provider = state.aiProvider,
        apiKey = state.aiApiKey,
        baseUrl = state.aiBaseUrl,
        model = state.aiModel,
        targetLanguage = state.translateLanguage,
        mode = state.translateMode,
        deeplApiKey = state.deeplApiKey,
        deeplFormality = state.deeplFormality,
    ).takeIf { it.usable }
}

/** How a lyric line sits in the list at a given distance from the sung one. */
private data class LineAppearance(val alpha: Float, val blurDp: Float)

/**
 * The line-level alpha and blur, per style.
 *
 * The mobile renderer carries these inside each line composable (its alpha
 * falloff, its progressive blur); the desktop draws them on the line wrapper,
 * so the same numbers live here — one place instead of one per style.
 */
private fun lineAppearance(
    options: LyricsDisplayOptions,
    style: LyricsAnimationStyle,
    isActive: Boolean,
    isBackground: Boolean,
    distance: Int,
): LineAppearance {
    val appleBlur = options.appleMusicBlur && style == LyricsAnimationStyle.VIVIMUSIC_1
    return when (style) {
        // VIVI Music: soft falloff 0.75 → 0.20 and the progressive blur the
        // style exposes as "Apple Music blur".
        LyricsAnimationStyle.VIVIMUSIC_1 -> LineAppearance(
            alpha = when {
                isActive -> 1f
                distance == 1 -> 0.75f
                distance == 2 -> 0.50f
                distance == 3 -> 0.30f
                else -> 0.20f
            },
            blurDp = when {
                !appleBlur || isActive -> 0f
                distance <= 2 -> 0f
                distance == 3 -> 2f
                distance == 4 -> 4f
                else -> 6f
            },
        )

        // Metro: a foreground line sits at 0.30, the others fall away fast, and
        // a background line keeps 0.50.
        LyricsAnimationStyle.METRO_LYRICS -> LineAppearance(
            alpha = when {
                isBackground -> 0.5f
                isActive -> 1f
                distance == 0 -> 0.3f
                distance <= 2 -> 0.2f
                distance == 3 -> 0.15f
                distance == 4 -> 0.1f
                else -> 0.08f
            },
            blurDp = if (!isActive && options.standardBlur) 1.2f else 0f,
        )

        // Pre-port look: the list did not fade or blur anything.
        LyricsAnimationStyle.ALPHA -> LineAppearance(alpha = 1f, blurDp = 0f)

        else -> LineAppearance(
            alpha = when {
                isActive -> 1f
                appleBlur -> 0.35f
                options.standardBlur -> 0.45f
                else -> 0.65f - (distance.coerceAtMost(6) * 0.05f)
            },
            blurDp = when {
                isActive -> 0f
                appleBlur -> 2.4f
                options.standardBlur -> 1.2f
                else -> 0f
            },
        )
    }
}

@Composable
fun LyricsList(
    lines: List<LyricLine>,
    positionMs: Long,
    options: LyricsDisplayOptions,
    onSeek: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    val inactive = MaterialTheme.colorScheme.onSurfaceVariant
    val listState = rememberLazyListState()

    // The live position, read inside the item lambdas: each visible line then
    // recomposes on its own as the words advance.
    val positionState = rememberUpdatedState(positionMs)

    var currentIndex by remember(lines) { mutableStateOf(-1) }

    // Poll ~8x/s only to decide which line is "current" (that drives the
    // auto-scroll and the dimming), so the list itself is not recomposed at
    // the full position rate.
    LaunchedEffect(lines) {
        while (true) {
            val index = LyricsParser.currentLineIndex(lines, positionState.value)
            if (index != currentIndex) currentIndex = index
            delay(120)
        }
    }

    LaunchedEffect(currentIndex, options.autoScroll) {
        if (options.autoScroll && currentIndex >= 0) {
            listState.animateScrollToItem(maxOf(0, currentIndex - 3))
        }
    }

    // Romanization is a pure function of the text: run it once per lyrics set.
    val romanized = remember(lines, options.romanize) {
        options.romanize?.let { opts -> lines.map { LyricsRomanizer.romanize(it.text, opts) } }
    }
    val translated = options.translated

    val textAlign = when (options.position) {
        LyricsPosition.LEFT -> TextAlign.Start
        LyricsPosition.CENTER -> TextAlign.Center
        LyricsPosition.RIGHT -> TextAlign.End
    }
    val lineAlignment = when (options.position) {
        LyricsPosition.LEFT -> Alignment.CenterStart
        LyricsPosition.CENTER -> Alignment.Center
        LyricsPosition.RIGHT -> Alignment.CenterEnd
    }
    val columnAlignment = when (options.position) {
        LyricsPosition.LEFT -> Alignment.Start
        LyricsPosition.CENTER -> Alignment.CenterHorizontally
        LyricsPosition.RIGHT -> Alignment.End
    }

    LazyColumn(state = listState, modifier = modifier.fillMaxSize()) {
        itemsIndexed(lines) { index, line ->
            val isActive = index == currentIndex
            // Distance from the sung line: drives the blur / dimming of the
            // lines around it, like the mobile renderer.
            val distance = if (currentIndex < 0) 10 else kotlin.math.abs(index - currentIndex)
            val isBackground = line.isBackground

            val romanizedText = romanized?.getOrNull(index)
            val hasRomanized = options.romanize != null &&
                romanizedText != null && romanizedText != line.text
            val mainText = if (options.romanizeAsMain && hasRomanized) romanizedText!! else line.text
            val subText = when {
                options.romanizeAsMain && hasRomanized -> line.text
                hasRomanized -> romanizedText
                else -> translated?.getOrNull(index)
            }
            // A word animation needs the words of the text it is drawing: with
            // the romanized form promoted to the main line there are none.
            val words = if (options.romanizeAsMain && hasRomanized) null else line.words
            // GLOW-style line effects need to know how long the line lasts (the
            // next line's start, 4 s otherwise).
            val lineEndMs = lines.getOrNull(index + 1)?.timeMs ?: (line.timeMs + 4_000)

            val baseSize = options.textSizeSp * (if (isBackground) 0.85f else 1f)
            val lineStyle = TextStyle(
                color = if (isActive) accent else inactive,
                fontSize = (baseSize * if (isActive) 1.12f else 1f).sp,
                lineHeight = (baseSize * options.lineSpacing).sp,
            )
            val appearance = lineAppearance(options, options.style, isActive, isBackground, distance)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (options.clickToSeek) {
                            Modifier.clickable { onSeek(line.timeMs) }
                        } else {
                            Modifier
                        },
                    )
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                contentAlignment = lineAlignment,
            ) {
                Column(horizontalAlignment = columnAlignment) {
                    Box(
                        modifier = Modifier
                            .then(if (appearance.blurDp > 0f) Modifier.blur(appearance.blurDp.dp) else Modifier)
                            .graphicsLayer { alpha = appearance.alpha },
                    ) {
                        AnimatedLyricLine(
                            text = mainText,
                            words = words,
                            isActive = isActive,
                            isPast = !isActive && index < currentIndex,
                            isBackground = isBackground,
                            lineStartMs = line.timeMs,
                            lineEndMs = lineEndMs,
                            positionProvider = { positionState.value },
                            style = options.style,
                            glowEffect = options.glowEffect,
                            accent = accent,
                            inactive = inactive,
                            textStyle = lineStyle,
                            textAlign = textAlign,
                        )
                    }

                    if (!subText.isNullOrBlank() && subText != mainText) {
                        Text(
                            text = subText,
                            fontSize = (baseSize * 0.78f).sp,
                            lineHeight = (baseSize * 0.78f * options.lineSpacing).sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isActive) 0.9f else 0.55f),
                            fontWeight = FontWeight.Medium,
                            textAlign = textAlign,
                            modifier = Modifier.padding(top = 1.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * One lyric line, animated with the selected style.
 *
 * The styles that only recolour the words are drawn as a single `Text` with an
 * [androidx.compose.ui.text.AnnotatedString]; the ones the mobile renderer
 * animates word by word lay the words out as separate composables so each one
 * can move, scale and blur on its own.
 */
@Composable
private fun AnimatedLyricLine(
    text: String,
    words: List<WordTimestamp>?,
    isActive: Boolean,
    isPast: Boolean,
    isBackground: Boolean,
    lineStartMs: Long,
    lineEndMs: Long,
    positionProvider: () -> Long,
    style: LyricsAnimationStyle,
    glowEffect: Boolean,
    accent: Color,
    inactive: Color,
    textStyle: TextStyle,
    textAlign: TextAlign,
) {
    val wordTimings = words?.takeIf { it.isNotEmpty() }

    when (style) {
        LyricsAnimationStyle.ALPHA -> {
            PlainLyricLine(text, isActive, glowEffect, accent, inactive, textStyle, textAlign)
            return
        }

        LyricsAnimationStyle.VIVIMUSIC_1 -> {
            ViviWaveLine(
                text = text,
                words = wordTimings,
                isActive = isActive,
                position = lyricPosition(positionProvider(), lineStartMs),
                lineStartMs = lineStartMs,
                accent = accent,
                textStyle = textStyle,
                textAlign = textAlign,
            )
            return
        }

        LyricsAnimationStyle.LYRICS_V2 -> {
            LyricsV2Line(
                text = text,
                words = wordTimings,
                isActive = isActive,
                isPast = isPast,
                isBackground = isBackground,
                position = positionProvider(),
                accent = accent,
                inactive = inactive,
                textStyle = textStyle,
                textAlign = textAlign,
            )
            return
        }

        LyricsAnimationStyle.METRO_LYRICS -> {
            MetroWordLine(
                text = text,
                words = wordTimings,
                isActive = isActive,
                isPast = isPast,
                isBackground = isBackground,
                position = lyricPosition(positionProvider(), lineStartMs),
                lineStartMs = lineStartMs,
                accent = accent,
                inactive = inactive,
                textStyle = textStyle,
                textAlign = textAlign,
            )
            return
        }

        LyricsAnimationStyle.APPLE_V2 -> {
            AppleV2Line(
                text = text,
                words = wordTimings,
                isActive = isActive,
                isPast = isPast,
                lineStartMs = lineStartMs,
                lineEndMs = lineEndMs,
                position = lyricPosition(positionProvider(), lineStartMs),
                accent = accent,
                textStyle = textStyle,
                textAlign = textAlign,
            )
            return
        }

        else -> Unit
    }

    if (wordTimings != null) {
        // NONE / FADE / GLOW / SLIDE / KARAOKE / APPLE: per-word colours and
        // shadows inside one text run. The position is read here so this line —
        // and only this one — recomposes as the words advance.
        val position = lyricPosition(positionProvider(), lineStartMs)
        val annotated = remember(style, isActive, isPast, position, accent, inactive) {
            mobileWordSpans(
                words = wordTimings,
                position = position,
                isActive = isActive,
                isPast = isPast,
                style = style,
                accent = accent,
                inactive = inactive,
            )
        }
        Text(
            text = annotated,
            style = textStyle,
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }

    // No per-word timings: the line is highlighted as a whole.
    PlainLyricLine(
        text = text,
        isActive = isActive,
        glowEffect = glowEffect,
        accent = accent,
        inactive = inactive,
        textStyle = textStyle,
        textAlign = textAlign,
    )
}

/**
 * The pre-port look (and the fallback of every word style on a line without
 * word timings): one plain text, the sung line in the accent colour and bold.
 */
@Composable
private fun PlainLyricLine(
    text: String,
    isActive: Boolean,
    glowEffect: Boolean,
    accent: Color,
    inactive: Color,
    textStyle: TextStyle,
    textAlign: TextAlign,
) {
    val glow = if (isActive && glowEffect) {
        Shadow(color = accent.copy(alpha = 0.45f), offset = Offset.Zero, blurRadius = 14f)
    } else {
        null
    }
    Text(
        text = text,
        style = textStyle.copy(
            color = if (isActive) accent else inactive,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            shadow = glow,
        ),
        textAlign = textAlign,
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Per-word `AnnotatedString` for the text-based animation styles.
 *
 * Every branch is the mobile renderer's own recipe for that style, values
 * included (alphas, weights, halo radius): that is what makes the entries of
 * the picker actually look like their mobile counterpart instead of six
 * variations of the same fill.
 */
private fun mobileWordSpans(
    words: List<WordTimestamp>,
    position: Long,
    isActive: Boolean,
    isPast: Boolean,
    style: LyricsAnimationStyle,
    accent: Color,
    inactive: Color,
) = buildAnnotatedString {
    words.forEachIndexed { index, word ->
        val startMs = (word.startTime * 1000).toLong()
        val endMs = (word.endTime * 1000).toLong()
        val duration = endMs - startMs
        val isWordActive = isActive && position in startMs..endMs
        val linear = if (isWordActive && duration > 0) {
            ((position - startMs).toFloat() / duration).coerceIn(0f, 1f)
        } else {
            0f
        }
        val passed = when (style) {
            // NONE / FADE / GLOW advance on the current line only; SLIDE,
            // KARAOKE and APPLE also treat an already-sung line as completed.
            LyricsAnimationStyle.SLIDE, LyricsAnimationStyle.KARAOKE, LyricsAnimationStyle.APPLE ->
                (isActive && position >= endMs) || isPast
            else -> isActive && position > endMs
        }
        // Smoothstep is the mobile easing for this family; SLIDE and KARAOKE
        // each apply their own curve on top of it.
        val progress = when {
            passed -> 1f
            isWordActive -> smoothstep(linear)
            else -> 0f
        }
        val lineColor = if (isActive) accent else inactive

        val wordStyle = when (style) {
            // Mobile NONE: the plainest fill of the family.
            LyricsAnimationStyle.NONE -> SpanStyle(
                color = accent.copy(
                    alpha = when {
                        !isActive -> 0.7f
                        passed -> 1f
                        isWordActive -> 0.5f + 0.5f * progress
                        else -> 0.35f
                    },
                ),
                fontWeight = when {
                    !isActive || passed -> FontWeight.Bold
                    isWordActive -> FontWeight.ExtraBold
                    else -> FontWeight.Medium
                },
            )

            LyricsAnimationStyle.FADE -> SpanStyle(
                color = accent.copy(
                    alpha = when {
                        !isActive -> 0.55f
                        passed -> 1f
                        isWordActive -> 0.4f + 0.6f * progress
                        else -> 0.4f
                    },
                ),
                fontWeight = if (isWordActive) FontWeight.ExtraBold else FontWeight.Bold,
                shadow = when {
                    isWordActive && progress > 0.2f -> Shadow(
                        color = accent.copy(alpha = 0.35f * progress),
                        offset = Offset.Zero,
                        blurRadius = 10f * progress,
                    )
                    passed -> Shadow(accent.copy(alpha = 0.15f), Offset.Zero, 6f)
                    else -> null
                },
            )

            // Mobile GLOW: a bright fill whose halo grows with the square of
            // the progress — a word-level effect, not a sweep across the line.
            LyricsAnimationStyle.GLOW -> {
                val glowIntensity = progress * progress
                SpanStyle(
                    color = accent.copy(
                        alpha = when {
                            !isActive -> 0.5f
                            isWordActive || passed -> 0.45f + 0.55f * progress
                            else -> 0.35f
                        },
                    ),
                    fontWeight = when {
                        !isActive -> FontWeight.Bold
                        isWordActive -> FontWeight.ExtraBold
                        passed -> FontWeight.Bold
                        else -> FontWeight.Medium
                    },
                    shadow = when {
                        isWordActive && glowIntensity > 0.05f -> Shadow(
                            color = accent.copy(alpha = 0.5f + 0.3f * glowIntensity),
                            offset = Offset.Zero,
                            blurRadius = 16f + 12f * glowIntensity,
                        )
                        passed -> Shadow(accent.copy(alpha = 0.25f), Offset.Zero, 8f)
                        else -> null
                    },
                )
            }

            // Mobile SLIDE: a tight leading edge plus a halo that "breathes"
            // while the word is sung (a slow sine).
            LyricsAnimationStyle.SLIDE -> {
                val elapsed = if (isWordActive) position - startMs else 0L
                val breathe = if (isWordActive) {
                    ((elapsed % 3000) / 3000f * 2f * PI.toFloat()).let { sin(it) * 0.03f }
                        .coerceIn(0f, 0.03f)
                } else {
                    0f
                }
                val glowIntensity = (0.3f + progress * 0.7f + breathe).coerceIn(0f, 1.1f)
                when {
                    isWordActive && duration > 0 -> SpanStyle(
                        brush = Brush.horizontalGradient(
                            0f to accent,
                            (progress * 0.95f).coerceIn(0f, 1f) to accent,
                            progress.coerceIn(0f, 1f) to accent.copy(alpha = 0.9f),
                            (progress + 0.02f).coerceIn(0f, 1f) to accent.copy(alpha = 0.5f),
                            (progress + 0.08f).coerceIn(0f, 1f) to accent.copy(alpha = 0.35f),
                            1f to accent.copy(alpha = 0.35f),
                        ),
                        fontWeight = FontWeight.ExtraBold,
                        shadow = Shadow(
                            color = accent.copy(alpha = 0.4f * glowIntensity),
                            offset = Offset.Zero,
                            blurRadius = 14f + 4f * progress,
                        ),
                    )
                    passed && isActive -> SpanStyle(
                        color = accent,
                        fontWeight = FontWeight.Bold,
                        shadow = Shadow(accent.copy(alpha = 0.4f), Offset.Zero, 12f),
                    )
                    else -> SpanStyle(
                        color = if (!isActive) lineColor else accent.copy(alpha = 0.35f),
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            // Mobile KARAOKE: a softer, wider fill (seven stops) whose halo
            // builds with the square of the progress.
            LyricsAnimationStyle.KARAOKE -> {
                val glowIntensity = progress * progress
                when {
                    isWordActive && duration > 0 -> SpanStyle(
                        brush = Brush.horizontalGradient(
                            0f to accent.copy(alpha = 0.4f),
                            (progress * 0.6f).coerceIn(0f, 1f) to accent.copy(alpha = 0.75f),
                            (progress * 0.85f).coerceIn(0f, 1f) to accent.copy(alpha = 0.95f),
                            progress.coerceIn(0f, 1f) to accent,
                            (progress + 0.03f).coerceIn(0f, 1f) to accent.copy(alpha = 0.85f),
                            (progress + 0.1f).coerceIn(0f, 1f) to accent.copy(alpha = 0.5f),
                            1f to accent.copy(alpha = if (progress >= 0.9f) 0.95f else 0.4f),
                        ),
                        fontWeight = FontWeight.ExtraBold,
                        shadow = Shadow(
                            color = accent.copy(alpha = 0.5f + 0.3f * glowIntensity),
                            offset = Offset.Zero,
                            blurRadius = 16f + 12f * glowIntensity,
                        ),
                    )
                    passed && isActive -> SpanStyle(
                        color = accent,
                        fontWeight = FontWeight.Bold,
                        shadow = Shadow(accent.copy(alpha = 0.25f), Offset.Zero, 8f),
                    )
                    else -> SpanStyle(
                        color = if (!isActive) lineColor else accent.copy(alpha = 0.4f),
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            LyricsAnimationStyle.APPLE -> {
                val glowIntensity = progress * progress
                SpanStyle(
                    color = accent.copy(
                        alpha = when {
                            !isActive -> 0.55f
                            passed -> 1f
                            isWordActive -> 0.55f + 0.45f * progress
                            else -> 0.4f
                        },
                    ),
                    fontWeight = when {
                        !isActive -> FontWeight.SemiBold
                        passed -> FontWeight.Bold
                        isWordActive -> FontWeight.ExtraBold
                        else -> FontWeight.Normal
                    },
                    shadow = when {
                        isWordActive -> Shadow(
                            color = accent.copy(alpha = 0.2f + 0.4f * glowIntensity),
                            offset = Offset.Zero,
                            blurRadius = 10f + 12f * glowIntensity,
                        )
                        passed && isActive -> Shadow(accent.copy(alpha = 0.2f), Offset.Zero, 8f)
                        else -> null
                    },
                )
            }

            // Drawn by their own renderer; these are placeholders.
            LyricsAnimationStyle.APPLE_V2, LyricsAnimationStyle.LYRICS_V2,
            LyricsAnimationStyle.VIVIMUSIC_1, LyricsAnimationStyle.METRO_LYRICS,
            LyricsAnimationStyle.ALPHA,
            -> SpanStyle(color = accent.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
        }

        withStyle(wordStyle) { append(word.text) }
        if (index < words.lastIndex) append(" ")
    }
}

/** The horizontal FlowRow arrangement matching a [TextAlign]. */
private fun flowArrangement(textAlign: TextAlign): Arrangement.Horizontal = when (textAlign) {
    TextAlign.Start -> Arrangement.Start
    TextAlign.End -> Arrangement.End
    else -> Arrangement.Center
}

/**
 * The gap between two wrapped word rows: mobile caps the line spacing at 1.3
 * here, so a tall spacing does not push the wrapped rows apart.
 */
@Composable
private fun wrapSpacing(textStyle: TextStyle) = with(LocalDensity.current) {
    val ratio = (textStyle.lineHeight.value / textStyle.fontSize.value)
        .takeIf { it.isFinite() && it > 0f } ?: 1.35f
    (textStyle.fontSize.value * (ratio.coerceAtMost(1.3f) - 1f)).sp.toDp()
}

/**
 * Mobile APPLE_V2: the line is revealed character by character, each word laid
 * out on its own; without word timings the mobile renderer splits the line's
 * own duration over its characters by count.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppleV2Line(
    text: String,
    words: List<WordTimestamp>?,
    isActive: Boolean,
    isPast: Boolean,
    lineStartMs: Long,
    lineEndMs: Long,
    position: Long,
    accent: Color,
    textStyle: TextStyle,
    textAlign: TextAlign,
) {
    val activeDuration = (((lineEndMs - lineStartMs).takeIf { it > 0 } ?: 4_000L) * 0.95)
        .toLong().coerceAtLeast(300L)

    // (word text, start, end) relative to the line start.
    val wordData = remember(text, words, activeDuration) {
        if (!words.isNullOrEmpty()) {
            words.map { word ->
                val start = ((word.startTime * 1000).toLong() - lineStartMs).coerceAtLeast(0L)
                val end = ((word.endTime * 1000).toLong() - lineStartMs).coerceAtLeast(start + 50L)
                Triple(word.text, start, end)
            }
        } else {
            val parts = text.split(" ").filter { it.isNotEmpty() }
            if (parts.isEmpty()) {
                listOf(Triple(text, 0L, activeDuration))
            } else {
                val totalChars = text.length
                var accumulated = 0L
                parts.mapIndexed { index, part ->
                    val chars = if (index < parts.lastIndex) part.length + 1 else part.length
                    val duration = if (totalChars > 0) {
                        (activeDuration * chars.toFloat() / totalChars).toLong()
                    } else {
                        activeDuration
                    }
                    val start = accumulated
                    accumulated += duration
                    Triple(part, start, start + duration)
                }
            }
        }
    }

    val lineRel = (position - lineStartMs).coerceAtLeast(0L)

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = flowArrangement(textAlign),
        verticalArrangement = Arrangement.spacedBy(wrapSpacing(textStyle)),
    ) {
        wordData.forEachIndexed { wordIndex, (wordText, startRelative, endRelative) ->
            val wordDuration = endRelative - startRelative
            Row {
                wordText.forEachIndexed { charIndex, char ->
                    val charDuration = if (wordText.isNotEmpty()) wordDuration / wordText.length else 0L
                    val charStart = startRelative + (charIndex * charDuration)
                    val charEnd = charStart + charDuration
                    val charProgress = when {
                        !isActive -> 0f
                        lineRel >= charEnd -> 1f
                        lineRel < charStart -> 0f
                        charDuration <= 0L -> 1f
                        else -> ((lineRel - charStart).toFloat() / charDuration).coerceIn(0f, 1f)
                    }
                    Text(
                        text = char.toString(),
                        fontSize = textStyle.fontSize,
                        lineHeight = textStyle.lineHeight,
                        color = accent.copy(
                            alpha = when {
                                !isActive -> 1f
                                charProgress >= 1f -> 1f
                                else -> 0.3f + 0.7f * charProgress
                            },
                        ),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                    )
                }
                if (wordIndex < wordData.lastIndex) {
                    Text(
                        text = " ",
                        fontSize = textStyle.fontSize,
                        lineHeight = textStyle.lineHeight,
                        letterSpacing = (-0.5).sp,
                    )
                }
            }
        }
    }
}

/**
 * Mobile LYRICS_V2: each word floats and bounces (a sine over its own progress)
 * while its bright copy is revealed behind an edge that travels across the word.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LyricsV2Line(
    text: String,
    words: List<WordTimestamp>?,
    isActive: Boolean,
    isPast: Boolean,
    isBackground: Boolean,
    position: Long,
    accent: Color,
    inactive: Color,
    textStyle: TextStyle,
    textAlign: TextAlign,
) {
    val inactiveAlpha = 0.35f
    if (words.isNullOrEmpty()) {
        // Mobile falls back to a plain line (italic for a background line).
        Text(
            text = text,
            style = textStyle.copy(
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                fontStyle = if (isBackground) FontStyle.Italic else FontStyle.Normal,
            ),
            color = accent.copy(alpha = if (isActive) 1f else inactiveAlpha),
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = flowArrangement(textAlign),
        verticalArrangement = Arrangement.spacedBy(wrapSpacing(textStyle)),
    ) {
        words.forEachIndexed { index, word ->
            LyricsV2Word(
                word = word,
                isLineActive = isActive,
                isLinePast = isPast,
                position = position,
                accent = accent,
                inactiveAlpha = inactiveAlpha,
                isBackground = isBackground,
                textStyle = textStyle,
            )
            if (index < words.lastIndex) {
                Text(
                    text = " ",
                    fontSize = textStyle.fontSize,
                    lineHeight = textStyle.lineHeight,
                )
            }
        }
    }
}

@Composable
private fun LyricsV2Word(
    word: WordTimestamp,
    isLineActive: Boolean,
    isLinePast: Boolean,
    position: Long,
    accent: Color,
    inactiveAlpha: Float,
    isBackground: Boolean,
    textStyle: TextStyle,
) {
    val startMs = (word.startTime * 1000).toLong()
    val endMs = (word.endTime * 1000).toLong()
    val duration = (endMs - startMs).coerceAtLeast(1L)
    val isWordComplete = isLinePast || position >= endMs
    val isWordActive = isLineActive && position in startMs until endMs
    val progress = when {
        isWordComplete -> 1f
        !isLineActive || position <= startMs -> 0f
        else -> ((position - startMs).toFloat() / duration).coerceIn(0f, 1f)
    }

    val sinProgress = sin(progress * PI).toFloat()
    val wordScale = 1f + (0.015f * sinProgress)
    // The float is animated on mobile (50 ms in, 350 ms back); the desktop draws
    // it straight from the position, which updates every frame anyway.
    val floatOffset = if (isWordActive) -4f * sinProgress else 0f
    val glowProgress = (progress * 2f).coerceAtMost(1f)
    val glowAlpha = if (isWordActive) glowProgress * 0.45f else 0f
    val glowRadius = if (isWordActive) glowProgress * 12f else 0f

    Box(
        modifier = Modifier.graphicsLayer {
            translationY = floatOffset * density
            scaleX = wordScale
            scaleY = wordScale
        },
    ) {
        Text(
            text = word.text,
            style = textStyle.copy(
                fontWeight = if (isLineActive) FontWeight.Bold else FontWeight.SemiBold,
                fontStyle = if (isBackground) FontStyle.Italic else FontStyle.Normal,
            ),
            color = accent.copy(alpha = if (isBackground) inactiveAlpha * 0.7f else inactiveAlpha),
        )
        if (isWordComplete || isWordActive) {
            Text(
                text = word.text,
                style = textStyle.copy(
                    fontWeight = if (isLineActive) FontWeight.Bold else FontWeight.SemiBold,
                    fontStyle = if (isBackground) FontStyle.Italic else FontStyle.Normal,
                    shadow = if (glowAlpha > 0f) {
                        Shadow(
                            color = accent.copy(alpha = glowAlpha),
                            offset = Offset.Zero,
                            blurRadius = glowRadius.coerceAtLeast(1f),
                        )
                    } else {
                        null
                    },
                ),
                color = accent.copy(alpha = if (isBackground) 0.75f else 1f),
                modifier = if (isWordActive) {
                    Modifier
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            val edge = 8.dp.toPx()
                            val center = (size.width + edge * 2f) * progress - edge
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color.Black, Color.Transparent),
                                    startX = center - edge,
                                    endX = center + edge,
                                ),
                                blendMode = BlendMode.DstIn,
                            )
                        }
                } else {
                    Modifier
                },
            )
        }
    }
}

/**
 * Mobile VIVI Music: one global wave sweeps the whole sentence, each word just
 * reads where the wave front sits inside its own bounds, and the halo grows
 * with that local progress. Without word timings the mobile style highlights
 * the sentence as a whole.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ViviWaveLine(
    text: String,
    words: List<WordTimestamp>?,
    isActive: Boolean,
    position: Long,
    lineStartMs: Long,
    accent: Color,
    textStyle: TextStyle,
    textAlign: TextAlign,
) {
    val scale = if (isActive) 1.05f else 1f
    if (words.isNullOrEmpty()) {
        val factor = if (isActive) 1f else 0f
        Text(
            text = text,
            style = textStyle.copy(
                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold,
                shadow = if (isActive) {
                    Shadow(accent.copy(alpha = 0.5f), Offset.Zero, 10f)
                } else {
                    null
                },
            ),
            color = accent.copy(alpha = 0.45f + 0.55f * factor),
            textAlign = textAlign,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { scaleX = scale; scaleY = scale },
        )
        return
    }

    // Wave span: from the line start to the end of its last word.
    val globalEnd = ((words.last().endTime * 1000).toLong() - lineStartMs).coerceAtLeast(1L)
    val lineRel = (position - lineStartMs).coerceAtLeast(0L)
    val wave = (lineRel.toFloat() / globalEnd.toFloat()).coerceIn(0f, 1f)
    val feather = 0.12f

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale },
        horizontalArrangement = flowArrangement(textAlign),
        verticalArrangement = Arrangement.spacedBy(wrapSpacing(textStyle)),
    ) {
        words.forEachIndexed { index, word ->
            val startRelative = ((word.startTime * 1000).toLong() - lineStartMs).coerceAtLeast(0L)
            val endRelative = ((word.endTime * 1000).toLong() - lineStartMs).coerceAtLeast(startRelative + 1L)
            val startFrac = startRelative.toFloat() / globalEnd
            val endFrac = endRelative.toFloat() / globalEnd
            val span = (endFrac - startFrac).coerceAtLeast(0.001f)
            val localProgress = ((wave - startFrac) / span).coerceIn(0f, 1f)

            val glowAlpha = 0.6f * localProgress
            val glowRadius = (12f * localProgress).coerceAtLeast(0.1f)
            val tail = (localProgress + feather).coerceAtMost(1f)
            val brush = when {
                localProgress <= 0f -> Brush.horizontalGradient(
                    listOf(accent.copy(alpha = 0.45f), accent.copy(alpha = 0.45f)),
                )
                localProgress >= 1f -> Brush.horizontalGradient(listOf(accent, accent))
                else -> Brush.horizontalGradient(
                    0f to accent,
                    localProgress to accent,
                    tail to accent.copy(alpha = 0.45f),
                    1f to accent.copy(alpha = 0.45f),
                )
            }

            Text(
                text = word.text,
                style = textStyle.copy(
                    brush = brush,
                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold,
                    shadow = Shadow(
                        color = accent.copy(alpha = glowAlpha),
                        offset = Offset.Zero,
                        blurRadius = glowRadius,
                    ),
                ),
            )

            if (index < words.lastIndex) {
                Text(
                    text = " ",
                    fontSize = textStyle.fontSize,
                    lineHeight = textStyle.lineHeight,
                    color = accent.copy(alpha = (0.45f + 0.55f * localProgress).coerceIn(0.45f, 1f)),
                )
            }
        }
    }
}

/**
 * Mobile MetroLyrics: a flat, bold fill per word with a halo while the word is
 * being sung and a short "wobble" when it lands (0–750 ms).
 *
 * ponytail: the mobile original runs per grapheme (hyphen crescendo, per-char
 * nudge and line push) on an Android canvas; here the same look is drawn per
 * word, which is what the style reads as at lyric size. Add the grapheme pass
 * only if someone asks for it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MetroWordLine(
    text: String,
    words: List<WordTimestamp>?,
    isActive: Boolean,
    isPast: Boolean,
    isBackground: Boolean,
    position: Long,
    lineStartMs: Long,
    accent: Color,
    inactive: Color,
    textStyle: TextStyle,
    textAlign: TextAlign,
) {
    // Metro is the style that animates a line with no word timings: it estimates
    // them (180 ms per word, 30 ms apart), like the mobile renderer.
    val effectiveWords = words ?: estimateMetroWords(text, lineStartMs)
    if (effectiveWords.isEmpty()) {
        Text(
            text = text,
            style = textStyle.copy(
                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold,
                fontStyle = if (isBackground) FontStyle.Italic else FontStyle.Normal,
            ),
            color = if (isActive) accent else inactive,
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = flowArrangement(textAlign),
        verticalArrangement = Arrangement.spacedBy(wrapSpacing(textStyle)),
    ) {
        effectiveWords.forEachIndexed { index, word ->
            val startMs = (word.startTime * 1000).toLong()
            val endMs = (word.endTime * 1000).toLong()
            val duration = (endMs - startMs).coerceAtLeast(1L)
            val sung = isPast || position > endMs
            val wordActive = isActive && position in startMs..endMs
            val fill = when {
                sung -> 1f
                wordActive -> ((position - startMs).toFloat() / duration).coerceIn(0f, 1f)
                else -> 0f
            }
            // Landing wobble: a 125 ms rise and a 625 ms fall after the word starts.
            val sinceStart = (position - startMs).toFloat()
            val wobble = if (sinceStart in 0f..750f) {
                if (sinceStart < 125f) sinceStart / 125f else (1f - (sinceStart - 125f) / 625f).coerceAtLeast(0f)
            } else {
                0f
            }
            val glowFade = if (wordActive) {
                (fill * 5f).coerceIn(0f, 1f) * ((1f - fill) * 8f).coerceIn(0f, 1f)
            } else {
                0f
            }
            val baseAlpha = 0.3f + (1f - 0.3f) * fill

            Box(
                modifier = Modifier.graphicsLayer {
                    scaleX = 1f + wobble * 0.025f
                    scaleY = 1f + wobble * 0.015f
                },
            ) {
                Text(
                    text = word.text,
                    style = textStyle.copy(
                        fontWeight = FontWeight.Bold,
                        fontStyle = if (isBackground) FontStyle.Italic else FontStyle.Normal,
                        letterSpacing = (-0.5).sp,
                    ),
                    color = accent.copy(alpha = 0.3f),
                )
                if (sung || wordActive) {
                    Text(
                        text = word.text,
                        style = textStyle.copy(
                            fontWeight = FontWeight.Bold,
                            fontStyle = if (isBackground) FontStyle.Italic else FontStyle.Normal,
                            letterSpacing = (-0.5).sp,
                            shadow = if (glowFade > 0.01f) {
                                Shadow(
                                    color = accent.copy(alpha = (0.35f * glowFade).coerceAtMost(0.4f)),
                                    offset = Offset.Zero,
                                    blurRadius = 12f * glowFade,
                                )
                            } else {
                                null
                            },
                        ),
                        color = accent.copy(alpha = if (sung) 1f else baseAlpha),
                        modifier = if (wordActive && !sung) {
                            Modifier
                                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                .drawWithContent {
                                    drawContent()
                                    val edge = (size.width * 0.45f).coerceAtLeast(1f)
                                    val front = size.width * fill
                                    drawRect(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(Color.Black, Color.Transparent),
                                            startX = front - edge,
                                            endX = front,
                                        ),
                                        blendMode = BlendMode.DstIn,
                                    )
                                }
                        } else {
                            Modifier
                        },
                    )
                }
            }
            if (index < effectiveWords.lastIndex) {
                Text(
                    text = " ",
                    fontSize = textStyle.fontSize,
                    lineHeight = textStyle.lineHeight,
                )
            }
        }
    }
}

/**
 * Mobile's METRO_LYRICS estimates word timings when the source only carries
 * line timings: 180 ms per word, each starting 30 ms after the previous one.
 * It is the only style that animates un-synced lines, so the estimation is
 * what the picker promises.
 */
private fun estimateMetroWords(text: String, lineStartMs: Long): List<WordTimestamp> {
    val parts = text.split(Regex("\\s+")).filter { it.isNotBlank() }
    if (parts.isEmpty()) return emptyList()
    val start = lineStartMs / 1000.0
    return parts.mapIndexed { index, word ->
        val from = start + index * 0.03
        WordTimestamp(text = word, startTime = from, endTime = from + 0.18)
    }
}
