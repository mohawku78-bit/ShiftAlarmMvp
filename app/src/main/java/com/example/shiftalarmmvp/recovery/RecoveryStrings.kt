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
    val watchdogEmpty: String,
    val restorePostCheckEmpty: String,
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
    val outcomeDegraded: String,
    val outcomeFull: String,
    val countZero: String,
    val countExactFormat: String,
    val countFallbackFormat: String,
    val countBlockedFormat: String,
    val homeSummaryFormat: String,
    val oneLineFullFormat: String,
    val oneLineDegradedFormat: String,
    val oneLinePartialFormat: String,
    val oneLineNoActiveFormat: String,
    val actionBoot: String,
    val actionTimezoneChanged: String,
    val actionTimeChanged: String,
    val actionDateChanged: String,
    val actionAppUpdated: String,
    val actionExactPermissionGranted: String,
    val actionExactPermissionLost: String,
    val actionManual: String,
    val actionRestore: String,
    val actionStateChanged: String,
    val actionWithReasonFormat: String,
    val reasonExactPermissionDenied: String,
    val reasonExactSecurityException: String,
    val reasonPlatformFailure: String,
    val reasonNoNextTrigger: String
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

data class RestorePostCheckStrings(
    val homeReasonPending: String,
    val linePendingFormat: String,
    val lineCompletedFormat: String
)

data class AlarmWatchdogStrings(

    val overviewEmpty: String,

    val lineCleanFormat: String,
    val lineMissedFormat: String,
    val lineSelfHealedFormat: String,
    val lineCheckFailedFormat: String,
    val homeReasonIncidentFormat: String,
    val homeReasonCheckFailed: String,
    val homeReasonClean: String,
    val inspectorIssueIncident: String,
    val notificationChannelName: String,
    val notificationChannelDescription: String,
    val notificationTitle: String,
    val notificationTextFormat: String,
    val unknownAlarmLabel: String,
    val causeExactPermissionLost: String,
    val causeInexactFallbackDelay: String,
    val causeBatteryRestriction: String,
    val causeRegistrationLoss: String,
    val causeUnknown: String
)

