package com.vivimusic.de.data

import java.io.File
import java.util.Properties

private fun settingsFile(): File {
    val dir = File(System.getProperty("user.home"), ".vivi-music-de")
    dir.mkdirs()
    return File(dir, "settings.properties")
}

private val cachedProperties: Properties by lazy {
    Properties().also { props ->
        val file = settingsFile()
        if (file.exists()) {
            file.inputStream().use { props.load(it) }
        }
    }
}

/** Reads from the in-memory snapshot instead of reopening the file per key. */
actual fun readSetting(key: String): String? =
    synchronized(cachedProperties) { cachedProperties.getProperty(key) }

/** Updates the memory snapshot immediately and persists it atomically enough
 * for this small settings file. */
actual fun writeSetting(key: String, value: String) {
    synchronized(cachedProperties) {
        cachedProperties.setProperty(key, value)
        settingsFile().outputStream().use {
            cachedProperties.store(it, "Vivi Music DE settings")
        }
    }
}
