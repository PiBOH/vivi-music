# AGENTS.md

Operational instructions for AI agents (and developers) working on this
repository. Read this file in full before modifying the code.

## 1. Project overview

**Vivi Music DE** is a **desktop** music client built with
**Kotlin Multiplatform (KMP)** and **Compose Multiplatform**, recreating the
experience of ViVi Music (an open source YouTube Music client). Code and UI are
shared across the **Desktop (JVM: Windows/macOS/Linux)** targets, and user data
(playlists, favorites, history) syncs in real time via **Supabase**.

### Tech stack

| Area      | Technology                                                           |
|-----------|----------------------------------------------------------------------|
| Language  | Kotlin (2.4.x)                                                       |
| UI        | Compose Multiplatform + Material 3                                   |
| Build     | Gradle (Kotlin DSL), version catalog in `gradle/libs.versions.toml`  |
| Network   | Ktor Client (CIO engine)                                              |
| Database  | Room KMP (`androidx.room3`), bundled SQLite driver                   |
| Sync      | Supabase (supabase-kt: PostgREST + Realtime + Auth)                  |
| i18n      | Compose Multiplatform resources (`composeResources/**`)              |
| CI/CD     | GitHub Actions (`.github/workflows/ci.yml` and `auto-release.yml`)   |

### Module structure

```
vivi-music-de/
├── .github/workflows/          # ci.yml, build-*.yml (per OS), auto-release.yml
├── gradle/
│   ├── libs.versions.toml      # version catalog
│   └── wrapper/                # Gradle wrapper
├── composeApp/                 # single KMP module (Desktop app)
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/         # shared code
│       │   ├── composeResources/   # string resources (values/ and values-XX/)
│       │   └── kotlin/com/vivimusic/de/
│       │       ├── data/           # db, network, sync, repository, container
│       │       ├── domain/         # domain models (Song, Playlist, ...)
│       │       ├── i18n/           # languages and locale handling
│       │       └── ui/             # shared Compose screens
│       └── desktopMain/        # main.kt, Desktop actuals
├── supabase/migrations/        # SQL schema + RLS + Realtime
├── AGENTS.md
└── CHANGELOG.md
```

The project uses a **single Gradle module** (`composeApp`) with the two source
sets `commonMain` and `desktopMain`. The `expect`/`actual` pattern is used for
platform-specific parts (HTTP engine, database builder, settings persistence,
locale handling).

### Data flow

- **UI** (`ui/`) -> **AppViewModel** -> **MusicRepository** -> local database /
  **InnerTubeClient** (YouTube Music catalog).
- **SyncManager** orchestrates sync between the local Room database and
  **SupabaseSyncClient** (PostgREST for pull/push, Realtime for mirroring).

## 2. Code conventions

- **Language**: code, comments, commit messages and all documentation in
  English.
- **Package**: `com.vivimusic.de`.
- **Style**: follow `kotlin.code.style=official` (already configured).
- **Names**: data classes and classes in PascalCase, functions/properties in
  camelCase, constants in UPPER_SNAKE_CASE.
- **Coroutines**: use `suspend` for I/O operations and `Flow`/`StateFlow` for
  reactive state. Never block the UI thread.
- **Resources**: every user-visible string must be a resource
  (`Res.string.*`), never a hardcoded string.
- **Dependencies**: add versions only in `gradle/libs.versions.toml`. Do not
  introduce new libraries without need and without checking whether they are
  already present in the project.
- **Errors**: do not swallow exceptions; handle or propagate them explicitly.
- **No emoji** in code, comments, strings, logs, resources and workflows.

## 3. Golden rule: "What works is not touched"

> **Do not refactor or modify modules, classes or functions that already work
> and are stable, unless it is strictly necessary for the requested feature or
> explicitly requested by the user.**

- Before touching existing code, verify that it is really needed for the task.
- Prefer non-invasive additions (new files, extensions, defaulted parameters)
  over rewrites.
- If a change to stable code is unavoidable, justify it in the commit and the
  changelog.
- After every change, run the build and tests to confirm nothing broke.

## 4. Versioning (SemVer)

Every version bump must strictly follow **Semantic Versioning**
(`MAJOR.MINOR.PATCH`), as defined at https://semver.org:

- **MAJOR**: incompatible changes with previous versions.
- **MINOR**: backwards-compatible new features.
- **PATCH**: backwards-compatible bug fixes.

The canonical app version is declared in `version.txt` at the repository root
(SemVer format, e.g. `0.0.1-alpha`). The `packageVersion` of the desktop
installers is a separate numeric value (jpackage requires `MAJOR >= 1` and does
not accept prerelease suffixes).

