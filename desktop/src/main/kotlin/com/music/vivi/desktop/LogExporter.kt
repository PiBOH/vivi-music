package com.music.vivi.desktop

import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Builds a structured archive of the app's diagnostic logs for support
 * requests: every `*.log` file under `~/.vivimusic/` plus a generated
 * `system-info.txt` (app version, OS, Java, settings summary) and a redacted
 * `settings-summary.txt` (`device-sync.json` itself is never included because
 * it holds the YouTube session cookie).
 *
 * The zip uses the STORED method on purpose: the archive is meant only for
 * packaging logs, not for saving space.
 */
object LogExporter {

    private val vivimusicDir: File
        get() = File(System.getProperty("user.home"), ".vivimusic")

    private val stamp: String
        get() = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))

    fun defaultFileName(): String = "vivi-de-logs-${AppInfo.FULL_VERSION}-$stamp.zip"

    /**
     * How many session folders the archive carries. A session folder is a
     * folder per app launch (`logs/20260917-153601/`), so a long-lived install
     * would otherwise export hundreds of them and build a zip nobody can open;
     * only the newest ones are packaged, while **all** of them stay on disk.
     */
    const val MAX_SESSION_FOLDERS = 20

    /** Session folders to export, newest first (`yyyyMMdd-HHmmss` names sort
     *  chronologically, so no filesystem timestamp is needed). */
    fun collectSessionDirs(): List<File> {
        val logsRoot = File(vivimusicDir, "logs")
        if (!logsRoot.exists()) return emptyList()
        return logsRoot.listFiles { f -> f.isDirectory }
            ?.sortedByDescending { it.name }
            ?.take(MAX_SESSION_FOLDERS)
            .orEmpty()
    }

    /** Every log file the archive carries, newest first: the legacy flat
     *  `*.log` files in `~/.vivimusic/` plus the logs of the newest
     *  [MAX_SESSION_FOLDERS] sessions. Older sessions are skipped on purpose -
     *  they remain on disk for the user, they are just not part of the zip. */
    fun collectLogFiles(): List<File> {
        val rootLogs = vivimusicDir.listFiles { f -> f.isFile && f.extension.equals("log", ignoreCase = true) }
            ?.toList()
            ?: emptyList()
        val sessionLogs = collectSessionDirs().flatMap { dir ->
            dir.walkTopDown()
                .filter { it.isFile && it.extension.equals("log", ignoreCase = true) }
                .toList()
        }
        return (rootLogs + sessionLogs).sortedByDescending { it.lastModified() }
    }

    /** Zip entry name for a log file: `logs/<relative-path>` keeps the
     *  session folders (`logs/20260907-2152/playback.log`) while legacy root
     *  files stay flat (`logs/actions.log`). */
    private fun entryName(log: File): String {
        val rel = log.relativeTo(vivimusicDir).path.replace('\\', '/')
        return if (rel.startsWith("logs/")) rel else "logs/$rel"
    }

    /** App/OS/hardware summary written as `system-info.txt` inside the zip. */
    fun buildSystemInfo(): String {
        val rt = Runtime.getRuntime()
        val s = DesktopSettings.load()
        val os = "${System.getProperty("os.name")} ${System.getProperty("os.version")} (${System.getProperty("os.arch")})"
        return buildString {
            appendLine("[VIVI Music DE — system info]")
            appendLine("Generated: ${LocalDateTime.now()}")
            appendLine("App version: ${AppInfo.FULL_VERSION} (${AppInfo.CHANNEL})")
            appendLine("OS: $os")
            appendLine("Java: ${System.getProperty("java.version")} (${System.getProperty("java.vendor")})")
            appendLine("Architecture: ${System.getProperty("os.arch")}")
            appendLine("Processors: ${rt.availableProcessors()}")
            appendLine("Max heap: ${formatBytes(rt.maxMemory())}")
            appendLine("Used heap: ${formatBytes(rt.totalMemory() - rt.freeMemory())}")
            appendLine("Language: ${s.language}")
            appendLine("Dark mode: ${s.darkMode}")
            appendLine("Density scale: ${s.densityScale}")
            appendLine("Screen transitions: ${s.screenTransition}")
            appendLine("Notification mode: ${s.notificationMode}")
            appendLine("Sync URL: ${s.serverUrl}")
            appendLine("Paired: ${if (s.pairId.isNotBlank()) "yes" else "no"}")
            appendLine("Sync VIVI volume: ${s.syncViviVolume}")
            appendLine("Logged in: ${if (LoginManager.isLoggedIn()) "yes" else "no"}")
        }
    }

    /** Redacted copy of the persisted settings (cookie/account fields stripped). */
    fun buildSettingsSummary(): String {
        val s = DesktopSettings.load()
        return buildString {
            appendLine("[VIVI Music DE — settings summary (redacted)]")
            appendLine("notification history entries: ${s.notificationHistory.size}")
            appendLine("custom accents: ${s.customAccents.size}")
            appendLine("equalizer profiles: ${s.eqProfiles.size}")
            appendLine("audio quality: ${s.audioQuality}")
            appendLine("slider style: ${s.sliderStyle}")
            appendLine("canvas enabled: ${s.canvasEnabled}")
            appendLine("save notification history: ${s.saveNotificationHistory}")
            appendLine("pause search history: ${s.pauseSearchHistory}")
            appendLine("pause listen history: ${s.pauseListenHistory}")
            appendLine("lyrics animation style: ${s.lyricsAnimationStyle}")
            appendLine("lyrics thumbnail play/pause: ${s.lyricsThumbnailPlayPause}")
            appendLine("swipe to change song: ${s.swipeThumbnail} (sensitivity ${s.swipeSensitivity})")
            appendLine("ai provider: ${s.aiProvider}")
            appendLine("data saver: ${s.dataSaver}")
            appendLine("(cookie, dataSyncId, visitorData, account email and other"
                + " sensitive fields are intentionally excluded from this export)")
        }
    }

    /**
     * Writes every diagnostic file into [target] as a STORED zip entry.
     *
     * @return the list of file names written (for the success message).
     */
    fun export(target: File): List<String> {
        val entries = mutableListOf("system-info.txt", "settings-summary.txt")
        val logs = collectLogFiles()

        ZipOutputStream(FileOutputStream(target).buffered()).use { zip ->
            fun addEntry(name: String, bytes: ByteArray) {
                val entry = ZipEntry(name)
                entry.method = ZipEntry.STORED
                entry.size = bytes.size.toLong()
                entry.crc = CRC32().apply { update(bytes) }.value
                zip.putNextEntry(entry)
                zip.write(bytes)
                zip.closeEntry()
            }
            addEntry("system-info.txt", buildSystemInfo().toByteArray(Charsets.UTF_8))
            addEntry("settings-summary.txt", buildSettingsSummary().toByteArray(Charsets.UTF_8))
            for (log in logs) {
                val bytes = runCatching { log.readBytes() }.getOrNull() ?: continue
                val name = entryName(log)
                addEntry(name, bytes)
                entries.add(name)
            }
        }
        return entries
    }

    /** Writes a support-ready text dump to [target] (debugging aid, no zip). */
    fun exportPlain(target: File): List<String> {
        val logs = collectLogFiles()
        PrintWriter(target.writer(Charsets.UTF_8)).use { out ->
            out.println("=== system-info.txt ===")
            out.println(buildSystemInfo())
            out.println()
            out.println("=== settings-summary.txt ===")
            out.println(buildSettingsSummary())
            for (log in logs) {
                out.println()
                out.println("=== ${entryName(log)} ===")
                out.println(runCatching { log.readText() }.getOrDefault("<unreadable>"))
            }
        }
        return listOf("system-info.txt", "settings-summary.txt") + logs.map { entryName(it) }
    }
}