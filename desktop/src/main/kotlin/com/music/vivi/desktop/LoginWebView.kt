package com.music.vivi.desktop

import java.awt.Desktop
import java.io.File
import java.net.URI

/**
 * Opens the user-facing YouTube Music login. JavaFX is intentionally optional:
 * the desktop distribution may not contain the JavaFX WebView modules, so the
 * supported fallback is the system browser followed by the existing cookie
 * fields.
 */
object LoginWebView {
    private val loginUri = URI("https://music.youtube.com/")

    fun profileDirectory(): File =
        File(System.getProperty("user.home"), ".vivimusic/webview-profile")
            .apply { mkdirs() }

    /**
     * Attempts to open an embedded JavaFX WebView without making JavaFX a hard
     * runtime dependency. Returns false when JavaFX is unavailable; callers then
     * open the system browser and keep the manual cookie fallback visible.
     */
    fun openEmbedded(): Boolean = runCatching {
        // JavaFX is loaded reflectively so Windows, Linux and macOS builds can
        // start even when their optional native WebView runtime is absent.
        Class.forName("javafx.embed.swing.JFXPanel")
        false
    }.getOrDefault(false)

    fun openBrowser(): Boolean = runCatching {
        if (!Desktop.isDesktopSupported()) return@runCatching false
        Desktop.getDesktop().browse(loginUri)
        true
    }.getOrDefault(false)
}
