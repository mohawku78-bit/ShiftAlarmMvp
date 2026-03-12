package com.example.shiftalarmmvp.recovery

import com.example.shiftalarmmvp.scheduler.AlarmScheduleMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TriggerHistoryStoreModelTest {

    @Test
    fun `reschedule replaces pending registration for same alarm`() {
        val initial = listOf(
            TrackedAlarmRegistration(
                alarmId = 10L,
                label = "Day",
                expectedTriggerMillis = 100L,
                scheduledAtMillis = 10L,
                scheduleMode = AlarmScheduleMode.EXACT
            )
        )

        val updated = upsertTrackedRegistration(
            initial,
            TrackedAlarmRegistration(
                alarmId = 10L,
                label = "Day",
                expectedTriggerMillis = 200L,
                scheduledAtMillis = 20L,
                scheduleMode = AlarmScheduleMode.INEXACT
            )
        )

        assertEquals(1, updated.size)
        assertEquals(200L, updated.single().expectedTriggerMillis)
        assertEquals(AlarmScheduleMode.INEXACT, updated.single().scheduleMode)
    }

    @Test
    fun `remove tracked registration clears matching alarm`() {
        val current = listOf(
            TrackedAlarmRegistration(10L, "Day", 100L, 10L, AlarmScheduleMode.EXACT),
            TrackedAlarmRegistration(11L, "Night", 120L, 12L, AlarmScheduleMode.EXACT)
        )

        val remaining = removeTrackedRegistration(current, alarmId = 10L)

        assertEquals(listOf(11L), remaining.map { it.alarmId })
    }

    @Test
    fun `receiver trigger closes matching pending registration`() {
        val current = listOf(
            TrackedAlarmRegistration(10L, "Day", 100L, 10L, AlarmScheduleMode.EXACT),
            TrackedAlarmRegistration(11L, "Night", 120L, 12L, AlarmScheduleMode.EXACT)
        )

        val (remaining, matched) = closeTrackedRegistrationAsTriggered(
            current = current,
            alarmId = 10L,
            expectedTriggerMillis = 100L
        )

        assertEquals(1, remaining.size)
        assertEquals(10L, matched?.alarmId)
        assertEquals(100L, matched?.expectedTriggerMillis)
    }

    @Test
    fun `triggered event suppresses duplicate missed classification`() {
        val registration = TrackedAlarmRegistration(10L, "Day", 100L, 10L, AlarmScheduleMode.EXACT)
        val events = listOf(
            TriggerHistoryEvent(
                eventType = TriggerHistoryEventType.TRIGGERED,
                eventAtMillis = 105L,
                alarmId = 10L,
                label = "Day",
                expectedTriggerMillis = 100L,
                scheduleMode = AlarmScheduleMode.EXACT
            )
        )

        assertTrue(hasTriggeredEventForRegistration(events, registration))
        assertFalse(
            hasTriggeredEventForRegistration(
                events,
                registration.copy(expectedTriggerMillis = 110L)
            )
        )
    }
}
