package com.example.shiftalarmmvp.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    var activePart by remember { mutableStateOf(TimeInputPart.HOUR) }
    var typedDigits by remember { mutableStateOf("") }
    var wheelHour by remember { mutableIntStateOf(time.hour) }
    var wheelMinute by remember { mutableIntStateOf(time.minute) }
    val latestOnTimePicked by rememberUpdatedState(onTimePicked)
    val resolvedLabel = label ?: stringResource(R.string.dialog_time_select)

    fun applyTime(nextHour: Int, nextMinute: Int) {
        val safeHour = wrapTimeValue(nextHour, 24)
        val safeMinute = wrapTimeValue(nextMinute, 60)
        wheelHour = safeHour
        wheelMinute = safeMinute
        latestOnTimePicked(LocalTime.of(safeHour, safeMinute))
    }

    fun selectPart(part: TimeInputPart) {
        expanded = true
        activePart = part
        typedDigits = ""
    }

    fun adjustActivePart(delta: Int) {
        typedDigits = ""
        if (activePart == TimeInputPart.HOUR) {
            applyTime(wheelHour + delta, wheelMinute)
        } else {
            applyTime(wheelHour, wheelMinute + delta)
        }
    }

    LaunchedEffect(time) {
        wheelHour = time.hour
        wheelMinute = time.minute
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ShiftPanel(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    expanded = true
                    typedDigits = ""
                },
            containerColor = ShiftDesign.Paper,
            borderColor = if (expanded) ShiftDesign.Harbor.copy(alpha = 0.72f) else ShiftDesign.Line
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = resolvedLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = ShiftDesign.InkSoft,
                        fontWeight = FontWeight.Bold
                    )
                    ShiftPill(
                        text = stringResource(
                            if (expanded) R.string.dialog_time_adjusting else R.string.dialog_time_tap_hint
                        ),
                        containerColor = if (expanded) ShiftDesign.Sun.copy(alpha = 0.28f) else ShiftDesign.Mist,
                        contentColor = ShiftDesign.Navy
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EditableTimePart(
                        label = stringResource(R.string.dialog_hour),
                        value = wheelHour,
                        selected = expanded && activePart == TimeInputPart.HOUR,
                        onClick = { selectPart(TimeInputPart.HOUR) },
                        onStep = { delta ->
                            activePart = TimeInputPart.HOUR
                            adjustActivePart(delta)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = ":",
                        style = MaterialTheme.typography.displaySmall,
                        color = ShiftDesign.InkSoft,
                        fontWeight = FontWeight.Bold
                    )
                    EditableTimePart(
                        label = stringResource(R.string.dialog_minute),
                        value = wheelMinute,
                        selected = expanded && activePart == TimeInputPart.MINUTE,
                        onClick = { selectPart(TimeInputPart.MINUTE) },
                        onStep = { delta ->
                            activePart = TimeInputPart.MINUTE
                            adjustActivePart(delta)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (expanded) {
                    Text(
                        text = stringResource(R.string.dialog_time_adjust_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ShiftDesign.InkSoft
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NeutralActionButton(
                            onClick = { adjustActivePart(-1) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.dialog_time_step_down))
                        }
                        ShiftPill(
                            text = stringResource(
                                R.string.dialog_time_keypad_hint,
                                stringResource(
                                    if (activePart == TimeInputPart.HOUR) {
                                        R.string.dialog_time_active_hour
                                    } else {
                                        R.string.dialog_time_active_minute
                                    }
                                )
                            ),
                            modifier = Modifier.weight(1f),
                            containerColor = ShiftDesign.Mist,
                            contentColor = ShiftDesign.Navy
                        )
                        NeutralActionButton(
                            onClick = { adjustActivePart(1) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.dialog_time_step_up))
                        }
                    }

                    TimeNumericKeypad(
                        activePart = activePart,
                        onDigit = { digit ->
                            val max = if (activePart == TimeInputPart.HOUR) 23 else 59
                            val nextDigits = (typedDigits + digit).takeLast(2)
                            val parsed = nextDigits.toIntOrNull()
                            if (parsed != null) {
                                val normalized = parsed.coerceIn(0, max)
                                typedDigits = if (parsed == normalized) {
                                    nextDigits
                                } else {
                                    String.format("%02d", normalized)
                                }
                                if (activePart == TimeInputPart.HOUR) {
                                    applyTime(normalized, wheelMinute)
                                } else {
                                    applyTime(wheelHour, normalized)
                                }
                            }
                        },
                        onBackspace = {
                            typedDigits = typedDigits.dropLast(1)
                        },
                        onDone = {
                            typedDigits = ""
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

private enum class TimeInputPart {
    HOUR,
    MINUTE
}

private fun wrapTimeValue(value: Int, maxExclusive: Int): Int {
    val mod = value % maxExclusive
    return if (mod < 0) mod + maxExclusive else mod
}

@Composable
private fun EditableTimePart(
    label: String,
    value: Int,
    selected: Boolean,
    onClick: () -> Unit,
    onStep: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    ShiftPanel(
        modifier = modifier.clickable(onClick = onClick),
        containerColor = if (selected) ShiftDesign.Paper else ShiftDesign.Mist.copy(alpha = 0.54f),
        borderColor = if (selected) ShiftDesign.Sun else ShiftDesign.Line
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) ShiftDesign.Navy else ShiftDesign.InkSoft,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format("%02d", value),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.displaySmall,
                    color = ShiftDesign.Ink,
                    fontWeight = FontWeight.ExtraBold
                )
                Column(
                    modifier = Modifier.weight(0.82f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TimeStepButton(label = "+", selected = selected, onClick = { onStep(1) })
                    TimeStepButton(label = "-", selected = selected, onClick = { onStep(-1) })
                }
            }
            Text(
                text = stringResource(
                    if (selected) R.string.dialog_time_drag_hint else R.string.dialog_time_tap_hint
                ),
                style = MaterialTheme.typography.labelMedium,
                color = ShiftDesign.InkSoft
            )
        }
    }
}

@Composable
private fun TimeStepButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = label,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selected) ShiftDesign.Sun.copy(alpha = 0.28f) else ShiftDesign.Mist,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.titleLarge,
        color = ShiftDesign.Navy,
        fontWeight = FontWeight.ExtraBold
    )
}

