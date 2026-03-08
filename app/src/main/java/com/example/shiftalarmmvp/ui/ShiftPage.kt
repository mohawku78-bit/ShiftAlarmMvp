package com.example.shiftalarmmvp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@Composable
fun ShiftPage(
    customWorkTypeInput: String,
    onCustomWorkTypeInputChange: (String) -> Unit,
    onAddType: () -> Unit,
    onResetDefaults: () -> Unit,
    quickTemplates: List<QuickShiftTemplate>,
    onApplyQuickTemplate: (QuickShiftTemplate) -> Unit,
    workTypeConfigs: List<WorkTypeAlarmConfig>,
    onAppendRotationType: (String) -> Unit,
    rotationSequence: List<String>,
    onDropLastRotation: () -> Unit,
    onClearRotation: () -> Unit,
    todayRotationIndex: Int,
    onTodayRotationIndexChange: (Int) -> Unit,
    onToggleConfigEnabled: (Int, Boolean) -> Unit,
    onDeleteConfigType: (String) -> Unit,
    onConfigPrimaryChange: (Int, String) -> Unit,
    onConfigSecondaryChange: (Int, String) -> Unit,
    infiniteRotationEnabled: Boolean,
    onInfiniteRotationEnabledChange: (Boolean) -> Unit,
    onAutoBuild: () -> Unit,
    showFirstSetupWizard: Boolean,
    onCompleteFirstSetup: () -> Unit,
    onHideFirstSetupWizard: () -> Unit,
    onReopenFirstSetupWizard: () -> Unit,
    preview: List<String>,
    autoBuildFeedback: String,
    selectedCategory: ShiftCategory,
    onSelectedCategoryChange: (ShiftCategory) -> Unit,
    anchorDate: LocalDate,
    onAnchorDateChange: (LocalDate) -> Unit,
    preview14: List<String>
) {
    var wizardStep by rememberSaveable(showFirstSetupWizard) { mutableIntStateOf(0) }
    var selectedTemplate by remember(showFirstSetupWizard, selectedCategory) { mutableStateOf<QuickShiftTemplate?>(null) }
    var previewDays by rememberSaveable(showFirstSetupWizard) { mutableIntStateOf(7) }
    var showAdvanced by rememberSaveable { mutableStateOf(false) }

    val categoryTemplates = quickTemplates.ifEmpty { templatesForCategory(selectedCategory) }
    val previewLines = if (previewDays == 14) preview14 else preview
    val canProceedFromStep0 = selectedTemplate != null || categoryTemplates.isNotEmpty()

    LaunchedEffect(selectedCategory, categoryTemplates) {
        val current = selectedTemplate
        if (categoryTemplates.isEmpty()) {
            selectedTemplate = null
        } else if (current == null || categoryTemplates.none { it.label == current.label }) {
            selectedTemplate = categoryTemplates.first()
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PrimaryActionButton(onClick = onReopenFirstSetupWizard, modifier = Modifier.weight(1f)) {
                Text("근무패턴 다시 설정")
            }
            NeutralActionButton(onClick = { showAdvanced = !showAdvanced }, modifier = Modifier.weight(1f)) {
                Text(if (showAdvanced) "고급 설정 숨기기" else "고급 설정 보기")
            }
        }
    }

    if (showFirstSetupWizard) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Settings, contentDescription = null)
                    Text("교대근무 맞춤 설정", style = MaterialTheme.typography.titleMedium)
                }
                Text("${wizardStep + 1}/5 단계")

                when (wizardStep) {
                    0 -> {
                        Text("어떤 근무 패턴을 사용하시나요?")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            CategoryButton("2교대", selectedCategory == ShiftCategory.TWO_SHIFT) {
                                onSelectedCategoryChange(ShiftCategory.TWO_SHIFT)
                                selectedTemplate = quickTemplates.firstOrNull { it.category == ShiftCategory.TWO_SHIFT }
                                    ?: templatesForCategory(ShiftCategory.TWO_SHIFT).firstOrNull()
                            }
                            CategoryButton("3교대", selectedCategory == ShiftCategory.THREE_SHIFT) {
                                onSelectedCategoryChange(ShiftCategory.THREE_SHIFT)
                                selectedTemplate = quickTemplates.firstOrNull { it.category == ShiftCategory.THREE_SHIFT }
                                    ?: templatesForCategory(ShiftCategory.THREE_SHIFT).firstOrNull()
                            }
                            CategoryButton("직접 설정", selectedCategory == ShiftCategory.CUSTOM) {
                                onSelectedCategoryChange(ShiftCategory.CUSTOM)
                                selectedTemplate = quickTemplates.firstOrNull { it.category == ShiftCategory.CUSTOM }
                                    ?: templatesForCategory(ShiftCategory.CUSTOM).firstOrNull()
                                showAdvanced = true
                            }
                        }

                        Text("템플릿 선택")
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            categoryTemplates.forEach { template ->
                                val selected = selectedTemplate?.label == template.label
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(template.label, style = MaterialTheme.typography.titleSmall)
                                            Text(template.sequence.joinToString(" → "))
                                        }
                                        Button(onClick = { selectedTemplate = template }, colors = segmentedActionButtonColors(selected)) {
                                            Text(if (selected) "선택됨" else "선택")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        Text("근무 유형 확인 및 알람 시간")
                        workTypeConfigs.forEachIndexed { index, config ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(config.type)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        Text("알람 사용")
                                        Switch(
                                            checked = config.enabled,
                                            onCheckedChange = { checked -> onToggleConfigEnabled(index, checked) }
                                        )
                                    }
                                    OutlinedTextField(
                                        value = config.primaryTime,
                                        onValueChange = { onConfigPrimaryChange(index, it.take(5)) },
                                        label = { Text("1차 알람(HH:mm)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = config.secondaryTime,
                                        onValueChange = { onConfigSecondaryChange(index, it.take(5)) },
                                        label = { Text("2차 알람(선택)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    2 -> {
                        Text("기준일과 오늘 위치를 확인하세요")
                        DatePickerButton(label = "기준일", date = anchorDate, onDatePicked = onAnchorDateChange)
                        if (rotationSequence.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                rotationSequence.forEachIndexed { index, type ->
                                    val selected = todayRotationIndex == index
                                    Button(onClick = { onTodayRotationIndexChange(index) }, colors = segmentedActionButtonColors(selected)) {
                                        Text("${index + 1}:$type")
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        Text("다음 근무 미리보기")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(onClick = { previewDays = 7 }, modifier = Modifier.weight(1f), colors = segmentedActionButtonColors(previewDays == 7)) { Text("7일") }
                            Button(onClick = { previewDays = 14 }, modifier = Modifier.weight(1f), colors = segmentedActionButtonColors(previewDays == 14)) { Text("14일") }
                        }
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (previewLines.isEmpty()) {
                                    Text("미리보기 없음")
                                } else {
                                    previewLines.forEach { line -> Text(line) }
                                }
                            }
                        }
                    }

                    else -> {
                        Text("설정을 확정하면 새 패턴 기준으로 자동 생성이 진행됩니다.")
                        Text("기존 알람은 즉시 삭제되지 않습니다.")
                        if (autoBuildFeedback.isNotBlank()) {
                            Text(autoBuildFeedback, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                if (!showAdvanced) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        if (wizardStep > 0) {
                            NeutralActionButton(onClick = { wizardStep -= 1 }, modifier = Modifier.weight(1f)) {
                                Text("이전")
                            }
                        }

                        if (wizardStep < 4) {
                            PrimaryActionButton(
                                onClick = {
                                    if (wizardStep == 0) {
                                        val chosen = selectedTemplate ?: categoryTemplates.firstOrNull()
                                        if (chosen != null) {
                                            onApplyQuickTemplate(chosen)
                                            if (chosen.category == ShiftCategory.CUSTOM || chosen.label.contains("직접")) {
                                                showAdvanced = true
                                            }
                                        } else if (selectedCategory != ShiftCategory.CUSTOM) {
                                            return@PrimaryActionButton
                                        } else {
                                            showAdvanced = true
                                        }
                                    }
                                    wizardStep += 1
                                },
                                enabled = wizardStep != 0 || canProceedFromStep0,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("다음")
                            }
                        } else {
                            PrimaryActionButton(onClick = onCompleteFirstSetup, modifier = Modifier.weight(1f)) {
                                Text("확정")
                            }
                            NeutralActionButton(onClick = onHideFirstSetupWizard, modifier = Modifier.weight(1f)) {
                                Text("닫기")
                            }
                        }
                    }
                }
            }
        }
    }

    if (!showAdvanced) return

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Settings, contentDescription = null)
                Text("고급 패턴 편집", style = MaterialTheme.typography.titleMedium)
            }

            OutlinedTextField(
                value = customWorkTypeInput,
                onValueChange = { onCustomWorkTypeInputChange(it.take(12)) },
                label = { Text("근무 유형 이름") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                SecondaryActionButton(onClick = onAddType, modifier = Modifier.weight(1f)) { Text("유형 추가") }
                NeutralActionButton(onClick = onResetDefaults, modifier = Modifier.weight(1f)) { Text("기본값") }
            }

            if (workTypeConfigs.isNotEmpty()) {
                val rotationAppendableConfigs = workTypeConfigs.filterNot { it.type.contains("휴가") }
                val vacationConfigs = workTypeConfigs.filter { it.type.contains("휴가") }

                Text("등록된 근무 유형")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    rotationAppendableConfigs.forEach { cfg ->
                        NeutralActionButton(onClick = { onAppendRotationType(cfg.type) }) {
                            Text(cfg.type)
                        }
                    }
                }

                Text("유형 관리")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    rotationAppendableConfigs.forEach { cfg ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            SecondaryActionButton(onClick = { onAppendRotationType(cfg.type) }, modifier = Modifier.weight(1f)) {
                                Text("${cfg.type} 추가")
                            }
                            DangerActionButton(onClick = { onDeleteConfigType(cfg.type) }, modifier = Modifier.weight(1f)) {
                                Text("${cfg.type} 삭제")
                            }
                        }
                    }
                    vacationConfigs.forEach { cfg ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            NeutralActionButton(onClick = { }, enabled = false, modifier = Modifier.weight(1f)) {
                                Text("${cfg.type}는 예외에서 처리")
                            }
                            DangerActionButton(onClick = { onDeleteConfigType(cfg.type) }, modifier = Modifier.weight(1f)) {
                                Text("${cfg.type} 삭제")
                            }
                        }
                    }
                }

                Text("알람 시간은 맞춤 설정 2단계에서 조정하세요.")
                if (!showFirstSetupWizard) {
                    NeutralActionButton(onClick = onReopenFirstSetupWizard, modifier = Modifier.fillMaxWidth()) {
                        Text("알람 시간 다시 설정")
                    }
                }
            }

            Text("로테이션")
            Text(
                if (rotationSequence.isEmpty()) "[ + ] 버튼으로 로테이션을 채우세요."
                else rotationSequence.mapIndexed { i, type -> "${i + 1}.$type" }.joinToString("  ->  ")
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                NeutralActionButton(onClick = onDropLastRotation, modifier = Modifier.weight(1f)) { Text("마지막 삭제") }
                DangerActionButton(onClick = onClearRotation, modifier = Modifier.weight(1f)) { Text("로테이션 비우기") }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Text("무한 반복")
                Switch(checked = infiniteRotationEnabled, onCheckedChange = onInfiniteRotationEnabledChange)
                Text(if (infiniteRotationEnabled) "ON" else "OFF")
                Spacer(modifier = Modifier.width(4.dp))
            }

            if (showFirstSetupWizard) {
                val progressLabel = when {
                    wizardStep <= 1 -> "3단계(기준일)로 진행"
                    wizardStep == 2 -> "4단계(미리보기)로 진행"
                    wizardStep == 3 -> "5단계(확정)로 진행"
                    else -> "설정 확정"
                }
                PrimaryActionButton(
                    onClick = {
                        when {
                            wizardStep <= 1 -> {
                                wizardStep = 2
                                showAdvanced = false
                            }
                            wizardStep in 2..3 -> {
                                wizardStep += 1
                                showAdvanced = false
                            }
                            else -> onCompleteFirstSetup()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(progressLabel)
                }
                Text("현재 단계: ${wizardStep + 1}/5")
                Text("직접구성은 3~5단계(기준일/미리보기/확정)까지 진행해야 완료됩니다.")
            } else {
                PrimaryActionButton(onClick = onAutoBuild, modifier = Modifier.fillMaxWidth()) {
                    Text("자동 생성")
                }
            }

            if (autoBuildFeedback.isNotBlank()) {
                Text(autoBuildFeedback, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun RowScope.CategoryButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Button(onClick = onClick, modifier = Modifier.weight(1f), colors = segmentedActionButtonColors(selected)) {
        Text(label)
    }
}











