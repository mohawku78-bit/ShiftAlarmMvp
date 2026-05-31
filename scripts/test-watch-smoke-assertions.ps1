$ErrorActionPreference = "Stop"

$RootDir = Split-Path -Parent $PSScriptRoot
$OutputDir = Join-Path $RootDir "build\tmp\watch-smoke-assertion-tests"
New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

function Write-TextFile {
    param(
        [string]$Path,
        [string]$Text
    )

    $Text.TrimStart("`r", "`n") | Set-Content -Encoding UTF8 -LiteralPath $Path
}

function Invoke-SmokeAssertion {
    param(
        [string]$Mode,
        [string]$PhoneLog,
        [string]$WatchLog,
        [bool]$ShouldPass
    )

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $output = & powershell -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "assert-watch-smoke-result.ps1") `
            -Mode $Mode `
            -PhoneLog $PhoneLog `
            -WatchLog $WatchLog `
            -ExpectedAction any 2>&1
        $exitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }

    $passed = $exitCode -eq 0
    if ($passed -ne $ShouldPass) {
        $output | ForEach-Object { Write-Host $_ }
        $expected = if ($ShouldPass) { "pass" } else { "fail" }
        $actual = if ($passed) { "passed" } else { "failed" }
        throw "Expected $Mode assertion to $expected, but it $actual."
    }
}

$phoneStop = Join-Path $OutputDir "phone-watch-smoke-stop-20260531-010001.log"
$watchStop = Join-Path $OutputDir "watch-smoke-stop-20260531-010001.log"
$phoneSnooze = Join-Path $OutputDir "phone-watch-smoke-snooze-20260531-010002.log"
$watchSnooze = Join-Path $OutputDir "watch-smoke-snooze-20260531-010002.log"
$watchWrongAck = Join-Path $OutputDir "watch-smoke-stop-20260531-010003.log"

Write-TextFile -Path $phoneStop -Text @"
broadcast watch control test start
sendAlarmStarted alarmId=888887
watch ack message alarmId=888887 label=test displayMode=notification
watch control message path=/shift_alarm/alarm/stop alarmId=888887
stop from watch alarmId=888887
"@

Write-TextFile -Path $watchStop -Text @"
alarm start message alarmId=888887
show alarm signal alarmId=888887 canSnooze=true
show alarm notification alarmId=888887
start one-shot alarm vibration alarmId=888887
send control path=/shift_alarm/alarm/stop attempt=1 nodes=1 alarmId=888887
control ack message action=/shift_alarm/alarm/stop alarmId=888887
cancel alarm notification
"@

Write-TextFile -Path $phoneSnooze -Text @"
broadcast watch control test start
sendAlarmStarted alarmId=888887
watch ack message alarmId=888887 label=test displayMode=notification
watch control message path=/shift_alarm/alarm/snooze alarmId=888887
snooze from watch alarmId=888887 minutes=1
"@

Write-TextFile -Path $watchSnooze -Text @"
alarm start message alarmId=888887
show alarm signal alarmId=888887 canSnooze=true
show alarm notification alarmId=888887
start one-shot alarm vibration alarmId=888887
send control path=/shift_alarm/alarm/snooze attempt=1 nodes=1 alarmId=888887
control ack message action=/shift_alarm/alarm/snooze alarmId=888887
cancel alarm notification
"@

Write-TextFile -Path $watchWrongAck -Text @"
alarm start message alarmId=888887
show alarm signal alarmId=888887 canSnooze=true
show alarm notification alarmId=888887
start one-shot alarm vibration alarmId=888887
send control path=/shift_alarm/alarm/stop attempt=1 nodes=1 alarmId=888887
control ack message action=/shift_alarm/alarm/snooze alarmId=888887
cancel alarm notification
"@

Invoke-SmokeAssertion -Mode stop -PhoneLog $phoneStop -WatchLog $watchStop -ShouldPass $true
Invoke-SmokeAssertion -Mode snooze -PhoneLog $phoneSnooze -WatchLog $watchSnooze -ShouldPass $true
Invoke-SmokeAssertion -Mode stop -PhoneLog $phoneStop -WatchLog $watchWrongAck -ShouldPass $false

Write-Host "Watch smoke assertion self-test: OK"
