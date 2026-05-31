package com.example.shiftalarmmvp.watch

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
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

data class WatchAlarmControl(
    val action: String,
    val payload: WatchAlarmPayload
)

data class WatchAlarmAckEnvelope(
    val payload: WatchAlarmPayload,
    val displayMode: String?
)

data class WatchAlarmSendResult(
    val connectedNodeCount: Int,
    val messageSendAttempts: Int,
    val reachableWatchAppNodeCount: Int,
    val reachableWatchAppNodeNames: List<String>,
    val watchAppLookupErrorMessage: String?,
    val errorMessage: String?
)

class WatchAlarmBridge(context: Context) {
    private val appContext = context.applicationContext

    fun sendAlarmStarted(
        alarmId: Long,
        label: String,
        snoozeMinutes: Int,
        snoozeMaxCount: Int,
        currentSnoozeCount: Int,
        soundType: String?,
        customSoundUri: String?,
        volumePercent: Int,
        vibrationEnabled: Boolean,
        snoozeAllowed: Boolean = true,
        triggeredAtMillis: Long = System.currentTimeMillis()
    ) {
        if (alarmId <= 0) return
        Log.i(TAG, "sendAlarmStarted alarmId=$alarmId snoozeAllowed=$snoozeAllowed")
        val payload = WatchAlarmPayload(
            alarmId = alarmId,
            label = label,
            snoozeMinutes = snoozeMinutes,
            snoozeMaxCount = snoozeMaxCount,
            currentSnoozeCount = currentSnoozeCount,
            soundType = soundType,
            customSoundUri = customSoundUri,
            volumePercent = volumePercent,
            vibrationEnabled = vibrationEnabled,
            snoozeAllowed = snoozeAllowed,
            triggeredAtMillis = triggeredAtMillis
        )
        sendAlarmStartedPayload(payload)
    }

    fun sendPreviewAlarm(
        label: String,
        snoozeMinutes: Int,
        soundType: String?,
        customSoundUri: String?,
        volumePercent: Int,
        vibrationEnabled: Boolean
    ) {
        sendPreviewAlarmWithResult(
            label = label,
            snoozeMinutes = snoozeMinutes,
            soundType = soundType,
            customSoundUri = customSoundUri,
            volumePercent = volumePercent,
            vibrationEnabled = vibrationEnabled,
            onResult = {}
        )
    }

    fun sendPreviewAlarmWithResult(
        label: String,
        snoozeMinutes: Int,
        soundType: String?,
        customSoundUri: String?,
        volumePercent: Int,
        vibrationEnabled: Boolean,
        onResult: (WatchAlarmSendResult) -> Unit
    ) {
        val payload = WatchAlarmPayload(
            alarmId = WATCH_PREVIEW_ALARM_ID,
            label = label.ifBlank { "Watch alarm preview" },
            snoozeMinutes = snoozeMinutes,
            snoozeMaxCount = 3,
            currentSnoozeCount = 0,
            soundType = soundType,
            customSoundUri = customSoundUri,
            volumePercent = volumePercent,
            vibrationEnabled = vibrationEnabled,
            snoozeAllowed = true,
            triggeredAtMillis = System.currentTimeMillis()
        )
        sendAlarmStartedPayload(payload, onResult)
    }

    fun sendAlarmCancelled(
        alarmId: Long,
        triggeredAtMillis: Long = System.currentTimeMillis(),
        clearActive: Boolean = true
    ) {
        if (alarmId <= 0) return
        Log.i(TAG, "sendAlarmCancelled alarmId=$alarmId triggeredAt=$triggeredAtMillis clearActive=$clearActive")
        val payload = JSONObject()
            .put(KEY_ALARM_ID, alarmId)
            .put(KEY_TRIGGERED_AT_MILLIS, triggeredAtMillis)
        sendToConnectedNodes(PATH_ALARM_CANCEL, payload)
        putDataItem(PATH_ALARM_CANCELLED, payload)
        if (clearActive) {
            deleteDataItem(PATH_ALARM_ACTIVE)
        }
    }

    fun sendControlAcknowledged(action: String, payload: WatchAlarmPayload) {
        if (payload.alarmId <= 0 || action.isBlank()) return
        val json = controlAcknowledgementJson(action, payload)
        Log.i(TAG, "sendControlAcknowledged action=$action alarmId=${payload.alarmId}")
        sendToConnectedNodes(PATH_ALARM_CONTROL_ACK, json)
        putControlAcknowledgementDataItem(json)
    }

    private fun sendAlarmStartedPayload(
        payload: WatchAlarmPayload,
        onResult: ((WatchAlarmSendResult) -> Unit)? = null
    ) {
        val json = payload.toJson()
        sendToConnectedNodes(PATH_ALARM_START, json) { result ->
            WatchAlarmDiagnosticsStore(appContext).recordSendAttempt(payload, result)
            onResult?.invoke(result)
        }
        putDataItem(PATH_ALARM_ACTIVE, json)
    }

