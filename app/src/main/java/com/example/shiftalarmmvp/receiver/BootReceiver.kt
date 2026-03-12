package com.example.shiftalarmmvp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.shiftalarmmvp.recovery.EnabledAlarmRescheduler
import com.example.shiftalarmmvp.recovery.rescheduleTriggerFromSystemAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal fun shouldRescheduleOnAction(action: String?): Boolean {
    return rescheduleTriggerFromSystemAction(action) != null
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
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
