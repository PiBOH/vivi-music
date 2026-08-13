package com.vivimusic.de.data

import android.content.Context

private val prefs
    get() = AppContextHolder.context.getSharedPreferences("vivi_music_de", Context.MODE_PRIVATE)

actual fun readSetting(key: String): String? = prefs.getString(key, null)

actual fun writeSetting(key: String, value: String) {
    prefs.edit().putString(key, value).apply()
}
