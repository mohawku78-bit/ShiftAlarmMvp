package com.example.shiftalarmmvp.ui

import android.app.DatePickerDialog
import android.widget.NumberPicker
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.shiftalarmmvp.R
import com.example.shiftalarmmvp.data.AlarmRule
import com.example.shiftalarmmvp.data.AlarmSoundType
import com.example.shiftalarmmvp.scheduler.AlarmTimeCalculator
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
@Composable
fun TimePickerButton(
    time: LocalTime,
    onTimePicked: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var inputMode by remember { mutableStateOf(false) }
    var wheelHour by remember { mutableIntStateOf(time.hour) }
    var wheelMinute by remember { mutableIntStateOf(time.minute) }
    val latestOnTimePicked by rememberUpdatedState(onTimePicked)
    val resolvedLabel = label ?: stringResource(R.string.dialog_time_select)
    var hourInput by remember(time) { mutableStateOf(String.format("%02d", time.hour)) }
    var minuteInput by remember(time) { mutableStateOf(String.format("%02d", time.minute)) }

    LaunchedEffect(time) {
        wheelHour = time.hour
        wheelMinute = time.minute
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SecondaryActionButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.page_components_time_picker_button_format, resolvedLabel, time.format(DateTimeFormatter.ofPattern("HH:mm"))))
        }

        if (expanded) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { inputMode = false },
                            modifier = Modifier.weight(1f),
                            colors = segmentedActionButtonColors(!inputMode)
                        ) {
                            Text(stringResource(R.string.dialog_time_wheel_mode))
                        }
                        Button(
                            onClick = { inputMode = true },
                            modifier = Modifier.weight(1f),
                            colors = segmentedActionButtonColors(inputMode)
                        ) {
                            Text(stringResource(R.string.dialog_time_number_mode))
                        }
                    }

                    if (inputMode) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = hourInput,
                                onValueChange = { value ->
                                    val next = value.filter { it.isDigit() }.take(2)
                                    hourInput = next
                                    val hour = next.toIntOrNull()
                                    val minute = minuteInput.toIntOrNull()
                                    if (hour != null && hour in 0..23 && minute != null && minute in 0..59) {
                                        latestOnTimePicked(LocalTime.of(hour, minute))
                                    }
                                },
                                label = { Text(stringResource(R.string.dialog_hour)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = minuteInput,
                                onValueChange = { value ->
                                    val next = value.filter { it.isDigit() }.take(2)
                                    minuteInput = next
                                    val hour = hourInput.toIntOrNull()
                                    val minute = next.toIntOrNull()
                                    if (hour != null && hour in 0..23 && minute != null && minute in 0..59) {
                                        latestOnTimePicked(LocalTime.of(hour, minute))
                                    }
                                },
                                label = { Text(stringResource(R.string.dialog_minute)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AndroidView(
                                modifier = Modifier.weight(1f),
                                factory = { context ->
                                    NumberPicker(context).apply {
                                        minValue = 0
                                        maxValue = 23
                                        wrapSelectorWheel = true
                                        setFormatter { String.format("%02d", it) }
                                        setOnValueChangedListener { _, _, newVal ->
                                            if (newVal != wheelHour) {
                                                wheelHour = newVal
                                                latestOnTimePicked(LocalTime.of(wheelHour, wheelMinute))
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
                                                latestOnTimePicked(LocalTime.of(wheelHour, wheelMinute))
                                            }
                                        }
                                    }
                                },
                                update = { picker ->
                                    if (picker.value != wheelMinute) picker.value = wheelMinute
                                }
                            )
                        }
                    }

                    Text(stringResource(R.string.dialog_current_selection_format, time.format(DateTimeFormatter.ofPattern("HH:mm"))))
                }
            }
        }
    }
}

