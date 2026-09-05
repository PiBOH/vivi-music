package com.music.vivi.desktop

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.Serializable
import java.awt.Color
import java.awt.RenderingHints
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage
import java.util.concurrent.TimeUnit

/**
 * Unified notification dispatcher. All app notifications (update available,
 * device paired/unpaired, developer options unlocked, …) go through [notify],
 * which honors the user's notification mode: native OS notification when
 * `notificationMode == "native"`, otherwise an in-app banner (emitted through
 * [events] and rendered by the main window).
 */
object DesktopNotifier {
    data class Notice(val title: String, val message: String)

    private val _events = MutableSharedFlow<Notice>(extraBufferCapacity = 8)
    val events: SharedFlow<Notice> = _events.asSharedFlow()

    fun notify(title: String, message: String, section: String? = null) {
        val mode = if (DesktopSettings.load().notificationMode == "native") "native" else "in_app"
        NotificationHistory.record(title, message, mode)
        if (mode == "native") {
            NativeNotifier.notify(title, message, section)
        } else {
            _events.tryEmit(Notice(title, message))
        }
    }
}

/** One recorded notification (in-app or native), newest first in the list. */
@Serializable
data class NotificationRecord(
    val timestamp: Long,
    val title: String,
    val message: String,
    /** "in_app" or "native" — which channel actually showed it. */
    val mode: String,
)

/**
 * Persistent history of notifications shown by the app, so the user can review
 * them (regardless of whether they were in-app banners or native OS toasts).
 */
object NotificationHistory {
    private const val MAX_ENTRIES = 100

    fun record(title: String, message: String, mode: String) {
        runCatching {
            val state = DesktopSettings.load()
            if (!state.saveNotificationHistory) return
            val entry = NotificationRecord(System.currentTimeMillis(), title, message, mode)
            DesktopSettings.update { s ->
                s.copy(notificationHistory = (listOf(entry) + s.notificationHistory).take(MAX_ENTRIES))
            }
        }
    }

    fun list(): List<NotificationRecord> = runCatching { DesktopSettings.load().notificationHistory }.getOrDefault(emptyList())

    fun clear() {
        runCatching {
            DesktopSettings.update { it.copy(notificationHistory = emptyList()) }
        }
    }
}

/**
 * Best-effort native OS notification.
 *
 * On a packaged Windows build this uses a WinRT toast ([WindowsToast]) so the
 * notification lands in the Action Center and, when clicked, opens [section].
 * Everywhere else (and on unpackaged/dev Windows) it falls back to the legacy
 * `java.awt.SystemTray` balloon (works on most Linux desktops and macOS).
 * Every call is guarded — on unsupported systems it silently no-ops, and the
 * in-app fallback stays available.
 */
object NativeNotifier {

    @Volatile
    private var trayIcon: TrayIcon? = null

    /** Actions wired into the tray icon's right-click menu (Cider-style tray controls). */
    data class TrayActions(
        val onPlayPause: () -> Unit,
        val onNext: () -> Unit,
        val onPrevious: () -> Unit,
        val onOpen: () -> Unit,
        val onQuit: () -> Unit,
        val labelPlayPause: String = "Play/Pause",
        val labelNext: String = "Next",
        val labelPrevious: String = "Previous",
        val labelOpen: String = "Open VIVI Music",
        val labelQuit: String = "Quit",
    )

    @Volatile
    var trayActions: TrayActions? = null

    /** Shows a native system notification with [title] and [message]. */
    fun notify(title: String, message: String, section: String? = null) {
        runCatching {
            val osName = System.getProperty("os.name").orEmpty().lowercase()
            WindowsToast.log("NativeNotifier.notify: os=$osName title=\"$title\"")
            // macOS: SystemTray balloons are unreliable there, so use the
            // native UNUserNotificationCenter helper (bundled with the
            // media-session dylib) when available — real Notification Center
            // banners attributed to the app, working on Intel and Apple
            // Silicon. Fall back to `osascript` when the helper isn't loaded.
            if (osName.startsWith("mac")) {
                MacMediaSession.requestNotificationPermissionOnce()
                if (MacMediaSession.notify(title, message)) return
                WindowsToast.log("NativeNotifier: mac helper unavailable, falling back to osascript")
                if (macOsNotify(title, message)) return
                WindowsToast.log("NativeNotifier: osascript failed, falling back to SystemTray")
            }
            val winToast = WindowsToast.isAvailable()
            if (winToast) {
                WindowsToast.show(title, message, section)
                return
            }
            if (!SystemTray.isSupported()) {
                WindowsToast.log("NativeNotifier: SystemTray not supported, dropping")
                return
            }
            val tray = SystemTray.getSystemTray()
            val icon = ensureIcon(tray)
            icon.displayMessage(title, message, TrayIcon.MessageType.INFO)
        }
    }

