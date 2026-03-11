package com.example.shiftalarmmvp.recovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReliabilityOverviewPolicyTest {

    private val texts = testRecoveryStrings()

    @Test
    fun `no recent signals show neutral placeholders`() {
        val model = ReliabilityOverviewPolicy.build(
            signals = ReliabilityOverviewSignals(
                recoveryStatus = null,
                latestRecoveryActionText = null,
                selfTestStatus = null,
                nightlyCheckStatus = null
            ),
            texts = texts
        )

        assertEquals(ReliabilityOverviewTone.NEUTRAL, model.recoveryLine.tone)
        assertEquals("복구 기록 없음", model.recoveryLine.text)
        assertEquals(ReliabilityOverviewTone.NEUTRAL, model.selfTestLine.tone)
        assertEquals(ReliabilityOverviewTone.NEUTRAL, model.nightlyCheckLine.tone)
    }

    @Test
    fun `partial recovery and recent action are surfaced with action and info tones`() {
        val recoveryStatus = RescheduleRecoveryState(
            action = android.content.Intent.ACTION_BOOT_COMPLETED,
            occurredAtMillis = 1_741_680_000_000L,
            enabledCount = 3,
            scheduledCount = 2,
            blockedCount = 1
        )

        val model = ReliabilityOverviewPolicy.build(
            signals = ReliabilityOverviewSignals(
                recoveryStatus = recoveryStatus,
                latestRecoveryActionText = "최근 복구 조치 03-11 10:30 · 재예약 실행",
                selfTestStatus = null,
                nightlyCheckStatus = null
            ),
            texts = texts
        )

        assertEquals(ReliabilityOverviewTone.ACTION, model.recoveryLine.tone)
        assertEquals(ReliabilityOverviewTone.INFO, model.latestRecoveryActionLine.tone)
        assertTrue(model.recoveryLine.text.contains("복구"))
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
            summary = "문제 없음"
        )

        val model = ReliabilityOverviewPolicy.build(
            signals = ReliabilityOverviewSignals(
                recoveryStatus = null,
                latestRecoveryActionText = null,
                selfTestStatus = selfTestStatus,
                nightlyCheckStatus = nightlyStatus
            ),
            texts = texts
        )

        assertEquals(ReliabilityOverviewTone.SAFE, model.selfTestLine.tone)
        assertEquals(ReliabilityOverviewTone.SAFE, model.nightlyCheckLine.tone)
        assertTrue(model.nightlyCheckLine.text.contains("문제 없음"))
    }

    @Test
    fun `blank latest recovery action falls back to neutral placeholder`() {
        val model = ReliabilityOverviewPolicy.build(
            signals = ReliabilityOverviewSignals(
                recoveryStatus = null,
                latestRecoveryActionText = "   ",
                selfTestStatus = null,
                nightlyCheckStatus = null
            ),
            texts = texts
        )

        assertEquals(ReliabilityOverviewTone.NEUTRAL, model.latestRecoveryActionLine.tone)
        assertTrue(model.latestRecoveryActionLine.text.isNotBlank())
    }

    @Test
    fun `nightly issues are surfaced as check tone`() {
        val nightlyStatus = NightlyReliabilityCheckStatus(
            checkedAtMillis = 1_741_680_000_000L,
            issueCount = 2,
            summary = "확인 필요"
        )

        val model = ReliabilityOverviewPolicy.build(
            signals = ReliabilityOverviewSignals(
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
}