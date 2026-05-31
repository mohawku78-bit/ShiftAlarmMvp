param(
    [ValidateSet("preview", "control", "orphan", "stop", "snooze")]
    [string]$Mode = "preview",
    [string]$PhoneLog = "",
    [string]$WatchLog = "",
    [string]$OutputDir = "manual-validation\watch-alarm",
    [ValidateSet("any", "stop", "snooze")]
    [string]$ExpectedAction = "any",
    [ValidateSet("any", "notification", "fallback")]
    [string]$ExpectedDisplayMode = "notification",
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

function Resolve-SmokeTimestamp {
    param([string]$Path)

    $fileName = [System.IO.Path]::GetFileName($Path)
    if ($fileName -match "$Mode-(\d{8}-\d{6})\.log$") {
        return $Matches[1]
    }
    return ""
}

function Read-OptionalText {
    param([string]$Path)

    if ([string]::IsNullOrWhiteSpace($Path) -or -not (Test-Path -LiteralPath $Path)) {
        return ""
    }
    return Get-Content -Raw -Encoding UTF8 -LiteralPath $Path
}

function Resolve-DiagnosticPath {
    param(
        [string]$DeviceLabel,
        [string]$Kind,
        [string]$Timestamp
    )

    if (-not [string]::IsNullOrWhiteSpace($Timestamp)) {
        $expected = Join-Path $ResolvedOutputDir "$DeviceLabel-diagnostics-$Mode-$Timestamp-$Kind.txt"
        if (Test-Path -LiteralPath $expected) {
            return $expected
        }
    }

    $latest = Get-ChildItem -LiteralPath $ResolvedOutputDir -Filter "$DeviceLabel-diagnostics-$Mode-*-$Kind.txt" -File -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if ($latest) {
        return $latest.FullName
    }
    return ""
}

function Read-Diagnostics {
    param(
        [string]$DeviceLabel,
        [string]$Timestamp
    )

    [pscustomobject]@{
        Package = Read-OptionalText -Path (Resolve-DiagnosticPath -DeviceLabel $DeviceLabel -Kind "package" -Timestamp $Timestamp)
        Channels = Read-OptionalText -Path (Resolve-DiagnosticPath -DeviceLabel $DeviceLabel -Kind "notification-channels" -Timestamp $Timestamp)
        AppOps = Read-OptionalText -Path (Resolve-DiagnosticPath -DeviceLabel $DeviceLabel -Kind "appops-post-notification" -Timestamp $Timestamp)
    }
}

function Write-DeviceDiagnosticHints {
    param(
        [string]$Label,
        [object]$Diagnostics
    )

    $combined = @(
        $Diagnostics.Package,
        $Diagnostics.Channels,
        $Diagnostics.AppOps
    ) -join [Environment]::NewLine
    if ([string]::IsNullOrWhiteSpace($combined)) {
        Write-Host "  No $Label permission/channel diagnostics found."
        return
    }

    $hints = Select-InterestingLines -Text $combined -Patterns @(
        "POST_NOTIFICATIONS",
        "granted=",
        "POST_NOTIFICATION",
        "allow|deny|ignore|default",
        "channel",
        "importance",
        "blocked",
        "vibration",
        "shift_alarm",
        "watch_alarm"
    ) -MaxLines 30
    if ($hints.Count -gt 0) {
        $hints | ForEach-Object { Write-Host "  $_" }
    } else {
        Write-Host "  No matching $Label diagnostic lines found."
    }
}

function Write-DiagnosticHints {
    param(
        [string]$PhoneText,
        [string]$WatchText,
        [object]$PhoneDiagnostics,
        [object]$WatchDiagnostics
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
        "show alarm notification skipped permission",
        "show alarm notification failed",
        "show alarm activity",
        "alarm display unavailable",
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

    Write-Host ""
    Write-Host "Phone permission/channel diagnostics:"
    Write-DeviceDiagnosticHints -Label "phone" -Diagnostics $PhoneDiagnostics

    Write-Host ""
    Write-Host "Watch permission/channel diagnostics:"
    Write-DeviceDiagnosticHints -Label "watch" -Diagnostics $WatchDiagnostics
}

if ([string]::IsNullOrWhiteSpace($PhoneLog)) {
    $PhoneLog = Resolve-LatestLog -Pattern "phone-watch-smoke-$Mode-*.log" -Label "phone"
}

if ([string]::IsNullOrWhiteSpace($WatchLog)) {
    $WatchLog = Resolve-LatestLog -Pattern "watch-smoke-$Mode-*.log" -Label "watch"
}

$phone = Read-Log -Path $PhoneLog -Label "Phone"
$watch = Read-Log -Path $WatchLog -Label "Watch"
$smokeTimestamp = Resolve-SmokeTimestamp -Path $PhoneLog
if ([string]::IsNullOrWhiteSpace($smokeTimestamp)) {
    $smokeTimestamp = Resolve-SmokeTimestamp -Path $WatchLog
}
$phoneDiagnostics = Read-Diagnostics -DeviceLabel "phone" -Timestamp $smokeTimestamp
$watchDiagnostics = Read-Diagnostics -DeviceLabel "watch" -Timestamp $smokeTimestamp
$checks = @()
$AssertionMode = if ($Mode -in @("stop", "snooze")) { "control" } else { $Mode }
$ResolvedExpectedAction = if ($Mode -in @("stop", "snooze") -and $ExpectedAction -eq "any") {
    $Mode
} else {
    $ExpectedAction
}
$expectedAlarmId = switch ($AssertionMode) {
    "control" { "888887" }
    default { "888888" }
}
$watchAlarmSignalPattern = "show alarm( signal)? alarmId=$expectedAlarmId"
$watchAlarmNotificationPattern = "show alarm notification alarmId=$expectedAlarmId"
$watchAlarmNotificationSkippedPattern = "show alarm notification skipped permission alarmId=$expectedAlarmId"
$watchAlarmActivityPattern = "show alarm activity alarmId=$expectedAlarmId"
$watchAlarmStartPattern = "alarm start message alarmId=$expectedAlarmId|alarm active data alarmId=$expectedAlarmId"
$phoneAnyAckPattern = "watch ack (message|data) alarmId=$expectedAlarmId\b"
$phoneNotificationAckPattern = "watch ack (message|data) alarmId=$expectedAlarmId\b.*displayMode=notification"
$phoneFallbackAckPattern = "watch ack (message|data) alarmId=$expectedAlarmId\b.*displayMode=fallback"

function Add-DisplayPathChecks {
    param(
        [object[]]$Checks,
        [string]$LabelPrefix = "watch"
    )

    if ($ExpectedDisplayMode -eq "fallback") {
        $Checks += Add-Check "$LabelPrefix detected blocked notification fallback" ($watch -match $watchAlarmNotificationSkippedPattern)
        $Checks += Add-Check "$LabelPrefix opened fallback alarm screen" ($watch -match $watchAlarmActivityPattern)
        $Checks += Add-Check "$LabelPrefix did not report unavailable display" ($watch -notmatch "alarm display unavailable alarmId=$expectedAlarmId")
        $Checks += Add-Check "phone confirmed fallback display mode" ($phone -match $phoneFallbackAckPattern)
    } elseif ($ExpectedDisplayMode -eq "any") {
        $Checks += Add-Check "$LabelPrefix showed notification or fallback controls" (($watch -match $watchAlarmNotificationPattern) -or ($watch -match $watchAlarmActivityPattern))
        $Checks += Add-Check "phone confirmed any display mode" ($phone -match $phoneAnyAckPattern)
    } else {
        $Checks += Add-Check "$LabelPrefix showed notification controls" ($watch -match $watchAlarmNotificationPattern)
        $Checks += Add-Check "phone confirmed notification display mode" ($phone -match $phoneNotificationAckPattern)
    }

    return $Checks
}

if ($AssertionMode -eq "preview") {
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
    $checks += Add-Check "watch received alarm start" ($watch -match $watchAlarmStartPattern) "alarmId=$expectedAlarmId"
    $checks += Add-Check "watch exposed alarm controls" ($watch -match $watchAlarmSignalPattern)
    $checks += Add-Check "watch acknowledged display path" ($phone -match $phoneAnyAckPattern) "alarmId=$expectedAlarmId expectedDisplay=$ExpectedDisplayMode"
    $checks = Add-DisplayPathChecks -Checks $checks -LabelPrefix "watch"
}

if ($AssertionMode -eq "control") {
    $acceptedStop = $phone -match "stop from watch alarmId=$expectedAlarmId"
    $acceptedSnooze = $phone -match "snooze from watch alarmId=$expectedAlarmId"
    $acceptedAction = $acceptedStop -or $acceptedSnooze
    $expectedControlPathPattern = switch ($ResolvedExpectedAction) {
        "stop" { "/shift_alarm/alarm/stop" }
        "snooze" { "/shift_alarm/alarm/snooze" }
        default { "/shift_alarm/alarm/(stop|snooze)" }
    }

    $expectedActionAccepted = switch ($ResolvedExpectedAction) {
        "stop" { $acceptedStop }
        "snooze" { $acceptedSnooze }
        default { $acceptedAction }
    }

    $checks += Add-Check "phone control broadcast started alarm" ($phone -match "broadcast watch control test start")
    $checks += Add-Check "phone sent active alarm to watch" ($phone -match "sendAlarmStarted alarmId=$expectedAlarmId|sendMessage path=/shift_alarm/alarm/start") "alarmId=$expectedAlarmId"
    $checks += Add-Check "watch received alarm start" ($watch -match $watchAlarmStartPattern) "alarmId=$expectedAlarmId"
    $checks += Add-Check "watch exposed alarm controls" ($watch -match $watchAlarmSignalPattern)
    $checks = Add-DisplayPathChecks -Checks $checks -LabelPrefix "watch"
    if ($AutoWatchActionSource -eq "hardwareKey") {
        $checks += Add-Check "watch received hardware key control" ($watch -match "hardware key control keyCode=.*action=$expectedControlPathPattern alarmId=$expectedAlarmId") "expected=$ResolvedExpectedAction alarmId=$expectedAlarmId"
    }
    $checks += Add-Check "watch sent expected control" ($watch -match "(send control path=$expectedControlPathPattern|put control data path=$expectedControlPathPattern).*alarmId=$expectedAlarmId") "expected=$ResolvedExpectedAction alarmId=$expectedAlarmId"
    $checks += Add-Check "phone received expected watch control" ($phone -match "watch control message path=$expectedControlPathPattern alarmId=$expectedAlarmId|watch control data action=$expectedControlPathPattern alarmId=$expectedAlarmId") "expected=$ResolvedExpectedAction alarmId=$expectedAlarmId"
    $checks += Add-Check "phone accepted expected watch control" $expectedActionAccepted "expected=$ResolvedExpectedAction alarmId=$expectedAlarmId stop=$acceptedStop snooze=$acceptedSnooze"
    $checks += Add-Check "watch received expected phone control ack" ($watch -match "control ack message action=$expectedControlPathPattern alarmId=$expectedAlarmId|control ack data action=$expectedControlPathPattern alarmId=$expectedAlarmId") "expected=$ResolvedExpectedAction alarmId=$expectedAlarmId"
    $checks += Add-Check "watch cleared local notification" ($watch -match "cancel alarm notification")
}

if ($AssertionMode -eq "orphan") {
    $expectedControlPathPattern = switch ($ResolvedExpectedAction) {
        "stop" { "/shift_alarm/alarm/stop" }
        "snooze" { "/shift_alarm/alarm/snooze" }
        default { "/shift_alarm/alarm/(stop|snooze)" }
    }

    $checks += Add-Check "phone orphan preview broadcast ran" ($phone -match "broadcast watch preview")
    $checks += Add-Check "phone sent orphan alarm to watch" ($phone -match "watch preview result connected=|sendMessage path=/shift_alarm/alarm/start|putDataItem path=/shift_alarm/alarm/active")
    $checks += Add-Check "watch received orphan alarm start" ($watch -match $watchAlarmStartPattern) "alarmId=$expectedAlarmId"
    $checks += Add-Check "watch exposed orphan alarm controls" ($watch -match $watchAlarmSignalPattern)
    $checks = Add-DisplayPathChecks -Checks $checks -LabelPrefix "watch orphan"
    if ($AutoWatchActionSource -eq "hardwareKey") {
        $checks += Add-Check "watch received hardware key control" ($watch -match "hardware key control keyCode=.*action=$expectedControlPathPattern alarmId=$expectedAlarmId") "expected=$ResolvedExpectedAction alarmId=$expectedAlarmId"
    }
    $checks += Add-Check "watch sent orphan expected control" ($watch -match "(send control path=$expectedControlPathPattern|put control data path=$expectedControlPathPattern).*alarmId=$expectedAlarmId") "expected=$ResolvedExpectedAction alarmId=$expectedAlarmId"
    $checks += Add-Check "phone received orphan expected watch control" ($phone -match "watch control message path=$expectedControlPathPattern alarmId=$expectedAlarmId|watch control data action=$expectedControlPathPattern alarmId=$expectedAlarmId") "expected=$ResolvedExpectedAction alarmId=$expectedAlarmId"
    $checks += Add-Check "phone rejected orphan expected watch control as stale" ($phone -match "ignore stale control action=$expectedControlPathPattern alarmId=$expectedAlarmId") "expected=$ResolvedExpectedAction alarmId=$expectedAlarmId"
    $checks += Add-Check "phone sent timestamp cancel for orphan watch alarm" ($phone -match "sendAlarmCancelled alarmId=$expectedAlarmId.*clearActive=false")
    $checks += Add-Check "watch received orphan cancel" ($watch -match "alarm cancel message alarmId=$expectedAlarmId|alarm cancelled data alarmId=$expectedAlarmId")
    $checks += Add-Check "watch cancelled orphan alarm" ($watch -match "cancel alarm alarmId=$expectedAlarmId")
    $checks += Add-Check "watch skipped orphan restore after cancel" ($watch -match "skip control ack timeout restore inactive alarm action=$expectedControlPathPattern alarmId=$expectedAlarmId")
    $checks += Add-Check "watch did not restore orphan alarm after cancel" ($watch -notmatch "control ack timeout action=$expectedControlPathPattern alarmId=$expectedAlarmId")
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
    Write-DiagnosticHints -PhoneText $phone -WatchText $watch -PhoneDiagnostics $phoneDiagnostics -WatchDiagnostics $watchDiagnostics
    throw "Watch smoke assertion failed: $($failed.Count) check(s) failed."
}

Write-Host ""
Write-Host "Watch smoke assertion passed."
