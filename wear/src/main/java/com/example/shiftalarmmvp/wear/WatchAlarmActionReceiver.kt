package com.example.shiftalarmmvp.wear

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class WatchAlarmActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val safeIntent = intent ?: return
        val payload = WatchAlarmProtocol.parsePayload(safeIntent) ?: return

        when (safeIntent.action) {
            WatchAlarmActions.ACTION_STOP -> {
                PhoneMessageBridge.send(context, WatchAlarmProtocol.PATH_ALARM_STOP, payload)
                dismissLocal(context, payload.alarmId)
            }

            WatchAlarmActions.ACTION_SNOOZE -> {
                if (!payload.canSnooze) return
                PhoneMessageBridge.send(context, WatchAlarmProtocol.PATH_ALARM_SNOOZE, payload)
                dismissLocal(context, payload.alarmId)
            }
        }
    }

    private fun dismissLocal(context: Context, alarmId: Long) {
        WatchAlarmRingingService.stop(context)
        WatchAlarmNotifier.cancel(context)
        AlarmActivity.dismissIfMatching(alarmId)
    }
}
