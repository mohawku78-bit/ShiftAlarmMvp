package com.example.shiftalarmmvp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.shiftalarmmvp.data.AlarmDatabase
import com.example.shiftalarmmvp.data.AlarmRule
import com.example.shiftalarmmvp.data.AlarmSoundType
import com.example.shiftalarmmvp.data.toDomain
import com.example.shiftalarmmvp.data.toEntity
import com.example.shiftalarmmvp.scheduler.AlarmScheduler
import com.example.shiftalarmmvp.scheduler.AlarmTimeCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

class AlarmViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AlarmDatabase.get(application).alarmDao()
    private val scheduler = AlarmScheduler(application)

    val alarms = dao.observeAll()
        .map { list -> list.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addAlarm(
        label: String,
        hour: Int,
        minute: Int,
        weeklyPattern: List<Set<DayOfWeek>>,
        intervalWeeks: Int,
        anchorDate: LocalDate,
        soundType: AlarmSoundType,
        customSoundUri: String?,
        volumePercent: Int,
        vibrationEnabled: Boolean,
        snoozeMinutes: Int,
        snoozeMaxCount: Int,
        skipDateEpochDays: Set<LocalDate>,
        addDateEpochDays: Set<LocalDate>
    ) {
        viewModelScope.launch {
            val interval = intervalWeeks.coerceIn(1, 6)
            val normalizedPattern = (0 until interval).map { index -> weeklyPattern.getOrNull(index).orEmpty() }
            val alarm = AlarmRule(
                id = 0,
                label = label.trim(),
                hour = hour,
                minute = minute,
                weeklyPattern = normalizedPattern,
                intervalWeeks = interval,
                anchorDate = anchorDate,
                snoozeMinutes = snoozeMinutes,
                snoozeMaxCount = snoozeMaxCount,
                soundType = soundType,
                customSoundUri = customSoundUri,
                volumePercent = volumePercent.coerceIn(0, 100),
                vibrationEnabled = vibrationEnabled,
                skipDateEpochDays = skipDateEpochDays,
                addDateEpochDays = addDateEpochDays,
                enabled = true
            )

            val id = dao.insert(alarm.toEntity())
            scheduler.schedule(alarm.copy(id = id))
        }
    }

    fun updateAlarm(
        id: Long,
        label: String,
        hour: Int,
        minute: Int,
        weeklyPattern: List<Set<DayOfWeek>>,
        intervalWeeks: Int,
        anchorDate: LocalDate,
        soundType: AlarmSoundType,
        customSoundUri: String?,
        volumePercent: Int,
        vibrationEnabled: Boolean,
        snoozeMinutes: Int,
        snoozeMaxCount: Int,
        enabled: Boolean,
        skipDateEpochDays: Set<LocalDate>,
        addDateEpochDays: Set<LocalDate>
    ) {
        viewModelScope.launch {
            val interval = intervalWeeks.coerceIn(1, 6)
            val normalizedPattern = (0 until interval).map { index -> weeklyPattern.getOrNull(index).orEmpty() }
            val updated = AlarmRule(
                id = id,
                label = label.trim(),
                hour = hour,
                minute = minute,
                weeklyPattern = normalizedPattern,
                intervalWeeks = interval,
                anchorDate = anchorDate,
                snoozeMinutes = snoozeMinutes,
                snoozeMaxCount = snoozeMaxCount,
                soundType = soundType,
                customSoundUri = customSoundUri,
                volumePercent = volumePercent.coerceIn(0, 100),
                vibrationEnabled = vibrationEnabled,
                skipDateEpochDays = skipDateEpochDays,
                addDateEpochDays = addDateEpochDays,
                enabled = enabled
            )
            dao.update(updated.toEntity())
            scheduler.cancel(id)
            if (enabled) scheduler.schedule(updated)
        }
    }

    fun toggleEnabled(alarm: AlarmRule) {
        viewModelScope.launch {
            val next = alarm.copy(enabled = !alarm.enabled)
            dao.update(next.toEntity())
            if (next.enabled) scheduler.schedule(next) else scheduler.cancel(next.id)
        }
    }

    fun delete(alarm: AlarmRule) {
        viewModelScope.launch {
            dao.delete(alarm.toEntity())
            scheduler.cancel(alarm.id)
        }
    }

    fun skipToday(alarm: AlarmRule) {
        viewModelScope.launch {
            val today = LocalDate.now()
            val updated = alarm.copy(
                skipDateEpochDays = alarm.skipDateEpochDays + today,
                addDateEpochDays = alarm.addDateEpochDays - today
            )
            dao.update(updated.toEntity())
            if (updated.enabled) {
                scheduler.cancel(updated.id)
                scheduler.schedule(updated)
            }
        }
    }

    fun unskipToday(alarm: AlarmRule) {
        viewModelScope.launch {
            val today = LocalDate.now()
            val updated = alarm.copy(skipDateEpochDays = alarm.skipDateEpochDays - today)
            dao.update(updated.toEntity())
            if (updated.enabled) {
                scheduler.cancel(updated.id)
                scheduler.schedule(updated)
            }
        }
    }

    fun addTomorrow(alarm: AlarmRule) {
        viewModelScope.launch {
            val tomorrow = LocalDate.now().plusDays(1)
            val updated = alarm.copy(
                addDateEpochDays = alarm.addDateEpochDays + tomorrow,
                skipDateEpochDays = alarm.skipDateEpochDays - tomorrow
            )
            dao.update(updated.toEntity())
            if (updated.enabled) {
                scheduler.cancel(updated.id)
                scheduler.schedule(updated)
            }
        }
    }

    fun removeTomorrow(alarm: AlarmRule) {
        viewModelScope.launch {
            val tomorrow = LocalDate.now().plusDays(1)
            val updated = alarm.copy(addDateEpochDays = alarm.addDateEpochDays - tomorrow)
            dao.update(updated.toEntity())
            if (updated.enabled) {
                scheduler.cancel(updated.id)
                scheduler.schedule(updated)
            }
        }
    }

    fun setVacationDateForAll(date: LocalDate) {
        applyShiftTypeForDate(date, "휴가")
    }

    fun clearVacationDateForAll(date: LocalDate) {
        clearDateOverridesForAll(date)
    }

    fun setVacationRangeForAll(start: LocalDate, end: LocalDate) {
        applyShiftTypeForDateRange(start, end, "휴가")
    }

    fun clearVacationRangeForAll(start: LocalDate, end: LocalDate) {
        clearDateOverridesForRange(start, end)
    }

    fun applyShiftTypeForDateRange(start: LocalDate, end: LocalDate, targetType: String) {
        val normalized = normalizeWorkType(targetType)
        if (normalized.isBlank()) return

        viewModelScope.launch {
            val range = buildDateRange(start, end)
            if (range.isEmpty()) return@launch

            alarms.value.forEach { alarm ->
                val alarmType = extractWorkTypeFromLabel(alarm.label)
                var skip = alarm.skipDateEpochDays
                var add = alarm.addDateEpochDays

                range.forEach { date ->
                    val scheduledOnDate = AlarmTimeCalculator.isScheduledOnDate(alarm, date)
                    if (scheduledOnDate) {
                        skip = skip + date
                        add = add - date
                    }

                    if (alarmType == normalized) {
                        add = add + date
                        skip = skip - date
                    } else {
                        add = add - date
                    }
                }

                if (skip == alarm.skipDateEpochDays && add == alarm.addDateEpochDays) return@forEach

                val updated = alarm.copy(
                    skipDateEpochDays = skip,
                    addDateEpochDays = add
                )
                dao.update(updated.toEntity())
                if (updated.enabled) {
                    scheduler.cancel(updated.id)
                    scheduler.schedule(updated)
                }
            }
        }
    }

    fun clearDateOverridesForAll(date: LocalDate) {
        viewModelScope.launch {
            alarms.value.forEach { alarm ->
                val updated = alarm.copy(
                    skipDateEpochDays = alarm.skipDateEpochDays - date,
                    addDateEpochDays = alarm.addDateEpochDays - date
                )
                if (
                    updated.skipDateEpochDays == alarm.skipDateEpochDays &&
                    updated.addDateEpochDays == alarm.addDateEpochDays
                ) return@forEach

                dao.update(updated.toEntity())
                if (updated.enabled) {
                    scheduler.cancel(updated.id)
                    scheduler.schedule(updated)
                }
            }
        }
    }

    fun clearDateOverridesForRange(start: LocalDate, end: LocalDate) {
        viewModelScope.launch {
            val range = buildDateRange(start, end)
            if (range.isEmpty()) return@launch

            alarms.value.forEach { alarm ->
                val updated = alarm.copy(
                    skipDateEpochDays = alarm.skipDateEpochDays - range,
                    addDateEpochDays = alarm.addDateEpochDays - range
                )
                if (
                    updated.skipDateEpochDays == alarm.skipDateEpochDays &&
                    updated.addDateEpochDays == alarm.addDateEpochDays
                ) return@forEach

                dao.update(updated.toEntity())
                if (updated.enabled) {
                    scheduler.cancel(updated.id)
                    scheduler.schedule(updated)
                }
            }
        }
    }
    fun setSkipDateForAlarmIds(date: LocalDate, alarmIds: Set<Long>) {
        if (alarmIds.isEmpty()) return
        viewModelScope.launch {
            alarms.value.filter { it.id in alarmIds }.forEach { alarm ->
                val updated = alarm.copy(
                    skipDateEpochDays = alarm.skipDateEpochDays + date,
                    addDateEpochDays = alarm.addDateEpochDays - date
                )
                dao.update(updated.toEntity())
                if (updated.enabled) {
                    scheduler.cancel(updated.id)
                    scheduler.schedule(updated)
                }
            }
        }
    }

    fun clearSkipDateForAlarmIds(date: LocalDate, alarmIds: Set<Long>) {
        if (alarmIds.isEmpty()) return
        viewModelScope.launch {
            alarms.value.filter { it.id in alarmIds }.forEach { alarm ->
                val updated = alarm.copy(skipDateEpochDays = alarm.skipDateEpochDays - date)
                dao.update(updated.toEntity())
                if (updated.enabled) {
                    scheduler.cancel(updated.id)
                    scheduler.schedule(updated)
                }
            }
        }
    }

    fun applyShiftTypeForDate(date: LocalDate, targetType: String) {
        val normalized = normalizeWorkType(targetType)
        if (normalized.isBlank()) return

        viewModelScope.launch {
            alarms.value.forEach { alarm ->
                val scheduledOnDate = AlarmTimeCalculator.isScheduledOnDate(alarm, date)
                val alarmType = extractWorkTypeFromLabel(alarm.label)

                var skip = alarm.skipDateEpochDays
                var add = alarm.addDateEpochDays

                if (scheduledOnDate) {
                    skip = skip + date
                    add = add - date
                }

                if (alarmType == normalized) {
                    add = add + date
                    skip = skip - date
                } else {
                    // 이전에 강제로 추가된 다른 유형을 정리해 중복 울림 방지
                    add = add - date
                }

                if (skip == alarm.skipDateEpochDays && add == alarm.addDateEpochDays) return@forEach

                val updated = alarm.copy(
                    skipDateEpochDays = skip,
                    addDateEpochDays = add
                )
                dao.update(updated.toEntity())
                if (updated.enabled) {
                    scheduler.cancel(updated.id)
                    scheduler.schedule(updated)
                }
            }
        }
    }


    private fun buildDateRange(start: LocalDate, end: LocalDate): Set<LocalDate> {
        val from = minOf(start, end)
        val to = maxOf(start, end)
        val range = mutableSetOf<LocalDate>()
        var cursor = from
        while (!cursor.isAfter(to)) {
            range += cursor
            cursor = cursor.plusDays(1)
        }
        return range
    }

    fun restoreExceptionSnapshot(snapshot: Map<Long, Pair<Set<LocalDate>, Set<LocalDate>>>) {
        if (snapshot.isEmpty()) return
        viewModelScope.launch {
            alarms.value.forEach { alarm ->
                val state = snapshot[alarm.id] ?: return@forEach
                val updated = alarm.copy(
                    skipDateEpochDays = state.first,
                    addDateEpochDays = state.second
                )
                if (
                    updated.skipDateEpochDays == alarm.skipDateEpochDays &&
                    updated.addDateEpochDays == alarm.addDateEpochDays
                ) {
                    return@forEach
                }
                dao.update(updated.toEntity())
                if (updated.enabled) {
                    scheduler.cancel(updated.id)
                    scheduler.schedule(updated)
                }
            }
        }
    }
    fun rescheduleAllEnabled() {
        viewModelScope.launch {
            dao.getAllEnabled()
                .map { it.toDomain() }
                .forEach { scheduler.schedule(it) }
        }
    }
}
