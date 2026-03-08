package com.example.shiftalarmmvp.scheduler

import com.example.shiftalarmmvp.data.AlarmRule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

object AlarmTimeCalculator {
    fun nextTrigger(rule: AlarmRule, now: LocalDateTime = LocalDateTime.now()): LocalDateTime? {
        if (!rule.enabled) return null

        for (offset in 0..120) {
            val date = now.toLocalDate().plusDays(offset.toLong())
            if (!isScheduledOnDate(rule, date)) continue

            val candidate = LocalDateTime.of(date, LocalTime.of(rule.hour, rule.minute))
            if (candidate.isAfter(now)) return candidate
        }
        return null
    }

    fun nextTriggers(rule: AlarmRule, count: Int, now: LocalDateTime = LocalDateTime.now()): List<LocalDateTime> {
        if (count <= 0) return emptyList()
        val result = mutableListOf<LocalDateTime>()
        var cursor = now

        repeat(count) {
            val next = nextTrigger(rule, cursor) ?: return result
            result.add(next)
            cursor = next.plusMinutes(1)
        }
        return result
    }

    fun isScheduledOnDate(rule: AlarmRule, date: LocalDate): Boolean {
        if (!rule.enabled) return false
        if (rule.skipDateEpochDays.contains(date)) return false
        if (rule.addDateEpochDays.contains(date)) return true

        val slot = weekSlot(date, rule.anchorDate, rule.intervalWeeks) ?: return false
        val weekDays = rule.weeklyPattern.getOrNull(slot).orEmpty()
        return date.dayOfWeek in weekDays
    }

    private fun weekSlot(date: LocalDate, anchorDate: LocalDate, intervalWeeks: Int): Int? {
        val interval = intervalWeeks.coerceIn(1, 3)
        val anchorWeekStart = anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val dateWeekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weeksBetween = ChronoUnit.WEEKS.between(anchorWeekStart, dateWeekStart)
        if (weeksBetween < 0) return null
        return (weeksBetween % interval.toLong()).toInt()
    }
}
