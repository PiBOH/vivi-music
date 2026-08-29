package com.music.vivi.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.ui.draw.rotate
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.Subject
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SpeakerGroup
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Translate
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.History
import java.net.URLEncoder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.application
import com.music.lastfm.LastFM
import com.music.innertube.YouTube
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.system.exitProcess
import com.music.innertube.YouTubeExtractor
import com.music.innertube.models.SongItem
import com.music.lrclib.LrcLib
import com.music.vivi.desktop.player.PlayerController
import com.music.vivi.desktop.player.RepeatMode
import com.music.vivi.desktop.player.StreamResolver
import com.music.vivi.sync.LibrarySnapshot
import com.music.vivi.sync.PlaybackSnapshot
import com.music.vivi.sync.SyncServer
import com.music.vivi.sync.SyncedSong
import com.music.vivi.sync.TrackRef
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun main(args: Array<String>) {
    // A toast / command-line launch can request a section (e.g. --open=updates).
    val openSection = AppCommand.parse(args)
    // Single-instance guard: if another instance is already running (or already
    // starting), forward the request to it and exit immediately.
    if (!SingleInstance.acquire()) {
        if (openSection != null) AppCommand.write(openSection)
        return
    }

    // Replace the default AWT "Error" dialog (OK only) with one that also
    // offers "Copy error", so crash details are easy to report.
    installGlobalErrorDialog()

    application {
    // Configure the shared YouTube client exactly like the Android App.onCreate() does,
    // honouring the saved content language/region (or the OS default).
    val initialSettings = DesktopSettings.load()
    YouTube.locale = resolveYouTubeLocale(initialSettings.contentLanguage, initialSettings.contentCountry)
    YouTubeExtractor.cacheDir = File(System.getProperty("user.home"), ".vivimusic/cache").apply { mkdirs() }
    LoginManager.restore()
    DesktopSettings.ensureFirstLaunchDate()
    // Dev tools are non-critical: never let their initialization crash the app
    // at startup (which the jpackage launcher reports as "Failed to launch JVM").
    runCatching { DeveloperOptions.load() }

    var language by remember { mutableStateOf(DesktopSettings.load().language) }
    var themeMode by remember { mutableStateOf(ThemeMode.from(DesktopSettings.load().darkMode)) }
    var accent by remember { mutableStateOf(argbIntToColor(DesktopSettings.load().accentColor)) }
    var accentIntensity by remember { mutableStateOf(DesktopSettings.load().accentIntensity) }
    var pureBlack by remember { mutableStateOf(DesktopSettings.load().pureBlack) }
    var selectedFont by remember { mutableStateOf(AppFont.fromValue(DesktopSettings.load().selectedFont)) }
    var customFontPath by remember { mutableStateOf(DesktopSettings.load().customFontPath) }
    // Make the runtime-imported font resolvable before the first theme pass.
    if (customFontPath.isNotBlank()) AppFonts.customFontPath = customFontPath
    // Spotify-style layout (3 panels) + flat theme, applied together. Default
    // on; when false the app falls back to the Material 3 tonal look.
    var spotifyLayout by remember { mutableStateOf(DesktopSettings.load().spotifyLayout) }

    fun saveTheme() {
        DesktopSettings.update {
            it.copy(
                darkMode = themeMode.key,
                accentColor = colorToArgbInt(accent),
                accentIntensity = accentIntensity,
                pureBlack = pureBlack,
            )
        }
    }

    // Import a user font (.ttf/.otf): native file dialog, copied into the app
    // data dir so the setting survives moving/deleting the original file.
    val importFont: () -> Unit = {
        val dialog = java.awt.FileDialog(null as java.awt.Frame?, "Import font", java.awt.FileDialog.LOAD)
        dialog.setFilenameFilter { _, name ->
            val n = name.lowercase()
            n.endsWith(".ttf") || n.endsWith(".otf")
        }
        dialog.isVisible = true
        val dir = dialog.directory
        val file = dialog.file
        dialog.dispose()
        if (file != null && dir != null) {
            runCatching {
                val src = java.io.File(dir, file)
                val fontsDir = java.io.File(System.getProperty("user.home"), ".vivimusic/fonts").apply { mkdirs() }
                val ext = src.extension.ifBlank { "ttf" }
                val dest = java.io.File(fontsDir, "custom_font.$ext")
                src.copyTo(dest, overwrite = true)
                AppFonts.customFontPath = dest.absolutePath
                customFontPath = dest.absolutePath
                selectedFont = AppFont.CUSTOM
                DesktopSettings.update {
                    it.copy(customFontPath = dest.absolutePath, selectedFont = AppFont.CUSTOM.value)
                }
            }
        }
    }

    // Live window title: shows CPU/RAM when dev options are on and either the
    // "show in title bar" toggle is set or the display mode is "title bar only".
    val devTitleEnabled by DeveloperOptions.enabled.collectAsState()
    val devTitleVisible by DeveloperOptions.showInTitleBar.collectAsState()
    val devMode by DeveloperOptions.mode.collectAsState()
    val titleStats by SystemMonitor.stats.collectAsState()
    val windowTitle = buildString {
        append("VIVI Music — desktop")
        if (devTitleEnabled && (devTitleVisible || devMode == DevToolsMode.TITLE_BAR)) {
            append(titleStats.titleBarText())
        }
    }

    var isFullscreen by remember { mutableStateOf(DesktopSettings.load().isFullscreen) }
    // Native OS title bar vs VIVI's custom one (window chrome is fixed at
    // creation, so this applies on the next launch).
    // LIVE value used by the toggle UI (saved on change). The actual window
    // chrome is decided at creation (see `nativeTitleBarAtStartup` below) and
    // cannot change on a displayed frame — Compose's `SwingWindow` calls
    // `setUndecorated()` on the live frame when the parameter changes, which
    // throws `IllegalComponentStateException: The frame is displayable`.
    var nativeTitleBar by remember { mutableStateOf(DesktopSettings.load().nativeTitleBar) }
    // FROZEN at first composition: the window chrome fixed at creation. It is
    // deliberately never updated after startup, so flipping the toggle can
    // never trigger a runtime `setUndecorated` on the shown frame; the new
    // value is picked up by the restart the toggle asks for.
    val nativeTitleBarAtStartup = remember { DesktopSettings.load().nativeTitleBar }
    // Tracks the OS-maximized state (updated by the AWT listener below) so the
    // custom title-bar buttons reflect the real window placement.
    var windowMaximized by remember { mutableStateOf(DesktopSettings.load().windowMaximized) }
    // Placement before entering fullscreen, so leaving it restores exactly
    // where the window was (floating bounds or maximized).
    var preFullscreenMaximized by remember { mutableStateOf(false) }
    var preFullscreenBounds by remember { mutableStateOf<java.awt.Rectangle?>(null) }
    // Restore bounds captured right before an OS maximize: the persisted
    // geometry must be the floating bounds, so the window never reopens
    // stretched over the taskbar after a maximized session.
    var preMaximizeBounds by remember { mutableStateOf<java.awt.Rectangle?>(null) }
    // Start floating: the saved placement (maximized / bounds) is restored with
    // the OS APIs inside the Window block. Compose's WindowPlacement.Maximized
    // on an undecorated window can produce a window LARGER than the screen when
    // the Windows display scale is not 100%, leaving the title bar off-screen
    // (users then have to kill the app from Task Manager).
    val windowState = rememberWindowState(placement = WindowPlacement.Floating)
    // Captured from the Window content so onCloseRequest can persist geometry.
    val awtWindowRef = arrayOfNulls<java.awt.Frame>(1)

    Window(
        onCloseRequest = {
            runCatching {
                awtWindowRef[0]?.let { w ->
                    val maximized = (w.extendedState and java.awt.Frame.MAXIMIZED_BOTH) != 0
                    // Save the RESTORE bounds, not the maximized ones: while
                    // maximized, `w.bounds` spans the whole screen including
                    // the taskbar area. Persisting that and re-applying it as a
                    // normal placement on the next start makes the window open
                    // sitting over/under the taskbar (with an auto-hide bar the
                    // window even stays above it). When maximized, fall back to
                    // the restore bounds kept by the maximize handler; when
                    // floating, save the actual bounds.
                    val b = (if (maximized) preMaximizeBounds else null) ?: w.bounds
                    DesktopSettings.update {
                        it.copy(
                            windowMaximized = maximized,
                            windowX = b.x,
                            windowY = b.y,
                            windowWidth = b.width,
                            windowHeight = b.height,
                        )
                    }
                }
            }
            exitApplication()
        },
        title = windowTitle,
        state = windowState,
        // Undecorated = VIVI's custom title bar; decorated = the native OS bar.
        // Uses the startup-frozen value: never changes while the window is
        // displayed (setting it at runtime throws on a displayable frame).
        undecorated = !nativeTitleBarAtStartup,
    ) {
        val frameWindow = window
        awtWindowRef[0] = frameWindow

        // Restore the last placement with the OS APIs: OS maximize respects the
        // taskbar and the Windows DPI scaling, unlike Compose's placement which
        // can oversize an undecorated window. Floating bounds are clamped to the
        // usable screen area so a stale/multi-DPI save can never leave the
        // window unreachable.
        LaunchedEffect(Unit) {
            val saved = DesktopSettings.load()
            // A saved fullscreen state is applied by LaunchedEffect(isFullscreen)
            // below (it also fires on first composition), via the OS API so the
            // auto-hide taskbar stays reachable on hover instead of being
            // covered by an oversized Compose placement.
            if (!saved.isFullscreen) {
                runCatching {
                    if (saved.windowMaximized) {
                        frameWindow.extendedState = java.awt.Frame.MAXIMIZED_BOTH
                    } else if (saved.windowWidth > 0 && saved.windowHeight > 0) {
                        // Use the work area of the monitor the window was last
                        // on (not just the primary one), so a window saved on a
                        // secondary screen is restored there and never clamped
                        // onto the primary monitor.
                        val usable = runCatching {
                            val ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                            ge.screenDevices
                                .map { it.defaultConfiguration.bounds }
                                .firstOrNull { r ->
                                    saved.windowX >= r.x && saved.windowX < r.x + r.width &&
                                        saved.windowY >= r.y && saved.windowY < r.y + r.height
                                }
                                ?.let { r -> java.awt.Rectangle(r.x, r.y, r.width, r.height) }
                        }.getOrNull() ?: java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
                        val w = saved.windowWidth.coerceIn(400, usable.width)
                        val h = saved.windowHeight.coerceIn(300, usable.height)
                        val x = saved.windowX.coerceIn(usable.x, usable.x + usable.width - w)
                        val y = saved.windowY.coerceIn(usable.y, usable.y + usable.height - h)
                        frameWindow.setBounds(x, y, w, h)
                    }
                }
            }
        }

        DisposableEffect(frameWindow) {
            val listener = java.awt.event.WindowStateListener { e ->
                val isIconified = (e.newState and java.awt.Frame.ICONIFIED) != 0
                if (!isIconified && windowState.isMinimized) {
                    windowState.isMinimized = false
                }
                // Track the OS maximize state so the title-bar buttons and the
                // persisted placement follow snapping / Win+Up as well.
                windowMaximized = (e.newState and java.awt.Frame.MAXIMIZED_BOTH) != 0
            }
            frameWindow.addWindowStateListener(listener)
            onDispose {
                frameWindow.removeWindowStateListener(listener)
            }
        }

        // Follow the fullscreen toggle with the OS API. Compose's
        // WindowPlacement.Fullscreen can oversize an undecorated window on
        // scaled displays (the same bug class as its Maximized placement),
        // pushing the window past the screen edge and trapping the auto-hide
        // taskbar behind it. OS-maximize never oversizes: on an auto-hide
        // taskbar it fills the whole screen and the taskbar still reveals on
        // hover at the bottom edge; with a visible taskbar it respects the
        // work area.
        LaunchedEffect(isFullscreen) {
            windowState.placement = WindowPlacement.Floating
            runCatching {
                if (isFullscreen) {
                    // Save where we were before the maximize kicks in (on a
                    // fresh start this is the initial floating placement).
                    preFullscreenMaximized =
                        (frameWindow.extendedState and java.awt.Frame.MAXIMIZED_BOTH) != 0
                    preFullscreenBounds = frameWindow.bounds
                    frameWindow.extendedState = java.awt.Frame.MAXIMIZED_BOTH
                } else {
                    if (preFullscreenMaximized) {
                        frameWindow.extendedState = java.awt.Frame.MAXIMIZED_BOTH
                    } else {
                        frameWindow.extendedState = java.awt.Frame.NORMAL
                        preFullscreenBounds?.let { frameWindow.setBounds(it) }
                    }
                }
            }
        }

        AppTheme(
            mode = themeMode,
            accent = accent,
            pureBlack = pureBlack,
            font = selectedFont,
            spotify = spotifyLayout,
            accentIntensity = accentIntensity,
            customFontPath = customFontPath,
        ) {
            // NOTE: do NOT wrap this in a global SelectionContainer. Popup-based
            // components (DropdownMenu, AlertDialog) inherit the selection
            // registrar and crash with "layouts are not part of the same
            // hierarchy" on pointer events (see Compose CMP-2326). Use targeted
            // SelectionContainer wrappers on individual text instead.
            var showIntro by remember { mutableStateOf(DesktopSettings.load().showIntroSplash) }
            Crossfade(targetState = showIntro, animationSpec = tween(400), label = "intro") { intro ->
                when {
                    intro -> IntroSplash(
                        language = language,
                        style = DesktopSettings.load().introStyle,
                        background = DesktopSettings.load().introBackground,
                        onFinished = { showIntro = false },
                    )
                    language.isBlank() -> LanguageSelectionScreen { selected ->
                        language = selected
                        DesktopSettings.update { it.copy(language = selected) }
                    }
                    else -> App(
                        language = language,
                        onLanguageChange = { selected ->
                            language = selected
                            DesktopSettings.update { it.copy(language = selected) }
                        },
                        font = selectedFont,
                        onFontChange = { f ->
                            selectedFont = f
                            DesktopSettings.update { it.copy(selectedFont = f.value) }
                        },
                        themeMode = themeMode,
                        accent = accent,
                        onThemeModeChange = {
                            themeMode = it
                            saveTheme()
                        },
                        onAccentChange = {
                            accent = it
                            saveTheme()
                        },
                        accentIntensity = accentIntensity,
                        onAccentIntensityChange = {
                            accentIntensity = it
                            saveTheme()
                        },
                        customFontPath = customFontPath,
                        onImportFont = importFont,
                        pureBlack = pureBlack,
                        onPureBlackChange = {
                            pureBlack = it
                            saveTheme()
                        },
                        spotifyLayout = spotifyLayout,
                        initialSection = openSection,
                        isFullscreen = isFullscreen,
                        isMaximized = (windowMaximized || isFullscreen),
                        nativeTitleBar = nativeTitleBar,
                        onNativeTitleBarChange = { v ->
                            nativeTitleBar = v
                            DesktopSettings.update { it.copy(nativeTitleBar = v) }
                        },
                        onRestart = ::restartApplication,
                        onToggleFullscreen = {
                            isFullscreen = !isFullscreen
                            DesktopSettings.update { it.copy(isFullscreen = isFullscreen) }
                        },
                        bringToFront = {
                            runCatching {
                                frameWindow.state = java.awt.Frame.NORMAL
                                frameWindow.toFront()
                                frameWindow.requestFocus()
                            }
                        },
                        onMinimize = {
                            windowState.isMinimized = true
                            runCatching { frameWindow.extendedState = frameWindow.extendedState or java.awt.Frame.ICONIFIED }
                        },
                        onMaximize = {
                            if (windowState.isMinimized) {
                                windowState.isMinimized = false
                            }
                            if (isFullscreen) {
                                // The maximize button while fullscreen leaves
                                // fullscreen; LaunchedEffect(isFullscreen)
                                // restores the pre-fullscreen placement.
                                isFullscreen = false
                                DesktopSettings.update { it.copy(isFullscreen = false) }
                            } else if ((frameWindow.extendedState and java.awt.Frame.MAXIMIZED_BOTH) != 0) {
                                // OS restore: respects the work area (taskbar)
                                // and the Windows DPI scaling.
                                runCatching { frameWindow.extendedState = java.awt.Frame.NORMAL }
                            } else {
                                // Capture the floating bounds BEFORE maximizing so
                                // they can be restored (and persisted) later.
                                if (preMaximizeBounds == null) {
                                    preMaximizeBounds = frameWindow.bounds
                                }
                                runCatching { frameWindow.extendedState = java.awt.Frame.MAXIMIZED_BOTH }
                            }
                            runCatching {
                                val maximized = (frameWindow.extendedState and java.awt.Frame.MAXIMIZED_BOTH) != 0
                                // Persist the restore bounds (the floating ones
                                // captured before the maximize), never the
                                // maximized full-screen bounds that include the
                                // taskbar area.
                                val b = if (maximized) preMaximizeBounds ?: frameWindow.bounds else frameWindow.bounds
                                DesktopSettings.update {
                                    it.copy(
                                        windowMaximized = maximized,
                                        windowX = b.x,
                                        windowY = b.y,
                                        windowWidth = b.width,
                                        windowHeight = b.height,
                                    )
                                }
                            }
                        },
                        onClose = ::exitApplication,
                    )
                }
            }
        }
    }
    }
}

/** Maps a toast/command-line section id to the screen it should open. */
private fun screenForSection(section: String?): Screen? = when (section) {
    "updates" -> Screen.SettingsUpdates
    "developer" -> Screen.SettingsDeveloper
    "devices" -> Screen.SettingsDevices
    else -> null
}

