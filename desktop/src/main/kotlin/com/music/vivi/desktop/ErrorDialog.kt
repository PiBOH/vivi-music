package com.music.vivi.desktop

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.SwingUtilities

/**
 * Installs a global uncaught-exception handler that shows a dialog with both a
 * "Copy error" action (copies the full message + stack trace to the clipboard)
 * and "OK", instead of the default AWT "Error" dialog that only offers OK.
 * Called once at startup, before the Compose window is created.
 *
 * A Skiko OpenGL crash on Linux is handled specially (see
 * [handleSkikoGlCrash]): instead of a dialog, the app relaunches once with the
 * software renderer so users without a working GL stack (e.g. the AppImage on
 * Arch) aren't greeted by an error dialog on every start.
 */
fun installGlobalErrorDialog() {
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        // Persist the crash before doing anything else: ~/.vivimusic/crash.log is
        // always overwritten with the most recent crash, while a timestamped
        // copy under ~/.vivimusic/logs/<timestamp>/crash_<timestamp>.log is kept
        // (never overwritten), so past crashes survive for diagnosis (issue #59).
        runCatching { writeCrashDump(thread, throwable) }
        if (!handleSkikoGlCrash(throwable)) {
            runCatching { showErrorDialog(throwable) }
        }
        previous?.uncaughtException(thread, throwable)
    }
}

/**
 * Writes a crash dump with the stack trace to the two crash log locations:
 *  - `~/.vivimusic/crash.log` — always overwritten with the latest crash;
 *  - `~/.vivimusic/logs/<yyyyMMdd-HHmmss>/crash_<yyyyMMdd-HHmmss>.log` — each
 *    crash gets its own timestamped file so it is never overwritten.
 * Synchronous on purpose: the JVM may be about to die.
 */
private fun writeCrashDump(thread: Thread, throwable: Throwable) {
    val vivimusic = File(System.getProperty("user.home"), ".vivimusic")
    val header = buildString {
        appendLine("VIVI Music DE crash dump")
        appendLine("Time: ${LocalDateTime.now()}")
        appendLine("App version: ${runCatching { AppInfo.FULL_VERSION }.getOrNull() ?: "unknown"} (${runCatching { AppInfo.CHANNEL }.getOrNull() ?: "unknown"})")
        appendLine("OS: ${System.getProperty("os.name")} ${System.getProperty("os.version")} (${System.getProperty("os.arch")})")
        appendLine("Java: ${System.getProperty("java.version")} (${System.getProperty("java.vendor")})")
        appendLine("Thread: ${thread.name} (${if (thread.isDaemon) "daemon" else "normal"})")
        appendLine("Throwable: ${throwable.javaClass.name}: ${throwable.message}")
    }
    val body = buildString {
        append(header)
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        append(sw.toString())
        // Include suppressed exceptions, which AWT/Swing exceptions often carry.
        throwable.suppressed.forEachIndexed { i, s ->
            append("\nSuppressed #$i: ${s.javaClass.name}: ${s.message}\n")
            val sw2 = StringWriter()
            s.printStackTrace(PrintWriter(sw2))
            append(sw2.toString())
        }
    }
    vivimusic.mkdirs()
    // 1) Root crash.log — overwritten with the newest crash.
    runCatching { File(vivimusic, "crash.log").writeText(body, Charsets.UTF_8) }
    // 2) Timestamped session + file — kept forever, never clobbered.
    runCatching {
        val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        val dir = File(File(vivimusic, "logs"), stamp)
        dir.mkdirs()
        File(dir, "crash_$stamp.log").writeText(body, Charsets.UTF_8)
    }
}

private val glRelaunchAttempted = AtomicBoolean(false)

/** Marker file read by [com.music.vivi.desktop.MainKt.configureRenderApi] on
 * the next launch: if it exists, the app starts directly in software rendering
 * instead of crashing again on OpenGL. */
private fun glSoftwareMarkerFile(): File =
    File(System.getProperty("user.home"), ".vivimusic/.gl-software")

/**
 * True when the crash is the Skiko OpenGL `UnsatisfiedLinkError` (Linux):
 * `OpenGLApi.glFlush()` throws because the GL function pointers were never
 * registered, which skiko's own render-API fallback does not catch (it only
 * handles `RenderException`). This is an Error, so it reaches this handler
 * even though the failure happens during the very first frame render.
 */
private fun isSkikoGlCrash(throwable: Throwable): Boolean {
    if (throwable !is UnsatisfiedLinkError) return false
    val message = throwable.message.orEmpty()
    if (message.contains("skiko", ignoreCase = true)) return true
    return throwable.stackTrace.any {
        it.className.startsWith("org.jetbrains.skiko") &&
            (it.methodName.contains("OpenGL") || it.methodName.contains("render"))
    }
}

/**
 * Handles the Skiko OpenGL startup crash by marking the machine and relaunching
 * once with `SKIKO_RENDER_API=SOFTWARE`.
 *
 * @return true if the crash was handled (dialog skipped). Returns false when
 * the crash isn't a GL crash, or when this process is already the retried one
 * (`VIVI_GL_RETRY=1`) — in that case the normal dialog is shown so the user
 * sees something even if software rendering also fails.
 */
private fun handleSkikoGlCrash(throwable: Throwable): Boolean {
    if (!isSkikoGlCrash(throwable)) return false
    if (System.getenv("VIVI_GL_RETRY") == "1") return false
    if (!glRelaunchAttempted.compareAndSet(false, true)) return true
    runCatching {
        glSoftwareMarkerFile().apply {
            parentFile?.mkdirs()
            writeText("Automatic software-rendering fallback (Skiko OpenGL crash on ${System.getProperty("os.name")}).")
        }
        restartApplication(
            mapOf(
                "SKIKO_RENDER_API" to "SOFTWARE",
                "VIVI_GL_RETRY" to "1",
            )
        )
    }
    return true
}

private fun showErrorDialog(throwable: Throwable) {
    val full = fullStack(throwable)
    val run = {
        val area = JTextArea(full)
        area.isEditable = false
        area.lineWrap = false
        val scroll = JScrollPane(area)
        scroll.preferredSize = Dimension(560, 220)

        val panel = JPanel(BorderLayout(0, 8))
        panel.add(JLabel(shortMessage(throwable)), BorderLayout.NORTH)
        panel.add(scroll, BorderLayout.CENTER)

        val options = arrayOf("Copy error", "OK")
        val choice = JOptionPane.showOptionDialog(
            null,
            panel,
            "Error",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.ERROR_MESSAGE,
            null,
            options,
            options[1],
        )
        if (choice == 0) {
            runCatching {
                Toolkit.getDefaultToolkit().systemClipboard
                    .setContents(StringSelection(full), null)
            }
        }
    }
    if (SwingUtilities.isEventDispatchThread()) run() else SwingUtilities.invokeLater(run)
}

private fun shortMessage(t: Throwable): String =
    t.message?.takeIf { it.isNotBlank() } ?: t.javaClass.simpleName ?: "Error"

/** Full stack trace, capped so the dialog never becomes enormous. */
private fun fullStack(t: Throwable): String {
    val sw = StringWriter()
    t.printStackTrace(PrintWriter(sw))
    return sw.toString().take(8000)
}
