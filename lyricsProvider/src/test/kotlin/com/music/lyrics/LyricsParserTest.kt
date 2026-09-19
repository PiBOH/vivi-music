package com.music.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The parser has to keep the word timings the providers send, because the
 * whole karaoke family of desktop lyric animations is driven by them. It also
 * has to accept every layout the provider chain can produce: the desktop build
 * used to strip those tags and fall back to line-by-line plain text.
 */
class LyricsParserTest {

    @Test
    fun `parses standard synced lrc, both fraction widths`() {
        val lines = LyricsParser.parse(
            "[00:12.50]first line\n" +
                "[00:15.250]second line",
        )
        assertEquals(2, lines.size)
        // ".50" is centiseconds -> 500 ms; ".250" is already milliseconds.
        assertEquals(12_500L, lines[0].timeMs)
        assertEquals("first line", lines[0].text)
        assertEquals(15_250L, lines[1].timeMs)
        assertNull(lines[0].words)
    }

    @Test
    fun `one line with several tags becomes several entries`() {
        val lines = LyricsParser.parse("[00:01.00][00:03.00]repeated chorus")
        assertEquals(2, lines.size)
        assertEquals(listOf(1_000L, 3_000L), lines.map { it.timeMs })
        assertEquals(listOf("repeated chorus", "repeated chorus"), lines.map { it.text })
    }

    @Test
    fun `parses rich sync word timings`() {
        val lines = LyricsParser.parse(
            "[00:10.00]<00:10.00>Hello <00:10.50>world <00:11.00>again\n" +
                "[00:12.00]plain next line",
        )
        assertEquals(2, lines.size)
        assertNotNull(lines[0].words)
        val words = requireNotNull(lines[0].words)
        assertEquals(listOf("Hello", "world", "again"), words.map { it.text })
        assertEquals(10.0, words[0].startTime, 0.0001)
        assertEquals(10.5, words[1].startTime, 0.0001)
        assertEquals(11.0, words[2].startTime, 0.0001)
        // Every word ends where the next one begins, the last at the next line.
        assertEquals(10.5, words[0].endTime, 0.0001)
        assertEquals(11.0, words[1].endTime, 0.0001)
        assertEquals(12.0, words[2].endTime, 0.0001)
        assertEquals("Hello world again", lines[0].text)
        assertNull(lines[1].words)
    }

    @Test
    fun `parses the word-list layout emitted by the TTML provider`() {
        val lines = LyricsParser.parse(
            "[00:05.20]Hold me now\n" +
                "<Hold:5.2:6.0|me:6.0:6.4|now:6.4:7.1>",
        )
        assertEquals(1, lines.size)
        assertNotNull(lines[0].words)
        val words = requireNotNull(lines[0].words)
        assertEquals(listOf("Hold", "me", "now"), words.map { it.text })
        assertEquals(5.2, words[0].startTime, 0.0001)
        assertEquals(7.1, words[2].endTime, 0.0001)
        // The bookkeeping line must not become a lyric line of its own.
        assertEquals("Hold me now", lines[0].text)
    }

    @Test
    fun `keeps agent and background markers out of the text`() {
        val lines = LyricsParser.parse(
            "[00:01.00]{agent:v1}main voice\n" +
                "[00:02.00]{bg}background voice",
        )
        assertEquals("main voice", lines[0].text)
        assertEquals("v1", lines[0].agent)
        assertTrue(!lines[0].isBackground)
        assertEquals("background voice", lines[1].text)
        assertNull(lines[1].agent)
        assertTrue(lines[1].isBackground)
    }

    @Test
    fun `drops offset lines and sorts by time`() {
        val lines = LyricsParser.parse(
            "[offset:-500]\n[00:30.00]later\n[00:10.00]earlier",
        )
        assertEquals(listOf("earlier", "later"), lines.map { it.text })
    }

    @Test
    fun `decodes html entities`() {
        assertEquals("Don't stop & go", LyricsParser.decodeHtmlEntities("Don&#39;t stop &amp; go"))
        assertEquals("A — B", LyricsParser.decodeHtmlEntities("A &#8212; B"))
        assertEquals("A — B", LyricsParser.decodeHtmlEntities("A &#x2014; B"))
        // An unknown entity is left alone rather than mangled.
        assertEquals("&weird;", LyricsParser.decodeHtmlEntities("&weird;"))
    }

    @Test
    fun `current line index walks the list`() {
        val lines = LyricsParser.parse("[00:01.00]a\n[00:02.00]b\n[00:03.00]c")
        assertEquals(-1, LyricsParser.currentLineIndex(lines, 500))
        assertEquals(0, LyricsParser.currentLineIndex(lines, 1_000))
        assertEquals(1, LyricsParser.currentLineIndex(lines, 2_500))
        assertEquals(2, LyricsParser.currentLineIndex(lines, 99_000))
    }

    @Test
    fun `unsynced text yields no lines instead of throwing`() {
        assertTrue(LyricsParser.parse("just some plain lyrics\nwithout timestamps").isEmpty())
        assertTrue(!LyricsParser.hasTimestamps("plain text"))
        assertTrue(LyricsParser.hasTimestamps("[00:01.00]synced"))
    }
}
