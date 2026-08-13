# Vivi Music DE

A **desktop** music client built with **Kotlin Multiplatform** and
**Compose Multiplatform**, recreating the experience of
[ViVi Music](https://github.com/25huizengek1/ViMusic) (an open source YouTube
Music client). The app runs on **Windows, macOS and Linux**, and user data
(playlists, favorites, history) syncs in real time via **Supabase**.

## Features

- **Desktop (JVM)** app for Windows, macOS and Linux from a single codebase.
- **Compose Multiplatform** (Material 3) UI: Home with search, Library,
  Settings, and a "now playing" bar.
- **YouTube Music** catalog via an InnerTube client (search, home feed,
  album/playlist, audio stream resolution).
- Local database with **Room KMP** and a bundled SQLite driver.
- **Supabase** sync (PostgREST + Realtime + Auth) with real-time mirroring
  across devices.
- **49 languages**, with manual selection or system language.
- Original **Axolotl** pixel-art mascot shown in the splash screen and About
  section, and used as the desktop app icon.
- **CI/CD** with GitHub Actions.

## Build requirements

- JDK 17+ (Gradle 8.x does not support JDK 25; packaging with JDK 17 lowers the
  runtime requirement to macOS 10.15+ and Windows 10+).
- A Supabase project (optional, for sync).

## System requirements (runtime)

- **Windows**: Windows 10 or later (x86-64). Windows 7/8/8.1 are not supported
  because the JDK 17+ runtime requires Windows 10+.
- **macOS**: macOS 10.15 (Catalina) or later, on both Intel and Apple Silicon.
- **Linux**: Debian/Ubuntu via `.deb`; Arch Linux and other (glibc) distros via
  `.AppImage`.
- **CPU**: x86-64 processor with SSE2 instructions (e.g. Intel Core i5-650 or
  later). AVX support is not required.

## Build and run

```bash
# Compile and run the app
./gradlew :composeApp:run

# Native installer for the current OS
./gradlew :composeApp:packageDistributionForCurrentOS

# All checks
./gradlew build
```

## Automatic releases

The canonical app version lives in `version.txt` (SemVer format, e.g.
`0.0.1-alpha`).

To publish a GitHub Release:
1. Update `version.txt` and `CHANGELOG.md`.
2. Commit and push with a message starting with `v` (e.g. `v0.0.1-alpha: ...`),
   or run the `auto-release.yml` workflow manually.

The per-OS build workflows produce the desktop installers
(Windows/macOS/Linux), and `auto-release.yml` publishes the release with those
artifacts attached.

## Supabase configuration

1. Create a project on [Supabase](https://supabase.com).
2. Run `supabase/migrations/0001_init.sql` in the SQL editor (creates tables,
   RLS, and enables Realtime).
3. Provide the credentials by setting the `SUPABASE_URL` and
   `SUPABASE_ANON_KEY` environment variables, or by creating a `supabase.env`
   file with `SUPABASE_URL=...` and `SUPABASE_ANON_KEY=...`.

Without credentials the app runs in local-only mode.

## Mascot

The app's mascot is an original pixel-art blue **Axolotl**, rendered in-app
(splash screen and About section) and used as the desktop icon. The artwork is
defined once in
[`Axolotl.kt`](composeApp/src/commonMain/kotlin/com/vivimusic/de/ui/Axolotl.kt)
and the icon files are generated with
[`tools/generate_icons.py`](tools/generate_icons.py).

## Project structure

See [`AGENTS.md`](AGENTS.md) for architecture, conventions, versioning (SemVer)
rules, and instructions for adding new languages.

## Note

The audio playback engine (player) is the next step: the app already resolves
the audio stream of tracks but does not play them yet. The InnerTube client is
a working subset of the full module from the original project.
