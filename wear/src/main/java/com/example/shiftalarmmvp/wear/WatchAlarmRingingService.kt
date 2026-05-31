package com.example.shiftalarmmvp.wear

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

class WatchAlarmRingingService : Service() {
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val signalTimeoutHandler = Handler(Looper.getMainLooper())
    private var signalTimeoutRunnable: Runnable? = null
    private var activeAlarmId: Long = -1L
    private var cancelNotificationOnDestroy: Boolean = true

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopRingingAndSelf()
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_STOP_KEEP_NOTIFICATION) {
            stopRingingAndSelf(removeNotification = false)
            return START_NOT_STICKY
        }

        val payload = intent?.let { WatchAlarmProtocol.parsePayload(it) }
        if (payload == null) {
            stopRingingAndSelf()
            return START_NOT_STICKY
        }

        activeAlarmId = payload.alarmId
        Log.i(TAG, "start foreground ringing alarmId=${payload.alarmId}")
        if (!startForegroundSafely(payload)) {
            Log.w(TAG, "foreground ringing unavailable, using notification fallback alarmId=${payload.alarmId}")
            WatchAlarmNotifier.show(this, payload)
            PhoneMessageBridge.sendAck(this, payload, WatchAlarmProtocol.ACK_DISPLAY_MODE_FALLBACK)
            cancelNotificationOnDestroy = false
            stopSelf()
            return START_NOT_STICKY
        }
        cancelNotificationOnDestroy = true
        acquireWakeLock(payload)
        startVibration(payload)
        scheduleSignalTimeout(payload)
        PhoneMessageBridge.sendAck(this, payload, WatchAlarmProtocol.ACK_DISPLAY_MODE_FOREGROUND_SERVICE)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        clearSignalTimeout()
        stopVibration()
        releaseWakeLock()
        val keepNotification = consumeKeepNotificationOnNextStop()
        if (cancelNotificationOnDestroy && !keepNotification) {
            WatchAlarmNotifier.cancel(this)
        }
        super.onDestroy()
    }

    private fun startVibration(payload: WatchAlarmPayload) {
        stopVibration()
        if (!payload.vibrationEnabled) return

        val vibe = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        vibrator = vibe

        val pattern = longArrayOf(0, 250, 120, 250)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibe.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibe.vibrate(pattern, -1)
        }
    }

    private fun scheduleSignalTimeout(payload: WatchAlarmPayload) {
        clearSignalTimeout()
        val timeout = Runnable {
            Log.i(TAG, "power saver stop watch signal alarmId=${payload.alarmId}")
            stopRingingAndSelf(removeNotification = false)
        }
        signalTimeoutRunnable = timeout
        signalTimeoutHandler.postDelayed(timeout, SIGNAL_SERVICE_TIMEOUT_MILLIS)
    }

    private fun clearSignalTimeout() {
        signalTimeoutRunnable?.let { signalTimeoutHandler.removeCallbacks(it) }
        signalTimeoutRunnable = null
    }

    private fun startForegroundSafely(payload: WatchAlarmPayload): Boolean {
        return runCatching {
            startForeground(
                WatchAlarmNotifier.NOTIFICATION_ID,
                WatchAlarmNotifier.buildNotification(this, payload, useLocalVibration = false)
            )
            true
        }.getOrElse { error ->
            Log.w(TAG, "startForeground failed alarmId=${payload.alarmId}", error)
            false
        }
    }

    private fun stopVibration() {
        runCatching { vibrator?.cancel() }
        vibrator = null
    }

    private fun acquireWakeLock(payload: WatchAlarmPayload) {
        releaseWakeLock()
        val powerManager = getSystemService(PowerManager::class.java)
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "$packageName:watch-alarm-ring"
        ).apply {
            setReferenceCounted(false)
            runCatching {
                acquire(WAKE_LOCK_TIMEOUT_MILLIS)
                Log.i(TAG, "acquired wake lock alarmId=${payload.alarmId}")
            }.onFailure { error ->
                Log.w(TAG, "acquire wake lock failed alarmId=${payload.alarmId}", error)
            }
        }
    }

    private fun releaseWakeLock() {
        val lock = wakeLock ?: return
        wakeLock = null
        runCatching {
            if (lock.isHeld) {
                lock.release()
                Log.i(TAG, "released wake lock alarmId=$activeAlarmId")
            }
        }.onFailure { error ->
            Log.w(TAG, "release wake lock failed alarmId=$activeAlarmId", error)
        }
    }

    private fun stopRingingAndSelf(removeNotification: Boolean = true) {
        Log.i(TAG, "stop foreground ringing alarmId=$activeAlarmId")
        clearSignalTimeout()
        cancelNotificationOnDestroy = removeNotification
        stopVibration()
        releaseWakeLock()
        if (removeNotification) {
            WatchAlarmNotifier.cancel(this)
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(STOP_FOREGROUND_DETACH)
        }
        stopSelf()
    }

    companion object {
        private const val TAG = "ShiftWearAlarm"
        private const val ACTION_START = "com.example.shiftalarmmvp.wear.action.START_RINGING"
        private const val ACTION_STOP = "com.example.shiftalarmmvp.wear.action.STOP_RINGING"
        private const val ACTION_STOP_KEEP_NOTIFICATION = "com.example.shiftalarmmvp.wear.action.STOP_RINGING_KEEP_NOTIFICATION"
        private const val SIGNAL_SERVICE_TIMEOUT_MILLIS = 15_000L
        private const val WAKE_LOCK_TIMEOUT_MILLIS = 20_000L

        fun start(context: Context, payload: WatchAlarmPayload): Boolean {
            val appContext = context.applicationContext
            val intent = Intent(appContext, WatchAlarmRingingService::class.java)
                .setAction(ACTION_START)
                .putExtra(WatchAlarmProtocol.EXTRA_PAYLOAD_JSON, WatchAlarmProtocol.toJson(payload))

            return runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appContext.startForegroundService(intent)
                } else {
                    appContext.startService(intent)
                }
                true
            }.getOrElse { error ->
                Log.w(TAG, "start foreground ringing failed alarmId=${payload.alarmId}", error)
                false
            }
        }

        fun stop(context: Context) {
            val appContext = context.applicationContext
            clearKeepNotificationOnNextStop()
            WatchAlarmNotifier.cancel(appContext)
            val stopped = runCatching {
                appContext.stopService(Intent(appContext, WatchAlarmRingingService::class.java))
            }.getOrDefault(false)
            Log.i(TAG, "request stop foreground ringing stopped=$stopped")
        }

        fun stopKeepingNotification(context: Context) {
            val appContext = context.applicationContext
            val requestedStopAction = runCatching {
                appContext.startService(
                    Intent(appContext, WatchAlarmRingingService::class.java)
                        .setAction(ACTION_STOP_KEEP_NOTIFICATION)
                ) != null
            }.getOrElse { error ->
                Log.w(TAG, "request stop foreground ringing keepNotification action failed", error)
                false
            }

            if (requestedStopAction) {
                Log.i(TAG, "request stop foreground ringing keepNotification=true action=service")
                return
            }

            setKeepNotificationOnNextStop()
            val stoppedByFallback = runCatching {
                appContext.stopService(Intent(appContext, WatchAlarmRingingService::class.java))
            }.getOrDefault(false)
            if (!stoppedByFallback) {
                clearKeepNotificationOnNextStop()
            }
            Log.i(TAG, "request stop foreground ringing keepNotification=true stopped=$stoppedByFallback")
        }

        @Volatile
        private var keepNotificationOnNextStop: Boolean = false

        @Synchronized
        private fun setKeepNotificationOnNextStop() {
            keepNotificationOnNextStop = true
        }

        @Synchronized
        private fun clearKeepNotificationOnNextStop() {
            keepNotificationOnNextStop = false
        }

        @Synchronized
        private fun consumeKeepNotificationOnNextStop(): Boolean {
            val keep = keepNotificationOnNextStop
            keepNotificationOnNextStop = false
            return keep
        }
    }
}
