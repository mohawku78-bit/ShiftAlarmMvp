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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
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
            title = { Text("알람 삭제") },
            text = { Text("'${targetLabel}' 알람을 삭제할까요?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(target)
                        pendingDeleteAlarm = null
                    }
                ) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteAlarm = null }) {
                    Text("취소")
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Settings, contentDescription = null)
                    Text("등록된 알람", style = MaterialTheme.typography.titleMedium)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    PrimaryActionButton(onClick = onReconfigurePattern, modifier = Modifier.weight(1f)) {
                        Text("템플릿에서 다시 시작")
                    }
                }
                if (alarms.isEmpty()) {
                    Text("아직 등록된 알람이 없습니다. 패턴 탭에서 자동 생성을 눌러보세요.")
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

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = null)
                    Text("알람 로그(최근 50건)", style = MaterialTheme.typography.titleMedium)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    SecondaryActionButton(onClick = onRefreshLogs, modifier = Modifier.weight(1f)) {
                        Text("로그 새로고침")
                    }
                    DangerActionButton(onClick = onClearLogs, modifier = Modifier.weight(1f)) {
                        Text("로그 초기화")
                    }
                }
                if (alarmLogs.isEmpty()) {
                    Text("기록 없음")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        alarmLogs.forEach { entry ->
                            val ts = entry.toLocalDateTime().format(DateTimeFormatter.ofPattern("MM-dd HH:mm:ss"))
                            val labelText = if (entry.label.isBlank()) "(이름 없음)" else entry.label
                            val detailText = if (entry.detail.isBlank()) "" else " / ${entry.detail}"
                            Text("$ts [${formatAlarmLogType(entry.type)}] $labelText(#${entry.alarmId})$detailText")
                        }
                    }
                }
            }
        }
    }
}






