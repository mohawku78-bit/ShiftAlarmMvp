param(
    [ValidateSet("preview", "control")]
    [string]$Mode = "preview",
    [string]$PhoneLog = "",
    [string]$WatchLog = "",
    [string]$OutputDir = "manual-validation\watch-alarm",
    [ValidateSet("any", "stop", "snooze")]
    [string]$ExpectedAction = "any"
)

$ErrorActionPreference = "Stop"

$RootDir = Split-Path -Parent $PSScriptRoot
$ResolvedOutputDir = Join-Path $RootDir $OutputDir

function Resolve-LatestLog {
    param(
        [string]$Pattern,
        [string]$Label
    )

    $file = Get-ChildItem -LiteralPath $ResolvedOutputDir -Filter $Pattern -File -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1

    if (-not $file) {
        throw "No $Label log found under $ResolvedOutputDir matching $Pattern"
    }

    return $file.FullName
}

function Read-Log {
    param(
        [string]$Path,
        [string]$Label
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        throw "$Label log not found: $Path"
    }

    return Get-Content -Raw -Encoding UTF8 -LiteralPath $Path
}

function Add-Check {
    param(
        [string]$Name,
        [bool]$Passed,
        [string]$Detail = ""
    )

    [pscustomobject]@{
        Name = $Name
        Passed = $Passed
        Detail = $Detail
    }
}

if ([string]::IsNullOrWhiteSpace($PhoneLog)) {
    $PhoneLog = Resolve-LatestLog -Pattern "phone-watch-smoke-$Mode-*.log" -Label "phone"
}

if ([string]::IsNullOrWhiteSpace($WatchLog)) {
    $WatchLog = Resolve-LatestLog -Pattern "watch-smoke-$Mode-*.log" -Label "watch"
}

$phone = Read-Log -Path $PhoneLog -Label "Phone"
$watch = Read-Log -Path $WatchLog -Label "Watch"
$checks = @()

if ($Mode -eq "preview") {
    $previewMatch = [regex]::Match(
        $phone,
        "watch preview result connected=(\d+)\s+watchApp=(\d+)\s+messageAttempts=(\d+).*?watchAppError=([^\r\n ]+).*?error=([^\r\n ]+)",
        [System.Text.RegularExpressions.RegexOptions]::Singleline
    )

    $connectedNodes = if ($previewMatch.Success) { [int]$previewMatch.Groups[1].Value } else { 0 }
    $watchAppNodes = if ($previewMatch.Success) { [int]$previewMatch.Groups[2].Value } else { 0 }
    $messageAttempts = if ($previewMatch.Success) { [int]$previewMatch.Groups[3].Value } else { 0 }
    $watchAppError = if ($previewMatch.Success) { $previewMatch.Groups[4].Value } else { "" }
    $sendError = if ($previewMatch.Success) { $previewMatch.Groups[5].Value } else { "" }

    $checks += Add-Check "phone preview broadcast ran" ($phone -match "broadcast watch preview")
    $checks += Add-Check "phone found connected Wear node" ($connectedNodes -gt 0) "connected=$connectedNodes"
    $checks += Add-Check "phone found reachable watch app capability" ($watchAppNodes -gt 0) "watchApp=$watchAppNodes"
    $checks += Add-Check "phone attempted message send" ($messageAttempts -gt 0) "messageAttempts=$messageAttempts"
    $checks += Add-Check "phone send result has no errors" (($watchAppError -eq "null") -and ($sendError -eq "null")) "watchAppError=$watchAppError error=$sendError"
    $checks += Add-Check "watch received alarm start" ($watch -match "alarm start message|alarm active data")
    $checks += Add-Check "watch displayed alarm" ($watch -match "show alarm alarmId=")
    $checks += Add-Check "watch acknowledged display path" ($phone -match "watch ack message|watch ack data")
}

if ($Mode -eq "control") {
    $acceptedStop = $phone -match "stop from watch alarmId="
    $acceptedSnooze = $phone -match "snooze from watch alarmId="
    $acceptedAction = $acceptedStop -or $acceptedSnooze
    $expectedControlPathPattern = switch ($ExpectedAction) {
        "stop" { "/shift_alarm/alarm/stop" }
        "snooze" { "/shift_alarm/alarm/snooze" }
        default { "/shift_alarm/alarm/(stop|snooze)" }
    }

    $expectedActionAccepted = switch ($ExpectedAction) {
        "stop" { $acceptedStop }
        "snooze" { $acceptedSnooze }
        default { $acceptedAction }
    }

    $checks += Add-Check "phone control broadcast started alarm" ($phone -match "broadcast watch control test start")
    $checks += Add-Check "phone sent active alarm to watch" ($phone -match "sendAlarmStarted alarmId=888887|sendMessage path=/shift_alarm/alarm/start")
    $checks += Add-Check "watch received alarm start" ($watch -match "alarm start message|alarm active data")
    $checks += Add-Check "watch displayed alarm" ($watch -match "show alarm alarmId=888887|show alarm alarmId=")
    $checks += Add-Check "watch sent expected control" ($watch -match "send control path=$expectedControlPathPattern|put control data path=$expectedControlPathPattern") "expected=$ExpectedAction"
    $checks += Add-Check "phone received watch control" ($phone -match "watch control message path=/shift_alarm/alarm/(stop|snooze)|watch control data action=/shift_alarm/alarm/(stop|snooze)")
    $checks += Add-Check "phone accepted expected watch control" $expectedActionAccepted "expected=$ExpectedAction stop=$acceptedStop snooze=$acceptedSnooze"
    $checks += Add-Check "watch received phone control ack" ($watch -match "control ack message action=/shift_alarm/alarm/(stop|snooze)|control ack data action=/shift_alarm/alarm/(stop|snooze)")
    $checks += Add-Check "watch alarm dismissed locally" ($watch -match "request stop foreground ringing|cancel alarm alarmId=|stop foreground ringing")
}

Write-Host "Phone log: $PhoneLog"
Write-Host "Watch log: $WatchLog"
Write-Host ""
Write-Host "Watch smoke assertion: $Mode"

$failed = @()
foreach ($check in $checks) {
    $prefix = if ($check.Passed) { "[PASS]" } else { "[FAIL]" }
    $detail = if ([string]::IsNullOrWhiteSpace($check.Detail)) { "" } else { " - $($check.Detail)" }
    Write-Host "$prefix $($check.Name)$detail"
    if (-not $check.Passed) {
        $failed += $check
    }
}

if ($failed.Count -gt 0) {
    throw "Watch smoke assertion failed: $($failed.Count) check(s) failed."
}

Write-Host ""
Write-Host "Watch smoke assertion passed."
