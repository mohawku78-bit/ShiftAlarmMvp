package com.example.shiftalarmmvp.recovery

import android.content.Context
import android.content.Intent
import com.example.shiftalarmmvp.scheduler.AlarmScheduleFailureReason
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val RECOVERY_PREF_NAME = "alarm_recovery_state"
private const val KEY_LAST_ACTION = "last_action"
private const val KEY_LAST_AT_MILLIS = "last_at_millis"
private const val KEY_ENABLED_COUNT = "enabled_count"
private const val KEY_SCHEDULED_COUNT = "scheduled_count"
private const val KEY_BLOCKED_COUNT = "blocked_count"
private const val KEY_EXACT_COUNT = "exact_count"
private const val KEY_INEXACT_COUNT = "inexact_count"
private const val KEY_PRIMARY_REASON = "primary_reason"

private val RECOVERY_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")

internal fun parseAlarmScheduleFailureReason(raw: String?): AlarmScheduleFailureReason? {
    return raw
        ?.takeIf { it.isNotBlank() }
        ?.let { value -> runCatching { AlarmScheduleFailureReason.valueOf(value) }.getOrNull() }
}

internal fun createRescheduleRecoveryState(
    action: String,
    occurredAtMillis: Long,
    enabledCount: Int,
    scheduledCount: Int,
    blockedCount: Int,
    exactCount: Int = scheduledCount,
    inexactCount: Int = 0,
    primaryReason: AlarmScheduleFailureReason? = null
): RescheduleRecoveryState {
    return RescheduleRecoveryState(
        action = action,
        occurredAtMillis = occurredAtMillis,
        enabledCount = enabledCount.coerceAtLeast(0),
        scheduledCount = scheduledCount.coerceAtLeast(0),
        blockedCount = blockedCount.coerceAtLeast(0),
        exactCount = exactCount.coerceAtLeast(0),
        inexactCount = inexactCount.coerceAtLeast(0),
        primaryReason = primaryReason
    )
}

data class RescheduleRecoveryState(
    val action: String,
    val occurredAtMillis: Long,
    val enabledCount: Int,
    val scheduledCount: Int,
    val blockedCount: Int,
    val exactCount: Int = scheduledCount,
    val inexactCount: Int = 0,
    val primaryReason: AlarmScheduleFailureReason? = null
) {
    enum class Outcome {
        FULL_RECOVERY,
        DEGRADED_RECOVERY,
        PARTIAL_RECOVERY,
        NO_ACTIVE_ALARMS
    }

    val outcome: Outcome
        get() = when {
            enabledCount <= 0 -> Outcome.NO_ACTIVE_ALARMS
            blockedCount > 0 -> Outcome.PARTIAL_RECOVERY
            inexactCount > 0 -> Outcome.DEGRADED_RECOVERY
            else -> Outcome.FULL_RECOVERY
        }

    val needsAttention: Boolean
        get() = outcome == Outcome.DEGRADED_RECOVERY || outcome == Outcome.PARTIAL_RECOVERY

    fun occurredAtText(): String {
        return Instant.ofEpochMilli(occurredAtMillis)
            .atZone(ZoneId.systemDefault())
            .format(RECOVERY_TIME_FORMATTER)
    }

    fun actionText(texts: RescheduleRecoveryStrings): String = actionToDisplayLabel(action, texts)

    fun reasonText(texts: RescheduleRecoveryStrings): String {
        val actionText = actionText(texts)
        val reasonText = primaryReason?.let { scheduleFailureReasonLabel(it, texts) } ?: return actionText
        return texts.actionWithReasonFormat.format(actionText, reasonText)
    }

    fun outcomeText(texts: RescheduleRecoveryStrings): String {
        return when (outcome) {
            Outcome.NO_ACTIVE_ALARMS -> texts.outcomeNoActive
            Outcome.PARTIAL_RECOVERY -> texts.outcomePartial
            Outcome.DEGRADED_RECOVERY -> texts.outcomeDegraded
            Outcome.FULL_RECOVERY -> texts.outcomeFull
        }
    }

    fun countText(texts: RescheduleRecoveryStrings): String {
        return when {
            enabledCount <= 0 -> texts.countZero
            blockedCount > 0 -> texts.countBlockedFormat.format(scheduledCount, enabledCount, inexactCount, blockedCount)
            inexactCount > 0 -> texts.countFallbackFormat.format(exactCount, inexactCount, enabledCount)
            else -> texts.countExactFormat.format(exactCount, enabledCount)
        }
    }

    fun toHomeSummary(texts: RescheduleRecoveryStrings): String {
        return texts.homeSummaryFormat.format(
            occurredAtText(),
            reasonText(texts),
            outcomeText(texts),
            countText(texts)
        )
    }

    fun homeOneLineSummary(texts: RescheduleRecoveryStrings): String {
        val happenedAt = occurredAtText()
        val actionText = actionText(texts)
        val count = countText(texts)
        return when (outcome) {
            Outcome.FULL_RECOVERY -> texts.oneLineFullFormat.format(happenedAt, actionText, count)
            Outcome.DEGRADED_RECOVERY -> texts.oneLineDegradedFormat.format(happenedAt, actionText, count)
            Outcome.PARTIAL_RECOVERY -> texts.oneLinePartialFormat.format(happenedAt, actionText, count)
            Outcome.NO_ACTIVE_ALARMS -> texts.oneLineNoActiveFormat.format(happenedAt, actionText)
        }
    }
}

