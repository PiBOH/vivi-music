package com.vivimusic.de.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Currently selected app language as a BCP-47 tag, or null to follow the system
 * locale. Written by the language selector in the settings screen.
 */
var customAppLocale by mutableStateOf<String?>(null)

/**
 * Platform specific locale controller. Updating the locale re-renders the whole
 * subtree (via [key]) so that every string resource is re-resolved.
 */
expect object LocalAppLocale {
    val current: String @Composable get

    @Composable
    infix fun provides(value: String?): ProvidedValue<*>
}

@Composable
fun AppEnvironment(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalAppLocale provides customAppLocale,
    ) {
        key(customAppLocale) {
            content()
        }
    }
}
