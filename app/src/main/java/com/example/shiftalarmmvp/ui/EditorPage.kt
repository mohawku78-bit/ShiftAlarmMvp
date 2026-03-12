package com.example.shiftalarmmvp.ui

import android.os.Build
import androidx.activity.compose.BackHandler
import android.widget.NumberPicker
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.shiftalarmmvp.R
import com.example.shiftalarmmvp.data.AlarmDateOverrideState
import com.example.shiftalarmmvp.data.AlarmDateOverrides
import com.example.shiftalarmmvp.data.AlarmSoundType
import com.example.shiftalarmmvp.recovery.HomeReliabilityAction
import com.example.shiftalarmmvp.recovery.NightlyReliabilityCheckStatus
import com.example.shiftalarmmvp.recovery.ReliabilityOverviewPolicy
import com.example.shiftalarmmvp.recovery.ReliabilityOverviewSignals
import com.example.shiftalarmmvp.recovery.ReliabilityOverviewTone
import com.example.shiftalarmmvp.recovery.ReliabilitySetupPolicy
import com.example.shiftalarmmvp.recovery.ReliabilitySetupSignals
import com.example.shiftalarmmvp.recovery.RescheduleRecoveryState
import com.example.shiftalarmmvp.recovery.SelfTestStatus
import com.example.shiftalarmmvp.recovery.recoveryStrings
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

private val EDITOR_STEP_LABEL_RES_IDS = listOf(
    R.string.editor_step_basic,
    R.string.editor_step_repeat,
    R.string.editor_step_alert,
    R.string.editor_step_advanced,
)

@Composable
private fun editorReliabilityOverviewToneColor(tone: ReliabilityOverviewTone): Color {
    return when (tone) {
        ReliabilityOverviewTone.NEUTRAL -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
        ReliabilityOverviewTone.SAFE -> Color(0xFF2E7D32)
        ReliabilityOverviewTone.INFO -> Color(0xFF1565C0)
        ReliabilityOverviewTone.CHECK -> Color(0xFFB26A00)
        ReliabilityOverviewTone.ACTION -> Color(0xFFC62828)
    }
}

@Composable
private fun editorReliabilityLevelColor(level: com.example.shiftalarmmvp.recovery.HomeReliabilityLevel): Color {
    return when (level) {
        com.example.shiftalarmmvp.recovery.HomeReliabilityLevel.SAFE -> Color(0xFF2E7D32)
        com.example.shiftalarmmvp.recovery.HomeReliabilityLevel.CHECK -> Color(0xFFB26A00)
        com.example.shiftalarmmvp.recovery.HomeReliabilityLevel.ACTION -> Color(0xFFC62828)
    }
}

@Composable
private fun reliabilityOverviewLabel(key: String): String {
    return when (key) {
        "recovery" -> stringResource(R.string.reliability_label_recovery)
        "latest_recovery_action" -> stringResource(R.string.reliability_label_latest_recovery)
        "self_test" -> stringResource(R.string.reliability_label_self_test)
        "nightly_check" -> stringResource(R.string.reliability_label_nightly_check)
        else -> key
    }
}

