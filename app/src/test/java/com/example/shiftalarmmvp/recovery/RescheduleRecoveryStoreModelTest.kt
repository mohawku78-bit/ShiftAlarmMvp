package com.example.shiftalarmmvp.recovery

import com.example.shiftalarmmvp.scheduler.AlarmScheduleFailureReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RescheduleRecoveryStoreModelTest {
    private val texts = testRecoveryStrings()

    @Test
    fun `legacy state defaults scheduled alarms to exact count`() {
        val state = createRescheduleRecoveryState(
            action = RescheduleTrigger.MANUAL.storageAction,
            occurredAtMillis = 1_741_680_000_000L,
            enabledCount = 2,
            scheduledCount = 2,
            blockedCount = 0
        )

        assertEquals(2, state.exactCount)
        assertEquals(0, state.inexactCount)
        assertEquals(RescheduleRecoveryState.Outcome.FULL_RECOVERY, state.outcome)
        assertEquals("exact 2 / total 2", state.countText(texts.reschedule))
    }

    @Test
    fun `fallback scheduling is degraded and needs attention`() {
        val state = createRescheduleRecoveryState(
            action = RescheduleTrigger.EXACT_PERMISSION_LOST.storageAction,
            occurredAtMillis = 1_741_680_000_000L,
            enabledCount = 3,
            scheduledCount = 3,
            blockedCount = 0,
            exactCount = 1,
            inexactCount = 2,
            primaryReason = AlarmScheduleFailureReason.EXACT_PERMISSION_DENIED
        )

        assertEquals(RescheduleRecoveryState.Outcome.DEGRADED_RECOVERY, state.outcome)
        assertTrue(state.needsAttention)
        assertEquals("exact 1 / fallback 2 / total 3", state.countText(texts.reschedule))
        assertTrue(state.reasonText(texts.reschedule).contains(texts.reschedule.reasonExactPermissionDenied))
    }

    @Test
    fun `blocked alarms remain partial recovery`() {
        val state = createRescheduleRecoveryState(
            action = RescheduleTrigger.MANUAL.storageAction,
            occurredAtMillis = 1_741_680_000_000L,
            enabledCount = 3,
            scheduledCount = 2,
            blockedCount = 1,
            exactCount = 1,
            inexactCount = 1,
            primaryReason = AlarmScheduleFailureReason.PLATFORM_FAILURE
        )

        assertEquals(RescheduleRecoveryState.Outcome.PARTIAL_RECOVERY, state.outcome)
        assertTrue(state.countText(texts.reschedule).contains("blocked 1"))
    }
}