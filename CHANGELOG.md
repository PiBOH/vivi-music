# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Desktop-specific changes are marked with `[DE]`. Desktop releases use a combined
version `<mobile>_DE-<desktop>` (e.g. `6.0.5_DE-1.0.0`), where the desktop part is
the program's own SemVer. `[APK]` marks mobile-only changes.

## [Unreleased]

## [6.4.41_DE-1.37.0-nightly] - 2026-08-28

### Added
- [DE] **"Download VIVI for Android (APK)" button on the Devices/sync screen**: fetches the newest Android APK from the selected update source's GitHub releases (default PiBOH/vivi-music — e.g. `VIVIMusic-6.4.41-debug.apk`) and opens it in the browser; if no APK is published it falls back to the releases page. 2 new strings translated in all 47 languages (batch 35).

## [6.4.41_DE-1.36.0-nightly] - 2026-08-28

### Added
- [DE] **Toggle between the native OS title bar and VIVI's custom one** (Settings → Aspetto): off by default (VIVI's bar). When the native system title bar is enabled, the window uses the OS chrome — its minimize/maximize/close buttons — and VIVI's own bar adapts by hiding its window buttons (the floating overlay on the player screen is hidden too). The change applies after a restart (a dialog offers to restart immediately), and all 4 new strings are translated in all 47 languages.

## [6.4.41_DE-1.35.8-nightly] - 2026-08-28

### Fixed
- [DE] **Fullscreen no longer traps the auto-hide Windows taskbar**: the fullscreen toggle used Compose's `WindowPlacement.Fullscreen`, which can oversize an undecorated window on non-100% DPI displays (the same bug class as its Maximized placement) — the window pushed past the screen edge and the auto-hide taskbar could never be revealed, forcing a window restore to reach it. Fullscreen is now applied with the OS API (`MAXIMIZED_BOTH`), which never oversizes: with an auto-hide taskbar the window fills the whole screen and the taskbar still slides up on hover at the bottom edge; with a visible taskbar the window respects the work area. Leaving fullscreen (toggle or maximize button) restores exactly the previous placement (floating bounds or maximized).

## [6.4.41_DE-1.35.7-nightly] - 2026-08-28

### Changed
- [DE] **Faster track start (time-to-audio roughly halved on first play)**: the stream resolver now returns the NewPipe URL immediately when the extractor succeeds, instead of also running the whole ~12-client chain + a HEAD validation round-trip as "insurance" on every first play (~0.7–1.4 s saved). The client chain only runs when NewPipe is bot-blocked, and it now collects just 2 playable candidates without a HEAD request per candidate — the download itself is the validation, and the player falls through to the next candidate on failure. The audio player's start threshold dropped from 64 KB to 32 KB (the `moov` is a few KB and the sample walker skips incomplete trailing atoms), so the decoder starts on the first fragment instead of waiting for a second one (~0.2–0.4 s saved on slower links). Track skips were already instant (prefetch + caches) and are unchanged.

## [6.4.41_DE-1.35.6-nightly] - 2026-08-28

### Fixed
- [DE] **Window controls (minimize / maximize / close) are now always visible**: the window is undecorated (no OS title bar), but the buttons only lived in the Spotify top header — which disappears on the full player screen and in the non-Spotify layout, leaving no way to minimize or close the window. The three buttons were extracted into a shared `WindowControls` composable; the Spotify header still hosts them, and a floating top-right overlay shows them on the player screen and in the non-Spotify layout (the transparent overlay passes clicks through to the content below).

## [6.4.41_DE-1.35.5-nightly] - 2026-08-28

### Changed
- [DE] **Playback starts while the track is still downloading (progressive streaming)**: the stream is downloaded to a unique `.part` file in the background and the decoder starts as soon as the first audio fragment is on disk, instead of waiting for the whole file. The sample table grows incrementally as new `moof` fragments arrive. A prefetch and a user play of the same track now share ONE download (no more concurrent-write races), completed downloads are promoted to the cache best-effort (copy fallback on Windows where an open file can't be renamed), and stale `.part` leftovers are swept on startup.
- [DE] **Stream resolution is much faster**: the client chain used to run all ~12 fallback clients sequentially even when the first URL validated, costing many round-trips per track. It now stops at the first HEAD-validated URL (NewPipe + any already-collected candidates remain as download fallbacks).
- [DE] **The "downloading" indicator is accurate**: the loading phase is no longer cleared the instant resolution ends — it now stays visible until audio is actually ready (the first decoded frame).

## [6.4.41_DE-1.35.4-nightly] - 2026-08-28

### Fixed
- [DE] **"Module with the Main dispatcher is missing" crash**: desktop code hops back to `Dispatchers.Main` after off-thread image blur, but the module that provides the Swing-backed Main dispatcher was never declared — only `kotlinx-coroutines-core` was. Running that path (artwork blur) threw the missing-dispatcher error once out of the dev classpath/into a packaged run. Added `kotlinx-coroutines-swing` at the same version as `core`, which backs `Dispatchers.Main` on Compose Desktop.

## [6.4.41_DE-1.35.3-nightly] - 2026-08-28

### Fixed
- [DE] **Expressive player volume slider actually works**: it was left with an empty `onValueChange` placeholder (``/* Volume update */``) from the UI port, so dragging it did nothing — now wired to the real volume handler (this is why "can't change VIVI's volume" also happened while not paired).
- [DE] **Pressing play after a track finished now restarts it**: a finished track keeps its end position, and restarting from the end instantly "completed" again and stopped — the play button looked broken. Play now restarts from 0:00.
- [DE] **Truncated audio cache files are re-downloaded**: a cache file that passes the header check but holds only a fraction of the track (interrupted download, or a prefetch/play race writing the same partial file) played a few seconds and "ended", which caused tracks to stop after ~10 s or skip by themselves. Partial downloads now use unique filenames (no more concurrent-write corruption), a sample-count check against the known duration detects truncation, and a failed play evicts the bad file so the retry re-downloads a clean copy.

## [6.4.41_DE-1.35.2-nightly] - 2026-08-28

### Fixed
- [DE] Device sync no longer lets a stale remote snapshot override a fresh local action:
  - **VIVI volume slider can now be changed while paired**: a local drag wins for 2 s, so an echoed or stale value from the phone can't snap the slider back the moment you let go (previously the peer's value kept re-applying and the slider appeared stuck).
  - **No more double-play after resolving**: when the stream finishes resolving, only a remote seek/play-pause that actually arrived *while* we were buffering is re-applied — a pre-play snapshot (common over a slow phone hotspot) no longer pauses the track the user just started, so pressing play once is enough again.
  - **Stale "paused" ticks ignored**: a periodic peer snapshot older than a full sync tick (5 s) with `isPlaying=false` is no longer applied, so the DE can't auto-pause a few seconds after you hit play because the phone's snapshot hasn't caught up yet.

## [6.4.41_DE-1.35.1-nightly] - 2026-08-28

### Changed
- [DE] Apple/Cider-style visual polish across the UI:
  - **Now-playing indicator in lists**: the current row in every song list (album, playlist, search, library…) is highlighted with the accent color and shows three animated equalizer bars that move with the real decoded audio level (falling back to a gentle idle pulse when paused); the row background is softly tinted.
  - **Artwork ambience**: the player artwork now sits on a colored glow derived from the blurred artwork and casts a soft specular reflection below it (Apple Music style).
  - **Glass transport controls**: Previous/Play/Next/Shuffle/Repeat are now semi-transparent glass circles (subtle sheen + border) instead of flat Material buttons, so they sit on the artwork; the play button keeps the accent color.
  - **Crossfade on track change**: background, artwork and controls fade between songs (≈300 ms) instead of cutting hard.
  - **Expressive player side panel** now uses the Material 3 theme surface + border instead of a hardcoded dark color, so it follows the accent/theme.

## [6.4.41_DE-1.35.0-nightly] - 2026-08-28

### Added
- [DE] Cider-inspired desktop features (Settings → Desktop features):
  - **Audio visualizer**: new player background style (`Visualizer`) whose bars react to the real decoded PCM level of the playing stream, with a dark scrim so the artwork/title stay readable.
  - **Now Playing widget**: small always-on-top, draggable window showing the current track (artwork, title, artist) with Previous/Play-Pause/Next controls; position persists across restarts; toggle in Settings → Desktop features.
  - **Global media keys (Windows)**: Play/Pause, Next, Previous and Stop keys work even when the window has no focus, via a JNA `WH_KEYBOARD_LL` hook on its own message-pump thread; Windows-only, toggled in Settings → Desktop features.
  - **Tray menu**: right-click the system tray icon for Play/Pause, Next, Previous, Open VIVI Music and Quit (labels localized, tooltip shows the current track); toggled in Settings → Desktop features.
  - **Fullscreen lyrics**: new "Fullscreen lyrics" button in the player (Classic/New/V2 designs) opens a Cider-style focus mode — blurred artwork backdrop, centered synced lyrics, bottom transport bar; Esc/back exits.
- [DE] New translations for all of the above across all 47 languages (13 new keys, `player_background_visualizer` … `quit`).

### Notes
- The Windows media flyout (SMTC: showing the track in the Win+volume popup) is **not** included yet: it requires WinRT COM interop that cannot be validated without a Windows machine; tracked in TODO.md.

## [6.4.41_DE-1.34.8-nightly] - 2026-08-28

### Fixed
- [DE] The full player (Classic/New/V2 designs) had no visible way back: the sidebar and the top header are hidden on the player screen and the window is undecorated, so the player felt like it filled the whole screen with no way to shrink it. A back button (chevron) now appears at the top-left of every player design (it was only present in the Expressive one), and the Escape key goes back like Backspace/Alt+Left.

## [6.4.41_DE-1.34.7-nightly] - 2026-08-28

### Reverted
- [APK] Reverted the Telegram channel change on the Android app (About + Welcome screens) and the issue template: the new channel `t.me/vivimusicde` is DE-only, so the mobile app keeps the original `t.me/vivimusicapp` and the template keeps its old invite link.

### Fixed
- [DE+APK] Google sign-in validation reported a bare "NullPointerException"/"unknown error" when the innertube `account_menu` answered as guest: `accountInfo` now reports "Not signed in: account_menu returned no active account" instead of crashing on the missing header.
- [DE] The embedded sign-in now captures the FULL session cookie set: it waits until the critical HttpOnly cookies (SID + `__Secure-3PSID`) are present (not just SAPISID), reloads `music.youtube.com` with the session to force every youtube.com cookie, re-captures before closing, and logs the captured cookie names + missing ones to `~/.vivimusic/login-debug.log` for diagnosis (validation failures are appended to the same file).

## [6.4.40_DE-1.34.6-nightly] - 2026-08-28

### Changed
- [DE+APK] Updated every reference to the old Telegram channel (`t.me/vivimusicapp` and the old invite link) to the new channel `https://t.me/vivimusicde` — DE About screen, Android About screen, Android Welcome screen, the issue template and the whole website (`.websitede/**`).

## [6.4.39_DE-1.34.5-nightly] - 2026-08-28

### Fixed
- [DE] Google sign-in via the embedded WebView ended with "unknown error" after the window closed: the session was captured as soon as SAPISID appeared, but the remaining redirect cookies (SID, HSID, SSID, APISID, `__Secure-3PSID`, …) were still arriving, so the innertube `account_menu` validation answered as guest (NPE -> generic error). The WebView now waits 3 s for the cookie set to settle, re-captures the full header before closing, validation retries once after a 2 s pause, and the failure message includes the real exception class instead of "unknown error". Captured cookie names are also logged (`[login-webview] captured N cookies: …`) for diagnosis.

## [6.4.39_DE-1.34.4-nightly] - 2026-08-28

### Fixed
- [DE] Selecting a player design in Settings → Riproduzione e audio → design del player (and the density screen) could make the whole UI explode and freeze the app, forcing Task Manager. Root cause: `PlayerDesignScreen`/`DensityScreen` used `fillMaxSize()` inside the scrollable `SettingsSubScreen`; with an infinite maximum height the layout sized to Infinity (everything looks enlarged and the app becomes unresponsive). Both screens now use `fillMaxWidth()` only.

## [6.4.39_DE-1.34.3-nightly] - 2026-08-28

### Fixed
- [DE] Windows window management overhaul: the app forced Compose's `WindowPlacement.Maximized`/Fullscreen on every start and when opening the player; on an undecorated window with a display scale other than 100% this can size the window LARGER than the screen (everything looks enlarged, the title bar ends up off-screen and the app must be killed from Task Manager, and the window can cover the auto-hiding taskbar). Now the app starts floating, restores the last placement with the OS APIs (`Frame.MAXIMIZED_BOTH` respects the taskbar and DPI scaling), persists position/size/maximized state across restarts, clamps restored bounds to the usable screen area, never resizes the window when opening the player, and uses true fullscreen only via the explicit toggle.

## [6.4.39_DE-1.34.2-nightly] - 2026-08-28

### Fixed
- [DE] The embedded login WebView stayed permanently white in the packaged app. Root cause found with a limited-modules reproduction of the jlink runtime: the packaged image was missing the `java.net.http` module (JavaFX ships as classpath jars, so jlink cannot see its requirements — the page load then hangs at RUNNING forever) and `jdk.unsupported` (`sun.misc.Unsafe`, required by the Marlin 2D rendering engine). Both modules are now included; the reproduction shows `state=SUCCEEDED` with the Google page actually rendered.

### Changed
- [DE] The sidebar now compresses to a compact icon rail (72dp, centered icons) when collapsed instead of disappearing entirely, in both the Spotify and classic layouts. A menu button inside the collapsed rail expands it; the classic layout header gains a collapse button, and the Spotify top bar keeps its toggle. State stays persisted across restarts.

