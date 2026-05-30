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
        Log.i(TAG, "show alarm alarmId=${payload.alarmId} canSnooze=${payload.canSnooze}")
        val serviceStarted = WatchAlarmRingingService.start(this, payload)
        if (!serviceStarted) {
            WatchAlarmNotifier.show(this, payload)
            PhoneMessageBridge.sendAck(this, payload, WatchAlarmProtocol.ACK_DISPLAY_MODE_FALLBACK)
        }
        AlarmActivity.show(this, payload, useLocalVibration = !serviceStarted)
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
        WatchAlarmRingingService.stop(this)
        AlarmActivity.dismissIfMatching(cancellation.alarmId)
    }

    companion object {
        private const val TAG = "ShiftWearAlarm"
    }
}
