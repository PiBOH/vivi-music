; Vivi Music DE custom Windows installer.
; The app image is produced by jpackage and supplied through /DSourceDir.

#ifndef AppVersion
  #define AppVersion "0.0.0-dev"
#endif
#ifndef SourceDir
  #define SourceDir "."
#endif
#ifndef OutputDir
  #define OutputDir "dist"
#endif
#ifndef LogoFile
  #define LogoFile "logo.png"
#endif
#ifndef IconFile
  #define IconFile "composeApp/icons/icon.ico"
#endif
#ifndef InstallerVersion
  #define InstallerVersion "1.0.0"
#endif

#define AppName "Vivi Music DE"
#define AppExe "ViviMusicDE.exe"
#define AppPublisher "PiBOH"
#define AppUrl "https://piboh.github.io/"
#define AppId "com.vivimusic.de.custom"

[Setup]
AppId={#AppId}
AppName={#AppName}
; Inno Setup requires a numeric application version. The pre-release
; SemVer remains visible in AppVerName and the output filename.
AppVersion={#InstallerVersion}
AppVerName={#AppName} {#AppVersion}
AppPublisher={#AppPublisher}
AppPublisherURL={#AppUrl}
AppSupportURL={#AppUrl}
AppUpdatesURL=https://github.com/PiBOH/vivi-music-de/releases
DefaultDirName={autopf}\ViviMusicDE
DefaultGroupName={#AppName}
DisableProgramGroupPage=no
OutputDir={#OutputDir}
OutputBaseFilename=ViviMusicDE-{#AppVersion}-setup
SetupIconFile={#IconFile}
WizardImageFile={#LogoFile}
WizardSmallImageFile={#LogoFile}
WizardStyle=modern
WizardImageStretch=no
; Keep the 200+ MB jpackage image quick to compile on GitHub runners.
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
Name: "startmenu"; Description: "Create a Start Menu shortcut"; GroupDescription: "Additional shortcuts and actions:"; Flags: checkedonce
Name: "desktopicon"; Description: "Create a desktop shortcut"; GroupDescription: "Additional shortcuts and actions:"; Flags: unchecked
Name: "taskbar"; Description: "Add Vivi Music DE to the taskbar"; GroupDescription: "Additional shortcuts and actions:"; Flags: unchecked
Name: "cleaninstall"; Description: "Clean install (remove settings, cache, database and saved sessions)"; GroupDescription: "Installation options:"; Flags: unchecked
Name: "launchafterinstall"; Description: "Start Vivi Music DE when setup is complete"; GroupDescription: "Installation options:"; Flags: unchecked

[Files]
Source: "{#SourceDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\Vivi Music DE"; Filename: "{app}\{#AppExe}"; Tasks: startmenu
Name: "{autodesktop}\Vivi Music DE"; Filename: "{app}\{#AppExe}"; Tasks: desktopicon
Name: "{userappdata}\Microsoft\Internet Explorer\Quick Launch\User Pinned\TaskBar\Vivi Music DE.lnk"; Filename: "{app}\{#AppExe}"; Tasks: taskbar

[Run]
Filename: "{app}\{#AppExe}"; Description: "Start Vivi Music DE"; Flags: nowait postinstall skipifsilent; Tasks: launchafterinstall

[Code]
function PrepareToInstall(var NeedsRestart: Boolean): String;
begin
  Result := '';
  if WizardIsTaskSelected('cleaninstall') then begin
    if MsgBox(
      'Clean install will remove local settings, cache, database, saved sessions and account data from this computer. Continue?',
      mbConfirmation,
      MB_YESNO) <> IDYES then begin
      Result := 'Clean installation cancelled.';
      Exit;
    end;
    DelTree(ExpandConstant('{app}'), True, True, True);
    DelTree(ExpandConstant('{userprofile}\\.vivi-music-de'), True, True, True);
  end;
end;

procedure PinToTaskbar;
var
  Shell, Folder, Item: Variant;
begin
  try
    Shell := CreateOleObject('Shell.Application');
    Folder := Shell.NameSpace(ExpandConstant('{app}'));
    Item := Folder.ParseName('{#AppExe}');
    if not VarIsEmpty(Item) then begin
      Item.InvokeVerb('taskbarpin');
    end;
  except
    { Windows may restrict programmatic taskbar pinning. The pinned-folder
      shortcut declared above remains as a best-effort fallback. }
  end;
end;

procedure CurStepChanged(CurStep: TSetupStep);
begin
  if (CurStep = ssPostInstall) and WizardIsTaskSelected('taskbar') then begin
    PinToTaskbar;
  end;
end;
