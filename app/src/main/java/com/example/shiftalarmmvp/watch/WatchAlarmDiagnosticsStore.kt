package com.example.shiftalarmmvp.watch

import android.content.Context
import android.content.Intent

data class WatchAlarmAck(
    val alarmId: Long,
    val label: String,
    val displayMode: String,
    val acknowledgedAtMillis: Long
)

data class WatchAlarmControlReceipt(
    val alarmId: Long,
    val label: String,
    val action: String,
    val receivedAtMillis: Long
)

class WatchAlarmDiagnosticsStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun recordAck(payload: WatchAlarmPayload, displayMode: String?) {
        prefs.edit()
            .putLong(KEY_ACK_ALARM_ID, payload.alarmId)
            .putString(KEY_ACK_LABEL, payload.label)
            .putString(KEY_ACK_DISPLAY_MODE, displayMode.orEmpty())
            .putLong(KEY_ACK_AT_MILLIS, System.currentTimeMillis())
            .apply()

        appContext.sendBroadcast(Intent(ACTION_WATCH_ALARM_DIAGNOSTICS_CHANGED).setPackage(appContext.packageName))
    }

    fun recordControlAccepted(action: String, payload: WatchAlarmPayload) {
        prefs.edit()
            .putLong(KEY_CONTROL_ALARM_ID, payload.alarmId)
            .putString(KEY_CONTROL_LABEL, payload.label)
            .putString(KEY_CONTROL_ACTION, action)
            .putLong(KEY_CONTROL_AT_MILLIS, System.currentTimeMillis())
            .apply()

        appContext.sendBroadcast(Intent(ACTION_WATCH_ALARM_DIAGNOSTICS_CHANGED).setPackage(appContext.packageName))
    }

    fun latestAck(): WatchAlarmAck? {
        val alarmId = prefs.getLong(KEY_ACK_ALARM_ID, -1L)
        val acknowledgedAtMillis = prefs.getLong(KEY_ACK_AT_MILLIS, 0L)
        if (alarmId <= 0L || acknowledgedAtMillis <= 0L) return null

        return WatchAlarmAck(
            alarmId = alarmId,
            label = prefs.getString(KEY_ACK_LABEL, "").orEmpty(),
            displayMode = prefs.getString(KEY_ACK_DISPLAY_MODE, "").orEmpty(),
            acknowledgedAtMillis = acknowledgedAtMillis
        )
    }

    fun latestAcceptedControl(): WatchAlarmControlReceipt? {
        val alarmId = prefs.getLong(KEY_CONTROL_ALARM_ID, -1L)
        val receivedAtMillis = prefs.getLong(KEY_CONTROL_AT_MILLIS, 0L)
        val action = prefs.getString(KEY_CONTROL_ACTION, "").orEmpty()
        if (alarmId <= 0L || receivedAtMillis <= 0L || action.isBlank()) return null

        return WatchAlarmControlReceipt(
            alarmId = alarmId,
            label = prefs.getString(KEY_CONTROL_LABEL, "").orEmpty(),
            action = action,
            receivedAtMillis = receivedAtMillis
        )
    }

    companion object {
        const val ACTION_WATCH_ALARM_DIAGNOSTICS_CHANGED =
            "com.example.shiftalarmmvp.action.WATCH_ALARM_DIAGNOSTICS_CHANGED"

        private const val PREFS_NAME = "watch_alarm_diagnostics"
        private const val KEY_ACK_ALARM_ID = "ack_alarm_id"
        private const val KEY_ACK_LABEL = "ack_label"
        private const val KEY_ACK_DISPLAY_MODE = "ack_display_mode"
        private const val KEY_ACK_AT_MILLIS = "ack_at_millis"
        private const val KEY_CONTROL_ALARM_ID = "control_alarm_id"
        private const val KEY_CONTROL_LABEL = "control_label"
        private const val KEY_CONTROL_ACTION = "control_action"
        private const val KEY_CONTROL_AT_MILLIS = "control_at_millis"
    }
}
