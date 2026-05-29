package com.example.shiftalarmmvp.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.shiftalarmmvp.R
import com.example.shiftalarmmvp.receiver.AlarmReceiver
import com.example.shiftalarmmvp.recovery.SELF_TEST_ALARM_ID
import com.example.shiftalarmmvp.service.AlarmRingingService
import java.time.LocalTime
import java.time.format.DateTimeFormatter

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
        val isSelfTestAlarm = alarmId == SELF_TEST_ALARM_ID
        val alarmBadge = shiftTypeToBadge(extractWorkTypeFromLabel(label))
        val alarmTitle = label.ifBlank { getString(R.string.alert_alarm_label_empty) }
        val firedTimeText = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

        val hasLimit = snoozeMaxCount > 0
        val canSnooze = !isSelfTestAlarm && (!hasLimit || currentSnoozeCount < snoozeMaxCount)
        val canOneMore = !isSelfTestAlarm && hasLimit && currentSnoozeCount == snoozeMaxCount
        val actionEnabled = canSnooze || canOneMore
        val hintText = when {
            isSelfTestAlarm -> getString(R.string.alert_self_test_hint)
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
        val snoozeButtonText = when {
            canSnooze -> getString(R.string.notification_retry, snoozeMinutes)
            canOneMore -> getString(R.string.notification_one_more)
            else -> getString(R.string.notification_snooze_finished)
        }

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
            ShiftAlarmTheme {
                AlarmAlertScreen(
                    title = alarmTitle,
                    badge = alarmBadge,
                    alarmId = alarmId,
                    firedTimeText = firedTimeText,
                    hintText = hintText,
                    snoozeButtonText = snoozeButtonText,
                    showSnooze = !isSelfTestAlarm,
                    snoozeEnabled = actionEnabled,
                    onSnooze = {
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
                    onStop = {
                        AlarmRingingService.stop(this@AlarmAlertActivity, alarmId)
                        finish()
                    }
                )
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

@Composable
private fun AlarmAlertScreen(
    title: String,
    badge: String,
    alarmId: Long,
    firedTimeText: String,
    hintText: String,
    snoozeButtonText: String,
    showSnooze: Boolean,
    snoozeEnabled: Boolean,
    onSnooze: () -> Unit,
    onStop: () -> Unit
) {
    val accent = shiftBadgeColor(badge)
    val shape = RoundedCornerShape(32.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(shiftAppBackgroundBrush())
            .padding(horizontal = 22.dp, vertical = 26.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AlarmAlertTopBar()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, accent.copy(alpha = 0.24f), shape),
                colors = CardDefaults.cardColors(containerColor = ShiftDesign.Paper.copy(alpha = 0.96f)),
                shape = shape
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ShiftTypeIllustration(
                        badge = badge,
                        modifier = Modifier.size(118.dp),
                        selected = true
                    )
                    Text(
                        text = firedTimeText,
                        style = MaterialTheme.typography.headlineSmall,
                        color = ShiftDesign.Ink,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = ShiftDesign.Ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = hintText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ShiftDesign.InkSoft,
                        textAlign = TextAlign.Center
                    )
                    HorizontalDivider(color = ShiftDesign.Line.copy(alpha = 0.72f))
                    ShiftPill(
                        text = if (alarmId > 0) {
                            stringResource(R.string.alert_alarm_id_format, alarmId)
                        } else {
                            stringResource(R.string.notification_screen_title)
                        },
                        containerColor = shiftBadgeBackgroundColor(badge),
                        contentColor = accent
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (showSnooze) {
                    Button(
                        onClick = onSnooze,
                        enabled = snoozeEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = secondaryActionButtonColors()
                    ) {
                        Text(
                            text = snoozeButtonText,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
                Button(
                    onClick = onStop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(66.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ShiftDesign.Coral,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = stringResource(R.string.alert_stop_primary),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun AlarmAlertTopBar() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.notification_screen_title),
            style = MaterialTheme.typography.titleSmall,
            color = ShiftDesign.Ink
        )
        ShiftPill(
            text = stringResource(R.string.alert_ringing_now),
            containerColor = ShiftDesign.Mist,
            contentColor = ShiftDesign.Navy
        )
    }
}
