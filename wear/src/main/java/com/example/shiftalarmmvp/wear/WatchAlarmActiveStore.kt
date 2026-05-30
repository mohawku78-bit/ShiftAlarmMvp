package com.example.shiftalarmmvp.wear

import android.content.Context

object WatchAlarmActiveStore {
    private const val PREFS_NAME = "watch_alarm_active"
    private const val KEY_PAYLOAD_JSON = "payload_json"

    @Synchronized
    fun record(context: Context, payload: WatchAlarmPayload) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PAYLOAD_JSON, WatchAlarmProtocol.toJson(payload))
            .apply()
    }

    @Synchronized
    fun read(context: Context): WatchAlarmPayload? {
        val rawJson = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PAYLOAD_JSON, null)
        return WatchAlarmProtocol.parsePayload(rawJson)
    }

    @Synchronized
    fun clearIfMatching(context: Context, alarmId: Long, triggeredAtMillis: Long) {
        val active = read(context) ?: return
        if (matches(active, alarmId, triggeredAtMillis)) {
            clear(context)
        }
    }

    @Synchronized
    fun isMatching(context: Context, alarmId: Long, triggeredAtMillis: Long): Boolean {
        return matches(read(context), alarmId, triggeredAtMillis)
    }

    @Synchronized
    fun clear(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_PAYLOAD_JSON)
            .apply()
    }

    internal fun matches(active: WatchAlarmPayload?, alarmId: Long, triggeredAtMillis: Long): Boolean {
        return active != null &&
            alarmId > 0L &&
            triggeredAtMillis > 0L &&
            active.alarmId == alarmId &&
            active.triggeredAtMillis == triggeredAtMillis
    }
}
