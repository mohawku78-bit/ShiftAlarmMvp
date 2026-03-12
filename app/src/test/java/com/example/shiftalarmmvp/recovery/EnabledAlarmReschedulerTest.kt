package com.example.shiftalarmmvp.recovery

import android.content.Intent
import com.example.shiftalarmmvp.scheduler.AlarmScheduleFailureReason
import com.example.shiftalarmmvp.scheduler.AlarmScheduleMode
import com.example.shiftalarmmvp.scheduler.AlarmScheduleResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EnabledAlarmReschedulerTest {
    @Test
    fun `permission transition helper detects gained and lost states`() {
        assertEquals(ExactAlarmPermissionTransition.NONE, detectExactAlarmPermissionTransition(null, false))
        assertEquals(ExactAlarmPermissionTransition.NONE, detectExactAlarmPermissionTransition(true, true))
        assertEquals(ExactAlarmPermissionTransition.GAINED, detectExactAlarmPermissionTransition(false, true))
        assertEquals(ExactAlarmPermissionTransition.LOST, detectExactAlarmPermissionTransition(true, false))
    }

    @Test
    fun `lifecycle trigger reconciles stale exact permission loss on cold start`() {
        assertEquals(
            RescheduleTrigger.EXACT_PERMISSION_LOST,
            resolveExactAlarmPermissionRescheduleTrigger(
                previousExactReady = null,
                currentExactReady = false,
                recoveryState = createRescheduleRecoveryState(
                    action = RescheduleTrigger.EXACT_PERMISSION_GRANTED.storageAction,
                    occurredAtMillis = 1L,
                    enabledCount = 1,
                    scheduledCount = 1,
                    blockedCount = 0,
                    exactCount = 1,
                    inexactCount = 0
                )
            )
        )
    }

    @Test
    fun `lifecycle trigger restores exact scheduling after stale degraded state`() {
        assertEquals(
            RescheduleTrigger.EXACT_PERMISSION_GRANTED,
            resolveExactAlarmPermissionRescheduleTrigger(
                previousExactReady = null,
                currentExactReady = true,
                recoveryState = createRescheduleRecoveryState(
                    action = RescheduleTrigger.EXACT_PERMISSION_LOST.storageAction,
                    occurredAtMillis = 1L,
                    enabledCount = 1,
                    scheduledCount = 1,
                    blockedCount = 0,
                    exactCount = 0,
                    inexactCount = 1,
                    primaryReason = AlarmScheduleFailureReason.EXACT_PERMISSION_DENIED
                )
            )
        )
    }

    @Test
    fun `lifecycle trigger stays idle when degraded loss already recorded`() {
        assertNull(
            resolveExactAlarmPermissionRescheduleTrigger(
                previousExactReady = null,
                currentExactReady = false,
                recoveryState = createRescheduleRecoveryState(
                    action = RescheduleTrigger.EXACT_PERMISSION_LOST.storageAction,
                    occurredAtMillis = 1L,
                    enabledCount = 1,
                    scheduledCount = 1,
                    blockedCount = 0,
                    exactCount = 0,
                    inexactCount = 1,
                    primaryReason = AlarmScheduleFailureReason.EXACT_PERMISSION_DENIED
                )
            )
        )
    }

    @Test
    fun `system actions map to shared reschedule triggers`() {
        assertEquals(RescheduleTrigger.BOOT_COMPLETED, rescheduleTriggerFromSystemAction(Intent.ACTION_BOOT_COMPLETED))
        assertEquals(RescheduleTrigger.TIME_CHANGED, rescheduleTriggerFromSystemAction(Intent.ACTION_TIME_CHANGED))
        assertEquals(RescheduleTrigger.DATE_CHANGED, rescheduleTriggerFromSystemAction(Intent.ACTION_DATE_CHANGED))
        assertEquals(RescheduleTrigger.TIMEZONE_CHANGED, rescheduleTriggerFromSystemAction(Intent.ACTION_TIMEZONE_CHANGED))
        assertEquals(RescheduleTrigger.APP_UPDATED, rescheduleTriggerFromSystemAction(Intent.ACTION_MY_PACKAGE_REPLACED))
        assertNull(rescheduleTriggerFromSystemAction(Intent.ACTION_AIRPLANE_MODE_CHANGED))
    }

    @Test
    fun `reschedule summary aggregates exact inexact and blocked outcomes`() {
        val report = summarizeEnabledAlarmReschedule(
            trigger = RescheduleTrigger.MANUAL,
            enabledCount = 4,
            results = listOf(
                AlarmScheduleResult(scheduled = true, mode = AlarmScheduleMode.EXACT),
                AlarmScheduleResult(
                    scheduled = true,
                    mode = AlarmScheduleMode.INEXACT,
                    failureReason = AlarmScheduleFailureReason.EXACT_PERMISSION_DENIED
                ),
                AlarmScheduleResult(
                    scheduled = false,
                    mode = AlarmScheduleMode.BLOCKED,
                    failureReason = AlarmScheduleFailureReason.PLATFORM_FAILURE
                ),
                AlarmScheduleResult(scheduled = true, mode = AlarmScheduleMode.EXACT)
            )
        )

        assertEquals(2, report.exactCount)
        assertEquals(1, report.inexactCount)
        assertEquals(1, report.blockedCount)
        assertEquals(3, report.scheduledCount)
        assertEquals(AlarmScheduleFailureReason.EXACT_PERMISSION_DENIED, report.primaryReason)
    }
}