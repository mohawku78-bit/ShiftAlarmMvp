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
    fun evaluate(signals: HomeReliabilitySignals): HomeReliabilityUiModel {
        if (!signals.exactReady) {
            return HomeReliabilityUiModel(
                level = HomeReliabilityLevel.ACTION,
                statusLabel = "조치 필요",
                reasonText = "정확 알람 권한을 켜야 시간 오차를 줄일 수 있습니다.",
                primaryActionLabel = "정확 알람 켜기",
                primaryAction = HomeReliabilityAction.OPEN_EXACT_ALARM_SETTINGS
            )
        }

        if (!signals.notificationReady) {
            return HomeReliabilityUiModel(
                level = HomeReliabilityLevel.ACTION,
                statusLabel = "조치 필요",
                reasonText = "알림 권한이 꺼져 있어 알람 표시가 제한됩니다.",
                primaryActionLabel = "알림 켜기",
                primaryAction = HomeReliabilityAction.REQUEST_NOTIFICATION_PERMISSION
            )
        }

        if (!signals.batteryReady) {
            return HomeReliabilityUiModel(
                level = HomeReliabilityLevel.ACTION,
                statusLabel = "조치 필요",
                reasonText = "배터리 절전이 알람 지연을 만들 수 있습니다.",
                primaryActionLabel = "배터리 예외 설정",
                primaryAction = HomeReliabilityAction.OPEN_BATTERY_SETTINGS
            )
        }

        if (signals.recoveryNeedsAttention) {
            return HomeReliabilityUiModel(
                level = HomeReliabilityLevel.ACTION,
                statusLabel = "조치 필요",
                reasonText = "일부 알람 복구가 누락되어 재예약이 필요합니다.",
                primaryActionLabel = "알람 재예약",
                primaryAction = HomeReliabilityAction.RESCHEDULE_ALARMS
            )
        }

        if (signals.selfTestEvent == SelfTestStatus.Event.FAILED) {
            return HomeReliabilityUiModel(
                level = HomeReliabilityLevel.ACTION,
                statusLabel = "조치 필요",
                reasonText = "최근 2분 테스트가 실패했습니다.",
                primaryActionLabel = "신뢰도 화면 열기",
                primaryAction = HomeReliabilityAction.OPEN_RELIABILITY_CENTER
            )
        }

        if (signals.selfTestNeedsFollowUp) {
            val followUpUi = when (signals.selfTestEvent) {
                SelfTestStatus.Event.SCHEDULED -> Triple(
                    "2분 테스트가 예약되었습니다. 알람이 울리면 끄기를 눌러 확인해 주세요.",
                    "상태 다시 확인",
                    HomeReliabilityAction.REFRESH_STATUS
                )
                SelfTestStatus.Event.TRIGGERED -> Triple(
                    "테스트 알람이 울렸다면 끄기만 누르면 확인이 완료됩니다.",
                    "상태 다시 확인",
                    HomeReliabilityAction.REFRESH_STATUS
                )
                SelfTestStatus.Event.UNCERTAIN -> Triple(
                    "이전 테스트를 확인하지 못했습니다. 2분 테스트를 다시 해 주세요.",
                    "2분 테스트",
                    HomeReliabilityAction.RUN_SELF_TEST
                )
                SelfTestStatus.Event.CANCELED -> Triple(
                    "2분 테스트가 취소되었습니다. 다시 실행해 주세요.",
                    "2분 테스트",
                    HomeReliabilityAction.RUN_SELF_TEST
                )
                else -> Triple(
                    "2분 테스트로 오늘 울림 상태를 확인해 주세요.",
                    "2분 테스트",
                    HomeReliabilityAction.RUN_SELF_TEST
                )
            }
            return HomeReliabilityUiModel(
                level = HomeReliabilityLevel.CHECK,
                statusLabel = "확인 필요",
                reasonText = followUpUi.first,
                primaryActionLabel = followUpUi.second,
                primaryAction = followUpUi.third
            )
        }

        if (signals.nightlyIssueCount > 0) {
            return HomeReliabilityUiModel(
                level = HomeReliabilityLevel.CHECK,
                statusLabel = "확인 필요",
                reasonText = "최근 점검에서 확인 항목이 있어 재점검이 권장됩니다.",
                primaryActionLabel = "재점검",
                primaryAction = HomeReliabilityAction.REFRESH_STATUS
            )
        }

        return HomeReliabilityUiModel(
            level = HomeReliabilityLevel.SAFE,
            statusLabel = "지금 안전함",
            reasonText = "알람 신뢰도 상태가 정상입니다.",
            primaryActionLabel = "재점검",
            primaryAction = HomeReliabilityAction.REFRESH_STATUS
        )
    }
}
