package com.example.shiftalarmmvp.ui

import android.os.Build
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.shiftalarmmvp.data.AlarmSoundType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

private val EDITOR_STEPS = listOf("기본", "반복", "알림", "고급")

@Composable
fun EditorPage(
    editingAlarmId: Long?,
    selectedLabel: String,
    onSelectedLabelChange: (String) -> Unit,
    canScheduleExact: Boolean,
    isIgnoringBatteryOptimization: Boolean,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenAppDetailSettings: () -> Unit,
    onRescheduleAllEnabled: () -> Unit,
    canPostNotifications: Boolean,
    onRequestNotificationPermission: () -> Unit,
    setupWizardDismissed: Boolean,
    onSetupWizardDismissedChange: (Boolean) -> Unit,
    autoSaveOnDuplicate: Boolean,
    onAutoSaveOnDuplicateChange: (Boolean) -> Unit,
    onPlayTestSound: () -> Unit,
    onStopTestSound: () -> Unit,
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
    requiresExactPermission: Boolean,
    initialStep: Int? = null
) {
    var step by rememberSaveable(editingAlarmId) { mutableIntStateOf(0) }
    var currentNow by remember { mutableStateOf(LocalDateTime.now()) }
    val lastStep = EDITOR_STEPS.lastIndex

    LaunchedEffect(initialStep, lastStep) {
        initialStep?.let { forced ->
            step = forced.coerceIn(0, lastStep)
        }
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
                Text("편집 단계", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EDITOR_STEPS.forEachIndexed { idx, label ->
                        val selected = step == idx
                        Button(onClick = { step = idx }, colors = segmentedActionButtonColors(selected)) {
                            Text("${idx + 1}. $label")
                        }
                    }
                }
                Text("현재 단계: ${step + 1}/${EDITOR_STEPS.size} ${EDITOR_STEPS[step]}")
            }
        }

        when (step) {
                        0 -> {
                OutlinedTextField(
                    value = selectedLabel,
                    onValueChange = { onSelectedLabelChange(it.take(24)) },
                    label = { Text("알람 이름(선택)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("현재 시간 기준 설정", style = MaterialTheme.typography.titleSmall)
                        Text(currentNow.format(DateTimeFormatter.ofPattern("HH:mm:ss")), style = MaterialTheme.typography.headlineSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(onClick = { onSelectedTimeChange(currentNow.toLocalTime().withSecond(0).withNano(0)) }, modifier = Modifier.weight(1f)) {
                                Text("현재시간 적용")
                            }
                            Button(onClick = { onSelectedTimeChange(currentNow.toLocalTime().plusMinutes(10).withSecond(0).withNano(0)) }, modifier = Modifier.weight(1f)) {
                                Text("+10분")
                            }
                            Button(onClick = { onSelectedTimeChange(currentNow.toLocalTime().plusMinutes(30).withSecond(0).withNano(0)) }, modifier = Modifier.weight(1f)) {
                                Text("+30분")
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
                DatePickerButton(label = "로테이션 기준일", date = anchorDate, onDatePicked = onAnchorDateChange)

                Text("로테이션 주기")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(2, 3, 4).forEach { value ->
                        Row {
                            RadioButton(
                                selected = intervalWeeks == value,
                                onClick = {
                                    onIntervalWeeksChange(value)
                                    if (activeWeekIndex >= value) onActiveWeekIndexChange(value - 1)
                                }
                            )
                            Text("${value}주")
                        }
                    }
                }

                Text("편집할 주차")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                ) {
                    repeat(intervalWeeks) { index ->
                        Button(onClick = { onActiveWeekIndexChange(index) }, colors = segmentedActionButtonColors(activeWeekIndex == index)) {
                            val mark = if (activeWeekIndex == index) "*" else ""
                            Text("${index + 1}주차$mark")
                        }
                    }
                }

                Text("${activeWeekIndex + 1}주차 요일 선택")
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
                        Text("무한 반복")
                        Switch(
                            checked = infiniteRotationEnabled,
                            onCheckedChange = onInfiniteRotationEnabledChange
                        )
                        Text(if (infiniteRotationEnabled) "ON" else "OFF")
                    }
                }
            }

            2 -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = onPlayTestSound, modifier = Modifier.weight(1f)) {
                        Text("테스트 소리 재생")
                    }
                    Button(onClick = onStopTestSound, modifier = Modifier.weight(1f)) {
                        Text("테스트 중지")
                    }
                }

                Text("소리 선택")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row {
                        RadioButton(
                            selected = selectedSoundType == AlarmSoundType.ALARM,
                            onClick = { onSelectedSoundTypeChange(AlarmSoundType.ALARM) }
                        )
                        Text("알람음")
                    }
                    Row {
                        RadioButton(
                            selected = selectedSoundType == AlarmSoundType.NOTIFICATION,
                            onClick = { onSelectedSoundTypeChange(AlarmSoundType.NOTIFICATION) }
                        )
                        Text("알림음")
                    }
                    Row {
                        RadioButton(
                            selected = selectedSoundType == AlarmSoundType.CUSTOM,
                            onClick = { onSelectedSoundTypeChange(AlarmSoundType.CUSTOM) }
                        )
                        Text("커스텀")
                    }
                }

                Button(onClick = onPickCustomSound) {
                    Text("커스텀 소리 선택")
                }
                Text("선택된 커스텀: ${selectedCustomSoundUri ?: "없음"}")
                if (customSoundMessage.isNotBlank()) Text(customSoundMessage)

                Text("볼륨: ${selectedVolume.toInt()}%")
                Slider(
                    value = selectedVolume,
                    onValueChange = onSelectedVolumeChange,
                    valueRange = 0f..100f
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("진동")
                    Switch(checked = vibrationEnabled, onCheckedChange = onVibrationEnabledChange)
                }

                Text("스누즈 설정")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                ) {
                    listOf(1, 3, 5, 10, 15, 20, 30).forEach { value ->
                        Button(onClick = { onSelectedSnoozeMinutesChange(value) }, colors = segmentedActionButtonColors(selectedSnoozeMinutes == value)) {
                            val mark = if (selectedSnoozeMinutes == value) "[x]" else "[ ]"
                            Text("$mark ${value}분")
                        }
                    }
                }

                OutlinedTextField(
                    value = customSnoozeInput,
                    onValueChange = onCustomSnoozeInputChange,
                    label = { Text("직접 입력(분)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("현재 스누즈: ${selectedSnoozeMinutes}분")

                Text("스누즈 횟수 (0 = 무제한)")
                OutlinedTextField(
                    value = customMaxCountInput,
                    onValueChange = onCustomMaxCountInputChange,
                    label = { Text("최대 스누즈 횟수") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    when {
                        selectedSnoozeMaxCount <= 0 -> "횟수 제한: 무제한"
                        else -> "횟수 제한: 최대 ${selectedSnoozeMaxCount}회"
                    }
                )
            }

            else -> {
                val exactReady = !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExact)
                val batteryReady = !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isIgnoringBatteryOptimization)
                val notificationReady = canPostNotifications
                val allSetupReady = exactReady && batteryReady && notificationReady

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("신뢰성 상태", style = MaterialTheme.typography.titleSmall)
                        Text(if (exactReady) "정확 알람 권한: 켜짐" else "정확 알람 권한: 꺼짐")
                        Text(if (batteryReady) "배터리 최적화: 예외 또는 미적용" else "배터리 최적화: 켜짐(지연 가능)")
                        Text(if (notificationReady) "알림 권한: 완료" else "알림 권한: 필요")

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            if (!exactReady && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                Button(onClick = onOpenExactAlarmSettings, modifier = Modifier.fillMaxWidth()) { Text("정확 알람 설정") }
                            }
                            if (!batteryReady && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                Button(onClick = onOpenBatterySettings, modifier = Modifier.fillMaxWidth()) { Text("배터리 설정") }
                            }
                            if (!notificationReady && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                Button(onClick = onRequestNotificationPermission, modifier = Modifier.fillMaxWidth()) { Text("알림 권한 허용") }
                            }
                            Button(onClick = onRescheduleAllEnabled, modifier = Modifier.fillMaxWidth()) { Text("알람 재예약") }
                            Button(onClick = onOpenAppDetailSettings, modifier = Modifier.fillMaxWidth()) { Text("앱 정보") }
                        }

                        if (!setupWizardDismissed || !allSetupReady) {
                            Button(
                                onClick = { onSetupWizardDismissedChange(true) },
                                enabled = allSetupReady
                            ) {
                                Text("설정 완료, 카드 숨기기")
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("복제 시 즉시 저장")
                    Switch(checked = autoSaveOnDuplicate, onCheckedChange = onAutoSaveOnDuplicateChange)
                }

                Text("예외일 설정")
                DatePickerButton(label = "예외 날짜 선택", date = exceptionDate, onDatePicked = onExceptionDateChange)
                val isSkipSelected = exceptionDate in skipDates
                val isAddSelected = exceptionDate in addDates
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            if (isSkipSelected) {
                                onSkipDatesChange(skipDates - exceptionDate)
                            } else {
                                onSkipDatesChange(skipDates + exceptionDate)
                                onAddDatesChange(addDates - exceptionDate)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isSkipSelected) "스킵일 취소" else "스킵일로 추가")
                    }
                    Button(
                        onClick = {
                            if (isAddSelected) {
                                onAddDatesChange(addDates - exceptionDate)
                            } else {
                                onAddDatesChange(addDates + exceptionDate)
                                onSkipDatesChange(skipDates - exceptionDate)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isAddSelected) "추가 알람일 취소" else "추가 알람일로 추가")
                    }
                }

                if (skipDates.isNotEmpty()) {
                    Text("스킵일: ${skipDates.sorted().joinToString { it.toString() }}")
                }
                if (addDates.isNotEmpty()) {
                    Text("추가일: ${addDates.sorted().joinToString { it.toString() }}")
                }

                Button(onClick = { onShowNext10Change(!showNext10) }) {
                    Text(if (showNext10) "다음 10회 예정 숨기기" else "다음 10회 예정 보기")
                }
                if (showNext10) {
                    if (next10Preview.isEmpty()) {
                        Text("없음")
                    } else {
                        next10Preview.forEachIndexed { idx, dt ->
                            Text("${idx + 1}. ${dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}")
                        }
                    }
                }

                if (requiresExactPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Text("정확 알람 권한이 꺼져 있어 근사 알람으로 동작할 수 있습니다. 가능하면 권한을 허용하세요.")
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            if (step > 0) {
                NeutralActionButton(onClick = { step -= 1 }, modifier = Modifier.weight(1f)) {
                    Text("이전")
                }
            }

            if (step < lastStep) {
                PrimaryActionButton(onClick = { step += 1 }, modifier = Modifier.weight(1f)) {
                    Text("다음")
                }
            } else {
                PrimaryActionButton(
                    onClick = onSaveOrUpdate,
                    enabled = canSaveByPermission,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (editingAlarmId == null) "알람 저장" else "알람 수정 저장")
                }
            }

            if (step == lastStep && editingAlarmId != null) {
                NeutralActionButton(onClick = onCancelEdit, modifier = Modifier.weight(1f)) {
                    Text("편집 취소")
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

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text("시간 입력 (휠 또는 텍스트)", style = MaterialTheme.typography.titleSmall)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            AndroidView(
                modifier = Modifier.weight(1f),
                factory = { context ->
                    NumberPicker(context).apply {
                        minValue = 0
                        maxValue = 23
                        wrapSelectorWheel = true
                        setOnValueChangedListener { _, _, newVal ->
                            onSelectedTimeChange(LocalTime.of(newVal, selectedTime.minute))
                        }
                    }
                },
                update = { picker ->
                    if (picker.value != selectedTime.hour) picker.value = selectedTime.hour
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
                            onSelectedTimeChange(LocalTime.of(selectedTime.hour, newVal))
                        }
                    }
                },
                update = { picker ->
                    if (picker.value != selectedTime.minute) picker.value = selectedTime.minute
                }
            )
        }

        OutlinedTextField(
            value = textInput,
            onValueChange = { value ->
                textInput = value.take(5)
                parseHm(value)?.let { parsed -> onSelectedTimeChange(parsed) }
            },
            label = { Text("직접 입력 (HH:mm)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Text("선택 시간: ${selectedTime.format(DateTimeFormatter.ofPattern("HH:mm"))}")
    }
}






