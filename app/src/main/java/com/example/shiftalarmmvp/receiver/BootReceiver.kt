package com.example.shiftalarmmvp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.shiftalarmmvp.recovery.DirectBootAlarmRescheduler
import com.example.shiftalarmmvp.recovery.EnabledAlarmRescheduler
import com.example.shiftalarmmvp.recovery.rescheduleTriggerFromSystemAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal fun shouldRescheduleOnAction(action: String?): Boolean {
    return action == Intent.ACTION_LOCKED_BOOT_COMPLETED || rescheduleTriggerFromSystemAction(action) != null
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && intent?.action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            DirectBootAlarmRescheduler(context).rescheduleAllFromSnapshot()
            return
        }

        val trigger = rescheduleTriggerFromSystemAction(intent?.action) ?: return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                EnabledAlarmRescheduler(context).rescheduleAllEnabled(trigger)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
