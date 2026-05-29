package com.example.shiftalarmmvp.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.shiftalarmmvp.R
import com.example.shiftalarmmvp.data.AlarmRule
import com.example.shiftalarmmvp.scheduler.AlarmTimeCalculator
import java.time.LocalDate
import java.time.LocalDateTime
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
    nextTrigger: LocalDateTime?,
    alarms: List<AlarmRule>,
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
    val resources = LocalContext.current.resources
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var vacationStart by remember { mutableStateOf(LocalDate.now()) }
    var vacationEnd by remember { mutableStateOf(LocalDate.now()) }
    var adjustMode by remember { mutableStateOf(DateAdjustMode.VACATION) }
    var selectedShiftType by remember { mutableStateOf<String?>(null) }
    val isCompactLayout = LocalConfiguration.current.screenWidthDp <= 380

    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1_000)
        }
    }

    val today = now.toLocalDate()
    val monthFormatter = DateTimeFormatter.ofPattern(stringResource(R.string.home_month_format))
    val alarmsForToday = alarms.filter { AlarmTimeCalculator.isScheduledOnDate(it, today) }
        .sortedWith(compareBy<AlarmRule> { it.hour }.thenBy { it.minute }.thenBy { it.label })
    val todayBadge = inferShiftBadgeForDate(today, alarms)
    val nextTriggerText = nextTrigger?.format(DateTimeFormatter.ofPattern("MM-dd HH:mm")) ?: stringResource(R.string.home_next_trigger_empty)
    val remainingText = nextTrigger?.let { formatTimeUntil(resources, it, now) } ?: "-"
    val unnamedAlarmText = stringResource(R.string.home_alarm_name_empty)
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
        val name = alarm.label.ifBlank { unnamedAlarmText }
        String.format("%02d:%02d %s", alarm.hour, alarm.minute, name)
    }
    val shiftTypeOptions = alarms.map { extractWorkTypeFromLabel(it.label) }.filter { it.isNotBlank() }.distinct()
    val selectedShiftTypeResolved = when {
        shiftTypeOptions.isEmpty() -> null
        selectedShiftType in shiftTypeOptions -> selectedShiftType
        else -> shiftTypeOptions.first()
    }

    val panelColors = CardDefaults.cardColors(
        containerColor = ShiftDesign.Paper
    )
    val softPanelColors = CardDefaults.cardColors(
        containerColor = ShiftDesign.Mist
    )
    val mutedButtonColors = neutralActionButtonColors()
    val selectedModeButtonColors = primaryActionButtonColors()
    val secondaryButtonColors = secondaryActionButtonColors()

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
                        Text(stringResource(R.string.home_undo))
                    }
                }
            }
        }

        HomeHeroCard(
            badge = todayBadge,
            alarmCount = alarmsForToday.size,
            nextTriggerText = nextTriggerText,
            remainingText = remainingText,
            compact = isCompactLayout
        )

        ShiftLegendRow(compact = isCompactLayout)

        Card(
            modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ShiftDesign.Line, MaterialTheme.shapes.large),
            colors = panelColors,
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = month.format(monthFormatter),
                        style = if (isCompactLayout) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CalendarStepButton(
                            symbol = "<",
                            contentDescription = stringResource(R.string.home_previous),
                            onClick = { month = month.minusMonths(1) }
                        )
                        CalendarStepButton(
                            symbol = ">",
                            contentDescription = stringResource(R.string.home_next),
                            onClick = { month = month.plusMonths(1) }
                        )
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
                    },
                    badgeForDate = { date -> inferShiftBadgeForDate(date, alarms) },
                    compact = isCompactLayout
                )

                if (chosenDate == null) {
                    Text(stringResource(R.string.home_select_date_hint))
                } else {
                    val tomorrow = today.plusDays(1)
                    val selectedDateHeadline = chosenDate.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일"))
                    SelectedDateOverviewCard(
                        dateText = selectedDateHeadline,
                        badge = selectedBadge,
                        isToday = chosenDate == today,
                        isTomorrow = chosenDate == tomorrow,
                        onSelectToday = {
                            selectedDate = today
                            vacationStart = today
                            vacationEnd = today
                        },
                        onSelectTomorrow = {
                            selectedDate = tomorrow
                            vacationStart = tomorrow
                            vacationEnd = tomorrow
                        }
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        HomeSelectionChip(
                            label = stringResource(R.string.home_mode_vacation),
                            selected = adjustMode == DateAdjustMode.VACATION,
                            modifier = Modifier.weight(1f),
                            onClick = { adjustMode = DateAdjustMode.VACATION }
                        )
                        HomeSelectionChip(
                            label = stringResource(R.string.home_mode_skip),
                            selected = adjustMode == DateAdjustMode.SKIP,
                            modifier = Modifier.weight(1f),
                            onClick = { adjustMode = DateAdjustMode.SKIP }
                        )
                        HomeSelectionChip(
                            label = stringResource(R.string.home_mode_shift_change),
                            selected = adjustMode == DateAdjustMode.SHIFT_CHANGE,
                            modifier = Modifier.weight(1f),
                            onClick = { adjustMode = DateAdjustMode.SHIFT_CHANGE }
                        )
                    }
                }
            }
        }

        if (chosenDate != null) {
            Card(
                modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ShiftDesign.Line, MaterialTheme.shapes.large),
        colors = panelColors,
        shape = MaterialTheme.shapes.large
    ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = when (adjustMode) {
                            DateAdjustMode.VACATION -> stringResource(R.string.home_vacation_range)
                            DateAdjustMode.SKIP -> stringResource(R.string.home_mode_skip)
                            DateAdjustMode.SHIFT_CHANGE -> stringResource(R.string.home_shift_type_change_title)
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    when (adjustMode) {
                        DateAdjustMode.VACATION -> {
                            val quickVacationClear = allVacationApplied
                            Button(
                                onClick = {
                                    if (quickVacationClear) onClearVacationDate(chosenDate) else onSetVacationDate(chosenDate)
                                },
                                enabled = alarms.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth(),
                                colors = if (quickVacationClear) secondaryButtonColors else selectedModeButtonColors
                            ) {
                                Text(
                                    if (quickVacationClear) {
                                        stringResource(R.string.home_quick_vacation_clear, vacationAppliedCount, alarms.size)
                                    } else {
                                        stringResource(R.string.home_quick_vacation_apply, vacationAppliedCount, alarms.size)
                                    }
                                )
                            }

                            if (anyVacationApplied && !allVacationApplied) {
                                Card(modifier = Modifier.fillMaxWidth(), colors = softPanelColors) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(stringResource(R.string.home_partial_vacation_title), color = MaterialTheme.colorScheme.primary)
                                        Text(stringResource(R.string.home_partial_vacation_status, vacationAppliedCount, vacationExcludedAlarms.size))
                                        Text(stringResource(R.string.home_partial_vacation_list_title))
                                        if (vacationExcludedPreview.isEmpty()) {
                                            Text(stringResource(R.string.home_none_with_dash))
                                        } else {
                                            vacationExcludedPreview.forEach { line -> Text("- $line") }
                                        }
                                        if (vacationExcludedAlarms.size > vacationExcludedPreview.size) {
                                            Text(stringResource(R.string.home_more_count, vacationExcludedAlarms.size - vacationExcludedPreview.size))
                                        }
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                SelectedDatePickerButton(
                                    label = stringResource(R.string.home_date_start),
                                    date = vacationStart,
                                    modifier = Modifier.weight(1f),
                                    onDatePicked = { vacationStart = it }
                                )
                                SelectedDatePickerButton(
                                    label = stringResource(R.string.home_date_end),
                                    date = vacationEnd,
                                    modifier = Modifier.weight(1f),
                                    onDatePicked = { vacationEnd = it }
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = { onSetVacationRange(vacationStart, vacationEnd) },
                                    enabled = alarms.isNotEmpty(),
                                    modifier = Modifier.weight(1f),
                                    colors = mutedButtonColors
                                ) {
                                    Text(stringResource(R.string.home_vacation_apply_range))
                                }
                                Button(
                                    onClick = { onClearVacationRange(vacationStart, vacationEnd) },
                                    enabled = alarms.isNotEmpty(),
                                    modifier = Modifier.weight(1f),
                                    colors = mutedButtonColors
                                ) {
                                    Text(stringResource(R.string.home_vacation_clear_range))
                                }
                            }
                        }

                        DateAdjustMode.SKIP -> {
                            val quickSkipClear = allSkipApplied
                            Text(
                                text = stringResource(R.string.home_selected_date_alarm_count, alarmsForSelectedDate.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = {
                                        if (quickSkipClear) onClearSkipDateForIds(chosenDate, selectedIds)
                                        else onSetSkipDateForIds(chosenDate, selectedIds)
                                    },
                                    enabled = selectedIds.isNotEmpty(),
                                    modifier = Modifier.weight(1f),
                                    colors = if (quickSkipClear) secondaryButtonColors else selectedModeButtonColors
                                ) {
                                    Text(
                                        if (quickSkipClear) {
                                            stringResource(R.string.home_quick_skip_clear, skipAppliedCount, selectedIds.size)
                                        } else {
                                            stringResource(R.string.home_quick_skip_apply, skipAppliedCount, selectedIds.size)
                                        }
                                    )
                                }
                                Button(
                                    onClick = { onClearSkipDateForIds(chosenDate, selectedIds) },
                                    enabled = selectedIds.isNotEmpty() && anySkipApplied,
                                    modifier = Modifier.weight(1f),
                                    colors = mutedButtonColors
                                ) {
                                    Text(stringResource(R.string.home_skip_clear_label))
                                }
                            }
                        }

                        DateAdjustMode.SHIFT_CHANGE -> {
                            if (shiftTypeOptions.isEmpty()) {
                                Text(stringResource(R.string.home_shift_type_missing))
                            } else {
                                Text(
                                    text = stringResource(R.string.home_apply_selected_shift_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                shiftTypeOptions.chunked(3).forEach { rowTypes ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        rowTypes.forEach { type ->
                                            HomeSelectionChip(
                                                label = type,
                                                selected = selectedShiftTypeResolved == type,
                                                modifier = Modifier.weight(1f),
                                                onClick = { selectedShiftType = type }
                                            )
                                        }
                                        repeat(3 - rowTypes.size) {
                                            Box(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                                Button(
                                    onClick = { onApplyShiftChange(chosenDate, selectedShiftTypeResolved ?: return@Button) },
                                    enabled = selectedShiftTypeResolved != null && alarms.isNotEmpty(),
                                    colors = selectedModeButtonColors,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.home_apply_selected_shift))
                                }
                            }
                        }
                    }
                }
            }

            SelectedDateAlarmListCard(
                alarmsForSelectedDate = alarmsForSelectedDate,
                unnamedAlarmText = unnamedAlarmText
            )
        }

    }
}

