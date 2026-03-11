package com.example.shiftalarmmvp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.shiftalarmmvp.R
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

@Composable
fun ShiftPage(
    customWorkTypeInput: String,
    onCustomWorkTypeInputChange: (String) -> Unit,
    onAddType: () -> Unit,
    onResetDefaults: () -> Unit,
    quickTemplates: List<QuickShiftTemplate>,
    onApplyQuickTemplate: (QuickShiftTemplate) -> Unit,
    workTypeConfigs: List<WorkTypeAlarmConfig>,
    onAppendRotationType: (String) -> Unit,
    rotationSequence: List<String>,
    onDropLastRotation: () -> Unit,
    onClearRotation: () -> Unit,
    todayRotationIndex: Int,
    onTodayRotationIndexChange: (Int) -> Unit,
    onToggleConfigEnabled: (Int, Boolean) -> Unit,
    onDeleteConfigType: (String) -> Unit,
    onConfigPrimaryChange: (Int, String) -> Unit,
    onConfigSecondaryChange: (Int, String) -> Unit,
    infiniteRotationEnabled: Boolean,
    onInfiniteRotationEnabledChange: (Boolean) -> Unit,
    onAutoBuild: () -> Unit,
    wizardState: FirstSetupWizardState,
    onWizardStateChange: (FirstSetupWizardState) -> Unit,
    showFirstSetupWizard: Boolean,
    onCompleteFirstSetup: () -> Unit,
    onHideFirstSetupWizard: () -> Unit,
    onReopenFirstSetupWizard: () -> Unit,
    autoBuildFeedback: String,
    selectedCategory: ShiftCategory,
    onSelectedCategoryChange: (ShiftCategory) -> Unit,
    anchorDate: LocalDate,
    onAnchorDateChange: (LocalDate) -> Unit,
) {
    val normalizedWizardState = wizardState.normalized(workTypeConfigs.size)
    val wizardStep = normalizedWizardState.step
    val showAdvanced = normalizedWizardState.showAdvanced
    val showStep1Advanced = normalizedWizardState.showStep1Advanced
    val selectedStep1TypeIndex = normalizedWizardState.selectedStep1TypeIndex
    val selectedAlarmSlot = normalizedWizardState.selectedAlarmSlot

    fun updateWizardState(transform: (FirstSetupWizardState) -> FirstSetupWizardState) {
        onWizardStateChange(transform(normalizedWizardState).normalized(workTypeConfigs.size))
    }

    BackHandler(enabled = showFirstSetupWizard && wizardStep > 0) {
        updateWizardState { it.back() }
    }

    val representativeTemplates = quickTemplates.filter {
        it.id in setOf(
            QUICK_TEMPLATE_ID_DAY_DUTY_OFF,
            QUICK_TEMPLATE_ID_DAY_NIGHT_OFF,
            QUICK_TEMPLATE_ID_DUTY_OFF,
            QUICK_TEMPLATE_ID_EVERY_OTHER_DAY
        )
    }.ifEmpty { quickTemplates.take(4) }
    val categoryTemplates: List<QuickShiftTemplate> = if (selectedCategory == ShiftCategory.CUSTOM) emptyList() else representativeTemplates
    val selectedTemplate = categoryTemplates.firstOrNull { it.id == normalizedWizardState.selectedTemplateId }
        ?: categoryTemplates.firstOrNull()
    val usesCustomPattern = selectedCategory == ShiftCategory.CUSTOM
    val canAdvanceWizard = canAdvanceFirstSetupWizard(
        step = wizardStep,
        usesCustomPattern = usesCustomPattern,
        hasCustomRotation = rotationSequence.isNotEmpty(),
        hasPresetTemplate = selectedTemplate != null || categoryTemplates.isNotEmpty()
    )
    val shouldApplyQuickTemplate = shouldApplyQuickTemplateOnAdvance(
        step = wizardStep,
        usesCustomPattern = usesCustomPattern
    )

    fun advanceWizard() {
        if (!canAdvanceWizard) return
        if (shouldApplyQuickTemplate) {
            val chosen = selectedTemplate ?: categoryTemplates.firstOrNull() ?: return
            onApplyQuickTemplate(chosen)
        }
        updateWizardState { it.next() }
    }
    val wizardLastStep = 2
    val wizardTotalSteps = wizardLastStep + 1




    if (!showFirstSetupWizard) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PrimaryActionButton(onClick = onReopenFirstSetupWizard, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.shift_reopen_setup))
                }
                NeutralActionButton(
                    onClick = { updateWizardState { it.copy(showAdvanced = !showAdvanced) } },
                    enabled = !showFirstSetupWizard,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        if (showAdvanced) {
                            stringResource(R.string.shift_hide_advanced)
                        } else {
                            stringResource(R.string.shift_show_advanced)
                        }
                    )
                }
            }
        }
    }

    if (showFirstSetupWizard) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Settings, contentDescription = null)
                    Text(stringResource(R.string.shift_wizard_title), style = MaterialTheme.typography.titleMedium)
                }
                Text(stringResource(R.string.shift_wizard_step_format, wizardStep + 1, wizardTotalSteps))

                when (wizardStep) {
                    0 -> {
                        Text(stringResource(R.string.shift_step_pattern_intro))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            CategoryButton(stringResource(R.string.shift_category_representative), selectedCategory != ShiftCategory.CUSTOM) {
                                onSelectedCategoryChange(ShiftCategory.THREE_SHIFT)
                                updateWizardState {
                                    it.copy(selectedTemplateId = representativeTemplates.firstOrNull()?.id)
                                }
                            }
                            CategoryButton(stringResource(R.string.shift_category_custom), selectedCategory == ShiftCategory.CUSTOM) {
                                onSelectedCategoryChange(ShiftCategory.CUSTOM)
                                updateWizardState { it.copy(selectedTemplateId = null) }
                            }
                        }

                        if (selectedCategory != ShiftCategory.CUSTOM) {
                            Text(stringResource(R.string.shift_template_select))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                categoryTemplates.forEach { template ->
                                    val selected = selectedTemplate?.id == template.id
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(stringResource(template.labelResId), style = MaterialTheme.typography.titleSmall)
                                                Text(template.sequence.joinToString(stringResource(R.string.shift_template_sequence_separator)))
                                            }
                                            Button(
                                                onClick = { updateWizardState { it.copy(selectedTemplateId = template.id) } },
                                                colors = segmentedActionButtonColors(selected)
                                            ) {
                                                Text(
                                                    if (selected) {
                                                        stringResource(R.string.shift_template_selected)
                                                    } else {
                                                        stringResource(R.string.shift_template_select_action)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Text(stringResource(R.string.shift_monthly_preview_hint), style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text(stringResource(R.string.shift_custom_pattern_input))
                            val appendableTypes = workTypeConfigs
                                .map { normalizeWorkType(it.type) }
                                .filter { it.isNotBlank() }
                                .distinct()
                                .ifEmpty { listOf(WORK_TYPE_DAY, WORK_TYPE_DUTY, WORK_TYPE_OFF, WORK_TYPE_REST) }

                            appendableTypes.chunked(4).forEach { rowItems ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    rowItems.forEach { type ->
                                        NeutralActionButton(
                                            onClick = { onAppendRotationType(type) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(type)
                                        }
                                    }
                                    repeat(4 - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }

                            Text(stringResource(R.string.shift_input_order))
                            Text(rotationSequenceSummary(rotationSequence, R.string.shift_wizard_sequence_empty))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                NeutralActionButton(onClick = onDropLastRotation, modifier = Modifier.weight(1f)) {
                                    Text(stringResource(R.string.shift_delete_one_step))
                                }
                                DangerActionButton(onClick = onClearRotation, modifier = Modifier.weight(1f)) {
                                    Text(stringResource(R.string.shift_clear_sequence))
                                }
                            }

                            Text(stringResource(R.string.shift_monthly_preview_hint), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    1 -> {
                        Text(stringResource(R.string.shift_step_alarm_intro))
                        Text(
                            stringResource(R.string.shift_step_alarm_hint),
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (workTypeConfigs.isEmpty()) {
                            Text(stringResource(R.string.shift_no_work_types))
                        } else {
                            val selectedConfigIndex = selectedStep1TypeIndex.coerceIn(0, workTypeConfigs.lastIndex)
                            val selectedConfig = workTypeConfigs[selectedConfigIndex]
                            Text(stringResource(R.string.shift_select_work_type))
                            workTypeConfigs.mapIndexed { index, config -> index to config.type }
                                .chunked(4)
                                .forEach { rowItems ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        rowItems.forEach { (index, type) ->
                                            val selected = selectedConfigIndex == index
                                            Button(
                                                onClick = { updateWizardState { it.copy(selectedStep1TypeIndex = index) } },
                                                modifier = Modifier.weight(1f),
                                                colors = segmentedActionButtonColors(selected)
                                            ) {
                                                Text(step1TypeChipLabel(type))
                                            }
                                        }
                                        repeat(4 - rowItems.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }

                            val primaryDisplay = parseHm(selectedConfig.primaryTime)
                                ?: parseHm(defaultPrimaryTime(selectedConfig.type))
                                ?: LocalTime.of(7, 0)
                            val secondaryEnabled = selectedConfig.secondaryTime.isNotBlank()
                            val secondaryDisplay = parseHm(selectedConfig.secondaryTime) ?: primaryDisplay

                            val editingSecondary = showStep1Advanced && selectedAlarmSlot == 1 && secondaryEnabled
                            val activeTime = if (editingSecondary) secondaryDisplay else primaryDisplay

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(selectedConfig.type, style = MaterialTheme.typography.titleMedium)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        Text(stringResource(R.string.shift_alarm_enabled))
                                        Switch(
                                            checked = selectedConfig.enabled,
                                            onCheckedChange = { checked -> onToggleConfigEnabled(selectedConfigIndex, checked) }
                                        )
                                    }

                                    Text(stringResource(R.string.shift_primary_alarm_title))
                                    Text(
                                        String.format("%02d:%02d", primaryDisplay.hour, primaryDisplay.minute),
                                        style = MaterialTheme.typography.headlineMedium
                                    )
                                    TimePickerButton(
                                        time = primaryDisplay,
                                        onTimePicked = { picked ->
                                            onConfigPrimaryChange(selectedConfigIndex, String.format("%02d:%02d", picked.hour, picked.minute))
                                        }
                                    )

                                    NeutralActionButton(
                                        onClick = { updateWizardState { it.copy(showStep1Advanced = !showStep1Advanced) } },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            if (showStep1Advanced) {
                                                stringResource(R.string.shift_hide_secondary_settings)
                                            } else {
                                                stringResource(R.string.shift_show_secondary_settings)
                                            }
                                        )
                                    }

                                    if (showStep1Advanced) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(10.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Button(
                                                        onClick = { updateWizardState { it.copy(selectedAlarmSlot = 0) } },
                                                        modifier = Modifier.weight(1f),
                                                        colors = segmentedActionButtonColors(!editingSecondary)
                                                    ) {
                                                        Text(stringResource(R.string.shift_alarm_slot_primary))
                                                    }
                                                    Button(
                                                        onClick = {
                                                            updateWizardState { it.copy(selectedAlarmSlot = 1) }
                                                            if (!secondaryEnabled) {
                                                                val defaultSecond = selectedConfig.secondaryTime.ifBlank {
                                                                    selectedConfig.primaryTime.ifBlank {
                                                                        String.format(
                                                                            "%02d:%02d",
                                                                            primaryDisplay.hour,
                                                                            primaryDisplay.minute
                                                                        )
                                                                    }
                                                                }
                                                                onConfigSecondaryChange(selectedConfigIndex, defaultSecond)
                                                            }
                                                        },
                                                        modifier = Modifier.weight(1f),
                                                        colors = segmentedActionButtonColors(editingSecondary)
                                                    ) {
                                                        Text(stringResource(R.string.shift_alarm_slot_secondary))
                                                    }
                                                }

                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        stringResource(
                                                            R.string.shift_secondary_alarm_status_format,
                                                            if (secondaryEnabled) {
                                                                stringResource(R.string.common_on)
                                                            } else {
                                                                stringResource(R.string.common_off)
                                                            }
                                                        )
                                                    )
                                                    Switch(
                                                        checked = secondaryEnabled,
                                                        onCheckedChange = { checked ->
                                                            if (checked) {
                                                                val defaultSecond = selectedConfig.secondaryTime.ifBlank {
                                                                    selectedConfig.primaryTime.ifBlank {
                                                                        String.format(
                                                                            "%02d:%02d",
                                                                            primaryDisplay.hour,
                                                                            primaryDisplay.minute
                                                                        )
                                                                    }
                                                                }
                                                                onConfigSecondaryChange(selectedConfigIndex, defaultSecond)
                                                                updateWizardState { it.copy(selectedAlarmSlot = 1) }
                                                            } else {
                                                                onConfigSecondaryChange(selectedConfigIndex, "")
                                                                updateWizardState { it.copy(selectedAlarmSlot = 0) }
                                                            }
                                                        }
                                                    )
                                                }

                                                Text(
                                                    if (editingSecondary) {
                                                        stringResource(R.string.shift_secondary_main_time)
                                                    } else {
                                                        stringResource(R.string.shift_primary_main_time)
                                                    }
                                                )
                                                Text(
                                                    String.format("%02d:%02d", activeTime.hour, activeTime.minute),
                                                    style = MaterialTheme.typography.headlineMedium
                                                )
                                                TimePickerButton(
                                                    time = activeTime,
                                                    onTimePicked = { picked ->
                                                        val formatted = String.format("%02d:%02d", picked.hour, picked.minute)
                                                        if (editingSecondary) {
                                                            onConfigSecondaryChange(selectedConfigIndex, formatted)
                                                        } else {
                                                            onConfigPrimaryChange(selectedConfigIndex, formatted)
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        Text(stringResource(R.string.shift_step_anchor_intro))
                        DatePickerButton(
                            label = stringResource(R.string.shift_anchor_date),
                            date = anchorDate,
                            onDatePicked = onAnchorDateChange
                        )
                        if (rotationSequence.isNotEmpty()) {
                            val sequenceItems = rotationSequence.mapIndexed { index, type -> index to type }
                            sequenceItems.chunked(3).forEach { rowItems ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    rowItems.forEach { (index, type) ->
                                        val selected = todayRotationIndex == index
                                        Button(
                                            onClick = { onTodayRotationIndexChange(index) },
                                            modifier = Modifier.weight(1f),
                                            colors = segmentedActionButtonColors(selected)
                                        ) {
                                            Text(stringResource(R.string.shift_rotation_position_format, index + 1, type))
                                        }
                                    }
                                    repeat(3 - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        Text(stringResource(R.string.shift_completion_message))
                        Text(stringResource(R.string.shift_existing_alarm_hint))
                        if (!showFirstSetupWizard && autoBuildFeedback.isNotBlank()) {
                            Text(autoBuildFeedback, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                if (!showAdvanced) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        if (wizardStep > 0) {
                            NeutralActionButton(onClick = { updateWizardState { it.back() } }, modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.shift_previous))
                            }
                        }

                        if (wizardStep < wizardLastStep) {
                            PrimaryActionButton(
                                onClick = { advanceWizard() },
                                enabled = canAdvanceWizard,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.shift_next))
                            }
                        } else {
                            PrimaryActionButton(onClick = onCompleteFirstSetup, modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.shift_confirm))
                            }
                            NeutralActionButton(onClick = onHideFirstSetupWizard, modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.shift_close))
                            }
                        }
                    }
                }
            }
        }
    }

    if (!showAdvanced) return

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Settings, contentDescription = null)
                Text(stringResource(R.string.shift_advanced_title), style = MaterialTheme.typography.titleMedium)
            }

            OutlinedTextField(
                value = customWorkTypeInput,
                onValueChange = { onCustomWorkTypeInputChange(it.take(12)) },
                label = { Text(stringResource(R.string.shift_work_type_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                SecondaryActionButton(onClick = onAddType, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.shift_add_type))
                }
                NeutralActionButton(onClick = onResetDefaults, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.shift_defaults))
                }
            }

            if (workTypeConfigs.isNotEmpty()) {
                val rotationAppendableConfigs = workTypeConfigs

                Text(stringResource(R.string.shift_registered_work_types))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    rotationAppendableConfigs.forEach { cfg ->
                        NeutralActionButton(onClick = { onAppendRotationType(cfg.type) }) {
                            Text(cfg.type)
                        }
                    }
                }

                Text(stringResource(R.string.shift_manage_types))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    rotationAppendableConfigs.forEach { cfg ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            SecondaryActionButton(onClick = { onAppendRotationType(cfg.type) }, modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.shift_append_type_format, cfg.type))
                            }
                            DangerActionButton(onClick = { onDeleteConfigType(cfg.type) }, modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.shift_delete_type_format, cfg.type))
                            }
                        }
                    }
                }
            }

            Text(stringResource(R.string.shift_rotation_title))
            Text(rotationSequenceSummary(rotationSequence, R.string.shift_rotation_empty))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                NeutralActionButton(onClick = onDropLastRotation, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.shift_delete_last))
                }
                DangerActionButton(onClick = onClearRotation, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.shift_clear_rotation))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.shift_infinite_rotation))
                Switch(checked = infiniteRotationEnabled, onCheckedChange = onInfiniteRotationEnabledChange)
                Text(
                    if (infiniteRotationEnabled) {
                        stringResource(R.string.common_on)
                    } else {
                        stringResource(R.string.common_off)
                    }
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            if (showFirstSetupWizard) {
                val progressLabel = if (wizardStep < wizardLastStep) {
                    stringResource(R.string.shift_progress_to_step_format, wizardStep + 2)
                } else {
                    stringResource(R.string.shift_finish_quick_start)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    if (wizardStep > 0) {
                        NeutralActionButton(
                            onClick = { updateWizardState { it.back() } },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.shift_previous))
                        }
                    }
                    PrimaryActionButton(
                        onClick = {
                            if (wizardStep < wizardLastStep) {
                                advanceWizard()
                            } else {
                                onCompleteFirstSetup()
                            }
                        },
                        enabled = wizardStep >= wizardLastStep || canAdvanceWizard,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(progressLabel)
                    }
                }
            } else {
                PrimaryActionButton(onClick = onAutoBuild, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.shift_auto_build))
                }
            }

            if (!showFirstSetupWizard && autoBuildFeedback.isNotBlank()) {
                Text(autoBuildFeedback, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun RowScope.CategoryButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Button(onClick = onClick, modifier = Modifier.weight(1f), colors = segmentedActionButtonColors(selected)) {
        Text(label)
    }
}

private enum class ShiftBadge {
    DAY,
    NIGHT,
    DUTY,
    OFF,
    REST,
    WORK
}

@Composable
private fun rotationSequenceSummary(rotationSequence: List<String>, emptyTextResId: Int): String {
    if (rotationSequence.isEmpty()) return stringResource(emptyTextResId)
    val separator = stringResource(R.string.shift_rotation_separator)
    return rotationSequence.mapIndexed { index, type ->
        stringResource(R.string.shift_rotation_item_format, index + 1, type)
    }.joinToString(separator)
}

private fun knownShiftBadge(type: String): ShiftBadge? {
    return when (normalizeWorkType(type.trim())) {
        WORK_TYPE_DAY -> ShiftBadge.DAY
        WORK_TYPE_NIGHT -> ShiftBadge.NIGHT
        WORK_TYPE_DUTY -> ShiftBadge.DUTY
        WORK_TYPE_OFF -> ShiftBadge.OFF
        WORK_TYPE_REST, WORK_TYPE_HOLIDAY, WORK_TYPE_VACATION -> ShiftBadge.REST
        else -> null
    }
}

@Composable
private fun step1TypeChipLabel(type: String): String {
    val normalized = type.trim()
    return when (knownShiftBadge(normalized)) {
        ShiftBadge.DAY -> stringResource(R.string.shift_work_type_short_day)
        ShiftBadge.NIGHT -> stringResource(R.string.shift_work_type_short_night)
        ShiftBadge.DUTY -> stringResource(R.string.shift_work_type_short_duty)
        ShiftBadge.OFF -> stringResource(R.string.shift_work_type_short_off)
        ShiftBadge.REST -> stringResource(R.string.shift_work_type_short_rest)
        ShiftBadge.WORK, null -> normalized
    }
}

private fun previewTypeBadge(type: String): ShiftBadge {
    return knownShiftBadge(type) ?: ShiftBadge.WORK
}

@Composable
private fun previewBadgeLabel(badge: ShiftBadge): String {
    return when (badge) {
        ShiftBadge.DAY -> stringResource(R.string.shift_preview_badge_day)
        ShiftBadge.NIGHT -> stringResource(R.string.shift_preview_badge_night)
        ShiftBadge.DUTY -> stringResource(R.string.shift_preview_badge_duty)
        ShiftBadge.OFF -> stringResource(R.string.shift_preview_badge_off)
        ShiftBadge.REST -> stringResource(R.string.shift_preview_badge_rest)
        ShiftBadge.WORK -> stringResource(R.string.shift_preview_badge_work)
    }
}

@Composable
private fun previewDayLabels(): List<String> {
    return listOf(
        stringResource(R.string.shift_day_mon),
        stringResource(R.string.shift_day_tue),
        stringResource(R.string.shift_day_wed),
        stringResource(R.string.shift_day_thu),
        stringResource(R.string.shift_day_fri),
        stringResource(R.string.shift_day_sat),
        stringResource(R.string.shift_day_sun)
    )
}

@Composable
private fun WorkPreviewCalendar(previewDays: List<Pair<LocalDate, String>>) {
    if (previewDays.isEmpty()) {
        Text(stringResource(R.string.shift_preview_empty))
        return
    }

    val previewMap = previewDays.toMap()
    val startDate = previewDays.first().first
    val endDate = previewDays.last().first
    val monthList = mutableListOf<YearMonth>()
    var cursor = YearMonth.from(startDate)
    val lastMonth = YearMonth.from(endDate)
    while (!cursor.isAfter(lastMonth)) {
        monthList += cursor
        cursor = cursor.plusMonths(1)
    }

    monthList.forEach { month ->
        Column(verticalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.shift_preview_month_format, month.year, month.monthValue),
                style = MaterialTheme.typography.titleSmall
            )
            PreviewMonthGrid(month = month, previewMap = previewMap, startDate = startDate, endDate = endDate)
        }
    }
}

