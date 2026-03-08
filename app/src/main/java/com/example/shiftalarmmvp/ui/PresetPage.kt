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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PresetPage(
    presetNameInput: String,
    onPresetNameInputChange: (String) -> Unit,
    onSaveCurrent: () -> Unit,
    onRefreshList: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    importMergeMode: Boolean,
    onImportMergeModeChange: (Boolean) -> Unit,
    presetFeedbackMessage: String,
    savedPresets: List<RotationPreset>,
    onApplyPreset: (RotationPreset) -> Unit,
    onDeletePreset: (String) -> Unit,
    onSetDefaultPreset: (String) -> Unit,
    onSetPresetInfinite: (String, Boolean) -> Unit,
    onMoveUp: (String) -> Unit,
    onMoveDown: (String) -> Unit
) {
    var pendingDeletePresetName by remember { mutableStateOf<String?>(null) }

    if (pendingDeletePresetName != null) {
        AlertDialog(
            onDismissRequest = { pendingDeletePresetName = null },
            title = { Text("프리셋 삭제") },
            text = { Text("'${pendingDeletePresetName}' 프리셋을 삭제할까요?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = pendingDeletePresetName
                        if (target != null) {
                            onDeletePreset(target)
                        }
                        pendingDeletePresetName = null
                    }
                ) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeletePresetName = null }) {
                    Text("취소")
                }
            }
        )
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Settings, contentDescription = null)
                Text("근무패턴 프리셋", style = MaterialTheme.typography.titleMedium)
            }
            OutlinedTextField(
                value = presetNameInput,
                onValueChange = { onPresetNameInputChange(it.take(24)) },
                label = { Text("프리셋 이름") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                PrimaryActionButton(onClick = onSaveCurrent, modifier = Modifier.weight(1f)) { Text("현재 설정 저장") }
                NeutralActionButton(onClick = onRefreshList, modifier = Modifier.weight(1f)) { Text("목록 새로고침") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                SecondaryActionButton(onClick = onExport, modifier = Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = null)
                        Text("내보내기")
                    }
                }
                SecondaryActionButton(onClick = onImport, modifier = Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = null)
                        Text("가져오기")
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Text("가져오기 모드")
                Switch(checked = importMergeMode, onCheckedChange = onImportMergeModeChange)
                Text(if (importMergeMode) "병합" else "교체")
            }
            if (presetFeedbackMessage.isNotBlank()) {
                Text(presetFeedbackMessage, color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    if (savedPresets.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text("저장된 프리셋이 없습니다. 먼저 현재 설정을 저장해보세요.", modifier = Modifier.padding(14.dp))
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            savedPresets.forEachIndexed { index, preset ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (preset.isDefault) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val title = if (preset.isDefault) "${preset.name} (기본)" else preset.name
                        Text(title, style = MaterialTheme.typography.titleMedium)
                        Text("대표형: ${inferShiftPatternBadge(preset)}")
                        Text("주기: ${preset.intervalWeeks}주 / 기준일: ${preset.anchorDate}")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Text("무한 반복")
                            Switch(
                                checked = preset.infiniteRotationEnabled,
                                onCheckedChange = { checked -> onSetPresetInfinite(preset.name, checked) }
                            )
                            Text(if (preset.infiniteRotationEnabled) "ON" else "OFF")
                        }
                        preset.weekPatterns.forEachIndexed { idx, days ->
                            val dayText = if (days.isEmpty()) "없음" else days
                                .sortedBy { it.value }
                                .joinToString { dayOfWeekLabel(it) }
                            Text("${idx + 1}주차: $dayText")
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            PrimaryActionButton(onClick = { onApplyPreset(preset) }, modifier = Modifier.weight(1f)) { Text("적용") }
                            DangerActionButton(onClick = { pendingDeletePresetName = preset.name }, modifier = Modifier.weight(1f)) { Text("삭제") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            SecondaryActionButton(onClick = { onSetDefaultPreset(preset.name) }, modifier = Modifier.weight(1f)) { Text("기본") }
                            NeutralActionButton(onClick = { onMoveUp(preset.name) }, enabled = index > 0, modifier = Modifier.weight(1f)) {
                                Text("위")
                            }
                            NeutralActionButton(onClick = { onMoveDown(preset.name) }, enabled = index < savedPresets.lastIndex, modifier = Modifier.weight(1f)) {
                                Text("아래")
                            }
                        }
                    }
                }
            }
        }
    }
}



