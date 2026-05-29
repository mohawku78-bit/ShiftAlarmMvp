package com.example.shiftalarmmvp.recovery

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.shiftalarmmvp.receiver.AlarmReceiver
import com.example.shiftalarmmvp.scheduler.AlarmExactStrategy
import com.example.shiftalarmmvp.scheduler.AlarmScheduleFailureReason
import com.example.shiftalarmmvp.scheduler.AlarmScheduleMode
import com.example.shiftalarmmvp.scheduler.AlarmScheduleResult
import com.example.shiftalarmmvp.scheduler.AlarmTimeCalculator
import com.example.shiftalarmmvp.scheduler.alarmPendingIntentRequestCode
import com.example.shiftalarmmvp.scheduler.scheduleRtcWakeupIntent
import com.example.shiftalarmmvp.ui.AlarmAlertActivity
import java.time.LocalDateTime
import java.time.ZoneId

internal data class DirectBootAlarmRescheduleReport(
    val snapshotCount: Int,
    val scheduledCount: Int,
    val blockedCount: Int
)

internal class DirectBootAlarmRescheduler(context: Context) {
    private val appContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        context.applicationContext.createDeviceProtectedStorageContext()
    } else {
        context.applicationContext
    }
    private val alarmManager: AlarmManager? = appContext.getSystemService(AlarmManager::class.java)
    private val snapshotStore = DirectBootAlarmSnapshotStore(appContext)

    fun rescheduleAllFromSnapshot(): DirectBootAlarmRescheduleReport {
        val snapshots = snapshotStore.loadAll()
        val results = snapshots.map { scheduleSnapshot(it) }
        return DirectBootAlarmRescheduleReport(
            snapshotCount = snapshots.size,
            scheduledCount = results.count { it.scheduled },
            blockedCount = results.count { it.mode == AlarmScheduleMode.BLOCKED }
        )
    }

    fun scheduleSnapshot(snapshot: DirectBootAlarmSnapshot): AlarmScheduleResult {
        val rule = runCatching { snapshot.toAlarmRule() }.getOrNull()
            ?: return AlarmScheduleResult(
                scheduled = false,
                mode = AlarmScheduleMode.BLOCKED,
                failureReason = AlarmScheduleFailureReason.NO_NEXT_TRIGGER
            )
        val nextDateTime = AlarmTimeCalculator.nextTrigger(rule, LocalDateTime.now())
            ?: return AlarmScheduleResult(
                scheduled = false,
                mode = AlarmScheduleMode.BLOCKED,
                failureReason = AlarmScheduleFailureReason.NO_NEXT_TRIGGER
            )
        val triggerMillis = nextDateTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        return scheduleRtcWakeupIntent(
            alarmManager = alarmManager,
            triggerMillis = triggerMillis,
            operation = pendingIntent(snapshot, triggerMillis),
            canScheduleExact = canScheduleExactAlarms(),
            exactStrategy = AlarmExactStrategy.ALARM_CLOCK,
            showIntent = activityPendingIntent(snapshot, triggerMillis)
        )
    }

    private fun canScheduleExactAlarms(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager?.canScheduleExactAlarms() == true
        } else {
            true
        }

    private fun pendingIntent(snapshot: DirectBootAlarmSnapshot, expectedTriggerMillis: Long): PendingIntent {
        val intent = DirectBootAlarmIntent.putSnapshot(
            intent = Intent(appContext, AlarmReceiver::class.java),
            snapshot = snapshot,
            expectedTriggerMillis = expectedTriggerMillis,
            directBootFallback = true
        )
        return PendingIntent.getBroadcast(
            appContext,
            alarmPendingIntentRequestCode(snapshot.id, 1),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun activityPendingIntent(snapshot: DirectBootAlarmSnapshot, expectedTriggerMillis: Long): PendingIntent {
        val intent = DirectBootAlarmIntent.putSnapshot(
            intent = Intent(appContext, AlarmAlertActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            snapshot = snapshot,
            expectedTriggerMillis = expectedTriggerMillis,
            directBootFallback = true
        )
        return PendingIntent.getActivity(
            appContext,
            alarmPendingIntentRequestCode(snapshot.id, 2),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
