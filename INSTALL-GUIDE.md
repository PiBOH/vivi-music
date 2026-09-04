# VIVI Music DE — Installation Guide

VIVI Music DE is the native desktop edition of
[VIVI Music](https://github.com/vivizzz007/vivi-music) (YouTube Music client),
built with Kotlin and Compose Multiplatform. It runs on **Windows 10+**,
**Linux (x86_64)** and **macOS 10.15+**.

> **Where to download:** all installers are attached to the
> [GitHub Releases](https://github.com/PiBOH/vivi-music/releases) page. Pick
> the latest version, then grab the file for your system below.

---

## Table of contents

- [System requirements](#system-requirements)
- [Windows](#windows)
- [Linux](#linux)
- [macOS](#macos)
- [First launch: signing in](#first-launch-signing-in)
- [Updating the app](#updating-the-app)
- [Where your data lives](#where-your-data-lives)
- [Troubleshooting](#troubleshooting)

---

## System requirements

| | Minimum |
|---|---|
| **OS** | Windows 10+ · Linux x86_64 (glibc) · macOS 10.15+ |
| **RAM** | 2 GB (the app itself uses ~350 MB) |
| **Disk** | ~500 MB free |
| **Java** | Bundled with the app — nothing to install |
| **Graphics** | Any GPU or driver; falls back to software rendering automatically |

> **Platform testing note:** the lead developer can currently test the app only
> on Windows, so Windows receives the most compatibility attention. The Linux
> and macOS builds are produced by CI and rely on community testing — bug
> reports for those platforms are especially appreciated.

---

## Windows

Two installers are published; the **`.exe` is recommended** (lighter and more
user-friendly). The `.msi` is provided for silent / enterprise deployments.

### Option A — Installer (.exe, recommended)

1. Download `VIVIMusic-…-x64.exe` from the
   [Releases](https://github.com/PiBOH/vivi-music/releases) page.
2. Double-click the file and follow the wizard.
3. The app installs to `C:\Program Files\VIVIMusic` and starts when the wizard
   finishes (or launch it from the Start menu / desktop shortcut).

### Option B — MSI package (.msi)

1. Download the `.msi` file.
2. Double-click to install, or deploy silently with:

   ```powershell
   msiexec /i VIVIMusic-….msi /qn
   ```

3. Launch from the Start menu.

### Uninstall

Open **Settings → Apps → Installed apps**, find **VIVI Music**, and choose
*Uninstall* — or run the uninstaller from `C:\Program Files\VIVIMusic`.

> Installed updates are cached under `~/.vivimusic/updates/` and cleaned up
> automatically after 7 days.
>
> **On uninstall, the app keeps exactly one final backup** of your settings,
> playlists and imported fonts at `%USERPROFILE%\.vivimusic\backups\uninstall-…`
> and deletes **everything else** in `~/.vivimusic` — downloaded updates, audio,
> video/canvas and lyrics caches, logs and any other leftover. Only that last
> backup remains on disk.

---

## Linux

Three ways to install, depending on your distribution.

### Option A — Debian / Ubuntu (.deb)

Works on Debian, Ubuntu and their derivatives.

```bash
sudo apt install ./VIVIMusic-….deb
```

or:

```bash
sudo dpkg -i VIVIMusic-….deb && sudo apt-get install -f
```

Launch **VIVI Music DE** from your application menu.

### Option B — AppImage (any distro, portable)

Works on most distributions, including Arch and Fedora.

```bash
chmod +x VIVIMusic-….AppImage
./VIVIMusic-….AppImage
```

If your system blocks AppImages (no FUSE), extract and run it instead:

```bash
./VIVIMusic-….AppImage --appimage-extract
./squashfs-root/AppRun
```

### Uninstall (Linux)

- **.deb**: uninstall normally with `sudo apt remove vivimusic` / `sudo dpkg -r`.
  The package's `postrm` hook keeps exactly one final backup of your settings,
  playlists and fonts at `~/.vivimusic/backups/uninstall-…` and deletes every
  cache (`~/.vivimusic/updates`, audio, video/canvas, lyrics, logs) — only that
  last backup remains.
- **AppImage**: delete the file. To clean your user data the same way, run:
  `sh scripts/uninstall-cleanup.sh` from this repo (or the script shipped in
  the release).
- **AUR**: `pacman -R vivi-music-de` runs the same cleanup automatically via
  the package's `post_remove` hook.

### Option C — Arch Linux (AUR-style PKGBUILD)

Every release also ships a **`PKGBUILD`** (plus `SRCINFO` and an uninstall
hook `vivi-music-de.install`) as release assets.
To build and install a proper system package:

```bash
# 1. Create a directory, put the PKGBUILD from the release assets inside it
mkdir vivi-music-de && cd vivi-music-de
# 2. Copy the PKGBUILD file from the release assets here, then:
makepkg -si
```

The PKGBUILD pins the exact release commit with a real checksum, builds the
app from source with `./gradlew`, and installs it into `/opt/vivi-music-de`
with a desktop entry, icon and `vivi-music-de` launcher. Requires
`jdk21-openjdk` and `unzip` (installed automatically as build dependencies).

> **Graphics note:** if OpenGL isn't available on your system, the app
> automatically switches to the software renderer — it just works, no
> configuration needed.

---

## macOS

Two formats are published (Intel and Apple Silicon builds separately).

### Option A — Disk image (.dmg)

1. Download the `.dmg` for your architecture.
2. Open it and drag **VIVI Music** into the **Applications** folder.
3. Launch from Applications. If macOS warns that the app is from an
   unidentified developer (it's not notarized), right-click the app → **Open**
   → **Open** again, or run:

   ```bash
   xattr -dr com.apple.quarantine /Applications/VIVI\ Music.app
   ```

### Option B — Installer package (.pkg)

1. Download the `.pkg` and double-click it.
2. Follow the installer steps; the app lands in **Applications**.

### Uninstall

Drag `VIVI Music.app` from **Applications** to the Trash, then clean your user
data the same way Windows/Linux do — keep exactly one final backup of settings,
playlists and fonts and delete every cache:

```bash
sh scripts/uninstall-cleanup.sh        # from this repo (or the release assets)
```

The backup is kept at `~/.vivimusic/backups/uninstall-…`; everything else
(updates, audio, video/canvas, lyrics caches, logs) is removed.

---

## First launch: signing in

1. Open the app and go to **Settings → Account**.
2. Choose **Sign in with Google** — an embedded sign-in window opens directly
   on Google. Sign in with your Google account; when the page returns to
   YouTube Music, the window closes by itself and the session is saved.
3. Prefer the manual method instead? **Sign in with cookies**: log in to
   `music.youtube.com` in your browser, open DevTools → Network, click any
   `music.youtube.com` request and paste the full `Cookie` header value into
   the app. The session is stored only on this device.

> Pairing with the Android app (LAN or cloud) is covered in the **Sync**
> section of the app and on the [website](https://piboh.github.io/vivi-music/sync.html).

---

## Updating the app

- **In-app updater:** go to **Settings → Updates** and check for updates. The
  new installer downloads (with progress) and opens for you — on Windows the
  `.exe` is preferred over the `.msi`.
- **Manual:** download the latest installer from
  [GitHub Releases](https://github.com/PiBOH/vivi-music/releases) and install
  over the current version. Your settings, library and playlists are kept.

---

## Where your data lives

| What | Location |
|---|---|
| Settings, login session, pairing | `~/.vivimusic/` |
| Playlists | `~/.vivimusic/playlists.json` |
| Audio & lyrics cache | `~/.vivimusic/cache/` |
| Backups | `~/.vivimusic/backups/` |
| Downloaded updates | `~/.vivimusic/updates/` |

On Windows `~` is `C:\Users\<you>`, on Linux/macOS it's `/home/<you>` /
`/Users/<you>`.

---

## Troubleshooting

| Problem | Solution |
|---|---|
| **App crashes at startup on Linux with `UnsatisfiedLinkError: OpenGLApi.glFlush()`** | Fixed in DE 1.41.14: the app detects the broken OpenGL stack, writes `~/.vivimusic/.gl-software` and restarts once with the software renderer. Every later launch starts directly in software rendering. If you fixed your GPU drivers and want OpenGL back, delete that marker file. |
| **Login fails with `401 UNAUTHENTICATED`** | Fixed in DE 1.41.13 (modern Google `__Secure-*PAPISID` cookies now authenticate correctly). Update to the latest version and try again. |
| **The embedded sign-in window stays blank** | Make sure you're on the latest version. As a fallback, use **Sign in with cookies** (see above). |
| **Volume doesn't reach the speakers while paired** | Set the Windows mixer volume to 0 % (not muted) so the mobile app can drive it. |
| **Playback stops / seekbar jumps while paired** | Update both apps to the latest version — older builds had sync races that were fixed. |
| **Still stuck?** | Check `~/.vivimusic/` log files, or ask in the [Telegram channel](https://t.me/vivimusicde). |

---

*VIVI Music DE — free software under a
[modified GPL-3.0](https://github.com/PiBOH/vivi-music/blob/vivi-music-de/LICENSE) license. The original
mobile app is created by [VIVIDH P ASHOKAN](https://github.com/vivizzz007).*
