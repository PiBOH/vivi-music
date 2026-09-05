package com.music.vivi.desktop

import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Native
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * macOS system "Now Playing" integration (issue #5).
 *
 * Loads the bundled native helper (`desktop/native/ViviMediaSession.m`, compiled
 * per-architecture by the CI workflow into `native/macos-<arch>/`) through JNA
 * and uses MediaPlayer.framework to:
 *  1. register the app as the system media session (Control Center / Lock
 *     Screen "Now Playing" tile + media keys on keyboard/Touch Bar/headsets —
 *     no Accessibility permission needed), and
 *  2. expose Play/Pause, Next, Previous and scrub-to-position as remote
 *     commands that drive the same PlayerController as the rest of the app.
 *
 * Metadata (title/artist/duration/position/playing) is pushed from Main.kt via
 * [setNowPlaying]; artwork is downloaded on a background thread into
 * `~/.vivimusic/` and handed to the native side as a local file path.
 *
 * Every failure is swallowed: the session must never crash or block the app.
 * On non-macOS this object is inert (nothing to load).
 */
object MacMediaSession {

    private val isMac: Boolean
        get() = System.getProperty("os.name", "").lowercase().contains("mac")

    private val started = AtomicBoolean(false)

    // Native callback signatures (JNA marshals these across the FFI boundary).
    private fun interface VoidCb : Callback {
        fun invoke()
    }

    private fun interface SeekCb : Callback {
        fun invoke(positionMs: Double)
    }

    // Typed JNA proxy (codebase pattern, e.g. MediaKeys' User32LL) — the C
    // functions are looked up once at load time.
    private interface ViviMediaLib : Library {
        fun viviRegisterCallbacks(pp: VoidCb, nx: VoidCb, pv: VoidCb, sk: SeekCb, art: VoidCb)
        fun viviSetAppIdentity(name: String)
        fun viviStartSession()
        fun viviSetNowPlaying(
            title: String?, artist: String?, album: String?,
            durationMs: Double, positionMs: Double, playing: Int, artworkPath: String?,
        )
        fun viviEndSession()
        fun viviRequestNotificationPermission()
        fun viviNotify(title: String?, message: String?)
    }

    private val native: ViviMediaLib? by lazy {
        if (!isMac) {
            null
        } else {
            runCatching {
                // The CI workflow compiles the helper into
                // `desktop/src/main/resources/native/macos-<arch>/` (folder name
                // is the matrix arch value: `arm64` on Apple Silicon, `x64` on
                // Intel) before packaging; extract it and load from disk
                // (System.load cannot map a bare classpath entry).
                val arch = System.getProperty("os.arch", "").lowercase()
                val folders = if (arch.contains("aarch64") || arch.contains("arm")) {
                    listOf("macos-arm64", "macos-aarch64")
                } else {
                    listOf("macos-x64", "macos-x86_64")
                }
                val (stream, folder) = folders
                    .firstNotNullOfOrNull { folder ->
                        val path = "/native/$folder/libvivi_media.dylib"
                        ViviMediaLib::class.java.getResourceAsStream(path)?.let { it to folder }
                    }
                    ?: error("native helper resource missing (tried /native/{${folders.joinToString("|")}}/libvivi_media.dylib)")
                val cacheDir = File(System.getProperty("user.home"), ".vivimusic")
                cacheDir.mkdirs()
                val dylib = File(cacheDir, "libvivi_media-$folder.dylib")
                stream.use { input ->
                    FileOutputStream(dylib).use { output -> input.copyTo(output) }
                }
                Native.load(dylib.absolutePath, ViviMediaLib::class.java)
            }.onFailure { t ->
                println("[mac-media] native helper load failed: $t")
            }.getOrNull()
        }
    }

    private val nativeApi: ViviMediaLib?
        get() = native

    // Callbacks the native side fires on the main dispatch queue. They must be
    // strongly referenced for as long as the native code can invoke them.
    @Volatile
    private var onPlayPause: (() -> Unit)? = null
    @Volatile
    private var onNext: (() -> Unit)? = null
    @Volatile
    private var onPrevious: (() -> Unit)? = null
    @Volatile
    private var onSeek: ((Long) -> Unit)? = null

    private class NativeCallbacks(
        val playPause: VoidCb,
        val next: VoidCb,
        val previous: VoidCb,
        val seek: SeekCb,
    )

    @Volatile
    private var callbacks: NativeCallbacks? = null

    /** Latest playback state, re-applied on [start] and on artwork arrival. */
    private class Metadata(
        val title: String = "",
        val artist: String = "",
        val album: String = "",
        val durationMs: Long = 0L,
        val positionMs: Long = 0L,
        val playing: Boolean = false,
        val artworkUrl: String? = null,
        val artworkLocalPath: String? = null,
    )

    @Volatile
    private var metadata = Metadata()

    /** Guards against launching a second download for the same track. */
    private val downloadingUrl = AtomicReference<String?>(null)

    /** True when the native helper is loaded and the session is active. */
    val isActive: Boolean
        get() = isMac && started.get() && nativeApi != null

    /**
     * Starts the session and registers the remote commands. The callbacks
     * mirror the [MediaKeys] contract so both share the same player wiring.
     */
    fun start(
        appName: String,
        onPlayPause: () -> Unit,
        onNext: () -> Unit,
        onPrevious: () -> Unit,
        onSeek: (Long) -> Unit,
    ) {
        this.onPlayPause = onPlayPause
        this.onNext = onNext
        this.onPrevious = onPrevious
        this.onSeek = onSeek
        if (!isMac) return
        val api = nativeApi ?: return
        if (started.compareAndSet(false, true)) {
            try {
                val cbs = NativeCallbacks(
                    playPause = VoidCb { this.onPlayPause?.invoke() },
                    next = VoidCb { this.onNext?.invoke() },
                    previous = VoidCb { this.onPrevious?.invoke() },
                    seek = SeekCb { posMs -> this.onSeek?.invoke(posMs.toLong()) },
                )
                callbacks = cbs
                api.viviRegisterCallbacks(cbs.playPause, cbs.next, cbs.previous, cbs.seek, VoidCb {})
                api.viviSetAppIdentity(appName)
            } catch (t: Throwable) {
                println("[mac-media] start failed: $t")
                started.set(false)
                return
            }
        }
        // Re-apply the latest metadata (the tile may have been cleared).
        syncMetadata()
    }

    /** Stops the session (clears the tile) but keeps the handlers registered. */
    fun endSession() {
        if (started.compareAndSet(true, false)) {
            runCatching { nativeApi?.viviEndSession() }
        }
    }

    /** Releases the handlers (app shutdown). */
    fun stop() {
        endSession()
        onPlayPause = null
        onNext = null
        onPrevious = null
        onSeek = null
        callbacks = null
    }

    // ------------------------------------------------------------------
    // Native macOS notifications (UNUserNotificationCenter)
    // ------------------------------------------------------------------

    @Volatile
    private var permissionRequested = AtomicBoolean(false)

    /**
     * Asks macOS for notification permission exactly once per run. Safe to
     * call before posting a notification; the system shows the prompt only
     * the first time.
     */
    fun requestNotificationPermissionOnce() {
        if (!isMac) return
        val api = nativeApi ?: return
        if (permissionRequested.compareAndSet(false, true)) {
            runCatching { api.viviRequestNotificationPermission() }
        }
    }

    /**
     * Posts a native macOS Notification Center banner. Returns true when the
     * helper is loaded and the request was handed to the system (delivery may
     * still be gated by the permission). Returns false on non-macOS or when
     * the helper is unavailable, so callers can fall back (e.g. osascript).
     */
    fun notify(title: String, message: String): Boolean {
        if (!isMac) return false
        val api = nativeApi ?: return false
        return runCatching {
            api.viviNotify(title, message)
            true
        }.getOrDefault(false)
    }

    /**
     * Pushes the current playback state to the system tile. Safe to call from
     * any thread and at any frequency (position ticks); only the artwork
     * download runs on a background thread.
     */
    fun setNowPlaying(
        title: String?,
        artist: String?,
        album: String? = null,
        durationMs: Long = 0L,
        positionMs: Long = 0L,
        playing: Boolean = false,
        artworkUrl: String? = null,
        artworkLocalPath: String? = null,
    ) {
        metadata = Metadata(
            title = title ?: "",
            artist = artist ?: "",
            album = album ?: "",
            durationMs = durationMs,
            positionMs = positionMs,
            playing = playing,
            artworkUrl = artworkUrl,
            artworkLocalPath = artworkLocalPath,
        )
        syncMetadata()
    }

    // ------------------------------------------------------------------
    // Metadata push + artwork
    // ------------------------------------------------------------------

    private fun syncMetadata() {
        if (!started.get()) return
        val api = nativeApi ?: return
        val m = metadata
        try {
            api.viviStartSession()
            api.viviSetNowPlaying(
                m.title.ifEmpty { null },
                m.artist.ifEmpty { null },
                m.album.ifEmpty { null },
                m.durationMs.toDouble(),
                m.positionMs.toDouble(),
                if (m.playing) 1 else 0,
                m.artworkLocalPath,
            )
        } catch (t: Throwable) {
            println("[mac-media] metadata push failed: $t")
        }
        ensureArtworkDownloaded(api)
    }

    /**
     * Downloads the artwork for the current track on a background thread and
     * re-pushes the metadata with the local file path once ready. On failure
     * the native side keeps the previous artwork — a bad thumbnail can never
     * break the session.
     */
    private fun ensureArtworkDownloaded(api: ViviMediaLib) {
        val url = metadata.artworkUrl ?: return
        if (metadata.artworkLocalPath != null) return // already resolved
        if (!downloadingUrl.compareAndSet(null, url)) return // already in flight

        val cacheDir = File(System.getProperty("user.home"), ".vivimusic")
        runCatching { cacheDir.mkdirs() }
        Thread {
            try {
                val path = downloadArtwork(url, cacheDir)
                if (path != null && started.get()) {
                    // The track may have changed while downloading: only push if
                    // the artwork belongs to the CURRENT metadata, otherwise the
                    // next syncMetadata (keyed on videoId) will carry the right
                    // artwork. Never override newer state with a stale snapshot.
                    val m = metadata
                    if (m.artworkUrl == url) {
                        api.viviSetNowPlaying(
                            m.title.ifEmpty { null },
                            m.artist.ifEmpty { null },
                            m.album.ifEmpty { null },
                            m.durationMs.toDouble(),
                            m.positionMs.toDouble(),
                            if (m.playing) 1 else 0,
                            path,
                        )
                        // Remember the local path so subsequent pushes (position
                        // ticks) skip re-downloading the same artwork.
                        metadata = Metadata(
                            title = m.title,
                            artist = m.artist,
                            album = m.album,
                            durationMs = m.durationMs,
                            positionMs = m.positionMs,
                            playing = m.playing,
                            artworkUrl = m.artworkUrl,
                            artworkLocalPath = path,
                        )
                    }
                }
            } catch (_: Throwable) {
                // Keep the previous artwork on failure.
            } finally {
                downloadingUrl.set(null)
            }
        }.apply {
            isDaemon = true
            name = "VIVI-macMedia-artwork"
            start()
        }
    }

    /** Downloads the artwork and returns a local path, or null on failure. */
    private fun downloadArtwork(url: String, cacheDir: File): String? {
        return try {
            val connection = java.net.URI(url).toURL().openConnection()
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)")
            connection.connect()
            connection.getInputStream().use { input ->
                val ext = when {
                    url.substringBefore('?').contains(".png", ignoreCase = true) -> "png"
                    url.substringBefore('?').contains(".webp", ignoreCase = true) -> "webp"
                    else -> "jpg"
                }
                val out = File(cacheDir, "macos-artwork.$ext")
                FileOutputStream(out).use { output -> input.copyTo(output) }
                if (out.length() == 0L) {
                    out.delete()
                    null
                } else {
                    out.absolutePath
                }
            }
        } catch (t: Throwable) {
            println("[mac-media] artwork download failed: $t")
            null
        }
    }
}
