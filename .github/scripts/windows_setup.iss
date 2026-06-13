#define MyAppName "Seal"
#define MyAppPublisher "Junkfood"
#define MyAppURL "https://github.com/Axiaobo7788/Seal-Desktop"
#ifndef MyAppLaunchPath
#define MyAppLaunchPath "Seal.exe"
#endif

[Setup]
; NOTE: The value of AppId uniquely identifies this application.
AppId={{2A4F23E1-7D15-4D04-8E82-990AC3733A9D}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}
DefaultDirName={autopf}\{#MyAppName}
DisableProgramGroupPage=yes
; Remove the following line to run in administrative install mode (install for all users.)
PrivilegesRequired=lowest
OutputDir={#MyAppOutput}
OutputBaseFilename=Seal_Setup_{#MyAppVersion}_{#MyAppArch}_{#MyAppFlavor}
SetupIconFile=..\..\desktop\src\main\resources\icon.ico
Compression=lzma2/ultra64
SolidCompression=yes
WizardStyle=modern
ArchitecturesAllowed=x64
ArchitecturesInstallIn64BitMode=x64
DisableWelcomePage=no
WizardSizePercent=100

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"
Name: "chinesesimplified"; MessagesFile: "ChineseSimplified.isl"
Name: "chinesetraditional"; MessagesFile: "ChineseTraditional.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
Source: "{#MyAppSrc}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
; NOTE: Don't use "Flags: ignoreversion" on any shared system files

[Icons]
Name: "{autoprograms}\{#MyAppName}"; Filename: "{app}\{#MyAppLaunchPath}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppLaunchPath}"; Tasks: desktopicon

[Run]
Filename: "{app}\{#MyAppLaunchPath}"; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent
