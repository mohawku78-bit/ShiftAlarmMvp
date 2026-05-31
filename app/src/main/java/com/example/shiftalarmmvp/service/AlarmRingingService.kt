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
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.shiftalarmmvp.data.AlarmSoundType
import com.example.shiftalarmmvp.receiver.AlarmActionReceiver
import com.example.shiftalarmmvp.receiver.AlarmReceiver
import com.example.shiftalarmmvp.recovery.DirectBootAlarmIntent
import com.example.shiftalarmmvp.recovery.DirectBootAlarmSnapshot
import com.example.shiftalarmmvp.recovery.ReliabilityStateCoordinator
import com.example.shiftalarmmvp.scheduler.AlarmExactStrategy
import com.example.shiftalarmmvp.scheduler.scheduleRtcWakeupIntent
import com.example.shiftalarmmvp.recovery.alarmServiceStrings
import com.example.shiftalarmmvp.recovery.SELF_TEST_ALARM_ID
import com.example.shiftalarmmvp.recovery.SelfTestStatus
import com.example.shiftalarmmvp.ui.AlarmAlertActivity
import com.example.shiftalarmmvp.ui.AlarmLogStore
import com.example.shiftalarmmvp.ui.AlarmLogType
import com.example.shiftalarmmvp.watch.WatchAlarmBridge
import com.example.shiftalarmmvp.watch.WatchAlarmDiagnosticsStore

