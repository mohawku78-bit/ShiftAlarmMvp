package com.example.shiftalarmmvp.recovery

import android.os.Build

internal data class ReliabilitySetupSignals(
    val exactReady: Boolean,
    val notificationReady: Boolean,
    val batteryReady: Boolean,
    val nextAlarmRegisteredReady: Boolean = true,
    val shouldCheckAlarmRegistration: Boolean = false
)

internal enum class ReliabilitySetupIssue {
    EXACT_ALARM,
    NOTIFICATION_PERMISSION,
    ALARM_REGISTRATION,
    BATTERY_OPTIMIZATION
}

internal data class ReliabilitySetupStepUi(
    val issue: ReliabilitySetupIssue,
    val stepNumber: Int,
    val totalStepCount: Int,
    val title: String,
    val statusText: String,
    val detailText: String,
    val actionLabel: String,
    val action: HomeReliabilityAction,
    val isResolved: Boolean
)

internal data class ReliabilitySetupUiModel(
    val steps: List<ReliabilitySetupStepUi>,
    val primaryStep: ReliabilitySetupStepUi?,
    val resolvedStepCount: Int,
    val totalStepCount: Int,
    val summaryText: String
) {
    val allReady: Boolean
        get() = primaryStep == null

    val unresolvedSteps: List<ReliabilitySetupStepUi>
        get() = steps.filterNot { it.isResolved }
}

internal object ReliabilitySetupPolicy {
    fun build(
        signals: ReliabilitySetupSignals,
        texts: ReliabilitySetupStrings,
        sdkInt: Int = Build.VERSION.SDK_INT
    ): ReliabilitySetupUiModel {
        val specs = mutableListOf<StepSpec>()

        if (sdkInt >= Build.VERSION_CODES.S) {
            specs += StepSpec(
                issue = ReliabilitySetupIssue.EXACT_ALARM,
                title = texts.exactAlarmTitle,
                ready = signals.exactReady,
                readyStatus = texts.exactAlarmReadyStatus,
                pendingStatus = texts.exactAlarmPendingStatus,
                detailText = texts.exactAlarmDetail,
                actionLabel = texts.exactAlarmAction,
                action = HomeReliabilityAction.OPEN_EXACT_ALARM_SETTINGS
            )
        }

        if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
            specs += StepSpec(
                issue = ReliabilitySetupIssue.NOTIFICATION_PERMISSION,
                title = texts.notificationTitle,
                ready = signals.notificationReady,
                readyStatus = texts.notificationReadyStatus,
                pendingStatus = texts.notificationPendingStatus,
                detailText = texts.notificationDetail,
                actionLabel = texts.notificationAction,
                action = HomeReliabilityAction.REQUEST_NOTIFICATION_PERMISSION
            )
        }

        if (signals.shouldCheckAlarmRegistration) {
            specs += StepSpec(
                issue = ReliabilitySetupIssue.ALARM_REGISTRATION,
                title = texts.alarmRegistrationTitle,
                ready = signals.nextAlarmRegisteredReady,
                readyStatus = texts.alarmRegistrationReadyStatus,
                pendingStatus = texts.alarmRegistrationPendingStatus,
                detailText = texts.alarmRegistrationDetail,
                actionLabel = texts.alarmRegistrationAction,
                action = HomeReliabilityAction.RESCHEDULE_ALARMS
            )
        }

        if (sdkInt >= Build.VERSION_CODES.M) {
            specs += StepSpec(
                issue = ReliabilitySetupIssue.BATTERY_OPTIMIZATION,
                title = texts.batteryTitle,
                ready = signals.batteryReady,
                readyStatus = texts.batteryReadyStatus,
                pendingStatus = texts.batteryPendingStatus,
                detailText = texts.batteryDetail,
                actionLabel = texts.batteryAction,
                action = HomeReliabilityAction.OPEN_BATTERY_SETTINGS
            )
        }

        val totalStepCount = specs.size
        val steps = specs.mapIndexed { index, spec ->
            ReliabilitySetupStepUi(
                issue = spec.issue,
                stepNumber = index + 1,
                totalStepCount = totalStepCount,
                title = spec.title,
                statusText = if (spec.ready) spec.readyStatus else spec.pendingStatus,
                detailText = spec.detailText,
                actionLabel = spec.actionLabel,
                action = spec.action,
                isResolved = spec.ready
            )
        }

        val primaryStep = steps.firstOrNull { !it.isResolved }
        val resolvedStepCount = steps.count { it.isResolved }
        val summaryText = when {
            totalStepCount == 0 -> texts.summaryNoExtraSteps
            primaryStep == null -> texts.summaryAllReady
            else -> texts.summaryProgressFormat.format(
                resolvedStepCount,
                totalStepCount,
                primaryStep.title
            )
        }

        return ReliabilitySetupUiModel(
            steps = steps,
            primaryStep = primaryStep,
            resolvedStepCount = resolvedStepCount,
            totalStepCount = totalStepCount,
            summaryText = summaryText
        )
    }

    private data class StepSpec(
        val issue: ReliabilitySetupIssue,
        val title: String,
        val ready: Boolean,
        val readyStatus: String,
        val pendingStatus: String,
        val detailText: String,
        val actionLabel: String,
        val action: HomeReliabilityAction
    )
}

internal fun ReliabilitySetupStepUi.toHomeReliabilityUiModel(
    texts: ReliabilitySetupStrings
): HomeReliabilityUiModel {
    return HomeReliabilityUiModel(
        level = HomeReliabilityLevel.ACTION,
        statusLabel = texts.statusActionNeeded,
        reasonText = detailText,
        primaryActionLabel = actionLabel,
        primaryAction = action
    )
}