@Composable
private fun TimeNumericKeypad(
    activePart: TimeInputPart,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeLabel = stringResource(
        if (activePart == TimeInputPart.HOUR) {
            R.string.dialog_time_active_hour
        } else {
            R.string.dialog_time_active_minute
        }
    )
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.dialog_time_keypad_hint, activeLabel),
            style = MaterialTheme.typography.bodyMedium,
            color = ShiftDesign.InkSoft
        )
        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9")
        )
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { digit ->
                    NeutralActionButton(onClick = { onDigit(digit) }, modifier = Modifier.weight(1f)) {
                        Text(digit, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            NeutralActionButton(onClick = onBackspace, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.common_backspace))
            }
            NeutralActionButton(onClick = { onDigit("0") }, modifier = Modifier.weight(1f)) {
                Text("0", style = MaterialTheme.typography.titleMedium)
            }
            PrimaryActionButton(onClick = onDone, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.common_done))
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

    ShiftPanel(
        modifier = Modifier.fillMaxWidth(),
        containerColor = if (alarm.enabled) ShiftDesign.Paper else ShiftDesign.Mist.copy(alpha = 0.72f),
        borderColor = if (alarm.enabled) ShiftDesign.Line else ShiftDesign.Line.copy(alpha = 0.72f)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                    if (isEditing) {
                        StatusChip(
                            stringResource(R.string.page_components_editing),
                            ShiftDesign.Sun.copy(alpha = 0.24f),
                            ShiftDesign.Day
                        )
                    }
                    if (alarm.label.isNotBlank()) {
                        Text(
                            alarm.label,
                            style = MaterialTheme.typography.titleMedium,
                            color = ShiftDesign.Ink
                        )
                    }
                    Text(
                        String.format("%02d:%02d", alarm.hour, alarm.minute),
                        style = MaterialTheme.typography.headlineMedium,
                        color = ShiftDesign.Navy,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        stringResource(R.string.exception_next_alarm, nextAlarmLabel),
                        color = ShiftDesign.InkSoft,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.page_components_enabled), color = ShiftDesign.InkSoft)
                    Switch(checked = alarm.enabled, onCheckedChange = { onToggle() })
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            ) {
                StatusChip(
                    stringResource(R.string.editor_rotation_cycle_value_format, alarm.intervalWeeks),
                    ShiftDesign.Navy.copy(alpha = 0.12f),
                    ShiftDesign.Navy
                )
                StatusChip(soundLabel, ShiftDesign.Mist, ShiftDesign.Harbor)
                StatusChip(
                    stringResource(R.string.page_components_snooze_chip_format, alarm.snoozeMinutes),
                    ShiftDesign.Mist,
                    ShiftDesign.InkSoft
                )
                if (exceptionSummary.isNotBlank()) {
                    StatusChip(exceptionSummary, ShiftDesign.Sun.copy(alpha = 0.22f), ShiftDesign.Day)
                }
            }

            if (showDetails) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(stringResource(R.string.page_components_anchor_date_format, alarm.anchorDate), color = ShiftDesign.InkSoft)
                    Text(stringResource(R.string.page_components_volume_vibration_count_format, alarm.volumePercent, if (alarm.vibrationEnabled) stringResource(R.string.common_on) else stringResource(R.string.common_off), snoozeLimitLabel), color = ShiftDesign.InkSoft)
                    if (weekSummary.isNotEmpty()) {
                        weekSummary.forEach { Text(it, color = ShiftDesign.InkSoft) }
                    }
                    if (alarm.skipDateEpochDays.isNotEmpty() || alarm.addDateEpochDays.isNotEmpty()) {
                        Text(
                            stringResource(
                                R.string.page_components_exception_dates_format,
                                alarm.skipDateEpochDays.sorted().joinToString(),
                                alarm.addDateEpochDays.sorted().joinToString()
                            ),
                            color = ShiftDesign.InkSoft
                        )
                    }
                    if (alarm.soundType == AlarmSoundType.CUSTOM) {
                        Text(
                            stringResource(
                                R.string.page_components_custom_uri_format,
                                alarm.customSoundUri ?: stringResource(R.string.common_none)
                            ),
                            color = ShiftDesign.InkSoft
                        )
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
            .background(bg, shape = RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = fg,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}







