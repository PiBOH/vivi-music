# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed
- Desktop app icon now uses the custom `logo.png` (Axolotl logo) instead of the
  generated pixel-art icon; `tools/generate_icons.py` reads `logo.png` to
  produce the Linux/macOS/Windows icons.

### Fixed
- macOS release build: set a reverse-DNS bundle identifier and use only the
  standard ICNS icon types, so `packageDistributionForCurrentOS` (DMG) no
  longer fails on the macOS runners.

## [0.0.1-alpha] - 2026-08-13

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
- Original pixel-art Axolotl mascot (splash screen, About section, and desktop
  app icons for Windows/macOS/Linux).
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

### Fixed
- Windows installer: add a Start Menu entry and desktop shortcut
  (`menu = true`, `shortcut = true`) and a stable `upgradeUuid`, so the app is
  discoverable and upgradable after install.

### Security
- Remove the hardcoded InnerTube (YouTube Music) API key from the source; it is
  now provided via the `INNERTUBE_API_KEY` environment variable (GitHub secret
  for releases) or a git-ignored `.env` file.