## [6.4.39_DE-1.34.1-nightly] - 2026-08-28

### Added
- [DE] Completed the translations of the 4 new UI strings (`close`, `listen_together_title`, `search_hint`, `up_next`) into all 47 non-English languages (batch `desktop_extra_translations_32`); every language table now contains all 441 keys with no fallback.

## [6.4.39_DE-1.34.0-nightly] - 2026-08-28

### Added
- [DE] Ported the new UI (from the NewUI_desktop.zip line, versions 1.34.x–1.35.x) onto the current codebase, replacing the previous player/mini-player design: Apple-style single-column player (tuned artwork 521dp, 25sp title/artist, 40dp favorite & options pills, 513dp seekbar, 64dp transport, 465dp volume bar) with the new `EXPRESSIVE` thick capsule track style in `ViviSlider`.
- [DE] Modernized MiniPlayer suite: 3 design variants (Classic, New single-column hero, Apple-style floating island) and 5 animated background styles (Follow Theme, Gradient, Blur, Glow Motion, Live Mesh), plus a Pure Black toggle and a Fullscreen action expanding into the full player.
- [DE] Spotify-inspired 3-panel card layout with top navigation header (`spotifyLayout`) and right Now-Playing panel (`showRightSidebar`) with multi-artist parsing and profile photo resolution.
- [DE] Player personalization options persisted in settings: `miniPlayerDesign`, `miniPlayerBackgroundStyle`, `pureBlackMiniPlayer`, `isFullscreen`, `showRightSidebar`, `spotifyLayout`, `playerArtSize`, `playerArtTopOffset`, `playerArtCornerRadius`.

### Changed
- [DE] Version numbering now follows SemVer properly: a feature-level change like this new UI bumps the minor version (1.33.x → 1.34.0); patch is reserved for fixes only.
- [DE] Re-integrated all features added since the NewUI base: embedded JavaFX Google sign-in (`LoginWebView`), account/Library inline login options (`LoginContent`), AI-translation disclaimer, installer auto-cleanup, official-logo toast path, and the latest localization table (441 keys). 4 new UI strings (`close`, `listen_together_title`, `search_hint`, `up_next`) added in English; full translations of the new strings follow in a later commit.

## [6.4.39_DE-1.33.129-nightly] - 2026-08-28

### Changed
- [DE] The Library sidebar entry (when not signed in) now shows the two sign-in options (Accedi con Google / manual cookie) directly instead of a "Log in" button that opened a separate screen — same behavior as Settings → Account, so every login entry point is a single screen.

## [6.4.39_DE-1.33.128-nightly] - 2026-08-28

### Changed
- [DE] Removed the intermediate "Log in" step in Settings → Account: when not signed in, the screen now shows the two sign-in options (Accedi con Google / manual cookie) directly, with no extra navigation.

### Fixed
- [DE] Embedded login WebView could stay blank white in the packaged app even though the page loaded: JavaFX now forces the software renderer (`prism.order=sw`) before starting, avoiding the GPU pipeline conflict with the Compose window that prevented painting on weaker hardware.

## [6.4.39_DE-1.33.127-nightly] - 2026-08-28

### Fixed
- [DE] Completed every missing desktop translation: 3,582 missing per-language strings were added across all 47 languages (new batches `desktop_extra_translations_24..31`). Every key used in the desktop sources now exists in the English table (13 raw keys such as `wrapped_show_on_home` or `screen_transitions` no longer show up in the UI), and every language table now contains all 437 keys (no more silent English fallback for the 41 keys that were mapped to inline English literals in the generator).
- [DE] Fixed the desktop localization generator dropping translations: extra translation batches are now merged per key instead of being replaced by the last file's language subset, so a key defined in two batches keeps all its languages.
- [DE] Added `scripts/check_localization.py` to verify used-key coverage and per-language completeness.

## [6.4.39_DE-1.33.126-nightly] - 2026-08-28

### Changed
- [DE] Removed the redundant "Accesso" heading from the login screen: when not signed in, the screen now shows the two sign-in options (Accedi con Google / manual cookie) directly.

## [6.4.39_DE-1.33.125-nightly] - 2026-08-27

### Fixed
- [DE] Embedded login WebView could stay blank white after opening in the packaged app: the WebView now gets explicit dimensions, a forced re-layout/paint nudge when the page starts loading and again on load success, and logs its load state/size/title (prefixed `[login-webview]`) so any remaining blank-page issue is diagnosable from the console.

## [6.4.39_DE-1.33.124-nightly] - 2026-08-27

### Fixed
- [DE] Fixed the embedded YouTube login WebView failing in packaged builds with `NoClassDefFoundError: com/sun/media/jfxmedia/events/PlayerStateListener`: the platform-specific `javafx-media` jar is now packaged alongside `javafx-web`, and the runtime image includes the required `jdk.jsobject` module. Verified with a packaged-app smoke test (`SMOKE: WEBVIEW OK`).

## [6.4.39_DE-1.33.123-nightly] - 2026-08-27

### Fixed
- [DE] Fixed embedded JavaFX login startup after the `.122` build: the toolkit is now initialized once with `Platform.startup` instead of the single-use `Application.launch`, avoiding false WebView-unavailable states after the first attempt.

## [6.4.39_DE-1.33.122-nightly] - 2026-08-27

### Fixed
- [DE] Fixed embedded WebView packaging by replacing the Swing-dependent `JFXPanel` with a direct JavaFX `Stage`. JavaFX jars and native runtime components are now present in the distributable application image, while browser fallback remains available if JavaFX cannot start.

## [6.4.39_DE-1.33.121-nightly] - 2026-08-27

### Fixed
- [DE] Fixed all desktop packaging jobs failing at `createRuntimeImage`: external JavaFX modules are no longer incorrectly passed to jlink without a module path. The embedded WebView remains optional and uses the browser fallback when its JavaFX runtime is unavailable.

## [6.4.39_DE-1.33.120-nightly] - 2026-08-27

### Fixed
- [DE] Fixed the embedded JavaFX login crash `NoClassDefFoundError: jdk/swing/interop/SwingInterOpUtils` by packaging the required `jdk.swing.interop` runtime module.
- [DE] Prevented repeated login WebView initialization failures from reopening the same error indefinitely; failed initialization now falls back cleanly and only once.

## [6.4.39_DE-1.33.119-nightly] - 2026-08-27

### Fixed
- [DE] Fixed Windows toast details continuing to display the old logo from the persistent `~/.vivimusic/logo_vmde.png` cache. Toasts now refresh the circular bundled logo through a new cache path on every notification.

## [6.4.39_DE-1.33.118-nightly] - 2026-08-27

### Added
- [DE] Reworked YouTube login with a real embedded JavaFX sign-in window that opens directly on Google's login page, explains the three required steps, captures the session automatically after returning to YouTube Music, and closes itself after saving the persistent session.
- [DE] Added a clear browser fallback and a collapsible manual Cookie / DATASYNC_ID / VISITOR_DATA login path for systems where the embedded WebView cannot start.
- [DE] Added a language-screen notice explaining that translations were created with AI tools and may not be 100% reliable.

### Fixed
- [DE] Fixed GitHub Auto Release changelog extraction when versioned entries include a release-channel suffix such as `-nightly`; release notes are no longer empty.

### Changed
- [DE] Documented in the README that Windows receives the most compatibility testing because it is the only platform currently available to the lead developer for local testing.

## [6.4.39_DE-1.33.117-nightly] - 2026-08-27

### Added
- [DE] When the Updates screen finds a new version, it now shows a "What's new" card with the pending release's changelog (fetched live from the repository, same source as About → Changelog), translated in all supported languages.

## [6.4.39_DE-1.33.116-nightly] - 2026-08-27

### Changed
- [DE] The official logo is now circular everywhere it appears: Windows/macOS/Linux app icons (`.ico`/`.icns`/`.png`), intro splash, tray icon, native notifications, installer wizard art, README and website.
- [DE] `scripts/generate_desktop_icons.py` applies an anti-aliased circular mask automatically, so future logo regenerations keep the round shape.

## [6.4.39_DE-1.33.115-nightly] - 2026-08-27

### Added
- [DE] Added a user-friendly YouTube Music login entry point with an optional embedded WebView attempt, persistent profile location and a system-browser/manual-cookie fallback on Windows, Linux and macOS.

## [6.4.39_DE-1.33.114-nightly] - 2026-08-27

### Added
- [DE] Update installers older than seven days are removed automatically while recent installers remain available for reuse.

## [6.4.39_DE-1.33.113-nightly] - 2026-08-27

### Changed
- [DE] Replaced the desktop branding in the intro, notifications, installers, app resources, website and README with the official `logo_vmde_official.jpg` artwork; regenerated the Windows, macOS, Linux and installer icon variants.

## [6.4.39_DE-1.33.112] - 2026-08-19

### Fixed

- [DE] The play/pause button could stay stuck on "play" while audio kept
  playing (and pausing then did nothing). The player's position callback was
  re-applying the initial `resumeWhenReady` intent as `isPlaying` on every
  ~100 ms report, so a manual play/pause toggle (e.g. after the phone started
  a track paused/resolving while paired) was overwritten the next tick. The
  intent is now applied only on the resolving→ready transition; later reports
  just advance the position.

## [6.4.39_DE-1.33.111] - 2026-08-19

### Added

- [APK] Complete translations for the Devices section in all 54 supported
  languages: Devices, Device sync, Relay server, Find desktop, Scan QR code,
  pairing code, Pair/Unpair, paired state and Sync VIVI volume (plus their
  descriptions). Previously the whole section fell back to English.
- [DE] A "How to connect your phone" step-by-step card on the Device sync
  screen (same Wi-Fi, Start LAN server, scan the QR code, confirm the code),
  translated in all 47 desktop languages and recommending the LAN server.

## [6.4.38_DE-1.33.110] - 2026-08-19

### Fixed

- [DE+APK] Player sync no longer stutters ("va a salti") when paired: the
  periodic drift tolerance was raised from 250 ms to 1 s, so the relay
  clock-offset jitter (especially on a phone hotspot) no longer triggers a
  forward catch-up seek every 5 s — only genuine drift is corrected.
- [DE] The desktop now waits for the phone while the phone is still resolving
  a new track (symmetric with the phone already waiting for the desktop):
  `applyRemotePlayback` holds playback on the peer's `isResolving` state and
  auto-resumes the moment the desktop's own stream is ready, without emitting
  a transient `isPlaying=false` snapshot that paused the phone. This stops the
  desktop from playing ahead/behind and jumping to catch up on a phone-initiated
  track change.

## [6.4.37_DE-1.33.109] - 2026-08-19

### Fixed

- [APK] Added `VISIONOS` and `ANDROID_NO_SDK` to the mobile fallback client
  chain, completing parity with the desktop edition's proven resolver. These
  clients cover the remaining music-only tracks that the generic clients still
  report as `Video non disponibile`.

## [6.4.36_DE-1.33.109] - 2026-08-19

### Fixed

- [APK] Some tracks still failed with `IO_UNSPECIFIED (2000): Video
  non disponibile`. The mobile fallback client chain was missing the
  music-specific clients (`IOS_MUSIC`, `ANDROID_MUSIC`) that resolve
  music-only / YouTube-Music-signed streams the generic clients report as
  "Video unavailable". These clients are now tried right after the main VR
  client, matching the desktop edition which plays those tracks reliably.

## [6.4.35_DE-1.33.109] - 2026-08-19

### Changed

- [DE] On Debian (and Debian-derived distros such as Ubuntu) the updater now
  prefers the `.deb` installer over the AppImage. Detection reads
  `/etc/os-release` (`ID`/`ID_LIKE` containing `debian` or `ubuntu`); other
  Linux distros keep the AppImage as the preferred installer.

## [6.4.35_DE-1.33.108] - 2026-08-19

### Fixed

- [APK] When every innerTube client is bot-flagged (`Video unavailable`), the
  mobile now falls back to a NewPipe-resolved stream URL instead of failing.
  NewPipe resolves its own signature independently, which is why the desktop
  edition (NewPipe-first) plays where the shared clients were refused. A
  minimal audio format is synthesized from the NewPipe itag so playback works
  without the client response.

## [6.4.34_DE-1.33.108] - 2026-08-19

### Fixed

- [APK] Stream downloads now send a browser-like `User-Agent` (Firefox),
  matching the UA already used to validate the URL and the one the desktop
  edition uses to download NewPipe URLs. The previous default media3 UA was
  getting the connection reset by the googlevideo CDN, surfacing as
  `IO_UNSPECIFIED (2000): Source error`. The on-screen playback error now also
  shows the nested cause (the real reason) instead of just the `Source error`
  wrapper.

## [6.4.33_DE-1.33.108] - 2026-08-19

### Fixed

- [APK] Mobile stream resolution now prefers a NewPipe-signed URL (matched to
  the selected audio format) over the shared ANDROID_VR client URL. NewPipe
  resolves its own signature with a browser-like user-agent, so its URLs are
  far less prone to the CDN bot-flagging that caused `IO_UNSPECIFIED (2000)`;
  the client URL remains the fallback. Mirrors the desktop edition's
  NewPipe-first resolver.

## [6.4.32_DE-1.33.108] - 2026-08-19

### Changed

- [APK] Simplified the mobile stream-retry path: extracted a shared
  `reResolveCurrentTrack` helper (removes the duplicated rotate/seek/prepare
  block) and deleted the no-op `YTPlayerUtils.forceRefreshForVideo` stub. No
  behavior change.

## [6.4.31_DE-1.33.108] - 2026-08-19

### Added

