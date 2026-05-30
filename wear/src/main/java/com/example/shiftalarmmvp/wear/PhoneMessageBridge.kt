package com.example.shiftalarmmvp.wear

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

object PhoneMessageBridge {
    fun send(context: Context, path: String, payload: WatchAlarmPayload) {
        val appContext = context.applicationContext
        val bytes = WatchAlarmProtocol.toJson(payload).toByteArray(Charsets.UTF_8)
        val nodeClient = Wearable.getNodeClient(appContext)
        val messageClient = Wearable.getMessageClient(appContext)

        nodeClient.connectedNodes
            .addOnSuccessListener { nodes ->
                Log.i(TAG, "send control path=$path nodes=${nodes.size} alarmId=${payload.alarmId}")
                nodes.forEach { node ->
                    messageClient.sendMessage(node.id, path, bytes)
                        .addOnSuccessListener { Log.i(TAG, "send control ok path=$path node=${node.displayName}") }
                        .addOnFailureListener { error -> Log.w(TAG, "send control failed path=$path node=${node.displayName}", error) }
                }
            }
            .addOnFailureListener { error -> Log.w(TAG, "connectedNodes failed control path=$path", error) }

        val request = PutDataMapRequest.create(WatchAlarmProtocol.PATH_ALARM_CONTROL).apply {
            WatchAlarmProtocol.writeControl(dataMap, path, payload)
        }.asPutDataRequest().setUrgent()
        Wearable.getDataClient(appContext).putDataItem(request)
            .addOnSuccessListener { Log.i(TAG, "put control data path=$path alarmId=${payload.alarmId}") }
            .addOnFailureListener { error -> Log.w(TAG, "put control data failed path=$path", error) }
    }

    fun sendAck(context: Context, payload: WatchAlarmPayload, displayMode: String) {
        val appContext = context.applicationContext
        val ackJson = WatchAlarmProtocol.toAckJson(payload, displayMode)
        val bytes = ackJson.toByteArray(Charsets.UTF_8)
        val nodeClient = Wearable.getNodeClient(appContext)
        val messageClient = Wearable.getMessageClient(appContext)

        nodeClient.connectedNodes
            .addOnSuccessListener { nodes ->
                Log.i(TAG, "send ack nodes=${nodes.size} alarmId=${payload.alarmId} displayMode=$displayMode")
                nodes.forEach { node ->
                    messageClient.sendMessage(node.id, WatchAlarmProtocol.PATH_ALARM_ACK, bytes)
                        .addOnSuccessListener { Log.i(TAG, "send ack ok node=${node.displayName}") }
                        .addOnFailureListener { error -> Log.w(TAG, "send ack failed node=${node.displayName}", error) }
                }
            }
            .addOnFailureListener { error -> Log.w(TAG, "connectedNodes failed ack", error) }

        val request = PutDataMapRequest.create(WatchAlarmProtocol.PATH_ALARM_ACK).apply {
            dataMap.putString(WatchAlarmProtocol.KEY_PAYLOAD_JSON, ackJson)
            dataMap.putLong(WatchAlarmProtocol.KEY_EVENT_TIME_MILLIS, System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()
        Wearable.getDataClient(appContext).putDataItem(request)
            .addOnSuccessListener { Log.i(TAG, "put ack data alarmId=${payload.alarmId}") }
            .addOnFailureListener { error -> Log.w(TAG, "put ack data failed", error) }
    }

    private const val TAG = "ShiftWearAlarm"
}
