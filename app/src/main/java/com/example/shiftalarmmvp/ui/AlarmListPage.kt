package com.example.shiftalarmmvp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.shiftalarmmvp.R
import com.example.shiftalarmmvp.data.AlarmRule
import java.time.format.DateTimeFormatter

@Composable
fun AlarmListPage(
    alarms: List<AlarmRule>,
    editingAlarmId: Long?,
    onToggle: (AlarmRule) -> Unit,
    onDelete: (AlarmRule) -> Unit,
    onToggleTodaySkip: (AlarmRule) -> Unit,
    onToggleTomorrowAdd: (AlarmRule) -> Unit,
    onDuplicate: (AlarmRule) -> Unit,
    onEdit: (AlarmRule) -> Unit,
    onReconfigurePattern: () -> Unit,
    alarmLogs: List<AlarmLogEntry>,
    onRefreshLogs: () -> Unit,
    onClearLogs: () -> Unit
) {
    val resources = LocalContext.current.resources
    var pendingDeleteAlarm by remember { mutableStateOf<AlarmRule?>(null) }

    if (pendingDeleteAlarm != null) {
        val target = pendingDeleteAlarm!!
        val targetLabel = if (target.label.isBlank()) {
            "${target.hour}:${target.minute.toString().padStart(2, '0')}"
        } else {
            target.label
        }

        AlertDialog(
            onDismissRequest = { pendingDeleteAlarm = null },
            title = { Text(stringResource(R.string.alarm_list_delete_title)) },
            text = { Text(stringResource(R.string.alarm_list_delete_message, targetLabel)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(target)
                        pendingDeleteAlarm = null
                    }
                ) {
                    Text(stringResource(R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteAlarm = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ShiftPanel(
            modifier = Modifier.fillMaxWidth(),
            containerColor = ShiftDesign.Paper,
            borderColor = ShiftDesign.Line
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Settings, contentDescription = null, tint = ShiftDesign.Navy)
                    Text(
                        stringResource(R.string.alarm_list_registered_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = ShiftDesign.Ink
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    PrimaryActionButton(onClick = onReconfigurePattern, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.alarm_list_restart_from_template))
                    }
                }
                if (alarms.isEmpty()) {
                    Text(
                        stringResource(R.string.alarm_list_empty),
                        color = ShiftDesign.InkSoft
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        alarms.forEach { alarm ->
                            AlarmItem(
                                alarm = alarm,
                                isEditing = editingAlarmId == alarm.id,
                                onToggle = { onToggle(alarm) },
                                onDelete = { pendingDeleteAlarm = alarm },
                                onToggleTodaySkip = { onToggleTodaySkip(alarm) },
                                onToggleTomorrowAdd = { onToggleTomorrowAdd(alarm) },
                                onDuplicate = { onDuplicate(alarm) },
                                onEdit = { onEdit(alarm) }
                            )
                        }
                    }
                }
            }
        }

        ShiftPanel(
            modifier = Modifier.fillMaxWidth(),
            containerColor = ShiftDesign.Mist.copy(alpha = 0.62f),
            borderColor = ShiftDesign.Line
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = null, tint = ShiftDesign.Harbor)
                    Text(
                        stringResource(R.string.alarm_list_logs_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = ShiftDesign.Ink
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    SecondaryActionButton(onClick = onRefreshLogs, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.alarm_list_logs_refresh))
                    }
                    DangerActionButton(onClick = onClearLogs, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.alarm_list_logs_clear))
                    }
                }
                if (alarmLogs.isEmpty()) {
                    Text(
                        stringResource(R.string.alarm_list_logs_empty),
                        color = ShiftDesign.InkSoft
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        alarmLogs.forEach { entry ->
                            val ts = entry.toLocalDateTime().format(DateTimeFormatter.ofPattern("MM-dd HH:mm:ss"))
                            val labelText = if (entry.label.isBlank()) {
                                stringResource(R.string.alarm_list_name_empty_wrapped)
                            } else {
                                entry.label
                            }
                            val detailText = if (entry.detail.isBlank()) {
                                ""
                            } else {
                                stringResource(R.string.alarm_list_log_detail_format, entry.detail)
                            }
                            Text(
                                stringResource(
                                    R.string.alarm_list_log_entry_format,
                                    ts,
                                    formatAlarmLogType(resources, entry.type),
                                    labelText,
                                    entry.alarmId,
                                    detailText
                                ),
                                color = ShiftDesign.InkSoft,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