- [APK] A visible on-screen toast now appears when playback fails, showing the
  exact error code (e.g. `IO_UNSPECIFIED (2000)`) and message so stream
  failures can be identified without logcat. The same error is also appended to
  the playback log (Settings → Content → Playback logs).

## [6.4.30_DE-1.33.108] - 2026-08-19

### Fixed

- [APK] Stream-resolution retry now rotates the guest identity (visitorData)
  before re-resolving on a 403/IO error, so a bot-flagged or expired
  googlevideo URL isn't reused verbatim. This stops tracks failing to start,
  auto-skipping, or pausing by themselves in the first seconds — even when the
  phone is not paired to the desktop.

## [6.4.29_DE-1.33.108] - 2026-08-18

### Fixed

- [DE] Developer options RAM now shows the program's real usage instead of the
  JVM heap-only figure. A new "Memory · Process" row reports the process RSS /
  working set (via psapi on Windows, `/proc/self/status` on Linux, committed
  heap+non-heap elsewhere); heap and system RAM remain as detail rows in the
  Full profile. Title-bar and overlay pill RAM also use the real figure.
- [DE] Developer options overlay switches (movable / show in title bar) now
  appear only when "Overlay" display mode is selected.

## [6.4.29_DE-1.33.107] - 2026-08-18

### Added

- [DE] Look-ahead cache prefetch: while a track is loaded (playing or paused)
  the next 3 tracks are resolved and downloaded to the audio cache in the
  background, and their lyrics are fetched and cached too (in-memory + on-disk
  under `~/.vivimusic/cache/lyrics`). The Lyrics screen now reads the cache
  first instead of re-fetching on every open.

## [6.4.29_DE-1.33.106] - 2026-08-18

### Fixed

- [DE] Songs failing to start or "ending" after a few seconds when the stream
  cache is set to "Forever". Two causes: (1) a retry re-used the same expired
  googlevideo URL because the resolver cached it forever — a failed stream now
  evicts its cache entry so the retry re-resolves a fresh URL; (2) `isValidMp4`
  only checked the `ftyp` header, so a truncated cache file was reused and the
  track "finished" early (auto-skip or auto-pause) — it now also requires at
  least one `moof` box and re-downloads on any parse error.

## [6.4.29_DE-1.33.105] - 2026-08-18

### Fixed

- [DE] Backspace no longer navigates back while typing in a search/text field.
  The global shortcut used the preview (tunnel) key phase, so it swallowed the
  key before the focused text field saw it. It now uses the bubble phase, so a
  text field consumes Backspace (and Ctrl+Z/Ctrl+Y for text undo) first; the
  shortcut only fires when no text field handled the key.

## [6.4.29_DE-1.33.104] - 2026-08-18

### Fixed

- [DE] A failed track auto-skipped through the whole queue. `AudioPlayer` fired
  `onComplete` (in a `finally`) even after an error, so `PlayerController`
  treated every failure as "track ended" and advanced to the next one. `onComplete`
  now fires only on a normal finish, so a 403/resolution failure retries and then
  stops instead of looping. This also stopped the constant queue/resolving churn
  that was flooding the sync channel and suppressing play/pause sync.

## [6.4.29_DE-1.33.103] - 2026-08-18

### Fixed

- [DE] Song recognition (Shazam) always returned "No match found". The desktop
  recorder assumed a fixed 44.1 kHz mono little-endian capture and resampled
  with naive linear interpolation, whose aliasing corrupted the spectral band
  Shazam fingerprints. It now reads the mic's actual negotiated format (rate,
  channels, endianness), downmixes to mono, and uses a band-limited sinc + Hann
  anti-aliased resampler.

## [6.4.29_DE-1.33.102] - 2026-08-18

### Added

- [DE] Settings search: a search icon sits next to the "Settings" title; it
  expands into a text field that live-filters the settings list by title and
  subtitle, with a "no results" state.

## [6.4.29_DE-1.33.101] - 2026-08-18

### Fixed

- [DE+APK] Players no longer freeze/pause at random while paired. The mobile
  marked `isResolving` on every mid-song rebuffer, and both devices paused the
  other whenever the peer was "resolving"; now a same-track `isResolving` only
  skips the stale-position re-sync and keeps playing (initial track resolution
  still holds the peer until audio actually starts).

## [6.4.28_DE-1.33.100] - 2026-08-18

### Changed

- [DE] Rebuilt the intro splash from scratch as a fully native Compose animation
  (logo fade/scale-in with a breathing pulse) instead of playing a frame
  sequence extracted from the MP4. Removed the bundled JPEG frames and the
  extraction tool. Two new selectors in the Intro screen: **Intro content**
  (Logo only / Logo + app name / Logo + name + version) and **Background**
  (Gradient / Glow / Dark). The original `desktop/icons/*.mp4`/`.gif` files are
  kept as source assets only.

## [6.4.28_DE-1.33.99] - 2026-08-18

### Added

- [DE] "Preview intro" button in the Intro screen: plays the startup intro
  fullscreen (click or end dismisses it) without restarting.

### Changed

- [DE] In Settings → Appearance the intro is now a dedicated entry ("Intro") that
  opens the Intro sub-screen, instead of a raw toggle in the Appearance list.

## [6.4.28_DE-1.33.98] - 2026-08-18

### Fixed

