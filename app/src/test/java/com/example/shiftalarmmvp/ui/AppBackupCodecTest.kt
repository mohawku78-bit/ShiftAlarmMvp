package com.example.shiftalarmmvp.ui

import com.example.shiftalarmmvp.data.AlarmDateOverrides
import com.example.shiftalarmmvp.data.AlarmRule
import com.example.shiftalarmmvp.data.AlarmSoundType
import com.example.shiftalarmmvp.data.withDateOverrides
import com.example.shiftalarmmvp.scheduler.AlarmTimeCalculator
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppBackupCodecTest {

    private val messages = AppBackupParseMessages(
        invalidFormat = "Invalid backup format.",
        unsupportedVersion = "Unsupported backup version.",
        unreadable = "Backup file could not be read."
    )

    @Test
    fun `export and parse keep long interval alarms presets and logs`() {
        val intervalWeeks = 13
        val longPattern = List(intervalWeeks) { index ->
            when (index % 3) {
                0 -> setOf(DayOfWeek.MONDAY)
                1 -> setOf(DayOfWeek.WEDNESDAY)
                else -> setOf(DayOfWeek.SATURDAY)
            }
        }
        val alarms = listOf(
            AlarmRule(
                id = 41,
                label = "Long interval alarm",
                hour = 6,
                minute = 30,
                weeklyPattern = longPattern,
                intervalWeeks = intervalWeeks,
                anchorDate = LocalDate.of(2026, 3, 9),
                snoozeMinutes = 10,
                snoozeMaxCount = 2,
                soundType = AlarmSoundType.CUSTOM,
                customSoundUri = "content://alarm/custom",
                volumePercent = 80,
                vibrationEnabled = false,
                skipDateEpochDays = setOf(LocalDate.of(2026, 3, 11)),
                addDateEpochDays = setOf(LocalDate.of(2026, 3, 12)),
                enabled = true
            )
        )
        val presets = listOf(
            RotationPreset(
                name = "Long rotation",
                intervalWeeks = intervalWeeks,
                anchorDate = LocalDate.of(2026, 3, 9),
                weekPatterns = longPattern,
                infiniteRotationEnabled = false,
                isDefault = true
            )
        )
        val logs = listOf(
            AlarmLogEntry(
                timestampMillis = 123456789L,
                alarmId = 41,
                label = "Long interval alarm",
                type = AlarmLogType.MANUAL_RECOVERY_ACTION,
                detail = "Rescheduled next alarm"
            )
        )

        val parsed = AppBackupCodec.parseJson(
            AppBackupCodec.exportJson(
                alarms = alarms,
                presets = presets,
                alarmLogs = logs,
                exportedAtEpochMillis = 987654321L
            ),
            messages
        )

        assertEquals(AppBackupCodec.CURRENT_SCHEMA_VERSION, parsed.schemaVersion)
        assertEquals(987654321L, parsed.exportedAtEpochMillis)
        assertEquals(alarms, parsed.alarms)
        assertEquals(presets.map { it.normalized() }, parsed.presets)
        assertEquals(logs, parsed.alarmLogs)
    }

    @Test
    fun `backup normalizes conflicting overrides without changing next trigger`() {
        val conflictDate = LocalDate.of(2026, 3, 12)
        val addOnlyDate = LocalDate.of(2026, 3, 13)
        val source = AlarmRule(
            id = 51,
            label = "Conflicting override",
            hour = 7,
            minute = 0,
            weeklyPattern = listOf(setOf(DayOfWeek.THURSDAY)),
            intervalWeeks = 1,
            anchorDate = LocalDate.of(2026, 3, 9),
            snoozeMinutes = 5,
            snoozeMaxCount = 0,
            soundType = AlarmSoundType.ALARM,
            customSoundUri = null,
            volumePercent = 100,
            vibrationEnabled = true,
            skipDateEpochDays = setOf(conflictDate),
            addDateEpochDays = setOf(conflictDate, addOnlyDate),
            enabled = true
        )
        val expected = source.withDateOverrides(
            AlarmDateOverrides.of(
                skipDates = source.skipDateEpochDays,
                addDates = source.addDateEpochDays
            )
        )
        val parsed = AppBackupCodec.parseJson(
            AppBackupCodec.exportJson(
                alarms = listOf(source),
                presets = emptyList(),
                alarmLogs = emptyList()
            ),
            messages
        ).alarms.single()
        val now = LocalDateTime.of(2026, 3, 11, 0, 0)

        assertEquals(expected.skipDateEpochDays, parsed.skipDateEpochDays)
        assertEquals(expected.addDateEpochDays, parsed.addDateEpochDays)
        assertEquals(
            AlarmTimeCalculator.nextTrigger(expected, now),
            AlarmTimeCalculator.nextTrigger(parsed, now)
        )
    }

    @Test
    fun `parse rejects preset only export`() {
        val rawPresetJson = "[{\"name\":\"legacy preset\"}]"

        val error = runCatching { AppBackupCodec.parseJson(rawPresetJson, messages) }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
    }
}