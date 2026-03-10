package com.example.shiftalarmmvp.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.shiftalarmmvp.R
import com.example.shiftalarmmvp.receiver.AlarmReceiver
import com.example.shiftalarmmvp.service.AlarmRingingService

class AlarmAlertActivity : ComponentActivity() {
    private var currentAlarmId: Long = -1L
    private var dismissReceiverRegistered = false

    private val dismissReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != AlarmRingingService.ACTION_DISMISS_ALERT) return

            val dismissAlarmId = intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1L)
            if (dismissAlarmId == -1L || dismissAlarmId == currentAlarmId) {
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        val alarmId = intent?.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1L) ?: -1L
        currentAlarmId = alarmId
        val label = intent?.getStringExtra(AlarmReceiver.EXTRA_LABEL).orEmpty()
        val soundType = intent?.getStringExtra(AlarmReceiver.EXTRA_SOUND_TYPE)
        val customSoundUri = intent?.getStringExtra(AlarmReceiver.EXTRA_CUSTOM_SOUND_URI)
        val volumePercent = intent?.getIntExtra(AlarmReceiver.EXTRA_VOLUME_PERCENT, 100) ?: 100
        val vibrationEnabled = intent?.getBooleanExtra(AlarmReceiver.EXTRA_VIBRATION_ENABLED, true) ?: true
        val snoozeMinutes = intent?.getIntExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, 5) ?: 5
        val snoozeMaxCount = intent?.getIntExtra(AlarmReceiver.EXTRA_SNOOZE_MAX_COUNT, 0) ?: 0
        val currentSnoozeCount = intent?.getIntExtra(AlarmReceiver.EXTRA_SNOOZE_CURRENT_COUNT, 0) ?: 0
        val isSelfTestAlarm = alarmId == 999_999L

        val hasLimit = snoozeMaxCount > 0
        val canSnooze = !isSelfTestAlarm && (!hasLimit || currentSnoozeCount < snoozeMaxCount)
        val canOneMore = !isSelfTestAlarm && hasLimit && currentSnoozeCount == snoozeMaxCount
        val actionEnabled = canSnooze || canOneMore

        if (alarmId > 0) {
            ensureRingingServiceStarted(
                alarmId = alarmId,
                label = label,
                soundType = soundType,
                customSoundUri = customSoundUri,
                volumePercent = volumePercent,
                vibrationEnabled = vibrationEnabled,
                snoozeMinutes = snoozeMinutes,
                snoozeMaxCount = snoozeMaxCount,
                currentSnoozeCount = currentSnoozeCount
            )
        }

        setContent {
            MaterialTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = getString(R.string.notification_screen_title), style = MaterialTheme.typography.headlineMedium)
                    if (label.isNotBlank()) {
                        Text(label, modifier = Modifier.padding(top = 8.dp))
                    }
                    Text("ID: $alarmId", modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))

                    val hintText = when {
                        isSelfTestAlarm -> "2분 테스트 알람이 울렸다면 '끄기'를 눌러 완료하세요."
                        canSnooze && hasLimit -> getString(
                            R.string.notification_snooze_count,
                            snoozeMinutes,
                            currentSnoozeCount + 1,
                            snoozeMaxCount
                        )

                        canSnooze -> getString(R.string.notification_snooze_after, snoozeMinutes)
                        canOneMore -> getString(R.string.notification_snooze_limit)
                        else -> getString(R.string.notification_snooze_finished)
                    }

                    Text(
                        text = hintText,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (!isSelfTestAlarm) {
                        Button(
                            onClick = {
                                when {
                                    canSnooze -> {
                                        AlarmRingingService.snooze(
                                            this@AlarmAlertActivity,
                                            alarmId,
                                            label = label,
                                            snoozeMinutes = snoozeMinutes,
                                            snoozeMaxCount = snoozeMaxCount,
                                            currentSnoozeCount = currentSnoozeCount,
                                            soundType = soundType,
                                            customSoundUri = customSoundUri,
                                            volumePercent = volumePercent,
                                            vibrationEnabled = vibrationEnabled
                                        )
                                    }

                                    canOneMore -> {
                                        AlarmRingingService.oneMore(
                                            this@AlarmAlertActivity,
                                            alarmId,
                                            label = label,
                                            snoozeMinutes = snoozeMinutes,
                                            snoozeMaxCount = snoozeMaxCount,
                                            currentSnoozeCount = currentSnoozeCount,
                                            soundType = soundType,
                                            customSoundUri = customSoundUri,
                                            volumePercent = volumePercent,
                                            vibrationEnabled = vibrationEnabled
                                        )
                                    }
                                }
                                finish()
                            },
                            enabled = actionEnabled,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = when {
                                    canSnooze -> getString(R.string.notification_retry, snoozeMinutes)
                                    canOneMore -> getString(R.string.notification_one_more)
                                    else -> getString(R.string.notification_snooze_finished)
                                }
                            )
                        }
                    }

                    Button(
                        onClick = {
                            AlarmRingingService.stop(this@AlarmAlertActivity, alarmId)
                            finish()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Text(text = getString(R.string.notification_stop))
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        registerDismissReceiverIfNeeded()
    }

    override fun onStop() {
        unregisterDismissReceiverIfNeeded()
        super.onStop()
    }

    private fun registerDismissReceiverIfNeeded() {
        if (dismissReceiverRegistered) return

        val filter = IntentFilter(AlarmRingingService.ACTION_DISMISS_ALERT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(dismissReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(dismissReceiver, filter)
        }
        dismissReceiverRegistered = true
    }

    private fun unregisterDismissReceiverIfNeeded() {
        if (!dismissReceiverRegistered) return

        runCatching { unregisterReceiver(dismissReceiver) }
        dismissReceiverRegistered = false
    }

    private fun ensureRingingServiceStarted(
        alarmId: Long,
        label: String,
        soundType: String?,
        customSoundUri: String?,
        volumePercent: Int,
        vibrationEnabled: Boolean,
        snoozeMinutes: Int,
        snoozeMaxCount: Int,
        currentSnoozeCount: Int
    ) {
        val startIntent = Intent(this, AlarmRingingService::class.java)
            .setAction(AlarmRingingService.ACTION_START)
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
            runCatching { startForegroundService(startIntent) }
        } else {
            runCatching { startService(startIntent) }
        }
    }
}
