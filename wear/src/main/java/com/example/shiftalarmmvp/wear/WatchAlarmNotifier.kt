package com.example.shiftalarmmvp.wear

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Log

object WatchAlarmNotifier {
    private const val TAG = "ShiftWearAlarm"
    private const val CHANNEL_ID = "shift_alarm_watch_alarm_v5"
    private val LEGACY_CHANNEL_IDS = arrayOf(
        "shift_alarm_watch_alarm_v1",
        "shift_alarm_watch_alarm_v2",
        "shift_alarm_watch_alarm_v3",
        "shift_alarm_watch_alarm_v4"
    )
    private val GENTLE_RAMP_VIBRATION_PATTERN =
        longArrayOf(0, 40, 620, 55, 560, 70, 500, 90, 440, 115, 380, 140)
    const val NOTIFICATION_ID = 3001

    fun show(context: Context, payload: WatchAlarmPayload): Boolean {
        val appContext = context.applicationContext
        val manager = appContext.getSystemService(NotificationManager::class.java)
        val notification = buildNotification(appContext, payload)
        if (!canPostNotifications(appContext, manager)) {
            Log.w(TAG, "show alarm notification skipped permission alarmId=${payload.alarmId}")
            return false
        }
        return runCatching {
            manager.notify(NOTIFICATION_ID, notification)
        }.onSuccess {
            Log.i(TAG, "show alarm notification alarmId=${payload.alarmId}")
        }.onFailure { error ->
            Log.w(TAG, "show alarm notification failed alarmId=${payload.alarmId}", error)
        }.isSuccess
    }

    fun showControlPending(
        context: Context,
        payload: WatchAlarmPayload,
        action: String,
        requestStartedAtMillis: Long
    ) {
        val appContext = context.applicationContext
        val manager = appContext.getSystemService(NotificationManager::class.java)
        createChannel(appContext, manager)
        if (!canPostNotifications(appContext, manager)) {
            Log.w(TAG, "show control pending notification skipped permission alarmId=${payload.alarmId}")
            return
        }

        val openIntent = PendingIntent.getActivity(
            appContext,
            requestCode(payload, 40_000),
            AlarmActivity.createPendingControlIntent(appContext, payload, action, requestStartedAtMillis),
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
            .setContentText(appContext.getString(R.string.alarm_waiting_phone_confirmation, actionLabel))
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
        useLocalVibration: Boolean = false
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
            .setOnlyAlertOnce(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
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
        }.onSuccess {
            Log.i(TAG, "cancel alarm notification")
        }
    }

    private fun createChannel(context: Context, manager: NotificationManager) {
        LEGACY_CHANNEL_IDS.forEach { legacyChannelId ->
            runCatching { manager.deleteNotificationChannel(legacyChannelId) }
                .onSuccess { Log.i(TAG, "delete legacy notification channel id=$legacyChannelId") }
        }
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            enableVibration(true)
            vibrationPattern = GENTLE_RAMP_VIBRATION_PATTERN
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    private fun canPostNotifications(context: Context, manager: NotificationManager): Boolean {
        if (!manager.areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return manager.getNotificationChannel(CHANNEL_ID)?.importance != NotificationManager.IMPORTANCE_NONE
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
