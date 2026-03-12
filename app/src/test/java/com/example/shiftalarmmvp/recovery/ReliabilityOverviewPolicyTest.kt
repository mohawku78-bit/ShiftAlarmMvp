package com.example.shiftalarmmvp.recovery

import android.content.Intent
import com.example.shiftalarmmvp.scheduler.AlarmScheduleMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReliabilityOverviewPolicyTest {

    private val texts = testRecoveryStrings()

    @Test
    fun `no recent signals show neutral placeholders`() {
        val model = ReliabilityOverviewPolicy.build(
            signals = ReliabilityOverviewSignals(
                watchdogStatus = null,
                restorePostCheckStatus = null,
                recoveryStatus = null,
                latestRecoveryActionText = null,
                selfTestStatus = null,
                nightlyCheckStatus = null
            ),
            texts = texts
        )

        assertEquals(ReliabilityOverviewTone.NEUTRAL, model.watchdogLine.tone)
        assertEquals(texts.overview.watchdogEmpty, model.watchdogLine.text)
        assertEquals(ReliabilityOverviewTone.NEUTRAL, model.restorePostCheckLine.tone)
        assertEquals(texts.overview.restorePostCheckEmpty, model.restorePostCheckLine.text)
        assertEquals(ReliabilityOverviewTone.NEUTRAL, model.recoveryLine.tone)
        assertEquals(texts.overview.recoveryEmpty, model.recoveryLine.text)
        assertEquals(ReliabilityOverviewTone.NEUTRAL, model.selfTestLine.tone)
        assertEquals(ReliabilityOverviewTone.NEUTRAL, model.nightlyCheckLine.tone)
    }

    @Test
    fun `watchdog recent incident is surfaced with action tone`() {
        val now = System.currentTimeMillis()
        val model = ReliabilityOverviewPolicy.build(
            signals = ReliabilityOverviewSignals(
                watchdogStatus = AlarmWatchdogStatus(
                    checkedAtMillis = now,
                    eventType = AlarmWatchdogEventType.SELF_HEALED,
                    eventAtMillis = now,
                    alarmId = 1L,
                    alarmLabel = "Day shift",
                    cause = AlarmWatchdogCause.EXACT_PERMISSION_LOST,
                    expectedTriggerMillis = now - 120_000L,
                    scheduleMode = AlarmScheduleMode.EXACT
                ),
                restorePostCheckStatus = null,
                recoveryStatus = null,
                latestRecoveryActionText = null,
                selfTestStatus = null,
                nightlyCheckStatus = null
            ),
            texts = texts
        )

        assertEquals(ReliabilityOverviewTone.ACTION, model.watchdogLine.tone)
        assertTrue(model.watchdogLine.text.contains("watchdog"))
    }

    @Test
    fun `partial recovery and recent action are surfaced with action and info tones`() {
        val recoveryStatus = RescheduleRecoveryState(
            action = Intent.ACTION_BOOT_COMPLETED,
            occurredAtMillis = 1_741_680_000_000L,
            enabledCount = 3,
            scheduledCount = 2,
            blockedCount = 1
        )

        val model = ReliabilityOverviewPolicy.build(
            signals = ReliabilityOverviewSignals(
                watchdogStatus = null,
                restorePostCheckStatus = null,
                recoveryStatus = recoveryStatus,
                latestRecoveryActionText = "Manual recovery 03-11 10:30",
                selfTestStatus = null,
                nightlyCheckStatus = null
            ),
            texts = texts
        )

        assertEquals(ReliabilityOverviewTone.ACTION, model.recoveryLine.tone)
        assertEquals(ReliabilityOverviewTone.INFO, model.latestRecoveryActionLine.tone)
        assertTrue(model.recoveryLine.text.contains("restored only"))
    }

    @Test
    fun `degraded recovery still surfaces as action tone`() {
        val recoveryStatus = RescheduleRecoveryState(
            action = RescheduleTrigger.EXACT_PERMISSION_LOST.storageAction,
            occurredAtMillis = 1_741_680_000_000L,
            enabledCount = 2,
            scheduledCount = 2,
            blockedCount = 0,
            exactCount = 0,
            inexactCount = 2,
            primaryReason = com.example.shiftalarmmvp.scheduler.AlarmScheduleFailureReason.EXACT_PERMISSION_DENIED
        )

        val model = ReliabilityOverviewPolicy.build(
            signals = ReliabilityOverviewSignals(
                watchdogStatus = null,
                restorePostCheckStatus = null,
                recoveryStatus = recoveryStatus,
                latestRecoveryActionText = null,
                selfTestStatus = null,
                nightlyCheckStatus = null
            ),
            texts = texts
        )

        assertEquals(ReliabilityOverviewTone.ACTION, model.recoveryLine.tone)
        assertTrue(model.recoveryLine.text.contains("fallback"))
    }

    @Test
    fun `passed self test and clean nightly check are safe`() {
        val selfTestStatus = SelfTestStatus(
            lastEvent = SelfTestStatus.Event.PASSED,
            scheduledAtMillis = 1_741_680_000_000L,
            triggerAtMillis = 1_741_680_120_000L,
            triggeredAtMillis = 1_741_680_120_000L,
            feedbackAtMillis = 1_741_680_180_000L
        )
        val nightlyStatus = NightlyReliabilityCheckStatus(
            checkedAtMillis = 1_741_680_000_000L,
            issueCount = 0,
            summary = "All clear"
        )

        val model = ReliabilityOverviewPolicy.build(
            signals = ReliabilityOverviewSignals(
                watchdogStatus = AlarmWatchdogStatus(
                    checkedAtMillis = 1_741_680_000_000L,
                    eventType = AlarmWatchdogEventType.CLEAN,
                    eventAtMillis = 1_741_680_000_000L
                ),
                restorePostCheckStatus = null,
                recoveryStatus = null,
                latestRecoveryActionText = null,
                selfTestStatus = selfTestStatus,
                nightlyCheckStatus = nightlyStatus
            ),
            texts = texts
        )

        assertEquals(ReliabilityOverviewTone.SAFE, model.watchdogLine.tone)
        assertEquals(ReliabilityOverviewTone.SAFE, model.selfTestLine.tone)
        assertEquals(ReliabilityOverviewTone.SAFE, model.nightlyCheckLine.tone)
        assertTrue(model.nightlyCheckLine.text.contains("All clear"))
    }

    @Test
    fun `blank latest recovery action falls back to neutral placeholder`() {
        val model = ReliabilityOverviewPolicy.build(
            signals = ReliabilityOverviewSignals(
                watchdogStatus = null,
                restorePostCheckStatus = null,
                recoveryStatus = null,
                latestRecoveryActionText = "   ",
                selfTestStatus = null,
                nightlyCheckStatus = null
            ),
            texts = texts
        )

        assertEquals(ReliabilityOverviewTone.NEUTRAL, model.latestRecoveryActionLine.tone)
        assertEquals(texts.overview.latestRecoveryActionEmpty, model.latestRecoveryActionLine.text)
    }

    @Test
    fun `nightly issues are surfaced as check tone`() {
        val nightlyStatus = NightlyReliabilityCheckStatus(
            checkedAtMillis = 1_741_680_000_000L,
            issueCount = 2,
            summary = "Check needed"
        )

        val model = ReliabilityOverviewPolicy.build(
            signals = ReliabilityOverviewSignals(
                watchdogStatus = null,
                restorePostCheckStatus = null,
                recoveryStatus = null,
                latestRecoveryActionText = null,
                selfTestStatus = null,
                nightlyCheckStatus = nightlyStatus
            ),
            texts = texts
        )

        assertEquals(ReliabilityOverviewTone.CHECK, model.nightlyCheckLine.tone)
        assertTrue(model.nightlyCheckLine.text.isNotBlank())
    }

    @Test
    fun `restore pending is surfaced with action tone`() {
        val model = ReliabilityOverviewPolicy.build(
            signals = ReliabilityOverviewSignals(
                watchdogStatus = null,
                restorePostCheckStatus = RestorePostCheckStatus(restoredAtMillis = 1_741_680_000_000L),
                recoveryStatus = null,
                latestRecoveryActionText = null,
                selfTestStatus = null,
                nightlyCheckStatus = null
            ),
            texts = texts
        )

        assertEquals(ReliabilityOverviewTone.ACTION, model.restorePostCheckLine.tone)
        assertTrue(model.restorePostCheckLine.text.contains("restore"))
    }
}