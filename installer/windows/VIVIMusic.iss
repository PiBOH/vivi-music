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
#define AppPublisher "PiBOH"
#define AppId "com.vivi.vivimusic.desktop"

[Setup]
AppId={#AppId}
AppName={#AppName}
; Inno Setup requires a numeric application version. The full `-DE` SemVer stays
; visible in AppVerName and in the output filename.
AppVersion={#InstallerVersion}
AppVerName={#AppName}
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
VersionInfoCopyright=Copyright (c) 2026 PiBOH

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
  UninstallCleanupOk: Boolean;

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

// The uninstall details box is only usable while the uninstall progress page
// exists: Inno Setup tears that page down as soon as the file-removal step is
// over, and touching the memo afterwards raises "Control 'TNewMemo' has no
// parent window". Every write is therefore guarded - a dead box must never
// abort the uninstallation - and the line falls back to the uninstall log file
// in %TEMP% so the record is never lost.
function UninstallLogPath(): String;
begin
  Result := GetEnv('TEMP') + '\VIVIMusic-uninstall.log';
end;

procedure AddUninstallLine(const Line: String);
begin
  if Line = '' then Exit;
  if UninstallDetailsMemo <> nil then begin
    try
      if UninstallDetailsMemo.Lines.Count > 2000 then
        UninstallDetailsMemo.Lines.Clear;
      UninstallDetailsMemo.Lines.Add(Line);
      Exit;
    except
      { The details box is gone: keep the record on disk instead. }
    end;
  end;
  SaveStringToFile(UninstallLogPath(), Line + #13#10, True);
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
  if UninstallProgressForm = nil then Exit;
  if UninstallProgressForm.InstallingPage = nil then Exit;
  try
    UninstallDetailsMemo := TNewMemo.Create(UninstallProgressForm);
    UninstallDetailsMemo.Parent := UninstallProgressForm.InstallingPage;
  except
    { No box on this machine: AddUninstallLine writes to the log file instead. }
    Exit;
  end;
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
// Uninstall cleanup: keep the newest *.vivide.backup and device-sync.json,
// delete everything else
// ---------------------------------------------------------------------------
// The app keeps its user data under `%USERPROFILE%\.vivimusic`: settings
// (device-sync.json), playlists, imported fonts, the backup history
// (`backups\*.vivide.backup`) and every cache (downloaded updates, audio,
// video/canvas, lyrics, logs, extracted helper libraries and artwork). On
// uninstall exactly two things survive: the NEWEST .vivide.backup - a full
// restore point with settings and playlists - and device-sync.json. Every other
// file, older backups included, and every cache are deleted.

{ "prefix_YYYYMMDD_HHMMSS.vivide.backup" -> "YYYYMMDD_HHMMSS". The timestamp is
  always the last 15 characters before the extension, so comparing those
  strings is chronological even when the prefixes differ (auto_backup_* vs
  vivimusic-de_*). }
function BackupTimestamp(const FileName: String): String;
var
  Base: String;
begin
  Base := Copy(FileName, 1, Length(FileName) - 14);
  if Length(Base) < 15 then
    Result := ''
  else
    Result := Copy(Base, Length(Base) - 14, 15);
end;

procedure BackupAndCleanUserData();
var
  ViviDir, BackupsDir, Entry, ItemPath, KeepBackup, KeepKey, Key: String;
  FindRec: TFindRec;
begin
  ViviDir := GetEnv('USERPROFILE') + '\.vivimusic';
  if not DirExists(ViviDir) then begin
    AddUninstallLine('No user data found at ' + ViviDir + ' - nothing to clean.');
    Exit;
  end;

  BackupsDir := ViviDir + '\backups';

  { Keep the newest backup: the largest timestamp wins. }
  KeepBackup := '';
  KeepKey := '';
  if FindFirst(BackupsDir + '\*.vivide.backup', FindRec) then begin
    try
      repeat
        Key := BackupTimestamp(FindRec.Name);
        if (Key <> '') and (Key > KeepKey) then begin
          KeepKey := Key;
          KeepBackup := FindRec.Name;
        end;
      until not FindNext(FindRec);
    finally
      FindClose(FindRec);
    end;
  end;
  if KeepBackup = '' then
    AddUninstallLine('No .vivide.backup found - nothing to keep.')
  else
    AddUninstallLine('Keeping the newest backup: ' + KeepBackup);

  { Delete everything under ~/.vivimusic except device-sync.json and backups. }
  if FindFirst(ViviDir + '\*', FindRec) then begin
    try
      repeat
        Entry := FindRec.Name;
        if (Entry = '.') or (Entry = '..') then Continue;
        ItemPath := ViviDir + '\' + Entry;
        if (FindRec.Attributes and FILE_ATTRIBUTE_DIRECTORY) <> 0 then begin
          if CompareText(Entry, 'backups') <> 0 then begin
            DelTree(ItemPath, True, True, True);
            AddUninstallLine('Removed cache: ' + Entry);
          end;
        end else begin
          if CompareText(Entry, 'device-sync.json') <> 0 then begin
            DeleteFile(ItemPath);
            AddUninstallLine('Removed file: ' + Entry);
          end;
        end;
      until not FindNext(FindRec);
    finally
      FindClose(FindRec);
    end;
  end;

  { Inside backups/, only the newest .vivide.backup survives. }
  if FindFirst(BackupsDir + '\*', FindRec) then begin
    try
      repeat
        Entry := FindRec.Name;
        if (Entry = '.') or (Entry = '..') then Continue;
        if CompareText(Entry, KeepBackup) <> 0 then begin
          ItemPath := BackupsDir + '\' + Entry;
          if (FindRec.Attributes and FILE_ATTRIBUTE_DIRECTORY) <> 0 then
            DelTree(ItemPath, True, True, True)
          else
            DeleteFile(ItemPath);
          AddUninstallLine('Removed old backup: ' + Entry);
        end;
      until not FindNext(FindRec);
    finally
      FindClose(FindRec);
    end;
  end;

  AddUninstallLine('Uninstall cleanup complete. Only the newest backup and device-sync.json remain.');
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
  { Deliberately empty: at this point the uninstall progress page exists but is
    not on screen yet, so it has no window handle and parenting a control to it
    is unreliable. The details box is created in
    CurUninstallStepChanged(usUninstall) instead, once the page is really up -
    and that is the ONLY window in which the memo can be written, so the box is
    created, filled and left alone inside that one step. }
end;

procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
begin
  if CurUninstallStep = usUninstall then begin
    CreateUninstallDetailsMemo();
    AddUninstallLine('Removing application files…');
    { The user-data cleanup runs here - in the same step as the details box -
      instead of in usPostUninstall. Two reasons: the progress page (and with it
      the box) is destroyed as soon as the file-removal step is over, so a later
      write would only be possible in the log file; and keeping it here means
      every cleanup line is shown in the box while the page is still up. }
    UninstallCleanupOk := True;
    try
      BackupAndCleanUserData();
    except
      UninstallCleanupOk := False;
      AddUninstallLine('Cleanup failed - user data left untouched.');
      AddUninstallLine('Details: ' + UninstallLogPath());
    end;
  end
  else if CurUninstallStep = usPostUninstall then begin
    if UninstallCleanupOk then
      MsgBox(
        '{#AppName} was successfully uninstalled.' + #13#10 + #13#10 +
        'Only these two were kept - the newest backup and your settings:' + #13#10 +
        GetEnv('USERPROFILE') + '\.vivimusic\backups' + #13#10 +
        GetEnv('USERPROFILE') + '\.vivimusic\device-sync.json',
        mbInformation, MB_OK)
    else
      MsgBox(
        '{#AppName} was successfully uninstalled.' + #13#10 + #13#10 +
        'The cache cleanup could not be completed, so your data was left untouched at:' + #13#10 +
        GetEnv('USERPROFILE') + '\.vivimusic' + #13#10 + #13#10 +
        'Log: ' + UninstallLogPath(),
        mbInformation, MB_OK);
  end;
end;