class RescheduleRecoveryStore(context: Context) {
    private val prefs = context.getSharedPreferences(RECOVERY_PREF_NAME, Context.MODE_PRIVATE)

    fun record(
        action: String,
        enabledCount: Int,
        scheduledCount: Int,
        blockedCount: Int,
        exactCount: Int = scheduledCount,
        inexactCount: Int = 0,
        primaryReason: AlarmScheduleFailureReason? = null,
        occurredAtMillis: Long = System.currentTimeMillis()
    ) {
        prefs.edit()
            .putString(KEY_LAST_ACTION, action)
            .putLong(KEY_LAST_AT_MILLIS, occurredAtMillis)
            .putInt(KEY_ENABLED_COUNT, enabledCount.coerceAtLeast(0))
            .putInt(KEY_SCHEDULED_COUNT, scheduledCount.coerceAtLeast(0))
            .putInt(KEY_BLOCKED_COUNT, blockedCount.coerceAtLeast(0))
            .putInt(KEY_EXACT_COUNT, exactCount.coerceAtLeast(0))
            .putInt(KEY_INEXACT_COUNT, inexactCount.coerceAtLeast(0))
            .putString(KEY_PRIMARY_REASON, primaryReason?.name)
            .commit()
    }

    internal fun record(
        report: EnabledAlarmRescheduleReport,
        occurredAtMillis: Long = System.currentTimeMillis()
    ) {
        record(
            action = report.trigger.storageAction,
            enabledCount = report.enabledCount,
            scheduledCount = report.scheduledCount,
            blockedCount = report.blockedCount,
            exactCount = report.exactCount,
            inexactCount = report.inexactCount,
            primaryReason = report.primaryReason,
            occurredAtMillis = occurredAtMillis
        )
    }

    fun load(): RescheduleRecoveryState? {
        val atMillis = prefs.getLong(KEY_LAST_AT_MILLIS, 0L)
        if (atMillis <= 0L) return null

        val scheduledCount = prefs.getInt(KEY_SCHEDULED_COUNT, 0)
        return createRescheduleRecoveryState(
            action = prefs.getString(KEY_LAST_ACTION, Intent.ACTION_BOOT_COMPLETED).orEmpty(),
            occurredAtMillis = atMillis,
            enabledCount = prefs.getInt(KEY_ENABLED_COUNT, 0),
            scheduledCount = scheduledCount,
            blockedCount = prefs.getInt(KEY_BLOCKED_COUNT, 0),
            exactCount = prefs.getInt(KEY_EXACT_COUNT, scheduledCount),
            inexactCount = prefs.getInt(KEY_INEXACT_COUNT, 0),
            primaryReason = parseAlarmScheduleFailureReason(prefs.getString(KEY_PRIMARY_REASON, null))
        )
    }
}

private fun actionToDisplayLabel(action: String, texts: RescheduleRecoveryStrings): String {
    return when (action) {
        Intent.ACTION_BOOT_COMPLETED -> texts.actionBoot
        Intent.ACTION_TIMEZONE_CHANGED -> texts.actionTimezoneChanged
        Intent.ACTION_TIME_CHANGED -> texts.actionTimeChanged
        Intent.ACTION_DATE_CHANGED -> texts.actionDateChanged
        Intent.ACTION_MY_PACKAGE_REPLACED -> texts.actionAppUpdated
        RescheduleTrigger.EXACT_PERMISSION_GRANTED.storageAction -> texts.actionExactPermissionGranted
        RescheduleTrigger.EXACT_PERMISSION_LOST.storageAction -> texts.actionExactPermissionLost
        RescheduleTrigger.MANUAL.storageAction -> texts.actionManual
        RescheduleTrigger.RESTORE.storageAction -> texts.actionRestore
        else -> texts.actionStateChanged
    }
}

private fun scheduleFailureReasonLabel(
    reason: AlarmScheduleFailureReason,
    texts: RescheduleRecoveryStrings
): String {
    return when (reason) {
        AlarmScheduleFailureReason.EXACT_PERMISSION_DENIED -> texts.reasonExactPermissionDenied
        AlarmScheduleFailureReason.EXACT_SECURITY_EXCEPTION -> texts.reasonExactSecurityException
        AlarmScheduleFailureReason.PLATFORM_FAILURE -> texts.reasonPlatformFailure
        AlarmScheduleFailureReason.NO_NEXT_TRIGGER -> texts.reasonNoNextTrigger
    }
}

