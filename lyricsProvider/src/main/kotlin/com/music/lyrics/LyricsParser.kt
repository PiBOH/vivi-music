package com.music.lyrics

/**
 * Lyrics model + parser shared by the desktop renderer.
 *
 * The desktop edition used to strip every `<mm:ss.xx>` word tag and keep only
 * the line timestamps, which made the whole word-by-word (karaoke) family of
 * animations impossible. This parser is a JVM port of the mobile app's
 * `LyricsUtils`: it understands every format the provider chain can return and
 * keeps the word timings instead of throwing them away.
 *
 * Supported inputs:
 *  - classic synced LRC: `[mm:ss.xx] text`, with several tags on one line;
 *  - rich-sync: `[mm:ss.xx]<mm:ss.xx> word <mm:ss.xx> word …` (YouLyPlus and the
 *    community servers' word-level files);
 *  - the TTML-backed layout BetterLyrics emits, where each line is followed by
 *    a `word:start:end|word:start:end` list inside angle brackets;
 *  - `{agent:v1}` (which side of the screen the line belongs to) and `{bg}`
 *    (background vocal) markers, both used by the same sources.
 */
data class WordTimestamp(
    /** Text of the word, spaces excluded; the renderer joins them with " ". */
    val text: String,
    /** Start time in SECONDS (double precision, as the sources give it). */
    val startTime: Double,
    /** End time in SECONDS. */
    val endTime: Double,
)

/** One lyric line, with per-word timings when the source provides them. */
data class LyricLine(
    /** Line start time in milliseconds. */
    val timeMs: Long,
    val text: String,
    val words: List<WordTimestamp>? = null,
    /** `{agent:…}` value when the source marks which voice sings the line. */
    val agent: String? = null,
    /** `{bg}` line: a background vocal, drawn smaller and dimmer. */
    val isBackground: Boolean = false,
) : Comparable<LyricLine> {
    override fun compareTo(other: LyricLine): Int = timeMs.compareTo(other.timeMs)
}

object LyricsParser {
    /** `[mm:ss.xx] text` (one or more time tags in front of the text). */
    private val LINE_REGEX = Regex("""((\[\d{1,2}:\d{2}\.\d{2,3}] ?)+)(.+)""")

    /** A single `[mm:ss.xx]` tag. */
    private val TIME_REGEX = Regex("""\[(\d{1,2}):(\d{2})\.(\d{2,3})]""")

    /** Rich-sync line: `[mm:ss.xx]` followed by at least one `<mm:ss.xx>` tag. */
    private val RICH_SYNC_LINE_REGEX = Regex("""\[(\d{1,2}):(\d{2})\.(\d{2,3})](.+)""")
    private val RICH_SYNC_WORD_REGEX = Regex("""<(\d{1,2}):(\d{2})\.(\d{2,3})>\s*([^<]+)""")

    /** `{agent:v1}` — which voice the line belongs to. */
    private val AGENT_REGEX = Regex("""\{agent:([^}]+)}""")

    /** `{bg}` — background vocal. */
    private val BACKGROUND_REGEX = Regex("""\{bg}""")

    /**
     * Parses whatever the providers returned. Never throws: an unrecognised
     * text simply comes back as a single unsynced line, so the caller can fall
     * back to plain lyrics instead of showing nothing.
     */
    fun parse(raw: String): List<LyricLine> {
        val text = decodeHtmlEntities(
            raw.replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t"),
        )
        val lines = text.lines().filter { it.isNotBlank() && !it.trim().startsWith("[offset:") }

        val isRichSync = lines.any { line ->
            RICH_SYNC_LINE_REGEX.matches(line.trim()) && RICH_SYNC_WORD_REGEX.containsMatchIn(line)
        }

        return if (isRichSync) parseRichSync(lines) else parseStandard(lines)
    }

    /** True when [raw] carries any timestamp (line-level or word-level). */
    fun hasTimestamps(raw: String): Boolean =
        TIME_REGEX.containsMatchIn(raw) || RICH_SYNC_WORD_REGEX.containsMatchIn(raw)

