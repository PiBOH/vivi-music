package com.music.vivi.desktop

import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Global media-key capture (Cider-style): intercepts the Play/Pause, Next,
 * Previous and Stop keys even when the app has no focus, exactly like a
 * native media player.
 *
 * - Windows: a `WH_KEYBOARD_LL` low-level hook delivered through a native
 *   message pump on its own daemon thread (`GetMessageW` loop).
 * - macOS/Linux: [JNativeHook]'s cross-platform global hook (its API is the
 *   same JNA-style approach). On macOS the OS asks for **Accessibility**
 *   permission the first time (System Settings → Privacy & Security); until
 *   it is granted the hook fails gracefully and media keys stay inert.
 *
 * Every failure is swallowed — a broken hook must never crash or block the app.
 */
object MediaKeys {

    // Virtual-key codes for the Windows multimedia keys.
    private const val VK_MEDIA_NEXT_TRACK = 0xB0
    private const val VK_MEDIA_PREV_TRACK = 0xB1
    private const val VK_MEDIA_STOP = 0xB2
    private const val VK_MEDIA_PLAY_PAUSE = 0xB3

    private const val WH_KEYBOARD_LL = 13
    private const val WM_KEYDOWN = 0x0100
    private const val WM_SYSKEYDOWN = 0x0104

    /** Overrides set by [start]; read from the hook thread. */
    @Volatile
    private var onPlayPause: (() -> Unit)? = null
    @Volatile
    private var onNext: (() -> Unit)? = null
    @Volatile
    private var onPrevious: (() -> Unit)? = null
    @Volatile
    private var onStop: (() -> Unit)? = null

    private val started = AtomicBoolean(false)

    // JNativeHook (macOS/Linux) state.
    private val jnativeStarted = AtomicBoolean(false)
    private var jnativeListener: com.github.kwhat.jnativehook.keyboard.NativeKeyListener? = null

    /** Minimal user32 surface (explicit `W` names, no jna-platform needed). */
    private interface User32LL : Library {
        fun SetWindowsHookExW(idHook: Int, lpfn: HookProc, hMod: Pointer?, dwThreadId: Int): Pointer?
        fun CallNextHookEx(hhk: Pointer?, nCode: Int, wParam: Int, lParam: Pointer?): Pointer?
        fun UnhookWindowsHookEx(hhk: Pointer?): Boolean
        fun GetMessageW(lpMsg: Pointer?, hWnd: Pointer?, wMsgFilterMin: Int, wMsgFilterMax: Int): Int
        fun TranslateMessage(lpMsg: Pointer?): Boolean
        fun DispatchMessageW(lpMsg: Pointer?): Pointer?

        fun interface HookProc : Callback {
            fun callback(nCode: Int, wParam: Int, lParam: Pointer?): Pointer?
        }
    }

    private val user32: User32LL? = runCatching {
        Native.load("user32", User32LL::class.java)
    }.getOrNull()

    private val isWindows: Boolean
        get() = System.getProperty("os.name", "").lowercase().contains("win")

    /** The handle returned by [androidx.compose.ui.window.Window] is irrelevant
     *  here — the hook is global by design (dwThreadId = 0). */

