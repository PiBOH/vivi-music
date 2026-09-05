package com.music.vivi.desktop

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Structured, timestamped log of what VIVI Music DE actually did: playback
 * commands, navigation, settings changes, login/sync events, errors and other
 * user actions. Two consumers:
 *
 *  1. **Live viewer** (Developer options → the "Live monitor" card, opened in
 *     a dedicated window): the last [MAX_LINES] lines are exposed as a
 *     [StateFlow] and rendered as they arrive.
 *  2. **Log export** (Settings → System → "Export logs"): every line is
 *     appended to `~/.vivimusic/actions.log`, which [LogExporter] already
 *     packages because it collects every `*.log` file under that directory.
 *
 * Lines are intentionally kept technical (English) — they are diagnostic
 * data, not UI, so they never go through localization.
 */
object AppLog {
    private const val MAX_LINES = 4000
    private const val MAX_FILE_BYTES = 2L * 1024 * 1024 // 2 MB cap before trimming

    private val vivimusicDir: File
        get() = File(System.getProperty("user.home"), ".vivimusic")

    private val file: File
        get() = File(vivimusicDir, "actions.log")

    private val lock = ReentrantLock()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _lines = MutableStateFlow<List<String>>(emptyList())
    /** Last [MAX_LINES] log lines, oldest first. */
    val lines: StateFlow<List<String>> = _lines.asStateFlow()

    private val stamp: String
        get() = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))

    init {
        vivimusicDir.mkdirs()
    }

    /** Appends a diagnostic line with a category tag, e.g. `log("playback", "seek to 42s")`. */
    fun log(category: String, message: String) {
        val line = "[$stamp] [$category] $message"
        _lines.update { (it + line).takeLast(MAX_LINES) }
        scope.launch {
            lock.withLock {
                runCatching {
                    file.parentFile?.mkdirs()
                    file.appendText(line + "\n", Charsets.UTF_8)
                    // Trim the file when it grows past the cap (keep the tail).
                    if (file.length() > MAX_FILE_BYTES) {
                        val tail = file.readText(Charsets.UTF_8).takeLast((MAX_FILE_BYTES / 2).toInt())
                        file.writeText(tail, Charsets.UTF_8)
                    }
                }
            }
        }
    }

    /** Clears the in-memory buffer and the on-disk file. */
    fun clear() {
        _lines.value = emptyList()
        scope.launch {
            lock.withLock {
                runCatching { file.delete() }
            }
        }
    }
}
