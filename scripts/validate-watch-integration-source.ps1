$ErrorActionPreference = "Stop"

$RootDir = Split-Path -Parent $PSScriptRoot

function Read-RepoFile {
    param([string]$RelativePath)
    $path = Join-Path $RootDir $RelativePath
    if (-not (Test-Path -LiteralPath $path)) {
        throw "Missing required file: $RelativePath"
    }
    return Get-Content -Raw -Encoding UTF8 -LiteralPath $path
}

function Assert-Contains {
    param(
        [string]$Name,
        [string]$Text,
        [string]$Pattern
    )
    if ($Text -notmatch [regex]::Escape($Pattern)) {
        throw "$Name does not contain required text: $Pattern"
    }
}

function Assert-Regex {
    param(
        [string]$Name,
        [string]$Text,
        [string]$Pattern
    )
    if ($Text -notmatch $Pattern) {
        throw "$Name does not match required pattern: $Pattern"
    }
}

$settings = Read-RepoFile "settings.gradle.kts"
$appBuild = Read-RepoFile "app\build.gradle.kts"
$wearBuild = Read-RepoFile "wear\build.gradle.kts"
$appManifest = Read-RepoFile "app\src\main\AndroidManifest.xml"
$appSideBySideManifest = Read-RepoFile "app\src\sideBySide\AndroidManifest.xml"
$wearManifest = Read-RepoFile "wear\src\main\AndroidManifest.xml"
$wearSideBySideManifest = Read-RepoFile "wear\src\sideBySide\AndroidManifest.xml"
$wearCapabilities = Read-RepoFile "wear\src\main\res\values\wear.xml"
$wearStrings = Read-RepoFile "wear\src\main\res\values\strings.xml"
$phoneBridge = Read-RepoFile "app\src\main\java\com\example\shiftalarmmvp\watch\WatchAlarmBridge.kt"
$phoneControlListener = Read-RepoFile "app\src\main\java\com\example\shiftalarmmvp\watch\WearAlarmControlListenerService.kt"
$sideBySideTestReceiver = Read-RepoFile "app\src\sideBySide\java\com\example\shiftalarmmvp\watch\SideBySideWatchAlarmTestReceiver.kt"
$sideBySideWatchActionReceiver = Read-RepoFile "wear\src\sideBySide\java\com\example\shiftalarmmvp\wear\SideBySideWatchAlarmActionTestReceiver.kt"
$wearProtocol = Read-RepoFile "wear\src\main\java\com\example\shiftalarmmvp\wear\WatchAlarmProtocol.kt"
$ringingService = Read-RepoFile "app\src\main\java\com\example\shiftalarmmvp\service\AlarmRingingService.kt"
$watchService = Read-RepoFile "wear\src\main\java\com\example\shiftalarmmvp\wear\WatchAlarmRingingService.kt"
$watchListener = Read-RepoFile "wear\src\main\java\com\example\shiftalarmmvp\wear\WatchAlarmListenerService.kt"
$watchPhoneBridge = Read-RepoFile "wear\src\main\java\com\example\shiftalarmmvp\wear\PhoneMessageBridge.kt"
$watchActions = Read-RepoFile "wear\src\main\java\com\example\shiftalarmmvp\wear\WatchAlarmActions.kt"
$watchActionReceiver = Read-RepoFile "wear\src\main\java\com\example\shiftalarmmvp\wear\WatchAlarmActionReceiver.kt"
$watchAlarmActivity = Read-RepoFile "wear\src\main\java\com\example\shiftalarmmvp\wear\AlarmActivity.kt"
$watchNotifier = Read-RepoFile "wear\src\main\java\com\example\shiftalarmmvp\wear\WatchAlarmNotifier.kt"
$watchControlAckStore = Read-RepoFile "wear\src\main\java\com\example\shiftalarmmvp\wear\WatchAlarmControlAckStore.kt"
$watchActiveStore = Read-RepoFile "wear\src\main\java\com\example\shiftalarmmvp\wear\WatchAlarmActiveStore.kt"
$installScript = Read-RepoFile "scripts\install-watch-side-by-side.ps1"
$smokeScript = Read-RepoFile "scripts\run-watch-side-by-side-smoke.ps1"
$smokeAssertScript = Read-RepoFile "scripts\assert-watch-smoke-result.ps1"
$fullValidationScript = Read-RepoFile "scripts\run-watch-full-validation.ps1"

