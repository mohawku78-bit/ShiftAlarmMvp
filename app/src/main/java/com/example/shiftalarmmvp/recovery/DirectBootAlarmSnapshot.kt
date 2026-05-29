package com.example.shiftalarmmvp.recovery

import com.example.shiftalarmmvp.data.AlarmRule
import com.example.shiftalarmmvp.data.AlarmRuleEntity
import com.example.shiftalarmmvp.data.toDomain
import com.example.shiftalarmmvp.data.toEntity
import org.json.JSONObject

internal data class DirectBootAlarmSnapshot(
    val id: Long,
    val label: String,
    val hour: Int,
    val minute: Int,
    val weeklyPatternCsv: String,
    val intervalWeeks: Int,
    val anchorEpochDay: Long,
    val snoozeMinutes: Int,
    val snoozeMaxCount: Int,
    val soundType: String,
    val customSoundUri: String?,
    val volumePercent: Int,
    val vibrationEnabled: Boolean,
    val skipDateEpochDays: String,
    val addDateEpochDays: String
) {
    fun toAlarmRule(): AlarmRule {
        return AlarmRuleEntity(
            id = id,
            label = label,
            hour = hour,
            minute = minute,
            weeklyPatternCsv = weeklyPatternCsv,
            intervalWeeks = intervalWeeks,
            anchorEpochDay = anchorEpochDay,
            snoozeMinutes = snoozeMinutes,
            snoozeMaxCount = snoozeMaxCount,
            soundType = soundType,
            customSoundUri = customSoundUri,
            volumePercent = volumePercent,
            vibrationEnabled = vibrationEnabled,
            skipDateEpochDays = skipDateEpochDays,
            addDateEpochDays = addDateEpochDays,
            enabled = true
        ).toDomain()
    }

    fun toJson(): JSONObject {
        return JSONObject()
            .put(KEY_ID, id)
            .put(KEY_LABEL, label)
            .put(KEY_HOUR, hour)
            .put(KEY_MINUTE, minute)
            .put(KEY_WEEKLY_PATTERN_CSV, weeklyPatternCsv)
            .put(KEY_INTERVAL_WEEKS, intervalWeeks)
            .put(KEY_ANCHOR_EPOCH_DAY, anchorEpochDay)
            .put(KEY_SNOOZE_MINUTES, snoozeMinutes)
            .put(KEY_SNOOZE_MAX_COUNT, snoozeMaxCount)
            .put(KEY_SOUND_TYPE, soundType)
            .put(KEY_CUSTOM_SOUND_URI, customSoundUri)
            .put(KEY_VOLUME_PERCENT, volumePercent)
            .put(KEY_VIBRATION_ENABLED, vibrationEnabled)
            .put(KEY_SKIP_DATE_EPOCH_DAYS, skipDateEpochDays)
            .put(KEY_ADD_DATE_EPOCH_DAYS, addDateEpochDays)
    }

    companion object {
        private const val KEY_ID = "id"
        private const val KEY_LABEL = "label"
        private const val KEY_HOUR = "hour"
        private const val KEY_MINUTE = "minute"
        private const val KEY_WEEKLY_PATTERN_CSV = "weeklyPatternCsv"
        private const val KEY_INTERVAL_WEEKS = "intervalWeeks"
        private const val KEY_ANCHOR_EPOCH_DAY = "anchorEpochDay"
        private const val KEY_SNOOZE_MINUTES = "snoozeMinutes"
        private const val KEY_SNOOZE_MAX_COUNT = "snoozeMaxCount"
        private const val KEY_SOUND_TYPE = "soundType"
        private const val KEY_CUSTOM_SOUND_URI = "customSoundUri"
        private const val KEY_VOLUME_PERCENT = "volumePercent"
        private const val KEY_VIBRATION_ENABLED = "vibrationEnabled"
        private const val KEY_SKIP_DATE_EPOCH_DAYS = "skipDateEpochDays"
        private const val KEY_ADD_DATE_EPOCH_DAYS = "addDateEpochDays"

        fun fromRule(rule: AlarmRule): DirectBootAlarmSnapshot {
            val entity = rule.toEntity()
            return DirectBootAlarmSnapshot(
                id = entity.id,
                label = entity.label,
                hour = entity.hour,
                minute = entity.minute,
                weeklyPatternCsv = entity.weeklyPatternCsv,
                intervalWeeks = entity.intervalWeeks,
                anchorEpochDay = entity.anchorEpochDay,
                snoozeMinutes = entity.snoozeMinutes,
                snoozeMaxCount = entity.snoozeMaxCount,
                soundType = entity.soundType,
                customSoundUri = entity.customSoundUri,
                volumePercent = entity.volumePercent,
                vibrationEnabled = entity.vibrationEnabled,
                skipDateEpochDays = entity.skipDateEpochDays,
                addDateEpochDays = entity.addDateEpochDays
            )
        }

        fun fromJson(obj: JSONObject): DirectBootAlarmSnapshot? {
            val id = obj.optLong(KEY_ID, 0L)
            if (id <= 0L) return null
            return DirectBootAlarmSnapshot(
                id = id,
                label = obj.optString(KEY_LABEL, ""),
                hour = obj.optInt(KEY_HOUR, -1),
                minute = obj.optInt(KEY_MINUTE, -1),
                weeklyPatternCsv = obj.optString(KEY_WEEKLY_PATTERN_CSV, ""),
                intervalWeeks = obj.optInt(KEY_INTERVAL_WEEKS, 1),
                anchorEpochDay = obj.optLong(KEY_ANCHOR_EPOCH_DAY, 0L),
                snoozeMinutes = obj.optInt(KEY_SNOOZE_MINUTES, 5),
                snoozeMaxCount = obj.optInt(KEY_SNOOZE_MAX_COUNT, 0),
                soundType = obj.optString(KEY_SOUND_TYPE, "ALARM"),
                customSoundUri = obj.optString(KEY_CUSTOM_SOUND_URI).ifBlank { null },
                volumePercent = obj.optInt(KEY_VOLUME_PERCENT, 100),
                vibrationEnabled = obj.optBoolean(KEY_VIBRATION_ENABLED, true),
                skipDateEpochDays = obj.optString(KEY_SKIP_DATE_EPOCH_DAYS, ""),
                addDateEpochDays = obj.optString(KEY_ADD_DATE_EPOCH_DAYS, "")
            ).takeIf { it.hour in 0..23 && it.minute in 0..59 }
        }
    }
}
