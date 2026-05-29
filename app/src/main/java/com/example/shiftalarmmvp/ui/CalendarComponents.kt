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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.shiftalarmmvp.R
import java.time.LocalDate
import java.time.YearMonth

const val SHIFT_BADGE_DAY = "day"
const val SHIFT_BADGE_NIGHT = "night"
const val SHIFT_BADGE_DUTY = "duty"
const val SHIFT_BADGE_OFF = "off"
const val SHIFT_BADGE_REST = "rest"
const val SHIFT_BADGE_DAY_NIGHT = "day_night"
const val SHIFT_BADGE_DAY_DUTY = "day_duty"
const val SHIFT_BADGE_NIGHT_DUTY = "night_duty"
const val SHIFT_BADGE_WORK = "work"

fun shiftTypeToBadge(type: String): String {
    return when (normalizeWorkType(type)) {
        WORK_TYPE_DAY -> SHIFT_BADGE_DAY
        WORK_TYPE_NIGHT -> SHIFT_BADGE_NIGHT
        WORK_TYPE_DUTY -> SHIFT_BADGE_DUTY
        WORK_TYPE_OFF -> SHIFT_BADGE_OFF
        WORK_TYPE_REST, WORK_TYPE_HOLIDAY, WORK_TYPE_VACATION -> SHIFT_BADGE_REST
        else -> SHIFT_BADGE_WORK
    }
}

fun shiftBadgeLabel(resources: android.content.res.Resources, badge: String): String {
    return when (badge) {
        SHIFT_BADGE_DAY -> resources.getString(R.string.shift_preview_badge_day)
        SHIFT_BADGE_NIGHT -> resources.getString(R.string.shift_preview_badge_night)
        SHIFT_BADGE_DUTY -> resources.getString(R.string.shift_preview_badge_duty)
        SHIFT_BADGE_OFF -> resources.getString(R.string.shift_preview_badge_off)
        SHIFT_BADGE_REST -> resources.getString(R.string.shift_preview_badge_rest)
        SHIFT_BADGE_DAY_NIGHT -> resources.getString(R.string.calendar_components_badge_day_night)
        SHIFT_BADGE_DAY_DUTY -> resources.getString(R.string.calendar_components_badge_day_duty)
        SHIFT_BADGE_NIGHT_DUTY -> resources.getString(R.string.calendar_components_badge_night_duty)
        else -> resources.getString(R.string.shift_preview_badge_work)
    }
}

fun shiftBadgeBackgroundColor(badge: String): Color {
    return when (badge) {
        SHIFT_BADGE_DAY -> Color(0xFFFFF4D8)
        SHIFT_BADGE_NIGHT -> Color(0xFFE8EDFF)
        SHIFT_BADGE_DUTY -> Color(0xFFDFF4F1)
        SHIFT_BADGE_OFF -> Color(0xFFF2F4F7)
        SHIFT_BADGE_REST -> Color(0xFFF7F8FA)
        SHIFT_BADGE_DAY_NIGHT -> Color(0xFFE9F0F4)
        SHIFT_BADGE_DAY_DUTY -> Color(0xFFE5F3EA)
        SHIFT_BADGE_NIGHT_DUTY -> Color(0xFFE5F0F7)
        else -> Color(0xFFF2F4F7)
    }
}

fun shiftBadgeColor(badge: String): Color {
    return when (badge) {
        SHIFT_BADGE_DAY -> Color(0xFF946200)
        SHIFT_BADGE_NIGHT -> Color(0xFF3346A8)
        SHIFT_BADGE_DUTY -> Color(0xFF0F766E)
        SHIFT_BADGE_OFF -> Color(0xFF747B86)
        SHIFT_BADGE_REST -> Color(0xFF8A8F98)
        SHIFT_BADGE_DAY_NIGHT -> Color(0xFF34547F)
        SHIFT_BADGE_DAY_DUTY -> Color(0xFF24705E)
        SHIFT_BADGE_NIGHT_DUTY -> Color(0xFF225C7E)
        else -> Color(0xFF747B86)
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
    val resources = LocalContext.current.resources
    val dayLabels = listOf(
        stringResource(R.string.shift_day_mon),
        stringResource(R.string.shift_day_tue),
        stringResource(R.string.shift_day_wed),
        stringResource(R.string.shift_day_thu),
        stringResource(R.string.shift_day_fri),
        stringResource(R.string.shift_day_sat),
        stringResource(R.string.shift_day_sun)
    )
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

    val gridSpacing = if (compact) 4.dp else 6.dp
    val rowSpacing = if (compact) 6.dp else 8.dp
    val dayLabelStyle = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium
    val badgeStyle = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium
    val cellVerticalPadding = if (compact) 8.dp else 10.dp
    val cellShape = RoundedCornerShape(if (compact) 14.dp else 16.dp)

    Column(verticalArrangement = Arrangement.spacedBy(rowSpacing), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gridSpacing)) {
            dayLabels.forEach { label ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(label, style = dayLabelStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Color.Transparent
                    }
                    val backgroundColor = when {
                        date == null -> Color.Transparent
                        isSelected -> baseBackground.copy(alpha = 1.0f)
                        isToday -> baseBackground.copy(alpha = 0.9f)
                        else -> baseBackground.copy(alpha = 0.82f)
                    }
                    val borderWidth = when {
                        isSelected -> 2.dp
                        else -> 0.dp
                    }
                    val borderColor = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        else -> Color.Transparent
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(backgroundColor, shape = cellShape)
                            .border(borderWidth, borderColor, cellShape)
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
                                    shiftBadgeLabel(resources, badge),
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

    val monthSpacing = if (compact) 5.dp else 6.dp

    monthList.forEach { month ->
        Column(verticalArrangement = Arrangement.spacedBy(monthSpacing), modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.shift_preview_month_format, month.year, month.monthValue),
                style = MaterialTheme.typography.titleSmall
            )
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
