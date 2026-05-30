package com.example.shiftalarmmvp.wear

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log

class WatchAlarmActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val safeIntent = intent ?: return
        val payload = WatchAlarmProtocol.parsePayload(safeIntent) ?: return
        val pendingResult = goAsync()
        val requestStartedAtMillis = System.currentTimeMillis()
        val handler = Handler(Looper.getMainLooper())
        var shouldHoldForRetry = false

        try {
            when (safeIntent.action) {
                WatchAlarmActions.ACTION_STOP -> {
                    shouldHoldForRetry = true
                    PhoneMessageBridge.send(context, WatchAlarmProtocol.PATH_ALARM_STOP, payload)
                    awaitPhoneAck(context, WatchAlarmProtocol.PATH_ALARM_STOP, payload)
                }

                WatchAlarmActions.ACTION_SNOOZE -> {
                    if (!payload.canSnooze) return
                    shouldHoldForRetry = true
                    PhoneMessageBridge.send(context, WatchAlarmProtocol.PATH_ALARM_SNOOZE, payload)
                    awaitPhoneAck(context, WatchAlarmProtocol.PATH_ALARM_SNOOZE, payload)
                }
            }
        } finally {
            if (shouldHoldForRetry) {
                handler.postDelayed(
                    { restoreIfPhoneAckMissing(context, safeIntent.action.orEmpty(), payload, requestStartedAtMillis) },
                    CONTROL_ACK_TIMEOUT_MILLIS
                )
                handler.postDelayed(
                    { pendingResult.finish() },
                    BROADCAST_FINISH_DELAY_MILLIS
                )
            } else {
                pendingResult.finish()
            }
        }
    }

    private fun awaitPhoneAck(context: Context, action: String, payload: WatchAlarmPayload) {
        WatchAlarmRingingService.stop(context)
        WatchAlarmNotifier.showControlPending(context, payload, action)
        AlarmActivity.awaitControlAcknowledgement(action, payload)
    }

    private fun restoreIfPhoneAckMissing(
        context: Context,
        action: String,
        payload: WatchAlarmPayload,
        requestStartedAtMillis: Long
    ) {
        if (WatchAlarmControlAckStore.hasAcknowledgementSince(context, action, payload, requestStartedAtMillis)) {
            return
        }

        Log.w(TAG, "control ack timeout action=$action alarmId=${payload.alarmId}")
        WatchAlarmNotifier.show(context, payload)
        AlarmActivity.restoreAfterMissingControlAck(payload)
    }

    companion object {
        private const val TAG = "ShiftWearAlarm"
        private const val CONTROL_ACK_TIMEOUT_MILLIS = 8_000L
        private const val BROADCAST_FINISH_DELAY_MILLIS = CONTROL_ACK_TIMEOUT_MILLIS + 500L
    }
}
