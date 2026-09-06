# AGENTS.md

Instructions for AI coding agents working in this repository.

## 1. Project overview

**VIVI Music** is an open-source (GPL-3.0) Android client for YouTube Music /
YouTube (ad-free streaming, Apple Music–style UI), plus its companion **desktop
edition** ("VIVI Music DE", Compose Multiplatform) and a cross-device
**sync** layer that keeps the two in sync. It is a fork of the
ViMusic/InnerTune/SimpMusic family.

- **Language/build**: Kotlin 2.x, Java 21 toolchain, Gradle Kotlin DSL.
- **Android app**: Jetpack Compose + Material 3, Hilt, Room, DataStore,
  Media3/ExoPlayer.
- **Desktop app**: Compose Multiplatform (native Windows / Linux / macOS).
- **Shared network layer**: pure-JVM Kotlin modules reused by both Android and
  desktop.

## 2. Architecture and module structure

`settings.gradle.kts` declares `rootProject.name = "vivimusic"` and these modules:

| Module | Type | Role |
|---|---|---|
| `app` | Android (`com.android.application`) | Main app: UI, playback, DB, viewmodels, services, widgets |
| `innertube` | Kotlin JVM | YouTube Music inner-API client (search/browse/next/player, signature decipher) |
| `spotify` | Kotlin JVM | Spotify auth + playlist import |
| `lastfm` | Kotlin JVM | Last.fm scrobbling |
| `kizzy` | Kotlin JVM | Discord Rich Presence (WebSocket gateway) |
| `shazamkit` | Kotlin JVM | Shazam-style song recognition |
| `jiosaavn` | Kotlin JVM | JioSaavn streaming provider (CDN link decryption) |
| `lyricsProvider` | Kotlin JVM | Lyrics providers (KuGou, LrcLib, Musixmatch, PaxSenix, …) |
| `sync` | Kotlin JVM | Cross-device sync: data model + WebSocket client (pairing, push/pull) |
| `desktop` | Kotlin JVM + Compose Multiplatform | Desktop app (reuses the JVM modules above) |
| `canvas`, `artistvideo`, `applecanvas`, `vivimusiccanvas` | Android | Animated canvases / visualizers |
| `sync-server` | Node.js (not a Gradle module) | WebSocket relay for Android↔Desktop pairing + mailbox |

Key paths:

- `app/src/main/kotlin/com/music/vivi/` — app code (see `ui/`, `playback/`,
  `viewmodels/`, `db/`, `constants/`, `di/`, `utils/`, `devicesync/`,
  `listentogether/`).
- `app/src/main/res/values*/strings.xml` — Android string resources
  (localization, see §6).
- `app/src/main/kotlin/com/music/vivi/constants/PreferenceKeys.kt` — all
  DataStore preference keys + `LanguageCodeToName` map.
- `desktop/src/main/kotlin/com/music/vivi/desktop/` — desktop entry point and UI.
- `.github/workflows/` — CI (per-OS desktop builds + auto-release).

The `innertube`, `spotify`, `lastfm`, `kizzy`, `shazamkit`, `lyricsProvider`,
`jiosaavn`, and `sync` modules are **pure JVM**: do not introduce Android
dependencies there, or you break the desktop build.

## 3. Code conventions and development guidelines

