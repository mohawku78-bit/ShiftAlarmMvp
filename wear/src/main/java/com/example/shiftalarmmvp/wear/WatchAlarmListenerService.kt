package com.example.shiftalarmmvp.wear

import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class WatchAlarmListenerService : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            WatchAlarmProtocol.PATH_ALARM_START -> {
                val payload = WatchAlarmProtocol.parsePayload(messageEvent.data) ?: return
                Log.i(TAG, "alarm start message alarmId=${payload.alarmId} label=${payload.label}")
                showAlarm(payload)
            }

            WatchAlarmProtocol.PATH_ALARM_CANCEL -> {
                val cancellation = WatchAlarmProtocol.parseCancellation(messageEvent.data) ?: return
                Log.i(TAG, "alarm cancel message alarmId=${cancellation.alarmId}")
                cancelAlarm(cancellation)
            }

            WatchAlarmProtocol.PATH_ALARM_CONTROL_ACK -> {
                val ack = WatchAlarmProtocol.parseControlAcknowledgement(messageEvent.data) ?: return
                Log.i(TAG, "control ack message action=${ack.action} alarmId=${ack.payload.alarmId}")
                handleControlAcknowledgement(ack)
            }
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents
            .filter { it.type == DataEvent.TYPE_CHANGED }
            .forEach { event ->
                when (event.dataItem.uri.path) {
                    WatchAlarmProtocol.PATH_ALARM_ACTIVE -> {
                        val payload = WatchAlarmProtocol.parsePayload(event.dataItem) ?: return@forEach
                        Log.i(TAG, "alarm active data alarmId=${payload.alarmId} label=${payload.label}")
                        showAlarm(payload)
                    }

                    WatchAlarmProtocol.PATH_ALARM_CANCELLED -> {
                        val cancellation = WatchAlarmProtocol.parseCancellation(event.dataItem) ?: return@forEach
                        Log.i(TAG, "alarm cancelled data alarmId=${cancellation.alarmId}")
                        cancelAlarm(cancellation)
                    }

                    WatchAlarmProtocol.PATH_ALARM_CONTROL_ACK -> {
                        val ack = WatchAlarmProtocol.parseControlAcknowledgement(event.dataItem) ?: return@forEach
                        Log.i(TAG, "control ack data action=${ack.action} alarmId=${ack.payload.alarmId}")
                        handleControlAcknowledgement(ack)
                    }
                }
            }
    }

    private fun showAlarm(payload: WatchAlarmPayload) {
        if (WatchAlarmEventGate.matchesAcceptedCancel(this, payload.alarmId, payload.triggeredAtMillis)) {
            Log.i(TAG, "ignore already cancelled alarm alarmId=${payload.alarmId}")
            return
        }
        if (!WatchAlarmEventGate.accept(this, WatchAlarmEventGate.EVENT_START, payload.alarmId, payload.triggeredAtMillis)) {
            Log.i(TAG, "ignore duplicate alarm alarmId=${payload.alarmId}")
            return
        }
        Log.i(TAG, "show alarm signal alarmId=${payload.alarmId} canSnooze=${payload.canSnooze}")
        WatchAlarmActiveStore.record(this, payload)
        val notificationShown = WatchAlarmNotifier.show(this, payload)
        if (!notificationShown) {
            AlarmActivity.show(this, payload, useLocalVibration = payload.vibrationEnabled)
        }
        val displayMode = if (notificationShown) {
            WatchAlarmProtocol.ACK_DISPLAY_MODE_NOTIFICATION
        } else {
            WatchAlarmProtocol.ACK_DISPLAY_MODE_FALLBACK
        }
        PhoneMessageBridge.sendAck(this, payload, displayMode)
    }

    private fun cancelAlarm(cancellation: WatchAlarmCancellation) {
        if (!WatchAlarmEventGate.accept(this, WatchAlarmEventGate.EVENT_CANCEL, cancellation.alarmId, cancellation.eventTimeMillis)) {
            Log.i(TAG, "ignore duplicate cancel alarmId=${cancellation.alarmId}")
            return
        }
        if (!WatchAlarmEventGate.matchesAcceptedStart(this, cancellation.alarmId, cancellation.eventTimeMillis)) {
            Log.i(
                TAG,
                "record cancel before matching start alarmId=${cancellation.alarmId} triggeredAt=${cancellation.eventTimeMillis}"
            )
            return
        }
        Log.i(TAG, "cancel alarm alarmId=${cancellation.alarmId}")
        val clearedActiveAlarm = WatchAlarmActiveStore.clearIfMatching(
            this,
            cancellation.alarmId,
            cancellation.eventTimeMillis
        )
        if (!clearedActiveAlarm) {
            Log.i(
                TAG,
                "ignore stale cancel for inactive alarmId=${cancellation.alarmId} " +
                    "triggeredAt=${cancellation.eventTimeMillis}"
            )
            AlarmActivity.dismissIfMatching(cancellation.alarmId, cancellation.eventTimeMillis)
            return
        }
        WatchAlarmNotifier.cancel(this)
        AlarmActivity.dismissIfMatching(cancellation.alarmId, cancellation.eventTimeMillis)
    }

    private fun handleControlAcknowledgement(ack: WatchAlarmControlAcknowledgement) {
        WatchAlarmControlAckStore.record(this, ack.action, ack.payload)
        if (ack.action != WatchAlarmProtocol.PATH_ALARM_STOP &&
            ack.action != WatchAlarmProtocol.PATH_ALARM_SNOOZE
        ) {
            Log.i(TAG, "ignore unsupported control ack action=${ack.action} alarmId=${ack.payload.alarmId}")
            return
        }

        val matchesActiveAlarm = WatchAlarmActiveStore.isMatching(
            this,
            ack.payload.alarmId,
            ack.payload.triggeredAtMillis
        )
        if (!matchesActiveAlarm) {
            Log.i(
                TAG,
                "ignore stale control ack action=${ack.action} alarmId=${ack.payload.alarmId} " +
                    "triggeredAt=${ack.payload.triggeredAtMillis}"
            )
            AlarmActivity.dismissIfControlAcknowledged(ack.action, ack.payload)
            return
        }

        WatchAlarmActiveStore.clearIfMatching(this, ack.payload.alarmId, ack.payload.triggeredAtMillis)
        WatchAlarmNotifier.cancel(this)
        AlarmActivity.dismissIfControlAcknowledged(ack.action, ack.payload)
    }

    companion object {
        private const val TAG = "ShiftWearAlarm"
    }
}
