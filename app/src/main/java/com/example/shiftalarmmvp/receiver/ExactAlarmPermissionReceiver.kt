package com.example.shiftalarmmvp.receiver

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.shiftalarmmvp.recovery.EnabledAlarmRescheduler
import com.example.shiftalarmmvp.recovery.RescheduleTrigger
import com.example.shiftalarmmvp.scheduler.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ExactAlarmPermissionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        if (intent?.action != AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) return

        val scheduler = AlarmScheduler(context)
        if (!scheduler.canScheduleExactAlarms()) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                EnabledAlarmRescheduler(context).rescheduleAllEnabled(RescheduleTrigger.EXACT_PERMISSION_GRANTED)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