    /**
     * Installs the global hook. Idempotent; safe to call from any thread.
     * [onStop] is optional (the Stop key is rarely present on modern boards).
     */
    fun start(
        onPlayPause: () -> Unit,
        onNext: () -> Unit,
        onPrevious: () -> Unit,
        onStop: () -> Unit = {},
    ) {
        this.onPlayPause = onPlayPause
        this.onNext = onNext
        this.onPrevious = onPrevious
        this.onStop = onStop
        if (!isWindows) {
            startJNativeHook()
            return
        }
        if (!started.compareAndSet(false, true)) return
        val api = user32 ?: run { started.set(false); return }

        Thread {
            var hook: Pointer? = null
            try {
                // The hook callback: returns 1 to swallow media keys (like a
                // native media player takes exclusive control), otherwise
                // forwards to the next hook.
                val proc = User32LL.HookProc { nCode, wParam, lParam ->
                    try {
                        if (nCode >= 0 && (wParam == WM_KEYDOWN || wParam == WM_SYSKEYDOWN) && lParam != null) {
                            val vk = lParam.getInt(0)
                            when (vk) {
                                VK_MEDIA_PLAY_PAUSE -> { onPlayPause?.invoke(); return@HookProc Pointer(1L) }
                                VK_MEDIA_NEXT_TRACK -> { onNext?.invoke(); return@HookProc Pointer(1L) }
                                VK_MEDIA_PREV_TRACK -> { onPrevious?.invoke(); return@HookProc Pointer(1L) }
                                VK_MEDIA_STOP -> { onStop?.invoke(); return@HookProc Pointer(1L) }
                            }
                        }
                    } catch (_: Throwable) {
                        // Never let a callback failure escape into native code.
                    }
                    api.CallNextHookEx(hook, nCode, wParam, lParam)
                }

                hook = api.SetWindowsHookExW(WH_KEYBOARD_LL, proc, null, 0)
                if (hook == null) return@Thread // runCatching below logs nothing; silent no-op

                // Pump messages: the LL hook is only delivered to a thread that
                // runs a native message loop. MSG on 64-bit Windows is 48 bytes;
                // 64 is plenty on both 32/64-bit.
                val msg = Memory(64L)
                while (true) {
                    val r = api.GetMessageW(msg, null, 0, 0)
                    if (r <= 0) break // -1 error, 0 WM_QUIT
                    api.TranslateMessage(msg)
                    api.DispatchMessageW(msg)
                }
            } catch (_: Throwable) {
                // Silent: the hook is best-effort.
            } finally {
                runCatching { hook?.let { api.UnhookWindowsHookEx(it) } }
            }
        }.apply {
            isDaemon = true
            name = "VIVI-MediaKeys"
            start()
        }
    }

    /**
     * Cross-platform global hook via JNativeHook (macOS/Linux). Best-effort:
     * without macOS Accessibility permission the native hook cannot register
     * and nothing happens — the app must not crash because of it.
     */
    private fun startJNativeHook() {
        if (!jnativeStarted.compareAndSet(false, true)) return
        try {
            // JNativeHook logs loudly through java.util.logging by default;
            // quiet it so the console stays clean.
            runCatching {
                java.util.logging.Logger.getLogger("com.github.kwhat.jnativehook")
                    .level = java.util.logging.Level.OFF
            }
            com.github.kwhat.jnativehook.GlobalScreen.registerNativeHook()
        } catch (t: Throwable) {
            println(
                "[media-keys] global hook unavailable on ${System.getProperty("os.name")}: $t" +
                    (if (isMac) " — grant VIVI Accessibility permission in System Settings → Privacy & Security" else "")
            )
            jnativeStarted.set(false)
            return
        }
        val listener = object : com.github.kwhat.jnativehook.keyboard.NativeKeyListener {
            override fun nativeKeyPressed(e: com.github.kwhat.jnativehook.keyboard.NativeKeyEvent) {
                try {
                    when (e.keyCode) {
                        com.github.kwhat.jnativehook.keyboard.NativeKeyEvent.VC_MEDIA_PLAY -> onPlayPause?.invoke()
                        com.github.kwhat.jnativehook.keyboard.NativeKeyEvent.VC_MEDIA_NEXT -> onNext?.invoke()
                        com.github.kwhat.jnativehook.keyboard.NativeKeyEvent.VC_MEDIA_PREVIOUS -> onPrevious?.invoke()
                        com.github.kwhat.jnativehook.keyboard.NativeKeyEvent.VC_MEDIA_STOP -> onStop?.invoke()
                    }
                } catch (_: Throwable) {
                    // Never let a callback failure escape into native code.
                }
            }

            override fun nativeKeyReleased(e: com.github.kwhat.jnativehook.keyboard.NativeKeyEvent) {}
            override fun nativeKeyTyped(e: com.github.kwhat.jnativehook.keyboard.NativeKeyEvent) {}
        }
        jnativeListener = listener
        com.github.kwhat.jnativehook.GlobalScreen.addNativeKeyListener(listener)
    }

    private val isMac: Boolean
        get() = System.getProperty("os.name", "").lowercase().contains("mac")

    /** Removes the hooks (used on shutdown; the daemon thread exits on its own). */
    fun stop() {
        onPlayPause = null
        onNext = null
        onPrevious = null
        onStop = null
        runCatching {
            if (jnativeStarted.compareAndSet(true, false)) {
                jnativeListener?.let { com.github.kwhat.jnativehook.GlobalScreen.removeNativeKeyListener(it) }
                jnativeListener = null
                com.github.kwhat.jnativehook.GlobalScreen.unregisterNativeHook()
            }
        }
    }
}
