package com.music.vivi.desktop

import javafx.application.Platform as FxPlatform
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.ProgressIndicator
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.web.WebView
import javafx.geometry.Insets
import javafx.scene.paint.Color
import javafx.scene.text.Font

import java.awt.Desktop
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.net.CookieHandler
import java.net.CookieManager
import java.net.URI
import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.WindowConstants

/**
 * Embedded YouTube sign-in window (JavaFX WebView).
 *
 * The window opens immediately when the user clicks "Sign in with Google" and
 * loads the **Google sign-in page directly** (not the YouTube Music home page),
 * so the user only has to do the one thing they know: sign in with their
 * Google account. When Google redirects back to YouTube Music, the session
 * cookies (SAPISID family on .youtube.com plus the .google.com identifiers)
 * are captured automatically and delivered to [onCaptured]; the window closes
 * by itself — the user never has to navigate YouTube Music.
 *
 * JavaFX is a real runtime dependency now, but the window still degrades
 * gracefully: if the toolkit cannot start (broken native install), the caller
 * falls back to the system browser and the manual cookie fields.
 */
object LoginWebView {

    @Volatile
    private var windowOpen = false

    @Volatile
    private var unavailable = false

    fun isWindowOpen(): Boolean = windowOpen

    /**
     * Opens the embedded sign-in window on a background thread (the JavaFX
     * toolkit must not be started on the Compose/AWT UI thread). Returns
     * `false` immediately when JavaFX is unavailable so the caller can fall
     * back; [onCaptured] receives the full captured `Cookie` header once the
     * login is detected, or `null` when the window closes without a login.
     */
    fun openEmbedded(language: String, onCaptured: (String?) -> Unit): Boolean =
        try {
            if (unavailable) return false
            Class.forName("javafx.embed.swing.JFXPanel")
            if (windowOpen) return true // already open, don't spawn a second one
            windowOpen = true
            Thread {
                try {
                    showWindow(language, onCaptured)
                } catch (t: Throwable) {
                    windowOpen = false
                    unavailable = true
                    runCatching { onCaptured(null) }
                }
            }.apply { name = "vivimusic-login-webview"; isDaemon = true }.start()
            true
        } catch (_: Throwable) {
            false
        }

    /** Opens YouTube Music in the system browser (fallback path). */
    fun openBrowser(): Boolean = runCatching {
        if (!Desktop.isDesktopSupported()) return@runCatching false
        Desktop.getDesktop().browse(URI("https://music.youtube.com/"))
        true
    }.getOrDefault(false)

    // ---------------------------------------------------------------- state

    @Volatile
    private var delivered = false
    @Volatile
    private var fxStatus: Label? = null
    @Volatile
    private var fxSpinner: ProgressIndicator? = null
    @Volatile
    private var frame: JFrame? = null

    private fun deliver(cookie: String?, onCaptured: (String?) -> Unit) {
        if (delivered) return
        delivered = true
        runCatching { onCaptured(cookie) }
    }

    // ---------------------------------------------------------------- window