    /** Index of the line being sung, or -1 before the first one. */
    fun currentLineIndex(lines: List<LyricLine>, positionMs: Long): Int {
        var index = -1
        for (i in lines.indices) {
            if (lines[i].timeMs <= positionMs) index = i else break
        }
        return index
    }

    // --- rich sync: [mm:ss.xx]<mm:ss.xx> word <mm:ss.xx> word … ---------------

    private fun parseRichSync(lines: List<String>): List<LyricLine> {
        val result = mutableListOf<LyricLine>()
        lines.forEachIndexed { index, line ->
            val match = RICH_SYNC_LINE_REGEX.matchEntire(line.trim()) ?: return@forEachIndexed
            val lineTimeMs = tagToMillis(match.groupValues[1], match.groupValues[2], match.groupValues[3])

            var content = match.groupValues[4].trimStart()
            val agentMatch = AGENT_REGEX.find(content)
            val agent = agentMatch?.groupValues?.get(1)
            if (agentMatch != null) content = content.replaceFirst(AGENT_REGEX, "")
            val isBackground = BACKGROUND_REGEX.containsMatchIn(content)
            if (isBackground) content = content.replaceFirst(BACKGROUND_REGEX, "")

            val words = parseRichSyncWords(content, index, lines)
            val plainText = content.replace(Regex("""<\d{1,2}:\d{2}\.\d{2,3}>\s*"""), "").trim()
            if (plainText.isNotBlank()) {
                result.add(
                    LyricLine(
                        timeMs = lineTimeMs,
                        text = plainText,
                        words = words,
                        agent = agent,
                        isBackground = isBackground,
                    ),
                )
            }
        }
        return result.sorted()
    }

    private fun parseRichSyncWords(
        content: String,
        currentIndex: Int,
        allLines: List<String>,
    ): List<WordTimestamp>? {
        val matches = RICH_SYNC_WORD_REGEX.findAll(content).toList()
        if (matches.isEmpty()) return null

        val timings = mutableListOf<WordTimestamp>()
        matches.forEachIndexed { index, match ->
            val startSeconds = tagToSeconds(match.groupValues[1], match.groupValues[2], match.groupValues[3])
            val wordText = match.groupValues[4].trim()
            // A word ends where the next one begins; the last word of a line
            // ends when the next LINE begins, or 500 ms later as a fallback.
            val endSeconds = if (index < matches.size - 1) {
                val next = matches[index + 1]
                tagToSeconds(next.groupValues[1], next.groupValues[2], next.groupValues[3])
            } else {
                nextLineStartSeconds(currentIndex, allLines) ?: (startSeconds + 0.5)
            }
            if (wordText.isNotBlank()) timings.add(WordTimestamp(wordText, startSeconds, endSeconds))
        }
        return timings.ifEmpty { null }
    }

    private fun nextLineStartSeconds(currentIndex: Int, allLines: List<String>): Double? {
        val next = allLines.getOrNull(currentIndex + 1)?.trim() ?: return null
        val match = RICH_SYNC_LINE_REGEX.matchEntire(next) ?: return null
        return tagToSeconds(match.groupValues[1], match.groupValues[2], match.groupValues[3])
    }

    // --- standard: [mm:ss.xx] text, optionally followed by a word list --------

    private fun parseStandard(lines: List<String>): List<LyricLine> {
        val result = mutableListOf<LyricLine>()
        lines.forEachIndexed { i, rawLine ->
            val line = rawLine.trim()
            // A `<word:start:end|…>` bookkeeping line belongs to the line above.
            if (line.startsWith("<") && line.endsWith(">")) return@forEachIndexed
            val entries = parseLine(rawLine) ?: return@forEachIndexed
            val words = lines.getOrNull(i + 1)
                ?.trim()
                ?.takeIf { it.startsWith("<") && it.endsWith(">") }
                ?.let { parseWordTimestamps(it.removeSurrounding("<", ">")) }
            result.addAll(entries.map { it.copy(words = words) })
        }
        return result.sorted()
    }

