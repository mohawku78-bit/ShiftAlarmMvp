package com.example.shiftalarmmvp.recovery

import android.content.Intent
import com.example.shiftalarmmvp.receiver.AlarmReceiver

internal object DirectBootAlarmIntent {
    private const val EXTRA_SNAPSHOT_HOUR = "extra_direct_boot_hour"
    private const val EXTRA_SNAPSHOT_MINUTE = "extra_direct_boot_minute"
    private const val EXTRA_SNAPSHOT_WEEKLY_PATTERN_CSV = "extra_direct_boot_weekly_pattern_csv"
    private const val EXTRA_SNAPSHOT_INTERVAL_WEEKS = "extra_direct_boot_interval_weeks"
    private const val EXTRA_SNAPSHOT_ANCHOR_EPOCH_DAY = "extra_direct_boot_anchor_epoch_day"
    private const val EXTRA_SNAPSHOT_SKIP_DATE_EPOCH_DAYS = "extra_direct_boot_skip_date_epoch_days"
    private const val EXTRA_SNAPSHOT_ADD_DATE_EPOCH_DAYS = "extra_direct_boot_add_date_epoch_days"

    fun putSnapshot(
        intent: Intent,
        snapshot: DirectBootAlarmSnapshot,
        expectedTriggerMillis: Long? = null,
        directBootFallback: Boolean = false
    ): Intent {
        intent
            .putExtra(AlarmReceiver.EXTRA_ALARM_ID, snapshot.id)
            .putExtra(AlarmReceiver.EXTRA_LABEL, snapshot.label)
            .putExtra(AlarmReceiver.EXTRA_SOUND_TYPE, snapshot.soundType)
            .putExtra(AlarmReceiver.EXTRA_CUSTOM_SOUND_URI, snapshot.customSoundUri)
            .putExtra(AlarmReceiver.EXTRA_VOLUME_PERCENT, snapshot.volumePercent)
            .putExtra(AlarmReceiver.EXTRA_VIBRATION_ENABLED, snapshot.vibrationEnabled)
            .putExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, snapshot.snoozeMinutes)
            .putExtra(AlarmReceiver.EXTRA_SNOOZE_MAX_COUNT, snapshot.snoozeMaxCount)
            .putExtra(AlarmReceiver.EXTRA_DIRECT_BOOT_FALLBACK, directBootFallback)
            .putExtra(EXTRA_SNAPSHOT_HOUR, snapshot.hour)
            .putExtra(EXTRA_SNAPSHOT_MINUTE, snapshot.minute)
            .putExtra(EXTRA_SNAPSHOT_WEEKLY_PATTERN_CSV, snapshot.weeklyPatternCsv)
            .putExtra(EXTRA_SNAPSHOT_INTERVAL_WEEKS, snapshot.intervalWeeks)
            .putExtra(EXTRA_SNAPSHOT_ANCHOR_EPOCH_DAY, snapshot.anchorEpochDay)
            .putExtra(EXTRA_SNAPSHOT_SKIP_DATE_EPOCH_DAYS, snapshot.skipDateEpochDays)
            .putExtra(EXTRA_SNAPSHOT_ADD_DATE_EPOCH_DAYS, snapshot.addDateEpochDays)

        if (expectedTriggerMillis != null && expectedTriggerMillis > 0L) {
            intent.putExtra(AlarmReceiver.EXTRA_EXPECTED_TRIGGER_MILLIS, expectedTriggerMillis)
        }
        return intent
    }

    fun fromIntent(intent: Intent): DirectBootAlarmSnapshot? {
        val id = intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1L)
        if (id <= 0L) return null
        val hour = intent.getIntExtra(EXTRA_SNAPSHOT_HOUR, -1)
        val minute = intent.getIntExtra(EXTRA_SNAPSHOT_MINUTE, -1)
        if (hour !in 0..23 || minute !in 0..59) return null

        return DirectBootAlarmSnapshot(
            id = id,
            label = intent.getStringExtra(AlarmReceiver.EXTRA_LABEL).orEmpty(),
            hour = hour,
            minute = minute,
            weeklyPatternCsv = intent.getStringExtra(EXTRA_SNAPSHOT_WEEKLY_PATTERN_CSV).orEmpty(),
            intervalWeeks = intent.getIntExtra(EXTRA_SNAPSHOT_INTERVAL_WEEKS, 1),
            anchorEpochDay = intent.getLongExtra(EXTRA_SNAPSHOT_ANCHOR_EPOCH_DAY, 0L),
            snoozeMinutes = intent.getIntExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, 5),
            snoozeMaxCount = intent.getIntExtra(AlarmReceiver.EXTRA_SNOOZE_MAX_COUNT, 0),
            soundType = intent.getStringExtra(AlarmReceiver.EXTRA_SOUND_TYPE) ?: "ALARM",
            customSoundUri = intent.getStringExtra(AlarmReceiver.EXTRA_CUSTOM_SOUND_URI),
            volumePercent = intent.getIntExtra(AlarmReceiver.EXTRA_VOLUME_PERCENT, 100),
            vibrationEnabled = intent.getBooleanExtra(AlarmReceiver.EXTRA_VIBRATION_ENABLED, true),
            skipDateEpochDays = intent.getStringExtra(EXTRA_SNAPSHOT_SKIP_DATE_EPOCH_DAYS).orEmpty(),
            addDateEpochDays = intent.getStringExtra(EXTRA_SNAPSHOT_ADD_DATE_EPOCH_DAYS).orEmpty()
        )
    }
}
