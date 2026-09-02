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

    /** What the window hands back after a sign-in attempt. */
    data class Capture(
        val cookie: String?,
        val dataSyncId: String?,
        val visitorData: String?,
    )

    fun isWindowOpen(): Boolean = windowOpen

    /** Starts JavaFX once, then creates the embedded login Stage. */
    fun openEmbedded(language: String, onCaptured: (Capture?) -> Unit): Boolean {
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
            deliver(null, null, null, onCaptured)
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

    private fun deliver(cookie: String?, dataSyncId: String?, visitorData: String?, callback: (Capture?) -> Unit) {
        if (delivered) return
        delivered = true
        runCatching { callback(Capture(cookie, dataSyncId, visitorData)) }
    }

    private fun createWindow(language: String, callback: (Capture?) -> Unit) {
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
                deliver(capture().header, null, null, callback)
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
                        // and re-capture the full header before closing. The ids
                        // come from the page itself (ytcfg), right there.
                        FxPlatform.runLater { browser.engine.load("https://music.youtube.com/") }
                        Thread.sleep(4500)
                        val ids = extractPageIds(browser)
                        val finalCap = capture()
                        logDebug(
                            "delivering full session: ${finalCap.names.size} cookies, missing=" +
                                "${finalCap.missing}, dataSyncId=${if (ids.first != null) "ok" else "MISSING"}, " +
                                "visitorData=${if (ids.second != null) "ok" else "MISSING"}"
                        )
                        deliver(finalCap.header ?: cap.header, ids.first, ids.second, callback)
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
                        deliver(cap.header, null, null, callback)
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
            deliver(null, null, null, callback)
        }
    }

    /**
     * Reads `DATASYNC_ID` and `VISITOR_DATA` straight from the loaded page's
     * ytcfg while the WebView is sitting on music.youtube.com with the session
     * — the same values that appear in the page source. These two are
     * mandatory (without them the account validation answers as guest), so
     * this is the authoritative source; the shell-fetch fallback in
     * [LoginManager] only runs when this capture comes back empty.
     */
    private fun extractPageIds(browser: WebView): Pair<String?, String?> {
        val result = arrayOfNulls<String>(2)
        val latch = CountDownLatch(1)
        FxPlatform.runLater {
            try {
                val getter = "(function(k){ try { if (window.ytcfg && window.ytcfg.get) { return window.ytcfg.get(k) || ''; } if (window.ytcfg && window.ytcfg.data_) { return window.ytcfg.data_[k] || ''; } return ''; } catch (e) { return ''; } })"
                result[0] = (browser.engine.executeScript("$getter('DATASYNC_ID')") as? String)
                    ?.takeIf { it.isNotBlank() }
                result[1] = (browser.engine.executeScript("$getter('VISITOR_DATA')") as? String)
                    ?.takeIf { it.isNotBlank() }
                if (result[0] == null || result[1] == null) {
                    logDebug("ytcfg ids incomplete: dataSyncId=${result[0] != null}, visitorData=${result[1] != null}")
                }
            } catch (t: Throwable) {
                logDebug("ytcfg extraction failed: $t")
            }
            latch.countDown()
        }
        runCatching { latch.await(5, TimeUnit.SECONDS) }
        return result[0] to result[1]
    }

    /**
     * Reads the cookies for youtube/google domains. The authoritative header
     * is the one the cookie handler itself would send to music.youtube.com —
     * the same domain/path/secure matching a browser applies when it builds
     * the Cookie header the manual method pastes (which is why manual paste
     * kept working while the WebView capture failed). SAPISID alone
     * authenticates nothing: the innertube account_menu validation answers
     * as guest (NPE) when the critical HttpOnly session cookies are missing.
     */
    private fun capture(): SessionCapture {
        val store = (CookieHandler.getDefault() as? CookieManager)?.cookieStore
        val cookies = store?.cookies.orEmpty().filter { c ->
            val domain = c.domain.removePrefix(".")
            domain.endsWith("youtube.com") || domain.endsWith("google.com")
        }
        // A cookie name can exist on several domains (e.g. SAPISID on .google.com
        // and .youtube.com). Keep the most specific domain per name, preferring
        // the youtube.com variant on ties: the domains have equal length, and
        // only the .youtube.com session authenticates the music.youtube.com API.
        val byName = cookies
            .groupBy { it.name }
            .mapValues { (_, list) ->
                list.maxByOrNull { (if ("youtube" in it.domain) 1 else 0) * 1000 + it.domain.length }
            }
            .mapNotNull { (_, c) -> c }
            .associateBy { it.name }
        // Primary header: ask the cookie handler which cookies it would send
        // to the API host. This applies the same domain/path/secure rules a
        // browser does, so the result matches the manually pasted header
        // exactly; a plain store dump mixes in cookies scoped to other
        // Google properties.
        val scopedHeader = runCatching {
            CookieHandler.getDefault()
                .get(URI("https://music.youtube.com/"), emptyMap<String, List<String>>())["Cookie"]
                ?.firstOrNull()
        }.getOrNull()?.takeIf { it.isNotBlank() }
        val scopedNames = scopedHeader.orEmpty().split(";")
            .mapNotNull { part -> part.substringBefore('=').trim().takeIf { n -> n.isNotEmpty() } }
            .toSet()
        val names = (scopedNames + cookies.map { it.name }).distinct().sorted()
        val critical = listOf("SID", "HSID", "SSID", "APISID", "__Secure-3PSID", "LOGIN_INFO")
        val missing = critical.filter { it !in byName && it !in scopedNames }
        val authNames = listOf("SAPISID", "__Secure-1PAPISID", "__Secure-3PAPISID")
        val hasSession = authNames.any { it in scopedNames } || authNames.any { byName.containsKey(it) }
        // A complete session has the SAPISID pair AND a session id cookie:
        // the legacy SID or either Secure PSID variant. Modern Google logins
        // often never issue the legacy SID — requiring it blocked the
        // full-session hand-over and made the WebView fail while the manual
        // paste of the very same session succeeded.
        val sessionIds = listOf("SID", "__Secure-1PSID", "__Secure-3PSID")
        val hasFullSession = hasSession &&
            (sessionIds.any { it in scopedNames } || sessionIds.any { byName.containsKey(it) })
        if (hasSession) {
            logDebug("captured ${cookies.size} cookies: $names | missing critical: $missing | scoped=${scopedNames.size}")
        }
        // Backfill: keep critical auth cookies the scoped lookup did not
        // return (store quirks), without duplicating names.
        val backfillNames = setOf(
            "SAPISID", "__Secure-1PAPISID", "__Secure-3PAPISID",
            "SID", "__Secure-1PSID", "__Secure-3PSID",
            "HSID", "SSID", "APISID", "LOGIN_INFO",
        )
        val backfill = byName.values
            .filter { it.name in backfillNames && it.name !in scopedNames }
            .joinToString("; ") { "${it.name}=${it.value}" }
        val header = when {
            !hasSession -> null
            scopedHeader == null -> byName.values.joinToString("; ") { "${it.name}=${it.value}" }
            backfill.isEmpty() -> scopedHeader
            else -> "$scopedHeader; $backfill"
        }
        return SessionCapture(
            header = header,
            names = names,
            missing = missing,
            hasSession = hasSession,
            hasFullSession = hasFullSession,
        )
    }
}
