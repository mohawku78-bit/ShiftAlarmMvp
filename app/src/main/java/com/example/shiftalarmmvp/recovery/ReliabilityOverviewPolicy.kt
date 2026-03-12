package com.example.shiftalarmmvp.recovery

enum class ReliabilityOverviewTone {
    NEUTRAL,
    SAFE,
    INFO,
    CHECK,
    ACTION
}

data class ReliabilityOverviewLineUi(
    val key: String,
    val text: String,
    val tone: ReliabilityOverviewTone
)

data class ReliabilityOverviewUiModel(
    val watchdogLine: ReliabilityOverviewLineUi,
    val restorePostCheckLine: ReliabilityOverviewLineUi,
    val recoveryLine: ReliabilityOverviewLineUi,
    val latestRecoveryActionLine: ReliabilityOverviewLineUi,
    val selfTestLine: ReliabilityOverviewLineUi,
    val nightlyCheckLine: ReliabilityOverviewLineUi
) {
    val lines: List<ReliabilityOverviewLineUi>
        get() = listOf(watchdogLine, restorePostCheckLine, recoveryLine, latestRecoveryActionLine, selfTestLine, nightlyCheckLine)
}

data class ReliabilityOverviewSignals(
    val watchdogStatus: AlarmWatchdogStatus?,
    val restorePostCheckStatus: RestorePostCheckStatus?,
    val recoveryStatus: RescheduleRecoveryState?,
    val latestRecoveryActionText: String?,
    val selfTestStatus: SelfTestStatus?,
    val nightlyCheckStatus: NightlyReliabilityCheckStatus?
)

object ReliabilityOverviewPolicy {
    fun build(
        signals: ReliabilityOverviewSignals,
        texts: RecoveryStrings
    ): ReliabilityOverviewUiModel {
        val watchdogLine = signals.watchdogStatus?.let { status ->
            ReliabilityOverviewLineUi(
                key = "watchdog",
                text = status.overviewText(texts.watchdog),
                tone = when {
                    status.needsAttention() -> ReliabilityOverviewTone.ACTION
                    status.eventType == AlarmWatchdogEventType.CLEAN -> ReliabilityOverviewTone.SAFE
                    else -> ReliabilityOverviewTone.INFO
                }
            )
        } ?: ReliabilityOverviewLineUi(
            key = "watchdog",
            text = texts.overview.watchdogEmpty,
            tone = ReliabilityOverviewTone.NEUTRAL
        )

        val restorePostCheckLine = signals.restorePostCheckStatus?.let { status ->
            ReliabilityOverviewLineUi(
                key = "restore_post_check",
                text = status.overviewText(texts.restorePostCheck),
                tone = if (status.isPending) {
                    ReliabilityOverviewTone.ACTION
                } else {
                    ReliabilityOverviewTone.SAFE
                }
            )
        } ?: ReliabilityOverviewLineUi(
            key = "restore_post_check",
            text = texts.overview.restorePostCheckEmpty,
            tone = ReliabilityOverviewTone.NEUTRAL
        )

        val recoveryLine = signals.recoveryStatus?.let { status ->
            ReliabilityOverviewLineUi(
                key = "recovery",
                text = status.homeOneLineSummary(texts.reschedule),
                tone = when (status.outcome) {
                    RescheduleRecoveryState.Outcome.FULL_RECOVERY -> ReliabilityOverviewTone.SAFE
                    RescheduleRecoveryState.Outcome.DEGRADED_RECOVERY,
                    RescheduleRecoveryState.Outcome.PARTIAL_RECOVERY -> ReliabilityOverviewTone.ACTION
                    RescheduleRecoveryState.Outcome.NO_ACTIVE_ALARMS -> ReliabilityOverviewTone.NEUTRAL
                }
            )
        } ?: ReliabilityOverviewLineUi(
            key = "recovery",
            text = texts.overview.recoveryEmpty,
            tone = ReliabilityOverviewTone.NEUTRAL
        )

        val latestRecoveryActionLine = signals.latestRecoveryActionText
            ?.takeIf { it.isNotBlank() }
            ?.let {
                ReliabilityOverviewLineUi(
                    key = "latest_recovery_action",
                    text = it,
                    tone = ReliabilityOverviewTone.INFO
                )
            }
            ?: ReliabilityOverviewLineUi(
                key = "latest_recovery_action",
                text = texts.overview.latestRecoveryActionEmpty,
                tone = ReliabilityOverviewTone.NEUTRAL
            )

        val selfTestLine = signals.selfTestStatus?.let { status ->
            ReliabilityOverviewLineUi(
                key = "self_test",
                text = status.homeSummary(texts.selfTestStatus),
                tone = when (status.lastEvent) {
                    SelfTestStatus.Event.PASSED -> ReliabilityOverviewTone.SAFE
                    SelfTestStatus.Event.FAILED -> ReliabilityOverviewTone.ACTION
                    SelfTestStatus.Event.SCHEDULED,
                    SelfTestStatus.Event.TRIGGERED -> ReliabilityOverviewTone.INFO
                    SelfTestStatus.Event.UNCERTAIN,
                    SelfTestStatus.Event.CANCELED -> ReliabilityOverviewTone.CHECK
                    SelfTestStatus.Event.NONE -> ReliabilityOverviewTone.NEUTRAL
                }
            )
        } ?: ReliabilityOverviewLineUi(
            key = "self_test",
            text = texts.overview.selfTestEmpty,
            tone = ReliabilityOverviewTone.NEUTRAL
        )

        val nightlyCheckLine = signals.nightlyCheckStatus?.let { status ->
            ReliabilityOverviewLineUi(
                key = "nightly_check",
                text = status.bannerText(texts.nightlyCheck),
                tone = if (status.issueCount > 0) {
                    ReliabilityOverviewTone.CHECK
                } else {
                    ReliabilityOverviewTone.SAFE
                }
            )
        } ?: ReliabilityOverviewLineUi(
            key = "nightly_check",
            text = texts.overview.nightlyCheckEmpty,
            tone = ReliabilityOverviewTone.NEUTRAL
        )

        return ReliabilityOverviewUiModel(
            watchdogLine = watchdogLine,
            restorePostCheckLine = restorePostCheckLine,
            recoveryLine = recoveryLine,
            latestRecoveryActionLine = latestRecoveryActionLine,
            selfTestLine = selfTestLine,
            nightlyCheckLine = nightlyCheckLine
        )
    }
}

