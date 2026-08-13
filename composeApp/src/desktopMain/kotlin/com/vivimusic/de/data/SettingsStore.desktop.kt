package com.vivimusic.de.data

import java.io.File
import java.util.Properties

private fun settingsFile(): File {
    val dir = File(System.getProperty("user.home"), ".vivi-music-de")
    dir.mkdirs()
    return File(dir, "settings.properties")
}

private fun loadProps(): Properties {
    val props = Properties()
    val file = settingsFile()
    if (file.exists()) {
        file.inputStream().use { props.load(it) }
    }
    return props
}

actual fun readSetting(key: String): String? = loadProps().getProperty(key)

actual fun writeSetting(key: String, value: String) {
    val props = loadProps()
    props.setProperty(key, value)
    settingsFile().outputStream().use { props.store(it, "Vivi Music DE settings") }
}
