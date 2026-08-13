# Porting TODO

Porting plan for **Vivi Music DE** — the desktop port of the Android
[ViVi Music](https://github.com/vivizzz007/vivi-music) client. Items are checked
off as they land; this file is updated on every release so the remaining work is
always visible.

Status legend: `[x]` = done, `[ ]` = pending.

## Phase 1 — Audio engine

- [x] Real desktop playback (LavaPlayer + Java Sound).
- [x] Opus/WebM stream decoding with native codecs.
- [x] Live seek bar, position and duration wired to the engine.
- [x] Detailed startup error reporting (dialog + crash log + copy action).
- [x] Lightweight startup shell with background service initialization.
- [x] Cached desktop settings access to avoid repeated file I/O during startup.

## Phase 2 — Content screens + data layer

- [x] Album detail screen.
- [x] Artist detail screen.
- [x] Playlist detail screen.
- [x] History screen.
- [x] InnerTube album/playlist/artist endpoints and models.
- [ ] Explore screen.
- [ ] Charts screen.
- [ ] New releases screen.
- [ ] Moods & genres screen.
- [ ] Statistics screen.

## Phase 3 — Account / auth

- [x] Supabase email/password sign-in and sign-up.
- [x] Login screen (desktop-adapted).
- [x] Account screen (profile, sign in/out, sync status).
- [x] Activity history screen (listening statistics).
- [x] Persistent session across restarts (file-backed session storage).
- [x] YouTube Music account login (paste cookie) + profile (name/email/avatar).
- [x] Original ViVi Music token-bundle import with cookie sanitization.
- [x] Copy detailed authentication errors to the clipboard.
- [x] Account library (liked playlists, albums, artists) with filter chips.
- [ ] Real Listen Together (currently a placeholder).

## Phase 4 — Player screens + playback controls

- [x] Play queue (next/previous, shuffle, repeat, remove, clear).
- [x] Queue screen.
- [x] Synchronized lyrics screen (LrcLib + LRC parsing).
- [x] Player page switcher (Player / Queue / Lyrics).
- [x] Wired shuffle/previous/next/repeat controls.
- [ ] Equalizer screen.

## Phase 5 — Settings

- [x] Full settings hub with nested pages.
- [x] Appearance (theme mode + accent color).
- [x] Player & audio (quality + toggles).
- [x] Content (language names displayed in each language's own writing).
- [x] Privacy (synchronization toggle).
- [x] Storage (clear search/history).
- [x] Updates (pre-releases + manual check).
- [x] In-app update download with automatic Windows/macOS/Linux asset selection.
- [x] About (mascot, version, source link).

## Phase 6 — Menus and remaining components

- [ ] Long-press / context menus on songs, albums, artists, playlists.
- [ ] Song recognition.
- [ ] Yearly "Wrapped" summary.
- [ ] Remaining screens and components from ViVi Music.

## Already shipped (before the phased port)

- [x] KMP project scaffold, build config, CI/CD and auto-release workflows.
- [x] Shared Compose UI: Home, Search, Library, mini player and full player.
- [x] InnerTube client (search, home, browse, stream resolution).
- [x] Local persistence (Room KMP) + Supabase sync (pull/push + realtime).
- [x] Multilingual support (49 languages).
- [x] Update checker (stable by default, pre-releases optional).
- [x] Axolotl mascot and desktop app icons.
