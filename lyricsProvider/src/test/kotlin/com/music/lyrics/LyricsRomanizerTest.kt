package com.music.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsRomanizerTest {

    @Test
    fun `katakana digraphs and sokuon`() {
        // トウキョウ = to-u-kyo-u (the digraph キョ is read as one unit).
        assertEquals("toukyou", LyricsRomanizer.romanizeJapanese("トウキョウ"))
        assertEquals("sha", LyricsRomanizer.romanizeJapanese("シャ"))
        // ッ doubles the consonant that follows.
        assertEquals("kitte", LyricsRomanizer.romanizeJapanese("キッテ"))
    }

    @Test
    fun `korean syllables are decomposed`() {
        assertEquals("hanguk", LyricsRomanizer.romanizeKorean("한국"))
        assertEquals("annyeong", LyricsRomanizer.romanizeKorean("안녕"))
    }

    @Test
    fun `russian uses the general cyrillic table`() {
        assertEquals("privet", LyricsRomanizer.romanizeCyrillic("привет"))
        // "ого"/"его" are Russian exceptions (ovo/evo), not ogo/ego.
        assertEquals("evo", LyricsRomanizer.romanizeCyrillic("его"))
    }

    @Test
    fun `ukrainian г is romanized as h`() {
        // Russian is tested first (as on mobile) and only claims text without
        // Ukrainian-only letters, so г becomes h as soon as an і/ї/є/ґ shows up.
        assertEquals("hirka", LyricsRomanizer.romanizeCyrillic("гірка"))
        assertEquals("yizhak", LyricsRomanizer.romanizeCyrillic("їжак"))
    }

    @Test
    fun `serbian specific letters pick the serbian table`() {
        assertEquals("ljub", LyricsRomanizer.romanizeCyrillic("љуб"))
        assertEquals("zdravo", LyricsRomanizer.romanizeCyrillic("здраво"))
    }

    @Test
    fun `hindi and punjabi tables with the inherent vowel`() {
        assertEquals("namaste", LyricsRomanizer.romanize("नमस्ते"))
        assertEquals("sati", LyricsRomanizer.romanize("ਸਤਿ"))
    }

    @Test
    fun `latin text is returned unchanged`() {
        assertEquals("hello world", LyricsRomanizer.romanize("hello world"))
    }

    @Test
    fun `disabled scripts are left alone`() {
        val off = LyricsRomanizer.Options(korean = false, russian = false)
        assertEquals("안녕", LyricsRomanizer.romanize("안녕", off))
        assertEquals("привет", LyricsRomanizer.romanize("привет", off))
    }

    @Test
    fun `cyrillic languages are gated one by one`() {
        // Ukrainian off (while Russian stays on) leaves a Ukrainian line alone.
        val ukrainianOff = LyricsRomanizer.Options(ukrainian = false)
        assertEquals("їжак", LyricsRomanizer.romanize("їжак", ukrainianOff))
        // ...and Russian text is still romanized.
        assertEquals("privet", LyricsRomanizer.romanize("привет", ukrainianOff))
        assertEquals(
            LyricsRomanizer.CyrillicLanguage.UKRAINIAN,
            LyricsRomanizer.detectCyrillicLanguage("їжак"),
        )
        assertEquals(
            LyricsRomanizer.CyrillicLanguage.RUSSIAN,
            LyricsRomanizer.detectCyrillicLanguage("привет"),
        )
    }

    @Test
    fun `a lone cyrillic e is not treated as cyrillic text`() {
        assertNull(LyricsRomanizer.romanizeCyrillic("e"))
        assertFalse(LyricsRomanizer.containsCyrillic("hello"))
    }

    @Test
    fun `detection helpers`() {
        assertTrue(LyricsRomanizer.containsKana("カタカナ"))
        assertTrue(LyricsRomanizer.containsHangul("한국"))
        assertTrue(LyricsRomanizer.containsDevanagari("नमस्ते"))
        assertTrue(LyricsRomanizer.containsGurmukhi("ਸਤਿ"))
        assertTrue(LyricsRomanizer.containsHan("漢字"))
    }
}
