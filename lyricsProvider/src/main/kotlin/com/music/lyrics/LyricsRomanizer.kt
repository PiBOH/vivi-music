package com.music.lyrics

/**
 * Script romanization for the desktop lyrics view.
 *
 * Port of the mobile app's `LyricsUtils` romanization half, with the same
 * tables, so a line that reads comfortably on the phone reads the same way on
 * the desktop. Two mobile helpers cannot exist here and degrade to a
 * pass-through instead of lying:
 *
 *  - the Japanese dictionary (kuromoji) that turns kanji into a reading: with
 *    no JVM dictionary bundled, only kana text is romanized, while kanji is
 *    copied as-is (the toggle still works, it simply has nothing to convert);
 *  - the Chinese pinyin dictionary (`Pinyin`): same reasoning, hanzi is
 *    copied as-is.
 *
 * Everything else is exact: katakana (including the sokuon ッ), Hangul (full
 * syllable decomposition with the final-consonant assimilation tables), the
 * seven Cyrillic orthographies mobile detects (Russian, Ukrainian, Serbian,
 * Bulgarian, Belarusian, Kyrgyz, Macedonian), Devanagari and Gurmukhi.
 */
object LyricsRomanizer {

    /** Which traditions the caller wants romanized (one flag per APK toggle). */
    data class Options(
        val japanese: Boolean = true,
        val korean: Boolean = true,
        val chinese: Boolean = true,
        val russian: Boolean = true,
        val ukrainian: Boolean = true,
        val serbian: Boolean = true,
        val bulgarian: Boolean = true,
        val belarusian: Boolean = true,
        val kyrgyz: Boolean = true,
        val macedonian: Boolean = true,
        val hindi: Boolean = true,
        val punjabi: Boolean = true,
    ) {
        fun enabled(language: CyrillicLanguage): Boolean = when (language) {
            CyrillicLanguage.RUSSIAN -> russian
            CyrillicLanguage.UKRAINIAN -> ukrainian
            CyrillicLanguage.SERBIAN -> serbian
            CyrillicLanguage.BULGARIAN -> bulgarian
            CyrillicLanguage.BELARUSIAN -> belarusian
            CyrillicLanguage.KYRGYZ -> kyrgyz
            CyrillicLanguage.MACEDONIAN -> macedonian
        }
    }

    /** The Cyrillic orthographies the mobile app lets the user toggle apart. */
    enum class CyrillicLanguage { RUSSIAN, UKRAINIAN, SERBIAN, BULGARIAN, BELARUSIAN, KYRGYZ, MACEDONIAN }

    /**
     * Romanizes [text] when it contains a script the caller enabled, or returns
     * the text unchanged when there is nothing to do. Never throws.
     */
    fun romanize(text: String, options: Options = Options()): String {
        if (text.isBlank()) return text
        return runCatching {
            when {
                options.korean && containsHangul(text) -> romanizeKorean(text)
                options.japanese && containsKana(text) -> romanizeJapanese(text)
                options.chinese && containsHan(text) -> text // no dictionary: pass-through
                options.hindi && containsDevanagari(text) -> romanizeByTable(text, DEVANAGARI_ROMAJI_MAP)
                options.punjabi && containsGurmukhi(text) -> romanizeByTable(text, GURMUKHI_ROMAJI_MAP)
                containsCyrillic(text) -> {
                    // The orthography is detected first: the user can keep, say,
                    // Russian on and Serbian off, and each line is then decided
                    // by the language it actually looks like.
                    val language = detectCyrillicLanguage(text)
                    if (language != null && options.enabled(language)) romanizeCyrillic(text) ?: text else text
                }
                else -> text
            }
        }.getOrDefault(text)
    }

    /** Which orthography [text] looks like, or null when it is not Cyrillic. */
    fun detectCyrillicLanguage(text: String): CyrillicLanguage? {
        if (!containsCyrillic(text)) return null
        val cyrillic = text.filter { it in '\u0400'..'\u04FF' }
        if (cyrillic.length == 1 && (cyrillic[0] == 'е' || cyrillic[0] == 'Е')) return null
        // Same order as the mobile app: Russian first, because the other letter
        // sets accept most Russian text too.
        return when {
            isRussian(text) -> CyrillicLanguage.RUSSIAN
            isUkrainian(text) -> CyrillicLanguage.UKRAINIAN
            isSerbian(text) -> CyrillicLanguage.SERBIAN
            isBulgarian(text) -> CyrillicLanguage.BULGARIAN
            isBelarusian(text) -> CyrillicLanguage.BELARUSIAN
            isKyrgyz(text) -> CyrillicLanguage.KYRGYZ
            isMacedonian(text) -> CyrillicLanguage.MACEDONIAN
            else -> null
        }
    }

    // --- script detection ----------------------------------------------------

    fun containsHangul(text: String) = text.any { it in '\uAC00'..'\uD7A3' || it in '\u1100'..'\u11FF' }

    fun containsKana(text: String) =
        text.any { it in '\u3040'..'\u309F' || it in '\u30A0'..'\u30FF' }

