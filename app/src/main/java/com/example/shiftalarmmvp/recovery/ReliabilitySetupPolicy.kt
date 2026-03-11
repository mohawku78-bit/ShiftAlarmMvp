package com.example.shiftalarmmvp.recovery

import android.os.Build

internal data class ReliabilitySetupSignals(
    val exactReady: Boolean,
    val notificationReady: Boolean,
    val batteryReady: Boolean
)

internal enum class ReliabilitySetupIssue {
    EXACT_ALARM,
    NOTIFICATION_PERMISSION,
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
        sdkInt: Int = Build.VERSION.SDK_INT
    ): ReliabilitySetupUiModel {
        val specs = mutableListOf<StepSpec>()

        if (sdkInt >= Build.VERSION_CODES.S) {
            specs += StepSpec(
                issue = ReliabilitySetupIssue.EXACT_ALARM,
                title = "정확 알람 권한",
                ready = signals.exactReady,
                readyStatus = "정확 알람: 준비됨",
                pendingStatus = "정확 알람: 권한 필요",
                detailText = "Android 12 이상에서는 정확한 시간에 깨우려면 정확 알람 권한이 필요합니다.",
                actionLabel = "정확 알람 켜기",
                action = HomeReliabilityAction.OPEN_EXACT_ALARM_SETTINGS
            )
        }

        if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
            specs += StepSpec(
                issue = ReliabilitySetupIssue.NOTIFICATION_PERMISSION,
                title = "알림 권한",
                ready = signals.notificationReady,
                readyStatus = "알림 권한: 허용됨",
                pendingStatus = "알림 권한: 허용 필요",
                detailText = "알림 권한이 꺼져 있으면 알람 표시와 상태 피드백이 제한됩니다.",
                actionLabel = "알림 켜기",
                action = HomeReliabilityAction.REQUEST_NOTIFICATION_PERMISSION
            )
        }

        if (sdkInt >= Build.VERSION_CODES.M) {
            specs += StepSpec(
                issue = ReliabilitySetupIssue.BATTERY_OPTIMIZATION,
                title = "배터리 최적화 예외",
                ready = signals.batteryReady,
                readyStatus = "배터리 최적화: 예외 적용",
                pendingStatus = "배터리 최적화: 예외 권장",
                detailText = "제조사 절전 설정이 알람을 늦출 수 있어 배터리 예외 설정을 권장합니다.",
                actionLabel = "배터리 예외 설정",
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
            totalStepCount == 0 -> "이 기기는 추가 권한 설정이 필요하지 않습니다."
            primaryStep == null -> "권한과 보호 설정이 모두 준비되었습니다."
            else -> "준비 단계 $resolvedStepCount/$totalStepCount 완료 · ${primaryStep.title}부터 진행해 주세요."
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

internal fun ReliabilitySetupStepUi.toHomeReliabilityUiModel(): HomeReliabilityUiModel {
    return HomeReliabilityUiModel(
        level = HomeReliabilityLevel.ACTION,
        statusLabel = "조치 필요",
        reasonText = detailText,
        primaryActionLabel = actionLabel,
        primaryAction = action
    )
}