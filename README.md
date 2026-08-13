# Vivi Music DE

A desktop and mobile music client built with **Kotlin Multiplatform** and
**Compose Multiplatform**, recreating the experience of
[ViVi Music](https://github.com/25huizengek1/ViMusic) (an open source YouTube
Music client). The UI and logic are shared between **Android** and
**Desktop (Windows/macOS/Linux)**, and user data (playlists, favorites,
history) syncs in real time via **Supabase**.

## Features

- **Android** and **Desktop (JVM)** targets from a single codebase.
- **Compose Multiplatform** (Material 3) UI: Home with search, Library,
  Settings, and a "now playing" bar.
- **YouTube Music** catalog via an InnerTube client (search, home feed,
  album/playlist, audio stream resolution).
- Local database with **Room KMP** and a bundled SQLite driver.
- **Supabase** sync (PostgREST + Realtime + Auth) with real-time mirroring
  across devices.
- **49 languages**, with manual selection or system language.
- **CI/CD** with GitHub Actions.

## Build requirements

- JDK 17+ (Gradle 8.x does not support JDK 25; packaging with JDK 17 lowers the
  runtime requirement to macOS 10.15+ and Windows 10+).
- Android SDK with platform 36 (only for the Android target).
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
# Desktop: compile and run
./gradlew :composeApp:run

# Desktop: native installer for the current OS
./gradlew :composeApp:packageDistributionForCurrentOS

# Android: debug APK
./gradlew :composeApp:assembleDebug

# Android: install on a connected device/emulator
./gradlew :composeApp:installDebug

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

The per-OS build workflows produce the Android APK and the desktop installers
(Windows/macOS/Linux), and `auto-release.yml` publishes the release with those
artifacts attached.

## Supabase configuration

1. Create a project on [Supabase](https://supabase.com).
2. Run `supabase/migrations/0001_init.sql` in the SQL editor (creates tables,
   RLS, and enables Realtime).
3. Provide the credentials:
   - **Android**: add to `local.properties` (a git-ignored file):
     ```properties
     supabase.url=https://your-project.supabase.co
     supabase.anonKey=your-anon-key
     ```
   - **Desktop**: set the `SUPABASE_URL` and `SUPABASE_ANON_KEY` environment
     variables, or create a `supabase.env` file with
     `SUPABASE_URL=...` and `SUPABASE_ANON_KEY=...`.

Without credentials the app runs in local-only mode.

## Project structure

See [`AGENTS.md`](AGENTS.md) for architecture, conventions, versioning (SemVer)
rules, and instructions for adding new languages.

## Note

The audio playback engine (player) is the next step: the app already resolves
the audio stream of tracks but does not play them yet. The InnerTube client is
a working subset of the full module from the original project.
