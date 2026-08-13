package com.vivimusic.de.data.network

/**
 * Removes line-ending/control characters that HTTP rejects in a Cookie header.
 * Browser exports and the original ViVi Music token editor may wrap long
 * values across lines, so those line endings are intentionally joined.
 */
fun normalizeYtCookie(raw: String): String =
    raw
        .filterNot { character -> character.code < 0x20 || character.code == 0x7F }
        .trim()
        .split(';')
        .mapNotNull { part ->
            val cookiePart = part.trim()
            if (cookiePart.isBlank()) return@mapNotNull null
            val separator = cookiePart.indexOf('=')
            if (separator <= 0) return@mapNotNull null
            val name = cookiePart.substring(0, separator).trim()
                .removePrefix("Cookie:")
                .trim()
            val value = cookiePart.substring(separator + 1).trim()
            if (name.isBlank() || value.isBlank()) null else "$name=$value"
        }
        .joinToString("; ")

/** Returns a concise validation error, or null when the cookie is usable. */
fun validateYtCookie(input: String): String? {
    val cookie = extractYtCookie(input)
    if (cookie.isBlank()) return "The pasted value is empty"
    if (!hasSapisidCookie(cookie)) return "SAPISID was not found in the pasted cookie"
    return null
}

/**
 * Extracts the cookie from either a raw Cookie header or the token bundle used
 * by the original ViVi Music app (`***INNERTUBE COOKIE*** =...`).
 */
fun extractYtCookie(input: String): String {
    val marker = "***INNERTUBE COOKIE***"
    val markedStart = input.indexOf(marker)
    val raw = if (markedStart >= 0) {
        input.substring(markedStart + marker.length)
            .substringAfter('=', "")
            .substringBefore("***VISITOR DATA***")
    } else {
        input
    }
    return normalizeYtCookie(raw)
}

fun hasSapisidCookie(cookie: String): Boolean =
    parseCookieString(cookie).containsKey("SAPISID")
