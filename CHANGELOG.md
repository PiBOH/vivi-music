# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.4.0-alpha] - 2026-08-13

### Added
- Full settings screen set, ported from ViVi Music and adapted for desktop: a
  grouped hub (Update / Appearance / Player and audio / Content / Privacy /
  Storage / About) that opens nested settings pages with back navigation.
- Appearance settings: theme mode (system default / light / dark) and a
  selectable accent color, both persisted and applied live.
- Player and audio settings: audio quality (auto / high / low), plus persisted
  toggles for audio normalization, skip silence and crossfade.
- Content settings: app language selector (moved out of the old flat settings
  screen).
- Privacy settings: a synchronization toggle (persisted, gates Supabase sync)
  with the sync status and a "Sync now" action.
- Storage settings: clear search history and clear playback history, each with
  a confirmation dialog.
- About settings: mascot, version, credits and a source-code link to the
  GitHub repository.

### Changed
- Audio quality selection is wired into InnerTube stream resolution (low =
  lowest bitrate audio format, high = highest, auto = Opus preferred).
- Settings UI uses a reusable Material 3 group/item component set (rounded
  cards, dividers, switches, radio-list and confirm dialogs), centered and
  width-capped for wide desktop windows.

## [0.3.0-alpha] - 2026-08-13

### Added
- Album detail screen (artwork header with title/artist/year, play button and
  the track list), ported from ViVi Music's `AlbumScreen`.
- Artist detail screen (circular avatar header, "Top songs" shelf and album
  carousels), ported from ViVi Music's `ArtistScreen`.
- Playlist detail screen (square thumbnail header and its songs, with a remove
  button per row), ported from ViVi Music's `LocalPlaylistScreen`.
- History screen listing recently played songs, reachable from the navigation
  rail.
- Library rows (albums, artists, playlists) now open their detail screen; local
  albums/artists are shown from favorites without a remote fetch.
- InnerTube `getArtist` endpoint and an album/playlist parser for the current
  `twoColumnBrowseResultsRenderer` + `musicResponsiveHeaderRenderer` response
  format (title, artist, year and thumbnail from the header/microformat).
- Repository helpers to add/remove songs in a local playlist.

### Fixed
- Linux `.AppImage` artifact: the build now wraps the jpackage app-image
  directory with `appimagetool` (AppRun + .desktop + icon) so Arch/other
  distros get the portable AppImage, in addition to the `.deb`.

## [0.2.2-alpha] - 2026-08-13

### Fixed
- Packaged app no longer fails to start with "Failed to launch JVM": the
  runtime image now includes the JDK modules LavaPlayer and its HTTP client
  need but jdeps does not detect (`java.management`, `java.scripting`,
  `jdk.management`, `java.naming`, `jdk.naming.dns`).

### Changed
- Startup errors are now shown in a detailed dialog (full message + stack
  trace) and written to a `ViviMusicDE-crash.log` file next to the executable,
  instead of a silent/cryptic exit.

## [0.2.1-alpha] - 2026-08-13

### Fixed
- Search: switched the search/browse/suggestion endpoints to the `WEB_REMIX`
  client with the current client version and required headers. YouTube Music
  was returning the new element renderer format (and empty results) with the
  previous client, so song search now returns the expected list of tracks.

## [0.2.0-alpha] - 2026-08-13

### Added
- Update checker: on startup the app queries the GitHub Releases API and shows
  an update dialog (with a download link) when a newer version exists. It
  checks stable releases by default; a Settings toggle makes it also consider
  pre-releases.

## [0.1.2-alpha] - 2026-08-13

### Fixed
- macOS DMG build: declare `packageAppImage` as a dependency of `packageDmg`,
  fixing the Gradle implicit-dependency validation error that failed the macOS
  release packages.

## [0.1.1-alpha] - 2026-08-13

### Changed
- Navigation rail: the Axolotl logo is now shown as the rail header and the
  Settings entry moved from the top bar into the rail (pinned to the bottom).

## [0.1.0-alpha] - 2026-08-13

### Added
- Audio playback engine (desktop): the InnerTube audio stream (preferring
  Opus/WebM) is streamed and decoded with LavaPlayer and played through Java
  Sound. The mini player and full player now actually play audio, with a live
  seek bar, position and duration.

