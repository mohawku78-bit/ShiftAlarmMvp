package com.example.shiftalarmmvp.recovery

internal fun testRecoveryStrings(): RecoveryStrings {
    return RecoveryStrings(
        home = HomeReliabilityStrings(
            statusActionNeeded = "조치 필요",
            statusCheckNeeded = "확인 필요",
            statusSafe = "지금 안전함",
            reasonRecoveryNeedsReschedule = "일부 알람 복구가 누락되어 재예약이 필요합니다.",
            reasonSelfTestFailed = "최근 2분 테스트가 실패했습니다.",
            actionOpenCenter = "신뢰도 화면 열기",
            actionRefreshStatus = "상태 다시 확인",
            actionRunSelfTest = "2분 테스트",
            actionRecheck = "재점검",
            followUpScheduled = "2분 테스트가 예약되었습니다. 알람이 울리면 끄기를 눌러 확인해 주세요.",
            followUpTriggered = "테스트 알람이 울렸다면 끄기만 누르면 확인이 완료됩니다.",
            followUpUncertain = "이전 테스트를 확인하지 못했습니다. 2분 테스트를 다시 해 주세요.",
            followUpCanceled = "2분 테스트가 취소되었습니다. 다시 실행해 주세요.",
            followUpDefault = "2분 테스트로 오늘 울림 상태를 확인해 주세요.",
            reasonNightlyIssue = "최근 점검에서 확인 항목이 있어 재점검이 권장됩니다.",
            reasonSafe = "알람 신뢰도 상태가 정상입니다."
        ),
        setup = ReliabilitySetupStrings(
            statusActionNeeded = "조치 필요",
            exactAlarmTitle = "정확 알람 권한",
            exactAlarmReadyStatus = "정확 알람: 준비됨",
            exactAlarmPendingStatus = "정확 알람: 권한 필요",
            exactAlarmDetail = "Android 12 이상에서는 정확한 시간에 깨우려면 정확 알람 접근이 필요합니다. 설정 화면이 안 보이면 앱 정보 > 알람 및 리마인더를 확인해 주세요.",
            exactAlarmAction = "정확 알람 켜기",
            notificationTitle = "알림 권한",
            notificationReadyStatus = "알림 권한: 허용됨",
            notificationPendingStatus = "알림 권한: 허용 필요",
            notificationDetail = "알림 권한이 꺼져 있으면 알람 표시와 상태 피드백이 제한됩니다.",
            notificationAction = "알림 켜기",
            alarmRegistrationTitle = "다음 알람 등록",
            alarmRegistrationReadyStatus = "다음 알람: 등록됨",
            alarmRegistrationPendingStatus = "다음 알람: 재예약 필요",
            alarmRegistrationDetail = "다음 울림이 시스템 알람에 보이지 않습니다. 알람 재예약 후 다시 확인해 주세요.",
            alarmRegistrationAction = "알람 재예약",
            batteryTitle = "배터리 최적화 예외",
            batteryReadyStatus = "배터리 최적화: 예외 적용",
            batteryPendingStatus = "배터리 최적화: 예외 권장",
            batteryDetail = "제조사 절전 설정이 알람을 늦출 수 있어 배터리 예외 설정을 권장합니다.",
            batteryAction = "배터리 예외 설정",
            summaryNoExtraSteps = "이 기기는 추가 권한 설정이 필요하지 않습니다.",
            summaryAllReady = "권한과 보호 설정 준비 완료",
            summaryProgressFormat = "준비 단계 %1\$d/%2\$d 완료 · %3\$s부터 진행해 주세요."
        ),
        overview = ReliabilityOverviewStrings(
            recoveryEmpty = "복구 기록 없음",
            latestRecoveryActionEmpty = "최근 복구 조치 없음",
            selfTestEmpty = "2분 테스트 이력 없음",
            nightlyCheckEmpty = "최근 점검 없음 · 재점검으로 지금 확인"
        ),
        nightlyCheck = NightlyCheckStrings(
            stateCheckNeeded = "확인 필요",
            stateGood = "양호",
            bannerFormat = "최근 점검 %1\$s · %2\$s · %3\$s",
            summaryNone = "최근 점검 기록 없음"
        ),
        reschedule = RescheduleRecoveryStrings(
            outcomeNoActive = "활성 알람 없음",
            outcomePartial = "일부 미복구",
            outcomeFull = "정상 복구",
            countZero = "0/0",
            countFormat = "%1\$d/%2\$d",
            homeSummaryFormat = "자동 복구 %1\$s · %2\$s · %3\$s (%4\$s)",
            oneLineFullFormat = "%1\$s %2\$s 후 알람 %3\$s를 정상 복구했습니다.",
            oneLinePartialFormat = "%1\$s %2\$s 후 알람 %3\$s만 복구되어 재예약이 필요합니다.",
            oneLineNoActiveFormat = "%1\$s %2\$s 시점에는 활성 알람이 없어 복구가 필요 없었습니다.",
            actionBoot = "부팅",
            actionTimezoneChanged = "시간대 변경",
            actionTimeChanged = "시간 변경",
            actionDateChanged = "날짜 변경",
            actionAppUpdated = "앱 업데이트",
            actionStateChanged = "상태 변경"
        ),
        selfTestStatus = SelfTestStatusStrings(
            statusNone = "테스트 이력 없음",
            statusScheduled = "테스트 예약됨",
            statusTriggered = "알림 감지됨",
            statusPassed = "알림 확인 완료",
            statusUncertain = "확인 못함",
            statusFailed = "실패 기록됨",
            statusCanceled = "테스트 취소됨",
            detailNone = "없음",
            homeSummaryFormat = "2분 테스트 %1\$s · %2\$s"
        ),
        inspector = ReliabilityInspectorStrings(
            issueExactAlarmPermission = "정확 알람 권한",
            issueNotificationPermission = "알림 권한",
            issueSelfTestFailed = "2분 테스트 실패",
            issueSelfTestRecheckRecommended = "2분 테스트 재확인 권장",
            issueSelfTestCheckNeeded = "2분 테스트 확인 필요",
            issueBatteryNotExempt = "배터리 예외 미설정",
            issueRecoveryPartial = "일부 알람 미복구",
            summaryReady = "문제 없이 준비됨",
            summaryOverflowFormat = "%1\$s 외 %2\$d건"
        ),
        nightlyNotification = NightlyNotificationStrings(
            channelName = "취침 전 자동 점검",
            channelDescription = "취침 전에 알람 신뢰도를 자동 점검하고 문제가 있으면 알려줍니다.",
            notificationTitleCheckNeeded = "취침 전 자동 점검: 확인 필요",
            notificationBigTextFormat = "%1\$s\n앱에서 추천 버튼으로 바로 조치하세요."
        )
    )
}