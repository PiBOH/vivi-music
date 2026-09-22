package com.music.vivi.desktop

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.lyrics.LyricLine
import com.music.lyrics.LyricsParser
import com.music.lyrics.LyricsRomanizer
import com.music.lyrics.WordTimestamp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/**
 * Animated lyrics view — the desktop port of the mobile app's lyrics renderer.
 *
 * Every entry of [LyricsAnimationStyle] reproduces the mobile recipe of the same
 * name, and **no two entries draw the same thing** (that is the point of having
 * them at all):
 *
 * | style | what moves |
 * |---|---|
 * | `NONE` | the plainest word fill of the family — a brightness step per word |
 * | `FADE` | the word fades in with a soft bloom |
 * | `GLOW` | a strong halo that grows with the square of the fill |
 * | `SLIDE` | a tight colour front slides across the word (plus a breathing halo) |
 * | `KARAOKE` | a wide, seven-stop colour front with a firm halo |
 * | `APPLE` | the Apple Music fill: weight-led, tight halo |
 * | `APPLE_V2` | the line is revealed character by character |
 * | `VIVIMUSIC_1` | one wave sweeps the whole sentence |
 * | `LYRICS_V2` | the words float and bounce while a bright copy is revealed |
 * | `METRO_LYRICS` | the Metro canvas: per-grapheme crescendo, nudge and line push |
 * | `ALPHA` | desktop-only: the pre-port look, one plain line, no effect |
 *
 * Three things the desktop renderer adds on top of the mobile one, all of them
 * because the picker pretends to do more than the mobile styles did on their
 * own:
 *
 *  - **every animated style gets word timings.** The community servers return
 *    plain LRC (line timings only) most of the time, and mobile animated only
 *    `APPLE_V2`/`METRO_LYRICS`/`VIVIMUSIC` in that case: the other six fell back
 *    to the same plain line, so on a plain file NONE/FADE/GLOW/SLIDE/KARAOKE/
 *    APPLE were literally indistinguishable. [effectiveWords] now estimates the
 *    per-word timings from the line's own duration (the estimator mobile already
 *    uses for `APPLE_V2`), so each style keeps its own recipe on every file.
 *  - **one smooth clock for the panel** ([rememberSmoothPosition]): the player
 *    polls the position ~40 times a second, and drawing that raw made the words
 *    move in ~25 ms steps. The clock extrapolates between samples and dissolves
 *    the correction, so the animation runs at the display's rate.
 *  - **the line cross-fades are animated** (alpha, scale and the progressive
 *    blur), with the mobile timings, instead of switching instantly.
 *
 * Lyrics animation is deliberately **independent from the "Animations" master
 * switch**: a user who turns the UI transitions off still expects the karaoke
 * fill to run (that is playback feedback, not decoration). Only "Animation
 * speed" reaches them, through [lyricTween].
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
 * A lyric animation duration, scaled by the global "Animation speed"
 * preference (Appearance → Animation speed).
 *
 * Deliberately *not* [Animations.ms] on the master switch's side: the lyrics run
 * even when "Animations" is off (see the file header). The speed preference is
 * the only one that reaches them.
 */
private fun lyricTween(base: Int): Int = when (Animations.speed) {
    "fast" -> (base * 0.6f).toInt().coerceAtLeast(1)
    "slow" -> (base * 1.6f).toInt().coerceAtLeast(1)
    else -> base
}

/**
 * How much faster than real time the lyric *motion* runs ("Animation speed":
 * 1.6x / 1x / 0.62x).
 *
 * The styles that draw on the clock follow the setting through [lyricTween];
 * the Metro springs have hard-coded constants instead, so they scale their
 * windows with this.
 */
