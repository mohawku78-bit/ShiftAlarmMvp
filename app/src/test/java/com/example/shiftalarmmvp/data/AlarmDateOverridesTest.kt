package com.example.shiftalarmmvp.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmDateOverridesTest {

    @Test
    fun `skip wins when same date exists in both sets`() {
        val conflictingDate = LocalDate.of(2026, 3, 12)
        val addOnlyDate = LocalDate.of(2026, 3, 13)

        val overrides = AlarmDateOverrides.of(
            skipDates = setOf(conflictingDate),
            addDates = setOf(conflictingDate, addOnlyDate)
        )

        assertEquals(setOf(conflictingDate), overrides.skipDates)
        assertEquals(setOf(addOnlyDate), overrides.addDates)
    }

    @Test
    fun `withState swaps a date between skip add and none`() {
        val date = LocalDate.of(2026, 3, 12)

        val skipped = AlarmDateOverrides.of().withState(date, AlarmDateOverrideState.SKIP)
        val added = skipped.withState(date, AlarmDateOverrideState.ADD)
        val cleared = added.withState(date, AlarmDateOverrideState.NONE)

        assertEquals(setOf(date), skipped.skipDates)
        assertEquals(emptySet<LocalDate>(), skipped.addDates)
        assertEquals(emptySet<LocalDate>(), added.skipDates)
        assertEquals(setOf(date), added.addDates)
        assertEquals(emptySet<LocalDate>(), cleared.skipDates)
        assertEquals(emptySet<LocalDate>(), cleared.addDates)
    }

    @Test
    fun `clearDates removes both skip and add states`() {
        val first = LocalDate.of(2026, 3, 12)
        val second = LocalDate.of(2026, 3, 13)
        val third = LocalDate.of(2026, 3, 14)

        val overrides = AlarmDateOverrides.of(
            skipDates = setOf(first, second),
            addDates = setOf(third)
        )

        val cleared = overrides.clearDates(setOf(second, third))

        assertEquals(setOf(first), cleared.skipDates)
        assertEquals(emptySet<LocalDate>(), cleared.addDates)
    }
}