@Composable
internal fun EditorPage(
    editingAlarmId: Long?,
    selectedLabel: String,
    onSelectedLabelChange: (String) -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenAppDetailSettings: () -> Unit,
    onRescheduleAllEnabled: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    setupWizardDismissed: Boolean,
    onSetupWizardDismissedChange: (Boolean) -> Unit,
    autoSaveOnDuplicate: Boolean,
    onAutoSaveOnDuplicateChange: (Boolean) -> Unit,
    onPlayTestSound: () -> Unit,
    onStopTestSound: () -> Unit,
    onScheduleSelfTest: () -> Unit,
    onCancelSelfTest: () -> Unit,
    selfTestMessage: String,
    reliabilityCenterUi: com.example.shiftalarmmvp.recovery.ReliabilityCenterUiModel,
    onOpenReliabilityCenter: () -> Unit,
    onRefreshReliabilityStatus: () -> Unit,
    selectedTime: LocalTime,
    onSelectedTimeChange: (LocalTime) -> Unit,
    anchorDate: LocalDate,
    onAnchorDateChange: (LocalDate) -> Unit,
    selectedSoundType: AlarmSoundType,
    onSelectedSoundTypeChange: (AlarmSoundType) -> Unit,
    onPickCustomSound: () -> Unit,
    selectedCustomSoundUri: String?,
    customSoundMessage: String,
    selectedVolume: Float,
    onSelectedVolumeChange: (Float) -> Unit,
    vibrationEnabled: Boolean,
    onVibrationEnabledChange: (Boolean) -> Unit,
    selectedSnoozeMinutes: Int,
    onSelectedSnoozeMinutesChange: (Int) -> Unit,
    customSnoozeInput: String,
    onCustomSnoozeInputChange: (String) -> Unit,
    selectedSnoozeMaxCount: Int,
    customMaxCountInput: String,
    onCustomMaxCountInputChange: (String) -> Unit,
    exceptionDate: LocalDate,
    onExceptionDateChange: (LocalDate) -> Unit,
    skipDates: Set<LocalDate>,
    onSkipDatesChange: (Set<LocalDate>) -> Unit,
    addDates: Set<LocalDate>,
    onAddDatesChange: (Set<LocalDate>) -> Unit,
    showNext10: Boolean,
    onShowNext10Change: (Boolean) -> Unit,
    next10Preview: List<LocalDateTime>,
    intervalWeeks: Int,
    onIntervalWeeksChange: (Int) -> Unit,
    activeWeekIndex: Int,
    onActiveWeekIndexChange: (Int) -> Unit,
    weekPatterns: List<Set<DayOfWeek>>,
    onWeekPatternsChange: (List<Set<DayOfWeek>>) -> Unit,
    infiniteRotationEnabled: Boolean,
    onInfiniteRotationEnabledChange: (Boolean) -> Unit,
    canSaveByPermission: Boolean,
    onSaveOrUpdate: () -> Unit,
    onCancelEdit: () -> Unit,
    initialStep: Int? = null
) {
    var step by rememberSaveable(editingAlarmId) { mutableIntStateOf(0) }
    var currentNow by remember { mutableStateOf(LocalDateTime.now()) }
    var intervalInput by rememberSaveable(editingAlarmId) { mutableStateOf(intervalWeeks.toString()) }
    val editorSteps = EDITOR_STEP_LABEL_RES_IDS.map { stringResource(it) }
    val lastStep = editorSteps.lastIndex

    BackHandler(enabled = step > 0) {
        step -= 1
    }

    fun updateIntervalWeeks(target: Int) {
        val normalized = target.coerceAtLeast(1)
        onIntervalWeeksChange(normalized)
        if (activeWeekIndex >= normalized) onActiveWeekIndexChange(normalized - 1)
    }

    LaunchedEffect(initialStep, lastStep) {
        initialStep?.let { forced ->
            step = forced.coerceIn(0, lastStep)
        }
    }

    LaunchedEffect(intervalWeeks) {
        intervalInput = intervalWeeks.toString()
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentNow = LocalDateTime.now()
            delay(1_000)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.editor_step_title), style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    editorSteps.forEachIndexed { idx, label ->
                        val selected = step == idx
                        Button(onClick = { step = idx }, colors = segmentedActionButtonColors(selected)) {
                            Text(stringResource(R.string.editor_step_chip_format, idx + 1, label))
                        }
                    }
                }
                Text(stringResource(R.string.editor_current_step_format, step + 1, editorSteps.size, editorSteps[step]))
            }
        }

        when (step) {
                        0 -> {
                OutlinedTextField(
                    value = selectedLabel,
                    onValueChange = { onSelectedLabelChange(it.take(24)) },
                    label = { Text(stringResource(R.string.editor_alarm_name_optional)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.editor_current_time_reference), style = MaterialTheme.typography.titleSmall)
                        Text(currentNow.format(DateTimeFormatter.ofPattern("HH:mm:ss")), style = MaterialTheme.typography.headlineSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(onClick = { onSelectedTimeChange(currentNow.toLocalTime().withSecond(0).withNano(0)) }, modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.editor_apply_current_time))
                            }
                            Button(onClick = { onSelectedTimeChange(currentNow.toLocalTime().plusMinutes(10).withSecond(0).withNano(0)) }, modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.editor_add_ten_minutes))
                            }
                            Button(onClick = { onSelectedTimeChange(currentNow.toLocalTime().plusMinutes(30).withSecond(0).withNano(0)) }, modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.editor_add_thirty_minutes))
                            }
                        }
                    }
                }

                GalaxyTimeInput(
                    selectedTime = selectedTime,
                    onSelectedTimeChange = onSelectedTimeChange
                )
            }

            1 -> {
                DatePickerButton(
                    label = stringResource(R.string.editor_rotation_anchor_date),
                    date = anchorDate,
                    onDatePicked = onAnchorDateChange
                )

                Text(stringResource(R.string.editor_rotation_cycle))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                ) {
                    listOf(1, 2, 3, 4, 6, 8, 12).forEach { value ->
                        Button(
                            onClick = { updateIntervalWeeks(value) },
                            colors = segmentedActionButtonColors(intervalWeeks == value)
                        ) {
                            Text(stringResource(R.string.editor_rotation_cycle_value_format, value))
                        }
                    }
                }
                OutlinedTextField(
                    value = intervalInput,
                    onValueChange = { value ->
                        val digits = value.filter { it.isDigit() }.take(3)
                        intervalInput = digits
                        digits.toIntOrNull()?.takeIf { it > 0 }?.let { updateIntervalWeeks(it) }
                    },
                    label = { Text(stringResource(R.string.editor_direct_week_input)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(stringResource(R.string.editor_current_cycle_format, intervalWeeks))

                Text(stringResource(R.string.editor_select_week_to_edit))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                ) {
                    repeat(intervalWeeks) { index ->
                        Button(onClick = { onActiveWeekIndexChange(index) }, colors = segmentedActionButtonColors(activeWeekIndex == index)) {
                            val mark = if (activeWeekIndex == index) stringResource(R.string.editor_selected_mark) else ""
                            Text(stringResource(R.string.editor_week_chip_format, index + 1, mark))
                        }
                    }
                }

                Text(stringResource(R.string.editor_weekdays_select_format, activeWeekIndex + 1))
                WeekdaySelector(selectedDays = weekPatterns[activeWeekIndex], onToggle = { day ->
                    val copy = weekPatterns.toMutableList()
                    val current = copy[activeWeekIndex]
                    copy[activeWeekIndex] = if (day in current) current - day else current + day
                    onWeekPatternsChange(copy)
                })

                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(stringResource(R.string.editor_infinite_rotation))
                        Switch(
                            checked = infiniteRotationEnabled,
                            onCheckedChange = onInfiniteRotationEnabledChange
                        )
                        Text(if (infiniteRotationEnabled) stringResource(R.string.common_on) else stringResource(R.string.common_off))
                    }
                }
            }

            2 -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = onPlayTestSound, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.editor_test_sound_play))
                    }
                    Button(onClick = onStopTestSound, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.editor_test_sound_stop))
                    }
                }

                Text(stringResource(R.string.editor_sound_selection))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row {
                        RadioButton(
                            selected = selectedSoundType == AlarmSoundType.ALARM,
                            onClick = { onSelectedSoundTypeChange(AlarmSoundType.ALARM) }
                        )
                        Text(stringResource(R.string.editor_sound_alarm))
                    }
                    Row {
                        RadioButton(
                            selected = selectedSoundType == AlarmSoundType.NOTIFICATION,
                            onClick = { onSelectedSoundTypeChange(AlarmSoundType.NOTIFICATION) }
                        )
                        Text(stringResource(R.string.editor_sound_notification))
                    }
                    Row {
                        RadioButton(
                            selected = selectedSoundType == AlarmSoundType.CUSTOM,
                            onClick = { onSelectedSoundTypeChange(AlarmSoundType.CUSTOM) }
                        )
                        Text(stringResource(R.string.editor_sound_custom))
                    }
                }

                Button(onClick = onPickCustomSound) {
                    Text(stringResource(R.string.editor_pick_custom_sound))
                }
                Text(
                    stringResource(
                        R.string.editor_selected_custom_sound_format,
                        selectedCustomSoundUri ?: stringResource(R.string.common_none)
                    )
                )
                if (customSoundMessage.isNotBlank()) Text(customSoundMessage)

                Text(stringResource(R.string.editor_volume_format, selectedVolume.toInt()))
                Slider(
                    value = selectedVolume,
                    onValueChange = onSelectedVolumeChange,
                    valueRange = 0f..100f
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.editor_vibration))
                    Switch(checked = vibrationEnabled, onCheckedChange = onVibrationEnabledChange)
                }

                Text(stringResource(R.string.editor_snooze_settings))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                ) {
                    listOf(1, 3, 5, 10, 15, 20, 30).forEach { value ->
                        Button(onClick = { onSelectedSnoozeMinutesChange(value) }, colors = segmentedActionButtonColors(selectedSnoozeMinutes == value)) {
                            Text(
                                if (selectedSnoozeMinutes == value) {
                                    stringResource(R.string.editor_snooze_option_selected, value)
                                } else {
                                    stringResource(R.string.editor_snooze_option_unselected, value)
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = customSnoozeInput,
                    onValueChange = onCustomSnoozeInputChange,
                    label = { Text(stringResource(R.string.editor_direct_minutes_input)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(stringResource(R.string.editor_current_snooze_format, selectedSnoozeMinutes))

                Text(stringResource(R.string.editor_snooze_count_title))
                OutlinedTextField(
                    value = customMaxCountInput,
                    onValueChange = onCustomMaxCountInputChange,
                    label = { Text(stringResource(R.string.editor_max_snooze_count)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    when {
                        selectedSnoozeMaxCount <= 0 -> stringResource(R.string.editor_snooze_limit_unlimited)
                        else -> stringResource(R.string.editor_snooze_limit_format, selectedSnoozeMaxCount)
                    }
                )
            }

            else -> {
                val allSetupReady = reliabilityCenterUi.setup.allReady
                val next3Preview = next10Preview.take(3)

                fun runReliabilityAction(action: HomeReliabilityAction) {
                    when (action) {
                        HomeReliabilityAction.OPEN_EXACT_ALARM_SETTINGS -> onOpenExactAlarmSettings()
                        HomeReliabilityAction.REQUEST_NOTIFICATION_PERMISSION -> onRequestNotificationPermission()
                        HomeReliabilityAction.OPEN_BATTERY_SETTINGS -> onOpenBatterySettings()
                        HomeReliabilityAction.RESCHEDULE_ALARMS -> onRescheduleAllEnabled()
                        HomeReliabilityAction.OPEN_RELIABILITY_CENTER -> onOpenReliabilityCenter()
                        HomeReliabilityAction.RUN_SELF_TEST -> onScheduleSelfTest()
                        HomeReliabilityAction.REFRESH_STATUS -> onRefreshReliabilityStatus()
                    }
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.editor_reliability_check_title), style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = reliabilityCenterUi.summary.statusLabel,
                            style = MaterialTheme.typography.titleSmall,
                            color = editorReliabilityLevelColor(reliabilityCenterUi.summary.level)
                        )
                        Text(reliabilityCenterUi.summary.reasonText, style = MaterialTheme.typography.bodySmall)
                        Button(
                            onClick = { runReliabilityAction(reliabilityCenterUi.summary.primaryAction) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(reliabilityCenterUi.summary.primaryActionLabel)
                        }
                        reliabilityCenterUi.batteryGuideHint?.let { hint ->
                            Text(
                                text = hint,
                                style = MaterialTheme.typography.bodySmall,
                                color = editorReliabilityOverviewToneColor(ReliabilityOverviewTone.CHECK)
                            )
                            Button(
                                onClick = { runReliabilityAction(HomeReliabilityAction.OPEN_BATTERY_SETTINGS) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.main_battery_guide_cta))
                            }
                        }
                    }
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.editor_reliability_check_title), style = MaterialTheme.typography.titleSmall)
                        Text(reliabilityCenterUi.setup.summaryText, style = MaterialTheme.typography.bodySmall)
                        reliabilityCenterUi.setup.primaryStep?.let { step ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(stringResource(R.string.editor_priority_action_progress, step.stepNumber, step.totalStepCount), style = MaterialTheme.typography.labelLarge)
                                    Text(step.title, style = MaterialTheme.typography.titleSmall)
                                    Text(step.detailText, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        reliabilityCenterUi.setup.steps.forEach { step ->
                            Text(stringResource(R.string.editor_setup_step_status_format, step.stepNumber, step.totalStepCount, step.statusText))
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            reliabilityCenterUi.setup.unresolvedSteps.forEach { step ->
                                val title = if (step == reliabilityCenterUi.setup.primaryStep) {
                                    stringResource(R.string.editor_priority_action_prefix, step.actionLabel)
                                } else {
                                    step.actionLabel
                                }
                                Button(onClick = { runReliabilityAction(step.action) }, modifier = Modifier.fillMaxWidth()) {
                                    Text(title)
                                }
                            }
                            Button(onClick = onRescheduleAllEnabled, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.editor_reschedule_alarms)) }
                            Button(onClick = onOpenAppDetailSettings, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.editor_app_info)) }
                        }

                        if (!setupWizardDismissed || !allSetupReady) {
                            Button(
                                onClick = { onSetupWizardDismissedChange(true) },
                                enabled = allSetupReady
                            ) {
                                Text(stringResource(R.string.editor_hide_setup_card))
                            }
                        }
                    }
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.editor_recent_reliability_records), style = MaterialTheme.typography.titleSmall)
                        reliabilityCenterUi.recent.lines.forEach { line ->
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth()) {
                                Text(reliabilityOverviewLabel(line.key), style = MaterialTheme.typography.labelLarge)
                                Text(
                                    text = line.text,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = editorReliabilityOverviewToneColor(line.tone)
                                )
                            }
                        }
                    }
                }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.editor_self_test_title), style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(onClick = onScheduleSelfTest, modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.editor_self_test_run))
                            }
                            Button(onClick = onCancelSelfTest, modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.editor_self_test_cancel))
                            }
                        }
                        if (selfTestMessage.isNotBlank()) {
                            Text(selfTestMessage, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.editor_next_reservations_title), style = MaterialTheme.typography.titleSmall)
                        if (next3Preview.isEmpty()) {
                            Text(stringResource(R.string.editor_next_reservations_empty))
                        } else {
                            next3Preview.forEachIndexed { idx, dt ->
                                Text(stringResource(R.string.editor_numbered_item_format, idx + 1, dt.format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))))
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.editor_auto_save_on_duplicate))
                    Switch(checked = autoSaveOnDuplicate, onCheckedChange = onAutoSaveOnDuplicateChange)
                }

                Text(stringResource(R.string.editor_exception_settings))
                DatePickerButton(
                    label = stringResource(R.string.editor_exception_date_select),
                    date = exceptionDate,
                    onDatePicked = onExceptionDateChange
                )
                val overrides = AlarmDateOverrides.of(skipDates = skipDates, addDates = addDates)
                val selectedOverrideState = overrides.stateFor(exceptionDate)
                val isSkipSelected = selectedOverrideState == AlarmDateOverrideState.SKIP
                val isAddSelected = selectedOverrideState == AlarmDateOverrideState.ADD

                fun updateOverrides(updated: AlarmDateOverrides) {
                    onSkipDatesChange(updated.skipDates)
                    onAddDatesChange(updated.addDates)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            updateOverrides(
                                overrides.withState(
                                    exceptionDate,
                                    if (isSkipSelected) AlarmDateOverrideState.NONE else AlarmDateOverrideState.SKIP
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isSkipSelected) stringResource(R.string.editor_skip_date_cancel) else stringResource(R.string.editor_skip_date_add))
                    }
                    Button(
                        onClick = {
                            updateOverrides(
                                overrides.withState(
                                    exceptionDate,
                                    if (isAddSelected) AlarmDateOverrideState.NONE else AlarmDateOverrideState.ADD
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isAddSelected) stringResource(R.string.editor_add_alarm_date_cancel) else stringResource(R.string.editor_add_alarm_date_add))
                    }
                }

                if (skipDates.isNotEmpty()) {
                    Text(stringResource(R.string.editor_skip_dates_format, skipDates.sorted().joinToString { it.toString() }))
                }
                if (addDates.isNotEmpty()) {
                    Text(stringResource(R.string.editor_add_dates_format, addDates.sorted().joinToString { it.toString() }))
                }

                Button(onClick = { onShowNext10Change(!showNext10) }) {
                    Text(
                        if (showNext10) {
                            stringResource(R.string.editor_next_ten_hide)
                        } else {
                            stringResource(R.string.editor_next_ten_show)
                        }
                    )
                }
                if (showNext10) {
                    if (next10Preview.isEmpty()) {
                        Text(stringResource(R.string.common_none))
                    } else {
                        next10Preview.forEachIndexed { idx, dt ->
                            Text(stringResource(R.string.editor_numbered_item_format, idx + 1, dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))))
                        }
                    }
                }


            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            if (step > 0) {
                NeutralActionButton(onClick = { step -= 1 }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.editor_back))
                }
            }

            if (step < lastStep) {
                PrimaryActionButton(onClick = { step += 1 }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.editor_next))
                }
            } else {
                PrimaryActionButton(
                    onClick = onSaveOrUpdate,
                    enabled = canSaveByPermission,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (editingAlarmId == null) stringResource(R.string.editor_save_alarm) else stringResource(R.string.editor_update_alarm))
                }
            }

            if (step == lastStep && editingAlarmId != null) {
                NeutralActionButton(onClick = onCancelEdit, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.editor_cancel_edit))
                }
            }
        }
    }
}




