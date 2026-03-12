package com.example.shiftalarmmvp.recovery

import android.content.Context
import android.content.Intent
import com.example.shiftalarmmvp.data.AlarmDatabase
import com.example.shiftalarmmvp.data.toDomain
import com.example.shiftalarmmvp.scheduler.AlarmScheduleFailureReason
import com.example.shiftalarmmvp.scheduler.AlarmScheduleMode
import com.example.shiftalarmmvp.scheduler.AlarmScheduleResult
import com.example.shiftalarmmvp.scheduler.AlarmScheduler
import com.example.shiftalarmmvp.scheduler.NightlyReliabilityCheckScheduler
import com.example.shiftalarmmvp.service.AlarmRingingService

internal enum class RescheduleTrigger(val storageAction: String) {
    BOOT_COMPLETED(Intent.ACTION_BOOT_COMPLETED),
    TIME_CHANGED(Intent.ACTION_TIME_CHANGED),
    DATE_CHANGED(Intent.ACTION_DATE_CHANGED),
    TIMEZONE_CHANGED(Intent.ACTION_TIMEZONE_CHANGED),
    APP_UPDATED(Intent.ACTION_MY_PACKAGE_REPLACED),
    EXACT_PERMISSION_GRANTED("exact_permission_granted"),
    EXACT_PERMISSION_LOST("exact_permission_lost"),
    MANUAL("manual_reschedule"),
    RESTORE("restore_completed")
}

internal enum class ExactAlarmPermissionTransition {
    NONE,
    GAINED,
    LOST
}

internal fun rescheduleTriggerFromSystemAction(action: String?): RescheduleTrigger? {
    return when (action) {
        Intent.ACTION_BOOT_COMPLETED -> RescheduleTrigger.BOOT_COMPLETED
        Intent.ACTION_TIME_CHANGED -> RescheduleTrigger.TIME_CHANGED
        Intent.ACTION_DATE_CHANGED -> RescheduleTrigger.DATE_CHANGED
        Intent.ACTION_TIMEZONE_CHANGED -> RescheduleTrigger.TIMEZONE_CHANGED
        Intent.ACTION_MY_PACKAGE_REPLACED -> RescheduleTrigger.APP_UPDATED
        else -> null
    }
}

internal fun detectExactAlarmPermissionTransition(
    previousExactReady: Boolean?,
    currentExactReady: Boolean
): ExactAlarmPermissionTransition {
    return when {
        previousExactReady == null || previousExactReady == currentExactReady -> ExactAlarmPermissionTransition.NONE
        !previousExactReady && currentExactReady -> ExactAlarmPermissionTransition.GAINED
        else -> ExactAlarmPermissionTransition.LOST
    }
}

internal fun resolveExactAlarmPermissionRescheduleTrigger(
    previousExactReady: Boolean?,
    currentExactReady: Boolean,
    recoveryState: RescheduleRecoveryState?
): RescheduleTrigger? {
    return when (detectExactAlarmPermissionTransition(previousExactReady, currentExactReady)) {
        ExactAlarmPermissionTransition.GAINED -> RescheduleTrigger.EXACT_PERMISSION_GRANTED
        ExactAlarmPermissionTransition.LOST -> RescheduleTrigger.EXACT_PERMISSION_LOST
        ExactAlarmPermissionTransition.NONE -> reconcileExactAlarmPermissionState(
            currentExactReady = currentExactReady,
            recoveryState = recoveryState
        )
    }
}

private fun reconcileExactAlarmPermissionState(
    currentExactReady: Boolean,
    recoveryState: RescheduleRecoveryState?
): RescheduleTrigger? {
    return when {
        currentExactReady && recoveryState?.action == RescheduleTrigger.EXACT_PERMISSION_LOST.storageAction ->
            RescheduleTrigger.EXACT_PERMISSION_GRANTED
        !currentExactReady && shouldReconcileExactPermissionLoss(recoveryState) ->
            RescheduleTrigger.EXACT_PERMISSION_LOST
        else -> null
    }
}

private fun shouldReconcileExactPermissionLoss(recoveryState: RescheduleRecoveryState?): Boolean {
    return when {
        recoveryState == null -> true
        recoveryState.action != RescheduleTrigger.EXACT_PERMISSION_LOST.storageAction -> true
        recoveryState.outcome == RescheduleRecoveryState.Outcome.FULL_RECOVERY -> true
        else -> false
    }
}

internal data class EnabledAlarmRescheduleReport(
    val trigger: RescheduleTrigger,
    val enabledCount: Int,
    val exactCount: Int,
    val inexactCount: Int,
    val blockedCount: Int,
    val primaryReason: AlarmScheduleFailureReason?
) {
    val scheduledCount: Int
        get() = exactCount + inexactCount
}

internal fun summarizeEnabledAlarmReschedule(
    trigger: RescheduleTrigger,
    enabledCount: Int,
    results: Iterable<AlarmScheduleResult>
): EnabledAlarmRescheduleReport {
    val resultList = results.toList()
    return EnabledAlarmRescheduleReport(
        trigger = trigger,
        enabledCount = enabledCount.coerceAtLeast(0),
        exactCount = resultList.count { it.mode == AlarmScheduleMode.EXACT },
        inexactCount = resultList.count { it.mode == AlarmScheduleMode.INEXACT },
        blockedCount = resultList.count { it.mode == AlarmScheduleMode.BLOCKED },
        primaryReason = selectPrimaryRescheduleReason(resultList)
    )
}

private fun selectPrimaryRescheduleReason(results: List<AlarmScheduleResult>): AlarmScheduleFailureReason? {
    return results.mapNotNull { it.failureReason }
        .minByOrNull(::rescheduleReasonPriority)
}

private fun rescheduleReasonPriority(reason: AlarmScheduleFailureReason): Int {
    return when (reason) {
        AlarmScheduleFailureReason.EXACT_PERMISSION_DENIED -> 0
        AlarmScheduleFailureReason.EXACT_SECURITY_EXCEPTION -> 1
        AlarmScheduleFailureReason.PLATFORM_FAILURE -> 2
        AlarmScheduleFailureReason.NO_NEXT_TRIGGER -> 3
    }
}

internal class EnabledAlarmRescheduler(context: Context) {
    private val appContext = context.applicationContext
    private val dao = AlarmDatabase.get(appContext).alarmDao()
    private val scheduler = AlarmScheduler(appContext)
    private val recoveryStore = RescheduleRecoveryStore(appContext)
    private val primaryAlarmScheduleTracker = PrimaryAlarmScheduleTracker(appContext)

    suspend fun rescheduleAllEnabled(trigger: RescheduleTrigger): EnabledAlarmRescheduleReport {
        return try {
            val enabledAlarms = dao.getAllEnabled().map { it.toDomain() }
            val report = summarizeEnabledAlarmReschedule(
                trigger = trigger,
                enabledCount = enabledAlarms.size,
                results = enabledAlarms.map { scheduler.scheduleWithResult(it) }
            )
            recoveryStore.record(report)
            primaryAlarmScheduleTracker.ensureScheduled()
            NightlyReliabilityCheckScheduler.schedule(appContext)
            report
        } finally {
            runCatching {
                appContext.sendBroadcast(Intent(AlarmRingingService.ACTION_RELIABILITY_STATE_CHANGED))
            }
        }
    }
}


