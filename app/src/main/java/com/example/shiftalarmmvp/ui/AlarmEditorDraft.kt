package com.example.shiftalarmmvp.ui

import android.content.res.Resources
import com.example.shiftalarmmvp.R
import com.example.shiftalarmmvp.data.AlarmRule
import com.example.shiftalarmmvp.data.AlarmSoundType
import com.example.shiftalarmmvp.data.normalizeIntervalWeeks
import com.example.shiftalarmmvp.data.normalizeWeekPatterns
import com.example.shiftalarmmvp.data.normalizedDateOverrides
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

data class NormalizedEditorPatternState(
    val intervalWeeks: Int,
    val weekPatterns: List<Set<DayOfWeek>>,
    val activeWeekIndex: Int
)

private val DEFAULT_EDITOR_WEEK_PATTERN_SEED: List<Set<DayOfWeek>> = listOf(
    setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY, DayOfWeek.SUNDAY),
    setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY)
)

fun defaultEditorWeekPatterns(intervalWeeks: Int): List<Set<DayOfWeek>> {
    return normalizeWeekPatterns(normalizeIntervalWeeks(intervalWeeks), DEFAULT_EDITOR_WEEK_PATTERN_SEED)
}

fun normalizeEditorPatternState(
    targetInterval: Int,
    incomingPatterns: List<Set<DayOfWeek>>,
    activeWeekIndex: Int
): NormalizedEditorPatternState {
    val normalizedInterval = normalizeIntervalWeeks(targetInterval)
    val normalizedPatterns = if (incomingPatterns.isEmpty()) {
        defaultEditorWeekPatterns(normalizedInterval)
    } else {
        normalizeWeekPatterns(normalizedInterval, incomingPatterns)
    }
    return NormalizedEditorPatternState(
        intervalWeeks = normalizedInterval,
        weekPatterns = normalizedPatterns,
        activeWeekIndex = activeWeekIndex.coerceIn(0, normalizedPatterns.lastIndex)
    )
}

fun AlarmRule.normalizedWeeklyPattern(): List<Set<DayOfWeek>> {
    return normalizeWeekPatterns(intervalWeeks, weeklyPattern)
}

fun AlarmRule.duplicateLabel(resources: Resources): String {
    return if (label.isBlank()) {
        resources.getString(R.string.editor_duplicate_label_empty)
    } else {
        resources.getString(R.string.editor_duplicate_label_format, label).take(24)
    }
}

fun AlarmRule.toEditorDraft(
    editingAlarmId: Long?,
    editingEnabled: Boolean,
    selectedLabel: String = label,
    weekPatterns: List<Set<DayOfWeek>> = normalizedWeeklyPattern(),
    exceptionDate: LocalDate = LocalDate.now()
): AlarmEditorDraft {
    val normalizedPatternState = normalizeEditorPatternState(intervalWeeks, weekPatterns, activeWeekIndex = 0)
    val overrides = normalizedDateOverrides()
    return AlarmEditorDraft(
        editingAlarmId = editingAlarmId,
        editingEnabled = editingEnabled,
        selectedTime = LocalTime.of(hour, minute),
        selectedLabel = selectedLabel,
        intervalWeeks = normalizedPatternState.intervalWeeks,
        anchorDate = anchorDate,
        weekPatterns = normalizedPatternState.weekPatterns,
        selectedSoundType = soundType,
        selectedCustomSoundUri = customSoundUri,
        selectedVolume = volumePercent.toFloat(),
        selectedSnoozeMinutes = snoozeMinutes,
        selectedSnoozeMaxCount = snoozeMaxCount,
        vibrationEnabled = vibrationEnabled,
        skipDates = overrides.skipDates,
        addDates = overrides.addDates,
        exceptionDate = exceptionDate
    )
}
