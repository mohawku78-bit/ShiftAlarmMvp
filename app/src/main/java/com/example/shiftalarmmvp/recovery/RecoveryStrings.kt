package com.example.shiftalarmmvp.recovery

import android.content.res.Resources
import com.example.shiftalarmmvp.R

data class HomeReliabilityStrings(
    val statusActionNeeded: String,
    val statusCheckNeeded: String,
    val statusSafe: String,
    val reasonRecoveryNeedsReschedule: String,
    val reasonSelfTestFailed: String,
    val actionOpenCenter: String,
    val actionRefreshStatus: String,
    val actionRunSelfTest: String,
    val actionRecheck: String,
    val followUpScheduled: String,
    val followUpTriggered: String,
    val followUpUncertain: String,
    val followUpCanceled: String,
    val followUpDefault: String,
    val reasonNightlyIssue: String,
    val reasonSafe: String
)

data class ReliabilitySetupStrings(
    val statusActionNeeded: String,
    val exactAlarmTitle: String,
    val exactAlarmReadyStatus: String,
    val exactAlarmPendingStatus: String,
    val exactAlarmDetail: String,
    val exactAlarmAction: String,
    val notificationTitle: String,
    val notificationReadyStatus: String,
    val notificationPendingStatus: String,
    val notificationDetail: String,
    val notificationAction: String,
    val alarmRegistrationTitle: String,
    val alarmRegistrationReadyStatus: String,
    val alarmRegistrationPendingStatus: String,
    val alarmRegistrationDetail: String,
    val alarmRegistrationAction: String,
    val batteryTitle: String,
    val batteryReadyStatus: String,
    val batteryPendingStatus: String,
    val batteryDetail: String,
    val batteryAction: String,
    val summaryNoExtraSteps: String,
    val summaryAllReady: String,
    val summaryProgressFormat: String
)

data class ReliabilityOverviewStrings(
    val recoveryEmpty: String,
    val latestRecoveryActionEmpty: String,
    val selfTestEmpty: String,
    val nightlyCheckEmpty: String
)

data class NightlyCheckStrings(
    val stateCheckNeeded: String,
    val stateGood: String,
    val bannerFormat: String,
    val summaryNone: String
)

data class RescheduleRecoveryStrings(
    val outcomeNoActive: String,
    val outcomePartial: String,
    val outcomeFull: String,
    val countZero: String,
    val countFormat: String,
    val homeSummaryFormat: String,
    val oneLineFullFormat: String,
    val oneLinePartialFormat: String,
    val oneLineNoActiveFormat: String,
    val actionBoot: String,
    val actionTimezoneChanged: String,
    val actionTimeChanged: String,
    val actionDateChanged: String,
    val actionAppUpdated: String,
    val actionStateChanged: String
)

data class SelfTestStatusStrings(
    val statusNone: String,
    val statusScheduled: String,
    val statusTriggered: String,
    val statusPassed: String,
    val statusUncertain: String,
    val statusFailed: String,
    val statusCanceled: String,
    val detailNone: String,
    val homeSummaryFormat: String
)

data class ReliabilityInspectorStrings(
    val issueExactAlarmPermission: String,
    val issueNotificationPermission: String,
    val issueSelfTestFailed: String,
    val issueSelfTestRecheckRecommended: String,
    val issueSelfTestCheckNeeded: String,
    val issueBatteryNotExempt: String,
    val issueRecoveryPartial: String,
    val summaryReady: String,
    val summaryOverflowFormat: String
)

data class NightlyNotificationStrings(
    val channelName: String,
    val channelDescription: String,
    val notificationTitleCheckNeeded: String,
    val notificationBigTextFormat: String
)

data class RecoveryStrings(
    val home: HomeReliabilityStrings,
    val setup: ReliabilitySetupStrings,
    val overview: ReliabilityOverviewStrings,
    val nightlyCheck: NightlyCheckStrings,
    val reschedule: RescheduleRecoveryStrings,
    val selfTestStatus: SelfTestStatusStrings,
    val inspector: ReliabilityInspectorStrings,
    val nightlyNotification: NightlyNotificationStrings
)

