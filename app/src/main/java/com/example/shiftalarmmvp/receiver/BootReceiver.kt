package com.example.shiftalarmmvp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.shiftalarmmvp.data.AlarmDatabase
import com.example.shiftalarmmvp.data.toDomain
import com.example.shiftalarmmvp.recovery.RescheduleRecoveryStore
import com.example.shiftalarmmvp.scheduler.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal fun shouldRescheduleOnAction(action: String?): Boolean {
    return action in setOf(
        Intent.ACTION_BOOT_COMPLETED,
        Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_DATE_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED,
        Intent.ACTION_MY_PACKAGE_REPLACED
    )
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (!shouldRescheduleOnAction(action)) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            val recoveryStore = RescheduleRecoveryStore(context)
            var enabledCount = 0
            var scheduledCount = 0
            var blockedCount = 0

            try {
                val dao = AlarmDatabase.get(context).alarmDao()
                val scheduler = AlarmScheduler(context)
                val enabledAlarms = dao.getAllEnabled().map { it.toDomain() }
                enabledCount = enabledAlarms.size

                enabledAlarms.forEach { alarm ->
                    val result = scheduler.scheduleWithResult(alarm)
                    if (result.scheduled) scheduledCount += 1 else blockedCount += 1
                }
            } finally {
                recoveryStore.record(
                    action = action.orEmpty(),
                    enabledCount = enabledCount,
                    scheduledCount = scheduledCount,
                    blockedCount = blockedCount
                )
                pendingResult.finish()
            }
        }
    }
}