    /** `word:start:end|word:start:end` (BetterLyrics/TTML layout). */
    private fun parseWordTimestamps(data: String): List<WordTimestamp>? {
        if (data.isBlank()) return null
        return runCatching {
            data.split("|").mapNotNull { wordData ->
                val parts = wordData.split(":")
                if (parts.size == 3) {
                    val start = parts[1].toDoubleOrNull() ?: return@mapNotNull null
                    val end = parts[2].toDoubleOrNull() ?: return@mapNotNull null
                    WordTimestamp(parts[0], start, end)
                } else {
                    null
                }
            }
        }.getOrNull()?.ifEmpty { null }
    }

    private fun parseLine(line: String): List<LyricLine>? {
        if (line.isEmpty()) return null
        val match = LINE_REGEX.matchEntire(line.trim()) ?: return null
        var text = match.groupValues[3]

        val agentMatch = AGENT_REGEX.find(text)
        val agent = agentMatch?.groupValues?.get(1)
        if (agentMatch != null) text = text.replaceFirst(AGENT_REGEX, "")
        val isBackground = BACKGROUND_REGEX.containsMatchIn(text)
        if (isBackground) text = text.replaceFirst(BACKGROUND_REGEX, "")

        return TIME_REGEX.findAll(match.groupValues[1]).map { tag ->
            val milString = tag.groupValues[3]
            var mil = milString.toLongOrNull() ?: 0L
            if (milString.length == 2) mil *= 10
            val minutes = tag.groupValues[1].toLongOrNull() ?: 0L
            val seconds = tag.groupValues[2].toLongOrNull() ?: 0L
            LyricLine(
                timeMs = minutes * 60_000 + seconds * 1_000 + mil,
                text = text,
                agent = agent,
                isBackground = isBackground,
            )
        }.toList()
    }

    // --- helpers -------------------------------------------------------------

    /** `[mm:ss.f]` groups -> milliseconds (two-digit fractions are centiseconds). */
    private fun tagToMillis(minutes: String, seconds: String, fraction: String): Long {
        val millis = fractionToMillis(fraction)
        return (minutes.toLongOrNull() ?: 0L) * 60_000 +
            (seconds.toLongOrNull() ?: 0L) * 1_000 +
            millis
    }

    /** `<mm:ss.f>` groups -> seconds. */
    private fun tagToSeconds(minutes: String, seconds: String, fraction: String): Double =
        (minutes.toLongOrNull() ?: 0L) * 60.0 +
            (seconds.toLongOrNull() ?: 0L) +
            fractionToMillis(fraction) / 1000.0

    private fun fractionToMillis(fraction: String): Long {
        val value = fraction.toLongOrNull() ?: 0L
        return if (fraction.length == 3) value else value * 10
    }

    /**
     * Providers hand back HTML-escaped text (`&#39;`, `&amp;`, …). Only the
     * entities that actually show up in lyrics are decoded, plus numeric
     * forms, so a title like `Don&#39;t` is not displayed raw.
     */
    internal fun decodeHtmlEntities(text: String): String {
        if (!text.contains('&')) return text
        return Regex("""&(#x?[0-9a-fA-F]+|[a-zA-Z]+);""").replace(text) { match ->
            val entity = match.groupValues[1]
            when {
                entity.startsWith("#x") || entity.startsWith("#X") ->
                    entity.drop(2).toIntOrNull(16)?.let { codePointToString(it) } ?: match.value

                entity.startsWith("#") ->
                    entity.drop(1).toIntOrNull()?.let { codePointToString(it) } ?: match.value

                else -> NAMED_ENTITIES[entity] ?: match.value
            }
        }
    }

    private fun codePointToString(codePoint: Int): String? =
        if (Character.isValidCodePoint(codePoint)) String(Character.toChars(codePoint)) else null

    private val NAMED_ENTITIES = mapOf(
        "amp" to "&",
        "lt" to "<",
        "gt" to ">",
        "quot" to "\"",
        "apos" to "'",
        "nbsp" to "\u00A0",
        "hellip" to "…",
        "ndash" to "–",
        "mdash" to "—",
        "lsquo" to "\u2018",
        "rsquo" to "\u2019",
        "ldquo" to "\u201C",
        "rdquo" to "\u201D",
        "deg" to "°",
        "laquo" to "«",
        "raquo" to "»",
    )
}
