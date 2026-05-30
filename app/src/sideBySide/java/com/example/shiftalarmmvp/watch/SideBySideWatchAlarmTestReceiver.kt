package com.example.shiftalarmmvp.watch

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.shiftalarmmvp.data.AlarmSoundType
import com.example.shiftalarmmvp.receiver.AlarmReceiver
import com.example.shiftalarmmvp.service.AlarmRingingService
import java.util.concurrent.atomic.AtomicBoolean

class SideBySideWatchAlarmTestReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            ACTION_WATCH_PREVIEW_TEST -> sendWatchPreview(context, intent)
            ACTION_WATCH_CONTROL_TEST -> startWatchControlTest(context, intent)
            ACTION_WATCH_CONTROL_TEST_STOP -> stopWatchControlTest(context)
        }
    }

    private fun sendWatchPreview(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val finished = AtomicBoolean(false)
        Handler(Looper.getMainLooper()).postDelayed({
            if (finished.compareAndSet(false, true)) {
                Log.w(TAG, "watch preview timed out before send result")
                pendingResult.finish()
            }
        }, SEND_RESULT_TIMEOUT_MILLIS)

        val label = intent.getStringExtra(EXTRA_LABEL).orEmpty().ifBlank { DEFAULT_PREVIEW_LABEL }
        val snoozeMinutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, DEFAULT_SNOOZE_MINUTES).coerceIn(1, 60)
        val soundType = intent.getStringExtra(EXTRA_SOUND_TYPE).orEmpty().ifBlank { AlarmSoundType.ALARM.name }
        val volumePercent = intent.getIntExtra(EXTRA_VOLUME_PERCENT, DEFAULT_VOLUME_PERCENT).coerceIn(0, 100)
        val vibrationEnabled = intent.getBooleanExtra(EXTRA_VIBRATION_ENABLED, true)

        Log.i(TAG, "broadcast watch preview label=$label snoozeMinutes=$snoozeMinutes")
        WatchAlarmBridge(context).sendPreviewAlarmWithResult(
            label = label,
            snoozeMinutes = snoozeMinutes,
            soundType = soundType,
            customSoundUri = null,
            volumePercent = volumePercent,
            vibrationEnabled = vibrationEnabled
        ) { result ->
            Log.i(
                TAG,
                "watch preview result connected=${result.connectedNodeCount} " +
                    "watchApp=${result.reachableWatchAppNodeCount} " +
                    "messageAttempts=${result.messageSendAttempts} " +
                    "watchNodes=${result.reachableWatchAppNodeNames.joinToString()} " +
                    "watchAppError=${result.watchAppLookupErrorMessage} " +
                    "error=${result.errorMessage}"
            )
            if (finished.compareAndSet(false, true)) {
                pendingResult.finish()
            }
        }
    }

    private fun startWatchControlTest(context: Context, intent: Intent) {
        val label = intent.getStringExtra(EXTRA_LABEL).orEmpty().ifBlank { DEFAULT_CONTROL_LABEL }
        val snoozeMinutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, DEFAULT_SNOOZE_MINUTES).coerceIn(1, 60)
        val volumePercent = intent.getIntExtra(EXTRA_VOLUME_PERCENT, DEFAULT_VOLUME_PERCENT).coerceIn(0, 100)
        val vibrationEnabled = intent.getBooleanExtra(EXTRA_VIBRATION_ENABLED, true)
        val soundType = intent.getStringExtra(EXTRA_SOUND_TYPE).orEmpty().ifBlank { AlarmSoundType.ALARM.name }

        val serviceIntent = Intent(context, AlarmRingingService::class.java)
            .setAction(AlarmRingingService.ACTION_START)
            .putExtra(AlarmReceiver.EXTRA_ALARM_ID, WATCH_CONTROL_TEST_ALARM_ID)
            .putExtra(AlarmReceiver.EXTRA_LABEL, label)
            .putExtra(AlarmReceiver.EXTRA_SOUND_TYPE, soundType)
            .putExtra(AlarmReceiver.EXTRA_VOLUME_PERCENT, volumePercent)
            .putExtra(AlarmReceiver.EXTRA_VIBRATION_ENABLED, vibrationEnabled)
            .putExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
            .putExtra(AlarmReceiver.EXTRA_SNOOZE_MAX_COUNT, DEFAULT_SNOOZE_MAX_COUNT)
            .putExtra(AlarmReceiver.EXTRA_SNOOZE_CURRENT_COUNT, 0)

        Log.i(TAG, "broadcast watch control test start label=$label snoozeMinutes=$snoozeMinutes")
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }.onFailure { error ->
            Log.w(TAG, "broadcast watch control test start failed", error)
        }
    }

    private fun stopWatchControlTest(context: Context) {
        Log.i(TAG, "broadcast watch control test stop")
        AlarmRingingService.stop(context, WATCH_CONTROL_TEST_ALARM_ID)
    }

    companion object {
        const val ACTION_WATCH_PREVIEW_TEST = "com.example.shiftalarmmvp.action.WATCH_PREVIEW_TEST"
        const val ACTION_WATCH_CONTROL_TEST = "com.example.shiftalarmmvp.action.WATCH_CONTROL_TEST"
        const val ACTION_WATCH_CONTROL_TEST_STOP = "com.example.shiftalarmmvp.action.WATCH_CONTROL_TEST_STOP"

        const val EXTRA_LABEL = "label"
        const val EXTRA_SNOOZE_MINUTES = "snoozeMinutes"
        const val EXTRA_SOUND_TYPE = "soundType"
        const val EXTRA_VOLUME_PERCENT = "volumePercent"
        const val EXTRA_VIBRATION_ENABLED = "vibrationEnabled"

        private const val TAG = "ShiftWatchTest"
        private const val WATCH_CONTROL_TEST_ALARM_ID = 888_887L
        private const val DEFAULT_SNOOZE_MINUTES = 1
        private const val DEFAULT_SNOOZE_MAX_COUNT = 3
        private const val DEFAULT_VOLUME_PERCENT = 70
        private const val DEFAULT_PREVIEW_LABEL = "ADB watch preview"
        private const val DEFAULT_CONTROL_LABEL = "ADB watch control test"
        private const val SEND_RESULT_TIMEOUT_MILLIS = 10_000L
    }
}
