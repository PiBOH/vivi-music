package com.vivimusic.de.data.network

/**
 * Returns the SHA-1 digest of [input] as a lowercase hex string. Platform
 * provided because the standard SHA-1 implementation lives in the JDK.
 */
expect fun sha1Hex(input: String): String

/**
 * Parses a raw `Cookie` header value into a map of name -> value. Keys and
 * values are split on the first `=`, matching ViVi Music's `parseCookieString`.
 */
fun parseCookieString(cookie: String): Map<String, String> =
    cookie.split(';')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .mapNotNull { part ->
            val splitIndex = part.indexOf('=')
            if (splitIndex <= 0) null
            else part.substring(0, splitIndex).trim() to part.substring(splitIndex + 1).trim()
        }
        .toMap()
