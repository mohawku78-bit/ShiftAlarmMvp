package com.example.shiftalarmmvp.ui

import android.content.res.Resources
import com.example.shiftalarmmvp.R
import com.example.shiftalarmmvp.data.AlarmDateOverrides
import com.example.shiftalarmmvp.data.AlarmRule
import com.example.shiftalarmmvp.data.AlarmSoundType
import com.example.shiftalarmmvp.data.normalizeIntervalWeeks
import com.example.shiftalarmmvp.data.normalizeWeekPatterns
import com.google.gson.Gson
import java.time.DayOfWeek
import java.time.LocalDate

data class AppBackupSnapshot(
    val schemaVersion: Int,
    val exportedAtEpochMillis: Long,
    val alarms: List<AlarmRule>,
    val presets: List<RotationPreset>,
    val alarmLogs: List<AlarmLogEntry>
)

data class AppBackupParseMessages(
    val invalidFormat: String,
    val unsupportedVersion: String,
    val unreadable: String
)

fun appBackupParseMessages(resources: Resources): AppBackupParseMessages {
    return AppBackupParseMessages(
        invalidFormat = resources.getString(R.string.backup_parse_invalid_format),
        unsupportedVersion = resources.getString(R.string.backup_parse_unsupported_version),
        unreadable = resources.getString(R.string.backup_parse_unreadable)
    )
}

object AppBackupCodec {
    const val CURRENT_SCHEMA_VERSION = 1

    private val gson = Gson()

    fun exportJson(
        alarms: List<AlarmRule>,
        presets: List<RotationPreset>,
        alarmLogs: List<AlarmLogEntry>,
        exportedAtEpochMillis: Long = System.currentTimeMillis()
    ): String {
        val payload = BackupPayloadDto(
            schemaVersion = CURRENT_SCHEMA_VERSION,
            exportedAtEpochMillis = exportedAtEpochMillis,
            alarms = alarms.map(::toAlarmDto),
            presets = presets.map(::toPresetDto),
            alarmLogs = alarmLogs.map(::toAlarmLogDto)
        )
        return gson.toJson(payload)
    }

    fun parseJson(raw: String, messages: AppBackupParseMessages): AppBackupSnapshot {
        return runCatching {
            val payload = gson.fromJson(raw, BackupPayloadDto::class.java)
                ?: throw IllegalArgumentException(messages.invalidFormat)
            val schemaVersion = payload.schemaVersion
            require(schemaVersion in 1..CURRENT_SCHEMA_VERSION) { messages.unsupportedVersion }

            AppBackupSnapshot(
                schemaVersion = schemaVersion,
                exportedAtEpochMillis = payload.exportedAtEpochMillis,
                alarms = payload.alarms.map(::toAlarmRule),
                presets = payload.presets.mapNotNull(::toRotationPreset),
                alarmLogs = payload.alarmLogs.mapNotNull(::toAlarmLogEntry)
            )
        }.getOrElse { error ->
            if (error is IllegalArgumentException) throw error
            throw IllegalArgumentException(error.message ?: messages.unreadable, error)
        }
    }

    private fun toAlarmDto(alarm: AlarmRule): AlarmBackupDto {
        val interval = normalizeIntervalWeeks(alarm.intervalWeeks)
        val normalizedPattern = normalizeWeekPatterns(interval, alarm.weeklyPattern)
        val overrides = AlarmDateOverrides.of(
            skipDates = alarm.skipDateEpochDays,
            addDates = alarm.addDateEpochDays
        )
        return AlarmBackupDto(
            id = alarm.id,
            label = alarm.label,
            hour = alarm.hour,
            minute = alarm.minute,
            intervalWeeks = interval,
            anchorEpochDay = alarm.anchorDate.toEpochDay(),
            snoozeMinutes = alarm.snoozeMinutes,
            snoozeMaxCount = alarm.snoozeMaxCount,
            soundType = alarm.soundType.name,
            customSoundUri = alarm.customSoundUri,
            volumePercent = alarm.volumePercent,
            vibrationEnabled = alarm.vibrationEnabled,
            enabled = alarm.enabled,
            patterns = normalizedPattern.map { days -> days.sortedBy { it.value }.map(DayOfWeek::name) },
            skipDateEpochDays = overrides.skipDates.map(LocalDate::toEpochDay).sorted(),
            addDateEpochDays = overrides.addDates.map(LocalDate::toEpochDay).sorted()
        )
    }

    private fun toPresetDto(preset: RotationPreset): PresetBackupDto {
        val interval = normalizeIntervalWeeks(preset.intervalWeeks)
        val normalizedPattern = normalizeWeekPatterns(interval, preset.weekPatterns)
        return PresetBackupDto(
            name = preset.name,
            intervalWeeks = interval,
            anchorEpochDay = preset.anchorDate.toEpochDay(),
            patterns = normalizedPattern.map { days -> days.sortedBy { it.value }.map(DayOfWeek::name) },
            infiniteRotationEnabled = preset.infiniteRotationEnabled,
            isDefault = preset.isDefault
        )
    }

