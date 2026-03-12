package com.example.shiftalarmmvp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.shiftalarmmvp.data.AlarmDatabase
import com.example.shiftalarmmvp.data.AlarmDateOverrideState
import com.example.shiftalarmmvp.data.AlarmDateOverrides
import com.example.shiftalarmmvp.data.AlarmRule
import com.example.shiftalarmmvp.data.AlarmSoundType
import com.example.shiftalarmmvp.data.normalizeIntervalWeeks
import com.example.shiftalarmmvp.data.normalizeWeekPatterns
import com.example.shiftalarmmvp.data.normalizedDateOverrides
import com.example.shiftalarmmvp.data.toDomain
import com.example.shiftalarmmvp.data.toEntity
import com.example.shiftalarmmvp.data.withDateOverrides
import com.example.shiftalarmmvp.recovery.EnabledAlarmRescheduler
import com.example.shiftalarmmvp.recovery.RescheduleTrigger
import com.example.shiftalarmmvp.scheduler.AlarmScheduler
import com.example.shiftalarmmvp.scheduler.AlarmTimeCalculator
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun overrideStateForShiftType(
    alarm: AlarmRule,
    date: LocalDate,
    normalizedTargetType: String
): AlarmDateOverrideState {
    val scheduledOnDate = AlarmTimeCalculator.isScheduledOnDate(alarm, date)
    val alarmType = extractWorkTypeFromLabel(alarm.label)
    return when {
        alarmType == normalizedTargetType -> AlarmDateOverrideState.ADD
        scheduledOnDate -> AlarmDateOverrideState.SKIP
        else -> AlarmDateOverrideState.NONE
    }
}

internal fun applyShiftTypeOverride(
    alarm: AlarmRule,
    date: LocalDate,
    normalizedTargetType: String,
    baseOverrides: AlarmDateOverrides = alarm.normalizedDateOverrides()
): AlarmDateOverrides {
    return baseOverrides.withState(date, overrideStateForShiftType(alarm, date, normalizedTargetType))
}

internal fun applyShiftTypeOverrides(
    alarm: AlarmRule,
    dates: Iterable<LocalDate>,
    normalizedTargetType: String,
    baseOverrides: AlarmDateOverrides = alarm.normalizedDateOverrides()
): AlarmDateOverrides {
    var overrides = baseOverrides
    dates.forEach { date ->
        overrides = applyShiftTypeOverride(alarm, date, normalizedTargetType, overrides)
    }
    return overrides
}

class AlarmViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AlarmDatabase.get(application)
    private val dao = database.alarmDao()
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
            val interval = normalizeIntervalWeeks(intervalWeeks)
            val normalizedPattern = normalizeWeekPatterns(interval, weeklyPattern)
            val overrides = AlarmDateOverrides.of(
                skipDates = skipDateEpochDays,
                addDates = addDateEpochDays
            )
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
                skipDateEpochDays = overrides.skipDates,
                addDateEpochDays = overrides.addDates,
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
            val interval = normalizeIntervalWeeks(intervalWeeks)
            val normalizedPattern = normalizeWeekPatterns(interval, weeklyPattern)
            val overrides = AlarmDateOverrides.of(
                skipDates = skipDateEpochDays,
                addDates = addDateEpochDays
            )
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
                skipDateEpochDays = overrides.skipDates,
                addDateEpochDays = overrides.addDates,
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
            val updated = alarm.withDateOverrides(
                alarm.normalizedDateOverrides().withState(today, AlarmDateOverrideState.SKIP)
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
            val updated = alarm.withDateOverrides(alarm.normalizedDateOverrides().withoutSkip(today))
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
            val updated = alarm.withDateOverrides(
                alarm.normalizedDateOverrides().withState(tomorrow, AlarmDateOverrideState.ADD)
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
            val updated = alarm.withDateOverrides(alarm.normalizedDateOverrides().withoutAdd(tomorrow))
            dao.update(updated.toEntity())
            if (updated.enabled) {
                scheduler.cancel(updated.id)
                scheduler.schedule(updated)
            }
        }
    }

    fun setVacationDateForAll(date: LocalDate) {
        applyShiftTypeForDate(date, WORK_TYPE_VACATION)
    }

    fun clearVacationDateForAll(date: LocalDate) {
        clearDateOverridesForAll(date)
    }

    fun setVacationRangeForAll(start: LocalDate, end: LocalDate) {
        applyShiftTypeForDateRange(start, end, WORK_TYPE_VACATION)
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
                val overrides = applyShiftTypeOverrides(
                    alarm = alarm,
                    dates = range,
                    normalizedTargetType = normalized
                )

                if (overrides.skipDates == alarm.skipDateEpochDays && overrides.addDates == alarm.addDateEpochDays) {
                    return@forEach
                }

                val updated = alarm.withDateOverrides(overrides)
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
                val updated = alarm.withDateOverrides(alarm.normalizedDateOverrides().clearDates(setOf(date)))
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
                val updated = alarm.withDateOverrides(alarm.normalizedDateOverrides().clearDates(range))
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
                val updated = alarm.withDateOverrides(
                    alarm.normalizedDateOverrides().withState(date, AlarmDateOverrideState.SKIP)
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
                val updated = alarm.withDateOverrides(alarm.normalizedDateOverrides().withoutSkip(date))
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
                val overrides = applyShiftTypeOverride(
                    alarm = alarm,
                    date = date,
                    normalizedTargetType = normalized
                )
                if (overrides.skipDates == alarm.skipDateEpochDays && overrides.addDates == alarm.addDateEpochDays) {
                    return@forEach
                }

                val updated = alarm.withDateOverrides(overrides)
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
                val overrides = AlarmDateOverrides.of(
                    skipDates = state.first,
                    addDates = state.second
                )
                val updated = alarm.withDateOverrides(overrides)
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
    suspend fun replaceAllAlarms(imported: List<AlarmRule>): Int {
        val normalizedImported = imported.map { alarm ->
            val interval = normalizeIntervalWeeks(alarm.intervalWeeks)
            val normalizedPattern = normalizeWeekPatterns(interval, alarm.weeklyPattern)
            val overrides = AlarmDateOverrides.of(
                skipDates = alarm.skipDateEpochDays,
                addDates = alarm.addDateEpochDays
            )
            alarm.copy(
                id = 0,
                label = alarm.label.trim(),
                weeklyPattern = normalizedPattern,
                intervalWeeks = interval,
                snoozeMinutes = alarm.snoozeMinutes.coerceIn(1, 60),
                snoozeMaxCount = alarm.snoozeMaxCount.coerceIn(0, 99),
                volumePercent = alarm.volumePercent.coerceIn(0, 100),
                skipDateEpochDays = overrides.skipDates,
                addDateEpochDays = overrides.addDates
            )
        }
        val existing = withContext(Dispatchers.IO) {
            dao.getAll().map { it.toDomain() }
        }
        withContext(Dispatchers.IO) {
            database.withTransaction {
                dao.deleteAll()
                normalizedImported.forEach { alarm ->
                    dao.insert(alarm.toEntity())
                }
            }
        }
        existing.forEach { scheduler.cancel(it.id) }
        withContext(Dispatchers.IO) {
            EnabledAlarmRescheduler(getApplication()).rescheduleAllEnabled(RescheduleTrigger.RESTORE)
        }
        return normalizedImported.size
    }

    fun rescheduleAllEnabled(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    EnabledAlarmRescheduler(getApplication()).rescheduleAllEnabled(RescheduleTrigger.MANUAL)
                }
            } finally {
                onComplete?.invoke()
            }
        }
    }
}





