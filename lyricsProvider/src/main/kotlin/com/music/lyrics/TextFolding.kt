package com.music.lyrics

import java.text.Normalizer

/**
 * Folds the decorative Unicode that YouTube Music titles/artists are full of
 * (`ＭＩＧＵＥＬ 𝑷𝒉𝒐𝒏𝒌`, `𝗖𝗥𝗢𝗪𝗡 𝗕𝗘𝗔𝗥`, curly quotes, en-dashes) down to
 * plain text.
 *
 * Why it matters: every lyrics provider matches the request against *its own*
 * plain-text catalogue. A stylized title was therefore unmatchable —
 * `tokens("ＭＩＧＵＥＬ 𝑷𝒉𝒐𝒏𝒌")` and a candidate named `Miguel Phonk` share
 * no token, so the strict KuGou title/artist check rejected the correct
 * candidate (and the other providers' queries were equally unmatchable), which
 * is why those tracks ended up with no lyrics at all — or, before the strict
 * check, with somebody else's.
 *
 * NFKD is what does the heavy lifting: it decomposes fullwidth/compatibility
 * forms (ＦＵＬＬ → FULL) and the mathematical alphanumeric alphabets
 * (𝐁𝐥𝐮𝐞 → Blue) into their ASCII bases. Combining marks are then dropped
 * (`Café` → `Cafe`), and the typographic quotes/dashes that providers spell
 * with ASCII are normalized so the *token* is identical on both sides.
 *
 * The original string is still what gets displayed and logged: folding is only
 * for matching and for the text sent to a search endpoint.
 */
object TextFolding {

    /** Combining diacritical marks left behind by NFKD (`e` + U+0301). */
    private val COMBINING_MARKS = Regex("[\\p{Mn}\\p{Me}]")

    /**
     * Invisible characters that videos carry inside their titles: the reported
     * one was `ＭＩＧＵＥＬ\uFEFF 𝑷𝒉𝒐𝒏𝒌` — a zero-width no-break space between
     * the words. Left in place they act as word separators, so a title with one
     * inside a word (`N\uFEFFo\uFEFFi\uFEFFs\uFEFFe`) tokenizes into single
     * letters, all of them discarded as too short, and the track again matches
     * nothing.
     */
    private val INVISIBLE = Regex("[\\u200B-\\u200F\\u202A-\\u202E\\u2060-\\u2064\\uFEFF]")

    private val TYPOGRAPHIC_QUOTES = Regex("[\\u2018\\u2019\\u201A\\u201B\\u2032\\u02BC]")
    private val TYPOGRAPHIC_DASHES = Regex("[\\u2010-\\u2015\\u2212]")

    /** Collapses the whitespace NFKD can leave behind, so tokens don't split oddly. */
    private val WHITESPACE = Regex("\\s+")

    fun fold(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFKD)
            .replace(INVISIBLE, "")
            .replace(COMBINING_MARKS, "")
            .replace(TYPOGRAPHIC_QUOTES, "'")
            .replace(TYPOGRAPHIC_DASHES, "-")
            .replace(WHITESPACE, " ")
            .trim()
}