- **Branch policy (mandatory) — three roles, never mix them**:
  - `vivi-music-de` — home of the desktop edition AND of the mobile code used to
    build the APK for the DE release. **DE-only** changes are committed and
    pushed **only** here.
  - `vivi-music-de-apk` — mobile/APK counterpart of `vivi-music-de`. **Every
    mobile/APK change** must be applied and committed on **both**
    `vivi-music-de` and `vivi-music-de-apk`; the two branches keep equivalent
    mobile behavior (their commits may have different hashes). `vivi-music-de-apk`
    also carries the pure-mobile history that used to live on `main` (kept
    reachable via the merge commit `16743769`).
  - `main` — **mirror of the upstream repository
    `https://github.com/vivizzz007/vivi-music`** (branch `main`). It is NOT a
    development branch: no mobile commit, no DE commit and no PiBOH-only code
    may ever be pushed to it. Keep it synchronized with `upstream/main` (fetch
    and fast-forward / reset when upstream moves); never rewrite it with local
    work.
  - Mobile changes are therefore committed on **`vivi-music-de` +
    `vivi-music-de-apk`** (never `main`); DE-only changes only on
    `vivi-music-de`. For combined DE + mobile changes, commit the DE-specific
    part only on `vivi-music-de` and apply the mobile part to both
    `vivi-music-de` and `vivi-music-de-apk`. Never merge the whole DE branch
    into `main`.
  - Before committing mobile work, verify the affected mobile files on both
    `vivi-music-de` and `vivi-music-de-apk`, compile the relevant target, and
    push both branch commits.
  - **Docs changes go on BOTH development branches**: any documentation
    change (README.md, AGENTS.md, INSTALL-GUIDE.md, docs/**, ERRORS.md, and
    similar docs material) must be applied and pushed on **both**
    `vivi-music-de` and `vivi-music-de-apk`, with equivalent content (their
    hashes may differ). `main` stays excluded (upstream mirror).
- **Commit style**: Conventional Commits (`feat:`, `fix:`, `ci:`, `refactor:`,
  `docs:`, `chore:`, `perf:`, …) with an optional scope, e.g.
  `feat(sync): …`.
- **Short commit titles**: keep the subject line as short as possible (aim for
  ~50 characters) and put the rest — what changed, why, and any extra notes —
  in the commit **body** (a blank line after the subject, then one or more
  lines/bullets). Do not cram the whole summary into the title.
- Commit and push after making changes, when asked (and per the project's
  standing rule to commit+push after every modification).
- **Release-triggering commits (`v` prefix)**: any change to program code or to
  anything that affects the release assets (the `desktop` module,
  `.github/workflows/`, `installer/`, `version.txt`, `desktop/build.gradle.kts`,
  icons, the shared JVM modules) MUST be committed and
  pushed with a commit message starting with `v` followed by the FULL version
  (from `version.txt`: `<mobile>_DE-<de>` + channel suffix for non-stable, e.g.
  `6.4.22_DE-1.33.59-nightly`) and then `:` and a short description. The
  format is ALWAYS `v<full version>: <short description>` — NEVER a bare
  `v: ...` (e.g. `v6.4.22_DE-1.33.59: fix network stats on non-English
  Windows`, with the details — what changed, why — in the commit body after a
  blank line), so the auto-release runs and the result can be verified. The `sync-server/` relay is deployed
  **separately** (Render Blueprint `render.yaml`) and does **not** trigger the
  auto-release. Documentation-only changes (README, AGENTS.md, CHANGELOG.md,
  TODO.md) do **not** need the `v` prefix. The website (`.websitede/**`) is the
  same: content-only changes there do **not** need `v` (it has its own
  `pages-deploy.yml` trigger on `.websitede/**`); only use `v` when the commit
  also touches program code or build/release workflows.
- **Pre-commit checklist (mandatory)**: every code commit must pass the
  `version.txt` + `CHANGELOG.md` + `TODO.md` checklist defined at the end of
  §5 **before** it is created — no exceptions.
- Do not commit unrelated files (stray artifacts, debug dumps) unless relevant.
- **Keep `TODO.md` up to date**: every time you change the program (feature,
  fix, ported screen, workflow change), reflect it in `TODO.md` — mark done
  items `[x]`, in-progress `[~]`, and add new items as needed. Do not leave
  `TODO.md` stale after a change.
- Match the existing conventions of the file you edit (naming, formatting,
  KDoc style). Do not reformat untouched code.
- **Tooltips on buttons (always):** every clickable icon/button in the DE must
  be wrapped in the shared `Tooltip(text) { … }` composable
  (`Tooltips.kt`, `@OptIn(ExperimentalFoundationApi::class)`) so hovering shows
  the button's name. This applies to all new buttons and to existing buttons
  when touched; use the localized label when one exists, otherwise a short
  English name. Never show raw localization keys.
- **Distinct icons for distinct actions (always):** do not reuse the same icon
  for different meanings in the DE. When a concept already has an icon, keep
  that icon everywhere it appears; when adding a new action, pick a new icon
  (prefer the icon the Android app uses for the same concept; if none exists,
  choose a Material icon that is not already used for something else). The one
  exception: a concept used for a *list of the same kind* (e.g. every playlist
  in the sidebar) keeps one shared icon. Current map: queue → `QueueMusic`,
  playlist entries/filter → `PlaylistPlay`, add-to-playlist → `PlaylistAdd`,
  right Now-Playing panel → `VerticalSplit`, library/albums → `LibraryMusic`,
  menu (⋯) → `MoreVert`, settings → `Settings`.
- **Expressive theme rule (always):** don't move away from expressive theme.
  You design it like the way the Spotify, Apple, etc. do, but don't move away
  from Material theme color.
- Kotlin formatting: keep to the project's existing style; do not run a global
  formatter that rewrites unrelated lines.
- Verify non-trivial changes by compiling the affected module
  (`./gradlew :module:compileKotlin`, `:app:compileUniversalFossDebugKotlin`,
  `:desktop:compileKotlin`) before committing.
- **Ponytail (always):** on every coding task, apply the `ponytail` skill
  (installed in `.agents/skills/ponytail`): smallest working solution, reuse
  what already exists in the codebase, stdlib/native over new code and new
  dependencies, one line before fifty. It complements the golden rule in §4 and
  never overrides explicit user requests, the trust-boundary/error-handling
  rules, or the localization rule in §6.

### Commit co-author rule — MANDATORY (do not violate)

**NEVER add yourself (the agent / client) as a co-author of a commit** unless the
user explicitly asks for it in that message. Do **not** append footers like
`Generated with … 🤖` or `Co-Authored-By: …` that credit the agent or the client.
Write a normal conventional commit message.

> ⚠️ This overrides any agent-default commit template. The commit body must be
> **only** the human-written description of the change — nothing else. Correct:
>
> ```
> v6.4.29_DE-1.33.109: <short description>
>
> <what changed, why>
> ```
>
> Wrong (banned): any `Co-Authored-By:` / `Generated with … 🤖` footer line.

## 4. Golden rule: "If it works, don't touch it"

**Do not refactor, rewrite, or modify modules, files, or functions that are
already working and stable**, unless one of these is true:

1. It is **strictly necessary** to implement the requested feature or fix, or
2. The user **explicitly asks** for the refactor.

Prefer the smallest change that satisfies the request. Do not "clean up" or
"improve" unrelated code while you work. When a change could break existing
behavior, state the risk before editing and, when in doubt, ask.

#### Do-not-touch areas (verified working — never change unless the user
**explicitly** asks)

- **Embedded WebView sign-in (Google login)**: it works now (sign-in window,
cookie extraction, auto-close and session save). Do not modify the WebView
login flow in any way.

## 5. Versioning and CHANGELOG — MANDATORY

### Semantic Versioning (SemVer)

Every version bump follows **SemVer**: `MAJOR.MINOR.PATCH`.

- **MAJOR** — breaking changes (incompatible API/behavior).
- **MINOR** — new features, backward-compatible.
- **PATCH** — backward-compatible fixes.

The agent must **autonomously advance the version** as part of each change that
warrants it (no need to wait for the user to ask). Update **all** of these to
keep them in sync:

1. `version.txt` — single source of truth for release metadata (mobile
   version + code + channel, DE version + code + channel — see "Desktop
   versioning" below).
2. `app/build.gradle.kts` — `versionName` (SemVer string) and `versionCode`
   (monotonically increasing integer; the Android requirement is that
   `versionCode` always increases on each release).

When in doubt about which segment to bump, prefer PATCH for fixes and MINOR for
features; only use MAJOR for genuinely breaking changes.

#### Explicit user versioning overrides (per-message)

If the user explicitly states the desired versioning **in a single message**
(e.g. writes "patch", "minor", "major", or "patch/fix are equivalent"), that
message overrides the default SemVer rules **for that message only**. In that
case follow the user's stated segment, treating **patch and fix as the same**
segment (a fix request without an explicit segment defaults to the SemVer
PATCH). The next message returns to the default SemVer behavior unless it
states an override again.

**Which version to bump depends on what changed** (this is the rule the user
considers obvious):

- A change to the **Android app** (`app/`, or an Android-only module/behavior)
  bumps the **mobile** version: `version.txt` line 1 **and**
  `app/build.gradle.kts` `versionName` (+ `versionCode`). Also advance
  `version.txt` line 2 (mobile version code) to match `versionCode`.
- A change to the **desktop edition** (`desktop/`, its build/installer, the
  `.github/workflows/` release pipeline, or a desktop-only behavior) bumps the
  **DE** version: `version.txt` line 4 (+ line 5 version code by 1).
- A change that affects **both** editions bumps **both** versions.
- A change that touches **only** the website (`.websitede/` content — pages,
  styles, scripts, images) bumps **no** version: no DE bump, no mobile bump,
  and the commit is **not** prefixed with `v` (it's not a release signal).
  Only if the same change also touches app code, build/installer config or
  release workflows does the usual DE/mobile bump apply.

Never bump the DE version for a mobile-only change, and never bump the mobile
version for a DE-only change.

#### Android (APK) versioning

The **Android version uses its own scheme, independent of SemVer**: only the
**last digit increments** on every APK update (e.g. `6.0.6` → `6.0.6.1`
→ `6.0.6.2`). The `versionCode` is a monotonic integer that must never
decrease (users must always be able to update without uninstalling). Keep
`version.txt` (lines 1-2), `app/build.gradle.kts` (`versionName` /
`versionCode`) and the release tag (`<mobile>_DE-<de>[-<channel>]`) in sync.
The **DE program follows standard SemVer** (`MAJOR.MINOR.PATCH`) as described
above.

#### Desktop versioning (`<mobile>_DE-<de>` + channel)

Desktop releases are distinguished from Android releases with a combined
version of the form `<mobile>_DE-<de>` (e.g. `6.0.5_DE-1.0.0`):

- `6.0.5` is the Android (mobile) version the desktop is paired with; `1.0.0`
  is the desktop ("DE") version — the program's own SemVer.
- `version.txt` holds the release metadata on **six lines** (comment lines
  prefixed with `#` may follow): line 1 = mobile version, line 2 = mobile
  version code, line 3 = mobile release channel, line 4 = DE version, line 5 =
  the desktop **version code** (a small monotonic counter matching the number
  of DE releases, e.g. `57` — shown in the About screen, and bumped by 1 on
  every DE release), line 6 = DE release channel. The Android app version also
  stays numeric in `app/build.gradle.kts` (`versionName` / `versionCode`,
  e.g. `6.0.5` / `57`).
- Release channels (lines 3 and 6): the **DE** channel (line 6) drives the
  desktop release — `stable` (or empty) publishes a stable GitHub release;
  any other value (`rc`, `beta`, `alpha`, `nightly`, …) publishes a
  pre-release. The channel is shown (uppercased) in the About screen. The
  mobile channel (line 3) is informational for the Android side.
- The GitHub release title and desktop artifact filenames use the full
  version (`VIVIMusic-6.0.5_DE-1.0.0-setup.exe`, …). Release **tags carry no
  `v` prefix**: stable releases use the bare version (`6.0.5_DE-1.0.0`), while
  non-stable releases append the channel (`6.0.5_DE-1.0.0-nightly`). The `v`
  prefix is used **only** in commit messages, as the auto-release trigger.
- Windows/macOS installers need a purely numeric `MAJOR.MINOR.PATCH`
  (jpackage JDK-8283707; Inno Setup `AppVersion` too), so the
  **installer/package version is the DE version** (`1.0.0`, the part after
  `DE-`). `desktop/build.gradle.kts` derives both values from `version.txt`
  (`fullVersion` for display, `numericPackageVersion` for jpackage) and
  generates `AppInfo` so the About screen can show `fullVersion` + channel.
  Keep that derivation in place — do not put the full `_DE-` version into
  `packageVersion`.

### CHANGELOG.md — Keep a Changelog

Update `CHANGELOG.md` on **every important change**, following
[Keep a Changelog](https://keepachangelog.com/). Use exactly these sections:

- `Added` — for new features.
- `Changed` — for changes in existing functionality.
- `Deprecated` — for soon-to-be-removed features.
- `Removed` — for removed features.
- `Fixed` — for bug fixes.
- `Security` — in case of vulnerabilities.

Keep an `## [Unreleased]` section at the top; when a version is released,
convert it to a dated entry (`## [X.Y.Z] - YYYY-MM-DD`) and add the new version
to the top of `CHANGELOG.md`. Omit sections that have no entries.

**Desktop-specific entries are marked with `[DE]`** (e.g.
`- [DE] New desktop feature.`), so desktop and Android changes stay
distinguishable in the changelog. Desktop releases use the combined
`<mobile>_DE-<de>` version (`## [6.0.5_DE-1.0.0] - …`).

### Mandatory pre-commit checklist — `version.txt` + `CHANGELOG.md` + `TODO.md`

Before creating **any** commit that touches program code, build files,
workflows, installer/assets or anything release-affecting, run through this
checklist. Every item is verified with a real command (`git diff` / `cat`),
never from memory:

1. **`version.txt` bumped — and in the SAME commit as the code?**
   - DE-only change → line 4 (DE version, SemVer) **and** line 5 (DE version
     code +1); mobile lines 1–3 untouched.
   - Mobile-only change → line 1 (mobile version) **and** line 2 (mobile
     version code); DE lines 4–6 untouched, and `app/build.gradle.kts`
     `versionName`/`versionCode` kept in sync.
   - Both editions → both pairs.
   - Verify: `git diff version.txt` actually shows the new values. Forgetting
     this has already produced releases with stale versions — the fix must
     never be left to the user.
2. **`CHANGELOG.md` updated?**
   - New dated section `## [<mobile>_DE-<de>-<channel>] - YYYY-MM-DD`
     directly under `## [Unreleased]`, entries in strict descending version
     order, correct `Added`/`Changed`/`Fixed`/… heading, changes marked
     `[DE]` and/or `[APK]`.
   - Verify: `git diff CHANGELOG.md` shows the new versioned section.
3. **`TODO.md` updated?**
   - Completed items marked `[x]`, started-but-unfinished `[~]`, new pending
     work added. Never leave it stale after a change.
   - Verify: `git diff TODO.md`.
4. **Compile the affected module** (`./gradlew :desktop:compileKotlin`,
   `:app:compileUniversalGmsDebugKotlin`, …) and get BUILD SUCCESSFUL.
5. **Commit title**: code/release-affecting commits use
   `v<full version from version.txt>: <short description>`; docs-only commits
   may use `docs:`. Short title, details in the body, **no agent co-author
   footer** (see §3).
6. **Final check before push**: `git status --short` shows only the intended
   files staged, and `git log -1 --stat` shows `version.txt`, `CHANGELOG.md`
   and `TODO.md` together with the code changes.

The checklist is not optional: skipping items has already caused releases
published with an un-bumped `version.txt` and changelog entries that never
appeared.

## 6. Localization (multilingual support)

The app is translated through Android string resources. **English is the
primary language** (the source of truth); every other language is a
translation of it.

The **desktop edition** is English-first too, using the same 49-language list
(locale tag → native name) in
`desktop/src/main/kotlin/com/music/vivi/desktop/Languages.kt`, with strings in
`Localization.kt` (English source of truth; other languages fall back to
English until translated). The language is chosen on first launch and can be
changed from the desktop Language menu.

> **Rule (always)**: when you modify code you MUST complete ALL missing
> translations for every new or changed string across all supported languages —
> never leave a key with an English-only fallback. For the desktop edition,
> add the missing entries to the `EXTRA_TRANSLATIONS` tables under
> `scripts/desktop_extra_translations*.py` and re-run
> `python3 scripts/generate_desktop_localization.py` so `Localization.kt` stays
> complete, then compile `:desktop`.

> **Translations-only work = patch (always)**: when a message consists only of
> translating strings (no code/feature change), it is ALWAYS a patch — even if
> no explicit "patch" label is given. Follow the user's explicit versioning
> when stated in the message; otherwise semver defaults apply. Never bump a
> minor/major for translations alone.

### Structure

- `app/src/main/res/values/strings.xml` — **default/English** strings.
- `app/src/main/res/values-<locale>/strings.xml` — one folder per language
  (e.g. `values-it/`, `values-de/`, `values-zh-rCN/`).
- Some folders also contain `vivi_strings.xml` and `updater_strings.xml`
  (app-specific and updater strings). Keep the same set of files per language
  as English when adding new translatable strings.
- The list of **selectable app languages** lives in code, in
  `app/src/main/kotlin/com/music/vivi/constants/PreferenceKeys.kt`, in the
  `LanguageCodeToName` map (locale tag → display name).

### How to add a new language

1. Create the resource folder for the locale, e.g.
   `app/src/main/res/values-<locale>/`, and add a `strings.xml` that translates
   every key from `values/strings.xml`. Do **not** invent new keys; translate
   the existing English keys.
2. Add the language to the `LanguageCodeToName` map in `PreferenceKeys.kt` so it
   appears in the language picker.
3. If the language was requested but is not in the supported list below, confirm
   with the user first.

### Supported languages

English is the base language. The supported translations are (display name →
locale tag):

| Language | Locale |
|---|---|
| English (primary) | `values/` |
| Azərbaycan dili | `az` |
| Bosanski | `bs` |
| Català | `ca` |
| Čeština | `cs` |
| Deutsch | `de` |
| Eesti | `et` |
| Español | `es` |
| Euskara | `eu` |
| Filipino | `fil` |
| Français | `fr` |
| Hrvatski | `hr` |
| Bahasa Indonesia | `id` |
| Italiano | `it` |
| Lietuvių | `lt` |
| Magyar | `hu` |
| Bahasa Melayu | `ms` |
| Nederlands | `nl` |
| Norsk bokmål | `nb` |
| Polski | `pl` |
| Português | `pt` |
| Română | `ro` |
| Slovenčina | `sk` |
| Slovenščina | `sl` |
| Српски | `sr` |
| Suomi | `fi` |
| Svenska | `sv` |
| Tiếng Việt | `vi` |
| Türkçe | `tr` |
| Ελληνικά | `el` |
| Беларуская | `be` |
| Български | `bg` |
| Русский | `ru` |
| Українська | `uk` |
| العربية | `ar` |
| हिन्दी | `hi` |
| অসমীয়া | `as` |
| বাংলা | `bn` |
| ਪੰਜਾਬੀ | `pa` |
| தமிழ் | `ta` |
| తెలుగు | `te` |
| മലയാളം | `ml` |
| ไทย | `th` |
| ខ្មែរ | `km` |
| 한국어 | `ko` |
| 简体中文 | `zh-rCN` |
| 繁體中文 | `zh-rTW` |
| 日本語 | `ja` |

## 7. GitHub Issues workflow — MANDATORY

Every user-reported problem or feature request MUST first become a GitHub
issue on `PiBOH/vivi-music` **before any code is changed**:

1. **Check for duplicates first**:
   `gh issue list --repo PiBOH/vivi-music --state all --search "<keywords>"`
   — only open a new issue when no equivalent open/closed issue exists.

2. **Open the issue first** (via the `gh` CLI; on this machine it is not on
   the bash PATH, use the full path `/c/Program Files/GitHub CLI/gh.exe`,
   or plain `gh` elsewhere). Follow the repo's issue templates in
   `.github/ISSUE_TEMPLATE/`:
   - Desktop (Windows/Linux/macOS) bug → `bug_report_de.yml`
     (`title: "[Bug][DE]: "`, labels `bug`, `triage`, `desktop`).
   - Desktop feature → `feature_request_de.yml`
     (`title: "[Feat][DE]: "`, labels `enhancement`, `triage`, `desktop`).
   - Mobile (Android) bug → `bug_report.yml` (`title: "[Bug][APK]: "`).
   - Mobile feature → `feature_request.yml` (`title: "[Feat][APK]: "`).
   Fill in every required field of the template (category, frequency, steps
   to reproduce, …); use `N/A` when a field does not apply.
   **Issue bodies and titles are written in English.**

3. **Fix the problem**, then reference the issue in the CHANGELOG entry and in
   the commit message (e.g. `Closes #NN` / `Fixes #NN`).

4. **Close the issue** after the fix is committed and pushed:
   `gh issue close <NN> --repo PiBOH/vivi-music`.

**NEVER open a GitHub issue for anything involving secrets** (keystores,
signing keys, API tokens, passwords, credentials, secret names/values,
signing-key selection in workflows, etc.). Secrets-related changes are done
directly, without an issue — they must not leave any trace on GitHub (no
issue, no issue link in the CHANGELOG/commit, and never log or echo secret
values). Keep secret material only in gitignored files (`.ignore/`,
`*.b64`, `*.keystore`).

Existing commit rules still apply: never add a "Co-Authored-By: Codebuff"
footer.

### Language rules

- **Chat replies** are always written in the **same language the user
  wrote the message in** (never forced to English or Italian).
- The **app/program text** is English-first and then translated into the
  other languages through the localization system (section 6).
- **GitHub issues** (title + body) are always written in **English**.
- The CHANGELOG is always written in English.

## 8. Ask before assuming — MANDATORY

Whenever there is **any doubt — even a minimal one** (intent, scope, versioning
type, branch to touch, wording of a string, which platform is affected, …),
the assistant MUST ask the user instead of assuming. Guessing is a bug. The
user explicitly requires being asked about every uncertainty, no matter how
small ("OBBLIGO DI CHIEDERE PER QUALSIASI DUBBIO, ANCHE MINIMO").

This applies before and during changes, and also to claims made in replies:
never state that something "is" a certain way unless it has been verified in
the code/configuration or confirmed by the user.