@Composable
private fun PreviewMonthGrid(
    month: YearMonth,
    previewMap: Map<LocalDate, String>,
    startDate: LocalDate,
    endDate: LocalDate
) {
    val dayLabels = previewDayLabels()
    val firstDay = month.atDay(1)
    val leading = firstDay.dayOfWeek.value - 1
    val dates = mutableListOf<LocalDate?>()
    repeat(leading) { dates += null }
    for (d in 1..month.lengthOfMonth()) {
        dates += month.atDay(d)
    }
    while (dates.size % 7 != 0) {
        dates += null
    }

    Column(verticalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            dayLabels.forEach { label ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(label, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        dates.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                week.forEach { date ->
                    val type = date?.let { previewMap[it] }
                    val badge = type?.let(::previewTypeBadge)
                    val inRange = date != null && !date.isBefore(startDate) && !date.isAfter(endDate)
                    val bg = when {
                        badge != null -> previewBadgeBackgroundColor(badge)
                        inRange -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
                    }
                    val fg = if (badge != null) previewBadgeColor(badge) else MaterialTheme.colorScheme.onSurfaceVariant
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(bg, shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                            .border(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                            .padding(vertical = 5.dp, horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = date?.dayOfMonth?.toString() ?: "",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                            if (badge != null) {
                                Text(
                                    text = previewBadgeLabel(badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = fg,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun previewBadgeBackgroundColor(badge: ShiftBadge): Color {
    return when (badge) {
        ShiftBadge.DAY -> Color(0xFFD9E8FA)
        ShiftBadge.NIGHT -> Color(0xFFFFE3C8)
        ShiftBadge.DUTY -> Color(0xFFFFE9D6)
        ShiftBadge.OFF -> Color(0xFFE3E8EE)
        ShiftBadge.REST -> Color(0xFFEEF1F4)
        ShiftBadge.WORK -> Color(0xFFE2F0EA)
    }
}

private fun previewBadgeColor(badge: ShiftBadge): Color {
    return when (badge) {
        ShiftBadge.DAY -> Color(0xFF1E4E8C)
        ShiftBadge.NIGHT -> Color(0xFF9A5400)
        ShiftBadge.DUTY -> Color(0xFF8A3E00)
        ShiftBadge.OFF -> Color(0xFF4F6375)
        ShiftBadge.REST -> Color(0xFF5B6670)
        ShiftBadge.WORK -> Color(0xFF4D6B5C)
    }
}