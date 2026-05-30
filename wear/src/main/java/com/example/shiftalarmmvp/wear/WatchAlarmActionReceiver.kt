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
        var controlAction: String? = null

        try {
            when (val requestedAction = WatchAlarmActions.controlPathFor(safeIntent.action)) {
                WatchAlarmProtocol.PATH_ALARM_STOP -> {
                    shouldHoldForRetry = true
                    controlAction = requestedAction
                    PhoneMessageBridge.send(context, controlAction, payload)
                    awaitPhoneAck(context, controlAction, payload, requestStartedAtMillis)
                }

                WatchAlarmProtocol.PATH_ALARM_SNOOZE -> {
                    if (!payload.canSnooze) return
                    shouldHoldForRetry = true
                    controlAction = requestedAction
                    PhoneMessageBridge.send(context, controlAction, payload)
                    awaitPhoneAck(context, controlAction, payload, requestStartedAtMillis)
                }
            }
        } finally {
            val requestedControlAction = controlAction
            if (shouldHoldForRetry && requestedControlAction != null) {
                handler.postDelayed(
                    { restoreIfPhoneAckMissing(context, requestedControlAction, payload, requestStartedAtMillis) },
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

    private fun awaitPhoneAck(
        context: Context,
        action: String,
        payload: WatchAlarmPayload,
        requestStartedAtMillis: Long
    ) {
        WatchAlarmRingingService.stopKeepingNotification(context)
        WatchAlarmNotifier.showControlPending(context, payload, action, requestStartedAtMillis)
        AlarmActivity.awaitControlAcknowledgement(action, payload, requestStartedAtMillis)
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
        if (!WatchAlarmActiveStore.isMatching(context, payload.alarmId, payload.triggeredAtMillis)) {
            Log.i(TAG, "skip control ack timeout restore inactive alarm action=$action alarmId=${payload.alarmId}")
            return
        }

        Log.w(TAG, "control ack timeout powerSaverRetry action=$action alarmId=${payload.alarmId}")
        WatchAlarmNotifier.show(context, payload)
        AlarmActivity.restoreAfterMissingControlAck(payload)
    }

    companion object {
        private const val TAG = "ShiftWearAlarm"
        private const val CONTROL_ACK_TIMEOUT_MILLIS = 8_000L
        private const val BROADCAST_FINISH_DELAY_MILLIS = CONTROL_ACK_TIMEOUT_MILLIS + 500L
    }
}
