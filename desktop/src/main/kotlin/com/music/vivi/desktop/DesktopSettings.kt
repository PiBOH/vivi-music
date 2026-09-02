package com.music.vivi.desktop

import com.music.vivi.sync.LibrarySnapshot
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/**
 * Minimal JSON-backed settings store for the desktop edition.
 * Persists the device id, pairing id, relay url and the last synced
 * settings snapshot under `~/.vivimusic/device-sync.json`.
 */
@Serializable
data class DesktopSyncState(
    val deviceId: String = "",
    val deviceName: String = "Desktop",
    val pairId: String = "",
    val serverUrl: String = "",
    val settings: Map<String, String> = emptyMap(),
    val language: String = "",
    val includePreReleases: Boolean = false,
    val darkMode: String = "system",
    val accentColor: Int = 0xFFED5564.toInt(),
    /** Accent saturation/vividness (0..1 scale, 1 = full). */
    val accentIntensity: Float = 1f,
    /** User-saved custom accent colors (ARGB ints), shown as extra palette swatches. */
    val customAccents: List<Int> = emptyList(),
    val selectedFont: String = "system",
    /** Path to a user-imported custom font file (empty = none). */
    val customFontPath: String = "",
    /** UI density scale (1f = 100%; supports 55%..200% via the density presets). */
    val densityScale: Float = 1f,
    /** Adaptive grid cell width in dp for album/artist/playlist grids. */
    val gridItemSize: Int = 160,
    /** Screen transition style between navigations: off / fade / slide. */
    val screenTransition: String = "fade",
    /** Master switch for UI animations; when off, screen transitions become instant. */
    val animationsEnabled: Boolean = true,
    /** Player slider style: slim / squiggly / wavy. */
    val sliderStyle: String = "slim",
    /** Full-player layout variant: classic / new / v2 / expressive. */
    val playerDesign: String = "classic",
    /** Full-player background style: canvas / gradient / blur / glow / apple_music / live_mesh. */
    val playerBackground: String = "canvas",
    /** Slowly rotate the player artwork while playing. */
    val rotatingThumbnail: Boolean = false,
    /** Custom artwork thumbnail size in dp (200..600). */
    val playerArtSize: Int = 521,
    /** Custom artwork top padding/offset in dp (0..120). */
    val playerArtTopOffset: Int = 33,
    /** Custom artwork corner radius in dp (0..36). */
    val playerArtCornerRadius: Int = 11,
    val miniPlayerDesign: String = "classic",
    val miniPlayerBackgroundStyle: String = "follow_theme",
    val pureBlackMiniPlayer: Boolean = false,
    val homeUseLastListen: Boolean = false,
    val randomizeHomeOrder: Boolean = false,
    /** Show the "VIVI Wrapped" card on the Home screen. */
    val showWrappedOnHome: Boolean = false,
    /** Play the native animated intro as a splash screen at startup. */
    val showIntroSplash: Boolean = true,
    /** Intro content variant: logo / logo_name / logo_tagline. */
    val introStyle: String = "logo_name",
    /** Intro backdrop variant: gradient / glow / dark. */
    val introBackground: String = "gradient",
    val pauseSearchHistory: Boolean = false,
    val pauseListenHistory: Boolean = false,
    val searchHistory: List<String> = emptyList(),
    val lyricsLineSpacing: Float = 1.35f,
    /** Stream-URL cache lifetime in minutes (1–60); 0 = never expire. */
    val streamCacheMinutes: Int = 10,
    val discordRpcEnabled: Boolean = false,
    val discordClientId: String = "",
    val lastfmEnabled: Boolean = false,
    val lastfmSession: String = "",
    val lastfmNowPlaying: Boolean = true,
    /** Apple-style mini player variant. */
    val canvasEnabled: Boolean = true,
    val canvasSource: String = "AUTO",
    val autoPlayNext: Boolean = true,
    val sidebarCollapsed: Boolean = false,
    /** Spotify-inspired 3-panel card layout & top navigation header. */
    val spotifyLayout: Boolean = true,
    /** Spotify right Now-Playing panel card. */
    val showRightSidebar: Boolean = true,
    /** Fullscreen mode toggle. */
    val isFullscreen: Boolean = false,
    /** Use the native OS title bar instead of VIVI's custom one (applies after a restart). */
    val nativeTitleBar: Boolean = false,
    /** Window placement persistence: OS-maximized flag + floating bounds in px. */
    val windowMaximized: Boolean = true,
    val windowX: Int = -1,
    val windowY: Int = -1,
    val windowWidth: Int = -1,
    val windowHeight: Int = -1,
    val cookie: String = "",
    val dataSyncId: String = "",
    val visitorData: String = "",
    val accountName: String = "",
    val accountEmail: String = "",
    val accountChannelHandle: String = "",
    val contentLanguage: String = "",
    val contentCountry: String = "",
    val syncedLyrics: Boolean = true,
    val pureBlack: Boolean = false,
    val audioQuality: String = "auto",
    val rememberShuffleRepeat: Boolean = false,
    val isShuffle: Boolean = false,
    val repeatModeKey: String = "OFF",
    val persistentQueue: Boolean = true,
    val queueJson: String = "",
    val queueIndex: Int = 0,
    val lyricsTextSize: Float = 18f,
    val library: LibrarySnapshot? = null,
    val firstLaunchDate: Long = 0L,
    val developerOptions: Boolean = false,
    val devToolsMode: String = "OVERLAY",
    val devOverlayMovable: Boolean = true,
    val devShowInTitleBar: Boolean = false,
    val devProfile: String = "FULL",
    val updateCheckIntervalHours: Int = 24,
    /** Update source: "fork" (PiBOH/vivi-music, default) or "original" (vivizzz007/vivi-music). */
    val updateSource: String = "fork",
    /** Where update notifications are shown: "in_app" (default) or "native". */
    val notificationMode: String = "in_app",
    /** Record every notification (in-app and native) for the history screen. */
    val saveNotificationHistory: Boolean = true,
    /** Seconds before an in-app (main window) notification auto-dismisses; 0 = never. */
    val inAppNotificationDurationSeconds: Int = 5,
    /** Recent notifications (newest first), capped at a small number. */
    val notificationHistory: List<NotificationRecord> = emptyList(),
    /** Master toggle for automatic backups. */
    val autoBackupEnabled: Boolean = false,
    /** Run an automatic backup once a week. */
    val autoBackupWeekly: Boolean = false,
    /** Run an automatic backup before installing an update. */
    val autoBackupBeforeUpdate: Boolean = true,
    /** Sync the in-app (VIVI) player volume slider between devices. */
    val syncViviVolume: Boolean = true,
    /** Last username used for Listen Together. */
    val listenTogetherUsername: String = "",
    /** Listen Together relay server URL (default = mobile's Hugging Face relay). */
    val listenTogetherServerUrl: String = "wss://devilmi-vivi-music-listen-together.hf.space",
    /** Auto-approve join requests without asking the host. */
    val listenTogetherAutoApproval: Boolean = false,
    /** Host syncs its in-app volume to guests. */
    val listenTogetherSyncVolume: Boolean = true,
    /** Guest re-requests a fresh sync after a reconnect. */
    val listenTogetherSmartResync: Boolean = true,
    /** Persisted Listen Together session (resume after restart). */
    val listenTogetherSessionToken: String = "",
    val listenTogetherRoomCode: String = "",
    val listenTogetherUserId: String = "",
    val listenTogetherIsHost: Boolean = false,
    val listenTogetherSessionTimestamp: Long = 0L,
    /** Usernames blocked from joining/suggesting (persisted). */
    val listenTogetherBlockedUsers: List<String> = emptyList(),
    /** Song recognition (Shazam) history, newest first. */
    val recognitionHistory: List<RecognitionHistoryItem> = emptyList(),
    /** Cider-style floating always-on-top "Now Playing" widget. */
    val showNowPlayingWidget: Boolean = false,
    /** Last widget position (px, -1 = unset/center). */
    val widgetX: Int = -1,
    val widgetY: Int = -1,
    /** Global media keys (Play/Pause/Next/Prev) even without focus (Windows). */
    val mediaKeysEnabled: Boolean = true,
    /** Tray icon right-click menu (Play/Pause/Next/Prev/Open/Quit). */
    val trayMenuEnabled: Boolean = true,
    /** Saved parametric-EQ profiles (port of the mobile equalizer). */
    val eqProfiles: List<SavedEQProfile> = emptyList(),
    /** Id of the currently active EQ profile (empty = equalization off). */
    val activeEqProfileId: String = "",
    /** Data saver: forces canvas/rotating artwork off and restores on disable. */
    val dataSaver: Boolean = false,
    /** Canvas value backed up while Data saver is active (restored on disable). */
    val dataSaverBackupCanvas: Boolean = true,
    /** Rotating-artwork value backed up while Data saver is active. */
    val dataSaverBackupRotating: Boolean = false,
    /** AI lyrics translation provider ("OpenRouter", "OpenAI", "DeepL", …). */
    val aiProvider: String = "OpenRouter",
    /** AI API key (or DeepL key when [aiProvider] is "DeepL"). */
    val aiApiKey: String = "",
    /** AI base URL (OpenAI-compatible chat-completions endpoint). */
    val aiBaseUrl: String = "https://openrouter.ai/api/v1/chat/completions",
    /** AI model id (e.g. google/gemini-2.5-flash-lite). */
    val aiModel: String = "google/gemini-2.5-flash-lite",
    /** Target language code for translated lyrics (e.g. "en"). */
    val translateLanguage: String = "en",
    /** Translation mode: "Literal" or "Transcribed". */
    val translateMode: String = "Literal",
    /** DeepL API key (used when [aiProvider] is "DeepL"). */
    val deeplApiKey: String = "",
    /** DeepL formality: "default" / "more" / "less". */
    val deeplFormality: String = "default",
)

