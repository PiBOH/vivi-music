package com.vivimusic.de

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.vivimusic.de.data.AppConfig
import com.vivimusic.de.data.AppContainer
import com.vivimusic.de.ui.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

fun main() {
    AppConfig.supabaseUrl = readConfig("SUPABASE_URL")
    AppConfig.supabaseAnonKey = readConfig("SUPABASE_ANON_KEY")
    AppConfig.innerTubeApiKey = readConfig("INNERTUBE_API_KEY")

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val container = AppContainer(scope)
    container.start()

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Vivi Music DE",
            state = rememberWindowState(width = 1000.dp, height = 720.dp),
        ) {
            App(container)
        }
    }
}

/**
 * Reads a config value from (in order) a JVM system property, the process
 * environment, or a `.env` file next to the executable.
 */
private fun readConfig(key: String): String =
    System.getProperty(key)?.takeIf { it.isNotBlank() }
        ?: System.getenv(key)?.takeIf { it.isNotBlank() }
        ?: readEnvFile()[key]?.takeIf { it.isNotBlank() }
        ?: ""

private fun readEnvFile(): Map<String, String> {
    val file = java.io.File(".env")
    if (!file.exists()) return emptyMap()
    return file.readLines()
        .mapNotNull { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) return@mapNotNull null
            val parts = line.split("=", limit = 2)
            if (parts.size == 2) parts[0].trim() to parts[1].trim() else null
        }
        .toMap()
}
