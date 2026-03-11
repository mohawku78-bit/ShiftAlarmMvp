package com.example.shiftalarmmvp.recovery

enum class HomeReliabilityLevel {
    SAFE,
    CHECK,
    ACTION
}

enum class HomeReliabilityAction {
    OPEN_EXACT_ALARM_SETTINGS,
    REQUEST_NOTIFICATION_PERMISSION,
    OPEN_BATTERY_SETTINGS,
    RESCHEDULE_ALARMS,
    OPEN_RELIABILITY_CENTER,
    RUN_SELF_TEST,
    REFRESH_STATUS
}

data class HomeReliabilitySignals(
    val exactReady: Boolean,
    val notificationReady: Boolean,
    val batteryReady: Boolean,
    val nextAlarmRegisteredReady: Boolean,
    val shouldCheckAlarmRegistration: Boolean,
    val recoveryNeedsAttention: Boolean,
    val selfTestEvent: SelfTestStatus.Event?,
    val selfTestNeedsFollowUp: Boolean,
    val nightlyIssueCount: Int
)

data class HomeReliabilityUiModel(
    val level: HomeReliabilityLevel,
    val statusLabel: String,
    val reasonText: String,
    val primaryActionLabel: String,
    val primaryAction: HomeReliabilityAction
)

object HomeReliabilityPolicy {
    fun evaluate(
        signals: HomeReliabilitySignals,
        texts: RecoveryStrings
    ): HomeReliabilityUiModel {
        val setupUi = ReliabilitySetupPolicy.build(
            signals = ReliabilitySetupSignals(
                exactReady = signals.exactReady,
                notificationReady = signals.notificationReady,
                batteryReady = signals.batteryReady,
                nextAlarmRegisteredReady = signals.nextAlarmRegisteredReady,
                shouldCheckAlarmRegistration = signals.shouldCheckAlarmRegistration
            ),
            texts = texts.setup
        )
        setupUi.primaryStep?.let { return it.toHomeReliabilityUiModel(texts.setup) }

        if (signals.recoveryNeedsAttention) {
            return HomeReliabilityUiModel(
                level = HomeReliabilityLevel.ACTION,
                statusLabel = texts.home.statusActionNeeded,
                reasonText = texts.home.reasonRecoveryNeedsReschedule,
                primaryActionLabel = texts.setup.alarmRegistrationAction,
                primaryAction = HomeReliabilityAction.RESCHEDULE_ALARMS
            )
        }

        if (signals.selfTestEvent == SelfTestStatus.Event.FAILED) {
            return HomeReliabilityUiModel(
                level = HomeReliabilityLevel.ACTION,
                statusLabel = texts.home.statusActionNeeded,
                reasonText = texts.home.reasonSelfTestFailed,
                primaryActionLabel = texts.home.actionOpenCenter,
                primaryAction = HomeReliabilityAction.OPEN_RELIABILITY_CENTER
            )
        }

        if (signals.selfTestNeedsFollowUp) {
            val followUpUi = when (signals.selfTestEvent) {
                SelfTestStatus.Event.SCHEDULED -> Triple(
                    texts.home.followUpScheduled,
                    texts.home.actionRefreshStatus,
                    HomeReliabilityAction.REFRESH_STATUS
                )
                SelfTestStatus.Event.TRIGGERED -> Triple(
                    texts.home.followUpTriggered,
                    texts.home.actionRefreshStatus,
                    HomeReliabilityAction.REFRESH_STATUS
                )
                SelfTestStatus.Event.UNCERTAIN -> Triple(
                    texts.home.followUpUncertain,
                    texts.home.actionRunSelfTest,
                    HomeReliabilityAction.RUN_SELF_TEST
                )
                SelfTestStatus.Event.CANCELED -> Triple(
                    texts.home.followUpCanceled,
                    texts.home.actionRunSelfTest,
                    HomeReliabilityAction.RUN_SELF_TEST
                )
                else -> Triple(
                    texts.home.followUpDefault,
                    texts.home.actionRunSelfTest,
                    HomeReliabilityAction.RUN_SELF_TEST
                )
            }
            return HomeReliabilityUiModel(
                level = HomeReliabilityLevel.CHECK,
                statusLabel = texts.home.statusCheckNeeded,
                reasonText = followUpUi.first,
                primaryActionLabel = followUpUi.second,
                primaryAction = followUpUi.third
            )
        }

        if (signals.nightlyIssueCount > 0) {
            return HomeReliabilityUiModel(
                level = HomeReliabilityLevel.CHECK,
                statusLabel = texts.home.statusCheckNeeded,
                reasonText = texts.home.reasonNightlyIssue,
                primaryActionLabel = texts.home.actionRecheck,
                primaryAction = HomeReliabilityAction.REFRESH_STATUS
            )
        }

        return HomeReliabilityUiModel(
            level = HomeReliabilityLevel.SAFE,
            statusLabel = texts.home.statusSafe,
            reasonText = texts.home.reasonSafe,
            primaryActionLabel = texts.home.actionRecheck,
            primaryAction = HomeReliabilityAction.REFRESH_STATUS
        )
    }
}