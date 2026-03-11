package com.example.shiftalarmmvp.ui

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.shiftalarmmvp.R
import com.example.shiftalarmmvp.data.AlarmSoundType
import com.example.shiftalarmmvp.receiver.AlarmReceiver
import com.example.shiftalarmmvp.recovery.ReliabilityStateCoordinator
import com.example.shiftalarmmvp.recovery.ReliabilityStateSnapshot
import com.example.shiftalarmmvp.recovery.SelfTestStatus
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class SelfTestScheduleConfig(
    val label: String,
    val soundType: AlarmSoundType,
    val customSoundUri: String?,
    val volumePercent: Int,
    val vibrationEnabled: Boolean,
    val snoozeMinutes: Int
)

data class SelfTestActionResult(
    val message: String,
    val snapshot: ReliabilityStateSnapshot?
)

class SelfTestActionHandler(
    context: Context,
    private val reliabilityCoordinator: ReliabilityStateCoordinator
) {
    private val appContext = context.applicationContext

    fun schedule(
        config: SelfTestScheduleConfig,
        canScheduleExact: Boolean,
        nowMillis: Long = System.currentTimeMillis()
    ): SelfTestActionResult {
        val alarmManager = appContext.getSystemService(AlarmManager::class.java)
        val triggerAtMillis = nowMillis + 2 * 60 * 1000L
        val testLabel = if (config.label.isBlank()) {
            appContext.getString(R.string.self_test_default_alarm_label)
        } else {
            appContext.getString(R.string.self_test_alarm_label_format, config.label)
        }
        val testIntent = Intent(appContext, AlarmReceiver::class.java)
            .putExtra(AlarmReceiver.EXTRA_ALARM_ID, 999_999L)
            .putExtra(AlarmReceiver.EXTRA_LABEL, testLabel)
            .putExtra(AlarmReceiver.EXTRA_SOUND_TYPE, config.soundType.name)
            .putExtra(AlarmReceiver.EXTRA_CUSTOM_SOUND_URI, config.customSoundUri)
            .putExtra(AlarmReceiver.EXTRA_VOLUME_PERCENT, config.volumePercent)
            .putExtra(AlarmReceiver.EXTRA_VIBRATION_ENABLED, config.vibrationEnabled)
            .putExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, config.snoozeMinutes)
            .putExtra(AlarmReceiver.EXTRA_SNOOZE_MAX_COUNT, 0)
            .putExtra(AlarmReceiver.EXTRA_SNOOZE_CURRENT_COUNT, 0)

        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            999_999,
            testIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && canScheduleExact -> {
                alarmManager?.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                alarmManager?.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
            else -> {
                alarmManager?.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        }

        val triggerAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(triggerAtMillis), ZoneId.systemDefault())
        val snapshot = reliabilityCoordinator.recordSelfTestScheduled(triggerAtMillis)
        return SelfTestActionResult(
            message = appContext.getString(
                R.string.self_test_schedule_success_format,
                triggerAt.format(DateTimeFormatter.ofPattern("HH:mm"))
            ),
            snapshot = snapshot
        )
    }

    fun cancel(): SelfTestActionResult {
        val alarmManager = appContext.getSystemService(AlarmManager::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            999_999,
            Intent(appContext, AlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent == null) {
            return SelfTestActionResult(
                message = appContext.getString(R.string.self_test_cancel_empty),
                snapshot = null
            )
        }

        alarmManager?.cancel(pendingIntent)
        pendingIntent.cancel()
        val snapshot = reliabilityCoordinator.recordSelfTestCanceled()
        return SelfTestActionResult(
            message = appContext.getString(R.string.self_test_cancel_success),
            snapshot = snapshot
        )
    }

    fun markFeedback(feedback: SelfTestStatus.Feedback): SelfTestActionResult {
        val snapshot = reliabilityCoordinator.recordSelfTestFeedback(feedback)
        val message = when (feedback) {
            SelfTestStatus.Feedback.PASSED -> appContext.getString(R.string.self_test_feedback_passed)
            SelfTestStatus.Feedback.UNCERTAIN -> appContext.getString(R.string.self_test_feedback_uncertain)
            SelfTestStatus.Feedback.FAILED -> appContext.getString(R.string.self_test_feedback_failed)
        }
        return SelfTestActionResult(
            message = message,
            snapshot = snapshot
        )
    }
}