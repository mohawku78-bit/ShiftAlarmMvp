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
}
