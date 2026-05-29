package com.example.shiftalarmmvp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
        SHIFT_BADGE_DAY -> Color(0xFFFFF0CB)
        SHIFT_BADGE_NIGHT -> Color(0xFFE4EAFE)
        SHIFT_BADGE_DUTY -> Color(0xFFD9F1EC)
        SHIFT_BADGE_OFF -> Color(0xFFF1F3F0)
        SHIFT_BADGE_REST -> Color(0xFFF7F4EC)
        SHIFT_BADGE_DAY_NIGHT -> Color(0xFFECE8D9)
        SHIFT_BADGE_DAY_DUTY -> Color(0xFFE7F0DA)
        SHIFT_BADGE_NIGHT_DUTY -> Color(0xFFE0EEF2)
        else -> ShiftDesign.Mist
    }
}

fun shiftBadgeColor(badge: String): Color {
    return when (badge) {
        SHIFT_BADGE_DAY -> ShiftDesign.Day
        SHIFT_BADGE_NIGHT -> ShiftDesign.Night
        SHIFT_BADGE_DUTY -> ShiftDesign.Duty
        SHIFT_BADGE_OFF -> ShiftDesign.Rest
        SHIFT_BADGE_REST -> ShiftDesign.Rest
        SHIFT_BADGE_DAY_NIGHT -> Color(0xFF5A5360)
        SHIFT_BADGE_DAY_DUTY -> Color(0xFF547342)
        SHIFT_BADGE_NIGHT_DUTY -> Color(0xFF2D6574)
        else -> ShiftDesign.InkSoft
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
    val cellHeight = if (compact) 48.dp else 58.dp
    val cellShape = RoundedCornerShape(if (compact) 16.dp else 18.dp)

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
                        isToday -> baseBackground.copy(alpha = 0.78f)
                        else -> baseBackground.copy(alpha = 0.56f)
                    }
                    val borderWidth = when {
                        isSelected -> 2.dp
                        else -> 0.dp
                    }
                    val borderColor = when {
                        isSelected -> ShiftDesign.Navy
                        else -> Color.Transparent
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(cellHeight)
                            .background(backgroundColor, shape = cellShape)
                            .border(borderWidth, borderColor, cellShape)
                            .clickable(enabled = date != null) { if (date != null) onDateSelected(date) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (badge.isNotBlank()) {
                            ShiftTypeWatermark(
                                badge = badge,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(if (compact) 3.dp else 4.dp),
                                alpha = if (isSelected || isToday) 0.30f else 0.22f
                            )
                        }
                        if (date != null) {
                            Text(
                                text = date.dayOfMonth.toString(),
                                modifier = Modifier
                                    .background(
                                        color = if (isSelected) {
                                            ShiftDesign.Navy
                                        } else {
                                            Color.White.copy(alpha = 0.74f)
                                        },
                                        shape = RoundedCornerShape(999.dp)
                                    )
                                    .padding(horizontal = if (compact) 6.dp else 7.dp, vertical = 2.dp),
                                color = if (isSelected) Color.White else ShiftDesign.Ink,
                                fontWeight = FontWeight.ExtraBold,
                                style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge
                            )
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
