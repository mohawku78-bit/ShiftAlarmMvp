package com.example.shiftalarmmvp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalDate

enum class AlarmSoundType {
    ALARM,
    NOTIFICATION,
    CUSTOM
}

@Entity(tableName = "alarms")
data class AlarmRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String = "",
    val hour: Int,
    val minute: Int,
    val weeklyPatternCsv: String,
    val intervalWeeks: Int,
    val anchorEpochDay: Long,
    val snoozeMinutes: Int = 5,
    val snoozeMaxCount: Int = 0,
    val soundType: String = AlarmSoundType.ALARM.name,
    val customSoundUri: String? = null,
    val volumePercent: Int = 100,
    val vibrationEnabled: Boolean = true,
    val skipDateEpochDays: String = "",
    val addDateEpochDays: String = "",
    val enabled: Boolean = true
)

data class AlarmRule(
    val id: Long,
    val label: String = "",
    val hour: Int,
    val minute: Int,
    val weeklyPattern: List<Set<DayOfWeek>>,
    val intervalWeeks: Int,
    val anchorDate: LocalDate,
    val snoozeMinutes: Int = 5,
    val snoozeMaxCount: Int = 0,
    val soundType: AlarmSoundType,
    val customSoundUri: String?,
    val volumePercent: Int,
    val vibrationEnabled: Boolean,
    val skipDateEpochDays: Set<LocalDate> = emptySet(),
    val addDateEpochDays: Set<LocalDate> = emptySet(),
    val enabled: Boolean
)

private fun parseWeekdays(csv: String): Set<DayOfWeek> {
    return csv.split(",")
        .filter { it.isNotBlank() }
        .map { DayOfWeek.valueOf(it) }
        .toSet()
}

private fun serializeWeekdays(days: Set<DayOfWeek>): String {
    return days.sortedBy { it.value }.joinToString(",") { it.name }
}

private fun parseDateSet(csv: String): Set<LocalDate> {
    return csv.split(",")
        .filter { it.isNotBlank() }
        .mapNotNull { it.toLongOrNull() }
        .map { LocalDate.ofEpochDay(it) }
        .toSet()
}

private fun serializeDateSet(dates: Set<LocalDate>): String {
    return dates
        .map { it.toEpochDay() }
        .sorted()
        .joinToString(",")
}

fun normalizeIntervalWeeks(intervalWeeks: Int): Int {
    return intervalWeeks.coerceAtLeast(1)
}

fun normalizeWeekPatterns(
    intervalWeeks: Int,
    weekPatterns: List<Set<DayOfWeek>>
): List<Set<DayOfWeek>> {
    val interval = normalizeIntervalWeeks(intervalWeeks)
    return List(interval) { index -> weekPatterns.getOrNull(index).orEmpty() }
}

fun AlarmRuleEntity.toDomain(): AlarmRule {
    val interval = normalizeIntervalWeeks(intervalWeeks)
    val slots = weeklyPatternCsv.split("|")
    val pattern = normalizeWeekPatterns(interval, slots.map(::parseWeekdays))
    val overrides = AlarmDateOverrides.of(
        skipDates = parseDateSet(skipDateEpochDays),
        addDates = parseDateSet(addDateEpochDays)
    )

    val safeSoundType = runCatching { AlarmSoundType.valueOf(soundType) }.getOrDefault(AlarmSoundType.ALARM)

    return AlarmRule(
        id = id,
        label = label,
        hour = hour,
        minute = minute,
        weeklyPattern = pattern,
        intervalWeeks = interval,
        anchorDate = LocalDate.ofEpochDay(anchorEpochDay),
        snoozeMinutes = snoozeMinutes.coerceIn(1, 60),
        snoozeMaxCount = snoozeMaxCount.coerceIn(0, 99),
        soundType = safeSoundType,
        customSoundUri = customSoundUri,
        volumePercent = volumePercent.coerceIn(0, 100),
        vibrationEnabled = vibrationEnabled,
        skipDateEpochDays = overrides.skipDates,
        addDateEpochDays = overrides.addDates,
        enabled = enabled
    )
}

fun AlarmRule.toEntity(): AlarmRuleEntity {
    val interval = normalizeIntervalWeeks(intervalWeeks)
    val normalized = normalizeWeekPatterns(interval, weeklyPattern)
    val overrides = normalizedDateOverrides()

    return AlarmRuleEntity(
        id = id,
        label = label.trim(),
        hour = hour,
        minute = minute,
        weeklyPatternCsv = normalized.joinToString("|") { serializeWeekdays(it) },
        intervalWeeks = interval,
        anchorEpochDay = anchorDate.toEpochDay(),
        snoozeMinutes = snoozeMinutes.coerceIn(1, 60),
        snoozeMaxCount = snoozeMaxCount.coerceIn(0, 99),
        soundType = soundType.name,
        customSoundUri = customSoundUri,
        volumePercent = volumePercent.coerceIn(0, 100),
        vibrationEnabled = vibrationEnabled,
        skipDateEpochDays = serializeDateSet(overrides.skipDates),
        addDateEpochDays = serializeDateSet(overrides.addDates),
        enabled = enabled
    )
}