    /**
     * Shows a macOS Notification Center banner via `osascript` (built into
     * every macOS). Returns true when the notification was actually posted.
     */
    private fun macOsNotify(title: String, message: String): Boolean = runCatching {
        fun esc(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"")
        val script = "display notification \"${esc(message)}\" with title \"${esc(title)}\""
        val p = ProcessBuilder("osascript", "-e", script)
            .redirectErrorStream(false)
            .start()
        val finished = p.waitFor(10, TimeUnit.SECONDS)
        if (finished) p.destroyForcibly()
        finished && p.exitValue() == 0
    }.getOrDefault(false)

    /**
     * Creates the tray icon eagerly (no notification needed) and (re)builds its
     * right-click menu from [actions]. Called when the app starts so the tray
     * controls are always available; call again when labels change (language).
     */
    fun configureTray(actions: TrayActions) {
        trayActions = actions
        runCatching {
            if (!SystemTray.isSupported()) return
            val icon = ensureIcon(SystemTray.getSystemTray())
            buildMenu(icon, actions)
            if (icon.image == null) icon.isImageAutoSize = true
        }
    }

    /** Sets the tray tooltip (e.g. the currently playing track). */
    fun setTrayTooltip(text: String) {
        runCatching { trayIcon?.toolTip = text }
    }

    /**
     * Removes the right-click menu AND the tray icon itself (toggle off),
     * so the setting applies live without a restart. The icon is recreated
     * lazily by [ensureIcon] when a notification needs it or the menu is
     * turned back on.
     */
    fun clearTrayMenu() {
        trayActions = null
        runCatching {
            val icon = trayIcon
            if (icon != null && SystemTray.isSupported()) {
                SystemTray.getSystemTray().remove(icon)
                trayIcon = null
            }
        }
    }

    private fun buildMenu(icon: TrayIcon, a: TrayActions) {
        val menu = java.awt.PopupMenu()
        fun add(label: String, action: () -> Unit) {
            val item = java.awt.MenuItem(label)
            item.addActionListener { runCatching { action() } }
            menu.add(item)
        }
        add(a.labelPlayPause, a.onPlayPause)
        add(a.labelNext, a.onNext)
        add(a.labelPrevious, a.onPrevious)
        menu.addSeparator()
        add(a.labelOpen, a.onOpen)
        add(a.labelQuit, a.onQuit)
        icon.popupMenu = menu
    }

    /**
     * Creates the tray icon once and keeps it for the app's lifetime. A
     * persistent icon (instead of add/remove on every notification) is more
     * reliable and keeps the VIVI logo consistent for every balloon.
     */
    private fun ensureIcon(tray: SystemTray): TrayIcon {
        trayIcon?.let { return it }
        return synchronized(this) {
            trayIcon ?: run {
                val icon = TrayIcon(trayImage(), "VIVI Music")
                icon.isImageAutoSize = true
                tray.add(icon)
                trayIcon = icon
                trayActions?.let { buildMenu(icon, it) }
                icon
            }
        }
    }

    /** Loads the bundled official VIVI Music DE logo (scaled to tray size) for the notification icon. */
    private fun trayImage(): BufferedImage {
        val stream = NativeNotifier::class.java.getResourceAsStream("/images/logo_vmde.png")
        if (stream != null) {
            val source = runCatching {
                stream.use { s -> javax.imageio.ImageIO.read(s) }
            }.getOrNull()
            if (source != null) {
                val size = runCatching { SystemTray.getSystemTray().trayIconSize.width }
                    .getOrDefault(16).coerceIn(16, 64)
                val scaled = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
                val g = scaled.createGraphics()
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
                g.drawImage(source, 0, 0, size, size, null)
                g.dispose()
                return scaled
            }
        }
        return fallbackTrayImage()
    }

    /** Placeholder glyph, used only if the bundled logo is missing (e.g. dev). */
    private fun fallbackTrayImage(): BufferedImage {
        val size = 32
        val img = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.color = Color(0xED, 0x55, 0x64) // VIVI accent
        g.fillRoundRect(0, 0, size, size, 10, 10)
        g.color = Color.WHITE
        g.font = g.font.deriveFont(18f).deriveFont(java.awt.Font.BOLD)
        g.drawString("♪", 8, 23)
        g.dispose()
        return img
    }
}
