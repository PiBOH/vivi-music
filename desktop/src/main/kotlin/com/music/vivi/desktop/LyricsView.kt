package com.music.vivi.desktop

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
            val words = if (options.romanizeAsMain && hasRomanized) null else line.words

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
                    val blurred = when {
                        !isActive && options.appleMusicBlur -> true
                        !isActive && options.standardBlur -> true
                        else -> false
                    }
                    val blurRadius = when {
                        !blurred -> 0f
                        options.appleMusicBlur -> 2.4f
                        else -> 1.2f
                    }
                    val dimmed = when {
                        isActive -> 1f
                        options.appleMusicBlur -> 0.35f
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
                            nextLineTimeMs = lines.getOrNull(index + 1)?.timeMs,
                            lineTimeMs = line.timeMs,
                            isActive = isActive,
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
 * Styles that only need per-word colours/weights/shadows are drawn as a single
 * `Text` with an [androidx.compose.ui.text.AnnotatedString]; the two that move
 * each word on its own (APPLE_V2, VIVIMUSIC_1) lay the words out as separate
 * composables so they can scale and blur individually.
 */
@Composable
private fun AnimatedLyricLine(
    text: String,
    words: List<WordTimestamp>?,
    nextLineTimeMs: Long?,
    lineTimeMs: Long,
    isActive: Boolean,
    positionProvider: () -> Long,
    style: LyricsAnimationStyle,
    glowEffect: Boolean,
    accent: Color,
    inactive: Color,
    textStyle: TextStyle,
    textAlign: TextAlign,
) {
    val wordTimings = words?.takeIf { it.isNotEmpty() }
    val useWordLayout = style == LyricsAnimationStyle.APPLE_V2 || style == LyricsAnimationStyle.VIVIMUSIC_1

    if (wordTimings != null && useWordLayout) {
        WordFlowLine(
            words = wordTimings,
            positionProvider = positionProvider,
            isActive = isActive,
            style = style,
            glowEffect = glowEffect,
            accent = accent,
            inactive = inactive,
            lineTimeMs = lineTimeMs,
            nextLineTimeMs = nextLineTimeMs,
            textStyle = textStyle,
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

/** Per-word `AnnotatedString` for the six text-based animation styles. */
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
        // Smoothstep easing, shared by every style (as on mobile).
        val progress = when {
            hasPassed -> 1f
            isWordActive -> linear * linear * (3f - 2f * linear)
            else -> 0f
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

            LyricsAnimationStyle.GLOW -> {
                val glowIntensity = progress * progress
                SpanStyle(
                    color = accent.copy(
                        alpha = when {
                            !isActive -> 0.5f
                            isWordActive || hasPassed -> 0.45f + 0.55f * progress
                            else -> 0.35f
                        },
                    ),
                    fontWeight = if (isWordActive) FontWeight.ExtraBold else FontWeight.Bold,
                    shadow = when {
                        !glowEffect -> null
                        isWordActive && glowIntensity > 0.05f -> Shadow(
                            color = accent.copy(alpha = 0.5f + 0.3f * glowIntensity),
                            offset = Offset.Zero,
                            blurRadius = 16f + 12f * glowIntensity,
                        )
                        hasPassed -> Shadow(accent.copy(alpha = 0.25f), Offset.Zero, 8f)
                        else -> null
                    },
                )
            }

            LyricsAnimationStyle.SLIDE, LyricsAnimationStyle.KARAOKE -> {
                // A gradient brush that sweeps across the word as it is sung;
                // SLIDE keeps a tighter edge, KARAOKE a softer glow.
                val head = progress.coerceIn(0f, 1f)
                val glowAlpha = if (style == LyricsAnimationStyle.KARAOKE) 0.5f + 0.3f * progress else 0.4f * progress
                val brush = when {
                    isWordActive -> Brush.horizontalGradient(
                        0f to accent,
                        (head * 0.85f).coerceIn(0f, 1f) to accent,
                        head to accent,
                        (head + 0.04f).coerceIn(0f, 1f) to accent.copy(alpha = 0.6f),
                        (head + 0.12f).coerceIn(0f, 1f) to accent.copy(alpha = 0.4f),
                        1f to accent.copy(alpha = if (head >= 0.9f) 0.95f else 0.4f),
                    )
                    else -> null
                }
                if (brush != null) {
                    SpanStyle(
                        brush = brush,
                        fontWeight = FontWeight.ExtraBold,
                        shadow = if (glowEffect) {
                            Shadow(accent.copy(alpha = glowAlpha), Offset.Zero, 14f + 6f * progress)
                        } else {
                            null
                        },
                    )
                } else {
                    SpanStyle(
                        color = when {
                            !isActive -> inactive
                            hasPassed -> accent
                            else -> accent.copy(alpha = 0.4f)
                        },
                        fontWeight = if (hasPassed) FontWeight.Bold else FontWeight.Medium,
                        shadow = if (glowEffect && hasPassed && isActive) {
                            Shadow(accent.copy(alpha = 0.25f), Offset.Zero, 8f)
                        } else {
                            null
                        },
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

            // Handled by the word layout above.
            LyricsAnimationStyle.APPLE_V2, LyricsAnimationStyle.VIVIMUSIC_1 ->
                SpanStyle(color = accent.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
        }

        withStyle(wordStyle) { append(word.text) }
        if (index < words.lastIndex) append(" ")
    }
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
    lineTimeMs: Long,
    nextLineTimeMs: Long?,
    textStyle: TextStyle,
) {
    val position = positionProvider()
    // Bounds the fallback span of a single-word line (kept for parity with the
    // mobile heuristic: the line's own duration, at least 300 ms).
    val lineDuration = ((nextLineTimeMs ?: (lineTimeMs + 4000L)) - lineTimeMs).coerceAtLeast(300L)
    @Suppress("UNUSED_VARIABLE")
    val ignoredDuration = lineDuration

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalArrangement = Arrangement.spacedBy((textStyle.fontSize.value * 0.25f).dp),
    ) {
        words.forEach { word ->
            val startMs = (word.startTime * 1000).toLong()
            val endMs = (word.endTime * 1000).toLong()
            val duration = (endMs - startMs).coerceAtLeast(1L)
            val hasPassed = isActive && position > endMs
            val isWordActive = isActive && position in startMs..endMs
            val linear = if (isWordActive) ((position - startMs).toFloat() / duration).coerceIn(0f, 1f) else 0f
            val progress = when {
                hasPassed -> 1f
                isWordActive -> linear * linear * (3f - 2f * linear)
                else -> 0f
            }

            val isVivi = style == LyricsAnimationStyle.VIVIMUSIC_1
            // VIVIMUSIC_1 is the "premium" style: the sung word blooms in place
            // (scale + glow) while the ones around it sit back.
            val scale = if (isVivi) 1f + 0.16f * progress else 1f
            val blurRadius = when {
                !isVivi || isWordActive || hasPassed -> 0f
                isActive -> 0.9f
                else -> 2.0f
            }
            val wordAlpha = when {
                isWordActive || hasPassed -> 1f
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
                        alpha = wordAlpha
                    }
                    .then(if (blurRadius > 0f) Modifier.blur(blurRadius.dp) else Modifier)
                    .padding(horizontal = 1.dp),
            ) {
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
