package com.example.shiftalarmmvp.wear

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper

class WatchAlarmActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val safeIntent = intent ?: return
        val payload = WatchAlarmProtocol.parsePayload(safeIntent) ?: return
        val pendingResult = goAsync()
        var shouldHoldForRetry = false

        try {
            when (safeIntent.action) {
                WatchAlarmActions.ACTION_STOP -> {
                    shouldHoldForRetry = true
                    PhoneMessageBridge.send(context, WatchAlarmProtocol.PATH_ALARM_STOP, payload)
                    dismissLocal(context, payload.alarmId)
                }

                WatchAlarmActions.ACTION_SNOOZE -> {
                    if (!payload.canSnooze) return
                    shouldHoldForRetry = true
                    PhoneMessageBridge.send(context, WatchAlarmProtocol.PATH_ALARM_SNOOZE, payload)
                    dismissLocal(context, payload.alarmId)
                }
            }
        } finally {
            if (shouldHoldForRetry) {
                Handler(Looper.getMainLooper()).postDelayed(
                    { pendingResult.finish() },
                    PhoneMessageBridge.CONTROL_RETRY_WINDOW_MILLIS
                )
            } else {
                pendingResult.finish()
            }
        }
    }

    private fun dismissLocal(context: Context, alarmId: Long) {
        WatchAlarmRingingService.stop(context)
        WatchAlarmNotifier.cancel(context)
        AlarmActivity.dismissIfMatching(alarmId)
    }
}
