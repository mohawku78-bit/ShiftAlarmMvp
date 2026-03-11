package com.example.shiftalarmmvp.data

import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmRuleEntityTest {

    @Test
    fun `entity round trip keeps long interval week patterns`() {
        val intervalWeeks = 13
        val pattern = List(intervalWeeks) { index ->
            when (index % 3) {
                0 -> setOf(DayOfWeek.MONDAY)
                1 -> setOf(DayOfWeek.WEDNESDAY)
                else -> setOf(DayOfWeek.SATURDAY)
            }
        }
        val rule = AlarmRule(
            id = 7,
            label = "장기 패턴",
            hour = 7,
            minute = 30,
            weeklyPattern = pattern,
            intervalWeeks = intervalWeeks,
            anchorDate = LocalDate.of(2026, 3, 10),
            snoozeMinutes = 5,
            snoozeMaxCount = 1,
            soundType = AlarmSoundType.ALARM,
            customSoundUri = null,
            volumePercent = 100,
            vibrationEnabled = true,
            skipDateEpochDays = emptySet(),
            addDateEpochDays = emptySet(),
            enabled = true
        )

        val restored = rule.toEntity().toDomain()

        assertEquals(intervalWeeks, restored.intervalWeeks)
        assertEquals(pattern, restored.weeklyPattern)
    }

    @Test
    fun `entity round trip normalizes conflicting overrides`() {
        val conflictingDate = LocalDate.of(2026, 3, 12)
        val addOnlyDate = LocalDate.of(2026, 3, 13)
        val rule = AlarmRule(
            id = 8,
            label = "예외 충돌",
            hour = 7,
            minute = 30,
            weeklyPattern = listOf(setOf(DayOfWeek.MONDAY)),
            intervalWeeks = 1,
            anchorDate = LocalDate.of(2026, 3, 10),
            snoozeMinutes = 5,
            snoozeMaxCount = 1,
            soundType = AlarmSoundType.ALARM,
            customSoundUri = null,
            volumePercent = 100,
            vibrationEnabled = true,
            skipDateEpochDays = setOf(conflictingDate),
            addDateEpochDays = setOf(conflictingDate, addOnlyDate),
            enabled = true
        )

        val restored = rule.toEntity().toDomain()

        assertEquals(setOf(conflictingDate), restored.skipDateEpochDays)
        assertEquals(setOf(addOnlyDate), restored.addDateEpochDays)
    }
}
