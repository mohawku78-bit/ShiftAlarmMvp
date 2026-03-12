package com.example.shiftalarmmvp.ui

import com.example.shiftalarmmvp.data.AlarmRule
import com.example.shiftalarmmvp.data.AlarmSoundType
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternNormalizationTest {

    private fun buildRule(
        intervalWeeks: Int,
        weeklyPattern: List<Set<DayOfWeek>>
    ): AlarmRule {
        return AlarmRule(
            id = 11,
            label = "Long rotation",
            hour = 7,
            minute = 30,
            weeklyPattern = weeklyPattern,
            intervalWeeks = intervalWeeks,
            anchorDate = LocalDate.of(2026, 3, 9),
            snoozeMinutes = 5,
            snoozeMaxCount = 1,
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
    fun `default editor week patterns match interval length`() {
        val patterns = defaultEditorWeekPatterns(4)

        assertEquals(4, patterns.size)
        assertEquals(
            setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY, DayOfWeek.SUNDAY),
            patterns[0]
        )
        assertEquals(
            setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY),
            patterns[1]
        )
        assertTrue(patterns.drop(2).all { it.isEmpty() })
    }

    @Test
    fun `editor pattern normalization pads trims and clamps active week index`() {
        val expanded = normalizeEditorPatternState(
            targetInterval = 4,
            incomingPatterns = listOf(setOf(DayOfWeek.MONDAY), setOf(DayOfWeek.FRIDAY)),
            activeWeekIndex = 9
        )
        val trimmed = normalizeEditorPatternState(
            targetInterval = 1,
            incomingPatterns = listOf(setOf(DayOfWeek.MONDAY), setOf(DayOfWeek.TUESDAY)),
            activeWeekIndex = 4
        )

        assertEquals(4, expanded.intervalWeeks)
        assertEquals(4, expanded.weekPatterns.size)
        assertEquals(setOf(DayOfWeek.MONDAY), expanded.weekPatterns[0])
        assertEquals(setOf(DayOfWeek.FRIDAY), expanded.weekPatterns[1])
        assertTrue(expanded.weekPatterns.drop(2).all { it.isEmpty() })
        assertEquals(3, expanded.activeWeekIndex)

        assertEquals(1, trimmed.intervalWeeks)
        assertEquals(1, trimmed.weekPatterns.size)
        assertEquals(setOf(DayOfWeek.MONDAY), trimmed.weekPatterns.single())
        assertEquals(0, trimmed.activeWeekIndex)
    }

    @Test
    fun `empty incoming editor patterns seed interval defaults`() {
        val normalized = normalizeEditorPatternState(
            targetInterval = 3,
            incomingPatterns = emptyList(),
            activeWeekIndex = 5
        )

        assertEquals(defaultEditorWeekPatterns(3), normalized.weekPatterns)
        assertEquals(2, normalized.activeWeekIndex)
    }

    @Test
    fun `long interval alarm keeps full pattern when converted to editor draft`() {
        val intervalWeeks = 13
        val pattern = List(intervalWeeks) { index ->
            when (index % 3) {
                0 -> setOf(DayOfWeek.MONDAY)
                1 -> setOf(DayOfWeek.WEDNESDAY)
                else -> setOf(DayOfWeek.SATURDAY)
            }
        }
        val draft = buildRule(intervalWeeks, pattern).toEditorDraft(
            editingAlarmId = 11,
            editingEnabled = true
        )
        val restoredEditorState = normalizeEditorPatternState(
            targetInterval = draft.intervalWeeks,
            incomingPatterns = draft.weekPatterns,
            activeWeekIndex = 20
        )

        assertEquals(intervalWeeks, draft.intervalWeeks)
        assertEquals(pattern, draft.weekPatterns)
        assertEquals(intervalWeeks, restoredEditorState.weekPatterns.size)
        assertEquals(pattern, restoredEditorState.weekPatterns)
        assertEquals(intervalWeeks - 1, restoredEditorState.activeWeekIndex)
    }

    @Test
    fun `rotation preset normalization pads and trims week patterns to interval`() {
        val normalizedLong = RotationPreset(
            name = "  Preset A  ",
            intervalWeeks = 4,
            anchorDate = LocalDate.of(2026, 3, 9),
            weekPatterns = listOf(setOf(DayOfWeek.MONDAY), setOf(DayOfWeek.FRIDAY))
        ).normalized()
        val normalizedShort = RotationPreset(
            name = "Preset B",
            intervalWeeks = 1,
            anchorDate = LocalDate.of(2026, 3, 9),
            weekPatterns = listOf(setOf(DayOfWeek.MONDAY), setOf(DayOfWeek.TUESDAY))
        ).normalized()

        assertEquals("Preset A", normalizedLong.name)
        assertEquals(4, normalizedLong.weekPatterns.size)
        assertEquals(setOf(DayOfWeek.MONDAY), normalizedLong.weekPatterns[0])
        assertEquals(setOf(DayOfWeek.FRIDAY), normalizedLong.weekPatterns[1])
        assertTrue(normalizedLong.weekPatterns.drop(2).all { it.isEmpty() })

        assertEquals(1, normalizedShort.weekPatterns.size)
        assertEquals(setOf(DayOfWeek.MONDAY), normalizedShort.weekPatterns.single())
    }
}
