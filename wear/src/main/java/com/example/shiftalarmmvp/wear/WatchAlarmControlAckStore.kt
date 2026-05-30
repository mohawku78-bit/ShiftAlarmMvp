package com.example.shiftalarmmvp.wear

import android.content.Context

object WatchAlarmControlAckStore {
    fun record(context: Context, action: String, payload: WatchAlarmPayload) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ACTION, action)
            .putLong(KEY_ALARM_ID, payload.alarmId)
            .putLong(KEY_TRIGGERED_AT_MILLIS, payload.triggeredAtMillis)
            .putLong(KEY_ACK_AT_MILLIS, System.currentTimeMillis())
            .apply()
    }

    fun hasAcknowledgementSince(
        context: Context,
        action: String,
        payload: WatchAlarmPayload,
        sinceMillis: Long
    ): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_ACTION, "") == action &&
            prefs.getLong(KEY_ALARM_ID, -1L) == payload.alarmId &&
            prefs.getLong(KEY_TRIGGERED_AT_MILLIS, 0L) == payload.triggeredAtMillis &&
            prefs.getLong(KEY_ACK_AT_MILLIS, 0L) >= sinceMillis
    }

    private const val PREFS_NAME = "watch_alarm_control_ack"
    private const val KEY_ACTION = "action"
    private const val KEY_ALARM_ID = "alarm_id"
    private const val KEY_TRIGGERED_AT_MILLIS = "triggered_at_millis"
    private const val KEY_ACK_AT_MILLIS = "ack_at_millis"
}
