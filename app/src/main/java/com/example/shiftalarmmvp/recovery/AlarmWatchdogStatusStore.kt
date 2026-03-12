package com.example.shiftalarmmvp.recovery

import android.content.Context
import com.example.shiftalarmmvp.scheduler.AlarmScheduleMode
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val ALARM_WATCHDOG_STATUS_PREF_NAME = "alarm_watchdog_status"
private const val KEY_STATUS_JSON = "status_json"
internal const val ALARM_WATCHDOG_EXACT_GRACE_MILLIS = 2L * 60L * 1000L
internal const val ALARM_WATCHDOG_INEXACT_GRACE_MILLIS = 20L * 60L * 1000L
internal const val ALARM_WATCHDOG_ATTENTION_WINDOW_MILLIS = 24L * 60L * 60L * 1000L
private val ALARM_WATCHDOG_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")

enum class AlarmWatchdogCause {
    EXACT_PERMISSION_LOST,
    INEXACT_FALLBACK_DELAY,
    BATTERY_RESTRICTION,
    REGISTRATION_LOSS,
    UNKNOWN
}

enum class AlarmWatchdogEventType {
    CLEAN,
    MISSED,
    SELF_HEALED,
    CHECK_FAILED
}

data class AlarmWatchdogStatus(
    val checkedAtMillis: Long,
    val eventType: AlarmWatchdogEventType,
    val eventAtMillis: Long = checkedAtMillis,
    val alarmId: Long? = null,
    val alarmLabel: String? = null,
    val cause: AlarmWatchdogCause? = null,
    val expectedTriggerMillis: Long = 0L,
    val scheduleMode: AlarmScheduleMode? = null,
    val detail: String = ""
) {
    fun needsAttention(nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (eventAtMillis <= 0L) return false
        return when (eventType) {
            AlarmWatchdogEventType.MISSED,
            AlarmWatchdogEventType.SELF_HEALED,
            AlarmWatchdogEventType.CHECK_FAILED -> nowMillis - eventAtMillis < ALARM_WATCHDOG_ATTENTION_WINDOW_MILLIS
            AlarmWatchdogEventType.CLEAN -> false
        }
    }

    fun overviewText(strings: AlarmWatchdogStrings): String {
        val occurredAt = formatMillis(eventAtMillis.takeIf { it > 0L } ?: checkedAtMillis)
        val label = alarmLabel.takeUnless { it.isNullOrBlank() } ?: strings.unknownAlarmLabel
        val causeText = causeText(strings)
        return when (eventType) {
            AlarmWatchdogEventType.CLEAN -> strings.lineCleanFormat.format(formatMillis(checkedAtMillis))
            AlarmWatchdogEventType.MISSED -> strings.lineMissedFormat.format(occurredAt, label, causeText)
            AlarmWatchdogEventType.SELF_HEALED -> strings.lineSelfHealedFormat.format(occurredAt, label, causeText)
            AlarmWatchdogEventType.CHECK_FAILED -> strings.lineCheckFailedFormat.format(occurredAt)
        }
    }

    fun homeReasonText(strings: AlarmWatchdogStrings): String {
        return when (eventType) {
            AlarmWatchdogEventType.CHECK_FAILED -> strings.homeReasonCheckFailed
            AlarmWatchdogEventType.MISSED,
            AlarmWatchdogEventType.SELF_HEALED -> {
                val label = alarmLabel.takeUnless { it.isNullOrBlank() } ?: strings.unknownAlarmLabel
                strings.homeReasonIncidentFormat.format(label, causeText(strings))
            }
            AlarmWatchdogEventType.CLEAN -> strings.homeReasonClean
        }
    }

    fun causeText(strings: AlarmWatchdogStrings): String {
        return when (cause) {
            AlarmWatchdogCause.EXACT_PERMISSION_LOST -> strings.causeExactPermissionLost
            AlarmWatchdogCause.INEXACT_FALLBACK_DELAY -> strings.causeInexactFallbackDelay
            AlarmWatchdogCause.BATTERY_RESTRICTION -> strings.causeBatteryRestriction
            AlarmWatchdogCause.REGISTRATION_LOSS -> strings.causeRegistrationLoss
            AlarmWatchdogCause.UNKNOWN,
            null -> strings.causeUnknown
        }
    }

    private fun formatMillis(millis: Long): String {
        if (millis <= 0L) return "--"
        return Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .format(ALARM_WATCHDOG_TIME_FORMATTER)
    }
}

internal fun watchdogGraceMillis(mode: AlarmScheduleMode): Long {
    return when (mode) {
        AlarmScheduleMode.EXACT -> ALARM_WATCHDOG_EXACT_GRACE_MILLIS
        AlarmScheduleMode.INEXACT -> ALARM_WATCHDOG_INEXACT_GRACE_MILLIS
        AlarmScheduleMode.BLOCKED -> ALARM_WATCHDOG_EXACT_GRACE_MILLIS
    }
}

internal fun watchdogDueAtMillis(expectedTriggerMillis: Long, mode: AlarmScheduleMode): Long {
    return expectedTriggerMillis + watchdogGraceMillis(mode)
}

internal fun overdueTrackedRegistrations(
    pendingRegistrations: Iterable<TrackedAlarmRegistration>,
    nowMillis: Long
): List<TrackedAlarmRegistration> {
    return pendingRegistrations
        .filter { it.expectedTriggerMillis > 0L }
        .filter { nowMillis > watchdogDueAtMillis(it.expectedTriggerMillis, it.scheduleMode) }
        .sortedBy { it.expectedTriggerMillis }
}

