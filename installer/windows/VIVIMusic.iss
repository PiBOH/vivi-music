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
var
  InstallDetailsMemo: TNewMemo;
  UninstallDetailsMemo: TNewMemo;
  LastInstallLine: String;

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

// ---------------------------------------------------------------------------
// Details/log box helpers
// ---------------------------------------------------------------------------
// Inno Setup 6 removed the built-in "Show details" memo/button that existed in
// Inno Setup 5 (there is no WizardForm.Memo / DetailsMemo / DetailsButton), so
// the log box is created at runtime below the progress gauge and filled with
// the files being extracted (install) and the cleanup steps (uninstall).

procedure AddInstallLine(const Line: String);
begin
  if InstallDetailsMemo = nil then Exit;
  if Line = '' then Exit;
  if InstallDetailsMemo.Lines.Count > 2000 then
    InstallDetailsMemo.Lines.Clear;
  InstallDetailsMemo.Lines.Add(Line);
end;

procedure AddUninstallLine(const Line: String);
begin
  if UninstallDetailsMemo = nil then Exit;
  if Line = '' then Exit;
  if UninstallDetailsMemo.Lines.Count > 2000 then
    UninstallDetailsMemo.Lines.Clear;
  UninstallDetailsMemo.Lines.Add(Line);
end;

procedure CreateInstallDetailsMemo();
begin
  if InstallDetailsMemo <> nil then Exit;
  InstallDetailsMemo := TNewMemo.Create(WizardForm);
  InstallDetailsMemo.Parent := WizardForm.InstallingPage;
  InstallDetailsMemo.Left := 0;
  InstallDetailsMemo.Top := WizardForm.ProgressGauge.Top + WizardForm.ProgressGauge.Height + ScaleY(8);
  InstallDetailsMemo.Width := WizardForm.InstallingPage.ClientWidth;
  InstallDetailsMemo.Height := WizardForm.InstallingPage.ClientHeight - InstallDetailsMemo.Top - ScaleY(8);
  InstallDetailsMemo.ReadOnly := True;
  InstallDetailsMemo.ScrollBars := ssVertical;
  InstallDetailsMemo.WordWrap := False;
  InstallDetailsMemo.Font.Name := 'Consolas';
  InstallDetailsMemo.Font.Size := 8;
  LastInstallLine := '';
end;

procedure CreateUninstallDetailsMemo();
begin
  if UninstallDetailsMemo <> nil then Exit;
  UninstallDetailsMemo := TNewMemo.Create(UninstallProgressForm);
  UninstallDetailsMemo.Parent := UninstallProgressForm.InstallingPage;
  UninstallDetailsMemo.Left := 0;
  UninstallDetailsMemo.Top := UninstallProgressForm.ProgressBar.Top + UninstallProgressForm.ProgressBar.Height + ScaleY(8);
  UninstallDetailsMemo.Width := UninstallProgressForm.InstallingPage.ClientWidth;
  UninstallDetailsMemo.Height := UninstallProgressForm.InstallingPage.ClientHeight - UninstallDetailsMemo.Top - ScaleY(8);
  UninstallDetailsMemo.ReadOnly := True;
  UninstallDetailsMemo.ScrollBars := ssVertical;
  UninstallDetailsMemo.WordWrap := False;
  UninstallDetailsMemo.Font.Name := 'Consolas';
  UninstallDetailsMemo.Font.Size := 8;
end;

// ---------------------------------------------------------------------------
// Uninstall cleanup: keep exactly ONE final backup, delete everything else
// ---------------------------------------------------------------------------
// The app keeps its user data under `%USERPROFILE%\.vivimusic` (settings,
// playlists, imported fonts, plus all the caches: downloaded updates, audio,
// video/canvas, lyrics, logs, extracted helper libraries and artwork). On
// uninstall we copy the real user data (device-sync.json, playlists.json,
// fonts) into `backups\uninstall-<timestamp>\` and then delete EVERYTHING
// else, including old backups — only the last backup remains on disk.

procedure BackupAndCleanUserData();
var
  ViviDir, BackupsDir, BackupDir, Ts, Entry, ItemPath, FontSrc, FontDst: String;
  FindRec: TFindRec;
