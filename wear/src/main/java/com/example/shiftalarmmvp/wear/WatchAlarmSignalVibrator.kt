package com.example.shiftalarmmvp.wear

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

object WatchAlarmSignalVibrator {
    private const val TAG = "ShiftWearAlarm"
    private var vibrator: Vibrator? = null

    fun startOneShot(context: Context, payload: WatchAlarmPayload) {
        cancel()
        if (!payload.vibrationEnabled) return

        val appContext = context.applicationContext
        val vibe = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            appContext.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator = vibe

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibe.vibrate(
                    VibrationEffect.createWaveform(
                        WatchVibrationPatterns.NOTIFICATION_RAMP_PATTERN,
                        WatchVibrationPatterns.NOTIFICATION_RAMP_AMPLITUDES,
                        -1
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibe.vibrate(WatchVibrationPatterns.NOTIFICATION_RAMP_PATTERN, -1)
            }
        }.onSuccess {
            Log.i(TAG, "start one-shot alarm vibration alarmId=${payload.alarmId}")
        }.onFailure { error ->
            Log.w(TAG, "start one-shot alarm vibration failed alarmId=${payload.alarmId}", error)
        }
    }

    fun cancel() {
        runCatching { vibrator?.cancel() }
        vibrator = null
    }
}
