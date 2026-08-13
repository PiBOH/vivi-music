# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.0.1-alpha] - 2026-08-13

### Added
- Initial Kotlin Multiplatform project with Android and Desktop (JVM) targets.
- Shared Compose Multiplatform UI: Home with search, Library (favorites,
  history, playlists), Settings and a "now playing" bar.
- InnerTube (YouTube Music) client in `commonMain` for search, home feed,
  album/playlist and audio stream resolution.
- Local persistence with Room KMP (entities for songs, playlists, favorites,
  history and sync state).
- User data sync with Supabase (PostgREST + Realtime + Auth) and a
  `SyncManager` with bidirectional pull/push and real-time mirroring.
- Multilingual support for 49 languages with manual or system-language
  selection.
- GitHub Actions CI (build on push/PR) and auto-release (Android and desktop
  artifacts for Windows/macOS/Linux) workflows.
- Initial Supabase SQL migration with Row Level Security and Realtime.
- `AGENTS.md` with project conventions, SemVer and changelog rules.
