package com.example.shiftalarmmvp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.shiftalarmmvp.data.AlarmRule
import com.example.shiftalarmmvp.scheduler.AlarmTimeCalculator
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

private enum class DateAdjustMode {
    VACATION,
    SKIP,
    SHIFT_CHANGE
}

@Composable
fun HomePage(
    selectedLabel: String,
    selectedTime: LocalTime,
    nextTrigger: LocalDateTime?,
    rotationPreview: List<String>,
    alarms: List<AlarmRule>,
    onReconfigurePattern: () -> Unit,
    onOpenManage: () -> Unit,
    onSetVacationDate: (LocalDate) -> Unit,
    onClearVacationDate: (LocalDate) -> Unit,
    onSetVacationRange: (LocalDate, LocalDate) -> Unit,
    onClearVacationRange: (LocalDate, LocalDate) -> Unit,
    onSetSkipDateForIds: (LocalDate, Set<Long>) -> Unit,
    onClearSkipDateForIds: (LocalDate, Set<Long>) -> Unit,
    onApplyShiftChange: (LocalDate, String) -> Unit,
    undoMessage: String?,
    onUndoLastChange: () -> Unit
) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var vacationStart by remember { mutableStateOf(LocalDate.now()) }
    var vacationEnd by remember { mutableStateOf(LocalDate.now()) }
    var adjustMode by remember { mutableStateOf(DateAdjustMode.VACATION) }
    var selectedShiftType by remember { mutableStateOf<String?>(null) }
    var showDateAdjustControls by remember { mutableStateOf(false) }
    var showLegend by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1_000)
        }
    }

    val today = now.toLocalDate()
    val chosenDate = selectedDate
    val selectedBadge = chosenDate?.let { inferShiftBadgeForDate(it, alarms) } ?: "-"
    val alarmsForSelectedDate = chosenDate?.let { date ->
        alarms.filter { AlarmTimeCalculator.isScheduledOnDate(it, date) }
            .sortedWith(compareBy<AlarmRule> { it.hour }.thenBy { it.minute }.thenBy { it.label })
    }.orEmpty()
    val selectedIds = alarmsForSelectedDate.map { it.id }.toSet()
    val anyVacationApplied = chosenDate != null && alarms.any { chosenDate in it.skipDateEpochDays }
    val allVacationApplied = chosenDate != null && alarms.isNotEmpty() && alarms.all { chosenDate in it.skipDateEpochDays }
    val anySkipApplied = chosenDate != null && alarmsForSelectedDate.any { chosenDate in it.skipDateEpochDays }
    val allSkipApplied = alarmsForSelectedDate.isNotEmpty() && chosenDate != null && alarmsForSelectedDate.all { chosenDate in it.skipDateEpochDays }
    val vacationAppliedCount = if (chosenDate == null) 0 else alarms.count { chosenDate in it.skipDateEpochDays }
    val skipAppliedCount = if (chosenDate == null) 0 else alarmsForSelectedDate.count { chosenDate in it.skipDateEpochDays }
    val vacationExcludedAlarms = if (chosenDate == null) {
        emptyList()
    } else {
        alarms.filter { chosenDate !in it.skipDateEpochDays }
            .sortedWith(compareBy<AlarmRule> { it.hour }.thenBy { it.minute }.thenBy { it.label })
    }
    val vacationExcludedPreview = vacationExcludedAlarms.take(5).map { alarm ->
        val name = alarm.label.ifBlank { "이름 없음" }
        String.format("%02d:%02d %s", alarm.hour, alarm.minute, name)
    }
    val shiftTypeOptions = alarms.map { extractWorkTypeFromLabel(it.label) }.filter { it.isNotBlank() }.distinct()
    val selectedShiftTypeResolved = when {
        shiftTypeOptions.isEmpty() -> null
        selectedShiftType in shiftTypeOptions -> selectedShiftType
        else -> shiftTypeOptions.first()
    }

    val panelColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
    )
    val softPanelColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f)
    )
    val mutedButtonColors = neutralActionButtonColors()
    val selectedModeButtonColors = primaryActionButtonColors()
    val secondaryButtonColors = secondaryActionButtonColors()
    val summaryCardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    val summaryButtonColors = overlayActionButtonColors()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (undoMessage != null) {
            Card(modifier = Modifier.fillMaxWidth(), colors = softPanelColors) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(undoMessage, modifier = Modifier.weight(1f))
                    Button(onClick = onUndoLastChange, colors = secondaryButtonColors) {
                        Text("실행 취소")
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), colors = panelColors) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = { month = month.minusMonths(1) }, colors = mutedButtonColors) { Text("이전") }
                    Text(month.format(DateTimeFormatter.ofPattern("yyyy년 M월")), style = MaterialTheme.typography.titleMedium)
                    Button(onClick = { month = month.plusMonths(1) }, colors = mutedButtonColors) { Text("다음") }
                }
                Button(
                    onClick = { showLegend = !showLegend },
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (showLegend) selectedModeButtonColors else mutedButtonColors
                ) {
                    Text(if (showLegend) "\uBC94\uB840 \uC228\uAE30\uAE30" else "\uBC94\uB840 \uBCF4\uAE30")
                }

                if (showLegend) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ShiftLegendChip("주간", Color(0xFFD9E8FA), Color(0xFF1E4E8C))
                        ShiftLegendChip("야간", Color(0xFFFFE3C8), Color(0xFF9A5400))
                        ShiftLegendChip("당직", Color(0xFFFFE9D6), Color(0xFF8A3E00))
                        ShiftLegendChip("비번", Color(0xFFE3E8EE), Color(0xFF4F6375))
                        ShiftLegendChip("휴무", Color(0xFFEEF1F4), Color(0xFF5B6670))
                    }
                }

                ShiftCalendarMonthGrid(
                    month = month,
                    today = today,
                    selectedDate = selectedDate,
                    onDateSelected = {
                        selectedDate = it
                        vacationStart = it
                        vacationEnd = it
                        showDateAdjustControls = false
                    },
                    badgeForDate = { date -> inferShiftBadgeForDate(date, alarms) },
                    compact = false
                )
                Card(modifier = Modifier.fillMaxWidth(), colors = summaryCardColors) {
                    val nextTriggerText = nextTrigger?.format(DateTimeFormatter.ofPattern("MM-dd HH:mm")) ?: "예정 없음"
                    val remainingText = nextTrigger?.let { formatTimeUntil(it, now) } ?: "-"

                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("다음 알람", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text(
                            nextTriggerText,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                        Text("남은 시간: $remainingText", color = Color.White.copy(alpha = 0.94f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(onClick = onOpenManage, colors = summaryButtonColors) {
                                Text("\uAD00\uB9AC \uD654\uBA74")
                            }
                            Text(
                                text = "\uD328\uD134 \uB2E4\uC2DC \uC124\uC815",
                                modifier = Modifier.clickable(onClick = onReconfigurePattern),
                                color = Color.White.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }

                if (chosenDate == null) {
                    Text("날짜를 선택하면 예외 처리를 진행할 수 있습니다.")
                } else {
                    Text(
                        "선택 날짜: ${chosenDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))} / 근무: ${shiftBadgeLabel(selectedBadge)}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        val tomorrow = today.plusDays(1)
                        Button(
                            onClick = {
                                selectedDate = today
                                vacationStart = today
                                vacationEnd = today
                            },
                            modifier = Modifier.weight(1f),
                            colors = if (chosenDate == today) selectedModeButtonColors else mutedButtonColors
                        ) {
                            Text("오늘")
                        }
                        Button(
                            onClick = {
                                selectedDate = tomorrow
                                vacationStart = tomorrow
                                vacationEnd = tomorrow
                            },
                            modifier = Modifier.weight(1f),
                            colors = if (chosenDate == tomorrow) selectedModeButtonColors else mutedButtonColors
                        ) {
                            Text("내일")
                        }
                    }

                    Text("원탭 예외 처리", style = MaterialTheme.typography.titleSmall)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        val quickVacationClear = allVacationApplied
                        Button(
                            onClick = {
                                if (quickVacationClear) onClearVacationDate(chosenDate) else onSetVacationDate(chosenDate)
                            },
                            enabled = alarms.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                            colors = if (quickVacationClear) secondaryButtonColors else selectedModeButtonColors
                        ) {
                            Text(if (quickVacationClear) "휴가 해제 ($vacationAppliedCount/${alarms.size})" else "휴가 적용 ($vacationAppliedCount/${alarms.size})")
                        }

                        val quickSkipClear = allSkipApplied
                        Button(
                            onClick = {
                                if (quickSkipClear) onClearSkipDateForIds(chosenDate, selectedIds)
                                else onSetSkipDateForIds(chosenDate, selectedIds)
                            },
                            enabled = selectedIds.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                            colors = if (quickSkipClear) secondaryButtonColors else selectedModeButtonColors
                        ) {
                            Text(if (quickSkipClear) "스킵 해제 ($skipAppliedCount/${selectedIds.size})" else "스킵 적용 ($skipAppliedCount/${selectedIds.size})")
                        }
                    }
                    Text(
                        "세부 설정은 아래 예외 처리 열기에서 조정하세요.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(
                        onClick = { showDateAdjustControls = !showDateAdjustControls },
                        modifier = Modifier.fillMaxWidth(),
                        colors = if (showDateAdjustControls) selectedModeButtonColors else mutedButtonColors
                    ) {
                        Text(if (showDateAdjustControls) "\uC608\uC678 \uCC98\uB9AC \uB2EB\uAE30" else "\uC608\uC678 \uCC98\uB9AC \uC5F4\uAE30")
                    }

                    if (showDateAdjustControls) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { adjustMode = DateAdjustMode.VACATION },
                            modifier = Modifier.weight(1f),
                            colors = if (adjustMode == DateAdjustMode.VACATION) selectedModeButtonColors else mutedButtonColors
                        ) { Text("휴가") }
                        Button(
                            onClick = { adjustMode = DateAdjustMode.SKIP },
                            modifier = Modifier.weight(1f),
                            colors = if (adjustMode == DateAdjustMode.SKIP) selectedModeButtonColors else mutedButtonColors
                        ) { Text("스킵") }
                        Button(
                            onClick = { adjustMode = DateAdjustMode.SHIFT_CHANGE },
                            modifier = Modifier.weight(1f),
                            colors = if (adjustMode == DateAdjustMode.SHIFT_CHANGE) selectedModeButtonColors else mutedButtonColors
                        ) { Text("근무변경") }
                    }

                    when (adjustMode) {
                        DateAdjustMode.VACATION -> {
                            if (anyVacationApplied && !allVacationApplied) {
                                Card(modifier = Modifier.fillMaxWidth(), colors = softPanelColors) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("일부 알람만 휴가 처리됨", color = MaterialTheme.colorScheme.primary)
                                        Text("적용 ${vacationAppliedCount}개 / 미적용 ${vacationExcludedAlarms.size}개")
                                        Text("미적용 알람")
                                        if (vacationExcludedPreview.isEmpty()) {
                                            Text("- 없음")
                                        } else {
                                            vacationExcludedPreview.forEach { line -> Text("- $line") }
                                        }
                                        if (vacationExcludedAlarms.size > vacationExcludedPreview.size) {
                                            Text("외 ${vacationExcludedAlarms.size - vacationExcludedPreview.size}개")
                                        }
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = { onSetVacationDate(chosenDate) },
                                    enabled = alarms.isNotEmpty() && !allVacationApplied,
                                    modifier = Modifier.weight(1f),
                                    colors = mutedButtonColors
                                ) {
                                    Text("휴가 처리(1일)")
                                }
                                Button(
                                    onClick = { onClearVacationDate(chosenDate) },
                                    enabled = alarms.isNotEmpty() && anyVacationApplied,
                                    modifier = Modifier.weight(1f),
                                    colors = mutedButtonColors
                                ) {
                                    Text("휴가 해제(1일)")
                                }
                            }

                            Text("기간 휴가", style = MaterialTheme.typography.titleSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                DatePickerButton(label = "시작", date = vacationStart, onDatePicked = { vacationStart = it })
                                DatePickerButton(label = "종료", date = vacationEnd, onDatePicked = { vacationEnd = it })
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = { onSetVacationRange(vacationStart, vacationEnd) },
                                    enabled = alarms.isNotEmpty(),
                                    modifier = Modifier.weight(1f),
                                    colors = mutedButtonColors
                                ) {
                                    Text("기간 적용")
                                }
                                Button(
                                    onClick = { onClearVacationRange(vacationStart, vacationEnd) },
                                    enabled = alarms.isNotEmpty(),
                                    modifier = Modifier.weight(1f),
                                    colors = mutedButtonColors
                                ) {
                                    Text("기간 해제")
                                }
                            }
                        }

                        DateAdjustMode.SKIP -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = { onSetSkipDateForIds(chosenDate, selectedIds) },
                                    enabled = selectedIds.isNotEmpty() && !allSkipApplied,
                                    modifier = Modifier.weight(1f),
                                    colors = mutedButtonColors
                                ) {
                                    Text("해당 알람 스킵")
                                }
                                Button(
                                    onClick = { onClearSkipDateForIds(chosenDate, selectedIds) },
                                    enabled = selectedIds.isNotEmpty() && anySkipApplied,
                                    modifier = Modifier.weight(1f),
                                    colors = mutedButtonColors
                                ) {
                                    Text("스킵 해제")
                                }
                            }
                        }

                        DateAdjustMode.SHIFT_CHANGE -> {
                            if (shiftTypeOptions.isEmpty()) {
                                Text("근무 유형을 찾을 수 없습니다. 패턴에서 유형을 먼저 설정하세요.")
                            } else {
                                Text("변경할 근무 유형")
                                shiftTypeOptions.chunked(3).forEach { rowTypes ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        rowTypes.forEach { type ->
                                            val selected = selectedShiftTypeResolved == type
                                            Button(
                                                onClick = { selectedShiftType = type },
                                                modifier = Modifier.weight(1f),
                                                colors = if (selected) selectedModeButtonColors else mutedButtonColors
                                            ) {
                                                Text(type)
                                            }
                                        }
                                        repeat(3 - rowTypes.size) {
                                            Box(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                                Button(
                                    onClick = { onApplyShiftChange(chosenDate, selectedShiftTypeResolved ?: return@Button) },
                                    enabled = selectedShiftTypeResolved != null && alarms.isNotEmpty(),
                                    colors = mutedButtonColors,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("선택 근무로 변경")
                                }
                                Text("선택한 날짜만 해당 근무 시간으로 알람이 바뀝니다.")
                            }
                        }
                    }
                    }
                }
            }
        }

        if (chosenDate != null && showDateAdjustControls) {
            Card(modifier = Modifier.fillMaxWidth(), colors = softPanelColors) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("선택 날짜 알람 (${alarmsForSelectedDate.size}개)", style = MaterialTheme.typography.titleSmall)
                    if (alarmsForSelectedDate.isEmpty()) {
                        Text("이 날짜에는 울릴 알람이 없습니다.")
                    } else {
                        alarmsForSelectedDate.forEach { alarm ->
                            val label = alarm.label.ifBlank { "이름 없음" }
                            Text(String.format("%02d:%02d  %s", alarm.hour, alarm.minute, label))
                        }
                    }
                }
            }
        }

    }
}

@Composable
private fun ShiftLegendChip(label: String, bg: Color, fg: Color) {
    Box(
        modifier = Modifier
            .background(bg, shape = RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(label, color = fg, style = MaterialTheme.typography.labelMedium)
    }
}

private fun inferShiftBadgeForDate(date: LocalDate, alarms: List<AlarmRule>): String {
    val dayTags = alarms.asSequence()
        .filter { AlarmTimeCalculator.isScheduledOnDate(it, date) }
        .map { inferShiftTagFromLabel(it.label) }
        .toSet()

    if (dayTags.isEmpty()) return "휴"

    return when {
        "야" in dayTags && "당" in dayTags -> "야/당"
        "주" in dayTags && "야" in dayTags -> "주/야"
        "주" in dayTags && "당" in dayTags -> "주/당"
        "당" in dayTags -> "당"
        "야" in dayTags -> "야"
        "주" in dayTags -> "주"
        "휴" in dayTags -> "휴"
        "비" in dayTags -> "비"
        else -> "근"
    }
}

private fun inferShiftTagFromLabel(label: String): String {
    return shiftTypeToBadge(extractWorkTypeFromLabel(label))
}