    private fun showWindow(language: String, onCaptured: (String?) -> Unit) {
        delivered = false

        // The WebView persists cookies through the JVM-wide CookieHandler, so a
        // CookieManager must be installed BEFORE the first WebView is created.
        if (CookieHandler.getDefault() !is CookieManager) {
            CookieHandler.setDefault(CookieManager())
        }

        SwingUtilities.invokeLater {
            val panel = JFXPanel() // boots the JavaFX toolkit (must run on the EDT)

            FxPlatform.runLater {
                val status = Label(Localization.get(language, "login_waiting"))
                val spinner = ProgressIndicator().apply { isVisible = true; prefWidth = 18.0; prefHeight = 18.0 }
                fxStatus = status
                fxSpinner = spinner

                val steps = VBox(6.0).apply {
                    padding = Insets(10.0, 14.0, 6.0, 14.0)
                    children.addAll(
                        Label("1. " + Localization.get(language, "login_step1")).apply { font = Font.font(13.0) },
                        Label("2. " + Localization.get(language, "login_step2")).apply { font = Font.font(13.0) },
                        Label("3. " + Localization.get(language, "login_step3")).apply { font = Font.font(13.0); isWrapText = true },
                    )
                }

                val header = javafx.scene.layout.HBox(10.0).apply {
                    padding = Insets(10.0, 14.0, 10.0, 14.0)
                    background = Background(BackgroundFill(Color.web("#1f1f2e"), CornerRadii.EMPTY, Insets.EMPTY))
                    children.addAll(spinner, status)
                }

                val browser = WebView().apply {
                    // A plain Safari-like UA: the default JavaFX UA makes Google
                    // flag the embedded browser as "not secure".
                    engine.userAgent = buildString {
                        append(
                            when (Platform.os) {
                                DesktopOs.WINDOWS -> "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
                                DesktopOs.MACOS -> "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)"
                                DesktopOs.LINUX -> "Mozilla/5.0 (X11; Linux x86_64)"
                            },
                        )
                        append(" AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Safari/605.1")
                    }
                    // The Google sign-in page DIRECTLY — never the YT Music home,
                    // so the user always knows what to do.
                    engine.load("https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fmusic.youtube.com%2F")
                }

                val root = VBox(header, browser).apply {
                    VBox.setVgrow(browser, Priority.ALWAYS)
                }
                panel.scene = Scene(root, 1000.0, 720.0)
            }

            val f = JFrame("VIVI Music DE — ${Localization.get(language, "login")}").apply {
                defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
                setSize(1000, 720)
                setLocationRelativeTo(null)
                addWindowListener(object : WindowAdapter() {
                    override fun windowClosed(e: WindowEvent) {
                        windowOpen = false
                        // If the user closes the window right after the cookies
                        // appeared but before auto-close, still deliver.
                        val cookie = capturedCookieHeader()
                        deliver(if (cookie != null) cookie else null, onCaptured)
                    }
                })
                add(panel)
                isVisible = true
            }
            frame = f

            // Poll the cookie store until the YouTube session appears, then
            // hand it over and close the window automatically.
            Thread {
                while (windowOpen && !delivered) {
                    val cookie = capturedCookieHeader()
                    if (cookie != null) {
                        FxPlatform.runLater {
                            fxSpinner?.isVisible = false
                            fxStatus?.text = Localization.get(language, "login_saving")
                        }
                        Thread.sleep(1200) // let the redirect settle so the shell fetch sees the session
                        deliver(cookie, onCaptured)
                        SwingUtilities.invokeLater { f.dispose() }
                        break
                    }
                    Thread.sleep(1000)
                }
            }.apply { name = "vivimusic-login-cookie-poll"; isDaemon = true }.start()
        }
    }

    /**
     * Builds the full `Cookie` header from the embedded browser's cookie store:
     * everything on `youtube.com` (SAPISID, __Secure-1PAPISID, VISITOR_INFO1_LIVE, …)
     * plus everything on `google.com` (SID, HSID, SSID, …) — the same set the
     * browser would send to music.youtube.com. Returns null until the session
     * cookies exist, i.e. until the user has actually signed in.
     */
    private fun capturedCookieHeader(): String? {
        val store = (CookieHandler.getDefault() as? CookieManager)?.cookieStore ?: return null
        val cookies = store.cookies.filter { c ->
            val d = c.domain.removePrefix(".")
            d.endsWith("youtube.com") || d.endsWith("google.com")
        }
        val hasSession = cookies.any { it.name == "SAPISID" || it.name == "__Secure-1PAPISID" || it.name == "__Secure-3PAPISID" }
        if (!hasSession) return null
        return cookies.joinToString("; ") { "${it.name}=${it.value}" }.takeIf { it.isNotBlank() }
    }
}