internal fun nextAlarmWatchdogCheckAtMillis(
    pendingRegistrations: Iterable<TrackedAlarmRegistration>,
    nowMillis: Long
): Long? {
    return pendingRegistrations
        .filter { it.expectedTriggerMillis > 0L }
        .map { watchdogDueAtMillis(it.expectedTriggerMillis, it.scheduleMode) }
        .minOrNull()
        ?.coerceAtLeast(nowMillis)
}

internal fun classifyWatchdogCause(
    registration: TrackedAlarmRegistration,
    exactReady: Boolean,
    batteryReady: Boolean
): AlarmWatchdogCause {
    return when {
        registration.scheduleMode == AlarmScheduleMode.EXACT && !exactReady -> AlarmWatchdogCause.EXACT_PERMISSION_LOST
        registration.scheduleMode == AlarmScheduleMode.INEXACT -> AlarmWatchdogCause.INEXACT_FALLBACK_DELAY
        !batteryReady -> AlarmWatchdogCause.BATTERY_RESTRICTION
        registration.scheduleMode == AlarmScheduleMode.EXACT -> AlarmWatchdogCause.REGISTRATION_LOSS
        else -> AlarmWatchdogCause.UNKNOWN
    }
}

internal fun shouldKeepWatchdogAttention(
    status: AlarmWatchdogStatus?,
    nowMillis: Long = System.currentTimeMillis()
): Boolean {
    return status?.needsAttention(nowMillis) == true
}

internal class AlarmWatchdogStatusStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(ALARM_WATCHDOG_STATUS_PREF_NAME, Context.MODE_PRIVATE)

    fun load(): AlarmWatchdogStatus? {
        val raw = prefs.getString(KEY_STATUS_JSON, null) ?: return null
        return runCatching {
            val obj = JSONObject(raw)
            val checkedAtMillis = obj.optLong("checkedAtMillis", 0L)
            if (checkedAtMillis <= 0L) return null
            AlarmWatchdogStatus(
                checkedAtMillis = checkedAtMillis,
                eventType = runCatching {
                    AlarmWatchdogEventType.valueOf(obj.optString("eventType", AlarmWatchdogEventType.CLEAN.name))
                }.getOrDefault(AlarmWatchdogEventType.CLEAN),
                eventAtMillis = obj.optLong("eventAtMillis", checkedAtMillis),
                alarmId = obj.optLong("alarmId", Long.MIN_VALUE).takeUnless { it == Long.MIN_VALUE },
                alarmLabel = obj.optString("alarmLabel", "").ifBlank { null },
                cause = runCatching { AlarmWatchdogCause.valueOf(obj.optString("cause")) }.getOrNull(),
                expectedTriggerMillis = obj.optLong("expectedTriggerMillis", 0L),
                scheduleMode = parseAlarmScheduleMode(obj.optString("scheduleMode")),
                detail = obj.optString("detail", "")
            )
        }.getOrNull()
    }

    fun recordCleanCheck(nowMillis: Long = System.currentTimeMillis()): AlarmWatchdogStatus {
        val current = load()
        val next = if (shouldKeepWatchdogAttention(current, nowMillis)) {
            current!!.copy(checkedAtMillis = nowMillis)
        } else {
            AlarmWatchdogStatus(
                checkedAtMillis = nowMillis,
                eventType = AlarmWatchdogEventType.CLEAN,
                eventAtMillis = nowMillis
            )
        }
        save(next)
        return next
    }

    fun recordIncident(
        registration: TrackedAlarmRegistration,
        cause: AlarmWatchdogCause,
        detail: String = "",
        nowMillis: Long = System.currentTimeMillis()
    ): AlarmWatchdogStatus {
        val next = AlarmWatchdogStatus(
            checkedAtMillis = nowMillis,
            eventType = AlarmWatchdogEventType.MISSED,
            eventAtMillis = nowMillis,
            alarmId = registration.alarmId,
            alarmLabel = registration.label,
            cause = cause,
            expectedTriggerMillis = registration.expectedTriggerMillis,
            scheduleMode = registration.scheduleMode,
            detail = detail
        )
        save(next)
        return next
    }

    fun recordSelfHealed(
        registration: TrackedAlarmRegistration,
        cause: AlarmWatchdogCause,
        detail: String = "",
        nowMillis: Long = System.currentTimeMillis()
    ): AlarmWatchdogStatus {
        val next = AlarmWatchdogStatus(
            checkedAtMillis = nowMillis,
            eventType = AlarmWatchdogEventType.SELF_HEALED,
            eventAtMillis = nowMillis,
            alarmId = registration.alarmId,
            alarmLabel = registration.label,
            cause = cause,
            expectedTriggerMillis = registration.expectedTriggerMillis,
            scheduleMode = registration.scheduleMode,
            detail = detail
        )
        save(next)
        return next
    }

    fun recordCheckFailed(
        detail: String,
        nowMillis: Long = System.currentTimeMillis()
    ): AlarmWatchdogStatus {
        val next = AlarmWatchdogStatus(
            checkedAtMillis = nowMillis,
            eventType = AlarmWatchdogEventType.CHECK_FAILED,
            eventAtMillis = nowMillis,
            detail = detail
        )
        save(next)
        return next
    }

    private fun save(status: AlarmWatchdogStatus) {
        val payload = JSONObject()
            .put("checkedAtMillis", status.checkedAtMillis)
            .put("eventType", status.eventType.name)
            .put("eventAtMillis", status.eventAtMillis)
            .put("alarmId", status.alarmId)
            .put("alarmLabel", status.alarmLabel)
            .put("cause", status.cause?.name)
            .put("expectedTriggerMillis", status.expectedTriggerMillis)
            .put("scheduleMode", status.scheduleMode?.name)
            .put("detail", status.detail)
        prefs.edit().putString(KEY_STATUS_JSON, payload.toString()).commit()
    }
}


