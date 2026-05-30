package com.example.shiftalarmmvp.watch

import android.util.Log
import com.example.shiftalarmmvp.service.AlarmRingingService
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class WearAlarmControlListenerService : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == WatchAlarmBridge.PATH_ALARM_ACK) {
            val ack = WatchAlarmBridge.parseAck(messageEvent.data) ?: return
            Log.i(
                TAG,
                "watch ack message alarmId=${ack.payload.alarmId} label=${ack.payload.label} displayMode=${ack.displayMode}"
            )
            WatchAlarmDiagnosticsStore(applicationContext).recordAck(ack.payload, ack.displayMode)
            return
        }
        val payload = WatchAlarmBridge.parsePayload(messageEvent.data) ?: return
        Log.i(TAG, "watch control message path=${messageEvent.path} alarmId=${payload.alarmId}")
        handleControl(messageEvent.path, payload)
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents
            .filter { it.type == DataEvent.TYPE_CHANGED }
            .forEach { event ->
                when (event.dataItem.uri.path) {
                    WatchAlarmBridge.PATH_ALARM_ACK -> {
                        WatchAlarmBridge.parseAck(event.dataItem)?.let { ack ->
                            Log.i(
                                TAG,
                                "watch ack data alarmId=${ack.payload.alarmId} label=${ack.payload.label} displayMode=${ack.displayMode}"
                            )
                            WatchAlarmDiagnosticsStore(applicationContext).recordAck(ack.payload, ack.displayMode)
                        }
                    }

                    WatchAlarmBridge.PATH_ALARM_CONTROL -> {
                        WatchAlarmBridge.parseControl(event.dataItem)?.let { control ->
                            Log.i(TAG, "watch control data action=${control.action} alarmId=${control.payload.alarmId}")
                            handleControl(control.action, control.payload)
                        }
                    }
                }
            }
    }

    private fun handleControl(action: String, payload: WatchAlarmPayload) {
        val diagnostics = WatchAlarmDiagnosticsStore(applicationContext)
        if (WatchAlarmAcceptedControlStore.matchesRecent(applicationContext, action, payload)) {
            Log.i(TAG, "resend accepted watch control ack action=$action alarmId=${payload.alarmId}")
            WatchAlarmBridge(applicationContext).sendControlAcknowledged(action, payload)
            return
        }

        if (!AlarmRingingService.isRinging(payload.alarmId, payload.triggeredAtMillis)) {
            Log.i(
                TAG,
                "ignore stale control action=$action alarmId=${payload.alarmId} triggeredAt=${payload.triggeredAtMillis}"
            )
            diagnostics.recordControlRejected(
                action,
                payload,
                WatchAlarmDiagnosticsStore.REJECTION_STALE_ALARM
            )
            WatchAlarmBridge(applicationContext).sendAlarmCancelled(
                alarmId = payload.alarmId,
                triggeredAtMillis = payload.triggeredAtMillis,
                clearActive = false
            )
            return
        }

        when (action) {
            WatchAlarmBridge.PATH_ALARM_STOP -> {
                if (!WatchAlarmControlGate.accept(applicationContext, action, payload)) {
                    Log.i(TAG, "ignore duplicate stop control alarmId=${payload.alarmId}")
                    diagnostics.recordControlRejected(
                        action,
                        payload,
                        WatchAlarmDiagnosticsStore.REJECTION_DUPLICATE_CONTROL
                    )
                    return
                }
                Log.i(TAG, "stop from watch alarmId=${payload.alarmId}")
                acknowledgeAcceptedControl(action, payload)
                AlarmRingingService.stop(applicationContext, payload.alarmId)
            }

            WatchAlarmBridge.PATH_ALARM_SNOOZE -> {
                if (!payload.canSnooze) {
                    Log.i(TAG, "ignore snooze not allowed alarmId=${payload.alarmId}")
                    diagnostics.recordControlRejected(
                        action,
                        payload,
                        WatchAlarmDiagnosticsStore.REJECTION_SNOOZE_NOT_ALLOWED
                    )
                    return
                }
                if (!WatchAlarmControlGate.accept(applicationContext, action, payload)) {
                    Log.i(TAG, "ignore duplicate snooze control alarmId=${payload.alarmId}")
                    diagnostics.recordControlRejected(
                        action,
                        payload,
                        WatchAlarmDiagnosticsStore.REJECTION_DUPLICATE_CONTROL
                    )
                    return
                }
                Log.i(TAG, "snooze from watch alarmId=${payload.alarmId} minutes=${payload.snoozeMinutes}")
                acknowledgeAcceptedControl(action, payload)
                AlarmRingingService.snooze(
                    context = applicationContext,
                    alarmId = payload.alarmId,
                    label = payload.label,
                    snoozeMinutes = payload.snoozeMinutes,
                    snoozeMaxCount = payload.snoozeMaxCount,
                    currentSnoozeCount = payload.currentSnoozeCount,
                    soundType = payload.soundType,
                    customSoundUri = payload.customSoundUri,
                    volumePercent = payload.volumePercent,
                    vibrationEnabled = payload.vibrationEnabled
                )
            }

            else -> {
                Log.i(TAG, "ignore unsupported control action=$action alarmId=${payload.alarmId}")
                diagnostics.recordControlRejected(
                    action,
                    payload,
                    WatchAlarmDiagnosticsStore.REJECTION_UNSUPPORTED_ACTION
                )
            }
        }
    }

    private fun acknowledgeAcceptedControl(action: String, payload: WatchAlarmPayload) {
        WatchAlarmAcceptedControlStore.record(applicationContext, action, payload)
        WatchAlarmDiagnosticsStore(applicationContext)
            .recordControlAccepted(action, payload)
        WatchAlarmBridge(applicationContext).sendControlAcknowledged(action, payload)
    }

    companion object {
        private const val TAG = "ShiftWatchBridge"
    }
}
