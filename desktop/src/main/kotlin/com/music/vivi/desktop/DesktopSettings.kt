package com.music.vivi.desktop

import com.music.vivi.sync.LibrarySnapshot
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
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
    /**
     * Bidirectional language-sync bookkeeping (no wall clocks involved).
     *
     * [languageSeq] grows by one on every MANUAL language change made on this
     * device. Settings snapshots carry (deviceId, languageSeq); the peer only
     * applies the value when those markers prove it is a newer manual change
     * from the other side — never an echo (same deviceId) and never a stale
     * value a peer pushes at pair time with an old/absent sequence. The last
     * applied peer change is remembered in [languagePeerId]/[languagePeerSeq]
     * so re-pushes of the same peer change are ignored across restarts.
     */
    val languageSeq: Long = 0L,
    val languagePeerId: String = "",
    val languagePeerSeq: Long = 0L,
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
    val screenTransition: String = "fade",    /** Master switch for UI animations; when off, screen transitions become instant. */
    val animationsEnabled: Boolean = true,
    /** Global UI animation speed: "fast" / "normal" / "slow" (multiplier on every tween/spring). */
    val animationSpeed: String = "normal",
    /** Navigate back/forward with the mouse X1/X2 thumb buttons (Windows/Linux). */
    /** Preferred audio output device (Java Sound mixer name; empty = system default). */
    val outputDeviceName: String = "",
    /** Player slider style: slim / squiggly / wavy. */
    val sliderStyle: String = "slim",
    /** Full-player layout variant: classic / new / expressive. */
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
    /**
     * Mobile "Auto sync with account": mirror the signed-in YouTube Music
     * playlists into the local playlist store. On by default there too.
     */
    val syncPlaylistsWithYoutube: Boolean = true,
    val pauseListenHistory: Boolean = false,
    /** Mobile "Enable swipe to change song": drag the mini-player artwork to skip. */
    val swipeThumbnail: Boolean = true,
    /** Swipe sensitivity 0..1: higher means a shorter drag changes the track. */
    val swipeSensitivity: Float = 0.73f,
    /** Content screen (port of the mobile one): hide the items flagged explicit. */
    val hideExplicit: Boolean = false,
    /** Hide the songs that only exist as a music video. */
    val hideVideoSongs: Boolean = false,
    /** Hide YouTube Shorts from the shelves. */
    val hideYoutubeShorts: Boolean = false,
    /** Artist page: show the artist description. */
    val showArtistDescription: Boolean = true,
    /** Artist page: show the subscriber count. */
    val showArtistSubscriberCount: Boolean = true,
    /**
     * Lyrics providers in the order they are asked; a provider that is not in
     * the list is skipped. Empty = the built-in order with all of them on.
     */
    val lyricsProviderPriority: List<String> = emptyList(),
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
    /** Auto-fetch related/radio tracks when the queue reaches its end (recommendations). */
    val autoLoadMore: Boolean = true,
    /** Keep a single copy of a track in the queue: adding one already present removes the old copy. */
    val preventDuplicateTracksInQueue: Boolean = false,
    /** Auto-skip to the next track when the current one fails after all retries. */
    val autoSkipNextOnError: Boolean = false,
    /** Pause playback while the OS output volume is muted or at zero. */
    val pauseWhenMediaMuted: Boolean = false,
    /** Keep the display/system awake while the expanded player screen is open. */
    val keepScreenOnWhenPlayerExpanded: Boolean = false,
    /** Keep shuffle enabled when starting new songs or playlists. */
    val persistentShuffle: Boolean = false,
    /** Double-click artwork seek adds +5s incrementally per rapid double-tap. */
    val progressiveSeek: Boolean = false,
    /** Minimum listen time (seconds) before a track enters the listen history. */
    val historyDurationSeconds: Int = 30,
    /** Auto-cache (download) a song into the audio cache when it is liked. */
    val autoDownloadOnLike: Boolean = false,
    /** Drop silent runs while a track plays ("Skip silence"). */
    val skipSilence: Boolean = false,
    /** Also cut leading silence immediately at start/seek ("Instantly skip silence"). */
    val skipSilenceInstant: Boolean = false,
    /** "Crossfade": overlap tracks near the end of the current one (port of the mobile option). */
    val crossfade: Boolean = false,
    /** Crossfade overlap duration in seconds (mobile range 1–12). */
    val crossfadeDurationSeconds: Int = 5,
    /** "Disable for gapless albums": skip crossfade between tracks of the same album. */
    val disableCrossfadeGapless: Boolean = false,
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
    /** Karaoke animation: NONE / FADE / GLOW / SLIDE / KARAOKE / APPLE / APPLE_V2 / VIVIMUSIC_1 (mirrors the APK). */
    val lyricsAnimationStyle: String = "VIVIMUSIC_1",
    /** Enhance the active word with a glow shadow (FADE/GLOW/SLIDE/KARAOKE/APPLE). */
    val lyricsGlowEffect: Boolean = true,
    /** Blur every non-active line (APPLE variant); the standard blur dims the rest. */
    val lyricsAppleMusicBlur: Boolean = false,
    val lyricsStandardBlur: Boolean = false,
    /** Tapping a line seeks the player to that line's time. */
    val lyricsClickToSeek: Boolean = true,
    /** Follow the sung line automatically as the track progresses. */
    val lyricsAutoScroll: Boolean = true,
    /**
     * Mobile "Show play/pause on thumbnail": clicking the artwork in the player
     * toggles playback. Off by default there too (it makes the artwork
     * clickable, which can get in the way of the drag gestures).
     */
    val lyricsThumbnailPlayPause: Boolean = false,
    /** Alignment of lyric lines: LEFT / CENTER / RIGHT. */
    val lyricsTextPosition: String = "CENTER",
    // Romanization — per-script toggles + how the result is displayed. The
    // maps/tables are ported from the APK's LyricsUtils; without a dictionary
    // library the kanji/hanzi half is a pass-through (the toggle still flips,
    // the renderer just shows the original text until a JVM dictionary is added).
    val lyricsRomanizeJapanese: Boolean = true,
    val lyricsRomanizeKorean: Boolean = true,
    val lyricsRomanizeChinese: Boolean = true,
    val lyricsRomanizeRussian: Boolean = true,
    val lyricsRomanizeUkrainian: Boolean = true,
    val lyricsRomanizeSerbian: Boolean = true,
    val lyricsRomanizeBulgarian: Boolean = true,
    val lyricsRomanizeBelarusian: Boolean = true,
    val lyricsRomanizeKyrgyz: Boolean = true,
    val lyricsRomanizeMacedonian: Boolean = true,
    val lyricsRomanizeHindi: Boolean = true,
    val lyricsRomanizePunjabi: Boolean = true,
    /** Show the romanized form in the main line instead of as subtitles. */
    val lyricsRomanizeAsMain: Boolean = false,
    /** For Cyrillic: apply romanization line-by-line (the APK's by-line toggle). */
    val lyricsRomanizeCyrillicByLine: Boolean = false,
    /** When true the translated lyric (when available) is shown as a subtitle. */
    val translateLyrics: Boolean = false,
    val pureBlack: Boolean = false,
    val audioQuality: String = "auto",
    /** In-app (VIVI) player volume (0..1), restored at startup. */
    val playerVolume: Float = 1f,
    val rememberShuffleRepeat: Boolean = false,
    val isShuffle: Boolean = false,
    val repeatModeKey: String = "OFF",
    val persistentQueue: Boolean = true,
    val queueJson: String = "",
    val queueIndex: Int = 0,
    val lyricsTextSize: Float = 18f,
    val library: LibrarySnapshot? = null,
    /**
     * The right-hand panel of the expressive player as the user left it:
     * "queue", "lyrics", "history", or "none" when it was closed. Remembered so
     * reopening the player does not always land on the queue.
     */
    val expressivePlayerTab: String = "queue",
    val firstLaunchDate: Long = 0L,
    val developerOptions: Boolean = false,
    val devToolsMode: String = "OVERLAY",
    val devOverlayMovable: Boolean = true,
    val devShowInTitleBar: Boolean = false,
    val devProfile: String = "FULL",
    val updateCheckIntervalHours: Int = 24,
    /** Update source: "fork" (PiBOH/vivi-music-de, default) or "original" (vivizzz007/vivi-music). */
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
    /**
     * Hides the "download the Android APK" button on the Devices (sync) screen.
     *
     * There is deliberately no switch for this one in the UI: it is an option
     * that only exists in `~/.vivimusic/settings.json` (see [SettingsFile]), and
     * it defaults to true (hidden).
     */
    @SerialName("hide_custom_apk_download_button")
    val hideCustomApkDownloadButton: Boolean = true,
)

