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
 *  2. **On-disk session logs** (`~/.vivimusic/logs/<yyyyMMdd-HHmmss>/`): every
 *     launch creates a timestamped session folder holding one file per
 *     category (`playback.log`, `queue.log`, `lyrics.log`, `nav.log`,
 *     `actions.log`, …) plus nothing else — the old single flat
 *     `~/.vivimusic/actions.log` is migrated into the newest session folder on
 *     startup. [LogExporter] packages the whole tree for support requests.
 *
 * Lines are intentionally kept technical (English) — they are diagnostic
 * data, not UI, so they never go through localization.
 */
object AppLog {
    private const val MAX_LINES = 4000
    private const val MAX_FILE_BYTES = 2L * 1024 * 1024 // 2 MB cap before trimming

    /** Every category that must always have a file in the session folder, even when empty. */
    private val KNOWN_CATEGORIES = listOf(
        "actions", "browse", "cache", "lyrics", "nav", "playback", "queue", "settings", "volume"
    )

    private val vivimusicDir: File
        get() = File(System.getProperty("user.home"), ".vivimusic")

    /** Session folder created at startup: `~/.vivimusic/logs/<yyyyMMdd-HHmmss>/`. */
    private val sessionDir: File = run {
        val logsRoot = File(File(System.getProperty("user.home"), ".vivimusic"), "logs")
        logsRoot.mkdirs()
        val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        File(logsRoot, stamp).apply { mkdirs() }
    }

    /** One file per category inside the session folder. */
    private fun categoryFile(category: String): File {
        val safe = category.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "misc" }
        return File(sessionDir, "$safe.log")
    }

    private val lock = ReentrantLock()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _lines = MutableStateFlow<List<String>>(emptyList())
    /** Last [MAX_LINES] log lines, oldest first. */
    val lines: StateFlow<List<String>> = _lines.asStateFlow()

    private val stamp: String
        get() = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))

    init {
        vivimusicDir.mkdirs()
        // Migrate the legacy flat actions.log into this session's folder so
        // the old diagnostics are not lost (and the root stays clean).
        val legacy = File(vivimusicDir, "actions.log")
        if (legacy.exists()) {
            runCatching {
                legacy.copyTo(File(sessionDir, "actions.log"), overwrite = true)
                legacy.delete()
            }
        }
        // Guarantee that every known category has a file in this session from
        // the start — even when nothing was logged to it yet — so the support
        // zip is always complete and an empty file is still exported.
        runCatching {
            for (cat in KNOWN_CATEGORIES) {
                val f = categoryFile(cat)
                if (!f.exists()) {
                    f.createNewFile()
                }
            }
        }
    }

    /** Appends a diagnostic line with a category tag, e.g. `log("playback", "seek to 42s")`. */
    fun log(category: String, message: String) {
        val line = "[$stamp] [$category] $message"
        _lines.update { (it + line).takeLast(MAX_LINES) }
        scope.launch {
            lock.withLock {
                runCatching {
                    val f = categoryFile(category)
                    f.parentFile?.mkdirs()
                    f.appendText(line + "\n", Charsets.UTF_8)
                    // Trim the file when it grows past the cap (keep the tail).
                    if (f.length() > MAX_FILE_BYTES) {
                        val tail = f.readText(Charsets.UTF_8).takeLast((MAX_FILE_BYTES / 2).toInt())
                        f.writeText(tail, Charsets.UTF_8)
                    }
                }
            }
        }
    }

    /**
     * Records a user click with a short human-readable target, e.g.
     * `click("Home shuffle")`. Goes to the session's `actions.log`, which is
     * the click trail used to reproduce a crash from the logs (issue #59).
     */
    fun click(target: String) {
        log("actions", "click $target")
    }

    /** Clears the in-memory buffer and the current session's on-disk logs. */
    fun clear() {
        _lines.value = emptyList()
        scope.launch {
            lock.withLock {
                runCatching {
                    sessionDir.listFiles { f -> f.isFile && f.extension.equals("log", ignoreCase = true) }
                        ?.forEach { it.delete() }
                }
            }
        }
    }
}