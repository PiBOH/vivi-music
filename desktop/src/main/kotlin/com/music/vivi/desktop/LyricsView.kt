package com.music.vivi.desktop

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
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
 * The desktop used to draw a plain list of lines with the active one in bold.
 * This adds what the APK's advanced lyrics have: the word-by-word animation
 * styles (which need the per-word timings the shared [LyricsParser] now keeps),
 * the glow/blur options, click-to-seek, text alignment, and the optional
 * romanized / translated subtitle under each line.
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
 * How much faster/slower than real time a word animation settles, from the
 * global "Animation speed" preference (Appearance → Animation speed).
 *
 * The lyrics animation is driven by the playback clock, so without this the
 * setting changed every other transition in the app but left the lyrics
 * filling at exactly the same rate (the reported "the animation ignores the
 * speed I set"). "Fast" reaches full colour before the word ends, "Slow"
 * lags behind it; "Normal" is 1:1 with the word's own duration.
 */
private val LYRICS_SPEED: Float
    get() = when (Animations.speed) {
        "fast" -> 1.6f
        "slow" -> 0.62f
        else -> 1f
    }

/** Smoothstep easing of a word's fill, scaled by the animation-speed setting. */
private fun lyricProgress(linear: Float): Float {
    val t = (linear * LYRICS_SPEED).coerceIn(0f, 1f)
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
            val lineWords = if (options.romanizeAsMain && hasRomanized) null else line.words
            // METRO_LYRICS animates even without word timings: the mobile style
            // estimates them (180 ms per word, 30 ms apart), which is what makes
            // that entry useful on plain LRC files.
            val words = lineWords ?: if (options.style == LyricsAnimationStyle.METRO_LYRICS) {
                estimateMetroWords(mainText, line.timeMs)
            } else {
                null
            }
            // GLOW sweeps a line over its own duration: the renderer has to know
            // how long the line lasts (the next line's start, 4 s otherwise).
            val lineDurationMs = (
                (lines.getOrNull(index + 1)?.timeMs ?: (line.timeMs + 4_000)) - line.timeMs
                ).coerceAtLeast(300)

            val baseSize = options.textSizeSp * (if (isBackground) 0.85f else 1f)
            val lineStyle = TextStyle(
                color = if (isActive) accent else inactive,
                fontSize = (baseSize * if (isActive) 1.12f else 1f).sp,
                lineHeight = (baseSize * options.lineSpacing).sp,
            )

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
                    // Mobile shows the Apple Music blur only for the VIVI Music
                    // style, and the option is hidden in the settings for every
                    // other one — mirroring that here keeps a leftover value
                    // from blurring a style the user cannot configure it for.
                    val appleBlur = options.appleMusicBlur &&
                        options.style == LyricsAnimationStyle.VIVIMUSIC_1
                    val blurred = when {
                        !isActive && appleBlur -> true
                        !isActive && options.standardBlur -> true
                        else -> false
                    }
                    val blurRadius = when {
                        !blurred -> 0f
                        appleBlur -> 2.4f
                        else -> 1.2f
                    }
                    val dimmed = when {
                        // METRO_LYRICS fades the whole list by distance from the
                        // sung line (20 / 15 / 10 / 8 %), the look of the mobile
                        // style; the active line stays fully opaque.
                        options.style == LyricsAnimationStyle.METRO_LYRICS && !isActive -> when (distance) {
                            1, 2 -> 0.2f
                            3 -> 0.15f
                            4 -> 0.1f
                            else -> 0.08f
                        }
                        isActive -> 1f
                        appleBlur -> 0.35f
                        options.standardBlur -> 0.45f
                        else -> 0.65f - (distance.coerceAtMost(6) * 0.05f)
                    }

                    Box(
                        modifier = Modifier
                            .then(if (blurRadius > 0f) Modifier.blur(blurRadius.dp) else Modifier)
                            .graphicsLayer { alpha = dimmed },
                    ) {
                        AnimatedLyricLine(
                            text = mainText,
                            words = words,
                            isActive = isActive,
                            lineDurationMs = lineDurationMs,
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

/**
 * One lyric line, animated with the selected style.
 *
 * Styles that only need per-word colours/weights/shadows are drawn as a single
 * `Text` with an [androidx.compose.ui.text.AnnotatedString]; the two that move
 * each word on its own (APPLE_V2, VIVIMUSIC_1) lay the words out as separate
 * composables so they can scale and blur individually.
 */
@Composable
private fun AnimatedLyricLine(
    text: String,
    words: List<WordTimestamp>?,
    isActive: Boolean,
    lineDurationMs: Long,
    positionProvider: () -> Long,
    style: LyricsAnimationStyle,
    glowEffect: Boolean,
    accent: Color,
    inactive: Color,
    textStyle: TextStyle,
    textAlign: TextAlign,
) {
    val wordTimings = words?.takeIf { it.isNotEmpty() }

    // GLOW is a *line* effect in the mobile renderer: a light travels across
    // the whole line while a halo breathes around it. Drawn per word it only
    // differed from FADE by a slightly larger shadow, which is exactly the
    // "the styles all look the same" report.
    if (style == LyricsAnimationStyle.GLOW) {
        GlowSweepLine(
            text = text,
            isActive = isActive,
            lineDurationMs = lineDurationMs,
            glowEffect = glowEffect,
            accent = accent,
            inactive = inactive,
            textStyle = textStyle,
            textAlign = textAlign,
        )
        return
    }

    // The two styles that move *each word* on its own are laid out as separate
    // composables (they scale, float and blur individually); APPLE_V2 left this
    // layout for the character-by-character reveal of the mobile style.
    val useWordLayout = style == LyricsAnimationStyle.VIVIMUSIC_1 ||
        style == LyricsAnimationStyle.LYRICS_V2

    if (wordTimings != null && useWordLayout) {
        WordFlowLine(
            words = wordTimings,
            positionProvider = positionProvider,
            isActive = isActive,
            style = style,
            glowEffect = glowEffect,
            accent = accent,
            inactive = inactive,
            textStyle = textStyle,
            textAlign = textAlign,
        )
        return
    }

    if (wordTimings != null) {
        // The style only changes colours inside the line, but the position has
        // to be read here so this line recomposes (and only this one) as the
        // words advance.
        val position = positionProvider()
        val annotated = remember(style, isActive, position, accent, glowEffect) {
            buildWordSpans(wordTimings, position, isActive, style, glowEffect, accent, inactive)
        }
        Text(
            text = annotated,
            style = textStyle,
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }

    // No per-word timings: a line-level highlight (what the old renderer did,
    // plus the active line's glow when the option is on).
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
 * Each branch is the mobile renderer's own recipe for that style, which is what
 * makes the entries in the picker actually look different from one another:
 * SLIDE draws a tight leading edge with a breathing halo, KARAOKE a wider and
 * softer fill with a stronger glow, APPLE_V2 reveals the line character by
 * character, METRO fills it flat and bold (its look is completed by the
 * per-line distance fade in [LyricsList]).
 */
private fun buildWordSpans(
    words: List<WordTimestamp>,
    position: Long,
    isActive: Boolean,
    style: LyricsAnimationStyle,
    glowEffect: Boolean,
    accent: Color,
    inactive: Color,
) = buildAnnotatedString {
    words.forEachIndexed { index, word ->
        val startMs = (word.startTime * 1000).toLong()
        val endMs = (word.endTime * 1000).toLong()
        val duration = (endMs - startMs).coerceAtLeast(1L)
        val hasPassed = isActive && position > endMs
        val isWordActive = isActive && position in startMs..endMs
        val linear = if (isWordActive) ((position - startMs).toFloat() / duration).coerceIn(0f, 1f) else 0f
        // Smoothstep easing, shared by every style (as on mobile) and scaled by
        // the "Animation speed" preference (see [LYRICS_SPEED]).
        val progress = when {
            hasPassed -> 1f
            isWordActive -> lyricProgress(linear)
            else -> 0f
        }

        // APPLE_V2 is the mobile style that reveals a line *character by
        // character* inside every word (the word's own duration is split over
        // its characters). Same colours as APPLE, different granularity — the
        // two used to be the same effect at word level here.
        if (style == LyricsAnimationStyle.APPLE_V2) {
            val chars = word.text
            val perChar = duration.toDouble() / chars.length.coerceAtLeast(1)
            // The reveal is split over the characters, but the playhead that
            // walks them is still stretched by the "Animation speed" setting:
            // without this the character style was the one entry in the picker
            // that ignored the option (it stepped at 1:1 with the word).
            val elapsed = ((position - startMs) * LYRICS_SPEED).toLong()
            chars.forEachIndexed { charIndex, char ->
                val charStart = (perChar * charIndex).toLong()
                val charEnd = charStart + perChar.toLong().coerceAtLeast(1L)
                val charProgress = when {
                    !isActive -> 1f
                    elapsed >= charEnd -> 1f
                    elapsed < charStart -> 0f
                    else -> ((elapsed - charStart).toFloat() /
                        (charEnd - charStart).toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                }
                withStyle(
                    SpanStyle(
                        color = accent.copy(alpha = 0.3f + 0.7f * charProgress),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                    ),
                ) { append(char) }
            }
            if (index < words.lastIndex) append(" ")
            return@forEachIndexed
        }

        val wordStyle = when (style) {
            LyricsAnimationStyle.NONE -> SpanStyle(
                color = accent.copy(
                    alpha = when {
                        !isActive -> 0.7f
                        hasPassed -> 1f
                        isWordActive -> 0.5f + 0.5f * progress
                        else -> 0.35f
                    },
                ),
                fontWeight = when {
                    !isActive || hasPassed -> FontWeight.Bold
                    isWordActive -> FontWeight.ExtraBold
                    else -> FontWeight.Medium
                },
            )

            LyricsAnimationStyle.FADE -> SpanStyle(
                color = accent.copy(
                    alpha = when {
                        !isActive -> 0.55f
                        hasPassed -> 1f
                        isWordActive -> 0.4f + 0.6f * progress
                        else -> 0.4f
                    },
                ),
                fontWeight = if (isWordActive) FontWeight.ExtraBold else FontWeight.Bold,
                shadow = when {
                    !glowEffect -> null
                    isWordActive && progress > 0.2f -> Shadow(
                        color = accent.copy(alpha = 0.35f * progress),
                        offset = Offset.Zero,
                        blurRadius = 10f * progress,
                    )
                    hasPassed -> Shadow(accent.copy(alpha = 0.15f), Offset.Zero, 6f)
                    else -> null
                },
            )

            // Only a fallback: GLOW is drawn by [GlowSweepLine] above.
            LyricsAnimationStyle.GLOW -> SpanStyle(
                color = if (hasPassed) accent else accent.copy(alpha = 0.4f),
                fontWeight = FontWeight.Bold,
            )

            // Mobile SLIDE: the fill has a tight leading edge, the halo
            // "breathes" while the word is sung (a slow sine), and completed
            // words keep a glow behind them.
            LyricsAnimationStyle.SLIDE -> {
                val elapsed = if (isWordActive) position - startMs else 0L
                val breathe = if (isWordActive) {
                    (sin(elapsed / 3000.0 * 2.0 * PI) * 0.03).coerceIn(0.0, 0.03).toFloat()
                } else {
                    0f
                }
                val glowIntensity = (0.3f + progress * 0.7f + breathe).coerceIn(0f, 1.1f)
                when {
                    isWordActive -> SpanStyle(
                        brush = Brush.horizontalGradient(
                            0f to accent,
                            (progress * 0.95f).coerceIn(0f, 1f) to accent,
                            progress.coerceIn(0f, 1f) to accent.copy(alpha = 0.9f),
                            (progress + 0.02f).coerceIn(0f, 1f) to accent.copy(alpha = 0.5f),
                            (progress + 0.08f).coerceIn(0f, 1f) to accent.copy(alpha = 0.35f),
                            1f to accent.copy(alpha = 0.35f),
                        ),
                        fontWeight = FontWeight.ExtraBold,
                        shadow = if (glowEffect) {
                            Shadow(
                                color = accent.copy(alpha = 0.4f * glowIntensity),
                                offset = Offset.Zero,
                                blurRadius = 14f + 4f * progress,
                            )
                        } else {
                            null
                        },
                    )
                    hasPassed -> SpanStyle(
                        color = accent,
                        fontWeight = FontWeight.Bold,
                        shadow = if (glowEffect) Shadow(accent.copy(alpha = 0.4f), Offset.Zero, 12f) else null,
                    )
                    else -> SpanStyle(
                        color = if (!isActive) inactive else accent.copy(alpha = 0.35f),
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            // Mobile KARAOKE: a softer, wider fill (seven stops) whose glow
            // builds with the square of the progress, plus a light halo on the
            // words already sung.
            LyricsAnimationStyle.KARAOKE -> {
                val glowIntensity = progress * progress
                when {
                    isWordActive -> SpanStyle(
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
                        shadow = if (glowEffect) {
                            Shadow(
                                color = accent.copy(alpha = 0.5f + 0.3f * glowIntensity),
                                offset = Offset.Zero,
                                blurRadius = 16f + 12f * glowIntensity,
                            )
                        } else {
                            null
                        },
                    )
                    hasPassed -> SpanStyle(
                        color = accent,
                        fontWeight = FontWeight.Bold,
                        shadow = if (glowEffect) Shadow(accent.copy(alpha = 0.25f), Offset.Zero, 8f) else null,
                    )
                    else -> SpanStyle(
                        color = if (!isActive) inactive else accent.copy(alpha = 0.4f),
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            LyricsAnimationStyle.APPLE -> SpanStyle(
                color = accent.copy(
                    alpha = when {
                        !isActive -> 0.55f
                        hasPassed -> 1f
                        isWordActive -> 0.55f + 0.45f * progress
                        else -> 0.4f
                    },
                ),
                fontWeight = when {
                    !isActive -> FontWeight.SemiBold
                    hasPassed -> FontWeight.Bold
                    isWordActive -> FontWeight.ExtraBold
                    else -> FontWeight.Normal
                },
                shadow = when {
                    !glowEffect -> null
                    isWordActive -> Shadow(
                        color = accent.copy(alpha = 0.2f + 0.4f * progress * progress),
                        offset = Offset.Zero,
                        blurRadius = 10f + 12f * progress * progress,
                    )
                    hasPassed -> Shadow(accent.copy(alpha = 0.2f), Offset.Zero, 8f)
                    else -> null
                },
            )

            // METRO_LYRICS is flat on purpose: the mobile style animates a
            // canvas with per-character timings, and what identifies it on a
            // desktop is the flat, bold karaoke fill without gradient or halo
            // (the per-line distance fade lives in [LyricsList]).
            LyricsAnimationStyle.METRO_LYRICS -> SpanStyle(
                color = accent.copy(alpha = if (!isActive) 0.55f else 0.3f + 0.7f * progress),
                fontWeight = when {
                    isWordActive -> FontWeight.ExtraBold
                    hasPassed -> FontWeight.Bold
                    else -> FontWeight.Medium
                },
            )

            // Drawn by [WordFlowLine]; this is only a placeholder colour.
            LyricsAnimationStyle.LYRICS_V2, LyricsAnimationStyle.VIVIMUSIC_1 ->
                SpanStyle(color = accent.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
        }

        withStyle(wordStyle) { append(word.text) }
        if (index < words.lastIndex) append(" ")
    }
}

/**
 * Mobile's GLOW style: a light travels across the whole line over its own
 * duration and a halo breathes around it for as long as the line is sung.
 *
 * This is the one style that is a *line* effect rather than a per-word colour,
 * which is what separates it from FADE in the picker.
 */
@Composable
private fun GlowSweepLine(
    text: String,
    isActive: Boolean,
    lineDurationMs: Long,
    glowEffect: Boolean,
    accent: Color,
    inactive: Color,
    textStyle: TextStyle,
    textAlign: TextAlign,
) {
    if (!isActive) {
        Text(
            text = text,
            style = textStyle.copy(color = inactive, fontWeight = FontWeight.Medium),
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }
    // The sweep runs over the line's own duration, shortened/lengthened by the
    // "Animation speed" option like every other style.
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(text, lineDurationMs) {
        sweep.snapTo(0f)
        sweep.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = (lineDurationMs / LYRICS_SPEED).toInt().coerceIn(250, 15_000),
                easing = LinearEasing,
            ),
        )
    }
    val pulse by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Reverse),
    )
    val head = sweep.value
    Text(
        text = text,
        style = textStyle.copy(
            brush = Brush.horizontalGradient(
                (head * 0.9f).coerceIn(0f, 1f) to accent,
                head.coerceIn(0f, 1f) to accent,
                (head + 0.05f).coerceIn(0f, 1f) to accent.copy(alpha = 0.7f),
                (head + 0.15f).coerceIn(0f, 1f) to accent.copy(alpha = 0.45f),
                1f to accent.copy(alpha = 0.3f),
            ),
            fontWeight = FontWeight.ExtraBold,
            shadow = if (glowEffect) {
                Shadow(
                    color = accent.copy(alpha = 0.3f + 0.25f * pulse),
                    offset = Offset.Zero,
                    blurRadius = 16f + 10f * pulse,
                )
            } else {
                null
            },
        ),
        textAlign = textAlign,
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Word-per-composable layout for APPLE_V2 and VIVIMUSIC_1: each word can then
 * scale, glow and blur on its own instead of sharing one text run.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordFlowLine(
    words: List<WordTimestamp>,
    positionProvider: () -> Long,
    isActive: Boolean,
    style: LyricsAnimationStyle,
    glowEffect: Boolean,
    accent: Color,
    inactive: Color,
    textStyle: TextStyle,
    textAlign: TextAlign,
) {
    val position = positionProvider()

    // The words are separate composables, so the line alignment cannot come
    // from `textAlign` alone: without this the default style (VIVIMUSIC_1)
    // always drew its words flush left and the "Text position" option looked
    // broken on every style that uses this layout.
    val horizontalArrangement = when (textAlign) {
        TextAlign.Start -> Arrangement.Start
        TextAlign.End -> Arrangement.End
        else -> Arrangement.Center
    }
    // The row gap between two wrapped word rows follows the line-spacing
    // option, which this layout used to ignore in favour of a fixed 0.25em.
    val lineSpacingRatio = (textStyle.lineHeight.value / textStyle.fontSize.value)
        .takeIf { it.isFinite() && it > 0f } ?: 1.35f

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = Arrangement.spacedBy(
            (textStyle.fontSize.value * 0.25f * lineSpacingRatio).dp,
        ),
    ) {
        words.forEach { word ->
            val startMs = (word.startTime * 1000).toLong()
            val endMs = (word.endTime * 1000).toLong()
            val duration = (endMs - startMs).coerceAtLeast(1L)
            val hasPassed = isActive && position > endMs
            val isWordActive = isActive && position in startMs..endMs
            val linear = if (isWordActive) ((position - startMs).toFloat() / duration).coerceIn(0f, 1f) else 0f
            // Scaled by the "Animation speed" preference exactly like the
            // text-based styles: this layout is the DEFAULT one, so leaving it
            // out is why the setting changed nothing at all on a fresh install.
            val progress = when {
                hasPassed -> 1f
                // A line that is already over counts as fully sung in
                // LYRICS_V2 (it has no other way to show it was completed).
                style == LyricsAnimationStyle.LYRICS_V2 && !isActive && position >= endMs -> 1f
                isWordActive -> lyricProgress(linear)
                else -> 0f
            }

            val isVivi = style == LyricsAnimationStyle.VIVIMUSIC_1
            // LYRICS_V2 is the mobile "bounce" style: the sung word lifts and
            // floats (a sine over its own progress) and its fill sweeps in
            // behind a moving edge instead of just changing colour.
            val isV2 = style == LyricsAnimationStyle.LYRICS_V2
            val sinProgress = if (isV2) sin(progress * PI).toFloat() else 0f
            // VIVIMUSIC_1 is the "premium" style: the sung word blooms in place
            // (scale + glow) while the ones around it sit back.
            val scale = when {
                isVivi -> 1f + 0.16f * progress
                isV2 -> 1f + 0.015f * sinProgress
                else -> 0.94f + 0.06f * progress
            }
            val blurRadius = when {
                !isVivi || isWordActive || hasPassed -> 0f
                isActive -> 0.9f
                else -> 2.0f
            }
            // APPLE_V2 used to be drawn with no motion whatsoever (scale fixed
            // at 1, no offset, no blur), so it was indistinguishable from plain
            // text: it is now a real style — un-sung words sit slightly smaller
            // and lower, and the sung word fades and lifts into place word by
            // word, the way the mobile renderer animates this one.
            val risePx = when {
                isVivi -> 0f
                // LYRICS_V2 floats the sung word up by up to 4 dp (scaled with
                // the text size, so the motion keeps its proportion).
                isV2 -> -4f * sinProgress * (textStyle.fontSize.value / 18f)
                else -> (1f - progress) * textStyle.fontSize.value * 0.55f
            }
            val wordAlpha = when {
                isWordActive -> if (isVivi) 1f else 0.45f + 0.55f * progress
                hasPassed -> 1f
                isActive -> 0.5f
                else -> 0.4f
            }
            val wordShadow = when {
                !glowEffect -> null
                isVivi && isWordActive -> Shadow(
                    color = accent.copy(alpha = 0.35f + 0.45f * progress),
                    offset = Offset.Zero,
                    blurRadius = 12f + 18f * progress,
                )
                isVivi && hasPassed -> Shadow(accent.copy(alpha = 0.2f), Offset.Zero, 10f)
                isV2 && isWordActive -> Shadow(
                    color = accent.copy(alpha = 0.45f * progress),
                    offset = Offset.Zero,
                    blurRadius = 12f * progress,
                )
                !isVivi && isWordActive -> Shadow(
                    color = accent.copy(alpha = 0.25f + 0.35f * progress),
                    offset = Offset.Zero,
                    blurRadius = 8f + 10f * progress,
                )
                else -> null
            }

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationY = risePx
                        alpha = wordAlpha
                    }
                    .then(if (blurRadius > 0f) Modifier.blur(blurRadius.dp) else Modifier)
                    .padding(horizontal = 1.dp),
            ) {
                if (isV2) {
                    // The dimmed base word...
                    Text(
                        text = word.text,
                        style = textStyle.copy(
                            color = if (isWordActive || hasPassed) accent.copy(alpha = 0.35f) else inactive,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    // ...and the bright copy, revealed behind an edge that
                    // travels across the word (the mobile style's liquid fill).
                    if (isWordActive || hasPassed) {
                        Text(
                            text = word.text,
                            style = textStyle.copy(
                                color = accent,
                                fontWeight = FontWeight.Bold,
                                shadow = wordShadow,
                            ),
                            modifier = Modifier
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
                                },
                        )
                    }
                } else {
                    Text(
                        text = word.text,
                        style = textStyle.copy(
                            color = if (isWordActive || hasPassed) accent else inactive,
                            fontWeight = if (isWordActive) FontWeight.ExtraBold else FontWeight.Bold,
                            shadow = wordShadow,
                        ),
                    )
                }
            }
        }
    }
}
