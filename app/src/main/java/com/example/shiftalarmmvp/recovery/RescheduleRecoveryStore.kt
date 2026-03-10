package com.example.shiftalarmmvp.recovery

import android.content.Context
import android.content.Intent
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val RECOVERY_PREF_NAME = "alarm_recovery_state"
private const val KEY_LAST_ACTION = "last_action"
private const val KEY_LAST_AT_MILLIS = "last_at_millis"
private const val KEY_ENABLED_COUNT = "enabled_count"
private const val KEY_SCHEDULED_COUNT = "scheduled_count"
private const val KEY_BLOCKED_COUNT = "blocked_count"

private val RECOVERY_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")

data class RescheduleRecoveryState(
    val action: String,
    val occurredAtMillis: Long,
    val enabledCount: Int,
    val scheduledCount: Int,
    val blockedCount: Int
) {
    enum class Outcome {
        FULL_RECOVERY,
        PARTIAL_RECOVERY,
        NO_ACTIVE_ALARMS
    }

    val outcome: Outcome
        get() = when {
            enabledCount <= 0 -> Outcome.NO_ACTIVE_ALARMS
            blockedCount > 0 -> Outcome.PARTIAL_RECOVERY
            else -> Outcome.FULL_RECOVERY
        }

    fun occurredAtText(): String {
        return Instant.ofEpochMilli(occurredAtMillis)
            .atZone(ZoneId.systemDefault())
            .format(RECOVERY_TIME_FORMATTER)
    }

    fun reasonText(): String = actionToKoreanLabel(action)

    fun outcomeText(): String {
        return when (outcome) {
            Outcome.NO_ACTIVE_ALARMS -> "활성 알람 없음"
            Outcome.PARTIAL_RECOVERY -> "일부 미복구"
            Outcome.FULL_RECOVERY -> "정상 복구"
        }
    }

    fun countText(): String {
        return if (enabledCount <= 0) {
            "0/0"
        } else {
            "$scheduledCount/$enabledCount"
        }
    }

    fun toHomeSummary(): String {
        val result = "${outcomeText()} (${countText()})"
        return "자동 복구 ${occurredAtText()} · ${reasonText()} · $result"
    }
}

class RescheduleRecoveryStore(context: Context) {
    private val prefs = context.getSharedPreferences(RECOVERY_PREF_NAME, Context.MODE_PRIVATE)

    fun record(
        action: String,
        enabledCount: Int,
        scheduledCount: Int,
        blockedCount: Int,
        occurredAtMillis: Long = System.currentTimeMillis()
    ) {
        prefs.edit()
            .putString(KEY_LAST_ACTION, action)
            .putLong(KEY_LAST_AT_MILLIS, occurredAtMillis)
            .putInt(KEY_ENABLED_COUNT, enabledCount.coerceAtLeast(0))
            .putInt(KEY_SCHEDULED_COUNT, scheduledCount.coerceAtLeast(0))
            .putInt(KEY_BLOCKED_COUNT, blockedCount.coerceAtLeast(0))
            .apply()
    }

    fun load(): RescheduleRecoveryState? {
        val atMillis = prefs.getLong(KEY_LAST_AT_MILLIS, 0L)
        if (atMillis <= 0L) return null

        return RescheduleRecoveryState(
            action = prefs.getString(KEY_LAST_ACTION, Intent.ACTION_BOOT_COMPLETED).orEmpty(),
            occurredAtMillis = atMillis,
            enabledCount = prefs.getInt(KEY_ENABLED_COUNT, 0),
            scheduledCount = prefs.getInt(KEY_SCHEDULED_COUNT, 0),
            blockedCount = prefs.getInt(KEY_BLOCKED_COUNT, 0)
        )
    }
}

private fun actionToKoreanLabel(action: String): String {
    return when (action) {
        Intent.ACTION_BOOT_COMPLETED -> "재부팅"
        Intent.ACTION_TIMEZONE_CHANGED -> "시간대 변경"
        Intent.ACTION_TIME_CHANGED -> "시간 변경"
        Intent.ACTION_DATE_CHANGED -> "날짜 변경"
        Intent.ACTION_MY_PACKAGE_REPLACED -> "앱 업데이트"
        else -> "상태 변경"
    }
}