data class ReliabilityInspectorStrings(
    val issueExactAlarmPermission: String,
    val issueNotificationPermission: String,
    val issueSelfTestFailed: String,
    val issueSelfTestRecheckRecommended: String,
    val issueSelfTestCheckNeeded: String,
    val issueBatteryNotExempt: String,
    val issueRecoveryPartial: String,
    val issueRecoveryDegraded: String,
    val issueWatchdogIncident: String,
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
    val restorePostCheck: RestorePostCheckStrings,
    val nightlyCheck: NightlyCheckStrings,
    val reschedule: RescheduleRecoveryStrings,
    val selfTestStatus: SelfTestStatusStrings,
    val watchdog: AlarmWatchdogStrings,
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
            watchdogEmpty = resources.getString(R.string.recovery_overview_watchdog_empty),
            restorePostCheckEmpty = resources.getString(R.string.recovery_overview_restore_post_check_empty),
            recoveryEmpty = resources.getString(R.string.recovery_overview_recovery_empty),
            latestRecoveryActionEmpty = resources.getString(R.string.recovery_overview_latest_action_empty),
            selfTestEmpty = resources.getString(R.string.recovery_overview_self_test_empty),
            nightlyCheckEmpty = resources.getString(R.string.recovery_overview_nightly_check_empty)
        ),
        restorePostCheck = RestorePostCheckStrings(
            homeReasonPending = resources.getString(R.string.recovery_home_reason_restore_post_check),
            linePendingFormat = resources.getString(R.string.recovery_restore_post_check_line_pending_format),
            lineCompletedFormat = resources.getString(R.string.recovery_restore_post_check_line_completed_format)
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
            outcomeDegraded = resources.getString(R.string.recovery_reschedule_outcome_degraded),
            outcomeFull = resources.getString(R.string.recovery_reschedule_outcome_full),
            countZero = resources.getString(R.string.recovery_reschedule_count_zero),
            countExactFormat = resources.getString(R.string.recovery_reschedule_count_exact_format),
            countFallbackFormat = resources.getString(R.string.recovery_reschedule_count_fallback_format),
            countBlockedFormat = resources.getString(R.string.recovery_reschedule_count_blocked_format),
            homeSummaryFormat = resources.getString(R.string.recovery_reschedule_home_summary_format),
            oneLineFullFormat = resources.getString(R.string.recovery_reschedule_one_line_full_format),
            oneLineDegradedFormat = resources.getString(R.string.recovery_reschedule_one_line_degraded_format),
            oneLinePartialFormat = resources.getString(R.string.recovery_reschedule_one_line_partial_format),
            oneLineNoActiveFormat = resources.getString(R.string.recovery_reschedule_one_line_no_active_format),
            actionBoot = resources.getString(R.string.recovery_reschedule_action_boot),
            actionTimezoneChanged = resources.getString(R.string.recovery_reschedule_action_timezone_changed),
            actionTimeChanged = resources.getString(R.string.recovery_reschedule_action_time_changed),
            actionDateChanged = resources.getString(R.string.recovery_reschedule_action_date_changed),
            actionAppUpdated = resources.getString(R.string.recovery_reschedule_action_app_updated),
            actionExactPermissionGranted = resources.getString(R.string.recovery_reschedule_action_exact_permission_granted),
            actionExactPermissionLost = resources.getString(R.string.recovery_reschedule_action_exact_permission_lost),
            actionManual = resources.getString(R.string.recovery_reschedule_action_manual),
            actionRestore = resources.getString(R.string.recovery_reschedule_action_restore),
            actionStateChanged = resources.getString(R.string.recovery_reschedule_action_state_changed),
            actionWithReasonFormat = resources.getString(R.string.recovery_reschedule_action_with_reason_format),
            reasonExactPermissionDenied = resources.getString(R.string.recovery_reschedule_reason_exact_permission_denied),
            reasonExactSecurityException = resources.getString(R.string.recovery_reschedule_reason_exact_security_exception),
            reasonPlatformFailure = resources.getString(R.string.recovery_reschedule_reason_platform_failure),
            reasonNoNextTrigger = resources.getString(R.string.recovery_reschedule_reason_no_next_trigger)
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
        watchdog = AlarmWatchdogStrings(
            overviewEmpty = resources.getString(R.string.recovery_watchdog_overview_empty),
            lineCleanFormat = resources.getString(R.string.recovery_watchdog_line_clean_format),
            lineMissedFormat = resources.getString(R.string.recovery_watchdog_line_missed_format),
            lineSelfHealedFormat = resources.getString(R.string.recovery_watchdog_line_self_healed_format),
            lineCheckFailedFormat = resources.getString(R.string.recovery_watchdog_line_check_failed_format),
            homeReasonIncidentFormat = resources.getString(R.string.recovery_watchdog_home_reason_incident_format),
            homeReasonCheckFailed = resources.getString(R.string.recovery_watchdog_home_reason_check_failed),
            homeReasonClean = resources.getString(R.string.recovery_watchdog_home_reason_clean),
            inspectorIssueIncident = resources.getString(R.string.recovery_watchdog_inspector_issue_incident),
            notificationChannelName = resources.getString(R.string.recovery_watchdog_notification_channel_name),
            notificationChannelDescription = resources.getString(R.string.recovery_watchdog_notification_channel_description),
            notificationTitle = resources.getString(R.string.recovery_watchdog_notification_title),
            notificationTextFormat = resources.getString(R.string.recovery_watchdog_notification_text_format),
            unknownAlarmLabel = resources.getString(R.string.recovery_watchdog_unknown_alarm_label),
            causeExactPermissionLost = resources.getString(R.string.recovery_watchdog_cause_exact_permission_lost),
            causeInexactFallbackDelay = resources.getString(R.string.recovery_watchdog_cause_inexact_fallback_delay),
            causeBatteryRestriction = resources.getString(R.string.recovery_watchdog_cause_battery_restriction),
            causeRegistrationLoss = resources.getString(R.string.recovery_watchdog_cause_registration_loss),
            causeUnknown = resources.getString(R.string.recovery_watchdog_cause_unknown)
        ),
        inspector = ReliabilityInspectorStrings(
            issueExactAlarmPermission = resources.getString(R.string.main_follow_up_exact_alarm_permission),
            issueNotificationPermission = resources.getString(R.string.main_follow_up_notification_permission),
            issueSelfTestFailed = resources.getString(R.string.recovery_inspector_issue_self_test_failed),
            issueSelfTestRecheckRecommended = resources.getString(R.string.recovery_inspector_issue_self_test_recheck_recommended),
            issueSelfTestCheckNeeded = resources.getString(R.string.recovery_inspector_issue_self_test_check_needed),
            issueBatteryNotExempt = resources.getString(R.string.recovery_inspector_issue_battery_not_exempt),
            issueRecoveryPartial = resources.getString(R.string.recovery_inspector_issue_recovery_partial),
            issueRecoveryDegraded = resources.getString(R.string.recovery_inspector_issue_recovery_degraded),
            issueWatchdogIncident = resources.getString(R.string.recovery_watchdog_inspector_issue_incident),
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



