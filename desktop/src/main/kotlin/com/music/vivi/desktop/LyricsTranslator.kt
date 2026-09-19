package com.music.vivi.desktop

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

/**
 * Translates lyric lines for the desktop lyrics view.
 *
 * The desktop edition already had the whole AI-translation settings page
 * (provider, key, base URL, model, target language, DeepL key/formality) but
 * nothing ever used it. This is the missing half: it takes the parsed lines and
 * returns a translated line for each one, in order.
 *
 * Two providers are supported, matching the settings screen:
 *  - any OpenAI-compatible chat-completions endpoint (OpenRouter by default),
 *    asked to translate a numbered list so the reply can be mapped back to the
 *    original lines even when the model merges or drops one;
 *  - DeepL, which translates an array of texts natively (50 per request).
 *
 * Requests run on [Dispatchers.IO] and every failure returns `null`: a lyrics
 * panel must never crash or block because a translation endpoint is down.
 */
object LyricsTranslator {

    /** Snapshot of the settings the translator needs. */
    data class Config(
        val provider: String = "OpenRouter",
        val apiKey: String = "",
        val baseUrl: String = "https://openrouter.ai/api/v1/chat/completions",
        val model: String = "google/gemini-2.5-flash-lite",
        val targetLanguage: String = "en",
        val mode: String = "Literal",
        val deeplApiKey: String = "",
        val deeplFormality: String = "default",
    ) {
        val isDeepL: Boolean get() = provider.equals("DeepL", ignoreCase = true)
        val key: String get() = if (isDeepL) deeplApiKey else apiKey
        val usable: Boolean get() = key.isNotBlank()
    }

    /** DeepL accepts at most 50 texts per request. */
    private const val DEEPL_BATCH = 50

    /** Chat models start losing lines well before this; stay well under it. */
    private const val CHAT_BATCH = 40

    private const val TIMEOUT_MS = 45_000

    /** `videoId|language|mode` -> translated lines (kept for the session). */
    private val cache = ConcurrentHashMap<String, List<String>>()

    fun clearCache() = cache.clear()

    /**
     * Returns one translation per input line, or null when nothing could be
     * translated (disabled key, offline, provider error, empty lyrics).
     */
    suspend fun translate(
        lines: List<String>,
        config: Config,
        cacheKey: String,
    ): List<String>? {
        if (lines.isEmpty() || !config.usable) return null
        cache[cacheKey]?.let { if (it.size == lines.size) return it }

        val translated = withContext(Dispatchers.IO) {
            runCatching {
                if (config.isDeepL) translateWithDeepL(lines, config) else translateWithChat(lines, config)
            }.getOrNull()
        } ?: return null

        if (translated.size != lines.size) {
            AppLog.log(
                "lyrics",
                "translation dropped/changed lines (${translated.size} of ${lines.size}) — not used",
            )
            return null
        }
        cache[cacheKey] = translated
        AppLog.log("lyrics", "translated ${translated.size} lines to '${config.targetLanguage}' via ${config.provider}")
        return translated
    }

    // --- OpenAI-compatible chat completions ----------------------------------

    private fun translateWithChat(lines: List<String>, config: Config): List<String> {
        val label = if (config.mode.equals("Transcribed", ignoreCase = true)) "transcribed" else "literal"
        val out = mutableListOf<String>()
        lines.chunked(CHAT_BATCH).forEach { batch ->
            val numbered = batch.mapIndexed { i, line -> "${i + 1}. $line" }.joinToString("\n")
            val prompt = buildString {
                append("Translate these song lyrics into the language with the ISO code '")
                append(config.targetLanguage)
                append("', keeping a $label tone. Return ONLY the translated lines, each prefixed with the same ")
                append("number and a dot, in the same order. Never merge or drop lines; if a line is instrumental ")
                append("or has no words, repeat it unchanged.\n\n")
                append(numbered)
            }
            val body = buildJson(
                config.model,
                prompt,
            )
            val response = post(config.baseUrl, body, mapOf("Authorization" to "Bearer ${config.key}"))
                ?: return out.ifEmpty { throw IllegalStateException("chat request failed") }
            val content = extractChatContent(response) ?: return out.ifEmpty { throw IllegalStateException("no content") }
            out.addAll(matchNumbered(content, batch.size))
        }
        return out
    }

    private fun buildJson(model: String, prompt: String): String = buildString {
        append("{\"model\":\"").append(jsonEscape(model)).append("\",\"temperature\":0.2,\"messages\":[")
        append("{\"role\":\"system\",\"content\":\"You are a precise lyrics translator. Answer only with the numbered lines.\"},")
        append("{\"role\":\"user\",\"content\":\"").append(jsonEscape(prompt)).append("\"}]}")
    }

    /** Pulls `choices[0].message.content` out of the reply without a JSON lib. */
    private fun extractChatContent(json: String): String? {
        val marker = "\"content\":"
        var index = json.indexOf(marker)
        // The message object's content is the one that matters; the first
        // occurrence in an OpenAI-compatible reply belongs to the assistant.
        while (index >= 0) {
            var i = index + marker.length
            while (i < json.length && json[i].isWhitespace()) i++
            if (i < json.length && json[i] == '"') {
                return readJsonString(json, i)
            }
            index = json.indexOf(marker, index + marker.length)
        }
        return null
    }