@Composable
fun WindowScope.App(
    language: String,
    onLanguageChange: (String) -> Unit,
    font: AppFont,
    onFontChange: (AppFont) -> Unit,
    customFontPath: String = "",
    onImportFont: () -> Unit = {},
    themeMode: ThemeMode,
    accent: Color,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAccentChange: (Color) -> Unit,
    accentIntensity: Float = 1f,
    onAccentIntensityChange: (Float) -> Unit = {},
    pureBlack: Boolean,
    onPureBlackChange: (Boolean) -> Unit,
    spotifyLayout: Boolean = false,
    initialSection: String? = null,
    isFullscreen: Boolean = false,
    isMaximized: Boolean = false,
    nativeTitleBar: Boolean = false,
    onNativeTitleBarChange: (Boolean) -> Unit = {},
    onRestart: () -> Unit = {},
    onToggleFullscreen: () -> Unit = {},
    bringToFront: () -> Unit = {},
    onMinimize: () -> Unit = {},
    onMaximize: () -> Unit = {},
    onClose: () -> Unit = {},
) {
    var backStack by remember { mutableStateOf(listOf<Screen>(Screen.Home)) }
    val player = remember { PlayerController() }
    val playerState by player.state.collectAsState()
    val nowPlaying = playerState.current
    val isPlaying = playerState.isPlaying
    val audioLevel by player.audioLevel.collectAsState()

    // Cider-style desktop features state (floating widget, media keys, tray menu).
    var showWidget by remember { mutableStateOf(DesktopSettings.load().showNowPlayingWidget) }
    var mediaKeysEnabled by remember { mutableStateOf(DesktopSettings.load().mediaKeysEnabled) }
    var trayMenuEnabled by remember { mutableStateOf(DesktopSettings.load().trayMenuEnabled) }

    // Cider-style desktop integrations: global media keys (Windows hook),
    // tray right-click menu and tray tooltip with the current track.
    val isWindows = System.getProperty("os.name", "").lowercase().contains("win")
    LaunchedEffect(mediaKeysEnabled) {
        if (mediaKeysEnabled && isWindows) {
            MediaKeys.start(
                onPlayPause = { player.toggle() },
                onNext = { player.next() },
                onPrevious = { player.previous() },
            )
        }
    }
    LaunchedEffect(language, trayMenuEnabled) {
        if (trayMenuEnabled) {
            NativeNotifier.configureTray(
                NativeNotifier.TrayActions(
                    onPlayPause = { player.toggle() },
                    onNext = { player.next() },
                    onPrevious = { player.previous() },
                    onOpen = { bringToFront() },
                    onQuit = onClose,
                    labelPlayPause = Localization.get(language, if (isPlaying) "pause" else "play"),
                    labelNext = Localization.get(language, "next"),
                    labelPrevious = Localization.get(language, "previous"),
                    labelOpen = Localization.get(language, "open_vivi"),
                    labelQuit = Localization.get(language, "quit"),
                )
            )
        } else {
            NativeNotifier.clearTrayMenu()
        }
    }
    LaunchedEffect(nowPlaying?.videoId) {
        NativeNotifier.setTrayTooltip(
            if (nowPlaying != null) "VIVI Music — ${nowPlaying.title} — ${nowPlaying.artist}" else "VIVI Music"
        )
    }

    // Cider-style floating "Now Playing" widget (always-on-top, draggable).
    if (showWidget) {
        NowPlayingWidgetWindow(
            player = player,
            language = language,
            themeMode = themeMode,
            accent = accent,
            pureBlack = pureBlack,
            font = font,
            onClose = {
                showWidget = false
                DesktopSettings.update { it.copy(showNowPlayingWidget = false) }
            },
        )
    }

    var autoPlayNext by remember { mutableStateOf(DesktopSettings.load().autoPlayNext) }
    player.autoPlayNext = autoPlayNext

    var densityScale by remember { mutableStateOf(DesktopSettings.load().densityScale) }
    var gridItemSize by remember { mutableStateOf(DesktopSettings.load().gridItemSize) }
    var screenTransition by remember { mutableStateOf(DesktopSettings.load().screenTransition) }
    var sliderStyle by remember { mutableStateOf(DesktopSettings.load().sliderStyle) }
    var playerDesign by remember { mutableStateOf(PlayerDesign.from(DesktopSettings.load().playerDesign)) }
    var playerBackground by remember { mutableStateOf(PlayerBackgroundStyle.from(DesktopSettings.load().playerBackground)) }
    var rotatingThumbnail by remember { mutableStateOf(DesktopSettings.load().rotatingThumbnail) }
    var miniPlayerStyle by remember { mutableStateOf(DesktopSettings.load().miniPlayerStyle) }
    var miniPlayerDesign by remember { mutableStateOf(MiniPlayerDesign.from(DesktopSettings.load().miniPlayerDesign)) }
    var miniPlayerBackgroundStyle by remember { mutableStateOf(MiniPlayerBackgroundStyle.from(DesktopSettings.load().miniPlayerBackgroundStyle)) }
    var pureBlackMiniPlayer by remember { mutableStateOf(DesktopSettings.load().pureBlackMiniPlayer) }
    var showRightSidebar by remember { mutableStateOf(DesktopSettings.load().showRightSidebar) }
    var homeUseLastListen by remember { mutableStateOf(DesktopSettings.load().homeUseLastListen) }
    var randomizeHomeOrder by remember { mutableStateOf(DesktopSettings.load().randomizeHomeOrder) }
    var showWrappedOnHome by remember { mutableStateOf(DesktopSettings.load().showWrappedOnHome) }
    var showIntroSplash by remember { mutableStateOf(DesktopSettings.load().showIntroSplash) }
    var introStyle by remember { mutableStateOf(DesktopSettings.load().introStyle) }
    var introBackground by remember { mutableStateOf(DesktopSettings.load().introBackground) }
    var pauseSearchHistory by remember { mutableStateOf(DesktopSettings.load().pauseSearchHistory) }
    var pauseListenHistory by remember { mutableStateOf(DesktopSettings.load().pauseListenHistory) }
    var searchHistory by remember { mutableStateOf(DesktopSettings.load().searchHistory) }
    val recordSearch: (String) -> Unit = { term ->
        if (!pauseSearchHistory && term.isNotBlank()) {
            val updated = (listOf(term.trim()) + searchHistory.filter { !it.equals(term.trim(), ignoreCase = true) }).take(12)
            searchHistory = updated
            DesktopSettings.update { it.copy(searchHistory = updated) }
        }
    }

    // Session listening stats for the Home "VIVI Wrapped" card (session-only).
    var sessionTrackStarts by remember { mutableStateOf(0) }
    var sessionPlayedMs by remember { mutableStateOf(0L) }
    var sessionTopSong by remember { mutableStateOf<Pair<String, String>?>(null) } // videoId to title
    var sessionTopCount by remember { mutableStateOf(0) }
    var lastSessionSongId by remember { mutableStateOf<String?>(null) }
    var lastSessionPosition by remember { mutableStateOf(0L) }
    LaunchedEffect(nowPlaying?.videoId, isPlaying, playerState.positionMs) {
        val id = nowPlaying?.videoId
        val pos = playerState.positionMs
        if (id != null && id != lastSessionSongId) {
            // New track started in this session: count it and track the top one.
            lastSessionSongId = id
            if (sessionTopSong?.first == id) {
                sessionTopCount++
            } else {
                sessionTopSong = id to (nowPlaying?.title ?: "")
                sessionTopCount = 1
            }
            sessionTrackStarts++
            lastSessionPosition = pos
        } else if (isPlaying) {
            val delta = pos - lastSessionPosition
            if (delta in 1..10_000) sessionPlayedMs += delta
            lastSessionPosition = pos
        }
    }
    var canvasEnabled by remember { mutableStateOf(DesktopSettings.load().canvasEnabled) }
    var canvasSource by remember { mutableStateOf(CanvasSource.from(DesktopSettings.load().canvasSource)) }

    // Guest sessions need a visitorData (like the Android app) or YouTube flags
    // the requests as bots and 403s audio playback.
    LaunchedEffect(Unit) { GuestSession.ensure() }

    // A muted Windows master volume ignores every volume write, so a paired
    // mobile device could never control it; unmute it (and drop it to 0%) once
    // at startup. Non-Windows and non-muted states are left untouched.
    LaunchedEffect(Unit) { SystemVolume.unmuteIfMuted() }

    // Scheduled automatic backups (weekly): check on startup, then hourly.
    LaunchedEffect(Unit) {
        while (true) {
            runCatching { BackupManager.maybeRunScheduled() }
            delay(3600_000L)
        }
    }

    var isLoggedIn by remember { mutableStateOf(LoginManager.isLoggedIn()) }
    var accountName by remember { mutableStateOf(DesktopSettings.load().accountName) }
    var accountChannelHandle by remember { mutableStateOf(DesktopSettings.load().accountChannelHandle) }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            YouTube.accountInfo().onSuccess { account ->
                accountName = account.name
                accountChannelHandle = account.channelHandle.orEmpty()
                DesktopSettings.update {
                    it.copy(
                        accountName = account.name,
                        accountEmail = account.email.orEmpty(),
                        accountChannelHandle = account.channelHandle.orEmpty(),
                    )
                }
            }
        }
    }

    val displayUserName = if (isLoggedIn && accountName.isNotBlank()) accountName else "Guest"
    val displayUserHandle = if (isLoggedIn && accountName.isNotBlank()) {
        accountChannelHandle.ifBlank { "@${accountName.lowercase().replace(" ", "")}" }
    } else {
        "Not signed in"
    }

    var sidebarCollapsed by remember { mutableStateOf(DesktopSettings.load().sidebarCollapsed) }
    var contentLanguage by remember { mutableStateOf(DesktopSettings.load().contentLanguage) }
    var contentCountry by remember { mutableStateOf(DesktopSettings.load().contentCountry) }
    var syncedLyrics by remember { mutableStateOf(DesktopSettings.load().syncedLyrics) }
    var audioQuality by remember { mutableStateOf(DesktopSettings.load().audioQuality) }
    var rememberShuffleRepeat by remember { mutableStateOf(DesktopSettings.load().rememberShuffleRepeat) }
    var persistentQueue by remember { mutableStateOf(DesktopSettings.load().persistentQueue) }
    var syncViviVolume by remember { mutableStateOf(DesktopSettings.load().syncViviVolume) }
    var lyricsTextSize by remember { mutableStateOf(DesktopSettings.load().lyricsTextSize) }
    var lyricsLineSpacing by remember { mutableStateOf(DesktopSettings.load().lyricsLineSpacing) }
    var streamCacheMinutes by remember { mutableStateOf(DesktopSettings.load().streamCacheMinutes) }
    var discordRpcEnabled by remember { mutableStateOf(DesktopSettings.load().discordRpcEnabled) }
    var discordClientId by remember { mutableStateOf(DesktopSettings.load().discordClientId) }
    var lastfmEnabled by remember { mutableStateOf(DesktopSettings.load().lastfmEnabled) }
    var lastfmSession by remember { mutableStateOf(DesktopSettings.load().lastfmSession) }
    var lastfmNowPlaying by remember { mutableStateOf(DesktopSettings.load().lastfmNowPlaying) }

    // Integrations: initialize Last.fm with env-provided credentials (like the
    // mobile BuildConfig) and mirror the Discord toggles to the RPC client.
    LaunchedEffect(Unit) {
        val key = System.getenv("LASTFM_API_KEY").orEmpty()
        val secret = System.getenv("LASTFM_SECRET").orEmpty()
        if (key.isNotEmpty() && secret.isNotEmpty()) LastFM.initialize(key, secret)
        DiscordRPC.clientId = DiscordRPC.clientId.ifBlank { discordClientId }
        DiscordRPC.enabled = discordRpcEnabled
    }
    LaunchedEffect(discordRpcEnabled, discordClientId) {
        DiscordRPC.clientId = discordClientId
        DiscordRPC.enabled = discordRpcEnabled
    }

    // Discord presence + Last.fm now-playing / scrobble feed.
    var lastfmReported by remember { mutableStateOf<String?>(null) }
    var lastfmScrobbled by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(nowPlaying?.videoId, isPlaying, playerState.positionMs, discordRpcEnabled, lastfmEnabled, lastfmSession) {
        val np = nowPlaying
        if (np == null) {
            DiscordRPC.updateActivity(null, null, null, null)
            return@LaunchedEffect
        }
        if (discordRpcEnabled && discordClientId.isNotBlank() && isPlaying) {
            DiscordRPC.updateActivity(details = np.title, state = np.artist, largeImage = "vivimusic")
        }
        if (lastfmEnabled && lastfmSession.isNotBlank() && LastFM.isInitialized()) {
            LastFM.sessionKey = lastfmSession
            if (isPlaying && lastfmReported != np.videoId) {
                lastfmReported = np.videoId
                if (lastfmNowPlaying) {
                    runCatching { LastFM.updateNowPlaying(artist = np.artist, track = np.title, duration = (np.durationMs / 1000L).toInt().coerceAtLeast(0)) }
                }
            }
            // Scrobble near the end of the track (>=5s before it finishes).
            val dur = np.durationMs
            val pos = playerState.positionMs
            if (dur > 30_000 && pos > 0 && pos >= dur - 15_000 && lastfmScrobbled != np.videoId) {
                lastfmScrobbled = np.videoId
                runCatching { LastFM.scrobble(artist = np.artist, track = np.title, timestamp = System.currentTimeMillis() / 1000, duration = (dur / 1000L).toInt()) }
            }
        }
    }

    // Persistent queue: restore the saved queue on startup (paused, not auto-played).
    LaunchedEffect(Unit) {
        val s = DesktopSettings.load()
        if (s.persistentQueue && s.queueJson.isNotBlank()) {
            runCatching { queueJson.decodeFromString<List<NowPlaying>>(s.queueJson) }
                .getOrNull()
                ?.let { player.restoreQueue(it, s.queueIndex) }
        }
    }

    // Persistent queue: save the queue whenever it changes.
    LaunchedEffect(playerState.queue, playerState.index, persistentQueue) {
        if (persistentQueue && playerState.queue.isNotEmpty()) {
            DesktopSettings.update {
                it.copy(queueJson = queueJson.encodeToString(playerState.queue), queueIndex = playerState.index)
            }
        }
    }

    val current = backStack.last()

    // Undo/redo stacks for keyboard navigation history (Ctrl+Z / Ctrl+Y).
    var undoStack by remember { mutableStateOf(listOf<Screen>()) }
    var redoStack by remember { mutableStateOf(listOf<Screen>()) }
    val navigate: (Screen) -> Unit = navigate@{ target ->
        // Never push a duplicate of the screen already on top (double-clicks
        // or a stale click handler must not create [X, X] entries).
        if (backStack.last() == target) return@navigate
        redoStack = emptyList()
        undoStack = undoStack + backStack.last()
        backStack = backStack + target
    }
    val openRoot: (Screen) -> Unit = { screen ->
        // Keep Home at the base so "back" from a root (e.g. Settings) returns
        // to Home instead of getting stuck with nothing to pop.
        backStack = if (screen == Screen.Home) listOf(Screen.Home) else listOf(Screen.Home, screen)
    }
    val goBack: () -> Unit = {
        if (backStack.size > 1) {
            undoStack = undoStack + backStack.last()
            backStack = backStack.dropLast(1)
        }
    }
    val undo: () -> Unit = {
        if (undoStack.isNotEmpty()) {
            redoStack = redoStack + backStack.last()
            backStack = backStack + undoStack.last()
            undoStack = undoStack.dropLast(1)
        }
    }
    val redo: () -> Unit = {
        if (redoStack.isNotEmpty()) {
            undoStack = undoStack + backStack.last()
            backStack = backStack + redoStack.last()
            redoStack = redoStack.dropLast(1)
        }
    }

    // Global keyboard navigation: Backspace/Alt+Left goes back, Ctrl+Z/Ctrl+Y
    // undo/redo the navigation history.
    val onGlobalKey: (KeyEvent) -> Boolean = { event ->
        when {
            event.type != KeyEventType.KeyDown -> false
            event.key == Key.F11 -> {
                onToggleFullscreen()
                true
            }
            event.key == Key.Backspace || event.key == Key.Escape || (event.isAltPressed && event.key == Key.DirectionLeft) -> {
                goBack(); true
            }
            event.isCtrlPressed && event.key == Key.Z -> { undo(); true }
            event.isCtrlPressed && event.key == Key.Y -> { redo(); true }
            else -> false
        }
    }

    // Toast / command-line "open section" requests: navigate and bring the
    // window to the foreground (handled once at startup, then polled for the
    // lifetime of the app so a toast click can reach a running instance).
    LaunchedEffect(Unit) {
        var pending = initialSection
        while (true) {
            AppCommand.poll()?.let { pending = it }
            if (pending != null) {
                screenForSection(pending)?.let { openRoot(it) }
                bringToFront()
                pending = null
            }
            delay(500)
        }
    }
    fun songToNowPlaying(song: SongItem): NowPlaying = NowPlaying(
        videoId = song.id,
        title = song.title,
        artist = song.artists.joinToString(", ") { it.name },
        thumbnail = song.thumbnail,
        durationMs = (song.duration ?: 0) * 1000L,
    )

    val playSong: (SongItem) -> Unit = { song -> player.play(songToNowPlaying(song)) }
    val addToQueue: (SongItem) -> Unit = { song -> player.addToQueue(songToNowPlaying(song)) }
    var recognitionHistory by remember { mutableStateOf(DesktopSettings.load().recognitionHistory) }
    var addToPlaylistSong by remember { mutableStateOf<SyncedSong?>(null) }
    val addToPlaylist: (SongItem) -> Unit = { song -> addToPlaylistSong = song.toSyncedSong() }
    // Same, but for the Player / Queue (which carry NowPlaying, not SongItem).
    val addNowPlayingToPlaylist: (NowPlaying) -> Unit = { np ->
        addToPlaylistSong = SyncedSong(id = np.videoId, title = np.title, artist = np.artist, thumbnail = np.thumbnail)
    }
    val playAll: (List<SongItem>) -> Unit = { songs -> player.playAll(songs.map(::songToNowPlaying)) }
    val shuffleAll: (List<SongItem>) -> Unit = { songs ->
        if (!playerState.isShuffle) player.toggleShuffle()
        player.playAll(songs.shuffled().map(::songToNowPlaying))
    }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        DesktopSnackbar.events.collectLatest { msg ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(msg)
        }
    }
    // Generic in-app notification banner (title + message) for notifications
    // dispatched in "main window" mode.
    var appNotification by remember { mutableStateOf<DesktopNotifier.Notice?>(null) }
    LaunchedEffect(Unit) {
        DesktopNotifier.events.collect { appNotification = it }
    }
    // Auto-dismiss the generic in-app notification after the configured time.
    LaunchedEffect(appNotification) {
        val notice = appNotification ?: return@LaunchedEffect
        val seconds = DesktopSettings.load().inAppNotificationDurationSeconds
        if (seconds > 0) {
            delay(seconds * 1000L)
            appNotification = null
        }
    }
    var headerSearchQuery by remember { mutableStateOf("") }
    var headerSearchFilter by remember { mutableStateOf<YouTube.SearchFilter?>(null) }
    var includePreReleases by remember {
        mutableStateOf(
            DesktopSettings.load().includePreReleases || AppInfo.CHANNEL.lowercase() != "stable"
        )
    }
    var updateIntervalHours by remember { mutableStateOf(DesktopSettings.load().updateCheckIntervalHours) }
    var updateSource by remember { mutableStateOf(DesktopSettings.load().updateSource) }
    var notificationMode by remember { mutableStateOf(DesktopSettings.load().notificationMode) }
    var notificationDurationSeconds by remember { mutableStateOf(DesktopSettings.load().inAppNotificationDurationSeconds) }
    var saveNotificationHistory by remember { mutableStateOf(DesktopSettings.load().saveNotificationHistory) }
    var updateStatus by remember { mutableStateOf<UpdateStatus>(UpdateStatus.Idle) }
    // Keep the shared download state (used by both the notification and the
    // Updates screen) in sync with the latest update status.
    LaunchedEffect(updateStatus) { UpdateState.syncWithStatus(updateStatus) }
    val devEnabled by DeveloperOptions.enabled.collectAsState()
    val devMode by DeveloperOptions.mode.collectAsState()
    val overlayMovable by DeveloperOptions.overlayMovable.collectAsState()

    // One-off hint when the developer options get unlocked. Respects the
    // notification mode (in-app banner vs native system notification).
    var showDevNotification by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        DeveloperOptions.unlocked.collect {
            val title = Localization.get(language, "dev_unlocked_title")
            val desc = Localization.get(language, "dev_unlocked_desc")
            if (DesktopSettings.load().notificationMode == "native") {
                DesktopNotifier.notify(title, desc, "developer")
            } else {
                NotificationHistory.record(title, desc, "in_app")
                showDevNotification = true
            }
        }
    }
    // Auto-dismiss the dev-unlocked hint after the configured time.
    LaunchedEffect(showDevNotification) {
        if (showDevNotification) {
            val seconds = DesktopSettings.load().inAppNotificationDurationSeconds
            if (seconds > 0) {
                delay(seconds * 1000L)
                showDevNotification = false
            }
        }
    }

    fun runUpdateCheck() {
        updateStatus = UpdateStatus.Checking
        scope.launch {
            val result = withContext(Dispatchers.IO) { UpdateChecker.check(includePreReleases) }
            updateStatus = result
        }
    }

    // Automatic update check on startup.
    LaunchedEffect(Unit) { runUpdateCheck() }

    // Periodic update check, at the user-selected interval (0 = manual only).
    LaunchedEffect(updateIntervalHours) {
        if (updateIntervalHours <= 0) return@LaunchedEffect
        while (true) {
            delay(updateIntervalHours * 3_600_000L)
            runUpdateCheck()
        }
    }

    // Update notification, shown once per new version. Where it appears
    // depends on the user's notification mode (in-app vs native system).
    var showUpdateNotification by remember { mutableStateOf(false) }
    var updateNotifiedVersion by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(updateStatus) {
        val available = updateStatus as? UpdateStatus.Available
        if (available != null && available.version != updateNotifiedVersion) {
            updateNotifiedVersion = available.version
            val mode = if (DesktopSettings.load().notificationMode == "native") "native" else "in_app"
            val title = Localization.get(language, "update_available")
            val message = "${Localization.get(language, "current_version")}: ${AppInfo.FULL_VERSION}\n${available.version}"
            NotificationHistory.record(title, message, mode)
            if (mode == "native") {
                NativeNotifier.notify(title, message, "updates")
            } else {
                showUpdateNotification = true
            }
        }
    }
    // Auto-dismiss the update banner after the configured time, but never while
    // it is showing download progress. The download itself keeps running in the
    // shared UpdateState (also visible in Settings → Updates).
    val updateDownloading by UpdateState.progress.collectAsState()
    val updateBusy = updateDownloading != null
    LaunchedEffect(showUpdateNotification, updateBusy) {
        if (showUpdateNotification && !updateBusy) {
            val seconds = DesktopSettings.load().inAppNotificationDurationSeconds
            if (seconds > 0) {
                delay(seconds * 1000L)
                // A download may have started while we waited; don't dismiss then.
                if (UpdateState.progress.value == null) {
                    showUpdateNotification = false
                }
            }
        }
    }

    // ---- Device sync (Android <-> desktop) ----
    val syncManager = remember { DesktopSyncManager() }

    // Echo guards: when we apply a remote volume, we must not push the
    // resulting local change straight back to the peer.
    val systemVolumeGuard = remember { VolumeGuard() }
    val volumeGuard = remember { VolumeGuard() }

    // Local change wins over the peer: a drag on the VIVI volume slider
    // stamps this timestamp, and incoming remote volumes are ignored for a
    // short window so a stale/echoed value can't snap the slider back.
    var localVolumeChangedAt by remember { mutableStateOf(0L) }
    val setLocalVolume: (Float) -> Unit = { v ->
        localVolumeChangedAt = System.currentTimeMillis()
        player.setVolume(v)
    }

    // Notify when a phone pairs or un-pairs (respects the notification mode).
    var wasPaired by remember { mutableStateOf(false) }
    LaunchedEffect(syncManager) {
        syncManager.paired.collect { paired ->
            // Keep the display/system awake while paired so the OS sleeping
            // the screen can't tear down the sync socket and unpair the two
            // devices.
            KeepAwake.setEnabled(paired)
            if (paired != wasPaired) {
                wasPaired = paired
                if (paired) {
                    val name = syncManager.peerDeviceName.value.ifBlank { null }
                    DesktopNotifier.notify(
                        Localization.get(language, "device_paired_title"),
                        name ?: Localization.get(language, "device_paired_desc"),
                        "devices",
                    )
                } else {
                    DesktopNotifier.notify(
                        Localization.get(language, "device_unpaired_title"),
                        Localization.get(language, "device_unpaired_desc"),
                        "devices",
                    )
                }
            }
        }
    }

    // Look-ahead prefetch: cache the next few tracks' audio and lyrics in the
    // background so they start instantly when skipped to. Runs regardless of
    // play/pause, so pausing still fills the cache for the upcoming tracks.
    LaunchedEffect(player) {
        player.state
            .map { it.queue to it.index }
            .distinctUntilChanged()
            .collect { (queue, index) ->
                for (track in queue.drop(index + 1).take(3)) {
                    // Audio: resolve + download to the on-disk cache (no play).
                    if (!player.isCached(track.videoId)) {
                        launch(Dispatchers.IO) {
                            val streams = StreamResolver.resolveAacStream(
                                track.videoId,
                                StreamResolver.AudioQuality.from(DesktopSettings.load().audioQuality),
                            )
                            if (streams.isNotEmpty()) player.prefetch(streams, track.videoId)
                        }
                    }
                    // Lyrics: fetch + keep in the persistent cache.
                    if (LyricsCache.get(track.videoId) == null) {
                        launch(Dispatchers.IO) {
                            LrcLib.getLyrics(title = track.title, artist = track.artist, duration = -1)
                                .onSuccess { LyricsCache.put(track.videoId, it) }
                        }
                    }
                }
            }
    }

    // Push the local playback state to the peer when the track, play/pause
    // state, or queue changes (position is sent as a best-effort snapshot).
    LaunchedEffect(syncManager) {
        player.state
            .map { s ->
                PlaybackSyncKey(
                    trackId = s.current?.videoId,
                    isPlaying = s.isPlaying,
                    isResolving = s.isResolving,
                    index = s.index,
                    queue = s.queue.map { it.videoId },
                    repeatMode = s.repeatMode.name,
                    isShuffle = s.isShuffle,
                )
            }
            .distinctUntilChanged()
            .collect { player.toPlaybackSnapshot()?.let { syncManager.updatePlayback(it) } }
    }

    // Push the in-app (VIVI) player volume to the peer. Polled (not event-
    // driven) so it also syncs when nothing is playing, and echo-guarded so a
    // locally-applied remote value isn't bounced straight back.
    LaunchedEffect(syncManager) {
        while (true) {
            if (DesktopSettings.load().syncViviVolume) {
                val v = player.state.value.volume
                val isEcho = System.currentTimeMillis() < volumeGuard.echoUntil &&
                    abs(v - volumeGuard.echoValue) < 0.01f
                val changed = volumeGuard.lastPushed == null ||
                    abs(v - volumeGuard.lastPushed!!) > 0.001f
                if (!isEcho && changed) {
                    val s = player.state.value
                    val snapshot = player.toPlaybackSnapshot() ?: PlaybackSnapshot(volume = s.volume)
                    // Only mark as pushed when it was actually sent, so a dropped
                    // push (echo-suppression window) is retried on the next tick.
                    if (syncManager.updatePlayback(snapshot)) {
                        volumeGuard.lastPushed = v
                    }
                }
            }
            delay(500L)
        }
    }

    // Push immediately on user seeks so the peer follows to the same position.
    // Marked as a user seek so the receiver applies it exactly (both directions).
    LaunchedEffect(syncManager) {
        player.seekEvents.collect {
            player.toPlaybackSnapshot()?.copy(userSeek = true)?.let { syncManager.updatePlayback(it) }
        }
    }

    // Periodic re-sync: while playing, re-push the position every few seconds so
    // the peer auto-corrects drift (buffering / clock skew) instead of waiting
    // for the next discrete seek/play/track event.
    LaunchedEffect(syncManager) {
        var lastPushedPositionMs = -1L
        while (true) {
            delay(SyncServer.RESYNC_TICK_MS)
            val s = player.state.value
            // Only push when the position actually advanced: a stalled/frozen
            // player must not repeatedly drag the peer back to the same point.
            if (s.isPlaying && s.positionMs != lastPushedPositionMs) {
                lastPushedPositionMs = s.positionMs
                player.toPlaybackSnapshot()?.let { syncManager.updatePlayback(it) }
            }
        }
    }

    // Poll the OS system volume and push changes to the peer (so changing the
    // Windows/Linux/mac volume controls the phone's system volume, and vice
    // versa). Echo-suppressed so a locally-applied remote value isn't bounced.
    LaunchedEffect(syncManager) {
        while (true) {
            val sv = SystemVolume.get()
            if (sv != null) {
                val isEcho = System.currentTimeMillis() < systemVolumeGuard.echoUntil &&
                    abs(sv - systemVolumeGuard.echoValue) < 0.02f
                val changed = systemVolumeGuard.lastPushed == null ||
                    abs(sv - systemVolumeGuard.lastPushed!!) > 0.01f
                if (!isEcho && changed) {
                    val s = player.state.value
                    val snapshot = player.toPlaybackSnapshot() ?: PlaybackSnapshot(volume = s.volume)
                    if (syncManager.updatePlayback(snapshot.copy(systemVolume = sv))) {
                        systemVolumeGuard.lastPushed = sv
                    }
                }
            }
            delay(800L)
        }
    }

    // Latest peer playback snapshot. While WE are still resolving our stream,
    // incoming seek/play-pause is held off (our own resolution decides when
    // audio starts), so a command sent during that window is re-applied as soon
    // as our audio actually starts flowing instead of being dropped until the
    // next 5s periodic re-sync (which was especially noticeable on a slow
    // phone-hotspot connection where resolution takes a while).
    // Latest peer playback snapshot, stamped with the local receipt time so
    // the re-apply-after-resolving effect can tell whether it arrived while
    // we were buffering (and must be honored) or before we started (and is
    // stale — a pre-play snapshot must not pause a track the user just
    // started).
    val latestRemotePlayback =
        remember { java.util.concurrent.atomic.AtomicReference<Pair<Long, PlaybackSnapshot>?>(null) }

    // Apply incoming playback snapshots from the peer.
    LaunchedEffect(syncManager) {
        syncManager.incomingPlayback.collect { pb ->
            latestRemotePlayback.set(System.currentTimeMillis() to pb)
            // App (player) volume sync: mirror the peer's in-app volume slider.
            // A very recent local drag wins: the peer's value may be an echo of
            // our own push or a stale pre-drag snapshot, and re-applying it
            // snaps the slider back the moment the user lets go.
            if (DesktopSettings.load().syncViviVolume) {
                pb.volume?.let { v ->
                    if (System.currentTimeMillis() - localVolumeChangedAt > 2_000L) {
                        volumeGuard.echoUntil = System.currentTimeMillis() + 1500L
                        volumeGuard.echoValue = v
                        volumeGuard.lastPushed = v
                        if (abs(v - player.state.value.volume) > 0.001f) player.setVolume(v)
                    }
                }
            }
            // Native OS system volume sync: mirror the peer's system volume.
            pb.systemVolume?.let { v ->
                systemVolumeGuard.echoUntil = System.currentTimeMillis() + 1500L
                systemVolumeGuard.echoValue = v
                systemVolumeGuard.lastPushed = v
                SystemVolume.set(v)
            }
            // Repeat mode + shuffle sync (independent of the queue/position).
            pb.repeatMode?.let { mode ->
                runCatching { RepeatMode.valueOf(mode) }.getOrNull()?.let { player.setRepeatMode(it) }
            }
            pb.isShuffle?.let { player.setShuffle(it) }
            val currentId = player.state.value.current?.videoId
            if (currentId != null && pb.trackId != null && pb.trackId == currentId) {
                // Same track: lightweight seek (instant + precise), no restart.
                // While WE are still resolving this track our own resolution
                // decides when playback starts: ignore the peer's play/pause
                // echoes (the peer pauses only because we told it to hold) so we
                // don't pause ourselves out of the startup. Drift is corrected
                // by the re-apply effect below once we actually start playing.
                if (!player.state.value.isResolving) {
                    val target = syncManager.effectivePosition(pb)
                    // Over a slow phone-hotspot link a peer's periodic tick can
                    // arrive seconds late: a stale "paused" snapshot (older than
                    // a full sync tick) must not pause a track the user just
                    // started locally. The next fresh tick corrects anyway.
                    val staleAgeMs = syncManager.snapshotAgeMs(pb)
                    val stalePaused = !pb.isPlaying &&
                        !pb.userSeek &&
                        staleAgeMs != null && staleAgeMs > 4_000L
                    // Grace window after a local play/navigation command: the
                    // peer keeps echoing its pre-action "paused" state until it
                    // processes our play, and if that echo lands just after we
                    // finished resolving it pauses the track the user just
                    // started ("must press play twice when paired"). Within the
                    // window, a fresh peer "paused" snapshot is ignored.
                    val localPlayAgo = System.currentTimeMillis() - player.lastLocalPlayIntentAt
                    val gracePaused = !pb.isPlaying &&
                        !pb.userSeek &&
                        localPlayAgo >= 0L && localPlayAgo < 3_000L
                    when {
                        pb.isResolving -> {
                            // Peer is mid-song buffering (position frozen): keep
                            // playing and skip the seek instead of pausing, so a
                            // brief rebuffer on the phone doesn't stop the desktop.
                        }
                        stalePaused -> {
                            // Ignored: stale peer "paused" snapshot (see above).
                        }
                        gracePaused -> {
                            // Ignored: peer pre-action "paused" echo (see above).
                            // The next fresh tick applies the peer's real state.
                        }
                        pb.userSeek -> player.seekRemote(target, pb.isPlaying, toleranceMs = 0L)
                        else -> player.seekRemoteCatchUp(target, pb.isPlaying, SyncServer.RESYNC_TOLERANCE_MS)
                    }
                }
            } else {
                // Last-write-wins for the queue: only replace the local queue if
                // the remote edit is newer (or unknown, from an older peer).
                // Volume/position sync above still runs regardless.
                val newerQueue = pb.queueUpdatedAt <= 0L || pb.queueUpdatedAt >= syncManager.queueUpdatedAt()
                val localPlayAgo = System.currentTimeMillis() - player.lastLocalPlayIntentAt
                val inGraceWindow = localPlayAgo >= 0L && localPlayAgo < 3_000L
                if (newerQueue && !(inGraceWindow && !pb.isPlaying && !pb.isResolving)) {
                    val tracks = pb.queue.map { ref ->
                        NowPlaying(videoId = ref.id, title = ref.title, artist = ref.artist.orEmpty(), thumbnail = ref.thumbnail, durationMs = ref.durationMs)
                    }
                    if (tracks.isNotEmpty()) {
                        player.applyRemotePlayback(tracks, pb.queueIndex, syncManager.effectivePosition(pb), pb.isPlaying, pb.isResolving)
                        syncManager.noteQueueApplied(pb)
                    }
                }
            }
        }
    }

    // Re-apply the peer's latest snapshot the moment our stream finishes
    // resolving: a seek/play-pause received while we were still buffering was
    // held off above, so without this it would be lost until the next periodic
    // re-sync (slow over a phone hotspot). Only same-track corrections are
    // re-applied; a changed queue is handled by the collector above.
    LaunchedEffect(syncManager) {
        var wasResolving = player.state.value.isResolving
        var resolvingSince = if (wasResolving) System.currentTimeMillis() else 0L
        player.state.map { it.isResolving }.distinctUntilChanged().collect { resolving ->
            if (resolving) {
                resolvingSince = System.currentTimeMillis()
            } else if (wasResolving) {
                // Only honor a snapshot that arrived WHILE we were buffering: a
                // pre-play snapshot (received before we started resolving) is
                // stale and must not pause a track the user just started — that
                // left the button stuck and forced a second play press.
                val (receivedAt, pb) = latestRemotePlayback.get() ?: (0L to null)
                val currentId = player.state.value.current?.videoId
                if (receivedAt >= resolvingSince && pb != null && !pb.isResolving &&
                    currentId != null && pb.trackId == currentId
                ) {
                    val target = syncManager.effectivePosition(pb)
                    if (pb.userSeek) {
                        player.seekRemote(target, pb.isPlaying, toleranceMs = 0L)
                    } else {
                        player.seekRemoteCatchUp(target, pb.isPlaying, SyncServer.RESYNC_TOLERANCE_MS)
                    }
                }
            }
            wasResolving = resolving
        }
    }

    // Apply incoming settings snapshots from the peer.
    LaunchedEffect(syncManager) {
        syncManager.incomingSettings.collect { settings ->
            settings["darkMode"]?.let { mode ->
                onThemeModeChange(
                    when (mode) {
                        "ON" -> ThemeMode.DARK
                        "OFF" -> ThemeMode.LIGHT
                        else -> ThemeMode.SYSTEM
                    }
                )
            }
            settings["appLanguage"]?.let { lang ->
                val normalized = Languages.fromMobileCode(lang)
                if (lang != "SYSTEM_DEFAULT" && Languages.all.any { it.code == normalized }) {
                    onLanguageChange(normalized)
                }
            }
            settings["selectedThemeColor"]?.toIntOrNull()?.let { argb ->
                onAccentChange(argbIntToColor(argb))
            }
            settings["syncViviVolume"]?.toBooleanStrictOrNull()?.let { v ->
                syncViviVolume = v
                DesktopSettings.update { it.copy(syncViviVolume = v) }
            }
        }
    }

    // Push the local settings when they change (also once on startup).
    LaunchedEffect(syncManager, language, themeMode, accent, syncViviVolume) {
        syncManager.updateSettings(desktopSettingsMap(language, themeMode, accent, syncViviVolume))
    }

    // Playlist sync: push the local playlists whenever they change and apply
    // the peer's list (last-write-wins per playlist id).
    LaunchedEffect(syncManager) {
        PlaylistStore.all.collect {
            syncManager.updateLibrary(LibrarySnapshot(playlists = PlaylistStore.toSynced()))
        }
    }
    LaunchedEffect(syncManager) {
        syncManager.incomingLibrary.collect { lib ->
            lib?.playlists?.let { PlaylistStore.applyRemote(it) }
        }
    }

    // UI density scale: multiply the density so every dp-based measurement
    // zooms (200% down to 55%), matching the Android density setting.
    val baseDensity = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(baseDensity.density * densityScale, baseDensity.fontScale)
    ) {
    CompositionLocalProvider(
        LocalPlayback provides PlaybackContext(
            videoId = nowPlaying?.videoId,
            isPlaying = isPlaying,
            audioLevel = player.audioLevel,
        )
    ) {
    Row(
        Modifier
            .fillMaxSize()
            // Spotify mode: the flat background (#121212) sits behind the
            // `surfaceContainer` panels (#181818) so the 3-panel shell gets
            // depth; the M3 path keeps the tonal background as before.
            .background(MaterialTheme.colorScheme.background)
            .onKeyEvent(onGlobalKey)
    ) {
        if (spotifyLayout) {
            AnimatedVisibility(
                // Collapsed state compresses the sidebar to a compact icon rail
                // (72dp) instead of hiding it; it is fully hidden only on the
                // full-screen player.
                visible = current != Screen.Player,
                enter = expandHorizontally() + fadeIn(),
                exit = shrinkHorizontally() + fadeOut(),
            ) {
                Surface(
                    modifier = Modifier.fillMaxHeight().padding(start = 6.dp, top = 6.dp, end = 4.dp, bottom = 6.dp),
                    // Spotify mode: the black sidebar floats directly on the
                    // flat background (no surface frame around it).
                    color = if (spotifyLayout) Color.Transparent else MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Sidebar(
                        hideHistory = pauseListenHistory,
                        language = language,
                        current = current,
                        collapsed = sidebarCollapsed,
                        showTitleHeader = false,
                        userName = displayUserName,
                        userHandle = displayUserHandle,
                        isLoggedIn = isLoggedIn,
                        spotify = spotifyLayout,
                        onToggleCollapsed = {
                            sidebarCollapsed = !sidebarCollapsed
                            DesktopSettings.update { it.copy(sidebarCollapsed = sidebarCollapsed) }
                        },
                        onSelect = openRoot,
                    )
                }
            }
        } else {
            AnimatedVisibility(
                visible = current != Screen.Player,
                enter = expandHorizontally() + fadeIn(),
                exit = shrinkHorizontally() + fadeOut(),
            ) {
                Sidebar(
                    hideHistory = pauseListenHistory,
                    language = language,
                    current = current,
                    collapsed = sidebarCollapsed,
                    userName = displayUserName,
                    userHandle = displayUserHandle,
                    isLoggedIn = isLoggedIn,
                    spotify = spotifyLayout,
                    onToggleCollapsed = {
                        sidebarCollapsed = !sidebarCollapsed
                        DesktopSettings.update { it.copy(sidebarCollapsed = sidebarCollapsed) }
                    },
                    onSelect = openRoot,
                )
            }
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            if (spotifyLayout && current != Screen.Player) {
                SpotifyTopHeader(
                    sidebarCollapsed = sidebarCollapsed,
                    onToggleSidebar = {
                        sidebarCollapsed = !sidebarCollapsed
                        DesktopSettings.update { it.copy(sidebarCollapsed = sidebarCollapsed) }
                    },
                    language = language,
                    canGoBack = backStack.size > 1,
                    onBack = goBack,
                    onOpenSearch = { navigate(Screen.Search) },
                    onOpenHome = { openRoot(Screen.Home) },
                    onOpenHistory = { navigate(Screen.History) },
                    onOpenStats = { navigate(Screen.SettingsWrapped) },
                    onOpenListenTogether = { openRoot(Screen.ListenTogether) },
                    onOpenSettings = { openRoot(Screen.Settings) },
                    onOpenLyrics = { navigate(Screen.Lyrics) },
                    onOpenQueue = { openRoot(Screen.Queue) },
                    onMinimize = onMinimize,
                    onMaximize = onMaximize,
                    onClose = onClose,
                    isMaximized = isMaximized,
                    // With the native title bar the OS provides the window
                    // controls, so VIVI's bar hides its own buttons.
                    showWindowControls = !nativeTitleBar,
                    searchQuery = headerSearchQuery,
                    onSearchQueryChange = { newQ -> headerSearchQuery = newQ },
                    onSearchSubmit = { q -> recordSearch(q) },
                    selectedFilter = headerSearchFilter,
                    onFilterSelect = { f -> headerSearchFilter = f },
                )
            }
            Row(Modifier.weight(1f).fillMaxWidth().padding(if (spotifyLayout) androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp) else androidx.compose.foundation.layout.PaddingValues(0.dp))) {
            Surface(
                modifier = Modifier.weight(1f).fillMaxHeight().padding(if (spotifyLayout) androidx.compose.foundation.layout.PaddingValues(start = 2.dp, top = 0.dp, end = 2.dp, bottom = 6.dp) else androidx.compose.foundation.layout.PaddingValues(0.dp)),
                color = if (spotifyLayout) MaterialTheme.colorScheme.surfaceContainer else Color.Transparent,
                shape = if (spotifyLayout) RoundedCornerShape(12.dp) else androidx.compose.ui.graphics.RectangleShape,
            ) {
                    Box(Modifier.fillMaxSize()) {
                        AnimatedContent(
                            targetState = current,
                            transitionSpec = {
                                when (screenTransition) {
                                    "slide" -> (slideInHorizontally(animationSpec = tween(220)) { it / 4 } + fadeIn(animationSpec = tween(220))) togetherWith
                                        (slideOutHorizontally(animationSpec = tween(220)) { -it / 4 } + fadeOut(animationSpec = tween(220)))
                                    "off" -> fadeIn(animationSpec = tween(0)) togetherWith fadeOut(animationSpec = tween(0))
                                    else -> fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(180))
                                }
                            },
                            label = "screenTransition",
                        ) { screen ->
                when (screen) {
                    is Screen.Home -> HomeScreen(
                        language = language,
                        onOpenAlbum = { navigate(Screen.Album(it)) },
                        onOpenArtist = { navigate(Screen.Artist(it)) },
                        onOpenPlaylist = { navigate(Screen.Playlist(it)) },
                        onPlaySong = playSong,
                        onAddToPlaylist = addToPlaylist,
                        onPlayAll = playAll,
                        onOpenBrowse = { browseId, params -> navigate(Screen.Browse(browseId, params)) },
                        userName = displayUserName,
                        useLastListen = homeUseLastListen,
                        onUseLastListenChange = { v ->
                            homeUseLastListen = v
                            DesktopSettings.update { it.copy(homeUseLastListen = v) }
                        },
                        randomizeOrder = randomizeHomeOrder,
                        onRandomizeOrderChange = { v ->
                            randomizeHomeOrder = v
                            DesktopSettings.update { it.copy(randomizeHomeOrder = v) }
                        },
                        wrappedStats = WrappedStats(
                            trackStarts = sessionTrackStarts,
                            playedMs = sessionPlayedMs,
                            topSongTitle = sessionTopSong?.second,
                            topSongCount = sessionTopCount,
                        ),
                        showWrapped = showWrappedOnHome,
                    )
                    is Screen.Search -> SearchScreen(
                        language = language,
                        gridItemSize = gridItemSize,
                        onOpenAlbum = { navigate(Screen.Album(it)) },
                        onOpenArtist = { navigate(Screen.Artist(it)) },
                        onOpenPlaylist = { navigate(Screen.Playlist(it)) },
                        onPlaySong = playSong,
                        onAddToQueue = addToQueue,
                        onAddToPlaylist = addToPlaylist,
                        searchHistory = if (pauseSearchHistory) emptyList() else searchHistory,
                        onRecordSearch = recordSearch,
                        onClearSearchHistory = {
                            searchHistory = emptyList()
                            DesktopSettings.update { it.copy(searchHistory = emptyList()) }
                        },
                        externalQuery = headerSearchQuery,
                        onQueryChange = { newQ -> headerSearchQuery = newQ },
                        showTextField = !spotifyLayout,
                        externalFilter = headerSearchFilter,
                        onFilterChange = { f -> headerSearchFilter = f },
                        showFiltersInBody = !spotifyLayout,
                    )
                    is Screen.Library -> LibraryScreen(
                        language = language,
                        isLoggedIn = isLoggedIn,
                        gridItemSize = gridItemSize,
                        onLoggedIn = {
                            isLoggedIn = true
                            accountName = DesktopSettings.load().accountName
                        },
                        onOpenAlbum = { navigate(Screen.Album(it)) },
                        onOpenArtist = { navigate(Screen.Artist(it)) },
                        onOpenPlaylist = { navigate(Screen.Playlist(it)) },
                        onPlaySong = playSong,
                        onAddToQueue = addToQueue,
                        onAddToPlaylist = addToPlaylist,
                        onShuffleAll = shuffleAll,
                    )
                    is Screen.History -> HistoryScreen(
                        language = language,
                        onBack = goBack,
                        onPlaySong = playSong,
                        onAddToQueue = addToQueue,
                        onAddToPlaylist = addToPlaylist,
                    )
                    is Screen.NewReleases -> NewReleasesScreen(
                        language = language,
                        gridItemSize = gridItemSize,
                        onBack = goBack,
                        onOpenAlbum = { navigate(Screen.Album(it)) },
                    )
                    is Screen.Charts -> ChartsScreen(
                        language = language,
                        onBack = goBack,
                        onOpenAlbum = { navigate(Screen.Album(it)) },
                        onOpenArtist = { navigate(Screen.Artist(it)) },
                        onOpenPlaylist = { navigate(Screen.Playlist(it)) },
                        onPlaySong = playSong,
                        onAddToQueue = addToQueue,
                        onAddToPlaylist = addToPlaylist,
                    )
                    is Screen.MoodGenres -> MoodGenresScreen(
                        language = language,
                        onBack = goBack,
                        onOpenBrowse = { browseId, params -> navigate(Screen.Browse(browseId, params)) },
                    )
                    is Screen.SongRecognition -> SongRecognitionScreen(
                        language = language,
                        onBack = goBack,
                        history = recognitionHistory,
                        onHistoryChange = { h ->
                            recognitionHistory = h
                            DesktopSettings.update { it.copy(recognitionHistory = h) }
                        },
                    )
                    is Screen.Settings -> SettingsScreen(
                        language = language,
                        themeMode = themeMode,
                        isLoggedIn = isLoggedIn,
                        accountName = accountName,
                        updateStatus = updateStatus,
                        wrappedStats = WrappedStats(
                            trackStarts = sessionTrackStarts,
                            playedMs = sessionPlayedMs,
                            topSongTitle = sessionTopSong?.second,
                            topSongCount = sessionTopCount,
                        ),
                        onOpen = navigate,
                    )
                    is Screen.SettingsLanguage -> SettingsLanguageScreen(
                        language = language,
                        onBack = goBack,
                        onLanguageChange = onLanguageChange,
                    )
                    is Screen.SettingsAppearance -> SettingsAppearanceScreen(
                        language = language,
                        onBack = goBack,
                        selectedFont = font,
                        densityScale = densityScale,
                        screenTransition = screenTransition,
                        onOpenTheme = { navigate(Screen.SettingsTheme) },
                        onOpenFont = { navigate(Screen.SettingsFont) },
                        onOpenCanvas = { navigate(Screen.SettingsCanvas) },
                        onOpenDensity = { navigate(Screen.SettingsDensity) },
                        onOpenTransitions = { navigate(Screen.SettingsTransitions) },
                        onOpenIntro = { navigate(Screen.SettingsIntro) },
                        onOpenPlayerDesign = { navigate(Screen.SettingsPlayerDesign) },
                        nativeTitleBar = nativeTitleBar,
                        onNativeTitleBarChange = onNativeTitleBarChange,
                        showRightSidebar = showRightSidebar,
                        onShowRightSidebarChange = { v ->
                            showRightSidebar = v
                            DesktopSettings.update { it.copy(showRightSidebar = v) }
                        },
                        onRestart = onRestart,
                    )
                    is Screen.SettingsTransitions -> SettingsTransitionsScreen(
                        language = language,
                        onBack = goBack,
                        screenTransition = screenTransition,
                        onScreenTransitionChange = { t ->
                            screenTransition = t
                            DesktopSettings.update { it.copy(screenTransition = t) }
                        },
                    )
                    is Screen.SettingsDensity -> SettingsDensityScreen(
                        language = language,
                        onBack = goBack,
                        densityScale = densityScale,
                        onDensityScaleChange = { s ->
                            densityScale = s
                            DesktopSettings.update { it.copy(densityScale = s) }
                        },
                        gridItemSize = gridItemSize,
                        onGridItemSizeChange = { g ->
                            gridItemSize = g
                            DesktopSettings.update { it.copy(gridItemSize = g) }
                        },
                    )
                    is Screen.SettingsTheme -> SettingsThemeScreen(
                        language = language,
                        onBack = goBack,
                        themeMode = themeMode,
                        accent = accent,
                        onThemeModeChange = onThemeModeChange,
                        onAccentChange = onAccentChange,
                        accentIntensity = accentIntensity,
                        onAccentIntensityChange = onAccentIntensityChange,
                        pureBlack = pureBlack,
                        onPureBlackChange = onPureBlackChange,
                    )
                    is Screen.SettingsFont -> SettingsFontScreen(
                        language = language,
                        onBack = goBack,
                        selectedFont = font,
                        onFontChange = onFontChange,
                        customFontPath = customFontPath,
                        onImportFont = onImportFont,
                    )
                    is Screen.SettingsCanvas -> SettingsCanvasScreen(
                        language = language,
                        onBack = goBack,
                        canvasEnabled = canvasEnabled,
                        onCanvasEnabledChange = { enabled ->
                            canvasEnabled = enabled
                            DesktopSettings.update { it.copy(canvasEnabled = enabled) }
                        },
                        canvasSource = canvasSource,
                        onCanvasSourceChange = { s ->
                            canvasSource = s
                            DesktopSettings.update { it.copy(canvasSource = s.key) }
                        },
                    )
                    is Screen.SettingsPlayer -> SettingsPlayerScreen(
                        language = language,
                        onBack = goBack,
                        autoPlayNext = autoPlayNext,
                        onToggleAutoPlayNext = { checked ->
                            autoPlayNext = checked
                            DesktopSettings.update { it.copy(autoPlayNext = checked) }
                        },
                        audioQuality = audioQuality,
                        onAudioQualityChange = { q ->
                            audioQuality = q
                            DesktopSettings.update { it.copy(audioQuality = q) }
                        },
                        rememberShuffleRepeat = rememberShuffleRepeat,
                        onToggleRememberShuffleRepeat = { checked ->
                            rememberShuffleRepeat = checked
                            DesktopSettings.update { it.copy(rememberShuffleRepeat = checked) }
                        },
                        persistentQueue = persistentQueue,
                        onTogglePersistentQueue = { checked ->
                            persistentQueue = checked
                            DesktopSettings.update { it.copy(persistentQueue = checked) }
                        },
                        syncViviVolume = syncViviVolume,
                        onToggleSyncViviVolume = { checked ->
                            syncViviVolume = checked
                            DesktopSettings.update { it.copy(syncViviVolume = checked) }
                        },
                        sliderStyle = sliderStyle,
                        onSliderStyleChange = { s ->
                            sliderStyle = s
                            DesktopSettings.update { it.copy(sliderStyle = s) }
                        },
                        onOpenPlayerDesign = { navigate(Screen.SettingsPlayerDesign) },
                        streamCacheMinutes = streamCacheMinutes,
                        onStreamCacheMinutesChange = { m ->
                            streamCacheMinutes = m
                            DesktopSettings.update { it.copy(streamCacheMinutes = m) }
                        },
                    )
                    is Screen.SettingsPlayerDesign -> SettingsPlayerDesignScreen(
                        language = language,
                        onBack = goBack,
                        design = playerDesign,
                        onDesignChange = { d ->
                            playerDesign = d
                            DesktopSettings.update { it.copy(playerDesign = d.key) }
                        },
                        background = playerBackground,
                        onBackgroundChange = { b ->
                            playerBackground = b
                            DesktopSettings.update { it.copy(playerBackground = b.key) }
                        },
                        rotatingThumbnail = rotatingThumbnail,
                        onRotatingThumbnailChange = { r ->
                            rotatingThumbnail = r
                            DesktopSettings.update { it.copy(rotatingThumbnail = r) }
                        },
                        miniPlayerStyle = miniPlayerStyle,
                        onMiniPlayerStyleChange = { s ->
                            miniPlayerStyle = s
                            DesktopSettings.update { it.copy(miniPlayerStyle = s) }
                        },
                        miniPlayerDesign = miniPlayerDesign,
                        onMiniPlayerDesignChange = { d ->
                            miniPlayerDesign = d
                            DesktopSettings.update { it.copy(miniPlayerDesign = d.key) }
                        },
                        miniPlayerBackgroundStyle = miniPlayerBackgroundStyle,
                        onMiniPlayerBackgroundStyleChange = { b ->
                            miniPlayerBackgroundStyle = b
                            DesktopSettings.update { it.copy(miniPlayerBackgroundStyle = b.key) }
                        },
                        pureBlackMiniPlayer = pureBlackMiniPlayer,
                        onPureBlackMiniPlayerChange = { p ->
                            pureBlackMiniPlayer = p
                            DesktopSettings.update { it.copy(pureBlackMiniPlayer = p) }
                        },
                    )
                    is Screen.SettingsAccount -> SettingsAccountScreen(
                        language = language,
                        onBack = goBack,
                        isLoggedIn = isLoggedIn,
                        accountName = accountName,
                        onOpenLogin = { navigate(Screen.Login) },
                        onLogout = {
                            LoginManager.logout()
                            isLoggedIn = false
                            accountName = ""
                            accountChannelHandle = ""
                        },
                        onLoggedIn = {
                            isLoggedIn = true
                            accountName = DesktopSettings.load().accountName
                        },
                    )
                    is Screen.SettingsDevices -> SettingsDevicesScreen(
                        language = language,
                        onBack = goBack,
                        syncManager = syncManager,
                        syncViviVolume = syncViviVolume,
                        onToggleSyncViviVolume = { checked ->
                            syncViviVolume = checked
                            DesktopSettings.update { it.copy(syncViviVolume = checked) }
                        },
                    )
                    is Screen.SettingsDesktop -> SettingsDesktopScreen(
                        language = language,
                        onBack = goBack,
                        isWindows = isWindows,
                        showWidget = showWidget,
                        onShowWidgetChange = { v ->
                            showWidget = v
                            DesktopSettings.update { it.copy(showNowPlayingWidget = v) }
                        },
                        mediaKeysEnabled = mediaKeysEnabled,
                        onMediaKeysChange = { v ->
                            mediaKeysEnabled = v
                            DesktopSettings.update { it.copy(mediaKeysEnabled = v) }
                        },
                        trayMenuEnabled = trayMenuEnabled,
                        onTrayMenuChange = { v ->
                            trayMenuEnabled = v
                            DesktopSettings.update { it.copy(trayMenuEnabled = v) }
                        },
                    )
                    is Screen.SettingsIntegrations -> SettingsIntegrationsScreen(
                        language = language,
                        onBack = goBack,
                        discordEnabled = discordRpcEnabled,
                        onDiscordEnabledChange = { v ->
                            discordRpcEnabled = v
                            DesktopSettings.update { it.copy(discordRpcEnabled = v) }
                        },
                        discordClientId = discordClientId,
                        onDiscordClientIdChange = { v ->
                            discordClientId = v
                            DesktopSettings.update { it.copy(discordClientId = v) }
                        },
                        lastfmEnabled = lastfmEnabled,
                        onLastfmEnabledChange = { v ->
                            lastfmEnabled = v
                            DesktopSettings.update { it.copy(lastfmEnabled = v) }
                        },
                        lastfmSession = lastfmSession,
                        onLastfmSessionChange = { v ->
                            lastfmSession = v
                            DesktopSettings.update { it.copy(lastfmSession = v) }
                        },
                        lastfmNowPlaying = lastfmNowPlaying,
                        onLastfmNowPlayingChange = { v ->
                            lastfmNowPlaying = v
                            DesktopSettings.update { it.copy(lastfmNowPlaying = v) }
                        },
                    )
                    is Screen.SettingsWrapped -> SettingsWrappedScreen(
                        language = language,
                        onBack = goBack,
                        wrappedStats = WrappedStats(
                            trackStarts = sessionTrackStarts,
                            playedMs = sessionPlayedMs,
                            topSongTitle = sessionTopSong?.second,
                            topSongCount = sessionTopCount,
                        ),
                        showWrappedOnHome = showWrappedOnHome,
                        onShowWrappedOnHomeChange = { v ->
                            showWrappedOnHome = v
                            DesktopSettings.update { it.copy(showWrappedOnHome = v) }
                        },
                    )
                    is Screen.SettingsPrivacy -> SettingsPrivacyScreen(
                        language = language,
                        onBack = goBack,
                        pauseListenHistory = pauseListenHistory,
                        onPauseListenHistoryChange = { v ->
                            pauseListenHistory = v
                            DesktopSettings.update { it.copy(pauseListenHistory = v) }
                        },
                        pauseSearchHistory = pauseSearchHistory,
                        onPauseSearchHistoryChange = { v ->
                            pauseSearchHistory = v
                            DesktopSettings.update { it.copy(pauseSearchHistory = v) }
                        },
                        onClearSearchHistory = {
                            searchHistory = emptyList()
                            DesktopSettings.update { it.copy(searchHistory = emptyList()) }
                        },
                    )
                    is Screen.SettingsContent -> SettingsContentScreen(
                        language = language,
                        onBack = goBack,
                        contentLanguage = contentLanguage,
                        contentCountry = contentCountry,
                        onContentLanguageChange = { code ->
                            contentLanguage = code
                            DesktopSettings.update { it.copy(contentLanguage = code) }
                            YouTube.locale = resolveYouTubeLocale(contentLanguage, contentCountry)
                        },
                        onContentCountryChange = { code ->
                            contentCountry = code
                            DesktopSettings.update { it.copy(contentCountry = code) }
                            YouTube.locale = resolveYouTubeLocale(contentLanguage, contentCountry)
                        },
                    )
                    is Screen.SettingsLyrics -> SettingsLyricsScreen(
                        language = language,
                        onBack = goBack,
                        syncedLyrics = syncedLyrics,
                        onToggleSyncedLyrics = { checked ->
                            syncedLyrics = checked
                            DesktopSettings.update { it.copy(syncedLyrics = checked) }
                        },
                        lyricsTextSize = lyricsTextSize,
                        onLyricsTextSizeChange = { size ->
                            lyricsTextSize = size
                            DesktopSettings.update { it.copy(lyricsTextSize = size) }
                        },
                        lyricsLineSpacing = lyricsLineSpacing,
                        onLyricsLineSpacingChange = { ls ->
                            lyricsLineSpacing = ls
                            DesktopSettings.update { it.copy(lyricsLineSpacing = ls) }
                        },
                    )
                    is Screen.SettingsStorage -> SettingsStorageScreen(
                        language = language,
                        onBack = goBack,
                    )
                    is Screen.SettingsUpdates -> SettingsUpdatesScreen(
                        language = language,
                        onBack = goBack,
                        updateStatus = updateStatus,
                        includePreReleases = includePreReleases,
                        updateIntervalHours = updateIntervalHours,
                        updateSource = updateSource,
                        onIntervalChange = { hours ->
                            updateIntervalHours = hours
                            DesktopSettings.update { it.copy(updateCheckIntervalHours = hours) }
                        },
                        onTogglePreReleases = { checked ->
                            includePreReleases = checked
                            DesktopSettings.update { it.copy(includePreReleases = checked) }
                            runUpdateCheck()
                        },
                        onUpdateSourceChange = { source ->
                            updateSource = source
                            DesktopSettings.update { it.copy(updateSource = source) }
                            runUpdateCheck()
                        },
                        onCheckUpdates = { runUpdateCheck() },
                        onOpenChangelog = { navigate(Screen.Changelog) },
                        onOpenCommits = { navigate(Screen.SettingsCommits) },
                    )
                    is Screen.SettingsCommits -> CommitsScreen(
                        language = language,
                        onBack = goBack,
                    )
                    is Screen.SettingsAbout -> SettingsAboutScreen(
                        language = language,
                        onBack = goBack,
                    )
                    is Screen.SettingsDeveloper -> SettingsDeveloperScreen(
                        language = language,
                        onBack = goBack,
                        syncManager = syncManager,
                    )
                    is Screen.SettingsSystem -> SettingsSystemScreen(
                        language = language,
                        onBack = goBack,
                        showIntroSplash = showIntroSplash,
                        onOpenDeveloper = { navigate(Screen.SettingsDeveloper) },
                        onOpenIntro = { navigate(Screen.SettingsIntro) },
                    )
                    is Screen.SettingsIntro -> SettingsIntroScreen(
                        language = language,
                        onBack = goBack,
                        showIntroSplash = showIntroSplash,
                        onShowIntroSplashChange = { v ->
                            showIntroSplash = v
                            DesktopSettings.update { it.copy(showIntroSplash = v) }
                        },
                        introStyle = introStyle,
                        onIntroStyleChange = { v ->
                            introStyle = v
                            DesktopSettings.update { it.copy(introStyle = v) }
                        },
                        introBackground = introBackground,
                        onIntroBackgroundChange = { v ->
                            introBackground = v
                            DesktopSettings.update { it.copy(introBackground = v) }
                        },
                    )
                    is Screen.SettingsBackup -> SettingsBackupScreen(
                        language = language,
                        onBack = goBack,
                    )
                    is Screen.SettingsNotifications -> SettingsNotificationsScreen(
                        language = language,
                        onBack = goBack,
                        notificationMode = notificationMode,
                        onNotificationModeChange = { mode ->
                            notificationMode = mode
                            DesktopSettings.update { it.copy(notificationMode = mode) }
                        },
                        notificationDurationSeconds = notificationDurationSeconds,
                        onNotificationDurationChange = { secs ->
                            notificationDurationSeconds = secs
                            DesktopSettings.update { it.copy(inAppNotificationDurationSeconds = secs) }
                        },
                        saveHistory = saveNotificationHistory,
                        onSaveHistoryChange = { save ->
                            saveNotificationHistory = save
                            DesktopSettings.update { it.copy(saveNotificationHistory = save) }
                        },
                        onOpenHistory = { navigate(Screen.SettingsNotificationsHistory) },
                        onTestNotification = {
                            DesktopNotifier.notify(
                                "VIVI Music DE",
                                Localization.get(language, "test_notification"),
                                null,
                            )
                        },
                    )
                    is Screen.SettingsNotificationsHistory -> NotificationHistoryScreen(
                        language = language,
                        onBack = goBack,
                    )
                    is Screen.Album -> AlbumScreen(
                        browseId = screen.browseId,
                        language = language,
                        onBack = goBack,
                        onOpenArtist = { navigate(Screen.Artist(it)) },
                        onPlaySong = playSong,
                        onAddToQueue = addToQueue,
                        onAddToPlaylist = addToPlaylist,
                        onPlayAll = playAll,
                        onShuffleAll = shuffleAll,
                    )
                    is Screen.Artist -> ArtistScreen(
                        browseId = screen.browseId,
                        language = language,
                        onBack = goBack,
                        onOpenAlbum = { navigate(Screen.Album(it)) },
                        onOpenArtist = { navigate(Screen.Artist(it)) },
                        onOpenPlaylist = { navigate(Screen.Playlist(it)) },
                        onPlaySong = playSong,
                        onAddToQueue = addToQueue,
                        onAddToPlaylist = addToPlaylist,
                    )
                    is Screen.Playlist -> PlaylistScreen(
                        playlistId = screen.playlistId,
                        language = language,
                        onBack = goBack,
                        onOpenArtist = { navigate(Screen.Artist(it)) },
                        onPlaySong = playSong,
                        onAddToQueue = addToQueue,
                        onAddToPlaylist = addToPlaylist,
                        onPlayAll = playAll,
                        onShuffleAll = shuffleAll,
                    )
                    is Screen.LocalPlaylists -> LocalPlaylistsScreen(
                        language = language,
                        onBack = goBack,
                        onOpenPlaylist = { navigate(Screen.LocalPlaylist(it)) },
                    )
                    is Screen.LocalPlaylist -> LocalPlaylistScreen(
                        playlistId = screen.playlistId,
                        language = language,
                        onBack = goBack,
                        onPlay = { s -> player.play(NowPlaying(videoId = s.id, title = s.title, artist = s.artist, thumbnail = s.thumbnail)) },
                        onPlayAll = { songs -> player.playAll(songs.map { NowPlaying(videoId = it.id, title = it.title, artist = it.artist, thumbnail = it.thumbnail) }) },
                    )
                    is Screen.Browse -> BrowseScreen(
                        browseId = screen.browseId,
                        params = screen.params,
                        language = language,
                        gridItemSize = gridItemSize,
                        onBack = goBack,
                        onOpenAlbum = { navigate(Screen.Album(it)) },
                        onOpenArtist = { navigate(Screen.Artist(it)) },
                        onOpenPlaylist = { navigate(Screen.Playlist(it)) },
                        onPlaySong = playSong,
                    )
                    is Screen.Player -> PlayerScreen(
                        queue = playerState.queue,
                        index = playerState.index,
                        isPlaying = isPlaying,
                        positionMs = playerState.positionMs,
                        durationMs = playerState.durationMs,
                        volume = playerState.volume,
                        isShuffle = playerState.isShuffle,
                        repeatMode = playerState.repeatMode,
                        errorKey = playerState.errorKey,
                        errorDetail = playerState.errorDetail,
                        loadPhase = playerState.loadPhase,
                        onTogglePlay = { player.toggle() },
                        onNext = { player.next() },
                        onPrevious = { player.previous() },
                        onSeek = { player.seekTo(it) },
                        onVolume = setLocalVolume,
                        onToggleShuffle = { player.toggleShuffle() },
                        onCycleRepeat = { player.cycleRepeatMode() },
                        language = language,
                        onOpenLyrics = { navigate(Screen.Lyrics) },
                        onOpenLyricsFocus = { navigate(Screen.LyricsFocus) },
                        onOpenQueue = { navigate(Screen.Queue) },
                        onAddToPlaylist = addNowPlayingToPlaylist,
                        onSkipTo = { player.skipTo(it) },
                        onRemoveAt = { player.removeAt(it) },
                        onClearQueue = { player.clearQueue() },
                        onReorderQueue = { player.reorder(it) },
                        sliderStyle = ViviSliderStyle.from(sliderStyle),
                        design = playerDesign,
                        background = playerBackground,
                        rotatingThumbnail = rotatingThumbnail,
                        accent = accent,
                        audioLevel = audioLevel,
                        onBack = goBack,
                    )
                    is Screen.LyricsFocus -> LyricsFocusScreen(
                        nowPlaying = nowPlaying,
                        positionMs = playerState.positionMs,
                        isPlaying = isPlaying,
                        language = language,
                        synced = syncedLyrics,
                        textSizeSp = lyricsTextSize,
                        lineSpacing = lyricsLineSpacing,
                        onTogglePlay = { player.toggle() },
                        onNext = { player.next() },
                        onPrevious = { player.previous() },
                        onBack = goBack,
                    )
                    is Screen.Lyrics -> LyricsScreen(
                        nowPlaying = nowPlaying,
                        positionMs = playerState.positionMs,
                        isPlaying = isPlaying,
                        language = language,
                        synced = syncedLyrics,
                        textSizeSp = lyricsTextSize,
                        lineSpacing = lyricsLineSpacing,
                        onTogglePlay = { player.toggle() },
                        onBack = goBack,
                    )
                    is Screen.Queue -> QueueScreen(
                        queue = playerState.queue,
                        index = playerState.index,
                        language = language,
                        onBack = goBack,
                        onSkipTo = { player.skipTo(it) },
                        onRemoveAt = { player.removeAt(it) },
                        onClear = { player.clearQueue() },
                        onReorder = { player.reorder(it) },
                        onAddToPlaylist = addNowPlayingToPlaylist,
                    )
                    is Screen.Changelog -> ChangelogScreen(
                        language = language,
                        onBack = goBack,
                    )
                    is Screen.ListenTogether -> ListenTogetherScreen(
                        language = language,
                        onBack = goBack,
                        onPlaySong = playSong,
                    )
                    is Screen.ArtistItems -> BrowseScreen(
                        browseId = screen.browseId,
                        params = screen.params,
                        language = language,
                        gridItemSize = gridItemSize,
                        onBack = goBack,
                        onOpenAlbum = { navigate(Screen.Album(it)) },
                        onOpenArtist = { navigate(Screen.Artist(it)) },
                        onOpenPlaylist = { navigate(Screen.Playlist(it)) },
                        onPlaySong = playSong,
                    )
                    is Screen.AutoPlaylist -> AutoPlaylistScreen(
                        browseId = screen.browseId,
                        title = screen.title,
                        language = language,
                        gridItemSize = gridItemSize,
                        onBack = goBack,
                        onOpenAlbum = { navigate(Screen.Album(it)) },
                        onOpenArtist = { navigate(Screen.Artist(it)) },
                        onOpenPlaylist = { navigate(Screen.Playlist(it)) },
                        onPlaySong = playSong,
                        onAddToQueue = addToQueue,
                        onAddToPlaylist = addToPlaylist,
                    )
                    is Screen.Login -> LoginScreen(
                        language = language,
                        onBack = goBack,
                        onLoggedIn = {
                            isLoggedIn = true
                            accountName = DesktopSettings.load().accountName
                        },
                    )
                }
                } // AnimatedContent
                if (showUpdateNotification && updateStatus is UpdateStatus.Available) {
                    UpdateNotification(
                        status = updateStatus as UpdateStatus.Available,
                        language = language,
                        onDismiss = { showUpdateNotification = false },
                        onDone = { showUpdateNotification = false },
                    )
                }
                if (showDevNotification) {
                    DevUnlockedNotification(
                        language = language,
                        onOpen = {
                            showDevNotification = false
                            navigate(Screen.SettingsDeveloper)
                        },
                        onDismiss = { showDevNotification = false },
                    )
                }
                appNotification?.let { notice ->
                    InAppNotification(
                        title = notice.title,
                        message = notice.message,
                        language = language,
                        onDismiss = { appNotification = null },
                    )
                }
                if (devEnabled && devMode == DevToolsMode.OVERLAY) {
                    DevToolsOverlay(
                        syncManager = syncManager,
                        language = language,
                        movable = overlayMovable,
                    )
                }
                addToPlaylistSong?.let { song ->
                    AddToPlaylistDialog(
                        language = language,
                        song = song,
                        onDismiss = { addToPlaylistSong = null },
                    )
                }
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
        if (showRightSidebar && spotifyLayout) {
            SpotifyRightNowPlayingPanel(
                nowPlaying = nowPlaying,
                isPlaying = isPlaying,
                positionMs = playerState.positionMs,
                language = language,
                onClose = {
                    showRightSidebar = false
                    DesktopSettings.update { it.copy(showRightSidebar = false) }
                },
                onOpenLyrics = { navigate(Screen.Lyrics) },
                onAddToPlaylist = addNowPlayingToPlaylist,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
            )
        }
    }
    if (current != Screen.Player) {
        // Spotify style: a thin top border separates the bottom bar from the
        // content above (like the desktop app).
        Column {
            if (spotifyLayout) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                )
            }
        SpotifyPlayerBar(
        nowPlaying = nowPlaying,
        isPlaying = isPlaying,
        isLoading = playerState.isLoading,
        positionMs = playerState.positionMs,
        durationMs = playerState.durationMs,
        volume = playerState.volume,
        isShuffle = playerState.isShuffle,
        repeatMode = playerState.repeatMode,
        onTogglePlay = { player.toggle() },
        onNext = { player.next() },
        onPrevious = { player.previous() },
        onSeek = { player.seekTo(it) },
        onVolume = setLocalVolume,
        onToggleShuffle = { player.toggleShuffle() },
        onCycleRepeat = { player.cycleRepeatMode() },
        onOpenPlayer = {
            if (current == Screen.Player) {
                goBack()
            } else {
                // Windows-friendly: opening the player must not resize or
                // maximize the window; the sidebar is hidden on the player
                // screen anyway.
                navigate(Screen.Player)
            }
        },
        onOpenQueue = { navigate(Screen.Queue) },
        onOpenLyrics = { navigate(Screen.Lyrics) },
        showRightSidebar = showRightSidebar,
        onToggleRightSidebar = {
            showRightSidebar = !showRightSidebar
            DesktopSettings.update { it.copy(showRightSidebar = showRightSidebar) }
        },
        isFullscreen = isFullscreen,
        onToggleFullscreen = onToggleFullscreen,
        language = language,
        miniPlayerDesign = miniPlayerDesign,
        miniPlayerBackgroundStyle = miniPlayerBackgroundStyle,
        pureBlackMiniPlayer = pureBlackMiniPlayer,
    )
        }
    }
}
}
    // The window is undecorated (no OS title bar) unless the native title bar
    // setting is on, so the window controls must always be visible: the Spotify
    // header hosts them when it is shown, and this overlay covers the full
    // player screen and the non-Spotify layout. With the native title bar the
    // OS provides the controls, so the overlay (and the header's own buttons)
    // are hidden. The transparent Box only hosts the buttons (top-right); its
    // empty area passes clicks through to the content below.
    if (!nativeTitleBar && (!spotifyLayout || current == Screen.Player)) {
        Box(Modifier.fillMaxSize()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f),
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 6.dp),
            ) {
                WindowControls(
                    isMaximized = isMaximized,
                    onMinimize = onMinimize,
                    onMaximize = onMaximize,
                    onClose = onClose,
                )
            }
        }
    }
    }
    } // density scale CompositionLocalProvider

    // Developer tools in a dedicated window (closing it falls back to overlay).
    if (devEnabled && devMode == DevToolsMode.WINDOW) {
        Window(
            onCloseRequest = { DeveloperOptions.setMode(DevToolsMode.OVERLAY) },
            title = "VIVI Music DE — Developer tools",
        ) {
            AppTheme(
                mode = themeMode,
                accent = accent,
                pureBlack = pureBlack,
                font = font,
                spotify = spotifyLayout,
                accentIntensity = accentIntensity,
                customFontPath = customFontPath,
            ) {
                SelectionContainer {
                    DevToolsPanel(syncManager = syncManager, language = language)
                }
            }
        }
    }
}