    private fun sendToConnectedNodes(
        path: String,
        payload: JSONObject,
        onResult: ((WatchAlarmSendResult) -> Unit)? = null
    ) {
        val bytes = payload.toString().toByteArray(Charsets.UTF_8)
        val handler = Handler(Looper.getMainLooper())
        repeat(MESSAGE_SEND_ATTEMPTS) { index ->
            val attempt = index + 1
            val delayMillis = index * MESSAGE_RETRY_DELAY_MILLIS
            val resultCallback = if (index == 0) onResult else null
            if (delayMillis == 0L) {
                sendToConnectedNodesOnce(path, bytes, attempt, resultCallback)
            } else {
                handler.postDelayed(
                    { sendToConnectedNodesOnce(path, bytes, attempt, resultCallback) },
                    delayMillis
                )
            }
        }
    }

    private fun sendToConnectedNodesOnce(
        path: String,
        bytes: ByteArray,
        attempt: Int,
        onResult: ((WatchAlarmSendResult) -> Unit)? = null
    ) {
        val nodeClient = Wearable.getNodeClient(appContext)
        val messageClient = Wearable.getMessageClient(appContext)

        nodeClient.connectedNodes
            .addOnSuccessListener { nodes ->
                Log.i(TAG, "sendMessage path=$path attempt=$attempt nodes=${nodes.size}")
                nodes.forEach { node ->
                    messageClient.sendMessage(node.id, path, bytes)
                        .addOnSuccessListener {
                            Log.i(TAG, "sendMessage ok path=$path attempt=$attempt node=${node.displayName}")
                        }
                        .addOnFailureListener { error ->
                            Log.w(TAG, "sendMessage failed path=$path attempt=$attempt node=${node.displayName}", error)
                        }
                }
                if (onResult != null) {
                    resolveReachableWatchAppNodes { watchAppNodeCount, watchAppNodeNames, watchAppError ->
                        onResult.invoke(
                            WatchAlarmSendResult(
                                connectedNodeCount = nodes.size,
                                messageSendAttempts = nodes.size * MESSAGE_SEND_ATTEMPTS,
                                reachableWatchAppNodeCount = watchAppNodeCount,
                                reachableWatchAppNodeNames = watchAppNodeNames,
                                watchAppLookupErrorMessage = watchAppError,
                                errorMessage = null
                            )
                        )
                    }
                }
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "connectedNodes failed path=$path attempt=$attempt", error)
                onResult?.invoke(
                    WatchAlarmSendResult(
                        connectedNodeCount = 0,
                        messageSendAttempts = MESSAGE_SEND_ATTEMPTS,
                        reachableWatchAppNodeCount = 0,
                        reachableWatchAppNodeNames = emptyList(),
                        watchAppLookupErrorMessage = null,
                        errorMessage = error.message ?: error.javaClass.simpleName
                    )
                )
            }
    }

    private fun resolveReachableWatchAppNodes(
        onResult: (nodeCount: Int, nodeNames: List<String>, errorMessage: String?) -> Unit
    ) {
        Wearable.getCapabilityClient(appContext)
            .getCapability(CAPABILITY_WATCH_ALARM_CONTROL, CapabilityClient.FILTER_REACHABLE)
            .addOnSuccessListener { capabilityInfo ->
                val nodes = capabilityInfo.nodes.toList()
                Log.i(TAG, "capability $CAPABILITY_WATCH_ALARM_CONTROL reachableNodes=${nodes.size}")
                onResult(
                    nodes.size,
                    nodes.map { it.displayName }.filter { it.isNotBlank() },
                    null
                )
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "capability lookup failed $CAPABILITY_WATCH_ALARM_CONTROL", error)
                onResult(
                    0,
                    emptyList(),
                    error.message ?: error.javaClass.simpleName
                )
            }
    }

    private fun putDataItem(path: String, payload: JSONObject) {
        val request = PutDataMapRequest.create(path).apply {
            dataMap.putString(KEY_PAYLOAD_JSON, payload.toString())
            dataMap.putLong(KEY_EVENT_TIME_MILLIS, System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()

        Wearable.getDataClient(appContext).putDataItem(request)
            .addOnSuccessListener { Log.i(TAG, "putDataItem path=$path") }
            .addOnFailureListener { error -> Log.w(TAG, "putDataItem failed path=$path", error) }
    }

    private fun putControlAcknowledgementDataItem(payload: JSONObject) {
        val request = PutDataMapRequest.create(PATH_ALARM_CONTROL_ACK).apply {
            dataMap.putString(KEY_ACTION, payload.optString(KEY_ACTION))
            dataMap.putString(KEY_PAYLOAD_JSON, payload.optString(KEY_PAYLOAD_JSON))
            dataMap.putLong(KEY_EVENT_TIME_MILLIS, payload.optLong(KEY_EVENT_TIME_MILLIS, System.currentTimeMillis()))
        }.asPutDataRequest().setUrgent()

        Wearable.getDataClient(appContext).putDataItem(request)
            .addOnSuccessListener { Log.i(TAG, "putDataItem path=$PATH_ALARM_CONTROL_ACK") }
            .addOnFailureListener { error -> Log.w(TAG, "putDataItem failed path=$PATH_ALARM_CONTROL_ACK", error) }
    }

    private fun deleteDataItem(path: String) {
        val uri = Uri.Builder()
            .scheme("wear")
            .path(path)
            .build()
        Wearable.getDataClient(appContext).deleteDataItems(uri)
            .addOnSuccessListener { Log.i(TAG, "deleteDataItem path=$path count=$it") }
            .addOnFailureListener { error -> Log.w(TAG, "deleteDataItem failed path=$path", error) }
    }

    companion object {
        private const val TAG = "ShiftWatchBridge"
        private const val MESSAGE_SEND_ATTEMPTS = 3
        private const val MESSAGE_RETRY_DELAY_MILLIS = 700L

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
        const val WATCH_PREVIEW_ALARM_ID = 888_888L
        const val CAPABILITY_WATCH_ALARM_CONTROL = "shift_alarm_watch_control"
        const val ACK_DISPLAY_MODE_NOTIFICATION = "notification"
        const val ACK_DISPLAY_MODE_FALLBACK = "fallback"

        private const val KEY_ACTION = "action"
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
        private const val KEY_ACK_DISPLAY_MODE = "ackDisplayMode"
        private const val KEY_PAYLOAD_JSON = "payloadJson"
        private const val KEY_EVENT_TIME_MILLIS = "eventTimeMillis"

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
            }.getOrNull()?.takeIf { it.alarmId > 0 && it.triggeredAtMillis > 0L }
        }

        fun parsePayload(dataItem: DataItem): WatchAlarmPayload? {
            val dataMap = runCatching { DataMapItem.fromDataItem(dataItem).dataMap }.getOrNull() ?: return null
            return parsePayload(dataMap.getString(KEY_PAYLOAD_JSON))
        }

        fun parseAck(bytes: ByteArray): WatchAlarmAckEnvelope? {
            return parseAck(bytes.toString(Charsets.UTF_8))
        }

        fun parseAck(dataItem: DataItem): WatchAlarmAckEnvelope? {
            val dataMap = runCatching { DataMapItem.fromDataItem(dataItem).dataMap }.getOrNull() ?: return null
            return parseAck(dataMap.getString(KEY_PAYLOAD_JSON))
        }

        private fun parseAck(rawJson: String?): WatchAlarmAckEnvelope? {
            val payload = parsePayload(rawJson) ?: return null
            val displayMode = runCatching {
                JSONObject(rawJson.orEmpty()).optionalString(KEY_ACK_DISPLAY_MODE)
            }.getOrNull()
            return WatchAlarmAckEnvelope(payload, displayMode)
        }

        fun parseCancelledAlarmId(bytes: ByteArray): Long {
            return runCatching {
                JSONObject(bytes.toString(Charsets.UTF_8)).optLong(KEY_ALARM_ID, -1L)
            }.getOrDefault(-1L)
        }

        fun parseCancelledAlarmId(dataItem: DataItem): Long {
            val dataMap = runCatching { DataMapItem.fromDataItem(dataItem).dataMap }.getOrNull() ?: return -1L
            return runCatching {
                JSONObject(dataMap.getString(KEY_PAYLOAD_JSON).orEmpty()).optLong(KEY_ALARM_ID, -1L)
            }.getOrDefault(-1L)
        }

        fun parseControl(dataItem: DataItem): WatchAlarmControl? {
            val dataMap = runCatching { DataMapItem.fromDataItem(dataItem).dataMap }.getOrNull() ?: return null
            val action = dataMap.getString(KEY_ACTION)?.takeIf { it.isNotBlank() } ?: return null
            val payload = parsePayload(dataMap.getString(KEY_PAYLOAD_JSON)) ?: return null
            return WatchAlarmControl(action, payload)
        }

        fun WatchAlarmPayload.toJson(): JSONObject {
            return JSONObject()
                .put(KEY_ALARM_ID, alarmId)
                .put(KEY_LABEL, label)
                .put(KEY_SNOOZE_MINUTES, snoozeMinutes)
                .put(KEY_SNOOZE_MAX_COUNT, snoozeMaxCount)
                .put(KEY_CURRENT_SNOOZE_COUNT, currentSnoozeCount)
                .put(KEY_VOLUME_PERCENT, volumePercent)
                .put(KEY_VIBRATION_ENABLED, vibrationEnabled)
                .put(KEY_SNOOZE_ALLOWED, snoozeAllowed)
                .put(KEY_TRIGGERED_AT_MILLIS, triggeredAtMillis)
                .apply {
                    soundType?.let { put(KEY_SOUND_TYPE, it) }
                    customSoundUri?.let { put(KEY_CUSTOM_SOUND_URI, it) }
                }
        }

        private fun controlAcknowledgementJson(action: String, payload: WatchAlarmPayload): JSONObject {
            return JSONObject()
                .put(KEY_ACTION, action)
                .put(KEY_PAYLOAD_JSON, payload.toJson().toString())
                .put(KEY_EVENT_TIME_MILLIS, System.currentTimeMillis())
        }

        private fun JSONObject.optionalString(key: String): String? {
            if (!has(key) || isNull(key)) return null
            return optString(key).takeIf { it.isNotBlank() }
        }
    }
}