@Composable
private fun SelectedDateOverviewCard(
    dateText: String,
    badge: String,
    isToday: Boolean,
    isTomorrow: Boolean,
    onSelectToday: () -> Unit,
    onSelectTomorrow: () -> Unit
) {
    val resources = LocalContext.current.resources
    val badgeLabel = shiftBadgeLabel(resources, badge)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ShiftDesign.Line, MaterialTheme.shapes.large),
        colors = CardDefaults.cardColors(containerColor = ShiftDesign.Paper),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.home_one_tap_exception),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    ShiftTypeIllustration(
                        badge = badge,
                        modifier = Modifier.size(54.dp),
                        selected = true
                    )
                    Text(
                        text = badgeLabel,
                        modifier = Modifier
                            .background(shiftBadgeBackgroundColor(badge), shape = RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        color = shiftBadgeColor(badge),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HomeSelectionChip(
                    label = stringResource(R.string.home_today),
                    selected = isToday,
                    modifier = Modifier.weight(1f),
                    onClick = onSelectToday
                )
                HomeSelectionChip(
                    label = stringResource(R.string.home_tomorrow),
                    selected = isTomorrow,
                    modifier = Modifier.weight(1f),
                    onClick = onSelectTomorrow
                )
            }
        }
    }
}

@Composable
private fun HomeSelectionChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .border(
                width = 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else Color(0xFFD5DDE9),
                shape = shape
            )
            .background(
                color = if (selected) ShiftDesign.Navy else ShiftDesign.Paper,
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else ShiftDesign.InkSoft,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SelectedDatePickerButton(
    label: String,
    date: LocalDate,
    modifier: Modifier = Modifier,
    onDatePicked: (LocalDate) -> Unit
) {
    val context = LocalContext.current
    Button(
        onClick = {
            DatePickerDialog(
                context,
                { _, y, m, d -> onDatePicked(LocalDate.of(y, m + 1, d)) },
                date.year,
                date.monthValue - 1,
                date.dayOfMonth
            ).show()
        },
        modifier = modifier,
        colors = neutralActionButtonColors()
    ) {
        Text(stringResource(R.string.page_components_date_picker_button_format, label, date))
    }
}

@Composable
private fun SelectedDateAlarmListCard(
    alarmsForSelectedDate: List<AlarmRule>,
    unnamedAlarmText: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ShiftDesign.Line, MaterialTheme.shapes.large),
        colors = CardDefaults.cardColors(containerColor = ShiftDesign.Mist),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.home_selected_date_alarm_count, alarmsForSelectedDate.size),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (alarmsForSelectedDate.isEmpty()) {
                Text(
                    text = stringResource(R.string.home_selected_date_alarm_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                alarmsForSelectedDate.forEach { alarm ->
                    val label = alarm.label.ifBlank { unnamedAlarmText }
                    val badge = shiftTypeToBadge(extractWorkTypeFromLabel(label))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = String.format("%02d:%02d", alarm.hour, alarm.minute),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        ShiftTypeIllustration(
                            badge = badge,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }
            }
        }
    }
}
@Composable
private fun ShiftLegendRow(compact: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShiftLegendChip(
            label = stringResource(R.string.home_legend_day),
            badge = SHIFT_BADGE_DAY,
            bg = Color(0xFFFFF0CB),
            fg = ShiftDesign.Day,
            modifier = Modifier.weight(1f)
        )
        ShiftLegendChip(
            label = stringResource(R.string.home_legend_night),
            badge = SHIFT_BADGE_NIGHT,
            bg = Color(0xFFE4EAFE),
            fg = ShiftDesign.Night,
            modifier = Modifier.weight(1f)
        )
        ShiftLegendChip(
            label = stringResource(R.string.home_legend_duty),
            badge = SHIFT_BADGE_DUTY,
            bg = Color(0xFFD9F1EC),
            fg = ShiftDesign.Duty,
            modifier = Modifier.weight(1f)
        )
        ShiftLegendChip(
            label = stringResource(R.string.home_legend_rest),
            badge = SHIFT_BADGE_REST,
            bg = Color(0xFFF7F4EC),
            fg = ShiftDesign.Rest,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ShiftLegendChip(
    label: String,
    badge: String,
    bg: Color,
    fg: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(bg, shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShiftTypeIllustration(
            badge = badge,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            modifier = Modifier.padding(start = 5.dp),
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HomeHeroCard(
    badge: String,
    alarmCount: Int,
    nextTriggerText: String,
    remainingText: String,
    compact: Boolean
) {
    val plainBadgeLabel = when (badge) {
        SHIFT_BADGE_DAY -> stringResource(R.string.home_legend_day)
        SHIFT_BADGE_NIGHT -> stringResource(R.string.home_legend_night)
        SHIFT_BADGE_DUTY -> stringResource(R.string.home_legend_duty)
        SHIFT_BADGE_OFF -> stringResource(R.string.home_legend_off)
        SHIFT_BADGE_REST -> stringResource(R.string.home_legend_rest)
        SHIFT_BADGE_DAY_NIGHT -> stringResource(R.string.home_legend_day) + "/" + stringResource(R.string.home_legend_night)
        SHIFT_BADGE_DAY_DUTY -> stringResource(R.string.home_legend_day) + "/" + stringResource(R.string.home_legend_duty)
        SHIFT_BADGE_NIGHT_DUTY -> stringResource(R.string.home_legend_night) + "/" + stringResource(R.string.home_legend_duty)
        else -> "근무"
    }
    val todayAlarmCountText = if (alarmCount == 0) {
        stringResource(R.string.home_today_alarm_empty)
    } else {
        stringResource(R.string.home_today_alarm_count, alarmCount)
    }
    val badgeAccentColor = shiftBadgeColor(badge)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.38f), MaterialTheme.shapes.large),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .background(
                    brush = shiftHeroBrush(),
                    shape = MaterialTheme.shapes.large
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_today_work_title),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.76f)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (compact) 8.dp else 9.dp)
                            .background(badgeAccentColor, CircleShape)
                    )
                    Text(
                        text = plainBadgeLabel,
                        style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = todayAlarmCountText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            ShiftTypeIllustration(
                badge = badge,
                modifier = Modifier.size(if (compact) 54.dp else 66.dp),
                selected = true
            )
            Box(
                modifier = Modifier
                    .size(1.dp, if (compact) 34.dp else 38.dp)
                    .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(999.dp))
            )
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_next_alarm_title),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.74f)
                )
                Text(
                    text = remainingText,
                    style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = nextTriggerText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.78f),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun CalendarStepButton(
    symbol: String,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .semantics { this.contentDescription = contentDescription }
            .background(ShiftDesign.Mist.copy(alpha = 0.92f), shape = MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun inferShiftBadgeForDate(date: LocalDate, alarms: List<AlarmRule>): String {
    val dayTags = alarms.asSequence()
        .filter { AlarmTimeCalculator.isScheduledOnDate(it, date) }
        .map { inferShiftTagFromLabel(it.label) }
        .toSet()

    if (dayTags.isEmpty()) return SHIFT_BADGE_REST

    return when {
        SHIFT_BADGE_NIGHT in dayTags && SHIFT_BADGE_DUTY in dayTags -> SHIFT_BADGE_NIGHT_DUTY
        SHIFT_BADGE_DAY in dayTags && SHIFT_BADGE_NIGHT in dayTags -> SHIFT_BADGE_DAY_NIGHT
        SHIFT_BADGE_DAY in dayTags && SHIFT_BADGE_DUTY in dayTags -> SHIFT_BADGE_DAY_DUTY
        SHIFT_BADGE_DUTY in dayTags -> SHIFT_BADGE_DUTY
        SHIFT_BADGE_NIGHT in dayTags -> SHIFT_BADGE_NIGHT
        SHIFT_BADGE_DAY in dayTags -> SHIFT_BADGE_DAY
        SHIFT_BADGE_REST in dayTags -> SHIFT_BADGE_REST
        SHIFT_BADGE_OFF in dayTags -> SHIFT_BADGE_OFF
        else -> SHIFT_BADGE_WORK
    }
}

private fun inferShiftTagFromLabel(label: String): String {
    return shiftTypeToBadge(extractWorkTypeFromLabel(label))
}





