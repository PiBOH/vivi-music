# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Desktop-specific changes are marked with `[DE]`. Desktop releases use a combined
version `<mobile>_DE-<desktop>` (e.g. `6.0.5_DE-1.0.0`), where the desktop part is
the program's own SemVer. `[APK]` marks mobile-only changes.

## [Unreleased]

## [6.0.6.8_DE-1.53.24-alpha] - 2026-09-25

### Changed
- [DE] **The translations are one file per language now, the way the APK keeps them.** The 53 maps lived in nine numbered `LocalizationTablesN.kt` files of six languages each — sliced that way for a JVM limit (a class file is capped at 64KB, which `ClassTooLargeException: Class too large: LocalizationKt` hit once the lyrics keys were completed), a boundary the compiler has and not one a reader has. Answering "what does the desktop say in Italian?" meant importing nine files and knowing which slice held Italian. The generator now emits `Localization_it.kt`, `Localization_ja.kt`, … one language each (the counterpart of the APK's `app/src/main/res/values-<lang>/strings.xml`), referenced from `Localization.kt`, and each file's docstring says what it holds **and what it deliberately does not** (keys whose Android resource is translated, which come from that resource via the generator's `MAPPING`). Verified rather than assumed: the tables were copied aside first, and comparing every entry line language by language reports *53 comparisons, 0 problems* — same strings, no key lost, added or reworded. **Constraint:** `check_localization.py`/`audit_desktop_localization.py` find the tables by file name, so a layout change must update them too (both are clean on the result: 0 missing/leaking, 0 values in a script the language does not use, 0 English left).

### Fixed
- [DE] **The history drew a track length on some rows and nothing on the others.** Both kinds of row share `SongRow`, and the local rows are built from this machine's own records (which know the length) while the rows the account returns do not — so the same list showed `3:42` on one line and nothing on the next, which reads as a bug. The length is off for the whole history, switched on the *row* rather than by emptying the durations, because a history answers "what did I play", not "how long is it". Every other list keeps its lengths.
- [DE] **The artists tab can take seconds to appear and looked stuck while it did.** It is the one tab whose list has to be derived from the songs when the account does not return an artist list (see `loadLibraryPage`). `LoadingBox` takes an optional hint line now and the artists tab says *"Loading can take up to 10 seconds"*. The hint is a new desktop-only key, so its English is in the generator's inline map and all 52 other languages are in the new `desktop_extra_translations_84.py` — a language left out would print the English sentence, which is exactly what the translation audits exist to catch.

## [6.0.6.8_DE-1.53.23-alpha] - 2026-09-25

### Fixed
- [DE] **The sidebar logo and the tray icon came from a fully transparent render.** The vector mark shipped in 1.53.21 was rasterized with the SVG's container size set to the *target* size, and the Skia bundled with Compose Desktop (0.9.37.3) answers that with a perfectly empty image at any size below 256 px — measured: **0** opaque pixels at 16, 26, 32, 48, 64 and 128 px, and at 256 px only 20 964 of the expected 51 489. So the sidebar had nothing to draw and the tray received an empty icon, which is why enabling "tray icon" produced no tray icon at all ("non è che non mi mostra l'icona, la tray proprio non appare"): an all-transparent tray image is an empty slot, and Windows shows nothing for it. The container is now the mark's own 1024 — what its `viewBox` says — and the *canvas* is scaled to the requested size, which renders correctly at every size (26 px: 537 opaque pixels of 676, the expected 79 %). An empty raster is also treated as a failure now rather than as a logo, so the PNG fallback is reached instead of nothing, and the decoded PNG is copied into `TYPE_INT_ARGB` because `toComposeImageBitmap` accepts no other type — with the old type the sidebar would have failed even on a good render.
- [DE] **The two expressive players are picked from the dropdown now, not from a switch.** The translucent Queue / Lyrics / History tab was a switch on the Player-design page that only drew itself when the expressive player was selected, so in the other two layouts it was invisible — the opposite of what the request was: the two expressive finishes should be *two entries of the player picker*. `PlayerDesign` has "Expressive" and "Expressive" with the translucent tab as separate entries, the dropdown lists all four players, and nothing else toggles the tab. The two labels are built from two keys that are already translated in all 52 languages (the player's name and the tab finish) instead of a brand-new key, so neither entry prints English anywhere. The old boolean is still written in step and a settings file that reads "expressive + translucent tab" is migrated to the translucent entry on load, so the finish is not silently lost when upgrading.
- [DE] **An upload updates the YouTube Music playlist that is already there instead of creating a second one.** The only check was against a *local* row that already knew the account playlist id: a playlist created on the phone, on another computer or in the YouTube Music web UI has no local row here, so uploading it created a second playlist with the same name, and the account then showed both. The account's own playlists are consulted before anything is created — one list request per minute instead of one per playlist, since a bulk upload would otherwise ask once for each — and a name match adopts that playlist and pushes only the songs it does not already hold. The songs already on the account copy are read first because a playlist accepts the same song twice.
- [DE] **Lyrics are refetched when the cached copy predates the styled renderer.** A cached lyric is served without asking any provider again, so an entry written by an earlier resolver keeps rendering with the old text after the renderer changes — which is what "the lyrics style isn't always loaded" looked like: the same setting gave a different result depending on whether the track had been played before. The cache version is bumped to v7 and those entries are refetched once, as the file's own rule requires ("any change to how a provider is picked or validated MUST bump this version").

## [6.0.6.8_DE-1.53.22-alpha] - 2026-09-25

### Fixed
- [DE] **The device now starts with a real cushion, not a third of a second (#3).** The reporter's 1.53.20 export on macOS names the mechanism in three lines that sit right next to each other: `audio output primed: device started with 278ms already queued in its buffer (ring ~4000ms)`, then `audio writer stalled: 325ms for one pass with only 0ms of it inside the device write … (cushion 278ms)`, and in another session the same pair with **556 ms**. One pass of the writer can take longer than the cushion the device was started with, so the ring empties — the "few ms pause, on almost every song" of this issue, and the mini UI hitch he sees at the same moment is that same pass. The priming target was `0.3 s`; it is now a full second, never more than half of what the backend actually granted (Windows caps the ring at 1 s, macOS grants 4 s). Equally important, the exit from "wait for the cushion" no longer fires on a momentary empty PCM queue: on a cached track the decoder hands its first fragments over in bursts, and the log shows 278 ms was chosen on the wrong side of one such burst. The ring now has to stop **growing** for 400 ms while the producer had audio to give, so a backend that refuses more while the line is stopped still starts promptly, and the log says which of the three reasons fired. (`audio integrity` stayed at 0 sample-table overlaps and 0 device stalls in the same export, so the PCM is not the suspect here.)
- [DE] **The macOS "Now Playing" tile kept the first song's artwork (#63).** The reporter's follow-up: registration works now, but "the current playing song image is not getting changed, so it stays always as a very first song played". The download wrote every track to the same file — `~/.vivimusic/macos-artwork.jpg` — and the native layer decodes an image once and caches it **per path** (`g_artworkLoadedPath` in `ViviMediaSession.m`), so pushing the same path back handed the system the picture it had already loaded. The file name is now derived from the artwork URL (short SHA-1 of it, original extension), so the path changes with the track, the native cache misses and the tile is redrawn — while position ticks, which re-push the same metadata, still hit the same file and decode nothing twice. Older artwork files are deleted as each new one lands.
- [DE] **Two diagnostics that sent this investigation the wrong way twice.** (1) The pass that opens the device is slow by nature — first write, CoreAudio open, JIT of the whole write path — and it was *the only* pass the export flagged as a stall (`325 ms`, `515 ms`, `556 ms`, each immediately after `audio output primed`). It is no longer reported as a frozen thread. (2) A line whose frame counter moved *backwards* is a new output line, not a stalled sound card: that is how `the sound card played only 3136ms of audio in 18350ms of wall time (17%)` was produced while a 3.1 s cushion sat unplayed in the ring. The window is restarted now, and the log says so.

## [6.0.6.8_DE-1.53.21-alpha] - 2026-09-25

### Fixed
- [DE] **"Enable swipe to change song" could not be switched off and the player's swipe-sensitivity slider could not be moved.** Both controls are drawn by `PlayerDesignScreen`, which has taken `swipeThumbnail`, `onSwipeThumbnailChange`, `swipeSensitivity` and `onSwipeSensitivityChange` all along — but `SettingsPlayerDesignScreen`, the wrapper the real Settings screen goes through, was built without them. Both therefore sat on their defaults: the switch snapped back on as soon as it was touched and the slider ignored every drag. The wrapper now takes and forwards all four, and the Settings call site persists them (`swipeThumbnail`, `swipeSensitivity`), so the gesture (`swipeToChangeTrack`, 90 dp at 0.0 → 18 dp at 1.0) follows the slider.
- [DE] **The "no text" dots appeared while the line was still being sung.** The break marker is only drawn when the lyrics genuinely stop saying words, and the desktop was guessing: it carried a `estimatedSungEndMs` fallback (last word's start + a per-word estimate) for entries with no `endTime`, so the dots came up over a line that was still in progress. The rule is now exactly mobile's `mergedLyricsList`: a break exists only when the last word has an `endTime` (or the entry's own time when the text is blank), the gap must exceed 4000 ms, and **there is no estimate** — a line whose end we cannot prove gets no marker at all. The indicator also collapses and fades with the auto-scroll setting, the way `IntervalIndicator` does on the phone.
- [DE] **The now-playing bars on the cover and in the queue did not move.** They scaled straight off the audio level (`smooth * maxH`) with a 3 px floor, and music RMS sits around 0.1-0.3, so every bar was pinned at the floor and the row looked frozen. The height is now `0.35 + 0.65 × smoothed` of the row with an 18 % floor, so quiet passages still breathe and the bars track the beat again.
- [DE] **No second expressive player.** The tab (Queue / Lyrics / History) was always opaque. Player design now offers **Translucent tab**: the same panel, but the player background shows through it. Off is the previous behaviour, and the choice persists like every other player setting.
- [DE] **The VIVI mark was a 1024-px PNG everywhere** — the tray icon and the sidebar were a downscale of it, which is why they never looked sharp. The mark now ships as a vector master (`desktop/icons/logo_vmde.svg`) — the 45° `#536DFE → #6B3DE8` gradient, the three 59-px white rings (the middle one broken by a 36° opening at 3 o'clock) and the bar that bridges inner to outer through it — and `BrandLogo` rasterizes it at exactly the size each caller asks for (Skiko `SVGDOM` into a raster surface), keeping the PNG only as a fallback for a Skiko without the SVG module. Renders within ~1 px of the PNG on every ring edge; the PNG stays the source the SVG was derived from.
- [DE] **The translation audit found two ways a string could show the wrong language.** (1) Filler batches wrote the Android *resource name* where a translation belonged — `wrapped_listening_time` maps to Android's `listening`, and 49 languages printed the bare word "listening" on the Wrapped screen. A value that is the key, the resource name or the English wording is never written now. (2) Strings were English or half-English: `wrapped_show_on_home_desc` was a whole English sentence in 36 languages, `stream_cache_minutes` in 38, and `randomize`, `mini_player_standard` and `wrapped_title` in all 50; "كلمات أغنية Romanize Kyrgyz", "Romanize Kyrgyz mahnı sözləri", "APIキーは、 deepl.com/pro-api for free and paid keys で取得できます", "Pro-grade acoustic tuning at mga epekto", the Azerbaijani and Tagalog tray descriptions mixed two languages in one line. All of them are translated now (batch 83), together with the player-background options that were still English in 9-33 languages (Glow, Gradient, Blur, Off, Pure black) and 36 languages' worth of `discord_presence_desc`, which the first pass missed because that filler was English with a *different wording* than the table's ("…on your Discord profile." against "…(Windows).") and only a detector for English prose caught it. Azerbaijani got its own pass — the desktop strings that glued English words into Azeri sentences ("Aktiv et glowing mahnı sözləri effekt", "Romanize Bulgarian mahnı sözləri", "Randomize Əsas səhifə Ekran Order", "Send İndi oxudulur") are proper Azerbaijani now, in the desktop batch and in the mobile resources the two share.

## [6.0.6.8_DE-1.53.20-alpha] - 2026-09-24

### Fixed
- [DE] **macOS: the system "Now Playing" tile appears at launch with a track restored from the persistent queue.** macOS grants the tile and the media-key routing to an app it has seen *playing*, and the persistent queue comes up **paused**: a paused claim on a freshly launched process is not enough — which is exactly what the reporter worked out on [#63](https://github.com/PiBOH/vivi-music-de/issues/63) ("you need to play and stop the current queue song to register it"). The app now walks the same two states a real play-and-stop does at startup — one push as *playing*, then the real paused state after 1.2 s — **in the metadata only**: the restored track stays paused at 0:00, no audio is started and the player is untouched. It runs at most once per process, and the sequence is logged (`startup claim: …`) so a future report can prove whether it ran. (Closes [#63](https://github.com/PiBOH/vivi-music-de/issues/63))
- [DE] **The "no text" indicator in the lyrics is three horizontal dots, not a ring.** The instrumental break used to draw a 36 dp grey ring filling up (a copy of the mobile `IntervalIndicator`, which is a *wavy* circle — what came out here was a plain ring that reads as a broken spinner). It is now a row of three dots that light up with the break's own progress (the first at a quarter, the second at half, the third at three quarters), the way a "this part has no lyrics" marker should look, and the row keeps its 36 dp height so nothing jumps.

## [6.0.6.8_DE-1.53.19-alpha] - 2026-09-24

### Fixed
- [DE] **The expressive player no longer crashes when the lyrics options are opened.** The lyrics menu (`LyricsQuickMenu`, the ⚙ button inside the player's lyrics panel) put `Modifier.verticalScroll` on the `DropdownMenu` itself; material3's menu *already* scrolls its own column, so the user scrolled our scroll, measured inside the menu's one with an unbounded height — and Compose refuses that (“Vertically scrollable component was measured with an infinity maximum height constraints”), which is the crash in the report (`crash.log`, 6.0.6.7_DE-1.53.17, `AWT-EventQueue-0`). The extra modifier is gone: the menu keeps scrolling, with the scroll state material3 owns. (Closes [#97](https://github.com/PiBOH/vivi-music-de/issues/97))
- [DE] **The “＋” next to the three dots is gone, and adding to the queue moved into the ⋮ menu.** On every song row of the library, of a playlist and of the history there was a bare `＋` glyph to the right of the ⋮: it *did* add the song to the queue, but with no icon, no tooltip and no feedback it read as a dead button (and the queue panel's header carried a second one, an icon with an empty `onClick` that really did nothing). The dead one is deleted, and *Add to queue* is now a named entry with an icon in the menu that already holds *Add to playlist* and *Share*. (Closes [#98](https://github.com/PiBOH/vivi-music-de/issues/98))
- [DE] **The tray icon and the logo in the sidebar are sharp.** Both drew the bundled 1024 px mark scaled down in a *single* bilinear step — to 16 px for the tray and to 26 dp for the sidebar, which is where the artwork loses the mark's gradients and its white rings. The logo is now resampled by repeated halving (each pass averages whole 2x2 blocks, alpha-weighted so the transparent edge does not darken the outline) down to the size actually drawn × the display density, the sidebar and the welcome screen draw it with `FilterQuality.High`, and the tray icon is generated for the *device* pixels (tray size × the display scale) instead of a 16 px image handed over to be scaled again. (Closes [#99](https://github.com/PiBOH/vivi-music-de/issues/99))
- [DE] **The lyrics show the “no text” indicator on plain LRC files too.** The gap indicator could only be computed from word timings (or from an empty line), and the lyrics most users get from the providers are a plain per-line LRC with no word timings at all — so on exactly those songs the hole in the lyrics drew nothing, while the mobile renderer showed it. A line with no word timings now ends where its own words end (about 500 ms per word, at least 1.2 s), capped at the next line, so the estimate can only make a hole *shorter* — it never invents one inside continuous singing. Verified against a cached file from the reporter's own cache (`[00:51.901] We live, we love, we lie` → next line at `00:59.453` now yields the indicator). (Reopens and closes [#83](https://github.com/PiBOH/vivi-music-de/issues/83))
- [DE] **The three “now playing” bars sit on the cover and they move.** The indicator was drawn *beside* the artwork of the current row (and only in the library-style rows): the queue, the up-next panel and the player's queue drew no indicator at all. It is now drawn over the cover of the current row, on a translucent plate so it stays legible on any artwork, in the library rows, in the up-next rows and in the queue screen — and it is driven by the live decoded level of the player (`LocalPlayback.audioLevel`), so it wobbles with the music instead of looking like a static icon. (Closes [#100](https://github.com/PiBOH/vivi-music-de/issues/100))
- [DE] **The Library screen refreshes when the library changes.** The screen loaded its page once and then sat on it: liking a song from the player, from a list row or from the paired phone (and unlike, and a playlist edit) changed the account while the tab kept showing the old list until it was left and reopened. The places that change the library now bump a revision the screen watches, and a refresh of the tab already on screen keeps the list visible while the new page is fetched instead of flashing through the loading state. (Closes [#101](https://github.com/PiBOH/vivi-music-de/issues/101))
- [DE] **The Artists screen lists the artists, not just one.** The corpus page (`FEmusic_library_corpus_artists`) of an account that follows no artist is empty — verified: `library FEmusic_library_corpus_artists → 0 item(s)` — and the fallback read the *liked songs* page only, which on that account is empty as well, so the screen fell back to a cache holding one artist from an older session. The derivation now also walks the **account's playlists** (8 per visit, cached for good): their songs carry their artists' channel ids, so the artists the user actually keeps are listed and open like any other. (Closes [#102](https://github.com/PiBOH/vivi-music-de/issues/102))
- [DE] **The playlists that exist twice on YouTube Music are listed once in the sidebar.** The sidebar hides the account's playlists it already has locally, but it only looked at the **live** local ones: the copy a merge had just retired is a tombstone and its account playlist was therefore listed again — which is why the duplicates were gone everywhere *except* the sidebar. The filter now also uses the account ids of the retired copies and the names of the active ones (the pair of same-name playlists a two-device sync used to create has a different id on each device). (Closes [#93](https://github.com/PiBOH/vivi-music-de/issues/93))
- [DE] **Skipping a track now syncs the position to the paired phone.** When the desktop applied a playback snapshot from the peer it recorded it *after* the player had already changed (`noteQueueApplied` after `applyRemotePlayback`), so the desktop's own state handler saw a queue it did not know and stamped a **fresh** last-write-wins time on the peer's own change — which then vetoed the peer's next one (“next does not sync, previous does”: the change right after is the one that gets rejected). The applied snapshot is now recorded before the player changes, the order the mobile app already used. The sync log also prints what the decision is made of (`pos`, `seek`, the peer's `queueAt` and the local one on receive; `pos`/`seek` on send) so a future report of this kind can be read off the log instead of guessed. (Closes [#103](https://github.com/PiBOH/vivi-music-de/issues/103))

## [6.0.6.8_DE-1.53.18-alpha] - 2026-09-24

### Fixed
- [DE] **A like made on the phone reaches the desktop, and one made here reaches the phone.** The device sync has carried a list of liked song ids since the beginning, but nothing ever read it: an incoming library snapshot had only its playlists applied, and the snapshot the desktop sent back held no liked songs at all, so the like state was the one part of the pairing that never travelled either way. The liked songs now travel as **entries** (`LibrarySnapshot.likedSongs`): a like carries the song's metadata — which is what lets the phone store and render a song it has never seen — an unlike a tombstone with the time it happened, and every entry carries the last-write-wins time of the pair's like state, so an unlike is not resurrected by the other device's older copy (a plain id with no time can only *add* a like, never remove one, which is what keeps an older peer safe). On the desktop the like map became that ledger (`SongActions`): it is pushed when the **user** likes or unlikes something and applied when the phone does — a remote like never fires *Auto download on like*, never marks itself as a local edit and never travels back. On the phone the entries are built from the liked songs of its database (each with the time of its `likedDate`) plus the unlikes made since the last read, and an arriving like is written into the database with the metadata the desktop sent when the song is unknown; an arriving unlike clears a like only when the tombstone is newer than it. **Constraint:** the YouTube account stays the only authority — nothing in this path calls `likeVideo`, so a snapshot can never like or unlike anything on YouTube Music; what is applied is the *other device's* state. (Closes [#96](https://github.com/PiBOH/vivi-music-de/issues/96))
- [APK] **The liked songs are shared with the desktop** (see the entry above): the phone sends its liked songs with the metadata and the time of each one, applies the desktop's likes *and* unlikes to its own library, and keeps the unlikes of this session so that one made while the desktop was offline still arrives. Its flat `songIds` list stays on the wire for an older desktop.
- [DE] **The copies a pairing made of the same playlist are collapsed again, and no second copy is created on YouTube Music (E1034).** 1.53.17 taught the sync to recognise a playlist by its **account** id, which fixed the copies that carry one — but the copies that carry none, or two different ones, were left in place, so the playlist list could still show the same name two or three times. A playlist is now also recognised by **what it holds**: the same name and one song list contained in the other (`samePlaylist`) means the same playlist, and the copies are merged — the entry that knows its account id survives, the songs of all of them are unioned in its order (nothing is dropped, nothing is reordered), and the others are tombstoned **locally**; the cleanup runs at startup, after every merge *and* at the moment a playlist learns its account id (`link`), which is exactly when the copy becomes recognisable. Two further ways the list grew are closed too: *Create on YouTube Music* no longer creates a second account copy when the account already holds that playlist — the local row adopts the account id of the copy that is already there and only the songs the account does not have are pushed — and the account's **special** playlists (liked songs, saved for later) no longer travel to the phone as ordinary playlists, since the phone has its own Liked list and its own saved-for-later row and the import showed up as a second entry for each. **Constraint:** every one of these merges is local and reversible by re-creating the playlist; nothing is ever deleted on YouTube Music, so a duplicate that genuinely exists *there* has to be removed by the user (the app refuses to guess which of two account playlists is the one to keep). (Closes [#93](https://github.com/PiBOH/vivi-music-de/issues/93))
- [APK] **A playlist the desktop already has is no longer imported as a second one**: when neither the row id nor the account id matches, the arriving playlist is matched by name and songs against the phone's own rows and merged into the one that fits, adopting its account id — and a row the phone had imported for the desktop's mirror of the account's liked-songs / saved-for-later lists is dropped, so those two stop appearing as playlists on the phone.

## [6.0.6.7_DE-1.53.17-alpha] - 2026-09-23

### Fixed
- [DE] **The seek bar can actually be dragged.** `ViviSlider` attached two `pointerInput` handlers to the same node — one `detectTapGestures`, one `detectDragGestures` — and they fought over the pointer: the tap detector takes the down event, the drag detector needs unconsumed changes to pass touch slop, so a drag was never registered as a drag and the thumb only ever moved by tapping. One gesture handler now covers press, drag and tap, and the press itself already moves the thumb (as on YouTube), so the seek follows the finger. (Closes [#91](https://github.com/PiBOH/vivi-music-de/issues/91))
- [DE] **A seek no longer snaps back to where it was.** Both seek bars dropped the scrubbed position the instant the finger was lifted, so the thumb jumped back to the live position until the engine caught up (a seek on a stream is a round trip) — which read as "the seek did nothing". The scrubbed position now stays on screen until the player reports it (within 1.5 s of the target), with a 4 s safeguard so it can never stick. (Closes [#91](https://github.com/PiBOH/vivi-music-de/issues/91))
- [DE] **The expressive player's countdown no longer reads `-0:00` for a whole track.** The remaining time was always drawn as `-` + remaining, so at or past the end it sat on `-0:00` for as long as the track stayed there. It is drawn only while something is really left, and the total duration shows otherwise. (Closes [#92](https://github.com/PiBOH/vivi-music-de/issues/92))
- [DE] **Pairing no longer multiplies the playlists, and no cleanup can delete one from YouTube Music (E1034).** The identity of a playlist across two devices is its **account** id, but both apps generate their own local row id (`LP` + 8 characters), so the copy arriving from the paired device could not be recognised as the playlist already there and each side imported the other's copy. The account id now travels with the playlist (`remoteId`), a mirrored copy is merged into the single local playlist (songs of both sides kept, order = the newer copy plus the extras), and a startup pass collapses the copies a pairing already created — that cleanup is **local only** (only the delete you perform reaches the account). Local playlists that YouTube Music does not have are still pushed, as before. (Closes [#93](https://github.com/PiBOH/vivi-music-de/issues/93))
- [DE] **The expressive player reopens on the tab you left it on**, including the closed state, and its autoplay button is now a real toggle: it drove a local flag that was reset on the next recomposition and never reached the player, so switching autoplay off there left it on. It now writes the window-level *Autoplay next track* setting (the same one Settings → Player shows). (Closes [#94](https://github.com/PiBOH/vivi-music-de/issues/94))
- [DE] **The sidebar is opened and collapsed from the "VIVI Music" title.** Clicking it (or the logo alone when the rail is collapsed) toggles the rail; the dedicated buttons are gone (the one in the top bar next to Back, the menu icon in the rail and the MenuOpen one next to the title), the "VIVI Music" label no longer collapses the home / new releases / radio group (that group is always open), and the no-op **Forward** button next to Back is gone too. (Closes [#95](https://github.com/PiBOH/vivi-music-de/issues/95))
- [APK] **The account playlist id is sent with the playlists**, which is what lets the desktop recognise them instead of importing a second copy of each one (see E1034 above).

## [6.0.6.6_DE-1.53.16-alpha] - 2026-09-23

### Fixed
- [DE] **The lyrics show the "no text" indicator again.** A hole in the lyrics (an instrumental break) draws nothing on the desktop while the mobile renderer puts an animated ring there that fills with the gap's own progress. The list is no longer one row per line: a line whose words end more than 4 s before the next one (or an LRC line that carries a timestamp and no text at all — how those files mark an instrumental) gets an indicator row, exactly where mobile computes it, and an empty line is not drawn as a blank row any more. **Constraint:** the indicator is information, not decoration — it is deliberately not gated behind the "Animations" switch. (Closes [#83](https://github.com/PiBOH/vivi-music-de/issues/83))
- [DE] **A line aligned left or right can no longer run off the window.** The per-line wrapper is full width while the text is not, and the active line's 1.05 scale was applied around the wrapper's **centre**: a left-aligned line grew past the left edge and a right-aligned one past the right (with "Animations" on, so only the animated styles showed it). The scale's origin now sits on the alignment edge, so the line grows inwards. `MetroLyrics` had the same defect for a different reason — its text layout was measured without the alignment, so the glyphs always sat at the left edge and the line push (which moves a line by its own growth) dragged a right-aligned line further left; the alignment is part of the measured layout now, which is what makes the push compensate the growth exactly. (Closes [#84](https://github.com/PiBOH/vivi-music-de/issues/84))
- [DE] **Artwork is loaded at its real quality when Data saver is off.** Every image was fed to the loader exactly as the provider returned it, which for YouTube Music is a `w120-h120` crop: covers were soft even with the saver off, while the mobile app asks for a high-resolution variant and only caps it when the saver is on. A single `adjustedThumbnailUrl` now applies the mobile rule (Google CDN `wN-hN-p-l90-rj` at ≥544 px, YouTube's `maxresdefault`/`hqdefault` tiers, 150 px cap when the saver is on) and every artwork path goes through it — rows, carousels, the player's big cover (the requested size follows the drawn size), the blurred backdrops, the canvas and the mini player. **Constraint:** Data saver is part of the URL, so an image is re-resolved when the setting changes. (Closes [#85](https://github.com/PiBOH/vivi-music-de/issues/85))
- [DE] **A forced playlist sync says where it is.** The upload/pull of a whole library is minutes of network work and the screen offered a spinner and "in progress" for all of it. The run now reports live progress (`PlaylistSync.Status`: playlists done/total, songs moved, the playlist being worked on, and which half is running) and both entry points (Account and the playlist list) render it: `Uploading · 3/12 · 128/540 — 'Feste'`. Every song that lands on the account is reported as it lands, and the pull advances one playlist at a time even though four are in flight. **Note:** the line is built from the numbers plus the already-translated labels, so no new string key was needed. (Closes [#86](https://github.com/PiBOH/vivi-music-de/issues/86))
- [DE] **The queue travels between paired devices as a whole.** Device sync only replaced the desktop's queue when the CURRENT track differed, so two devices sitting on the same song kept their own lists (the desktop's often a single track) and, of a fully synced queue, only the song that happened to match ever lined up. A matching track now also adopts the peer's queue in place — the list around it is replaced, the track is not restarted — and both directions log what they carry (`sync.log`: `send:`/`recv:` with the track, the queue size, the index and the play/resolving flags), which is the part that was invisible before. (Closes [#87](https://github.com/PiBOH/vivi-music-de/issues/87))
- [DE] **The player picker no longer offers two names for one player.** The old `V2` entry rendered the CLASSIC layout with a larger artwork, so "Classic" and "V2" were the same player; the entry is gone from the picker, the enum and the string table (a saved setting resolves to Classic). (Closes [#88](https://github.com/PiBOH/vivi-music-de/issues/88))
- [DE] **A font can be picked straight out of `C:\Windows\Fonts`.** The native AWT file dialog refuses to open the system font folder for a non-elevated process and answers "access is denied", which forced users to copy the font elsewhere and only then select it — the import now uses the Swing chooser, which lists directories through the plain file APIs (with hidden files shown, so the system fonts are visible) and only reads the file. A copy that fails is reported in the log and as a notification instead of being swallowed, which is why the old behaviour looked like nothing had happened at all. (Closes [#89](https://github.com/PiBOH/vivi-music-de/issues/89))
- [APK] **The desktop pairing code is recognized at the first frame.** The desktop's QR carries a full `vivimusic://pair` URL, so at 180 dp each module was only a few pixels wide and the camera needed several attempts; the code is drawn at 220 dp now. On the phone the scanner is no longer locked to portrait (which kept the preview at its rotated, smaller buffer) and, more importantly, a scan now **pairs immediately**: the QR already carries the relay address and the code, so the extra confirmation tap made a good scan look like the code had not been read. The address is persisted before the client is created for it, so the join cannot race the URL change. **Note:** the manual code field keeps working; a bare `ws://` QR still only sets the relay address. (Closes [#90](https://github.com/PiBOH/vivi-music-de/issues/90))

## [6.0.6.5_DE-1.53.15-alpha] - 2026-09-22

### Fixed
- [DE] **A full translation audit, and the class of bug behind "Alpha written in Arabic letters".** The leak had two causes and both are closed. (1) `Localization.get` resolves a key as *language → English → the first map that has it*, so a key with translations but **no English value** prints another language on an English build; the generator now refuses to emit such a key (`SystemExit`, listing them), which is why the Alpha style showed Arabic letters until 1.53.15. (2) Seven keys were mapped to an inline English literal instead of the Android resource of the same name (`close`, `integrations`, `player_background_blur`, `lyrics_line_spacing`, `lastfm_now_playing`, `mini_player`, `randomize_home_order`), so they showed **English in every language** while the mobile app had them translated — they now pull the real resource, which translates between 24 and 41 of the 52 languages. (Batches 13/14 had filled those five with a *copy of the English text* for every language — `_all("Blur")` — which overrode the mapping; those placeholder entries are gone and `desktop_extra_translations_80.py` translates the languages the resource leaves out.) The remaining mobile gaps are filled by two new batches (`desktop_extra_translations_78/79.py`, ~2 000 strings): every lyrics style name, every romanization/translation switch, the language list, the Apple Music blur, the text position and the line spacing — all previously English. `lyrics_style_none` was rewritten in every language too: the resource behind it says *None*, but the style is mobile's *Simple* one, so the picker showed a style called "None" in 26 languages and "Simple" in the other 26. **Verification:** `scripts/audit_desktop_localization.py` (new, committed) reports **0 keys missing or leaking and 0 values written in a script the language does not use**, over 771 keys × 52 languages; it also lists the 1 031 strings the mobile resources themselves left in English (device readouts, font names, Discord/Last.fm sentences) so the next pass has a work list. **Constraint:** a key with translations and no English source is a bug, not a gap — the generator fails rather than shipping it; and a key whose Android resource exists must map to the resource, not to an English literal.
- [DE] **The string table no longer breaks the build: `ClassTooLargeException`.** Completing the translations pushed `LocalizationKt` past the JVM's 64 KB class-file cap (`Class too large: com/music/vivi/desktop/LocalizationKt`) because every literal of all 53 maps lived in one file facade. The tables are emitted in numbered files (`LocalizationTables1..9.kt`, six languages each) referenced from `Localization.kt`; the generator deletes the slices of a previous run so a regenerated table cannot leave a stale one behind.
- [DE] **Deleting a synced playlist now deletes the account's copy too, like the mobile app, and *Create on YouTube Music* is in the playlists screen as well.** 1.53.14 left the deletion local on the grounds that removing a playlist from the account is not reversible; mobile propagates it, so the desktop does now — and only from the delete the user performs here: a tombstone that arrives over the device sync still removes the playlist locally and never reaches into the account. An unsigned session or a playlist with no account copy is a no-op, and the outcome is in `playlists.log` (`deleted` / `NOT deleted (it is still on YouTube Music)`). The upload action the Account screen gained is now in the playlist list header too, where a user looks for it, with the same confirmation and the pending count on the button.

## [6.0.6.5_DE-1.53.14-alpha] - 2026-09-22

### Added
- [DE] **Creating a playlist offers to sync it with YouTube Music, like the mobile app.** The name dialog carries the mobile create dialog's *Sync playlist* switch (its title and description are the app's own, translated, strings; without a session it shows *Not logged in to YouTube* under the switch and the playlist stays local). With it on, the playlist is created here first — the screen never waits on the network — and its copy is created on the account in the background, with the account's id recorded on the local playlist. Two consequences the mobile app has too: a song added to such a playlist is uploaded as it is added, and renaming it renames the account's copy. **Note:** deleting is deliberately not propagated — that would remove a playlist from the user's YouTube account, and it is not reversible.
- [DE] **Account → *Create on YouTube Music*: the playlists that already exist locally can be created on the account in one confirmed action.** Mobile only offers the account copy at creation time (the dialog's switch), so the playlists made before it — or made with the switch off — had no way over. The Account screen now lists that action, disabled with *Every playlist is already on YouTube Music* when there is nothing to create, and it asks first (*This creates N playlist(s) on your YouTube Music account and uploads their songs. Continue?*): each local playlist without an account copy is created there sequentially (one request each, so a large library does not get rate-limited) and its songs uploaded, and the account's playlists are pulled down in the same run so both sides mirror each other. It runs even with *Auto sync with account* switched off — it is an explicit request — but never without a session. Every step is in `playlists.log`. **Constraint:** a playlist that already exists on both sides is never mirrored a second time (the pull skips it and the sidebar hides its online copy), otherwise every upload would come back as a duplicate local playlist. `SyncedPlaylist` gained the `remoteId` field for this; a playlist mirrored from the account still carries it in its id (`yt-…`), and `PlaylistSync.accountPlaylistId()` answers for both forms.

## [6.0.6.5_DE-1.53.13-alpha] - 2026-09-22

### Fixed
- [DE] **The History screen is the listening history, not the notifications list.** The two screens shared one empty-state string (`history_empty`), and the value that key carried in **every** language was the *notifications* wording — Italian had "Ancora nessuna notifica", German "Noch keine Benachrichtigungen" — so an empty History screen announced itself as the notification list: opening `Cronologia` from the sidebar genuinely looked like it had opened the notifications, exactly as reported. The notification list has its own key (`notification_history_empty`) in all 52 languages now (the batch that supplied the wording was renamed, with `fa`, `iw` and `pt-rBR` filled in), and `history_empty` keeps the listening-history wording (`Nessuna cronologia ancora`, `Noch keine Historie`, …). The screen also records what it is actually made of (`history.log`: local tracks, remembered searches, account sections), so a support zip can tell "the history is empty" apart from "the history was not read". **Verification:** `scripts/check_localization.py` reports neither key as falling back to English. **Constraint:** the two screens never share a string again — one key, one screen.

## [6.0.6.5_DE-1.53.12-alpha] - 2026-09-22

### Fixed
- [DE] **The Artists screen has content — the page was understood, and the list is no longer a server page that can be empty.** Three things were wrong. (1) The saved-artists corpus (`FEmusic_library_corpus_artists`) answers with its `gridRenderer` **straight in the tab content**, a field the tab model did not have: the response deserialized to "no content", `YouTube.library` fell into its shelf branch, found no shelf and returned an **empty page with no error at all** — the screen was blank while `browse.log` only said `1 section(s), 0 item(s)`. The tab content models the grid now, and the library parser looks for a grid/shelf in *every* shape a library page can arrive in (the tab content, a top-level section list, the two-column secondary contents, every tab) and takes the first one that actually holds items, instead of the first renderer of the first tab. (2) Even a correctly parsed grid dropped every card: the corpus artist cards are marked `MUSIC_PAGE_TYPE_LIBRARY_ARTIST` and `MusicTwoRowItemRenderer.isArtist` accepted only `MUSIC_PAGE_TYPE_ARTIST` (the responsive-list parser next to it already accepted both). (3) The corpus is genuinely empty for an account that has saved songs but has never followed an artist, and mobile still lists artists there because its Artists screen reads the **artists table of its database**, not a server page. The desktop has no such table, so the list is derived from the account's saved songs (a few pages, one entry per channel id, the artist's song artwork as its picture) and cached in `~/.vivimusic/artists.json` by `ArtistsStore` — the desktop counterpart of that table, which also gives the screen something to draw offline. The artist page itself now also parses the several shapes its sections and title can come in (single-column tabs, two-column tabs, a top-level section list), which is the shape that used to fail the request outright. Every library response logs what it carried (`shape=grid:12`, `shelf:8`, `none (…)`) and a page that loads with nothing in it gets a message and a **retry** instead of a blank grid that looks like a broken screen.

## [6.0.6.5_DE-1.53.11-alpha] - 2026-09-22

### Fixed
- [DE] **The lyrics styles are ten genuinely different styles again, and they animate on every file.** The six word-by-word styles (`Simple`, `Fade`, `Glow`, `Slide`, `Karaoke`, `Apple`) only animated when the provider returned **per-word** timings, and the community servers ship plain LRC (line timings only) most of the time: on those songs all six fell back to the same plain line, so they *were* the same style — exactly as reported. Each style now estimates the word timings from the line's own duration (the estimator the mobile `Apple Music V2`/`VIVI Music` styles already use) and keeps its own recipe: the brightness step, the fade with its bloom, the halo growing with the square of the fill, the tight sliding front, the wide karaoke front, the weight-led Apple fill, the per-character reveal, the single wave of `VIVI Music`, the floating words of `Lyrics V2` and the `MetroLyrics` canvas. `Alpha` stays the no-effect style, and the sentence-level fallback of each style (used when romanization is promoted to the main line) is its own too, so the picker never shows a style other than the one chosen.
- [DE] **`MetroLyrics` finally runs on graphemes, not words.** It is the only mobile style that animates *graphemes*, and the desktop was drawing it word by word (the port had noted the shortcut). It now draws on a measured text canvas with mobile's three effects: the **hyphen crescendo** (`go-o-o-o` is sung as one group, each segment scaled by position, the last one springing in and the whole group springing back out over 600 ms with a decaying cosine), the **per-character nudge** (`0.038·sin(π·lp)·e^(−3·lp)`, its own local progress, so the word expands as a wave instead of uniformly) and the **line push** (every character's scale widens it, the widths are summed per line and the line is shifted by half that sum, which is what keeps a centred line centred while it sings). Two deliberate departures: the halo is emulated with offset glyph copies (Android's `BlurMaskFilter` has no portable equivalent inside a `DrawScope`) and mobile's two divergent sets of spring constants are unified, which is what makes the centring correct rather than approximately correct. **Constraint:** the glyph runs on the JVM's own `BreakIterator`, so a Devanagari or emoji line is never torn apart mid-cluster.
- [DE] **The lyrics move at the display's rate instead of in ~25 ms steps.** The player polls the position about 40 times a second and the renderer drew it raw, which is what made the animation look choppy. A panel-wide clock now extrapolates between samples (the way the mobile Metro renderer does) and dissolves the difference from the next sample over a few frames instead of snapping back; a seek snaps, a play/pause toggle re-syncs, and a static line does not read the clock at all — only the singing line and its neighbour follow it, and the Metro canvas reads it inside its draw scope, so a frame invalidates a drawing rather than a composition and a text layout. The line cross-fades (alpha, scale, the progressive blur) are animated with the mobile timings instead of switching instantly.
- [DE] **Lyrics animation no longer depends on the "Animations" switch, and no longer desyncs from the audio.** The karaoke fill is playback feedback, not decoration: it now runs with the master switch off, and "Animation speed" reaches the lyrics on its own (`lyricTween` for the durations, `LYRIC_MOTION` for the Metro springs). The old timeline stretch (`LYRICS_SPEED`) is gone: it ran the timeline up to 1.6x fast, so on `fast` the words lit up **before** the voice. The position is honest now and only the animation durations scale.
- [DE] **`{agent:v1|v2|v1000}` lines follow their own voice.** The alignment markers the multi-singer sources ship with were parsed and then ignored, so a duet line stayed on the user's own text position instead of moving to its singer's side of the screen.

## [6.0.6.5_DE-1.53.10-alpha] - 2026-09-22

### Fixed
- [DE] **The sidebar groups stack again.** The three collapsible groups (Home/New/Charts, the Library entries and the playlist list) were put inside `AnimatedVisibility` in 1.53.5 — and `AnimatedVisibility` lays multiple children out **on top of each other**, so every entry of a group was drawn over the same spot: the sidebar looked like one squeezed row. Each group is now a single `Column` inside its `AnimatedVisibility`, which is what the transition expects. **Constraint:** `AnimatedVisibility` is not a `Column`; a group of rows must be wrapped before it.
- [DE] **The embedded sign-in no longer hands over a session without `LOGIN_INFO`.** The reported logs show the exact mechanism: the window captured 25 cookies (`SID`, `SAPISID`, `__Secure-1PSID`, …) and `missing critical: [LOGIN_INFO]`, delivered them, and the account validation answered `401 UNAUTHENTICATED` (`account/account_menu`) — while the same cookies pasted by hand authenticate, because the manual header carries `LOGIN_INFO`. YouTube issues that cookie for **its own** domain, and the window is opened on `accounts.google.com` with `music.youtube.com` as the post-login target, so the store can come back complete for Google and incomplete for YouTube. The window now visits `www.youtube.com` once after the session appears (that is where the cookie is issued), waits for the store, goes back to `music.youtube.com` for the `DATASYNC_ID`/`VISITOR_DATA` ids and only then hands over; `hasFullSession` requires the critical set, and if `LOGIN_INFO` still does not appear the session is handed over anyway (logged as `delivering PARTIAL session, missing critical: […]`) so nobody can get stuck. A 401 on a header with no `LOGIN_INFO` is now tagged `E1033` instead of being reported as a stale session (see ERRORS.md).
- [DE] **The player's transport buttons answer to a press.** The glass circles now press in while held (an expressive spring, the same 0.88 scale on release), so a tap is visible feedback instead of only the action.

## [6.0.6.5_DE-1.53.9-alpha] - 2026-09-22

### Fixed
- [DE] **The strings added with the player and sync work are translated in every language.** `lyrics_options`, `Show play/pause on thumbnail` and its description, the swipe description and the two playlist-sync status lines have no mobile counterpart, so they are hand-written for all 50 translations (`desktop_extra_translations_76.py`); the wording that comes from the mobile app (`Enable swipe to change song`, the swipe sensitivity and its description, `Auto sync with account`, `Automatically sync with your Music account`, `Sync playlist`) is mapped to the Android resources and the 7-21 languages that app leaves untranslated are filled in there too. **Verification:** the batch is checked against the languages list (a missing value aborts the generator) and `scripts/check_localization.py` reports none of the twelve keys as falling back to English.

## [6.0.6.5_DE-1.53.8-alpha] - 2026-09-22

### Changed
- [DE] **The app stops redrawing while nothing is playing.** The mini player sits on screen almost all the time, and its background layer created its glow/live-mesh loops **unconditionally** — every style, playing or not — so the window kept redrawing 60 times a second from launch to exit: the equalizer bars wobbled while paused, the player background pulsing/mesh/visualizer loops ran with the track stopped, and the canvas backdrop kept its Ken Burns zoom running on a paused player. Each of those loops now only exists while it has something to show (the moving styles, while playing), and the paused frame is a fixed, sensible value instead of a second animation. The player's canvas backdrop also stopped re-blurring a window-sized bitmap on every frame: the blur is rasterized on a fixed 384 dp layer and only *scaled* to cover (the same rule the blurred artwork backdrop follows since 1.52.5 — size-dependent effects must not sit on a `fillMaxSize` node). **Evidence from the reported logs:** the audio path is not the load — `audio device check: played 10020ms of 10020ms wall (100%)` with `0 device stalls` all session and `gc.log` peaking at 33 ms, i.e. the cost was the UI never going idle.

## [6.0.6.5_DE-1.53.7-alpha] - 2026-09-22

### Added
- [DE] **`Auto sync with account`, the mobile app's playlist sync.** With a signed-in account the app's playlists are mirrored into the local playlist store (`PlaylistSync`, ids `yt-<playlistId>`), so they can be played, queued, reordered and edited like a local playlist instead of only being listed by the sidebar. It runs on sign-in and whenever the option is switched on, and the Account settings offer a `Sync playlist` button with the result of the last run; every run is recorded in `playlists.log`. **Constraint:** a mirrored playlist is written only when its name or song list actually changed — `updatedAt` is the last-write-wins key of the device sync, so bumping it on every sync would make an untouched playlist win against a real edit made on the paired phone. With the sync on, the sidebar no longer lists the same playlist twice (once mirrored, once online).

## [6.0.6.5_DE-1.53.6-alpha] - 2026-09-22

### Fixed
- [DE] **The player has the mobile lyrics controls.** A **Lyrics options** button next to the lyrics buttons opens the mobile app's lyrics menu — style, position, word glow, Apple Music blur (only under the VIVI Music style), blur, tap-a-line-to-seek, auto-scroll, the new thumbnail play/pause, text size and line spacing — and every change is applied to the running player immediately (and kept in the settings file). The expressive player reaches the same menu from the lyrics panel it shows. The swipe-to-change-song gesture the mini players gained is now on the full player's artwork too, and the new **Show play/pause on thumbnail** option makes a click on the artwork start or stop the song.
- [DE] **Four settings rows were showing their raw key** (`enable_swipe_thumbnail`, `enable_swipe_thumbnail_desc`, `swipe_sensitivity`, `swipe_sensitivity_desc`): the swipe options added with the mini-player gesture were never added to the localization table, so the Appearance screen printed the key itself in every language. They are mapped now (the mobile app ships the wording for the switch and the slider, the description is the desktop one).

## [6.0.6.5_DE-1.53.5-alpha] - 2026-09-21

### Fixed
- [DE] **Every expansion now animates, and all of them follow the settings.** The sidebar's three groups, the mini player bar and the right Now Playing panel used to appear and disappear with no transition at all (a plain `if`), the sidebar's own expand/shrink used a bare default spec, the right panel had no animation, and the group chevrons and the 72 dp rail kept moving their fixed spring even with the master switch off. They all go through the shared `Animations.panelEnter/panelExit` (sidebar, right panel), `barEnter/barExit` (bottom bar) and `sectionEnter/sectionExit` (sidebar groups) — an expressive spring with a light overshoot and no bounce on the way out — so `Animation speed` scales them and turning **Animations** off makes them instant. Opening or closing the full player is now a vertical expansion of the bar rather than one more horizontal screen slide (the screen-to-screen slide/fade stays for every other navigation). **Constraint:** a new panel must take its enter/exit from `Animations`, never a bare `expandHorizontally()` — that is exactly how the right panel ended up with no transition and with an animation the master switch could not turn off.

## [6.0.6.5_DE-1.53.4-alpha] - 2026-09-21

### Fixed
- [DE] **The history now remembers.** Only pressing Enter saved a search term, while the results appear as you type — so acting on a result (playing it, queueing it, adding it to a playlist, opening an album or artist) is what records the search now. Below that, the app keeps a real local history in `~/.vivimusic/history.json`: the 200 most recently started tracks (one row per track, with a play count) and the 50 most recent search terms. The **History** screen shows both, above the signed-in account's server history, each with its own `Clear`, and tapping a remembered term reopens it in Search — which is also what makes the screen useful without a signed-in account. `Pause listen history` now stops the writes (it used to only hide the screen), the file is written with a 1.5 s debounce so skipping tracks does not rewrite it per skip, and the last write is flushed on exit. The Home recommendations keep their seeds across restarts for the same reason. **Constraint:** the history is per machine and local — nothing here is pushed to the account.
- [DE] **Six settings keys were showing as raw keys in the UI** (`hide_explicit`, `hide_video_songs`, `hide_youtube_shorts`, `lyrics_provider_priority`, `show_artist_description`, `show_artist_subscriber_count`): the Content screen port translated them in every language, but the English table had no wording for them, and the extra translation batches cannot define English. They now map to the Android resources, like the rest.
- [DE] **The lyrics options are no longer English-only in every language.** The 42 keys of the lyrics section had no Android mapping, so the whole section fell back to English everywhere. 33 of them now map to the mobile app's own wording (`lyrics_animation_style`, `lyrics_auto_scroll`, `lyrics_text_position`, the style names `Simple`/`Fade`/`Glow`/`Slide`/`Karaoke`/`Apple Music`…, the romanization scripts, Apple Music blur), which took the untranslated leftovers from 2184 to 860 strings, and the nine desktop-only sentences with no mobile counterpart are translated in all 49 languages (`desktop_extra_translations_75.py`). **Note:** the remaining 860 are concentrated in the ~15 languages the mobile app itself does not translate those keys in (as, be, bn, et, eu, fa, fi, fil, hi, hr, iw, km, ml, ms, sr, sv, ta, te…); they are listed by `scripts/check_localization.py`.

## [6.0.6.5_DE-1.53.3-alpha] - 2026-09-21

### Added
- [DE] **The Content screen is the mobile one.** The rows the mobile app keeps there are ported and each one actually does something: `Hide explicit` (removes the items flagged explicit from home shelves, browse, search, album, playlist, artist and library results), `Hide video songs`, `Hide YouTube Shorts`, `Show artist description` (the artist page shows the description now, in four lines) and `Show artist subscriber count`. Beneath them, the **lyrics provider list** is editable — every provider the resolver knows is a row with a switch and up/down buttons, and the saved order is the order they are asked in (`DesktopLyrics.fetch` skips the providers that are off). The wording reuses Android's own translations wherever they exist and is translated in all 49 languages otherwise (`desktop_extra_translations_72.py`, `_73.py`). **Constraint:** the filters are applied where the items enter the UI (`ContentFilters`), never per row, and an empty provider list means the built-in order with everything on — which is why the last enabled provider cannot be switched off.

### Fixed
- [DE] **The artist page gets its content rows and the description.** `ArtistPage.description` was parsed but never drawn, so a language of artist information that the mobile app shows simply did not exist here.

## [6.0.6.5_DE-1.53.2-alpha] - 2026-09-21

### Fixed
- [DE] **The Artists screen is no longer empty.** It opens a *library* page (`FEmusic_library_corpus_artists`), which carries the `gridRenderer` / `musicShelfRenderer` shape that `YouTube.library` maps; the screen was going through `YouTube.browse`, which only reads the two-row cards of a browse page, so the request succeeded and returned nothing — no error, no log line, blank screen (the same for the "See all" link in the Library feed). It now uses the library parser, the one the Library screen already uses, and every browse outcome is recorded in `browse.log` (`browse ok … → N section(s), M item(s)` / `(empty page)`), so an exported log tells an empty server page apart from a parse miss.

## [6.0.6.5_DE-1.53.1-alpha] - 2026-09-21

### Added
- [DE] **A new lyrics style, `Alpha`.** It draws the lyrics the way the desktop did before the mobile look was ported (one plain line per lyric, the sung one in the accent colour), so the old behaviour is still one click away in the picker. Its label is translated in all 49 languages (`desktop_extra_translations_71.py`).

### Fixed
- [DE] **Every animation style now follows the mobile recipe of the same name.** `Simple`, `Fade`, `Glow`, `Slide`, `Karaoke` and `Apple Music` reuse the exact alphas, weights and halo strengths of the mobile renderer (mobile's `Glow` is a word-level halo that grows with the square of the fill — 1.53.0 had turned it into a sweep across the line, which is not what the mobile style does), `Apple Music V2` lays the line out word by word and reveals it character by character (estimating the word timings from the character counts when the source carries none), `VIVI Music` runs one global wave over the sentence with the per-word halo, the 0.75/0.50/0.30/0.20 alpha falloff, the progressive blur and the 1.05 active-line scale of the mobile style, `Lyrics V2 (Fluid)` floats each word by up to 4 dp while its bright copy is revealed behind a travelling edge, and `MetroLyrics` draws the per-word flat fill with a landing wobble and the mobile distance falloff. The whole renderer is driven by one speed-scaled timeline, so the `Animation speed` option reaches every style uniformly.
- [DE] **The expressive player's lyrics ignored the lyrics options.** They were handed only the display object while the size and line spacing parameters defaulted to 18 sp / 1.35 and then *overrode* it, so the user's size and spacing never reached that player; it also always requested synced lyrics even with the option off. It now keeps the values the display object carries and honours the `Synced lyrics` setting.

## [6.0.6.5_DE-1.53.0-alpha] - 2026-09-21

### Added
- [DE] **Two more lyrics styles, so the picker matches the mobile one.** `Lyrics V2 (Fluid)` lays the sung word out as its own composable, floats it up (a sine over its own progress) and reveals its bright copy behind an edge that travels across the word instead of just recolouring it; `MetroLyrics` also animates lines whose source carries no word timings — it estimates them the way the mobile style does (180 ms per word, 30 ms apart) — and fades the rest of the list by distance from the sung line (20 / 15 / 10 / 8 %).

### Changed
- [DE] **Every lyrics style is now a style of its own.** `Glow` is a *line* effect (a light sweeps the whole line over its own duration while a halo breathes around it) instead of a slightly larger shadow on the per-word fill; `Apple Music V2` reveals the line *character by character* (the mobile granularity) instead of being the same word-level effect as `Apple Music`; `Slide` (tight leading edge, breathing halo, glow on the words already sung), `Karaoke` (wider seven-stop fill whose halo grows with the square of the progress) and `Metro` (flat, bold karaoke fill) each follow their own mobile recipe. `Animation speed` now scales all of them, including the sweep of `Glow` and the character split of `Apple Music V2`, which were the two that ignored it.
- [DE] **Settings are grouped and ordered like the mobile app's.** The root screen went from seven groups to the mobile app's four — account and updates, media & player experience, features & data, system & support — with the mobile row order inside each (`Updates` and `Account` first, `AI translation` → `Privacy` → `Storage` → `Data saver`, `Backup & restore` → `About`); the desktop-only rows keep their place next to the mobile row they belong to (the `Lyrics` screen sits beside `Appearance`, which is where the mobile app keeps those options, and `Language` beside `Content`). `Player & audio` follows the mobile grouping too: `Audio quality` first, then a `Crossfade` group with its duration, then history duration and the playback group, then the misc group with the volume/media-key rows, and only then the desktop-only slider style and stream cache. The lyrics sub-screen uses the mobile order (position, style, glow, blur, size, spacing, tap-to-seek, auto-scroll).

### Fixed
- [DE] **`Apple Music blur` is offered only where it does anything.** The mobile app shows that option just for the VIVI Music style, and the renderer only applies it there; the desktop offered it for every style (where it silently dimmed the lines) — the row now appears for VIVI Music only and the renderer ignores a leftover value on any other style. `Animation speed` is likewise hidden while the master `Animations` switch is off, since it is a derivative of it.

## [6.0.6.5_DE-1.52.9-alpha] - 2026-09-21

### Added
- [DE] **GC pauses are recorded per session (`gc.log`).** The collection counters printed next to a stall say how much collection time accumulated since the JVM started, not which pause a thread hit, so "the JVM stopped" and "the OS did not schedule us" could not be told apart in an exported log. `GcMonitor` now listens to the JVM's own GC notifications (no `-Xlog`, which a packaged `jpackage` image cannot point at the session folder), writes every collection to the session's new `gc.log` (always created and always exported, like the other categories), flags pauses over 100 ms in `playback.log` (`gc pause: 214ms (G1 ...) — every thread was stopped here`), and the stall watchdog now carries `last GC pause Nms Ns ago (worst …, total …)` in its line.

### Fixed
- [DE] **The Windows audio threads now join the OS's "Audio" scheduling class, and the app can no longer be power-throttled (issue #3).** The exported `playback.log` of 1.52.8 on Windows contains the mechanism in one place: `audio priority stall: 50ms sleep returned 1211ms late (= 1161ms held up at the writer's priority; heap 38/2048MB, gc G1 Young Generation:31/1633ms …, cpu 5% of 57% busy)` — and the device check right after it reports `audio device stall: the sound card played only 9570ms of audio in 10098ms of wall time (94%)`, with an earlier one at 59% next to `audio cushion low: only 11ms of audio left in the device buffer`. Every GC counter is frozen, the heap sits at 38 MB and the process uses 4-6 % of the CPU: the writer thread — which only has to hand PCM to the line ~8 times a second — was simply **not scheduled**, so the device ring drained and the gap became audible. Its whole cushion is the device ring, and Java Sound on Windows hard-caps that at 1000 ms (measured here: requests of 8 s, 4 s and 2 s are all granted 176400 bytes = 1000 ms; macOS grants 4 s), so a freeze longer than a second cannot be buffered away on Windows — which is why the same pipeline is inaudible on macOS. Browsers do not have this problem because their audio thread does not merely run at high priority: it registers with the Multimedia Class Scheduler Service. The new `AudioThreadBoost` does the same through JNA (`avrt.dll`): the writer, the watchdog and the decode thread call `AvSetMmThreadCharacteristicsW("Audio")` + `AvSetMmThreadPriority(CRITICAL)`, and once at startup the process asks for a 1 ms timer (`timeBeginPeriod`), `ABOVE_NORMAL_PRIORITY_CLASS` and **power throttling (EcoQoS) off** (`SetProcessInformation(ProcessPowerThrottling)`), which is what Windows 11 otherwise applies to a background app. Every call was verified live on Windows 11 (all return success) and each outcome is logged (`audio scheduling: …`), so an exported session states whether the protection is in effect.
- [DE] **The writer now says *why* it was late.** The device check can only report a number; the new per-pass attribution separates the two cases that look identical: a long pass whose time was spent *inside* `out.write()` is normal backpressure (the ring is full and the sound card paces us), while a long pass with PCM already queued and almost none of it inside the write is the thread not being scheduled — logged as `audio writer stalled: 1161ms for one pass with only 3ms of it inside the device write (queue 7987ms, cushion 941ms) — the thread was not scheduled, the output itself is fine`.

## [6.0.6.5_DE-1.52.8-alpha] - 2026-09-21

### Changed
- [DE] **The Arch/AUR files now ship as a single `VIVIMusic-<version>-AUR.tar.gz` asset.** They used to be attached one by one, which put a `.install` in the release listing (noise the Telegram bot had to filter out) — but `makepkg` requires the hook file to sit next to the `PKGBUILD` (`install=` points at it), so the three files cannot simply be split: they now travel together in the archive. The install guide and the site explain the two-step extract + `makepkg -si` flow.

### Fixed
- [DE] **A half-finished update download can no longer look finished (issue #82).** The installer was written straight to its final name, so leaving the Updates screen — or quitting the app — during a transfer left a truncated file that every later check accepted: the screen said "downloaded", and "open installer" then failed on a broken installer. The transfer is now written to `<name>.part` and renamed only after every byte arrived (with the release asset's size as the check when the API reported one), leftover `.part` files are cleaned up, and the download runs on `UpdateState`'s own scope instead of the screen's `rememberCoroutineScope` — leaving the screen no longer cancels it, and both the Updates screen and the notification banner keep showing the progress. Failures are logged in `cache.log` (`update download failed: …`), successes with the final size.
- [DE] **Two playback diagnostics were measuring pauses (issue #3).** On macOS a resume after a long pause produced `audio priority stall: 50ms sleep returned 393772ms late` and `audio device stall: the sound card played only 7445ms of audio in 581733ms of wall time (1%)` — both figures are the pause itself: the probe slept while the writer was parked and the device check compared played audio against a window that spanned the pause. The watchdog now skips while paused (and discards the first sample after a resume) and the device check starts a fresh window after every resume, so a reported stall is a stall. In the same logs, with all sessions of the reporter counted, there was **no** `audio stall`, `audio output starved` or "device ran dry" line — the audible dropouts are not reproducing on 1.52.6, and the device played 99-100% of the wall time with a ~3 s cushion.

## [6.0.6.5_DE-1.52.7-alpha] - 2026-09-21

### Added
- [DE] **Native Fedora/RPM package.** Every desktop release now ships a `VIVIMusic-<version>.rpm` alongside the `.deb` and the AppImage. It is not built by jpackage: RPM keeps its scriptlets in the package header, so the uninstall cleanup can be inlined in `%postun` (the `.deb` needs a helper file copied to `/usr/share`, because dpkg removes the payload before `postrm` runs) — `dnf remove vivi-music-de` therefore keeps only the newest `~/.vivimusic/backups/*.vivide.backup` plus `device-sync.json` and wipes all the caches, exactly like the Windows, `.deb` and AUR uninstallers. The spec is generated from the jpackage app image by `scripts/generate_rpm_spec.py`, which reads the cleanup body from the shared `scripts/uninstall-cleanup.sh` instead of duplicating it, and installs the same layout as the AUR package (`/opt/vivi-music-de`, `/usr/bin/vivi-music-de`, `vivi-music-de.desktop`, hicolor icon) with `Vendor: PiBOH` and `License: GPL-3.0-or-later`. Dependencies are left to `rpmbuild`'s automatic soname detection, so nothing hard-codes Fedora-only package names.

## [6.0.6.5_DE-1.52.6-alpha] - 2026-09-20

### Fixed
- [DE] **`Build Android APK` publishes the APKs again.** Both flavours built and were downloaded correctly, but the publish job assumed the artifact root (`apk/vivi-<flavor>.apk`) and stopped with `apk/vivi-gsm.apk is missing - not publishing a partial build` — `upload-artifact` picks the artifact's internal root itself (the common ancestor of the uploaded paths), so a single-file upload is not guaranteed to land there. The job now locates the two files wherever they landed and flattens them before publishing, and the presence check moved to that step, so a genuinely missing APK still aborts the run instead of publishing a partial build.

## [6.0.6.5_DE-1.52.5-alpha] - 2026-09-20

### Fixed
- [DE] **The lyrics animation speed, text position and line spacing are honored by every style.** The word-by-word layout (`APPLE_V2` / `VIVIMUSIC_1`, the default one) was built as separate word composables and read none of the three: it multiplied its progress by the raw smoothstep instead of the speed-scaled one, laid every word flush left regardless of the "Text position" option, and spaced wrapped rows with a fixed 0.25 em instead of the line-spacing value. It also ignored the text size, because the sliders wrote a separate state and the renderer drew from the `LyricsDisplayOptions` copy it had been given — so the number moved and nothing changed until another option was touched; both sliders now update the display object as well. `APPLE_V2` had no motion at all (fixed scale, no offset, no blur), i.e. it was indistinguishable from plain text: un-sung words now sit slightly smaller and lower and the sung word fades and lifts into place.
- [DE] **The lyrics style and text-position selectors are dropdowns** (`M3SettingsDropdownItem`, the same row style the player/audio settings use) instead of a bare outlined button opening a menu, so every single-choice option in the app looks and behaves the same.
- [DE] **Resizing the player no longer stutters.** The "Blur" and "Apple Music" backgrounds drew the artwork with `fillMaxSize().blur(48.dp)`, so the image was requested from the loader at the current window size and Gaussian-blurred at that size: both steps depend on the size, so every frame of a window resize re-decoded and re-blurred a full-window bitmap. The backdrop is now rasterized once at a fixed 256 dp layer and only *scaled* to cover the window, which makes a resize a transform instead of a re-render.
- [DE] **Playing music no longer pushes work on the position tick.** Two app-level effects were keyed on `playerState.positionMs`: the Discord presence / Last.fm loop (a presence update on every one of the ~20 position reports per second) and the listening-session accounting, so both were cancelled and restarted continuously; they are keyed on the track now and poll the controller once a second, and the macOS now-playing loop reads the controller instead of a value captured when it started.
- [DE] **The "Artists" screen is no longer empty.** In the parser, an artist card from the library grid was rejected (and, without its optional menu, crashed the page) because the shuffle/radio endpoints were required: those cards carry neither, so every item parsed to `null` and the sidebar list came up blank even though the page itself had loaded. Both endpoints are optional now and the menu lookup is null-safe, matching the mobile behaviour.
- [DE] **Two playback diagnostics no longer cry wolf.** `audio integrity` reported ~20 "sample table discontinuities" on every single track: the byte gap it compared across a fragment boundary (a constant 1824 bytes) is the next fragment's own `moof` box — a gap in *bytes*, not in *audio* — while the samples inside one fragment are contiguous by construction. Only a fragment that starts inside the previous one is reported now, which is what a mis-parsed container would actually look like. The `audio cushion low` warning also fired at every track change, where the outgoing line's ring is empty by design while the PCM queue already holds the next track; it is only evaluated while that line is still being fed.

## [6.0.6.5_DE-1.52.4-alpha] - 2026-09-20

### Fixed
- [APK] **The Android build compiles again (no APK could be produced since 6.0.6.3).** Two independent leftovers stopped it. The Azerbaijani resources were split over four files in `values-az/`, three of which came from a translation batch that wrote the language-suffixed names (`strings_az.xml`, `vivi_strings_az.xml`, `updater_strings_az.xml`); two of them declared the same 67 keys, and `aapt2` refuses to merge that (`Duplicate resources`), so `assembleUniversalGmsRelease` failed and the GMS/FOSS publishing job never ran. That batch is consolidated into the canonical `strings.xml` / `vivi_strings.xml` / `updater_strings.xml` now — same 1384 keys, none lost, and the old file's value kept wherever the batch had retranslated a key with English fragments left in it (`Already in pleylist:`) — which also means the desktop localization generator, which only reads those three names, finally sees the Azerbaijani strings. Behind it, `SimpMusicLyricsProvider.kt` was still in the sources after SimpMusic was dropped from the lyrics providers: it imported `EnableSimpMusicKey` (deleted with the provider) and was not referenced by the registry nor the UI, so the Kotlin compilation failed with `Unresolved reference 'EnableSimpMusicKey'` as soon as the resources were fixed; the orphan file is gone.

## [6.0.6.4_DE-1.52.4-alpha] - 2026-09-20

### Changed
- [DE] **Every issue reference now uses the official repository's numbering.** The tracker moved from the old fork (`PiBOH/vivi-music`, now a read-only mirror) to `PiBOH/vivi-music-de` and the 80 issues were transferred in their original order, so each of them has a new number there: every `#N` in this changelog, in `TODO.md`, in `AGENTS.md` and in the code comments was rewritten to that number, and every issue URL was pointed at `PiBOH/vivi-music-de`. The old fork URLs would still redirect, but a bare `#N` would have silently landed on a different issue.

## [6.0.6.4_DE-1.52.3-alpha] - 2026-09-20

### Fixed
- [DE] **The release-notes filter really filters.** The line that was supposed to hide the hourly `chore(website): refresh the static release manifest` commit used an escaped regular expression, which GNU grep did not match as written, so the commit still showed up in the notes; it is now a literal match (`grep -F`, no anchors, so a trailing CR cannot defeat it), the same way for the commit list and for the changelog section the notes are built from, and it is verified against a real commit range.

## [6.0.6.4_DE-1.52.2-alpha] - 2026-09-20

### Changed
- [DE] **The website now lives on its own branch.** The site used to be a folder of this branch (`.websitede/`) published by a workflow that read it from there; it is now the root of the **`gh-pages`** branch — pages, styles, images, fonts and the two data files the site reads — with a copy of the deploy workflow next to it, so a site edit is a normal push to that branch. The default-branch copy of that workflow keeps feeding `schedule` and `workflow_dispatch` (GitHub always runs those from the default branch), and `Release Manifest` refreshes `releases.json` / `changelog.json` in place on `gh-pages` instead of committing them here. The address is unchanged: `https://piboh.github.io/vivi-music-de/`.
- [DE] **Every change now goes to the official repository, on the branch that owns it** (`vivi-music-de` desktop, `vivi-music-de-apk` mobile, `gh-pages` website, `apk-latest` APK binaries). The old `PiBOH/vivi-music` fork is a read-only mirror from now on.

### Fixed
- [DE] **The release notes no longer show the website bookkeeping commit**: `Auto Release` filters `chore(website): refresh the static release manifest` out of the commit list and out of the extracted changelog section (and since the manifest moved, it is no longer a commit of this branch).
- [DE] **The dead build workflows are gone.** Removed: the four legacy workflows that compiled the deleted `composeApp` project (`build-linux`, `build-macos`, `build-windows`, `build-windows-custom`) with the files only they used (`installer/windows/ViviMusicDE.iss`, `installer/windows/README.md`, `logo.png`), and the upstream leftovers that could only fail here (`nightly` and `nightly-telegram`, which built from a `beta` branch that does not exist, with their `scripts/send_telegram.py`; plus `build`, `build_pr` and `ci`, which targeted a `main` branch that does not exist either).

### Notes
- [BOT] The Telegram bot (`PiBOH/vivimusicde_bot`) now follows the new layout: it posts releases from `PiBOH/vivi-music-de`, keeps its hourly check, and builds the optional custom-APK line from the fixed `.releases/apk/latest` URLs — the APKs are no longer release assets.

## [6.0.6.4_DE-1.52.1-alpha] - 2026-09-20

### Fixed
- [DE] **The update source and the remaining website links point at the official repository.** The desktop's own update source still resolved `PiBOH/vivi-music`, so the update check, the download it offered and the live changelog read the old repository; the About screen's website row, the README badge, the install guide and the site notes still carried the old Pages address. Every one of them now uses `PiBOH/vivi-music-de` (the mobile app already did).

## [6.0.6.4_DE-1.52.0-alpha] - 2026-09-20

### Added
- [DE] **`~/.vivimusic/settings.json` — every option in one editable file.** `device-sync.json` stays the app's own store (queue, library, histories, account/credentials, pairing), while the new file mirrors the options only, keyed with the app's own field names. The app rewrites it on every change and watches it: a value edited while the app is running is applied immediately (no restart), and a value edited while the app was closed wins at startup. Credentials, API keys, histories, queue/library/playlists, pairing and window state are never written there, and are ignored if they are added by hand.

### Changed
- [DE] **The Android APKs are no longer release assets.** `Auto Release (Desktop)` no longer builds, waits for or attaches an APK of any kind: a release now carries the desktop installers only. `Build Android APK` is manual-only and publishes the newest GMS and FOSS builds — fixed file names `vivi-gsm.apk` / `vivi-foss.apk`, plus a `version.json` describing the build — to `.releases/apk/latest` on the dedicated `apk-latest` branch, which is recreated as a single force-pushed commit so the repository history never grows. The Devices screen offers the two variants as direct links (GMS first) instead of resolving an APK out of the releases API. The obsolete upstream `Production Release Build` workflow (the one that published an APK release from `main`) was removed.
- [APK] The companion app checks for updates against that channel: it reads `.releases/apk/latest/version.json` and offers the build it describes, with **the version code** deciding what is newer (falling back to the version comparison only when that manifest carries none), and downloads the fixed URL — no GitHub API and no release page involved.
- [DE] **Every repository link now points at `PiBOH/vivi-music-de`**, the official repository (branches `vivi-music-de` for the desktop edition, `vivi-music-de-apk` for the mobile side): the app (about screen, update source, changelog, contributors), the Windows installer, the issue templates and security policy, the AUR PKGBUILD generator, the docs and the website. The site is served from `https://piboh.github.io/vivi-music-de/`.
- [WEBSITE] The APK buttons (download dialog and downloads page) point at the fixed `.releases/apk/latest` URLs, so they no longer depend on a release asset or on an API call.

### Fixed
- [DE] General improvements and fixes.

## [6.0.6.3_DE-1.51.2-alpha] - 2026-09-19

### Fixed
- [DE] **The 30-second music stutter and UI hitch of #3 were a hidden `System.gc()`, not the audio pipeline.** Reproduced on the packaged image with `-Xlog:gc`: Skiko — Compose Desktop's own renderer — keeps a `FrameWatcher` coroutine that waits `gcDelayMillis = 30000`, then calls `System.gc()` whenever the window rendered fewer than `minFramesToRenderer = 1000` frames in that window, which is the normal state of a player that only redraws a seek bar. Each of those calls was a **full, stop-the-world collection**: the log shows `Pause Full (System.gc())` at 30.05 s intervals, 46-69 ms on a fresh session and growing to **1976 ms** in a long one, freezing the audio writer thread (the audible skip) and the Compose snapshot loop (the mini UI lag the reporter noticed at the same moment) together. The desktop JVM now runs with `-XX:+ExplicitGCInvokesConcurrent`, which turns those calls into concurrent G1 cycles: same memory reclamation, **zero `Pause Full`**, and the 30 s event becomes a bounded 2.9-9.9 ms young pause that the existing 8 s PCM queue + 1 s device ring absorb completely. Verified on the packaged image before/after, not by reading the code. (Refs #3)

## [6.0.6.3_DE-1.51.1-alpha] - 2026-09-19

### Changed
- [DE] **Playback diagnostics now cover the two things the exported logs could not see (#3).** Every existing check (device starvation, cushion, JVM/priority stalls) assumes the PCM handed to the sound card is itself continuous, so a report of "it still skips" could come back with a perfectly clean log while the audio was in fact discontinuous. Two new measurements close that gap:
  - the **sample table is verified for continuity**: each newly scanned `moof` fragment must continue exactly where the previous sample ended, and a gap/overlap is now logged as `sample table discontinuity at frame N … (delta X bytes) — the decoded audio skips/repeats here`;
  - the **sound card is sampled every 10 s** for how much audio it actually played against the wall time (`audio device check: played 10000ms of 10000ms wall (100%)…`), and a device that consumes less than real time is logged as `audio device stall` — a gap *below* our buffers, which no other check can detect.
  Each track then ends with a one-line summary (`audio integrity: N sample-table discontinuities, M device stalls, K frames scanned`), so a log alone says whether that track was ever fed discontinuous audio.

### Notes
- [DE] Findings behind this change, from the exported 1.50.71/1.50.76 logs: on the reporting macOS machine there is **not one** starvation, starved-queue, low-cushion or device-stall line, and the device granted a **4 s** ring (the app asks 1 s, macOS accepts the 4 s candidate first) in front of the 8 s PCM queue; on the reporting Windows machine the only events are `audio priority stall`s of ~120-160 ms every few minutes (GC counters frozen, heap ~32 MB of 2048 MB, CPU ~10 %) and the 8 s queue + 1 s ring absorb every one of them. The macOS ring is deliberately left at 4 s: those same logs contain pauses of the writer thread of over a second, and a smaller ring would turn them into audible dropouts.

## [6.0.6.3_DE-1.51.0-alpha] - 2026-09-19

### Added
- [DE] **Animated lyrics, ported from the mobile app.** The desktop lyrics panel used to be a plain list of lines with the current one in bold. It now has the APK's word-by-word animation styles, selectable in *Settings → Lyrics*: **Simple** (per-word weights), **Fade**, **Glow**, **Slide** (a gradient sweeps the word), **Karaoke**, **Apple Music**, **Apple Music V2** and **VIVI Music** (the last two bloom each word in place with its own scale and blur).
- [DE] **Every display option around them**: word glow, blur of the lines that are not being sung (standard and the stronger Apple Music variant), tap-a-line-to-seek, auto-scroll, text alignment (left / center / right) and the text size / line spacing sliders. The derived switches only appear once their parent option is on, so the list never fills up with toggles that cannot do anything.
- [DE] **Romanization and lyric translation actually render.** The lyrics screen now shows the romanized text under each line (`LyricsRomanizer`, a pure-JVM port of the mobile tables: katakana incl. the sokuon, Hangul with the final-consonant assimilation rules, the seven Cyrillic orthographies with per-language detection, Devanagari and Gurmukhi with the inherent vowel restored) or as the main line, and — when *Translate lyrics* is on — a translation of every line, obtained from the AI provider already configured in Settings (OpenRouter/OpenAI-compatible chat completions, or DeepL) and cached per track/language.

### Changed
- [DE] **The lyrics parser keeps the per-word timings.** The desktop used to strip every `<mm:ss.xx>` tag and keep only the line times, which is why no karaoke animation was possible; the shared `lyricsProvider` module now parses classic LRC, rich-sync, the TTML word-list layout and the `{agent:v1}` / `{bg}` markers (JVM port of the mobile `LyricsUtils`, with unit tests).
- [DE] The lyrics position is read per line, so the ~40 playback updates per second recompose only the visible lines instead of the whole panel; the sung line is still decided by a slower poll so the list is not scrolled at the full position rate.
- [DE] Settings search (already able to reach sub-screens) now indexes the new lyrics keys as well.

## [6.0.6.3_DE-1.50.76-alpha] - 2026-09-18

### Changed
- [DE] **The site's release manifest now refreshes itself every hour**: `Release Manifest` only ran when `version.txt` changed, and that happens the moment a release is pushed — minutes *before* the release workflow has built and uploaded the assets. The manifest it wrote therefore described a release whose files did not exist yet, so the downloads page and the download dialog stayed incomplete (and needed a manual page refresh) until the next release happened to bump `version.txt`. It now also runs on `cron: '0 * * * *'` (every hour, at :00 CEST), re-reads the published releases and commits `releases.json`/`changelog.json` only when something really changed. Running it on a schedule is safe here because the repository's default branch is `vivi-music-de`, not the upstream `main` mirror (which stays a read-only mirror), and the job now pins `ref: vivi-music-de` on its checkout so that stays true even if the default branch ever changes.
- [DE] **A manual release can be published without the APKs**: `Auto Release (Desktop)` gained a `workflow_dispatch` input named `ignore_apk_failure` (**off by default**, manual dispatch only) which makes the release step warn and publish the desktop assets instead of failing when the Android build for that commit failed, its artifacts cannot be downloaded, or `vivi-gsm.apk`/`vivi-foss.apk` are missing. With the toggle off nothing changes: on a release push the two APKs remain mandatory assets and a failed Android build still fails the release.

## [6.0.6.3_DE-1.50.75-alpha] - 2026-09-18

### Fixed
- [DE] **Emergency fix — the app crashed on launch in 1.50.72, 1.50.73 and 1.50.74**: every build in that range died before drawing its first frame with `NoClassDefFoundError: androidx.compose.material.icons.Icons$Outlined` (the crash dump points at `MainKt.Sidebar`, the first icon the UI asks for). The cause was the icon-minimization change of 1.50.72: it replaced the ~36 MB `material-icons-extended` artifact on the **runtime** classpath by *excluding* that module, but `material-icons-extended-desktop` is the only route through which `material-icons-core-desktop` reaches the app, and `material-icons-core` is the artifact that declares the `Icons` accessors (`Icons.class`, `Icons$Filled`, `Icons$Outlined`, `Icons$Rounded`, `Icons$Sharp`, `Icons$TwoTone`, `Icons$AutoMirrored`). The extended jar itself only carries the per-icon `...Kt.class` files, so no additional entry in the minimized jar could have repaired it. The exclusion now drops only the extended jar, and `material-icons-core` is put back explicitly at the version the Compose plugin resolves (read from the resolved compile classpath, so it can never drift away from the plugin's own selection). Verified against the packaged app image: its `app/` folder now holds `material-icons-core-desktop-1.7.3.jar` (858 KB) next to the minimized jar (1.3 MB), and a probe that loads every `Icons` accessor plus the per-icon classes on that classpath succeeds — while the same probe without the core jar reproduces exactly the reported `ClassNotFoundException: Icons$Outlined`. A `check` in `desktop/build.gradle.kts` now fails the packaging loudly if `material-icons-core` ever leaves the graph again, so a build that cannot start can no longer ship silently.
- [DE] **If you are on 1.50.72, 1.50.73 or 1.50.74, install this build by hand**: those versions cannot start, so the in-app updater never gets a chance to run. Windows: download `VIVIMusic-6.0.6.3_DE-1.50.75-setup.exe` and run it over the existing installation (your settings and cache are kept). macOS: the `.dmg`/`.pkg` for your architecture; Linux: `.deb` or `.AppImage`.

## [6.0.6.3_DE-1.50.74-alpha] - 2026-09-17

### Changed
- [DE] **Exported logs stop growing forever**: `Export logs` now packages the logs of the **20 newest sessions** only (`LogExporter.collectSessionDirs`, i.e. the newest 20 `~/.vivimusic/logs/<yyyyMMdd-HHmmss>/` folders, sorted by name so it is chronological). Older sessions are **not deleted** — they stay on disk, they are just no longer part of the support zip, so a long-lived install no longer produces an archive nobody can open.

### Fixed
- [DE] **No more dropout at the very beginning of a track**: the output line was started the moment it was opened, so the device began consuming from a ring that was still empty and went dry before the first decoded block arrived — the reported `playback.log` shows exactly that (`audio output starved … the device ran dry here` **70 ms** after a track had started, `line headroom 1000ms, unplayed 0ms`), which is the click/skip heard at the start of a song. The line is now left stopped (JavaSound buffers writes) and started only once 300 ms of audio already sit in the device ring — or sooner, when the producer has nothing more to hand over, so a slow decode cannot delay the start forever; the cushion is logged (`audio output primed: device started with 360ms already queued in its buffer`). Pausing before the first block now keeps the start with the writer instead of starting an empty ring on resume, and the starvation line distinguishes `nothing had been handed to the device yet` from a real `the device ran dry here`. ([issue #3](https://github.com/PiBOH/vivi-music-de/issues/3))

## [6.0.6.3_DE-1.50.73-alpha] - 2026-09-17

### Changed
- [DE] **The activity log keeps the full picture**: the change history written to `~/.vivimusic/logs/<session>/settings.log` now records every setting at its complete value, instead of shortening long ones, so an exported log zip describes exactly what was changed when a report is reproduced.

## [6.0.6.3_DE-1.50.72-alpha] - 2026-09-17

### Changed
- [DE] **The installers are much smaller**: the packaged app carried the whole `material-icons-extended` artifact (~36 MB, ~10k vector icons) while the desktop app references fewer than 300 of them. A new `MinimizeIconsJarTask` (`desktop/build.gradle.kts`) keeps that artifact on the *compile* classpath but puts a jar holding only the referenced icons on the *runtime* classpath — the one every jpackage setup consumes — so the installed app image drops from ~194 MB to ~134 MB. No user-visible change: the same icons are shown, and the task takes the six style directories into account (`Default` maps to `filled`).
- [DE] **Windows `.exe` setup compressed harder**: `installer/windows/VIVIMusic.iss` now uses `lzma2/max` with `SolidCompression=yes` (there are no optional `[Components]`, only the two shortcut `[Tasks]`, so a partial install still does not decompress the whole block). Together with the icon trimming the setup goes from 156.0 MB to ~129 MB, and the same payload reduction applies to the `.msi`, the macOS `.dmg`/`.pkg` and the Linux `.deb`/`.AppImage` (their formats are unchanged).
- [DE] Published formats are unchanged: Windows ships both `setup.exe` and `.msi`, macOS both `.dmg` and `.pkg`, Linux `.deb` + `.AppImage` + `PKGBUILD`. The JavaFX WebView jars (`javafx-web`, `icudtl.dat`) are required by the working sign-in WebView and were deliberately left untouched.

## [6.0.6.3_DE-1.50.71-alpha] - 2026-09-16

### Fixed
- [DE] **The volume slider is heard immediately, not a second later**: the 1.50.68 fix removed the *queue* delay (the gain is applied by the writer on the block it is about to hand over instead of while decoding, up to 8 s ahead), but the device's own ring still held a full second of already-scaled audio — the output line is opened with a 1000 ms buffer on purpose, to absorb the decode/GC stalls of issue #3 — so the change could only be heard after that second had played out. The level now goes to the sound card's own gain (`MASTER_GAIN`, verified available and settable on the reporting machine: -80 dB … +6 dB), which the driver applies while it consumes the ring, so the change is audible at once; scaling the PCM stays as the fallback for a line without that control, and the startup line says which path is in use (`volume 42% (device gain)`). ([issue #79](https://github.com/PiBOH/vivi-music-de/issues/79))
- [DE] **Lyrics are found for tracks whose title uses stylized Unicode**: every provider matches against its own plain-text catalogue, while YouTube Music titles are full of decorative characters — `ＭＩＧＵＥＬ 𝑷𝒉𝒐𝒏𝒌` (fullwidth), `𝑷𝒉𝒐𝒏𝒌`/`𝗖𝗥𝗢𝗪𝗡` (mathematical alphanumerics), plus accents, typographic quotes and even invisible characters (the reported title carries a `U+FEFF` inside it). The request and the candidate shared no token, so the strict KuGou title/artist check rejected the correct result and the other providers were queried with a string they could not match either: the reported track ended with `no lyrics from any provider` even though KuGou's search returns candidates for it. Titles, artists and albums are now folded to plain Unicode (NFKD, so fullwidth and mathematical alphabets decompose to ASCII, then combining marks and invisibles are dropped) for matching and for provider queries, while the original text is still what is displayed and logged. Bumped the lyrics cache version, so answers produced by the older matcher are re-fetched once. ([issue #81](https://github.com/PiBOH/vivi-music-de/issues/81))
- [DE] **Playing music no longer recomposes the whole app ~20 times a second**: the app root read the audio level (`player.audioLevel.collectAsState()`), so every audio tick recomposed the entire composable tree — all screens — on top of the audio-reactive visualizer background repainting at display rate. Profiling the running app while it played put `AWT-EventQueue-0` at ~50 % of a core of continuous painting, and the stall log of the same session shows 0.4-1.2 s freezes with **no GC activity and a ~50-150 MB heap** (of 2048 MB), i.e. CPU contention rather than a JVM pause. The level is now passed down as a flow and collected only inside the visualizer background that displays it, the level feed is decimated to ~14/s (it already had a 120 ms tween, so it still looks continuous), and the device sync loops (in-app volume push, OS volume mirror, periodic position resync) skip entirely while unpaired instead of poking the sync socket and doing a native COM volume read twice a second for the whole session. ([issue #3](https://github.com/PiBOH/vivi-music-de/issues/3))

## [6.0.6.3_DE-1.50.70-alpha] - 2026-09-16

### Fixed
- [DE] **Wrong lyrics finally disappear**: fetched lyrics are cached on disk and served without asking a provider again, so a wrong association survives any fix to the resolver — the reported `Blu Da Ba Dee` kept showing `Move Your Body (DJ Gabry Ponte Original Video Edit) - Eiffel 65` even after 1.50.67 fixed exactly that matching, because the entry was written before it. Verified on the reporting machine: `~/.vivimusic/cache/lyrics` held a single file, `68ugkg9RePc.s.v4.txt` (the video id of `Blu Da Ba Dee`), containing that other song. The cache version is bumped to v5, so entries written by an older resolver are ignored and re-fetched once with the current matching; the version is a named constant now, with the rule that **any** change to provider selection or validation must bump it. ([issue #80](https://github.com/PiBOH/vivi-music-de/issues/80))

### Removed
- [DE] **The video-captions lyrics source added in 1.50.69 is gone again**: `get_transcript` answers `400 FAILED_PRECONDITION` to the synthesised params for **every** video — captioned ones included (`dQw4w9WgXcQ`, `9bZkp7q19f0` and the reported track tested with the exact client context and headers the app sends, with and without a visitor id) — so that source cannot answer anything; the Android app lists the same one as `YouTubeSubtitle`, which means it is failing silently there as well. It only added a failed round-trip to every lookup, so the 1.50.69 entry above is corrected by this one. ([issue #80](https://github.com/PiBOH/vivi-music-de/issues/80))

## [6.0.6.3_DE-1.50.69-alpha] - 2026-09-16

### Fixed
- [DE] **Volume changes are heard immediately**: the gain was applied to the PCM while it was *decoded*, and the producer renders up to 8 s of audio ahead of the sound card, so moving the slider only reached the output after the whole queue had played out — on a cached track that is ~8 s of "the slider moved and nothing happened". The block that is about to be handed to the device is scaled by the writer instead, so the delay is one write block (~120 ms), and the value the output actually used is logged (`volume: output gain now 60%`). ([issue #79](https://github.com/PiBOH/vivi-music-de/issues/79))
- [DE] **Dragging the volume no longer rewrites the settings file once per frame**: a slider drag emits ~20 changes per second and every one of them re-read and rewrote the whole settings file on the calling thread — the very thread that has to keep the UI responsive while audio plays. The write is coalesced now (the last value is persisted once the drag has been still for 400 ms) while the audio and the UI still follow the slider instantly. ([issue #79](https://github.com/PiBOH/vivi-music-de/issues/79))
- [DE] **The settings file is no longer read and parsed twice a second while playing**: the polling loops that run for the whole session (volume sync every 500 ms, "pause when muted" watchdog every 800 ms, command poll every 500 ms) each called `DesktopSettings.load()`, which re-read and re-parsed ~58 KB of JSON every time. Loads now come from an in-memory snapshot that every write refreshes, so the same work happens once per session instead of ~3 times per second. ([issue #3](https://github.com/PiBOH/vivi-music-de/issues/3))
- [DE] **The silence-cut gate measures the right thing**: the cushion that decides whether "skip silence" may cut was computed by dividing downloaded *AAC* bytes by the *PCM* byte rate — the stream is compressed, so the cushion was under-reported by ~10x and cuts were held back even on fully cached tracks (`skip silence: held back at ~125s — only 0.0s of source buffered` in the reporting user's log). It is measured in decoded frames now (scanned source seconds minus consumed source seconds), so a cached track has its whole length as cushion and a streaming one stops cutting exactly when the download is really at the frontier; the `audio stall` line's "download N s behind" uses the same measurement. ([issue #3](https://github.com/PiBOH/vivi-music-de/issues/3))
- [DE] **The playback-stall probe now measures the audio thread instead of a lowest-priority helper**: it ran at `MIN_PRIORITY` (Windows `THREAD_PRIORITY_LOWEST`), so it reported "frozen" for delays the writer never saw — on the reporting machine it logged 100-1200 ms stalls whose exported log contained no starvation line at all, i.e. the 8 s queue and the 1 s device buffer had absorbed them. It sleeps at the *same priority as the writer* now and logs `audio priority stall: … (= N ms held up at the writer's priority; heap …, gc …, cpu …)` with the process and system CPU: a JVM pause shows the GC counters moving, a saturated machine shows the CPU figure. ([issue #3](https://github.com/PiBOH/vivi-music-de/issues/3))
- [DE] **Lyrics for the tracks the community servers do not index**: the chain now asks for the **video's own timed captions** (the mobile app's `YouTubeSubtitle` source, `YouTube.transcript(videoId)`) before falling back to the official plain-text lyrics. It is keyed by the video that is playing, so it can never be another song's lyrics, and it is timed — remixes, slowed/"phonk" edits and covers with captions now show lyrics instead of failing on every provider, as in the reported case. ([issue #80](https://github.com/PiBOH/vivi-music-de/issues/80))

## [6.0.6.3_DE-1.50.68-alpha] - 2026-09-16

### Fixed
- [DE] **Streaming playback keeps a real cushion instead of running on the download frontier**: playback started with only the first fragment (~2 s) on disk, so the 8 s PCM queue could never fill up and the whole pipeline stayed pinned to the frontier — every pause in the delivery reached the sound card as a gap. The output line now waits for the pre-buffer (8 s of source) before it is opened, capped at 3 s of wall clock so a slow link still starts in seconds and skipped entirely for a cached file, and the wait is written to `playback.log` (`pre-buffered 6.2s of source before starting the output (wanted 8s)`). ([issue #3](https://github.com/PiBOH/vivi-music-de/issues/3))
- [DE] **"Skip silence" can no longer starve the sound card**: a cut runs the decoder forward *without* producing output, so making one while the download is close behind is exactly what caused the reported dropouts (the log shows 1.5-2.2 s of silence cut every few seconds on a silence-heavy track, and `audio stall: waited 424ms for data` right next to it, while the output line was empty). A cut is now only allowed when at least 3 s of source is already buffered ahead; otherwise the silence is played instead — a natural pause beats a dropout, and it lets the download catch back up — with each hold-back logged (`skip silence: held back at ~42s — only 0.4s of source buffered`). ([issue #3](https://github.com/PiBOH/vivi-music-de/issues/3))
- [DE] **The stall diagnostics now say how far behind the download is**: `audio stall: waited 424ms for data at ~1s (line headroom 1000ms, queued 0ms, download 0.9s behind)` — the pair "waited X ms for Y s of source" tells a slow network apart from a slow decoder, which the previous line could not. The `audio output starved` line also reports the unplayed cushion now and states when the device ran dry, the one state the `audio cushion low` check could never see because it only runs when there is something left to write. ([issue #3](https://github.com/PiBOH/vivi-music-de/issues/3))
- [DE] **The look-ahead cache pass yields to the track that is playing**: the queue prefetch waits while the playing track's own cushion (decoded PCM + source already downloaded) is below 20 s, so filling the cache can no longer take bandwidth away from the stream the user is listening to; the wait is bounded at 60 s per track, so a failed download cannot park the pass. ([issue #3](https://github.com/PiBOH/vivi-music-de/issues/3))

## [6.0.6.3_DE-1.50.67-alpha] - 2026-09-15

### Fixed
- [DE] **The live log window can no longer crash the app**: the log list used each line's text as its `LazyColumn` key, and two identical lines in the same millisecond are completely normal (a retried resolve logs the same text twice, as it did for `CHICKEN BANANA` in the reported crash), so Compose threw `Key "…" was already used` and killed the process. Every line now carries a monotonic id assigned by the logger itself, which is what the list is keyed on — the same audit was applied to the other lists whose keys could repeat, and the two that did (the home chip rows and the mixed-for-you row, where the same title can legitimately appear twice) are keyed by position plus title now. ([issue #76](https://github.com/PiBOH/vivi-music-de/issues/76))
- [DE] **The lyrics shown are the ones of the song that is playing**: the KuGou provider picked the first candidate whose duration was within ±8 s of the track, and its search is fuzzy — asked for `Blu Da Ba Dee` (219 s) it answers with eight Eiffel 65 tracks, one of which is `Move Your Body` (211 s), so a completely different song's lyrics were shown (and, being timed, drifted). Candidates now have to match the requested names as well: the song and artist are compared token by token (with the parenthesised suffixes, `feat.` credits and formatting noise ignored, and both readings accepted because that endpoint sometimes swaps the two fields), and a synced result that runs more than a minute past the end of the track is discarded as another version. Lyrics that cannot be matched from any provider are better than confidently wrong ones. The module is shared with the Android app, so both clients are fixed. ([issue #77](https://github.com/PiBOH/vivi-music-de/issues/77))
- [DE] **Resuming from pause no longer starts with a short silence**: the pre-rolled PCM that was already queued was dropped on pause, so the first sound after pressing play had to wait for the decode thread (and the network, when the track is still streaming) to hand the writer new audio — the `audio output starved: queue empty waiting for decode` line in the reported `playback.log`. Pausing keeps the queued audio instead of discarding it, so play continues from the cushion that is already there. ([issue #3](https://github.com/PiBOH/vivi-music-de/issues/3))
- [DE] **The Home "Your artists feed" header opens a real page again**: it asked the server for `FEmusic_library_corpus`, the library's *tabbed container*, without the selection params such a page requires, so the request was rejected with `400 INVALID_ARGUMENT` — and every browse failure was reported as E1031 (`BROWSE_UNAUTHENTICATED`, "sign in again"), which sent users to re-authenticate for what was a request problem. The header now opens `FEmusic_library_corpus_artists`, the self-contained view the Android app opens too, and a request the server rejects as invalid has its own code (E1032) in the error box and in `browse.log`. ([issue #78](https://github.com/PiBOH/vivi-music-de/issues/78))

### Added
- [DE] **`window.log`: the window itself is now part of the exported diagnostics**, together with a hint for screen recording. Tools like OBS only list windows that are visible and not minimized, so "my recorder does not see VIVI" is normally invisible in the logs; the session log now records the window's state at creation, when it is shown and on every minimize/maximize/restore (`decorated`, `visible`, `iconified`, bounds), the Skiko render API in use and the OS. The category is always created and always exported, like the others.

## [6.0.6.3_DE-1.50.66-alpha] - 2026-09-15

### Fixed
- [DE] **Nothing application-side can delay the audio thread any more**: the position, level, buffered and duration callbacks (seek bar, synced lyrics, crossfade scheduling, listen history, and every Compose/state update behind them) are no longer invoked on the decode or writer thread — they are published to a single dedicated pump thread, which drops intermediate values instead of queueing them. Application code running on the thread that hands PCM to the sound card was a stall waiting to happen. ([issue #3](https://github.com/PiBOH/vivi-music-de/issues/3))
- [DE] **The device buffer — the cushion that actually absorbs a late writer — is now verified, maximized and logged**: the app reads back what the device granted and asks for up to 4 s (a 1 s ask before), keeping the legacy 8-16 KB sizes as the very last resort, and writes at every track start `audio output: 44100Hz 16bit 2ch, device buffer Xms (asked 1000ms) …` so the real cushion is visible in the exported log instead of being an assumption. ([issue #3](https://github.com/PiBOH/vivi-music-de/issues/3))
- [DE] **A real audio gap is finally measurable**: a cushion monitor computes the audio already handed to the device minus the audio it has played, and logs `audio cushion low: only Nms of audio left in the device buffer` right before the output can run dry. The previous diagnostics could only see the software PCM queue run empty — and that queue can hold seconds of audio while the device ring empties, which is exactly the class of gap users still hear. A JVM stall watchdog completes the picture: a 50 ms sleep that returns 120 ms late is nobody's fault but the JVM being frozen (stop-the-world GC), so `jvm stall: … (heap X/Y MB, gc …)` is recorded next to the heap and GC counters. ([issue #3](https://github.com/PiBOH/vivi-music-de/issues/3))
- [DE] **PCM is handed to the sound card in ~120 ms blocks instead of one AAC frame (~23 ms)**: ~8 wakeups per second rather than ~43, each of which used to have to be scheduled in time to keep the device fed, with the same latency (the audio is written ahead anyway). ([issue #3](https://github.com/PiBOH/vivi-music-de/issues/3))
- [DE] **The JVM is tuned for low pause times**: the heap is capped at 2 GB and the young generation at 384 MB with a 20 ms G1 pause target. The default max heap is 25% of the machine's RAM (6 GB in the reporting user's system info) and G1 may then grow the young generation to 60% of it, i.e. collection sizes — and stop-the-world pauses — far beyond what a music player needs. ([issue #3](https://github.com/PiBOH/vivi-music-de/issues/3))
- [DE] **"Skip silence" now declares what it cuts**: every suppression run is logged with its length (`skip silence: cut 210ms of silence at ~42s`) plus a per-track total, because the option deliberately drops audio and a short jump is expected — a user hearing skips must not be left wondering whether the app is supposed to cut that part of the song. ([issue #3](https://github.com/PiBOH/vivi-music-de/issues/3))
- [DE] **Turning "Media keys" off on macOS now takes effect immediately**: the remote-command handlers are removed instead of only being marked disabled (which left the app as the system's "Now Playing" owner, so the keys kept arriving until the next restart), and a metadata push can no longer re-claim the tile after the switch was turned off. ([issue #63](https://github.com/PiBOH/vivi-music-de/issues/63))
- [DE] **macOS "Now Playing" keeps its claim while a track is loaded but paused**: with the persistent queue restored, nothing has been played since launch, so the tile is now refreshed every 2 s (while playing it stays at 500 ms) — if the claim goes stale the first media-key press cannot reach the app. Every remote command the system actually delivers is logged (`[mac-media] remote play/pause`, `next`, `previous`, `seek`), which tells "the key never reached VIVI" apart from "the key arrived and did nothing". ([issue #63](https://github.com/PiBOH/vivi-music-de/issues/63))
- [DE] macOS: the now-playing artwork is decoded once per track instead of on every metadata push (twice a second), so a track's tile no longer re-reads and re-decodes the image from disk on the main thread while playing. ([issue #63](https://github.com/PiBOH/vivi-music-de/issues/63))

## [6.0.6.3_DE-1.50.65-alpha] - 2026-09-14

### Fixed
- [DE] **Playback no longer pauses or skips: decoding and output are two separate threads now**: the 1.50.63 buffer increase only bought headroom, because a single thread was still decoding the AAC, walking the sample table, waiting on the network *and* calling the blocking `SourceDataLine.write` on the same deadline — every hiccup of that thread (a 256 KB atom scan, a GC pause, a download wait while streaming, UI contention) went straight to the sound card as the micro-pause users hear on almost every song. The decode thread now renders PCM into an 8-second queue and a dedicated maximum-priority writer thread does nothing but hand those bytes to the line, so the writer can only ever be late if the queue itself ran dry — several seconds of audio, not a few hundred milliseconds. The queue is filled ahead at every play and seek, the queued tail is played out on a normal end of track instead of being cut, and a stop/seek/failure drops it immediately. (Fixes #3)
- [DE] **The random audio gap is now measurable in the exported log**: the stall line in `playback.log` also reports how much audio was still queued (so "the decoder was slow but covered" is distinguishable from a real output gap), and the writer logs `audio output starved: queue empty waiting for decode` with the line's remaining headroom whenever the queue runs dry — the one and only situation that can still be audible. (Fixes #3)
- [DE] **The macOS "Now Playing" tile appears on a fresh launch without touching the switch**: the app called `endSession` whenever there was no track — which includes startup, when `nowPlaying` is still null — and that unregistered the session; since registration only happened from the media-keys effect (keyed on its own switch), a first track afterwards reached a session that was gone and `setNowPlaying` returned early, so Control Center and the media keys stayed dead until the switch was toggled. Clearing the tile is now a separate native call (`viviClearNowPlaying`) that keeps the session, its handlers and the app identity alive, and pushing a track re-registers the session by itself if it was never registered or was torn down, so playback always brings Now Playing back. (Fixes #63)

## [6.0.6.3_DE-1.50.64-alpha] - 2026-09-13

### Fixed
- [DE] **The header's audio output device button works**: it was a leftover placeholder whose click handler was an empty block, so the picker could only be reached from the mini player. It now opens the same dialog and the chosen device is written to the session log. The device list no longer relies on the strict "does this mixer accept 44.1 kHz / 16-bit / stereo right now" probe either — the default OS endpoint answers "no" to that and negotiates the format only when the line is opened, which could leave the picker showing nothing but "System default"; the mixer's own playback-line list is probed instead. (Fixes #73)

### Changed
- [DE] **The mouse back/forward buttons are always on**: the X1 (back) / X2 (forward) behaviour is now unconditional and the "Mouse back / forward buttons" switch was removed from Settings → Appearance, along with its persisted setting. (Closes #74)
- [DE] **The first button in the header is the sidebar toggle**: the leading control used to be a no-op "Open menu" placeholder (empty click handler); it is gone and the expand/collapse sidebar button now sits in its place, ahead of back and forward. (Closes #75)

## [6.0.6.3_DE-1.50.63-alpha] - 2026-09-13

### Fixed
- [DE] **Audio micro-pauses/skips are greatly reduced and are now diagnosable**: the output line held only ~250 ms of audio, which is the sole jitter headroom between the decode thread (disk scans, network waits, GC pauses) and the sound card — a 256 KB burst scan on top of a GC pause could exceed it, the line underran and you heard the random sub-frame "pause" reported on every song. The line now asks for ~500 ms of audio; the fallback chain halves that (500 → 250 → 125 ms) before it ever drops to the legacy 8-16 KB sizes that made the problem audible; and every real underrun (a wait of more than 40 ms for download data while the line is nearly empty) is written to `playback.log` together with the wait and the line's remaining headroom, so a future report is something we can check instead of guess. (Fixes #3)
- [DE] **The macOS "Now Playing" tile and its media keys survive a restart**: the remote commands were enabled once at registration, before the handlers were installed, so a fresh launch (or a session cleared by `viviEndSession`) could leave a visible tile whose buttons were disabled — toggling the "Media keys" switch off and on again was the only way to bring them back. The enabled intent now lives on the native side and is re-asserted at registration, on every session start and on every new track, so the tile works from the first launch. (Fixes #63)
- [DE] **The Library empty state no longer shows the raw `refresh` key**: the key was used by the source but never added to the string table, so the Retry button literally read "refresh". (#68)

### Added
- [DE] **Every new desktop string is translated into all 52 languages**: the animation-speed options (4), the mouse back/forward row (2), the audio output device row (2) and `refresh` shipped English-only in 1.50.62; the generic "System default" label reuses the existing Android translation where one exists. `scripts/check_localization.py` reports every language table complete again (batch 69).

## [6.0.6.3_DE-1.50.62-alpha] - 2026-09-12

### Added
- [DE] **The mouse side buttons navigate back and forward**: pressing X1 (button 4) goes back and X2 (button 5) goes forward through the screen history, like every browser and media app, with a new switch in Settings → Appearance to turn it off. The listener is installed at the AWT level because Compose's Skia canvas consumes the mouse event before it ever reaches a listener attached to the window, and it only reacts to clicks inside the main window (the floating widget and dialogs keep their own handling). (#64)
- [DE] **Real audio output device picker**: the "output device" button in the mini player used to just bump the volume up by 10%; it now lists the actual Java Sound mixers the OS exposes (Speakers, Headphones, virtual devices…) plus "System default", persists the choice and reopens the audio line on the selected device. A device that disappeared since it was saved falls back to the system default instead of breaking playback. (#65)
- [DE] **"Animation speed" option (Fast / Normal / Slow)**: a global multiplier applied to the UI transitions (screen transitions, player crossfade), on top of the existing on/off switch. (#72)
- [DE] **The like button actually likes**: the full player had no heart at all and the mini player's heart only flipped a local flag that was lost on exit. Both now read and write the account state through the same path as the song menu (optimistic toggle + InnerTube `likeVideo`), so the heart reflects the YouTube account and "Auto download on like" fires from the player too. (#66)

### Fixed
- [DE] **Restoring a backup restores the playlists again**: the playlists entry of the archive was written and read independently of the settings, so a backup whose playlists entry failed to decode (or predated it) restored the settings but silently dropped every playlist. Each entry is now decoded independently and, when the archive carries no playlists, the copy embedded in the settings' library snapshot is used as a fallback; the restored count is written to the `backup` log. (#67)
- [DE] **Empty "Liked songs" / albums / artists / playlists library tabs can be reloaded**: an empty list caused by a transient failure (expired session, 401, network hiccup) left the screen blank with no way to retry without leaving and reopening it; the empty state now offers a Retry button (and the screen's data-loading effect tracks it). (#68)

### Changed
- [DE] **Seekbar lag reduced (~50 ms)**: the decoded position was reported to the UI at most every 100 ms, so the slider could trail the audio by up to two ticks; it is now reported every 50 ms (~20/s), still far below one report per decoded frame. (#69)
- [DE] **Playback & network stack**: the audio engine now uses a shared OkHttp connection pool (5-minute keep-alive, 12 connections) and a dispatcher that allows the current track and the surrounding prefetches to download in parallel, so TLS handshakes are not paid again for every song; the connect timeout dropped from 15 s to 10 s and the "wait for the buffer to catch up" poll interval halved to 15 ms, cutting the worst-case start/seek latency. (#70)
- [DE] **Faster startup**: the settings file was read and JSON-parsed about a dozen times while building the first frame (once per setting); it is now parsed once per screen and the values are reused, and the redundant duplicate reads were removed. (#71)
- [DE] **Regression scan for the #61 class of bug**: every `pointerInput` block in the desktop module was analysed for a `detectTapGestures` call followed by a drag detector in the *same* block (the second detector never runs because the first never returns). The theme picker was the only occurrence and it was already fixed in 1.50.61; nothing else needed changing.

## [6.0.6.3_DE-1.50.61-alpha] - 2026-09-12

### Fixed
- [DE] **Theme picker: the color bars follow the pointer and a HEX field was added**: each gradient bar of the custom-color picker ran `detectTapGestures` first and `detectDragGestures` after it inside the same `pointerInput` - but `detectTapGestures` never returns (it loops forever), so the drag detector was dead code and the bars only reacted the moment the mouse button was released, which is exactly what the report describes ("I don't see the color or the sliders move until I release the mouse"). A single gesture loop now tracks the press and every movement, so the thumb and the previews update in real time; a `#RRGGBB` field applies a typed or pasted color immediately; and the accent-intensity slider only writes the settings file when the pointer is released instead of on every frame. (Fixes #61)
- [DE] **macOS "Now Playing" and the media keys no longer need the Accessibility permission**: the MediaPlayer session (Control Center / Lock Screen tile, keyboard + Touch Bar + headset buttons) was already implemented, but everything that turns it on was gated behind the macOS Accessibility permission and the "Media keys" switch - which that permission also disabled. Whenever the saved value was off, the session was never registered and the tile never appeared. On macOS the session is now always registered (it is an OS-level integration, so no permission is involved at all) and the switch only enables/disables its remote commands, clearing the tile when they are turned off instead of leaving buttons that do nothing; the Accessibility hint and its "Open System Settings" button are gone from that screen. The helper now also sets `MPNowPlayingInfoCenter.playbackState` (playing / paused / stopped), which is what makes macOS treat an app as the system's Now Playing source in the first place, and it no longer publishes the app name as the track title ("VIVI Music" could show up as the song). Every step - helper loaded, session registered, current track, failures - is written to the session `playback.log` so a report can be diagnosed. (Fixes #63)

### Added
- [DE] **The macOS window chrome follows the app's theme**: with the native title bar enabled it was always drawn light, which looked wrong over a dark VIVI on a light desktop. A new native call (`viviSetWindowAppearance`) applies `NSAppearanceNameDarkAqua` / `NSAppearanceNameAqua` to `NSApp` and every window, and it is re-applied whenever the app's Light/Dark mode changes. (Fixes #62)

## [6.0.6.3_DE-1.50.60-alpha] - 2026-09-11

### Fixed
- [DE] **The Windows uninstaller really is fixed this time - no `Control has no parent window` error and the user-data cleanup actually runs**: 1.50.59 moved the details box of the uninstall progress page into `CurUninstallStepChanged(usUninstall)`, but the cleanup (and every line it logs) still ran in `usPostUninstall`. By then Inno Setup has already torn the progress page down, so the very first write to the memo raised `Control 'TNewMemo' has no parent window`; the code then tried to log the failure in its own `except` handler, which raised the same error again - that uncaught second exception is the runtime error users saw - and because it aborted before doing any work, older backups and every cache were left behind on disk too. Verified by reproducing both on a real install/uninstall with Inno Setup locally. The box is now created, filled and left alone inside `usUninstall` (the only step in which the memo can be written) and the user-data cleanup moved there as well, so every cleanup line stays visible in the box; each write is additionally guarded, and if the box is ever unusable the line goes to `%TEMP%\VIVIMusic-uninstall.log` instead of stopping the uninstall. (Closes [#48](https://github.com/PiBOH/vivi-music-de/issues/48))

## [6.0.6.3_DE-1.50.59-alpha] - 2026-09-11

### Fixed
- [DE] **The floating "Now Playing" widget can no longer be dragged off screen**: the window position was saved as-is, so dropping the widget past an edge (or plugging out the monitor its saved position belonged to) pushed it where it could not be grabbed again. The position is now clamped to the union of every attached screen — both when it is restored at launch and after each drag, where an off-screen drop snaps it back to the nearest fully visible spot. (Closes [#60](https://github.com/PiBOH/vivi-music-de/issues/60))
- [DE] **The Windows uninstaller no longer aborts with 'Control has no parent window'**: the details box of the uninstall progress page was created in `InitializeUninstallProgressForm`, but the progress page is not displayed yet at that point, so it has no window handle — the memo was left orphaned and the uninstaller died while freeing its controls (`Control 'TNewMemo' has no parent window`), aborting the uninstall before any cleanup ran. The box is now created in `CurUninstallStepChanged(usUninstall)`, once the page is really on screen, which is what the installer side already did. (Closes [#48](https://github.com/PiBOH/vivi-music-de/issues/48))

### Changed
- [WEBSITE] **Motion is now forced on every page and on every device**: card lifts and most of the polish were hover-driven, so phones and iPhones had no motion at all, and the reveal observer's 12% visibility threshold never triggered for blocks taller than a small viewport (which is exactly what happens on a phone). Every page now auto-tags its sections, cards, rows and article paragraphs for the scroll reveal (threshold 0, staggered, with a fallback that never leaves a block invisible), a slow ambient gradient drifts in the page background everywhere, tapping a card gives the same response hover gives on desktop, and the body fades in on load. There is no `prefers-reduced-motion` opt-out: motion is part of the brand.

## [6.0.6.3_DE-1.50.58-alpha] - 2026-09-11

### Fixed
- [DE] **The Windows uninstaller's closing message matches the new retention rule**: it still said "a final backup of your settings, playlists and fonts was kept", but since 1.50.57 the only survivors are the newest `.vivide.backup` and `device-sync.json` (fonts and `playlists.json` are inside the backup, not next to it). It now lists exactly the two paths that were kept. [#10](https://github.com/PiBOH/vivi-music-de/issues/10)

## [6.0.6.3_DE-1.50.57-alpha] - 2026-09-11

### Changed
- [DE] **Uninstall now keeps only the newest `.vivide.backup` plus `device-sync.json`**: instead of building a new `backups\uninstall-<timestamp>\` folder out of raw `device-sync.json` / `playlists.json` / fonts, the Windows uninstaller, the shared Linux/macOS/AppImage `scripts/uninstall-cleanup.sh` and the AUR `post_remove` hook now keep the app's newest `.vivide.backup` (a real restore point - settings + playlists - that the app can import back) and `~/.vivimusic/device-sync.json`, deleting every other file: older backups, `playlists.json`, fonts, updates/audio/video/canvas/lyrics caches, logs and artwork. Windows picks the newest backup from the `YYYYMMDD_HHMMSS` timestamp at the end of the file name (the prefixes differ: `auto_backup_*` vs `vivimusic-de_*`), the shell hooks use `ls -1t`. INSTALL-GUIDE and the website install guide updated for every OS. (Closes [#10](https://github.com/PiBOH/vivi-music-de/issues/10))

## [6.0.6.3_DE-1.50.56-alpha] - 2026-09-11

### Fixed
- [DE] **The floating "Now Playing" widget no longer crashes the app on launch**: until the widget is dragged for the first time it uses an aligned position (the default `WindowPosition(Alignment.TopEnd)`), whose x/y are unspecified (NaN). The debounced position save ran `roundToInt()` on that NaN and threw `IllegalArgumentException: Cannot round NaN value` about half a second after the widget appeared — on every start, so the widget could never be dragged to save a real position. Unspecified positions are now ignored (the widget still remembers the position as soon as it is moved). (Closes [#59](https://github.com/PiBOH/vivi-music-de/issues/59))
- [DE] **Windows uninstall no longer aborts with 'Type Mismatch'**: the uninstall cleanup built the backup folder name with `GetDateTimeString('yyyymmdd_hhnnss', '', '')` — passing an empty string for the two `Char` parameters compiled fine but made the uninstaller stop with `Runtime error: Type Mismatch`, so the uninstall never completed and the user data was never backed up. The separators are now the documented `#0`, and the whole cleanup runs inside a `try/except` so no cleanup failure can abort the uninstall (the closing dialog then states that the data was left untouched). (Closes [#48](https://github.com/PiBOH/vivi-music-de/issues/48))

## [6.0.6.3_DE-1.50.55-alpha] - 2026-09-10

### Fixed
- [DE] **Persian (fa) and Hebrew (iw) tables completed (batch 68)**: those locale tags arrive on the desktop only via device-sync from the Android app and were the last two languages with keys falling back to English — 592 keys translated into Persian and Hebrew, so `scripts/check_localization.py` now reports **every language table contains every English key** (100% coverage, all 52 languages + English).

## [6.0.6.3_DE-1.50.54-alpha] - 2026-09-10

### Fixed
- [DE] **Player background "Visualizer" no longer shows in Arabic**: the desktop-only key `player_background_visualizer` was missing from the English table (the generator only shipped it via the non-English extra batch), so with the default/English language the fallback safety net picked the first dictionary that had the key — the Arabic one. The key is now mapped in the generator (`en` = "Visualizer"), so every language falls back to English instead.
- [DE] **Completed the missing desktop translations (batch 67)**: all 47 selectable languages now carry the `tooltip_*` strings (derived from the already-translated base keys), `create_room` / `join_room` (reusing the Android wording), the remaining `auto_load_more` / `auto_skip_next_on_error` / `skip_silence` / `forgotten_favorites` / `similar_to` / `recommended` subsets and the device-sync tooltips — `scripts/check_localization.py` reports 0 missing keys for every selectable language (plus the `in` / `nb-rNO` / `pt-rBR` locale aliases now copy their twin `id` / `nb` / `pt` tables, and `fa` / `iw` / `pt-rBR` Android resources are imported instead of skipped).

## [6.0.6.3_DE-1.50.53-alpha] - 2026-09-10

### Fixed
- [DE] **Seek bar stuck at ~19 s for some tracks (persisted)**: when a stream carried no duration, the player derived the length from the sample table — which only held the first ~256 KB scan window (~19 s) when the whole file was already on disk. Complete cached tracks were then misjudged as "truncated" (thrown away and re-downloaded on every play), while genuinely truncated cache files played their first ~19 s and "ended" — the seek bar of the player and mini player never moving past ~19 s. The player now scans the entire file when it is already on disk, so the duration is correct immediately, the truncation guard only fires for really truncated files, and cached tracks are reused instead of re-downloaded.

### Changed
- [DE] De-duplicated `formatBytes`/`formatSpeed` (three `formatBytes` + two `formatSpeed` copies with slightly different behaviour in `Main.kt`, `DevTools.kt` and `LogExporter.kt`) into shared helpers, and centralized the 9 copy-pasted `Json { ignoreUnknownKeys = true }` codecs into shared `sharedJson*` values — no behaviour change.

## [6.0.6.3_DE-1.50.52-alpha] - 2026-09-10

### Fixed
- [DE] **[CRITICAL] Tracks no longer appear to last ~19 seconds**: when a stream carried no duration (NewPipe fast path, cached files, Listen Together guest tracks), the player derived the length from the sample table — which only holds the first ~256 KB scan window (~19 s) when playback starts — and froze it there, so the seek bar showed every track as ~19 s, the position clamped at that value and (with crossfade on) the next track started after ~19 s. The duration now comes from the track metadata when the stream has none (`fallbackDurationMs`), and the AAC-derived fallback GROWS as fragments are scanned and is re-reported, so the seek range follows the real track length.
- [DE] Listen Together: leaving a room now forces a clean socket reconnect, so the next Create/Join is no longer silently dropped on the old (possibly server-closed) connection — the "after leaving a room I can't leave or re-enter one" lock-up with a stuck spinner is fixed.

### Changed
- [DE] Listen Together lobby now has a single morphing action button like the mobile app: **Create room** when no code is typed, **Join room** when the 8-character code is complete.

## [6.0.6.3_DE-1.50.51-alpha] - 2026-09-09

### Fixed
- [DE] **Browse pages no longer fail with 401 `BROWSE_UNAUTHENTICATED` (E1031)**: `POST music.youtube.com/youtubei/v1/browse → 401 Request is missing required authentication credential` — e.g. opening *New release albums* (`FEmusic_new_releases_albums`) — came from two causes that were both fixed. (a) When the session cookie missed an APISID token (`SAPISID` / `__Secure-3PAPISID`), the client sent a partial auth (cookie without `SAPISIDHASH`) which is rejected more strictly than an anonymous request — it now falls back to anonymous for that call, so public catalog pages always load. (b) When a real authed session expired mid-use, browse retried the same authed request once and still hit 401 — it now retries once **anonymously**, so public pages stay available even with an expired session. E1031 remains in `ERRORS.md` for truly private browse calls.

## [6.0.6.3_DE-1.50.50-alpha] - 2026-09-09

### Fixed
- [DE] **Browse failures are no longer invisible in the logs and the app**: `POST music.youtube.com/youtubei/v1/browse → 401 UNAUTHENTICATED (E1031)` failures — e.g. opening *New release albums* — now land in `~/.vivimusic/logs/<ts>/browse.log` (with `browseId`/`params`/`loggedIn`/`cookie`/`visitorData`/`dataSyncId` context and a prefixed `E1031 …`) and surface their code-text in the browse error card (instead of a blank JSON), so the support zip is never empty for this error.
- [DE] **Every log file now exists from the first launch**: each session `logs/<ts>/` now creates all 9 category logs (`actions`, `browse`, `cache`, `lyrics`, `nav`, `playback`, `queue`, `settings`, `volume`) — even when empty — so the support zip is always complete and a session's `browse.log` is never missing.

## [6.0.6.3_DE-1.50.49-alpha] - 2026-09-09

### Fixed
- [DE] **Browsing no longer crashes with duplicate keys**: `BrowseScreen` (`LazyVerticalGrid` for mood/genre or New-releases pages) keyed every card by `ytItem.id` alone — when YouTube returns the same album/playlist/song in more than one section the duplicate `RDCLAK…` / playlist id appeared twice and Compose crashed with `Key "…" was already used`. Keys are now scoped by section + position (`browse-$section-$position-$id`), so duplicates no longer collide.
- [DOCS] **New error code E1031 — `BROWSE_UNAUTHENTICATED`**: a `POST music.youtube.com/youtubei/v1/browse → 401 UNAUTHENTICATED (Request is missing required authentication credential)` — e.g. opening *New release albums* — now has its own documented code in `ERRORS.md` with the cause (expired / missing `__Secure-3PAPISID` / `VISITOR_DATA` / `SAPISIDHASH`) and the fix (sign in again from **Settings → Account**).

## [6.0.6.3_DE-1.50.48-alpha] - 2026-09-08

### Fixed
- [DE] **Full players and mini-player backgrounds now follow the Light/Dark/System theme**: the canvas, blur and Apple Music backgrounds hardcoded dark scrims over the artwork (and over the plain surface when no artwork was loaded), so in light mode the player surface stayed dark while the rest of the UI went light. Scrims are now theme-aware (stronger in dark, lighter in light, skipped entirely without artwork), and the autoplay indicator and artist-card borders use theme tokens instead of hardcoded white. (Closes #23)

## [6.0.6.3_DE-1.50.47-alpha] - 2026-09-08

### Fixed
- [DE] **The classic mini-player seek bar now works without opening the full player first**: scrubbing a track that was already loaded (restored from the persistent queue, prefetched at startup, or cached) used to be a silent no-op, because the seek path only started the stream when `loadedVideoId` differed from the current track — but a loaded track always has a matching `loadedVideoId`. The scrub now always starts (or restarts) the stream at the chosen point when nothing is resolving. (Closes #22)

## [6.0.6.3_DE-1.50.46-alpha] - 2026-09-08

### Changed
- [DE] **Remaining tooltips now use action verbs**: "Menu" → "Open menu", "Back" → "Go back", "Forward" → "Go forward", "Home" → "Go to home", "Settings" → "Open settings", "Queue" → "Open queue", "Lyrics" → "Show lyrics", "History" → "Open history", "Notifications" → "Open notifications", "More" → "Show more options", "Next" → "Skip to next", "Previous" → "Skip to previous", "Output device" → "Select output device", "Queue options" → "Show queue options", "Autoplay" → "Toggle autoplay", "Favorite" → "Add to favorites", "Connection method" → "Select connection method", "Wrapped" → "Open VIVI Wrapped", "Listen Together" → "Start Listen Together". New `tooltip_*` keys were added to the localization generator so every language falls back to the action wording.

## [6.0.6.3_DE-1.50.45-alpha] - 2026-09-08

### Changed
- [DE] **The "Open live log" entry moved from Developer options to Settings → System and is now always available**: it no longer requires enabling Developer options, and the live-log window opens directly from the System screen. (Closes #56)
- [DE] **Tooltips now use action verbs and reflect the current state**: the right-panel button says "Show/Hide right panel", the sidebar toggle "Expand/Collapse sidebar", the window button "Maximize/Restore" and the mini player "Play/Pause" — all localized instead of static English labels. (Closes #57)

### Fixed
- [DE] **The crash log is confirmed included in the exported log archive**: `~/.vivimusic/crash.log` and the timestamped `logs/<ts>/crash_<ts>.log` were already packaged by the log exporter (it collects every `*.log` recursively); no change was needed.

### Commits
- v: DE 1.50.45-alpha — move live log to System (always available); action-verb stateful tooltips

## [6.0.6.3_DE-1.50.44-alpha] - 2026-09-08

### Fixed
- [DE] **Clicking any button on Home no longer crashes with an NPE**: the Home `LazyColumn` content dereferenced the reloadable `home`… [#54](https://github.com/PiBOH/vivi-music-de/issues/54)

### Added
- [DE] **Crash dumps are now written to disk on every uncaught error**: `~/.vivimusic/crash.log` is always overwritten with the most recent… [#55](https://github.com/PiBOH/vivi-music-de/issues/55)
- [DE] **Every click is now recorded in the session `actions.log`**: cards, song rows, section headers, mood & genres chips, back buttons… [#55](https://github.com/PiBOH/vivi-music-de/issues/55)

## [6.0.6.3_DE-1.50.43-alpha] - 2026-09-08

### Fixed
- [DE] **Crossfade sub-options are now hidden when crossfade is off**: the "Disable for gapless albums" toggle and the crossfade duration slider no longer show when the master crossfade toggle is off. (Closes #52)
- [DE] **Browsing "Pinned for later" and similar playlists no longer crashes**: `MusicResponsiveHeaderRenderer.buttons` is now nullable with a default empty list, so YouTube responses that omit the field are handled gracefully instead of throwing a serialization error. (Closes #53)

## [6.0.6.3_DE-1.50.42-alpha] - 2026-09-08

### Fixed
- [DE] **Windows uninstall no longer crashes with 'Type Mismatch'**: the uninstaller tried to create a detail log box using `TNewMemo` on… [#48](https://github.com/PiBOH/vivi-music-de/issues/48)
- [DE] **Metadata now show 'PiBOH' as publisher and 'VIVI Music' as product name**: the Windows Control Panel showed 'Vivi Music' as author and included the version string in the display name; the Linux .deb was placed in the 'Other' category instead of 'Audio'. [#49](https://github.com/PiBOH/vivi-music-de/issues/49)
- [DE] **In-app changelog now shows all released versions**: versions 1.50.36 through 1.50.38 had their release notes nested inside the 1.50.39 entry instead of having their own `## [version]` headings, so the parser skipped them — each now has a proper heading. [#50](https://github.com/PiBOH/vivi-music-de/issues/50)
- [Website] **Screenshot gallery loads reliably**: a duplicate `</script>` tag broke the inline script that initializes the gallery, so the 'Loading screenshots…' placeholder was never replaced. [#51](https://github.com/PiBOH/vivi-music-de/issues/51)

### Commits
- v: DE 1.50.42-alpha — fix uninstall crash, metadata author/category, changelog headings, website gallery

## [6.0.6.3_DE-1.50.41-alpha] - 2026-09-08

### Fixed
- [DE] **Stale cached lyrics can no longer hide the resolver fixes**: the lyrics cache used only the video id as its key, so a plain (non-timed) or wrong-version result cached earlier. [#47](https://github.com/PiBOH/vivi-music-de/issues/47)
- [DE] **Results fetched without a known duration are no longer cached**: a duration-less lookup (duration −1) is the most likely to match the wrong recording (radio edit vs original, live vs studio); those results are shown but not persisted, so the next time the track duration is known the search re-runs with a precise match.
- [DE] **The album is now passed to every lyrics provider** (LrcLib, BetterLyrics, YouLyPlus, KuGou, Musixmatch, Paxsenix, Unison), matching the mobile app — album-aware providers use it to pick the right recording instead of relying on title/artist alone.

### Commits
- v: DE 1.50.41-alpha — lyrics cache keyed by sync mode; never cache duration-less matches; pass album to providers

## [6.0.6.3_DE-1.50.40-alpha] - 2026-09-08

### Fixed
- [DE] **The two "keep the queue going" settings are merged into one**: "Auto load more songs" and "Enable similar content" both gated the exact same queue-extension code (fetching YouTube up-next/related tracks), so toggling one off silently disabled the other — they were duplicates. [#44](https://github.com/PiBOH/vivi-music-de/issues/44)
- [DE] **"Instantly skip silence" only appears when "Skip silence" is on**: the instant variant is a derivative of the master toggle (it… [#45](https://github.com/PiBOH/vivi-music-de/issues/45)
- [DE] **The slider style setting now applies to every player**: previously it only affected the classic full-screen player, while the… [#46](https://github.com/PiBOH/vivi-music-de/issues/46)

### Commits
- v: DE 1.50.40-alpha — merge duplicate auto-load-more settings; gate instant skip silence; apply slider style to all players

## [6.0.6.3_DE-1.50.39-alpha] - 2026-09-07

### Translations
- [DE] **Full translation sweep across all 47 supported languages**: every desktop key now has a non-English translation — no key falls back to raw English anymore.

### Commits
- v: DE 1.50.39-alpha — full 47-language translation sweep (Player & audio port, device sync, login, live log)

## [6.0.6.3_DE-1.50.38-alpha] - 2026-09-07

### Fixed
- [DE] **The similar/up-next queue is no longer wiped when a track's first attempt fails**: starting a single song builds the queue with ~15 up-next/automix tracks, but a playback error. [#43](https://github.com/PiBOH/vivi-music-de/issues/43)
- [DE] **The "cannot find the file specified" playback error right after "stream ready" is fixed**: the decoder opened the shared `.part` download file the instant the download thread had created its handle but not yet created the file on disk (a race that produced `FileNotFoundException` and forced the retry above). The decoder now waits until the file actually exists before opening the channel.
- [DE] **Logs are now organized per session**: every launch writes `~/.vivimusic/logs/<yyyyMMdd-HHmmss>/` with one file per category (`playback.log`, `queue.log`, `lyrics.log`, `nav.log`, `settings.log`, …).

### Commits
- v: DE 1.50.38-alpha — retries keep the grown similar queue; decoder waits for the .part file; per-session categorized logs

## [6.0.6.3_DE-1.50.37-alpha] - 2026-09-07

### Added
- [DE] **Crossfade** (port of the mobile option, Settings → Player & audio): tracks now overlap with a short fade at the end of each song instead of hard-cutting. [#38](https://github.com/PiBOH/vivi-music-de/issues/38) — last item of the Player & audio port)

### Commits
- v: DE 1.50.37-alpha — crossfade with duration slider and same-album gapless exemption completes the #38 port

## [6.0.6.3_DE-1.50.36-alpha] - 2026-09-07

### Changed
- [DE] **Lyrics prefer the synced (timed) version when the "Synced lyrics" option is on**: instead of returning the first provider that… [#42](https://github.com/PiBOH/vivi-music-de/issues/42)

### Commits
- v: DE 1.50.36-alpha — synced lyrics preferred across providers when the Synced lyrics option is on

## [6.0.6.3_DE-1.50.35-alpha] - 2026-09-07

### Fixed
- [DE] **[Critical] Tapping a song on Home / Library now creates a real queue again**: a single track no longer sits alone in the queue and stops after one song. [#40](https://github.com/PiBOH/vivi-music-de/issues/40)
- [DE] **[Critical] Lyrics are no longer missing, wrong-version or permanently wrong**: the desktop edition asked a single community server (LrcLib) **without the real track duration** and cached the result forever (even the look-ahead pre-fetch poisoned the cache). [#41](https://github.com/PiBOH/vivi-music-de/issues/41)

### Commits
- v: DE 1.50.35-alpha — critical fixes: single-song taps build a real up-next queue (automix first); multi-provider, duration-aware lyrics with official YouTube fallback

## [6.0.6.3_DE-1.50.34-alpha] - 2026-09-07

### Added
- [DE] **"Skip silence" and "Instantly skip silence" options in Player & audio** (port from the mobile app, issue… [#38](https://github.com/PiBOH/vivi-music-de/issues/38)): silent runs of a track are dropped from the output while it plays (the normal option skips runs longer than ~150 ms so breaths/quiet attacks stay intact; the instant option cuts the leading silence at the start/after a seek right away and jumps mid-track silences as soon as they are detected). Implemented as a pure add-on on the decoded-PCM output path inside `AudioPlayer`: with both options off the audio path stays byte-identical. Both default off; labels/descriptions English-only for now; changes apply from the next played track.

## [6.0.6.3_DE-1.50.33-alpha] - 2026-09-07

### Added
- [DE] **More "Player & audio" options ported from the mobile app** (issue [#38](https://github.com/PiBOH/vivi-music-de/issues/38)):
  - **Persistent shuffle** (default off): a freshly started queue (new song / playlist / album) now resets shuffle unless the option is enabled — matching the mobile per-queue shuffle behavior (previously shuffle always carried over).
- **Progressive seek** (default off): double-clicking the left/right half of the artwork in the full player skips ∓5 seconds; with the option on, each rapid repeat (<1 s) adds 5 extra seconds incrementally (5 → 10 → 15…), exactly like the mobile double-tap seek.
  - **History duration** (default 30 s, slider 1–100 s): a track is only recorded into the listen history — the seeds behind the Home "Recommended" row — after it has actually played for this long, so quick skips no longer pollute the recommendations.
  - **Auto download on like** (default off): liking a song now downloads it straight into the audio cache in the background (same join-safe path as the look-ahead prefetch), so it plays instantly later; cached files still follow the user's audio-cache retention setting.
- Both "Resume on Bluetooth connect" and "Shuffle playlist/album first" have no real desktop equivalent (the desktop has no app-level Bluetooth audio routing, and similar content is only ever appended after the original queue is exhausted — the "original first, then similar" behavior already always holds), so they are not ported and are documented as not-applicable in issue #38.
- All new labels/descriptions are English-only for now.

## [6.0.6.3_DE-1.50.32-alpha] - 2026-09-07

### Added
- [DE] **New "Player & audio" options** (port from the mobile app, issue [#38](https://github.com/PiBOH/vivi-music-de/issues/38)):
  - **Prevent duplicate tracks in queue**: adding a track that is already queued removes its old copy first, so every track appears once. Applies to "Add to queue", "Add all to queue" and "Play next".
  - **Auto skip to next song when error occurs**: after all retries for a failing track are exhausted, playback continues with the next queued track (wrapping only when repeat-all is on) instead of stopping on the error.
- **Pause music when media is muted**: when the OS output volume is muted or at zero while VIVI is playing, playback pauses and resumes when the volume comes back (reacts only to transitions, so pressing play manually while muted still works).
  - **Keep screen on when player is expanded**: holds a keep-awake request while the full player screen is open (Windows `SetThreadExecutionState` / macOS `caffeinate`). Keep-awake is now multi-source — the pairing request and the expanded-player request are independent and no longer cancel each other.
- All four options are **off by default** (as on mobile) and the labels/descriptions are English-only for now.

## [6.0.6.3_DE-1.50.31-alpha] - 2026-09-07

### Added
- [DE] **"Auto load more songs" and "Enable similar content" options in Player & audio** (port from the mobile app, issue… [#38](https://github.com/PiBOH/vivi-music-de/issues/38)): when the queue reaches its end and autoplay is on, VIVI now fetches related/radio tracks for the last song (same innertube path as the Home "Recommended" row — `YouTube.next` + `YouTube.related`), appends the new tracks and keeps the music going instead of stopping. A single song played alone therefore continues into a radio-like stream of similar songs, and duplicates already in the queue are never re-added. Both options are enabled by default and can be turned off separately in Settings → Player & audio; the labels/descriptions are English-only for now.

## [6.0.6.3_DE-1.50.30-alpha] - 2026-09-07

### Fixed
- [APK] **Prerelease updates on the fork now come from GitHub Releases, not nightly runs**: the nightly-workflow mechanism only exists… [#39](https://github.com/PiBOH/vivi-music-de/issues/39)
- [APK] **The About screen shows the real release channel**: a new `BuildConfig.RELEASE_CHANNEL` (fed from the mobile channel in `version.txt`) makes companion builds read **ALPHA** instead of the stale NIGHTLY label.

## [6.0.6.2_DE-1.50.29-alpha] - 2026-09-06

### Changed
- [APK] **The Android release workflow lets you choose the signing key**: on manual runs (`workflow_dispatch`) a new `signing_key` input selects `auto` (default — `RELEASE_KEYSTORE` when set, otherwise `DEBUG_KEYSTORE`), `release` or `debug`; pushes still resolve automatically.

## [6.0.6.2_DE-1.50.28-alpha] - 2026-09-06

### Added
- [DE] **Do Not Disturb is detected before sending native notifications**: when native notifications are enabled but the OS is suppressing… [#36](https://github.com/PiBOH/vivi-music-de/issues/36)
- [APK] **A dedicated release keystore now exists** (`app/keystore/release.keystore`, generated locally and gitignored, with a copy in `.ignore/`): the release signing on CI consumes it through the `RELEASE_KEYSTORE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD` secrets, so release builds no longer depend on the debug key. Instructions are in `.ignore/KEYSTORE-INFO.txt`.

### Changed
- [APK] **Android versioning is back on the `6.0.6.x` line** (`6.0.6.2`, versionCode 132): the previous one-off bumps (`6.4.45` / `6.4.46` / `6.4.46.1`) were temporary; per the documented scheme only the last digit increments on each APK update, and the versionCode stays monotonic. [#35](https://github.com/PiBOH/vivi-music-de/issues/35)

### Translations
- [DE] Added the `notif_dnd_title` / `notif_dnd_body` keys in all 52 supported languages (thanks to @codebuffai).

## [6.4.46.1_DE-1.50.27-alpha] - 2026-09-06

### Fixed
- [DE] **Website footer cleaned up**: the "verified Open Source" badge is gone, and the GitHub/Telegram round buttons no longer show raw `code`/`send` text overflowing the containers. [#31](https://github.com/PiBOH/vivi-music-de/issues/31)
- [DE] **Website animations now run on desktop too**: the `prefers-reduced-motion` kill switch was removed from the site CSS and JS, so the entrance, floating and scroll-reveal animations always play. [#33](https://github.com/PiBOH/vivi-music-de/issues/33)
- [DE] **Website changelog is lazy now**: the page renders only the latest release and reveals older ones in batches through a "Load more…" button; search and the version picker still scan everything loaded. (Closes [#32](https://github.com/PiBOH/vivi-music-de/issues/32))

### Changed
- [APK] **The Android companion app is now "VIVI for DE"** (was "VIVI" / "VIVI Debug") and installs as `com.vivi.music.desktop` instead of the old debug package, so it can sit next to the upstream app without conflicts.
- [APK] **`build-android.yml` rewritten**: it now triggers on the same release signal as the desktop autorelease (push whose commit message… [#34](https://github.com/PiBOH/vivi-music-de/issues/34)

## [6.4.46_DE-1.50.26-alpha] - 2026-09-06

### Added
- [DE] **The settings search now covers every sub-screen**: each top-level Settings row is indexed with the localized labels of the options… [#29](https://github.com/PiBOH/vivi-music-de/issues/29)

### Translations
- `home_empty` (the empty Home state message) is now fully translated in all 47 supported languages instead of falling back to English. Thanks to @codebuffai for the translation pass.


## [6.4.46_DE-1.50.25-alpha] - 2026-09-06

### Added
- [DE] **The activity log now records every settings change**: whenever an option is modified, the log line names the field and shows `old → new` (large lists are summarized; secrets such as cookies, visitor data and API keys are redacted). [#30](https://github.com/PiBOH/vivi-music-de/issues/30)

### Fixed
- [DE] **The Home "Recommended" row no longer disappears on fresh profiles**: when there is no in-session listening history yet, it seeds from the first songs of the loaded Home feed, so the row shows up even right after a clean install. (Closes [#27](https://github.com/PiBOH/vivi-music-de/issues/27))
- [DE] **The Home empty state shows real text instead of a raw `home_empty` key**: the string was missing from every language table; it is now in the English base table and the other languages pick it up with the next dedicated translation pass.


## [6.4.46_DE-1.50.24-alpha] - 2026-09-05

### Added
- [DE] **Detailed activity logging**: every playback command (play/pause/seek/next/previous/queue/skip/volume/shuffle/repeat), navigation change and playback error is now recorded with a timestamp in a new in-app log. [#24](https://github.com/PiBOH/vivi-music-de/issues/24)
- [DE] **Live log viewer window** (Developer options → "Open live log"): a dedicated window shows the activity log in real time (auto-scrolled, selectable text to copy lines, Clear button). (Closes [#24](https://github.com/PiBOH/vivi-music-de/issues/24))

### Fixed
- [DE] **Stream cache minimum is now 10 minutes** instead of 1 minute: the slider in Player settings starts at 10 and any legacy saved value below the floor is clamped to 10 minutes. (Closes [#25](https://github.com/PiBOH/vivi-music-de/issues/25))

### Changed
- [APK] **One-off version bump to 6.4.46 (versionCode 130)** so users still on 6.4.45/128 receive this update; the next release returns to the 6.0.6.x line with a higher code. (Closes [#26](https://github.com/PiBOH/vivi-music-de/issues/26))
- [APK] **Update check is now chronology-aware**: "latest release" is chosen by published date (GitHub returns releases newest-first) instead of comparing version strings, which stalled updates whenever the versioning scheme changed. [#26](https://github.com/PiBOH/vivi-music-de/issues/26)

## [6.0.6.1_DE-1.50.23-alpha] - 2026-09-05

### Changed
- [DE][APK] **Playback resolution ported from upstream vivizzz007/main (6.0.6)** to finally settle the song-resolution issues on both… Closes #17)
- [APK] **Rebased on upstream 6.0.6**: StreamUrlCache, ContentAwareFallbackStrategy, CipherDeobfuscator (WEB_REMIX streaming), SponsorBlock, NetworkConfig, the new lyrics providers and all upstream fixes come in via the merge; our device-sync (pairing) glue is the only feature kept on top. Closes #19)
- [DE] Upstream fallback order also restored `Android VR 1.65.10` and the Chrome UA set that upstream 6.0.6 ships.

### Added
- [DE] **Home "Recommended" section** (port of the mobile Daily-Discover mechanism): the three most recent tracks you played seed `YouTube.next` → `YouTube.related`, and the related songs appear as a horizontally scrollable section; tapping one plays the whole recommendation list as a queue. Closes #18)

### Fixed
- [APK] Device-sync now persists/restores the `EnableUnisonKey` lyrics preference (renamed upstream from SimpMusic), so lyrics-provider sync keeps working after the rebase.
- [APK] **Updater defaults now match the fork distribution**: the prerelease ("beta updates") switch is ON by default (releases ship as pre-releases), and the update source defaults to `PiBOH/vivi-music-de` instead of the upstream repo — both remain switchable in Settings → Updates. (Closes #20)
- [APK] **Fixed a broken CI workflow**: the nightly-flag line added in 1.50.22 for the About-screen badge contained a literal CR/LF inside a shell string, which made the whole `build.yml` workflow file invalid (the Android build job never ran and the APK asset was missing from the release). The line is now valid shell.
- [DE] The release tag now correctly reads `6.0.6.1_DE-1.50.23-alpha`: the Android part of the combined version follows the APK version (`6.0.6.1`), documented in `AGENTS.md` — the previous tag (`6.4.45_DE-...`) was built from a stale `version.txt` and its release has been replaced.


## [6.4.45_DE-1.50.22-alpha] - 2026-09-04

### Changed
- [DE] Desktop releases now publish on the **alpha** channel instead of nightly (release channel in `version.txt`).

### Fixed
- [DE] **Players, mini players and the sidebar now follow the app theme in light mode**: they decided dark/light from the OS theme (`isSystemInDarkTheme()`) instead of the app's own mode, so with a dark OS + light app they stayed dark (and could get light text on light surfaces). Closes #14)
- [DE] **Custom colors now appear in the palette live**: the Theme screen only persisted new/removed custom accents to disk without updating the list it displays, so swatches appeared only after re-entering the screen. Closes #15)
- [APK] **Debug APK About screen now shows NIGHTLY for non-stable builds**: the CI built the APK without `-Pnightly=true`, so `BuildConfig.IS_NIGHTLY` was false and the About badge read "STABLE". The flag is now derived from the mobile channel in `version.txt` (line 3). (Closes #16)


## [6.4.45_DE-1.50.21-nightly] - 2026-09-04

### Fixed
- [DE] **Tracks stuck in a resolve loop after clearing the cache**: the in-app "Clear cache" buttons (Settings → Storage / Privacy) delete every subfolder of `~/.vivimusic/cache`, including `audio/`, which `AudioPlayer` only created once at startup. Closes #13)

## [6.4.45_DE-1.50.20-nightly] - 2026-09-04

### Fixed
- [DE] **Scrubbing the seek bar now starts the stream of an unresolved track** (issue #11): dragging the seek bar on a track restored from… Closes #11.
- [DE] **The YouTube-style buffered bar is now always visible** (issue #12): the fainter secondary segment behind the played portion previously vanished as soon as the download finished or the track was already cached, so it was practically never seen. Closes #12.

## [6.4.45_DE-1.50.19-nightly] - 2026-09-04

### Fixed
- [DE] **Windows installer no longer fails to compile** (issue #9): the details/log box is now created at runtime with `TNewMemo` — Inno Setup 6 removed the built-in `DetailsMemo`/`DetailsButton` that the previous version referenced.

### Added
- [DE] **Uninstall now keeps exactly one final backup and wipes every cache** (issue #10): on Windows the Inno Setup uninstaller copies `device-sync.json`, `playlists.json` and imported fonts into `~/.vivimusic/backups/uninstall-<timestamp>/` right before finishing, then deletes everything else in `~/.vivimusic` (downloaded updates, audio/video/canvas/lyrics caches, logs, helper libraries, artwork) — only that last backup remains. Linux mirrors it: the `.deb` ships a `postrm` hook (plus the shared `scripts/uninstall-cleanup.sh` embedded in `/opt` and copied to `/usr/share`), the AUR PKGBUILD gets a `post_remove` hook via `vivi-music-de.install`, and macOS/AppImage users can run the same script manually. INSTALL-GUIDE updated for every OS.

## [6.4.45_DE-1.50.18-nightly] - 2026-09-04

### Changed
- [DE] The Inno Setup installer now always shows the installation details box (the extraction log) below the progress bar on the Installing page, instead of hiding it behind the "Show details" toggle.


## [6.4.45_DE-1.50.17-nightly] - 2026-09-04

### Fixed
- [DE] **Radio/Charts discovery screen no longer stays empty**: the chart parser now converts every item type YouTube returns (songs, video-chart playlists, top artists, albums) instead of dropping everything that is not a song — the "Nothing to show here yet" screen with the endless refresh loop is gone. Closes #5.
- [DE] **Native macOS notifications actually delivered**: the bundled native helper now posts through `UNUserNotificationCenter`, so notifications land in the macOS Notification Center and are attributed to the app (the old `osascript` path was unreliable/blocked on modern macOS). Closes #6.
- [DE] **Home screen can no longer be silently blank**: an empty response auto-retries once and, if it still comes back empty, shows a clear "Nothing to show here yet" state with a Retry button instead of an endless spinner with no content. Closes #7.
- [DE] **Changelog issue references are now clickable**: every `#N` mention (e.g. "Closes #2") across the whole changelog — past and future entries — renders as a link that opens the GitHub issue. Closes #8.


## [6.4.45_DE-1.50.16-nightly] - 2026-09-04

### Added
- [DE] **Native system playback controls and "Now Playing" on macOS**: the app now registers with the system media session (Control Center… Closes #4.


## [6.4.45_DE-1.50.15-nightly] - 2026-09-04

### Fixed
- [DE] **Audio micro-pauses/skips on macOS (random brief glitches on almost every track, coinciding with small UI hitches) reduced**: the… Closes #3.

## [6.4.45_DE-1.50.14-nightly] - 2026-09-04

### Added
- [DE] Sidebar "Playlists" entry now opens the full playlist list screen (all local playlists with create/rename/delete); the chevron arrow is the only control that expands/collapses the inline playlist list in the sidebar. Closes #2.

## [6.4.45_DE-1.50.13-nightly] - 2026-09-04

### Fixed
- [DE] **Mini player seek bar now works on tracks restored from the persistent queue without opening the full player**: restored tracks used to lose their duration, so the seek bar fell back to a 0..1 range and the thumb snapped back to the start after a scrub, making the seek look dead.

## [6.4.45_DE-1.50.12-nightly] - 2026-09-03

### Added
- [DE] **Dedicated Contributors sub-screen**: the About screen now shows a single "Contributors" row right under the lead developer card; tapping it opens a dedicated screen with the full list, so the About page no longer gets crowded as the list grows.
- [DE] **Seek bars usable before the duration is known**: a track loaded from the queue but never played can be scrubbed even while its length is still unknown (no more disabled slider); the chosen point is remembered as a fraction and playback starts from it the moment the duration becomes available (metadata or resolved stream).

### Changed
- [DE] `contributorsde.json` is now always read from the repository (`vivi-music-de` branch) with a silent refresh every time the Contributors screen opens — no local `~/.vivimusic/contributorsde.json` copy is created or read anymore (a stale one from older builds is deleted by the app); the bundled copy is only the offline fallback.

## [6.4.45_DE-1.50.11-nightly] - 2026-09-03

### Added
- [DE] **YouTube-style buffered progress on the seek bars of every player**: while a track is still streaming/downloading, a fainter secondary segment shows how much of the audio is already available (full player — M3 Expressive and Classic designs, the Classic mini player slider, the Apple mini player bottom bar and the New mini player progress ring). Fully cached or finished downloads show no extra segment, and the buffer keeps advancing while the track is paused.
- [DE] **Seek bars can now be scrubbed before playback starts**: dragging the slider on a loaded-but-not-yet-started track (restored persistent queue, or a track that already ended) remembers the chosen position, and pressing play starts from there instead of ignoring the drag.

### Changed
- [DE] The About -> Contributors list is now fetched live from GitHub (`contributorsde.json` on the `vivi-music-de` branch) every time the screen opens, so new contributors appear without an app update; the local copy (`~/.vivimusic/contributorsde.json`) is kept only as an offline cache/fallback and is refreshed silently.

## [6.4.45_DE-1.50.10-nightly] - 2026-09-03

### Changed
- [DE] The contributor list is now read from `~/.vivimusic/contributorsde.json` (the bundled default is copied there on the first launch), so contributors can be added, edited and reordered without rebuilding the app — the About screen picks the changes up at the next launch.

## [6.4.45_DE-1.50.9-nightly] - 2026-09-03

### Added
- [DE] About screen: new CONTRIBUTORS section below the developer card, listing the people behind the project (bogdan-developer, Ansu216, vivizzz007, dumbshrn) with their role, a link to their GitHub profile and their avatar fetched automatically from GitHub.

## [6.4.45_DE-1.50.8-nightly] - 2026-09-03

### Fixed
- [DE] **Classic mini player seeking**: the seek slider now seeks once when the drag ends, like the full player. Previously every drag tick restarted the whole decode thread, so the slider fought the live position reports and felt dead (could not scrub forward/backward reliably).
- [DE] **Instant start on the restored queue**: the startup prefetch now downloads the current track first.

## [6.4.45_DE-1.50.7-nightly] - 2026-09-03

### Fixed
- [DE+APK] **Bidirectional language sync that can never be hijacked at pair time**: every manual language change now travels with a (deviceId, per-device sequence) marker.
- [DE] **Radio/Charts screen can never be silently blank**: the charts parser now walks every section-list the response can use (any single-column tab, a top-level section list, the two-column tabs) instead of only the first tab, auto-issues the continuation once when the shell is empty, and the UI shows a localized empty state with a Retry button (plus one automatic retry) instead of an empty page.
- [DE] **Single density selector**: the duplicate "Density & grid" info row inside the Density sub-screen was folded into the one dropdown (its description), so the screen offers exactly one density option plus the separate grid-item-size picker.
- [DE] **Every error is selectable**: the shared error box, the login error/status lines, the update-failed / open-error texts and the Listen Together error lines are wrapped in `SelectionContainer`, so any in-app error can be copied for a report.
- [DE] **Sign-in now strips the `||…` suffix from `DATASYNC_ID`**: the delegated account id YouTube embeds in `ytcfg` can carry a trailing pipe-suffixed token; sent verbatim as `onBehalfOfUser` it made validation fail (401/500).
- [DE] Player-design dropdowns now highlight the selected option (the check compares the translated labels, not a raw key against the label) and `Localization.get` gained a last-resort fallback that can never surface a raw snake-case key on screen.
- Translations: 2 new keys (`retry`, `charts_empty`) for the Charts empty state, translated across all 47 languages (translations assisted by AI — thanks to @codebuffai).

## [6.4.44_DE-1.50.6-nightly] - 2026-09-03

### Fixed
- [DE] **Settings lists are ~15% more compact** across the main Settings screen and every sub-screen: entry rows use tighter padding (20/16 dp → 17/14 dp), the tinted icon tiles shrank from 40 dp to 34 dp with 24 dp → 20 dp glyphs inside, the gap between rows went from 4 dp to 3 dp, the chevron is 20 dp instead of 24 dp, and the sub-screen content margin went from 16 dp to 14 dp — the Material 3 settings rows now match the density of the rest of the interface.

## [6.4.44_DE-1.50.5-nightly] - 2026-09-03

### Fixed
- [DE] **Seeking now lands precisely on the clicked position instead of re-scanning the whole seek bar**: the player used to seek by decoding (and discarding) every frame from the start of the track until the target, which made the slider visibly reload from zero and — for tracks still downloading — could only land once the download had reached the target. Seeking now jumps straight to the AAC frame containing the requested time (AAC-LC frames are independent and ~constant-size), so it is instant on cached tracks and accurate to within one frame (~23 ms).
- [DE] **A fully cached track is played straight from disk without being "resolved" again**: play always resolved a fresh stream URL before checking the cache, so every restart (and every return to a song) showed the resolving phase even with "cache forever" and a complete file on disk.
- [DE] **Look-ahead prefetch now covers the whole queue**: previously only the 3 upcoming tracks were prefetched.
- [DE] **The in-app (VIVI) volume no longer resets to 100% on every launch**: the volume slider was never persisted. It is now saved (including when changed by device sync) and restored at startup.

## [6.4.44_DE-1.50.4-nightly] - 2026-09-03

### Fixed
- [DE] **"Mood & genres" no longer crashes when it scrolls into view on the Home page** (or on the dedicated screen): the API can return the same mood/genre (identical `browseId` + title) more than once across category sections, and the lists were keyed by `browseId + title` — two identical keys in a LazyColumn/Row throw "key … was already used". Both lists now deduplicate before rendering.
- [DE] **macOS: the Accessibility permission prompt no longer reappears on every launch** (reported on 1.50.1–1.50.3): the global media-key hook (JNativeHook) was registered unconditionally at startup, and macOS re-asks for the permission every time the hook is attempted while untrusted.
- [DE] **Expressive player: clearing the queue no longer strands the user on the full-player screen**: a clear stops playback and empties the queue, and that "Nothing playing" state had no collapse control — the only way out was relaunching.
- [DE] **Device sync (relay): the pairing code is now generated automatically when connecting** — the Connect button moved below the relay-server field and merged with code generation into one "Connect & Generate Pair Code" action; once connected the same spot becomes "Regenerate Pair Code" (with a separate Disconnect button), and a code is also issued automatically whenever the relay is connected, unpaired and no code is pending, so the pairing QR is always ready.
- [DE] **Audio micro-pauses/skips on macOS** (reported alongside small UI hitches): the audio decode thread now runs at maximum priority so UI/GC work cannot starve the sound-buffer refill, and the audio-reactive visualizer level is decimated to every other decoded frame (~20 Hz) to halve the UI recomposition load competing with the audio scheduler. Best-effort mitigation pending confirmation from the affected Mac.

## [6.4.44_DE-1.50.3-nightly] - 2026-09-03

### Fixed
- [APK] **Startup freeze after in-place updates** (previously only recoverable by uninstalling and reinstalling the app): the first image load ran an unbounded `runBlocking` on the main thread waiting for DataStore, the preference cache stopped retrying after a single DataStore error (so every synchronous settings read fell into the slow fallback path and stalled startup), and the persisted queue/automix/player-state files were deserialized on the main thread. All three are now non-blocking: the image-loader cache-size read is bounded with a timeout and default, the preference cache keeps retrying, and the persisted queue restore runs off the main thread (DE 1.50.3 / APK 6.4.44).

## [6.4.43_DE-1.50.3-nightly] - 2026-09-03

### Fixed
- [DE] [APK] The "Sync VIVI volume" toggle now gates only the in-app VIVI volume channel — the native OS system-volume sync is an independent channel again and keeps syncing whenever the devices are paired, so turning the toggle off no longer stops the system volume from following the peer (on either edition).
- [DE] The Devices sync screen gained a connection-method selector at the top (Server relay, recommended / Local LAN server) and the "how to connect" steps adapt to the chosen method; the pairing QR code is now generated for both methods; the Connect button shows "Connecting…" (with a note that it can take a few seconds — up to 2 minutes if the relay server has to wake up) instead of staying static; and the mobile-download button reads "Download the adapted VIVI Music for Android (APK)".

## [6.4.42_DE-1.50.2-nightly] - 2026-09-03

### Fixed
- [APK] The relay server field in Settings → Devices can be cleared and changed again: it is now kept as local state while editing and persisted with an app-lifetime scope, so a cleared or edited value no longer snaps back to the last-used one when leaving the screen.
- [DE] The Device sync section no longer claims to be "Connected" when it is only connected to the relay: until a device is actually paired the status says it is waiting for a device to pair.

## [6.4.41_DE-1.50.1-nightly] - 2026-09-02

### Fixed
- [DE] **Listen Together: creating a room no longer hangs on the spinner**: the client stored the create/join request in a closure that captured the socket variable *before* connecting — when the WebSocket was still offline the captured value was `null`, so `create_room` was silently dropped and the UI stayed stuck on the loading indicator (the connection badge said "connected" but no room was ever created). Pending messages are now queued as text and flushed from `onOpen` on the actual open socket; a dropped connection while a create/join is in flight also releases the spinner so the user can retry, and a failed connection no longer leaves the button disabled.

## [6.4.41_DE-1.50.0-nightly] - 2026-09-02

### Added
- [DE] **Real Listen Together (full port of the mobile feature)**: the desktop Listen Together screen now speaks the complete mobile wire protocol and synchronizes real playback between devices.
  - **Host**: observes the local player and broadcasts track changes (with the full queue), play/pause, user seeks, queue edits (debounced) and in-app volume (when "Sync volume" is on), plus a 10-second playback heartbeat so guests auto-correct drift.
  - **Guest**: applies the host's actions with debounce + position tolerance (no audible seek glitches), the buffering protocol (buffer-ready/wait/complete), whole-queue replacement that preserves the current track, queue add/remove/clear, volume sync and smart re-sync after reconnection.
- **Rooms**: create/join with room code, join requests with approve/reject (optional auto-approve), kick and transfer-host, suggestion flow (guests paste a YouTube link/ID, host approves/rejects → inserted next), chat with replies, buffering indicator, connection state badge, request-sync/reconnect buttons and a copy-code button.
  - **Session persistence**: the room session token is saved in settings, so a reconnect (or app restart) resumes the room automatically.
- [DE] **PlayerController: `insertNext` + `replaceQueuePreservingCurrent`** — the two primitives Listen Together needs to play-next and to swap a guest's queue without restarting the current track.
- [DE] 18 new Listen Together strings translated into all 47 languages (618 keys total; `check_localization` passes).

## [6.4.41_DE-1.49.9-nightly] - 2026-09-02

### Fixed
- [DE] **Data saver toggle now updates live**: flipping the switch in Settings → Data saver previously only reflected the new state after leaving and re-entering the screen (the value was read once from the settings file instead of from observable state).

## [6.4.41_DE-1.49.8-nightly] - 2026-09-02

### Fixed
- [DE+APK] **Volume sync now fully obeys the "Sync VIVI volume" toggle**: the native OS system-volume channel used to sync unconditionally (changing the system volume on one device moved it on both even with every toggle off).
- [DE] **Tray menu toggle applies live**: disabling "Tray menu" now removes the tray icon immediately (no restart needed); re-enabling recreates it. Notifications still work on every OS (WinRT toasts / osascript / temporary tray balloon).
- [DE] **Home section songs now play as a queue like Android**: tapping a song in a Home recommendation section (Quick Picks, Last Listen, …) enqueues the whole section starting from the tapped track, instead of playing a single song. Fixed a `artist - artist` text bug on the mini-player card (now `title - artist`).
- [DE] **Mac/Linux physical media keys work (with permission)**: global Play/Pause, Next, Previous and Stop now work on macOS and Linux through JNativeHook (the Windows low-level hook is unchanged). On macOS the OS asks for Accessibility permission once; until granted, VIVI logs a hint instead of failing silently.
- [DE] **Audio glitches** (especially macOS): the output line buffer was doubled (16 KB with fallback to the old 8 KB) so scheduler/GC hiccups no longer underrun as easily.
- [DE] **Log export moved to Settings → System** (it was under Developer options): same .zip export (logs + redacted system info), now in the System sub-menu.
- [DE] **Remaining raw UI strings localized in all 47 languages**: Home greeting (Good morning/afternoon/evening), "Your Artists Feed", "Made For You", "See all"/"View section", and every button tooltip (Menu, Collapse/Toggle sidebar, Clear, Output device, Minimize, Forward, Queue options, Autoplay, Open full player, Favorite, …). Localization grew from 582 to 600 keys.

## [6.4.41_DE-1.49.7-nightly] - 2026-09-02

### Fixed
- [DE] **Sign-in now requires `DATASYNC_ID` and `VISITOR_DATA`** (they were treated as optional, which made the account validation answer as a guest — the cryptic NPE / 5xx some users hit after the embedded window closed).
- [DE] Login labels/hint updated from "optional" to "required" wording in all 47 languages.

## [6.4.41_DE-1.49.6-nightly] - 2026-09-02

### Added
- [DE] **Developer options → "Export logs (.zip)"**: packages VIVI's diagnostic data into a STORED .zip for support requests — every log file from `~/.vivimusic/` (login-debug.log, native-notify.log, …) plus a generated `system-info.txt` (app version, OS, Java, key settings) and a redacted `settings-summary.txt` (the settings file itself is never included because it holds the YouTube cookie). The save dialog suggests `vivi-de-logs-<version>-<date>.zip`.
- [DE] **Appearance → Native system title bar now shows a compatibility hint** below the description: recommended when window/rendering problems occur (translated in all 47 languages).

### Fixed
- [DE] **Native notifications on macOS now actually fire**: the old path used the `java.awt.SystemTray` balloon, which usually never shows on macOS (especially Apple Silicon). Native notifications now go through `osascript display notification` (Notification Center) with a fallback to the tray balloon if that fails.
- [DE] **Login validation failures caused by a YouTube Music server error (HTTP 5xx.

## [6.4.41_DE-1.49.5-nightly] - 2026-09-02

### Fixed
- [DE] **"Player design" entry in Settings → Player & audio now opens the design screen**: the row was rendered in the player section but its navigation callback was never wired, so clicking it did nothing (the identical entry under Appearance worked).

## [6.4.41_DE-1.49.4-nightly] - 2026-09-02

### Changed
- [DE] **Font sub-screen restored to the pre-1.44.0 rich layout**: the big themed typography preview card (primary-container, "Typography Preview" label + the quote rendered in the selected font) is back, and each font is listed as its own radio row rendered in that typeface with its description (System / Google Sans / Sans Flex / Outfit / Plus Jakarta Sans / Custom), instead of the compact dropdown. Verified live on Windows.

## [6.4.41_DE-1.49.3-nightly] - 2026-09-02

### Fixed
- [DE] **[CRITICAL FIX] All single-choice dropdown options were invisible since 1.44.0**: `M3SettingsDropdownItem` constructed its row (`M3SettingsItem`) but never rendered it, so every dropdown-based option disappeared from the UI — Font / Canvas source / Density & grid / Screen transitions / Player design / Mini-player design & background / Slider style / Notification mode & duration / Intro style & background all looked "missing", their sub-screens appeared empty and the rows did nothing when clicked. The row is now actually composed (wrapped in `M3SettingsItemRow`), restoring every dropdown everywhere.

## [6.4.41_DE-1.49.2-nightly] - 2026-09-02

### Added
- [DE] **EQ band range labels**: the live equalizer editor now shows the frequency region next to each band's center frequency (Sub-bass / Bass / Low mid / Mid / High mid / Treble), translated in all 47 languages (e.g. "Band 2 · 150 Hz · Bass").

## [6.4.41_DE-1.49.1-nightly] - 2026-09-02

### Fixed
- [DE] **[CRITICAL FIX] Restored the Appearance sub-screens that were flattened into inline dropdowns in 1.44.0**: Font, VIVI Music Canvas, Density & grid, Screen transitions and Player design are back in Settings → Appearance as Material 3 sub-screens (anchored dropdowns inside each screen), and the Player design entry is back under Player & audio.
- [DE] **Mini-player design style no longer resets on launch**: removed the legacy `miniPlayerStyle` setting that duplicated `miniPlayerDesign` and could shadow the chosen style; a single setting is now read, changed and saved consistently.
- [DE] **Player background option "Canvas" was showing the raw key in every language**: it had no translation entry at all; added the `canvas` label translated in all 47 languages.

### Changed
- [DE] **First-launch screen redesigned as a Material 3 welcome**: bundled logo, welcome title and description, the language list rendered as M3 option cards with a checkmark, and a full-width Continue button (previously a bare text list that went straight in on click).

## [6.4.41_DE-1.48.0-nightly] - 2026-09-01

### Changed
- [DE] **Inner settings sub-screens restyled to Material 3**: the Intro screen (show-intro switch into a card + style/background as anchored dropdowns), the Privacy screen (history toggles into M3 grouped cards) and the Notifications screen (mode + duration as inline dropdowns instead of cramped radio rows). Removed the squashed flat rows.

## [6.4.41_DE-1.47.0-nightly] - 2026-09-01

### Added
- [DE] **Animations master switch** (Appearance → Animations): turning it off makes screen transitions instant (no fade/slide) and the intro splash switches without animation. When animations are off, the Screen transitions picker is hidden.

## [6.4.41_DE-1.46.0-nightly] - 2026-09-01

### Added
- [DE] **Equalizer example profile + live editor**: a one-click "Add example profile" inserts a predefined V-shape profile (7 bands), and selecting a profile now opens an inline live editor with draggable sliders for preamp, per-band gain (−12…+12 dB) and Q factor (0.4…8) applied to playback in real time — no more guessing the AutoEQ text format.

## [6.4.41_DE-1.45.0-nightly] - 2026-09-01

### Added
- [DE] **Custom accent color picker**: in the Theme screen there is now a full HSV gradient picker (hue / saturation / brightness bars you can click or drag) with a live preview, the hex code, and an “Add to palette” button.
- [DE] **6 new accent palette colors**: Magenta, Turquoise, Coral, Lavender, Gold and Navy (with tooltips in all 47 languages).
- [i18n] Completed the missing `mini_player_*` translations (mini-player design/background options and the pure-black mini-player toggle) in all 47 languages.

## [6.4.41_DE-1.44.0-nightly] - 2026-09-01

### Added
- [DE] **Inline dropdowns for single-choice settings**: options that previously opened a dedicated sub-screen or an unanchored menu (font, canvas source, UI density, grid size, player design, player background, mini-player design, mini-player background, audio quality, slider style) are now Material 3 dropdowns anchored directly under their row — no more popup menus appearing in the top-left corner of the window. The dedicated Font/Canvas/Density/Transitions/Player-design sub-screens were removed.

## [6.4.41_DE-1.43.2-nightly] - 2026-09-01

### Fixed
- [DE] **UI density calibrated**: the "100%" preset no longer looks oversized next to the phone — it now matches the size the mobile UI has at 75% (a calibration factor of 0.75 is applied to the density scale, keeping all presets from 55% to 200% relative).
- [DE] **Duplicate option labels disambiguated**: in 11 languages (including Italian) the slider styles `Squiggly` and `Wavy` translated to the same word.

### Added
- [DOCS] `ERRORS.md` expanded from 13 to 29 VIVI-specific error codes (E1000–E1028): new codes for update check/download/installer-not-found (E1013–E1015), sync self-pair/not-paired/relay-bind/LAN (E1016–E1019), backup empty-archive/create-failed (E1020–E1021), login cookie empty/missing-SAPISID/webview-timeout (E1022–E1024), song recognition no-mic (E1025), Listen Together (E1026), commit list (E1027) and stream resolution (E1028). Each new code mirrors a real failure path in the DE code.
- [WEBSITE] New interactive **Error codes** page (`errors.html`, linked in the nav and footer of every page) that loads `ERRORS.md` from the repository automatically, parses the Markdown tables and renders them as searchable rows with Playback/VIVI category tabs, a result counter and a click-to-copy code button.

## [6.4.41_DE-1.43.1-nightly] - 2026-09-01

### Changed
- [DE] License references now correctly say **modified GPL-3.0** (the LICENSE file itself was left untouched): README footer, INSTALL-GUIDE.md, the website (About, home, install guide) and the desktop About screen link (now pointing at the `vivi-music-de` branch LICENSE, matching the README badge).

## [6.4.41_DE-1.43.0-nightly] - 2026-09-01

### Added
- [DE] **Settings sub-menus redesigned to match the mobile Material 3 look**: a shared card-based settings component (rounded cards, tinted icon tiles, section titles) now styles the main Settings screen — grouped into General / Appearance / Player & audio / Account / Content / Privacy / About sections with the search bar still working — and the sub-screens (Appearance, Player & audio, Notifications, Lyrics, System, Data saver) use the same card rows for navigation and toggle entries.
- [DE] **ERRORS.md**: new error-code reference (root + linked from the website footer) listing every playback error code (1000–6008, ExoPlayer/media3) and VIVI-specific codes (E1000–E1012: login, sync, backup, update, EQ import, AI translation) in ascending order, each with cause and how to fix it.
- [WEBSITE] Footer now links to the error-code reference; the fake "Now playing · demo" card and the LIVE version badge in the hero were removed (the real latest version is still shown on Downloads/Changelog).

## [6.4.41_DE-1.42.1-nightly] - 2026-09-01

### Added
- [DE] **Full translation coverage for the new sub-screens**: the Equalizer, Data saver and AI Lyrics Translation keys are now translated in all 47 languages (reusing the mobile translations where they exist and filling every gap, so no string falls back to English).

## [6.4.41_DE-1.42.0-nightly] - 2026-09-01

### Added
- [DE] **Equalizer** (Settings → Player & audio → Equalizer): import AutoEQ `ParametricEQ.txt` profiles (native file picker), select the active profile or "Disabled", delete with confirmation.
- [DE] **Data saver** (Settings → Data saver): master toggle that backs up the current canvas and rotating-artwork settings, forces them off while enabled, and restores the saved values on disable (port of the mobile `DataSaverSetting`).
- [DE] **AI Lyrics Translation** (Settings → AI Lyrics Translation): provider picker (OpenRouter/OpenAI/Perplexity/Claude/Gemini/XAi/Mistral/DeepL/Custom) with per-provider base URL + first model auto-selection, API key entry (masked), editable base URL, model dropdown (or hidden for DeepL/Custom), translation mode (Literal/Transcribed), DeepL formality and target language — all persisted for the future lyrics-translation integration.

## [6.4.41_DE-1.41.17-nightly] - 2026-09-01

### Fixed
- [DE] **WebView sign-in now captures the same cookies the manual method does**: the capture dumped the whole cookie store with a coin-flip domain tie-break (`.google.com` vs `.youtube.com` have equal length), and the full-session gate required the legacy `SID` cookie that modern Google logins never issue. The capture now asks the cookie handler which cookies it would actually send to `music.youtube.com` (identical to the manually pasted header), prefers the `.youtube.com` variant on domain ties, accepts `__Secure-1PSID`/`__Secure-3PSID` as session ids, and backfills any critical auth cookies the scoped lookup misses.

## [6.4.41_DE-1.41.16-nightly] - 2026-09-01

### Fixed
- [DE] **WebView sign-in now captures the same cookie set the manual method does**: the capture built the session header by dumping the whole cookie store with an arbitrary domain tie-break, so `SAPISID`/`__Secure-3PSID` could be taken from `.google.com` instead of `.youtube.com`, and the full-session gate required the legacy `SID` cookie that modern Google logins never issue. The capture now asks the cookie handler which cookies it would actually send to `music.youtube.com` (same domain/path/secure matching a browser applies — identical to the manually pasted header), prefers the `.youtube.com` variant on domain ties, accepts `__Secure-1PSID`/`__Secure-3PSID` as session ids, and backfills any critical auth cookie the scoped lookup missed. Failed WebView logins are kept in the manual cookie field for a one-click retry.

## [6.4.41_DE-1.41.15-nightly] - 2026-08-29

### Added
- [DE] **Install guide shipped with every release and on the website**: new `INSTALL-GUIDE.md` covers Windows (`.exe`/`.msi`), Linux (`.deb`/AppImage/PKGBUILD) and macOS (`.dmg`/`.pkg`), plus first-launch sign-in, updating, data locations and troubleshooting; the auto-release workflow now attaches it as a release asset. The website gains an interactive `install-guide.html` (OS tabs + copy buttons) linked from every page's navigation.

## [6.4.41_DE-1.41.14-nightly] - 2026-08-29

### Fixed
- [DE] **Linux crash with `UnsatisfiedLinkError: OpenGLApi.glFlush()` now heals itself**: the previous startup probe only checked that a `libGL` was loadable, but a loadable library is not a working GLX/EGL context, so the first frame could still die on Arch/AppImage systems.

### Added
- [DE] **AUR packaging assets are now attached to every release**: the auto-release workflow generates `PKGBUILD` + `SRCINFO` (scripts/generate_aur_pkgbuild.py) pinned to the exact release commit with a real sha256 checksum, so Arch users can grab them and run `makepkg -si` for a proper system package instead of relying on the AppImage.

## [6.4.41_DE-1.41.13-nightly] - 2026-08-29

### Fixed
- [DE] **Login with the embedded WebView failed with `401 UNAUTHENTICATED` on `account_menu` after showing "saving session"**: Google's modern logins emit `__Secure-3PAPISID`/`__Secure-1PAPISID` instead of the legacy `SAPISID`, so the API request carried the cookie but no `Authorization: SAPISIDHASH` header and YouTube rejected it. The hash is now computed from any of `SAPISID` / `__Secure-3PAPISID` / `__Secure-1PAPISID`, login validation accepts all three, and cookie capture keeps the most specific domain per name (e.g. `SAPISID` on `.youtube.com` instead of `.google.com`) so the header authenticates correctly.

## [6.4.41_DE-1.41.12-nightly] - 2026-08-29

### Fixed
- [DE] **AppImage no longer crashes on Linux without a working libGL** (e.g. Arch): the first frame died with `UnsatisfiedLinkError` in `OpenGLApi.glFlush()` because Skiko defaults to OpenGL on Linux.

## [6.4.41_DE-1.41.11-nightly] - 2026-08-29

### Changed
- [DE] **Expressive theme now stays inside the Material palette**: the "Made For You" mix cards, the player's live-mesh background, artist placeholder gradients and the Expressive player's bottom toolbar used fixed brand colors (red/blue/purple/grey hex values).

## [6.4.41_DE-1.41.10-nightly] - 2026-08-29

### Fixed
- [DE] **12 strings showed raw key names in the UI**: `open_vivi`, `quit`, `desktop_features`, `desktop_features_desc`, `lyrics_focus`, `now_playing_widget`, `now_playing_widget_desc`, `media_keys`, `media_keys_desc`, `tray_menu`, `tray_menu_desc` and `windows_only` were translated in every language but were never registered in the generator's mapping, so the English table lacked them and the fallback showed the raw key. The keys are now wired into the mapping, regenerated, and present in all 47 languages + English.

## [6.4.41_DE-1.41.9-nightly] - 2026-08-29

### Fixed
- [DE] **Tooltips appear next to the pointer again**: the smart placement was using the cursor position in component-local coordinates while the popup expects window coordinates, so every tooltip showed at the window's top-left.

## [6.4.41_DE-1.41.8-nightly] - 2026-08-29

### Changed
- [DE] **Tooltip placement is smarter**: tooltips now appear below-right of the pointer by default; if there is no room below they flip above, and they always clamp to the window edges so they are never cut off and never cover the click target.

## [6.4.41_DE-1.41.7-nightly] - 2026-08-29

### Fixed
- [DE] **Right Now Playing panel hides while the full player is open**: it disappears when any player type is expanded (it would duplicate the player content) and comes back automatically when leaving the player, without changing the saved preference.
- [DE] **Shuffle and repeat buttons now present in every player**: they were missing in the Expressive player design and in the New/Apple mini players; they are now shown with the same active/inactive accent highlighting as the classic layouts.

## [6.4.41_DE-1.41.6-nightly] - 2026-08-29

### Fixed
- [DE] **Tooltips no longer cover the click target**: they now appear above the button with an 8dp gap (`TooltipPlacement.ComponentRect(TopCenter)`) instead of directly on the cursor, so they never block the click.

## [6.4.41_DE-1.41.5-nightly] - 2026-08-29

### Fixed
- [DE] **Distinct icons for distinct actions**: the right Now Playing panel toggle now uses the split-panel icon (`VerticalSplit`) instead of reusing the queue icon; playlist entries in the sidebar and the playlist search filter now use the playlist icon (`PlaylistPlay`) instead of the queue icon.

## [6.4.41_DE-1.41.4-nightly] - 2026-08-29

### Added
- [DE] **Right Now Playing panel toggle** (Settings → Appearance): a switch shows/hides the Spotify-style right sidebar in the player, persisted per machine. 2 new strings (`right_panel`, `right_panel_desc`) in all 47 languages (batch 39).

## [6.4.41_DE-1.41.3-nightly] - 2026-08-29

### Added
- [DE] **Tooltips on accent color swatches** (Settings → Appearance → Theme & colors): hovering a palette color now shows its name in the app language (e.g. Italian "Viola", "Blu", "Azzurro"). 22 new localized strings `accent_*` in all 47 languages (batch 38); the English fallback matches the palette names.

## [6.4.41_DE-1.41.2-nightly] - 2026-08-29

### Added
- [DE] **Hover tooltips on buttons**: resting the pointer on a button now shows a small hint with its name (like website tooltips).

## [6.4.41_DE-1.41.1-nightly] - 2026-08-29

### Fixed
- [DE] The sidebar "Artists" entry now opens the actual Artists screen (the library artists list) instead of the Queue; the queue keeps its dedicated button.

## [6.4.41_DE-1.41.0-nightly] - 2026-08-29

### Added
- [DE] **Import your own font** (Settings → Appearance → Font): a new "Import your own font" button opens a native file dialog (`.ttf`/`.otf`); the chosen font is copied into the app data dir (`~/.vivimusic/fonts/`) and becomes selectable as a "Custom font" option in the list, applying instantly to the whole UI and persisting across restarts (even if the original font file is later moved). 2 new strings translated in all 47 languages.

### Changed
- [DE] The font preview text in Italian now reads "Dove la parola fallisce la musica stupisce." (was "Il viaggio stesso è stato più bello della destinazione.").

## [6.4.41_DE-1.40.0-nightly] - 2026-08-29

### Added
- [DE] **Accent color intensity slider** (Settings → Appearance → Theme & colors): tune how vivid the accent appears from 100% down to 0% (fully desaturated grey of the same lightness).

## [6.4.41_DE-1.39.0-nightly] - 2026-08-29

### Added
- [DE] **Spotify-style shell (Phase 1)** — the 3-panel shell now follows the Spotify look when the Spotify style is active:
- **Sidebar**: pure-black background in dark mode (white in light), no surface frame around it; selected items use a grey pill (`surfaceContainerHighest`) with full-contrast bold text instead of the accent-filled selection; entries get a subtle hover background (`surfaceContainerHigh`); corner radius reduced to 8dp for the main/library/playlist entries. The classic Material layout keeps its accent selection untouched.
  - **Bottom player bar**: a thin 1dp top border separates it from the content above (Spotify-style). The bar already had the Spotify layout (cover + title/artist left, transport center, volume right).
  - **Right Now-Playing panel**: its background was hardcoded `#121212` (dark-only, wrong in light mode); it now uses the theme panel color (`surfaceContainer` — `#181818` in dark Spotify, `#F6F6F6` in light).

## [6.4.41_DE-1.38.2-nightly] - 2026-08-29

### Fixed
- [DE] **No more accent color in the queue screen**: the standalone Queue (opened from the sidebar's Artists entry) painted the current track's title, its "▶" marker and the add-to-playlist icon in the accent color, and the swipe-reveal "Play" hint used the accent-tinted container — so the queue kept showing accent-colored elements. All queue rows are now neutral: the current track is distinguished by a bold title + full-contrast "▶" (no accent paint), the swipe hint uses a neutral surface, and the add-to-playlist icon uses the secondary text color.

## [6.4.41_DE-1.38.1-nightly] - 2026-08-29

### Fixed
- [DE] **Accent color selection works again in Spotify style**: the flat Spotify scheme hardcoded the green `#1DB954` as the primary color, so picking a different accent did nothing.
- [DE] **Queue/Lyrics/History panel in the Expressive player no longer sits on an accent-tinted background**: the right panel was `surface` at 55% opacity over the accent-colored player background, so the accent bled through behind the track list. The panel is now fully opaque, like the plain playlist screens.

## [6.4.41_DE-1.38.0-nightly] - 2026-08-29

### Added
- [DE] **Spotify-style UI redesign — Phase 0 (theme foundations)**: when the Spotify layout is active (the default 3-panel shell), the app now uses a flat Spotify palette instead of the tonal Material 3 scheme — dark `#121212` background with `#181818` panels and `#282828` hover surfaces, fixed green accent `#1DB954`, secondary text `#B3B3B3`; light mode `#FFFFFF` / `#F6F6F6` panels / text `#191414`. All surfaces drop the Material 3 tonal variation, corners become a uniform 8dp (`Shapes`), and titles/labels render bolder (Spotify-like). Pure black in dark mode forces a true black background. The main window root now paints the flat background behind the `surfaceContainer` panels so the 3-panel shell has real depth. The tonal Material 3 theme is untouched and returns whenever the Spotify layout is off; theme/accent sync with the mobile app is unaffected.

## [6.4.41_DE-1.37.3-nightly] - 2026-08-29

### Fixed
- [DE] **Mini-player design no longer resets to Apple on every start**: `MiniPlayerDesign.from()` ignored the saved key and always returned `APPLE`, so any chosen design (Classic / New) was lost at the next launch and the mini player snapped to the Apple layout — looking like it had synced back to the mobile default. The mapping now honors the saved value (with legacy key aliases) and falls back to the declared default (`CLASSIC`) only for unknown/missing keys.
- [DE] **Mini-player background style falls back to "Follow theme" for unknown keys** (was `BLUR`), matching the field's declared default.

## [6.4.41_DE-1.37.2-nightly] - 2026-08-28

### Fixed
- [DE] **Enabling the native title bar no longer crashes** (`IllegalComponentStateException: The frame is displayable`): the toggle changed reactive state that Compose's `SwingWindow` reads to call `setUndecorated()` on the already-displayed frame, which throws.
- [DE] **Window no longer reopens stretched over the Windows taskbar**: closing the app while maximized saved the maximized (full-screen) bounds, and the next start re-applied them as a normal placement — the window opened sitting over/under the taskbar (with an auto-hide bar it stayed above it).

## [6.4.41_DE-1.37.1-nightly] - 2026-08-28

### Fixed
- [DE] **"Must press play twice when paired"**: a local play/navigation command now opens a 3-second grace window during which a peer's "paused" echo (the phone keeps pushing its pre-action state until it processes our play) is ignored, so the track no longer pauses itself right after the stream finishes resolving.
- [DE] **"Next" at the end of the queue now wraps to the first track** instead of being a dead button (manual next always wraps; repeat mode only affects auto-advance).

## [6.4.41_DE-1.37.0-nightly] - 2026-08-28

### Added
- [DE] **"Download VIVI for Android (APK)" button on the Devices/sync screen**: fetches the newest Android APK from the selected update source's GitHub releases (default PiBOH/vivi-music-de.

## [6.4.41_DE-1.36.0-nightly] - 2026-08-28

### Added
- [DE] **Toggle between the native OS title bar and VIVI's custom one** (Settings → Appearance): off by default (VIVI's bar).

## [6.4.41_DE-1.35.8-nightly] - 2026-08-28

### Fixed
- [DE] **Fullscreen no longer traps the auto-hide Windows taskbar**: the fullscreen toggle used Compose's `WindowPlacement.Fullscreen`, which can oversize an undecorated window on non-100% DPI displays (the same bug class as its Maximized placement) — the window pushed past the screen edge and the auto-hide taskbar could never be revealed, forcing a window restore to reach it. Fullscreen is now applied with the OS API (`MAXIMIZED_BOTH`), which never oversizes: with an auto-hide taskbar the window fills the whole screen and the taskbar still slides up on hover at the bottom edge; with a visible taskbar the window respects the work area. Leaving fullscreen (toggle or maximize button) restores exactly the previous placement (floating bounds or maximized).

## [6.4.41_DE-1.35.7-nightly] - 2026-08-28

### Changed
- [DE] **Faster track start (time-to-audio roughly halved on first play)**: the stream resolver now returns the NewPipe URL immediately when the extractor succeeds, instead of also running the whole ~12-client chain + a HEAD validation round-trip as "insurance" on every first play (~0.7–1.4 s saved). The client chain only runs when NewPipe is bot-blocked, and it now collects just 2 playable candidates without a HEAD request per candidate — the download itself is the validation, and the player falls through to the next candidate on failure. The audio player's start threshold dropped from 64 KB to 32 KB (the `moov` is a few KB and the sample walker skips incomplete trailing atoms), so the decoder starts on the first fragment instead of waiting for a second one (~0.2–0.4 s saved on slower links). Track skips were already instant (prefetch + caches) and are unchanged.

## [6.4.41_DE-1.35.6-nightly] - 2026-08-28

### Fixed
- [DE] **Window controls (minimize / maximize / close) are now always visible**: the window is undecorated (no OS title bar), but the buttons only lived in the Spotify top header — which disappears on the full player screen and in the non-Spotify layout, leaving no way to minimize or close the window. The three buttons were extracted into a shared `WindowControls` composable; the Spotify header still hosts them, and a floating top-right overlay shows them on the player screen and in the non-Spotify layout (the transparent overlay passes clicks through to the content below).

## [6.4.41_DE-1.35.5-nightly] - 2026-08-28

### Changed
- [DE] **Playback starts while the track is still downloading (progressive streaming)**: the stream is downloaded to a unique `.part` file in the background and the decoder starts as soon as the first audio fragment is on disk, instead of waiting for the whole file.
- [DE] **Stream resolution is much faster**: the client chain used to run all ~12 fallback clients sequentially even when the first URL validated, costing many round-trips per track. It now stops at the first HEAD-validated URL (NewPipe + any already-collected candidates remain as download fallbacks).
- [DE] **The "downloading" indicator is accurate**: the loading phase is no longer cleared the instant resolution ends — it now stays visible until audio is actually ready (the first decoded frame).

## [6.4.41_DE-1.35.4-nightly] - 2026-08-28

### Fixed
- [DE] **"Module with the Main dispatcher is missing" crash**: desktop code hops back to `Dispatchers.Main` after off-thread image blur, but the module that provides the Swing-backed Main dispatcher was never declared — only `kotlinx-coroutines-core` was.

## [6.4.41_DE-1.35.3-nightly] - 2026-08-28

### Fixed
- [DE] **Expressive player volume slider actually works**: it was left with an empty `onValueChange` placeholder (``/* Volume update */``) from the UI port, so dragging it did nothing — now wired to the real volume handler (this is why "can't change VIVI's volume" also happened while not paired).
- [DE] **Pressing play after a track finished now restarts it**: a finished track keeps its end position, and restarting from the end instantly "completed" again and stopped — the play button looked broken. Play now restarts from 0:00.
- [DE] **Truncated audio cache files are re-downloaded**: a cache file that passes the header check but holds only a fraction of the track (interrupted download, or a prefetch/play race writing the same partial file) played a few seconds and "ended", which caused tracks to stop after ~10 s or skip by themselves. Partial downloads now use unique filenames (no more concurrent-write corruption), a sample-count check against the known duration detects truncation, and a failed play evicts the bad file so the retry re-downloads a clean copy.

## [6.4.41_DE-1.35.2-nightly] - 2026-08-28

### Fixed
- [DE] Device sync no longer lets a stale remote snapshot override a fresh local action:
  - **VIVI volume slider can now be changed while paired**: a local drag wins for 2 s, so an echoed or stale value from the phone can't snap the slider back the moment you let go (previously the peer's value kept re-applying and the slider appeared stuck).
  - **No more double-play after resolving**: when the stream finishes resolving, only a remote seek/play-pause that actually arrived *while* we were buffering is re-applied — a pre-play snapshot (common over a slow phone hotspot) no longer pauses the track the user just started, so pressing play once is enough again.
  - **Stale "paused" ticks ignored**: a periodic peer snapshot older than a full sync tick (5 s) with `isPlaying=false` is no longer applied, so the DE can't auto-pause a few seconds after you hit play because the phone's snapshot hasn't caught up yet.

## [6.4.41_DE-1.35.1-nightly] - 2026-08-28

### Changed
- [DE] Apple/Cider-style visual polish across the UI:
  - **Now-playing indicator in lists**: the current row in every song list (album, playlist, search, library…) is highlighted with the accent color and shows three animated equalizer bars that move with the real decoded audio level (falling back to a gentle idle pulse when paused); the row background is softly tinted.
  - **Artwork ambience**: the player artwork now sits on a colored glow derived from the blurred artwork and casts a soft specular reflection below it (Apple Music style).
  - **Glass transport controls**: Previous/Play/Next/Shuffle/Repeat are now semi-transparent glass circles (subtle sheen + border) instead of flat Material buttons, so they sit on the artwork; the play button keeps the accent color.
  - **Crossfade on track change**: background, artwork and controls fade between songs (≈300 ms) instead of cutting hard.
  - **Expressive player side panel** now uses the Material 3 theme surface + border instead of a hardcoded dark color, so it follows the accent/theme.

## [6.4.41_DE-1.35.0-nightly] - 2026-08-28

### Added
- [DE] Cider-inspired desktop features (Settings → Desktop features):
  - **Audio visualizer**: new player background style (`Visualizer`) whose bars react to the real decoded PCM level of the playing stream, with a dark scrim so the artwork/title stay readable.
  - **Now Playing widget**: small always-on-top, draggable window showing the current track (artwork, title, artist) with Previous/Play-Pause/Next controls; position persists across restarts; toggle in Settings → Desktop features.
  - **Global media keys (Windows)**: Play/Pause, Next, Previous and Stop keys work even when the window has no focus, via a JNA `WH_KEYBOARD_LL` hook on its own message-pump thread; Windows-only, toggled in Settings → Desktop features.
  - **Tray menu**: right-click the system tray icon for Play/Pause, Next, Previous, Open VIVI Music and Quit (labels localized, tooltip shows the current track); toggled in Settings → Desktop features.
  - **Fullscreen lyrics**: new "Fullscreen lyrics" button in the player (Classic/New/V2 designs) opens a Cider-style focus mode — blurred artwork backdrop, centered synced lyrics, bottom transport bar; Esc/back exits.
- [DE] New translations for all of the above across all 47 languages (13 new keys, `player_background_visualizer` … `quit`).

### Notes
- The Windows media flyout (SMTC: showing the track in the Win+volume popup) is **not** included yet: it requires WinRT COM interop that cannot be validated without a Windows machine; tracked in TODO.md.

## [6.4.41_DE-1.34.8-nightly] - 2026-08-28

### Fixed
- [DE] The full player (Classic/New/V2 designs) had no visible way back: the sidebar and the top header are hidden on the player screen and the window is undecorated, so the player felt like it filled the whole screen with no way to shrink it.

## [6.4.41_DE-1.34.7-nightly] - 2026-08-28

### Reverted
- [APK] Reverted the Telegram channel change on the Android app (About + Welcome screens) and the issue template: the new channel `t.me/vivimusicde` is DE-only, so the mobile app keeps the original `t.me/vivimusicapp` and the template keeps its old invite link.

### Fixed
- [DE+APK] Google sign-in validation reported a bare "NullPointerException"/"unknown error" when the innertube `account_menu` answered as guest: `accountInfo` now reports "Not signed in: account_menu returned no active account" instead of crashing on the missing header.
- [DE] The embedded sign-in now captures the FULL session cookie set: it waits until the critical HttpOnly cookies (SID + `__Secure-3PSID`) are present (not just SAPISID), reloads `music.youtube.com` with the session to force every youtube.com cookie, re-captures before closing, and logs the captured cookie names + missing ones to `~/.vivimusic/login-debug.log` for diagnosis (validation failures are appended to the same file).

## [6.4.40_DE-1.34.6-nightly] - 2026-08-28

### Changed
- [DE+APK] Updated every reference to the old Telegram channel (`t.me/vivimusicapp` and the old invite link) to the new channel `https://t.me/vivimusicde` — DE About screen, Android About screen, Android Welcome screen, the issue template and the whole website (`.websitede/**`).

## [6.4.39_DE-1.34.5-nightly] - 2026-08-28

### Fixed
- [DE] Google sign-in via the embedded WebView ended with "unknown error" after the window closed: the session was captured as soon as SAPISID appeared, but the remaining redirect cookies (SID, HSID, SSID, APISID, `__Secure-3PSID`, …) were still arriving, so the innertube `account_menu` validation answered as guest (NPE -> generic error). The WebView now waits 3 s for the cookie set to settle, re-captures the full header before closing, validation retries once after a 2 s pause, and the failure message includes the real exception class instead of "unknown error". Captured cookie names are also logged (`[login-webview] captured N cookies: …`) for diagnosis.

## [6.4.39_DE-1.34.4-nightly] - 2026-08-28

### Fixed
- [DE] Selecting a player design in Settings → Player & audio → player design (and the density screen) could make the whole UI explode and freeze the app, forcing Task Manager.

## [6.4.39_DE-1.34.3-nightly] - 2026-08-28

### Fixed
- [DE] Windows window management overhaul: the app forced Compose's `WindowPlacement.Maximized`/Fullscreen on every start and when opening the player; on an undecorated window with a display scale other than 100% this can size the window LARGER than the screen (everything looks enlarged, the title bar ends up off-screen and the app must be killed from Task Manager, and the window can cover the auto-hiding taskbar). Now the app starts floating, restores the last placement with the OS APIs (`Frame.MAXIMIZED_BOTH` respects the taskbar and DPI scaling), persists position/size/maximized state across restarts, clamps restored bounds to the usable screen area, never resizes the window when opening the player, and uses true fullscreen only via the explicit toggle.

## [6.4.39_DE-1.34.2-nightly] - 2026-08-28

### Fixed
- [DE] The embedded login WebView stayed permanently white in the packaged app.

### Changed
- [DE] The sidebar now compresses to a compact icon rail (72dp, centered icons) when collapsed instead of disappearing entirely, in both the Spotify and classic layouts.

## [6.4.39_DE-1.34.1-nightly] - 2026-08-28

### Added
- [DE] Completed the translations of the 4 new UI strings (`close`, `listen_together_title`, `search_hint`, `up_next`) into all 47 non-English languages (batch `desktop_extra_translations_32`); every language table now contains all 441 keys with no fallback.

## [6.4.39_DE-1.34.0-nightly] - 2026-08-28

### Added
- [DE] Ported the new UI (from the NewUI_desktop.zip line, versions 1.34.x–1.35.x) onto the current codebase, replacing the previous player/mini-player design: Apple-style single-column player (tuned artwork 521dp, 25sp title/artist, 40dp favorite & options pills, 513dp seekbar, 64dp transport, 465dp volume bar) with the new `EXPRESSIVE` thick capsule track style in `ViviSlider`.
- [DE] Modernized MiniPlayer suite: 3 design variants (Classic, New single-column hero, Apple-style floating island) and 5 animated background styles (Follow Theme, Gradient, Blur, Glow Motion, Live Mesh), plus a Pure Black toggle and a Fullscreen action expanding into the full player.
- [DE] Spotify-inspired 3-panel card layout with top navigation header (`spotifyLayout`) and right Now-Playing panel (`showRightSidebar`) with multi-artist parsing and profile photo resolution.
- [DE] Player personalization options persisted in settings: `miniPlayerDesign`, `miniPlayerBackgroundStyle`, `pureBlackMiniPlayer`, `isFullscreen`, `showRightSidebar`, `spotifyLayout`, `playerArtSize`, `playerArtTopOffset`, `playerArtCornerRadius`.

### Changed
- [DE] Version numbering now follows SemVer properly: a feature-level change like this new UI bumps the minor version (1.33.x → 1.34.0); patch is reserved for fixes only.
- [DE] Re-integrated all features added since the NewUI base: embedded JavaFX Google sign-in (`LoginWebView`), account/Library inline login options (`LoginContent`), AI-translation disclaimer, installer auto-cleanup, official-logo toast path, and the latest localization table (441 keys).

## [6.4.39_DE-1.33.129-nightly] - 2026-08-28

### Changed
- [DE] The Library sidebar entry (when not signed in) now shows the two sign-in options (Accedi con Google / manual cookie) directly instead of a "Log in" button that opened a separate screen — same behavior as Settings → Account, so every login entry point is a single screen.

## [6.4.39_DE-1.33.128-nightly] - 2026-08-28

### Changed
- [DE] Removed the intermediate "Log in" step in Settings → Account: when not signed in, the screen now shows the two sign-in options (Accedi con Google / manual cookie) directly, with no extra navigation.

### Fixed
- [DE] Embedded login WebView could stay blank white in the packaged app even though the page loaded: JavaFX now forces the software renderer (`prism.order=sw`) before starting, avoiding the GPU pipeline conflict with the Compose window that prevented painting on weaker hardware.

## [6.4.39_DE-1.33.127-nightly] - 2026-08-28

### Fixed
- [DE] Completed every missing desktop translation: 3,582 missing per-language strings were added across all 47 languages (new batches `desktop_extra_translations_24..31`).
- [DE] Fixed the desktop localization generator dropping translations: extra translation batches are now merged per key instead of being replaced by the last file's language subset, so a key defined in two batches keeps all its languages.
- [DE] Added `scripts/check_localization.py` to verify used-key coverage and per-language completeness.

## [6.4.39_DE-1.33.126-nightly] - 2026-08-28

### Changed
- [DE] Removed the redundant "Accesso" heading from the login screen: when not signed in, the screen now shows the two sign-in options (Accedi con Google / manual cookie) directly.

## [6.4.39_DE-1.33.125-nightly] - 2026-08-27

### Fixed
- [DE] Embedded login WebView could stay blank white after opening in the packaged app: the WebView now gets explicit dimensions, a forced re-layout/paint nudge when the page starts loading and again on load success, and logs its load state/size/title (prefixed `[login-webview]`) so any remaining blank-page issue is diagnosable from the console.

## [6.4.39_DE-1.33.124-nightly] - 2026-08-27

### Fixed
- [DE] Fixed the embedded YouTube login WebView failing in packaged builds with `NoClassDefFoundError: com/sun/media/jfxmedia/events/PlayerStateListener`: the platform-specific `javafx-media` jar is now packaged alongside `javafx-web`, and the runtime image includes the required `jdk.jsobject` module. Verified with a packaged-app smoke test (`SMOKE: WEBVIEW OK`).

## [6.4.39_DE-1.33.123-nightly] - 2026-08-27

### Fixed
- [DE] Fixed embedded JavaFX login startup after the `.122` build: the toolkit is now initialized once with `Platform.startup` instead of the single-use `Application.launch`, avoiding false WebView-unavailable states after the first attempt.

## [6.4.39_DE-1.33.122-nightly] - 2026-08-27

### Fixed
- [DE] Fixed embedded WebView packaging by replacing the Swing-dependent `JFXPanel` with a direct JavaFX `Stage`. JavaFX jars and native runtime components are now present in the distributable application image, while browser fallback remains available if JavaFX cannot start.

## [6.4.39_DE-1.33.121-nightly] - 2026-08-27

### Fixed
- [DE] Fixed all desktop packaging jobs failing at `createRuntimeImage`: external JavaFX modules are no longer incorrectly passed to jlink without a module path. The embedded WebView remains optional and uses the browser fallback when its JavaFX runtime is unavailable.

## [6.4.39_DE-1.33.120-nightly] - 2026-08-27

### Fixed
- [DE] Fixed the embedded JavaFX login crash `NoClassDefFoundError: jdk/swing/interop/SwingInterOpUtils` by packaging the required `jdk.swing.interop` runtime module.
- [DE] Prevented repeated login WebView initialization failures from reopening the same error indefinitely; failed initialization now falls back cleanly and only once.

## [6.4.39_DE-1.33.119-nightly] - 2026-08-27

### Fixed
- [DE] Fixed Windows toast details continuing to display the old logo from the persistent `~/.vivimusic/logo_vmde.png` cache. Toasts now refresh the circular bundled logo through a new cache path on every notification.

## [6.4.39_DE-1.33.118-nightly] - 2026-08-27

### Added
- [DE] Reworked YouTube login with a real embedded JavaFX sign-in window that opens directly on Google's login page, explains the three required steps, captures the session automatically after returning to YouTube Music, and closes itself after saving the persistent session.
- [DE] Added a clear browser fallback and a collapsible manual Cookie / DATASYNC_ID / VISITOR_DATA login path for systems where the embedded WebView cannot start.
- [DE] Added a language-screen notice explaining that translations were created with AI tools and may not be 100% reliable.

### Fixed
- [DE] Fixed GitHub Auto Release changelog extraction when versioned entries include a release-channel suffix such as `-nightly`; release notes are no longer empty.

### Changed
- [DE] Documented in the README that Windows receives the most compatibility testing because it is the only platform currently available to the lead developer for local testing.

## [6.4.39_DE-1.33.117-nightly] - 2026-08-27

### Added
- [DE] When the Updates screen finds a new version, it now shows a "What's new" card with the pending release's changelog (fetched live from the repository, same source as About → Changelog), translated in all supported languages.

## [6.4.39_DE-1.33.116-nightly] - 2026-08-27

### Changed
- [DE] The official logo is now circular everywhere it appears: Windows/macOS/Linux app icons (`.ico`/`.icns`/`.png`), intro splash, tray icon, native notifications, installer wizard art, README and website.
- [DE] `scripts/generate_desktop_icons.py` applies an anti-aliased circular mask automatically, so future logo regenerations keep the round shape.

## [6.4.39_DE-1.33.115-nightly] - 2026-08-27

### Added
- [DE] Added a user-friendly YouTube Music login entry point with an optional embedded WebView attempt, persistent profile location and a system-browser/manual-cookie fallback on Windows, Linux and macOS.

## [6.4.39_DE-1.33.114-nightly] - 2026-08-27

### Added
- [DE] Update installers older than seven days are removed automatically while recent installers remain available for reuse.

## [6.4.39_DE-1.33.113-nightly] - 2026-08-27

### Changed
- [DE] Replaced the desktop branding in the intro, notifications, installers, app resources, website and README with the official `logo_vmde_official.jpg` artwork; regenerated the Windows, macOS, Linux and installer icon variants.

## [6.4.39_DE-1.33.112] - 2026-08-19

### Fixed

- [DE] The play/pause button could stay stuck on "play" while audio kept
  playing (and pausing then did nothing). The player's position callback was
  re-applying the initial `resumeWhenReady` intent as `isPlaying` on every
  ~100 ms report, so a manual play/pause toggle (e.g. after the phone started
  a track paused/resolving while paired) was overwritten the next tick. The
  intent is now applied only on the resolving→ready transition; later reports
  just advance the position.

## [6.4.39_DE-1.33.111] - 2026-08-19

### Added

- [APK] Complete translations for the Devices section in all 54 supported
  languages: Devices, Device sync, Relay server, Find desktop, Scan QR code,
  pairing code, Pair/Unpair, paired state and Sync VIVI volume (plus their
  descriptions). Previously the whole section fell back to English.
- [DE] A "How to connect your phone" step-by-step card on the Device sync
  screen (same Wi-Fi, Start LAN server, scan the QR code, confirm the code),
  translated in all 47 desktop languages and recommending the LAN server.

## [6.4.38_DE-1.33.110] - 2026-08-19

### Fixed

- [DE+APK] Player sync no longer stutters ("va a salti") when paired: the
  periodic drift tolerance was raised from 250 ms to 1 s, so the relay
  clock-offset jitter (especially on a phone hotspot) no longer triggers a
  forward catch-up seek every 5 s — only genuine drift is corrected.
- [DE] The desktop now waits for the phone while the phone is still resolving
  a new track (symmetric with the phone already waiting for the desktop):
  `applyRemotePlayback` holds playback on the peer's `isResolving` state and
  auto-resumes the moment the desktop's own stream is ready, without emitting
  a transient `isPlaying=false` snapshot that paused the phone. This stops the
  desktop from playing ahead/behind and jumping to catch up on a phone-initiated
  track change.

## [6.4.37_DE-1.33.109] - 2026-08-19

### Fixed

- [APK] Added `VISIONOS` and `ANDROID_NO_SDK` to the mobile fallback client
  chain, completing parity with the desktop edition's proven resolver. These
  clients cover the remaining music-only tracks that the generic clients still
  report as `Video non disponibile`.

## [6.4.36_DE-1.33.109] - 2026-08-19

### Fixed

- [APK] Some tracks still failed with `IO_UNSPECIFIED (2000): Video
  non disponibile`. The mobile fallback client chain was missing the
  music-specific clients (`IOS_MUSIC`, `ANDROID_MUSIC`) that resolve
  music-only / YouTube-Music-signed streams the generic clients report as
  "Video unavailable". These clients are now tried right after the main VR
  client, matching the desktop edition which plays those tracks reliably.

## [6.4.35_DE-1.33.109] - 2026-08-19

### Changed

- [DE] On Debian (and Debian-derived distros such as Ubuntu) the updater now
  prefers the `.deb` installer over the AppImage. Detection reads
  `/etc/os-release` (`ID`/`ID_LIKE` containing `debian` or `ubuntu`); other
  Linux distros keep the AppImage as the preferred installer.

## [6.4.35_DE-1.33.108] - 2026-08-19

### Fixed

- [APK] When every innerTube client is bot-flagged (`Video unavailable`), the
  mobile now falls back to a NewPipe-resolved stream URL instead of failing.
  NewPipe resolves its own signature independently, which is why the desktop
  edition (NewPipe-first) plays where the shared clients were refused. A
  minimal audio format is synthesized from the NewPipe itag so playback works
  without the client response.

## [6.4.34_DE-1.33.108] - 2026-08-19

### Fixed

- [APK] Stream downloads now send a browser-like `User-Agent` (Firefox),
  matching the UA already used to validate the URL and the one the desktop
  edition uses to download NewPipe URLs. The previous default media3 UA was
  getting the connection reset by the googlevideo CDN, surfacing as
  `IO_UNSPECIFIED (2000): Source error`. The on-screen playback error now also
  shows the nested cause (the real reason) instead of just the `Source error`
  wrapper.

## [6.4.33_DE-1.33.108] - 2026-08-19

### Fixed

- [APK] Mobile stream resolution now prefers a NewPipe-signed URL (matched to
  the selected audio format) over the shared ANDROID_VR client URL. NewPipe
  resolves its own signature with a browser-like user-agent, so its URLs are
  far less prone to the CDN bot-flagging that caused `IO_UNSPECIFIED (2000)`;
  the client URL remains the fallback. Mirrors the desktop edition's
  NewPipe-first resolver.

## [6.4.32_DE-1.33.108] - 2026-08-19

### Changed

- [APK] Simplified the mobile stream-retry path: extracted a shared
  `reResolveCurrentTrack` helper (removes the duplicated rotate/seek/prepare
  block) and deleted the no-op `YTPlayerUtils.forceRefreshForVideo` stub. No
  behavior change.

## [6.4.31_DE-1.33.108] - 2026-08-19

### Added

- [APK] A visible on-screen toast now appears when playback fails, showing the
  exact error code (e.g. `IO_UNSPECIFIED (2000)`) and message so stream
  failures can be identified without logcat. The same error is also appended to
  the playback log (Settings → Content → Playback logs).

## [6.4.30_DE-1.33.108] - 2026-08-19

### Fixed

- [APK] Stream-resolution retry now rotates the guest identity (visitorData)
  before re-resolving on a 403/IO error, so a bot-flagged or expired
  googlevideo URL isn't reused verbatim. This stops tracks failing to start,
  auto-skipping, or pausing by themselves in the first seconds — even when the
  phone is not paired to the desktop.

## [6.4.29_DE-1.33.108] - 2026-08-18

### Fixed

- [DE] Developer options RAM now shows the program's real usage instead of the
  JVM heap-only figure. A new "Memory · Process" row reports the process RSS /
  working set (via psapi on Windows, `/proc/self/status` on Linux, committed
  heap+non-heap elsewhere); heap and system RAM remain as detail rows in the
  Full profile. Title-bar and overlay pill RAM also use the real figure.
- [DE] Developer options overlay switches (movable / show in title bar) now
  appear only when "Overlay" display mode is selected.

## [6.4.29_DE-1.33.107] - 2026-08-18

### Added

- [DE] Look-ahead cache prefetch: while a track is loaded (playing or paused)
  the next 3 tracks are resolved and downloaded to the audio cache in the
  background, and their lyrics are fetched and cached too (in-memory + on-disk
  under `~/.vivimusic/cache/lyrics`). The Lyrics screen now reads the cache
  first instead of re-fetching on every open.

## [6.4.29_DE-1.33.106] - 2026-08-18

### Fixed

- [DE] Songs failing to start or "ending" after a few seconds when the stream
  cache is set to "Forever". Two causes: (1) a retry re-used the same expired
  googlevideo URL because the resolver cached it forever — a failed stream now
  evicts its cache entry so the retry re-resolves a fresh URL; (2) `isValidMp4`
  only checked the `ftyp` header, so a truncated cache file was reused and the
  track "finished" early (auto-skip or auto-pause) — it now also requires at
  least one `moof` box and re-downloads on any parse error.

## [6.4.29_DE-1.33.105] - 2026-08-18

### Fixed

- [DE] Backspace no longer navigates back while typing in a search/text field.
  The global shortcut used the preview (tunnel) key phase, so it swallowed the
  key before the focused text field saw it. It now uses the bubble phase, so a
  text field consumes Backspace (and Ctrl+Z/Ctrl+Y for text undo) first; the
  shortcut only fires when no text field handled the key.

## [6.4.29_DE-1.33.104] - 2026-08-18

### Fixed

- [DE] A failed track auto-skipped through the whole queue. `AudioPlayer` fired
  `onComplete` (in a `finally`) even after an error, so `PlayerController`
  treated every failure as "track ended" and advanced to the next one. `onComplete`
  now fires only on a normal finish, so a 403/resolution failure retries and then
  stops instead of looping. This also stopped the constant queue/resolving churn
  that was flooding the sync channel and suppressing play/pause sync.

## [6.4.29_DE-1.33.103] - 2026-08-18

### Fixed

- [DE] Song recognition (Shazam) always returned "No match found". The desktop
  recorder assumed a fixed 44.1 kHz mono little-endian capture and resampled
  with naive linear interpolation, whose aliasing corrupted the spectral band
  Shazam fingerprints. It now reads the mic's actual negotiated format (rate,
  channels, endianness), downmixes to mono, and uses a band-limited sinc + Hann
  anti-aliased resampler.

## [6.4.29_DE-1.33.102] - 2026-08-18

### Added

- [DE] Settings search: a search icon sits next to the "Settings" title; it
  expands into a text field that live-filters the settings list by title and
  subtitle, with a "no results" state.

## [6.4.29_DE-1.33.101] - 2026-08-18

### Fixed

- [DE+APK] Players no longer freeze/pause at random while paired. The mobile
  marked `isResolving` on every mid-song rebuffer, and both devices paused the
  other whenever the peer was "resolving"; now a same-track `isResolving` only
  skips the stale-position re-sync and keeps playing (initial track resolution
  still holds the peer until audio actually starts).

## [6.4.28_DE-1.33.100] - 2026-08-18

### Changed

- [DE] Rebuilt the intro splash from scratch as a fully native Compose animation
  (logo fade/scale-in with a breathing pulse) instead of playing a frame
  sequence extracted from the MP4. Removed the bundled JPEG frames and the
  extraction tool. Two new selectors in the Intro screen: **Intro content**
  (Logo only / Logo + app name / Logo + name + version) and **Background**
  (Gradient / Glow / Dark). The original `desktop/icons/*.mp4`/`.gif` files are
  kept as source assets only.

## [6.4.28_DE-1.33.99] - 2026-08-18

### Added

- [DE] "Preview intro" button in the Intro screen: plays the startup intro
  fullscreen (click or end dismisses it) without restarting.

### Changed

- [DE] In Settings → Appearance the intro is now a dedicated entry ("Intro") that
  opens the Intro sub-screen, instead of a raw toggle in the Appearance list.

## [6.4.28_DE-1.33.98] - 2026-08-18

### Fixed

- [DE] Intro splash now plays with correct colours and smooth fps. The frames
  were extracted with a limited-range BT.601 YUV→RGB formula while jcodec
  outputs full-range YUV420, which shifted/clamped every channel (the "wrong
  colours"). Re-extracted with the correct full-range matrix, downscaled to
  960×540, and the player now decodes each JPEG with Skia
  (`Image.makeFromEncoded`) instead of `ImageIO` + a pixel copy, so 30 fps
  stays smooth.

## [6.4.28_DE-1.33.97] - 2026-08-18

### Added

- [DE] "Show intro on startup" toggle also available in Settings → Appearance
  (same setting as System → Intro).

## [6.4.28_DE-1.33.96] - 2026-08-18

### Fixed

- [DE] Changing the app font now actually changes the whole interface: the
  selected font was only used in the App font picker preview, never applied to
  the theme. `AppTheme` now builds the Material typography from the selected
  `AppFont` (hoisted to the app root and passed to both the main window and
  the developer-tools window), so every text style updates immediately.

## [6.4.28_DE-1.33.95] - 2026-08-18

### Changed

- [DE] Full redesign of the Developer options screen: a prominent enable
  card, a live monitor card with real-time CPU / memory / GPU / network / thread
  / uptime tiles (respecting the Full vs Performance profile), display mode and
  profile as radio groups, and the overlay / title-bar switches grouped in a
  card. Functionality is unchanged (same settings, same persistence).

## [6.4.28_DE-1.33.94] - 2026-08-18

### Changed

- [DE] The startup intro is no longer a GIF: it now plays a sequence of
  full-color JPEG frames pre-extracted from the MP4 (`scripts/ExtractIntroFrames.java`).
  The GIF showed visible color banding (256-color limit); the JPEG sequence
  keeps the original quality while staying smooth and cross-platform (no native
  video decoder needed).

## [6.4.28_DE-1.33.93] - 2026-08-18

### Fixed

- [DE] Back navigation no longer bounces between repeated screens: navigating
  to the screen already on top is ignored (no duplicate stack entries), and
  sidebar roots now keep Home at the base so pressing back from Settings
  returns to Home instead of getting stuck.

## [6.4.28_DE-1.33.92] - 2026-08-18

### Added

- [DE] Animated intro splash screen (the bundled GIF) played once at startup,
  with click-to-skip and a toggle in Settings → System → Intro.
- [DE] Settings → System sub-menu: Developer options moved here (under
  System → Developer options) alongside the new Intro option.

### Removed

- [DE] Removed the changelog button from the About screen (still available in
  Settings → Updates).

## [6.4.28_DE-1.33.91] - 2026-08-18

### Removed

- [DE] Removed the standalone Stats screen (duplicate of VIVI Wrapped) and
  replaced its sidebar entry with a direct link to VIVI Wrapped.

## [6.4.28_DE-1.33.90] - 2026-08-18

### Added

- [DE] Completed translations for the Phase 10 port across all 47 languages:
  Listen Together (title, descriptions, room code, leave room, connected
  users), song recognition (recognize, listening, history, error), Stats /
  Charts / New releases / Mood & genres, comments, username, and the
  desktop-only Undo / Redo / Items labels.

## [6.4.28_DE-1.33.89] - 2026-08-18

### Added

- [DE] Artist page sub-tabs: Songs / Albums / Items (TabRow). Items loads
  the section's "see all" endpoint via `YouTube.artistItems`.
- [DE] Song recognition (Shazam): records the microphone, resamples to
  16 kHz mono, generates a Shazam fingerprint (pure-JVM port) and queries
  `shazamkit`. Recognitions are saved to a history list in Settings.
- [DE] Keyboard navigation: Ctrl+Z / Ctrl+Y undo/redo the screen history,
  and Backspace or Alt+Left go back.

## [6.4.28_DE-1.33.88] - 2026-08-18

### Added

- [DE] New Release albums screen (grid of the latest albums, from the
  sidebar).
- [DE] Charts screen (trending + top songs/videos sections, from the
  sidebar).
- [DE] Dedicated Mood & genres screen (full list instead of the Home preview).
- [DE] Stats screen (session listening time, tracks played and top songs,
  from the sidebar).
- [DE] Auto-playlist detail screens: Liked / Top / etc. now open as their
  own pushed screen (`AutoPlaylistScreen`) instead of being a Library filter.
- [DE] Listen Together screen: a JSON WebSocket client (same protocol as the
  mobile app) with create/join room, connected users, host join-request
  approval and a chat panel.

## [6.4.28_DE-1.33.87] - 2026-08-18

### Added

- [DE] Commit screen (Settings → Updates → Commits): lists the most recent
  commits of the selected update source branch (fork `vivi-music-de` or
  original `main`), with author, date, short SHA and avatar. Clicking a commit
  opens it on GitHub.

## [6.4.28_DE-1.33.86] - 2026-08-18

### Fixed

- [DE] The Queue swipe-to-play hint ("▶ Play") no longer stays visible on top
  of the song artwork: the hint (background + text) now fades in only while
  dragging right, so it is fully hidden when the row is idle.

## [6.4.28_DE-1.33.85] - 2026-08-18

### Fixed

- [DE] Player sync (seek bar + play/pause) now follows the peer even over a
  phone hotspot: a seek/play command that arrived while the desktop was still
  resolving its own stream was dropped (our own resolution decides when audio
  starts) and only recovered on the next 5s re-sync tick. The latest peer
  snapshot is now re-applied the moment our stream finishes resolving, so the
  slower resolution over a hotspot no longer leaves seek/play-pause unsynced.

## [6.4.28_DE-1.33.84] - 2026-08-18

### Added

- [DE] Stream cache now offers a "Forever" option past 60 minutes: the resolved
  stream URL is kept for the whole app session instead of expiring.

## [6.4.28_DE-1.33.83] - 2026-08-18

### Changed

- [DE] The player design variants now actually differ: Classic is the
  two-column layout, New is a single-column hero with a pill play button,
  V2 keeps two columns with the title overlaid on the artwork, and Expressive
  is a single-column hero with the title overlaid on the largest artwork.

## [6.4.28_DE-1.33.82] - 2026-08-18

### Changed

- [DE] UI density scale now also offers values above 100% (110, 120, 125, 130,
  140, 150, 180, 200) in addition to the existing 100/85/75/65/55%.

## [6.4.28_DE-1.33.81] - 2026-08-18

### Fixed

- [DE+APK] The "Sync VIVI volume" toggle is now part of the shared settings
  snapshot, so enabling/disabling it on either device reflects on the other.

## [6.4.27_DE-1.33.80] - 2026-08-18

### Changed

- [DE] The VIVI Wrapped card is now hidden from the Home screen by default.
  It can be re-enabled via Settings → VIVI Wrapped → "Show on Home".

## [6.4.27_DE-1.33.79] - 2026-08-18

### Added

- [DE] Settings → VIVI Wrapped sub-screen: the session listening-stats card
  now lives in its own settings sub-menu, like the mobile app. The Home card
  stays as a quick glance.
- [DE] Appearance now hosts the player personalization: a new "Player
  design" row (Material 3 style) opens the design / background / rotating
  thumbnail / mini-player style screen.

### Fixed

- [DE] Raw keys no longer appear as "code language" UI: 10 keys were missing
  from the desktop string table (`remove_from_queue`, `pause_search_history`,
  `pause_listen_history`, `quick_picks`, `search_history`, `listen_history`,
  `clear_search_history`, `clear_search_history_confirm`, `theme`, `ok`) and
  are now wired to the Android translations (fallback OK).
- [DE] Queue screen: the swipe-left remove hint ("✕ remove from queue")
  duplicated the row's X button; the hint was removed — the X is now the
  single remove control (swipe-left still works).
- [DE] Full player: add-to-playlist now sits under the song title, next to
  the Queue button; the duplicate header Queue shortcut and the old
  bottom-row buttons were removed.
- [DE] Startup volume guard: if the Windows master volume is muted, VIVI
  Music DE unmutes it and sets it to 0% so a paired mobile device can always
  control it (a muted master ignores volume writes).

### Removed

- [DE] Quick settings (Tune) button in the sidebar.

## [6.4.27_DE-1.33.78] - 2026-08-18

### Changed

- [DE] The stream resolution cache TTL is now configurable: a slider in
  Settings → Player & audio (1–60 minutes, default 10) controls how long a
  resolved stream URL is reused before the resolution chain runs again.
  (Resolution only — the audio decode/playback core is untouched.)

## [6.4.27_DE-1.33.77] - 2026-08-17

### Changed

- [DE] Stream resolution cache: resolved audio URLs are cached in memory for
  up to 10 minutes, so replaying or retrying a track doesn't re-run the whole
  resolution chain when we already have a valid stream. (Resolution only —
  the audio decode/playback core is untouched.)

## [6.4.27_DE-1.33.76] - 2026-08-17

### Added

- [DE] Integrations sub-screen (Settings → Integrations):
  - Discord Rich Presence over the local IPC pipe (Windows; toggle + your
    Discord application ID; shows the current track).
  - Last.fm scrobbling: enable toggle, session key field, now-playing
    update and auto-scrobble near the end of each track (credentials via
    the LASTFM_API_KEY / LASTFM_SECRET env vars, like mobile build config).
  Fully translated.

## [6.4.27_DE-1.33.75] - 2026-08-17

### Added

- [DE] Advanced lyrics: line-spacing slider (1.0–2.0, Settings → Lyrics)
  and a thumbnail with play/pause overlay on the Lyrics screen (port of the
  mobile advanced-lyrics controls). Fully translated.

## [6.4.27_DE-1.33.74] - 2026-08-17

### Added

- [DE] Quick settings popup: a Tune button at the bottom of the sidebar
  opens a compact panel with theme (System/Light/Dark), pure black toggle,
  accent swatches and a shortcut to the full Appearance settings (port of
  the mobile quick-settings shortcut). Fully translated.

## [6.4.27_DE-1.33.73] - 2026-08-17

### Added

- [DE] Local search history: recent searches appear as chips on the Search
  screen (saved on submit / suggestion click, max 12) with a clear button.
- [DE] Privacy sub-screen (Settings → Privacy): "Pause listen history"
  (hides the History screen from the sidebar) and "Pause search history"
  (stops saving new searches) toggles + "Clear search history".
  Fully translated.

## [6.4.27_DE-1.33.72] - 2026-08-17

### Added

- [DE] Home: "Quick Picks vs Last Listen" toggle (chip row) so only the
  chosen section shows, like on mobile.
- [DE] Home: "Randomize" button that shuffles the order of the home
  sections (persisted).
- [DE] Home: "VIVI Wrapped · This session" card with tracks played,
  listening time and top song of the current session. Fully translated.

## [6.4.27_DE-1.33.71] - 2026-08-17

### Added

- [DE] Sort chips in the Library (all tabs): A–Z / Z–A, plus "By artist"
  for the songs tab. Fully translated.

## [6.4.27_DE-1.33.70] - 2026-08-17

### Added

- [DE] Dynamic theme (Material You): the "Dynamic" accent swatch now reads
  the OS accent color instead of a fixed seed — Windows DWM accent, macOS
  accent (defaults), GNOME accent (gsettings) — with fallback to the default
  palette. Re-detected each time Dynamic is picked.

## [6.4.27_DE-1.33.69] - 2026-08-17

### Added

- [DE] Song swipe gestures in the Queue screen: swipe a row right to play
  it, swipe left to remove it from the queue (action hints appear behind
  the row while dragging). Fully translated.

## [6.4.27_DE-1.33.68] - 2026-08-17

### Added

- [DE] Mini-player styles: Standard / Apple / Outline / Pure black
  (Settings → Player & audio → Player design → Mini player), replacing the
  old Apple-only toggle. Fully translated.
- [DE] Swipe-to-expand on the mini player: drag it up to open the full
  player (with a drag handle hint at the top).

## [6.4.27_DE-1.33.67] - 2026-08-17

### Added

- [DE] Player design variants: Classic / New / V2 / Expressive (Settings →
  Player & audio → Player design), reworking the full-player layout and the
  Apple Music-style rounded mini-player variant. Fully translated.
- [DE] Player background styles: Gradient / Blur / Glow / Apple Music /
  Live mesh (Settings → Player & audio → Player design), animated behind
  the full player. Fully translated.
- [DE] Rotating artwork option (settings toggle): the album art rotates
  slowly while playing, like the mobile rotating-thumbnail option.

## [6.4.27_DE-1.33.66] - 2026-08-17

### Added

- [DE] Player slider styles: Slim / Squiggly / Wavy (Settings → Player & audio),
  applied to the seek bar and the volume slider via a custom `ViviSlider`.
  Fully translated.

## [6.4.27_DE-1.33.65] - 2026-08-17

### Added

- [DE] Screen transitions between navigations: Off / Fade / Slide (Settings →
  Appearance → Screen transitions), applied with `AnimatedContent` around the
  main screen switch. Fully translated.

## [6.4.27_DE-1.33.64] - 2026-08-17

### Added

- [DE] UI density scale (100 / 85 / 75 / 65 / 55 %) applied to the whole
  interface via a density override (Settings → Appearance → Density & grid),
  plus a custom adaptive grid item size (small / medium / large / extra
  large) used by the album / artist / playlist grids. Fully translated.

## [6.4.27_DE-1.33.63] - 2026-08-17

### Fixed

- [DE] The desktop no longer stays silent (appears in the Windows mixer but
  emits no sound) when a synced track change starts. The desktop held
  (`startPaused`) whenever the peer was still resolving, and the peer held for
  the desktop's own resolution, so both paused and neither ever started. The
  desktop now starts when the peer says it is playing and ignores the peer's
  play/pause echoes while it is still resolving its own stream; the phone keeps
  holding for the desktop and both resume together once the desktop is ready.

## [6.4.27_DE-1.33.62] - 2026-08-17

### Fixed

- [DE+APK] A track change initiated from the phone no longer plays ahead of
  the desktop while the desktop is still resolving its stream. The
  resolving/ready transition was being swallowed by the 1.5s echo-suppression
  window that runs after applying a remote snapshot, so the desktop's
  `isResolving=true` push never reached the phone. Resolving transitions now
  bypass echo suppression on both sides, so the phone holds while the desktop
  buffers and resumes the moment the desktop is ready (and vice versa).

## [6.4.26_DE-1.33.61] - 2026-08-17

### Fixed

- [APK] Restore swap is no longer silent on failure: the database target
  directory is created if missing (clean install), and if the staged
  settings/database copy fails the staged backup is kept and the error is
  logged (with a stack trace) so the restore can be retried on the next launch
  instead of the backup being deleted without being applied.

## [6.4.25_DE-1.33.61] - 2026-08-17

### Fixed

- [APK] The restore picker now accepts any file (`*/*`) so old `.backup` files
  created by the original 6.0.5 app (which have no registered MIME type) always
  appear in the file selector instead of being hidden/unselectable.

## [6.4.24_DE-1.33.61] - 2026-08-17

### Fixed

- [DE+APK] Playback start is now synchronized while a device is still resolving
  its stream. The desktop marks the snapshot as `isResolving` from the moment it
  starts resolving until audio actually flows (first position report), and the
  mobile marks it while ExoPlayer is `STATE_BUFFERING`. The receiver now
  prepares the queue but holds playback (instead of playing ahead of the peer),
  and `effectivePosition` no longer extrapolates a frozen position while the
  peer is resolving. This fixes the phone starting the track before the desktop
  had finished resolving/downloading.

## [6.4.23_DE-1.33.60] - 2026-08-17

### Fixed

- [APK] Update check from the fork source (`PiBOH/vivi-music-de`) now works:
  the updater extracts the mobile version from the combined desktop tag
  (`6.4.22_DE-1.33.60-nightly` → `6.4.22`) before comparing, and accepts the
  fork's `VIVIMusic-<version>-debug.apk` asset instead of only `vivi.apk`.

## [6.4.22_DE-1.33.60] - 2026-08-17

### Fixed

- [DE] Native Windows toast notifications now actually appear in the Action
  Center. The AUMID registration was failing with `0x80070057` because the
  `SHGetPropertyStoreFromParsingName` P/Invoke was missing the
  `GETPROPERTYSTOREFLAGS flags` parameter, and the `PROPVARIANT` was declared
  as a sequential struct instead of an explicit-layout class. Both are fixed,
  so the Start-menu shortcut gets its `System.AppUserModel.ID` correctly.

## [6.4.22_DE-1.33.59] - 2026-08-17

### Fixed

- [DE] Developer options network stats (down/up speed + total traffic) now show
  real values on non-English Windows. They were parsing the localized
  `netstat -e` output ("Byte" / "Ricevuti"/"Trasmessi" instead of "Bytes"),
  which never matched and left the values at "—". Replaced with
  `Get-NetAdapterStatistics` (culture-invariant property names).

## [6.4.22_DE-1.33.58] - 2026-08-17

### Fixed

- [DE+APK] Playback sync no longer "jumps back": explicit user seeks are now
  flagged and applied exactly on the peer (both directions, no tolerance),
  while the periodic drift-tic only catch up FORWARD. This stops the device
  that is slightly ahead (the leader) from being dragged back every 5s by the
  follower's stale position, which was the visible seekbar jump-back.

## [6.4.21_DE-1.33.57] - 2026-08-17

### Fixed

- [DE] The in-app update notification no longer auto-dismisses while it is
  showing the download progress bar; the timer pauses during a download and
  resumes after it finishes.

## [6.4.21_DE-1.33.56] - 2026-08-17

### Added

- [DE] A "Send test notification" button in Settings → Notifications so native
  notifications can be triggered on demand.

### Changed

- [DE] Native notification path now writes a diagnostic log to
  `~/.vivimusic/native-notify.log` (which branch is used, AUMID registration
  result, and PowerShell output) to help diagnose Windows toast issues.

## [6.4.21_DE-1.33.55] - 2026-08-17

### Changed

- [DE] The About "website" entry now points to the VIVI Music DE GitHub Pages
  site (`https://piboh.github.io/vivi-music-de/`).

### Website

- Made the site fully responsive for mobile (collapsible hamburger nav,
  stacking download rows / platform cards).
- Compact sticky footer (always pinned to the bottom of the viewport).
- Removed the Android APK download from the DE site; it now links to the
  upstream VIVI Music site, with credits to VIVIDH P ASHOKAN
  (`https://vivimusic.mkmdevilmi.workers.dev/`).

## [6.4.21_DE-1.33.54] - 2026-08-17

### Fixed

- [DE] Tracks restored from the persistent queue (or whose load failed earlier)
  now actually start on the first Play press: pressing play on a track whose
  stream is not loaded yet triggers a real resolution + load instead of a
  no-op `resume()` that silently did nothing.

## [6.4.21_DE-1.33.53] - 2026-08-17

### Added

- [DE] Windows native notifications now land in the **Action Center / notification
  history** via WinRT toasts (PowerShell helper). On a packaged Windows build the
  app registers an AppUserModelID by creating a Start-menu shortcut with the
  `System.AppUserModel.ID` property (inline C# `Add-Type` + shell property
  store), then shows `ToastGeneric` toasts with the VIVI Music DE logo.
  Clicking a toast launches the app with `--open=<section>` and opens the
  relevant screen (Updates / Developer options / Devices), bringing the window
  to the front; a file-based command mailbox forwards the request to an
  already-running instance. Non-Windows and unpackaged/dev builds keep the
  `SystemTray` balloon fallback.

## [6.4.21_DE-1.33.52] - 2026-08-17

### Changed

- [DE] `version.txt` is reorganized into a self-documenting six-line layout:
  mobile version / mobile version code / mobile channel, then DE version / DE
  version code / DE channel, with comment lines below explaining each field.
  `desktop/build.gradle.kts`, `AppInfo`, and the release/build workflows now
  read the new positions (DE version = line 4, DE channel = line 6, DE version
  code = line 5).

## [6.4.21_DE-1.33.51] - 2026-08-17

### Fixed

- [DE] Opening Appearance → Theme & colors, App font, or Canvas no longer
  crashes with "Vertically scrollable component was measured with an infinity
  maximum height constraints". Those sub-screens had their own
  `verticalScroll` nested inside the settings screen's scrollable scaffold; the
  inner scroll was removed so the content scrolls with the outer scaffold only.

## [6.4.21_DE-1.33.50] - 2026-08-17

### Added

- [APK] Restoring a backup now shows a confirmation dialog with the backup's
  file name, date, and the app version it was created from, before anything is
  applied.

## [6.4.20_DE-1.33.50] - 2026-08-17

### Added

- [APK] Restore now validates the backup's database before applying it (SQLite
  header magic + `PRAGMA integrity_check` + schema-version guard). A corrupt or
  incompatible backup fails with a clear "backup is corrupt" message instead of
  swapping in a bad file and crashing the app on the next launch.

## [6.4.19_DE-1.33.50] - 2026-08-17

### Fixed

- [DE+APK] Playback position no longer jumps back and forth between the two
  devices. The shared-clock offset used to extrapolate the live position was
  only measured 25 s after connecting and converged slowly via a running
  average, so during that window both devices extrapolated from raw local
  clocks and kept seeking each other back/forth by the clock skew. The first
  PING is now sent immediately on connect, the first measurement sets the
  offset directly, and a position is only timestamped once the offset is known
  (older relays fall back to the raw position instead of a skew-corrupted
  extrapolation).

## [6.4.18_DE-1.33.49] - 2026-08-17

### Added

- [DE] Complete port of the mobile Appearance sub-menu into three dedicated
  sub-screens: **Theme** (4-mode selector System/Light/Dark/Pure black, the
  full 21-color accent palette and a live preview card), **App font** (the five
  mobile fonts — System, Google Sans, Sans Flex, Outfit, Plus Jakarta Sans —
  bundled into the desktop resources with a live typography preview) and
  **Canvas** (enable toggle + source Auto / Apple Music / ViViMusic / Tidal,
  wired into the player's animated background). All new strings reuse the
  Android translations (47 languages).

## [6.4.18_DE-1.33.48] - 2026-08-17

### Fixed

- [DE+APK] Playlist changes made on the desktop now actually reach the phone.
  The mobile side was stamping the local "now" into `lastUpdateTime` when
  applying a remote playlist, so the next desktop rename/delete compared newer
  than an artificially-updated timestamp and was silently dropped by the
  last-write-wins check. The remote edit timestamp is now preserved, so create /
  rename / delete all propagate.

## [6.4.17_DE-1.33.48] - 2026-08-17

### Fixed

- [APK] Restoring an old backup (e.g. from the original 6.0.5) no longer crashes.
  The restore previously closed the shared Room database while the app's live
  queries were still running, which crashed with an uncaught "database is
  closed" exception. It now stages the backup to `filesDir/pending_restore`,
  exits, and swaps the settings + database in at startup (in `App.onCreate()`)
  before Room/DataStore are opened.

## [6.4.16_DE-1.33.48] - 2026-08-17

### Changed

- [APK] Mobile backups now use the `.vividroid.backup` extension (desktop keeps
  `.vivide.backup`), so the two editions' backup files are clearly
  distinguishable. Older `.backup` files are still listed and importable.

## [6.4.15_DE-1.33.48] - 2026-08-17

### Fixed

- [DE] The Inno Setup installer now actually launches the app when "Start VIVI
  Music DE" is checked on the final page. The `[Run]` entry was gated on both
  the final-page checkbox *and* a separate (unchecked) `launchafterinstall`
  task, so the app never started; the redundant task is removed and the
  final-page checkbox alone controls the launch.

## [6.4.15_DE-1.33.47] - 2026-08-17

### Changed

- [APK] Mobile backup files now use the `.vivide.backup` extension (manual and
  automatic) to match the desktop edition. Older `.backup` files are still listed
  and importable, so nothing is lost.

## [6.4.14_DE-1.33.47] - 2026-08-17

### Fixed

- [APK] The debug APK build (CI) no longer fails during resource merge: the new
  `sync_vivi_volume_desc` string used a bare apostrophe that aapt2 rejected as an
  "Invalid unicode escape sequence"; it is now escaped (`\'`) like the rest of the
  Android strings, so `assembleUniversalGmsDebug` completes again.

## [6.4.13_DE-1.33.47] - 2026-08-17

### Added

- [DE+APK] New "Sync VIVI volume" toggle (Settings → Devices and Settings →
  Player & audio, on both editions). When off, each device keeps its own
  in-app volume slider independent; the native OS (system) volume sync is
  unaffected.

### Fixed

- [DE] The seek slider no longer stays disabled or stuck at the end: the track
  duration is reported as soon as it is known (before the stream resolves) and
  the live position is clamped to the track length so it can't overshoot.
- [DE] Position sync no longer fights itself: the periodic re-sync tick only
  pushes when the position actually advanced, so a stalled/frozen player can't
  repeatedly drag the paired device back to the same point.

## [6.4.12_DE-1.33.46] - 2026-08-16

### Fixed

- [APK] Restoring a backup no longer crashes while choosing the file or when
  reopening the app: the archive is decompressed on a background thread and
  staged to temp files, then the settings + database are swapped in and the
  process is killed in a single synchronous block on the main thread. This
  removes the race where the UI queried the database after it was closed on a
  background thread (the crash introduced by the previous fix), while still
  deleting the WAL/SHM sidecars so the restored DB isn't corrupted on launch.

## [6.4.11_DE-1.33.46] - 2026-08-16

### Fixed

- [DE] All in-app notifications now auto-dismiss after the configured time
  (Settings → Notifications → In-app notification duration): the update
  banner and the developer-options-unlocked hint previously stayed on screen
  until dismissed manually, ignoring the setting.
- [DE] Native notifications keep a single persistent tray icon (created once
  with the VIVI Music DE logo) instead of adding/removing a temporary icon per
  notification, so the logo shows reliably and the icon is scaled with
  high-quality interpolation.

## [6.4.11_DE-1.33.45] - 2026-08-16

### Fixed

- [DE] LAN sync now works when the computer is connected to the phone's
  hotspot: the desktop advertises the address of the interface that actually
  routes to the phone (resolved via the outbound-route source address, then
  preferring Wi-Fi/wlan adapters) instead of the first site-local address,
  which on multi-homed machines was often a virtual adapter the phone could
  not reach. Start/stop of the relay is now serialized and the bound-port
  lookup is guarded, so rapid Stop→Start (or a failed bind) no longer throws
  and crashes the app — failures surface in the status line instead.

## [6.4.11_DE-1.33.44] - 2026-08-16

### Changed

- [DE] Manual backups now include the date and timestamp in their filename
  (`vivimusic-de_yyyyMMdd_HHmmss.vivide.backup`) instead of a fixed
  `vivimusic-de.vivide.backup`. Automatic backups already carried the
  timestamp, and the stored-backups list shows it as `yyyy-MM-dd HH:mm`.

## [6.4.11_DE-1.33.43] - 2026-08-16

### Fixed

- [APK] Restoring a backup no longer freezes and then corrupts the app: the
  restore (settings + DB copy) now runs off the main thread instead of blocking
  the UI, and the WAL/SHM sidecar files are deleted before overwriting the
  Room database, so a restored `song.db` is no longer mixed with stale journal
  frames (which corrupted the DB on the next launch and required an
  uninstall/reinstall). Playback is stopped before the database is touched.

## [6.4.10_DE-1.33.43] - 2026-08-16

### Added

- [DE+APK] Selectable update source: pick whether update checks read from the
  original repo (`vivizzz007/vivi-music`) or the PiBOH fork
  (`PiBOH/vivi-music-de`). Desktop defaults to the fork, mobile defaults to the
  original. The source is also used for the download/notification URLs.

## [6.4.9_DE-1.33.42] - 2026-08-16

### Added

- [DE] The crash/error dialog now has a "Copy error" button alongside "OK":
  it copies the full message + stack trace to the clipboard. A global
  uncaught-exception handler replaces the default AWT "Error" dialog (OK only).

## [6.4.9_DE-1.33.41] - 2026-08-16

### Changed

- [DE] Backups (manual and automatic) now use a single `.vivide.backup` file
  that contains everything (settings + playlists + account + library). Old
  `.backup` files are still importable.

### Fixed

- [DE] "Restart now" after restoring a backup now actually relaunches the app:
  it releases the single-instance lock, starts a new instance (the jpackage
  launcher when packaged, `java -cp … MainKt` in dev), and then exits.

## [6.4.9_DE-1.33.40] - 2026-08-16

### Fixed

- [DE] Native system notifications now use the real VIVI Music DE logo as their
  icon instead of a placeholder glyph. `logo_vmde.png` is bundled under
  `images/` and loaded for the tray icon (scaled, with a fallback).

## [6.4.9_DE-1.33.39] - 2026-08-16

### Added

- [DE+APK] Repeat mode (off / all / one) and shuffle now sync in real time
  between the phone and the desktop, both ways, like the rest of the playback
  state. `PlaybackSnapshot` carries `repeatMode` ("OFF"/"ALL"/"ONE") and
  `isShuffle`; each side applies them on receive and pushes them on change.

## [6.4.8_DE-1.33.38] - 2026-08-16

### Fixed

- [DE+APK] Queue sync now follows last-write-wins like playlists: `PlaybackSnapshot`
  carries a `queueUpdatedAt` timestamp (shared relay-time frame) and each side
  only replaces its queue when the remote edit is newer, so a mobile edit can't
  be overwritten by an older desktop queue (and vice versa). Older peers
  (`queueUpdatedAt = 0`) still apply unconditionally for compatibility.

## [6.4.7_DE-1.33.37] - 2026-08-16

### Fixed

- [DE] The update notification and the Updates screen now share a single
  download state (`UpdateState`), so downloading/opening an installer from one
  is reflected in the other (and vice versa). The notification no longer
  re-offers a download for an installer the Updates screen already fetched.

## [6.4.7_DE-1.33.36] - 2026-08-16

### Added

- [DE] Full backup & restore, ported from mobile: a backup now includes
  settings, playlists, account/login and library (ZIP with `settings.json` +
  `playlists.json`), and old single-JSON `.backup` files are still importable.
- [DE] Automatic backups: optional weekly backup and an optional "backup before
  update" that runs automatically before opening the installer. Automatic
  backups are stored under `~/.vivimusic/backups/` and can be restored or
  deleted from Settings → Backup.

### Changed

- [DE] Developer options screen reorganized into clear sections (display,
  monitoring profile, overlay behaviour, title bar) separated by dividers.

## [6.4.7_DE-1.33.35] - 2026-08-16

### Fixed

- [DE] Settings (e.g. where notifications are shown) no longer get forgotten on
  restart or update: `DesktopSettings` now saves through an atomic
  read-modify-write (`update`) instead of `save(load().copy(…))`, which could
  race between the UI thread and the device-sync IO coroutines and silently
  drop the value the user had just changed.

## [6.4.7_DE-1.33.34] - 2026-08-16

### Fixed

- [DE] The player seek slider no longer snaps to the start or the end: it is
  disabled until the track duration is known (so it can't degenerate into a
  0..1 range) and, while dragging, the live playback position is ignored so it
  can't fight the drag and yank the thumb back.

## [6.4.7_DE-1.33.33] - 2026-08-16

### Fixed

- [DE+APK] The two devices no longer unpair when one screen turns off: while
  paired, the Android app keeps the screen on (`FLAG_KEEP_SCREEN_ON`) and the
  desktop keeps the display/system awake (kernel32 `SetThreadExecutionState` on
  Windows, `caffeinate` on macOS). This stops the OS from sleeping the display
  and suspending the network, which was tearing down the sync socket.

## [6.4.6_DE-1.33.32] - 2026-08-16

### Changed

- [DE] The CI debug build now restores a persistent debug keystore from the
  `DEBUG_KEYSTORE` GitHub secret instead of generating a fresh key on every
  run, so the debug APK keeps the same signature and can be installed over the
  previous build without uninstalling first. When the secret is absent the
  workflow still falls back to generating a fresh key, so CI never breaks.

## [6.4.6_DE-1.33.31] - 2026-08-16

### Fixed

- [APK] Scanning a QR code now first disconnects and un-pairs an existing
  desktop connection, so the new code can pair to a (possibly different)
  desktop from a clean slate.

## [6.4.5_DE-1.33.31] - 2026-08-16

### Changed

- [DE] On Windows the updater now prefers the Inno Setup `.exe` installer over
  the `.msi` (lighter and more user-friendly). The `.msi` remains the fallback
  when a release has no `.exe`.

## [6.4.5_DE-1.33.30] - 2026-08-16

### Fixed

- [DE] The Updates screen no longer offers to re-download an installer that is
  already on disk: it now detects the previously-downloaded file for the
  available version and shows "Open installer" directly (the update banner
  already behaved this way).

## [6.4.5_DE-1.33.29] - 2026-08-16

### Added

- [DE] Notification history: every notification (in-app and native) is recorded
  and can be reviewed from Settings → Notifications → Notification history, with
  a "Save notification history" toggle and a "Clear history" action.
- [DE] Configurable in-app notification auto-dismiss (3/5/10/15/30 seconds,
  default 5s) in Settings → Notifications.

## [6.4.5_DE-1.33.28] - 2026-08-16

### Changed

- [DE] Windows system volume now drives the **master** volume (the speaker icon
  in the tray) via WASAPI `IAudioEndpointVolume` instead of WinMM, which only
  moved the per-app `VIVIMusic` mixer entry. The app's own session is now
  pinned to 100% so the mixer never quiets VIVI under the master. Sync with
  the phone remains bidirectional (channel `systemVolume`).

## [6.4.5_DE-1.33.27] - 2026-08-16

### Fixed

- [DE] The Linux `.deb` now installs on Debian: jpackage auto-detected
  dependencies on ubuntu-latest and emitted Ubuntu's `t64`-renamed package
  names (e.g. `libasound2t64`, `libglib2.0-0t64`) that don't exist on Debian
  Bookworm. A post-build step rewrites them to `<name> | <name>t64`
  alternatives so apt picks whichever name the distro actually provides.

## [6.4.5_DE-1.33.26] - 2026-08-16

### Changed

- [DE] Clearer updater wording: the update button now reads "Check for
  available updates" (instead of the Android toggle's "Automatically check for
  updates") and "Open installer" now reads "Close Vivi and open installer",
  matching what the button actually does. Updated across all languages.

## [6.4.5_DE-1.33.25] - 2026-08-16

### Fixed

- [DE] Critical startup crash on Windows: the WinMM binding looked up
  `waveOutOpenW`, but `winmm.dll` exports the function with **no A/W suffix**
  (it takes no string argument, so `waveOutOpenW`/`waveOutOpenA` exist only as
  C header macros). JNA threw "Error looking up function 'waveOutOpenW'" and the
  app crashed. Restored the correct `waveOutOpen` symbol and guarded every
  native call so a missing symbol can never crash the app again.

## [6.4.5_DE-1.33.24] - 2026-08-16

### Changed

- [DE] Translation quality pass: filled in the remaining desktop-only keys
  (device sync, updates, player basics) that were still falling back to
  English, and corrected translations whose Android mapping had a different
  (longer or wrong-context) meaning — e.g. "Check for updates" is now a short
  button label instead of "check automatically…", "Error" no longer reads
  "unknown error", and CPU/GPU keep their short technical form.

## [6.4.5_DE-1.33.23] - 2026-08-16

### Fixed

- [DE] Windows Inno Setup installer now always shows the "Select Destination
  Location" page so the install path is visible (and editable), matching the
  MSI installer.

## [6.4.5_DE-1.33.22] - 2026-08-16

### Fixed

- [DE] Native (OS) volume now actually syncs on Windows: the WinMM call used
  `waveOutOpen`, which is a macro — `winmm.dll` only exports `waveOutOpenW`/
  `waveOutOpenA` — so `Native.load` failed and every native volume read/write
  silently no-opped. The default wave device is now opened via `waveOutOpenW`
  (with `StdCallLibrary`).
- [DE+APK] Volume pushes (in-app `volume` and native `systemVolume`) are now
  retried instead of being silently dropped: both sides poll volume and only
  mark it as pushed once the snapshot is actually sent, so a push that lands in
  the echo-suppression window is re-sent on the next tick. The mobile side is
  echo-guarded per-field so an applied remote value isn't bounced back.

## [6.4.4_DE-1.33.21] - 2026-08-16

### Added

- [DE] Single-instance guard: launching the app while another instance is
  already running (or still starting) exits immediately, always keeping the
  first instance that started.
- [DE] Notifications now cover **all** app notifications, not just updates:
  update available, device paired/unpaired and developer-options unlocked all
  route through the chosen notification mode (main window vs native). Native
  system notifications are marked "experimental".

### Changed

- [DE] Completed all remaining desktop-only translations (developer options and
  backup/restore strings) across all 47 supported languages — every key now has
  a real translation instead of an English fallback.
- [DE] `Localization.kt` is now emitted as one top-level function per language
  (instead of a single giant `mapOf`) to stay under the JVM 64KB `<clinit>`
  limit that caused a "Method too large" compiler error.

## [6.4.4_DE-1.33.20] - 2026-08-16

### Fixed

- [DE] Seek bar: decoded position is now reported at ~10 fps (throttled from
  ~43 fps) so the player seek slider no longer fights the constant frame-by-frame
  updates — it stays smooth and can be dragged to any position instead of
  sticking at the start/end.
- [DE] In-app (VIVI) volume sync now also pushes when nothing is playing, and
  uses a per-field echo guard (mirroring the OS-volume loop) so a local change
  is no longer silently dropped by the generic echo-suppression window.
- [DE] Windows OS volume sync: `waveOutGetVolume`/`waveOutSetVolume` were
  called with the `WAVE_MAPPER` constant as if it were an open handle, which
  made every call fail (so Windows native volume never synced). The default
  wave device is now opened first via `waveOutOpen` before reading/writing
  volume.

## [6.4.4_DE-1.33.19] - 2026-08-16

### Added

- [DE] New "Notifications" settings sub-menu: update notifications can now be
  shown either in the main window (in-app, default) or as a native system
  notification (`java.awt.SystemTray`, best-effort across OS).

## [6.4.4_DE-1.33.18] - 2026-08-16

### Added

- [DE] "Add to playlist" is now also available on the full Player screen
  (secondary actions) and on every row of the Queue screen.

## [6.4.4_DE-1.33.17] - 2026-08-16

### Added

- [DE] A dedicated "Add to playlist" button on every song row (Home, Search,
  Album, Artist, Playlist, Library), alongside the ⋮ context menu — no more
  hiding the action behind the menu.

## [6.4.4_DE-1.33.16] - 2026-08-16

### Fixed

- [DE] Seek slider couldn't be dragged to the middle: the track duration is now
  taken from the player response (`videoDetails.lengthSeconds`) and reported
  immediately, so the slider has a correct range (previously the AAC-derived
  fallback could be 0/wrong and the slider only landed on start or end).

### Changed

- [DE+APK] Device-sync volume now uses two separate channels: the in-app player
  volume (mobile slider <-> desktop slider, pixel-synced) and the native OS
  system volume (Android STREAM_MUSIC <-> desktop OS volume). The desktop reads
  and writes its OS volume via WinMM (Windows), `pactl`/`amixer` (Linux) and
  `osascript` (macOS), all best-effort and guarded.

## [6.4.3_DE-1.33.15] - 2026-08-16

### Fixed

- [DE] Crash ("layouts are not part of the same hierarchy") when interacting
  with any dropdown or dialog (update-check frequency, playlist delete, …).
  Root cause was the global `SelectionContainer`: popup components
  (`DropdownMenu`, `AlertDialog`) inherit its selection registrar and crash on
  pointer events (Compose CMP-2326). The global wrapper is removed; targeted
  selectable text is kept for the player error detail and the pairing code.

## [6.4.3_DE-1.33.14] - 2026-08-16

### Fixed

- [DE] Crash ("layouts are not part of the same hierarchy") when confirming a
  playlist deletion. The confirmation dialog now dismisses first and the row
  removal is deferred to the next frame, so the playlist list no longer
  reflows while the dialog window is being torn down.

## [6.4.3_DE-1.33.13] - 2026-08-16

### Fixed

- [DE] Apostrophes now render correctly everywhere instead of showing a
  literal `\'`. The desktop localization generator now decodes Android's
  `\'` resource escape (plus `\n`/`\t`/`\"`/`\\`) into real characters before
  re-encoding them as Kotlin string literals.

## [6.4.3_DE-1.33.12] - 2026-08-16

### Changed

- [DE] GitHub release titles now use the `Vivi Music <mobile>_DE <desktop>`
  format (e.g. `Vivi Music 6.4.3_DE 1.33.12`) instead of `Vivi Music DE
  <mobile>_DE-<desktop>`. Tags remain unchanged.

## [6.4.3_DE-1.33.11] - 2026-08-16

### Fixed

- [DE] Crash when changing the update-check interval: selecting an option in
  the frequency dropdown no longer throws "layouts are not part of the same
  hierarchy". The popup is now dismissed before the interval state (which
  reflows that row) is applied.

## [6.4.3_DE-1.33.10] - 2026-08-16

### Fixed

- [DE+APK] Device-sync regression: a transient network drop (or a socket
  reconnecting) no longer tears down a healthy pairing. Both relays now wait a
  15-second grace period for the device to reconnect before unpairing, and only
  the device's live socket triggers the unpair. Closing an app still un-pairs
  the other side (after the grace period).

### Note

- The cloud relay needs a redeploy of `sync-server/server.js` for this to apply
  over `wss://`; the LAN relay is fixed immediately.

## [6.4.3_DE-1.33.9] - 2026-08-16

### Added

- [DE] Backup & restore sub-menu in Settings: export the desktop settings to a
  `.backup` file (native save dialog) and import them back (native open
  dialog). Importing preserves the device id and first-launch date, drops any
  stale pairing, and prompts a restart to apply.

## [6.4.3_DE-1.33.8] - 2026-08-16

### Changed

- [DE] The GitHub logo in About → Community is now a vector icon (ported from
  the mobile app) instead of a static PNG, so it tints with the accent color
  and adapts to dark/light mode.

## [6.4.3_DE-1.33.7] - 2026-08-16

### Changed

- [DE+APK] Closing either app (mobile or desktop) now un-pairs both devices.
  The relays (cloud `sync-server` and the desktop LAN relay) detect the socket
  close, clear the pair, and tell the still-open peer it is no longer paired,
  so it stops showing "paired" for a peer that is gone.

### Note

- The cloud relay needs a redeploy of `sync-server/server.js` for this to take
  effect over `wss://`; the LAN relay works immediately.

## [6.4.3_DE-1.33.6] - 2026-08-16

### Fixed

- [DE+APK] Volume now syncs as the **system** volume: raising/lowering the
  Android volume (rocker or player bar) drives the desktop volume and vice
  versa, and the change is pushed immediately (no longer only on the periodic
  re-sync tick). This also fixes the volume bar position not following on the
  other device while the audible level did.

## [6.4.2_DE-1.33.6] - 2026-08-16

### Added

- [DE+APK] The desktop QR code now embeds the relay address **and** the current
  6-digit pairing code (`vivimusic://pair?addr=…&code=…`). Scanning it on the
  phone auto-fills both the server URL and the code, so you only verify the code
  and tap Pair. Plain `ws://` URLs still work for manual entry.

## [6.4.1_DE-1.33.5] - 2026-08-16

### Added

- [DE] The player now shows its load state while a track starts: "Resolving
  audio…" then "Downloading…" with a spinner, in both the full player and the
  mini-player, so you can tell it's working instead of appearing frozen.

## [6.4.1_DE-1.33.4] - 2026-08-16

### Fixed

- [DE] Playback now retries automatically: when the stream fails to resolve or
  download (e.g. a stale googlevideo 403), the player rotates the guest
  identity, re-resolves a fresh stream URL and retries up to 3 attempts before
  surfacing the error.

## [6.4.1_DE-1.33.3] - 2026-08-16

### Changed

- [DE] Completed the translations for the playlist and song-menu strings
  (rename / delete playlist / confirmation / empty / not-found / song count /
  like / library / share, …) across all 47 supported languages. The
  delete-playlist confirmation now shows the desktop message instead of the
  Android "Really delete … %s" template.

## [6.4.1_DE-1.33.2] - 2026-08-16

### Added

- [DE] Full song context menu (⋮): like / unlike, add to / remove from library,
  add to playlist and share (copies the YouTube Music link to the clipboard).
  Like and library actions use the signed-in YouTube account; a non-invasive
  snackbar confirms clipboard copies.

## [6.4.1_DE-1.33.1] - 2026-08-16

### Added

- [DE] Drag-to-reorder inside the playlist detail screen (drag the ⠿ handle);
  the new order is saved and synced like any other playlist edit.

## [6.4.1_DE-1.33.0] - 2026-08-16

### Added

- [DE] Local playlist system: create / rename / delete playlists, add songs to
  a playlist from any song row (Home, Search, Library, Album, Artist, Playlist
  and History), and a per-playlist detail screen with play-all and per-song
  remove.
- [DE+APK] Playlists now sync between the desktop and the phone over the
  device-sync channel: the full playlist (name + ordered songs) plus a
  per-playlist edit timestamp is shared, and edits are merged with
  last-write-wins. The most recently updated copy of each playlist wins,
  deletions propagate as tombstones, and a change made on either device appears
  on the other.

### Changed

- [DE] The sidebar gains a "Playlists" entry that opens the local playlist list.

## [6.4.0_DE-1.32.2] - 2026-08-16

### Fixed

- [DE] The player's seek slider could only land on the start or the end: the
  track duration was reported as 0 because YouTube's fragmented MP4 has an empty
  `mdhd` (jcodec returns `totalDuration == 0`). The duration is now derived from
  the decoded AAC sample count, so the slider spans the whole track and seeking
  works anywhere.
- [DE] Seeking while paused now stays paused instead of forcing playback to
  resume.
- [DE] Stale or truncated cached audio files are now detected and re-downloaded,
  so a leftover `.m4a` from an interrupted download no longer decodes into
  silence.

## [6.4.0_DE-1.32.1] - 2026-08-16

### Fixed

- [DE] "Start LAN server" now retries the pairing-code request until the relay
  answers, so the 6-digit code is generated automatically and reliably right
  after starting the server.

## [6.4.0_DE-1.32.0] - 2026-08-16

### Added

- [DE] Developer options "Title bar only" display mode (stats only in the
  window title, no overlay or separate window).
- [DE] Queue entry in the sidebar (opens the Queue screen directly).
- [DE] "View changelog" button in Settings → Updates.
- [DE+APK] Volume sync: the volume slider now syncs between the two devices.

### Changed

- [DE] Player layout is now two columns: a smaller artwork on the left and the
  seek bar + controls + volume on the right.
- [DE] Clicking the mini-player toggles the full player (open / go back).
- [DE] The About screen's GitHub row now uses the correct GitHub logo.
- [DE] README: smaller logo and @PiBOH added to Special Thanks.

### Fixed

- [DE] Stream resolution is more resilient: it retries transient failures and
  throttles guest-session refreshes so a track can start even with no paired
  device.

## [6.3.0_DE-1.31.0] - 2026-08-15

### Added

- [DE+APK] Periodic re-sync tick: while a track is playing, the position is
  re-pushed every 5 seconds so the paired device auto-corrects drift
  (buffering / clock skew) instead of waiting for the next seek/play/track
  event. A 250 ms tolerance skips near-no-op seeks so the correction doesn't
  glitch the audio.

## [6.2.5_DE-1.30.3] - 2026-08-15

### Changed

- [DE] The Changelog screen now uses a vertical version selector (left list of
  version buttons, like the mobile chips but top-to-bottom) with the selected
  version's details in a pane on the right, instead of stacking every version
  in one long scroll.

## [6.2.5_DE-1.30.2] - 2026-08-15

### Changed

- [DE] The About screen now shows real thumbnails: the developer's avatar
  (`author.png`) instead of the `< >` icon, and the GitHub logo on the
  GitHub Repository row (bundled from `[DE]_images/`).

## [6.2.5_DE-1.30.1] - 2026-08-15

### Changed

- [APK] The Devices entry in Settings now uses the same phone+monitor
  "devices" icon as the desktop edition, instead of the circular sync arrows.

## [6.2.4_DE-1.30.1] - 2026-08-15

### Fixed

- [DE+APK] Both sides now show the paired device's name: the Android Devices
  screen displays the desktop's machine name, and the desktop Device sync
  section displays the phone's make/model. The desktop now advertises its real
  hostname instead of the generic "Desktop", and the peer name is restored from
  the peer's snapshot on reconnect (not just at pairing time).

## [6.2.3_DE-1.30.0] - 2026-08-15

### Added

- [DE] Developer options improvements: the "Developer options" entry is now
  always visible in Settings (showing "Disabled" until enabled), and can be
  unlocked either by tapping the About "Version code" seven times or from that
  screen. Once unlocked, a non-invasive banner points to the settings screen.
  New options: a display profile (Full vs Performance — CPU/RAM/GPU only), a
  "movable overlay" toggle (drag the overlay anywhere on the main window,
  on by default), and a "show in title bar" toggle that puts live CPU/RAM
  usage in the window title.

### Fixed

- [DE] Starting the LAN server now reliably auto-generates the pairing code:
  the code request waits for the local relay connection to be established
  instead of racing the relay startup (which could leave the code ungenerated).

## [6.2.3_DE-1.29.0] - 2026-08-15

### Added

- [DE+APK] Precise, instant player sync between the desktop and the phone.
  Seeking now pushes the new position immediately (both directions) and the
  receiver applies it as a lightweight in-place seek instead of restarting the
  stream, so the two players stay aligned to the second. Playback positions
  now carry a timestamp and both devices estimate their clock offset to the
  relay (PING/PONG), so the live position is extrapolated during playback
  without clock-skew drift.

## [6.2.2_DE-1.28.6] - 2026-08-15

### Fixed

- [DE] Installing both the MSI and the Inno Setup EXE no longer leaves two
  "VIVI Music" entries in "Apps & features": the Inno Setup installer now
  uninstalls any previously-installed jpackage MSI of the app before copying
  its files, so a single uninstall entry remains.

## [6.2.2_DE-1.28.5] - 2026-08-15

### Fixed

- [DE] **Critical:** the packaged app still showed "Failed to launch JVM" at
  startup even after bundling the management modules, because `SystemMonitor`
  read a field before its initializer ran (a Kotlin forward-reference) and
  threw a NullPointerException during `DeveloperOptions.load()`. Fixed the
  declaration order and made dev-tools initialization non-fatal.

## [6.2.2_DE-1.28.4] - 2026-08-15

### Fixed

- [APK] The Android APK now reports version `6.2.2` (it was still showing
  `6.2.1`): `app/build.gradle.kts` `versionName`/`versionCode` were out of
  sync with `version.txt`.

## [6.2.2_DE-1.28.3] - 2026-08-15

### Fixed

- [DE] **Critical:** the packaged app no longer fails to start with
  "Failed to launch JVM". The dev tools (CPU/RAM/thread stats) use
  `java.lang.management` and `com.sun.management`, but jlink was bundling only
  the default modules; those two modules are now declared so the packaged
  runtime includes them.

## [6.2.2_DE-1.28.2] - 2026-08-15

### Fixed

- [DE] The Changelog screen now lists every version vertically (newest first)
  in a single scrollable list, so older versions are reachable with the mouse
  wheel — the previous horizontally-scrolling version chips were unusable on
  desktop.

## [6.2.2_DE-1.28.1] - 2026-08-15

### Fixed

- [DE] Audio playback no longer fails with "HTTP 403 downloading audio": the
  desktop now keeps a fresh guest `visitorData` (like the Android app) and
  rotates it once when YouTube flags the request as a bot, so the googlevideo
  CDN stops rejecting the resolved stream URLs.

## [6.2.2_DE-1.28.0] - 2026-08-15

### Added

- [DE] The Updates screen now checks for updates automatically every time it
  is opened, and in the background at a configurable interval (manual only,
  6 hours, 12 hours, 24 hours, 3 days or 7 days), selectable in
  Settings → Updates.

### Fixed

- [DE] "Download" in the Updates screen no longer opens the browser instead of
  downloading in-app: the updater now picks the newest release that actually
  ships an installer for your OS (skipping releases whose build for your
  platform is missing) and only falls back to the release page when no
  installer exists. Nightly/alpha/beta/rc builds now include pre-releases by
  default so they can see updates without toggling the option.

## [6.2.2_DE-1.27.1] - 2026-08-15

### Fixed

- [DE] Content no longer gets clipped when the window is resized smaller: the
  Album/Artist/Playlist headers now shrink their title/artist text (ellipsis)
  instead of overflowing, and the LAN pairing screen ellipsizes and constrains
  the relay address next to the QR code.

## [6.2.2_DE-1.27.0] - 2026-08-15

### Added

- [DE] Developer options: tap the About "Version code" seven times to enable
  them. Once enabled, a new "Developer options" settings screen lets you show
  live CPU, RAM and network stats (download/upload speed + total traffic), the
  GPU device, thread count, uptime, OS/Java info and the paired phone
  name/model — either as a non-invasive collapsible overlay in the main window
  or in a dedicated window.

## [6.2.2_DE-1.26.4] - 2026-08-15

### Fixed

- [DE] Audio playback no longer fails with "HTTP 403 downloading audio": the
  desktop resolver stopped using the `WEB`/`WEB_REMIX` clients (their
  googlevideo URLs require a PoToken the desktop cannot generate) and now uses
  only PoToken-free clients (added `VISIONOS` and `IOS_MUSIC`). It also only
  applies the n-parameter throttle transform to web clients, so
  Android/iOS/VisionOS stream URLs are no longer corrupted into a 403.

## [6.2.2_DE-1.26.3] - 2026-08-15

### Fixed

- [DE] The About screen "Version code" was derived from the SemVer
  (`1.26.0` → `12600`), which looked like a huge number. It is now an explicit
  monotonic counter stored in `version.txt` (line 4) that tracks the number of
  DE releases (currently 57).

## [6.2.2_DE-1.26.2] - 2026-08-15

### Changed

- [DE] Starting the LAN server now automatically generates a pairing code, and
  the code + "Generate code" button are shown to the right of the QR code
  instead of below it.

## [6.2.2_DE-1.26.1] - 2026-08-15

### Fixed

- [DE+APK] Device pairing is now kept in sync across both devices: unpairing
  from the phone or the desktop unpairs the other side, and stopping the LAN
  server notifies the phone to unpair too. Reconnecting to a relay that no
  longer knows the pair (for example after the desktop is restarted) now clears
  the stale "paired" state instead of leaving it stuck.

## [6.2.1_DE-1.26.0] - 2026-08-15

### Added

- [DE] Redesigned the About screen to mirror the mobile layout: centered
  title + version/channel badge, a Developer section (PiBOH — lead developer
  of the DE edition — with website link), a Community section (GitHub repo +
  Telegram), and an App info section showing the first-launch date (not the
  last-update install date), the numeric version code and the GPL-3.0 license
  link.

## [6.2.1_DE-1.25.0] - 2026-08-15

### Added

- [DE+APK] Library sync over the device-sync channel: the mobile app now
  observes its library (liked songs, bookmarked albums/artists/playlists) and
  pushes a `LibrarySnapshot` whenever it changes; the desktop receives,
  persists and exposes it (and pushes its own).

## [6.2.1_DE-1.24.2] - 2026-08-15

### Fixed

- [DE] "HTTP 403 downloading audio" is now far more robust: the resolver
  returns an ordered list of candidate stream URLs (NewPipe plus every
  innerTube client), and the player tries them in order — retrying without the
  `Range` header when a request is refused — instead of giving up after the
  first URL. NewPipe URLs now use the decrypted `getUrl()` result with the
  n-param transform applied.

## [6.2.1_DE-1.24.1] - 2026-08-15

### Changed

- [DE] The update notification now offers "Open installer" (instead of
  downloading again) when the installer for that version is already present
  in the downloads folder.

## [6.2.1_DE-1.24.0] - 2026-08-15

### Added

- [DE] A non-invasive banner now appears when a newer release is available,
  with "Install now" (downloads and launches the installer) and a dismiss
  button. It is shown once per new version.

## [6.2.1_DE-1.23.0] - 2026-08-15

### Added

- [DE] The settings sub-screens now carry real functionality instead of being
  empty shells:
  - **Appearance**: pure black background toggle (true black in dark mode).
  - **Player & audio**: audio quality (Auto / High 256kbps / Low 128kbps,
    wired to the itag 141/140 picker), "remember shuffle and repeat" across
    restarts, and "persistent queue" (the queue is saved and restored between
    sessions).
  - **Lyrics**: adjustable lyrics text size.

## [6.2.1_DE-1.22.1] - 2026-08-15

### Fixed

- [DE] Audio download no longer fails with "HTTP 403 downloading audio".
  googlevideo ties a stream URL to the client that requested it, so the player
  now downloads with the same User-Agent used to resolve the URL (and a
  `Range: bytes=0-` header), instead of a fixed browser UA that YouTube
  rejected.

## [6.2.1_DE-1.22.0] - 2026-08-15

### Added

- [DE] Search now has mobile-style filter chips (All / Songs / Videos / Albums /
  Artists / Playlists) backed by the innerTube filtered search, plus live query
  suggestions as you type.
- [DE] Library tabs now use Material 3 filter chips and the Songs tab gains a
  "Shuffle all" action.
- [DE] Album and Playlist screens gain a "Shuffle" action next to "Play all".

## [6.2.1_DE-1.21.0] - 2026-08-15

### Added

- [DE] Settings are now organized into mobile-style sub-screens (Language,
  Updates, Appearance, Player & audio, Account, Devices, Content, Lyrics,
  Privacy, Storage, About) instead of one long scrollable page.
- [DE] New Content sub-screen: pick the YouTube content language and region
  (innerTube `hl`/`gl`), applied live and persisted across restarts.
- [DE] New Lyrics sub-screen with a "synced lyrics" toggle that enables or
  disables line-by-line highlighting.
- [DE] New Privacy sub-screen to clear the session, cache and downloaded
  installers.

## [6.2.1_DE-1.20.1] - 2026-08-15

### Changed

- [DE] The auto-release no longer builds the APK itself: it waits for the
  existing "CI & Debug Build" (`build.yml`) run on the same commit and attaches
  its debug APK artifact to the desktop release. The dedicated
  `release-mobile.yml` build workflow was removed.

## [6.2.1_DE-1.20.0] - 2026-08-15

### Fixed

- [DE] Audio playback now works. YouTube serves its `audio/mp4` streams as
  fragmented MP4 (DASH fMP4, `ftyp` brand "dash"), whose samples live in
  `moof`/`trun` boxes instead of the `moov` sample table — which `jaad`'s
  `MP4Container` demuxer does not understand, so every track failed with
  "No audio frames to decode". The player now walks the fragments directly
  with jcodec and decodes with the bundled jaad AAC decoder.
- [DE] The stream resolver now prefers AAC-LC (codec `mp4a.40.2`, itag 140/141)
  over HE-AAC/SBR (`mp4a.40.5`, itag 139), which jaad cannot decode ("FIL
  element overread").
- [DE] Language sync with the mobile app now maps the differing locale codes
  (mobile `no`/`pt-PT`/`zh-CN`/`zh-TW` ↔ desktop `nb`/`pt`/`zh-rCN`/`zh-rTW`),
  so changing the language on one device is correctly reflected on the other.

### Added

- [DE] All text (errors, options, settings, LAN server details, etc.) is now
  selectable and copyable across the whole desktop app.

## [6.2.1_DE-1.19.3] - 2026-08-15

### Changed

- [DE] APK delivery for desktop releases: a dedicated workflow
  (`release-mobile.yml`, a copy of the mobile CI adapted for `vivi-music-de`)
  builds and signs the GMS + FOSS APKs on this branch and attaches them to the
  release created by `auto-release.yml`. The in-pipeline `build-android` job was
  removed from `auto-release.yml` to avoid building the APK twice.

## [6.2.1_DE-1.19.2] - 2026-08-15

### Fixed

- [DE] Content now scales with the window: the Player artwork resizes with the
  window width (180–360dp) instead of being fixed at 300dp, and Library cards
  fill their adaptive grid cells instead of a fixed 140dp width.

## [6.2.1_DE-1.19.1] - 2026-08-15

### Changed

- [APK] Bumped the mobile version to 6.2.1 (versionCode 77).
- The desktop auto-release now builds and attaches the Android APK directly from
  this branch instead of trying to download it from the mobile CI release
  (`release.yml` runs on `main`, which had fallen out of sync), so the APK now
  reliably appears among the release assets.

## [6.2.0_DE-1.19.1] - 2026-08-15

### Changed

- [APK] The pairing-code field on Android now opens the numeric keypad and only
  accepts the 6 digits of the code shown by the desktop.

## [6.2.0_DE-1.19.0] - 2026-08-15

### Changed

- [DE] The About → Changelog screen now matches the mobile app: a horizontally
  scrollable row of version chips on top, and, for the selected version, a bold
  primary title plus its Added/Fixed/Changed sections rendered as bullet items
  (instead of a flat markdown dump of the whole file).

## [6.2.0_DE-1.18.0] - 2026-08-15

### Added

- [DE] The desktop pairing code now shows a live countdown until it expires
  (5 minutes) and offers a "Generate new code" button to mint a fresh code.

### Changed

- [DE] The desktop Device sync section is now generate-only: it no longer shows
  an "enter code" field. The desktop generates the 6-digit code, and the phone
  enters it.
- [APK] The Android Devices screen is now insert-only: the "Generate code"
  button was removed, leaving just the "enter code" + Pair flow that reads the
  code shown by the desktop.

## [6.2.0_DE-1.17.1] - 2026-08-15

### Fixed

- [DE] Fixed desktop audio not playing: the player reused a stale cache file when
  switching tracks, silently swallowed every decode/download/audio-device error
  (so playback stopped with no message), and did not verify the downloaded file.
  Failures now surface a clear reason in the player, and the decode pipeline is
  more robust.
- [DE] The app now closes itself right after launching an update installer so the
  installer can replace the running files (updates could not install otherwise).

### Changed

- [DE] The About → Changelog screen now fetches `CHANGELOG.md` live from the
  repository (falling back to the bundled copy when offline), so it always shows
  the current changelog without waiting for a new build.

## [6.2.0_DE-1.17.0] - 2026-08-15

### Added

- [DE] Ported the Home screen to the Android app's style: filter chips, mobile-style
  section headers (label + bold primary title, "Play all" button, chevron), songs-only
  sections rendered as horizontal song lists (Quick picks style), mixed sections as
  card carousels, and a Mood & genres section whose buttons open a new generic
  Browse screen.
- [DE] The QR code in Device sync is now always rendered on a solid white card so it
  scans reliably in both light and dark themes.

## [6.2.0_DE-1.16.2] - 2026-08-15

### Fixed

- [DE] Fixed the changelog being unclear about desktop versions: releases are
  now versioned as `<mobile>_DE-<de>` sections in `CHANGELOG.md` (instead of
  everything accumulating under "Unreleased"), and the changelog screen shows
  the current DE version and channel at the top.

## [6.2.0_DE-1.16.1] - 2026-08-15

### Fixed

- [DE] Fixed the language selector always showing English: the desktop string
  table now ships real translations for 45 languages (generated from the
  Android app's `strings.xml` / `vivi_strings.xml` via
  `scripts/generate_desktop_localization.py`). Keys without a translation
  still fall back to English.

## [6.2.0_DE-1.16.0] - 2026-08-15

### Added

- [DE] Started the pixel-perfect UI port from the Android app: the theme now
  uses the same seed-based Material 3 palette (materialKolor TonalSpot, seed =
  accent color) as the mobile app, and the fixed text sidebar was replaced with
  a collapsible/expandable sidebar with Material icons + labels (persisted).

## [6.2.0_DE-1.15.1] - 2026-08-15

### Fixed

- [DE] Fixed the update check picking the wrong release: it now selects the
  desktop release with the highest `_DE-<version>` tag instead of the first /
  "latest" entry (GitHub orders releases by publish date, not by version), so
  an older tag no longer masks a newer one. The release list window was also
  raised to 100 and the changelog notes follow the same highest-version rule.

## [6.2.0_DE-1.15.0] - 2026-08-15

### Added

- [DE] Wired device sync end-to-end: the desktop now pushes its playback
  (track, queue, position, play/pause) and settings, and applies incoming
  snapshots — remote playback starts on the desktop player and language /
  theme / accent follow the phone. A persistent `DesktopSyncManager` owns the
  client + LAN relay for the whole app lifetime (no more state loss when
  leaving Settings), with echo suppression to avoid ping-pong loops.
- [APK] Android now pushes its playback to the desktop and applies incoming
  playback snapshots (desktop → phone), so starting a song on either device
  resumes on the other.

## [6.2.0_DE-1.14.3] - 2026-08-15

### Fixed

- [DE] Fixed most text not adapting to the dark/light theme: Material 3's
  `MaterialTheme` does not set `LocalContentColor`, so text without an explicit
  color fell back to black. The app root now provides `LocalContentColor =
  onBackground`, so titles, headers and other uncolored text follow the theme.

## [6.2.0_DE-1.14.2] - 2026-08-15

### Changed

- [DE] Debounced the Lyrics screen position updates: the highlighted line is
  now polled ~5×/s and only recomposed when it changes, instead of recomposing
  the list on every decoded-frame position update (~40×/s).

## [6.2.0_DE-1.14.1] - 2026-08-15

### Fixed

- [DE] Fixed the "Open installer" button not launching the downloaded
  installer: opening now falls back to the OS's native opener (`cmd /c start`
  on Windows, `open` on macOS, `xdg-open` on Linux) when `Desktop.open()`
  fails, and reports an error instead of failing silently.

## [6.2.0_DE-1.14.0] - 2026-08-15

### Added

- [DE] Added optional manual `DATASYNC_ID` / `VISITOR_DATA` fields to the
  desktop login screen as a fallback for when the automatic extraction from
  the music.youtube.com shell fails.

## [6.2.0_DE-1.13.0] - 2026-08-15

### Added

- [DE] Rebuilt the Player screen as a full Material 3 player: a seek slider
  with elapsed/total time, a volume slider, shuffle and repeat (off/all/one),
  proper Material icons and a large artwork presentation. Playback now reports
  the track duration and supports seeking (the stream is cached locally), the
  volume is adjustable, and the mini-player shows a progress bar.

## [6.2.0_DE-1.12.1] - 2026-08-15

### Fixed

- [DE] Fixed dark mode not repainting the page background: the app root now
  paints the theme's `background` color, so switching to dark converts the
  whole window instead of leaving the native light background showing through.

## [6.2.0_DE-1.12.0] - 2026-08-15

### Added

- [DE] Added the animated canvas to the Player: a blurred, slowly-zooming
  (Ken Burns) artwork background behind the track. Canvas artwork is resolved
  from the same providers as the Android app (Apple Music / Tidal / VIVI Music
  canvas); animated GIF/WebP URLs play via Coil, while video canvases
  (MP4/HLS) fall back to static art + zoom.

## [6.2.0_DE-1.11.0] - 2026-08-15

### Added

- [DE] Added synced lyrics: the Lyrics screen now parses LRC timestamps,
  highlights the current line and auto-scrolls to it as the song plays
  (falls back to plain text when lyrics aren't synced).

## [6.2.0_DE-1.10.0] - 2026-08-15

### Added

- [DE] Added drag-to-reorder to the Queue screen (drag the ⠿ handle), reusing
  the same `sh.calvin.reorderable` library as the Android app.

## [6.2.0_DE-1.9.0] - 2026-08-15

### Added

- [DE] Added YouTube login on desktop: paste the music.youtube.com `Cookie`
  header (no WebView needed), which auto-extracts the account's
  `DATASYNC_ID`/`VISITOR_DATA`, validates the session and persists it locally.
  History now works when signed in, and Library gained Songs / Albums / Artists
  / Playlists tabs (liked songs, albums, artists and playlists).

## [6.2.0_DE-1.8.0] - 2026-08-14

### Added

- [DE] Added an Updates section in Settings with in-app downloads: it detects
  the right installer for the host OS/arch (MSI/AppImage/DMG with EXE/DEB/PKG
  fallback), downloads it with progress % + speed, opens it, and can delete
  downloaded installers.
- [DE] Added Player & audio settings (autoplay next track) and a Storage
  section (cache size + clear cache) to Settings.
- [DE] Added a changelog screen (About → Changelog) showing the bundled
  `CHANGELOG.md` plus the latest GitHub release notes.

## [6.2.0_DE-1.7.0] - 2026-08-14

### Added

- [DE] Added a full playback queue: "add to queue" on every song row, "Play
  all" on albums/playlists, next/previous, auto-advance, and a Queue screen
  (jump / remove / clear).
- [DE] Added a History screen (sidebar) listing the user's listening history.

## [6.2.0_DE-1.6.0] - 2026-08-14

### Added

- [DE] Added a light/dark/system theme with a selectable accent color palette
  (Settings → Appearance), applied across the whole desktop app.

## [6.2.0_DE-1.5.1] - 2026-08-14

### Fixed

- [DE] Fixed the desktop player showing "could not resolve the audio stream":
  the stream resolver now uses the same multi-client fallback chain as the
  mobile app (ANDROID_VR + 11 fallback clients, n-param deobfuscation and URL
  validation) instead of a single ANDROID_VR attempt that YouTube often answers
  with `LOGIN_REQUIRED`.

## [6.2.0_DE-1.5.0] - 2026-08-14

### Added

- [APK] Added LAN discovery to the Android Devices screen: "Find desktop"
  (mDNS/NSD `_vivimusic._tcp`) and "Scan QR code" auto-fill the relay server
  URL when pairing with VIVI Music DE over the same Wi-Fi. (Mobile version
  bumped 6.1.0 → 6.2.0.)

## [6.1.0_DE-1.5.0] - 2026-08-14

### Added

- [DE] Added LAN discovery aids to the desktop Device sync section: a QR code
  encoding the local relay address, and mDNS service registration
  (`_vivimusic._tcp`) so the Android app can discover/scan the desktop.

## [6.1.0_DE-1.4.0] - 2026-08-14

### Added

- [DE] Added offline LAN (same Wi-Fi) device pairing: the desktop can start a
  local WebSocket relay from Settings → Device sync, so the Android app can
  pair directly without the cloud relay.

## [6.1.0_DE-1.3.1] - 2026-08-14

### Changed

- [DE] The auto-release now attaches the mobile APKs by downloading them from
  the mobile CI release (tag `v<mobile>`) instead of rebuilding them in a
  separate Android job, making desktop releases much faster.

## [6.1.0_DE-1.3.0] - 2026-08-14

### Added

- [DE] Added an Updates section in Settings: an automatic update check on
  startup plus a manual "Check for updates" button, an opt-in toggle to include
  pre-releases, and a download link when a newer desktop release is available.

## [6.1.0_DE-1.2.1] - 2026-08-14

### Fixed

- [DE] Fixed the auto-release workflow's invalid YAML: `continue-on-error` is not
  allowed on a job that calls a reusable workflow, so the Android APK build is
  now made optional with per-step `continue-on-error` inside `build-android.yml`
  instead (the desktop release no longer requires the APK to succeed).

## [6.1.0_DE-1.2.0] - 2026-08-14

### Added

- [DE] Integrated self-contained audio playback (Phase 4): AAC stream
  resolution (NewPipe + ANDROID_VR) and a pure-Java AAC decoder (`jaad`)
  played through Java Sound — no external player or native codec required.
  The mini-player and Player screen now actually play/pause/resume songs and
  show the playback position.

## [6.1.0_DE-1.1.2] - 2026-08-14

### Changed

- [DE] Release notes now collapse the commit list into an expandable section
  when there are more than 7 commits.

## [6.1.0_DE-1.1.1] - 2026-08-14

### Changed

- [DE] The Android APK build in the auto-release is now optional (per-step
  best-effort), so a missing signing secret or failed APK build no longer
  blocks the desktop release.

## [6.1.0_DE-1.1.0] - 2026-08-14

### Added

- [DE] Full desktop UI: sidebar navigation with Home, Search, Album, Artist,
  Playlist, Library, Player, Lyrics and Settings screens, artwork thumbnails,
  and an Apple Music–style mini-player. (Library is a placeholder pending
  login; audio playback and the animated canvas are deferred to later phases.)

## [6.1.0_DE-1.0.4] - 2026-08-14

### Changed

- [DE] The auto-release now also builds and attaches the Android APKs
  (GMS + FOSS) to the same release, so each release ships desktop + mobile
  assets together.

## [6.1.0_DE-1.0.3] - 2026-08-14

### Fixed

- [DE] Fixed desktop device pairing: the desktop client now actually connects
  (the `connect()` call was unreachable) and defaults to the same relay URL as
  the Android app instead of the local `wss://localhost:8080` placeholder, so
  "Generate code" produces a code.

## [6.1.0_DE-1.0.2] - 2026-08-14

### Changed

- [DE] Split the Linux build into independent DEB and AppImage jobs so a
  failure in the AppImage step no longer blocks the DEB package (or the
  release).

## [6.1.0_DE-1.0.1] - 2026-08-14

### Changed

- [DE] The Windows build now produces both an Inno Setup installer and a
  jpackage MSI.
- [DE] Removed the Inno Setup wizard images that could make the installer open
  and immediately close on some systems.

## [6.1.0_DE-1.0.0] - 2026-08-14

### Added

- [APK] Added a "Devices" section in the Android Settings to pair the phone with
  VIVI Music DE (relay server URL, generate/join pairing code, unpair).

## [6.0.5_DE-1.0.0] - 2026-08-14

### Added

- [DE] Compose Multiplatform desktop target (`desktop` module) reusing the
  pure-JVM network modules.
- [DE] Native desktop icons (Windows `.ico`, macOS `.icns`, Linux `.png`) using
  the VIVI Music DE logo.
- [DE] Per-OS GitHub Actions builds (MSI/EXE, DEB/AppImage, DMG/PKG) and an
  auto-release workflow.
- [DE] Cross-device sync foundation: shared `sync` module, Node.js WebSocket
  relay (`sync-server/`), Android `DeviceSyncManager`, and desktop pairing UI.

### Changed

- Converted `innertube`, `spotify`, `lastfm`, `kizzy`, `shazamkit`,
  `jiosaavn`, and `lyricsProvider` to pure-JVM Kotlin modules so they can be
  shared between Android and desktop.
- Desktop releases now use a combined `<mobile>_DE-<desktop>` version
  (e.g. `6.0.5_DE-1.0.0`): `version.txt` line 1 = mobile version, line 2 = DE
  version, line 3 = channel. The About screen shows the full version + channel;
  desktop changelog entries are marked `[DE]`.
- The release channel is now read from line 3 of `version.txt`: `stable` (or
  empty) publishes a stable release; any other value (`rc`/`beta`/`alpha`/
  `nightly`) publishes a pre-release.
- Release tags no longer carry a `v` prefix; non-stable releases append the
  channel to the tag (e.g. `6.0.5_DE-1.0.0-nightly`).
- [DE] The desktop UI is now English-first with a 49-language picker (first
  launch + Language menu); non-English strings fall back to English until
  translated.
- [DE] The Windows installer now performs a machine-wide install into
  `C:\Program Files\VIVIMusic` (requires admin rights) instead of a per-user
  install into `%LOCALAPPDATA%`.
- [DE] The Windows installer is now a branded Inno Setup wizard and shows a
  "successfully uninstalled" confirmation message after removal.

### Fixed

- [DE] Made `gradlew` executable in the repository and in the desktop build
  workflows (fixes `./gradlew: Permission denied` on Linux/macOS runners).
- [DE] Replaced the retired `macos-13` runner with `macos-15-intel` for the
  Intel macOS build.

### Security

- Cross-device sync traffic is currently TLS-only; end-to-end encryption is
  planned for a future release.
