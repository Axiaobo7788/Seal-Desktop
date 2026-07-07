param(
  [Parameter(Mandatory = $true)]
  [string]$AppRoot,

  [Parameter(Mandatory = $true)]
  [string]$LaunchPath,

  [string]$Label = "Windows app image",

  [int]$TimeoutSeconds = 12
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

$rootPath = (Resolve-Path $AppRoot).Path
$launcher = Resolve-SmokeLauncher $rootPath $LaunchPath
$tempRoot = if ([string]::IsNullOrWhiteSpace($env:RUNNER_TEMP)) { [System.IO.Path]::GetTempPath() } else { $env:RUNNER_TEMP }
$stdoutPath = Join-Path $tempRoot ("seal-smoke-{0}-stdout.log" -f ([Guid]::NewGuid().ToString("N")))
$stderrPath = Join-Path $tempRoot ("seal-smoke-{0}-stderr.log" -f ([Guid]::NewGuid().ToString("N")))

Write-Host "$Label root: $rootPath"
Write-Host "$Label configured launch path: $LaunchPath"
Write-Host "$Label smoke launcher: $($launcher.DiagnosticPath)"

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

$process = [System.Diagnostics.Process]::new()
$process.StartInfo = $startInfo
$process.Start() | Out-Null

$stdoutTask = $process.StandardOutput.ReadToEndAsync()
$stderrTask = $process.StandardError.ReadToEndAsync()
$exited = $process.WaitForExit($TimeoutSeconds * 1000)

if ($exited) {
  $stdoutTask.Wait()
  $stderrTask.Wait()
  $stdoutTask.Result | Out-File -FilePath $stdoutPath -Encoding utf8
  $stderrTask.Result | Out-File -FilePath $stderrPath -Encoding utf8

  if ($stdoutTask.Result.Trim().Length -gt 0) {
    Write-Host "----- $Label stdout -----"
    Write-Host $stdoutTask.Result
    Write-Host "----- end stdout -----"
  }
  if ($stderrTask.Result.Trim().Length -gt 0) {
    Write-Host "----- $Label stderr -----"
    Write-Host $stderrTask.Result
    Write-Host "----- end stderr -----"
  }

  if ($process.ExitCode -ne 0) {
    throw "$Label exited during smoke test with code $($process.ExitCode)."
  }

  Write-Host "$Label exited cleanly during smoke test."
  exit 0
}

Write-Host "$Label stayed alive for $TimeoutSeconds seconds; treating app-image startup as successful."
try {
  $process.Kill($true)
} catch {
  Write-Host "Failed to terminate smoke process cleanly: $($_.Exception.Message)"
}
