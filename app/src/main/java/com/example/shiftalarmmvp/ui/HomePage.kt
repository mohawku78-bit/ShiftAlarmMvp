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
    onUndoLastChange: () -> Unit,
    recentChangeLogs: List<String>
) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var vacationStart by remember { mutableStateOf(LocalDate.now()) }
    var vacationEnd by remember { mutableStateOf(LocalDate.now()) }
    var adjustMode by remember { mutableStateOf(DateAdjustMode.VACATION) }
    var selectedShiftType by remember { mutableStateOf<String?>(null) }

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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ShiftLegendChip("주간", Color(0xFFD9E8FA), Color(0xFF1E4E8C))
                    ShiftLegendChip("야간", Color(0xFFFFE3C8), Color(0xFF9A5400))
                    ShiftLegendChip("비번", Color(0xFFE3E8EE), Color(0xFF4F6375))
                    ShiftLegendChip("휴무", Color(0xFFEEF1F4), Color(0xFF5B6670))
                }

                CalendarMonthGrid(
                    month = month,
                    today = today,
                    selectedDate = selectedDate,
                    onDateSelected = {
                        selectedDate = it
                        vacationStart = it
                        vacationEnd = it
                    },
                    badgeForDate = { date -> inferShiftBadgeForDate(date, alarms) }
                )

                if (chosenDate == null) {
                    Text("날짜를 선택하면 예외 처리를 진행할 수 있습니다.")
                } else {
                    Text(
                        "선택 날짜: ${chosenDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))} / 근무: ${badgeLabel(selectedBadge)}",
                        style = MaterialTheme.typography.bodyMedium
                    )

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
                                Text("일부 알람만 휴가 처리됨", color = MaterialTheme.colorScheme.primary)
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

        Card(modifier = Modifier.fillMaxWidth(), colors = softPanelColors) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("선택 날짜 알람 (${alarmsForSelectedDate.size}개)", style = MaterialTheme.typography.titleSmall)
                if (chosenDate == null) {
                    Text("날짜를 먼저 선택하세요.")
                } else if (alarmsForSelectedDate.isEmpty()) {
                    Text("이 날짜에는 울릴 알람이 없습니다.")
                } else {
                    alarmsForSelectedDate.forEach { alarm ->
                        val label = alarm.label.ifBlank { "이름 없음" }
                        Text(String.format("%02d:%02d  %s", alarm.hour, alarm.minute, label))
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), colors = softPanelColors) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("변경 로그(최근 5건)", style = MaterialTheme.typography.titleSmall)
                if (recentChangeLogs.isEmpty()) {
                    Text("변경 기록이 없습니다.")
                } else {
                    recentChangeLogs.forEach { line -> Text(line) }
                }
            }
        }

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
                if (selectedLabel.isNotBlank()) {
                    Text("알람 이름: $selectedLabel", color = Color.White.copy(alpha = 0.94f))
                }
                Text(
                    "기본 시간: ${selectedTime.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                    color = Color.White.copy(alpha = 0.94f)
                )
                if (rotationPreview.isNotEmpty()) {
                    Text("오늘 근무: ${rotationPreview.first()}", color = Color.White.copy(alpha = 0.94f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = onOpenManage, modifier = Modifier.weight(1f), colors = summaryButtonColors) {
                        Text("관리 화면")
                    }
                    Button(onClick = onReconfigurePattern, modifier = Modifier.weight(1f), colors = summaryButtonColors) {
                        Text("패턴 다시 설정")
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

@Composable
private fun CalendarMonthGrid(
    month: YearMonth,
    today: LocalDate,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    badgeForDate: (LocalDate) -> String
) {
    val dayLabels = listOf("월", "화", "수", "목", "금", "토", "일")
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

    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            dayLabels.forEach { label ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(label, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        dates.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { date ->
                    val isToday = date == today
                    val isSelected = date != null && date == selectedDate
                    val badge = date?.let { badgeForDate(it) } ?: ""
                    val baseBackground = if (date != null) {
                        badgeBackgroundColor(badge)
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    }
                    val backgroundColor = when {
                        isSelected -> baseBackground.copy(alpha = 0.98f)
                        isToday -> baseBackground.copy(alpha = 0.9f)
                        else -> baseBackground
                    }
                    val borderWidth = when {
                        isSelected -> 1.8.dp
                        isToday -> 1.2.dp
                        else -> 0.dp
                    }
                    val borderColor = when {
                        isSelected -> badgeColor(badge).copy(alpha = 0.6f)
                        isToday -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        else -> Color.Transparent
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(backgroundColor, shape = RoundedCornerShape(8.dp))
                            .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
                            .clickable(enabled = date != null) { if (date != null) onDateSelected(date) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                date?.dayOfMonth?.toString() ?: "",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                            if (badge.isNotBlank()) {
                                Text(
                                    badgeLabel(badge),
                                    color = badgeColor(badge),
                                    style = MaterialTheme.typography.labelMedium,
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

private fun inferShiftBadgeForDate(date: LocalDate, alarms: List<AlarmRule>): String {
    val dayTags = alarms.asSequence()
        .filter { AlarmTimeCalculator.isScheduledOnDate(it, date) }
        .map { inferShiftTagFromLabel(it.label) }
        .toSet()

    if (dayTags.isEmpty()) return "휴"

    return when {
        "주" in dayTags && "야" in dayTags -> "주/야"
        "야" in dayTags -> "야"
        "주" in dayTags -> "주"
        "휴" in dayTags -> "휴"
        "비" in dayTags -> "비"
        else -> "근"
    }
}

private fun inferShiftTagFromLabel(label: String): String {
    return when (extractWorkTypeFromLabel(label)) {
        "주간" -> "주"
        "야간", "당직" -> "야"
        "휴가", "휴무", "휴일" -> "휴"
        "비번" -> "비"
        else -> "근"
    }
}

private fun badgeLabel(badge: String): String {
    return when (badge) {
        "주" -> "▲ 주"
        "야" -> "■ 야"
        "비" -> "● 비"
        "휴" -> "○ 휴"
        "석" -> "◆ 석"
        "주/야" -> "▣ 주/야"
        else -> "• 근"
    }
}

private fun badgeBackgroundColor(badge: String): Color {
    return when (badge) {
        "주" -> Color(0xFFD9E8FA)
        "야" -> Color(0xFFFFE3C8)
        "비" -> Color(0xFFE3E8EE)
        "휴" -> Color(0xFFEEF1F4)
        "석" -> Color(0xFFE9DCF7)
        "주/야" -> Color(0xFFE8E6F8)
        else -> Color(0xFFE2F0EA)
    }
}

@Composable
private fun badgeColor(badge: String): Color {
    return when (badge) {
        "주" -> Color(0xFF1E4E8C)
        "야" -> Color(0xFF9A5400)
        "비" -> Color(0xFF4F6375)
        "휴" -> Color(0xFF5B6670)
        "석" -> Color(0xFF68478F)
        "주/야" -> Color(0xFF5A4D99)
        else -> Color(0xFF4D6B5C)
    }
}
