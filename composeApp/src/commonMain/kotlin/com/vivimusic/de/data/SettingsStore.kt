package com.vivimusic.de.data

/**
 * Minimal persistent key/value store used for settings such as the selected
 * language. Implemented per platform (SharedPreferences on Android, a
 * properties file on the desktop JVM).
 */
expect fun readSetting(key: String): String?

expect fun writeSetting(key: String, value: String)
