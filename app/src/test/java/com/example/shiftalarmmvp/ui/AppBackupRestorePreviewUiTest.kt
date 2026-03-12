package com.example.shiftalarmmvp.ui

import com.example.shiftalarmmvp.data.AlarmRule
import com.example.shiftalarmmvp.data.AlarmSoundType
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class AppBackupRestorePreviewUiTest {

    private val strings = AppBackupRestorePreviewStrings(
        exportedAtUnknown = "기록 없음",
        replaceWarningText = "복구를 진행하면 현재 데이터가 교체됩니다."
    )

    @Test
    fun `preview builder keeps schema exportedAt and counts`() {
        val snapshot = AppBackupSnapshot(
            schemaVersion = AppBackupCodec.CURRENT_SCHEMA_VERSION,
            exportedAtEpochMillis = Instant.parse("2026-03-12T03:04:00Z").toEpochMilli(),
            alarms = listOf(sampleAlarm(1L)),
            presets = listOf(samplePreset("A"), samplePreset("B")),
            alarmLogs = listOf(sampleLog(1L), sampleLog(2L), sampleLog(3L))
        )
        val currentCounts = AppBackupCurrentCounts(alarmCount = 5, presetCount = 4, alarmLogCount = 6)

        val preview = buildAppBackupRestorePreviewUi(
            snapshot = snapshot,
            currentCounts = currentCounts,
            strings = strings,
            zoneId = ZoneOffset.UTC
        )

        assertEquals(AppBackupCodec.CURRENT_SCHEMA_VERSION, preview.schemaVersion)
        assertEquals("2026-03-12 03:04", preview.exportedAtText)
        assertEquals(AppBackupCurrentCounts(1, 2, 3), preview.incomingCounts)
        assertEquals(currentCounts, preview.currentCounts)
        assertEquals(strings.replaceWarningText, preview.replaceWarningText)
    }

    @Test
    fun `preview builder uses fallback for non positive exportedAt`() {
        val snapshot = AppBackupSnapshot(
            schemaVersion = AppBackupCodec.CURRENT_SCHEMA_VERSION,
            exportedAtEpochMillis = 0L,
            alarms = emptyList(),
            presets = emptyList(),
            alarmLogs = emptyList()
        )

        val preview = buildAppBackupRestorePreviewUi(
            snapshot = snapshot,
            currentCounts = AppBackupCurrentCounts(0, 0, 0),
            strings = strings,
            zoneId = ZoneOffset.UTC
        )

        assertEquals(strings.exportedAtUnknown, preview.exportedAtText)
    }

    @Test
    fun `preview builder keeps warning when incoming counts are smaller than current`() {
        val snapshot = AppBackupSnapshot(
            schemaVersion = AppBackupCodec.CURRENT_SCHEMA_VERSION,
            exportedAtEpochMillis = 1L,
            alarms = emptyList(),
            presets = emptyList(),
            alarmLogs = emptyList()
        )

        val preview = buildAppBackupRestorePreviewUi(
            snapshot = snapshot,
            currentCounts = AppBackupCurrentCounts(alarmCount = 9, presetCount = 7, alarmLogCount = 5),
            strings = strings,
            zoneId = ZoneOffset.UTC
        )

        assertEquals(strings.replaceWarningText, preview.replaceWarningText)
        assertEquals(AppBackupCurrentCounts(0, 0, 0), preview.incomingCounts)
        assertEquals(AppBackupCurrentCounts(9, 7, 5), preview.currentCounts)
    }

    @Test
    fun `preview builder keeps incoming and current counts independent`() {
        val snapshot = AppBackupSnapshot(
            schemaVersion = AppBackupCodec.CURRENT_SCHEMA_VERSION,
            exportedAtEpochMillis = Instant.parse("2026-03-12T00:00:00Z").toEpochMilli(),
            alarms = listOf(sampleAlarm(10L), sampleAlarm(11L)),
            presets = listOf(samplePreset("Long rotation")),
            alarmLogs = listOf(sampleLog(7L))
        )
        val currentCounts = AppBackupCurrentCounts(alarmCount = 3, presetCount = 8, alarmLogCount = 13)

        val preview = buildAppBackupRestorePreviewUi(
            snapshot = snapshot,
            currentCounts = currentCounts,
            strings = strings,
            zoneId = ZoneOffset.UTC
        )

        assertEquals(AppBackupCurrentCounts(2, 1, 1), preview.incomingCounts)
        assertEquals(currentCounts, preview.currentCounts)
    }

    private fun sampleAlarm(id: Long): AlarmRule {
        return AlarmRule(
            id = id,
            label = "Alarm $id",
            hour = 7,
            minute = 0,
            weeklyPattern = listOf(setOf(DayOfWeek.MONDAY)),
            intervalWeeks = 1,
            anchorDate = LocalDate.of(2026, 3, 9),
            snoozeMinutes = 5,
            snoozeMaxCount = 0,
            soundType = AlarmSoundType.ALARM,
            customSoundUri = null,
            volumePercent = 100,
            vibrationEnabled = true,
            skipDateEpochDays = emptySet(),
            addDateEpochDays = emptySet(),
            enabled = true
        )
    }

    private fun samplePreset(name: String): RotationPreset {
        return RotationPreset(
            name = name,
            intervalWeeks = 1,
            anchorDate = LocalDate.of(2026, 3, 9),
            weekPatterns = listOf(setOf(DayOfWeek.MONDAY)),
            infiniteRotationEnabled = true
        )
    }

    private fun sampleLog(alarmId: Long): AlarmLogEntry {
        return AlarmLogEntry(
            timestampMillis = alarmId,
            alarmId = alarmId,
            label = "Alarm $alarmId",
            type = AlarmLogType.MANUAL_RECOVERY_ACTION,
            detail = "Restored"
        )
    }
}
