package com.example.shiftalarmmvp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
    val resources = LocalContext.current.resources
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var vacationStart by remember { mutableStateOf(LocalDate.now()) }
    var vacationEnd by remember { mutableStateOf(LocalDate.now()) }
    var adjustMode by remember { mutableStateOf(DateAdjustMode.VACATION) }
    var selectedShiftType by remember { mutableStateOf<String?>(null) }
    var showDateAdjustControls by remember { mutableStateOf(false) }
    var showLegend by remember { mutableStateOf(false) }
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
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
    )
    val softPanelColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f)
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

        if (isCompactLayout) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HomeTodaySummaryCard(
                    badge = todayBadge,
                    alarmCount = alarmsForToday.size,
                    preview = buildHomeAlarmPreview(alarmsForToday)
                )
                HomeNextAlarmSummaryCard(
                    nextTriggerText = nextTriggerText,
                    remainingText = remainingText,
                    onOpenManage = onOpenManage,
                    onReconfigurePattern = onReconfigurePattern
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HomeTodaySummaryCard(
                    modifier = Modifier.weight(1f),
                    badge = todayBadge,
                    alarmCount = alarmsForToday.size,
                    preview = buildHomeAlarmPreview(alarmsForToday)
                )
                HomeNextAlarmSummaryCard(
                    modifier = Modifier.weight(1f),
                    nextTriggerText = nextTriggerText,
                    remainingText = remainingText,
                    onOpenManage = onOpenManage,
                    onReconfigurePattern = onReconfigurePattern
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), colors = panelColors) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { month = month.minusMonths(1) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        colors = mutedButtonColors
                    ) {
                        Text(stringResource(R.string.home_previous), maxLines = 1, softWrap = false)
                    }
                    Text(
                        text = month.format(monthFormatter),
                        modifier = Modifier.weight(1.4f),
                        style = if (isCompactLayout) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = { month = month.plusMonths(1) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        colors = mutedButtonColors
                    ) {
                        Text(stringResource(R.string.home_next), maxLines = 1, softWrap = false)
                    }
                }
                Button(
                    onClick = { showLegend = !showLegend },
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (showLegend) selectedModeButtonColors else mutedButtonColors
                ) {
                    Text(if (showLegend) stringResource(R.string.home_legend_hide) else stringResource(R.string.home_legend_show))
                }

                if (showLegend) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ShiftLegendChip(stringResource(R.string.home_legend_day), Color(0xFFD9E8FA), Color(0xFF1E4E8C))
                        ShiftLegendChip(stringResource(R.string.home_legend_night), Color(0xFFFFE3C8), Color(0xFF9A5400))
                        ShiftLegendChip(stringResource(R.string.home_legend_duty), Color(0xFFFFE9D6), Color(0xFF8A3E00))
                        ShiftLegendChip(stringResource(R.string.home_legend_off), Color(0xFFE3E8EE), Color(0xFF4F6375))
                        ShiftLegendChip(stringResource(R.string.home_legend_rest), Color(0xFFEEF1F4), Color(0xFF5B6670))
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
                    compact = isCompactLayout
                )

                if (chosenDate == null) {
                    Text(stringResource(R.string.home_select_date_hint))
                } else {
                    Text(
                        stringResource(R.string.home_selected_date_format, chosenDate.format(DateTimeFormatter.ISO_LOCAL_DATE), shiftBadgeLabel(resources, selectedBadge)),
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
                            Text(stringResource(R.string.home_today))
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
                            Text(stringResource(R.string.home_tomorrow))
                        }
                    }

                    Text(stringResource(R.string.home_one_tap_exception), style = MaterialTheme.typography.titleSmall)

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
                            Text(if (quickVacationClear) stringResource(R.string.home_quick_vacation_clear, vacationAppliedCount, alarms.size) else stringResource(R.string.home_quick_vacation_apply, vacationAppliedCount, alarms.size))
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
                            Text(if (quickSkipClear) stringResource(R.string.home_quick_skip_clear, skipAppliedCount, selectedIds.size) else stringResource(R.string.home_quick_skip_apply, skipAppliedCount, selectedIds.size))
                        }
                    }
                    Text(
                        stringResource(R.string.home_detail_exception_hint),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(
                        onClick = { showDateAdjustControls = !showDateAdjustControls },
                        modifier = Modifier.fillMaxWidth(),
                        colors = if (showDateAdjustControls) selectedModeButtonColors else mutedButtonColors
                    ) {
                        Text(if (showDateAdjustControls) stringResource(R.string.home_exception_close) else stringResource(R.string.home_exception_open))
                    }

                    if (showDateAdjustControls) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { adjustMode = DateAdjustMode.VACATION },
                            modifier = Modifier.weight(1f),
                            colors = if (adjustMode == DateAdjustMode.VACATION) selectedModeButtonColors else mutedButtonColors
                        ) { Text(stringResource(R.string.home_mode_vacation)) }
                        Button(
                            onClick = { adjustMode = DateAdjustMode.SKIP },
                            modifier = Modifier.weight(1f),
                            colors = if (adjustMode == DateAdjustMode.SKIP) selectedModeButtonColors else mutedButtonColors
                        ) { Text(stringResource(R.string.home_mode_skip)) }
                        Button(
                            onClick = { adjustMode = DateAdjustMode.SHIFT_CHANGE },
                            modifier = Modifier.weight(1f),
                            colors = if (adjustMode == DateAdjustMode.SHIFT_CHANGE) selectedModeButtonColors else mutedButtonColors
                        ) { Text(stringResource(R.string.home_mode_shift_change)) }
                    }

                    when (adjustMode) {
                        DateAdjustMode.VACATION -> {
                            if (anyVacationApplied && !allVacationApplied) {
                                Card(modifier = Modifier.fillMaxWidth(), colors = softPanelColors) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
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
                                Button(
                                    onClick = { onSetVacationDate(chosenDate) },
                                    enabled = alarms.isNotEmpty() && !allVacationApplied,
                                    modifier = Modifier.weight(1f),
                                    colors = mutedButtonColors
                                ) {
                                    Text(stringResource(R.string.home_vacation_apply_single))
                                }
                                Button(
                                    onClick = { onClearVacationDate(chosenDate) },
                                    enabled = alarms.isNotEmpty() && anyVacationApplied,
                                    modifier = Modifier.weight(1f),
                                    colors = mutedButtonColors
                                ) {
                                    Text(stringResource(R.string.home_vacation_clear_single))
                                }
                            }

                            Text(stringResource(R.string.home_vacation_range), style = MaterialTheme.typography.titleSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                DatePickerButton(label = stringResource(R.string.home_date_start), date = vacationStart, onDatePicked = { vacationStart = it })
                                DatePickerButton(label = stringResource(R.string.home_date_end), date = vacationEnd, onDatePicked = { vacationEnd = it })
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
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = { onSetSkipDateForIds(chosenDate, selectedIds) },
                                    enabled = selectedIds.isNotEmpty() && !allSkipApplied,
                                    modifier = Modifier.weight(1f),
                                    colors = mutedButtonColors
                                ) {
                                    Text(stringResource(R.string.home_skip_selected_alarm))
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
                                Text(stringResource(R.string.home_shift_type_change_title))
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
                                    Text(stringResource(R.string.home_apply_selected_shift))
                                }
                                Text(stringResource(R.string.home_apply_selected_shift_hint))
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
                    Text(stringResource(R.string.home_selected_date_alarm_count, alarmsForSelectedDate.size), style = MaterialTheme.typography.titleSmall)
                    if (alarmsForSelectedDate.isEmpty()) {
                        Text(stringResource(R.string.home_selected_date_alarm_empty))
                    } else {
                        alarmsForSelectedDate.forEach { alarm ->
                            val label = alarm.label.ifBlank { unnamedAlarmText }
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

@Composable
private fun HomeTodaySummaryCard(
    modifier: Modifier = Modifier,
    badge: String,
    alarmCount: Int,
    preview: String
) {
    val resources = LocalContext.current.resources
    val badgeLabel = shiftBadgeLabel(resources, badge)
    val badgeBackground = shiftBadgeBackgroundColor(badge)
    val badgeColor = shiftBadgeColor(badge)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.home_today_work_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = badgeLabel,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Box(
                    modifier = Modifier
                        .background(badgeBackground, shape = RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badgeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = badgeColor
                    )
                }
            }
            Text(
                text = if (alarmCount == 0) {
                    stringResource(R.string.home_today_alarm_empty)
                } else {
                    stringResource(R.string.home_today_alarm_count, alarmCount)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.88f)
            )
            Text(
                text = preview,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.76f)
            )
        }
    }
}