    private fun toAlarmLogDto(entry: AlarmLogEntry): AlarmLogBackupDto {
        return AlarmLogBackupDto(
            timestampMillis = entry.timestampMillis,
            alarmId = entry.alarmId,
            label = entry.label,
            type = entry.type.name,
            detail = entry.detail
        )
    }

    private fun toAlarmRule(dto: AlarmBackupDto): AlarmRule {
        val interval = normalizeIntervalWeeks(dto.intervalWeeks)
        val normalizedPattern = normalizeWeekPatterns(interval, parsePatterns(dto.patterns, interval))
        val overrides = AlarmDateOverrides.of(
            skipDates = dto.skipDateEpochDays.map(LocalDate::ofEpochDay).toSet(),
            addDates = dto.addDateEpochDays.map(LocalDate::ofEpochDay).toSet()
        )
        val soundType = runCatching { AlarmSoundType.valueOf(dto.soundType) }.getOrDefault(AlarmSoundType.ALARM)
        return AlarmRule(
            id = dto.id,
            label = dto.label.trim(),
            hour = dto.hour.coerceIn(0, 23),
            minute = dto.minute.coerceIn(0, 59),
            weeklyPattern = normalizedPattern,
            intervalWeeks = interval,
            anchorDate = LocalDate.ofEpochDay(dto.anchorEpochDay),
            snoozeMinutes = dto.snoozeMinutes.coerceIn(1, 60),
            snoozeMaxCount = dto.snoozeMaxCount.coerceIn(0, 99),
            soundType = soundType,
            customSoundUri = dto.customSoundUri?.takeIf { it.isNotBlank() },
            volumePercent = dto.volumePercent.coerceIn(0, 100),
            vibrationEnabled = dto.vibrationEnabled,
            skipDateEpochDays = overrides.skipDates,
            addDateEpochDays = overrides.addDates,
            enabled = dto.enabled
        )
    }

    private fun toRotationPreset(dto: PresetBackupDto): RotationPreset? {
        val name = dto.name.trim()
        if (name.isBlank()) return null
        val interval = normalizeIntervalWeeks(dto.intervalWeeks)
        return RotationPreset(
            name = name,
            intervalWeeks = interval,
            anchorDate = LocalDate.ofEpochDay(dto.anchorEpochDay),
            weekPatterns = normalizeWeekPatterns(interval, parsePatterns(dto.patterns, interval)),
            infiniteRotationEnabled = dto.infiniteRotationEnabled,
            isDefault = dto.isDefault
        )
    }

    private fun toAlarmLogEntry(dto: AlarmLogBackupDto): AlarmLogEntry? {
        val type = runCatching { AlarmLogType.valueOf(dto.type) }.getOrNull() ?: return null
        return AlarmLogEntry(
            timestampMillis = dto.timestampMillis,
            alarmId = dto.alarmId,
            label = dto.label,
            type = type,
            detail = dto.detail
        )
    }

    private fun parsePatterns(patterns: List<List<String>>, interval: Int): List<Set<DayOfWeek>> {
        return List(interval) { index ->
            patterns.getOrNull(index)
                ?.mapNotNull { day -> runCatching { DayOfWeek.valueOf(day) }.getOrNull() }
                ?.toSet()
                .orEmpty()
        }
    }

    private data class BackupPayloadDto(
        var schemaVersion: Int = 0,
        var exportedAtEpochMillis: Long = 0L,
        var alarms: List<AlarmBackupDto> = emptyList(),
        var presets: List<PresetBackupDto> = emptyList(),
        var alarmLogs: List<AlarmLogBackupDto> = emptyList()
    )

    private data class AlarmBackupDto(
        var id: Long = 0L,
        var label: String = "",
        var hour: Int = 7,
        var minute: Int = 0,
        var intervalWeeks: Int = 1,
        var anchorEpochDay: Long = LocalDate.now().toEpochDay(),
        var snoozeMinutes: Int = 5,
        var snoozeMaxCount: Int = 0,
        var soundType: String = AlarmSoundType.ALARM.name,
        var customSoundUri: String? = null,
        var volumePercent: Int = 100,
        var vibrationEnabled: Boolean = true,
        var enabled: Boolean = true,
        var patterns: List<List<String>> = emptyList(),
        var skipDateEpochDays: List<Long> = emptyList(),
        var addDateEpochDays: List<Long> = emptyList()
    )

    private data class PresetBackupDto(
        var name: String = "",
        var intervalWeeks: Int = 1,
        var anchorEpochDay: Long = LocalDate.now().toEpochDay(),
        var patterns: List<List<String>> = emptyList(),
        var infiniteRotationEnabled: Boolean = true,
        var isDefault: Boolean = false
    )

    private data class AlarmLogBackupDto(
        var timestampMillis: Long = 0L,
        var alarmId: Long = -1L,
        var label: String = "",
        var type: String = "",
        var detail: String = ""
    )
}