package com.example.shiftalarmmvp.ui

import com.example.shiftalarmmvp.data.AlarmDateOverrideState
import com.example.shiftalarmmvp.data.AlarmDateOverrides
import com.example.shiftalarmmvp.data.AlarmRule
import com.example.shiftalarmmvp.data.AlarmSoundType
import com.example.shiftalarmmvp.data.normalizedDateOverrides
import com.example.shiftalarmmvp.data.withDateOverrides
import com.example.shiftalarmmvp.scheduler.AlarmTimeCalculator
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmViewModelOverrideRulesTest {

    private fun buildRule(
        label: String,
        weeklyPattern: List<Set<DayOfWeek>>,
        skipDates: Set<LocalDate> = emptySet(),
        addDates: Set<LocalDate> = emptySet()
    ): AlarmRule {
        return AlarmRule(
            id = 21,
            label = label,
            hour = 7,
            minute = 0,
            weeklyPattern = weeklyPattern,
            intervalWeeks = 1,
            anchorDate = LocalDate.of(2026, 3, 9),
            snoozeMinutes = 5,
            snoozeMaxCount = 0,
            soundType = AlarmSoundType.ALARM,
            customSoundUri = null,
            volumePercent = 100,
            vibrationEnabled = true,
            skipDateEpochDays = skipDates,
            addDateEpochDays = addDates,
            enabled = true
        )
    }

    @Test
    fun `single date helper matches singleton range helper`() {
        val date = LocalDate.of(2026, 3, 12)
        val alarm = buildRule(
            label = WORK_TYPE_DAY,
            weeklyPattern = listOf(setOf(DayOfWeek.THURSDAY))
        )
        val baseOverrides = AlarmDateOverrides.of(addDates = setOf(date.minusDays(1)))
        val normalizedTargetType = normalizeWorkType(WORK_TYPE_DAY)

        val single = applyShiftTypeOverride(alarm, date, normalizedTargetType, baseOverrides)
        val range = applyShiftTypeOverrides(alarm, listOf(date), normalizedTargetType, baseOverrides)

        assertEquals(range, single)
    }

    @Test
    fun `override state follows target add scheduled skip and otherwise none`() {
        val date = LocalDate.of(2026, 3, 12)
        val targetAlarm = buildRule(
            label = WORK_TYPE_NIGHT,
            weeklyPattern = listOf(setOf(DayOfWeek.THURSDAY))
        )
        val scheduledNonTargetAlarm = buildRule(
            label = WORK_TYPE_DAY,
            weeklyPattern = listOf(setOf(DayOfWeek.THURSDAY))
        )
        val unscheduledNonTargetAlarm = buildRule(
            label = WORK_TYPE_DAY,
            weeklyPattern = listOf(setOf(DayOfWeek.MONDAY))
        )
        val normalizedTargetType = normalizeWorkType(WORK_TYPE_NIGHT)

        assertEquals(AlarmDateOverrideState.ADD, overrideStateForShiftType(targetAlarm, date, normalizedTargetType))
        assertEquals(AlarmDateOverrideState.SKIP, overrideStateForShiftType(scheduledNonTargetAlarm, date, normalizedTargetType))
        assertEquals(AlarmDateOverrideState.NONE, overrideStateForShiftType(unscheduledNonTargetAlarm, date, normalizedTargetType))
    }

    @Test
    fun `normalizing conflicting overrides preserves next trigger`() {
        val conflictDate = LocalDate.of(2026, 3, 12)
        val addOnlyDate = LocalDate.of(2026, 3, 13)
        val source = buildRule(
            label = WORK_TYPE_DAY,
            weeklyPattern = listOf(setOf(DayOfWeek.THURSDAY)),
            skipDates = setOf(conflictDate),
            addDates = setOf(conflictDate, addOnlyDate)
        )
        val normalized = source.withDateOverrides(source.normalizedDateOverrides())
        val now = LocalDateTime.of(2026, 3, 11, 0, 0)

        assertEquals(setOf(conflictDate), normalized.skipDateEpochDays)
        assertEquals(setOf(addOnlyDate), normalized.addDateEpochDays)
        assertEquals(
            AlarmTimeCalculator.nextTrigger(source, now),
            AlarmTimeCalculator.nextTrigger(normalized, now)
        )
    }
}