object DesktopSettings {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }

    /** Serializes load/save so concurrent writers can't clobber each other. */
    private val lock = Any()

    private val file: File by lazy {
        File(System.getProperty("user.home"), ".vivimusic/device-sync.json").apply {
            parentFile?.mkdirs()
        }
    }

    fun load(): DesktopSyncState = synchronized(lock) {
        try {
            if (file.exists()) json.decodeFromString(DesktopSyncState.serializer(), file.readText())
            else DesktopSyncState()
        } catch (_: Exception) {
            DesktopSyncState()
        }
    }

    fun save(state: DesktopSyncState) {
        synchronized(lock) {
            try {
                file.writeText(json.encodeToString(DesktopSyncState.serializer(), state))
            } catch (_: Exception) {
                // best-effort
            }
        }
    }

    /**
     * Atomic read-modify-write: applies [transform] to the freshly-loaded state
     * and saves the result under the same lock. Use this instead of
     * `save(load().copy(...))`, which races when the UI thread and the
     * device-sync IO coroutines save at the same time and can silently drop a
     * setting the user just changed (e.g. the notification mode).
     */
    fun update(transform: (DesktopSyncState) -> DesktopSyncState) {
        synchronized(lock) {
            save(transform(load()))
        }
    }

    fun newDeviceId(): String {
        val existing = load().deviceId
        if (existing.isNotEmpty()) return existing
        val id = UUID.randomUUID().toString()
        update { it.copy(deviceId = id) }
        return id
    }

    /**
     * Returns the first-launch timestamp (epoch millis), recording "now" on the
     * very first call so the About screen shows the first-launch date rather than
     * the last-update install date.
     */
    fun ensureFirstLaunchDate(): Long {
        val state = load()
        if (state.firstLaunchDate > 0) return state.firstLaunchDate
        val now = System.currentTimeMillis()
        update { it.copy(firstLaunchDate = now) }
        return now
    }
}
