package com.vivimusic.de.data

import java.io.File

private fun sessionFile(): File {
    val dir = File(System.getProperty("user.home"), ".vivi-music-de")
    dir.mkdirs()
    return File(dir, "session.json")
}

actual fun loadSessionJson(): String? =
    sessionFile().takeIf { it.exists() }?.readText()

actual fun saveSessionJson(json: String) {
    sessionFile().writeText(json)
}

actual fun clearSessionJson() {
    sessionFile().delete()
}
