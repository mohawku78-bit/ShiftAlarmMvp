package com.example.shiftalarmmvp.recovery

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

internal object AlarmWatchdogScheduler {
    private const val PERIODIC_WORK_NAME = "alarm-watchdog-periodic"
    private const val ONE_TIME_WORK_NAME = "alarm-watchdog-next"

    fun schedule(
        context: Context,
        pendingRegistrations: List<TrackedAlarmRegistration>? = null,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val appContext = context.applicationContext
        val workManager = WorkManager.getInstance(appContext)
        val periodicRequest = PeriodicWorkRequestBuilder<AlarmWatchdogWorker>(6, TimeUnit.HOURS).build()
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest
        )

        val pending = pendingRegistrations ?: TriggerHistoryStore(appContext).loadPendingRegistrations()
        val nextCheckAtMillis = nextAlarmWatchdogCheckAtMillis(pending, nowMillis)
        if (nextCheckAtMillis == null) {
            workManager.cancelUniqueWork(ONE_TIME_WORK_NAME)
            return
        }

        val delayMillis = (nextCheckAtMillis - nowMillis).coerceAtLeast(0L)
        val oneTimeRequest = OneTimeWorkRequestBuilder<AlarmWatchdogWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniqueWork(
            ONE_TIME_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            oneTimeRequest
        )
    }
}
