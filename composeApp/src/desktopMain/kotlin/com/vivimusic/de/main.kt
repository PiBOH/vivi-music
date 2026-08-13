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

/** Reads a config value from the environment or from a `supabase.env` file. */
private fun readConfig(key: String): String =
    System.getenv(key) ?: readSupabaseEnvFile()[key] ?: ""

private fun readSupabaseEnvFile(): Map<String, String> {
    val file = java.io.File("supabase.env")
    if (!file.exists()) return emptyMap()
    return file.readLines()
        .mapNotNull { line ->
            val parts = line.split("=", limit = 2)
            if (parts.size == 2) parts[0].trim() to parts[1].trim() else null
        }
        .toMap()
}
