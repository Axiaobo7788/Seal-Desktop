param(
  [Parameter(Mandatory = $true)]
  [string]$AppRoot,

  [Parameter(Mandatory = $true)]
  [string]$LaunchPath,

  [string]$Label = "Windows app image",

  [int]$TimeoutSeconds = 12,

  [switch]$VerifySqlite
)

$ErrorActionPreference = "Stop"

function Resolve-SmokeLauncher([string]$root, [string]$launchPath) {
  $nativeLauncher = Join-Path $root $launchPath
  if (-not (Test-Path $nativeLauncher)) {
    throw "$Label launch target not found: $nativeLauncher"
  }

  if ($nativeLauncher.EndsWith(".exe", [System.StringComparison]::OrdinalIgnoreCase)) {
    $batLauncher = [System.IO.Path]::ChangeExtension($nativeLauncher, ".bat")
    if (Test-Path $batLauncher) {
      return [PSCustomObject]@{
        FileName = "cmd.exe"
        Arguments = "/c ""$batLauncher"""
        DiagnosticPath = $batLauncher
      }
    }
  }

  return [PSCustomObject]@{
    FileName = $nativeLauncher
    Arguments = ""
    DiagnosticPath = $nativeLauncher
  }
}

function Stop-SmokeProcess([System.Diagnostics.Process]$process) {
  if (-not $process.HasExited) {
    try {
      $process.Kill($true)
    } catch {
      Write-Host "Failed to terminate smoke process cleanly: $($_.Exception.Message)"
    }
  }
  $process.WaitForExit()
}

function Write-SmokeLogs([string]$stdout, [string]$stderr, [switch]$Print) {
  $stdout | Out-File -FilePath $stdoutPath -Encoding utf8
  $stderr | Out-File -FilePath $stderrPath -Encoding utf8

  if ($Print -and $stdout.Trim().Length -gt 0) {
    Write-Host "----- $Label stdout -----"
    Write-Host $stdout
    Write-Host "----- end stdout -----"
  }
  if ($Print -and $stderr.Trim().Length -gt 0) {
    Write-Host "----- $Label stderr -----"
    Write-Host $stderr
    Write-Host "----- end stderr -----"
  }
}

$rootPath = (Resolve-Path $AppRoot).Path
$launcher = Resolve-SmokeLauncher $rootPath $LaunchPath
$tempRoot = if ([string]::IsNullOrWhiteSpace($env:RUNNER_TEMP)) { [System.IO.Path]::GetTempPath() } else { $env:RUNNER_TEMP }
$stdoutPath = Join-Path $tempRoot ("seal-smoke-{0}-stdout.log" -f ([Guid]::NewGuid().ToString("N")))
$stderrPath = Join-Path $tempRoot ("seal-smoke-{0}-stderr.log" -f ([Guid]::NewGuid().ToString("N")))
$stateRoot = $null
$databasePath = $null
if ($VerifySqlite) {
  $stateRoot = Join-Path $tempRoot ("seal-sqlite-smoke-{0}" -f ([Guid]::NewGuid().ToString("N")))
  New-Item -ItemType Directory -Path $stateRoot -Force | Out-Null
  $databasePath = Join-Path $stateRoot "seal\seal.db"
}

Write-Host "$Label root: $rootPath"
Write-Host "$Label configured launch path: $LaunchPath"
Write-Host "$Label smoke launcher: $($launcher.DiagnosticPath)"
if ($VerifySqlite) {
  Write-Host "$Label SQLite state root: $stateRoot"
}

$startInfo = [System.Diagnostics.ProcessStartInfo]::new()
$startInfo.FileName = $launcher.FileName
$startInfo.WorkingDirectory = $rootPath
$startInfo.UseShellExecute = $false
$startInfo.RedirectStandardOutput = $true
$startInfo.RedirectStandardError = $true
$startInfo.CreateNoWindow = $true
if (-not [string]::IsNullOrWhiteSpace($launcher.Arguments)) {
  $startInfo.Arguments = $launcher.Arguments
}
$startInfo.Environment["JPACKAGE_DEBUG"] = "true"
if ($VerifySqlite) {
  $startInfo.Environment["SEAL_DESKTOP_STORAGE_BACKEND"] = "sqlite"
  $startInfo.Environment["SEAL_DESKTOP_STORAGE_STATE_DIR"] = $stateRoot
}

$process = [System.Diagnostics.Process]::new()
$process.StartInfo = $startInfo
$processStarted = $false
try {
  $process.Start() | Out-Null
  $processStarted = $true

  $stdoutTask = $process.StandardOutput.ReadToEndAsync()
  $stderrTask = $process.StandardError.ReadToEndAsync()
  $exited = $process.WaitForExit($TimeoutSeconds * 1000)

  if ($exited) {
    $stdoutTask.Wait()
    $stderrTask.Wait()
    Write-SmokeLogs $stdoutTask.Result $stderrTask.Result -Print
    throw "$Label exited before the $TimeoutSeconds-second smoke-test window completed with code $($process.ExitCode)."
  }

  if ($VerifySqlite) {
    if (-not (Test-Path $databasePath) -or (Get-Item $databasePath).Length -le 0) {
      Stop-SmokeProcess $process
      $stdoutTask.Wait()
      $stderrTask.Wait()
      Write-SmokeLogs $stdoutTask.Result $stderrTask.Result -Print
      throw "$Label stayed alive but did not create a non-empty SQLite database: $databasePath"
    }
  }

  Stop-SmokeProcess $process
  $stdoutTask.Wait()
  $stderrTask.Wait()
  $combinedOutput = "$($stdoutTask.Result)`n$($stderrTask.Result)"
  $sqliteFailurePattern = "sqlite_storage_warning|No suitable driver|UnsatisfiedLinkError"
  if ($VerifySqlite -and $combinedOutput -match $sqliteFailurePattern) {
    Write-SmokeLogs $stdoutTask.Result $stderrTask.Result -Print
    throw "$Label reported a SQLite driver or native-library failure."
  }
  Write-SmokeLogs $stdoutTask.Result $stderrTask.Result

  if ($VerifySqlite) {
    Write-Host "$Label created SQLite database: $databasePath ($((Get-Item $databasePath).Length) bytes)"
  }
  Write-Host "$Label stayed alive for $TimeoutSeconds seconds; startup succeeded."
} finally {
  if ($processStarted -and -not $process.HasExited) {
    Stop-SmokeProcess $process
  }
  $process.Dispose()
  if ($VerifySqlite -and (Test-Path $stateRoot)) {
    Remove-Item -Recurse -Force $stateRoot
  }
}
