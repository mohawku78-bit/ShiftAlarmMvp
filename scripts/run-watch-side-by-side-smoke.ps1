param(
    [Parameter(Mandatory = $true)]
    [string]$PhoneSerial,
    [Parameter(Mandatory = $true)]
    [string]$WatchSerial,
    [ValidateSet("preview", "control", "stop")]
    [string]$Mode = "preview",
    [string]$PackageName = "com.example.shiftalarmmvp.next",
    [string]$AdbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
    [string]$OutputDir = "manual-validation\watch-alarm",
    [string]$Label = "",
    [ValidateSet("ALARM", "NOTIFICATION")]
    [string]$SoundType = "ALARM",
    [int]$SnoozeMinutes = 1,
    [int]$VolumePercent = 70,
    [bool]$VibrationEnabled = $true,
    [ValidateSet("none", "stop", "snooze")]
    [string]$AutoWatchAction = "none",
    [ValidateSet("broadcast", "hardwareKey")]
    [string]$AutoWatchActionSource = "broadcast",
    [string]$AutoWatchStopKeyCode = "KEYCODE_STEM_PRIMARY",
    [string]$AutoWatchSnoozeKeyCode = "KEYCODE_BACK",
    [int]$AutoWatchActionDelaySeconds = 4,
    [int]$AutoWatchActionAttempts = 3,
    [int]$AutoWatchActionRetrySeconds = 2,
    [int]$WaitSeconds = 20,
    [switch]$Assert,
    [ValidateSet("any", "stop", "snooze")]
    [string]$ExpectedAction = "any",
    [switch]$Clear
)

$ErrorActionPreference = "Stop"

$RootDir = Split-Path -Parent $PSScriptRoot
$ResolvedOutputDir = Join-Path $RootDir $OutputDir

$Actions = @{
    preview = "com.example.shiftalarmmvp.action.WATCH_PREVIEW_TEST"
    control = "com.example.shiftalarmmvp.action.WATCH_CONTROL_TEST"
    stop = "com.example.shiftalarmmvp.action.WATCH_CONTROL_TEST_STOP"
}

$WatchActions = @{
    stop = "com.example.shiftalarmmvp.action.WATCH_TEST_STOP"
    snooze = "com.example.shiftalarmmvp.action.WATCH_TEST_SNOOZE"
}

$HardwareKeys = @{
    stop = $AutoWatchStopKeyCode
    snooze = $AutoWatchSnoozeKeyCode
}

function Invoke-Adb {
    param(
        [Parameter(ValueFromRemainingArguments = $true)]
        [string[]]$Args
    )
    & $AdbPath @Args
}

function Assert-Device {
    param(
        [string]$Serial,
        [string]$Label
    )

    $state = (Invoke-Adb -s $Serial get-state) -join ""
    if ($state.Trim() -ne "device") {
        throw "$Label is not ready through adb: $Serial ($state)"
    }
}

function Save-FilteredLog {
    param(
        [string]$Serial,
        [string]$Path,
        [string[]]$Tags
    )

    $args = @("-s", $Serial, "logcat", "-d", "-v", "time") + $Tags + @("*:S")
    Invoke-Adb @args | Set-Content -Encoding UTF8 -LiteralPath $Path
}

if (-not (Test-Path -LiteralPath $AdbPath)) {
    throw "adb not found: $AdbPath"
}

New-Item -ItemType Directory -Force -Path $ResolvedOutputDir | Out-Null

Write-Host "Connected devices:"
Invoke-Adb devices -l

Assert-Device -Serial $PhoneSerial -Label "Phone"
Assert-Device -Serial $WatchSerial -Label "Watch"

if ($AutoWatchAction -ne "none" -and $Mode -ne "control") {
    throw "-AutoWatchAction can only be used with -Mode control."
}

if ($Clear) {
    Invoke-Adb -s $PhoneSerial logcat -c
    Invoke-Adb -s $WatchSerial logcat -c
}

$action = $Actions[$Mode]
$resolvedLabel = if ([string]::IsNullOrWhiteSpace($Label)) {
    if ($Mode -eq "preview") { "ADB watch preview" } else { "ADB watch control test" }
} else {
    $Label
}

