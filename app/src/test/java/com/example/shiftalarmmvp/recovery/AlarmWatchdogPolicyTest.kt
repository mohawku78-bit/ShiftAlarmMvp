package com.example.shiftalarmmvp.recovery

import com.example.shiftalarmmvp.scheduler.AlarmScheduleMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmWatchdogPolicyTest {

    @Test
    fun `exact and inexact grace windows use different overdue thresholds`() {
        val exact = TrackedAlarmRegistration(1L, "Day", 1_000L, 900L, AlarmScheduleMode.EXACT)
        val inexact = TrackedAlarmRegistration(2L, "Night", 1_000L, 900L, AlarmScheduleMode.INEXACT)

        assertTrue(overdueTrackedRegistrations(listOf(exact), nowMillis = 1_000L + ALARM_WATCHDOG_EXACT_GRACE_MILLIS + 1L).contains(exact))
        assertTrue(overdueTrackedRegistrations(listOf(inexact), nowMillis = 1_000L + ALARM_WATCHDOG_INEXACT_GRACE_MILLIS + 1L).contains(inexact))
        assertFalse(overdueTrackedRegistrations(listOf(inexact), nowMillis = 1_000L + ALARM_WATCHDOG_EXACT_GRACE_MILLIS + 1L).contains(inexact))
    }

    @Test
    fun `next watchdog check uses earliest due registration`() {
        val pending = listOf(
            TrackedAlarmRegistration(1L, "Day", 5_000L, 1_000L, AlarmScheduleMode.INEXACT),
            TrackedAlarmRegistration(2L, "Night", 2_000L, 1_000L, AlarmScheduleMode.EXACT)
        )

        val nextCheckAt = nextAlarmWatchdogCheckAtMillis(pending, nowMillis = 0L)

        assertEquals(2_000L + ALARM_WATCHDOG_EXACT_GRACE_MILLIS, nextCheckAt)
    }

    @Test
    fun `cause classification follows fixed priority`() {
        val exact = TrackedAlarmRegistration(1L, "Day", 1_000L, 900L, AlarmScheduleMode.EXACT)
        val inexact = TrackedAlarmRegistration(2L, "Night", 1_000L, 900L, AlarmScheduleMode.INEXACT)

        assertEquals(AlarmWatchdogCause.EXACT_PERMISSION_LOST, classifyWatchdogCause(exact, exactReady = false, batteryReady = false))
        assertEquals(AlarmWatchdogCause.INEXACT_FALLBACK_DELAY, classifyWatchdogCause(inexact, exactReady = true, batteryReady = false))
        assertEquals(AlarmWatchdogCause.BATTERY_RESTRICTION, classifyWatchdogCause(exact, exactReady = true, batteryReady = false))
        assertEquals(AlarmWatchdogCause.REGISTRATION_LOSS, classifyWatchdogCause(exact, exactReady = true, batteryReady = true))
    }

    @Test
    fun `attention window expires after twenty four hours`() {
        val status = AlarmWatchdogStatus(
            checkedAtMillis = 10L,
            eventType = AlarmWatchdogEventType.MISSED,
            eventAtMillis = 10L,
            alarmId = 1L,
            alarmLabel = "Day",
            cause = AlarmWatchdogCause.UNKNOWN,
            expectedTriggerMillis = 1L,
            scheduleMode = AlarmScheduleMode.EXACT
        )

        assertTrue(status.needsAttention(10L + ALARM_WATCHDOG_ATTENTION_WINDOW_MILLIS - 1L))
        assertFalse(status.needsAttention(10L + ALARM_WATCHDOG_ATTENTION_WINDOW_MILLIS + 1L))
    }
}