@Composable
private fun GalaxyTimeInput(
    selectedTime: LocalTime,
    onSelectedTimeChange: (LocalTime) -> Unit
) {
    var textInput by remember(selectedTime) {
        mutableStateOf(selectedTime.format(DateTimeFormatter.ofPattern("HH:mm")))
    }
    var wheelHour by remember { mutableIntStateOf(selectedTime.hour) }
    var wheelMinute by remember { mutableIntStateOf(selectedTime.minute) }
    val latestOnSelectedTimeChange by rememberUpdatedState(onSelectedTimeChange)

    LaunchedEffect(selectedTime) {
        wheelHour = selectedTime.hour
        wheelMinute = selectedTime.minute
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.editor_time_input_title), style = MaterialTheme.typography.titleSmall)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            AndroidView(
                modifier = Modifier.weight(1f),
                factory = { context ->
                    NumberPicker(context).apply {
                        minValue = 0
                        maxValue = 23
                        wrapSelectorWheel = true
                        setOnValueChangedListener { _, _, newVal ->
                            if (newVal != wheelHour) {
                                wheelHour = newVal
                                latestOnSelectedTimeChange(LocalTime.of(wheelHour, wheelMinute))
                            }
                        }
                    }
                },
                update = { picker ->
                    if (picker.value != wheelHour) picker.value = wheelHour
                }
            )
            AndroidView(
                modifier = Modifier.weight(1f),
                factory = { context ->
                    NumberPicker(context).apply {
                        minValue = 0
                        maxValue = 59
                        wrapSelectorWheel = true
                        setFormatter { String.format("%02d", it) }
                        setOnValueChangedListener { _, _, newVal ->
                            if (newVal != wheelMinute) {
                                wheelMinute = newVal
                                latestOnSelectedTimeChange(LocalTime.of(wheelHour, wheelMinute))
                            }
                        }
                    }
                },
                update = { picker ->
                    if (picker.value != wheelMinute) picker.value = wheelMinute
                }
            )
        }

        OutlinedTextField(
            value = textInput,
            onValueChange = { value ->
                textInput = value.take(5)
                parseHm(value)?.let { parsed -> latestOnSelectedTimeChange(parsed) }
            },
            label = { Text(stringResource(R.string.editor_time_direct_input)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Text(stringResource(R.string.editor_selected_time_format, selectedTime.format(DateTimeFormatter.ofPattern("HH:mm"))))
    }
}







