# TODO — VIVI Music DE (desktop) + Android ↔ Desktop sync

Legend: `[x]` done · `[ ]` to do · `[~]` in progress

What shipped and *why* (with the constraint every fix carries) is in
`CHANGELOG.md`. This file is only what is still open, one line per release for
orientation.

## Open

### Audio (Windows)
- [ ] **A scheduling freeze longer than ~1 s is still audible.** Java Sound's device ring is hard-capped at 1000 ms there (measured: any request above 1 s is granted exactly 176400 bytes; macOS grants 4 s) and MMCSS now covers the ~1 s range. Beyond that it means a native WASAPI render path (event-driven, own ring) or a bigger ring in the current backend. **Do not start without a measurement:** the logs must first show the writer frozen *outside* `out.write()`.

### Performance (Windows)
- [ ] **Playing still costs ~0.5 core** on the reporting machine (`AWT-EventQueue-0` ≈ 3.1 s CPU in 6 s, Skiko OpenGL redrawer vs `dwmFlush`). The per-tick recomposition went in 1.50.71; what is left is the renderer itself (candidates: software/ANGLE backend, lower animation rate) (#3).
- [ ] **The setup is ~129 MB and cannot go below ~100 MB** while the sign-in WebView ships: `javafx-web` ~31 MB, `icudtl.dat` 10 MB, `skiko` 12 MB, the 78 MB jlink runtime. Levers left: subset the four bundled fonts, shrink the jlink image.

### Lyrics
- [ ] **An "exact video" timed source for the tracks the community servers do not index.** The captions live in the player response (`captions.playerCaptionsTracklistRenderer.captionTracks[].baseUrl`), which needs a model in the shared `innertube` module (#80). Mobile's `YouTubeSubtitle` was removed in 1.50.70 because the endpoint did not answer.

### UI parity (mobile → desktop)
- [ ] Album / artist / playlist context menus (the song menu — like, library, add to playlist, share — is done).
- [ ] Gradient header on Album / Artist / Playlist (today a plain row).
- [ ] Swipe / canvas thumbnails (the rotating one is done).
- [ ] Spotify import, JioSaavn, the notification-permission row.

### Infra
- [ ] **`vivi-music-de-apk` still carries the desktop sources and stale leftovers** (`.websitede/`, `NewUI_desktop.zip`, `Changelog`, `News`, the superseded workflows) next to the Android app; it should hold only the app and the modules it compiles against.
- [ ] `WINDOWS_SIGNING_CERT` + `WINDOWS_SIGNING_PASSWORD` secrets, to sign the Windows installer.
- [ ] **Windows media flyout (SMTC):** needs WinRT COM interop (`ISystemMediaTransportControlsInterop::GetForWindow` + hwnd) that cannot be validated without a Windows machine — deferred, do not ship blind.
- [ ] **OBS / screen capture:** window capture works in both chrome modes (verified with BitBlt); waiting for the exact symptom (window missing from the list vs black preview) before documenting the WGC method.
- [ ] **End-to-end encryption (Phase 7):** per-pair key exchanged at pairing, snapshots encrypted before the relay sees them.

## Done — one line per release

- [x] **DE 1.53.18** — the liked songs travel between phone and desktop (#96); the playlist copies a pairing left behind are collapsed and no second account copy is created (#93)
- [x] **DE 1.53.17** — seek bar drag and snap-back, the `-0:00` countdown, the playlist duplication (E1034), the expressive player's tab + autoplay, the rail toggled by the title
- [x] **DE 1.53.16** — the lyrics "no text" indicator, aligned-line overflow, artwork quality, playlist-sync progress, the whole queue in the device sync, the player picker, the font import, the pairing scan (APK)
- [x] **DE 1.53.15** — full translation audit: no key can leak another language
- [x] **DE 1.53.14** — a playlist can live on YouTube Music too (create, rename, delete propagation)
- [x] **DE 1.53.13 … 1.53.9** — the notifications empty state, the Artists screen, one distinct style per lyrics style, the sidebar/sign-in/transport fixes, the strings of 1.53.6-1.53.8, the UI idling while paused
- [x] **DE 1.53.8 … 1.53.0** — the mobile Content screen and the lyrics controls, the account's playlists mirrored locally, the local history, the mobile settings layout
- [x] **DE 1.52.x** — the audio diagnosis chain (MMCSS, the Skiko `System.gc()`, the ring/gap checks), the `.rpm` + AUR packages, the updater integrity, `settings.json`, the `apk-latest` download path, the website on `gh-pages`
- [x] **DE 1.51.x** — animated lyrics, romanization and AI translation
- [x] **≤ DE 1.50.76** — the desktop foundation: audio playback, persistence, authentication, the UI port, the in-app updater, the installer trimming
- [x] **APK 6.0.6.x** — the companion build (`com.vivi.music.desktop`, "VIVI for DE") and its two-way device sync with the desktop