- [DE] Intro splash now plays with correct colours and smooth fps. The frames
  were extracted with a limited-range BT.601 YUV→RGB formula while jcodec
  outputs full-range YUV420, which shifted/clamped every channel (the "wrong
  colours"). Re-extracted with the correct full-range matrix, downscaled to
  960×540, and the player now decodes each JPEG with Skia
  (`Image.makeFromEncoded`) instead of `ImageIO` + a pixel copy, so 30 fps
  stays smooth.

## [6.4.28_DE-1.33.97] - 2026-08-18

### Added

- [DE] "Show intro on startup" toggle also available in Settings → Appearance
  (same setting as System → Intro).

## [6.4.28_DE-1.33.96] - 2026-08-18

### Fixed

- [DE] Changing the app font now actually changes the whole interface: the
  selected font was only used in the App font picker preview, never applied to
  the theme. `AppTheme` now builds the Material typography from the selected
  `AppFont` (hoisted to the app root and passed to both the main window and
  the developer-tools window), so every text style updates immediately.

## [6.4.28_DE-1.33.95] - 2026-08-18

### Changed

- [DE] Full redesign of the Developer options screen: a prominent enable
  card, a live monitor card with real-time CPU / memory / GPU / network / thread
  / uptime tiles (respecting the Full vs Performance profile), display mode and
  profile as radio groups, and the overlay / title-bar switches grouped in a
  card. Functionality is unchanged (same settings, same persistence).

## [6.4.28_DE-1.33.94] - 2026-08-18

### Changed

- [DE] The startup intro is no longer a GIF: it now plays a sequence of
  full-color JPEG frames pre-extracted from the MP4 (`scripts/ExtractIntroFrames.java`).
  The GIF showed visible color banding (256-color limit); the JPEG sequence
  keeps the original quality while staying smooth and cross-platform (no native
  video decoder needed).

## [6.4.28_DE-1.33.93] - 2026-08-18

### Fixed

- [DE] Back navigation no longer bounces between repeated screens: navigating
  to the screen already on top is ignored (no duplicate stack entries), and
  sidebar roots now keep Home at the base so pressing back from Settings
  returns to Home instead of getting stuck.

## [6.4.28_DE-1.33.92] - 2026-08-18

### Added

- [DE] Animated intro splash screen (the bundled GIF) played once at startup,
  with click-to-skip and a toggle in Settings → System → Intro.
- [DE] Settings → System sub-menu: Developer options moved here (under
  System → Developer options) alongside the new Intro option.

### Removed

- [DE] Removed the changelog button from the About screen (still available in
  Settings → Updates).

## [6.4.28_DE-1.33.91] - 2026-08-18

### Removed

- [DE] Removed the standalone Stats screen (duplicate of VIVI Wrapped) and
  replaced its sidebar entry with a direct link to VIVI Wrapped.

## [6.4.28_DE-1.33.90] - 2026-08-18

### Added

- [DE] Completed translations for the Phase 10 port across all 47 languages:
  Listen Together (title, descriptions, room code, leave room, connected
  users), song recognition (recognize, listening, history, error), Stats /
  Charts / New releases / Mood & genres, comments, username, and the
  desktop-only Undo / Redo / Items labels.

## [6.4.28_DE-1.33.89] - 2026-08-18

### Added

- [DE] Artist page sub-tabs: Songs / Albums / Items (TabRow). Items loads
  the section's "see all" endpoint via `YouTube.artistItems`.
- [DE] Song recognition (Shazam): records the microphone, resamples to
  16 kHz mono, generates a Shazam fingerprint (pure-JVM port) and queries
  `shazamkit`. Recognitions are saved to a history list in Settings.
- [DE] Keyboard navigation: Ctrl+Z / Ctrl+Y undo/redo the screen history,
  and Backspace or Alt+Left go back.

## [6.4.28_DE-1.33.88] - 2026-08-18

### Added

- [DE] New Release albums screen (grid of the latest albums, from the
  sidebar).
- [DE] Charts screen (trending + top songs/videos sections, from the
  sidebar).
- [DE] Dedicated Mood & genres screen (full list instead of the Home preview).
- [DE] Stats screen (session listening time, tracks played and top songs,
  from the sidebar).
- [DE] Auto-playlist detail screens: Liked / Top / etc. now open as their
  own pushed screen (`AutoPlaylistScreen`) instead of being a Library filter.
- [DE] Listen Together screen: a JSON WebSocket client (same protocol as the
  mobile app) with create/join room, connected users, host join-request
  approval and a chat panel.

## [6.4.28_DE-1.33.87] - 2026-08-18

### Added

- [DE] Commit screen (Settings → Updates → Commits): lists the most recent
  commits of the selected update source branch (fork `vivi-music-de` or
  original `main`), with author, date, short SHA and avatar. Clicking a commit
  opens it on GitHub.

## [6.4.28_DE-1.33.86] - 2026-08-18

### Fixed

- [DE] The Queue swipe-to-play hint ("▶ Play") no longer stays visible on top
  of the song artwork: the hint (background + text) now fades in only while
  dragging right, so it is fully hidden when the row is idle.

## [6.4.28_DE-1.33.85] - 2026-08-18

### Fixed

- [DE] Player sync (seek bar + play/pause) now follows the peer even over a
  phone hotspot: a seek/play command that arrived while the desktop was still
  resolving its own stream was dropped (our own resolution decides when audio
  starts) and only recovered on the next 5s re-sync tick. The latest peer
  snapshot is now re-applied the moment our stream finishes resolving, so the
  slower resolution over a hotspot no longer leaves seek/play-pause unsynced.

## [6.4.28_DE-1.33.84] - 2026-08-18

### Added

- [DE] Stream cache now offers a "Forever" option past 60 minutes: the resolved
  stream URL is kept for the whole app session instead of expiring.

## [6.4.28_DE-1.33.83] - 2026-08-18

### Changed

- [DE] The player design variants now actually differ: Classic is the
  two-column layout, New is a single-column hero with a pill play button,
  V2 keeps two columns with the title overlaid on the artwork, and Expressive
  is a single-column hero with the title overlaid on the largest artwork.

## [6.4.28_DE-1.33.82] - 2026-08-18

### Changed

- [DE] UI density scale now also offers values above 100% (110, 120, 125, 130,
  140, 150, 180, 200) in addition to the existing 100/85/75/65/55%.

## [6.4.28_DE-1.33.81] - 2026-08-18

### Fixed

- [DE+APK] The "Sync VIVI volume" toggle is now part of the shared settings
  snapshot, so enabling/disabling it on either device reflects on the other.

## [6.4.27_DE-1.33.80] - 2026-08-18

### Changed

- [DE] The VIVI Wrapped card is now hidden from the Home screen by default.
  It can be re-enabled via Settings → VIVI Wrapped → "Show on Home".

## [6.4.27_DE-1.33.79] - 2026-08-18

### Added

- [DE] Settings → VIVI Wrapped sub-screen: the session listening-stats card
  now lives in its own settings sub-menu, like the mobile app. The Home card
  stays as a quick glance.
- [DE] Appearance now hosts the player personalization: a new "Player
  design" row (Material 3 style) opens the design / background / rotating
  thumbnail / mini-player style screen.

### Fixed

- [DE] Raw keys no longer appear as "code language" UI: 10 keys were missing
  from the desktop string table (`remove_from_queue`, `pause_search_history`,
  `pause_listen_history`, `quick_picks`, `search_history`, `listen_history`,
  `clear_search_history`, `clear_search_history_confirm`, `theme`, `ok`) and
  are now wired to the Android translations (fallback OK).
- [DE] Queue screen: the swipe-left remove hint ("✕ remove from queue")
  duplicated the row's X button; the hint was removed — the X is now the
  single remove control (swipe-left still works).
- [DE] Full player: add-to-playlist now sits under the song title, next to
  the Queue button; the duplicate header Queue shortcut and the old
  bottom-row buttons were removed.
- [DE] Startup volume guard: if the Windows master volume is muted, VIVI
  Music DE unmutes it and sets it to 0% so a paired mobile device can always
  control it (a muted master ignores volume writes).

### Removed

- [DE] Quick settings (Tune) button in the sidebar.

## [6.4.27_DE-1.33.78] - 2026-08-18

### Changed

- [DE] The stream resolution cache TTL is now configurable: a slider in
  Settings → Player & audio (1–60 minutes, default 10) controls how long a
  resolved stream URL is reused before the resolution chain runs again.
  (Resolution only — the audio decode/playback core is untouched.)

## [6.4.27_DE-1.33.77] - 2026-08-17

### Changed

- [DE] Stream resolution cache: resolved audio URLs are cached in memory for
  up to 10 minutes, so replaying or retrying a track doesn't re-run the whole
  resolution chain when we already have a valid stream. (Resolution only —
  the audio decode/playback core is untouched.)

## [6.4.27_DE-1.33.76] - 2026-08-17

### Added

- [DE] Integrations sub-screen (Settings → Integrations):
  - Discord Rich Presence over the local IPC pipe (Windows; toggle + your
    Discord application ID; shows the current track).
  - Last.fm scrobbling: enable toggle, session key field, now-playing
    update and auto-scrobble near the end of each track (credentials via
    the LASTFM_API_KEY / LASTFM_SECRET env vars, like mobile build config).
  Fully translated.

## [6.4.27_DE-1.33.75] - 2026-08-17

### Added

- [DE] Advanced lyrics: line-spacing slider (1.0–2.0, Settings → Lyrics)
  and a thumbnail with play/pause overlay on the Lyrics screen (port of the
  mobile advanced-lyrics controls). Fully translated.

## [6.4.27_DE-1.33.74] - 2026-08-17

### Added

- [DE] Quick settings popup: a Tune button at the bottom of the sidebar
  opens a compact panel with theme (System/Light/Dark), pure black toggle,
  accent swatches and a shortcut to the full Appearance settings (port of
  the mobile quick-settings shortcut). Fully translated.

## [6.4.27_DE-1.33.73] - 2026-08-17

### Added

- [DE] Local search history: recent searches appear as chips on the Search
  screen (saved on submit / suggestion click, max 12) with a clear button.
- [DE] Privacy sub-screen (Settings → Privacy): "Pause listen history"
  (hides the History screen from the sidebar) and "Pause search history"
  (stops saving new searches) toggles + "Clear search history".
  Fully translated.

## [6.4.27_DE-1.33.72] - 2026-08-17

### Added

- [DE] Home: "Quick Picks vs Last Listen" toggle (chip row) so only the
  chosen section shows, like on mobile.
- [DE] Home: "Randomize" button that shuffles the order of the home
  sections (persisted).
- [DE] Home: "VIVI Wrapped · This session" card with tracks played,
  listening time and top song of the current session. Fully translated.

## [6.4.27_DE-1.33.71] - 2026-08-17

### Added

- [DE] Sort chips in the Library (all tabs): A–Z / Z–A, plus "By artist"
  for the songs tab. Fully translated.

## [6.4.27_DE-1.33.70] - 2026-08-17

### Added

- [DE] Dynamic theme (Material You): the "Dynamic" accent swatch now reads
  the OS accent color instead of a fixed seed — Windows DWM accent, macOS
  accent (defaults), GNOME accent (gsettings) — with fallback to the default
  palette. Re-detected each time Dynamic is picked.

## [6.4.27_DE-1.33.69] - 2026-08-17

### Added

- [DE] Song swipe gestures in the Queue screen: swipe a row right to play
  it, swipe left to remove it from the queue (action hints appear behind
  the row while dragging). Fully translated.

## [6.4.27_DE-1.33.68] - 2026-08-17

### Added

- [DE] Mini-player styles: Standard / Apple / Outline / Pure black
  (Settings → Player & audio → Player design → Mini player), replacing the
  old Apple-only toggle. Fully translated.
- [DE] Swipe-to-expand on the mini player: drag it up to open the full
  player (with a drag handle hint at the top).

## [6.4.27_DE-1.33.67] - 2026-08-17

### Added

- [DE] Player design variants: Classic / New / V2 / Expressive (Settings →
  Player & audio → Player design), reworking the full-player layout and the
  Apple Music-style rounded mini-player variant. Fully translated.
- [DE] Player background styles: Gradient / Blur / Glow / Apple Music /
  Live mesh (Settings → Player & audio → Player design), animated behind
  the full player. Fully translated.
- [DE] Rotating artwork option (settings toggle): the album art rotates
  slowly while playing, like the mobile rotating-thumbnail option.

## [6.4.27_DE-1.33.66] - 2026-08-17

### Added

- [DE] Player slider styles: Slim / Squiggly / Wavy (Settings → Player & audio),
  applied to the seek bar and the volume slider via a custom `ViviSlider`.
  Fully translated.

## [6.4.27_DE-1.33.65] - 2026-08-17

### Added

- [DE] Screen transitions between navigations: Off / Fade / Slide (Settings →
  Appearance → Screen transitions), applied with `AnimatedContent` around the
  main screen switch. Fully translated.

## [6.4.27_DE-1.33.64] - 2026-08-17

### Added

- [DE] UI density scale (100 / 85 / 75 / 65 / 55 %) applied to the whole
  interface via a density override (Settings → Appearance → Density & grid),
  plus a custom adaptive grid item size (small / medium / large / extra
  large) used by the album / artist / playlist grids. Fully translated.

## [6.4.27_DE-1.33.63] - 2026-08-17

### Fixed

- [DE] The desktop no longer stays silent (appears in the Windows mixer but
  emits no sound) when a synced track change starts. The desktop held
  (`startPaused`) whenever the peer was still resolving, and the peer held for
  the desktop's own resolution, so both paused and neither ever started. The
  desktop now starts when the peer says it is playing and ignores the peer's
  play/pause echoes while it is still resolving its own stream; the phone keeps
  holding for the desktop and both resume together once the desktop is ready.

## [6.4.27_DE-1.33.62] - 2026-08-17

### Fixed

- [DE+APK] A track change initiated from the phone no longer plays ahead of
  the desktop while the desktop is still resolving its stream. The
  resolving/ready transition was being swallowed by the 1.5s echo-suppression
  window that runs after applying a remote snapshot, so the desktop's
  `isResolving=true` push never reached the phone. Resolving transitions now
  bypass echo suppression on both sides, so the phone holds while the desktop
  buffers and resumes the moment the desktop is ready (and vice versa).

## [6.4.26_DE-1.33.61] - 2026-08-17

### Fixed

- [APK] Restore swap is no longer silent on failure: the database target
  directory is created if missing (clean install), and if the staged
  settings/database copy fails the staged backup is kept and the error is
  logged (with a stack trace) so the restore can be retried on the next launch
  instead of the backup being deleted without being applied.

## [6.4.25_DE-1.33.61] - 2026-08-17

### Fixed

- [APK] The restore picker now accepts any file (`*/*`) so old `.backup` files
  created by the original 6.0.5 app (which have no registered MIME type) always
  appear in the file selector instead of being hidden/unselectable.

## [6.4.24_DE-1.33.61] - 2026-08-17

### Fixed

- [DE+APK] Playback start is now synchronized while a device is still resolving
  its stream. The desktop marks the snapshot as `isResolving` from the moment it
  starts resolving until audio actually flows (first position report), and the
  mobile marks it while ExoPlayer is `STATE_BUFFERING`. The receiver now
  prepares the queue but holds playback (instead of playing ahead of the peer),
  and `effectivePosition` no longer extrapolates a frozen position while the
  peer is resolving. This fixes the phone starting the track before the desktop
  had finished resolving/downloading.

## [6.4.23_DE-1.33.60] - 2026-08-17

### Fixed

- [APK] Update check from the fork source (`PiBOH/vivi-music`) now works:
  the updater extracts the mobile version from the combined desktop tag
  (`6.4.22_DE-1.33.60-nightly` → `6.4.22`) before comparing, and accepts the
  fork's `VIVIMusic-<version>-debug.apk` asset instead of only `vivi.apk`.

## [6.4.22_DE-1.33.60] - 2026-08-17

### Fixed

- [DE] Native Windows toast notifications now actually appear in the Action
  Center. The AUMID registration was failing with `0x80070057` because the
  `SHGetPropertyStoreFromParsingName` P/Invoke was missing the
  `GETPROPERTYSTOREFLAGS flags` parameter, and the `PROPVARIANT` was declared
  as a sequential struct instead of an explicit-layout class. Both are fixed,
  so the Start-menu shortcut gets its `System.AppUserModel.ID` correctly.

## [6.4.22_DE-1.33.59] - 2026-08-17

### Fixed

- [DE] Developer options network stats (down/up speed + total traffic) now show
  real values on non-English Windows. They were parsing the localized
  `netstat -e` output ("Byte" / "Ricevuti"/"Trasmessi" instead of "Bytes"),
  which never matched and left the values at "—". Replaced with
  `Get-NetAdapterStatistics` (culture-invariant property names).

## [6.4.22_DE-1.33.58] - 2026-08-17

### Fixed

- [DE+APK] Playback sync no longer "jumps back": explicit user seeks are now
  flagged and applied exactly on the peer (both directions, no tolerance),
  while the periodic drift-tic only catch up FORWARD. This stops the device
  that is slightly ahead (the leader) from being dragged back every 5s by the
  follower's stale position, which was the visible seekbar jump-back.

## [6.4.21_DE-1.33.57] - 2026-08-17

### Fixed

- [DE] The in-app update notification no longer auto-dismisses while it is
  showing the download progress bar; the timer pauses during a download and
  resumes after it finishes.

## [6.4.21_DE-1.33.56] - 2026-08-17

### Added

- [DE] A "Send test notification" button in Settings → Notifications so native
  notifications can be triggered on demand.

### Changed

- [DE] Native notification path now writes a diagnostic log to
  `~/.vivimusic/native-notify.log` (which branch is used, AUMID registration
  result, and PowerShell output) to help diagnose Windows toast issues.

## [6.4.21_DE-1.33.55] - 2026-08-17

### Changed

- [DE] The About "website" entry now points to the VIVI Music DE GitHub Pages
  site (`https://piboh.github.io/vivi-music/`).

### Website

- Made the site fully responsive for mobile (collapsible hamburger nav,
  stacking download rows / platform cards).
- Compact sticky footer (always pinned to the bottom of the viewport).
- Removed the Android APK download from the DE site; it now links to the
  upstream VIVI Music site, with credits to VIVIDH P ASHOKAN
  (`https://vivimusic.mkmdevilmi.workers.dev/`).

## [6.4.21_DE-1.33.54] - 2026-08-17

### Fixed

- [DE] Tracks restored from the persistent queue (or whose load failed earlier)
  now actually start on the first Play press: pressing play on a track whose
  stream is not loaded yet triggers a real resolution + load instead of a
  no-op `resume()` that silently did nothing.

## [6.4.21_DE-1.33.53] - 2026-08-17

### Added

- [DE] Windows native notifications now land in the **Action Center / notification
  history** via WinRT toasts (PowerShell helper). On a packaged Windows build the
  app registers an AppUserModelID by creating a Start-menu shortcut with the
  `System.AppUserModel.ID` property (inline C# `Add-Type` + shell property
  store), then shows `ToastGeneric` toasts with the VIVI Music DE logo.
  Clicking a toast launches the app with `--open=<section>` and opens the
  relevant screen (Updates / Developer options / Devices), bringing the window
  to the front; a file-based command mailbox forwards the request to an
  already-running instance. Non-Windows and unpackaged/dev builds keep the
  `SystemTray` balloon fallback.

## [6.4.21_DE-1.33.52] - 2026-08-17

### Changed

- [DE] `version.txt` is reorganized into a self-documenting six-line layout:
  mobile version / mobile version code / mobile channel, then DE version / DE
  version code / DE channel, with comment lines below explaining each field.
  `desktop/build.gradle.kts`, `AppInfo`, and the release/build workflows now
  read the new positions (DE version = line 4, DE channel = line 6, DE version
  code = line 5).

## [6.4.21_DE-1.33.51] - 2026-08-17

### Fixed

- [DE] Opening Appearance → Theme & colors, App font, or Canvas no longer
  crashes with "Vertically scrollable component was measured with an infinity
  maximum height constraints". Those sub-screens had their own
  `verticalScroll` nested inside the settings screen's scrollable scaffold; the
  inner scroll was removed so the content scrolls with the outer scaffold only.

## [6.4.21_DE-1.33.50] - 2026-08-17

### Added

- [APK] Restoring a backup now shows a confirmation dialog with the backup's
  file name, date, and the app version it was created from, before anything is
  applied.

## [6.4.20_DE-1.33.50] - 2026-08-17

### Added

- [APK] Restore now validates the backup's database before applying it (SQLite
  header magic + `PRAGMA integrity_check` + schema-version guard). A corrupt or
  incompatible backup fails with a clear "backup is corrupt" message instead of
  swapping in a bad file and crashing the app on the next launch.

## [6.4.19_DE-1.33.50] - 2026-08-17

### Fixed

- [DE+APK] Playback position no longer jumps back and forth between the two
  devices. The shared-clock offset used to extrapolate the live position was
  only measured 25 s after connecting and converged slowly via a running
  average, so during that window both devices extrapolated from raw local
  clocks and kept seeking each other back/forth by the clock skew. The first
  PING is now sent immediately on connect, the first measurement sets the
  offset directly, and a position is only timestamped once the offset is known
  (older relays fall back to the raw position instead of a skew-corrupted
  extrapolation).

## [6.4.18_DE-1.33.49] - 2026-08-17

### Added

