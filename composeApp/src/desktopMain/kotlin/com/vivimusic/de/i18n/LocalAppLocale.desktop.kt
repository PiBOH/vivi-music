package com.vivimusic.de.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

actual object LocalAppLocale {
    private var default: Locale? = null
    private val localeState = staticCompositionLocalOf { Locale.getDefault().toString() }

    actual val current: String
        @Composable get() = localeState.current

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        if (default == null) {
            default = Locale.getDefault()
        }
        val new = value?.let { Locale(it) } ?: default!!
        Locale.setDefault(new)
        return localeState.provides(new.toString())
    }
}
