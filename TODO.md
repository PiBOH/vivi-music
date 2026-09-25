# TODO — VIVI Music DE (desktop) + Android ↔ Desktop sync

Legend: `[x]` done · `[ ]` to do · `[~]` in progress

What shipped and *why* (with the constraint every fix carries) is in
`CHANGELOG.md`. This file is only what is still open, one line per release for
orientation.

## Open

### Audio (macOS)
- [ ] **The audible jump the macOS reporter still hears (#3).** His last export has *no* stall line and a 99-100 % device check with a 4000 ms ring, so nothing in the pipeline we measure starves: either the PCM leaving the decoder is already discontinuous (the AAC sample-table walk — `audio integrity` only counts *overlaps* today) or the machine froze for over 4 s. **Do not guess:** the next export taken *while the jump happens* has to name it (`jvm stall`, `audio priority stall`, `audio cushion low`, `audio integrity`).

### Audio (Windows)
- [ ] **A scheduling freeze longer than ~1 s is still audible.** Java Sound's device ring is hard-capped at 1000 ms there (measured: any request above 1 s is granted exactly 176400 bytes; macOS grants 4 s) and MMCSS now covers the ~1 s range. Beyond that it means a native WASAPI render path (event-driven, own ring) or a bigger ring in the current backend. **Do not start without a measurement:** the logs must first show the writer frozen *outside* `out.write()`.

### Audio (macOS, #3)
- [ ] **The reporter still hears it on 1.53.20, and 1.53.22 changes exactly one thing about the start of a track** — the device is primed with a real second instead of 0.3 s (his export: `device started with 278ms already queued` followed by a 325-556 ms writer pass, i.e. the ring could not absorb the pass that followed the start). His next export is what decides: `audio output primed: … — reached the 1000ms target` with no `cushion low`/`device ran dry` line means the start is covered; a `plateau` in the priming line means his backend refuses a deeper cushion while stopped (then the ring size itself is the bound and the next step is an event-driven output, not another cushion change); a genuine `audio writer stalled` line *during* steady playback with the cushion high would be the first real evidence of a frozen thread on macOS, which 24 sessions have not shown yet.

### Performance (Windows)
- [ ] **Playing still costs ~0.5 core** on the reporting machine (`AWT-EventQueue-0` ≈ 3.1 s CPU in 6 s, Skiko OpenGL redrawer vs `dwmFlush`). The per-tick recomposition went in 1.50.71; what is left is the renderer itself (candidates: software/ANGLE backend, lower animation rate) (#3).
- [ ] **The setup is ~129 MB and cannot go below ~100 MB** while the sign-in WebView ships: `javafx-web` ~31 MB, `icudtl.dat` 10 MB, `skiko` 12 MB, the 78 MB jlink runtime. Levers left: subset the four bundled fonts, shrink the jlink image.

### Lyrics
- [ ] **An "exact video" timed source for the tracks the community servers do not index.** The captions live in the player response (`captions.playerCaptionsTracklistRenderer.captionTracks[].baseUrl`), which needs a model in the shared `innertube` module (#80). Mobile's `YouTubeSubtitle` was removed in 1.50.70 because the endpoint did not answer.

### Translations
- [ ] **Azerbaijani: the desktop is clean, the phone's long paragraphs are not.** Every string the desktop shows is proper Azerbaijani now (the batch 83 pass), but `app/src/main/res/values-az/vivi_strings.xml` still holds ~250 word-by-word ones like "göstər sıxlıq dəyiş will take effekt sonra restarting tətbiq. Do siz want -a yenidən başlat now?" — multi-sentence descriptions that need a native Azerbaijani pass, not a guess from here. They only reach the desktop through the keys it maps to an Android resource, and those were fixed in the extras batch; on the phone they show as-is. The generator's no-op guard already stops such a string from silently *becoming* the desktop wording, so the remaining work is visible instead of hidden.

### UI parity (mobile → desktop)
- [ ] Album / artist / playlist context menus (the song menu — like, library, add to playlist, share — is done).
- [ ] Gradient header on Album / Artist / Playlist (today a plain row).
- [ ] Swipe / canvas thumbnails (the rotating one is done).
- [ ] Spotify import, JioSaavn, the notification-permission row.

### Infra
- [ ] **`vivi-music-de-apk` still carries the desktop sources and stale leftovers** (`.websitede/`, `NewUI_desktop.zip`, `Changelog`, `News`, the superseded workflows) next to the Android app; it should hold only the app and the modules it compiles against.
- [ ] `WINDOWS_SIGNING_CERT` + `WINDOWS_SIGNING_PASSWORD` secrets, to sign the Windows installer.
- [ ] **Windows media flyout (SMTC):** needs WinRT COM interop (`ISystemMediaTransportControlsInterop::GetForWindow` + hwnd) that cannot be validated without a Windows machine — deferred, do not ship blind.
- [x] **OBS / screen capture:** window capture works in both chrome modes (verified with BitBlt); waiting for the exact symptom (window missing from the list vs black preview) before documenting the WGC method.
- [ ] **End-to-end encryption (Phase 7):** per-pair key exchanged at pairing, snapshots encrypted before the relay sees them.

## Done — one line per release

- [x] **DE 1.53.24** — the translations as one file per language (the APK's `values-<lang>/strings.xml` shape, proven identical to the old tables language by language), the history row without a track length, the artists tab saying its list can take ten seconds
- [x] **DE 1.53.23** — the sidebar logo and the tray icon (an all-transparent vector render, so the tray never appeared), the two expressive finishes as entries of the player picker, a YouTube Music playlist that already exists is updated instead of uploaded again, the lyrics cache bumped to v7 for the styled renderer
- [x] **DE 1.53.22** — the device is primed with a real cushion instead of 0.3 s (#3), the macOS tile artwork follows the track instead of sticking to the first song (#63), and two diagnostics that misled this investigation are fixed
- [x] **DE 1.53.21** — the swipe switch and sensitivity slider, the "no text" dots starting too early (mobile's rule, no estimate), the frozen now-playing bars, the translucent-tab player variant, the VIVI mark as a vector, the translation audit (bare "listening" in 49 languages, English and half-English strings)
- [x] **DE 1.53.20** — macOS: the "Now Playing" tile is claimed at launch with a restored (paused) queue (#63); the lyrics "no text" marker is three horizontal dots instead of the ring
- [x] **DE 1.53.19** — the expressive player's lyrics-menu crash, the queue's "+" moved into the ⋮ menu, sharp tray/sidebar logos, the "no text" indicator on plain LRC, the now-playing bars on the cover, the Library live refresh, the Artists list, the duplicates left in the sidebar, the position after a skip
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
