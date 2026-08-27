package com.music.vivi.desktop

import javafx.application.Application
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

/**
 * Embedded YouTube sign-in window using JavaFX directly.
 *
 * This deliberately uses `Stage`, not `JFXPanel`: JFXPanel requires the JDK
 * `jdk.swing.interop` module, which is not present in the Temurin JDK runtime
 * image used by the GitHub Actions runners. A JavaFX Stage needs no Swing
 * bridge, so it works in the packaged runtime on Windows, Linux and macOS.
 */
object LoginWebView {
    @Volatile private var windowOpen = false
    @Volatile private var unavailable = false
    @Volatile private var delivered = false

    fun isWindowOpen(): Boolean = windowOpen

    /** Starts the JavaFX application and opens the embedded login window. */
    fun openEmbedded(language: String, onCaptured: (String?) -> Unit): Boolean {
        if (unavailable) return false
        if (windowOpen) return true
        return try {
            if (CookieHandler.getDefault() !is CookieManager) {
                CookieHandler.setDefault(CookieManager())
            }
            windowOpen = true
            delivered = false
            LoginApplication.configure(language, onCaptured)
            Thread {
                try {
                    Application.launch(LoginApplication::class.java)
                } catch (_: IllegalStateException) {
                    // JavaFX can only be launched once per JVM. If the toolkit
                    // was already started, the configured Stage is created by
                    // the existing toolkit instead.
                    unavailable = true
                    windowOpen = false
                    deliver(null, onCaptured)
                } catch (_: Throwable) {
                    unavailable = true
                    windowOpen = false
                    deliver(null, onCaptured)
                }
            }.apply { name = "vivimusic-login-webview"; isDaemon = true }.start()
            true
        } catch (_: Throwable) {
            unavailable = true
            windowOpen = false
            false
        }
    }

    fun openBrowser(): Boolean = runCatching {
        if (!Desktop.isDesktopSupported()) return@runCatching false
        Desktop.getDesktop().browse(URI("https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fmusic.youtube.com%2F"))
        true
    }.getOrDefault(false)

    private fun deliver(cookie: String?, callback: (String?) -> Unit) {
        if (delivered) return
        delivered = true
        runCatching { callback(cookie) }
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

    /** JavaFX Application instance used by [Application.launch]. */
    class LoginApplication : Application() {
        override fun start(stage: Stage) {
            val language = configuredLanguage
            val callback = configuredCallback
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
            stage.title = "VIVI Music DE — ${Localization.get(language, "login") }"
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
        }

        companion object {
            @Volatile var configuredLanguage: String = "en"
            @Volatile var configuredCallback: (String?) -> Unit = {}
            fun configure(language: String, callback: (String?) -> Unit) {
                configuredLanguage = language
                configuredCallback = callback
            }
        }
    }
}
