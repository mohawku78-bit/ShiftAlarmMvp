package com.example.shiftalarmmvp.wear

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class SideBySideWatchAlarmActionTestReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val payload = WatchAlarmActiveStore.read(context)
        if (payload == null) {
            Log.w(TAG, "side-by-side watch action requested without active alarm action=${intent?.action}")
            return
        }

        if (intent?.action == ACTION_WATCH_TEST_OPEN_ALARM) {
            Log.i(
                TAG,
                "side-by-side watch open alarm activity alarmId=${payload.alarmId} " +
                    "triggeredAt=${payload.triggeredAtMillis}"
            )
            AlarmActivity.show(context, payload, useLocalVibration = false)
            return
        }

        val receiverAction = when (intent?.action) {
            ACTION_WATCH_TEST_STOP -> WatchAlarmActions.ACTION_STOP
            ACTION_WATCH_TEST_SNOOZE -> {
                if (!payload.canSnooze) {
                    Log.w(TAG, "side-by-side snooze requested but not allowed alarmId=${payload.alarmId}")
                    return
                }
                WatchAlarmActions.ACTION_SNOOZE
            }

            else -> return
        }

        Log.i(
            TAG,
            "side-by-side watch action action=$receiverAction " +
                "alarmId=${payload.alarmId} triggeredAt=${payload.triggeredAtMillis}"
        )
        context.sendBroadcast(
            Intent(context, WatchAlarmActionReceiver::class.java)
                .setAction(receiverAction)
                .putExtra(WatchAlarmProtocol.EXTRA_PAYLOAD_JSON, WatchAlarmProtocol.toJson(payload))
        )
    }

    companion object {
        const val ACTION_WATCH_TEST_STOP = "com.example.shiftalarmmvp.action.WATCH_TEST_STOP"
        const val ACTION_WATCH_TEST_SNOOZE = "com.example.shiftalarmmvp.action.WATCH_TEST_SNOOZE"
        const val ACTION_WATCH_TEST_OPEN_ALARM = "com.example.shiftalarmmvp.action.WATCH_TEST_OPEN_ALARM"

        private const val TAG = "ShiftWearAlarm"
    }
}
