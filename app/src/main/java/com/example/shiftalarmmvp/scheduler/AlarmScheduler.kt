package com.example.shiftalarmmvp.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.shiftalarmmvp.data.AlarmRule
import com.example.shiftalarmmvp.receiver.AlarmReceiver
import com.example.shiftalarmmvp.ui.MainActivity
import java.time.LocalDateTime
import java.time.ZoneId

enum class AlarmScheduleMode {
    EXACT,
    INEXACT,
    BLOCKED
}

data class AlarmScheduleResult(
    val scheduled: Boolean,
    val mode: AlarmScheduleMode
)

internal fun shouldUseExactAlarm(sdkInt: Int, canScheduleExact: Boolean): Boolean {
    return sdkInt < Build.VERSION_CODES.S || canScheduleExact
}

class AlarmScheduler(private val context: Context) {
    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(rule: AlarmRule): Boolean =
        scheduleWithResult(rule).scheduled

    fun scheduleWithResult(rule: AlarmRule): AlarmScheduleResult {
        val nextDateTime = AlarmTimeCalculator.nextTrigger(rule)
            ?: return AlarmScheduleResult(false, AlarmScheduleMode.BLOCKED)
        val triggerMillis = nextDateTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val alarmPi = pendingIntent(rule.id)
        val showPi = activityPendingIntent(rule.id)
        val canExact = shouldUseExactAlarm(Build.VERSION.SDK_INT, canScheduleExactAlarms())

        if (canExact) {
            val exactResult = runCatching {
                val info = AlarmManager.AlarmClockInfo(triggerMillis, showPi)
                alarmManager.setAlarmClock(info, alarmPi)
                AlarmScheduleResult(true, AlarmScheduleMode.EXACT)
            }.getOrNull()
            if (exactResult != null) return exactResult
        }

        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, alarmPi)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, alarmPi)
            }
            AlarmScheduleResult(true, AlarmScheduleMode.INEXACT)
        }.getOrElse {
            AlarmScheduleResult(false, AlarmScheduleMode.BLOCKED)
        }
    }

    fun canScheduleExactAlarms(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

    fun nextOwnedAlarmClockTriggerMillis(): Long? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return null
        val nextAlarm = alarmManager.nextAlarmClock ?: return null
        return if (nextAlarm.showIntent.creatorPackage == context.packageName) {
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
        alarmManager.cancel(pendingIntent(id))
    }

    private fun pendingIntent(id: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).putExtra(AlarmReceiver.EXTRA_ALARM_ID, id)
        return PendingIntent.getBroadcast(
            context,
            requestCode(id, 1),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun activityPendingIntent(id: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(AlarmReceiver.EXTRA_ALARM_ID, id)
        return PendingIntent.getActivity(
            context,
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



