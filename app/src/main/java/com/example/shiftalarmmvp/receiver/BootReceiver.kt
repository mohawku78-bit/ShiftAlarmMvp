package com.example.shiftalarmmvp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.shiftalarmmvp.data.AlarmDatabase
import com.example.shiftalarmmvp.data.toDomain
import com.example.shiftalarmmvp.scheduler.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal fun shouldRescheduleOnAction(action: String?): Boolean {
    return action in setOf(
        Intent.ACTION_BOOT_COMPLETED,
        "android.intent.action.TIME_SET",
        Intent.ACTION_DATE_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED
    )
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (!shouldRescheduleOnAction(intent?.action)) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AlarmDatabase.get(context).alarmDao()
                val scheduler = AlarmScheduler(context)
                dao.getAllEnabled().forEach { scheduler.schedule(it.toDomain()) }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
