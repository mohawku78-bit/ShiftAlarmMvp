package com.example.shiftalarmmvp.recovery

import android.os.Build

internal data class ReliabilityCenterSignals(
    val exactReady: Boolean,
    val notificationReady: Boolean,
    val batteryReady: Boolean,
    val nextAlarmRegisteredReady: Boolean,
    val shouldCheckAlarmRegistration: Boolean,
    val recoveryNeedsAttention: Boolean,
    val watchdogStatus: AlarmWatchdogStatus?,
    val restorePostCheckStatus: RestorePostCheckStatus?,
    val recoveryStatus: RescheduleRecoveryState?,
    val latestRecoveryActionText: String?,
    val selfTestStatus: SelfTestStatus?,
    val selfTestNeedsFollowUp: Boolean,
    val nightlyCheckStatus: NightlyReliabilityCheckStatus?,
    val batteryGuideHint: String?
)

internal data class ReliabilityCenterUiModel(
    val summary: HomeReliabilityUiModel,
    val setup: ReliabilitySetupUiModel,
    val recent: ReliabilityOverviewUiModel,
    val homeRecentLines: List<ReliabilityOverviewLineUi>,
    val batteryGuideHint: String?
)

internal object ReliabilityCenterPolicy {
    fun build(
        signals: ReliabilityCenterSignals,
        texts: RecoveryStrings,
        sdkInt: Int = Build.VERSION.SDK_INT
    ): ReliabilityCenterUiModel {
        val setup = ReliabilitySetupPolicy.build(
            signals = ReliabilitySetupSignals(
                exactReady = signals.exactReady,
                notificationReady = signals.notificationReady,
                batteryReady = signals.batteryReady,
                nextAlarmRegisteredReady = signals.nextAlarmRegisteredReady,
                shouldCheckAlarmRegistration = signals.shouldCheckAlarmRegistration
            ),
            texts = texts.setup,
            sdkInt = sdkInt
        )
        val summary = HomeReliabilityPolicy.evaluate(
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
            sdkInt = sdkInt
        )
        val recent = ReliabilityOverviewPolicy.build(
            signals = ReliabilityOverviewSignals(
                watchdogStatus = signals.watchdogStatus,
                restorePostCheckStatus = signals.restorePostCheckStatus,
                recoveryStatus = signals.recoveryStatus,
                latestRecoveryActionText = signals.latestRecoveryActionText,
                selfTestStatus = signals.selfTestStatus,
                nightlyCheckStatus = signals.nightlyCheckStatus
            ),
            texts = texts
        )

        return ReliabilityCenterUiModel(
            summary = summary,
            setup = setup,
            recent = recent,
            homeRecentLines = prioritizedHomeRecentLines(recent),
            batteryGuideHint = signals.batteryGuideHint
                ?.takeIf { !signals.batteryReady }
                ?.takeIf { it.isNotBlank() }
        )
    }

    private fun prioritizedHomeRecentLines(recent: ReliabilityOverviewUiModel): List<ReliabilityOverviewLineUi> {
        val ranked = recent.lines.sortedWith(
            compareBy<ReliabilityOverviewLineUi>(
                { homeRecentTonePriority(it.tone) },
                { homeRecentKeyPriority(it.key) }
            )
        )
        val selected = mutableListOf<ReliabilityOverviewLineUi>()

        fun addMatching(predicate: (ReliabilityOverviewLineUi) -> Boolean) {
            ranked.forEach { line ->
                if (selected.size >= 3) return
                if (predicate(line) && line !in selected) {
                    selected += line
                }
            }
        }

        addMatching { line ->
            line.tone == ReliabilityOverviewTone.ACTION ||
                line.tone == ReliabilityOverviewTone.CHECK ||
                line.tone == ReliabilityOverviewTone.INFO
        }
        if (selected.size < 2) {
            addMatching { it.tone == ReliabilityOverviewTone.SAFE }
        }
        if (selected.size < 2) {
            addMatching { it.tone == ReliabilityOverviewTone.NEUTRAL }
        }
        if (selected.isEmpty()) {
            return ranked.take(3)
        }
        if (selected.size < 3) {
            addMatching { true }
        }
        return selected.take(3)
    }

    private fun homeRecentTonePriority(tone: ReliabilityOverviewTone): Int {
        return when (tone) {
            ReliabilityOverviewTone.ACTION -> 0
            ReliabilityOverviewTone.CHECK -> 1
            ReliabilityOverviewTone.INFO -> 2
            ReliabilityOverviewTone.SAFE -> 3
            ReliabilityOverviewTone.NEUTRAL -> 4
        }
    }

    private fun homeRecentKeyPriority(key: String): Int {
        return when (key) {
            "watchdog" -> 0
            "restore_post_check" -> 1
            "recovery" -> 2
            "self_test" -> 3
            "nightly_check" -> 4
            "latest_recovery_action" -> 5
            else -> 5
        }
    }
}