Assert-Contains "settings.gradle.kts" $settings 'include(":wear")'
Assert-Contains "app/build.gradle.kts" $appBuild 'implementation("com.google.android.gms:play-services-wearable:18.2.0")'
Assert-Contains "wear/build.gradle.kts" $wearBuild 'implementation("com.google.android.gms:play-services-wearable:18.2.0")'
Assert-Contains "wear/build.gradle.kts" $wearBuild 'applicationId = "com.example.shiftalarmmvp"'
Assert-Contains "app/build.gradle.kts" $appBuild 'applicationIdSuffix = ".next"'
Assert-Contains "wear/build.gradle.kts" $wearBuild 'applicationIdSuffix = ".next"'

Assert-Contains "app AndroidManifest" $appManifest '.watch.WearAlarmControlListenerService'
Assert-Contains "app AndroidManifest" $appManifest 'com.google.android.gms.wearable.MESSAGE_RECEIVED'
Assert-Contains "app AndroidManifest" $appManifest 'com.google.android.gms.wearable.DATA_CHANGED'
Assert-Contains "app AndroidManifest" $appManifest 'android:pathPrefix="/shift_alarm/alarm"'
Assert-Contains "app sideBySide AndroidManifest" $appSideBySideManifest '.watch.SideBySideWatchAlarmTestReceiver'
Assert-Contains "app sideBySide AndroidManifest" $appSideBySideManifest 'com.example.shiftalarmmvp.action.WATCH_PREVIEW_TEST'
Assert-Contains "app sideBySide AndroidManifest" $appSideBySideManifest 'com.example.shiftalarmmvp.action.WATCH_CONTROL_TEST'
Assert-Contains "SideBySideWatchAlarmTestReceiver.kt" $sideBySideTestReceiver 'sendPreviewAlarmWithResult'
Assert-Contains "SideBySideWatchAlarmTestReceiver.kt" $sideBySideTestReceiver 'AlarmRingingService.ACTION_START'
Assert-Contains "run-watch-side-by-side-smoke.ps1" $smokeScript 'assert-watch-smoke-result.ps1'
Assert-Contains "assert-watch-smoke-result.ps1" $smokeAssertScript 'watch preview result connected='
Assert-Contains "assert-watch-smoke-result.ps1" $smokeAssertScript 'phone accepted expected watch control'
Assert-Contains "assert-watch-smoke-result.ps1" $smokeAssertScript 'watch received phone control ack'

Assert-Contains "wear AndroidManifest" $wearManifest 'android.hardware.type.watch'
Assert-Contains "wear AndroidManifest" $wearManifest '.WatchAlarmListenerService'
Assert-Contains "wear AndroidManifest" $wearManifest '.WatchAlarmRingingService'
Assert-Contains "wear AndroidManifest" $wearManifest 'android.permission.FOREGROUND_SERVICE'
Assert-Contains "wear AndroidManifest" $wearManifest 'android.permission.FOREGROUND_SERVICE_SPECIAL_USE'
Assert-Contains "wear AndroidManifest" $wearManifest 'android:foregroundServiceType="specialUse"'
Assert-Contains "wear AndroidManifest" $wearManifest 'android:pathPrefix="/shift_alarm/alarm"'
Assert-Contains "wear sideBySide AndroidManifest" $wearSideBySideManifest '.SideBySideWatchAlarmActionTestReceiver'
Assert-Contains "wear sideBySide AndroidManifest" $wearSideBySideManifest 'com.example.shiftalarmmvp.action.WATCH_TEST_STOP'
Assert-Contains "wear sideBySide AndroidManifest" $wearSideBySideManifest 'com.example.shiftalarmmvp.action.WATCH_TEST_SNOOZE'
Assert-Contains "wear capabilities" $wearCapabilities 'android_wear_capabilities'
Assert-Contains "wear capabilities" $wearCapabilities 'shift_alarm_watch_control'
Assert-Contains "WatchAlarmBridge.kt" $phoneBridge 'CAPABILITY_WATCH_ALARM_CONTROL = "shift_alarm_watch_control"'
Assert-Contains "WatchAlarmBridge.kt" $phoneBridge 'getCapability(CAPABILITY_WATCH_ALARM_CONTROL, CapabilityClient.FILTER_REACHABLE)'