- [DE] Complete port of the mobile Appearance sub-menu into three dedicated
  sub-screens: **Theme** (4-mode selector System/Light/Dark/Pure black, the
  full 21-color accent palette and a live preview card), **App font** (the five
  mobile fonts — System, Google Sans, Sans Flex, Outfit, Plus Jakarta Sans —
  bundled into the desktop resources with a live typography preview) and
  **Canvas** (enable toggle + source Auto / Apple Music / ViViMusic / Tidal,
  wired into the player's animated background). All new strings reuse the
  Android translations (47 languages).

## [6.4.18_DE-1.33.48] - 2026-08-17

### Fixed

- [DE+APK] Playlist changes made on the desktop now actually reach the phone.
  The mobile side was stamping the local "now" into `lastUpdateTime` when
  applying a remote playlist, so the next desktop rename/delete compared newer
  than an artificially-updated timestamp and was silently dropped by the
  last-write-wins check. The remote edit timestamp is now preserved, so create /
  rename / delete all propagate.

## [6.4.17_DE-1.33.48] - 2026-08-17

### Fixed

- [APK] Restoring an old backup (e.g. from the original 6.0.5) no longer crashes.
  The restore previously closed the shared Room database while the app's live
  queries were still running, which crashed with an uncaught "database is
  closed" exception. It now stages the backup to `filesDir/pending_restore`,
  exits, and swaps the settings + database in at startup (in `App.onCreate()`)
  before Room/DataStore are opened.

## [6.4.16_DE-1.33.48] - 2026-08-17

### Changed

- [APK] Mobile backups now use the `.vividroid.backup` extension (desktop keeps
  `.vivide.backup`), so the two editions' backup files are clearly
  distinguishable. Older `.backup` files are still listed and importable.

## [6.4.15_DE-1.33.48] - 2026-08-17

### Fixed

- [DE] The Inno Setup installer now actually launches the app when "Start VIVI
  Music DE" is checked on the final page. The `[Run]` entry was gated on both
  the final-page checkbox *and* a separate (unchecked) `launchafterinstall`
  task, so the app never started; the redundant task is removed and the
  final-page checkbox alone controls the launch.

## [6.4.15_DE-1.33.47] - 2026-08-17

### Changed

- [APK] Mobile backup files now use the `.vivide.backup` extension (manual and
  automatic) to match the desktop edition. Older `.backup` files are still listed
  and importable, so nothing is lost.

## [6.4.14_DE-1.33.47] - 2026-08-17

### Fixed

- [APK] The debug APK build (CI) no longer fails during resource merge: the new
  `sync_vivi_volume_desc` string used a bare apostrophe that aapt2 rejected as an
  "Invalid unicode escape sequence"; it is now escaped (`\'`) like the rest of the
  Android strings, so `assembleUniversalGmsDebug` completes again.

## [6.4.13_DE-1.33.47] - 2026-08-17

### Added

- [DE+APK] New "Sync VIVI volume" toggle (Settings → Devices and Settings →
  Player & audio, on both editions). When off, each device keeps its own
  in-app volume slider independent; the native OS (system) volume sync is
  unaffected.

### Fixed

- [DE] The seek slider no longer stays disabled or stuck at the end: the track
  duration is reported as soon as it is known (before the stream resolves) and
  the live position is clamped to the track length so it can't overshoot.
- [DE] Position sync no longer fights itself: the periodic re-sync tick only
  pushes when the position actually advanced, so a stalled/frozen player can't
  repeatedly drag the paired device back to the same point.

## [6.4.12_DE-1.33.46] - 2026-08-16

### Fixed

- [APK] Restoring a backup no longer crashes while choosing the file or when
  reopening the app: the archive is decompressed on a background thread and
  staged to temp files, then the settings + database are swapped in and the
  process is killed in a single synchronous block on the main thread. This
  removes the race where the UI queried the database after it was closed on a
  background thread (the crash introduced by the previous fix), while still
  deleting the WAL/SHM sidecars so the restored DB isn't corrupted on launch.

## [6.4.11_DE-1.33.46] - 2026-08-16

### Fixed

- [DE] All in-app notifications now auto-dismiss after the configured time
  (Settings → Notifications → In-app notification duration): the update
  banner and the developer-options-unlocked hint previously stayed on screen
  until dismissed manually, ignoring the setting.
- [DE] Native notifications keep a single persistent tray icon (created once
  with the VIVI Music DE logo) instead of adding/removing a temporary icon per
  notification, so the logo shows reliably and the icon is scaled with
  high-quality interpolation.

## [6.4.11_DE-1.33.45] - 2026-08-16

### Fixed

- [DE] LAN sync now works when the computer is connected to the phone's
  hotspot: the desktop advertises the address of the interface that actually
  routes to the phone (resolved via the outbound-route source address, then
  preferring Wi-Fi/wlan adapters) instead of the first site-local address,
  which on multi-homed machines was often a virtual adapter the phone could
  not reach. Start/stop of the relay is now serialized and the bound-port
  lookup is guarded, so rapid Stop→Start (or a failed bind) no longer throws
  and crashes the app — failures surface in the status line instead.

## [6.4.11_DE-1.33.44] - 2026-08-16

### Changed

- [DE] Manual backups now include the date and timestamp in their filename
  (`vivimusic-de_yyyyMMdd_HHmmss.vivide.backup`) instead of a fixed
  `vivimusic-de.vivide.backup`. Automatic backups already carried the
  timestamp, and the stored-backups list shows it as `yyyy-MM-dd HH:mm`.

## [6.4.11_DE-1.33.43] - 2026-08-16

### Fixed

- [APK] Restoring a backup no longer freezes and then corrupts the app: the
  restore (settings + DB copy) now runs off the main thread instead of blocking
  the UI, and the WAL/SHM sidecar files are deleted before overwriting the
  Room database, so a restored `song.db` is no longer mixed with stale journal
  frames (which corrupted the DB on the next launch and required an
  uninstall/reinstall). Playback is stopped before the database is touched.

## [6.4.10_DE-1.33.43] - 2026-08-16

### Added

- [DE+APK] Selectable update source: pick whether update checks read from the
  original repo (`vivizzz007/vivi-music`) or the PiBOH fork
  (`PiBOH/vivi-music`). Desktop defaults to the fork, mobile defaults to the
  original. The source is also used for the download/notification URLs.

## [6.4.9_DE-1.33.42] - 2026-08-16

### Added

- [DE] The crash/error dialog now has a "Copy error" button alongside "OK":
  it copies the full message + stack trace to the clipboard. A global
  uncaught-exception handler replaces the default AWT "Error" dialog (OK only).

## [6.4.9_DE-1.33.41] - 2026-08-16

### Changed

- [DE] Backups (manual and automatic) now use a single `.vivide.backup` file
  that contains everything (settings + playlists + account + library). Old
  `.backup` files are still importable.

### Fixed

- [DE] "Restart now" after restoring a backup now actually relaunches the app:
  it releases the single-instance lock, starts a new instance (the jpackage
  launcher when packaged, `java -cp … MainKt` in dev), and then exits.

## [6.4.9_DE-1.33.40] - 2026-08-16

### Fixed

- [DE] Native system notifications now use the real VIVI Music DE logo as their
  icon instead of a placeholder glyph. `logo_vmde.png` is bundled under
  `images/` and loaded for the tray icon (scaled, with a fallback).

## [6.4.9_DE-1.33.39] - 2026-08-16

### Added

- [DE+APK] Repeat mode (off / all / one) and shuffle now sync in real time
  between the phone and the desktop, both ways, like the rest of the playback
  state. `PlaybackSnapshot` carries `repeatMode` ("OFF"/"ALL"/"ONE") and
  `isShuffle`; each side applies them on receive and pushes them on change.

## [6.4.8_DE-1.33.38] - 2026-08-16

### Fixed

- [DE+APK] Queue sync now follows last-write-wins like playlists: `PlaybackSnapshot`
  carries a `queueUpdatedAt` timestamp (shared relay-time frame) and each side
  only replaces its queue when the remote edit is newer, so a mobile edit can't
  be overwritten by an older desktop queue (and vice versa). Older peers
  (`queueUpdatedAt = 0`) still apply unconditionally for compatibility.

## [6.4.7_DE-1.33.37] - 2026-08-16

### Fixed

- [DE] The update notification and the Updates screen now share a single
  download state (`UpdateState`), so downloading/opening an installer from one
  is reflected in the other (and vice versa). The notification no longer
  re-offers a download for an installer the Updates screen already fetched.

## [6.4.7_DE-1.33.36] - 2026-08-16

### Added

- [DE] Full backup & restore, ported from mobile: a backup now includes
  settings, playlists, account/login and library (ZIP with `settings.json` +
  `playlists.json`), and old single-JSON `.backup` files are still importable.
- [DE] Automatic backups: optional weekly backup and an optional "backup before
  update" that runs automatically before opening the installer. Automatic
  backups are stored under `~/.vivimusic/backups/` and can be restored or
  deleted from Settings → Backup.

### Changed

- [DE] Developer options screen reorganized into clear sections (display,
  monitoring profile, overlay behaviour, title bar) separated by dividers.

## [6.4.7_DE-1.33.35] - 2026-08-16

### Fixed

- [DE] Settings (e.g. where notifications are shown) no longer get forgotten on
  restart or update: `DesktopSettings` now saves through an atomic
  read-modify-write (`update`) instead of `save(load().copy(…))`, which could
  race between the UI thread and the device-sync IO coroutines and silently
  drop the value the user had just changed.

## [6.4.7_DE-1.33.34] - 2026-08-16

### Fixed

- [DE] The player seek slider no longer snaps to the start or the end: it is
  disabled until the track duration is known (so it can't degenerate into a
  0..1 range) and, while dragging, the live playback position is ignored so it
  can't fight the drag and yank the thumb back.

## [6.4.7_DE-1.33.33] - 2026-08-16

### Fixed

- [DE+APK] The two devices no longer unpair when one screen turns off: while
  paired, the Android app keeps the screen on (`FLAG_KEEP_SCREEN_ON`) and the
  desktop keeps the display/system awake (kernel32 `SetThreadExecutionState` on
  Windows, `caffeinate` on macOS). This stops the OS from sleeping the display
  and suspending the network, which was tearing down the sync socket.

## [6.4.6_DE-1.33.32] - 2026-08-16

### Changed

- [DE] The CI debug build now restores a persistent debug keystore from the
  `DEBUG_KEYSTORE` GitHub secret instead of generating a fresh key on every
  run, so the debug APK keeps the same signature and can be installed over the
  previous build without uninstalling first. When the secret is absent the
  workflow still falls back to generating a fresh key, so CI never breaks.

## [6.4.6_DE-1.33.31] - 2026-08-16

### Fixed

- [APK] Scanning a QR code now first disconnects and un-pairs an existing
  desktop connection, so the new code can pair to a (possibly different)
  desktop from a clean slate.

## [6.4.5_DE-1.33.31] - 2026-08-16

### Changed

- [DE] On Windows the updater now prefers the Inno Setup `.exe` installer over
  the `.msi` (lighter and more user-friendly). The `.msi` remains the fallback
  when a release has no `.exe`.

## [6.4.5_DE-1.33.30] - 2026-08-16

### Fixed

- [DE] The Updates screen no longer offers to re-download an installer that is
  already on disk: it now detects the previously-downloaded file for the
  available version and shows "Open installer" directly (the update banner
  already behaved this way).

## [6.4.5_DE-1.33.29] - 2026-08-16

### Added

- [DE] Notification history: every notification (in-app and native) is recorded
  and can be reviewed from Settings → Notifications → Notification history, with
  a "Save notification history" toggle and a "Clear history" action.
- [DE] Configurable in-app notification auto-dismiss (3/5/10/15/30 seconds,
  default 5s) in Settings → Notifications.

## [6.4.5_DE-1.33.28] - 2026-08-16

### Changed

- [DE] Windows system volume now drives the **master** volume (the speaker icon
  in the tray) via WASAPI `IAudioEndpointVolume` instead of WinMM, which only
  moved the per-app `VIVIMusic` mixer entry. The app's own session is now
  pinned to 100% so the mixer never quiets VIVI under the master. Sync with
  the phone remains bidirectional (channel `systemVolume`).

## [6.4.5_DE-1.33.27] - 2026-08-16

### Fixed

- [DE] The Linux `.deb` now installs on Debian: jpackage auto-detected
  dependencies on ubuntu-latest and emitted Ubuntu's `t64`-renamed package
  names (e.g. `libasound2t64`, `libglib2.0-0t64`) that don't exist on Debian
  Bookworm. A post-build step rewrites them to `<name> | <name>t64`
  alternatives so apt picks whichever name the distro actually provides.

## [6.4.5_DE-1.33.26] - 2026-08-16

### Changed

- [DE] Clearer updater wording: the update button now reads "Check for
  available updates" (instead of the Android toggle's "Automatically check for
  updates") and "Open installer" now reads "Close Vivi and open installer",
  matching what the button actually does. Updated across all languages.

## [6.4.5_DE-1.33.25] - 2026-08-16

### Fixed

- [DE] Critical startup crash on Windows: the WinMM binding looked up
  `waveOutOpenW`, but `winmm.dll` exports the function with **no A/W suffix**
  (it takes no string argument, so `waveOutOpenW`/`waveOutOpenA` exist only as
  C header macros). JNA threw "Error looking up function 'waveOutOpenW'" and the
  app crashed. Restored the correct `waveOutOpen` symbol and guarded every
  native call so a missing symbol can never crash the app again.

## [6.4.5_DE-1.33.24] - 2026-08-16

### Changed

- [DE] Translation quality pass: filled in the remaining desktop-only keys
  (device sync, updates, player basics) that were still falling back to
  English, and corrected translations whose Android mapping had a different
  (longer or wrong-context) meaning — e.g. "Check for updates" is now a short
  button label instead of "check automatically…", "Error" no longer reads
  "unknown error", and CPU/GPU keep their short technical form.

## [6.4.5_DE-1.33.23] - 2026-08-16

### Fixed

- [DE] Windows Inno Setup installer now always shows the "Select Destination
  Location" page so the install path is visible (and editable), matching the
  MSI installer.

## [6.4.5_DE-1.33.22] - 2026-08-16

### Fixed

- [DE] Native (OS) volume now actually syncs on Windows: the WinMM call used
  `waveOutOpen`, which is a macro — `winmm.dll` only exports `waveOutOpenW`/
  `waveOutOpenA` — so `Native.load` failed and every native volume read/write
  silently no-opped. The default wave device is now opened via `waveOutOpenW`
  (with `StdCallLibrary`).
- [DE+APK] Volume pushes (in-app `volume` and native `systemVolume`) are now
  retried instead of being silently dropped: both sides poll volume and only
  mark it as pushed once the snapshot is actually sent, so a push that lands in
  the echo-suppression window is re-sent on the next tick. The mobile side is
  echo-guarded per-field so an applied remote value isn't bounced back.

## [6.4.4_DE-1.33.21] - 2026-08-16

### Added

- [DE] Single-instance guard: launching the app while another instance is
  already running (or still starting) exits immediately, always keeping the
  first instance that started.
- [DE] Notifications now cover **all** app notifications, not just updates:
  update available, device paired/unpaired and developer-options unlocked all
  route through the chosen notification mode (main window vs native). Native
  system notifications are marked "experimental".

### Changed

- [DE] Completed all remaining desktop-only translations (developer options and
  backup/restore strings) across all 47 supported languages — every key now has
  a real translation instead of an English fallback.
- [DE] `Localization.kt` is now emitted as one top-level function per language
  (instead of a single giant `mapOf`) to stay under the JVM 64KB `<clinit>`
  limit that caused a "Method too large" compiler error.

## [6.4.4_DE-1.33.20] - 2026-08-16

### Fixed

- [DE] Seek bar: decoded position is now reported at ~10 fps (throttled from
  ~43 fps) so the player seek slider no longer fights the constant frame-by-frame
  updates — it stays smooth and can be dragged to any position instead of
  sticking at the start/end.
- [DE] In-app (VIVI) volume sync now also pushes when nothing is playing, and
  uses a per-field echo guard (mirroring the OS-volume loop) so a local change
  is no longer silently dropped by the generic echo-suppression window.
- [DE] Windows OS volume sync: `waveOutGetVolume`/`waveOutSetVolume` were
  called with the `WAVE_MAPPER` constant as if it were an open handle, which
  made every call fail (so Windows native volume never synced). The default
  wave device is now opened first via `waveOutOpen` before reading/writing
  volume.

## [6.4.4_DE-1.33.19] - 2026-08-16

### Added

- [DE] New "Notifications" settings sub-menu: update notifications can now be
  shown either in the main window (in-app, default) or as a native system
  notification (`java.awt.SystemTray`, best-effort across OS).

## [6.4.4_DE-1.33.18] - 2026-08-16

### Added

- [DE] "Add to playlist" is now also available on the full Player screen
  (secondary actions) and on every row of the Queue screen.

## [6.4.4_DE-1.33.17] - 2026-08-16

### Added

- [DE] A dedicated "Add to playlist" button on every song row (Home, Search,
  Album, Artist, Playlist, Library), alongside the ⋮ context menu — no more
  hiding the action behind the menu.

## [6.4.4_DE-1.33.16] - 2026-08-16

### Fixed

- [DE] Seek slider couldn't be dragged to the middle: the track duration is now
  taken from the player response (`videoDetails.lengthSeconds`) and reported
  immediately, so the slider has a correct range (previously the AAC-derived
  fallback could be 0/wrong and the slider only landed on start or end).

### Changed

- [DE+APK] Device-sync volume now uses two separate channels: the in-app player
  volume (mobile slider <-> desktop slider, pixel-synced) and the native OS
  system volume (Android STREAM_MUSIC <-> desktop OS volume). The desktop reads
  and writes its OS volume via WinMM (Windows), `pactl`/`amixer` (Linux) and
  `osascript` (macOS), all best-effort and guarded.

## [6.4.3_DE-1.33.15] - 2026-08-16

### Fixed

- [DE] Crash ("layouts are not part of the same hierarchy") when interacting
  with any dropdown or dialog (update-check frequency, playlist delete, …).
  Root cause was the global `SelectionContainer`: popup components
  (`DropdownMenu`, `AlertDialog`) inherit its selection registrar and crash on
  pointer events (Compose CMP-2326). The global wrapper is removed; targeted
  selectable text is kept for the player error detail and the pairing code.

## [6.4.3_DE-1.33.14] - 2026-08-16

### Fixed

- [DE] Crash ("layouts are not part of the same hierarchy") when confirming a
  playlist deletion. The confirmation dialog now dismisses first and the row
  removal is deferred to the next frame, so the playlist list no longer
  reflows while the dialog window is being torn down.

## [6.4.3_DE-1.33.13] - 2026-08-16

### Fixed

- [DE] Apostrophes now render correctly everywhere instead of showing a
  literal `\'`. The desktop localization generator now decodes Android's
  `\'` resource escape (plus `\n`/`\t`/`\"`/`\\`) into real characters before
  re-encoding them as Kotlin string literals.

## [6.4.3_DE-1.33.12] - 2026-08-16

### Changed

- [DE] GitHub release titles now use the `Vivi Music <mobile>_DE <desktop>`
  format (e.g. `Vivi Music 6.4.3_DE 1.33.12`) instead of `Vivi Music DE
  <mobile>_DE-<desktop>`. Tags remain unchanged.

## [6.4.3_DE-1.33.11] - 2026-08-16

### Fixed

- [DE] Crash when changing the update-check interval: selecting an option in
  the frequency dropdown no longer throws "layouts are not part of the same
  hierarchy". The popup is now dismissed before the interval state (which
  reflows that row) is applied.

