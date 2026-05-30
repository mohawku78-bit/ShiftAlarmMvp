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

data class WatchAlarmSendAttempt(
    val alarmId: Long,
    val label: String,
    val connectedNodeCount: Int,
    val messageSendAttempts: Int,
    val reachableWatchAppNodeCount: Int,
    val watchAppLookupErrorMessage: String?,
    val errorMessage: String?,
    val sentAtMillis: Long
)

data class WatchAlarmControlRejection(
    val alarmId: Long,
    val label: String,
    val action: String,
    val reason: String,
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

    fun recordSendAttempt(payload: WatchAlarmPayload, result: WatchAlarmSendResult) {
        prefs.edit()
            .putLong(KEY_SEND_ALARM_ID, payload.alarmId)
            .putString(KEY_SEND_LABEL, payload.label)
            .putInt(KEY_SEND_CONNECTED_NODE_COUNT, result.connectedNodeCount)
            .putInt(KEY_SEND_MESSAGE_ATTEMPTS, result.messageSendAttempts)
            .putInt(KEY_SEND_REACHABLE_WATCH_APP_NODE_COUNT, result.reachableWatchAppNodeCount)
            .putString(KEY_SEND_WATCH_APP_LOOKUP_ERROR, result.watchAppLookupErrorMessage.orEmpty())
            .putString(KEY_SEND_ERROR, result.errorMessage.orEmpty())
            .putLong(KEY_SEND_AT_MILLIS, System.currentTimeMillis())
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

    fun recordControlRejected(action: String, payload: WatchAlarmPayload, reason: String) {
        prefs.edit()
            .putLong(KEY_REJECTED_CONTROL_ALARM_ID, payload.alarmId)
            .putString(KEY_REJECTED_CONTROL_LABEL, payload.label)
            .putString(KEY_REJECTED_CONTROL_ACTION, action)
            .putString(KEY_REJECTED_CONTROL_REASON, reason)
            .putLong(KEY_REJECTED_CONTROL_AT_MILLIS, System.currentTimeMillis())
            .apply()

        appContext.sendBroadcast(Intent(ACTION_WATCH_ALARM_DIAGNOSTICS_CHANGED).setPackage(appContext.packageName))
    }

    fun latestSendAttempt(): WatchAlarmSendAttempt? {
        val alarmId = prefs.getLong(KEY_SEND_ALARM_ID, -1L)
        val sentAtMillis = prefs.getLong(KEY_SEND_AT_MILLIS, 0L)
        if (alarmId <= 0L || sentAtMillis <= 0L) return null

        return WatchAlarmSendAttempt(
            alarmId = alarmId,
            label = prefs.getString(KEY_SEND_LABEL, "").orEmpty(),
            connectedNodeCount = prefs.getInt(KEY_SEND_CONNECTED_NODE_COUNT, 0),
            messageSendAttempts = prefs.getInt(KEY_SEND_MESSAGE_ATTEMPTS, 0),
            reachableWatchAppNodeCount = prefs.getInt(KEY_SEND_REACHABLE_WATCH_APP_NODE_COUNT, 0),
            watchAppLookupErrorMessage = prefs.getString(KEY_SEND_WATCH_APP_LOOKUP_ERROR, "").orEmpty().ifBlank { null },
            errorMessage = prefs.getString(KEY_SEND_ERROR, "").orEmpty().ifBlank { null },
            sentAtMillis = sentAtMillis
        )
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

    fun latestRejectedControl(): WatchAlarmControlRejection? {
        val alarmId = prefs.getLong(KEY_REJECTED_CONTROL_ALARM_ID, -1L)
        val receivedAtMillis = prefs.getLong(KEY_REJECTED_CONTROL_AT_MILLIS, 0L)
        val action = prefs.getString(KEY_REJECTED_CONTROL_ACTION, "").orEmpty()
        val reason = prefs.getString(KEY_REJECTED_CONTROL_REASON, "").orEmpty()
        if (alarmId <= 0L || receivedAtMillis <= 0L || action.isBlank() || reason.isBlank()) return null

        return WatchAlarmControlRejection(
            alarmId = alarmId,
            label = prefs.getString(KEY_REJECTED_CONTROL_LABEL, "").orEmpty(),
            action = action,
            reason = reason,
            receivedAtMillis = receivedAtMillis
        )
    }

    companion object {
        const val ACTION_WATCH_ALARM_DIAGNOSTICS_CHANGED =
            "com.example.shiftalarmmvp.action.WATCH_ALARM_DIAGNOSTICS_CHANGED"

        const val REJECTION_STALE_ALARM = "stale_alarm"
        const val REJECTION_DUPLICATE_CONTROL = "duplicate_control"
        const val REJECTION_SNOOZE_NOT_ALLOWED = "snooze_not_allowed"
        const val REJECTION_UNSUPPORTED_ACTION = "unsupported_action"

        private const val PREFS_NAME = "watch_alarm_diagnostics"
        private const val KEY_SEND_ALARM_ID = "send_alarm_id"
        private const val KEY_SEND_LABEL = "send_label"
        private const val KEY_SEND_CONNECTED_NODE_COUNT = "send_connected_node_count"
        private const val KEY_SEND_MESSAGE_ATTEMPTS = "send_message_attempts"
        private const val KEY_SEND_REACHABLE_WATCH_APP_NODE_COUNT = "send_reachable_watch_app_node_count"
        private const val KEY_SEND_WATCH_APP_LOOKUP_ERROR = "send_watch_app_lookup_error"
        private const val KEY_SEND_ERROR = "send_error"
        private const val KEY_SEND_AT_MILLIS = "send_at_millis"
        private const val KEY_ACK_ALARM_ID = "ack_alarm_id"
        private const val KEY_ACK_LABEL = "ack_label"
        private const val KEY_ACK_DISPLAY_MODE = "ack_display_mode"
        private const val KEY_ACK_AT_MILLIS = "ack_at_millis"
        private const val KEY_CONTROL_ALARM_ID = "control_alarm_id"
        private const val KEY_CONTROL_LABEL = "control_label"
        private const val KEY_CONTROL_ACTION = "control_action"
        private const val KEY_CONTROL_AT_MILLIS = "control_at_millis"
        private const val KEY_REJECTED_CONTROL_ALARM_ID = "rejected_control_alarm_id"
        private const val KEY_REJECTED_CONTROL_LABEL = "rejected_control_label"
        private const val KEY_REJECTED_CONTROL_ACTION = "rejected_control_action"
        private const val KEY_REJECTED_CONTROL_REASON = "rejected_control_reason"
        private const val KEY_REJECTED_CONTROL_AT_MILLIS = "rejected_control_at_millis"
    }
}