class AlarmRingingService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var isForegroundStarted = false
    private var activeAlarmId: Long = -1L
    private val watchBridgeFallbackHandler = Handler(Looper.getMainLooper())
    private var watchBridgeFallbackRunnable: Runnable? = null

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
        val directBootSnapshot = intent?.let(DirectBootAlarmIntent::fromIntent)
        val texts = alarmServiceStrings(resources)

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
                    currentSnoozeCount = snoozeCurrentCount,
                    directBootSnapshot = directBootSnapshot
                )
            }

            ACTION_STOP -> {
                AlarmLogStore(this).append(alarmId, label, AlarmLogType.STOP, texts.logStopUser)
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
                    vibrationEnabled = vibrationEnabled,
                    directBootSnapshot = directBootSnapshot
                )
                val scheduled = scheduleSnooze(
                    alarmId = alarmId,
                    label = label,
                    soundType = soundType,
                    customSoundUri = customSoundUri,
                    volumePercent = volumePercent,
                    vibrationEnabled = vibrationEnabled,
                    snoozeMinutes = snoozeMinutes,
                    currentSnoozeCount = snoozeCurrentCount,
                    snoozeMaxCount = snoozeMaxCount,
                    directBootSnapshot = directBootSnapshot
                )
                AlarmLogStore(this).append(
                    alarmId = alarmId,
                    label = label,
                    type = if (scheduled) AlarmLogType.SNOOZE_SCHEDULED else AlarmLogType.SNOOZE_BLOCKED,
                    detail = texts.logSnoozeDetailFormat.format(snoozeMinutes, snoozeCurrentCount + 1)
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
                    vibrationEnabled = vibrationEnabled,
                    directBootSnapshot = directBootSnapshot
                )
                val scheduled = if (snoozeMaxCount > 0 && snoozeCurrentCount == snoozeMaxCount) {
                    scheduleOneMoreSnooze(
                        alarmId = alarmId,
                        label = label,
                        soundType = soundType,
                        customSoundUri = customSoundUri,
                        volumePercent = volumePercent,
                        vibrationEnabled = vibrationEnabled,
                        snoozeMinutes = snoozeMinutes,
                        currentSnoozeCount = snoozeCurrentCount,
                        snoozeMaxCount = snoozeMaxCount,
                        directBootSnapshot = directBootSnapshot
                    )
                } else {
                    false
                }
                AlarmLogStore(this).append(
                    alarmId = alarmId,
                    label = label,
                    type = if (scheduled) AlarmLogType.ONE_MORE_SCHEDULED else AlarmLogType.ONE_MORE_SKIPPED,
                    detail = texts.logOneMoreDetailFormat.format(snoozeMinutes)
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
        currentSnoozeCount: Int,
        directBootSnapshot: DirectBootAlarmSnapshot?
    ) {
        val watchTriggeredAtMillis = System.currentTimeMillis()
        activeAlarmId = alarmId
        activeRingingAlarmId = alarmId
        activeRingingTriggeredAtMillis = watchTriggeredAtMillis

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
                        vibrationEnabled = vibrationEnabled,
                        directBootSnapshot = directBootSnapshot
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

        scheduleWatchBridgeFallbackNotification(
            alarmId = alarmId,
            label = label,
            snoozeMinutes = snoozeMinutes,
            snoozeMaxCount = snoozeMaxCount,
            currentSnoozeCount = currentSnoozeCount,
            soundType = soundType,
            customSoundUri = customSoundUri,
            volumePercent = volumePercent,
            vibrationEnabled = vibrationEnabled,
            directBootSnapshot = directBootSnapshot,
            triggeredAtMillis = watchTriggeredAtMillis
        )

        WatchAlarmBridge(this).sendAlarmStarted(
            alarmId = alarmId,
            label = label,
            snoozeMinutes = snoozeMinutes,
            snoozeMaxCount = snoozeMaxCount,
            currentSnoozeCount = currentSnoozeCount,
            soundType = soundType.name,
            customSoundUri = customSoundUri,
            volumePercent = volumePercent,
            vibrationEnabled = vibrationEnabled,
            snoozeAllowed = alarmId != SELF_TEST_ALARM_ID,
            triggeredAtMillis = watchTriggeredAtMillis
        )

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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibe.vibrate(
                        VibrationEffect.createWaveform(
                            AlarmVibrationPatterns.PHONE_RAMP_TIMINGS,
                            AlarmVibrationPatterns.PHONE_RAMP_AMPLITUDES,
                            AlarmVibrationPatterns.PHONE_RAMP_REPEAT_INDEX
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibe.vibrate(AlarmVibrationPatterns.PHONE_RAMP_TIMINGS, AlarmVibrationPatterns.PHONE_RAMP_REPEAT_INDEX)
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
        activeAlarmId.takeIf { it > 0L }?.let { alarmId ->
            WatchAlarmBridge(this).sendAlarmCancelled(
                alarmId = alarmId,
                triggeredAtMillis = activeRingingTriggeredAtMillis
            )
        }
        activeAlarmId = -1L
        activeRingingAlarmId = -1L
        activeRingingTriggeredAtMillis = 0L
        runCatching { mediaPlayer?.stop() }
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null

        runCatching { vibrator?.cancel() }
        vibrator = null

        cancelWatchBridgeNotification()

        if (isForegroundStarted) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            isForegroundStarted = false
        }

        stopSelf()
    }
    private fun scheduleSnooze(
        alarmId: Long,
        label: String,
        soundType: AlarmSoundType,
        customSoundUri: String?,
        volumePercent: Int,
        vibrationEnabled: Boolean,
        snoozeMinutes: Int,
        currentSnoozeCount: Int,
        snoozeMaxCount: Int,
        directBootSnapshot: DirectBootAlarmSnapshot?
    ): Boolean {
        if (alarmId <= 0) return false
        if (snoozeMaxCount > 0 && currentSnoozeCount >= snoozeMaxCount) return false

        val resolvedMinutes = snoozeMinutes.coerceIn(1, 60)
        val nextCount = currentSnoozeCount + 1
        val triggerAt = System.currentTimeMillis() + resolvedMinutes * 60 * 1000L
        val alarmManager = getSystemService(AlarmManager::class.java)
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_LABEL, label)
            putExtra(AlarmReceiver.EXTRA_SOUND_TYPE, soundType.name)
            putExtra(AlarmReceiver.EXTRA_CUSTOM_SOUND_URI, customSoundUri)
            putExtra(AlarmReceiver.EXTRA_VOLUME_PERCENT, volumePercent)
            putExtra(AlarmReceiver.EXTRA_VIBRATION_ENABLED, vibrationEnabled)
            putExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, resolvedMinutes)
            putExtra(AlarmReceiver.EXTRA_SNOOZE_MAX_COUNT, snoozeMaxCount)
            putExtra(AlarmReceiver.EXTRA_SNOOZE_CURRENT_COUNT, nextCount)
        }
        directBootSnapshot?.let {
            DirectBootAlarmIntent.putSnapshot(intent, it, triggerAt, directBootFallback = true)
            intent.putExtra(AlarmReceiver.EXTRA_SNOOZE_CURRENT_COUNT, nextCount)
        }
        val pi = PendingIntent.getBroadcast(
            this,
            alarmId.toInt() + 70_000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return scheduleRtcWakeupIntent(
            alarmManager = alarmManager,
            triggerMillis = triggerAt,
            operation = pi,
            canScheduleExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager?.canScheduleExactAlarms() == true,
            sdkInt = Build.VERSION.SDK_INT,
            exactStrategy = AlarmExactStrategy.EXACT_ALLOW_WHILE_IDLE
        ).scheduled
    }

    private fun scheduleOneMoreSnooze(
        alarmId: Long,
        label: String,
        soundType: AlarmSoundType,
        customSoundUri: String?,
        volumePercent: Int,
        vibrationEnabled: Boolean,
        snoozeMinutes: Int,
        currentSnoozeCount: Int,
        snoozeMaxCount: Int,
        directBootSnapshot: DirectBootAlarmSnapshot?
    ): Boolean {
        if (alarmId <= 0) return false

        val resolvedMinutes = snoozeMinutes.coerceIn(1, 60)
        val nextCount = currentSnoozeCount + 1
        val triggerAt = System.currentTimeMillis() + resolvedMinutes * 60 * 1000L
        val alarmManager = getSystemService(AlarmManager::class.java)
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_LABEL, label)
            putExtra(AlarmReceiver.EXTRA_SOUND_TYPE, soundType.name)
            putExtra(AlarmReceiver.EXTRA_CUSTOM_SOUND_URI, customSoundUri)
            putExtra(AlarmReceiver.EXTRA_VOLUME_PERCENT, volumePercent)
            putExtra(AlarmReceiver.EXTRA_VIBRATION_ENABLED, vibrationEnabled)
            putExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, resolvedMinutes)
            putExtra(AlarmReceiver.EXTRA_SNOOZE_MAX_COUNT, snoozeMaxCount)
            putExtra(AlarmReceiver.EXTRA_SNOOZE_CURRENT_COUNT, nextCount)
        }
        directBootSnapshot?.let {
            DirectBootAlarmIntent.putSnapshot(intent, it, triggerAt, directBootFallback = true)
            intent.putExtra(AlarmReceiver.EXTRA_SNOOZE_CURRENT_COUNT, nextCount)
        }
        val pi = PendingIntent.getBroadcast(
            this,
            alarmId.toInt() + 71_000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return scheduleRtcWakeupIntent(
            alarmManager = alarmManager,
            triggerMillis = triggerAt,
            operation = pi,
            canScheduleExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager?.canScheduleExactAlarms() == true,
            sdkInt = Build.VERSION.SDK_INT,
            exactStrategy = AlarmExactStrategy.EXACT_ALLOW_WHILE_IDLE
        ).scheduled
    }

    private fun createAlarmActionPendingIntent(
        action: String,
        requestOffset: Int,
        alarmId: Long,
        label: String,
        soundType: AlarmSoundType,
        customSoundUri: String?,
        volumePercent: Int,
        vibrationEnabled: Boolean,
        snoozeMinutes: Int,
        snoozeMaxCount: Int,
        currentSnoozeCount: Int,
        directBootSnapshot: DirectBootAlarmSnapshot?
    ): PendingIntent {
        val actionIntent = Intent(this, AlarmActionReceiver::class.java)
            .setAction(action)
            .putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            .putExtra(AlarmReceiver.EXTRA_LABEL, label)
            .putExtra(AlarmReceiver.EXTRA_SOUND_TYPE, soundType.name)
            .putExtra(AlarmReceiver.EXTRA_CUSTOM_SOUND_URI, customSoundUri)
            .putExtra(AlarmReceiver.EXTRA_VOLUME_PERCENT, volumePercent)
            .putExtra(AlarmReceiver.EXTRA_VIBRATION_ENABLED, vibrationEnabled)
            .putExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
            .putExtra(AlarmReceiver.EXTRA_SNOOZE_MAX_COUNT, snoozeMaxCount)
            .putExtra(AlarmReceiver.EXTRA_SNOOZE_CURRENT_COUNT, currentSnoozeCount)
        directBootSnapshot?.let {
            DirectBootAlarmIntent.putSnapshot(actionIntent, it, null, directBootFallback = true)
            actionIntent.putExtra(AlarmReceiver.EXTRA_SNOOZE_CURRENT_COUNT, currentSnoozeCount)
        }

        return PendingIntent.getBroadcast(
            this,
            alarmId.toInt() + requestOffset,
            actionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationAction(
        title: String,
        pendingIntent: PendingIntent
    ): NotificationCompat.Action {
        return NotificationCompat.Action.Builder(0, title, pendingIntent).build()
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
        vibrationEnabled: Boolean,
        directBootSnapshot: DirectBootAlarmSnapshot?
    ): Notification {
        val texts = alarmServiceStrings(resources)
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            texts.channelName,
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

        val fullScreenActivityIntent = Intent(this, AlarmAlertActivity::class.java)
            .putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            .putExtra(AlarmReceiver.EXTRA_LABEL, label)
            .putExtra(AlarmReceiver.EXTRA_SOUND_TYPE, soundType.name)
            .putExtra(AlarmReceiver.EXTRA_CUSTOM_SOUND_URI, customSoundUri)
            .putExtra(AlarmReceiver.EXTRA_VOLUME_PERCENT, volumePercent)
            .putExtra(AlarmReceiver.EXTRA_VIBRATION_ENABLED, vibrationEnabled)
            .putExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
            .putExtra(AlarmReceiver.EXTRA_SNOOZE_MAX_COUNT, snoozeMaxCount)
            .putExtra(AlarmReceiver.EXTRA_SNOOZE_CURRENT_COUNT, currentSnoozeCount)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        directBootSnapshot?.let {
            DirectBootAlarmIntent.putSnapshot(fullScreenActivityIntent, it, null, directBootFallback = true)
            fullScreenActivityIntent.putExtra(AlarmReceiver.EXTRA_SNOOZE_CURRENT_COUNT, currentSnoozeCount)
        }
        val fullScreenIntent = PendingIntent.getActivity(
            this,
            alarmId.toInt() + 20_000,
            fullScreenActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopActionIntent = Intent(this, AlarmActionReceiver::class.java)
            .setAction(ACTION_STOP)
            .putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            .putExtra(AlarmReceiver.EXTRA_LABEL, label)
            .putExtra(AlarmReceiver.EXTRA_SOUND_TYPE, soundType.name)
            .putExtra(AlarmReceiver.EXTRA_CUSTOM_SOUND_URI, customSoundUri)
            .putExtra(AlarmReceiver.EXTRA_VOLUME_PERCENT, volumePercent)
            .putExtra(AlarmReceiver.EXTRA_VIBRATION_ENABLED, vibrationEnabled)
            .putExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
            .putExtra(AlarmReceiver.EXTRA_SNOOZE_MAX_COUNT, snoozeMaxCount)
            .putExtra(AlarmReceiver.EXTRA_SNOOZE_CURRENT_COUNT, currentSnoozeCount)
        directBootSnapshot?.let {
            DirectBootAlarmIntent.putSnapshot(stopActionIntent, it, null, directBootFallback = true)
            stopActionIntent.putExtra(AlarmReceiver.EXTRA_SNOOZE_CURRENT_COUNT, currentSnoozeCount)
        }
        val stopIntent = PendingIntent.getBroadcast(
            this,
            alarmId.toInt() + 30_000,
            stopActionIntent,
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

        val isSelfTestAlarm = alarmId == SELF_TEST_ALARM_ID
        if (isSelfTestAlarm) {
            builder.setSubText(texts.selfTestSubText)
        } else {
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
                    val snoozeActionIntent = Intent(this, AlarmActionReceiver::class.java)
                        .setAction(ACTION_SNOOZE)
                        .putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
                        .putExtra(AlarmReceiver.EXTRA_LABEL, label)
                        .putExtra(AlarmReceiver.EXTRA_SOUND_TYPE, soundType.name)
                        .putExtra(AlarmReceiver.EXTRA_CUSTOM_SOUND_URI, customSoundUri)
                        .putExtra(AlarmReceiver.EXTRA_VOLUME_PERCENT, volumePercent)
                        .putExtra(AlarmReceiver.EXTRA_VIBRATION_ENABLED, vibrationEnabled)
                        .putExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
                        .putExtra(AlarmReceiver.EXTRA_SNOOZE_MAX_COUNT, snoozeMaxCount)
                        .putExtra(AlarmReceiver.EXTRA_SNOOZE_CURRENT_COUNT, currentSnoozeCount)
                    directBootSnapshot?.let {
                        DirectBootAlarmIntent.putSnapshot(snoozeActionIntent, it, null, directBootFallback = true)
                        snoozeActionIntent.putExtra(AlarmReceiver.EXTRA_SNOOZE_CURRENT_COUNT, currentSnoozeCount)
                    }
                    val snoozeIntent = PendingIntent.getBroadcast(
                        this,
                        alarmId.toInt() + 40_000,
                        snoozeActionIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    builder.addAction(0, actionText, snoozeIntent)
                }

                canOneMore -> {
                    builder.setSubText(getString(com.example.shiftalarmmvp.R.string.notification_snooze_limit))
                    val oneMoreActionIntent = Intent(this, AlarmActionReceiver::class.java)
                        .setAction(ACTION_ONE_MORE)
                        .putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
                        .putExtra(AlarmReceiver.EXTRA_LABEL, label)
                        .putExtra(AlarmReceiver.EXTRA_SOUND_TYPE, soundType.name)
                        .putExtra(AlarmReceiver.EXTRA_CUSTOM_SOUND_URI, customSoundUri)
                        .putExtra(AlarmReceiver.EXTRA_VOLUME_PERCENT, volumePercent)
                        .putExtra(AlarmReceiver.EXTRA_VIBRATION_ENABLED, vibrationEnabled)
                        .putExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
                        .putExtra(AlarmReceiver.EXTRA_SNOOZE_MAX_COUNT, snoozeMaxCount)
                        .putExtra(AlarmReceiver.EXTRA_SNOOZE_CURRENT_COUNT, currentSnoozeCount)
                    directBootSnapshot?.let {
                        DirectBootAlarmIntent.putSnapshot(oneMoreActionIntent, it, null, directBootFallback = true)
                        oneMoreActionIntent.putExtra(AlarmReceiver.EXTRA_SNOOZE_CURRENT_COUNT, currentSnoozeCount)
                    }
                    val oneMoreIntent = PendingIntent.getBroadcast(
                        this,
                        alarmId.toInt() + 60_000,
                        oneMoreActionIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    builder.addAction(0, getString(com.example.shiftalarmmvp.R.string.notification_one_more), oneMoreIntent)
                }

                else -> {
                    builder.setSubText(getString(com.example.shiftalarmmvp.R.string.notification_snooze_finished))
                }
            }
        }

        return builder.build()
    }

    private fun scheduleWatchBridgeFallbackNotification(
        alarmId: Long,
        label: String,
        snoozeMinutes: Int,
        snoozeMaxCount: Int,
        currentSnoozeCount: Int,
        soundType: AlarmSoundType,
        customSoundUri: String?,
        volumePercent: Int,
        vibrationEnabled: Boolean,
        directBootSnapshot: DirectBootAlarmSnapshot?,
        triggeredAtMillis: Long
    ) {
        watchBridgeFallbackRunnable?.let(watchBridgeFallbackHandler::removeCallbacks)
        val runnable = Runnable {
            if (!isRinging(alarmId, triggeredAtMillis)) return@Runnable
            val nativeWatchHandledAlarm = WatchAlarmDiagnosticsStore(this).hasHandledDisplayAckFor(
                alarmId = alarmId,
                triggeredAtMillis = triggeredAtMillis,
                sinceMillis = triggeredAtMillis
            )
            if (nativeWatchHandledAlarm) return@Runnable

            postWatchBridgeNotification(
                alarmId = alarmId,
                label = label,
                snoozeMinutes = snoozeMinutes,
                snoozeMaxCount = snoozeMaxCount,
                currentSnoozeCount = currentSnoozeCount,
                soundType = soundType,
                customSoundUri = customSoundUri,
                volumePercent = volumePercent,
                vibrationEnabled = vibrationEnabled,
                directBootSnapshot = directBootSnapshot
            )
        }
        watchBridgeFallbackRunnable = runnable
        watchBridgeFallbackHandler.postDelayed(runnable, WATCH_BRIDGE_FALLBACK_DELAY_MILLIS)
    }

    private fun postWatchBridgeNotification(
        alarmId: Long,
        label: String,
        snoozeMinutes: Int,
        snoozeMaxCount: Int,
        currentSnoozeCount: Int,
        soundType: AlarmSoundType,
        customSoundUri: String?,
        volumePercent: Int,
        vibrationEnabled: Boolean,
        directBootSnapshot: DirectBootAlarmSnapshot?
    ) {
        val notifier = NotificationManagerCompat.from(this)
        if (!notifier.areNotificationsEnabled()) return

        runCatching {
            notifier.notify(
                WATCH_BRIDGE_NOTIFICATION_ID,
                buildWatchBridgeNotification(
                    alarmId = alarmId,
                    label = label,
                    snoozeMinutes = snoozeMinutes,
                    snoozeMaxCount = snoozeMaxCount,
                    currentSnoozeCount = currentSnoozeCount,
                    soundType = soundType,
                    customSoundUri = customSoundUri,
                    volumePercent = volumePercent,
                    vibrationEnabled = vibrationEnabled,
                    directBootSnapshot = directBootSnapshot
                )
            )
        }
    }

    private fun buildWatchBridgeNotification(
        alarmId: Long,
        label: String,
        snoozeMinutes: Int,
        snoozeMaxCount: Int,
        currentSnoozeCount: Int,
        soundType: AlarmSoundType,
        customSoundUri: String?,
        volumePercent: Int,
        vibrationEnabled: Boolean,
        directBootSnapshot: DirectBootAlarmSnapshot?
    ): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        WATCH_BRIDGE_LEGACY_CHANNEL_IDS.forEach { legacyChannelId ->
            runCatching { manager.deleteNotificationChannel(legacyChannelId) }
        }
        val channel = NotificationChannel(
            WATCH_BRIDGE_CHANNEL_ID,
            getString(com.example.shiftalarmmvp.R.string.notification_watch_bridge_channel_name),
            NotificationManager.IMPORTANCE_MAX
        ).apply {
            enableVibration(true)
            vibrationPattern = AlarmVibrationPatterns.WATCH_BRIDGE_RAMP_PATTERN
            description = getString(com.example.shiftalarmmvp.R.string.notification_watch_bridge_channel_description)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)

        val contentText = if (label.isBlank()) {
            getString(com.example.shiftalarmmvp.R.string.notification_text)
        } else {
            label
        }
        val contentIntent = PendingIntent.getActivity(
            this,
            alarmId.toInt() + 120_000,
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
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .also { openIntent ->
                    directBootSnapshot?.let {
                        DirectBootAlarmIntent.putSnapshot(openIntent, it, null, directBootFallback = true)
                        openIntent.putExtra(AlarmReceiver.EXTRA_SNOOZE_CURRENT_COUNT, currentSnoozeCount)
                    }
                },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val actions = mutableListOf<NotificationCompat.Action>()
        actions += createNotificationAction(
            getString(com.example.shiftalarmmvp.R.string.notification_stop),
            createAlarmActionPendingIntent(
                action = ACTION_STOP,
                requestOffset = 130_000,
                alarmId = alarmId,
                label = label,
                soundType = soundType,
                customSoundUri = customSoundUri,
                volumePercent = volumePercent,
                vibrationEnabled = vibrationEnabled,
                snoozeMinutes = snoozeMinutes,
                snoozeMaxCount = snoozeMaxCount,
                currentSnoozeCount = currentSnoozeCount,
                directBootSnapshot = directBootSnapshot
            )
        )

        val isSelfTestAlarm = alarmId == SELF_TEST_ALARM_ID
        if (!isSelfTestAlarm) {
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
                    actions += createNotificationAction(
                        actionText,
                        createAlarmActionPendingIntent(
                            action = ACTION_SNOOZE,
                            requestOffset = 140_000,
                            alarmId = alarmId,
                            label = label,
                            soundType = soundType,
                            customSoundUri = customSoundUri,
                            volumePercent = volumePercent,
                            vibrationEnabled = vibrationEnabled,
                            snoozeMinutes = snoozeMinutes,
                            snoozeMaxCount = snoozeMaxCount,
                            currentSnoozeCount = currentSnoozeCount,
                            directBootSnapshot = directBootSnapshot
                        )
                    )
                }

                canOneMore -> {
                    actions += createNotificationAction(
                        getString(com.example.shiftalarmmvp.R.string.notification_one_more),
                        createAlarmActionPendingIntent(
                            action = ACTION_ONE_MORE,
                            requestOffset = 160_000,
                            alarmId = alarmId,
                            label = label,
                            soundType = soundType,
                            customSoundUri = customSoundUri,
                            volumePercent = volumePercent,
                            vibrationEnabled = vibrationEnabled,
                            snoozeMinutes = snoozeMinutes,
                            snoozeMaxCount = snoozeMaxCount,
                            currentSnoozeCount = currentSnoozeCount,
                            directBootSnapshot = directBootSnapshot
                        )
                    )
                }
            }
        }

        val wearableExtender = NotificationCompat.WearableExtender()
        actions.forEach { wearableExtender.addAction(it) }

        return NotificationCompat.Builder(this, WATCH_BRIDGE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(getString(com.example.shiftalarmmvp.R.string.notification_title))
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSilent(false)
            .setVibrate(AlarmVibrationPatterns.WATCH_BRIDGE_RAMP_PATTERN)
            .setDefaults(0)
            .setOnlyAlertOnce(false)
            .setLocalOnly(false)
            .setOngoing(false)
            .setAutoCancel(false)
            .setContentIntent(contentIntent)
            .setTimeoutAfter(WATCH_BRIDGE_TIMEOUT_MILLIS)
            .also { builder -> actions.forEach { builder.addAction(it) } }
            .extend(wearableExtender)
            .build()
    }

    private fun cancelWatchBridgeNotification() {
        watchBridgeFallbackRunnable?.let(watchBridgeFallbackHandler::removeCallbacks)
        watchBridgeFallbackRunnable = null
        cancelWatchBridgeNotification(this)
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
        vibrationEnabled: Boolean = true,
        directBootSnapshot: DirectBootAlarmSnapshot? = null
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
                        vibrationEnabled,
                        directBootSnapshot
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
        activeAlarmId.takeIf { it > 0L }?.let { alarmId ->
            WatchAlarmBridge(this).sendAlarmCancelled(
                alarmId = alarmId,
                triggeredAtMillis = activeRingingTriggeredAtMillis
            )
        }
        activeAlarmId = -1L
        activeRingingAlarmId = -1L
        activeRingingTriggeredAtMillis = 0L
        runCatching { mediaPlayer?.stop() }
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null

        runCatching { vibrator?.cancel() }
        vibrator = null

        cancelWatchBridgeNotification()

        isForegroundStarted = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.example.shiftalarmmvp.action.START"
        const val ACTION_STOP = "com.example.shiftalarmmvp.action.STOP"
        const val ACTION_SNOOZE = "com.example.shiftalarmmvp.action.SNOOZE"
        const val ACTION_ONE_MORE = "com.example.shiftalarmmvp.action.ONE_MORE"
        const val ACTION_DISMISS_ALERT = "com.example.shiftalarmmvp.action.DISMISS_ALERT"
        const val ACTION_RELIABILITY_STATE_CHANGED = "com.example.shiftalarmmvp.action.RELIABILITY_STATE_CHANGED"

        private const val CHANNEL_ID = "ringing_alarm_channel_silent_v4"
        private const val WATCH_BRIDGE_CHANNEL_ID = "watch_alarm_bridge_channel_v5"
        private val WATCH_BRIDGE_LEGACY_CHANNEL_IDS = arrayOf(
            "watch_alarm_bridge_channel_v2",
            "watch_alarm_bridge_channel_v3",
            "watch_alarm_bridge_channel_v4"
        )
        private const val NOTIFICATION_ID = 1001
        private const val WATCH_BRIDGE_NOTIFICATION_ID = 1002
        private const val WATCH_BRIDGE_FALLBACK_DELAY_MILLIS = 6_000L
        private const val WATCH_BRIDGE_TIMEOUT_MILLIS = 2 * 60 * 1000L
        @Volatile
        private var isRingingActive: Boolean = false
        @Volatile
        private var activeRingingAlarmId: Long = -1L
        @Volatile
        private var activeRingingTriggeredAtMillis: Long = 0L

        fun isRinging(alarmId: Long): Boolean {
            return isRingingActive && activeRingingAlarmId == alarmId
        }

        fun isRinging(alarmId: Long, triggeredAtMillis: Long): Boolean {
            return isRinging(alarmId) &&
                triggeredAtMillis > 0L &&
                activeRingingTriggeredAtMillis == triggeredAtMillis
        }

        fun cancelWatchBridgeFallback(context: Context, alarmId: Long, triggeredAtMillis: Long) {
            if (!isRinging(alarmId, triggeredAtMillis)) return
            cancelWatchBridgeNotification(context)
        }

        private fun cancelWatchBridgeNotification(context: Context) {
            runCatching {
                context.applicationContext
                    .getSystemService(NotificationManager::class.java)
                    ?.cancel(WATCH_BRIDGE_NOTIFICATION_ID)
            }
        }

        fun stop(context: Context, alarmId: Long) {
            val appContext = context.applicationContext
            if (alarmId == SELF_TEST_ALARM_ID) {
                val coordinator = ReliabilityStateCoordinator(appContext)
                val selfTestEvent = coordinator.snapshot().selfTestStatus?.lastEvent
                val shouldPass = selfTestEvent == SelfTestStatus.Event.TRIGGERED ||
                    selfTestEvent == SelfTestStatus.Event.SCHEDULED
                if (shouldPass) {
                    coordinator.recordSelfTestFeedback(SelfTestStatus.Feedback.PASSED)
                }
            }

            runCatching {
                appContext.getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
            }

            runCatching {
                appContext.getSystemService(NotificationManager::class.java)?.cancel(WATCH_BRIDGE_NOTIFICATION_ID)
            }

            WatchAlarmBridge(appContext).sendAlarmCancelled(
                alarmId = alarmId,
                triggeredAtMillis = activeRingingTriggeredAtMillis
            )

            runCatching {
                appContext.stopService(
                    Intent(appContext, AlarmRingingService::class.java)
                        .putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
                )
            }

            runCatching {
                appContext.sendBroadcast(
                    Intent(ACTION_DISMISS_ALERT)
                        .setPackage(appContext.packageName)
                        .putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
                )
            }

            runCatching {
                appContext.sendBroadcast(
                    Intent(ACTION_RELIABILITY_STATE_CHANGED)
                        .setPackage(appContext.packageName)
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