## [6.4.3_DE-1.33.10] - 2026-08-16

### Fixed

- [DE+APK] Device-sync regression: a transient network drop (or a socket
  reconnecting) no longer tears down a healthy pairing. Both relays now wait a
  15-second grace period for the device to reconnect before unpairing, and only
  the device's live socket triggers the unpair. Closing an app still un-pairs
  the other side (after the grace period).

### Note

- The cloud relay needs a redeploy of `sync-server/server.js` for this to apply
  over `wss://`; the LAN relay is fixed immediately.

## [6.4.3_DE-1.33.9] - 2026-08-16

### Added

- [DE] Backup & restore sub-menu in Settings: export the desktop settings to a
  `.backup` file (native save dialog) and import them back (native open
  dialog). Importing preserves the device id and first-launch date, drops any
  stale pairing, and prompts a restart to apply.

## [6.4.3_DE-1.33.8] - 2026-08-16

### Changed

- [DE] The GitHub logo in About → Community is now a vector icon (ported from
  the mobile app) instead of a static PNG, so it tints with the accent color
  and adapts to dark/light mode.

## [6.4.3_DE-1.33.7] - 2026-08-16

### Changed

- [DE+APK] Closing either app (mobile or desktop) now un-pairs both devices.
  The relays (cloud `sync-server` and the desktop LAN relay) detect the socket
  close, clear the pair, and tell the still-open peer it is no longer paired,
  so it stops showing "paired" for a peer that is gone.

### Note

- The cloud relay needs a redeploy of `sync-server/server.js` for this to take
  effect over `wss://`; the LAN relay works immediately.

## [6.4.3_DE-1.33.6] - 2026-08-16

### Fixed

- [DE+APK] Volume now syncs as the **system** volume: raising/lowering the
  Android volume (rocker or player bar) drives the desktop volume and vice
  versa, and the change is pushed immediately (no longer only on the periodic
  re-sync tick). This also fixes the volume bar position not following on the
  other device while the audible level did.

## [6.4.2_DE-1.33.6] - 2026-08-16

### Added

- [DE+APK] The desktop QR code now embeds the relay address **and** the current
  6-digit pairing code (`vivimusic://pair?addr=…&code=…`). Scanning it on the
  phone auto-fills both the server URL and the code, so you only verify the code
  and tap Pair. Plain `ws://` URLs still work for manual entry.

## [6.4.1_DE-1.33.5] - 2026-08-16

### Added

- [DE] The player now shows its load state while a track starts: "Resolving
  audio…" then "Downloading…" with a spinner, in both the full player and the
  mini-player, so you can tell it's working instead of appearing frozen.

## [6.4.1_DE-1.33.4] - 2026-08-16

### Fixed

- [DE] Playback now retries automatically: when the stream fails to resolve or
  download (e.g. a stale googlevideo 403), the player rotates the guest
  identity, re-resolves a fresh stream URL and retries up to 3 attempts before
  surfacing the error.

## [6.4.1_DE-1.33.3] - 2026-08-16

### Changed

- [DE] Completed the translations for the playlist and song-menu strings
  (rename / delete playlist / confirmation / empty / not-found / song count /
  like / library / share, …) across all 47 supported languages. The
  delete-playlist confirmation now shows the desktop message instead of the
  Android "Really delete … %s" template.

## [6.4.1_DE-1.33.2] - 2026-08-16

### Added

- [DE] Full song context menu (⋮): like / unlike, add to / remove from library,
  add to playlist and share (copies the YouTube Music link to the clipboard).
  Like and library actions use the signed-in YouTube account; a non-invasive
  snackbar confirms clipboard copies.

## [6.4.1_DE-1.33.1] - 2026-08-16

### Added

- [DE] Drag-to-reorder inside the playlist detail screen (drag the ⠿ handle);
  the new order is saved and synced like any other playlist edit.

## [6.4.1_DE-1.33.0] - 2026-08-16

### Added

- [DE] Local playlist system: create / rename / delete playlists, add songs to
  a playlist from any song row (Home, Search, Library, Album, Artist, Playlist
  and History), and a per-playlist detail screen with play-all and per-song
  remove.
- [DE+APK] Playlists now sync between the desktop and the phone over the
  device-sync channel: the full playlist (name + ordered songs) plus a
  per-playlist edit timestamp is shared, and edits are merged with
  last-write-wins. The most recently updated copy of each playlist wins,
  deletions propagate as tombstones, and a change made on either device appears
  on the other.

### Changed

- [DE] The sidebar gains a "Playlists" entry that opens the local playlist list.

## [6.4.0_DE-1.32.2] - 2026-08-16

### Fixed

- [DE] The player's seek slider could only land on the start or the end: the
  track duration was reported as 0 because YouTube's fragmented MP4 has an empty
  `mdhd` (jcodec returns `totalDuration == 0`). The duration is now derived from
  the decoded AAC sample count, so the slider spans the whole track and seeking
  works anywhere.
- [DE] Seeking while paused now stays paused instead of forcing playback to
  resume.
- [DE] Stale or truncated cached audio files are now detected and re-downloaded,
  so a leftover `.m4a` from an interrupted download no longer decodes into
  silence.

## [6.4.0_DE-1.32.1] - 2026-08-16

### Fixed

- [DE] "Start LAN server" now retries the pairing-code request until the relay
  answers, so the 6-digit code is generated automatically and reliably right
  after starting the server.

## [6.4.0_DE-1.32.0] - 2026-08-16

### Added

- [DE] Developer options "Title bar only" display mode (stats only in the
  window title, no overlay or separate window).
- [DE] Queue entry in the sidebar (opens the Queue screen directly).
- [DE] "View changelog" button in Settings → Updates.
- [DE+APK] Volume sync: the volume slider now syncs between the two devices.

### Changed

- [DE] Player layout is now two columns: a smaller artwork on the left and the
  seek bar + controls + volume on the right.
- [DE] Clicking the mini-player toggles the full player (open / go back).
- [DE] The About screen's GitHub row now uses the correct GitHub logo.
- [DE] README: smaller logo and @PiBOH added to Special Thanks.

### Fixed

- [DE] Stream resolution is more resilient: it retries transient failures and
  throttles guest-session refreshes so a track can start even with no paired
  device.

## [6.3.0_DE-1.31.0] - 2026-08-15

### Added

- [DE+APK] Periodic re-sync tick: while a track is playing, the position is
  re-pushed every 5 seconds so the paired device auto-corrects drift
  (buffering / clock skew) instead of waiting for the next seek/play/track
  event. A 250 ms tolerance skips near-no-op seeks so the correction doesn't
  glitch the audio.

## [6.2.5_DE-1.30.3] - 2026-08-15

### Changed

- [DE] The Changelog screen now uses a vertical version selector (left list of
  version buttons, like the mobile chips but top-to-bottom) with the selected
  version's details in a pane on the right, instead of stacking every version
  in one long scroll.

## [6.2.5_DE-1.30.2] - 2026-08-15

### Changed

- [DE] The About screen now shows real thumbnails: the developer's avatar
  (`author.png`) instead of the `< >` icon, and the GitHub logo on the
  GitHub Repository row (bundled from `[DE]_images/`).

## [6.2.5_DE-1.30.1] - 2026-08-15

### Changed

- [APK] The Devices entry in Settings now uses the same phone+monitor
  "devices" icon as the desktop edition, instead of the circular sync arrows.

## [6.2.4_DE-1.30.1] - 2026-08-15

### Fixed

- [DE+APK] Both sides now show the paired device's name: the Android Devices
  screen displays the desktop's machine name, and the desktop Device sync
  section displays the phone's make/model. The desktop now advertises its real
  hostname instead of the generic "Desktop", and the peer name is restored from
  the peer's snapshot on reconnect (not just at pairing time).

## [6.2.3_DE-1.30.0] - 2026-08-15

### Added

- [DE] Developer options improvements: the "Developer options" entry is now
  always visible in Settings (showing "Disabled" until enabled), and can be
  unlocked either by tapping the About "Version code" seven times or from that
  screen. Once unlocked, a non-invasive banner points to the settings screen.
  New options: a display profile (Full vs Performance — CPU/RAM/GPU only), a
  "movable overlay" toggle (drag the overlay anywhere on the main window,
  on by default), and a "show in title bar" toggle that puts live CPU/RAM
  usage in the window title.

