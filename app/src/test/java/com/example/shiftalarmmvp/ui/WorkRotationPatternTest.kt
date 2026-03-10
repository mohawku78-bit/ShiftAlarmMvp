package com.example.shiftalarmmvp.ui

import com.example.shiftalarmmvp.data.AlarmRule
import com.example.shiftalarmmvp.data.AlarmSoundType
import com.example.shiftalarmmvp.scheduler.AlarmTimeCalculator
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkRotationPatternTest {

    private fun buildRule(
        label: String,
        pattern: List<Set<DayOfWeek>>,
        intervalWeeks: Int,
        anchorDate: LocalDate
    ): AlarmRule {
        return AlarmRule(
            id = 1,
            label = label,
            hour = 7,
            minute = 0,
            weeklyPattern = pattern,
            intervalWeeks = intervalWeeks,
            anchorDate = anchorDate,
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

    @Test
    fun `dang-bi daily sequence stays alternating across week boundaries`() {
        val sequence = listOf("당직", "비번")
        val anchor = LocalDate.of(2026, 3, 10) // Tue
        val rotation = buildWorkTemplateRotation(sequence, todayIndex = 0)

        val dangRule = buildRule(
            label = "당직",
            pattern = buildWeeklyPatternForType(rotation, "당직", anchor),
            intervalWeeks = rotation.intervalWeeks,
            anchorDate = anchor
        )
        val biRule = buildRule(
            label = "비번",
            pattern = buildWeeklyPatternForType(rotation, "비번", anchor),
            intervalWeeks = rotation.intervalWeeks,
            anchorDate = anchor
        )

        repeat(28) { offset ->
            val date = anchor.plusDays(offset.toLong())
            val isDang = AlarmTimeCalculator.isScheduledOnDate(dangRule, date)
            val isBi = AlarmTimeCalculator.isScheduledOnDate(biRule, date)

            assertTrue("exactly one type must match on $date", isDang.xor(isBi))
            assertEquals("daily alternation broken on $date", offset % 2 == 0, isDang)
        }
    }

    @Test
    fun `ju-dang-bi keeps exact phase even when anchor is not monday`() {
        val sequence = listOf("주간", "당직", "비번")
        val anchor = LocalDate.of(2026, 3, 10) // Tue
        val rotation = buildWorkTemplateRotation(sequence, todayIndex = 0)

        val rulesByType = sequence.associateWith { type ->
            buildRule(
                label = type,
                pattern = buildWeeklyPatternForType(rotation, type, anchor),
                intervalWeeks = rotation.intervalWeeks,
                anchorDate = anchor
            )
        }

        val start = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        repeat(42) { offset ->
            val date = start.plusDays(offset.toLong())
            val matched = rulesByType.filterValues { rule ->
                AlarmTimeCalculator.isScheduledOnDate(rule, date)
            }.keys

            assertEquals("exactly one type must match on $date", 1, matched.size)

            val phase = Math.floorMod(ChronoUnit.DAYS.between(anchor, date).toInt(), sequence.size)
            val expected = sequence[phase]
            assertEquals("phase mismatch on $date", expected, matched.first())
        }
    }
    @Test
    fun `ju-dang-bi-hu-dang-bi keeps exact phase for full 6-week cycle`() {
        val sequence = listOf("주간", "당직", "비번", "휴무", "당직", "비번")
        val anchor = LocalDate.of(2026, 3, 10) // Tue
        val rotation = buildWorkTemplateRotation(sequence, todayIndex = 0)

        assertEquals("interval weeks should match 6-day cycle", 6, rotation.intervalWeeks)

        val uniqueTypes = sequence.distinct()
        val rulesByType = uniqueTypes.associateWith { type ->
            buildRule(
                label = type,
                pattern = buildWeeklyPatternForType(rotation, type, anchor),
                intervalWeeks = rotation.intervalWeeks,
                anchorDate = anchor
            )
        }

        repeat(84) { offset -> // 14 weeks = two full 42-day cycles
            val date = anchor.plusDays(offset.toLong())
            val matched = rulesByType.filterValues { rule ->
                AlarmTimeCalculator.isScheduledOnDate(rule, date)
            }.keys

            assertEquals("exactly one type must match on $date", 1, matched.size)

            val expected = sequence[offset % sequence.size]
            assertEquals("phase mismatch on $date", expected, matched.first())
        }
    }
}

