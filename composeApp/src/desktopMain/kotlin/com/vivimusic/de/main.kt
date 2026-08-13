package com.vivimusic.de

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.vivimusic.de.data.AppConfig
import com.vivimusic.de.data.AppContainer
import com.vivimusic.de.ui.AppStartup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import javax.swing.JButton
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import java.awt.BorderLayout
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

private const val CRASH_LOG_FILE = "ViviMusicDE-crash.log"

// Single-instance lock, held for the whole process lifetime so a second launch
// detects an already-running instance and exits instead of stacking duplicates.
private var instanceLockChannel: FileChannel? = null

fun main() {
    // Show uncaught exceptions (on any thread) as a detailed dialog instead of
    // a silent exit, and always write the stack trace to a crash log next to
    // the executable.
    Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
        reportFatalError("Uncaught exception", throwable)
    }

    if (!tryAcquireSingleInstanceLock()) {
        JOptionPane.showMessageDialog(
            null,
            "Vivi Music DE is already running.",
            "Vivi Music DE",
            JOptionPane.INFORMATION_MESSAGE,
        )
        return
    }

    try {
        runApplication()
    } catch (throwable: Throwable) {
        reportFatalError("Application failed to start", throwable)
    }
}

/**
 * Acquires an exclusive OS file lock in the app data directory. Returns false
 * when another instance already holds it (the app is already running).
 */
private fun tryAcquireSingleInstanceLock(): Boolean = try {
    val lockFile = File(System.getProperty("user.home"), ".vivi-music-de/instance.lock")
    lockFile.parentFile?.mkdirs()
    val channel = FileChannel.open(
        lockFile.toPath(),
        StandardOpenOption.CREATE,
        StandardOpenOption.WRITE,
    )
    val lock = channel.tryLock()
    if (lock == null) {
        channel.close()
        false
    } else {
        // Keep the channel (and therefore the OS lock) alive for the whole run.
        instanceLockChannel = channel
        true
    }
} catch (e: Exception) {
    // If locking is unavailable, allow the app to run rather than blocking it.
    true
}

private fun runApplication() {
    AppConfig.supabaseUrl = readConfig("SUPABASE_URL")
    AppConfig.supabaseAnonKey = readConfig("SUPABASE_ANON_KEY")
    AppConfig.innerTubeApiKey = readConfig("INNERTUBE_API_KEY")
    AppConfig.appVersion = readConfig("APP_VERSION").ifBlank { "dev" }

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Create the native window before constructing Room, the HTTP client and
    // Supabase. AppStartup shows the lightweight shell first and initializes
    // those services off the UI thread after the first frame.
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Vivi Music DE",
            state = rememberWindowState(width = 1000.dp, height = 720.dp),
        ) {
            AppStartup(scope = scope) { AppContainer(scope) }
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
        val details = buildString {
            appendLine(title)
            appendLine()
            appendLine(throwable.message ?: "No error message")
            appendLine()
            appendLine("Stack trace and full details written to:")
            appendLine(logFile)
            appendLine()
            appendLine(stackTrace)
        }
        val textArea = JTextArea(details)
        textArea.isEditable = false
        textArea.rows = 20
        textArea.columns = 100

        val copyButton = JButton("Copy error")
        copyButton.addActionListener {
            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(details), null)
        }
        val panel = JPanel(BorderLayout(8, 8))
        panel.add(JScrollPane(textArea), BorderLayout.CENTER)
        panel.add(copyButton, BorderLayout.SOUTH)

        JOptionPane.showMessageDialog(
            null,
            panel,
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

/** Parsed `.env` file, read once and reused for every config lookup. */
private val envFile: Map<String, String> by lazy { readEnvFile() }

/**
 * Reads a config value from (in order) a JVM system property, the process
 * environment, or a `.env` file next to the executable.
 */
private fun readConfig(key: String): String =
    System.getProperty(key)?.takeIf { it.isNotBlank() }
        ?: System.getenv(key)?.takeIf { it.isNotBlank() }
        ?: envFile[key]?.takeIf { it.isNotBlank() }
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
