package com.example.shiftalarmmvp.wear

import android.content.Intent
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import org.json.JSONObject

data class WatchAlarmPayload(
    val alarmId: Long,
    val label: String,
    val snoozeMinutes: Int,
    val snoozeMaxCount: Int,
    val currentSnoozeCount: Int,
    val soundType: String?,
    val customSoundUri: String?,
    val volumePercent: Int,
    val vibrationEnabled: Boolean,
    val snoozeAllowed: Boolean,
    val triggeredAtMillis: Long
) {
    val canSnooze: Boolean
        get() = snoozeAllowed && (snoozeMaxCount <= 0 || currentSnoozeCount < snoozeMaxCount)
}

data class WatchAlarmCancellation(
    val alarmId: Long,
    val eventTimeMillis: Long
)

data class WatchAlarmControlAcknowledgement(
    val action: String,
    val payload: WatchAlarmPayload
)

object WatchAlarmProtocol {
    const val PATH_ALARM_START = "/shift_alarm/alarm/start"
    const val PATH_ALARM_CANCEL = "/shift_alarm/alarm/cancel"
    const val PATH_ALARM_STOP = "/shift_alarm/alarm/stop"
    const val PATH_ALARM_SNOOZE = "/shift_alarm/alarm/snooze"
    const val PATH_ALARM_ACK = "/shift_alarm/alarm/ack"
    const val PATH_ALARM_ACTIVE = "/shift_alarm/alarm/active"
    const val PATH_ALARM_CANCELLED = "/shift_alarm/alarm/cancelled"
    const val PATH_ALARM_CONTROL = "/shift_alarm/alarm/control"
    const val PATH_ALARM_CONTROL_ACK = "/shift_alarm/alarm/control_ack"
    const val PATH_PREFIX = "/shift_alarm/alarm"

    const val EXTRA_PAYLOAD_JSON = "extra_payload_json"

    const val KEY_ACTION = "action"
    const val KEY_PAYLOAD_JSON = "payloadJson"
    const val KEY_EVENT_TIME_MILLIS = "eventTimeMillis"
    const val KEY_ACK_DISPLAY_MODE = "ackDisplayMode"
    const val ACK_DISPLAY_MODE_FOREGROUND_SERVICE = "foreground_service"
    const val ACK_DISPLAY_MODE_FALLBACK = "fallback"

    private const val KEY_ALARM_ID = "alarmId"
    private const val KEY_LABEL = "label"
    private const val KEY_SNOOZE_MINUTES = "snoozeMinutes"
    private const val KEY_SNOOZE_MAX_COUNT = "snoozeMaxCount"
    private const val KEY_CURRENT_SNOOZE_COUNT = "currentSnoozeCount"
    private const val KEY_SOUND_TYPE = "soundType"
    private const val KEY_CUSTOM_SOUND_URI = "customSoundUri"
    private const val KEY_VOLUME_PERCENT = "volumePercent"
    private const val KEY_VIBRATION_ENABLED = "vibrationEnabled"
    private const val KEY_SNOOZE_ALLOWED = "snoozeAllowed"
    private const val KEY_TRIGGERED_AT_MILLIS = "triggeredAtMillis"

    fun parsePayload(bytes: ByteArray): WatchAlarmPayload? {
        return parsePayload(bytes.toString(Charsets.UTF_8))
    }

    fun parsePayload(rawJson: String?): WatchAlarmPayload? {
        if (rawJson.isNullOrBlank()) return null
        return runCatching {
            val json = JSONObject(rawJson)
            WatchAlarmPayload(
                alarmId = json.optLong(KEY_ALARM_ID, -1L),
                label = json.optString(KEY_LABEL, ""),
                snoozeMinutes = json.optInt(KEY_SNOOZE_MINUTES, 5).coerceIn(1, 60),
                snoozeMaxCount = json.optInt(KEY_SNOOZE_MAX_COUNT, 0).coerceIn(0, 99),
                currentSnoozeCount = json.optInt(KEY_CURRENT_SNOOZE_COUNT, 0).coerceAtLeast(0),
                soundType = json.optionalString(KEY_SOUND_TYPE),
                customSoundUri = json.optionalString(KEY_CUSTOM_SOUND_URI),
                volumePercent = json.optInt(KEY_VOLUME_PERCENT, 100).coerceIn(0, 100),
                vibrationEnabled = json.optBoolean(KEY_VIBRATION_ENABLED, true),
                snoozeAllowed = json.optBoolean(KEY_SNOOZE_ALLOWED, true),
                triggeredAtMillis = json.optLong(KEY_TRIGGERED_AT_MILLIS, 0L)
            )
        }.getOrNull()?.takeIf { it.alarmId > 0L }
    }

