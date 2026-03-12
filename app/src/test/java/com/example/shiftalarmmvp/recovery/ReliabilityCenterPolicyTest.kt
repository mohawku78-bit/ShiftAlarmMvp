package com.example.shiftalarmmvp.recovery

import android.content.Intent
import android.os.Build
import com.example.shiftalarmmvp.scheduler.AlarmScheduleFailureReason
import com.example.shiftalarmmvp.scheduler.AlarmScheduleMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReliabilityCenterPolicyTest {

    private val texts = testRecoveryStrings()

    @Test
    fun `unresolved setup item promotes summary and selects matching primary action`() {
        val model = ReliabilityCenterPolicy.build(
            signals = baseSignals(exactReady = false),
            texts = texts,
            sdkInt = Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        )

        assertEquals(texts.home.statusActionNeeded, model.summary.statusLabel)
        assertEquals(HomeReliabilityAction.OPEN_EXACT_ALARM_SETTINGS, model.summary.primaryAction)
        assertEquals(ReliabilitySetupIssue.EXACT_ALARM, model.setup.primaryStep?.issue)
    }

    @Test
    fun `battery guide hint only appears when battery setup is unresolved`() {
        val unresolved = ReliabilityCenterPolicy.build(
            signals = baseSignals(batteryReady = false, batteryGuideHint = "battery-guide"),
            texts = texts,
            sdkInt = Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        )
        val ready = ReliabilityCenterPolicy.build(
            signals = baseSignals(batteryReady = true, batteryGuideHint = "battery-guide"),
            texts = texts,
            sdkInt = Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        )

        assertEquals("battery-guide", unresolved.batteryGuideHint)
        assertNull(ready.batteryGuideHint)
    }

    @Test
    fun `home recent lines prioritize watchdog before recovery and self test`() {
        val now = System.currentTimeMillis()
        val model = ReliabilityCenterPolicy.build(
            signals = baseSignals(
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
                recoveryStatus = RescheduleRecoveryState(
                    action = Intent.ACTION_BOOT_COMPLETED,
                    occurredAtMillis = now,
                    enabledCount = 3,
                    scheduledCount = 2,
                    blockedCount = 1
                ),
                latestRecoveryActionText = "manual recovery",
                selfTestStatus = SelfTestStatus(
                    lastEvent = SelfTestStatus.Event.FAILED,
                    scheduledAtMillis = now,
                    triggerAtMillis = now,
                    triggeredAtMillis = now,
                    feedbackAtMillis = now
                ),
                nightlyCheckStatus = NightlyReliabilityCheckStatus(
                    checkedAtMillis = now,
                    issueCount = 2,
                    summary = "needs check"
                )
            ),
            texts = texts,
            sdkInt = Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        )

        assertEquals(listOf("watchdog", "recovery", "self_test"), model.homeRecentLines.map { it.key })
    }

    @Test
    fun `safe state keeps shared summary and action between home and editor consumers`() {
        val signals = baseSignals()
        val model = ReliabilityCenterPolicy.build(
            signals = signals,
            texts = texts,
            sdkInt = Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        )
        val expectedSummary = HomeReliabilityPolicy.evaluate(
            signals = HomeReliabilitySignals(
                exactReady = signals.exactReady,
                notificationReady = signals.notificationReady,
                batteryReady = signals.batteryReady,
                nextAlarmRegisteredReady = signals.nextAlarmRegisteredReady,
                shouldCheckAlarmRegistration = signals.shouldCheckAlarmRegistration,
                recoveryNeedsAttention = signals.recoveryNeedsAttention,
                watchdogStatus = signals.watchdogStatus,
                restorePostCheckStatus = signals.restorePostCheckStatus,
                selfTestEvent = signals.selfTestStatus?.lastEvent,
                selfTestNeedsFollowUp = signals.selfTestNeedsFollowUp,
                nightlyIssueCount = signals.nightlyCheckStatus?.issueCount ?: 0
            ),
            texts = texts,
            sdkInt = Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        )

        assertEquals(expectedSummary, model.summary)
        assertEquals(texts.home.statusSafe, model.summary.statusLabel)
        assertTrue(model.setup.allReady)
    }

    @Test
    fun `watchdog incident promotes summary to action and opens center`() {
        val now = System.currentTimeMillis()
        val model = ReliabilityCenterPolicy.build(
            signals = baseSignals(
                watchdogStatus = AlarmWatchdogStatus(
                    checkedAtMillis = now,
                    eventType = AlarmWatchdogEventType.MISSED,
                    eventAtMillis = now,
                    alarmId = 2L,
                    alarmLabel = "Night shift",
                    cause = AlarmWatchdogCause.BATTERY_RESTRICTION,
                    expectedTriggerMillis = now - 120_000L,
                    scheduleMode = AlarmScheduleMode.EXACT
                )
            ),
            texts = texts,
            sdkInt = Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        )

        assertEquals(texts.home.statusActionNeeded, model.summary.statusLabel)
        assertEquals(HomeReliabilityAction.OPEN_RELIABILITY_CENTER, model.summary.primaryAction)
        assertEquals(ReliabilityOverviewTone.ACTION, model.recent.watchdogLine.tone)
    }

    @Test
    fun `restore pending promotes summary to action and surfaces recent line`() {
        val status = RestorePostCheckStatus(restoredAtMillis = 1_741_680_000_000L)
        val model = ReliabilityCenterPolicy.build(
            signals = baseSignals(restorePostCheckStatus = status),
            texts = texts,
            sdkInt = Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        )

        assertEquals(texts.home.statusActionNeeded, model.summary.statusLabel)
        assertEquals(HomeReliabilityAction.REFRESH_STATUS, model.summary.primaryAction)
        assertEquals(ReliabilityOverviewTone.ACTION, model.recent.restorePostCheckLine.tone)
        assertEquals("restore_post_check", model.homeRecentLines.first().key)
    }

    @Test
    fun `exact permission issue keeps degraded fallback state out of safe summary`() {
        val model = ReliabilityCenterPolicy.build(
            signals = baseSignals(
                exactReady = false,
                recoveryNeedsAttention = true,
                recoveryStatus = RescheduleRecoveryState(
                    action = RescheduleTrigger.EXACT_PERMISSION_LOST.storageAction,
                    occurredAtMillis = 1_741_680_000_000L,
                    enabledCount = 2,
                    scheduledCount = 2,
                    blockedCount = 0,
                    exactCount = 0,
                    inexactCount = 2,
                    primaryReason = AlarmScheduleFailureReason.EXACT_PERMISSION_DENIED
                )
            ),
            texts = texts,
            sdkInt = Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        )

        assertEquals(texts.home.statusActionNeeded, model.summary.statusLabel)
        assertEquals(HomeReliabilityAction.OPEN_EXACT_ALARM_SETTINGS, model.summary.primaryAction)
        assertEquals(ReliabilityOverviewTone.ACTION, model.recent.recoveryLine.tone)
    }

    @Test
    fun `registration mismatch is reflected as pending shared setup item`() {
        val model = ReliabilityCenterPolicy.build(
            signals = baseSignals(
                nextAlarmRegisteredReady = false,
                shouldCheckAlarmRegistration = true
            ),
            texts = texts,
            sdkInt = Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        )

        assertEquals(ReliabilitySetupIssue.ALARM_REGISTRATION, model.setup.primaryStep?.issue)
        assertEquals(HomeReliabilityAction.RESCHEDULE_ALARMS, model.summary.primaryAction)
    }

    private fun baseSignals(
        exactReady: Boolean = true,
        notificationReady: Boolean = true,
        batteryReady: Boolean = true,
        nextAlarmRegisteredReady: Boolean = true,
        shouldCheckAlarmRegistration: Boolean = false,
        recoveryNeedsAttention: Boolean = false,
        watchdogStatus: AlarmWatchdogStatus? = null,
        restorePostCheckStatus: RestorePostCheckStatus? = null,
        recoveryStatus: RescheduleRecoveryState? = null,
        latestRecoveryActionText: String? = null,
        selfTestStatus: SelfTestStatus? = null,
        selfTestNeedsFollowUp: Boolean = false,
        nightlyCheckStatus: NightlyReliabilityCheckStatus? = null,
        batteryGuideHint: String? = null
    ): ReliabilityCenterSignals {
        return ReliabilityCenterSignals(
            exactReady = exactReady,
            notificationReady = notificationReady,
            batteryReady = batteryReady,
            nextAlarmRegisteredReady = nextAlarmRegisteredReady,
            shouldCheckAlarmRegistration = shouldCheckAlarmRegistration,
            recoveryNeedsAttention = recoveryNeedsAttention,
            watchdogStatus = watchdogStatus,
            restorePostCheckStatus = restorePostCheckStatus,
            recoveryStatus = recoveryStatus,
            latestRecoveryActionText = latestRecoveryActionText,
            selfTestStatus = selfTestStatus,
            selfTestNeedsFollowUp = selfTestNeedsFollowUp,
            nightlyCheckStatus = nightlyCheckStatus,
            batteryGuideHint = batteryGuideHint
        )
    }
}

