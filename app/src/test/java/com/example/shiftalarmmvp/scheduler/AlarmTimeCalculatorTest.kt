package com.example.shiftalarmmvp.scheduler

import com.example.shiftalarmmvp.data.AlarmRule
import com.example.shiftalarmmvp.data.AlarmSoundType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class AlarmTimeCalculatorTest {
    private fun buildRule(
        anchorDate: LocalDate,
        intervalWeeks: Int,
        patterns: List<Set<DayOfWeek>>,
        skipDates: Set<LocalDate> = emptySet(),
        addDates: Set<LocalDate> = emptySet()
    ): AlarmRule {
        return AlarmRule(
            id = 1,
            hour = 7,
            minute = 0,
            weeklyPattern = patterns,
            intervalWeeks = intervalWeeks,
            anchorDate = anchorDate,
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

    private fun nextEpoch(rule: AlarmRule, now: LocalDateTime, zone: ZoneId): Long {
        val next = AlarmTimeCalculator.nextTrigger(rule, now) ?: throw IllegalStateException("no trigger")
        return next.atZone(zone).toInstant().toEpochMilli()
    }

    @Test
    fun `nextTriggers follows 2-week alternating slots`() {
        val rule = buildRule(
            anchorDate = LocalDate.of(2026, 3, 2),
            intervalWeeks = 2,
            patterns = listOf(
                setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY, DayOfWeek.SUNDAY),
                setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY)
            )
        )

        val now = LocalDateTime.of(2026, 3, 1, 0, 0)
        val actual = AlarmTimeCalculator.nextTriggers(rule, 10, now)

        val expected = listOf(
            LocalDateTime.of(2026, 3, 2, 7, 0),
            LocalDateTime.of(2026, 3, 4, 7, 0),
            LocalDateTime.of(2026, 3, 6, 7, 0),
            LocalDateTime.of(2026, 3, 8, 7, 0),
            LocalDateTime.of(2026, 3, 10, 7, 0),
            LocalDateTime.of(2026, 3, 12, 7, 0),
            LocalDateTime.of(2026, 3, 14, 7, 0),
            LocalDateTime.of(2026, 3, 16, 7, 0),
            LocalDateTime.of(2026, 3, 18, 7, 0),
            LocalDateTime.of(2026, 3, 20, 7, 0)
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `nextTriggers follows 3-week rotation with sparse days`() {
        val rule = buildRule(
            anchorDate = LocalDate.of(2026, 3, 2),
            intervalWeeks = 3,
            patterns = listOf(
                setOf(DayOfWeek.MONDAY),
                setOf(DayOfWeek.TUESDAY),
                setOf(DayOfWeek.WEDNESDAY)
            )
        )

        val now = LocalDateTime.of(2026, 3, 1, 6, 0)
        val actual = AlarmTimeCalculator.nextTriggers(rule, 8, now)

        val expected = listOf(
            LocalDateTime.of(2026, 3, 2, 7, 0),
            LocalDateTime.of(2026, 3, 10, 7, 0),
            LocalDateTime.of(2026, 3, 18, 7, 0),
            LocalDateTime.of(2026, 3, 23, 7, 0),
            LocalDateTime.of(2026, 3, 31, 7, 0),
            LocalDateTime.of(2026, 4, 8, 7, 0),
            LocalDateTime.of(2026, 4, 13, 7, 0),
            LocalDateTime.of(2026, 4, 21, 7, 0)
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `empty weekly pattern returns no trigger`() {
        val rule = buildRule(
            anchorDate = LocalDate.of(2026, 3, 2),
            intervalWeeks = 2,
            patterns = listOf(emptySet(), emptySet())
        )

        val actual = AlarmTimeCalculator.nextTriggers(rule, 10, LocalDateTime.of(2026, 3, 1, 0, 0))

        assertEquals(0, actual.size)
    }

    @Test
    fun `candidate exactly at now is skipped to next occurrence`() {
        val rule = buildRule(
            anchorDate = LocalDate.of(2026, 3, 2),
            intervalWeeks = 1,
            patterns = listOf(setOf(DayOfWeek.MONDAY))
        )

        val now = LocalDateTime.of(2026, 3, 2, 7, 0)

        val first = AlarmTimeCalculator.nextTrigger(rule, now)

        assertEquals(LocalDateTime.of(2026, 3, 9, 7, 0), first)
    }

    @Test
    fun `skip date excludes occurrence even if pattern matches`() {
        val rule = buildRule(
            anchorDate = LocalDate.of(2026, 3, 2),
            intervalWeeks = 2,
            patterns = listOf(
                setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY, DayOfWeek.SUNDAY),
                setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY)
            ),
            skipDates = setOf(LocalDate.of(2026, 3, 2))
        )

        val now = LocalDateTime.of(2026, 3, 1, 0, 0)
        val actual = AlarmTimeCalculator.nextTrigger(rule, now)

        assertEquals(LocalDateTime.of(2026, 3, 4, 7, 0), actual)
    }

    @Test
    fun `add date can add occurrence outside normal rotation`() {
        val rule = buildRule(
            anchorDate = LocalDate.of(2026, 3, 2),
            intervalWeeks = 2,
            patterns = listOf(emptySet(), emptySet()),
            addDates = setOf(LocalDate.of(2026, 3, 4))
        )

        val now = LocalDateTime.of(2026, 3, 1, 0, 0)

        val first = AlarmTimeCalculator.nextTrigger(rule, now)

        assertEquals(LocalDateTime.of(2026, 3, 4, 7, 0), first)
    }

    @Test
    fun `timezone changes change epoch while keeping local date and time`() {
        val rule = buildRule(
            anchorDate = LocalDate.of(2026, 3, 2),
            intervalWeeks = 1,
            patterns = listOf(setOf(DayOfWeek.MONDAY))
        )

        val now = LocalDateTime.of(2026, 1, 3, 10, 0)

        val seoul = nextEpoch(rule, now, ZoneId.of("Asia/Seoul"))
        val newYork = nextEpoch(rule, now, ZoneId.of("America/New_York"))

        assertNotEquals(seoul, newYork)

        val diff = Duration.between(Instant.ofEpochMilli(newYork), Instant.ofEpochMilli(seoul)).abs()
        assertEquals(Duration.ofHours(14), diff)
    }
}