begin
  ViviDir := GetEnv('USERPROFILE') + '\.vivimusic';
  if not DirExists(ViviDir) then begin
    AddUninstallLine('No user data found at ' + ViviDir + ' — nothing to clean.');
    Exit;
  end;

  Ts := GetDateTimeString('yyyymmdd_hhnnss', '', '');
  BackupsDir := ViviDir + '\backups';
  BackupDir := BackupsDir + '\uninstall-' + Ts;
  if not DirExists(BackupsDir) then
    CreateDir(BackupsDir);
  CreateDir(BackupDir);
  AddUninstallLine('Creating final backup: ' + BackupDir);

  if FileExists(ViviDir + '\device-sync.json') then
    FileCopy(ViviDir + '\device-sync.json', BackupDir + '\device-sync.json', False);
  if FileExists(ViviDir + '\playlists.json') then
    FileCopy(ViviDir + '\playlists.json', BackupDir + '\playlists.json', False);

  if DirExists(ViviDir + '\fonts') then begin
    CreateDir(BackupDir + '\fonts');
    if FindFirst(ViviDir + '\fonts\*', FindRec) then begin
      try
        repeat
          if (FindRec.Attributes and faDirectory) = 0 then begin
            FontSrc := ViviDir + '\fonts\' + FindRec.Name;
            FontDst := BackupDir + '\fonts\' + FindRec.Name;
            FileCopy(FontSrc, FontDst, False);
          end;
        until not FindNext(FindRec);
      finally
        FindClose(FindRec);
      end;
    end;
  end;
  AddUninstallLine('Backup done.');

  // Delete everything under ~/.vivimusic except the "backups" folder.
  if FindFirst(ViviDir + '\*', FindRec) then begin
    try
      repeat
        Entry := FindRec.Name;
        if (Entry = '.') or (Entry = '..') then Continue;
        ItemPath := ViviDir + '\' + Entry;
        if (FindRec.Attributes and faDirectory) <> 0 then begin
          if CompareText(Entry, 'backups') <> 0 then begin
            DelTree(ItemPath, True, True, True);
            AddUninstallLine('Removed cache: ' + Entry);
          end;
        end else begin
          DeleteFile(ItemPath);
          AddUninstallLine('Removed file: ' + Entry);
        end;
      until not FindNext(FindRec);
    finally
      FindClose(FindRec);
    end;
  end;

  // Inside backups/, keep only the backup we just created.
  if FindFirst(BackupsDir + '\*', FindRec) then begin
    try
      repeat
        Entry := FindRec.Name;
        if (Entry = '.') or (Entry = '..') then Continue;
        if CompareText(Entry, 'uninstall-' + Ts) <> 0 then
          DelTree(BackupsDir + '\' + Entry, True, True, True);
      until not FindNext(FindRec);
    finally
      FindClose(FindRec);
    end;
  end;
  AddUninstallLine('Uninstall cleanup complete. Only the final backup remains.');
end;

// ---------------------------------------------------------------------------
// Installer events
// ---------------------------------------------------------------------------

procedure InitializeWizard();
begin
  CreateInstallDetailsMemo();
end;

procedure CurStepChanged(CurStep: TSetupStep);
begin
  if CurStep = ssInstall then
    UninstallExistingMsi();
end;

procedure CurPageChanged(CurPageID: Integer);
begin
  if CurPageID = wpInstalling then
    CreateInstallDetailsMemo();
end;

// Mirror the file currently being extracted into the details box so the user
// sees a live log of what is being written (the IS5 "Show details" behavior).
procedure CurInstallProgressChanged(Current, Total: Integer);
var
  Line: String;
begin
  Line := Trim(WizardForm.FilenameLabel.Caption);
  if (Line <> '') and (Line <> LastInstallLine) then begin
    LastInstallLine := Line;
    AddInstallLine(Line);
  end;
end;

// ---------------------------------------------------------------------------
// Uninstaller events
// ---------------------------------------------------------------------------

procedure InitializeUninstallProgressForm();
begin
  CreateUninstallDetailsMemo();
end;

procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
begin
  if CurUninstallStep = usUninstall then
    AddUninstallLine('Removing application files…')
  else if CurUninstallStep = usPostUninstall then begin
    BackupAndCleanUserData();
    MsgBox(
      '{#AppName} was successfully uninstalled.' + #13#10 + #13#10 +
      'A final backup of your settings, playlists and fonts was kept at:' + #13#10 +
      GetEnv('USERPROFILE') + '\.vivimusic\backups',
      mbInformation, MB_OK);
  end;
end;