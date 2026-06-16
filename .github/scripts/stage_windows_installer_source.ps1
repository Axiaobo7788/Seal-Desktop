param(
  [Parameter(Mandatory = $true)]
  [string]$Workspace,

  [Parameter(Mandatory = $true)]
  [string]$StageDir,

  [Parameter(Mandatory = $true)]
  [string]$EnvName,

  [string]$LaunchEnvName = "$($EnvName)_LAUNCH_PATH",

  [switch]$DebugLauncher
)

$ErrorActionPreference = "Stop"

function Find-ComposeBinariesRoot([string]$workspace) {
  $desktopBuild = Join-Path $workspace "desktop/build"
  if (-not (Test-Path $desktopBuild)) {
    throw "Build directory not found: $desktopBuild"
  }

  $binariesRoot = Join-Path $desktopBuild "compose/binaries"
  if (Test-Path $binariesRoot) {
    return (Get-Item $binariesRoot).FullName
  }

  $fallback = Get-ChildItem $desktopBuild -Recurse -Directory -Filter binaries |
    Where-Object { $_.FullName -like "*compose*binaries" } |
    Select-Object -First 1
  if ($fallback) {
    return $fallback.FullName
  }

  throw "Compose binaries path not found under: $desktopBuild"
}

function Find-WindowsAppImageRoot([string]$binariesRoot) {
  $composeDistributionCandidates = Get-ChildItem $binariesRoot -Recurse -Directory -Depth 10 |
    Where-Object {
      (Test-Path (Join-Path $_.FullName "bin")) -and
      (Test-Path (Join-Path $_.FullName "lib/runtime")) -and
      (Test-Path (Join-Path $_.FullName "lib/app"))
    }

  foreach ($candidate in ($composeDistributionCandidates | Sort-Object { $_.FullName.Length })) {
    $launchers = @(
      @{ Path = "bin\Seal.exe"; File = Join-Path $candidate.FullName "bin/Seal.exe" },
      @{ Path = "bin\Seal.bat"; File = Join-Path $candidate.FullName "bin/Seal.bat" },
      @{ Path = "bin\Seal"; File = Join-Path $candidate.FullName "bin/Seal" }
    )
    foreach ($launcher in $launchers) {
      if (Test-Path $launcher.File) {
        return [PSCustomObject]@{
          Root = $candidate.FullName
          LaunchPath = $launcher.Path
        }
      }
    }
  }

  $jpackageCandidates = Get-ChildItem $binariesRoot -Recurse -Directory -Depth 10 |
    Where-Object {
      (Test-Path (Join-Path $_.FullName "Seal.exe")) -and
      (Test-Path (Join-Path $_.FullName "runtime")) -and
      (Test-Path (Join-Path $_.FullName "app"))
    }

  if ($jpackageCandidates) {
    $candidate = $jpackageCandidates | Sort-Object { $_.FullName.Length } | Select-Object -First 1
    return [PSCustomObject]@{
      Root = $candidate.FullName
      LaunchPath = "Seal.exe"
    }
  }

  Write-Host "Available directories under ${binariesRoot} (depth<=4):"
  Get-ChildItem $binariesRoot -Directory -Recurse -Depth 4 | ForEach-Object {
    Write-Host "- $($_.FullName)"
  }
  throw "Cannot locate Windows app-image root. Expected either bin/ + lib/runtime + lib/app, or Seal.exe + runtime/ + app/."
}

function Assert-InstallerSource([string]$sourceDir, [string]$launchPath) {
  $hasComposeLayout =
    (Test-Path (Join-Path $sourceDir "lib/runtime")) -and
    (Test-Path (Join-Path $sourceDir "lib/app"))
  $hasJpackageLayout =
    (Test-Path (Join-Path $sourceDir "runtime")) -and
    (Test-Path (Join-Path $sourceDir "app"))
  $hasLauncher = Test-Path (Join-Path $sourceDir $launchPath)

  if (-not $hasLauncher -or (-not $hasComposeLayout -and -not $hasJpackageLayout)) {
    Write-Host "Files at staged installer source:"
    Get-ChildItem $sourceDir -Force | ForEach-Object { Write-Host "- $($_.Name)" }
    throw "Invalid installer source. Missing launcher or bundled runtime/app layout."
  }
}

function Write-DebugLauncher([string]$sourceDir, [string]$launchPath) {
  $debugLauncherPath = Join-Path $sourceDir "Seal_Debug.cmd"
  $content = @"
@echo off
setlocal
cd /d "%~dp0"
set JPACKAGE_DEBUG=true
echo Seal debug launcher
echo Install dir: %CD%
echo Launch target: $launchPath
echo.
"%~dp0$launchPath"
set EXIT_CODE=%ERRORLEVEL%
echo.
echo Seal exited with code %EXIT_CODE%.
echo Press any key to close this window.
pause >nul
"@
  Set-Content -Path $debugLauncherPath -Value $content -Encoding ASCII
}

function Print-Diagnostics([string]$sourceDir, [string]$launchPath) {
  Write-Host "Staged top-level entries:"
  Get-ChildItem $sourceDir -Force | ForEach-Object {
    Write-Host "- $($_.Name)"
  }

  $configPath = Join-Path $sourceDir "app/Seal.cfg"
  if (Test-Path $configPath) {
    Write-Host "----- app/Seal.cfg -----"
    Get-Content $configPath | ForEach-Object { Write-Host $_ }
    Write-Host "----- end app/Seal.cfg -----"
  } else {
    Write-Host "app/Seal.cfg not found under staged source."
  }

  Write-Host "Installer shortcut launch path: $launchPath"
}

$workspacePath = (Resolve-Path $Workspace).Path
$binariesRoot = Find-ComposeBinariesRoot $workspacePath
$appImage = Find-WindowsAppImageRoot $binariesRoot
$stagePath = [System.IO.Path]::GetFullPath($StageDir)

if (Test-Path $stagePath) {
  Remove-Item $stagePath -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $stagePath | Out-Null

Copy-Item (Join-Path $appImage.Root "*") -Destination $stagePath -Recurse -Force
Assert-InstallerSource $stagePath $appImage.LaunchPath
if ($DebugLauncher) {
  Write-DebugLauncher $stagePath $appImage.LaunchPath
}

Write-Host "Windows app-image root: $($appImage.Root)"
Write-Host "Windows launch path: $($appImage.LaunchPath)"
Write-Host "Windows debug launcher: $DebugLauncher"
Write-Host "Staged installer source: $stagePath"
if ($DebugLauncher) {
  Print-Diagnostics $stagePath "Seal_Debug.cmd"
} else {
  Print-Diagnostics $stagePath $appImage.LaunchPath
}
"$EnvName=$stagePath" | Out-File -FilePath $env:GITHUB_ENV -Append -Encoding utf8
if ($DebugLauncher) {
  "$LaunchEnvName=Seal_Debug.cmd" | Out-File -FilePath $env:GITHUB_ENV -Append -Encoding utf8
} else {
  "$LaunchEnvName=$($appImage.LaunchPath)" | Out-File -FilePath $env:GITHUB_ENV -Append -Encoding utf8
}
