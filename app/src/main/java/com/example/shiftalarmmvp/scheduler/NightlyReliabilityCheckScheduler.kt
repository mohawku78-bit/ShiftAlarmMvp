package com.example.shiftalarmmvp.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.shiftalarmmvp.receiver.NightlyReliabilityCheckReceiver
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

private const val NIGHTLY_CHECK_REQUEST_CODE = 770042
private val NIGHTLY_CHECK_TIME: LocalTime = LocalTime.of(21, 30)

object NightlyReliabilityCheckScheduler {
    const val ACTION_NIGHTLY_RELIABILITY_CHECK = "com.example.shiftalarmmvp.action.NIGHTLY_RELIABILITY_CHECK"

    fun schedule(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val triggerAtMillis = nextTriggerAtMillis()
        val pendingIntent = pendingIntent(context)

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
            else -> {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        }
    }

    private fun nextTriggerAtMillis(now: LocalDateTime = LocalDateTime.now()): Long {
        var trigger = now
            .withHour(NIGHTLY_CHECK_TIME.hour)
            .withMinute(NIGHTLY_CHECK_TIME.minute)
            .withSecond(0)
            .withNano(0)

        if (!trigger.isAfter(now)) {
            trigger = trigger.plusDays(1)
        }

        return trigger
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, NightlyReliabilityCheckReceiver::class.java).apply {
            action = ACTION_NIGHTLY_RELIABILITY_CHECK
        }
        return PendingIntent.getBroadcast(
            context,
            NIGHTLY_CHECK_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