    /** Maps "1. text" / "1) text" replies back to [count] lines, in order. */
    private fun matchNumbered(content: String, count: Int): List<String> {
        val byIndex = HashMap<Int, String>()
        val numberPrefix = Regex("""^\s*(\d{1,3})\s*[.):-]\s?""")
        content.lines().forEach { raw ->
            val match = numberPrefix.find(raw) ?: return@forEach
            val number = match.groupValues[1].toIntOrNull() ?: return@forEach
            val text = raw.substring(match.range.last + 1).trim()
            if (number in 1..count && text.isNotEmpty()) byIndex.putIfAbsent(number - 1, text)
        }
        // A model that forgot the numbering is still usable when it returned
        // exactly the expected number of lines.
        if (byIndex.isEmpty()) {
            val plain = content.lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (plain.size == count) return plain
            throw IllegalStateException("translation is not line-mapped")
        }
        return (0 until count).map { byIndex[it] ?: "" }
    }

    // --- DeepL ---------------------------------------------------------------

    private fun translateWithDeepL(lines: List<String>, config: Config): List<String> {
        val out = mutableListOf<String>()
        lines.chunked(DEEPL_BATCH).forEach { batch ->
            val payload = buildString {
                append("{\"text\":[")
                append(batch.joinToString(",") { "\"${jsonEscape(it)}\"" })
                append("],\"target_lang\":\"").append(jsonEscape(config.targetLanguage.uppercase())).append("\"")
                if (!config.deeplFormality.equals("default", ignoreCase = true)) {
                    append(",\"formality\":\"").append(jsonEscape(config.deeplFormality)).append("\"")
                }
                append("}")
            }
            // Free keys live on api-free.deepl.com; pro keys on api.deepl.com.
            val endpoint = if (config.key.endsWith(":fx")) {
                "https://api-free.deepl.com/v2/translate"
            } else {
                "https://api.deepl.com/v2/translate"
            }
            val response = post(endpoint, payload, mapOf("Authorization" to "DeepL-Auth-Key ${config.key}"))
                ?: throw IllegalStateException("deepl request failed")
            val texts = Regex("\"text\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"")
                .findAll(response)
                .map { unescapeJson(it.groupValues[1]) }
                .toList()
            if (texts.size != batch.size) throw IllegalStateException("deepl line count mismatch")
            out.addAll(texts)
        }
        return out
    }

    // --- HTTP ----------------------------------------------------------------

    private fun post(url: String, body: String, headers: Map<String, String>): String? {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(body) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.let { input: java.io.InputStream ->
                input.bufferedReader().use(BufferedReader::readText)
            }.orEmpty()
            if (code !in 200..299) {
                AppLog.log("lyrics", "translation request to $url failed: HTTP $code ${text.take(200)}")
                null
            } else {
                text
            }
        } catch (e: Exception) {
            AppLog.log("lyrics", "translation request to $url failed: ${e.message}")
            null
        } finally {
            runCatching { connection.disconnect() }
        }
    }

    private fun jsonEscape(text: String): String = buildString(text.length + 16) {
        text.forEach { char ->
            when (char) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (char < ' ') append("\\u%04x".format(char.code)) else append(char)
            }
        }
    }

    private fun unescapeJson(text: String): String = buildString(text.length) {
        var i = 0
        while (i < text.length) {
            val char = text[i]
            if (char == '\\' && i + 1 < text.length) {
                when (val next = text[i + 1]) {
                    'n' -> { append('\n'); i += 2 }
                    'r' -> { append('\r'); i += 2 }
                    't' -> { append('\t'); i += 2 }
                    '"' -> { append('"'); i += 2 }
                    '\\' -> { append('\\'); i += 2 }
                    'u' -> {
                        val hex = text.drop(i + 2).take(4)
                        val code = hex.toIntOrNull(16)
                        if (code != null) { append(code.toChar()); i += 6 } else { append(next); i += 2 }
                    }
                    else -> { append(next); i += 2 }
                }
            } else {
                append(char); i += 1
            }
        }
    }

    /** Reads a JSON string literal starting at the opening quote at [start]. */
    private fun readJsonString(json: String, start: Int): String? {
        val out = StringBuilder()
        var i = start + 1
        while (i < json.length) {
            when (val char = json[i]) {
                '"' -> return out.toString()
                '\\' -> {
                    if (i + 1 >= json.length) return null
                    when (val next = json[i + 1]) {
                        'n' -> out.append('\n')
                        'r' -> out.append('\r')
                        't' -> out.append('\t')
                        'u' -> {
                            val hex = json.drop(i + 2).take(4)
                            val code = hex.toIntOrNull(16) ?: return null
                            out.append(code.toChar())
                            i += 4
                        }
                        else -> out.append(next)
                    }
                    i += 2
                }
                else -> { out.append(char); i += 1 }
            }
        }
        return null
    }
}