$broadcastArgs = @(
    "-s", $PhoneSerial,
    "shell", "am", "broadcast",
    "-p", $PackageName,
    "-a", $action,
    "--es", "label", $resolvedLabel,
    "--es", "soundType", $SoundType,
    "--ei", "snoozeMinutes", ([Math]::Max(1, [Math]::Min(60, $SnoozeMinutes))).ToString(),
    "--ei", "volumePercent", ([Math]::Max(0, [Math]::Min(100, $VolumePercent))).ToString(),
    "--ez", "vibrationEnabled", $VibrationEnabled.ToString().ToLowerInvariant()
)

Write-Host ""
Write-Host "Sending $Mode smoke broadcast to $PackageName on phone $PhoneSerial"
Invoke-Adb @broadcastArgs

if ($Mode -eq "control") {
    if ($AutoWatchAction -eq "none") {
        Write-Host ""
        Write-Host "Control mode: tap Stop or Snooze on the watch during the wait window."
    } else {
        $delay = [Math]::Max(0, $AutoWatchActionDelaySeconds)
        if ($delay -gt 0) {
            Write-Host "Waiting $delay seconds before sending automatic watch $AutoWatchAction action..."
            Start-Sleep -Seconds $delay
        }

        $watchAction = $WatchActions[$AutoWatchAction]
        $hardwareKey = $HardwareKeys[$AutoWatchAction]
        $attempts = [Math]::Max(1, $AutoWatchActionAttempts)
        $retrySeconds = [Math]::Max(0, $AutoWatchActionRetrySeconds)
        for ($attempt = 1; $attempt -le $attempts; $attempt++) {
            Write-Host ""
            if ($AutoWatchActionSource -eq "hardwareKey") {
                Write-Host "Sending automatic watch $AutoWatchAction hardware key $hardwareKey attempt $attempt/$attempts on watch $WatchSerial"
                Invoke-Adb -s $WatchSerial shell input keyevent $hardwareKey
            } else {
                Write-Host "Sending automatic watch $AutoWatchAction broadcast attempt $attempt/$attempts to $PackageName on watch $WatchSerial"
                Invoke-Adb -s $WatchSerial shell am broadcast -p $PackageName -a $watchAction
            }
            if ($attempt -lt $attempts -and $retrySeconds -gt 0) {
                Start-Sleep -Seconds $retrySeconds
            }
        }
    }
}

if ($WaitSeconds -gt 0) {
    Write-Host "Waiting $WaitSeconds seconds before collecting filtered logs..."
    Start-Sleep -Seconds $WaitSeconds
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$phoneLog = Join-Path $ResolvedOutputDir "phone-watch-smoke-$Mode-$timestamp.log"
$watchLog = Join-Path $ResolvedOutputDir "watch-smoke-$Mode-$timestamp.log"

Save-FilteredLog -Serial $PhoneSerial -Path $phoneLog -Tags @("ShiftWatchTest:I", "ShiftWatchBridge:I", "ShiftWearAlarm:I")
Save-FilteredLog -Serial $WatchSerial -Path $watchLog -Tags @("ShiftWearAlarm:I", "ShiftWatchBridge:I")

Write-Host ""
Write-Host "Phone log: $phoneLog"
Write-Host "Watch log: $watchLog"
Write-Host "Smoke trigger complete."

if ($Assert -and $Mode -ne "stop") {
    Write-Host ""
    Write-Host "Running smoke assertion."
    $resolvedExpectedAction = if ($Mode -eq "control" -and $AutoWatchAction -ne "none" -and $ExpectedAction -eq "any") {
        $AutoWatchAction
    } else {
        $ExpectedAction
    }
    & powershell -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "assert-watch-smoke-result.ps1") `
        -Mode $Mode `
        -PhoneLog $phoneLog `
        -WatchLog $watchLog `
        -ExpectedAction $resolvedExpectedAction `
        -AutoWatchActionSource $AutoWatchActionSource
}