### Fixed

- [DE] Starting the LAN server now reliably auto-generates the pairing code:
  the code request waits for the local relay connection to be established
  instead of racing the relay startup (which could leave the code ungenerated).

## [6.2.3_DE-1.29.0] - 2026-08-15

### Added

- [DE+APK] Precise, instant player sync between the desktop and the phone.
  Seeking now pushes the new position immediately (both directions) and the
  receiver applies it as a lightweight in-place seek instead of restarting the
  stream, so the two players stay aligned to the second. Playback positions
  now carry a timestamp and both devices estimate their clock offset to the
  relay (PING/PONG), so the live position is extrapolated during playback
  without clock-skew drift.

## [6.2.2_DE-1.28.6] - 2026-08-15

### Fixed

- [DE] Installing both the MSI and the Inno Setup EXE no longer leaves two
  "VIVI Music" entries in "Apps & features": the Inno Setup installer now
  uninstalls any previously-installed jpackage MSI of the app before copying
  its files, so a single uninstall entry remains.

## [6.2.2_DE-1.28.5] - 2026-08-15

### Fixed

- [DE] **Critical:** the packaged app still showed "Failed to launch JVM" at
  startup even after bundling the management modules, because `SystemMonitor`
  read a field before its initializer ran (a Kotlin forward-reference) and
  threw a NullPointerException during `DeveloperOptions.load()`. Fixed the
  declaration order and made dev-tools initialization non-fatal.

## [6.2.2_DE-1.28.4] - 2026-08-15

### Fixed

- [APK] The Android APK now reports version `6.2.2` (it was still showing
  `6.2.1`): `app/build.gradle.kts` `versionName`/`versionCode` were out of
  sync with `version.txt`.

## [6.2.2_DE-1.28.3] - 2026-08-15

### Fixed

- [DE] **Critical:** the packaged app no longer fails to start with
  "Failed to launch JVM". The dev tools (CPU/RAM/thread stats) use
  `java.lang.management` and `com.sun.management`, but jlink was bundling only
  the default modules; those two modules are now declared so the packaged
  runtime includes them.

## [6.2.2_DE-1.28.2] - 2026-08-15

### Fixed

- [DE] The Changelog screen now lists every version vertically (newest first)
  in a single scrollable list, so older versions are reachable with the mouse
  wheel — the previous horizontally-scrolling version chips were unusable on
  desktop.

## [6.2.2_DE-1.28.1] - 2026-08-15

### Fixed

- [DE] Audio playback no longer fails with "HTTP 403 downloading audio": the
  desktop now keeps a fresh guest `visitorData` (like the Android app) and
  rotates it once when YouTube flags the request as a bot, so the googlevideo
  CDN stops rejecting the resolved stream URLs.

## [6.2.2_DE-1.28.0] - 2026-08-15

### Added

- [DE] The Updates screen now checks for updates automatically every time it
  is opened, and in the background at a configurable interval (manual only,
  6 hours, 12 hours, 24 hours, 3 days or 7 days), selectable in
  Settings → Updates.

### Fixed

- [DE] "Download" in the Updates screen no longer opens the browser instead of
  downloading in-app: the updater now picks the newest release that actually
  ships an installer for your OS (skipping releases whose build for your
  platform is missing) and only falls back to the release page when no
  installer exists. Nightly/alpha/beta/rc builds now include pre-releases by
  default so they can see updates without toggling the option.

## [6.2.2_DE-1.27.1] - 2026-08-15

### Fixed

- [DE] Content no longer gets clipped when the window is resized smaller: the
  Album/Artist/Playlist headers now shrink their title/artist text (ellipsis)
  instead of overflowing, and the LAN pairing screen ellipsizes and constrains
  the relay address next to the QR code.

## [6.2.2_DE-1.27.0] - 2026-08-15

### Added

- [DE] Developer options: tap the About "Version code" seven times to enable
  them. Once enabled, a new "Developer options" settings screen lets you show
  live CPU, RAM and network stats (download/upload speed + total traffic), the
  GPU device, thread count, uptime, OS/Java info and the paired phone
  name/model — either as a non-invasive collapsible overlay in the main window
  or in a dedicated window.

## [6.2.2_DE-1.26.4] - 2026-08-15

### Fixed

- [DE] Audio playback no longer fails with "HTTP 403 downloading audio": the
  desktop resolver stopped using the `WEB`/`WEB_REMIX` clients (their
  googlevideo URLs require a PoToken the desktop cannot generate) and now uses
  only PoToken-free clients (added `VISIONOS` and `IOS_MUSIC`). It also only
  applies the n-parameter throttle transform to web clients, so
  Android/iOS/VisionOS stream URLs are no longer corrupted into a 403.

## [6.2.2_DE-1.26.3] - 2026-08-15

### Fixed

- [DE] The About screen "Version code" was derived from the SemVer
  (`1.26.0` → `12600`), which looked like a huge number. It is now an explicit
  monotonic counter stored in `version.txt` (line 4) that tracks the number of
  DE releases (currently 57).

## [6.2.2_DE-1.26.2] - 2026-08-15

### Changed

- [DE] Starting the LAN server now automatically generates a pairing code, and
  the code + "Generate code" button are shown to the right of the QR code
  instead of below it.

## [6.2.2_DE-1.26.1] - 2026-08-15

### Fixed

- [DE+APK] Device pairing is now kept in sync across both devices: unpairing
  from the phone or the desktop unpairs the other side, and stopping the LAN
  server notifies the phone to unpair too. Reconnecting to a relay that no
  longer knows the pair (for example after the desktop is restarted) now clears
  the stale "paired" state instead of leaving it stuck.

## [6.2.1_DE-1.26.0] - 2026-08-15

### Added

- [DE] Redesigned the About screen to mirror the mobile layout: centered
  title + version/channel badge, a Developer section (PiBOH — lead developer
  of the DE edition — with website link), a Community section (GitHub repo +
  Telegram), and an App info section showing the first-launch date (not the
  last-update install date), the numeric version code and the GPL-3.0 license
  link.

## [6.2.1_DE-1.25.0] - 2026-08-15

### Added

- [DE+APK] Library sync over the device-sync channel: the mobile app now
  observes its library (liked songs, bookmarked albums/artists/playlists) and
  pushes a `LibrarySnapshot` whenever it changes; the desktop receives,
  persists and exposes it (and pushes its own).

## [6.2.1_DE-1.24.2] - 2026-08-15

### Fixed

- [DE] "HTTP 403 downloading audio" is now far more robust: the resolver
  returns an ordered list of candidate stream URLs (NewPipe plus every
  innerTube client), and the player tries them in order — retrying without the
  `Range` header when a request is refused — instead of giving up after the
  first URL. NewPipe URLs now use the decrypted `getUrl()` result with the
  n-param transform applied.

## [6.2.1_DE-1.24.1] - 2026-08-15

### Changed

- [DE] The update notification now offers "Open installer" (instead of
  downloading again) when the installer for that version is already present
  in the downloads folder.

## [6.2.1_DE-1.24.0] - 2026-08-15

### Added

- [DE] A non-invasive banner now appears when a newer release is available,
  with "Install now" (downloads and launches the installer) and a dismiss
  button. It is shown once per new version.

## [6.2.1_DE-1.23.0] - 2026-08-15

### Added

- [DE] The settings sub-screens now carry real functionality instead of being
  empty shells:
  - **Appearance**: pure black background toggle (true black in dark mode).
  - **Player & audio**: audio quality (Auto / High 256kbps / Low 128kbps,
    wired to the itag 141/140 picker), "remember shuffle and repeat" across
    restarts, and "persistent queue" (the queue is saved and restored between
    sessions).
  - **Lyrics**: adjustable lyrics text size.

## [6.2.1_DE-1.22.1] - 2026-08-15

### Fixed

- [DE] Audio download no longer fails with "HTTP 403 downloading audio".
  googlevideo ties a stream URL to the client that requested it, so the player
  now downloads with the same User-Agent used to resolve the URL (and a
  `Range: bytes=0-` header), instead of a fixed browser UA that YouTube
  rejected.

## [6.2.1_DE-1.22.0] - 2026-08-15

### Added

- [DE] Search now has mobile-style filter chips (All / Songs / Videos / Albums /
  Artists / Playlists) backed by the innerTube filtered search, plus live query
  suggestions as you type.
- [DE] Library tabs now use Material 3 filter chips and the Songs tab gains a
  "Shuffle all" action.
- [DE] Album and Playlist screens gain a "Shuffle" action next to "Play all".

## [6.2.1_DE-1.21.0] - 2026-08-15

### Added

- [DE] Settings are now organized into mobile-style sub-screens (Language,
  Updates, Appearance, Player & audio, Account, Devices, Content, Lyrics,
  Privacy, Storage, About) instead of one long scrollable page.
- [DE] New Content sub-screen: pick the YouTube content language and region
  (innerTube `hl`/`gl`), applied live and persisted across restarts.
- [DE] New Lyrics sub-screen with a "synced lyrics" toggle that enables or
  disables line-by-line highlighting.
- [DE] New Privacy sub-screen to clear the session, cache and downloaded
  installers.

## [6.2.1_DE-1.20.1] - 2026-08-15

### Changed

- [DE] The auto-release no longer builds the APK itself: it waits for the
  existing "CI & Debug Build" (`build.yml`) run on the same commit and attaches
  its debug APK artifact to the desktop release. The dedicated
  `release-mobile.yml` build workflow was removed.

## [6.2.1_DE-1.20.0] - 2026-08-15

### Fixed

- [DE] Audio playback now works. YouTube serves its `audio/mp4` streams as
  fragmented MP4 (DASH fMP4, `ftyp` brand "dash"), whose samples live in
  `moof`/`trun` boxes instead of the `moov` sample table — which `jaad`'s
  `MP4Container` demuxer does not understand, so every track failed with
  "No audio frames to decode". The player now walks the fragments directly
  with jcodec and decodes with the bundled jaad AAC decoder.
- [DE] The stream resolver now prefers AAC-LC (codec `mp4a.40.2`, itag 140/141)
  over HE-AAC/SBR (`mp4a.40.5`, itag 139), which jaad cannot decode ("FIL
  element overread").
- [DE] Language sync with the mobile app now maps the differing locale codes
  (mobile `no`/`pt-PT`/`zh-CN`/`zh-TW` ↔ desktop `nb`/`pt`/`zh-rCN`/`zh-rTW`),
  so changing the language on one device is correctly reflected on the other.

### Added

- [DE] All text (errors, options, settings, LAN server details, etc.) is now
  selectable and copyable across the whole desktop app.

## [6.2.1_DE-1.19.3] - 2026-08-15

### Changed

- [DE] APK delivery for desktop releases: a dedicated workflow
  (`release-mobile.yml`, a copy of the mobile CI adapted for `vivi-music-de`)
  builds and signs the GMS + FOSS APKs on this branch and attaches them to the
  release created by `auto-release.yml`. The in-pipeline `build-android` job was
  removed from `auto-release.yml` to avoid building the APK twice.

## [6.2.1_DE-1.19.2] - 2026-08-15

### Fixed

- [DE] Content now scales with the window: the Player artwork resizes with the
  window width (180–360dp) instead of being fixed at 300dp, and Library cards
  fill their adaptive grid cells instead of a fixed 140dp width.

## [6.2.1_DE-1.19.1] - 2026-08-15

### Changed

- [APK] Bumped the mobile version to 6.2.1 (versionCode 77).
- The desktop auto-release now builds and attaches the Android APK directly from
  this branch instead of trying to download it from the mobile CI release
  (`release.yml` runs on `main`, which had fallen out of sync), so the APK now
  reliably appears among the release assets.

## [6.2.0_DE-1.19.1] - 2026-08-15

### Changed

- [APK] The pairing-code field on Android now opens the numeric keypad and only
  accepts the 6 digits of the code shown by the desktop.

## [6.2.0_DE-1.19.0] - 2026-08-15

### Changed

- [DE] The About → Changelog screen now matches the mobile app: a horizontally
  scrollable row of version chips on top, and, for the selected version, a bold
  primary title plus its Added/Fixed/Changed sections rendered as bullet items
  (instead of a flat markdown dump of the whole file).

## [6.2.0_DE-1.18.0] - 2026-08-15

### Added

- [DE] The desktop pairing code now shows a live countdown until it expires
  (5 minutes) and offers a "Generate new code" button to mint a fresh code.

### Changed

- [DE] The desktop Device sync section is now generate-only: it no longer shows
  an "enter code" field. The desktop generates the 6-digit code, and the phone
  enters it.
- [APK] The Android Devices screen is now insert-only: the "Generate code"
  button was removed, leaving just the "enter code" + Pair flow that reads the
  code shown by the desktop.

## [6.2.0_DE-1.17.1] - 2026-08-15

### Fixed

- [DE] Fixed desktop audio not playing: the player reused a stale cache file when
  switching tracks, silently swallowed every decode/download/audio-device error
  (so playback stopped with no message), and did not verify the downloaded file.
  Failures now surface a clear reason in the player, and the decode pipeline is
  more robust.
- [DE] The app now closes itself right after launching an update installer so the
  installer can replace the running files (updates could not install otherwise).

### Changed

- [DE] The About → Changelog screen now fetches `CHANGELOG.md` live from the
  repository (falling back to the bundled copy when offline), so it always shows
  the current changelog without waiting for a new build.

## [6.2.0_DE-1.17.0] - 2026-08-15

### Added

- [DE] Ported the Home screen to the Android app's style: filter chips, mobile-style
  section headers (label + bold primary title, "Play all" button, chevron), songs-only
  sections rendered as horizontal song lists (Quick picks style), mixed sections as
  card carousels, and a Mood & genres section whose buttons open a new generic
  Browse screen.
- [DE] The QR code in Device sync is now always rendered on a solid white card so it
  scans reliably in both light and dark themes.

## [6.2.0_DE-1.16.2] - 2026-08-15

### Fixed

- [DE] Fixed the changelog being unclear about desktop versions: releases are
  now versioned as `<mobile>_DE-<de>` sections in `CHANGELOG.md` (instead of
  everything accumulating under "Unreleased"), and the changelog screen shows
  the current DE version and channel at the top.

## [6.2.0_DE-1.16.1] - 2026-08-15

### Fixed

- [DE] Fixed the language selector always showing English: the desktop string
  table now ships real translations for 45 languages (generated from the
  Android app's `strings.xml` / `vivi_strings.xml` via
  `scripts/generate_desktop_localization.py`). Keys without a translation
  still fall back to English.

## [6.2.0_DE-1.16.0] - 2026-08-15

### Added

- [DE] Started the pixel-perfect UI port from the Android app: the theme now
  uses the same seed-based Material 3 palette (materialKolor TonalSpot, seed =
  accent color) as the mobile app, and the fixed text sidebar was replaced with
  a collapsible/expandable sidebar with Material icons + labels (persisted).

## [6.2.0_DE-1.15.1] - 2026-08-15

### Fixed

- [DE] Fixed the update check picking the wrong release: it now selects the
  desktop release with the highest `_DE-<version>` tag instead of the first /
  "latest" entry (GitHub orders releases by publish date, not by version), so
  an older tag no longer masks a newer one. The release list window was also
  raised to 100 and the changelog notes follow the same highest-version rule.

## [6.2.0_DE-1.15.0] - 2026-08-15

### Added

- [DE] Wired device sync end-to-end: the desktop now pushes its playback
  (track, queue, position, play/pause) and settings, and applies incoming
  snapshots — remote playback starts on the desktop player and language /
  theme / accent follow the phone. A persistent `DesktopSyncManager` owns the
  client + LAN relay for the whole app lifetime (no more state loss when
  leaving Settings), with echo suppression to avoid ping-pong loops.
- [APK] Android now pushes its playback to the desktop and applies incoming
  playback snapshots (desktop → phone), so starting a song on either device
  resumes on the other.

## [6.2.0_DE-1.14.3] - 2026-08-15

### Fixed

- [DE] Fixed most text not adapting to the dark/light theme: Material 3's
  `MaterialTheme` does not set `LocalContentColor`, so text without an explicit
  color fell back to black. The app root now provides `LocalContentColor =
  onBackground`, so titles, headers and other uncolored text follow the theme.

## [6.2.0_DE-1.14.2] - 2026-08-15

### Changed

- [DE] Debounced the Lyrics screen position updates: the highlighted line is
  now polled ~5×/s and only recomposed when it changes, instead of recomposing
  the list on every decoded-frame position update (~40×/s).

## [6.2.0_DE-1.14.1] - 2026-08-15

### Fixed

- [DE] Fixed the "Open installer" button not launching the downloaded
  installer: opening now falls back to the OS's native opener (`cmd /c start`
  on Windows, `open` on macOS, `xdg-open` on Linux) when `Desktop.open()`
  fails, and reports an error instead of failing silently.