private data class SidebarEntry(
    val screen: Screen,
    val key: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

/**
 * M3 Expressive Cider-Inspired Navigation Rail (Sidebar).
 * Solid surfaceContainer background, spring collapsible sections, active indicator pills,
 * and pinned bottom user account capsule.
 */
@Composable
fun Sidebar(
    language: String,
    current: Screen,
    collapsed: Boolean,
    hideHistory: Boolean = false,
    showTitleHeader: Boolean = true,
    userName: String = "Guest",
    userHandle: String = "Not signed in",
    isLoggedIn: Boolean = false,
    spotify: Boolean = false,
    onToggleCollapsed: () -> Unit,
    onSelect: (Screen) -> Unit,
) {
    // Spotify style: pure-black sidebar (dark) / white (light) with a grey
    // selected pill; the classic layout keeps the accent-filled selection.
    val selectedBg = if (spotify) MaterialTheme.colorScheme.surfaceContainerHighest
    else MaterialTheme.colorScheme.primaryContainer
    val selectedFg = if (spotify) MaterialTheme.colorScheme.onSurface
    else MaterialTheme.colorScheme.onPrimaryContainer
    val mainRadius = if (spotify) 8.dp else 14.dp
    val subRadius = if (spotify) 8.dp else 12.dp
    var mainExpanded by remember { mutableStateOf(true) }
    var libraryExpanded by remember { mutableStateOf(true) }
    var playlistsExpanded by remember { mutableStateOf(true) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val localPlaylists by PlaylistStore.all.collectAsState()
    val activeLocalPlaylists = remember(localPlaylists) {
        localPlaylists.filter { !it.deleted }.sortedByDescending { it.updatedAt }
    }

    var onlinePlaylists by remember { mutableStateOf<List<com.music.innertube.models.PlaylistItem>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            com.music.innertube.YouTube.library("FEmusic_liked_playlists").fold(
                onSuccess = { page ->
                    onlinePlaylists = page.items.filterIsInstance<com.music.innertube.models.PlaylistItem>()
                },
                onFailure = {
                    onlinePlaylists = emptyList()
                }
            )
        } else {
            onlinePlaylists = emptyList()
        }
    }

    val mainChevronRotation by animateFloatAsState(
        targetValue = if (mainExpanded) 90f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "mainChevronRotation",
    )
    val libraryChevronRotation by animateFloatAsState(
        targetValue = if (libraryExpanded) 90f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "libraryChevronRotation",
    )
    val playlistsChevronRotation by animateFloatAsState(
        targetValue = if (playlistsExpanded) 90f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "playlistsChevronRotation",
    )

    val mainEntries = listOf(
        SidebarEntry(Screen.Home, "home", Icons.Outlined.Home, Icons.Filled.Home),
        SidebarEntry(Screen.NewReleases, "new", Icons.Outlined.Explore, Icons.Filled.Explore),
        SidebarEntry(Screen.Charts, "radio", Icons.Outlined.Radio, Icons.Filled.Radio),
    )

    val librarySubEntries = listOf(
        SidebarEntry(Screen.History, "history", Icons.Outlined.History, Icons.Filled.History),
        SidebarEntry(Screen.Library, "songs", Icons.Outlined.MusicNote, Icons.Filled.MusicNote),
        SidebarEntry(Screen.LocalPlaylists, "albums", Icons.Outlined.LibraryMusic, Icons.Filled.LibraryMusic),
        SidebarEntry(
            Screen.ArtistItems("FEmusic_library_corpus_artists", null),
            "artists",
            Icons.Outlined.Group,
            Icons.Filled.Group,
        ),
    )

    val width by animateDpAsState(if (collapsed) 72.dp else 230.dp, label = "sidebarWidth")

    Surface(
        color = if (spotify) {
            if (isSystemInDarkTheme()) Color(0xFF000000) else MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        }
    ) {
        Column(
            Modifier
                .width(width)
                .fillMaxHeight()
                .padding(horizontal = if (collapsed) 8.dp else 12.dp, vertical = 12.dp),
        ) {
            // Collapsed: menu button to expand the rail (works in both the
            // Spotify layout and the classic layout).
            if (collapsed) {
                Tooltip("Menu") {
                    IconButton(
                        onClick = onToggleCollapsed,
                        modifier = Modifier.padding(bottom = 8.dp),
                    ) {
                        Icon(
                            Icons.Filled.Menu,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            } else if (showTitleHeader) {
                // Expanded classic layout: app title with a collapse button so
                // the sidebar can be compressed to the icon rail.
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "VIVI Music",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.weight(1f))
                    Tooltip("Collapse sidebar") {
                        IconButton(onClick = onToggleCollapsed) {
                            Icon(
                                Icons.AutoMirrored.Filled.MenuOpen,
                                contentDescription = "Collapse sidebar",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            // Scrollable Content Region
            val scrollState = rememberScrollState()
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
            ) {
                // Top Main Group Header (Collapsible "VIVI Music")
                if (!collapsed) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { mainExpanded = !mainExpanded }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "VIVI Music",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Expand VIVI Music",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(mainChevronRotation),
                        )
                    }
                }

                if (mainExpanded || collapsed) {
                    mainEntries.forEach { entry ->
                        val selected = current == entry.screen
                        val interaction = remember(entry.screen) { MutableInteractionSource() }
                        val hovered by interaction.collectIsHoveredAsState()
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(mainRadius))
                                .background(
                                    when {
                                        selected -> selectedBg
                                        spotify && hovered -> MaterialTheme.colorScheme.surfaceContainerHigh
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable(
                                    interactionSource = interaction,
                                    indication = if (spotify) null else LocalIndication.current,
                                ) { onSelect(entry.screen) }
                                .padding(horizontal = if (collapsed) 0.dp else 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = if (collapsed) Arrangement.Center else Arrangement.Start,
                        ) {
                            Icon(
                                if (selected) entry.selectedIcon else entry.icon,
                                contentDescription = Localization.get(language, entry.key),
                                tint = if (selected) selectedFg
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (!collapsed) {
                                Spacer(Modifier.width(14.dp))
                                Text(
                                    Localization.get(language, entry.key),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (selected) selectedFg
                                    else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Library Section Header (Collapsible)
                if (!collapsed) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { libraryExpanded = !libraryExpanded }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = Localization.get(language, "library"),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Expand Library",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(libraryChevronRotation),
                        )
                    }
                }

                if (libraryExpanded || collapsed) {
                    librarySubEntries.forEach { entry ->
                        val selected = current == entry.screen
                        val interaction = remember(entry.screen) { MutableInteractionSource() }
                        val hovered by interaction.collectIsHoveredAsState()
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(subRadius))
                                .background(
                                    when {
                                        selected -> selectedBg
                                        spotify && hovered -> MaterialTheme.colorScheme.surfaceContainerHigh
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable(
                                    interactionSource = interaction,
                                    indication = if (spotify) null else LocalIndication.current,
                                ) { onSelect(entry.screen) }
                                .padding(horizontal = if (collapsed) 0.dp else 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = if (collapsed) Arrangement.Center else Arrangement.Start,
                        ) {
                            Icon(
                                if (selected) entry.selectedIcon else entry.icon,
                                contentDescription = Localization.get(language, entry.key),
                                tint = if (selected) selectedFg
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                            if (!collapsed) {
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    Localization.get(language, entry.key),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (selected) selectedFg
                                    else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Playlists Section Header (Collapsible)
                if (!collapsed) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { playlistsExpanded = !playlistsExpanded }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = Localization.get(language, "playlists"),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Expand Playlists",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(playlistsChevronRotation),
                        )
                    }
                }

                if (playlistsExpanded || collapsed) {
                    // Create New Playlist
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showCreateDialog = true }
                            .padding(horizontal = if (collapsed) 0.dp else 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = if (collapsed) Arrangement.Center else Arrangement.Start,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Create New",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        if (!collapsed) {
                            Spacer(Modifier.width(12.dp))
                            Text(
                                Localization.get(language, "new_playlist").let { if (it == "new_playlist" || it.isBlank()) "Create New..." else it },
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    // Active Local Playlists
                    activeLocalPlaylists.forEach { p ->
                        val selected = current == Screen.LocalPlaylist(p.id)
                        val interaction = remember(p.id) { MutableInteractionSource() }
                        val hovered by interaction.collectIsHoveredAsState()
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(subRadius))
                                .background(
                                    when {
                                        selected -> selectedBg
                                        spotify && hovered -> MaterialTheme.colorScheme.surfaceContainerHigh
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable(
                                    interactionSource = interaction,
                                    indication = if (spotify) null else LocalIndication.current,
                                ) { onSelect(Screen.LocalPlaylist(p.id)) }
                                .padding(horizontal = if (collapsed) 0.dp else 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = if (collapsed) Arrangement.Center else Arrangement.Start,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlaylistPlay,
                                contentDescription = p.name,
                                tint = if (selected) selectedFg
                                else if (spotify) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            if (!collapsed) {
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = p.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (selected) selectedFg else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                    }

                    // Logged-in YouTube Playlists
                    if (isLoggedIn) {
                        onlinePlaylists.forEach { op ->
                            val selected = current == Screen.Playlist(op.id)
                            val interaction = remember(op.id) { MutableInteractionSource() }
                            val hovered by interaction.collectIsHoveredAsState()
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(subRadius))
                                    .background(
                                        when {
                                            selected -> selectedBg
                                            spotify && hovered -> MaterialTheme.colorScheme.surfaceContainerHigh
                                            else -> Color.Transparent
                                        }
                                    )
                                    .clickable(
                                        interactionSource = interaction,
                                        indication = if (spotify) null else LocalIndication.current,
                                    ) { onSelect(Screen.Playlist(op.id)) }
                                    .padding(horizontal = if (collapsed) 0.dp else 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = if (collapsed) Arrangement.Center else Arrangement.Start,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MusicNote,
                                    contentDescription = op.title,
                                    tint = if (selected) selectedFg
                                    else if (spotify) MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp),
                                )
                                if (!collapsed) {
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = op.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (selected) selectedFg else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                        }
                    }

                    // Fallback Favourite Songs entry if no playlists exist yet
                    if (activeLocalPlaylists.isEmpty() && onlinePlaylists.isEmpty()) {
                        val favSelected = current == Screen.LocalPlaylists
                        val interaction = remember { MutableInteractionSource() }
                        val hovered by interaction.collectIsHoveredAsState()
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(subRadius))
                                .background(
                                    when {
                                        favSelected -> selectedBg
                                        spotify && hovered -> MaterialTheme.colorScheme.surfaceContainerHigh
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable(
                                    interactionSource = interaction,
                                    indication = if (spotify) null else LocalIndication.current,
                                ) { onSelect(Screen.LocalPlaylists) }
                                .padding(horizontal = if (collapsed) 0.dp else 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = if (collapsed) Arrangement.Center else Arrangement.Start,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "Favourite Songs",
                                tint = if (favSelected) selectedFg
                                else if (spotify) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            if (!collapsed) {
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "Favourite Songs",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (favSelected) selectedFg else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }

            if (showCreateDialog) {
                PlaylistNameDialog(
                    language = language,
                    initialName = "",
                    confirmLabel = Localization.get(language, "create").let { if (it == "create" || it.isBlank()) "Create" else it },
                    onConfirm = { name ->
                        val newPlaylist = PlaylistStore.create(name)
                        if (isLoggedIn) {
                            scope.launch {
                                try {
                                    com.music.innertube.YouTube.createPlaylist(name)
                                    com.music.innertube.YouTube.library("FEmusic_liked_playlists").getOrNull()?.let { page ->
                                        onlinePlaylists = page.items.filterIsInstance<com.music.innertube.models.PlaylistItem>()
                                    }
                                } catch (_: Exception) {}
                            }
                        }
                        showCreateDialog = false
                        onSelect(Screen.LocalPlaylist(newPlaylist.id))
                    },
                    onDismiss = { showCreateDialog = false },
                )
            }

            Spacer(Modifier.height(8.dp))

            // User Account Row (Pinned Bottom Active-Pill)
            val settingsSelected = current == Screen.Settings
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelect(Screen.Settings) },
                color = if (settingsSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (settingsSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = userName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (settingsSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary,
                        )
                    }

                    if (!collapsed) {
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = userName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (settingsSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = userHandle,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (settingsSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WindowScope.SpotifyTopHeader(
    language: String,
    canGoBack: Boolean,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenHome: () -> Unit,
    onOpenHistory: () -> Unit = {},
    onOpenStats: () -> Unit = {},
    onOpenListenTogether: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLyrics: () -> Unit = {},
    onOpenQueue: () -> Unit = {},
    onMinimize: () -> Unit,
    onMaximize: () -> Unit,
    onClose: () -> Unit,
    isMaximized: Boolean = false,
    showWindowControls: Boolean = true,
    sidebarCollapsed: Boolean = false,
    onToggleSidebar: () -> Unit = {},
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onSearchSubmit: (String) -> Unit = {},
    selectedFilter: YouTube.SearchFilter? = null,
    onFilterSelect: (YouTube.SearchFilter?) -> Unit = {},
) {
    WindowDraggableArea {
        Row(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Tooltip("Menu") {
                    IconButton(
                        onClick = { /* overflow menu */ },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.Filled.MoreHoriz,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Tooltip(Localization.get(language, "back")) {
                    IconButton(
                        onClick = onBack,
                        enabled = canGoBack,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = Localization.get(language, "back"),
                            tint = if (canGoBack) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Tooltip("Forward") {
                    IconButton(
                        onClick = { /* forward */ },
                        enabled = false,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Forward",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Tooltip("Toggle sidebar") {
                    IconButton(
                        onClick = onToggleSidebar,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.Filled.ViewColumn,
                            contentDescription = "Toggle sidebar",
                            tint = if (sidebarCollapsed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.weight(1f, fill = false).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Tooltip(Localization.get(language, "home")) {
                    IconButton(
                        onClick = onOpenHome,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            Icons.Filled.Home,
                            contentDescription = Localization.get(language, "home"),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Surface(
                    onClick = onOpenSearch,
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.widthIn(min = 140.dp, max = 420.dp).weight(1f, fill = false).height(42.dp),
                ) {
                    Row(
                        Modifier.padding(start = 14.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = Localization.get(language, "search"),
                            tint = if (searchQuery.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (searchQuery.isEmpty()) {
                                val hintText = Localization.get(language, "search_hint").let {
                                    if (it.isBlank() || it == "search_hint") "What do you want to play?" else it
                                }
                                Text(
                                    text = hintText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { newQuery ->
                                    onSearchQueryChange(newQuery)
                                    onOpenSearch()
                                },
                                modifier = Modifier.fillMaxWidth().onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        onOpenSearch()
                                    }
                                },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = {
                                    if (searchQuery.trim().isNotEmpty()) {
                                        onSearchSubmit(searchQuery.trim())
                                    }
                                }),
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            Tooltip("Clear") {
                                IconButton(
                                    onClick = { onSearchQueryChange("") },
                                    modifier = Modifier.size(24.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.width(4.dp))
                        }

                        // Divider between input and filter dropdown
                        Box(
                            Modifier
                                .width(1.dp)
                                .height(18.dp)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                        )
                        Spacer(Modifier.width(4.dp))

                        // Right-aligned filter dropdown button
                        Box {
                            var filterMenuExpanded by remember { mutableStateOf(false) }
                            val currentFilterLabel = when (selectedFilter) {
                                YouTube.SearchFilter.FILTER_SONG -> Localization.get(language, "filter_songs")
                                YouTube.SearchFilter.FILTER_VIDEO -> Localization.get(language, "filter_videos")
                                YouTube.SearchFilter.FILTER_ALBUM -> Localization.get(language, "filter_albums")
                                YouTube.SearchFilter.FILTER_ARTIST -> Localization.get(language, "filter_artists")
                                YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST -> Localization.get(language, "filter_playlists")
                                else -> Localization.get(language, "filter_all")
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { filterMenuExpanded = true }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = currentFilterLabel,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (selectedFilter != null) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (selectedFilter != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                )
                                Spacer(Modifier.width(2.dp))
                                Icon(
                                    Icons.Filled.ArrowDropDown,
                                    contentDescription = "Filter",
                                    tint = if (selectedFilter != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp),
                                )
                            }

                            DropdownMenu(
                                expanded = filterMenuExpanded,
                                onDismissRequest = { filterMenuExpanded = false },
                                modifier = Modifier
                                    .width(180.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                                    .padding(vertical = 4.dp),
                            ) {
                                data class FilterOption(
                                    val filter: YouTube.SearchFilter?,
                                    val label: String,
                                    val icon: ImageVector
                                )
                                val filters = listOf(
                                    FilterOption(null, Localization.get(language, "filter_all"), Icons.Filled.Tune),
                                    FilterOption(YouTube.SearchFilter.FILTER_SONG, Localization.get(language, "filter_songs"), Icons.Filled.MusicNote),
                                    FilterOption(YouTube.SearchFilter.FILTER_VIDEO, Localization.get(language, "filter_videos"), Icons.Filled.PlayCircle),
                                    FilterOption(YouTube.SearchFilter.FILTER_ALBUM, Localization.get(language, "filter_albums"), Icons.Filled.Album),
                                    FilterOption(YouTube.SearchFilter.FILTER_ARTIST, Localization.get(language, "filter_artists"), Icons.Filled.Person),
                                    FilterOption(YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST, Localization.get(language, "filter_playlists"), Icons.Filled.PlaylistPlay),
                                )
                                filters.forEach { opt ->
                                    val isSelected = selectedFilter == opt.filter
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.fillMaxWidth(),
                                            ) {
                                                Icon(
                                                    opt.icon,
                                                    contentDescription = null,
                                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                                Text(
                                                    text = opt.label,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    ),
                                                    modifier = Modifier.weight(1f),
                                                )
                                                if (isSelected) {
                                                    Icon(
                                                        Icons.Filled.Check,
                                                        contentDescription = "Selected",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp),
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            onFilterSelect(opt.filter)
                                            filterMenuExpanded = false
                                            onOpenSearch()
                                        },
                                        modifier = Modifier
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                                else Color.Transparent
                                            ),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Tooltip("Output device") {
                    IconButton(
                        onClick = { /* output device */ },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.Filled.SpeakerGroup,
                            contentDescription = "Output device",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Tooltip(Localization.get(language, "lyrics")) {
                    IconButton(
                        onClick = onOpenLyrics,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Subject,
                            contentDescription = Localization.get(language, "lyrics"),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Tooltip(Localization.get(language, "queue")) {
                    IconButton(
                        onClick = onOpenQueue,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = Localization.get(language, "queue"),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Tooltip(Localization.get(language, "history")) {
                    IconButton(
                        onClick = onOpenHistory,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.Filled.History,
                            contentDescription = Localization.get(language, "history"),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Tooltip(Localization.get(language, "wrapped_title")) {
                    IconButton(
                        onClick = onOpenStats,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.Filled.Leaderboard,
                            contentDescription = Localization.get(language, "wrapped_title"),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Tooltip(Localization.get(language, "listen_together_title")) {
                    IconButton(
                        onClick = onOpenListenTogether,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.Filled.Group,
                            contentDescription = Localization.get(language, "listen_together_title"),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Tooltip(Localization.get(language, "settings")) {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = Localization.get(language, "settings"),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Spacer(Modifier.width(4.dp))
                if (showWindowControls) {
                    WindowControls(
                        isMaximized = isMaximized,
                        onMinimize = onMinimize,
                        onMaximize = onMaximize,
                        onClose = onClose,
                    )
                }
            }
        }
    }
}


/** Minimize / maximize / close buttons. These must be visible at all times
 * because the window is undecorated (no OS title bar): they are shown in the
 * Spotify header when it is visible and overlaid at the top-right otherwise. */
@Composable
private fun WindowControls(
    isMaximized: Boolean,
    onMinimize: () -> Unit,
    onMaximize: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tooltip("Minimize") {
            IconButton(
                onClick = onMinimize,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    Icons.Filled.Remove,
                    contentDescription = "Minimize",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Tooltip(if (isMaximized) "Restore" else "Maximize") {
            IconButton(
                onClick = onMaximize,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    if (isMaximized) Icons.Filled.FilterNone else Icons.Filled.CropSquare,
                    contentDescription = if (isMaximized) "Restore" else "Maximize",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    modifier = Modifier.size(if (isMaximized) 13.dp else 14.dp),
                )
            }
        }
        Tooltip("Close") {
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}


@Composable
fun MiniPlayer(
    nowPlaying: NowPlaying?,
    isPlaying: Boolean,
    isLoading: Boolean,
    positionMs: Long,
    durationMs: Long,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onOpen: () -> Unit,
    onOpenQueue: () -> Unit,
    style: MiniPlayerStyle = MiniPlayerStyle.STANDARD,
) {
    val np = nowPlaying ?: return
    val pureBlack = style == MiniPlayerStyle.PURE_BLACK

    // Swipe-to-expand: dragging the mini player up reveals the full player.
    var dragOffset by remember { mutableStateOf(0f) }
    val openThreshold = 90.dp
    val density = LocalDensity.current
    val thresholdPx = with(density) { openThreshold.toPx() }
    Box(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = -dragOffset.coerceAtLeast(0f) }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        dragOffset = (dragOffset + dragAmount).coerceAtLeast(0f)
                    },
                    onDragEnd = {
                        if (dragOffset > thresholdPx) onOpen()
                        dragOffset = 0f
                    },
                    onDragCancel = { dragOffset = 0f },
                )
            },
    ) {
        Surface(
            color = when {
                pureBlack -> Color.Black
                style == MiniPlayerStyle.OUTLINE -> MaterialTheme.colorScheme.surface
                else -> MaterialTheme.colorScheme.surfaceContainerHigh
            },
            tonalElevation = if (style == MiniPlayerStyle.OUTLINE) 0.dp else 4.dp,
            shadowElevation = 4.dp,
            border = if (style == MiniPlayerStyle.OUTLINE) {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            } else null,
            shape = when (style) {
                MiniPlayerStyle.APPLE -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                MiniPlayerStyle.OUTLINE -> RoundedCornerShape(14.dp)
                else -> RoundedCornerShape(0.dp)
            },
        ) {
            Column {
                // Drag handle (hint that the bar can be swiped up to expand).
                Box(
                    Modifier.fillMaxWidth().padding(top = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .width(36.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (pureBlack) Color.White.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant),
                    )
                }
                if (durationMs > 0) {
                    LinearProgressIndicator(
                        progress = { (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(if (style == MiniPlayerStyle.APPLE) 3.dp else 2.dp),
                        color = if (pureBlack) Color.White else MaterialTheme.colorScheme.primary,
                    )
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpen)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (style == MiniPlayerStyle.APPLE) {
                        Box(Modifier.clip(RoundedCornerShape(10.dp))) {
                            Thumbnail(np.thumbnail, Modifier.size(40.dp))
                        }
                    } else {
                        Thumbnail(np.thumbnail, Modifier.size(44.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            np.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            np.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (pureBlack) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Tooltip(if (isPlaying) "Pause" else "Play") {
                        IconButton(onClick = onTogglePlay) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = if (pureBlack) Color.White else MaterialTheme.colorScheme.primary,
                                )
                            } else {
                                Icon(
                                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = if (pureBlack) Color.White else LocalContentColor.current,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    }
                    Tooltip("Next") {
                        IconButton(onClick = onNext) {
                            Icon(
                                Icons.Filled.SkipNext,
                                contentDescription = "Next",
                                tint = if (pureBlack) Color.White else LocalContentColor.current,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                    Tooltip("Queue") {
                        IconButton(onClick = onOpenQueue) {
                            Icon(
                                Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = "Queue",
                                tint = if (pureBlack) Color.White else LocalContentColor.current,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class SettingsRowSpec(
    val title: String,
    val subtitle: String?,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@Composable
fun SettingsScreen(
    language: String,
    themeMode: ThemeMode,
    isLoggedIn: Boolean,
    accountName: String,
    updateStatus: UpdateStatus,
    wrappedStats: WrappedStats = WrappedStats(),
    onOpen: (Screen) -> Unit,
) {
    val devEnabled by DeveloperOptions.enabled.collectAsState()
    var searchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val themeLabel = when (themeMode) {
        ThemeMode.SYSTEM -> Localization.get(language, "theme_system")
        ThemeMode.LIGHT -> Localization.get(language, "theme_light")
        ThemeMode.DARK -> Localization.get(language, "theme_dark")
    }
    val rows = listOf(
        SettingsRowSpec(Localization.get(language, "language"), Languages.name(language), Icons.Filled.Translate) { onOpen(Screen.SettingsLanguage) },
        SettingsRowSpec(Localization.get(language, "updates"), if (updateStatus is UpdateStatus.Available) Localization.get(language, "update_available") else AppInfo.FULL_VERSION, Icons.Filled.Refresh) { onOpen(Screen.SettingsUpdates) },
        SettingsRowSpec(Localization.get(language, "notifications"), Localization.get(language, if (DesktopSettings.load().notificationMode == "native") "notification_native" else "notification_main_window"), Icons.Filled.Notifications) { onOpen(Screen.SettingsNotifications) },
        SettingsRowSpec(Localization.get(language, "appearance"), themeLabel, Icons.Filled.Palette) { onOpen(Screen.SettingsAppearance) },
        SettingsRowSpec(Localization.get(language, "player_audio"), null, Icons.Filled.GraphicEq) { onOpen(Screen.SettingsPlayer) },
        SettingsRowSpec(Localization.get(language, "account"), if (isLoggedIn) accountName.ifBlank { "YouTube" } else Localization.get(language, "not_logged_in"), Icons.Filled.Person) { onOpen(Screen.SettingsAccount) },
        SettingsRowSpec(Localization.get(language, "device_sync"), null, Icons.Filled.Devices) { onOpen(Screen.SettingsDevices) },
        SettingsRowSpec(Localization.get(language, "content"), null, Icons.Filled.Language) { onOpen(Screen.SettingsContent) },
        SettingsRowSpec(Localization.get(language, "lyrics"), null, Icons.Filled.Lyrics) { onOpen(Screen.SettingsLyrics) },
        SettingsRowSpec(Localization.get(language, "privacy"), null, Icons.Filled.Security) { onOpen(Screen.SettingsPrivacy) },
        SettingsRowSpec(Localization.get(language, "storage"), null, Icons.Filled.Storage) { onOpen(Screen.SettingsStorage) },
        SettingsRowSpec(Localization.get(language, "wrapped_title"), "${wrappedStats.trackStarts} ${Localization.get(language, "wrapped_tracks")}", Icons.Filled.AutoAwesome) { onOpen(Screen.SettingsWrapped) },
        SettingsRowSpec(Localization.get(language, "integrations"), if (DesktopSettings.load().discordRpcEnabled || DesktopSettings.load().lastfmEnabled) Localization.get(language, "integrations_active") else Localization.get(language, "integrations_inactive"), Icons.Filled.Tune) { onOpen(Screen.SettingsIntegrations) },
        SettingsRowSpec(Localization.get(language, "backup_restore"), null, Icons.Filled.SettingsBackupRestore) { onOpen(Screen.SettingsBackup) },
        SettingsRowSpec(Localization.get(language, "desktop_features"), null, Icons.Filled.DesktopWindows) { onOpen(Screen.SettingsDesktop) },
        SettingsRowSpec(Localization.get(language, "system"), if (devEnabled) Localization.get(language, "developer_options_enabled") else Localization.get(language, "dev_tools_disabled"), Icons.Filled.Build) { onOpen(Screen.SettingsSystem) },
        SettingsRowSpec(Localization.get(language, "about"), null, Icons.Filled.Info) { onOpen(Screen.SettingsAbout) },
    )

    val query = searchQuery.trim()
    val visible = rows.filter { row ->
        query.isEmpty() ||
            row.title.contains(query, ignoreCase = true) ||
            (row.subtitle?.contains(query, ignoreCase = true) == true)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(Localization.get(language, "settings"), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.width(16.dp))
            if (searchActive) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text(Localization.get(language, "search")) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        Tooltip(Localization.get(language, "dismiss")) {
                            IconButton(onClick = { searchActive = false; searchQuery = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = Localization.get(language, "dismiss"))
                            }
                        }
                    },
                )
            } else {
                Tooltip(Localization.get(language, "search")) {
                    IconButton(onClick = { searchActive = true }) {
                        Icon(Icons.Filled.Search, contentDescription = Localization.get(language, "search"))
                    }
                }
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 12.dp))

        visible.forEach { row ->
            SettingsEntryRow(
                language = language,
                icon = row.icon,
                title = row.title,
                subtitle = row.subtitle,
                onClick = row.onClick,
            )
        }
        if (query.isNotEmpty() && visible.isEmpty()) {
            Text(
                Localization.get(language, "no_results_found"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp),
            )
        }
    }
}

@Composable
fun SettingsSystemScreen(
    language: String,
    onBack: () -> Unit,
    showIntroSplash: Boolean,
    onOpenDeveloper: () -> Unit,
    onOpenIntro: () -> Unit,
) {
    val devEnabled by DeveloperOptions.enabled.collectAsState()
    SettingsSubScreen(language, onBack) {
        Text(Localization.get(language, "system"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 12.dp))
        SettingsEntryRow(
            language = language,
            icon = Icons.Filled.Build,
            title = Localization.get(language, "developer_options"),
            subtitle = if (devEnabled) Localization.get(language, "developer_options_enabled") else Localization.get(language, "dev_tools_disabled"),
            onClick = onOpenDeveloper,
        )
        SettingsEntryRow(
            language = language,
            icon = Icons.Filled.Movie,
            title = Localization.get(language, "intro"),
            subtitle = if (showIntroSplash) Localization.get(language, "integrations_active") else Localization.get(language, "integrations_inactive"),
            onClick = onOpenIntro,
        )
    }
}

@Composable
private fun SettingsEntryRow(
    language: String,
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun DeviceSyncSection(
    language: String,
    syncManager: DesktopSyncManager,
    syncViviVolume: Boolean,
    onToggleSyncViviVolume: (Boolean) -> Unit,
) {
    var serverUrl by remember {
        val saved = DesktopSettings.load().serverUrl
        // Default to the same relay the Android app uses; treat the old
        // hardcoded localhost placeholder as "not set" so it gets migrated.
        mutableStateOf(if (saved.isBlank() || saved == "wss://localhost:8080") SyncServer.DEFAULT_URL else saved)
    }
    val connectionState by syncManager.connectionState.collectAsState()
    val status by syncManager.status.collectAsState()
    val pairCode by syncManager.pairCode.collectAsState()
    val pairCodeExpiresAt by syncManager.pairCodeExpiresAt.collectAsState()
    val paired by syncManager.paired.collectAsState()
    val peerDeviceName by syncManager.peerDeviceName.collectAsState()
    val lanRunning by syncManager.lanRunning.collectAsState()
    val lanAddress by syncManager.lanAddress.collectAsState()
    val syncedSettings by syncManager.syncedSettings.collectAsState()

    // Ticking clock for the pairing-code expiry countdown.
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(pairCodeExpiresAt) {
        while (pairCodeExpiresAt > 0L) {
            nowMs = System.currentTimeMillis()
            if (nowMs >= pairCodeExpiresAt) break
            delay(1000)
        }
        nowMs = System.currentTimeMillis()
    }
    val remainingMs = (pairCodeExpiresAt - nowMs).coerceAtLeast(0L)

    // The QR code carries both the LAN relay address and the current 6-digit
    // pairing code (when available), so the phone can auto-fill the code and
    // the user only has to verify it before tapping Pair.
    val qrContent = if (lanAddress.isNotEmpty() && pairCode.isNotEmpty()) {
        "vivimusic://pair?addr=${URLEncoder.encode(lanAddress, "UTF-8")}&code=$pairCode"
    } else {
        lanAddress
    }

    Text(Localization.get(language, "device_sync"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))

    DeviceSyncHowTo(language)

    // Download link for the mobile version of VIVI (the Android APK), so the
    // phone runs the matching build before pairing. The APK URL is fetched
    // from the latest GitHub release (follows the selected update source); if
    // no APK is found, the releases page opens instead.
    var openingDownload by remember { mutableStateOf(false) }
    var downloadFailed by remember { mutableStateOf(false) }
    val syncScope = rememberCoroutineScope()
    OutlinedButton(
        onClick = {
            syncScope.launch {
                openingDownload = true
                downloadFailed = false
                val asset = withContext(Dispatchers.IO) { UpdateChecker.latestApkAsset() }
                val opened = if (asset != null && asset.browserDownloadUrl.isNotBlank()) {
                    openUrl(asset.browserDownloadUrl)
                } else {
                    openUrl("https://github.com/${UpdateSource.repo()}/releases")
                }
                openingDownload = false
                if (!opened) downloadFailed = true
            }
        },
        enabled = !openingDownload,
        modifier = Modifier.padding(top = 8.dp),
    ) {
        Icon(
            Icons.Filled.Download,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(Localization.get(language, if (openingDownload) "opening_download" else "download_mobile_apk"))
    }
    if (downloadFailed) {
        Text(
            Localization.get(language, "open_failed"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 4.dp),
        )
    }

    SettingSwitch(
        language = language,
        key = "sync_vivi_volume",
        checked = syncViviVolume,
        onCheckedChange = onToggleSyncViviVolume,
    )

    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = serverUrl,
            onValueChange = { serverUrl = it },
            modifier = Modifier.weight(1f),
            singleLine = true,
            label = { Text(Localization.get(language, "relay_server")) },
        )
        Button(onClick = { syncManager.connect(serverUrl) }) { Text(Localization.get(language, "connect")) }
    }

    Text(Localization.get(language, "lan_sync"), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
    Button(
        onClick = { if (lanRunning) syncManager.stopLan() else syncManager.startLan() },
        modifier = Modifier.padding(top = 4.dp),
    ) {
        Text(Localization.get(language, if (lanRunning) "stop_lan" else "start_lan"))
    }
    if (lanRunning && lanAddress.isNotEmpty()) {
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                Modifier.widthIn(max = 200.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                QrCode(qrContent, size = 180.dp)
                Text(
                    "${Localization.get(language, "lan_address")}: $lanAddress",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    Localization.get(language, "scan_qr"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            // Pairing code + generate button sit to the right of the QR code.
            PairingCodePanel(
                language = language,
                pairCode = pairCode,
                remainingMs = remainingMs,
                onGenerate = { syncManager.requestPairingCode() },
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            Localization.get(language, "lan_hint"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    } else {
        // When the LAN server is off, the code can still be generated against
        // the relay configured in the field above.
        PairingCodePanel(
            language = language,
            pairCode = pairCode,
            remainingMs = remainingMs,
            onGenerate = { syncManager.requestPairingCode() },
            modifier = Modifier.padding(top = 8.dp),
        )
    }

    if (paired) {
        Button(onClick = { syncManager.unpair() }, modifier = Modifier.padding(top = 8.dp)) {
            Text(Localization.get(language, "unpair"))
        }
    }

    if (paired && peerDeviceName.isNotBlank()) {
        Text(
            "${Localization.get(language, "paired_device")}: $peerDeviceName",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
    }

    Text("${Localization.get(language, "status")}: $connectionState — $status", modifier = Modifier.padding(top = 8.dp))

    if (syncedSettings.isNotEmpty()) {
        Text(Localization.get(language, "synced_settings"), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
        Column(Modifier.padding(top = 4.dp)) {
            syncedSettings.entries.sortedBy { it.key }.forEach { (k, v) ->
                Text("$k = $v", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 1.dp))
            }
        }
    }
}

@Composable
private fun DeviceSyncHowTo(language: String) {
    Surface(
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                Localization.get(language, "how_to_connect"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            HowToStep(1, Localization.get(language, "how_to_step1"))
            HowToStep(2, Localization.get(language, "how_to_step2"))
            HowToStep(3, Localization.get(language, "how_to_step3"))
            HowToStep(4, Localization.get(language, "how_to_step4"))
        }
    }
}

@Composable
private fun HowToStep(number: Int, text: String) {
    Row(Modifier.padding(vertical = 4.dp)) {
        Text(
            number.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.widthIn(min = 20.dp),
        )
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun PairingCodePanel(
    language: String,
    pairCode: String,
    remainingMs: Long,
    onGenerate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        // The desktop is the code generator: the phone only enters this code.
        Button(onClick = onGenerate) {
            Text(Localization.get(language, if (pairCode.isNotEmpty()) "generate_new_code" else "generate_code"))
        }
        if (pairCode.isNotEmpty()) {
            // Selectable so the user can copy the code if the QR scan fails.
            SelectionContainer {
                Text(
                    "${Localization.get(language, "code_hint")}: $pairCode",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Text(
                if (remainingMs > 0L) {
                    "${Localization.get(language, "code_expires_in")} ${formatCountdown(remainingMs)}"
                } else {
                    Localization.get(language, "code_expired")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
fun AccountSection(
    language: String,
    isLoggedIn: Boolean,
    accountName: String,
    onOpenLogin: () -> Unit,
    onLogout: () -> Unit,
    onLoggedIn: () -> Unit,
) {
    Text(Localization.get(language, "account"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
    if (isLoggedIn) {
        Text(
            "${Localization.get(language, "logged_in_as")}: ${accountName.ifBlank { "YouTube" }}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        Button(onClick = onLogout, modifier = Modifier.padding(top = 8.dp)) {
            Text(Localization.get(language, "logout"))
        }
    } else {
        // Show the sign-in options directly here instead of a "Log in" button
        // that opens a second screen: the user picks Google or manual cookies
        // without an extra navigation step.
        LoginContent(language = language, onLoggedIn = {
            onLoggedIn()
        })
    }
}

@Composable
fun LanguageSection(language: String, onLanguageChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Text(Localization.get(language, "language"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
    Text(
        Localization.get(language, "translation_ai_disclaimer"),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp),
    )

    Box(Modifier.padding(top = 8.dp)) {
        OutlinedButton(onClick = { expanded = true }) {
            Text(Languages.name(language))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Languages.all.forEach { lang ->
                DropdownMenuItem(
                    text = { Text(lang.name) },
                    onClick = {
                        expanded = false
                        onLanguageChange(lang.code)
                    },
                )
            }
        }
    }
}

@Composable
fun LanguageSelectionScreen(onSelect: (String) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text("Choose your language", style = MaterialTheme.typography.headlineMedium)
        Text(
            "VIVI Music DE is available in the following languages. You can change this later from the Language menu.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        Column(Modifier.padding(top = 16.dp)) {
            Languages.all.forEach { lang ->
                Text(
                    lang.name,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(lang.code) }
                        .padding(vertical = 8.dp),
                )
            }
        }
    }
}

/**
 * Generic non-invasive in-app notification banner (title + message + dismiss),
 * used for notifications dispatched in "main window" mode.
 */
@Composable
fun BoxScope.InAppNotification(
    title: String,
    message: String,
    language: String,
    onDismiss: () -> Unit,
) {
    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
    ) {
        Row(
            Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.widthIn(max = 300.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onDismiss, modifier = Modifier.padding(start = 12.dp)) {
                Text(Localization.get(language, "dismiss"))
            }
        }
    }
}

/**
 * One-off banner shown right after the developer options are unlocked,
 * pointing the user to the settings screen where they can configure them.
 */
@Composable
fun BoxScope.DevUnlockedNotification(
    language: String,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
    ) {
        Row(
            Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.widthIn(max = 300.dp)) {
                Text(
                    Localization.get(language, "dev_unlocked_title"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    Localization.get(language, "dev_unlocked_desc"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = onOpen, modifier = Modifier.padding(start = 12.dp)) {
                Text(Localization.get(language, "dev_unlocked_open"))
            }
            TextButton(onClick = onDismiss) {
                Text(Localization.get(language, "dismiss"))
            }
        }
    }
}

/**
 * Non-invasive banner shown when a newer desktop release is available, with
 * "Install now" (download + launch the installer) and a dismiss button.
 */
@Composable
fun BoxScope.UpdateNotification(
    status: UpdateStatus.Available,
    language: String,
    onDismiss: () -> Unit,
    onDone: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val progress by UpdateState.progress.collectAsState()
    val downloadedFile by UpdateState.downloadedFile.collectAsState()
    // Shared with the Updates screen: if the installer for this version is
    // already downloaded (from anywhere), offer to open it, not re-download.
    val existingInstaller = downloadedFile

    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(16.dp),
    ) {
        Row(
            Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.widthIn(max = 300.dp)) {
                Text(
                    Localization.get(language, "update_available"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    status.version,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                progress?.let { p ->
                    LinearProgressIndicator(
                        progress = { p.percent / 100f },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
            }
            Button(
                onClick = {
                    if (progress != null) return@Button
                    existingInstaller?.let { file ->
                        scope.launch {
                            prepareAndOpenInstaller(file)
                            onDone()
                        }
                        return@Button
                    }
                    val asset = status.asset
                    if (asset == null) {
                        openUrl(status.url)
                        onDone()
                    } else {
                        scope.launch {
                            val file = UpdateState.download(asset)
                            if (file != null) prepareAndOpenInstaller(file)
                            onDone()
                        }
                    }
                },
                enabled = progress == null,
                modifier = Modifier.padding(start = 12.dp),
            ) {
                Text(
                    Localization.get(
                        language,
                        when {
                            existingInstaller != null -> "open_installer"
                            progress != null -> "downloading"
                            else -> "install_now"
                        },
                    )
                )
            }
            Tooltip(Localization.get(language, "dismiss")) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = Localization.get(language, "dismiss"))
                }
            }
        }
    }
}

@Composable
fun UpdateSection(
    language: String,
    status: UpdateStatus,
    includePreReleases: Boolean,
    updateIntervalHours: Int,
    updateSource: String,
    onIntervalChange: (Int) -> Unit,
    onTogglePreReleases: (Boolean) -> Unit,
    onUpdateSourceChange: (String) -> Unit,
    onCheckUpdates: () -> Unit,
    onOpenChangelog: () -> Unit,
    onOpenCommits: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    // Shared download state (also used by the notification banner), so the two
    // surfaces stay in sync.
    val progress by UpdateState.progress.collectAsState()
    val downloadedFile by UpdateState.downloadedFile.collectAsState()
    val installerCount by UpdateState.installerCount.collectAsState()
    var openError by remember { mutableStateOf<String?>(null) }
    var intervalMenuOpen by remember { mutableStateOf(false) }
    var sourceMenuOpen by remember { mutableStateOf(false) }

    Text(Localization.get(language, "updates"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
    Text(
        "${Localization.get(language, "current_version")}: ${AppInfo.FULL_VERSION} (${Localization.get(language, "de")} ${AppInfo.DE_VERSION})",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 4.dp),
    )

    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onOpenChangelog) {
            Text(Localization.get(language, "changelog"))
        }
        OutlinedButton(onClick = onOpenCommits) {
            Text(Localization.get(language, "commits"))
        }
    }

    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onCheckUpdates, enabled = status != UpdateStatus.Checking) {
            Text(Localization.get(language, "check_updates"))
        }
        Switch(checked = includePreReleases, onCheckedChange = onTogglePreReleases)
        Text(Localization.get(language, "include_prereleases"))
    }

    // Update source (fork vs original repo).
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(Localization.get(language, "update_source"), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.weight(1f))
        Box {
            OutlinedButton(onClick = { sourceMenuOpen = true }) {
                Text(UpdateSource.repoFor(updateSource))
            }
            DropdownMenu(expanded = sourceMenuOpen, onDismissRequest = { sourceMenuOpen = false }) {
                listOf(UpdateSource.FORK, UpdateSource.ORIGINAL).forEach { source ->
                    DropdownMenuItem(
                        text = { Text(UpdateSource.repoFor(source)) },
                        onClick = {
                            sourceMenuOpen = false
                            onUpdateSourceChange(source)
                        },
                    )
                }
            }
        }
    }

    // Automatic check frequency.
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(Localization.get(language, "update_check_interval"), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.weight(1f))
        Box {
            OutlinedButton(onClick = { intervalMenuOpen = true }) {
                Text(intervalLabel(language, updateIntervalHours))
            }
            DropdownMenu(expanded = intervalMenuOpen, onDismissRequest = { intervalMenuOpen = false }) {
                updateCheckIntervalOptions().forEach { hours ->
                    DropdownMenuItem(
                        text = { Text(intervalLabel(language, hours)) },
                        onClick = {
                            // Dismiss the popup before mutating the state that
                            // changes this row's layout, or Compose throws
                            // "layouts are not part of the same hierarchy".
                            intervalMenuOpen = false
                            onIntervalChange(hours)
                        },
                    )
                }
            }
        }
    }

    when (status) {
        is UpdateStatus.Checking -> Text(Localization.get(language, "checking"), modifier = Modifier.padding(top = 8.dp))
        is UpdateStatus.UpToDate -> Text(Localization.get(language, "up_to_date"), modifier = Modifier.padding(top = 8.dp))
        is UpdateStatus.Available -> {
            Text("${Localization.get(language, "update_available")}: ${status.version}", modifier = Modifier.padding(top = 8.dp))
            val asset = status.asset
            when {
                asset == null -> {
                    Text(
                        Localization.get(language, "no_installer"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Button(
                        onClick = {
                            openError = null
                            if (!openUrl(status.url)) openError = Localization.get(language, "open_failed")
                        },
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Text(Localization.get(language, "open_release_page"))
                    }
                }
                downloadedFile != null -> {
                    Text(
                        "${Localization.get(language, "downloaded")}: ${downloadedFile!!.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Button(
                        onClick = {
                            openError = null
                            scope.launch {
                                if (!prepareAndOpenInstaller(downloadedFile!!)) {
                                    openError = Localization.get(language, "open_failed")
                                }
                            }
                        },
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Text(Localization.get(language, "open_installer"))
                    }
                }
                progress != null -> {
                    val p = progress!!
                    Text(
                        "${Localization.get(language, "downloading")}: ${p.percent}% · ${formatBytes(p.downloadedBytes)} / ${formatBytes(p.totalBytes)} · ${formatSpeed(p.speedBytesPerSecond)}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    LinearProgressIndicator(
                        progress = { p.percent / 100f },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
                else -> Button(
                    onClick = {
                        scope.launch { UpdateState.download(asset) }
                    },
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text("${Localization.get(language, "download")} (${formatBytes(asset.sizeBytes)})")
                }
            }
        }
        is UpdateStatus.Failed -> Text(
            "${Localization.get(language, "update_failed")}: ${status.message}",
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 8.dp),
        )
        is UpdateStatus.Idle -> Unit
    }

    openError?.let {
        Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
    }

    // Downloaded installer management.
    Text(
        "${Localization.get(language, "installers_downloaded")}: $installerCount",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 16.dp),
    )
    if (installerCount > 0) {
        Button(
            onClick = { UpdateState.deleteAllInstallers() },
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Text(Localization.get(language, "delete_installers"))
        }
    }
}

internal fun openUrl(url: String): Boolean {
    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
        if (runCatching { Desktop.getDesktop().browse(URI(url)) }.isSuccess) return true
    }
    return runCatching {
        val cmd = when (Platform.os) {
            DesktopOs.WINDOWS -> listOf("cmd", "/c", "start", "", url)
            DesktopOs.MACOS -> listOf("open", url)
            DesktopOs.LINUX -> listOf("xdg-open", url)
        }
        ProcessBuilder(cmd).start()
        true
    }.getOrDefault(false)
}

private fun openFile(file: File): Boolean {
    if (!file.exists()) return false
    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
        if (runCatching { Desktop.getDesktop().open(file) }.isSuccess) return true
    }
    return runCatching {
        val path = file.absolutePath
        val cmd = when (Platform.os) {
            DesktopOs.WINDOWS -> listOf("cmd", "/c", "start", "", path)
            DesktopOs.MACOS -> listOf("open", path)
            DesktopOs.LINUX -> listOf("xdg-open", path)
        }
        ProcessBuilder(cmd).start()
        true
    }.getOrDefault(false)
}

private fun formatSpeed(bps: Long): String =
    if (bps <= 0) "0 B/s" else "${formatBytes(bps)}/s"

/**
 * Runs the optional "backup before update" (if enabled) and then opens the
 * installer, exiting the app on success so the installer can replace the
 * running files. Returns false when the installer could not be opened. Shared
 * by the update notification and the Updates screen so both behave the same.
 */
private suspend fun prepareAndOpenInstaller(file: File): Boolean {
    val s = DesktopSettings.load()
    if (s.autoBackupEnabled && s.autoBackupBeforeUpdate) {
        withContext(Dispatchers.IO) { BackupManager.autoBackup("before_update") }
    }
    val ok = openFile(file)
    if (ok) exitProcess(0)
    return ok
}

/** Available update-check intervals, in hours (0 = manual only). */
private fun updateCheckIntervalOptions(): List<Int> = listOf(0, 6, 12, 24, 72, 168)

/** Localized label for an update-check interval. */
private fun intervalLabel(language: String, hours: Int): String = when (hours) {
    0 -> Localization.get(language, "interval_manual")
    6 -> Localization.get(language, "interval_6h")
    12 -> Localization.get(language, "interval_12h")
    24 -> Localization.get(language, "interval_24h")
    72 -> Localization.get(language, "interval_3d")
    168 -> Localization.get(language, "interval_7d")
    else -> "$hours h"
}

/** Formats a millisecond duration as `M:SS` for the pairing-code countdown. */
private fun formatCountdown(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}

/** GitHub mark (octocat) taken from the mobile app's drawable, so it tints
 *  with the accent color and adapts to dark/light mode like a normal icon. */
private val GithubIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Github",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        addPath(
            pathData = PathParser().parsePathString(GITHUB_MARK_PATH).toNodes(),
            fill = SolidColor(Color.White),
        )
    }.build()
}

private const val GITHUB_MARK_PATH =
    "M12,2A10,10 0,0 0,2 12c0,4.42 2.87,8.17 6.84,9.5c0.5,0.08 0.66,-0.23 0.66,-0.5c0,-0.23 0,-0.86 0,-1.69c-2.77,0.6 -3.36,-1.34 -3.36,-1.34c-0.46,-1.16 -1.11,-1.47 -1.11,-1.47c-0.91,-0.62 0.07,-0.6 0.07,-0.6c1,0.07 1.53,1.03 1.53,1.03c0.87,1.52 2.34,1.07 2.91,0.83c0.09,-0.65 0.35,-1.09 0.63,-1.34c-2.22,-0.25 -4.55,-1.11 -4.55,-4.92c0,-1.11 0.38,-2 1.03,-2.71c-0.1,-0.25 -0.45,-1.29 0.1,-2.64c0,0 0.84,-0.27 2.75,1.02c0.79,-0.22 1.65,-0.33 2.5,-0.33c0.85,0 1.71,0.11 2.5,0.33c1.91,-1.29 2.75,-1.02 2.75,-1.02c0.55,1.35 0.2,2.39 0.1,2.64c0.65,0.71 1.03,1.6 1.03,2.71c0,3.82 -2.34,4.66 -4.57,4.91c0.36,0.31 0.69,0.92 0.69,1.85c0,1.34 0,2.42 0,2.74c0,0.27 0.16,0.59 0.67,0.5C19.14,20.16 22,16.42 22,12A10,10 0,0 0,12 2Z"

@Composable
fun AboutSection(language: String) {
    val firstLaunchDate = remember { DesktopSettings.load().firstLaunchDate }
    var versionCodeTaps by remember { mutableStateOf(0) }
    val devEnabled by DeveloperOptions.enabled.collectAsState()
    val authorImage = remember { loadResourceImage("author.png") }

    Column(
        Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "VIVI MUSIC DE",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(12.dp))
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        ) {
            Text(
                "v${AppInfo.FULL_VERSION} • ${AppInfo.CHANNEL.uppercase()}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }

    AboutSectionHeader(Localization.get(language, "developer_section"))
    AboutInfoRow(
        image = authorImage,
        title = "PiBOH",
        description = Localization.get(language, "app_developer") + " (DE)",
        onClick = { openUrl("https://github.com/PiBOH") },
    )
    AboutInfoRow(
        icon = Icons.Filled.Public,
        title = Localization.get(language, "website"),
        onClick = { openUrl("https://piboh.github.io/vivi-music/") },
    )

    AboutSectionHeader(Localization.get(language, "community_section"))
    AboutInfoRow(
        icon = GithubIcon,
        title = Localization.get(language, "github_repository"),
        onClick = { openUrl("https://github.com/PiBOH/vivi-music") },
    )
    AboutInfoRow(
        icon = Icons.Filled.Send,
        title = Localization.get(language, "telegram_channel"),
        onClick = { openUrl("https://t.me/vivimusicde") },
    )

    AboutSectionHeader(Localization.get(language, "app_info_section"))
    AboutInfoRow(
        icon = Icons.Filled.DateRange,
        title = Localization.get(language, "installed_date_title"),
        description = formatInstalledDate(firstLaunchDate, language),
    )
    AboutInfoRow(
        icon = Icons.Filled.Info,
        title = Localization.get(language, "version_code"),
        description = AppInfo.VERSION_CODE.toString(),
        onClick = {
            if (!devEnabled) {
                versionCodeTaps++
                if (versionCodeTaps >= 7) {
                    DeveloperOptions.setEnabled(true)
                    versionCodeTaps = 0
                }
            }
        },
    )
    Text(
        if (devEnabled) {
            Localization.get(language, "developer_options_enabled")
        } else if (versionCodeTaps > 0) {
            Localization.get(language, "tap_version_code_hint") + " (${7 - versionCodeTaps})"
        } else {
            ""
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 4.dp),
    )
    AboutInfoRow(
        icon = Icons.Filled.Description,
        title = Localization.get(language, "license"),
        onClick = { openUrl("https://github.com/PiBOH/vivi-music/blob/main/LICENSE") },
    )
}

@Composable
private fun AboutSectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 24.dp, bottom = 4.dp),
    )
}

@Composable
private fun AboutInfoRow(
    icon: ImageVector? = null,
    image: ImageBitmap? = null,
    title: String,
    description: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val base = Modifier.fillMaxWidth()
    val rowModifier = if (onClick != null) base.clickable(onClick = onClick) else base
    Row(
        rowModifier.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            image != null -> Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(28.dp).clip(CircleShape),
            )
            icon != null -> Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (description != null) {
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (onClick != null) {
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Loads a bundled classpath image from `desktop/src/main/resources/images`. */
private fun loadResourceImage(name: String): ImageBitmap? = runCatching {
    val stream = AppInfo::class.java.getResourceAsStream("/images/$name") ?: return null
    stream.use { s -> javax.imageio.ImageIO.read(s)?.toComposeImageBitmap() }
}.getOrNull()

private fun formatInstalledDate(epochMs: Long, language: String): String {
    if (epochMs <= 0) return Localization.get(language, "unknown")
    return try {
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(epochMs))
    } catch (_: Exception) {
        "—"
    }
}

@Composable
fun PlayerSection(
    language: String,
    autoPlayNext: Boolean,
    onToggleAutoPlayNext: (Boolean) -> Unit,
    audioQuality: String,
    onAudioQualityChange: (String) -> Unit,
    rememberShuffleRepeat: Boolean,
    onToggleRememberShuffleRepeat: (Boolean) -> Unit,
    persistentQueue: Boolean,
    onTogglePersistentQueue: (Boolean) -> Unit,
    syncViviVolume: Boolean,
    onToggleSyncViviVolume: (Boolean) -> Unit,
    sliderStyle: String,
    onSliderStyleChange: (String) -> Unit,
    onOpenPlayerDesign: () -> Unit,
    streamCacheMinutes: Int = 10,
    onStreamCacheMinutesChange: (Int) -> Unit = {},
) {
    var qualityExpanded by remember { mutableStateOf(false) }
    var sliderExpanded by remember { mutableStateOf(false) }

    Text(Localization.get(language, "player_audio"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))

    Text(Localization.get(language, "audio_quality"), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
    Box(Modifier.padding(top = 8.dp)) {
        OutlinedButton(onClick = { qualityExpanded = true }) {
            Text(audioQualityLabel(language, audioQuality))
        }
        DropdownMenu(expanded = qualityExpanded, onDismissRequest = { qualityExpanded = false }) {
            listOf("auto", "high", "low").forEach { q ->
                DropdownMenuItem(
                    text = { Text(audioQualityLabel(language, q)) },
                    onClick = { qualityExpanded = false; onAudioQualityChange(q) },
                )
            }
        }
    }

    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp).clickable(onClick = onOpenPlayerDesign),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(Localization.get(language, "player_design"), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
    }

    SettingSwitch(language, "autoplay_next", autoPlayNext, onToggleAutoPlayNext)
    SettingSwitch(language, "remember_shuffle_repeat", rememberShuffleRepeat, onToggleRememberShuffleRepeat)
    SettingSwitch(language, "persistent_queue", persistentQueue, onTogglePersistentQueue)
    SettingSwitch(language, "sync_vivi_volume", syncViviVolume, onToggleSyncViviVolume)

    Text(
        "${Localization.get(language, "stream_cache_minutes")}: " +
            if (streamCacheMinutes <= 0) Localization.get(language, "stream_cache_forever")
            else "${streamCacheMinutes} min",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 16.dp),
    )
    Text(
        Localization.get(language, "stream_cache_minutes_desc"),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Slider(
        value = if (streamCacheMinutes <= 0) 61f else streamCacheMinutes.toFloat(),
        onValueChange = {
            val v = it.roundToInt()
            onStreamCacheMinutesChange(if (v >= 61) 0 else v.coerceIn(1, 60))
        },
        valueRange = 1f..61f,
        steps = 59,
        modifier = Modifier.fillMaxWidth(),
    )

    Text(Localization.get(language, "slider_style"), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
    Box(Modifier.padding(top = 8.dp)) {
        OutlinedButton(onClick = { sliderExpanded = true }) {
            Text(sliderStyleLabel(language, sliderStyle))
        }
        DropdownMenu(expanded = sliderExpanded, onDismissRequest = { sliderExpanded = false }) {
            listOf("slim", "squiggly", "wavy").forEach { s ->
                DropdownMenuItem(
                    text = { Text(sliderStyleLabel(language, s)) },
                    onClick = { sliderExpanded = false; onSliderStyleChange(s) },
                )
            }
        }
    }
}

private fun sliderStyleLabel(language: String, style: String): String = when (style) {
    "squiggly" -> Localization.get(language, "slider_squiggly")
    "wavy" -> Localization.get(language, "slider_wavy")
    else -> Localization.get(language, "slider_slim")
}

@Composable
private fun SettingSwitch(language: String, key: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Switch(checked = checked, onCheckedChange = onCheckedChange)
        Text(Localization.get(language, key))
    }
}

private fun audioQualityLabel(language: String, quality: String): String = when (quality) {
    "high" -> Localization.get(language, "audio_quality_high")
    "low" -> Localization.get(language, "audio_quality_low")
    else -> Localization.get(language, "audio_quality_auto")
}

private fun dirSize(dir: File): Long =
    dir.walkBottomUp().filter { it.isFile }.sumOf { it.length() }

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    return "%.1f MB".format(kb / 1024.0)
}

@Composable
fun StorageSection(language: String) {
    val scope = rememberCoroutineScope()
    val cacheDir = remember { File(System.getProperty("user.home"), ".vivimusic/cache") }
    var sizeText by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        scope.launch {
            sizeText = withContext(Dispatchers.IO) { formatBytes(dirSize(cacheDir)) }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Text(Localization.get(language, "storage"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
    Text(
        "${Localization.get(language, "cache_size")}: ${sizeText ?: "…"}",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 8.dp),
    )
    Button(
        onClick = {
            scope.launch {
                withContext(Dispatchers.IO) { cacheDir.listFiles()?.forEach { it.deleteRecursively() } }
                sizeText = withContext(Dispatchers.IO) { formatBytes(dirSize(cacheDir)) }
            }
        },
        modifier = Modifier.padding(top = 8.dp),
    ) { Text(Localization.get(language, "clear_cache")) }
}

/** JSON codec for persisting the queue between sessions. */
private val queueJson = Json { ignoreUnknownKeys = true }

/** Key used to detect discrete playback changes worth syncing (no per-frame pushes). */
private data class PlaybackSyncKey(
    val trackId: String?,
    val isPlaying: Boolean,
    val isResolving: Boolean,
    val index: Int,
    val queue: List<String>,
    val repeatMode: String,
    val isShuffle: Boolean,
)

/** Mutable echo-suppression state for a volume sync loop. */
private class VolumeGuard {
    var echoUntil = 0L
    var echoValue = -1f
    var lastPushed: Float? = null
}

private fun SongItem.toSyncedSong() = SyncedSong(
    id = id,
    title = title,
    artist = artists.joinToString(", ") { it.name },
    thumbnail = thumbnail,
)

/** Builds a [PlaybackSnapshot] from the current player state (null if nothing plays). */
private fun PlayerController.toPlaybackSnapshot(): PlaybackSnapshot? {
    val s = state.value
    val current = s.current ?: return null
    return PlaybackSnapshot(
        trackId = current.videoId,
        trackTitle = current.title,
        positionMs = s.positionMs,
        isResolving = s.isResolving,
        isPlaying = s.isPlaying,
        volume = if (DesktopSettings.load().syncViviVolume) s.volume else null,
        systemVolume = SystemVolume.get(),
        repeatMode = s.repeatMode.name,
        isShuffle = s.isShuffle,
        queue = s.queue.map { np ->
            TrackRef(id = np.videoId, title = np.title, artist = np.artist, thumbnail = np.thumbnail)
        },
        queueIndex = s.index,
    )
}

/** Maps the desktop theme/language/accent onto the Android shared-preference keys. */
private fun desktopSettingsMap(
    language: String,
    themeMode: ThemeMode,
    accent: Color,
    syncViviVolume: Boolean,
): Map<String, String> = mapOf(
    "appLanguage" to Languages.toMobileCode(language).ifBlank { "SYSTEM_DEFAULT" },
    "darkMode" to when (themeMode) {
        ThemeMode.SYSTEM -> "AUTO"
        ThemeMode.LIGHT -> "OFF"
        ThemeMode.DARK -> "ON"
    },
    "selectedThemeColor" to colorToArgbInt(accent).toString(),
    "pureBlack" to "false",
    "dynamicTheme" to "false",
    "syncViviVolume" to syncViviVolume.toString(),
)