$requiredProtocolConstants = @(
    'PATH_ALARM_START = "/shift_alarm/alarm/start"',
    'PATH_ALARM_CANCEL = "/shift_alarm/alarm/cancel"',
    'PATH_ALARM_STOP = "/shift_alarm/alarm/stop"',
    'PATH_ALARM_SNOOZE = "/shift_alarm/alarm/snooze"',
    'PATH_ALARM_ACK = "/shift_alarm/alarm/ack"',
    'PATH_ALARM_ACTIVE = "/shift_alarm/alarm/active"',
    'PATH_ALARM_CANCELLED = "/shift_alarm/alarm/cancelled"',
    'PATH_ALARM_CONTROL = "/shift_alarm/alarm/control"',
    'PATH_ALARM_CONTROL_ACK = "/shift_alarm/alarm/control_ack"',
    'ACK_DISPLAY_MODE_FOREGROUND_SERVICE = "foreground_service"',
    'ACK_DISPLAY_MODE_FALLBACK = "fallback"'
)

foreach ($constant in $requiredProtocolConstants) {
    Assert-Contains "WatchAlarmBridge.kt" $phoneBridge $constant
    Assert-Contains "WatchAlarmProtocol.kt" $wearProtocol $constant
}

Assert-Contains "AlarmRingingService.kt" $ringingService 'WatchAlarmBridge(this).sendAlarmStarted'
Assert-Contains "AlarmRingingService.kt" $ringingService 'WatchAlarmBridge(this).sendAlarmCancelled'
Assert-Contains "AlarmRingingService.kt" $ringingService 'triggeredAtMillis = activeRingingTriggeredAtMillis'
Assert-Contains "AlarmRingingService.kt" $ringingService 'fun isRinging(alarmId: Long, triggeredAtMillis: Long)'
Assert-Contains "WatchAlarmRingingService.kt" $watchService 'startForegroundSafely'
Assert-Contains "WatchAlarmRingingService.kt" $watchService 'acquireWakeLock(payload)'
Assert-Contains "WatchAlarmRingingService.kt" $watchService 'releaseWakeLock()'
Assert-Contains "WatchAlarmRingingService.kt" $watchService 'PowerManager.PARTIAL_WAKE_LOCK'
Assert-Contains "WatchAlarmRingingService.kt" $watchService 'ACK_DISPLAY_MODE_FOREGROUND_SERVICE'
Assert-Contains "WatchAlarmRingingService.kt" $watchService 'ACK_DISPLAY_MODE_FALLBACK'
Assert-Contains "WatchAlarmListenerService.kt" $watchListener 'matchesAcceptedCancel'
Assert-Contains "WatchAlarmListenerService.kt" $watchListener 'matchesAcceptedStart'
Assert-Contains "WatchAlarmListenerService.kt" $watchListener 'parseControlAcknowledgement'
Assert-Contains "WatchAlarmListenerService.kt" $watchListener 'dismissIfControlAcknowledged'
Assert-Contains "WatchAlarmListenerService.kt" $watchListener 'WatchAlarmControlAckStore.record'
Assert-Contains "WatchAlarmListenerService.kt" $watchListener 'WatchAlarmActiveStore.record'
Assert-Contains "WatchAlarmListenerService.kt" $watchListener 'WatchAlarmActiveStore.clearIfMatching'
Assert-Contains "AlarmActivity.kt" $watchAlarmActivity 'sendActionAndAwaitAck'
Assert-Contains "AlarmActivity.kt" $watchAlarmActivity 'awaitControlAcknowledgement'
Assert-Contains "AlarmActivity.kt" $watchAlarmActivity 'restoreAfterMissingControlAck'
Assert-Contains "AlarmActivity.kt" $watchAlarmActivity 'CONTROL_ACK_TIMEOUT_MILLIS'
Assert-Contains "AlarmActivity.kt" $watchAlarmActivity 'alarm_waiting_stop_ack'
Assert-Contains "AlarmActivity.kt" $watchAlarmActivity 'alarm_missing_phone_ack'
Assert-Contains "WearAlarmControlListenerService.kt" $phoneControlListener 'sendControlAcknowledged'
Assert-Contains "PhoneMessageBridge.kt" $watchPhoneBridge 'CONTROL_SEND_ATTEMPTS = 3'
Assert-Contains "PhoneMessageBridge.kt" $watchPhoneBridge 'sendControlOnce'
Assert-Contains "WatchAlarmActions.kt" $watchActions 'fun controlPathFor'
Assert-Contains "WatchAlarmActions.kt" $watchActions 'ACTION_STOP -> WatchAlarmProtocol.PATH_ALARM_STOP'
Assert-Contains "WatchAlarmActions.kt" $watchActions 'ACTION_SNOOZE -> WatchAlarmProtocol.PATH_ALARM_SNOOZE'
Assert-Contains "WatchAlarmActionReceiver.kt" $watchActionReceiver 'goAsync()'
Assert-Contains "WatchAlarmActionReceiver.kt" $watchActionReceiver 'awaitPhoneAck'
Assert-Contains "WatchAlarmActionReceiver.kt" $watchActionReceiver 'restoreIfPhoneAckMissing'
Assert-Contains "WatchAlarmActionReceiver.kt" $watchActionReceiver 'WatchAlarmActions.controlPathFor'
Assert-Contains "WatchAlarmActionReceiver.kt" $watchActionReceiver 'controlAction = requestedAction'
Assert-Contains "WatchAlarmActionReceiver.kt" $watchActionReceiver 'restoreIfPhoneAckMissing(context, requestedControlAction'
Assert-Contains "WatchAlarmControlAckStore.kt" $watchControlAckStore 'hasAcknowledgementSince'
Assert-Contains "WatchAlarmActiveStore.kt" $watchActiveStore 'fun record'
Assert-Contains "WatchAlarmActiveStore.kt" $watchActiveStore 'fun read'
Assert-Contains "WatchAlarmNotifier.kt" $watchNotifier 'showControlPending'
Assert-Contains "WatchAlarmNotifier.kt" $watchNotifier 'alarm_waiting_phone_confirmation'
Assert-Contains "wear strings" $wearStrings 'alarm_snooze_after_minutes'
Assert-Contains "wear strings" $wearStrings 'alarm_waiting_phone_confirmation'
Assert-Contains "SideBySideWatchAlarmActionTestReceiver.kt" $sideBySideWatchActionReceiver 'WatchAlarmActiveStore.read'
Assert-Contains "SideBySideWatchAlarmActionTestReceiver.kt" $sideBySideWatchActionReceiver 'WatchAlarmActionReceiver::class.java'
Assert-Contains "install-watch-side-by-side.ps1" $installScript 'JavaHome'
Assert-Contains "install-watch-side-by-side.ps1" $installScript 'Invoke-Gradle'
Assert-Contains "run-watch-side-by-side-smoke.ps1" $smokeScript 'AutoWatchAction'
Assert-Contains "run-watch-side-by-side-smoke.ps1" $smokeScript 'AutoWatchActionAttempts'
Assert-Contains "run-watch-side-by-side-smoke.ps1" $smokeScript 'WATCH_TEST_STOP'
Assert-Contains "assert-watch-smoke-result.ps1" $smokeAssertScript 'watch sent expected control'
Assert-Contains "run-watch-full-validation.ps1" $fullValidationScript 'install-watch-side-by-side.ps1'
Assert-Contains "run-watch-full-validation.ps1" $fullValidationScript 'validate-watch-integration-source.ps1'
Assert-Contains "run-watch-full-validation.ps1" $fullValidationScript 'Resolve-DeviceSerials'
Assert-Contains "run-watch-full-validation.ps1" $fullValidationScript 'Auto-selected watch serial'
Assert-Contains "run-watch-full-validation.ps1" $fullValidationScript 'android.hardware.type.watch'
Assert-Contains "run-watch-full-validation.ps1" $fullValidationScript 'Invoke-Smoke -Scenario preview'
Assert-Contains "run-watch-full-validation.ps1" $fullValidationScript 'Invoke-Smoke -Scenario stop'
Assert-Contains "run-watch-full-validation.ps1" $fullValidationScript 'Invoke-Smoke -Scenario snooze'

$phoneApk = Join-Path $RootDir "app\build\outputs\apk\sideBySide\app-sideBySide.apk"
$watchApk = Join-Path $RootDir "wear\build\outputs\apk\sideBySide\wear-sideBySide.apk"

Write-Host "Watch integration source preflight: OK"
if ((Test-Path -LiteralPath $phoneApk) -and (Test-Path -LiteralPath $watchApk)) {
    Write-Host "Side-by-side APKs are present:"
    Get-Item -LiteralPath $phoneApk, $watchApk | Select-Object FullName, Length, LastWriteTime | Format-Table -AutoSize
} else {
    Write-Host "Side-by-side APKs are not both present. Run:"
    Write-Host "  .\gradlew.bat :app:assembleSideBySide :wear:assembleSideBySide"
}
