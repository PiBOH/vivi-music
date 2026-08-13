package com.vivimusic.de.data.network

import java.security.MessageDigest

actual fun sha1Hex(input: String): String =
    MessageDigest.getInstance("SHA-1")
        .digest(input.toByteArray())
        .joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
