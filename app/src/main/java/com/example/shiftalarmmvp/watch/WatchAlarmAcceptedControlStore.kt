package com.example.shiftalarmmvp.watch

import android.content.Context

object WatchAlarmAcceptedControlStore {
    private const val PREFS_NAME = "watch_alarm_accepted_control"
    private const val KEY_ACTION = "action"
    private const val KEY_ALARM_ID = "alarm_id"
    private const val KEY_TRIGGERED_AT_MILLIS = "triggered_at_millis"
    private const val KEY_CURRENT_SNOOZE_COUNT = "current_snooze_count"
    private const val KEY_ACCEPTED_AT_MILLIS = "accepted_at_millis"
    private const val REPLAY_WINDOW_MILLIS = 2 * 60 * 1000L

    fun record(context: Context, action: String, payload: WatchAlarmPayload) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ACTION, action)
            .putLong(KEY_ALARM_ID, payload.alarmId)
            .putLong(KEY_TRIGGERED_AT_MILLIS, payload.triggeredAtMillis)
            .putInt(KEY_CURRENT_SNOOZE_COUNT, payload.currentSnoozeCount)
            .putLong(KEY_ACCEPTED_AT_MILLIS, System.currentTimeMillis())
            .apply()
    }

    fun matchesRecent(context: Context, action: String, payload: WatchAlarmPayload): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return matchesRecent(
            action = action,
            payload = payload,
            acceptedAction = prefs.getString(KEY_ACTION, "").orEmpty(),
            acceptedAlarmId = prefs.getLong(KEY_ALARM_ID, -1L),
            acceptedTriggeredAtMillis = prefs.getLong(KEY_TRIGGERED_AT_MILLIS, 0L),
            acceptedCurrentSnoozeCount = prefs.getInt(KEY_CURRENT_SNOOZE_COUNT, -1),
            acceptedAtMillis = prefs.getLong(KEY_ACCEPTED_AT_MILLIS, 0L),
            nowMillis = System.currentTimeMillis()
        )
    }

    internal fun matchesRecent(
        action: String,
        payload: WatchAlarmPayload,
        acceptedAction: String,
        acceptedAlarmId: Long,
        acceptedTriggeredAtMillis: Long,
        acceptedCurrentSnoozeCount: Int,
        acceptedAtMillis: Long,
        nowMillis: Long
    ): Boolean {
        if (acceptedAtMillis <= 0L) return false
        if (nowMillis < acceptedAtMillis) return false
        if (nowMillis - acceptedAtMillis > REPLAY_WINDOW_MILLIS) return false
        return action == acceptedAction &&
            payload.alarmId == acceptedAlarmId &&
            payload.triggeredAtMillis == acceptedTriggeredAtMillis &&
            payload.currentSnoozeCount == acceptedCurrentSnoozeCount
    }
}
