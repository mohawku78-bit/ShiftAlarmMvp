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

function Assert-NotContains {
    param(
        [string]$Name,
        [string]$Text,
        [string]$Pattern
    )
    if ($Text -match [regex]::Escape($Pattern)) {
        throw "$Name contains forbidden text: $Pattern"
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
$appStrings = Read-RepoFile "app\src\main\res\values\strings.xml"
$wearStrings = Read-RepoFile "wear\src\main\res\values\strings.xml"
$phoneBridge = Read-RepoFile "app\src\main\java\com\example\shiftalarmmvp\watch\WatchAlarmBridge.kt"
$phoneControlListener = Read-RepoFile "app\src\main\java\com\example\shiftalarmmvp\watch\WearAlarmControlListenerService.kt"
$phoneAcceptedControlStore = Read-RepoFile "app\src\main\java\com\example\shiftalarmmvp\watch\WatchAlarmAcceptedControlStore.kt"
$phoneDiagnosticsStore = Read-RepoFile "app\src\main\java\com\example\shiftalarmmvp\watch\WatchAlarmDiagnosticsStore.kt"
$mainActivity = Read-RepoFile "app\src\main\java\com\example\shiftalarmmvp\ui\MainActivity.kt"
$sideBySideTestReceiver = Read-RepoFile "app\src\sideBySide\java\com\example\shiftalarmmvp\watch\SideBySideWatchAlarmTestReceiver.kt"
$sideBySideWatchActionReceiver = Read-RepoFile "wear\src\sideBySide\java\com\example\shiftalarmmvp\wear\SideBySideWatchAlarmActionTestReceiver.kt"
$wearProtocol = Read-RepoFile "wear\src\main\java\com\example\shiftalarmmvp\wear\WatchAlarmProtocol.kt"
$ringingService = Read-RepoFile "app\src\main\java\com\example\shiftalarmmvp\service\AlarmRingingService.kt"
$watchListener = Read-RepoFile "wear\src\main\java\com\example\shiftalarmmvp\wear\WatchAlarmListenerService.kt"
$watchPhoneBridge = Read-RepoFile "wear\src\main\java\com\example\shiftalarmmvp\wear\PhoneMessageBridge.kt"
$watchActions = Read-RepoFile "wear\src\main\java\com\example\shiftalarmmvp\wear\WatchAlarmActions.kt"
$watchActionReceiver = Read-RepoFile "wear\src\main\java\com\example\shiftalarmmvp\wear\WatchAlarmActionReceiver.kt"
$watchAlarmActivity = Read-RepoFile "wear\src\main\java\com\example\shiftalarmmvp\wear\AlarmActivity.kt"
$watchMainActivity = Read-RepoFile "wear\src\main\java\com\example\shiftalarmmvp\wear\MainActivity.kt"
$watchNotifier = Read-RepoFile "wear\src\main\java\com\example\shiftalarmmvp\wear\WatchAlarmNotifier.kt"
$watchControlAckStore = Read-RepoFile "wear\src\main\java\com\example\shiftalarmmvp\wear\WatchAlarmControlAckStore.kt"
$watchEventGate = Read-RepoFile "wear\src\main\java\com\example\shiftalarmmvp\wear\WatchAlarmEventGate.kt"
$watchHardwareKeys = Read-RepoFile "wear\src\main\java\com\example\shiftalarmmvp\wear\WatchAlarmHardwareKeys.kt"
$watchActiveStore = Read-RepoFile "wear\src\main\java\com\example\shiftalarmmvp\wear\WatchAlarmActiveStore.kt"
$watchProtocolTest = Read-RepoFile "wear\src\test\java\com\example\shiftalarmmvp\wear\WatchAlarmProtocolTest.kt"
$installScript = Read-RepoFile "scripts\install-watch-side-by-side.ps1"
$connectWatchScript = Read-RepoFile "scripts\connect-watch-wireless.ps1"
$verifyScript = Read-RepoFile "scripts\verify-watch-side-by-side.ps1"
$smokeScript = Read-RepoFile "scripts\run-watch-side-by-side-smoke.ps1"
$smokeAssertScript = Read-RepoFile "scripts\assert-watch-smoke-result.ps1"
$fullValidationScript = Read-RepoFile "scripts\run-watch-full-validation.ps1"
$watchRingingServicePath = Join-Path $RootDir "wear\src\main\java\com\example\shiftalarmmvp\wear\WatchAlarmRingingService.kt"
if (Test-Path -LiteralPath $watchRingingServicePath) {
    throw "Watch foreground ringing service should not exist in notification-control mode: $watchRingingServicePath"
}

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
Assert-Contains "run-watch-side-by-side-smoke.ps1" $smokeScript 'Assert-NotificationPermission'
Assert-Contains "run-watch-side-by-side-smoke.ps1" $smokeScript 'pm", "grant", $PackageName, "android.permission.POST_NOTIFICATIONS'
Assert-Contains "run-watch-side-by-side-smoke.ps1" $smokeScript 'POST_NOTIFICATIONS permission is not granted'
Assert-Contains "run-watch-side-by-side-smoke.ps1" $smokeScript 'BlockWatchNotifications'
Assert-Contains "run-watch-side-by-side-smoke.ps1" $smokeScript 'Set-NotificationPermission -Serial $WatchSerial -Label "Watch" -Granted $false'
Assert-Contains "run-watch-side-by-side-smoke.ps1" $smokeScript 'Set-NotificationPermission -Serial $WatchSerial -Label "Watch" -Granted $true'
Assert-Contains "run-watch-side-by-side-smoke.ps1" $smokeScript 'ExpectedDisplayMode'
Assert-Contains "run-watch-side-by-side-smoke.ps1" $smokeScript 'Save-DeviceDiagnostics'
Assert-Contains "run-watch-side-by-side-smoke.ps1" $smokeScript 'cmd", "notification", "channels"'
Assert-Contains "run-watch-side-by-side-smoke.ps1" $smokeScript 'appops", "get", $PackageName, "POST_NOTIFICATION"'
Assert-Contains "assert-watch-smoke-result.ps1" $smokeAssertScript 'watch preview result connected='
Assert-Contains "assert-watch-smoke-result.ps1" $smokeAssertScript 'phone accepted expected watch control'
Assert-Contains "assert-watch-smoke-result.ps1" $smokeAssertScript 'watch received phone control ack'
Assert-Contains "assert-watch-smoke-result.ps1" $smokeAssertScript 'Resolve-DiagnosticPath'
Assert-Contains "assert-watch-smoke-result.ps1" $smokeAssertScript 'Read-Diagnostics'
Assert-Contains "assert-watch-smoke-result.ps1" $smokeAssertScript 'permission/channel diagnostics'
Assert-Contains "assert-watch-smoke-result.ps1" $smokeAssertScript 'POST_NOTIFICATION'
Assert-Contains "assert-watch-smoke-result.ps1" $smokeAssertScript 'ExpectedDisplayMode'
Assert-Contains "assert-watch-smoke-result.ps1" $smokeAssertScript 'show alarm notification skipped permission'
Assert-Contains "assert-watch-smoke-result.ps1" $smokeAssertScript 'show alarm activity alarmId='
Assert-Contains "assert-watch-smoke-result.ps1" $smokeAssertScript 'did not report unavailable display'
Assert-Contains "assert-watch-smoke-result.ps1" $smokeAssertScript 'displayMode=fallback'
Assert-Contains "assert-watch-smoke-result.ps1" $smokeAssertScript '$expectedAlarmId'
Assert-Contains "assert-watch-smoke-result.ps1" $smokeAssertScript 'alarmId=$expectedAlarmId'
Assert-Contains "assert-watch-smoke-result.ps1" $smokeAssertScript 'watch ack (message|data) alarmId=$expectedAlarmId'

Assert-Contains "wear AndroidManifest" $wearManifest 'android.hardware.type.watch'
Assert-Contains "wear AndroidManifest" $wearManifest 'android.permission.POST_NOTIFICATIONS'
Assert-Contains "wear AndroidManifest" $wearManifest 'android.permission.VIBRATE'
Assert-Contains "wear AndroidManifest" $wearManifest '.WatchAlarmListenerService'
Assert-Contains "wear AndroidManifest" $wearManifest 'android:pathPrefix="/shift_alarm/alarm"'
Assert-NotContains "wear AndroidManifest" $wearManifest '.WatchAlarmRingingService'
Assert-NotContains "wear AndroidManifest" $wearManifest 'android.permission.FOREGROUND_SERVICE'
Assert-NotContains "wear AndroidManifest" $wearManifest 'android.permission.FOREGROUND_SERVICE_SPECIAL_USE'
Assert-NotContains "wear AndroidManifest" $wearManifest 'android.permission.WAKE_LOCK'
Assert-NotContains "wear AndroidManifest" $wearManifest 'android.permission.USE_FULL_SCREEN_INTENT'
Assert-NotContains "wear AndroidManifest" $wearManifest 'android:foregroundServiceType="specialUse"'
Assert-Contains "wear sideBySide AndroidManifest" $wearSideBySideManifest '.SideBySideWatchAlarmActionTestReceiver'
Assert-Contains "wear sideBySide AndroidManifest" $wearSideBySideManifest 'com.example.shiftalarmmvp.action.WATCH_TEST_STOP'
Assert-Contains "wear sideBySide AndroidManifest" $wearSideBySideManifest 'com.example.shiftalarmmvp.action.WATCH_TEST_SNOOZE'
Assert-Contains "wear sideBySide AndroidManifest" $wearSideBySideManifest 'com.example.shiftalarmmvp.action.WATCH_TEST_OPEN_ALARM'
Assert-Contains "wear capabilities" $wearCapabilities 'android_wear_capabilities'
Assert-Contains "wear capabilities" $wearCapabilities 'shift_alarm_watch_control'
Assert-Contains "WatchAlarmBridge.kt" $phoneBridge 'CAPABILITY_WATCH_ALARM_CONTROL = "shift_alarm_watch_control"'
Assert-Contains "WatchAlarmBridge.kt" $phoneBridge 'getCapability(CAPABILITY_WATCH_ALARM_CONTROL, CapabilityClient.FILTER_REACHABLE)'
Assert-Contains "WatchAlarmBridge.kt" $phoneBridge 'recordSendAttempt'
Assert-Contains "WatchAlarmBridge.kt" $phoneBridge 'MESSAGE_SEND_ATTEMPTS = 3'
Assert-Contains "WatchAlarmBridge.kt" $phoneBridge 'sendToConnectedNodesOnce'
Assert-Contains "WatchAlarmBridge.kt" $phoneBridge 'attempt=$attempt'
Assert-Contains "WatchAlarmBridge.kt" $phoneBridge 'clearActive: Boolean = true'
Assert-Contains "WatchAlarmBridge.kt" $phoneBridge 'if (clearActive)'
Assert-Contains "WatchAlarmBridge.kt" $phoneBridge 'it.triggeredAtMillis > 0L'

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
    'ACK_DISPLAY_MODE_NOTIFICATION = "notification"',
    'ACK_DISPLAY_MODE_FALLBACK = "fallback"'
)

foreach ($constant in $requiredProtocolConstants) {
    Assert-Contains "WatchAlarmBridge.kt" $phoneBridge $constant
    Assert-Contains "WatchAlarmProtocol.kt" $wearProtocol $constant
}
Assert-Contains "WatchAlarmProtocol.kt" $wearProtocol 'it.triggeredAtMillis > 0L'

Assert-Contains "AlarmRingingService.kt" $ringingService 'WatchAlarmBridge(this).sendAlarmStarted'
Assert-Contains "AlarmRingingService.kt" $ringingService 'WatchAlarmBridge(this).sendAlarmCancelled'
Assert-Contains "AlarmRingingService.kt" $ringingService 'triggeredAtMillis = activeRingingTriggeredAtMillis'
Assert-Contains "AlarmRingingService.kt" $ringingService 'fun isRinging(alarmId: Long, triggeredAtMillis: Long)'
Assert-Contains "AlarmRingingService.kt" $ringingService 'scheduleWatchBridgeFallbackNotification'
Assert-Contains "AlarmRingingService.kt" $ringingService 'WATCH_BRIDGE_FALLBACK_DELAY_MILLIS = 6_000L'
Assert-Contains "AlarmRingingService.kt" $ringingService 'WatchAlarmDiagnosticsStore(this).hasNotificationAckFor'
Assert-Contains "AlarmRingingService.kt" $ringingService 'cancelWatchBridgeFallback'
Assert-Contains "AlarmRingingService.kt" $ringingService 'ALARM_RAMP_VIBRATION_AMPLITUDES'
Assert-Contains "AlarmRingingService.kt" $ringingService 'ALARM_RAMP_VIBRATION_REPEAT_INDEX'
Assert-Contains "AlarmRingingService.kt" $ringingService 'WATCH_BRIDGE_VIBRATION_PATTERN'
Assert-Contains "AlarmRingingService.kt" $ringingService 'watch_alarm_bridge_channel_v4'
Assert-Contains "AlarmRingingService.kt" $ringingService 'watch_alarm_bridge_channel_v3'
Assert-Contains "AlarmRingingService.kt" $ringingService 'setDefaults(0)'
Assert-Contains "AlarmRingingService.kt" $ringingService 'WATCH_BRIDGE_LEGACY_CHANNEL_IDS'
Assert-Contains "AlarmRingingService.kt" $ringingService 'deleteNotificationChannel'
Assert-Contains "WatchAlarmListenerService.kt" $watchListener 'matchesAcceptedCancel'
Assert-Contains "WatchAlarmListenerService.kt" $watchListener 'matchesAcceptedStart'
Assert-Contains "WatchAlarmListenerService.kt" $watchListener 'parseControlAcknowledgement'
Assert-Contains "WatchAlarmListenerService.kt" $watchListener 'handleControlAcknowledgement'
Assert-Contains "WatchAlarmListenerService.kt" $watchListener 'WatchAlarmActiveStore.isMatching'
Assert-Contains "WatchAlarmListenerService.kt" $watchListener 'clearedActiveAlarm'
Assert-Contains "WatchAlarmListenerService.kt" $watchListener 'ignore stale cancel for inactive alarmId'
Assert-Contains "WatchAlarmListenerService.kt" $watchListener 'ignore stale control ack'
Assert-Contains "WatchAlarmListenerService.kt" $watchListener 'ignore unsupported control ack'
Assert-Contains "WatchAlarmListenerService.kt" $watchListener 'dismissIfControlAcknowledged'
Assert-Contains "WatchAlarmListenerService.kt" $watchListener 'WatchAlarmControlAckStore.record'
Assert-Contains "WatchAlarmListenerService.kt" $watchListener 'WatchAlarmActiveStore.record'
Assert-Contains "WatchAlarmListenerService.kt" $watchListener 'WatchAlarmActiveStore.clearIfMatching'
Assert-Contains "WatchAlarmListenerService.kt" $watchListener 'ACK_DISPLAY_MODE_NOTIFICATION'
Assert-Contains "WatchAlarmListenerService.kt" $watchListener 'val notificationShown = WatchAlarmNotifier.show(this, payload)'
Assert-Contains "WatchAlarmListenerService.kt" $watchListener 'val fallbackShown = if (!notificationShown)'
Assert-Contains "WatchAlarmListenerService.kt" $watchListener 'AlarmActivity.show(this, payload, useLocalVibration = payload.vibrationEnabled)'
Assert-Contains "WatchAlarmListenerService.kt" $watchListener 'notificationShown -> WatchAlarmProtocol.ACK_DISPLAY_MODE_NOTIFICATION'
Assert-Contains "WatchAlarmListenerService.kt" $watchListener 'fallbackShown -> WatchAlarmProtocol.ACK_DISPLAY_MODE_FALLBACK'
Assert-Contains "WatchAlarmListenerService.kt" $watchListener 'WatchAlarmActiveStore.clearIfMatching(this, payload.alarmId, payload.triggeredAtMillis)'
Assert-Contains "WatchAlarmListenerService.kt" $watchListener 'alarm display unavailable alarmId='
Assert-Contains "WatchAlarmListenerService.kt" $watchListener 'PhoneMessageBridge.sendAck(this, payload, displayMode)'
Assert-NotContains "WatchAlarmListenerService.kt" $watchListener 'WatchAlarmRingingService.start(this, payload)'
Assert-NotContains "WatchAlarmListenerService.kt" $watchListener 'WatchAlarmRingingService.stop(this)'
Assert-Contains "AlarmActivity.kt" $watchAlarmActivity 'sendActionAndAwaitAck'
Assert-Contains "AlarmActivity.kt" $watchAlarmActivity 'awaitControlAcknowledgement'
Assert-Contains "AlarmActivity.kt" $watchAlarmActivity 'restoreAfterMissingControlAck'
Assert-Contains "AlarmActivity.kt" $watchAlarmActivity 'restoreControlRetryAfterMissingAck'
Assert-Contains "AlarmActivity.kt" $watchAlarmActivity 'WatchAlarmNotifier.showControlPending'
Assert-Contains "AlarmActivity.kt" $watchAlarmActivity 'WatchAlarmNotifier.show(this, payload)'
Assert-Contains "AlarmActivity.kt" $watchAlarmActivity 'WatchAlarmHardwareKeys.controlPathFor'
Assert-Contains "AlarmActivity.kt" $watchAlarmActivity 'pendingControlAction != null'
Assert-Contains "AlarmActivity.kt" $watchAlarmActivity 'EXTRA_PENDING_CONTROL_ACTION'
Assert-Contains "AlarmActivity.kt" $watchAlarmActivity 'EXTRA_PENDING_CONTROL_STARTED_AT_MILLIS'
Assert-Contains "AlarmActivity.kt" $watchAlarmActivity 'createPendingControlIntent'
Assert-Contains "AlarmActivity.kt" $watchAlarmActivity 'restorePendingControlState'
Assert-Contains "AlarmActivity.kt" $watchAlarmActivity 'dismissIfStoredControlAck'
Assert-Contains "AlarmActivity.kt" $watchAlarmActivity 'WatchAlarmControlAckStore.hasAcknowledgementSince'
Assert-Contains "AlarmActivity.kt" $watchAlarmActivity 'pendingControlPayload?.triggeredAtMillis == payload.triggeredAtMillis'
Assert-Contains "AlarmActivity.kt" $watchAlarmActivity 'hardware key control'
Assert-Contains "AlarmActivity.kt" $watchAlarmActivity 'hardware key ignored'
Assert-Contains "AlarmActivity.kt" $watchAlarmActivity 'onKeyUp'
Assert-Contains "AlarmActivity.kt" $watchAlarmActivity 'powerSaverRetry'
Assert-NotContains "AlarmActivity.kt" $watchAlarmActivity 'FLAG_KEEP_SCREEN_ON'
Assert-NotContains "AlarmActivity.kt" $watchAlarmActivity 'WatchAlarmRingingService.stopKeepingNotification'
Assert-NotContains "AlarmActivity.kt" $watchAlarmActivity 'WatchAlarmRingingService.stop(this)'
Assert-Contains "AlarmActivity.kt" $watchAlarmActivity 'dismissIfMatching(alarmId: Long, triggeredAtMillis: Long)'
Assert-Contains "AlarmActivity.kt" $watchAlarmActivity 'show alarm activity alarmId='
Assert-Contains "AlarmActivity.kt" $watchAlarmActivity 'show alarm activity failed alarmId='
Assert-Contains "AlarmActivity.kt" $watchAlarmActivity 'control ack timeout in activity'
Assert-Contains "AlarmActivity.kt" $watchAlarmActivity 'CONTROL_ACK_TIMEOUT_MILLIS'
Assert-Contains "AlarmActivity.kt" $watchAlarmActivity 'alarm_waiting_stop_ack'
Assert-Contains "AlarmActivity.kt" $watchAlarmActivity 'alarm_missing_phone_ack'
Assert-Contains "AlarmActivity.kt" $watchAlarmActivity 'RAMP_VIBRATION_AMPLITUDES'
Assert-Contains "AlarmActivity.kt" $watchAlarmActivity 'RAMP_VIBRATION_REPEAT_INDEX'
Assert-Contains "WearAlarmControlListenerService.kt" $phoneControlListener 'sendControlAcknowledged'
Assert-Contains "WearAlarmControlListenerService.kt" $phoneControlListener 'WatchAlarmAcceptedControlStore.matchesRecent'
Assert-Contains "WearAlarmControlListenerService.kt" $phoneControlListener 'resend accepted watch control ack'
Assert-Contains "WearAlarmControlListenerService.kt" $phoneControlListener 'acknowledgeAcceptedControl'
Assert-Contains "WearAlarmControlListenerService.kt" $phoneControlListener 'recordControlRejected'
Assert-Contains "WearAlarmControlListenerService.kt" $phoneControlListener 'REJECTION_STALE_ALARM'
Assert-Contains "WearAlarmControlListenerService.kt" $phoneControlListener 'clearActive = false'
Assert-Contains "WearAlarmControlListenerService.kt" $phoneControlListener 'cancelBridgeFallbackIfWatchDisplayHandled'
Assert-Contains "WearAlarmControlListenerService.kt" $phoneControlListener 'ACK_DISPLAY_MODE_NOTIFICATION'
Assert-Contains "WearAlarmControlListenerService.kt" $phoneControlListener 'ACK_DISPLAY_MODE_FALLBACK'
Assert-Contains "WearAlarmControlListenerService.kt" $phoneControlListener 'AlarmRingingService.cancelWatchBridgeFallback'
Assert-Contains "WatchAlarmAcceptedControlStore.kt" $phoneAcceptedControlStore 'fun record'
Assert-Contains "WatchAlarmAcceptedControlStore.kt" $phoneAcceptedControlStore 'fun matchesRecent'
Assert-Contains "WatchAlarmDiagnosticsStore.kt" $phoneDiagnosticsStore 'data class WatchAlarmSendAttempt'
Assert-Contains "WatchAlarmDiagnosticsStore.kt" $phoneDiagnosticsStore 'data class WatchAlarmControlRejection'
Assert-Contains "WatchAlarmDiagnosticsStore.kt" $phoneDiagnosticsStore 'triggeredAtMillis: Long'
Assert-Contains "WatchAlarmDiagnosticsStore.kt" $phoneDiagnosticsStore 'KEY_ACK_TRIGGERED_AT_MILLIS'
Assert-Contains "WatchAlarmDiagnosticsStore.kt" $phoneDiagnosticsStore 'fun hasNotificationAckFor'
Assert-Contains "WatchAlarmDiagnosticsStore.kt" $phoneDiagnosticsStore 'matchesNotificationAck'
Assert-Contains "WatchAlarmDiagnosticsStore.kt" $phoneDiagnosticsStore 'fun recordSendAttempt'
Assert-Contains "WatchAlarmDiagnosticsStore.kt" $phoneDiagnosticsStore 'fun latestSendAttempt'
Assert-Contains "WatchAlarmDiagnosticsStore.kt" $phoneDiagnosticsStore 'fun recordControlRejected'
Assert-Contains "WatchAlarmDiagnosticsStore.kt" $phoneDiagnosticsStore 'fun latestRejectedControl'
Assert-Contains "MainActivity.kt" $mainActivity 'watchSendAttemptStatusLine'
Assert-Contains "MainActivity.kt" $mainActivity 'watchControlRejectionReasonLabel'
Assert-Contains "MainActivity.kt" $mainActivity 'ACK_DISPLAY_MODE_NOTIFICATION'
Assert-Contains "app strings" $appStrings 'editor_watch_send_status_format'
Assert-Contains "app strings" $appStrings 'editor_watch_control_rejected_status_format'
Assert-Contains "app strings" $appStrings 'editor_watch_ack_mode_notification'
Assert-Contains "PhoneMessageBridge.kt" $watchPhoneBridge 'CONTROL_SEND_ATTEMPTS = 3'
Assert-Contains "PhoneMessageBridge.kt" $watchPhoneBridge 'sendControlOnce'
Assert-Contains "WatchAlarmActions.kt" $watchActions 'fun controlPathFor'
Assert-Contains "WatchAlarmActions.kt" $watchActions 'ACTION_STOP -> WatchAlarmProtocol.PATH_ALARM_STOP'
Assert-Contains "WatchAlarmActions.kt" $watchActions 'ACTION_SNOOZE -> WatchAlarmProtocol.PATH_ALARM_SNOOZE'
Assert-Contains "WatchAlarmActionReceiver.kt" $watchActionReceiver 'goAsync()'
Assert-Contains "WatchAlarmActionReceiver.kt" $watchActionReceiver 'awaitPhoneAck'
Assert-Contains "WatchAlarmActionReceiver.kt" $watchActionReceiver 'restoreIfPhoneAckMissing'
Assert-Contains "WatchAlarmActionReceiver.kt" $watchActionReceiver 'WatchAlarmActiveStore.isMatching'
Assert-Contains "WatchAlarmActionReceiver.kt" $watchActionReceiver 'skip control ack timeout restore inactive alarm'
Assert-Contains "WatchAlarmActionReceiver.kt" $watchActionReceiver 'control ack timeout powerSaverRetry'
Assert-Contains "WatchAlarmActionReceiver.kt" $watchActionReceiver 'WatchAlarmNotifier.show(context, payload)'
Assert-Contains "WatchAlarmActionReceiver.kt" $watchActionReceiver 'WatchAlarmActions.controlPathFor'
Assert-Contains "WatchAlarmActionReceiver.kt" $watchActionReceiver 'controlAction = requestedAction'
Assert-Contains "WatchAlarmActionReceiver.kt" $watchActionReceiver 'restoreIfPhoneAckMissing(context, requestedControlAction'
Assert-Contains "WatchAlarmActionReceiver.kt" $watchActionReceiver 'requestStartedAtMillis'
Assert-NotContains "WatchAlarmActionReceiver.kt" $watchActionReceiver 'WatchAlarmRingingService.stopKeepingNotification'
Assert-Contains "WatchAlarmControlAckStore.kt" $watchControlAckStore 'hasAcknowledgementSince'
Assert-Contains "WatchAlarmControlAckStore.kt" $watchControlAckStore 'matchesAcknowledgement'
Assert-Contains "WatchAlarmEventGate.kt" $watchEventGate 'eventKey(eventType: String, alarmId: Long, eventTimeMillis: Long)'
Assert-Contains "WatchAlarmEventGate.kt" $watchEventGate 'prefs.contains(key)'
Assert-Contains "WatchAlarmEventGate.kt" $watchEventGate 'pruneOldEvents'
Assert-Contains "WatchAlarmHardwareKeys.kt" $watchHardwareKeys 'KEYCODE_STEM_PRIMARY'
Assert-Contains "WatchAlarmHardwareKeys.kt" $watchHardwareKeys 'KEYCODE_HOME'
Assert-Contains "WatchAlarmHardwareKeys.kt" $watchHardwareKeys 'KEYCODE_ASSIST'
Assert-Contains "WatchAlarmHardwareKeys.kt" $watchHardwareKeys 'KEYCODE_STEM_1'
Assert-Contains "WatchAlarmHardwareKeys.kt" $watchHardwareKeys 'KEYCODE_STEM_2'
Assert-Contains "WatchAlarmHardwareKeys.kt" $watchHardwareKeys 'KEYCODE_STEM_3'
Assert-Contains "WatchAlarmHardwareKeys.kt" $watchHardwareKeys 'KEYCODE_BACK'
Assert-Contains "WatchAlarmHardwareKeys.kt" $watchHardwareKeys 'KEYCODE_VOLUME_UP'
Assert-Contains "WatchAlarmHardwareKeys.kt" $watchHardwareKeys 'KEYCODE_VOLUME_DOWN'
Assert-Contains "WatchAlarmActiveStore.kt" $watchActiveStore 'fun record'
Assert-Contains "WatchAlarmActiveStore.kt" $watchActiveStore 'fun read'
Assert-Contains "WatchAlarmActiveStore.kt" $watchActiveStore 'fun clearIfMatching(context: Context, alarmId: Long, triggeredAtMillis: Long): Boolean'
Assert-Contains "WatchAlarmActiveStore.kt" $watchActiveStore 'fun isMatching'
Assert-Contains "WatchAlarmActiveStore.kt" $watchActiveStore 'internal fun matches'
Assert-Contains "WatchAlarmNotifier.kt" $watchNotifier 'showControlPending'
Assert-Contains "WatchAlarmNotifier.kt" $watchNotifier 'canPostNotifications'
Assert-Contains "WatchAlarmNotifier.kt" $watchNotifier 'areNotificationsEnabled'
Assert-Contains "WatchAlarmNotifier.kt" $watchNotifier 'POST_NOTIFICATIONS'
Assert-Contains "WatchAlarmNotifier.kt" $watchNotifier 'IMPORTANCE_NONE'
Assert-Contains "WatchAlarmNotifier.kt" $watchNotifier 'createPendingControlIntent'
Assert-Contains "WatchAlarmNotifier.kt" $watchNotifier 'requestStartedAtMillis'
Assert-Contains "WatchAlarmNotifier.kt" $watchNotifier 'alarm_waiting_phone_confirmation'
Assert-Contains "WatchAlarmNotifier.kt" $watchNotifier 'shift_alarm_watch_alarm_v5'
Assert-Contains "WatchAlarmNotifier.kt" $watchNotifier 'deleteNotificationChannel'
Assert-Contains "WatchAlarmNotifier.kt" $watchNotifier 'shift_alarm_watch_alarm_v1'
Assert-Contains "WatchAlarmNotifier.kt" $watchNotifier 'shift_alarm_watch_alarm_v2'
Assert-Contains "WatchAlarmNotifier.kt" $watchNotifier 'shift_alarm_watch_alarm_v3'
Assert-Contains "WatchAlarmNotifier.kt" $watchNotifier 'shift_alarm_watch_alarm_v4'
Assert-Contains "WatchAlarmNotifier.kt" $watchNotifier 'GENTLE_RAMP_VIBRATION_PATTERN'
Assert-Contains "WatchAlarmNotifier.kt" $watchNotifier 'vibrationPattern = GENTLE_RAMP_VIBRATION_PATTERN'
Assert-Contains "WatchAlarmNotifier.kt" $watchNotifier 'useLocalVibration: Boolean = false'
Assert-Contains "WatchAlarmNotifier.kt" $watchNotifier 'show alarm notification alarmId='
Assert-Contains "WatchAlarmNotifier.kt" $watchNotifier 'NotificationManager.IMPORTANCE_HIGH'
Assert-Contains "WatchAlarmNotifier.kt" $watchNotifier 'Notification.CATEGORY_ALARM'
Assert-Contains "WatchAlarmNotifier.kt" $watchNotifier 'setOngoing(true)'
Assert-Contains "WatchAlarmNotifier.kt" $watchNotifier 'PendingIntent.getBroadcast'
Assert-Contains "WatchAlarmNotifier.kt" $watchNotifier 'WatchAlarmActionReceiver::class.java'
Assert-Contains "WatchAlarmNotifier.kt" $watchNotifier 'WatchAlarmActions.ACTION_STOP'
Assert-Contains "WatchAlarmNotifier.kt" $watchNotifier 'if (payload.canSnooze)'
Assert-Contains "WatchAlarmNotifier.kt" $watchNotifier 'WatchAlarmActions.ACTION_SNOOZE'
Assert-NotContains "WatchAlarmNotifier.kt" $watchNotifier 'setFullScreenIntent'
Assert-Contains "wear strings" $wearStrings 'alarm_snooze_after_minutes'
Assert-Contains "wear strings" $wearStrings 'alarm_waiting_phone_confirmation'
Assert-Contains "wear strings" $wearStrings 'setup_preview_button'
Assert-Contains "wear strings" $wearStrings 'setup_preview_label'
Assert-Contains "wear MainActivity.kt" $watchMainActivity 'requestNotificationPermissionIfNeeded'
Assert-Contains "wear MainActivity.kt" $watchMainActivity 'setup_preview_button'
Assert-Contains "SideBySideWatchAlarmActionTestReceiver.kt" $sideBySideWatchActionReceiver 'WatchAlarmActiveStore.read'
Assert-Contains "SideBySideWatchAlarmActionTestReceiver.kt" $sideBySideWatchActionReceiver 'WatchAlarmActionReceiver::class.java'
Assert-Contains "SideBySideWatchAlarmActionTestReceiver.kt" $sideBySideWatchActionReceiver 'WATCH_TEST_OPEN_ALARM'
Assert-Contains "SideBySideWatchAlarmActionTestReceiver.kt" $sideBySideWatchActionReceiver 'AlarmActivity.show(context, payload, useLocalVibration = false)'
Assert-Contains "install-watch-side-by-side.ps1" $installScript 'JavaHome'
Assert-Contains "install-watch-side-by-side.ps1" $installScript 'Invoke-Gradle'
Assert-Contains "install-watch-side-by-side.ps1" $installScript 'Get-ConnectedDevices'
Assert-Contains "install-watch-side-by-side.ps1" $installScript 'Resolve-DeviceSerials'
Assert-Contains "install-watch-side-by-side.ps1" $installScript 'android.hardware.type.watch'
Assert-Contains "install-watch-side-by-side.ps1" $installScript 'Auto-selected watch serial'
Assert-Contains "install-watch-side-by-side.ps1" $installScript 'connect-watch-wireless.ps1'
Assert-Contains "connect-watch-wireless.ps1" $connectWatchScript '_adb-tls-pairing._tcp'
Assert-Contains "connect-watch-wireless.ps1" $connectWatchScript '_adb-tls-connect._tcp'
Assert-Contains "connect-watch-wireless.ps1" $connectWatchScript 'PairCode'
Assert-Contains "connect-watch-wireless.ps1" $connectWatchScript 'android.hardware.type.watch'
Assert-Contains "connect-watch-wireless.ps1" $connectWatchScript 'Could not connect'
Assert-Contains "verify-watch-side-by-side.ps1" $verifyScript 'Assert-DeviceKind'
Assert-Contains "verify-watch-side-by-side.ps1" $verifyScript 'Assert-SideBySideVersion'
Assert-Contains "verify-watch-side-by-side.ps1" $verifyScript 'Assert-NotificationPermission'
Assert-Contains "verify-watch-side-by-side.ps1" $verifyScript 'android.hardware.type.watch'
Assert-Contains "verify-watch-side-by-side.ps1" $verifyScript 'android.permission.POST_NOTIFICATIONS'
Assert-Contains "run-watch-side-by-side-smoke.ps1" $smokeScript 'AutoWatchAction'
Assert-Contains "run-watch-side-by-side-smoke.ps1" $smokeScript 'AutoWatchActionSource'
Assert-Contains "run-watch-side-by-side-smoke.ps1" $smokeScript '"input", "keyevent"'
Assert-Contains "run-watch-side-by-side-smoke.ps1" $smokeScript 'WATCH_TEST_OPEN_ALARM'
Assert-Contains "run-watch-side-by-side-smoke.ps1" $smokeScript 'orphan'
Assert-Contains "run-watch-side-by-side-smoke.ps1" $smokeScript 'AutoWatchActionAttempts'
Assert-Contains "run-watch-side-by-side-smoke.ps1" $smokeScript 'WATCH_TEST_STOP'
Assert-Contains "assert-watch-smoke-result.ps1" $smokeAssertScript 'watch sent expected control'
Assert-Contains "assert-watch-smoke-result.ps1" $smokeAssertScript 'Write-DiagnosticHints'
Assert-Contains "assert-watch-smoke-result.ps1" $smokeAssertScript 'showed notification controls'
Assert-Contains "assert-watch-smoke-result.ps1" $smokeAssertScript 'displayMode=notification'
Assert-Contains "assert-watch-smoke-result.ps1" $smokeAssertScript 'watch received hardware key control'
Assert-Contains "assert-watch-smoke-result.ps1" $smokeAssertScript 'phone rejected orphan watch control as stale'
Assert-Contains "assert-watch-smoke-result.ps1" $smokeAssertScript 'watch skipped orphan restore after cancel'
Assert-Contains "assert-watch-smoke-result.ps1" $smokeAssertScript 'watch did not restore orphan alarm after cancel'
Assert-Contains "assert-watch-smoke-result.ps1" $smokeAssertScript 'Diagnostic log hints'
Assert-Contains "assert-watch-smoke-result.ps1" $smokeAssertScript 'cancel alarm notification'
Assert-Contains "assert-watch-smoke-result.ps1" $smokeAssertScript 'show control pending notification failed'
Assert-Contains "run-watch-full-validation.ps1" $fullValidationScript 'install-watch-side-by-side.ps1'
Assert-Contains "run-watch-full-validation.ps1" $fullValidationScript 'validate-watch-integration-source.ps1'
Assert-Contains "run-watch-full-validation.ps1" $fullValidationScript 'Resolve-DeviceSerials'
Assert-Contains "run-watch-full-validation.ps1" $fullValidationScript 'Auto-selected watch serial'
Assert-Contains "run-watch-full-validation.ps1" $fullValidationScript 'android.hardware.type.watch'
Assert-Contains "run-watch-full-validation.ps1" $fullValidationScript 'Invoke-Smoke -Scenario preview'
Assert-Contains "run-watch-full-validation.ps1" $fullValidationScript 'Notification-blocked fallback smoke'
Assert-Contains "run-watch-full-validation.ps1" $fullValidationScript 'SkipFallbackSmoke'
Assert-Contains "run-watch-full-validation.ps1" $fullValidationScript 'ExpectedDisplayMode fallback'
Assert-Contains "run-watch-full-validation.ps1" $fullValidationScript 'Orphaned watch alarm stale-control smoke'
Assert-Contains "run-watch-full-validation.ps1" $fullValidationScript 'Invoke-Smoke -Scenario stop'
Assert-Contains "run-watch-full-validation.ps1" $fullValidationScript 'Invoke-Smoke -Scenario snooze'
Assert-Contains "run-watch-full-validation.ps1" $fullValidationScript 'Watch hardware-key stop control smoke'
Assert-Contains "run-watch-full-validation.ps1" $fullValidationScript 'Watch hardware-key snooze control smoke'
Assert-Contains "WatchAlarmProtocolTest.kt" $watchProtocolTest 'hardwareKeysMapToStopAndSnoozeControls'
Assert-Contains "WatchAlarmProtocolTest.kt" $watchProtocolTest 'activeStoreMatchingRequiresSameAlarmOccurrence'
Assert-Contains "WatchAlarmProtocolTest.kt" $watchProtocolTest 'controlAckStoreMatchesOnlySameActionOccurrenceAndRequestWindow'
Assert-Contains "WatchAlarmProtocolTest.kt" $watchProtocolTest 'eventGateKey_separatesStartCancelAndAlarmOccurrences'

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
