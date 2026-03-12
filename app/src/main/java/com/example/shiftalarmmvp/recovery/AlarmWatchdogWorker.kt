package com.example.shiftalarmmvp.recovery

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.shiftalarmmvp.R
import com.example.shiftalarmmvp.data.AlarmDatabase
import com.example.shiftalarmmvp.data.toDomain
import com.example.shiftalarmmvp.scheduler.AlarmScheduler
import com.example.shiftalarmmvp.service.AlarmRingingService
import com.example.shiftalarmmvp.ui.AlarmLogStore
import com.example.shiftalarmmvp.ui.AlarmLogType
import com.example.shiftalarmmvp.ui.MainActivity

internal class AlarmWatchdogWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val appContext = applicationContext
        val triggerHistoryStore = TriggerHistoryStore(appContext)
        val statusStore = AlarmWatchdogStatusStore(appContext)
        val alarmLogStore = AlarmLogStore(appContext)
        val texts = recoveryStrings(appContext.resources).watchdog
        val nowMillis = System.currentTimeMillis()

        return try {
            val pendingRegistrations = triggerHistoryStore.loadPendingRegistrations()
            val overdueRegistrations = overdueTrackedRegistrations(pendingRegistrations, nowMillis)
            if (overdueRegistrations.isEmpty()) {
                statusStore.recordCleanCheck(nowMillis)
                Result.success()
            } else {
                val scheduler = AlarmScheduler(appContext)
                val enabledAlarms = AlarmDatabase.get(appContext).alarmDao().getAllEnabled()
                    .map { it.toDomain() }
                    .associateBy { it.id }
                val recentEvents = triggerHistoryStore.loadRecentEvents()
                val batteryReady = isBatteryReady(appContext)
                var firstAttentionStatus: AlarmWatchdogStatus? = null

                overdueRegistrations.forEach { registration ->
                    if (hasTriggeredEventForRegistration(recentEvents, registration)) {
                        triggerHistoryStore.removePendingRegistration(
                            alarmId = registration.alarmId,
                            expectedTriggerMillis = registration.expectedTriggerMillis,
                            eventAtMillis = nowMillis
                        )
                        return@forEach
                    }

                    val alarm = enabledAlarms[registration.alarmId]
                    if (alarm == null || !alarm.enabled) {
                        triggerHistoryStore.removePendingRegistration(
                            alarmId = registration.alarmId,
                            expectedTriggerMillis = registration.expectedTriggerMillis,
                            eventAtMillis = nowMillis
                        )
                        return@forEach
                    }

                    val cause = classifyWatchdogCause(
                        registration = registration,
                        exactReady = scheduler.canScheduleExactAlarms(),
                        batteryReady = batteryReady
                    )
                    triggerHistoryStore.removePendingRegistration(
                        alarmId = registration.alarmId,
                        expectedTriggerMillis = registration.expectedTriggerMillis,
                        eventAtMillis = nowMillis
                    )
                    triggerHistoryStore.recordMissed(
                        registration = registration,
                        detail = cause.name,
                        eventAtMillis = nowMillis
                    )
                    val missedStatus = statusStore.recordIncident(
                        registration = registration,
                        cause = cause,
                        detail = cause.name,
                        nowMillis = nowMillis
                    )
                    alarmLogStore.append(
                        alarmId = registration.alarmId,
                        label = registration.label.ifBlank { texts.unknownAlarmLabel },
                        type = AlarmLogType.WATCHDOG_MISSED_ALARM,
                        detail = missedStatus.overviewText(texts)
                    )
                    if (firstAttentionStatus == null) {
                        firstAttentionStatus = missedStatus
                    }

                    val rescheduleResult = scheduler.scheduleWithResult(alarm)
                    if (rescheduleResult.scheduled) {
                        triggerHistoryStore.recordSelfHealed(
                            registration = registration,
                            detail = cause.name,
                            eventAtMillis = nowMillis
                        )
                        val healedStatus = statusStore.recordSelfHealed(
                            registration = registration,
                            cause = cause,
                            detail = cause.name,
                            nowMillis = nowMillis
                        )
                        alarmLogStore.append(
                            alarmId = registration.alarmId,
                            label = registration.label.ifBlank { texts.unknownAlarmLabel },
                            type = AlarmLogType.WATCHDOG_SELF_HEALED,
                            detail = healedStatus.overviewText(texts)
                        )
                        if (firstAttentionStatus == null) {
                            firstAttentionStatus = healedStatus
                        }
                    }
                }

                if (firstAttentionStatus == null) {
                    statusStore.recordCleanCheck(nowMillis)
                } else {
                    postWatchdogNotification(appContext, firstAttentionStatus!!, texts)
                }
                Result.success()
            }
        } catch (error: Throwable) {
            val status = statusStore.recordCheckFailed(
                detail = error.message ?: error.javaClass.simpleName,
                nowMillis = nowMillis
            )
            alarmLogStore.append(
                alarmId = -1L,
                label = appContext.getString(R.string.app_name),
                type = AlarmLogType.WATCHDOG_CHECK_FAILED,
                detail = status.overviewText(texts)
            )
            Result.success()
        } finally {
            AlarmWatchdogScheduler.schedule(appContext)
            appContext.sendBroadcast(Intent(AlarmRingingService.ACTION_RELIABILITY_STATE_CHANGED))
        }
    }

    private fun isBatteryReady(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val powerManager = context.getSystemService(PowerManager::class.java)
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
    }

    private fun postWatchdogNotification(
        context: Context,
        status: AlarmWatchdogStatus,
        texts: AlarmWatchdogStrings
    ) {
        if (!status.needsAttention()) return
        if (!canPostNotifications(context)) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(
                NotificationChannel(
                    WATCHDOG_NOTIFICATION_CHANNEL_ID,
                    texts.notificationChannelName,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = texts.notificationChannelDescription
                }
            )
        }

        val contentIntent = PendingIntent.getActivity(
            context,
            WATCHDOG_NOTIFICATION_ID,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(MainActivity.EXTRA_OPEN_RELIABILITY_CENTER, true),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val label = status.alarmLabel.takeUnless { it.isNullOrBlank() } ?: texts.unknownAlarmLabel
        val cause = status.causeText(texts)
        val message = texts.notificationTextFormat.format(label, cause)
        val notification = NotificationCompat.Builder(context, WATCHDOG_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(texts.notificationTitle)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(context).notify(WATCHDOG_NOTIFICATION_ID, notification)
    }

    private fun canPostNotifications(context: Context): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    private companion object {
        const val WATCHDOG_NOTIFICATION_CHANNEL_ID = "alarm_watchdog_incident"
        const val WATCHDOG_NOTIFICATION_ID = 87_001
    }
}