object DesktopSettings {
    private val json = sharedJsonPretty

    /** Serializes load/save so concurrent writers can't clobber each other. */
    private val lock = Any()

    private val file: File by lazy {
        File(System.getProperty("user.home"), ".vivimusic/device-sync.json").apply {
            parentFile?.mkdirs()
        }
    }

    /**
     * In-memory snapshot of the file, so [load] does not have to read and parse
     * it every time. It is called from the polling loops that run for the whole
     * session (volume sync every 500 ms, mute watchdog every 800 ms, command
     * poll every 500 ms), and each call used to re-read AND re-parse ~58 KB of
     * JSON on an IO thread while audio was playing — the same file that the
     * writer of a settings change rewrites. Every writer goes through [save],
     * which refreshes this snapshot, so the cache cannot go stale.
     */
    @Volatile private var cached: DesktopSyncState? = null

    fun load(): DesktopSyncState = cached ?: synchronized(lock) {
        cached ?: read().also { cached = it }
    }

    private fun read(): DesktopSyncState = try {
        if (file.exists()) json.decodeFromString(DesktopSyncState.serializer(), file.readText())
        else DesktopSyncState()
    } catch (_: Exception) {
        DesktopSyncState()
    }

    fun save(state: DesktopSyncState) {
        synchronized(lock) {
            try {
                file.writeText(json.encodeToString(DesktopSyncState.serializer(), state))
                cached = state
            } catch (_: Exception) {
                // best-effort
            }
            // Keep the human-editable mirror (settings.json) in step with every
            // change made from the UI, so editing it is always starting from the
            // current options. Inside the lock, so two writers cannot land in
            // the file out of order.
            SettingsFile.mirror(state)
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
            val before = load()
            val after = transform(before)
            logChanges(before, after)
            save(after)
        }
    }

