package com.example.shiftalarmmvp.watch

import android.content.Context

object WatchAlarmControlGate {
    private const val PREFS_NAME = "watch_alarm_control_gate"
    private const val KEY_LAST_ACCEPTED_EVENT = "last_accepted_event"

    @Synchronized
    fun accept(context: Context, action: String, payload: WatchAlarmPayload): Boolean {
        if (action !in supportedActions) return false
        val eventKey = eventKey(payload) ?: return false
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_LAST_ACCEPTED_EVENT, "") == eventKey) return false

        prefs.edit().putString(KEY_LAST_ACCEPTED_EVENT, eventKey).apply()
        return true
    }

    internal fun eventKey(payload: WatchAlarmPayload): String? {
        if (payload.alarmId <= 0L) return null

        return buildString {
            append(payload.alarmId)
            append('|')
            append(payload.triggeredAtMillis)
            append('|')
            append(payload.currentSnoozeCount)
        }
    }

    private val supportedActions = setOf(
        WatchAlarmBridge.PATH_ALARM_STOP,
        WatchAlarmBridge.PATH_ALARM_SNOOZE
    )
}