    fun containsHan(text: String) = text.any { it in '\u4E00'..'\u9FFF' }

    fun containsDevanagari(text: String) = text.any { it in '\u0900'..'\u097F' }

    fun containsGurmukhi(text: String) = text.any { it in '\u0A00'..'\u0A7F' }

    fun containsCyrillic(text: String) = text.any { it in '\u0400'..'\u04FF' }

    // --- Japanese ------------------------------------------------------------

    /** Katakana -> romaji (in katakana order); the input is lowercased. */
    fun romanizeJapanese(text: String): String {
        val out = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            val two = if (i + 1 < text.length) text.substring(i, i + 2) else null
            val mappedTwo = two?.let { KANA_ROMAJI_MAP[it] }
            when {
                mappedTwo != null -> {
                    out.append(mappedTwo); i += 2
                }
                text[i] == 'ッ' -> {
                    // Sokuon: double the next consonant.
                    val next = text.getOrNull(i + 1)
                    val nextRomaji = next?.let { KANA_ROMAJI_MAP[it.toString()] }?.firstOrNull()
                    out.append((nextRomaji ?: next)?.toString()?.lowercase()?.trim() ?: "")
                    i += 1
                }
                else -> {
                    out.append(KANA_ROMAJI_MAP[text[i].toString()] ?: text[i])
                    i += 1
                }
            }
        }
        return out.toString().lowercase()
    }

    /** Katakana, kept under its mobile name for callers porting that code. */
    fun katakanaToRomaji(katakana: String?): String = if (katakana.isNullOrEmpty()) "" else romanizeJapanese(katakana)

    // --- Korean --------------------------------------------------------------

    fun romanizeKorean(text: String): String {
        val out = StringBuilder()
        var prevFinal: String? = null
        for (char in text) {
            if (char in '\uAC00'..'\uD7A3') {
                val syllable = char.code - 0xAC00
                val choIndex = syllable / (21 * 28)
                val jungIndex = (syllable % (21 * 28)) / 28
                val jongIndex = syllable % 28

                val choChar = (0x1100 + choIndex).toChar().toString()
                val jungChar = (0x1161 + jungIndex).toChar().toString()
                val jongChar = if (jongIndex == 0) null else (0x11A7 + jongIndex).toChar().toString()

                // Settle the previous syllable's final consonant, which may
                // change depending on the consonant that starts this one.
                if (prevFinal != null) {
                    val context = prevFinal + choChar
                    out.append(HANGUL_JONG[context] ?: HANGUL_JONG[prevFinal] ?: prevFinal)
                }
                out.append(HANGUL_CHO[choChar] ?: choChar)
                out.append(HANGUL_JUNG[jungChar] ?: jungChar)
                prevFinal = jongChar
            } else {
                if (prevFinal != null) {
                    out.append(HANGUL_JONG[prevFinal] ?: prevFinal)
                    prevFinal = null
                }
                out.append(char)
            }
        }
        if (prevFinal != null) out.append(HANGUL_JONG[prevFinal] ?: prevFinal)
        return out.toString()
    }

    // --- Cyrillic ------------------------------------------------------------

    /**
     * Romanizes Cyrillic text after detecting its orthography, or null when
     * the text is not Cyrillic (or is too short to tell).
     */
    fun romanizeCyrillic(text: String): String? {
        if (text.isEmpty()) return null
        val cyrillic = text.filter { it in '\u0400'..'\u04FF' }
        // A lone 'е' is a false positive (it is also a Cyrillic letter used in
        // Latin-looking words); mobile ignores it the same way.
        if (cyrillic.isEmpty()) return null
        if (cyrillic.length == 1 && (cyrillic[0] == 'е' || cyrillic[0] == 'Е')) return null

        // Order matters and mirrors the mobile app: Russian is tested first,
        // because the Ukrainian/Bulgarian letter sets accept most Russian text
        // too and would otherwise claim every Russian line.
        return when {
            isRussian(text) -> romanizeWith(text, RUSSIAN_ROMAJI_MAP) { russianWord(it) }
            isUkrainian(text) -> romanizeWith(text, UKRAINIAN_ROMAJI_MAP) { ukrainianWord(it) }
            isSerbian(text) -> romanizeWith(text, SERBIAN_ROMAJI_MAP)
            isBulgarian(text) -> romanizeWith(text, BULGARIAN_ROMAJI_MAP)
            isBelarusian(text) -> romanizeWith(text, BELARUSIAN_ROMAJI_MAP)
            isKyrgyz(text) -> romanizeWith(text, KYRGYZ_ROMAJI_MAP)
            isMacedonian(text) -> romanizeWith(text, MACEDONIAN_ROMAJI_MAP)
            else -> romanizeWith(text, emptyMap())
        }
    }

    /** Splits on whitespace/punctuation, romanizes each word, keeps the rest. */
    private fun romanizeWith(
        text: String,
        languageMap: Map<String, String>,
        wordTransform: ((String) -> String)? = null,
    ): String {
        val parts = text.split(Regex("((?<=\\s|[.,!?;])|(?=\\s|[.,!?;]))")).filter { it.isNotEmpty() }
        val out = StringBuilder(text.length)
        for (part in parts) {
            out.append(
                when {
                    part.isBlank() || part.matches(Regex("[.,!?;]")) -> part
                    wordTransform != null -> wordTransform(part)
                    else -> mapChars(part, languageMap)
                },
            )
        }
        return out.toString()
    }

    /** Longest-match table lookup, three characters first (Russian `ого`…). */
    private fun mapChars(
        word: String,
        languageMap: Map<String, String>,
        wordStartE: Boolean = false,
    ): String {
        val out = StringBuilder(word.length)
        var i = 0
        while (i < word.length) {
            var consumed = false
            if (i + 2 < word.length) {
                val three = word.substring(i, i + 3)
                val mapped = languageMap[three]
                if (mapped != null) {
                    out.append(mapped); i += 3; consumed = true
                }
            }
            if (!consumed) {
                val char = word[i].toString()
                if (wordStartE && i == 0 && (char == "е" || char == "Е")) {
                    out.append(if (char == "е") "ye" else "Ye")
                } else {
                    out.append(languageMap[char] ?: GENERAL_CYRILLIC_ROMAJI_MAP[char] ?: char)
                }
                i += 1
            }
        }
        return out.toString()
    }

    /** Russian: `е`/`Е` at a word start is `ye`, the rest uses the general map. */
    private fun russianWord(word: String): String = mapChars(word, RUSSIAN_ROMAJI_MAP, wordStartE = true)

    /**
     * Ukrainian: a `ю`/`я` after a consonant becomes `iu`/`ia` (the mobile
     * table's only context-sensitive rule).
     */
    private fun ukrainianWord(word: String): String {
        val out = StringBuilder(word.length)
        word.forEachIndexed { index, char ->
            val charStr = char.toString()
            val afterConsonant = index > 0 && word[index - 1].isLetter() && !isCyrillicVowel(word[index - 1])
            val context = when {
                afterConsonant && charStr == "Ю" -> "Iu"
                afterConsonant && charStr == "ю" -> "iu"
                afterConsonant && charStr == "Я" -> "Ia"
                afterConsonant && charStr == "я" -> "ia"
                else -> null
            }
            out.append(context ?: UKRAINIAN_ROMAJI_MAP[charStr] ?: GENERAL_CYRILLIC_ROMAJI_MAP[charStr] ?: charStr)
        }
        return out.toString()
    }

    private fun isCyrillicVowel(char: Char) = "аеёиоуыэюяіїєАЕЁИОУЫЭЮЯІЇЄ".contains(char)

    // --- Devanagari / Gurmukhi ----------------------------------------------

    /**
     * Longest-match table romanization with the implicit vowel restored.
     *
     * Both Indic scripts write an inherent `a` after a consonant unless a vowel
     * sign (matra) or the virama follows it; the mobile table only lists
     * explicit letters, so reading it literally turns `नमस्ते` into `nmste`.
     * The inherent vowel is added back here, which is what makes Hindi and
     * Punjabi lyrics readable (a deliberate improvement over the raw table).
     */
    private fun romanizeByTable(text: String, table: Map<String, String>): String {
        val ordered = table.keys.sortedByDescending { it.length }
        val out = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            val match = ordered.firstOrNull { key -> key.isNotEmpty() && text.startsWith(key, i) }
            if (match != null) {
                out.append(table[match])
                i += match.length
                // A consonant with no vowel sign / virama after it takes the
                // inherent "a".
                if (isIndicConsonant(match[0]) && !isIndicVowelSign(text.getOrNull(i))) out.append('a')
            } else {
                out.append(text[i]); i += 1
            }
        }
        return out.toString()
    }

    /** Devanagari and Gurmukhi consonant code points. */
    private fun isIndicConsonant(c: Char): Boolean =
        c in '\u0915'..'\u0939' || c in '\u0958'..'\u095F' ||
            c in '\u0A15'..'\u0A39' || c in '\u0A59'..'\u0A5E'

    /** Vowel signs (matras), virama and the anusvara/visarga marks. */
    private fun isIndicVowelSign(c: Char?): Boolean {
        if (c == null) return false
        return c in '\u0900'..'\u0903' || c in '\u093A'..'\u094D' ||
            c in '\u0A00'..'\u0A03' || c in '\u0A3C'..'\u0A4D'
    }

    // --- language detection (same rules as the mobile app) -------------------

    private val CYRILLIC_CHAR = Regex("[\\u0400-\\u04FF]")

    private fun matchesAny(text: String, letters: Set<String>): Boolean =
        text.any { letters.contains(it.toString()) } &&
            text.all { letters.contains(it.toString()) || !CYRILLIC_CHAR.matches(it.toString()) }

    fun isRussian(text: String) = matchesAny(text, RUSSIAN_LETTERS)

    private fun isUkrainian(text: String) =
        matchesAny(text, UKRAINIAN_LETTERS + UKRAINIAN_SPECIFIC_LETTERS)

    private fun isSerbian(text: String) =
        matchesAny(text, SERBIAN_LETTERS + SERBIAN_SPECIFIC_LETTERS)

    private fun isBulgarian(text: String) = matchesAny(text, BULGARIAN_LETTERS)

    private fun isBelarusian(text: String) =
        matchesAny(text, BELARUSIAN_LETTERS + BELARUSIAN_SPECIFIC_LETTERS)

    private fun isKyrgyz(text: String) = matchesAny(text, KYRGYZ_LETTERS + KYRGYZ_SPECIFIC_LETTERS)

    private fun isMacedonian(text: String) =
        matchesAny(text, MACEDONIAN_LETTERS + MACEDONIAN_SPECIFIC_LETTERS)

    // --- tables --------------------------------------------------------------

    private val KANA_ROMAJI_MAP: Map<String, String> = mapOf(
        // Yōon digraphs
        "キャ" to "kya", "キュ" to "kyu", "キョ" to "kyo",
        "シャ" to "sha", "シュ" to "shu", "ショ" to "sho",
        "チャ" to "cha", "チュ" to "chu", "チョ" to "cho",
        "ニャ" to "nya", "ニュ" to "nyu", "ニョ" to "nyo",
        "ヒャ" to "hya", "ヒュ" to "hyu", "ヒョ" to "hyo",
        "ミャ" to "mya", "ミュ" to "myu", "ミョ" to "myo",
        "リャ" to "rya", "リュ" to "ryu", "リョ" to "ryo",
        "ギャ" to "gya", "ギュ" to "gyu", "ギョ" to "gyo",
        "ジャ" to "ja", "ジュ" to "ju", "ジョ" to "jo",
        "ヂャ" to "ja", "ヂュ" to "ju", "ヂョ" to "jo",
        "ビャ" to "bya", "ビュ" to "byu", "ビョ" to "byo",
        "ピャ" to "pya", "ピュ" to "pyu", "ピョ" to "pyo",
        // Basic katakana
        "ア" to "a", "イ" to "i", "ウ" to "u", "エ" to "e", "オ" to "o",
        "カ" to "ka", "キ" to "ki", "ク" to "ku", "ケ" to "ke", "コ" to "ko",
        "サ" to "sa", "シ" to "shi", "ス" to "su", "セ" to "se", "ソ" to "so",
        "タ" to "ta", "チ" to "chi", "ツ" to "tsu", "テ" to "te", "ト" to "to",
        "ナ" to "na", "ニ" to "ni", "ヌ" to "nu", "ネ" to "ne", "ノ" to "no",
        "ハ" to "ha", "ヒ" to "hi", "フ" to "fu", "ヘ" to "he", "ホ" to "ho",
        "マ" to "ma", "ミ" to "mi", "ム" to "mu", "メ" to "me", "モ" to "mo",
        "ヤ" to "ya", "ユ" to "yu", "ヨ" to "yo",
        "ラ" to "ra", "リ" to "ri", "ル" to "ru", "レ" to "re", "ロ" to "ro",
        "ワ" to "wa", "ヲ" to "o", "ン" to "n",
        // Dakuten
        "ガ" to "ga", "ギ" to "gi", "グ" to "gu", "ゲ" to "ge", "ゴ" to "go",
        "ザ" to "za", "ジ" to "ji", "ズ" to "zu", "ゼ" to "ze", "ゾ" to "zo",
        "ダ" to "da", "ヂ" to "ji", "ヅ" to "zu", "デ" to "de", "ド" to "do",
        // Handakuten
        "バ" to "ba", "ビ" to "bi", "ブ" to "bu", "ベ" to "be", "ボ" to "bo",
        "パ" to "pa", "ピ" to "pi", "プ" to "pu", "ペ" to "pe", "ポ" to "po",
        // Chōonpu
        "ー" to "",
    )

    private val HANGUL_CHO: Map<String, String> = mapOf(
        "\u1100" to "g", "\u1101" to "kk", "\u1102" to "n", "\u1103" to "d",
        "\u1104" to "tt", "\u1105" to "r", "\u1106" to "m", "\u1107" to "b",
        "\u1108" to "pp", "\u1109" to "s", "\u110A" to "ss", "\u110B" to "",
        "\u110C" to "j", "\u110D" to "jj", "\u110E" to "ch", "\u110F" to "k",
        "\u1110" to "t", "\u1111" to "p", "\u1112" to "h",
    )

    private val HANGUL_JUNG: Map<String, String> = mapOf(
        "\u1161" to "a", "\u1162" to "ae", "\u1163" to "ya", "\u1164" to "yae",
        "\u1165" to "eo", "\u1166" to "e", "\u1167" to "yeo", "\u1168" to "ye",
        "\u1169" to "o", "\u116A" to "wa", "\u116B" to "wae", "\u116C" to "oe",
        "\u116D" to "yo", "\u116E" to "u", "\u116F" to "wo", "\u1170" to "we",
        "\u1171" to "wi", "\u1172" to "yu", "\u1173" to "eu", "\u1174" to "eui",
        "\u1175" to "i",
    )

    /** Final consonants, including the assimilation forms of the mobile table. */
    private val HANGUL_JONG: Map<String, String> = buildMap {
        put("\u11A8", "k"); put("\u11A8\u110B", "g"); put("\u11A8\u1102", "ngn")
        put("\u11A8\u1105", "ngn"); put("\u11A8\u1106", "ngm"); put("\u11A8\u1112", "kh")
        put("\u11A9", "kk"); put("\u11A9\u110B", "kg"); put("\u11A9\u1102", "ngn")
        put("\u11A9\u1105", "ngn"); put("\u11A9\u1106", "ngm"); put("\u11A9\u1112", "kh")
        put("\u11AA", "k"); put("\u11AA\u110B", "ks"); put("\u11AA\u1102", "ngn")
        put("\u11AA\u1105", "ngn"); put("\u11AA\u1106", "ngm"); put("\u11AA\u1112", "kch")
        put("\u11AB", "n"); put("\u11AB\u1105", "ll")
        put("\u11AC", "n"); put("\u11AC\u110B", "nj"); put("\u11AC\u1102", "nn")
        put("\u11AC\u1105", "nn"); put("\u11AC\u1106", "nm"); put("\u11AC\u1112", "nch")
        put("\u11AD", "n"); put("\u11AD\u110B", "nh"); put("\u11AD\u1105", "nn")
        put("\u11AE", "t"); put("\u11AE\u110B", "d"); put("\u11AE\u1102", "nn")
        put("\u11AE\u1105", "nn"); put("\u11AE\u1106", "nm"); put("\u11AE\u1112", "th")
        put("\u11AF", "l"); put("\u11AF\u110B", "r"); put("\u11AF\u1102", "ll")
        put("\u11AF\u1105", "ll")
        put("\u11B0", "k"); put("\u11B0\u110B", "lg"); put("\u11B0\u1102", "ngn")
        put("\u11B0\u1105", "ngn"); put("\u11B0\u1106", "ngm"); put("\u11B0\u1112", "lkh")
        put("\u11B1", "m"); put("\u11B1\u110B", "lm"); put("\u11B1\u1102", "mn")
        put("\u11B1\u1105", "mn"); put("\u11B1\u1106", "mm"); put("\u11B1\u1112", "lmh")
        put("\u11B2", "p"); put("\u11B2\u110B", "lb"); put("\u11B2\u1102", "mn")
        put("\u11B2\u1105", "mn"); put("\u11B2\u1106", "mm"); put("\u11B2\u1112", "lph")
        put("\u11B3", "t"); put("\u11B3\u110B", "ls"); put("\u11B3\u1102", "nn")
        put("\u11B3\u1105", "nn"); put("\u11B3\u1106", "nm"); put("\u11B3\u1112", "lsh")
        put("\u11B4", "t"); put("\u11B4\u110B", "lt"); put("\u11B4\u1102", "nn")
        put("\u11B4\u1105", "nn"); put("\u11B4\u1106", "nm"); put("\u11B4\u1112", "lth")
        put("\u11B5", "p"); put("\u11B5\u110B", "lp"); put("\u11B5\u1102", "mn")
        put("\u11B5\u1105", "mn"); put("\u11B5\u1106", "mm"); put("\u11B5\u1112", "lph")
        put("\u11B6", "l"); put("\u11B6\u110B", "lh"); put("\u11B6\u1102", "ll")
        put("\u11B6\u1105", "ll"); put("\u11B6\u1106", "lm"); put("\u11B6\u1112", "lh")
        put("\u11B7", "m"); put("\u11B7\u1105", "mn")
        put("\u11B8", "p"); put("\u11B8\u110B", "b"); put("\u11B8\u1102", "mn")
        put("\u11B8\u1105", "mn"); put("\u11B8\u1106", "mm"); put("\u11B8\u1112", "ph")
        put("\u11B9", "p"); put("\u11B9\u110B", "ps"); put("\u11B9\u1102", "mn")
        put("\u11B9\u1105", "mn"); put("\u11B9\u1106", "mm"); put("\u11B9\u1112", "psh")
        put("\u11BA", "t"); put("\u11BA\u110B", "s"); put("\u11BA\u1102", "nn")
        put("\u11BA\u1105", "nn"); put("\u11BA\u1106", "nm"); put("\u11BA\u1112", "sh")
        put("\u11BB", "t"); put("\u11BB\u110B", "ss"); put("\u11BB\u1102", "tn")
        put("\u11BB\u1105", "tn"); put("\u11BB\u1106", "nm"); put("\u11BB\u1112", "th")
        put("\u11BC", "ng")
        put("\u11BD", "t"); put("\u11BD\u110B", "j"); put("\u11BD\u1102", "nn")
        put("\u11BD\u1105", "nn"); put("\u11BD\u1106", "nm"); put("\u11BD\u1112", "ch")
        put("\u11BE", "t"); put("\u11BE\u110B", "ch"); put("\u11BE\u1102", "nn")
        put("\u11BE\u1105", "nn"); put("\u11BE\u1106", "nm"); put("\u11BE\u1112", "ch")
        put("\u11BF", "k"); put("\u11BF\u110B", "k"); put("\u11BF\u1102", "ngn")
        put("\u11BF\u1105", "ngn"); put("\u11BF\u1106", "ngm"); put("\u11BF\u1112", "kh")
        put("\u11C0", "t"); put("\u11C0\u110B", "t"); put("\u11C0\u1102", "nn")
        put("\u11C0\u1105", "nn"); put("\u11C0\u1106", "nm"); put("\u11C0\u1112", "th")
        put("\u11C1", "p"); put("\u11C1\u110B", "p"); put("\u11C1\u1102", "mn")
        put("\u11C1\u1105", "mn"); put("\u11C1\u1106", "mm"); put("\u11C1\u1112", "ph")
        put("\u11C2", "t"); put("\u11C2\u110B", "h"); put("\u11C2\u1102", "nn")
        put("\u11C2\u1105", "nn"); put("\u11C2\u1106", "mm"); put("\u11C2\u1112", "t")
        put("\u11C2\u1100", "k")
    }

    private val GENERAL_CYRILLIC_ROMAJI_MAP: Map<String, String> = mapOf(
        "А" to "A", "Б" to "B", "В" to "V", "Г" to "G", "Ґ" to "G", "Д" to "D",
        "Ѓ" to "Ǵ", "Ђ" to "Đ", "Е" to "E", "Ё" to "Yo", "Є" to "Ye", "Ж" to "Zh",
        "З" to "Z", "Ѕ" to "Dz", "И" to "I", "І" to "I", "Ї" to "Yi", "Й" to "Y",
        "Ј" to "Y", "К" to "K", "Л" to "L", "Љ" to "Ly", "М" to "M", "Н" to "N",
        "Њ" to "Ny", "О" to "O", "П" to "P", "Р" to "R", "С" to "S", "Т" to "T",
        "Ћ" to "Ć", "У" to "U", "Ў" to "Ŭ", "Ф" to "F", "Х" to "Kh", "Ц" to "Ts",
        "Ч" to "Ch", "Џ" to "Dž", "Ш" to "Sh", "Щ" to "Shch", "Ъ" to "ʺ", "Ы" to "Y",
        "Ь" to "ʹ", "Э" to "E", "Ю" to "Yu", "Я" to "Ya",
        "Ѡ" to "O", "Ѣ" to "Ya", "Ѥ" to "Ye", "Ѧ" to "Ya", "Ѩ" to "Ya",
        "Ѫ" to "U", "Ѭ" to "Yu", "Ѯ" to "Ks", "Ѱ" to "Ps", "Ѳ" to "F",
        "Ѵ" to "I", "Ѷ" to "I", "Ғ" to "Gh", "Ҕ" to "G", "Җ" to "Zh",
        "Ҙ" to "Dz", "Қ" to "Q", "Ҝ" to "K", "Ҟ" to "K", "Ҡ" to "K",
        "Ң" to "Ng", "Ҥ" to "Ng", "Ҧ" to "P", "Ҩ" to "O", "Ҫ" to "S",
        "Ҭ" to "T", "Ү" to "U", "Ұ" to "U", "Ҳ" to "Kh", "Ҵ" to "Ts",
        "Ҷ" to "Ch", "Ҹ" to "Ch", "Һ" to "H", "Ҽ" to "Ch", "Ҿ" to "Ch",
        "Ќ" to "Ḱ", "Ө" to "Ö",
        "а" to "a", "б" to "b", "в" to "v", "г" to "g", "ґ" to "g", "д" to "d",
        "ѓ" to "ǵ", "ђ" to "đ", "е" to "e", "ё" to "yo", "є" to "ye", "ж" to "zh",
        "з" to "z", "ѕ" to "dz", "и" to "i", "і" to "i", "ї" to "yi", "й" to "y",
        "ј" to "y", "к" to "k", "л" to "l", "љ" to "ly", "м" to "m", "н" to "n",
        "њ" to "ny", "о" to "o", "п" to "p", "р" to "r", "с" to "s", "т" to "t",
        "ћ" to "ć", "у" to "u", "ў" to "ŭ", "ф" to "f", "х" to "kh", "ц" to "ts",
        "ч" to "ch", "џ" to "dž", "ш" to "sh", "щ" to "shch", "ъ" to "ʺ", "ы" to "y",
        "ь" to "ʹ", "э" to "e", "ю" to "yu", "я" to "ya",
        "ѡ" to "o", "ѣ" to "ya", "ѥ" to "ye", "ѧ" to "ya", "ѩ" to "ya",
        "ѫ" to "u", "ѭ" to "yu", "ѯ" to "ks", "ѱ" to "ps", "ѳ" to "f",
        "ѵ" to "i", "ѷ" to "i", "ғ" to "gh", "ҕ" to "g", "җ" to "zh",
        "ҙ" to "dz", "қ" to "q", "ҝ" to "k", "ҟ" to "k", "ҡ" to "k",
        "ң" to "ng", "ҥ" to "ng", "ҧ" to "p", "ҩ" to "o", "ҫ" to "s",
        "ҭ" to "t", "ү" to "u", "ұ" to "u", "ҳ" to "kh", "ҵ" to "ts",
        "ҷ" to "ch", "ҹ" to "ch", "һ" to "h", "ҽ" to "ch", "ҿ" to "ch",
        "ќ" to "ḱ", "ө" to "ö",
    )

    private val RUSSIAN_ROMAJI_MAP: Map<String, String> = mapOf(
        "ого" to "ovo", "Ого" to "Ovo", "его" to "evo", "Его" to "Evo",
    )

    private val UKRAINIAN_ROMAJI_MAP: Map<String, String> = mapOf(
        "Г" to "H", "г" to "h",
        "Ґ" to "G", "ґ" to "g",
        "Є" to "Ye", "є" to "ye",
        "І" to "I", "і" to "i",
        "Ї" to "Yi", "ї" to "yi",
    )

    private val SERBIAN_ROMAJI_MAP: Map<String, String> = mapOf(
        "Ж" to "Ž", "Љ" to "Lj", "Њ" to "Nj", "Ц" to "C", "Ч" to "Č",
        "Џ" to "Dž", "Ш" to "Š", "Х" to "H",
        "ж" to "ž", "љ" to "lj", "њ" to "nj", "ц" to "c", "ч" to "č",
        "џ" to "dž", "ш" to "š", "х" to "h",
    )

    private val BULGARIAN_ROMAJI_MAP: Map<String, String> = mapOf(
        "Ж" to "Zh", "Ц" to "Ts", "Ч" to "Ch", "Ш" to "Sh", "Щ" to "Sht",
        "Ъ" to "A", "Ь" to "Y", "Ю" to "Yu", "Я" to "Ya",
        "ж" to "zh", "ц" to "ts", "ч" to "ch", "ш" to "sh", "щ" to "sht",
        "ъ" to "a", "ь" to "y", "ю" to "yu", "я" to "ya",
    )

    private val BELARUSIAN_ROMAJI_MAP: Map<String, String> = mapOf(
        "Г" to "H", "г" to "h", "Ў" to "W", "ў" to "w",
    )

    private val KYRGYZ_ROMAJI_MAP: Map<String, String> = mapOf(
        "Ү" to "Ü", "ү" to "ü", "Ы" to "Y", "ы" to "y",
    )

    private val MACEDONIAN_ROMAJI_MAP: Map<String, String> = mapOf(
        "Ѓ" to "Gj", "Ѕ" to "Dz", "И" to "I", "Ј" to "J", "Љ" to "Lj",
        "Њ" to "Nj", "Ќ" to "Kj", "Џ" to "Dž", "Ч" to "Č", "Ш" to "Sh",
        "Ж" to "Zh", "Ц" to "C", "Х" to "H",
        "ѓ" to "gj", "ѕ" to "dz", "и" to "i", "ј" to "j", "љ" to "lj",
        "њ" to "nj", "ќ" to "kj", "џ" to "dž", "ч" to "č", "ш" to "sh",
        "ж" to "zh", "ц" to "c", "х" to "h",
    )

    private val DEVANAGARI_ROMAJI_MAP: Map<String, String> = mapOf(
        "अ" to "a", "आ" to "aa", "इ" to "i", "ई" to "ee", "उ" to "u", "ऊ" to "oo",
        "ऋ" to "ri", "ए" to "e", "ऐ" to "ai", "ओ" to "o", "औ" to "au",
        "क" to "k", "ख" to "kh", "ग" to "g", "घ" to "gh", "ङ" to "ng",
        "च" to "ch", "छ" to "chh", "ज" to "j", "झ" to "jh", "ञ" to "ny",
        "ट" to "t", "ठ" to "th", "ड" to "d", "ढ" to "dh", "ण" to "n",
        "त" to "t", "थ" to "th", "द" to "d", "ध" to "dh", "न" to "n",
        "प" to "p", "फ" to "ph", "ब" to "b", "भ" to "bh", "म" to "m",
        "य" to "y", "र" to "r", "ल" to "l", "व" to "v",
        "श" to "sh", "ष" to "sh", "स" to "s", "ह" to "h",
        "क्ष" to "ksh", "त्र" to "tr", "ज्ञ" to "gy", "श्र" to "shr",
        "ा" to "aa", "ि" to "i", "ी" to "ee", "ु" to "u", "ू" to "oo",
        "ृ" to "ri", "े" to "e", "ै" to "ai", "ो" to "o", "ौ" to "au",
        "ं" to "n", "ः" to "h", "ँ" to "n", "़" to "", "्" to "",
        "०" to "0", "१" to "1", "२" to "2", "३" to "3", "४" to "4",
        "५" to "5", "६" to "6", "७" to "7", "८" to "8", "९" to "9",
        "ॐ" to "Om", "ऽ" to "",
        "क़" to "q", "ख़" to "kh", "ग़" to "g", "ज़" to "z", "ड़" to "r", "ढ़" to "rh",
        "फ़" to "f", "य़" to "y",
        "क\u093C" to "q", "ख\u093C" to "kh", "ग\u093C" to "g", "ज\u093C" to "z",
        "ड\u093C" to "r", "ढ\u093C" to "rh", "फ\u093C" to "f", "य\u093C" to "y",
    )

    private val GURMUKHI_ROMAJI_MAP: Map<String, String> = mapOf(
        "ੳ" to "o", "ਅ" to "a", "ੲ" to "e", "ਸ" to "s", "ਹ" to "h",
        "ਕ" to "k", "ਖ" to "kh", "ਗ" to "g", "ਘ" to "gh", "ਙ" to "ng",
        "ਚ" to "ch", "ਛ" to "chh", "ਜ" to "j", "ਝ" to "jh", "ਞ" to "ny",
        "ਟ" to "t", "ਠ" to "th", "ਡ" to "d", "ਢ" to "dh", "ਣ" to "n",
        "ਤ" to "t", "ਥ" to "th", "ਦ" to "d", "ਧ" to "dh", "ਨ" to "n",
        "ਪ" to "p", "ਫ" to "ph", "ਬ" to "b", "ਭ" to "bh", "ਮ" to "m",
        "ਯ" to "y", "ਰ" to "r", "ਲ" to "l", "ਵ" to "v", "ੜ" to "r",
        "ਸ਼" to "sh", "ਖ਼" to "kh", "ਗ਼" to "g", "ਜ਼" to "z", "ਫ਼" to "f", "ਲ਼" to "l",
        "ਾ" to "aa", "ਿ" to "i", "ੀ" to "ee", "ੁ" to "u", "ੂ" to "oo",
        "ੇ" to "e", "ੈ" to "ai", "ੋ" to "o", "ੌ" to "au",
        "ੰ" to "n", "ਂ" to "n", "ੱ" to "", "੍" to "", "਼" to "",
        "ੴ" to "Ek Onkar",
        "੦" to "0", "੧" to "1", "੨" to "2", "੩" to "3", "੪" to "4",
        "੫" to "5", "੬" to "6", "੭" to "7", "੮" to "8", "੯" to "9",
    )

    private val RUSSIAN_LETTERS: Set<String> = "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюя"
        .map { it.toString() }.toSet()

    private val UKRAINIAN_LETTERS: Set<String> = "АБВГҐДЕЄЖЗИІЇЙКЛМНОПРСТУФХЦЧШЩЬЮЯабвгґдеєжзиіїйклмнопрстуфхцчшщьюя"
        .map { it.toString() }.toSet()

    private val SERBIAN_LETTERS: Set<String> = "АБВГДЂЕЖЗИЈКЛЉМНЊОПРСТЋУФХЦЧЏШабвгдђежзијклљмнњопрстћуфхцчџш"
        .map { it.toString() }.toSet()

    private val BULGARIAN_LETTERS: Set<String> = "АБВГДЕЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЬЮЯабвгдежзийклмнопрстуфхцчшщъьюя"
        .map { it.toString() }.toSet()

    private val BELARUSIAN_LETTERS: Set<String> = "АБВГДЕЁЖЗІЙКЛМНОПРСТУЎФХЦЧШЬЮЯЫЭабвгдеёжзійклмнопрстуўфхцчшььюяыэ"
        .map { it.toString() }.toSet()

    private val KYRGYZ_LETTERS: Set<String> = "АБВГДЕЁЖЗИЙКЛМНҢОӨПРСТУҮФХЦЧШЩЪЫЬЭЮЯабвгдеёжзийклмнңоөпрстуүфхцчшщъыьэюя"
        .map { it.toString() }.toSet()

    private val MACEDONIAN_LETTERS: Set<String> = "АБВГДЃЕЖЗЅИЈКЛЉМНЊОПРСТЌУФХЦЧЏШабвгдѓежзѕијклљмнњопрстќуфхцчџш"
        .map { it.toString() }.toSet()

    private val UKRAINIAN_SPECIFIC_LETTERS: Set<String> = setOf("Ґ", "ґ", "Є", "є", "І", "і", "Ї", "ї")
    private val SERBIAN_SPECIFIC_LETTERS: Set<String> = setOf("Ђ", "ђ", "Ј", "ј", "Љ", "љ", "Њ", "њ", "Ћ", "ћ", "Џ", "џ")
    private val BELARUSIAN_SPECIFIC_LETTERS: Set<String> = setOf("Ў", "ў", "І", "і")
    private val KYRGYZ_SPECIFIC_LETTERS: Set<String> = setOf("Ң", "ң", "Ө", "ө", "Ү", "ү")
    private val MACEDONIAN_SPECIFIC_LETTERS: Set<String> = setOf("Ѓ", "ѓ", "Ѕ", "ѕ", "Ќ", "ќ")
}
