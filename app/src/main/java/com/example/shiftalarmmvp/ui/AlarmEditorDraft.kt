package com.example.shiftalarmmvp.ui

import com.example.shiftalarmmvp.data.AlarmRule
import com.example.shiftalarmmvp.data.AlarmSoundType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

data class AlarmEditorDraft(
    val editingAlarmId: Long?,
    val editingEnabled: Boolean,
    val selectedTime: LocalTime,
    val selectedLabel: String,
    val intervalWeeks: Int,
    val anchorDate: LocalDate,
    val weekPatterns: List<Set<DayOfWeek>>,
    val selectedSoundType: AlarmSoundType,
    val selectedCustomSoundUri: String?,
    val selectedVolume: Float,
    val selectedSnoozeMinutes: Int,
    val selectedSnoozeMaxCount: Int,
    val vibrationEnabled: Boolean,
    val skipDates: Set<LocalDate>,
    val addDates: Set<LocalDate>,
    val exceptionDate: LocalDate
)

fun AlarmRule.normalizedWeeklyPattern(): List<Set<DayOfWeek>> {
    val interval = intervalWeeks.coerceIn(1, 6)
    return (0 until interval).map { index ->
        weeklyPattern.getOrNull(index).orEmpty()
    }
}

fun AlarmRule.duplicateLabel(): String {
    return if (label.isBlank()) {
        "알람 복사본"
    } else {
        "${label} 복사본".take(24)
    }
}

fun AlarmRule.toEditorDraft(
    editingAlarmId: Long?,
    editingEnabled: Boolean,
    selectedLabel: String = label,
    weekPatterns: List<Set<DayOfWeek>> = normalizedWeeklyPattern(),
    exceptionDate: LocalDate = LocalDate.now()
): AlarmEditorDraft {
    return AlarmEditorDraft(
        editingAlarmId = editingAlarmId,
        editingEnabled = editingEnabled,
        selectedTime = LocalTime.of(hour, minute),
        selectedLabel = selectedLabel,
        intervalWeeks = intervalWeeks.coerceIn(2, 6),
        anchorDate = anchorDate,
        weekPatterns = weekPatterns,
        selectedSoundType = soundType,
        selectedCustomSoundUri = customSoundUri,
        selectedVolume = volumePercent.toFloat(),
        selectedSnoozeMinutes = snoozeMinutes,
        selectedSnoozeMaxCount = snoozeMaxCount,
        vibrationEnabled = vibrationEnabled,
        skipDates = skipDateEpochDays,
        addDates = addDateEpochDays,
        exceptionDate = exceptionDate
    )
}