fun recoveryStrings(resources: Resources): RecoveryStrings {
    return RecoveryStrings(
        home = HomeReliabilityStrings(
            statusActionNeeded = resources.getString(R.string.recovery_status_action_needed),
            statusCheckNeeded = resources.getString(R.string.recovery_status_check_needed),
            statusSafe = resources.getString(R.string.recovery_status_safe),
            reasonRecoveryNeedsReschedule = resources.getString(R.string.recovery_home_reason_recovery_needs_reschedule),
            reasonSelfTestFailed = resources.getString(R.string.recovery_home_reason_self_test_failed),
            actionOpenCenter = resources.getString(R.string.recovery_home_action_open_center),
            actionRefreshStatus = resources.getString(R.string.recovery_home_action_refresh_status),
            actionRunSelfTest = resources.getString(R.string.recovery_home_action_run_self_test),
            actionRecheck = resources.getString(R.string.recovery_home_action_recheck),
            followUpScheduled = resources.getString(R.string.recovery_home_follow_up_scheduled),
            followUpTriggered = resources.getString(R.string.recovery_home_follow_up_triggered),
            followUpUncertain = resources.getString(R.string.recovery_home_follow_up_uncertain),
            followUpCanceled = resources.getString(R.string.recovery_home_follow_up_canceled),
            followUpDefault = resources.getString(R.string.recovery_home_follow_up_default),
            reasonNightlyIssue = resources.getString(R.string.recovery_home_reason_nightly_issue),
            reasonSafe = resources.getString(R.string.recovery_home_reason_safe)
        ),
        setup = ReliabilitySetupStrings(
            statusActionNeeded = resources.getString(R.string.recovery_status_action_needed),
            exactAlarmTitle = resources.getString(R.string.main_follow_up_exact_alarm_permission),
            exactAlarmReadyStatus = resources.getString(R.string.recovery_setup_exact_alarm_ready),
            exactAlarmPendingStatus = resources.getString(R.string.recovery_setup_exact_alarm_pending),
            exactAlarmDetail = resources.getString(R.string.recovery_setup_exact_alarm_detail),
            exactAlarmAction = resources.getString(R.string.recovery_setup_exact_alarm_action),
            notificationTitle = resources.getString(R.string.main_follow_up_notification_permission),
            notificationReadyStatus = resources.getString(R.string.recovery_setup_notification_ready),
            notificationPendingStatus = resources.getString(R.string.recovery_setup_notification_pending),
            notificationDetail = resources.getString(R.string.recovery_setup_notification_detail),
            notificationAction = resources.getString(R.string.recovery_setup_notification_action),
            alarmRegistrationTitle = resources.getString(R.string.recovery_setup_alarm_registration_title),
            alarmRegistrationReadyStatus = resources.getString(R.string.recovery_setup_alarm_registration_ready),
            alarmRegistrationPendingStatus = resources.getString(R.string.recovery_setup_alarm_registration_pending),
            alarmRegistrationDetail = resources.getString(R.string.recovery_setup_alarm_registration_detail),
            alarmRegistrationAction = resources.getString(R.string.editor_reschedule_alarms),
            batteryTitle = resources.getString(R.string.main_follow_up_battery_optimization),
            batteryReadyStatus = resources.getString(R.string.recovery_setup_battery_ready),
            batteryPendingStatus = resources.getString(R.string.recovery_setup_battery_pending),
            batteryDetail = resources.getString(R.string.recovery_setup_battery_detail),
            batteryAction = resources.getString(R.string.recovery_setup_battery_action),
            summaryNoExtraSteps = resources.getString(R.string.recovery_setup_summary_no_extra_steps),
            summaryAllReady = resources.getString(R.string.main_follow_up_ready),
            summaryProgressFormat = resources.getString(R.string.recovery_setup_summary_progress_format)
        ),
        overview = ReliabilityOverviewStrings(
            recoveryEmpty = resources.getString(R.string.recovery_overview_recovery_empty),
            latestRecoveryActionEmpty = resources.getString(R.string.recovery_overview_latest_action_empty),
            selfTestEmpty = resources.getString(R.string.recovery_overview_self_test_empty),
            nightlyCheckEmpty = resources.getString(R.string.recovery_overview_nightly_check_empty)
        ),
        nightlyCheck = NightlyCheckStrings(
            stateCheckNeeded = resources.getString(R.string.recovery_nightly_state_check_needed),
            stateGood = resources.getString(R.string.recovery_nightly_state_good),
            bannerFormat = resources.getString(R.string.recovery_nightly_banner_format),
            summaryNone = resources.getString(R.string.main_reliability_summary_none)
        ),
        reschedule = RescheduleRecoveryStrings(
            outcomeNoActive = resources.getString(R.string.recovery_reschedule_outcome_no_active),
            outcomePartial = resources.getString(R.string.recovery_reschedule_outcome_partial),
            outcomeFull = resources.getString(R.string.recovery_reschedule_outcome_full),
            countZero = resources.getString(R.string.recovery_reschedule_count_zero),
            countFormat = resources.getString(R.string.recovery_reschedule_count_format),
            homeSummaryFormat = resources.getString(R.string.recovery_reschedule_home_summary_format),
            oneLineFullFormat = resources.getString(R.string.recovery_reschedule_one_line_full_format),
            oneLinePartialFormat = resources.getString(R.string.recovery_reschedule_one_line_partial_format),
            oneLineNoActiveFormat = resources.getString(R.string.recovery_reschedule_one_line_no_active_format),
            actionBoot = resources.getString(R.string.recovery_reschedule_action_boot),
            actionTimezoneChanged = resources.getString(R.string.recovery_reschedule_action_timezone_changed),
            actionTimeChanged = resources.getString(R.string.recovery_reschedule_action_time_changed),
            actionDateChanged = resources.getString(R.string.recovery_reschedule_action_date_changed),
            actionAppUpdated = resources.getString(R.string.recovery_reschedule_action_app_updated),
            actionStateChanged = resources.getString(R.string.recovery_reschedule_action_state_changed)
        ),
        selfTestStatus = SelfTestStatusStrings(
            statusNone = resources.getString(R.string.recovery_self_test_status_none),
            statusScheduled = resources.getString(R.string.recovery_self_test_status_scheduled),
            statusTriggered = resources.getString(R.string.recovery_self_test_status_triggered),
            statusPassed = resources.getString(R.string.recovery_self_test_status_passed),
            statusUncertain = resources.getString(R.string.recovery_self_test_status_uncertain),
            statusFailed = resources.getString(R.string.recovery_self_test_status_failed),
            statusCanceled = resources.getString(R.string.recovery_self_test_status_canceled),
            detailNone = resources.getString(R.string.common_none),
            homeSummaryFormat = resources.getString(R.string.recovery_self_test_home_summary_format)
        ),
        inspector = ReliabilityInspectorStrings(
            issueExactAlarmPermission = resources.getString(R.string.main_follow_up_exact_alarm_permission),
            issueNotificationPermission = resources.getString(R.string.main_follow_up_notification_permission),
            issueSelfTestFailed = resources.getString(R.string.recovery_inspector_issue_self_test_failed),
            issueSelfTestRecheckRecommended = resources.getString(R.string.recovery_inspector_issue_self_test_recheck_recommended),
            issueSelfTestCheckNeeded = resources.getString(R.string.recovery_inspector_issue_self_test_check_needed),
            issueBatteryNotExempt = resources.getString(R.string.recovery_inspector_issue_battery_not_exempt),
            issueRecoveryPartial = resources.getString(R.string.recovery_inspector_issue_recovery_partial),
            summaryReady = resources.getString(R.string.recovery_inspector_summary_ready),
            summaryOverflowFormat = resources.getString(R.string.recovery_inspector_summary_overflow_format)
        ),
        nightlyNotification = NightlyNotificationStrings(
            channelName = resources.getString(R.string.recovery_nightly_notification_channel_name),
            channelDescription = resources.getString(R.string.recovery_nightly_notification_channel_description),
            notificationTitleCheckNeeded = resources.getString(R.string.recovery_nightly_notification_title_check_needed),
            notificationBigTextFormat = resources.getString(R.string.recovery_nightly_notification_big_text_format)
        )
    )
}

