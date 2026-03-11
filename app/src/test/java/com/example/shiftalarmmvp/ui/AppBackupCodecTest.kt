package com.example.shiftalarmmvp.ui

import com.example.shiftalarmmvp.data.AlarmRule
import com.example.shiftalarmmvp.data.AlarmSoundType
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppBackupCodecTest {

    private val messages = AppBackupParseMessages(
        invalidFormat = "앱 백업 파일 형식이 아닙니다.",
        unsupportedVersion = "지원하지 않는 백업 버전입니다.",
        unreadable = "앱 백업 파일을 읽을 수 없습니다."
    )

    @Test
    fun `export and parse keep alarms presets and logs`() {
        val alarms = listOf(
            AlarmRule(
                id = 41,
                label = "주간 알람",
                hour = 6,
                minute = 30,
                weeklyPattern = listOf(
                    setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
                    setOf(DayOfWeek.FRIDAY)
                ),
                intervalWeeks = 2,
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
                name = "3조2교대",
                intervalWeeks = 2,
                anchorDate = LocalDate.of(2026, 3, 9),
                weekPatterns = listOf(
                    setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
                    setOf(DayOfWeek.TUESDAY)
                ),
                infiniteRotationEnabled = false,
                isDefault = true
            )
        )
        val logs = listOf(
            AlarmLogEntry(
                timestampMillis = 123456789L,
                alarmId = 41,
                label = "주간 알람",
                type = AlarmLogType.MANUAL_RECOVERY_ACTION,
                detail = "다음 알람 재등록"
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
        assertEquals(presets, parsed.presets)
        assertEquals(logs, parsed.alarmLogs)
    }

    @Test
    fun `parse rejects preset only export`() {
        val rawPresetJson = "[{\"name\":\"기존 프리셋\"}]"

        val error = runCatching { AppBackupCodec.parseJson(rawPresetJson, messages) }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
    }
}