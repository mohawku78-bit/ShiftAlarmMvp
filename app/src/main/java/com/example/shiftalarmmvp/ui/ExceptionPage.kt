package com.example.shiftalarmmvp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.shiftalarmmvp.R
import com.example.shiftalarmmvp.data.AlarmRule
import com.example.shiftalarmmvp.scheduler.AlarmTimeCalculator
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ExceptionPage(
    alarms: List<AlarmRule>,
    onToggleTodaySkip: (AlarmRule) -> Unit,
    onToggleTomorrowAdd: (AlarmRule) -> Unit,
    onOpenManage: () -> Unit,
    onOpenEditor: () -> Unit
) {
    val resources = LocalContext.current.resources
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ShiftPanel(
            modifier = Modifier.fillMaxWidth(),
            containerColor = ShiftDesign.Paper,
            borderColor = ShiftDesign.Line
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    stringResource(R.string.exception_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = ShiftDesign.Ink
                )
                Text(
                    stringResource(R.string.exception_description),
                    color = ShiftDesign.InkSoft
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PrimaryActionButton(onClick = onOpenEditor, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.exception_add_alarm))
                    }
                    NeutralActionButton(onClick = onOpenManage, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.exception_manage_screen))
                    }
                }
            }
        }

        if (alarms.isEmpty()) {
            ShiftPanel(
                modifier = Modifier.fillMaxWidth(),
                containerColor = ShiftDesign.Mist.copy(alpha = 0.62f),
                borderColor = ShiftDesign.Line
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        stringResource(R.string.exception_empty),
                        color = ShiftDesign.InkSoft
                    )
                }
            }
        } else {
            alarms.forEach { alarm ->
                val label = if (alarm.label.isBlank()) {
                    String.format("%02d:%02d", alarm.hour, alarm.minute)
                } else {
                    alarm.label
                }
                val skippedToday = LocalDate.now() in alarm.skipDateEpochDays
                val tomorrowAdded = LocalDate.now().plusDays(1) in alarm.addDateEpochDays
                val next = AlarmTimeCalculator.nextTrigger(alarm)
                val nextLabel = if (next == null) {
                    stringResource(R.string.common_none)
                } else {
                    stringResource(
                        R.string.exception_next_label_format,
                        next.format(DateTimeFormatter.ofPattern("MM-dd HH:mm")),
                        formatTimeUntil(resources, next)
                    )
                }

                ShiftPanel(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = ShiftDesign.Paper,
                    borderColor = ShiftDesign.Line
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(label, style = MaterialTheme.typography.titleSmall, color = ShiftDesign.Ink)
                        Text(
                            stringResource(R.string.exception_next_alarm, nextLabel),
                            color = ShiftDesign.InkSoft
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SecondaryActionButton(onClick = { onToggleTodaySkip(alarm) }, modifier = Modifier.weight(1f)) {
                                Text(
                                    if (skippedToday) {
                                        stringResource(R.string.exception_today_skip_cancel)
                                    } else {
                                        stringResource(R.string.exception_today_skip)
                                    }
                                )
                            }
                            SecondaryActionButton(onClick = { onToggleTomorrowAdd(alarm) }, modifier = Modifier.weight(1f)) {
                                Text(
                                    if (tomorrowAdded) {
                                        stringResource(R.string.exception_tomorrow_add_cancel)
                                    } else {
                                        stringResource(R.string.exception_tomorrow_add)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
