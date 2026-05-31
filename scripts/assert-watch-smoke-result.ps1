param(
    [ValidateSet("preview", "control", "orphan")]
    [string]$Mode = "preview",
    [string]$PhoneLog = "",
    [string]$WatchLog = "",
    [string]$OutputDir = "manual-validation\watch-alarm",
    [ValidateSet("any", "stop", "snooze")]
    [string]$ExpectedAction = "any",
    [ValidateSet("broadcast", "hardwareKey")]
    [string]$AutoWatchActionSource = "broadcast"
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

function Select-InterestingLines {
    param(
        [string]$Text,
        [string[]]$Patterns,
        [int]$MaxLines = 40
    )

    $pattern = ($Patterns | ForEach-Object { "(?:$_)" }) -join "|"
    $matches = $Text -split "`r?`n" | Where-Object { $_ -match $pattern }
    return @($matches | Select-Object -Last $MaxLines)
}

function Write-DiagnosticHints {
    param(
        [string]$PhoneText,
        [string]$WatchText
    )

    $phoneHints = Select-InterestingLines -Text $PhoneText -Patterns @(
        "broadcast watch",
        "watch preview result",
        "capability shift_alarm_watch_control",
        "sendMessage path=/shift_alarm/alarm",
        "putDataItem path=/shift_alarm/alarm",
        "watch ack",
        "watch control",
        "stop from watch",
        "snooze from watch",
        "ignore stale control",
        "ignore duplicate",
        "sendAlarmCancelled",
        "clearActive=false",
        "sendControlAcknowledged",
        "resend accepted watch control ack"
    )

    $watchHints = Select-InterestingLines -Text $WatchText -Patterns @(
        "alarm start message",
        "alarm active data",
        "show alarm",
        "show alarm notification",
        "show alarm notification failed",
        "show fallback notification",
        "show fallback notification failed",
        "show control pending notification",
        "show control pending notification failed",
        "hardware key control",
        "side-by-side watch action",
        "without active alarm",
        "send control",
        "send control failed",
        "put control data",
        "put control data failed",
        "send ack failed",
        "put ack data failed",
        "control ack",
        "control ack timeout",
        "skip control ack timeout restore inactive alarm",
        "alarm cancel message",
        "alarm cancelled data",
        "cancel alarm",
        "cancel alarm notification"
    )

    Write-Host ""
    Write-Host "Diagnostic log hints:"
    Write-Host ""
    Write-Host "Phone:"
    if ($phoneHints.Count -gt 0) {
        $phoneHints | ForEach-Object { Write-Host "  $_" }
    } else {
        Write-Host "  No matching phone diagnostic lines found."
    }

    Write-Host ""
    Write-Host "Watch:"
    if ($watchHints.Count -gt 0) {
        $watchHints | ForEach-Object { Write-Host "  $_" }
    } else {
        Write-Host "  No matching watch diagnostic lines found."
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
$watchAlarmSignalPattern = "show alarm( signal)? alarmId="

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
    $checks += Add-Check "watch exposed alarm controls" ($watch -match $watchAlarmSignalPattern)
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
    $checks += Add-Check "watch exposed alarm controls" ($watch -match "show alarm( signal)? alarmId=888887|$watchAlarmSignalPattern")
    if ($AutoWatchActionSource -eq "hardwareKey") {
        $checks += Add-Check "watch received hardware key control" ($watch -match "hardware key control keyCode=.*action=$expectedControlPathPattern") "expected=$ExpectedAction"
    }
    $checks += Add-Check "watch sent expected control" ($watch -match "send control path=$expectedControlPathPattern|put control data path=$expectedControlPathPattern") "expected=$ExpectedAction"
    $checks += Add-Check "phone received watch control" ($phone -match "watch control message path=/shift_alarm/alarm/(stop|snooze)|watch control data action=/shift_alarm/alarm/(stop|snooze)")
    $checks += Add-Check "phone accepted expected watch control" $expectedActionAccepted "expected=$ExpectedAction stop=$acceptedStop snooze=$acceptedSnooze"
    $checks += Add-Check "watch received phone control ack" ($watch -match "control ack message action=/shift_alarm/alarm/(stop|snooze)|control ack data action=/shift_alarm/alarm/(stop|snooze)")
    $checks += Add-Check "watch cleared local notification" ($watch -match "cancel alarm notification")
}

if ($Mode -eq "orphan") {
    $expectedControlPathPattern = switch ($ExpectedAction) {
        "stop" { "/shift_alarm/alarm/stop" }
        "snooze" { "/shift_alarm/alarm/snooze" }
        default { "/shift_alarm/alarm/(stop|snooze)" }
    }

    $checks += Add-Check "phone orphan preview broadcast ran" ($phone -match "broadcast watch preview")
    $checks += Add-Check "phone sent orphan alarm to watch" ($phone -match "watch preview result connected=|sendMessage path=/shift_alarm/alarm/start|putDataItem path=/shift_alarm/alarm/active")
    $checks += Add-Check "watch received orphan alarm start" ($watch -match "alarm start message|alarm active data")
    $checks += Add-Check "watch exposed orphan alarm controls" ($watch -match "show alarm( signal)? alarmId=888888|$watchAlarmSignalPattern")
    if ($AutoWatchActionSource -eq "hardwareKey") {
        $checks += Add-Check "watch received hardware key control" ($watch -match "hardware key control keyCode=.*action=$expectedControlPathPattern") "expected=$ExpectedAction"
    }
    $checks += Add-Check "watch sent orphan expected control" ($watch -match "send control path=$expectedControlPathPattern|put control data path=$expectedControlPathPattern") "expected=$ExpectedAction"
    $checks += Add-Check "phone received orphan watch control" ($phone -match "watch control message path=/shift_alarm/alarm/(stop|snooze)|watch control data action=/shift_alarm/alarm/(stop|snooze)")
    $checks += Add-Check "phone rejected orphan watch control as stale" ($phone -match "ignore stale control action=/shift_alarm/alarm/(stop|snooze) alarmId=888888")
    $checks += Add-Check "phone sent timestamp cancel for orphan watch alarm" ($phone -match "sendAlarmCancelled alarmId=888888.*clearActive=false")
    $checks += Add-Check "watch received orphan cancel" ($watch -match "alarm cancel message alarmId=888888|alarm cancelled data alarmId=888888")
    $checks += Add-Check "watch cancelled orphan alarm" ($watch -match "cancel alarm alarmId=888888")
    $checks += Add-Check "watch skipped orphan restore after cancel" ($watch -match "skip control ack timeout restore inactive alarm action=$expectedControlPathPattern alarmId=888888")
    $checks += Add-Check "watch did not restore orphan alarm after cancel" ($watch -notmatch "control ack timeout action=$expectedControlPathPattern alarmId=888888")
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
    Write-DiagnosticHints -PhoneText $phone -WatchText $watch
    throw "Watch smoke assertion failed: $($failed.Count) check(s) failed."
}

Write-Host ""
Write-Host "Watch smoke assertion passed."