    fun parsePayload(intent: Intent): WatchAlarmPayload? {
        return parsePayload(intent.getStringExtra(EXTRA_PAYLOAD_JSON))
    }

    fun parsePayload(dataItem: DataItem): WatchAlarmPayload? {
        val dataMap = runCatching { DataMapItem.fromDataItem(dataItem).dataMap }.getOrNull() ?: return null
        return parsePayload(dataMap.getString(KEY_PAYLOAD_JSON))
    }

    fun toJson(payload: WatchAlarmPayload): String {
        return JSONObject()
            .put(KEY_ALARM_ID, payload.alarmId)
            .put(KEY_LABEL, payload.label)
            .put(KEY_SNOOZE_MINUTES, payload.snoozeMinutes)
            .put(KEY_SNOOZE_MAX_COUNT, payload.snoozeMaxCount)
            .put(KEY_CURRENT_SNOOZE_COUNT, payload.currentSnoozeCount)
            .put(KEY_VOLUME_PERCENT, payload.volumePercent)
            .put(KEY_VIBRATION_ENABLED, payload.vibrationEnabled)
            .put(KEY_SNOOZE_ALLOWED, payload.snoozeAllowed)
            .put(KEY_TRIGGERED_AT_MILLIS, payload.triggeredAtMillis)
            .apply {
                payload.soundType?.let { put(KEY_SOUND_TYPE, it) }
                payload.customSoundUri?.let { put(KEY_CUSTOM_SOUND_URI, it) }
            }
            .toString()
    }

    fun toAckJson(payload: WatchAlarmPayload, displayMode: String): String {
        return JSONObject(toJson(payload))
            .put(KEY_ACK_DISPLAY_MODE, displayMode)
            .toString()
    }

    fun parseCancelledAlarmId(bytes: ByteArray): Long {
        return parseCancellation(bytes)?.alarmId ?: -1L
    }

    fun parseCancelledAlarmId(dataItem: DataItem): Long {
        return parseCancellation(dataItem)?.alarmId ?: -1L
    }

    fun parseCancellation(bytes: ByteArray): WatchAlarmCancellation? {
        return parseCancellation(bytes.toString(Charsets.UTF_8))
    }

    fun parseCancellation(dataItem: DataItem): WatchAlarmCancellation? {
        val dataMap = runCatching { DataMapItem.fromDataItem(dataItem).dataMap }.getOrNull() ?: return null
        return parseCancellation(dataMap.getString(KEY_PAYLOAD_JSON))
    }

    private fun parseCancellation(rawJson: String?): WatchAlarmCancellation? {
        if (rawJson.isNullOrBlank()) return null
        return runCatching {
            val json = JSONObject(rawJson)
            WatchAlarmCancellation(
                alarmId = json.optLong(KEY_ALARM_ID, -1L),
                eventTimeMillis = json.optLong(KEY_TRIGGERED_AT_MILLIS, 0L)
            )
        }.getOrNull()?.takeIf { it.alarmId > 0L }
    }

    fun writeControl(dataMap: DataMap, action: String, payload: WatchAlarmPayload) {
        dataMap.putString(KEY_ACTION, action)
        dataMap.putString(KEY_PAYLOAD_JSON, toJson(payload))
        dataMap.putLong(KEY_EVENT_TIME_MILLIS, System.currentTimeMillis())
    }

    fun parseControlAcknowledgement(bytes: ByteArray): WatchAlarmControlAcknowledgement? {
        return parseControlAcknowledgement(bytes.toString(Charsets.UTF_8))
    }

    fun parseControlAcknowledgement(dataItem: DataItem): WatchAlarmControlAcknowledgement? {
        val dataMap = runCatching { DataMapItem.fromDataItem(dataItem).dataMap }.getOrNull() ?: return null
        val action = dataMap.getString(KEY_ACTION)?.takeIf { it.isNotBlank() } ?: return null
        val payload = parsePayload(dataMap.getString(KEY_PAYLOAD_JSON)) ?: return null
        return WatchAlarmControlAcknowledgement(action, payload)
    }

    private fun parseControlAcknowledgement(rawJson: String?): WatchAlarmControlAcknowledgement? {
        if (rawJson.isNullOrBlank()) return null
        return runCatching {
            val json = JSONObject(rawJson)
            val action = json.optionalString(KEY_ACTION) ?: return null
            val payload = parsePayload(json.optionalString(KEY_PAYLOAD_JSON)) ?: return null
            WatchAlarmControlAcknowledgement(action, payload)
        }.getOrNull()
    }

    private fun JSONObject.optionalString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key).takeIf { it.isNotBlank() }
    }
}
