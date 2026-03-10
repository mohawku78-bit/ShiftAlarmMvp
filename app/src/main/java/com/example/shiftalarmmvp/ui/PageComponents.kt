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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
    label: String = "시간 선택"
) {
    var expanded by remember { mutableStateOf(false) }
    var inputMode by remember { mutableStateOf(false) }
    val latestTime by rememberUpdatedState(time)
    var hourInput by remember(time) { mutableStateOf(String.format("%02d", time.hour)) }
    var minuteInput by remember(time) { mutableStateOf(String.format("%02d", time.minute)) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SecondaryActionButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("$label ${time.format(DateTimeFormatter.ofPattern("HH:mm"))}")
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
                            Text("휠 조절")
                        }
                        Button(
                            onClick = { inputMode = true },
                            modifier = Modifier.weight(1f),
                            colors = segmentedActionButtonColors(inputMode)
                        ) {
                            Text("숫자 입력")
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
                                        onTimePicked(LocalTime.of(hour, minute))
                                    }
                                },
                                label = { Text("시") },
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
                                        onTimePicked(LocalTime.of(hour, minute))
                                    }
                                },
                                label = { Text("분") },
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
                                            val current = latestTime
                                            if (newVal != current.hour) {
                                                onTimePicked(LocalTime.of(newVal, current.minute))
                                            }
                                        }
                                    }
                                },
                                update = { picker ->
                                    if (picker.value != time.hour) picker.value = time.hour
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
                                            val current = latestTime
                                            if (newVal != current.minute) {
                                                onTimePicked(LocalTime.of(current.hour, newVal))
                                            }
                                        }
                                    }
                                },
                                update = { picker ->
                                    if (picker.value != time.minute) picker.value = time.minute
                                }
                            )
                        }
                    }

                    Text("현재 선택: ${time.format(DateTimeFormatter.ofPattern("HH:mm"))}")
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
        Text("$label: $date")
    }
}

@Composable
fun WeekdaySelector(selectedDays: Set<DayOfWeek>, onToggle: (DayOfWeek) -> Unit) {
    val labels = listOf(
        DayOfWeek.MONDAY to "월",
        DayOfWeek.TUESDAY to "화",
        DayOfWeek.WEDNESDAY to "수",
        DayOfWeek.THURSDAY to "목",
        DayOfWeek.FRIDAY to "금",
        DayOfWeek.SATURDAY to "토",
        DayOfWeek.SUNDAY to "일"
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            labels.take(4).forEach { (day, label) ->
                Button(onClick = { onToggle(day) }, modifier = Modifier.weight(1f), colors = segmentedActionButtonColors(day in selectedDays)) {
                    val mark = if (day in selectedDays) "[x]" else "[ ]"
                    Text("$mark $label")
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            labels.drop(4).forEach { (day, label) ->
                Button(onClick = { onToggle(day) }, modifier = Modifier.weight(1f), colors = segmentedActionButtonColors(day in selectedDays)) {
                    val mark = if (day in selectedDays) "[x]" else "[ ]"
                    Text("$mark $label")
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
    val soundLabel = when (alarm.soundType) {
        AlarmSoundType.ALARM -> "알람음"
        AlarmSoundType.NOTIFICATION -> "알림음"
        AlarmSoundType.CUSTOM -> "커스텀"
    }
    val snoozeLimitLabel = if (alarm.snoozeMaxCount <= 0) "무제한" else "최대 ${alarm.snoozeMaxCount}회"
    val skippedToday = LocalDate.now() in alarm.skipDateEpochDays
    val tomorrowAdded = LocalDate.now().plusDays(1) in alarm.addDateEpochDays
    val nextAlarmLabel = if (next == null) {
        "없음"
    } else {
        "${next.format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))} (${formatTimeUntil(next)} 후)"
    }

    val weekSummary = alarm.weeklyPattern.mapIndexedNotNull { idx, days ->
        if (days.isEmpty()) null
        else {
            val dayText = days.sortedBy { it.value }.joinToString { dayOfWeekLabel(it) }
            "${idx + 1}주차 $dayText"
        }
    }
    val exceptionSummary = listOf(
        if (alarm.skipDateEpochDays.isNotEmpty()) "스킵 ${alarm.skipDateEpochDays.size}일" else null,
        if (alarm.addDateEpochDays.isNotEmpty()) "추가 ${alarm.addDateEpochDays.size}일" else null
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
                        StatusChip("편집 중", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    if (alarm.label.isNotBlank()) {
                        Text(alarm.label, style = MaterialTheme.typography.titleMedium)
                    }
                    Text(String.format("%02d:%02d", alarm.hour, alarm.minute), style = MaterialTheme.typography.headlineMedium)
                    Text("다음 울림: $nextAlarmLabel")
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("사용")
                    Switch(checked = alarm.enabled, onCheckedChange = { onToggle() })
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                StatusChip("${alarm.intervalWeeks}주", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                StatusChip(soundLabel, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                StatusChip("스누즈 ${alarm.snoozeMinutes}분", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                if (exceptionSummary.isNotBlank()) {
                    StatusChip(exceptionSummary, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }

            if (showDetails) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("기준일: ${alarm.anchorDate}")
                    Text("볼륨 ${alarm.volumePercent}% / 진동 ${if (alarm.vibrationEnabled) "ON" else "OFF"} / 횟수 $snoozeLimitLabel")
                    if (weekSummary.isNotEmpty()) {
                        weekSummary.forEach { Text(it) }
                    }
                    if (alarm.skipDateEpochDays.isNotEmpty() || alarm.addDateEpochDays.isNotEmpty()) {
                        Text(
                            "예외일\n스킵: ${alarm.skipDateEpochDays.sorted().joinToString()}\n추가: ${alarm.addDateEpochDays.sorted().joinToString()}"
                        )
                    }
                    if (alarm.soundType == AlarmSoundType.CUSTOM) {
                        Text("커스텀 URI: ${alarm.customSoundUri ?: "없음"}")
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                PrimaryActionButton(onClick = onEdit, modifier = Modifier.weight(1f)) { Text("편집") }
                NeutralActionButton(onClick = { showActions = !showActions }, modifier = Modifier.weight(1f)) {
                    Text(if (showActions) "더보기 닫기" else "더보기")
                }
            }

            if (showActions) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    SecondaryActionButton(onClick = onToggleTodaySkip, modifier = Modifier.weight(1f)) {
                        Text(if (skippedToday) "오늘 스킵 취소" else "오늘 스킵")
                    }
                    SecondaryActionButton(onClick = onToggleTomorrowAdd, modifier = Modifier.weight(1f)) {
                        Text(if (tomorrowAdded) "내일 추가 취소" else "내일 추가")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    NeutralActionButton(onClick = { showDetails = !showDetails }, modifier = Modifier.weight(1f)) {
                        Text(if (showDetails) "상세 닫기" else "상세 보기")
                    }
                    NeutralActionButton(onClick = onDuplicate, modifier = Modifier.weight(1f)) { Text("복제") }
                }
                DangerActionButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) { Text("삭제") }
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







