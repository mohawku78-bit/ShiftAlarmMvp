package com.example.shiftalarmmvp.recovery

import com.example.shiftalarmmvp.data.AlarmSoundType
import com.example.shiftalarmmvp.data.AlarmRule
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DirectBootAlarmSnapshotTest {
    @Test
    fun `snapshot round trips alarm rule fields needed before unlock`() {
        val rule = AlarmRule(
            id = 42L,
            label = "당직 1차",
            hour = 8,
            minute = 30,
            weeklyPattern = listOf(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)),
            intervalWeeks = 1,
            anchorDate = LocalDate.of(2026, 5, 1),
            snoozeMinutes = 7,
            snoozeMaxCount = 2,
            soundType = AlarmSoundType.ALARM,
            customSoundUri = null,
            volumePercent = 90,
            vibrationEnabled = true,
            skipDateEpochDays = setOf(LocalDate.of(2026, 5, 3)),
            addDateEpochDays = setOf(LocalDate.of(2026, 5, 4)),
            enabled = true
        )

        val restored = DirectBootAlarmSnapshot.fromRule(rule).toAlarmRule()

        assertNotNull(restored)
        assertEquals(rule.id, restored.id)
        assertEquals(rule.label, restored.label)
        assertEquals(rule.hour, restored.hour)
        assertEquals(rule.minute, restored.minute)
        assertEquals(rule.weeklyPattern, restored.weeklyPattern)
        assertEquals(rule.anchorDate, restored.anchorDate)
        assertEquals(rule.snoozeMinutes, restored.snoozeMinutes)
        assertEquals(rule.snoozeMaxCount, restored.snoozeMaxCount)
        assertEquals(rule.volumePercent, restored.volumePercent)
        assertEquals(rule.skipDateEpochDays, restored.skipDateEpochDays)
        assertEquals(rule.addDateEpochDays, restored.addDateEpochDays)
    }
}
