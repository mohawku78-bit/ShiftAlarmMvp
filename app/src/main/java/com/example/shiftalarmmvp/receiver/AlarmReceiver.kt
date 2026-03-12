package com.example.shiftalarmmvp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.shiftalarmmvp.data.AlarmDatabase
import com.example.shiftalarmmvp.data.AlarmSoundType
import com.example.shiftalarmmvp.data.toDomain
import com.example.shiftalarmmvp.recovery.PrimaryAlarmScheduleTracker
import com.example.shiftalarmmvp.recovery.SELF_TEST_ALARM_ID
import com.example.shiftalarmmvp.recovery.SelfTestStatusStore
import com.example.shiftalarmmvp.recovery.alarmReceiverStrings
import com.example.shiftalarmmvp.scheduler.AlarmScheduler
import com.example.shiftalarmmvp.service.AlarmRingingService
import com.example.shiftalarmmvp.ui.AlarmAlertActivity
import com.example.shiftalarmmvp.ui.AlarmLogStore
import com.example.shiftalarmmvp.ui.AlarmLogType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val safeIntent = intent ?: return
        val alarmId = safeIntent.getLongExtra(EXTRA_ALARM_ID, -1L)
        if (alarmId <= 0) return

        val texts = alarmReceiverStrings(context.resources)
        val currentSnoozeCount = safeIntent.getIntExtra(EXTRA_SNOOZE_CURRENT_COUNT, 0)
        val snoozeMaxFromIntent = safeIntent.getIntExtra(EXTRA_SNOOZE_MAX_COUNT, Int.MIN_VALUE)
        val incomingSnoozeMinutes = safeIntent.getIntExtra(EXTRA_SNOOZE_MINUTES, Int.MIN_VALUE)
        val expectedTriggerMillis = safeIntent.getLongExtra(EXTRA_EXPECTED_TRIGGER_MILLIS, 0L).takeIf { it > 0L }

        val testLabel = safeIntent.getStringExtra(EXTRA_LABEL).orEmpty()
        val testSoundType = runCatching {
            AlarmSoundType.valueOf(safeIntent.getStringExtra(EXTRA_SOUND_TYPE) ?: AlarmSoundType.ALARM.name)
        }.getOrDefault(AlarmSoundType.ALARM)
        val testCustomSoundUri = safeIntent.getStringExtra(EXTRA_CUSTOM_SOUND_URI)
        val testVolumePercent = safeIntent.getIntExtra(EXTRA_VOLUME_PERCENT, 100).coerceIn(0, 100)
        val testVibrationEnabled = safeIntent.getBooleanExtra(EXTRA_VIBRATION_ENABLED, true)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AlarmDatabase.get(context).alarmDao()
                val alarm = dao.getById(alarmId)?.toDomain()
                val tracker = PrimaryAlarmScheduleTracker(context)

                if (alarm != null && alarm.enabled) {
                    val resolvedMax = if (snoozeMaxFromIntent == Int.MIN_VALUE) alarm.snoozeMaxCount else snoozeMaxFromIntent.coerceIn(0, 99)
                    val resolvedMinutes = if (incomingSnoozeMinutes == Int.MIN_VALUE || incomingSnoozeMinutes <= 0) alarm.snoozeMinutes else incomingSnoozeMinutes

                    startRingingService(
                        context = context,
                        alarmId = alarmId,
                        label = alarm.label,
                        soundType = alarm.soundType,
                        customSoundUri = alarm.customSoundUri,
                        volumePercent = alarm.volumePercent,
                        vibrationEnabled = alarm.vibrationEnabled,
                        snoozeMinutes = resolvedMinutes,
                        snoozeMaxCount = resolvedMax,
                        currentSnoozeCount = currentSnoozeCount
                    )
                    AlarmLogStore(context).append(
                        alarmId = alarmId,
                        label = alarm.label,
                        type = AlarmLogType.RING_START,
                        detail = if (currentSnoozeCount > 0) {
                            texts.ringStartSnoozeFormat.format(currentSnoozeCount)
                        } else {
                            texts.ringStartRegular
                        }
                    )
                    tracker.recordTrigger(
                        alarmId = alarmId,
                        label = alarm.label,
                        expectedTriggerMillis = expectedTriggerMillis
                    )
                    AlarmScheduler(context).schedule(alarm)
                } else if (alarmId == SELF_TEST_ALARM_ID) {
                    SelfTestStatusStore(context).recordTriggered()
                    val resolvedMinutes = if (incomingSnoozeMinutes == Int.MIN_VALUE || incomingSnoozeMinutes <= 0) 5 else incomingSnoozeMinutes
                    val resolvedMax = if (snoozeMaxFromIntent == Int.MIN_VALUE) 0 else snoozeMaxFromIntent.coerceIn(0, 99)
                    startRingingService(
                        context = context,
                        alarmId = alarmId,
                        label = if (testLabel.isBlank()) texts.testAlarmFallback else testLabel,
                        soundType = testSoundType,
                        customSoundUri = testCustomSoundUri,
                        volumePercent = testVolumePercent,
                        vibrationEnabled = testVibrationEnabled,
                        snoozeMinutes = resolvedMinutes,
                        snoozeMaxCount = resolvedMax,
                        currentSnoozeCount = currentSnoozeCount
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun startRingingService(
        context: Context,
        alarmId: Long,
        label: String,
        soundType: AlarmSoundType,
        customSoundUri: String?,
        volumePercent: Int,
        vibrationEnabled: Boolean,
        snoozeMinutes: Int,
        snoozeMaxCount: Int,
        currentSnoozeCount: Int
    ) {
        val serviceIntent = Intent(context, AlarmRingingService::class.java)
            .setAction(AlarmRingingService.ACTION_START)
            .putExtra(EXTRA_ALARM_ID, alarmId)
            .putExtra(EXTRA_LABEL, label)
            .putExtra(EXTRA_SOUND_TYPE, soundType.name)
            .putExtra(EXTRA_CUSTOM_SOUND_URI, customSoundUri)
            .putExtra(EXTRA_VOLUME_PERCENT, volumePercent)
            .putExtra(EXTRA_VIBRATION_ENABLED, vibrationEnabled)
            .putExtra(EXTRA_SNOOZE_MINUTES, snoozeMinutes)
            .putExtra(EXTRA_SNOOZE_MAX_COUNT, snoozeMaxCount)
            .putExtra(EXTRA_SNOOZE_CURRENT_COUNT, currentSnoozeCount)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching { context.startForegroundService(serviceIntent) }
        } else {
            runCatching { context.startService(serviceIntent) }
        }

        runCatching {
            context.startActivity(
                Intent(context, AlarmAlertActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(EXTRA_ALARM_ID, alarmId)
                    .putExtra(EXTRA_LABEL, label)
                    .putExtra(EXTRA_SOUND_TYPE, soundType.name)
                    .putExtra(EXTRA_CUSTOM_SOUND_URI, customSoundUri)
                    .putExtra(EXTRA_VOLUME_PERCENT, volumePercent)
                    .putExtra(EXTRA_VIBRATION_ENABLED, vibrationEnabled)
                    .putExtra(EXTRA_SNOOZE_MINUTES, snoozeMinutes)
                    .putExtra(EXTRA_SNOOZE_MAX_COUNT, snoozeMaxCount)
                    .putExtra(EXTRA_SNOOZE_CURRENT_COUNT, currentSnoozeCount)
            )
        }
    }

    companion object {
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_LABEL = "extra_label"
        const val EXTRA_SOUND_TYPE = "extra_sound_type"
        const val EXTRA_CUSTOM_SOUND_URI = "extra_custom_sound_uri"
        const val EXTRA_VOLUME_PERCENT = "extra_volume_percent"
        const val EXTRA_VIBRATION_ENABLED = "extra_vibration_enabled"
        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
        const val EXTRA_SNOOZE_MAX_COUNT = "extra_snooze_max_count"
        const val EXTRA_SNOOZE_CURRENT_COUNT = "extra_snooze_current_count"
        const val EXTRA_EXPECTED_TRIGGER_MILLIS = "extra_expected_trigger_millis"
    }
}