private val LYRIC_MOTION: Float
    get() = when (Animations.speed) {
        "fast" -> 1.6f
        "slow" -> 0.62f
        else -> 1f
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

// ---------------------------------------------------------------------------
// Per-style line appearance
// ---------------------------------------------------------------------------

/** How a lyric line sits in the list at a given distance from the sung one. */
private data class LineAppearance(val alpha: Float, val blurDp: Float)

/**
 * The line-level alpha and blur, per style.
 *
 * These are the numbers the mobile renderer carries inside each line wrapper:
 * the generic lines go 1 / 0.5 (and 0.8 of that for a background vocal), `VIVI
 * Music` keeps its soft 0.75 → 0.20 falloff and `MetroLyrics` its fast
 * 0.3/0.2/0.15/0.1/0.08 curve. The values here are the *targets*: the caller
 * animates towards them ([animateFloatAsState]), like the mobile style does.
 */
private fun lineAppearance(
    options: LyricsDisplayOptions,
    style: LyricsAnimationStyle,
    isActive: Boolean,
    isBackground: Boolean,
    distance: Int,
): LineAppearance {
    val appleBlur = options.appleMusicBlur && options.autoScroll
    val standardBlur = options.standardBlur && options.autoScroll
    val blur = when {
        isActive -> 0f
        appleBlur && style == LyricsAnimationStyle.VIVIMUSIC_1 -> progressiveBlur(distance)
        standardBlur -> progressiveBlur(distance)
        else -> 0f
    }
    val baseAlpha = when (style) {
        // The pre-port look: the list did not fade anything.
        LyricsAnimationStyle.ALPHA -> 1f

        // VIVI Music: soft falloff 0.75 → 0.20.
        LyricsAnimationStyle.VIVIMUSIC_1 -> when {
            isActive -> 1f
            distance == 1 -> 0.75f
            distance == 2 -> 0.50f
            distance == 3 -> 0.30f
            else -> 0.20f
        }

        // Metro: a foreground line sits at 0.30, the others fall away fast.
        LyricsAnimationStyle.METRO_LYRICS -> if (isBackground) {
            0.5f
        } else {
            when {
                isActive -> 1f
                distance == 0 -> 0.3f
                distance <= 2 -> 0.2f
                distance == 3 -> 0.15f
                distance == 4 -> 0.1f
                else -> 0.08f
            }
        }

        else -> if (isActive) 1f else 0.5f
    }
    // Background vocals are drawn slightly dimmer on top of their own alpha.
    val alpha = if (isBackground && style != LyricsAnimationStyle.METRO_LYRICS) {
        baseAlpha * 0.8f
    } else {
        baseAlpha
    }
    return LineAppearance(alpha = alpha, blurDp = blur)
}

/** The mobile progressive blur: nothing close to the sung line, 6 dp far away. */
private fun progressiveBlur(distance: Int): Float = when (distance) {
    1, 2 -> 0f
    3 -> 2f
    4 -> 4f
    else -> 6f
}

// ---------------------------------------------------------------------------
// Word timings
// ---------------------------------------------------------------------------

/**
 * The word timings a style should animate with.
 *
 * Real ones when the source has them (rich-sync / TTML sources), otherwise a
 * line-level estimate — see the file header for why every style gets one.
 * [LyricsAnimationStyle.ALPHA] is the exception: it is the no-effect style, so
 * it never animates anything.
 */
private fun effectiveWords(
    words: List<WordTimestamp>?,
    text: String,
    lineStartMs: Long,
    lineEndMs: Long,
    style: LyricsAnimationStyle,
): List<WordTimestamp>? {
    if (!words.isNullOrEmpty()) return words
    if (style == LyricsAnimationStyle.ALPHA) return null
    // Metro estimates its own way (180 ms per word, 30 ms apart): that cadence
    // is part of how the style reads.
    val estimate = if (style == LyricsAnimationStyle.METRO_LYRICS) {
        estimateMetroWords(text, lineStartMs)
    } else {
        estimateLineWords(text, lineStartMs, lineEndMs)
    }
    return estimate.ifEmpty { null }
}

/**
 * Estimates per-word timings from the line's own duration, weighted by how many
 * characters each word has (the estimator the mobile `APPLE_V2`/`VIVI Music`
 * styles already use). The line's active window is 95 % of its duration, the
 * same heuristic mobile applies, so the words do not run into the next line.
 */
private fun estimateLineWords(text: String, lineStartMs: Long, lineEndMs: Long): List<WordTimestamp> {
    val parts = text.split(Regex("\\s+")).filter { it.isNotBlank() }
    if (parts.isEmpty()) return emptyList()
    val duration = (lineEndMs - lineStartMs).takeIf { it > 0 } ?: 4_000L
    val active = (duration * 0.95).toLong().coerceAtLeast(300L)
    val totalChars = text.length.coerceAtLeast(1)
    var accumulated = 0L
    return parts.mapIndexed { index, part ->
        val chars = if (index < parts.lastIndex) part.length + 1 else part.length
        val wordDuration = (active * chars / totalChars).coerceAtLeast(30L)
        val start = lineStartMs + accumulated
        accumulated += wordDuration
        WordTimestamp(part, start / 1000.0, (start + wordDuration) / 1000.0)
    }
}

/**
 * Mobile's `METRO_LYRICS` estimation for a line with no word timings: 180 ms
 * per word, each starting 30 ms after the previous one.
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

// ---------------------------------------------------------------------------
// The panel clock
// ---------------------------------------------------------------------------

/**
 * The renderer's playback clock, at the display's rate.
 *
 * The player hands over a position sample ~40 times a second; drawing it raw
 * moves the words in ~25 ms steps, which is what made the animation look
 * choppy. This clock keeps the last sample plus the wall time elapsed since it
 * arrived (exactly how the mobile Metro renderer extrapolates), and dissolves
 * the difference between that prediction and the next sample over a few frames
 * instead of snapping back to it.
 *
 * A jump larger than [SNAP_THRESHOLD_MS] is a seek: it snaps, because
 * interpolating across a seek would sweep the lyrics through every line in
 * between.
 */
@Composable
private fun rememberSmoothPosition(positionMs: Long, isPlaying: Boolean): State<Long> {
    val playing = rememberUpdatedState(isPlaying)
    val output = remember { mutableStateOf(positionMs) }
    var sample by remember { mutableStateOf(positionMs.toFloat()) }
    var sampleAtNanos by remember { mutableStateOf(0L) }
    var drift by remember { mutableStateOf(0f) }
    var started by remember { mutableStateOf(false) }
    var wasPlaying by remember { mutableStateOf(isPlaying) }

    // A play/pause toggle is re-keyed too: on resume the extrapolation must
    // start from the sample the player just handed over, not from a timestamp
    // that was frozen for the whole pause.
    LaunchedEffect(positionMs, isPlaying) {
        val now = System.nanoTime()
        val toggled = wasPlaying != isPlaying
        wasPlaying = isPlaying
        if (!started || toggled || abs(positionMs - output.value) > SNAP_THRESHOLD_MS) {
            started = true
            drift = 0f
            sample = positionMs.toFloat()
        } else {
            // What we were showing when this sample was taken, against what the
            // player now reports: usually a few milliseconds, hidden by the
            // decay below rather than shown as a step back.
            val predicted = sample + (now - sampleAtNanos) / 1_000_000f
            drift = (positionMs - predicted).coerceIn(-200f, 200f)
            sample = positionMs.toFloat()
        }
        sampleAtNanos = now
        output.value = positionMs
    }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { now ->
                if (!started) return@withFrameNanos
                drift *= 0.75f
                if (drift < 0.4f && drift > -0.4f) drift = 0f
                // Clamped as a safety net: if the player ever stops reporting
                // for a while, the lyrics stall half a second ahead instead of
                // running off on their own.
                val advance = if (playing.value) {
                    ((now - sampleAtNanos) / 1_000_000f).coerceIn(0f, MAX_EXTRAPOLATION_MS)
                } else {
                    0f
                }
                output.value = (sample + advance + drift).toLong()
            }
        }
    }

    return output
}

/** A position change bigger than this is a seek, not a poll. */
private const val SNAP_THRESHOLD_MS = 1_500L

/** How far the clock may run past its last sample before it stops advancing. */
private const val MAX_EXTRAPOLATION_MS = 500f

// ---------------------------------------------------------------------------
// The list
// ---------------------------------------------------------------------------

