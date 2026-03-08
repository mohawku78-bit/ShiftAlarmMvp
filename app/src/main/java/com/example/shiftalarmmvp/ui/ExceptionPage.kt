package com.example.shiftalarmmvp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("예외 처리", style = MaterialTheme.typography.titleMedium)
                Text("오늘 스킵/내일 추가를 빠르게 처리합니다.")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PrimaryActionButton(onClick = onOpenEditor, modifier = Modifier.weight(1f)) {
                        Text("새 알람 추가")
                    }
                    NeutralActionButton(onClick = onOpenManage, modifier = Modifier.weight(1f)) {
                        Text("관리 화면")
                    }
                }
            }
        }

        if (alarms.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("등록된 알람이 없습니다.")
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
                    "없음"
                } else {
                    "${next.format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))} (${formatTimeUntil(next)} 후)"
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(label, style = MaterialTheme.typography.titleSmall)
                        Text("다음 울림: $nextLabel")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SecondaryActionButton(onClick = { onToggleTodaySkip(alarm) }, modifier = Modifier.weight(1f)) {
                                Text(if (skippedToday) "오늘 스킵 취소" else "오늘 스킵")
                            }
                            SecondaryActionButton(onClick = { onToggleTomorrowAdd(alarm) }, modifier = Modifier.weight(1f)) {
                                Text(if (tomorrowAdded) "내일 추가 취소" else "내일 추가")
                            }
                        }
                    }
                }
            }
        }
    }
}

