; VIVI Music DE — Windows installer (Inno Setup).
;
; The self-contained app image is produced by jpackage (Gradle
; `createDistributable`) and passed in through /DSourceDir. Version and icon
; paths are also supplied from the CI workflow so this file stays the single
; source of the installer layout.

#ifndef AppVersion
#define AppVersion "0.0.0-dev"
#endif
#ifndef InstallerVersion
#define InstallerVersion "0.0.0"
#endif
#ifndef SourceDir
#define SourceDir "."
#endif
#ifndef OutputDir
#define OutputDir "dist"
#endif
#ifndef IconFile
#define IconFile "desktop/icons/logo_vmde.ico"
#endif

#define AppName "VIVI Music DE"
#define AppExe "VIVIMusic.exe"
#define AppPublisher "VIVI Music"
#define AppId "com.vivi.vivimusic.desktop"

[Setup]
AppId={#AppId}
AppName={#AppName}
; Inno Setup requires a numeric application version. The full `-DE` SemVer stays
; visible in AppVerName and in the output filename.
AppVersion={#InstallerVersion}
AppVerName={#AppName} {#AppVersion}
AppPublisher={#AppPublisher}
AppPublisherURL=https://github.com/PiBOH/vivi-music
AppSupportURL=https://github.com/PiBOH/vivi-music
AppUpdatesURL=https://github.com/PiBOH/vivi-music/releases
DefaultDirName={autopf}\VIVIMusic
DefaultGroupName={#AppName}
DisableProgramGroupPage=yes
; Always show the "Select Destination Location" page so the user can see (and
; change) the folder the app will install into — matching the MSI installer,
; which shows the destination path.
DisableDirPage=no
OutputDir={#OutputDir}
OutputBaseFilename=VIVIMusic-{#AppVersion}-setup
SetupIconFile={#IconFile}
WizardStyle=modern
; The jpackage image is 200+ MB; avoid solid compression so CI stays fast.
Compression=lzma
SolidCompression=no
PrivilegesRequired=admin
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
CloseApplications=yes
RestartApplications=no
Uninstallable=yes
UninstallDisplayIcon={app}\{#AppExe}
VersionInfoCompany={#AppPublisher}
VersionInfoDescription={#AppName} desktop client
VersionInfoProductName={#AppName}
VersionInfoVersion={#InstallerVersion}
VersionInfoProductVersion={#InstallerVersion}
VersionInfoCopyright=Copyright (c) 2026 VIVI Music

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "startmenu"; Description: "Create a Start Menu shortcut"; GroupDescription: "Additional shortcuts:"; Flags: checkedonce
Name: "desktopicon"; Description: "Create a desktop shortcut"; GroupDescription: "Additional shortcuts:"; Flags: unchecked

[Files]
Source: "{#SourceDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\{#AppName}"; Filename: "{app}\{#AppExe}"; Tasks: startmenu
Name: "{autodesktop}\{#AppName}"; Filename: "{app}\{#AppExe}"; Tasks: desktopicon

[Run]
Filename: "{app}\{#AppExe}"; Description: "Start {#AppName}"; Flags: nowait postinstall skipifsilent

[Code]
// Uninstall any previously-installed jpackage MSI of this app. The MSI and this
// Inno Setup installer are two different installer technologies, so each
// registers its own entry under "Apps & features". Removing the MSI here keeps
// a single uninstall entry when the user installs the .exe after the .msi.
procedure UninstallExistingMsi();
var
  RootKeys: array of String;
  Names: TArrayOfString;
  I, J: Integer;
  DisplayName, UninstallString: String;
  ResultCode: Integer;
begin
  SetArrayLength(RootKeys, 2);
  RootKeys[0] := 'SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall';
  RootKeys[1] := 'SOFTWARE\WOW6432Node\Microsoft\Windows\CurrentVersion\Uninstall';
  for J := 0 to GetArrayLength(RootKeys) - 1 do
  begin
    if RegGetSubkeyNames(HKLM, RootKeys[J], Names) then
    begin
      for I := 0 to GetArrayLength(Names) - 1 do
      begin
        if RegQueryStringValue(HKLM, RootKeys[J] + '\' + Names[I], 'DisplayName', DisplayName) and
           RegQueryStringValue(HKLM, RootKeys[J] + '\' + Names[I], 'UninstallString', UninstallString) then
        begin
          if (Pos('VIVI', Uppercase(DisplayName)) > 0) and (Pos('MSIEXEC', Uppercase(UninstallString)) > 0) then
          begin
            Exec('msiexec.exe', '/x ' + Names[I] + ' /qn /norestart', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
          end;
        end;
      end;
    end;
  end;
end;

procedure CurStepChanged(CurStep: TSetupStep);
begin
  if CurStep = ssInstall then
    UninstallExistingMsi();
end;

// Always show the installation details box (the extraction log) below the
// progress bar on the "Installing" page. Inno Setup keeps this log hidden by
// default behind the "Show details" toggle; we reveal it and remove the toggle
// so the details are always visible while installing.
procedure CurPageChanged(CurPageID: Integer);
begin
  if CurPageID = wpInstalling then
  begin
    WizardForm.DetailsMemo.Visible := True;
    WizardForm.DetailsButton.Visible := False;
    // Position the log right under the status line and size it to fill the
    // page down to where the (now hidden) details button used to sit.
    WizardForm.DetailsMemo.Top := WizardForm.StatusLabel.Top + WizardForm.StatusLabel.Height + ScaleY(8);
    WizardForm.DetailsMemo.Height := WizardForm.DetailsButton.Top - WizardForm.DetailsMemo.Top - ScaleY(8);
  end;
end;

// Same for the uninstaller: show the details (log) box below the progress bar
// on the uninstall progress page, with no toggle to collapse it.
procedure InitializeUninstallProgressForm();
begin
  UninstallProgressForm.DetailsMemo.Visible := True;
  UninstallProgressForm.DetailsButton.Visible := False;
  UninstallProgressForm.DetailsMemo.Top := UninstallProgressForm.StatusLabel.Top + UninstallProgressForm.StatusLabel.Height + ScaleY(8);
  UninstallProgressForm.DetailsMemo.Height := UninstallProgressForm.DetailsButton.Top - UninstallProgressForm.DetailsMemo.Top - ScaleY(8);
end;

// Show a confirmation once the uninstaller has finished removing the app.
procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
begin
  if CurUninstallStep = usPostUninstall then
    MsgBox('{#AppName} was successfully uninstalled.', mbInformation, MB_OK);
end;