## [6.2.0_DE-1.14.0] - 2026-08-15

### Added

- [DE] Added optional manual `DATASYNC_ID` / `VISITOR_DATA` fields to the
  desktop login screen as a fallback for when the automatic extraction from
  the music.youtube.com shell fails.

## [6.2.0_DE-1.13.0] - 2026-08-15

### Added

- [DE] Rebuilt the Player screen as a full Material 3 player: a seek slider
  with elapsed/total time, a volume slider, shuffle and repeat (off/all/one),
  proper Material icons and a large artwork presentation. Playback now reports
  the track duration and supports seeking (the stream is cached locally), the
  volume is adjustable, and the mini-player shows a progress bar.

## [6.2.0_DE-1.12.1] - 2026-08-15

### Fixed

- [DE] Fixed dark mode not repainting the page background: the app root now
  paints the theme's `background` color, so switching to dark converts the
  whole window instead of leaving the native light background showing through.

## [6.2.0_DE-1.12.0] - 2026-08-15

### Added

- [DE] Added the animated canvas to the Player: a blurred, slowly-zooming
  (Ken Burns) artwork background behind the track. Canvas artwork is resolved
  from the same providers as the Android app (Apple Music / Tidal / VIVI Music
  canvas); animated GIF/WebP URLs play via Coil, while video canvases
  (MP4/HLS) fall back to static art + zoom.

## [6.2.0_DE-1.11.0] - 2026-08-15

### Added

- [DE] Added synced lyrics: the Lyrics screen now parses LRC timestamps,
  highlights the current line and auto-scrolls to it as the song plays
  (falls back to plain text when lyrics aren't synced).

## [6.2.0_DE-1.10.0] - 2026-08-15

### Added

- [DE] Added drag-to-reorder to the Queue screen (drag the ⠿ handle), reusing
  the same `sh.calvin.reorderable` library as the Android app.

## [6.2.0_DE-1.9.0] - 2026-08-15

### Added

- [DE] Added YouTube login on desktop: paste the music.youtube.com `Cookie`
  header (no WebView needed), which auto-extracts the account's
  `DATASYNC_ID`/`VISITOR_DATA`, validates the session and persists it locally.
  History now works when signed in, and Library gained Songs / Albums / Artists
  / Playlists tabs (liked songs, albums, artists and playlists).

## [6.2.0_DE-1.8.0] - 2026-08-14

### Added

- [DE] Added an Updates section in Settings with in-app downloads: it detects
  the right installer for the host OS/arch (MSI/AppImage/DMG with EXE/DEB/PKG
  fallback), downloads it with progress % + speed, opens it, and can delete
  downloaded installers.
- [DE] Added Player & audio settings (autoplay next track) and a Storage
  section (cache size + clear cache) to Settings.
- [DE] Added a changelog screen (About → Changelog) showing the bundled
  `CHANGELOG.md` plus the latest GitHub release notes.

## [6.2.0_DE-1.7.0] - 2026-08-14

### Added

- [DE] Added a full playback queue: "add to queue" on every song row, "Play
  all" on albums/playlists, next/previous, auto-advance, and a Queue screen
  (jump / remove / clear).
- [DE] Added a History screen (sidebar) listing the user's listening history.

## [6.2.0_DE-1.6.0] - 2026-08-14

### Added

- [DE] Added a light/dark/system theme with a selectable accent color palette
  (Settings → Appearance), applied across the whole desktop app.

## [6.2.0_DE-1.5.1] - 2026-08-14

### Fixed

- [DE] Fixed the desktop player showing "could not resolve the audio stream":
  the stream resolver now uses the same multi-client fallback chain as the
  mobile app (ANDROID_VR + 11 fallback clients, n-param deobfuscation and URL
  validation) instead of a single ANDROID_VR attempt that YouTube often answers
  with `LOGIN_REQUIRED`.

## [6.2.0_DE-1.5.0] - 2026-08-14

### Added

- [APK] Added LAN discovery to the Android Devices screen: "Find desktop"
  (mDNS/NSD `_vivimusic._tcp`) and "Scan QR code" auto-fill the relay server
  URL when pairing with VIVI Music DE over the same Wi-Fi. (Mobile version
  bumped 6.1.0 → 6.2.0.)

## [6.1.0_DE-1.5.0] - 2026-08-14

### Added

- [DE] Added LAN discovery aids to the desktop Device sync section: a QR code
  encoding the local relay address, and mDNS service registration
  (`_vivimusic._tcp`) so the Android app can discover/scan the desktop.

## [6.1.0_DE-1.4.0] - 2026-08-14

### Added

- [DE] Added offline LAN (same Wi-Fi) device pairing: the desktop can start a
  local WebSocket relay from Settings → Device sync, so the Android app can
  pair directly without the cloud relay.

## [6.1.0_DE-1.3.1] - 2026-08-14

### Changed

- [DE] The auto-release now attaches the mobile APKs by downloading them from
  the mobile CI release (tag `v<mobile>`) instead of rebuilding them in a
  separate Android job, making desktop releases much faster.

## [6.1.0_DE-1.3.0] - 2026-08-14

### Added

- [DE] Added an Updates section in Settings: an automatic update check on
  startup plus a manual "Check for updates" button, an opt-in toggle to include
  pre-releases, and a download link when a newer desktop release is available.

## [6.1.0_DE-1.2.1] - 2026-08-14

### Fixed

- [DE] Fixed the auto-release workflow's invalid YAML: `continue-on-error` is not
  allowed on a job that calls a reusable workflow, so the Android APK build is
  now made optional with per-step `continue-on-error` inside `build-android.yml`
  instead (the desktop release no longer requires the APK to succeed).

## [6.1.0_DE-1.2.0] - 2026-08-14

### Added

- [DE] Integrated self-contained audio playback (Phase 4): AAC stream
  resolution (NewPipe + ANDROID_VR) and a pure-Java AAC decoder (`jaad`)
  played through Java Sound — no external player or native codec required.
  The mini-player and Player screen now actually play/pause/resume songs and
  show the playback position.

## [6.1.0_DE-1.1.2] - 2026-08-14

### Changed

- [DE] Release notes now collapse the commit list into an expandable section
  when there are more than 7 commits.

## [6.1.0_DE-1.1.1] - 2026-08-14

### Changed

- [DE] The Android APK build in the auto-release is now optional (per-step
  best-effort), so a missing signing secret or failed APK build no longer
  blocks the desktop release.

## [6.1.0_DE-1.1.0] - 2026-08-14

### Added

- [DE] Full desktop UI: sidebar navigation with Home, Search, Album, Artist,
  Playlist, Library, Player, Lyrics and Settings screens, artwork thumbnails,
  and an Apple Music–style mini-player. (Library is a placeholder pending
  login; audio playback and the animated canvas are deferred to later phases.)

## [6.1.0_DE-1.0.4] - 2026-08-14

### Changed

- [DE] The auto-release now also builds and attaches the Android APKs
  (GMS + FOSS) to the same release, so each release ships desktop + mobile
  assets together.

## [6.1.0_DE-1.0.3] - 2026-08-14

### Fixed

- [DE] Fixed desktop device pairing: the desktop client now actually connects
  (the `connect()` call was unreachable) and defaults to the same relay URL as
  the Android app instead of the local `wss://localhost:8080` placeholder, so
  "Generate code" produces a code.

## [6.1.0_DE-1.0.2] - 2026-08-14

### Changed

- [DE] Split the Linux build into independent DEB and AppImage jobs so a
  failure in the AppImage step no longer blocks the DEB package (or the
  release).

## [6.1.0_DE-1.0.1] - 2026-08-14

### Changed

- [DE] The Windows build now produces both an Inno Setup installer and a
  jpackage MSI.
- [DE] Removed the Inno Setup wizard images that could make the installer open
  and immediately close on some systems.

## [6.1.0_DE-1.0.0] - 2026-08-14

### Added

- [APK] Added a "Devices" section in the Android Settings to pair the phone with
  VIVI Music DE (relay server URL, generate/join pairing code, unpair).

## [6.0.5_DE-1.0.0] - 2026-08-14

### Added

- [DE] Compose Multiplatform desktop target (`desktop` module) reusing the
  pure-JVM network modules.
- [DE] Native desktop icons (Windows `.ico`, macOS `.icns`, Linux `.png`) using
  the VIVI Music DE logo.
- [DE] Per-OS GitHub Actions builds (MSI/EXE, DEB/AppImage, DMG/PKG) and an
  auto-release workflow.
- [DE] Cross-device sync foundation: shared `sync` module, Node.js WebSocket
  relay (`sync-server/`), Android `DeviceSyncManager`, and desktop pairing UI.

### Changed

- Converted `innertube`, `spotify`, `lastfm`, `kizzy`, `shazamkit`,
  `jiosaavn`, and `lyricsProvider` to pure-JVM Kotlin modules so they can be
  shared between Android and desktop.
- Desktop releases now use a combined `<mobile>_DE-<desktop>` version
  (e.g. `6.0.5_DE-1.0.0`): `version.txt` line 1 = mobile version, line 2 = DE
  version, line 3 = channel. The About screen shows the full version + channel;
  desktop changelog entries are marked `[DE]`.
- The release channel is now read from line 3 of `version.txt`: `stable` (or
  empty) publishes a stable release; any other value (`rc`/`beta`/`alpha`/
  `nightly`) publishes a pre-release.
- Release tags no longer carry a `v` prefix; non-stable releases append the
  channel to the tag (e.g. `6.0.5_DE-1.0.0-nightly`).
- [DE] The desktop UI is now English-first with a 49-language picker (first
  launch + Language menu); non-English strings fall back to English until
  translated.
- [DE] The Windows installer now performs a machine-wide install into
  `C:\Program Files\VIVIMusic` (requires admin rights) instead of a per-user
  install into `%LOCALAPPDATA%`.
- [DE] The Windows installer is now a branded Inno Setup wizard and shows a
  "successfully uninstalled" confirmation message after removal.

### Fixed

- [DE] Made `gradlew` executable in the repository and in the desktop build
  workflows (fixes `./gradlew: Permission denied` on Linux/macOS runners).
- [DE] Replaced the retired `macos-13` runner with `macos-15-intel` for the
  Intel macOS build.

### Security

- Cross-device sync traffic is currently TLS-only; end-to-end encryption is
  planned for a future release.
