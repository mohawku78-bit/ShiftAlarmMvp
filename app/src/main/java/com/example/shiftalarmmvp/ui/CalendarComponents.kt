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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth

fun shiftTypeToBadge(type: String): String {
    return when (normalizeWorkType(type)) {
        "주간" -> "주"
        "야간" -> "야"
        "당직" -> "당"
        "비번" -> "비"
        "휴무", "휴일", "휴가" -> "휴"
        else -> "근"
    }
}

fun shiftBadgeLabel(badge: String): String {
    return when (badge) {
        "주" -> "▲ 주"
        "야" -> "■ 야"
        "당" -> "◆ 당"
        "비" -> "● 비"
        "휴" -> "○ 휴"
        "주/야" -> "▣ 주/야"
        "주/당" -> "▣ 주/당"
        "야/당" -> "▣ 야/당"
        else -> "• 근"
    }
}

fun shiftBadgeBackgroundColor(badge: String): Color {
    return when (badge) {
        "주" -> Color(0xFFD9E8FA)
        "야" -> Color(0xFFFFE3C8)
        "당" -> Color(0xFFFFE9D6)
        "비" -> Color(0xFFE3E8EE)
        "휴" -> Color(0xFFEEF1F4)
        "주/야" -> Color(0xFFE8E6F8)
        "주/당" -> Color(0xFFE9ECFB)
        "야/당" -> Color(0xFFF7E7DB)
        else -> Color(0xFFE2F0EA)
    }
}

fun shiftBadgeColor(badge: String): Color {
    return when (badge) {
        "주" -> Color(0xFF1E4E8C)
        "야" -> Color(0xFF9A5400)
        "당" -> Color(0xFF8A3E00)
        "비" -> Color(0xFF4F6375)
        "휴" -> Color(0xFF5B6670)
        "주/야" -> Color(0xFF5A4D99)
        "주/당" -> Color(0xFF4A5EA8)
        "야/당" -> Color(0xFF8E4B16)
        else -> Color(0xFF4D6B5C)
    }
}

@Composable
fun ShiftCalendarMonthGrid(
    month: YearMonth,
    today: LocalDate,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    badgeForDate: (LocalDate) -> String,
    compact: Boolean
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

    val gridSpacing = if (compact) 3.dp else 4.dp
    val rowSpacing = if (compact) 5.dp else 6.dp
    val dayLabelStyle = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium
    val badgeStyle = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium
    val cellVerticalPadding = if (compact) 5.dp else 6.dp

    Column(verticalArrangement = Arrangement.spacedBy(rowSpacing), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gridSpacing)) {
            dayLabels.forEach { label ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(label, style = dayLabelStyle)
                }
            }
        }

        dates.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gridSpacing)) {
                week.forEach { date ->
                    val isToday = date == today
                    val isSelected = date != null && date == selectedDate
                    val badge = date?.let { badgeForDate(it) } ?: ""
                    val baseBackground = if (date != null) {
                        shiftBadgeBackgroundColor(badge)
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
                        isSelected -> shiftBadgeColor(badge).copy(alpha = 0.6f)
                        isToday -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        else -> Color.Transparent
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(backgroundColor, shape = RoundedCornerShape(8.dp))
                            .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
                            .clickable(enabled = date != null) { if (date != null) onDateSelected(date) }
                            .padding(vertical = cellVerticalPadding),
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
                                    shiftBadgeLabel(badge),
                                    color = shiftBadgeColor(badge),
                                    style = badgeStyle,
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

@Composable
fun ShiftPreviewCalendar(
    previewDays: List<Pair<LocalDate, String>>,
    compact: Boolean
) {
    if (previewDays.isEmpty()) {
        Text("미리보기 없음")
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

    val monthSpacing = if (compact) 5.dp else 6.dp

    monthList.forEach { month ->
        Column(verticalArrangement = Arrangement.spacedBy(monthSpacing), modifier = Modifier.fillMaxWidth()) {
            Text("${month.year}년 ${month.monthValue}월", style = MaterialTheme.typography.titleSmall)
            ShiftCalendarMonthGrid(
                month = month,
                today = LocalDate.MIN,
                selectedDate = null,
                onDateSelected = {},
                badgeForDate = { date -> previewMap[date]?.let(::shiftTypeToBadge).orEmpty() },
                compact = compact
            )
        }
    }
}