    /**
     * Fields that change on their own (window/bookkeeping, playback state,
     * history accumulators, sync bookkeeping) would only spam the activity
     * log, so they are skipped by [logChanges].
     */
    private val volatileFields = setOf(
        "windowX", "windowY", "windowWidth", "windowHeight", "windowMaximized",
        "widgetX", "widgetY",
        "queueJson", "queueIndex",
        "library",
        "notificationHistory", "recognitionHistory", "searchHistory",
        "languageSeq", "languagePeerId", "languagePeerSeq",
        "firstLaunchDate",
    )

    /** Credentials and API keys that must never be written to the activity log
     *  in clear text (they would be handed out inside every exported log zip). */
    private val redactedFields = setOf(
        "cookie", "dataSyncId", "visitorData",
        "aiApiKey", "deeplApiKey", "lastfmSession",
        "listenTogetherSessionToken",
    )

    private fun jsonValue(el: kotlinx.serialization.json.JsonElement?): String = when (el) {
        null -> "unset"
        is JsonPrimitive -> el.content
        else -> el.toString()
    }

    /**
     * Records every setting the user actually changed (field: old → new) into
     * the activity log at full value — nothing is shortened, because a
     * truncated value is what makes a support log useless. Skipping
     * [volatileFields] keeps the log readable; [redactedFields] (credentials
     * and API keys only) are never dumped in clear text.
     */
    private fun logChanges(before: DesktopSyncState, after: DesktopSyncState) {
        if (before == after) return
        runCatching {
            val a = json.encodeToJsonElement(after).jsonObject
            val b = json.encodeToJsonElement(before).jsonObject
            val parts = mutableListOf<String>()
            for ((key, newValue) in a) {
                if (key in volatileFields) continue
                val oldValue = b[key]
                if (oldValue == newValue) continue
                val shownOld = if (key in redactedFields) "[redacted]" else jsonValue(oldValue)
                val shownNew = if (key in redactedFields) "[redacted]" else jsonValue(newValue)
                parts += "$key: $shownOld → $shownNew"
            }
            if (parts.isNotEmpty()) AppLog.log("settings", parts.joinToString(" | "))
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
