package com.music.vivi.desktop

import javafx.application.Platform as FxPlatform
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
import javafx.stage.Stage

import java.awt.Desktop
import java.net.CookieHandler
import java.net.CookieManager
import java.net.URI
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Embedded YouTube sign-in window using JavaFX directly.
 *
 * JavaFX is initialized with `Platform.startup` exactly once. This is important
 * in a Compose Desktop process: `Application.launch` is single-use and can race
 * with the already-running AWT/Compose event loop, causing the WebView startup
 * failure to be reported repeatedly. No JFXPanel/Swing interop is used.
 */
object LoginWebView {
    @Volatile private var windowOpen = false
    @Volatile private var unavailable = false
    @Volatile private var delivered = false
    @Volatile private var fxStarted = false
    private val fxStartupLock = Any()

    fun isWindowOpen(): Boolean = windowOpen

    /** Starts JavaFX once, then creates the embedded login Stage. */
    fun openEmbedded(language: String, onCaptured: (String?) -> Unit): Boolean {
        if (unavailable || windowOpen) return !unavailable
        return try {
            if (CookieHandler.getDefault() !is CookieManager) {
                CookieHandler.setDefault(CookieManager())
            }
            windowOpen = true
            delivered = false
            ensureFxStarted()
            FxPlatform.runLater { createWindow(language, onCaptured) }
            true
        } catch (_: Throwable) {
            windowOpen = false
            unavailable = true
            deliver(null, onCaptured)
            false
        }
    }

    /** Opens the direct Google sign-in page in the system browser as fallback. */
    fun openBrowser(): Boolean = runCatching {
        if (!Desktop.isDesktopSupported()) return@runCatching false
        Desktop.getDesktop().browse(URI("https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fmusic.youtube.com%2F"))
        true
    }.getOrDefault(false)

    private fun ensureFxStarted() {
        if (fxStarted) return
        synchronized(fxStartupLock) {
            if (fxStarted) return
            val failure = arrayOfNulls<Throwable>(1)
            val ready = CountDownLatch(1)
            Thread {
                try {
                    FxPlatform.startup { ready.countDown() }
                } catch (t: IllegalStateException) {
                    // Toolkit was started by another component between the
                    // check and startup; it is safe to use runLater now.
                    ready.countDown()
                } catch (t: Throwable) {
                    failure[0] = t
                    ready.countDown()
                }
            }.apply { name = "vivimusic-javafx-startup"; isDaemon = true }.start()
            if (!ready.await(15, TimeUnit.SECONDS)) {
                throw IllegalStateException("JavaFX toolkit startup timed out")
            }
            failure[0]?.let { throw it }
            fxStarted = true
        }
    }

    private fun deliver(cookie: String?, callback: (String?) -> Unit) {
        if (delivered) return
        delivered = true
        runCatching { callback(cookie) }
    }

    private fun createWindow(language: String, callback: (String?) -> Unit) {
        try {
            val stage = Stage()
            val status = Label(Localization.get(language, "login_waiting"))
            val spinner = ProgressIndicator().apply {
                prefWidth = 18.0
                prefHeight = 18.0
            }
            val header = HBox(10.0, spinner, status).apply {
                padding = Insets(10.0, 14.0, 10.0, 14.0)
                background = Background(BackgroundFill(Color.web("#1f1f2e"), CornerRadii.EMPTY, Insets.EMPTY))
            }
            val steps = VBox(6.0).apply {
                padding = Insets(10.0, 14.0, 6.0, 14.0)
                children.addAll(
                    Label("1. " + Localization.get(language, "login_step1")).apply { font = Font.font(13.0) },
                    Label("2. " + Localization.get(language, "login_step2")).apply { font = Font.font(13.0) },
                    Label("3. " + Localization.get(language, "login_step3")).apply { font = Font.font(13.0); isWrapText = true },
                )
            }
            val browser = WebView().apply {
                engine.userAgent = when (Platform.os) {
                    DesktopOs.WINDOWS -> "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/131 Safari/537.36"
                    DesktopOs.MACOS -> "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 Chrome/131 Safari/537.36"
                    DesktopOs.LINUX -> "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/131 Safari/537.36"
                }
                engine.load("https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fmusic.youtube.com%2F")
            }
            val root = VBox(header, steps, browser).apply {
                VBox.setVgrow(browser, Priority.ALWAYS)
            }
            stage.title = "VIVI Music DE — ${Localization.get(language, "login")}"
            stage.scene = Scene(root, 1000.0, 720.0)
            stage.setOnCloseRequest {
                windowOpen = false
                deliver(capturedCookieHeader(), callback)
            }
            stage.show()

            Thread {
                while (windowOpen && !delivered) {
                    val cookie = capturedCookieHeader()
                    if (cookie != null) {
                        FxPlatform.runLater {
                            spinner.isVisible = false
                            status.text = Localization.get(language, "login_saving")
                        }
                        Thread.sleep(1200)
                        deliver(cookie, callback)
                        FxPlatform.runLater { stage.close() }
                        break
                    }
                    Thread.sleep(1000)
                }
            }.apply { name = "vivimusic-login-cookie-poll"; isDaemon = true }.start()
        } catch (t: Throwable) {
            windowOpen = false
            unavailable = true
            deliver(null, callback)
        }
    }

    private fun capturedCookieHeader(): String? {
        val store = (CookieHandler.getDefault() as? CookieManager)?.cookieStore ?: return null
        val cookies = store.cookies.filter { c ->
            val domain = c.domain.removePrefix(".")
            domain.endsWith("youtube.com") || domain.endsWith("google.com")
        }
        val hasSession = cookies.any {
            it.name == "SAPISID" || it.name == "__Secure-1PAPISID" || it.name == "__Secure-3PAPISID"
        }
        return if (hasSession) cookies.joinToString("; ") { "${it.name}=${it.value}" } else null
    }
}