data class AlarmReceiverStrings(
    val ringStartSnoozeFormat: String,
    val ringStartRegular: String,
    val testAlarmFallback: String
)

fun alarmReceiverStrings(resources: Resources): AlarmReceiverStrings {
    return AlarmReceiverStrings(
        ringStartSnoozeFormat = resources.getString(R.string.alarm_receiver_ring_start_snooze_format),
        ringStartRegular = resources.getString(R.string.alarm_receiver_ring_start_regular),
        testAlarmFallback = resources.getString(R.string.self_test_default_alarm_label)
    )
}

data class AlarmServiceStrings(
    val channelName: String,
    val logStopUser: String,
    val logSnoozeDetailFormat: String,
    val logOneMoreDetailFormat: String,
    val selfTestSubText: String
)

fun alarmServiceStrings(resources: Resources): AlarmServiceStrings {
    return AlarmServiceStrings(
        channelName = resources.getString(R.string.alarm_service_channel_name),
        logStopUser = resources.getString(R.string.alarm_service_log_stop_user),
        logSnoozeDetailFormat = resources.getString(R.string.alarm_service_log_snooze_detail_format),
        logOneMoreDetailFormat = resources.getString(R.string.alarm_service_log_one_more_detail_format),
        selfTestSubText = resources.getString(R.string.alarm_service_self_test_subtext)
    )
}