## [0.0.2-alpha] - 2026-08-13

### Added
- Initial Kotlin Multiplatform project with a Desktop (JVM) target for
  Windows/macOS/Linux.
- Shared Compose Multiplatform UI: Home, Search, Library, Settings and a
  "now playing" bar.
- InnerTube (YouTube Music) client in `commonMain` for search, home feed,
  album/playlist, search suggestions and audio stream resolution.
- Local persistence with Room KMP (entities for songs, playlists, favorites,
  history, search history and sync state).
- User data sync with Supabase (PostgREST + Realtime + Auth) and a
  `SyncManager` with bidirectional pull/push and real-time mirroring.
- Multilingual support for 49 languages with manual or system-language
  selection.
- GitHub Actions CI (build on push/PR) and auto-release (desktop artifacts
  for Windows/macOS/Linux) workflows.
- Initial Supabase SQL migration with Row Level Security and Realtime.
- `AGENTS.md` with project conventions, SemVer and changelog rules.
- Original Axolotl mascot (`logo.png`) shown in the splash screen and About
  section, and used for the desktop app icons (Windows/macOS/Linux).
- Material 3 Expressive design system ported from ViVi Music: seed color
  (`0xFFED5564`) with materialKolor (SPEC 2025 + TonalSpot), the M3 Expressive
  type scale, grouped list-item shapes, and the mobile bottom navigation
  (Home / Search / Listen Together / Library) with a mini player.
- Mini player and full player (bottom-sheet style) ported to match ViVi Music:
  a rounded pill mini player (play/pause, song info, skip next) and an expanded
  player with artwork, title/artist, seek bar and shuffle/previous/play/next/
  repeat controls. Audio playback is not wired yet, so the controls are visual
  placeholders.
- Search screen ported to match ViVi Music's OnlineSearchScreen: live
  autocomplete suggestions from InnerTube (`get_search_suggestions`),
  persistent search history (Room table) and a grouped results list.

### Changed
- Home screen ported to match ViVi Music: the home feed is parsed into sections
  with titles and rendered as section headers + horizontal card carousels with
  thumbnails (Coil 3).
- Library screen ported to match ViVi Music: a horizontally scrolling filter
  chip row (Playlists / Songs / Albums / Artists, with toggle-to-deselect) and
  per-filter views plus a combined "mix" view. Albums and artists are derived
  from the local favorites until dedicated library tables are added.
- Desktop app icon and the in-app mascot (splash screen and About section) now
  use the custom `logo.png` (Axolotl logo) instead of the generated pixel-art
  icon; `tools/generate_icons.py` reads `logo.png` to produce the
  Linux/macOS/Windows icons.
- Navigation moved from a bottom bar to a side `NavigationRail` (desktop
  adaptation), keeping the mini player at the bottom of the content area.
- CI and per-OS build workflows now expose manual `workflow_dispatch`
  triggers.
- Installer and artifact version now derive from `version.txt` (single source
  of truth) instead of a hardcoded `1.0.0`; the in-app About version is also
  injected from `version.txt` at build time. The installer `packageVersion`
  maps the SemVer to a MAJOR >= 1 numeric version (e.g. `0.0.2-alpha` ->
  `1.0.2`).
- Package author/publisher set to PiBOH (https://piboh.github.io/).

### Fixed
- Windows installer: add a Start Menu entry and desktop shortcut
  (`menu = true`, `shortcut = true`) and a stable `upgradeUuid`, so the app is
  discoverable and upgradable after install.
- macOS release build: set a reverse-DNS bundle identifier and use only the
  standard ICNS icon types, so `packageDistributionForCurrentOS` (DMG) no
  longer fails on the macOS runners.
- CI: make `gradlew` executable in git and run the Gradle steps with
  `shell: bash`, fixing the "Permission denied" (exit 126) on the Linux and
  macOS runners.

### Security
- Remove the hardcoded InnerTube (YouTube Music) API key from the source; it is
  now provided via the `INNERTUBE_API_KEY` environment variable (GitHub secret
  for releases) or a git-ignored `.env` file.