@Composable
fun LyricsList(
    lines: List<LyricLine>,
    positionMs: Long,
    options: LyricsDisplayOptions,
    isPlaying: Boolean = true,
    onSeek: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    val inactive = MaterialTheme.colorScheme.onSurfaceVariant
    val listState = rememberLazyListState()

    // The smooth position: read inside the item lambdas (and, for the Metro
    // canvas, inside its draw scope), so only the lines that actually animate
    // follow it.
    val positionState = rememberSmoothPosition(positionMs, isPlaying)

    // Which line is being sung, straight off the clock instead of a poll: the
    // active line then changes within a frame of the audio, and because it is a
    // derived value the list only reacts when the index actually moves.
    val currentIndex by remember(lines) {
        derivedStateOf { LyricsParser.currentLineIndex(lines, positionState.value) }
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
    val index = currentIndex

    LazyColumn(state = listState, modifier = modifier.fillMaxSize()) {
        itemsIndexed(lines) { itemIndex, line ->
            val isActive = itemIndex == index
            val isPast = index >= 0 && itemIndex < index
            // Distance from the sung line: drives the blur / dimming of the
            // lines around it, like the mobile renderer.
            val distance = if (index < 0) 10 else abs(itemIndex - index)
            val isBackground = line.isBackground

            val romanizedText = romanized?.getOrNull(itemIndex)
            val hasRomanized = options.romanize != null &&
                romanizedText != null && romanizedText != line.text
            val mainText = if (options.romanizeAsMain && hasRomanized) romanizedText!! else line.text
            val subText = when {
                options.romanizeAsMain && hasRomanized -> line.text
                hasRomanized -> romanizedText
                else -> translated?.getOrNull(itemIndex)
            }
            // A word animation needs the words of the text it is drawing: with
            // the romanized form promoted to the main line there are none.
            val words = if (options.romanizeAsMain && hasRomanized) null else line.words
            // The line's own end, for the styles that time a sentence: the next
            // line's start, or 4 s later on the last one.
            val lineEndMs = lines.getOrNull(itemIndex + 1)?.timeMs ?: (line.timeMs + 4_000)

            // Only the lines that can actually be animating read the clock;
            // everything else is drawn from a fixed position, so a static line
            // does not recompose 60 times a second.
            val animated = options.style != LyricsAnimationStyle.ALPHA && (isActive || distance == 1)
            // The Metro canvas reads the clock in its draw scope, which
            // invalidates only the drawing, not the composition.
            val livePosition = when {
                options.style == LyricsAnimationStyle.METRO_LYRICS -> line.timeMs
                animated -> positionState.value
                // A static line is drawn as if the playhead sat at its end when
                // it has already been sung (so the wave styles show a completed
                // wave instead of an untouched one) and at its start otherwise.
                isPast -> lineEndMs
                else -> line.timeMs
            }

            // {agent:v1}/{agent:v2}/{agent:v1000} from the source override the
            // user's own text position (mobile's multi-singer support).
            val textAlign = when {
                isBackground -> TextAlign.Center
                line.agent == "v1" -> TextAlign.Start
                line.agent == "v2" -> TextAlign.End
                line.agent == "v1000" -> TextAlign.Center
                else -> when (options.position) {
                    LyricsPosition.LEFT -> TextAlign.Start
                    LyricsPosition.CENTER -> TextAlign.Center
                    LyricsPosition.RIGHT -> TextAlign.End
                }
            }
            val lineAlignment = when (textAlign) {
                TextAlign.Start -> Alignment.CenterStart
                TextAlign.End -> Alignment.CenterEnd
                else -> Alignment.Center
            }
            val columnAlignment = when (textAlign) {
                TextAlign.Start -> Alignment.Start
                TextAlign.End -> Alignment.End
                else -> Alignment.CenterHorizontally
            }

            val baseSize = options.textSizeSp * (if (isBackground) 0.85f else 1f)
            val lineStyle = TextStyle(
                color = if (isActive) accent else inactive,
                fontSize = (baseSize * if (isActive) 1.12f else 1f).sp,
                lineHeight = (baseSize * options.lineSpacing).sp,
            )

            // The line-level cross-fade, animated with the mobile timings (the
            // alpha in 400 ms for the generic styles, the blur in ~600/1000 ms).
            val appearance = lineAppearance(options, options.style, isActive, isBackground, distance)
            val alpha by animateFloatAsState(
                targetValue = appearance.alpha,
                animationSpec = tween(lyricTween(if (isActive) 260 else 400), easing = FastOutSlowInEasing),
                label = "lyricLineAlpha",
            )
            val scale by animateFloatAsState(
                targetValue = if (isActive) 1.05f else 1f,
                animationSpec = tween(lyricTween(400), easing = FastOutSlowInEasing),
                label = "lyricLineScale",
            )
            val blurDp by animateFloatAsState(
                targetValue = appearance.blurDp,
                animationSpec = tween(lyricTween(900), easing = FastOutSlowInEasing),
                label = "lyricLineBlur",
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
                Column(
                    horizontalAlignment = columnAlignment,
                    modifier = Modifier
                        .graphicsLayer {
                            // Metro carries its line alpha inside the canvas (its
                            // unsung characters need the 0.3 baseline), so the
                            // wrapper must not dim it a second time.
                            if (options.style != LyricsAnimationStyle.METRO_LYRICS) this.alpha = alpha
                            scaleX = scale
                            scaleY = scale
                        }
                        .then(if (blurDp >= 0.1f) Modifier.blur(blurDp.dp) else Modifier),
                ) {
                    AnimatedLyricLine(
                        text = mainText,
                        words = words,
                        isActive = isActive,
                        isPast = isPast,
                        isBackground = isBackground,
                        lineStartMs = line.timeMs,
                        lineEndMs = lineEndMs,
                        position = livePosition,
                        positionState = positionState,
                        lineAlpha = if (options.style == LyricsAnimationStyle.METRO_LYRICS) alpha else 1f,
                        style = options.style,
                        glowEffect = options.glowEffect,
                        accent = accent,
                        inactive = inactive,
                        textStyle = lineStyle,
                        textAlign = textAlign,
                    )

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

// ---------------------------------------------------------------------------
// One line, one style
// ---------------------------------------------------------------------------

/**
 * One lyric line, animated with the selected style.
 *
 * The styles that only recolour the words are drawn as a single `Text` with an
 * [androidx.compose.ui.text.AnnotatedString]; the ones the mobile renderer
 * animates word by word lay the words out as separate composables so each one
 * can move, scale and blur on its own, and `METRO_LYRICS` draws on a canvas.
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
    position: Long,
    positionState: State<Long>,
    /** Line-level alpha, applied inside the canvas by the styles that need it. */
    lineAlpha: Float = 1f,
    style: LyricsAnimationStyle,
    glowEffect: Boolean,
    accent: Color,
    inactive: Color,
    textStyle: TextStyle,
    textAlign: TextAlign,
) {
    if (style == LyricsAnimationStyle.ALPHA) {
        PlainLyricLine(text, isActive, glowEffect, accent, inactive, textStyle, textAlign)
        return
    }

    val wordTimings = effectiveWords(words, text, lineStartMs, lineEndMs, style)

    when (style) {
        LyricsAnimationStyle.VIVIMUSIC_1 -> {
            ViviWaveLine(
                text = text,
                words = wordTimings,
                isActive = isActive,
                position = position,
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
                position = position,
                accent = accent,
                inactive = inactive,
                textStyle = textStyle,
                textAlign = textAlign,
            )
            return
        }

        LyricsAnimationStyle.METRO_LYRICS -> {
            MetroGraphemeLine(
                text = text,
                words = wordTimings.orEmpty(),
                isActive = isActive,
                positionState = positionState,
                accent = accent,
                lineAlpha = lineAlpha,
                focusedAlpha = if (isBackground) 0.5f else 0.3f,
                isBackground = isBackground,
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
                lineStartMs = lineStartMs,
                lineEndMs = lineEndMs,
                position = position,
                accent = accent,
                textStyle = textStyle,
                textAlign = textAlign,
            )
            return
        }

        else -> Unit
    }

    if (!wordTimings.isNullOrEmpty()) {
        // NONE / FADE / GLOW / SLIDE / KARAOKE / APPLE: per-word colours and
        // shadows inside one text run, each one its own recipe.
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

    // No words at all (a one-word line, or romanization promoted to the main
    // text): the style still has to look like itself.
    SentenceLyricLine(
        text = text,
        style = style,
        isActive = isActive,
        isBackground = isBackground,
        lineStartMs = lineStartMs,
        lineEndMs = lineEndMs,
        position = position,
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
 * included (alphas, weights, halo radii, easing): that is what makes the
 * entries of the picker look like their mobile counterpart instead of six
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
            // SLIDE, KARAOKE and APPLE also treat an already-sung line as done;
            // NONE / FADE / GLOW advance on the current line only.
            LyricsAnimationStyle.SLIDE, LyricsAnimationStyle.KARAOKE, LyricsAnimationStyle.APPLE ->
                (isActive && position >= endMs) || isPast

            else -> isActive && position > endMs
        }
        // SLIDE has no easing of its own (it drives a gradient edge with the raw
        // progress); the rest of the family eases with the mobile smoothstep.
        val eased = smoothstep(linear)
        val progress = when {
            passed -> 1f
            isWordActive -> eased
            else -> 0f
        }
        val lineColor = if (isActive) accent else inactive

        val wordStyle = when (style) {
            // Mobile NONE: the plainest fill of the family — a brightness step.
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

            // Mobile FADE: the word fades in with a soft bloom behind it.
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

            // Mobile SLIDE: a TIGHT colour front (2 % of the word) plus a halo
            // that "breathes" while the word is sung (a slow sine).
            LyricsAnimationStyle.SLIDE -> {
                val elapsed = if (isWordActive) position - startMs else 0L
                val breathe = if (isWordActive) {
                    (sin(((elapsed % 3000) / 3000f * 2f * PI).toDouble()).toFloat() * 0.03f)
                        .coerceIn(0f, 0.03f)
                } else {
                    0f
                }
                val glowIntensity = (0.3f + linear * 0.7f + breathe).coerceIn(0f, 1.1f)
                when {
                    isWordActive && duration > 0 -> SpanStyle(
                        brush = Brush.horizontalGradient(
                            0f to accent,
                            (linear * 0.95f).coerceIn(0f, 1f) to accent,
                            linear.coerceIn(0f, 1f) to accent.copy(alpha = 0.9f),
                            (linear + 0.02f).coerceIn(0f, 1f) to accent.copy(alpha = 0.5f),
                            (linear + 0.08f).coerceIn(0f, 1f) to accent.copy(alpha = 0.35f),
                            1f to accent.copy(alpha = 0.35f),
                        ),
                        fontWeight = FontWeight.ExtraBold,
                        shadow = Shadow(
                            color = accent.copy(alpha = 0.4f * glowIntensity),
                            offset = Offset.Zero,
                            blurRadius = 14f + 4f * linear,
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

            // Mobile KARAOKE: a WIDE seven-stop front whose halo builds with
            // the square of the (eased) progress.
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

            // Mobile APPLE: weight-led fill with a tight halo.
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

            // Drawn by their own renderer; this branch is unreachable.
            LyricsAnimationStyle.APPLE_V2, LyricsAnimationStyle.LYRICS_V2,
            LyricsAnimationStyle.VIVIMUSIC_1, LyricsAnimationStyle.METRO_LYRICS,
            LyricsAnimationStyle.ALPHA,
            -> SpanStyle(color = accent.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
        }

        withStyle(wordStyle) { append(word.text) }
        if (index < words.lastIndex) append(" ")
    }
}

/**
 * A one-piece line for the styles whose animation is per word, used when the
 * line has no words at all. Each style keeps its own character here too (the
 * fade, the halo, the sweeping front), so falling back never shows the user a
 * style other than the one they picked.
 */
@Composable
private fun SentenceLyricLine(
    text: String,
    style: LyricsAnimationStyle,
    isActive: Boolean,
    isBackground: Boolean,
    lineStartMs: Long,
    lineEndMs: Long,
    position: Long,
    accent: Color,
    inactive: Color,
    textStyle: TextStyle,
    textAlign: TextAlign,
) {
    val duration = (lineEndMs - lineStartMs).coerceAtLeast(300L)
    val linear = if (!isActive) 0f else ((position - lineStartMs).toFloat() / duration).coerceIn(0f, 1f)
    val eased = smoothstep(linear)
    val fade = if (isActive) easeInAlpha(linear) else 0f

    val paint = when (style) {
        // A brightness step, no halo, no gradient.
        LyricsAnimationStyle.NONE,
        LyricsAnimationStyle.ALPHA,
        -> SpanStyle(
            color = if (isActive) accent.copy(alpha = 0.5f + 0.5f * eased) else inactive.copy(alpha = 0.7f),
            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold,
        )

        // A whole-sentence fade with a soft bloom.
        LyricsAnimationStyle.FADE -> SpanStyle(
            color = if (isActive) accent.copy(alpha = 0.4f + 0.6f * fade) else inactive.copy(alpha = 0.55f),
            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold,
            shadow = if (isActive && fade > 0.05f) {
                Shadow(accent.copy(alpha = 0.35f * fade), Offset.Zero, 12f * fade)
            } else {
                null
            },
        )

        // A halo that grows with the square of the fill.
        LyricsAnimationStyle.GLOW -> {
            val glow = eased * eased
            SpanStyle(
                color = if (isActive) accent.copy(alpha = 0.45f + 0.55f * eased) else inactive.copy(alpha = 0.5f),
                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold,
                shadow = if (isActive) {
                    Shadow(accent.copy(alpha = 0.5f + 0.3f * glow), Offset.Zero, 16f + 12f * glow)
                } else {
                    null
                },
            )
        }

        // A tight colour front travelling across the sentence (and, once it has
        // arrived, a solid line: the gradient's own tail stays dim by design).
        LyricsAnimationStyle.SLIDE -> if (linear >= 1f && isActive) {
            SpanStyle(
                color = accent,
                fontWeight = FontWeight.Bold,
                shadow = Shadow(accent.copy(alpha = 0.4f), Offset.Zero, 12f),
            )
        } else {
            SpanStyle(
                brush = Brush.horizontalGradient(
                    0f to accent,
                    (linear * 0.95f).coerceIn(0f, 1f) to accent,
                    linear.coerceIn(0f, 1f) to accent.copy(alpha = 0.9f),
                    (linear + 0.02f).coerceIn(0f, 1f) to accent.copy(alpha = 0.5f),
                    (linear + 0.08f).coerceIn(0f, 1f) to accent.copy(alpha = 0.35f),
                    1f to accent.copy(alpha = 0.35f),
                ),
                fontWeight = FontWeight.ExtraBold,
                shadow = Shadow(
                    color = accent.copy(alpha = 0.4f * (0.3f + linear * 0.7f)),
                    offset = Offset.Zero,
                    blurRadius = 14f + 4f * linear,
                ),
            )
        }

        // The wide karaoke front, solid once complete.
        LyricsAnimationStyle.KARAOKE -> if (eased >= 1f && isActive) {
            SpanStyle(
                color = accent,
                fontWeight = FontWeight.Bold,
                shadow = Shadow(accent.copy(alpha = 0.25f), Offset.Zero, 8f),
            )
        } else {
            SpanStyle(
                brush = Brush.horizontalGradient(
                    0f to accent.copy(alpha = 0.4f),
                    (eased * 0.6f).coerceIn(0f, 1f) to accent.copy(alpha = 0.75f),
                    (eased * 0.85f).coerceIn(0f, 1f) to accent.copy(alpha = 0.95f),
                    eased.coerceIn(0f, 1f) to accent,
                    (eased + 0.03f).coerceIn(0f, 1f) to accent.copy(alpha = 0.85f),
                    (eased + 0.1f).coerceIn(0f, 1f) to accent.copy(alpha = 0.5f),
                    1f to accent.copy(alpha = if (eased >= 0.9f) 0.95f else 0.4f),
                ),
                fontWeight = FontWeight.ExtraBold,
                shadow = Shadow(
                    color = accent.copy(alpha = 0.5f + 0.3f * (eased * eased)),
                    offset = Offset.Zero,
                    blurRadius = 16f + 12f * (eased * eased),
                ),
            )
        }

        // Apple Music: the weight leads, the halo stays tight.
        LyricsAnimationStyle.APPLE,
        LyricsAnimationStyle.LYRICS_V2,
        LyricsAnimationStyle.VIVIMUSIC_1,
        LyricsAnimationStyle.APPLE_V2,
        -> {
            val glow = eased * eased
            SpanStyle(
                color = if (isActive) accent.copy(alpha = 0.55f + 0.45f * eased) else inactive.copy(alpha = 0.55f),
                fontWeight = when {
                    !isActive -> FontWeight.SemiBold
                    eased >= 1f -> FontWeight.Bold
                    else -> FontWeight.ExtraBold
                },
                shadow = if (isActive && glow > 0.02f) {
                    Shadow(accent.copy(alpha = 0.2f + 0.4f * glow), Offset.Zero, 10f + 12f * glow)
                } else {
                    null
                },
            )
        }

        LyricsAnimationStyle.METRO_LYRICS -> SpanStyle(
            color = if (isActive) accent.copy(alpha = 0.3f + 0.7f * eased) else inactive.copy(alpha = 0.3f),
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
        )
    }

    Text(
        text = buildAnnotatedString { withStyle(paint) { append(text) } },
        style = textStyle.copy(
            fontStyle = if (isBackground) FontStyle.Italic else FontStyle.Normal,
        ),
        textAlign = textAlign,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** A slow-in fade used by the sentence-level FADE style. */
private fun easeInAlpha(linear: Float): Float {
    val t = linear.coerceIn(0f, 1f)
    return t * t
}

// ---------------------------------------------------------------------------
// Shared layout helpers
// ---------------------------------------------------------------------------

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
 * out on its own; without word timings the line's own duration is split over
 * its characters by count.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppleV2Line(
    text: String,
    words: List<WordTimestamp>?,
    isActive: Boolean,
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

    val sinProgress = sin((progress * PI).toDouble()).toFloat()
    val wordScale = 1f + (0.015f * sinProgress)
    // Mobile animates the float (50 ms in, 350 ms back); the desktop runs it off
    // the smooth clock, which is already frame rate and honours the speed
    // setting only through the animations themselves.
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
 * with that local progress. The space glyphs follow the wave too, which is what
 * keeps the sentence reading as a single motion instead of separate fills.
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

// ---------------------------------------------------------------------------
// Metro lyrics — the per-grapheme canvas
// ---------------------------------------------------------------------------

/**
 * Mobile `METRO_LYRICS`, ported to a desktop canvas.
 *
 * The mobile style is the only one that does not animate words but *graphemes*,
 * on a measured text layout, and it has three effects that nothing else in the
 * picker has:
 *
 *  - **hyphen crescendo**: a word split on `-` (`go-o-o-o`) is sung as one
 *    group; each segment gets a baseline scale by position and the last one
 *    springs, then the whole group springs back out over 600 ms (a decaying
 *    cosine).
 *  - **per-character nudge**: each character inside the word being sung gets
 *    `0.038 · sin(π·lp) · e^(−3·lp)`, its own local progress, so the word
 *    expands as a wave instead of uniformly.
 *  - **line push**: every character's scale widens it, the widths are summed
 *    per line and the whole line is shifted by (half) that sum, so a centred
 *    line stays centred while it sings and does not drift.
 *
 * Two deliberate differences from the mobile code:
 *
 *  - mobile measures the line's pushes with one set of spring constants and
 *    then draws with another (two loops that disagree); here the values are
 *    computed once and both used for the push and the draw, which is what makes
 *    the centring actually correct.
 *  - the halo is emulated with offset copies instead of Android's
 *    `BlurMaskFilter`, which has no portable equivalent inside a `DrawScope`.
 *
 * The clock is read **inside the draw lambda**: a frame only invalidates the
 * drawing of this line, never its composition or text layout.
 */
@Composable
private fun MetroGraphemeLine(
    text: String,
    words: List<WordTimestamp>,
    isActive: Boolean,
    positionState: State<Long>,
    accent: Color,
    /** The line's own (animated) alpha, for the lines that are not singing. */
    lineAlpha: Float,
    /** The baseline of the unsung characters on the line that IS singing. */
    focusedAlpha: Float,
    isBackground: Boolean,
    textStyle: TextStyle,
    textAlign: TextAlign,
) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val graphemes = remember(text) { text.toGraphemeClusters() }
    val flowWords = remember(words, isBackground) {
        words.mapIndexed { index, word ->
            FlowWord(
                text = if (isBackground) {
                    var t = word.text
                    if (index == 0) t = t.removePrefix("(")
                    if (index == words.lastIndex) t = t.removeSuffix(")")
                    t
                } else {
                    word.text
                },
                startMs = (word.startTime * 1000).toLong(),
                endMs = (word.endTime * 1000).toLong(),
                hasTrailingSpace = index < words.lastIndex,
            )
        }
    }
    // A hyphenated word is sung in segments, each getting an equal share of the
    // word's own time.
    val segments = remember(flowWords) { splitHyphenSegments(flowWords) }
    val clusterToWord = remember(graphemes, segments.words, text) {
        mapClustersToWords(graphemes, segments.words, text)
    }
    val groups = remember(segments.words) { hyphenGroups(segments.words) }

    if (graphemes.isEmpty()) return

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val maxWidthPx = constraints.maxWidth
        val layout = remember(text, maxWidthPx, textStyle) {
            measurer.measure(
                text = text,
                style = textStyle,
                constraints = Constraints(
                    minWidth = if (maxWidthPx > 0) maxWidthPx else 0,
                    maxWidth = if (maxWidthPx > 0) maxWidthPx else Constraints.Infinity,
                ),
                softWrap = true,
            )
        }
        val letters = remember(graphemes, textStyle) {
            graphemes.map { measurer.measure(it, textStyle) }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { layout.size.height.toDp() }),
        ) {
            val position = positionState.value
            val lineColor = accent.copy(alpha = lineAlpha)
            if (!isActive) {
                drawText(layout, color = lineColor)
                return@Canvas
            }
            val wordCount = segments.words.size
            if (wordCount == 0) {
                drawText(layout, color = accent)
                return@Canvas
            }

            val clusterCount = graphemes.size
            val wordOfCluster = clusterToWord.word
            val charInWord = clusterToWord.charInWord
            val wordLength = clusterToWord.wordLength

            // ---- per-word state -------------------------------------------------
            val sungFactor = FloatArray(wordCount)
            val sung = BooleanArray(wordCount)
            for (w in 0 until wordCount) {
                val word = segments.words[w]
                val span = (word.endMs - word.startMs).coerceAtLeast(1L)
                when {
                    position > word.endMs -> {
                        sung[w] = true
                        sungFactor[w] = 1f
                    }
                    position >= word.startMs -> {
                        sungFactor[w] = ((position - word.startMs).toFloat() / span).coerceIn(0f, 1f)
                    }
                }
            }

            // Landing wobble: 125 ms up, 625 ms down (the whole window scales
            // with the animation-speed preference).
            val motion = LYRIC_MOTION
            val wobbleWindow = WOBBLE_WINDOW_MS / motion
            val wobbleRise = WOBBLE_RISE_MS / motion
            val wobble = FloatArray(flowWords.size)
            flowWords.forEachIndexed { index, word ->
                val since = (position - word.startMs).toFloat()
                if (since in 0f..wobbleWindow) {
                    wobble[index] = if (since < wobbleRise) {
                        since / wobbleRise
                    } else {
                        (1f - (since - wobbleRise) / (wobbleWindow - wobbleRise)).coerceAtLeast(0f)
                    }
                }
            }

            // ---- per-grapheme state --------------------------------------------
            val scaleXs = FloatArray(clusterCount)
            val scaleYs = FloatArray(clusterCount)
            val lineOfCluster = IntArray(clusterCount)
            val bounds = arrayOfNulls<Rect>(clusterCount)
            val charProgress = FloatArray(clusterCount)
            val linePush = FloatArray(layout.lineCount)
            for (i in 0 until clusterCount) {
                val offset = clusterToWord.offsets[i]
                lineOfCluster[i] = layout.getLineForOffset(offset)
                val box = layout.getBoundingBox(offset)
                bounds[i] = box
                val w = wordOfCluster[i]
                val original = if (w != -1) segments.toOriginal[w] else -1
                if (w != -1) {
                    val word = segments.words[w]
                    val span = (word.endMs - word.startMs).coerceAtLeast(100L)
                    val wordProgress = (position - word.startMs).toDouble() / span.toDouble()
                    val inside = charInWord[i].toDouble() / wordLength[i].coerceAtLeast(1).toDouble()
                    charProgress[i] = ((wordProgress - inside) * wordLength[i].coerceAtLeast(1))
                        .coerceIn(0.0, 1.0).toFloat()
                }
                val wobbleX = if (original != -1) wobble[original] * 0.025f else 0f
                val wobbleY = if (original != -1) wobble[original] * 0.015f else 0f
                val crescendo = if (w != -1) crescendoDelta(groups[w], sungFactor[w], position, motion) else 0f
                val nudge = if (w != -1 && !sung[w] && sungFactor[w] > 0f) {
                    NUDGE_STRENGTH * sin((charProgress[i] * PI).toDouble()).toFloat() *
                        exp(-3f * charProgress[i])
                } else {
                    0f
                }
                scaleXs[i] = 1f + wobbleX + crescendo + nudge * 0.3f
                scaleYs[i] = 1f + wobbleY + crescendo + nudge
                linePush[lineOfCluster[i]] += box.width * (scaleXs[i] - 1f)
            }

            // ---- draw -----------------------------------------------------------
            val drawnPush = FloatArray(layout.lineCount)
            for (i in 0 until clusterCount) {
                val box = bounds[i] ?: continue
                val lineIndex = lineOfCluster[i]
                val alignShift = when (textAlign) {
                    TextAlign.Center -> -linePush[lineIndex] / 2f
                    TextAlign.End, TextAlign.Right -> -linePush[lineIndex]
                    else -> 0f
                }
                val w = wordOfCluster[i]
                val sungNow = w != -1 && sung[w]
                val wave = if (w != -1) {
                    val group = groups[w]
                    if (group != null) {
                        // A slow ripple travels the hyphen group while it sings.
                        val nowSeconds = (position / 1000f)
                        val fadeMs = 200f / motion
                        val inGroup = ((position - group.startMs).toFloat() / fadeMs).coerceIn(0f, 1f)
                        val toEnd = ((group.endMs - position).toFloat() / fadeMs).coerceIn(0f, 1f)
                        val fade = inGroup * toEnd
                        if (fade > 0.01f) {
                            sin((nowSeconds * 6f + i * 0.4f).toDouble()).toFloat() * 3.24f * fade
                        } else {
                            0f
                        }
                    } else {
                        0f
                    }
                } else {
                    0f
                }

                withTransform({
                    translate(left = alignShift + drawnPush[lineIndex] + box.left, top = box.top + wave)
                    if (w != -1) {
                        scale(scaleXs[i], scaleYs[i], pivot = Offset(box.width / 2f, box.height))
                    }
                }) {
                    if (w != -1 && !sungNow && sungFactor[w] > 0.001f) {
                        drawImpactGlow(
                            letter = letters[i],
                            word = segments.words[w],
                            factor = sungFactor[w],
                            accent = accent,
                            radiusPx = 12.dp.toPx(),
                        )
                    }
                    val alpha = if (w == -1) {
                        focusedAlpha
                    } else if (sungNow || charProgress[i] > 0.99f) {
                        1f
                    } else {
                        focusedAlpha + (1f - focusedAlpha) * sungFactor[w]
                    }
                    drawText(letters[i], topLeft = Offset.Zero, color = accent.copy(alpha = alpha))

                    // The per-character fill: a solid head and a feathered edge,
                    // exactly like the mobile twelve-step ramp.
                    val lp = charProgress[i]
                    if (w != -1 && !sungNow && lp > 0f && lp < 1f) {
                        val front = box.width * lp
                        val edge = (box.width * 0.45f).coerceAtLeast(1f)
                        val solidEnd = (front - edge).coerceAtLeast(0f)
                        if (solidEnd > 0f) {
                            clipRect(left = 0f, top = 0f, right = solidEnd, bottom = box.height) {
                                drawText(letters[i], topLeft = Offset.Zero, color = accent)
                            }
                        }
                        for (j in 0 until 12) {
                            val start = solidEnd + (j * edge / 12f)
                            val end = (solidEnd + ((j + 1) * edge / 12f) + 0.5f).coerceAtMost(front)
                            if (end > start) {
                                clipRect(left = start, top = 0f, right = end, bottom = box.height) {
                                    drawText(
                                        letters[i],
                                        topLeft = Offset.Zero,
                                        color = accent.copy(alpha = 1f - (j + 0.5f) / 12f),
                                    )
                                }
                            }
                        }
                    }
                }
                drawnPush[lineIndex] += box.width * (scaleXs[i] - 1f)
            }
        }
    }
}

/** The per-character nudge strength of the Metro style. */
private const val NUDGE_STRENGTH = 0.038f

/** A word as the Metro canvas needs it (all times in milliseconds). */
private data class FlowWord(
    val text: String,
    val startMs: Long,
    val endMs: Long,
    val hasTrailingSpace: Boolean,
)

/** The result of splitting hyphenated words. */
private data class HyphenSplit(
    val words: List<FlowWord>,
    /** Index into [words] for every original word, after the split. */
    val toOriginal: IntArray,
)

/**
 * Mobile's hyphen split: a word containing `-` is sung one segment per hyphen
 * (`go-o-o-o`), each getting an equal share of the word's own duration. Words
 * that carry a trailing space are left alone unless the line is a single word —
 * that is the mobile rule, and it is what stops a normal `well-known` from
 * being sung as two separate notes in the middle of a sentence.
 */
private fun splitHyphenSegments(words: List<FlowWord>): HyphenSplit {
    val out = mutableListOf<FlowWord>()
    val toOriginal = mutableListOf<Int>()
    words.forEachIndexed { index, word ->
        val split = word.text.contains('-') && word.text.length > 1 &&
            (!word.hasTrailingSpace || words.size == 1)
        if (!split) {
            out.add(word)
            toOriginal.add(index)
            return@forEachIndexed
        }
        val segments = mutableListOf<String>()
        var start = 0
        for (i in word.text.indices) {
            if (word.text[i] == '-') {
                segments.add(word.text.substring(start, i + 1))
                start = i + 1
            }
        }
        if (start < word.text.length) segments.add(word.text.substring(start))
        if (segments.size <= 1) {
            out.add(word)
            toOriginal.add(index)
            return@forEachIndexed
        }
        val total = (word.endMs - word.startMs).coerceAtLeast(1L)
        val each = total / segments.size
        segments.forEachIndexed { segmentIndex, segmentText ->
            out.add(
                FlowWord(
                    text = segmentText,
                    startMs = word.startMs + segmentIndex * each,
                    endMs = word.startMs + (segmentIndex + 1) * each,
                    hasTrailingSpace = segmentIndex == segments.size - 1 && word.hasTrailingSpace,
                ),
            )
            toOriginal.add(index)
        }
    }
    return HyphenSplit(out, toOriginal.toIntArray())
}

/**
 * A word of the hyphen group the character belongs to: the group is a run of
 * consecutive words each ending with `-` (the last one closes it).
 */
private data class HyphenGroup(val position: Int, val isLast: Boolean, val startMs: Long, val endMs: Long)

private fun hyphenGroups(words: List<FlowWord>): Map<Int, HyphenGroup> {
    val map = mutableMapOf<Int, HyphenGroup>()
    var current = mutableListOf<Int>()
    words.forEachIndexed { index, word ->
        current.add(index)
        if (!word.text.endsWith("-")) {
            if (current.size > 1) {
                val startMs = words[current.first()].startMs
                val endMs = word.endMs
                current.forEachIndexed { pos, idx ->
                    map[idx] = HyphenGroup(pos, pos == current.size - 1, startMs, endMs)
                }
            }
            current = mutableListOf()
        }
    }
    return map
}

/**
 * The hyphen crescendo: each segment of the group keeps a small scale by
 * position, the last one springs in while the group is sung, and the whole
 * group springs back out over 600 ms afterwards (a decaying cosine).
 */
private fun crescendoDelta(group: HyphenGroup?, sungFactor: Float, position: Long, motion: Float): Float {
    if (group == null) return 0f
    val base = group.position * BASE_SCALE_PER_SEGMENT
    val timeSinceEnd = (position - group.endMs).toFloat()
    val out = (timeSinceEnd / (EXIT_DURATION_MS / motion)).coerceIn(0f, 1f)
    return if (out > 0f) {
        val total = base + PEAK_SCALE
        total * exp(-DECAY * out) * cos((FREQ * out * PI).toDouble()).toFloat() * (1f - out)
    } else if (group.isLast) {
        base + PEAK_SCALE * (1f - exp(-DECAY * sungFactor) * cos((FREQ * sungFactor * PI).toDouble()).toFloat() * (1f - sungFactor))
    } else {
        base + if (sungFactor > 0f) 0.02f * (1f - sungFactor) else 0f
    }
}

private const val BASE_SCALE_PER_SEGMENT = 0.012f
private const val PEAK_SCALE = 0.06f
private const val DECAY = 3.5f
private const val FREQ = 5.0f
private const val EXIT_DURATION_MS = 600f

/** The landing-bounce window of the Metro style: 125 ms up, then 625 ms down. */
private const val WOBBLE_WINDOW_MS = 750f
private const val WOBBLE_RISE_MS = 125f

/** Where every grapheme sits inside its word. */
private data class ClusterWords(
    val word: IntArray,
    val charInWord: IntArray,
    val wordLength: IntArray,
    /** Character offset of each grapheme in the whole line. */
    val offsets: IntArray,
)

/**
 * Maps the line's graphemes onto the words that produced them: the word is
 * located in the line text, and the characters of the space that follows it
 * belong to that same word (mobile's rule, so the gap between two words fills
 * together with the word before it).
 */
private fun mapClustersToWords(graphemes: List<String>, words: List<FlowWord>, text: String): ClusterWords {
    val count = graphemes.size
    val wordOf = IntArray(count) { -1 }
    val charInWord = IntArray(count)
    val wordLength = IntArray(count) { 1 }
    val offsets = IntArray(count)
    var charOffset = 0
    graphemes.forEachIndexed { index, grapheme ->
        offsets[index] = charOffset
        charOffset += grapheme.length
    }

    var searchFrom = 0
    var cursor = 0
    words.forEachIndexed { wordIndex, word ->
        val found = text.indexOf(word.text, searchFrom)
        if (found == -1) return@forEachIndexed
        val wordEnd = found + word.text.length
        while (cursor < count && offsets[cursor] < found) cursor++
        val members = mutableListOf<Int>()
        while (cursor < count && offsets[cursor] < wordEnd) {
            members.add(cursor)
            cursor++
        }
        members.forEachIndexed { positionInWord, cluster ->
            wordOf[cluster] = wordIndex
            charInWord[cluster] = positionInWord
            wordLength[cluster] = members.size
        }
        if (cursor < count && offsets[cursor] == wordEnd && wordEnd < text.length && text[wordEnd] == ' ') {
            wordOf[cursor] = wordIndex
            charInWord[cursor] = members.size
            wordLength[cursor] = members.size + 1
            cursor++
        }
        searchFrom = wordEnd
    }
    return ClusterWords(wordOf, charInWord, wordLength, offsets)
}

/**
 * The character clusters of a line, via the JVM's own grapheme iterator (the
 * same `BreakIterator` the mobile helper wraps). Animating whole graphemes —
 * not chars — is what keeps a Devanagari or an emoji line from tearing apart.
 */
private fun String.toGraphemeClusters(): List<String> {
    if (isEmpty()) return emptyList()
    val result = mutableListOf<String>()
    val iterator = java.text.BreakIterator.getCharacterInstance()
    iterator.setText(this)
    var start = iterator.first()
    var end = iterator.next()
    while (end != java.text.BreakIterator.DONE) {
        result.add(substring(start, end))
        start = end
        end = iterator.next()
    }
    return result
}

/**
 * The Metro impact glow behind the word being sung: the same strength curve the
 * mobile style computes for its `BlurMaskFilter`, drawn here as offset copies
 * of the glyph instead of a masked blur (no portable blur mask inside a
 * `DrawScope`).
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawImpactGlow(
    letter: androidx.compose.ui.text.TextLayoutResult,
    word: FlowWord,
    factor: Float,
    accent: Color,
    radiusPx: Float,
) {
    val duration = (word.endMs - word.startMs).toFloat()
    val letters = word.text.length.coerceAtLeast(1)
    val impactRatio = duration / letters
    val fade = (factor * 5f).coerceIn(0f, 1f) * ((1f - factor) * 8f).coerceIn(0f, 1f)
    val impact = (
        ((impactRatio - 100f) / 250f).coerceIn(0f, 1f) * 0.6f +
            ((duration - 300f) / 1500f).coerceIn(0f, 1f) * 0.4f
        ).coerceIn(0f, 1f) * fade
    if (impact <= 0.01f) return
    val alpha = (0.35f * impact).coerceIn(0f, 0.4f)
    val radius = radiusPx * impact
    val taps = 8
    for (tap in 0 until taps) {
        val angle = tap * (2f * PI.toFloat() / taps)
        translate(left = cos(angle) * radius, top = sin(angle) * radius) {
            drawText(letter, topLeft = Offset.Zero, color = accent.copy(alpha = alpha / taps))
        }
    }
}
