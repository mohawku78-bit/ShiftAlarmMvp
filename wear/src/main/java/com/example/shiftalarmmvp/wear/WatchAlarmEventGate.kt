package com.example.shiftalarmmvp.wear

import android.content.Context

object WatchAlarmEventGate {
    const val EVENT_START = "start"
    const val EVENT_CANCEL = "cancel"

    private const val PREFS_NAME = "watch_alarm_event_gate"
    private const val KEY_PREFIX = "alarm_event_"

    @Synchronized
    fun accept(context: Context, eventType: String, alarmId: Long, eventTimeMillis: Long): Boolean {
        if (alarmId <= 0L || eventType.isBlank()) return false

        val resolvedEventTime = if (eventTimeMillis > 0L) {
            eventTimeMillis
        } else {
            System.currentTimeMillis()
        }
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = eventKey(eventType, alarmId) ?: return false
        val lastEventTime = prefs.getLong(key, 0L)
        if (resolvedEventTime <= lastEventTime) return false

        prefs.edit().putLong(key, resolvedEventTime).apply()
        return true
    }

    internal fun eventKey(eventType: String, alarmId: Long): String? {
        if (alarmId <= 0L || eventType.isBlank()) return null
        return "$KEY_PREFIX$eventType-$alarmId"
    }

    fun matchesAcceptedStart(context: Context, alarmId: Long, eventTimeMillis: Long): Boolean {
        return matchesAcceptedEvent(context, EVENT_START, alarmId, eventTimeMillis)
    }

    fun matchesAcceptedCancel(context: Context, alarmId: Long, eventTimeMillis: Long): Boolean {
        return matchesAcceptedEvent(context, EVENT_CANCEL, alarmId, eventTimeMillis)
    }

    private fun matchesAcceptedEvent(
        context: Context,
        eventType: String,
        alarmId: Long,
        eventTimeMillis: Long
    ): Boolean {
        if (alarmId <= 0L || eventTimeMillis <= 0L) return false
        val key = eventKey(eventType, alarmId) ?: return false
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(key, 0L) == eventTimeMillis
    }
}