@Composable
fun DatePickerButton(label: String, date: LocalDate, onDatePicked: (LocalDate) -> Unit) {
    val context = LocalContext.current
    NeutralActionButton(
        onClick = {
            DatePickerDialog(
                context,
                { _, y, m, d -> onDatePicked(LocalDate.of(y, m + 1, d)) },
                date.year,
                date.monthValue - 1,
                date.dayOfMonth
            ).show()
        }
    ) {
        Text(stringResource(R.string.page_components_date_picker_button_format, label, date))
    }
}

@Composable
fun WeekdaySelector(selectedDays: Set<DayOfWeek>, onToggle: (DayOfWeek) -> Unit) {
    val labels = listOf(
        DayOfWeek.MONDAY to stringResource(R.string.shift_day_mon),
        DayOfWeek.TUESDAY to stringResource(R.string.shift_day_tue),
        DayOfWeek.WEDNESDAY to stringResource(R.string.shift_day_wed),
        DayOfWeek.THURSDAY to stringResource(R.string.shift_day_thu),
        DayOfWeek.FRIDAY to stringResource(R.string.shift_day_fri),
        DayOfWeek.SATURDAY to stringResource(R.string.shift_day_sat),
        DayOfWeek.SUNDAY to stringResource(R.string.shift_day_sun)
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            labels.take(4).forEach { (day, label) ->
                Button(onClick = { onToggle(day) }, modifier = Modifier.weight(1f), colors = segmentedActionButtonColors(day in selectedDays)) {
                    val mark = if (day in selectedDays) "[x]" else "[ ]"
                    Text(stringResource(R.string.page_components_weekday_toggle_format, mark, label))
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            labels.drop(4).forEach { (day, label) ->
                Button(onClick = { onToggle(day) }, modifier = Modifier.weight(1f), colors = segmentedActionButtonColors(day in selectedDays)) {
                    val mark = if (day in selectedDays) "[x]" else "[ ]"
                    Text(stringResource(R.string.page_components_weekday_toggle_format, mark, label))
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun AlarmItem(
    alarm: AlarmRule,
    isEditing: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onToggleTodaySkip: () -> Unit,
    onToggleTomorrowAdd: () -> Unit,
    onDuplicate: () -> Unit,
    onEdit: () -> Unit
) {
    val next = remember(alarm) { AlarmTimeCalculator.nextTrigger(alarm) }
    val resources = LocalContext.current.resources
    val soundLabel = when (alarm.soundType) {
        AlarmSoundType.ALARM -> stringResource(R.string.editor_sound_alarm)
        AlarmSoundType.NOTIFICATION -> stringResource(R.string.editor_sound_notification)
        AlarmSoundType.CUSTOM -> stringResource(R.string.editor_sound_custom)
    }
    val snoozeLimitLabel = if (alarm.snoozeMaxCount <= 0) stringResource(R.string.page_components_snooze_limit_unlimited_short) else stringResource(R.string.page_components_snooze_limit_format_short, alarm.snoozeMaxCount)
    val skippedToday = LocalDate.now() in alarm.skipDateEpochDays
    val tomorrowAdded = LocalDate.now().plusDays(1) in alarm.addDateEpochDays
    val nextAlarmLabel = if (next == null) {
        stringResource(R.string.common_none)
    } else {
        stringResource(
            R.string.exception_next_label_format,
            next.format(DateTimeFormatter.ofPattern("MM-dd HH:mm")),
            formatTimeUntil(resources, next)
        )
    }

    val weekSummary = alarm.weeklyPattern.mapIndexedNotNull { idx, days ->
        if (days.isEmpty()) null
        else {
            val dayText = days.sortedBy { it.value }.joinToString { dayOfWeekLabel(resources, it) }
            stringResource(R.string.page_components_week_summary_format, idx + 1, dayText)
        }
    }
    val exceptionSummary = listOf(
        if (alarm.skipDateEpochDays.isNotEmpty()) stringResource(R.string.page_components_exception_skip_days_format, alarm.skipDateEpochDays.size) else null,
        if (alarm.addDateEpochDays.isNotEmpty()) stringResource(R.string.page_components_exception_add_days_format, alarm.addDateEpochDays.size) else null
    ).filterNotNull().joinToString(" / ")

    var showDetails by remember(alarm.id) { mutableStateOf(false) }
    var showActions by remember(alarm.id) { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (alarm.enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                    if (isEditing) {
                        StatusChip(stringResource(R.string.page_components_editing), MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    if (alarm.label.isNotBlank()) {
                        Text(alarm.label, style = MaterialTheme.typography.titleMedium)
                    }
                    Text(String.format("%02d:%02d", alarm.hour, alarm.minute), style = MaterialTheme.typography.headlineMedium)
                    Text(stringResource(R.string.exception_next_alarm, nextAlarmLabel))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.page_components_enabled))
                    Switch(checked = alarm.enabled, onCheckedChange = { onToggle() })
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                StatusChip(stringResource(R.string.editor_rotation_cycle_value_format, alarm.intervalWeeks), MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                StatusChip(soundLabel, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                StatusChip(stringResource(R.string.page_components_snooze_chip_format, alarm.snoozeMinutes), MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                if (exceptionSummary.isNotBlank()) {
                    StatusChip(exceptionSummary, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }

            if (showDetails) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(stringResource(R.string.page_components_anchor_date_format, alarm.anchorDate))
                    Text(stringResource(R.string.page_components_volume_vibration_count_format, alarm.volumePercent, if (alarm.vibrationEnabled) stringResource(R.string.common_on) else stringResource(R.string.common_off), snoozeLimitLabel))
                    if (weekSummary.isNotEmpty()) {
                        weekSummary.forEach { Text(it) }
                    }
                    if (alarm.skipDateEpochDays.isNotEmpty() || alarm.addDateEpochDays.isNotEmpty()) {
                        Text(
                            stringResource(
                                R.string.page_components_exception_dates_format,
                                alarm.skipDateEpochDays.sorted().joinToString(),
                                alarm.addDateEpochDays.sorted().joinToString()
                            )
                        )
                    }
                    if (alarm.soundType == AlarmSoundType.CUSTOM) {
                        Text(stringResource(R.string.page_components_custom_uri_format, alarm.customSoundUri ?: stringResource(R.string.common_none)))
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                PrimaryActionButton(onClick = onEdit, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.page_components_edit_action)) }
                NeutralActionButton(onClick = { showActions = !showActions }, modifier = Modifier.weight(1f)) {
                    Text(if (showActions) stringResource(R.string.page_components_more_close) else stringResource(R.string.page_components_more_show))
                }
            }

            if (showActions) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    SecondaryActionButton(onClick = onToggleTodaySkip, modifier = Modifier.weight(1f)) {
                        Text(if (skippedToday) stringResource(R.string.exception_today_skip_cancel) else stringResource(R.string.exception_today_skip))
                    }
                    SecondaryActionButton(onClick = onToggleTomorrowAdd, modifier = Modifier.weight(1f)) {
                        Text(if (tomorrowAdded) stringResource(R.string.exception_tomorrow_add_cancel) else stringResource(R.string.exception_tomorrow_add))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    NeutralActionButton(onClick = { showDetails = !showDetails }, modifier = Modifier.weight(1f)) {
                        Text(if (showDetails) stringResource(R.string.page_components_details_close) else stringResource(R.string.page_components_details_show))
                    }
                    NeutralActionButton(onClick = onDuplicate, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.page_components_duplicate)) }
                }
                DangerActionButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.common_delete)) }
            }
        }
    }
}

@Composable
private fun StatusChip(text: String, bg: Color, fg: Color) {
    Box(
        modifier = Modifier
            .background(bg, shape = RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = text, color = fg, style = MaterialTheme.typography.labelMedium)
    }
}







