package com.example.shiftalarmmvp.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.shiftalarmmvp.data.AlarmRule
import com.example.shiftalarmmvp.receiver.AlarmReceiver
import com.example.shiftalarmmvp.recovery.PrimaryAlarmScheduleTracker
import com.example.shiftalarmmvp.ui.MainActivity
import java.time.LocalDateTime
import java.time.ZoneId

enum class AlarmScheduleMode {
    EXACT,
    INEXACT,
    BLOCKED
}

enum class AlarmScheduleFailureReason {
    NO_NEXT_TRIGGER,
    EXACT_PERMISSION_DENIED,
    EXACT_SECURITY_EXCEPTION,
    PLATFORM_FAILURE
}

internal enum class AlarmExactStrategy {
    ALARM_CLOCK,
    EXACT_ALLOW_WHILE_IDLE
}

data class AlarmScheduleResult(
    val scheduled: Boolean,
    val mode: AlarmScheduleMode,
    val failureReason: AlarmScheduleFailureReason? = null,
    val scheduledTriggerMillis: Long? = null
)

internal fun shouldUseExactAlarm(sdkInt: Int, canScheduleExact: Boolean): Boolean {
    return sdkInt < Build.VERSION_CODES.S || canScheduleExact
}

internal fun scheduleRtcWakeupIntent(
    alarmManager: AlarmManager?,
    triggerMillis: Long,
    operation: PendingIntent,
    canScheduleExact: Boolean,
    sdkInt: Int = Build.VERSION.SDK_INT,
    exactStrategy: AlarmExactStrategy = AlarmExactStrategy.ALARM_CLOCK,
    showIntent: PendingIntent? = null
): AlarmScheduleResult {
    if (alarmManager == null) {
        return AlarmScheduleResult(
            scheduled = false,
            mode = AlarmScheduleMode.BLOCKED,
            failureReason = AlarmScheduleFailureReason.PLATFORM_FAILURE
        )
    }

    var degradeReason: AlarmScheduleFailureReason? = null
    if (shouldUseExactAlarm(sdkInt, canScheduleExact)) {
        val exactResult = runCatching {
            when (exactStrategy) {
                AlarmExactStrategy.ALARM_CLOCK -> {
                    val info = AlarmManager.AlarmClockInfo(triggerMillis, showIntent ?: operation)
                    alarmManager.setAlarmClock(info, operation)
                }
                AlarmExactStrategy.EXACT_ALLOW_WHILE_IDLE -> {
                    if (sdkInt >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, operation)
                    } else {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMillis, operation)
                    }
                }
            }
            AlarmScheduleResult(
                scheduled = true,
                mode = AlarmScheduleMode.EXACT,
                scheduledTriggerMillis = triggerMillis
            )
        }.getOrElse { error ->
            degradeReason = if (error is SecurityException) {
                AlarmScheduleFailureReason.EXACT_SECURITY_EXCEPTION
            } else {
                AlarmScheduleFailureReason.PLATFORM_FAILURE
            }
            null
        }
        if (exactResult != null) return exactResult
    } else if (sdkInt >= Build.VERSION_CODES.S) {
        degradeReason = AlarmScheduleFailureReason.EXACT_PERMISSION_DENIED
    }

    return runCatching {
        if (sdkInt >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, operation)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, operation)
        }
        AlarmScheduleResult(
            scheduled = true,
            mode = AlarmScheduleMode.INEXACT,
            failureReason = degradeReason,
            scheduledTriggerMillis = triggerMillis
        )
    }.getOrElse {
        AlarmScheduleResult(
            scheduled = false,
            mode = AlarmScheduleMode.BLOCKED,
            failureReason = degradeReason ?: AlarmScheduleFailureReason.PLATFORM_FAILURE
        )
    }
}

class AlarmScheduler(private val context: Context) {
    private val appContext = context.applicationContext
    private val alarmManager: AlarmManager? = appContext.getSystemService(AlarmManager::class.java)
    private val primaryAlarmScheduleTracker = PrimaryAlarmScheduleTracker(appContext)

    fun schedule(rule: AlarmRule): Boolean =
        scheduleWithResult(rule).scheduled

    fun scheduleWithResult(rule: AlarmRule): AlarmScheduleResult {
        val nextDateTime = AlarmTimeCalculator.nextTrigger(rule)
            ?: return AlarmScheduleResult(
                scheduled = false,
                mode = AlarmScheduleMode.BLOCKED,
                failureReason = AlarmScheduleFailureReason.NO_NEXT_TRIGGER
            ).also { primaryAlarmScheduleTracker.recordSchedule(rule, it) }
        val triggerMillis = nextDateTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val result = scheduleRtcWakeupIntent(
            alarmManager = alarmManager,
            triggerMillis = triggerMillis,
            operation = pendingIntent(rule.id, triggerMillis),
            canScheduleExact = canScheduleExactAlarms(),
            exactStrategy = AlarmExactStrategy.ALARM_CLOCK,
            showIntent = activityPendingIntent(rule.id)
        )
        primaryAlarmScheduleTracker.recordSchedule(rule, result)
        return result
    }

    fun canScheduleExactAlarms(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager?.canScheduleExactAlarms() == true
        } else {
            true
        }

    fun nextOwnedAlarmClockTriggerMillis(): Long? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return null
        val nextAlarm = alarmManager?.nextAlarmClock ?: return null
        return if (nextAlarm.showIntent.creatorPackage == appContext.packageName) {
            nextAlarm.triggerTime
        } else {
            null
        }
    }

    fun computeNextTriggerMillis(
        rule: AlarmRule,
        now: LocalDateTime = LocalDateTime.now(),
        zone: ZoneId = ZoneId.systemDefault()
    ): Long? {
        val next = AlarmTimeCalculator.nextTrigger(rule, now) ?: return null
        return next.atZone(zone).toInstant().toEpochMilli()
    }

    fun cancel(id: Long) {
        alarmManager?.cancel(pendingIntent(id))
        primaryAlarmScheduleTracker.clearAlarm(id)
    }

    private fun pendingIntent(id: Long, expectedTriggerMillis: Long? = null): PendingIntent {
        val intent = Intent(appContext, AlarmReceiver::class.java)
            .putExtra(AlarmReceiver.EXTRA_ALARM_ID, id)
        if (expectedTriggerMillis != null && expectedTriggerMillis > 0L) {
            intent.putExtra(AlarmReceiver.EXTRA_EXPECTED_TRIGGER_MILLIS, expectedTriggerMillis)
        }
        return PendingIntent.getBroadcast(
            appContext,
            requestCode(id, 1),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun activityPendingIntent(id: Long): PendingIntent {
        val intent = Intent(appContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(AlarmReceiver.EXTRA_ALARM_ID, id)
        return PendingIntent.getActivity(
            appContext,
            requestCode(id, 2),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun requestCode(id: Long, base: Int): Int {
        val normalizedId = id.toInt()
        return (normalizedId * 31 + base).xor(0x5a5a5a5a)
    }
}
