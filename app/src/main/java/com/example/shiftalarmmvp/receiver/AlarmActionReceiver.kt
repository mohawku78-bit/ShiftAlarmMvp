package com.example.shiftalarmmvp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.shiftalarmmvp.recovery.DirectBootAlarmIntent
import com.example.shiftalarmmvp.service.AlarmRingingService

class AlarmActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val safeIntent = intent ?: return

        val action = safeIntent.action ?: return
        val alarmId = safeIntent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1L)
        val label = safeIntent.getStringExtra(AlarmReceiver.EXTRA_LABEL)
        val soundType = safeIntent.getStringExtra(AlarmReceiver.EXTRA_SOUND_TYPE)
        val customSoundUri = safeIntent.getStringExtra(AlarmReceiver.EXTRA_CUSTOM_SOUND_URI)
        val volumePercent = safeIntent.getIntExtra(AlarmReceiver.EXTRA_VOLUME_PERCENT, 100)
        val vibrationEnabled = safeIntent.getBooleanExtra(AlarmReceiver.EXTRA_VIBRATION_ENABLED, true)
        val snoozeMinutes = safeIntent.getIntExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, 5)
        val snoozeMaxCount = safeIntent.getIntExtra(AlarmReceiver.EXTRA_SNOOZE_MAX_COUNT, 0)
        val currentSnoozeCount = safeIntent.getIntExtra(AlarmReceiver.EXTRA_SNOOZE_CURRENT_COUNT, 0)
        val directBootSnapshot = DirectBootAlarmIntent.fromIntent(safeIntent)

        if (action == AlarmRingingService.ACTION_STOP) {
            AlarmRingingService.stop(context, alarmId)
            return
        }

        val serviceIntent = Intent(context, AlarmRingingService::class.java)
            .setAction(action)
            .putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            .putExtra(AlarmReceiver.EXTRA_LABEL, label)
            .putExtra(AlarmReceiver.EXTRA_SOUND_TYPE, soundType)
            .putExtra(AlarmReceiver.EXTRA_CUSTOM_SOUND_URI, customSoundUri)
            .putExtra(AlarmReceiver.EXTRA_VOLUME_PERCENT, volumePercent)
            .putExtra(AlarmReceiver.EXTRA_VIBRATION_ENABLED, vibrationEnabled)
            .putExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
            .putExtra(AlarmReceiver.EXTRA_SNOOZE_MAX_COUNT, snoozeMaxCount)
            .putExtra(AlarmReceiver.EXTRA_SNOOZE_CURRENT_COUNT, currentSnoozeCount)
        directBootSnapshot?.let {
            DirectBootAlarmIntent.putSnapshot(serviceIntent, it, null, directBootFallback = true)
            serviceIntent.putExtra(AlarmReceiver.EXTRA_SNOOZE_CURRENT_COUNT, currentSnoozeCount)
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
