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
        return matchesAcknowledgement(
            action = action,
            payload = payload,
            storedAction = prefs.getString(KEY_ACTION, ""),
            storedAlarmId = prefs.getLong(KEY_ALARM_ID, -1L),
            storedTriggeredAtMillis = prefs.getLong(KEY_TRIGGERED_AT_MILLIS, 0L),
            ackAtMillis = prefs.getLong(KEY_ACK_AT_MILLIS, 0L),
            sinceMillis = sinceMillis
        )
    }

    internal fun matchesAcknowledgement(
        action: String,
        payload: WatchAlarmPayload,
        storedAction: String?,
        storedAlarmId: Long,
        storedTriggeredAtMillis: Long,
        ackAtMillis: Long,
        sinceMillis: Long
    ): Boolean {
        return storedAction == action &&
            storedAlarmId == payload.alarmId &&
            storedTriggeredAtMillis == payload.triggeredAtMillis &&
            ackAtMillis >= sinceMillis
    }

    private const val PREFS_NAME = "watch_alarm_control_ack"
    private const val KEY_ACTION = "action"
    private const val KEY_ALARM_ID = "alarm_id"
    private const val KEY_TRIGGERED_AT_MILLIS = "triggered_at_millis"
    private const val KEY_ACK_AT_MILLIS = "ack_at_millis"
}
