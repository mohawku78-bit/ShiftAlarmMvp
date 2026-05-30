package com.example.shiftalarmmvp.wear

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.util.Log

object WatchAlarmNotifier {
    private const val TAG = "ShiftWearAlarm"
    private const val CHANNEL_ID = "shift_alarm_watch_alarm_v1"
    const val NOTIFICATION_ID = 3001

    fun show(context: Context, payload: WatchAlarmPayload) {
        val appContext = context.applicationContext
        val manager = appContext.getSystemService(NotificationManager::class.java)
        runCatching {
            manager.notify(NOTIFICATION_ID, buildNotification(appContext, payload))
        }.onSuccess {
            Log.i(TAG, "show fallback notification alarmId=${payload.alarmId}")
        }.onFailure { error ->
            Log.w(TAG, "show fallback notification failed alarmId=${payload.alarmId}", error)
        }
    }

    fun showControlPending(context: Context, payload: WatchAlarmPayload, action: String) {
        val appContext = context.applicationContext
        val manager = appContext.getSystemService(NotificationManager::class.java)
        createChannel(appContext, manager)

        val openIntent = PendingIntent.getActivity(
            appContext,
            requestCode(payload, 40_000),
            AlarmActivity.createIntent(appContext, payload, useLocalVibration = false),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val actionLabel = if (action == WatchAlarmProtocol.PATH_ALARM_SNOOZE) {
            appContext.getString(R.string.alarm_snooze)
        } else {
            appContext.getString(R.string.alarm_stop)
        }
        val notification = Notification.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_watch_alarm)
            .setContentTitle(appContext.getString(R.string.alarm_title))
            .setContentText("Waiting for phone confirmation: $actionLabel")
            .setCategory(Notification.CATEGORY_ALARM)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(openIntent)
            .build()

        runCatching {
            manager.notify(NOTIFICATION_ID, notification)
        }.onSuccess {
            Log.i(TAG, "show control pending notification action=$action alarmId=${payload.alarmId}")
        }.onFailure { error ->
            Log.w(TAG, "show control pending notification failed alarmId=${payload.alarmId}", error)
        }
    }

    fun buildNotification(
        context: Context,
        payload: WatchAlarmPayload,
        useLocalVibration: Boolean = true
    ): Notification {
        val appContext = context.applicationContext
        val manager = appContext.getSystemService(NotificationManager::class.java)
        createChannel(appContext, manager)

        val openIntent = PendingIntent.getActivity(
            appContext,
            requestCode(payload, 10_000),
            AlarmActivity.createIntent(appContext, payload, useLocalVibration = useLocalVibration),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = Notification.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_watch_alarm)
            .setContentTitle(appContext.getString(R.string.alarm_title))
            .setContentText(payload.label.ifBlank { appContext.getString(R.string.alarm_default_label) })
            .setCategory(Notification.CATEGORY_ALARM)
            .setOngoing(true)
            .setOnlyAlertOnce(false)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setFullScreenIntent(openIntent, true)
            .setContentIntent(openIntent)
            .addAction(
                createAction(
                    context = appContext,
                    title = appContext.getString(R.string.alarm_stop),
                    action = WatchAlarmActions.ACTION_STOP,
                    requestCode = requestCode(payload, 20_000),
                    payload = payload
                )
            )

        if (payload.canSnooze) {
            builder.addAction(
                createAction(
                    context = appContext,
                    title = appContext.getString(R.string.alarm_snooze),
                    action = WatchAlarmActions.ACTION_SNOOZE,
                    requestCode = requestCode(payload, 30_000),
                    payload = payload
                )
            )
        }

        return builder.build()
    }

    fun cancel(context: Context) {
        runCatching {
            context.applicationContext
                .getSystemService(NotificationManager::class.java)
                .cancel(NOTIFICATION_ID)
        }
    }

    private fun createChannel(context: Context, manager: NotificationManager) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 650, 180, 650, 420)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    private fun createAction(
        context: Context,
        title: String,
        action: String,
        requestCode: Int,
        payload: WatchAlarmPayload
    ): Notification.Action {
        val intent = Intent(context, WatchAlarmActionReceiver::class.java)
            .setAction(action)
            .putExtra(WatchAlarmProtocol.EXTRA_PAYLOAD_JSON, WatchAlarmProtocol.toJson(payload))
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val icon = Icon.createWithResource(context, R.drawable.ic_watch_alarm)
        return Notification.Action.Builder(icon, title, pendingIntent).build()
    }

    private fun requestCode(payload: WatchAlarmPayload, offset: Int): Int {
        return (payload.alarmId % 1_000_000L).toInt() + offset
    }
}
