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
import java.io.File
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

    private val debugLog = File(System.getProperty("user.home"), ".vivimusic/login-debug.log")

    private fun logDebug(msg: String) {
        runCatching {
            debugLog.parentFile?.mkdirs()
            debugLog.appendText("[${java.time.LocalDateTime.now()}] $msg\n")
        }
    }

    private data class SessionCapture(
        val header: String?,
        val names: List<String>,
        val missing: List<String>,
        val hasSession: Boolean,
        val hasFullSession: Boolean,
    )

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
            // The packaged app runs a Compose/Skia window on the same display;
            // on machines with weak or conflicting GPU drivers the JavaFX WebView
            // then stays blank white even though the page loaded (paint never
            // happens). The WebView is the only JavaFX surface we have, so force
            // the software renderer: slower but guaranteed to paint.
            runCatching {
                if (System.getProperty("prism.order") == null) {
                    System.setProperty("prism.order", "sw")
                }
                if (System.getProperty("prism.dirtyopts") == null) {
                    System.setProperty("prism.dirtyopts", "false")
                }
            }
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
                prefWidth = 1000.0
                prefHeight = 640.0
                minWidth = 400.0
                minHeight = 300.0
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
                deliver(capture().header, callback)
            }
            stage.show()

            // Kick the WebView so it paints its first frame. In a process where
            // Compose/AWT already owns the display, the WebView can stay blank
            // (known JavaFX painting bug) until it is nudged: force a re-layout
            // once the page starts loading, and again on load success.
            browser.engine.loadWorker.stateProperty().addListener { _, _, newState ->
                val loaded = newState == javafx.concurrent.Worker.State.SUCCEEDED
                val running = newState == javafx.concurrent.Worker.State.RUNNING
                if (loaded || running) {
                    FxPlatform.runLater {
                        browser.resize(browser.width + 1.0, browser.height)
                        browser.resize(browser.width - 1.0, browser.height)
                        browser.requestLayout()
                        println("[login-webview] state=$newState size=${browser.width}x${browser.height} title=${browser.engine.title}")
                    }
                }
            }

            Thread {
                val deadline = System.currentTimeMillis() + 120_000
                while (windowOpen && !delivered) {
                    val cap = capture()
                    if (cap.hasFullSession) {
                        FxPlatform.runLater {
                            spinner.isVisible = false
                            status.text = Localization.get(language, "login_saving")
                        }
                        // Reload music.youtube.com WITH the session cookie so
                        // every youtube.com session cookie is set, then settle
                        // and re-capture the full header before closing.
                        FxPlatform.runLater { browser.engine.load("https://music.youtube.com/") }
                        Thread.sleep(4500)
                        val finalCap = capture()
                        logDebug("delivering full session: ${finalCap.names.size} cookies, missing=${finalCap.missing}")
                        deliver(finalCap.header ?: cap.header, callback)
                        FxPlatform.runLater { stage.close() }
                        break
                    }
                    if (cap.hasSession && System.currentTimeMillis() > deadline - 60_000) {
                        // A session cookie appeared but the critical set never
                        // completed: hand over what we have (validation will
                        // fail; the captured header is kept in the manual field
                        // for a one-click retry) and log the missing names.
                        logDebug("delivering PARTIAL session, missing critical: ${cap.missing}")
                        FxPlatform.runLater {
                            spinner.isVisible = false
                            status.text = Localization.get(language, "login_saving")
                        }
                        deliver(cap.header, callback)
                        FxPlatform.runLater { stage.close() }
                        break
                    }
                    if (System.currentTimeMillis() > deadline) {
                        logDebug("capture timeout — no session cookies")
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

    /**
     * Reads the cookies stored for youtube/google domains. The full set is
     * required: SAPISID alone authenticates nothing — the innertube
     * account_menu validation answers as guest (NPE) when the critical
     * HttpOnly session cookies (SID, HSID, SSID, APISID, __Secure-3PSID, …)
     * are missing, which is exactly the "Login validation failed" the user
     * saw after the window closed.
     */
    private fun capture(): SessionCapture {
        val store = (CookieHandler.getDefault() as? CookieManager)?.cookieStore
        val cookies = store?.cookies.orEmpty().filter { c ->
            val domain = c.domain.removePrefix(".")
            domain.endsWith("youtube.com") || domain.endsWith("google.com")
        }
        // A cookie name can exist on several domains (e.g. SAPISID on .google.com
        // and .youtube.com). Keep the most specific domain (longest) per name so
        // the Authorization hash is computed with the session cookie that
        // actually authenticates the music.youtube.com API.
        val byName = cookies
            .groupBy { it.name }
            .mapValues { (_, list) -> list.maxByOrNull { it.domain.length } }
            .mapNotNull { (_, c) -> c }
            .associateBy { it.name }
        val names = cookies.map { it.name }.distinct().sorted()
        val critical = listOf("SID", "HSID", "SSID", "APISID", "__Secure-3PSID", "LOGIN_INFO")
        val missing = critical.filter { it !in byName }
        val hasSession = byName.containsKey("SAPISID") ||
            byName.containsKey("__Secure-1PAPISID") ||
            byName.containsKey("__Secure-3PAPISID")
        // A complete session has at least the SAPISID pair AND the HttpOnly
        // session id (SID / __Secure-3PSID).
        val hasFullSession = hasSession && byName.containsKey("SID") && byName.containsKey("__Secure-3PSID")
        if (hasSession) {
            logDebug("captured ${cookies.size} cookies: $names | missing critical: $missing")
        }
        return SessionCapture(
            header = if (hasSession) byName.values.joinToString("; ") { "${it.name}=${it.value}" } else null,
            names = names,
            missing = missing,
            hasSession = hasSession,
            hasFullSession = hasFullSession,
        )
    }
}
