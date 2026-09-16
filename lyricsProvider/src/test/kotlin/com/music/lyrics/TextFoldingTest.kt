package com.music.lyrics

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The folding exists to make a stylized YouTube Music title comparable with a
 * provider's plain-text catalogue entry: without it the two share no token and
 * the correct lyrics are rejected (the reported "no lyrics / wrong lyrics").
 *
 * The strings below are copied out of the reported log, code point for code
 * point (including the invisible characters YouTube Music puts in the title).
 */
class TextFoldingTest {

    /** Title of video x8-Wpxu7hag, exactly as the log recorded it. */
    private val stylizedTitle =
        "\uFEFF\uFF2D\uFF29\uFF27\uFF35\uFF25\uFF2C\uFEFF\u0020\uD835\uDC77\uD835\uDC89\uD835\uDC90\uD835\uDC8F\uD835\uDC8C\uFEFF\u0020\u0028\uD835\uDD4A\uD835\uDD43\uD835\uDD46\uD835\uDD4E\uD835\uDD3C\uD835\uDD3B\u0029"

    /** Artist of the same video. */
    private val stylizedArtist =
        "\uD835\uDDD6\uD835\uDDE5\uD835\uDDE2\uD835\uDDEA\uD835\uDDE1\u0020\uD835\uDDD5\uD835\uDDD8\uD835\uDDD4\uD835\uDDE5"

    @Test
    fun `folds the fullwidth and mathematical letters of a real title`() {
        assertEquals("MIGUEL Phonk (SLOWED)", TextFolding.fold(stylizedTitle))
    }

    @Test
    fun `folds the mathematical sans-serif alphabet used by artists`() {
        assertEquals("CROWN BEAR", TextFolding.fold(stylizedArtist))
    }

    @Test
    fun `drops invisible characters instead of letting them split words`() {
        assertEquals("Noise", TextFolding.fold("N\uFEFFo\u200Bi\u200Ds\u2060e"))
    }

    @Test
    fun `drops accents and normalizes typographic punctuation`() {
        assertEquals("Cafe Tacvba", TextFolding.fold("Café Tacvba"))
        assertEquals("Don't Stop - Live", TextFolding.fold("Don\u2019t Stop \u2013 Live"))
    }

    @Test
    fun `keeps the plain text untouched`() {
        assertEquals("Blue (Da Ba Dee)", TextFolding.fold("Blue (Da Ba Dee)"))
    }
}