## 5. Changelog (Keep a Changelog)

The `CHANGELOG.md` file follows the **Keep a Changelog** standard
(https://keepachangelog.com). **It must be updated on every important change**,
using the sections:

- `Added` — new features.
- `Changed` — changes to existing features.
- `Deprecated` — deprecated features.
- `Removed` — removed features.
- `Fixed` — bug fixes.
- `Security` — security fixes.

Each entry lives under a `## [VERSION] - YYYY-MM-DD` section. Do not delete
past entries.

## 6. Localization (i18n)

- Strings live in `composeApp/src/commonMain/composeResources/`.
- `values/strings.xml` is the **English default** and contains the canonical
  list of keys.
- Each language has `values-<qualifier>/strings.xml` (e.g. `values-it`,
  `values-de`, `values-zh-rCN`, `values-zh-rTW`). Missing keys fall back to
  English.
- The list of supported languages is in
  `i18n/AppLanguage.kt` (`supportedLanguages`), with BCP-47 code and native
  name.
- Manual selection uses the `expect/actual LocalAppLocale` pattern
  (`i18n/Locale.kt` + platform actuals); the choice is persisted via
  `SettingsStore` (`data/SettingsStore.kt`).

### Adding a new language

1. Add an entry to `supportedLanguages` in `i18n/AppLanguage.kt`.
2. Create `composeApp/src/commonMain/composeResources/values-XX/strings.xml`
   (replace `XX` with the language qualifier) translating the keys from the
   default `values/strings.xml`.
3. Translating every key is not required: missing keys fall back to English.
4. Update `CHANGELOG.md` (`Added` section).

## 7. Supabase sync

- Configuration is read at runtime in `data/AppConfig.kt` from the
  `SUPABASE_URL`/`SUPABASE_ANON_KEY` environment variables or from the
  `supabase.env` file.
- The schema (tables, RLS, Realtime) is in `supabase/migrations/0001_init.sql`.
- If credentials are missing, the app runs in local-only mode
  (`SyncStatus.Disabled`).
- The current sync pushes the entire local dataset (no dirty flag): it is
  correct and simple for small libraries; for large libraries, per-row change
  tracking should be introduced.

## 8. Build, test and run

Requirements: JDK 17+ (17 recommended for packaging with maximum compatibility;
Gradle 8.x does not support JDK 25), network access to download dependencies.

```bash
# Compile and run the app
./gradlew :composeApp:run

# Native installer for the current OS (.msi/.dmg/.deb/.AppImage)
./gradlew :composeApp:packageDistributionForCurrentOS

# All checks (tests + build)
./gradlew build

# Clean
./gradlew clean
```

Windows note: JDK 25 (or later) may not be supported by Gradle 8.x; use JDK 21
(e.g. `JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot`).

## 9. GitHub Actions workflows

- **CI** (`.github/workflows/ci.yml`): on every push to `main` and pull request
  it builds the desktop target (`./gradlew :composeApp:build`).
- **Build workflows** (reusable, invoked by Auto Release):
  - `build-windows.yml` -> `.msi`/`.exe` on `windows-latest` (JDK 17);
  - `build-linux.yml` -> `.deb`/`.AppImage` on `ubuntu-latest` (JDK 17);
  - `build-macos.yml` -> `.dmg` on `macos-15-intel` and `macos-15` (JDK 17).
- **Auto Release** (`.github/workflows/auto-release.yml`): does not build
  anything itself. It collects the artifacts produced by the build workflows,
  the matching `CHANGELOG.md` section and the commits since the previous tag,
  then publishes the GitHub Release. It triggers on:
  - a push to `main` whose commit message starts with `v` (e.g.
    `v0.0.1-alpha: ...`), or
  - a manual `workflow_dispatch` (with an optional version).
  The version is read from `version.txt` (tag = version without the `v` prefix).
- For MSI packaging on Windows (which requires WiX) consult the documentation
  and add the required tools.

### Releasing a new version

1. Update `version.txt` with the new SemVer version.
2. Update `CHANGELOG.md` with a `## [VERSION] - YYYY-MM-DD` section.
3. Commit with a message starting with `v` and push:
   `git commit -m "v$(cat version.txt): description" && git push`.

## 10. Definition of "Done"

A task is complete only when:

1. the code compiles (`./gradlew build`) and the tests pass;
2. no emoji were introduced in code/resources/workflows;
3. no stable code was refactored without necessity;
4. `CHANGELOG.md` is updated (correct section) when the change is relevant;
5. `version.txt` and `CHANGELOG.md` follow SemVer/Keep a Changelog when the app
   behavior changed.
