param(
    [string]$PhoneSerial,
    [string]$WatchSerial,
    [string]$AdbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
    [string]$OutputDir = "manual-validation\watch-alarm",
    [switch]$Clear
)

$ErrorActionPreference = "Stop"

$RootDir = Split-Path -Parent $PSScriptRoot
$ResolvedOutputDir = Join-Path $RootDir $OutputDir

function Invoke-Adb {
    param(
        [Parameter(ValueFromRemainingArguments = $true)]
        [string[]]$Args
    )
    & $AdbPath @Args
}

if (-not (Test-Path -LiteralPath $AdbPath)) {
    throw "adb not found: $AdbPath"
}

New-Item -ItemType Directory -Force -Path $ResolvedOutputDir | Out-Null

Write-Host "Connected devices:"
Invoke-Adb devices -l

if ([string]::IsNullOrWhiteSpace($PhoneSerial) -or [string]::IsNullOrWhiteSpace($WatchSerial)) {
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\collect-watch-alarm-logcat.ps1 -PhoneSerial PHONE_SERIAL -WatchSerial WATCH_SERIAL"
    Write-Host ""
    Write-Host "Use Ctrl+C to stop collection after the preview/control test."
    exit 2
}

if ($Clear) {
    Invoke-Adb -s $PhoneSerial logcat -c
    Invoke-Adb -s $WatchSerial logcat -c
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$phoneLog = Join-Path $ResolvedOutputDir "phone-watch-alarm-$timestamp.log"
$watchLog = Join-Path $ResolvedOutputDir "watch-alarm-$timestamp.log"

Write-Host "Writing phone log: $phoneLog"
Write-Host "Writing watch log: $watchLog"
Write-Host "Use Ctrl+C to stop collection."

$phoneArgs = @(
    "-s", $PhoneSerial,
    "logcat",
    "-v", "time",
    "ShiftWatchTest:I",
    "ShiftWatchBridge:I",
    "ShiftWearAlarm:I",
    "*:S"
)
$watchArgs = @(
    "-s", $WatchSerial,
    "logcat",
    "-v", "time",
    "ShiftWearAlarm:I",
    "ShiftWatchBridge:I",
    "*:S"
)

$phoneProcess = Start-Process -FilePath $AdbPath -ArgumentList $phoneArgs -RedirectStandardOutput $phoneLog -NoNewWindow -PassThru
$watchProcess = Start-Process -FilePath $AdbPath -ArgumentList $watchArgs -RedirectStandardOutput $watchLog -NoNewWindow -PassThru

try {
    while (-not $phoneProcess.HasExited -and -not $watchProcess.HasExited) {
        Start-Sleep -Seconds 1
    }
} finally {
    if (-not $phoneProcess.HasExited) { Stop-Process -Id $phoneProcess.Id -Force }
    if (-not $watchProcess.HasExited) { Stop-Process -Id $watchProcess.Id -Force }
}
