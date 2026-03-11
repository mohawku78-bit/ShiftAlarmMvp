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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.shiftalarmmvp.R

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
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    backupFeedbackMessage: String,
    savedPresets: List<RotationPreset>,
    onApplyPreset: (RotationPreset) -> Unit,
    onDeletePreset: (String) -> Unit,
    onSetDefaultPreset: (String) -> Unit,
    onSetPresetInfinite: (String, Boolean) -> Unit,
    onMoveUp: (String) -> Unit,
    onMoveDown: (String) -> Unit
) {
    val resources = LocalContext.current.resources
    var pendingDeletePresetName by remember { mutableStateOf<String?>(null) }

    if (pendingDeletePresetName != null) {
        AlertDialog(
            onDismissRequest = { pendingDeletePresetName = null },
            title = { Text(stringResource(R.string.preset_delete_title)) },
            text = { Text(stringResource(R.string.preset_delete_message, pendingDeletePresetName!!)) },
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
                    Text(stringResource(R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeletePresetName = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Settings, contentDescription = null)
                Text(stringResource(R.string.preset_title), style = MaterialTheme.typography.titleMedium)
            }
            OutlinedTextField(
                value = presetNameInput,
                onValueChange = { onPresetNameInputChange(it.take(24)) },
                label = { Text(stringResource(R.string.preset_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                PrimaryActionButton(onClick = onSaveCurrent, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.preset_save_current))
                }
                NeutralActionButton(onClick = onRefreshList, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.preset_refresh_list))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                SecondaryActionButton(onClick = onExport, modifier = Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = null)
                        Text(stringResource(R.string.preset_export))
                    }
                }
                SecondaryActionButton(onClick = onImport, modifier = Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = null)
                        Text(stringResource(R.string.preset_import))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.preset_import_mode))
                Switch(checked = importMergeMode, onCheckedChange = onImportMergeModeChange)
                Text(
                    if (importMergeMode) {
                        stringResource(R.string.preset_import_mode_merge)
                    } else {
                        stringResource(R.string.preset_import_mode_replace)
                    }
                )
            }
            if (presetFeedbackMessage.isNotBlank()) {
                Text(presetFeedbackMessage, color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Settings, contentDescription = null)
                Text(stringResource(R.string.preset_backup_title), style = MaterialTheme.typography.titleMedium)
            }
            Text(
                stringResource(R.string.preset_backup_description),
                style = MaterialTheme.typography.bodyMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                SecondaryActionButton(onClick = onExportBackup, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.preset_backup_save))
                }
                SecondaryActionButton(onClick = onImportBackup, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.preset_backup_restore))
                }
            }
            Text(
                stringResource(R.string.preset_backup_restore_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (backupFeedbackMessage.isNotBlank()) {
                Text(backupFeedbackMessage, color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    if (savedPresets.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text(stringResource(R.string.preset_empty), modifier = Modifier.padding(14.dp))
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
                        val title = if (preset.isDefault) {
                            stringResource(R.string.preset_default_title_format, preset.name)
                        } else {
                            preset.name
                        }
                        Text(title, style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.preset_badge_format, inferShiftPatternBadge(resources, preset)))
                        Text(stringResource(R.string.preset_cycle_format, preset.intervalWeeks, preset.anchorDate))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.preset_infinite_rotation))
                            Switch(
                                checked = preset.infiniteRotationEnabled,
                                onCheckedChange = { checked -> onSetPresetInfinite(preset.name, checked) }
                            )
                            Text(
                                if (preset.infiniteRotationEnabled) {
                                    stringResource(R.string.common_on)
                                } else {
                                    stringResource(R.string.common_off)
                                }
                            )
                        }
                        preset.weekPatterns.forEachIndexed { idx, days ->
                            val dayText = if (days.isEmpty()) {
                                stringResource(R.string.common_none)
                            } else {
                                days.sortedBy { it.value }.joinToString { dayOfWeekLabel(resources, it) }
                            }
                            Text(stringResource(R.string.preset_week_pattern_format, idx + 1, dayText))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            PrimaryActionButton(onClick = { onApplyPreset(preset) }, modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.preset_apply))
                            }
                            DangerActionButton(onClick = { pendingDeletePresetName = preset.name }, modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.common_delete))
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            SecondaryActionButton(onClick = { onSetDefaultPreset(preset.name) }, modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.preset_set_default))
                            }
                            NeutralActionButton(onClick = { onMoveUp(preset.name) }, enabled = index > 0, modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.preset_move_up))
                            }
                            NeutralActionButton(onClick = { onMoveDown(preset.name) }, enabled = index < savedPresets.lastIndex, modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.preset_move_down))
                            }
                        }
                    }
                }
            }
        }
    }
}