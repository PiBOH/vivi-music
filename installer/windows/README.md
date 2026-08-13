# Vivi Music DE Windows installer

The Windows release contains a branded Inno Setup executable named
`ViviMusicDE-<version>-setup.exe`. It is the recommended Windows installer.
The jpackage MSI and portable executable remain available as compatibility
artifacts.

## Installer options

The setup wizard uses the Axolotl logo and lets the user select:

- Start Menu shortcut (enabled by default);
- desktop shortcut (disabled by default);
- taskbar entry (disabled by default);
- clean installation (disabled by default);
- launch Vivi Music DE when setup finishes (disabled by default).

A clean installation removes the installed application directory and the local
`.vivi-music-de` settings/database/session directory. It is intentionally
opt-in and asks for confirmation before deleting data.

The taskbar option first attempts Windows' `taskbarpin` shell verb and also
creates the standard pinned-taskbar shortcut as a fallback. Windows can block
programmatic taskbar pinning through policy or shell restrictions; in that case
the shortcut is still created and can be pinned manually.

## Build locally

Install Inno Setup 6 and run the Gradle packaging task first:

```powershell
./gradlew :composeApp:packageDistributionForCurrentOS --no-daemon
& 'C:\Program Files (x86)\Inno Setup 6\ISCC.exe' `
  /DAppVersion=0.15.6-alpha `
  /DInstallerVersion=1.15.6 `
  /DSourceDir="$PWD\composeApp\build\compose\binaries\main\app\ViviMusicDE" `
  /DOutputDir="$PWD\dist-custom" `
  /DLogoFile="$PWD\logo.png" `
  /DIconFile="$PWD\composeApp\icons\icon.ico" `
  "$PWD\installer\windows\ViviMusicDE.iss"
```

The `build-windows-custom.yml` GitHub Actions workflow installs Inno Setup on
the runner, builds the app image, locates `ISCC.exe` explicitly so the compiler
does not depend on the runner's refreshed PATH, and compiles this setup. The
custom workflow runs first. The standard `build-windows.yml` workflow is
started only when the custom compiler fails and then provides the known-good MSI
and portable packages. A successful custom build therefore does not duplicate
standard Windows packaging.