@Composable
private fun HomeNextAlarmSummaryCard(
    modifier: Modifier = Modifier,
    nextTriggerText: String,
    remainingText: String,
    onOpenManage: () -> Unit,
    onReconfigurePattern: () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(stringResource(R.string.home_next_alarm_title), style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text(
                nextTriggerText,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(stringResource(R.string.home_remaining_time, remainingText), color = Color.White.copy(alpha = 0.94f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onOpenManage, colors = overlayActionButtonColors()) {
                    Text(stringResource(R.string.home_manage_screen))
                }
                Text(
                    text = stringResource(R.string.home_reconfigure_pattern),
                    modifier = Modifier.clickable(onClick = onReconfigurePattern),
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

@Composable
private fun buildHomeAlarmPreview(alarms: List<AlarmRule>): String {
    if (alarms.isEmpty()) return stringResource(R.string.home_preview_empty)

    val unnamedAlarmText = stringResource(R.string.home_alarm_name_empty)
    val lines = alarms.take(3).map { alarm ->
        val label = alarm.label.ifBlank { unnamedAlarmText }
        String.format("%02d:%02d %s", alarm.hour, alarm.minute, label)
    }
    val extraCount = alarms.size - lines.size
    return if (extraCount > 0) {
        "${lines.joinToString(" · ")} ${stringResource(R.string.home_more_count, extraCount)}"
    } else {
        lines.joinToString(" · ")
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
