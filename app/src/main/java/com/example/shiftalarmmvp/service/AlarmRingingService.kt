package com.example.shiftalarmmvp.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.shiftalarmmvp.data.AlarmSoundType
import com.example.shiftalarmmvp.receiver.AlarmActionReceiver
import com.example.shiftalarmmvp.receiver.AlarmReceiver
import com.example.shiftalarmmvp.ui.AlarmAlertActivity
import com.example.shiftalarmmvp.ui.AlarmLogStore
import com.example.shiftalarmmvp.ui.AlarmLogType

class AlarmRingingService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var isForegroundStarted = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        val alarmId = intent?.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1L) ?: -1L
        val label = intent?.getStringExtra(AlarmReceiver.EXTRA_LABEL).orEmpty()
        val snoozeMinutes = intent?.getIntExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, 5) ?: 5
        val snoozeMaxCount = intent?.getIntExtra(AlarmReceiver.EXTRA_SNOOZE_MAX_COUNT, 0) ?: 0
        val snoozeCurrentCount = intent?.getIntExtra(AlarmReceiver.EXTRA_SNOOZE_CURRENT_COUNT, 0) ?: 0
        val customSoundUri = intent?.getStringExtra(AlarmReceiver.EXTRA_CUSTOM_SOUND_URI)
        val soundTypeName = intent?.getStringExtra(AlarmReceiver.EXTRA_SOUND_TYPE)
        val soundType = runCatching {
            AlarmSoundType.valueOf(soundTypeName ?: AlarmSoundType.ALARM.name)
        }.getOrDefault(AlarmSoundType.ALARM)
        val volumePercent = (intent?.getIntExtra(AlarmReceiver.EXTRA_VOLUME_PERCENT, 100) ?: 100).coerceIn(0, 100)
        val vibrationEnabled = intent?.getBooleanExtra(AlarmReceiver.EXTRA_VIBRATION_ENABLED, true) ?: true

        when (action) {
            ACTION_START -> {
                startRinging(
                    alarmId = alarmId,
                    label = label,
                    soundType = soundType,
                    customSoundUri = customSoundUri,
                    volumePercent = volumePercent,
                    vibrationEnabled = vibrationEnabled,
                    snoozeMinutes = snoozeMinutes,
                    snoozeMaxCount = snoozeMaxCount,
                    currentSnoozeCount = snoozeCurrentCount
                )
            }

            ACTION_STOP -> {
                AlarmLogStore(this).append(alarmId, label, AlarmLogType.STOP, "사용자 끄기")
                stopRingingAndSelf()
            }

            ACTION_SNOOZE -> {
                ensureForegroundForControl(
                    alarmId = alarmId,
                    label = label,
                    snoozeMinutes = snoozeMinutes,
                    snoozeMaxCount = snoozeMaxCount,
                    currentSnoozeCount = snoozeCurrentCount,
                    soundType = soundType,
                    customSoundUri = customSoundUri,
                    volumePercent = volumePercent,
                    vibrationEnabled = vibrationEnabled
                )
                val scheduled = scheduleSnooze(alarmId, snoozeMinutes, snoozeCurrentCount, snoozeMaxCount)
                AlarmLogStore(this).append(
                    alarmId = alarmId,
                    label = label,
                    type = if (scheduled) AlarmLogType.SNOOZE_SCHEDULED else AlarmLogType.SNOOZE_BLOCKED,
                    detail = "${snoozeMinutes}분, ${snoozeCurrentCount + 1}회차"
                )
                stopRingingAndSelf()
            }

            ACTION_ONE_MORE -> {
                ensureForegroundForControl(
                    alarmId = alarmId,
                    label = label,
                    snoozeMinutes = snoozeMinutes,
                    snoozeMaxCount = snoozeMaxCount,
                    currentSnoozeCount = snoozeCurrentCount,
                    soundType = soundType,
                    customSoundUri = customSoundUri,
                    volumePercent = volumePercent,
                    vibrationEnabled = vibrationEnabled
                )
                val scheduled = if (snoozeMaxCount > 0 && snoozeCurrentCount == snoozeMaxCount) {
                    scheduleOneMoreSnooze(alarmId, snoozeMinutes, snoozeCurrentCount, snoozeMaxCount)
                } else {
                    false
                }
                AlarmLogStore(this).append(
                    alarmId = alarmId,
                    label = label,
                    type = if (scheduled) AlarmLogType.ONE_MORE_SCHEDULED else AlarmLogType.ONE_MORE_SKIPPED,
                    detail = "${snoozeMinutes}분"
                )
                stopRingingAndSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun startRinging(
        alarmId: Long,
        label: String,
        soundType: AlarmSoundType,
        customSoundUri: String?,
        volumePercent: Int,
        vibrationEnabled: Boolean,
        snoozeMinutes: Int,
        snoozeMaxCount: Int,
        currentSnoozeCount: Int
    ) {
        if (!isForegroundStarted) {
            val started = runCatching {
                startForeground(
                    NOTIFICATION_ID,
                    buildNotification(
                        alarmId = alarmId,
                        label = label,
                        snoozeMinutes = snoozeMinutes,
                        snoozeMaxCount = snoozeMaxCount,
                        currentSnoozeCount = currentSnoozeCount,
                        soundType = soundType,
                        customSoundUri = customSoundUri,
                        volumePercent = volumePercent,
                        vibrationEnabled = vibrationEnabled
                    )
                )
                true
            }.getOrDefault(false)

            if (!started) {
                stopRingingAndSelf()
                return
            }

            isForegroundStarted = true
        }

        if (mediaPlayer?.isPlaying == true) return

        val targetVolume = volumePercent / 100f
        val candidates = buildSoundCandidates(soundType, customSoundUri)

        val played = candidates.any { uri -> tryStartPlayer(uri, targetVolume) }
        if (!played) {
            isRingingActive = false
            stopRingingAndSelf()
            return
        }

        isRingingActive = true

        if (vibrationEnabled) {
            runCatching {
                val vibe = getVibrator()
                vibrator = vibe
                val pattern = longArrayOf(0, 500, 250, 500)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibe.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibe.vibrate(pattern, 0)
                }
            }
        }
    }

    private fun buildSoundCandidates(soundType: AlarmSoundType, customSoundUri: String?): List<Uri> {
        val list = mutableListOf<Uri>()

        when (soundType) {
            AlarmSoundType.CUSTOM -> {
                customSoundUri?.let { raw -> runCatching { Uri.parse(raw) }.getOrNull() }?.let { list.add(it) }
            }

            AlarmSoundType.ALARM -> {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.let { list.add(it) }
            }

            AlarmSoundType.NOTIFICATION -> {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)?.let { list.add(it) }
            }
        }

        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.let { if (it !in list) list.add(it) }
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)?.let { if (it !in list) list.add(it) }

        return list
    }

    private fun tryStartPlayer(uri: Uri, targetVolume: Float): Boolean {
        return runCatching {
            runCatching { mediaPlayer?.stop() }
            runCatching { mediaPlayer?.release() }
            mediaPlayer = null

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                setVolume(targetVolume, targetVolume)
                setDataSource(this@AlarmRingingService, uri)
                prepare()
                start()
            }
        }.isSuccess
    }

    private fun getVibrator(): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(VibratorManager::class.java)
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
    }

    private fun stopRingingAndSelf() {
        isRingingActive = false
        runCatching { mediaPlayer?.stop() }
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null

        runCatching { vibrator?.cancel() }
        vibrator = null

        if (isForegroundStarted) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            isForegroundStarted = false
        }

        stopSelf()
    }

            private fun scheduleSnooze(alarmId: Long, snoozeMinutes: Int, currentSnoozeCount: Int, snoozeMaxCount: Int): Boolean {
        if (alarmId <= 0) return false
        if (snoozeMaxCount > 0 && currentSnoozeCount >= snoozeMaxCount) return false

        val resolvedMinutes = snoozeMinutes.coerceIn(1, 60)
        val nextCount = currentSnoozeCount + 1
        val triggerAt = System.currentTimeMillis() + resolvedMinutes * 60 * 1000L

        val am = getSystemService(AlarmManager::class.java)
        val pi = PendingIntent.getBroadcast(
            this,
            alarmId.toInt() + 70_000,
            Intent(this, AlarmReceiver::class.java).apply {
                putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
                putExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, resolvedMinutes)
                putExtra(AlarmReceiver.EXTRA_SNOOZE_MAX_COUNT, snoozeMaxCount)
                putExtra(AlarmReceiver.EXTRA_SNOOZE_CURRENT_COUNT, nextCount)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return runCatching {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            true
        }.getOrDefault(false)
    }

    private fun scheduleOneMoreSnooze(
        alarmId: Long,
        snoozeMinutes: Int,
        currentSnoozeCount: Int,
        snoozeMaxCount: Int
    ): Boolean {
        if (alarmId <= 0) return false

        val resolvedMinutes = snoozeMinutes.coerceIn(1, 60)
        val nextCount = currentSnoozeCount + 1
        val triggerAt = System.currentTimeMillis() + resolvedMinutes * 60 * 1000L

        val am = getSystemService(AlarmManager::class.java)
        val pi = PendingIntent.getBroadcast(
            this,
            alarmId.toInt() + 71_000,
            Intent(this, AlarmReceiver::class.java).apply {
                putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
                putExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, resolvedMinutes)
                putExtra(AlarmReceiver.EXTRA_SNOOZE_MAX_COUNT, snoozeMaxCount)
                putExtra(AlarmReceiver.EXTRA_SNOOZE_CURRENT_COUNT, nextCount)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return runCatching {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            true
        }.getOrDefault(false)
    }

    private fun buildNotification(
        alarmId: Long,
        label: String,
        snoozeMinutes: Int,
        snoozeMaxCount: Int,
        currentSnoozeCount: Int,
        soundType: AlarmSoundType,
        customSoundUri: String?,
        volumePercent: Int,
        vibrationEnabled: Boolean
    ): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Ringing Alarm",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)

        val contentText = if (label.isBlank()) {
            getString(com.example.shiftalarmmvp.R.string.notification_text)
        } else {
            label
        }

        val fullScreenIntent = PendingIntent.getActivity(
            this,
            alarmId.toInt() + 20_000,
            Intent(this, AlarmAlertActivity::class.java)
                .putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
                .putExtra(AlarmReceiver.EXTRA_LABEL, label)
                .putExtra(AlarmReceiver.EXTRA_SOUND_TYPE, soundType.name)
                .putExtra(AlarmReceiver.EXTRA_CUSTOM_SOUND_URI, customSoundUri)
                .putExtra(AlarmReceiver.EXTRA_VOLUME_PERCENT, volumePercent)
                .putExtra(AlarmReceiver.EXTRA_VIBRATION_ENABLED, vibrationEnabled)
                .putExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
                .putExtra(AlarmReceiver.EXTRA_SNOOZE_MAX_COUNT, snoozeMaxCount)
                .putExtra(AlarmReceiver.EXTRA_SNOOZE_CURRENT_COUNT, currentSnoozeCount)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getBroadcast(
            this,
            alarmId.toInt() + 30_000,
            Intent(this, AlarmActionReceiver::class.java)
                .setAction(ACTION_STOP)
                .putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
                .putExtra(AlarmReceiver.EXTRA_LABEL, label)
                .putExtra(AlarmReceiver.EXTRA_SOUND_TYPE, soundType.name)
                .putExtra(AlarmReceiver.EXTRA_CUSTOM_SOUND_URI, customSoundUri)
                .putExtra(AlarmReceiver.EXTRA_VOLUME_PERCENT, volumePercent)
                .putExtra(AlarmReceiver.EXTRA_VIBRATION_ENABLED, vibrationEnabled)
                .putExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
                .putExtra(AlarmReceiver.EXTRA_SNOOZE_MAX_COUNT, snoozeMaxCount)
                .putExtra(AlarmReceiver.EXTRA_SNOOZE_CURRENT_COUNT, currentSnoozeCount),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(getString(com.example.shiftalarmmvp.R.string.notification_title))
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(null)
            .setVibrate(longArrayOf())
            .setDefaults(0)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(fullScreenIntent)
            .setFullScreenIntent(fullScreenIntent, true)
            .addAction(0, getString(com.example.shiftalarmmvp.R.string.notification_stop), stopIntent)

        val hasLimit = snoozeMaxCount > 0
        val canSnooze = !hasLimit || currentSnoozeCount < snoozeMaxCount
        val canOneMore = hasLimit && currentSnoozeCount == snoozeMaxCount

        when {
            canSnooze -> {
                val actionText = if (hasLimit) {
                    getString(
                        com.example.shiftalarmmvp.R.string.notification_snooze_count,
                        snoozeMinutes,
                        currentSnoozeCount + 1,
                        snoozeMaxCount
                    )
                } else {
                    getString(com.example.shiftalarmmvp.R.string.notification_snooze_after, snoozeMinutes)
                }
                val snoozeIntent = PendingIntent.getBroadcast(
                    this,
                    alarmId.toInt() + 40_000,
                    Intent(this, AlarmActionReceiver::class.java)
                        .setAction(ACTION_SNOOZE)
                        .putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
                        .putExtra(AlarmReceiver.EXTRA_LABEL, label)
                        .putExtra(AlarmReceiver.EXTRA_SOUND_TYPE, soundType.name)
                        .putExtra(AlarmReceiver.EXTRA_CUSTOM_SOUND_URI, customSoundUri)
                        .putExtra(AlarmReceiver.EXTRA_VOLUME_PERCENT, volumePercent)
                        .putExtra(AlarmReceiver.EXTRA_VIBRATION_ENABLED, vibrationEnabled)
                        .putExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
                        .putExtra(AlarmReceiver.EXTRA_SNOOZE_MAX_COUNT, snoozeMaxCount)
                        .putExtra(AlarmReceiver.EXTRA_SNOOZE_CURRENT_COUNT, currentSnoozeCount),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(0, actionText, snoozeIntent)
            }

            canOneMore -> {
                builder.setSubText(getString(com.example.shiftalarmmvp.R.string.notification_snooze_limit))
                val oneMoreIntent = PendingIntent.getBroadcast(
                    this,
                    alarmId.toInt() + 60_000,
                    Intent(this, AlarmActionReceiver::class.java)
                        .setAction(ACTION_ONE_MORE)
                        .putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
                        .putExtra(AlarmReceiver.EXTRA_LABEL, label)
                        .putExtra(AlarmReceiver.EXTRA_SOUND_TYPE, soundType.name)
                        .putExtra(AlarmReceiver.EXTRA_CUSTOM_SOUND_URI, customSoundUri)
                        .putExtra(AlarmReceiver.EXTRA_VOLUME_PERCENT, volumePercent)
                        .putExtra(AlarmReceiver.EXTRA_VIBRATION_ENABLED, vibrationEnabled)
                        .putExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
                        .putExtra(AlarmReceiver.EXTRA_SNOOZE_MAX_COUNT, snoozeMaxCount)
                        .putExtra(AlarmReceiver.EXTRA_SNOOZE_CURRENT_COUNT, currentSnoozeCount),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(0, getString(com.example.shiftalarmmvp.R.string.notification_one_more), oneMoreIntent)
            }

            else -> {
                builder.setSubText(getString(com.example.shiftalarmmvp.R.string.notification_snooze_finished))
            }
        }

        return builder.build()
    }

    private fun ensureForegroundForControl(
        alarmId: Long,
        label: String,
        snoozeMinutes: Int,
        snoozeMaxCount: Int,
        currentSnoozeCount: Int,
        soundType: AlarmSoundType = AlarmSoundType.ALARM,
        customSoundUri: String? = null,
        volumePercent: Int = 100,
        vibrationEnabled: Boolean = true
    ) {
        if (!isForegroundStarted) {
            val started = runCatching {
                startForeground(
                    NOTIFICATION_ID,
                    buildNotification(
                        alarmId,
                        label,
                        snoozeMinutes,
                        snoozeMaxCount,
                        currentSnoozeCount,
                        soundType,
                        customSoundUri,
                        volumePercent,
                        vibrationEnabled
                    )
                )
                true
            }.getOrDefault(false)

            if (!started) {
                stopRingingAndSelf()
                return
            }

            isForegroundStarted = true
        }
    }

    override fun onDestroy() {
        isRingingActive = false
        runCatching { mediaPlayer?.stop() }
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null

        runCatching { vibrator?.cancel() }
        vibrator = null

        isForegroundStarted = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.example.shiftalarmmvp.action.START"
        const val ACTION_STOP = "com.example.shiftalarmmvp.action.STOP"
        const val ACTION_SNOOZE = "com.example.shiftalarmmvp.action.SNOOZE"
        const val ACTION_ONE_MORE = "com.example.shiftalarmmvp.action.ONE_MORE"

        private const val CHANNEL_ID = "ringing_alarm_channel_silent_v4"
        private const val NOTIFICATION_ID = 1001

        @Volatile
        private var isRingingActive: Boolean = false

        fun stop(context: Context, alarmId: Long) {
            val appContext = context.applicationContext
            runCatching {
                appContext.getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
            }

            runCatching {
                appContext.stopService(
                    Intent(appContext, AlarmRingingService::class.java)
                        .putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
                )
            }
        }

        fun snooze(
            context: Context,
            alarmId: Long,
            label: String? = null,
            snoozeMinutes: Int = 5,
            snoozeMaxCount: Int = 0,
            currentSnoozeCount: Int = 0,
            soundType: String? = null,
            customSoundUri: String? = null,
            volumePercent: Int = 100,
            vibrationEnabled: Boolean = true
        ) {
            val intent = Intent(context, AlarmRingingService::class.java)
                .setAction(ACTION_SNOOZE)
                .putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
                .putExtra(AlarmReceiver.EXTRA_LABEL, label)
                .putExtra(AlarmReceiver.EXTRA_SOUND_TYPE, soundType)
                .putExtra(AlarmReceiver.EXTRA_CUSTOM_SOUND_URI, customSoundUri)
                .putExtra(AlarmReceiver.EXTRA_VOLUME_PERCENT, volumePercent)
                .putExtra(AlarmReceiver.EXTRA_VIBRATION_ENABLED, vibrationEnabled)
                .putExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
                .putExtra(AlarmReceiver.EXTRA_SNOOZE_MAX_COUNT, snoozeMaxCount)
                .putExtra(AlarmReceiver.EXTRA_SNOOZE_CURRENT_COUNT, currentSnoozeCount)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun oneMore(
            context: Context,
            alarmId: Long,
            label: String? = null,
            snoozeMinutes: Int = 5,
            snoozeMaxCount: Int = 0,
            currentSnoozeCount: Int = 0,
            soundType: String? = null,
            customSoundUri: String? = null,
            volumePercent: Int = 100,
            vibrationEnabled: Boolean = true
        ) {
            val intent = Intent(context, AlarmRingingService::class.java)
                .setAction(ACTION_ONE_MORE)
                .putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
                .putExtra(AlarmReceiver.EXTRA_LABEL, label)
                .putExtra(AlarmReceiver.EXTRA_SOUND_TYPE, soundType)
                .putExtra(AlarmReceiver.EXTRA_CUSTOM_SOUND_URI, customSoundUri)
                .putExtra(AlarmReceiver.EXTRA_VOLUME_PERCENT, volumePercent)
                .putExtra(AlarmReceiver.EXTRA_VIBRATION_ENABLED, vibrationEnabled)
                .putExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
                .putExtra(AlarmReceiver.EXTRA_SNOOZE_MAX_COUNT, snoozeMaxCount)
                .putExtra(AlarmReceiver.EXTRA_SNOOZE_CURRENT_COUNT, currentSnoozeCount)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}





