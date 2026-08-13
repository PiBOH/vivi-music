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
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import javax.swing.JOptionPane
import javax.swing.JScrollPane
import javax.swing.JTextArea

private const val CRASH_LOG_FILE = "ViviMusicDE-crash.log"

fun main() {
    // Show uncaught exceptions (on any thread) as a detailed dialog instead of
    // a silent exit, and always write the stack trace to a crash log next to
    // the executable.
    Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
        reportFatalError("Uncaught exception", throwable)
    }

    try {
        runApplication()
    } catch (throwable: Throwable) {
        reportFatalError("Application failed to start", throwable)
    }
}

private fun runApplication() {
    AppConfig.supabaseUrl = readConfig("SUPABASE_URL")
    AppConfig.supabaseAnonKey = readConfig("SUPABASE_ANON_KEY")
    AppConfig.innerTubeApiKey = readConfig("INNERTUBE_API_KEY")
    AppConfig.appVersion = readConfig("APP_VERSION").ifBlank { "dev" }

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
 * Logs a fatal error to a crash log file and shows a dialog with the full
 * message and stack trace so the user can report it.
 */
private fun reportFatalError(title: String, throwable: Throwable) {
    val stackTrace = stackTraceOf(throwable)
    val logFile = try {
        val file = File(CRASH_LOG_FILE)
        file.writeText("$title\n\n$stackTrace")
        file.absoluteFile.toString()
    } catch (e: Throwable) {
        "unable to write crash log: ${e.message}"
    }

    // Best effort: also print to stderr so it is visible when launched from a
    // terminal (e.g. `./gradlew :composeApp:run`).
    System.err.println("$title\n$stackTrace")

    try {
        val textArea = JTextArea("$title\n\n${throwable.message}\n\nStack trace and full details written to:\n$logFile\n\n$stackTrace")
        textArea.isEditable = false
        textArea.rows = 20
        textArea.columns = 100
        JOptionPane.showMessageDialog(
            null,
            JScrollPane(textArea),
            "Vivi Music DE - Error",
            JOptionPane.ERROR_MESSAGE,
        )
    } catch (e: Throwable) {
        // The dialog itself failed; nothing more we can do. The crash log and
        // stderr already contain the details.
    }
}

private fun stackTraceOf(throwable: Throwable): String {
    val sw = StringWriter()
    throwable.printStackTrace(PrintWriter(sw))
    return sw.toString()
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
