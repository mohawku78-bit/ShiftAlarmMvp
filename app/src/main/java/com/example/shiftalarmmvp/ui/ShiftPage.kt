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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import java.time.YearMonth

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
    autoBuildFeedback: String,
    selectedCategory: ShiftCategory,
    onSelectedCategoryChange: (ShiftCategory) -> Unit,
    anchorDate: LocalDate,
    onAnchorDateChange: (LocalDate) -> Unit,
) {
    var wizardStep by rememberSaveable(showFirstSetupWizard) { mutableIntStateOf(0) }
    var selectedTemplate by remember(showFirstSetupWizard, selectedCategory) { mutableStateOf<QuickShiftTemplate?>(null) }
    var previewDays by rememberSaveable(showFirstSetupWizard) { mutableIntStateOf(7) }
    var showAdvanced by rememberSaveable { mutableStateOf(false) }
    var selectedStep1TypeIndex by rememberSaveable(showFirstSetupWizard) { mutableIntStateOf(0) }

    val categoryTemplates = quickTemplates.ifEmpty { templatesForCategory(selectedCategory) }
    val selectedTemplatePreview30Days = selectedTemplate?.let { template ->
        val sequence = template.sequence
        if (sequence.isEmpty()) emptyList() else {
            (0 until 30).map { offset ->
                anchorDate.plusDays(offset.toLong()) to sequence[offset % sequence.size]
            }
        }
    }.orEmpty()
    val previewDaysData = if (rotationSequence.isEmpty()) {
        emptyList()
    } else {
        val startIndex = todayRotationIndex.coerceIn(0, rotationSequence.size - 1)
        (0 until previewDays).map { offset ->
            anchorDate.plusDays(offset.toLong()) to rotationSequence[(startIndex + offset) % rotationSequence.size]
        }
    }
    val canProceedFromStep0 = selectedTemplate != null || categoryTemplates.isNotEmpty()

    LaunchedEffect(selectedCategory, categoryTemplates) {
        val current = selectedTemplate
        if (categoryTemplates.isEmpty()) {
            selectedTemplate = null
        } else if (current == null || categoryTemplates.none { it.label == current.label }) {
            selectedTemplate = categoryTemplates.first()
        }
    }

    LaunchedEffect(showFirstSetupWizard) {
        if (showFirstSetupWizard) showAdvanced = false
    }

    LaunchedEffect(workTypeConfigs.size) {
        selectedStep1TypeIndex = if (workTypeConfigs.isEmpty()) {
            0
        } else {
            selectedStep1TypeIndex.coerceIn(0, workTypeConfigs.lastIndex)
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
            NeutralActionButton(onClick = { showAdvanced = !showAdvanced }, enabled = !showFirstSetupWizard, modifier = Modifier.weight(1f)) {
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
                Text("${wizardStep + 1}/4 단계")

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
                                // Keep wizard focused; advanced stays hidden during setup.
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

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("선택 템플릿 30일 미리보기", style = MaterialTheme.typography.titleSmall)
                                if (selectedTemplate == null) {
                                    Text("템플릿을 선택하면 다음 30일 근무가 표시됩니다.")
                                } else {
                                    Text(
                                        "기준일 ${anchorDate}부터 30일",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    WorkPreviewCalendar(previewDays = selectedTemplatePreview30Days)
                                }
                            }
                        }
                    }

                    1 -> {
                        Text("근무 유형 확인 및 알람 시간")
                        Text(
                            "\uC704 \uADFC\uBB34\uC720\uD615 \uBC84\uD2BC\uC744 \uB20C\uB7EC \uC2DC\uAC04\uC744 \uC124\uC815\uD558\uC138\uC694.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (workTypeConfigs.isEmpty()) {
                            Text("\uB4F1\uB85D\uB41C \uADFC\uBB34 \uC720\uD615\uC774 \uC5C6\uC2B5\uB2C8\uB2E4.")
                        } else {
                            val selectedConfigIndex = selectedStep1TypeIndex.coerceIn(0, workTypeConfigs.lastIndex)
                            val selectedConfig = workTypeConfigs[selectedConfigIndex]
                            Text("\uADFC\uBB34 \uC720\uD615 \uC120\uD0DD")
                            workTypeConfigs.mapIndexed { index, config -> index to config.type }
                                .chunked(4)
                                .forEach { rowItems ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        rowItems.forEach { (index, type) ->
                                            val selected = selectedConfigIndex == index
                                            Button(
                                                onClick = { selectedStep1TypeIndex = index },
                                                modifier = Modifier.weight(1f),
                                                colors = segmentedActionButtonColors(selected)
                                            ) {
                                                Text(step1TypeChipLabel(type))
                                            }
                                        }
                                        repeat(4 - rowItems.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(selectedConfig.type, style = MaterialTheme.typography.titleMedium)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        Text("알람 사용")
                                        Switch(
                                            checked = selectedConfig.enabled,
                                            onCheckedChange = { checked -> onToggleConfigEnabled(selectedConfigIndex, checked) }
                                        )
                                    }
                                    OutlinedTextField(
                                        value = selectedConfig.primaryTime,
                                        onValueChange = { onConfigPrimaryChange(selectedConfigIndex, it.take(5)) },
                                        label = { Text("1차 알람(HH:mm)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = selectedConfig.secondaryTime,
                                        onValueChange = { onConfigSecondaryChange(selectedConfigIndex, it.take(5)) },
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
                            val sequenceItems = rotationSequence.mapIndexed { index, type -> index to type }
                            sequenceItems.chunked(3).forEach { rowItems ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    rowItems.forEach { (index, type) ->
                                        val selected = todayRotationIndex == index
                                        Button(
                                            onClick = { onTodayRotationIndexChange(index) },
                                            modifier = Modifier.weight(1f),
                                            colors = segmentedActionButtonColors(selected)
                                        ) {
                                            Text("${index + 1}:$type")
                                        }
                                    }
                                    repeat(3 - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
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
                            Button(onClick = { previewDays = 30 }, modifier = Modifier.weight(1f), colors = segmentedActionButtonColors(previewDays == 30)) { Text("30일") }
                        }
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (previewDaysData.isEmpty()) {
                                    Text("미리보기 없음")
                                } else {
                                    WorkPreviewCalendar(previewDays = previewDaysData)
                                }
                            }
                        }
                    }

                    else -> {
                        Text("설정을 확정하면 새 패턴 기준으로 자동 생성이 진행됩니다.")
                        Text("기존 알람은 즉시 삭제되지 않습니다.")
                        if (!showFirstSetupWizard && autoBuildFeedback.isNotBlank()) {
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

                        if (wizardStep < 3) {
                            PrimaryActionButton(
                                onClick = {
                                    if (wizardStep == 0) {
                                        val chosen = selectedTemplate ?: categoryTemplates.firstOrNull()
                                        if (chosen != null) {
                                            onApplyQuickTemplate(chosen)
                                            if (chosen.category == ShiftCategory.CUSTOM || chosen.label.contains("직접")) {
                                                // Keep wizard focused; advanced stays hidden during setup.
                                            }
                                        } else if (selectedCategory != ShiftCategory.CUSTOM) {
                                            return@PrimaryActionButton
                                        } else {
                                            return@PrimaryActionButton
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
                val rotationAppendableConfigs = workTypeConfigs

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
            } else {
                PrimaryActionButton(onClick = onAutoBuild, modifier = Modifier.fillMaxWidth()) {
                    Text("자동 생성")
                }
            }

            if (!showFirstSetupWizard && autoBuildFeedback.isNotBlank()) {
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

private fun step1TypeChipLabel(type: String): String {
    val normalized = type.trim()
    return when (normalized) {
        "\uC8FC\uAC04" -> "\uC8FC"
        "\uC57C\uAC04" -> "\uC57C"
        "\uB2F9\uC9C1" -> "\uB2F9"
        "\uBE44\uBC88" -> "\uBE44"
        "\uD734\uBB34", "\uD734\uC77C", "\uD734\uAC00" -> "\uD734"
        else -> normalized
    }
}

@Composable
private fun WorkPreviewCalendar(previewDays: List<Pair<LocalDate, String>>) {
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

    monthList.forEach { month ->
        Column(verticalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
            Text("${month.year}년 ${month.monthValue}월", style = MaterialTheme.typography.titleSmall)
            PreviewMonthGrid(month = month, previewMap = previewMap, startDate = startDate, endDate = endDate)
        }
    }
}

@Composable
private fun PreviewMonthGrid(
    month: YearMonth,
    previewMap: Map<LocalDate, String>,
    startDate: LocalDate,
    endDate: LocalDate
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

    Column(verticalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            dayLabels.forEach { label ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(label, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        dates.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                week.forEach { date ->
                    val type = date?.let { previewMap[it] }
                    val badge = type?.let(::previewTypeBadge)
                    val inRange = date != null && !date.isBefore(startDate) && !date.isAfter(endDate)
                    val bg = when {
                        badge != null -> previewBadgeBackgroundColor(badge)
                        inRange -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
                    }
                    val fg = if (badge != null) previewBadgeColor(badge) else MaterialTheme.colorScheme.onSurfaceVariant
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(bg, shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                            .border(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                            .padding(vertical = 5.dp, horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = date?.dayOfMonth?.toString() ?: "",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                            if (badge != null) {
                                Text(
                                    text = previewBadgeLabel(badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = fg,
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

private fun previewTypeBadge(type: String): String {
    return when (normalizeWorkType(type)) {
        "주간" -> "주"
        "야간" -> "야"
        "당직" -> "당"
        "비번" -> "비"
        "휴무", "휴일", "휴가" -> "휴"
        else -> "근"
    }
}

private fun previewBadgeLabel(badge: String): String {
    return when (badge) {
        "주" -> "▲ 주"
        "야" -> "■ 야"
        "당" -> "◆ 당"
        "비" -> "● 비"
        "휴" -> "○ 휴"
        else -> "• 근"
    }
}

private fun previewBadgeBackgroundColor(badge: String): Color {
    return when (badge) {
        "주" -> Color(0xFFD9E8FA)
        "야" -> Color(0xFFFFE3C8)
        "당" -> Color(0xFFFFE9D6)
        "비" -> Color(0xFFE3E8EE)
        "휴" -> Color(0xFFEEF1F4)
        else -> Color(0xFFE2F0EA)
    }
}

private fun previewBadgeColor(badge: String): Color {
    return when (badge) {
        "주" -> Color(0xFF1E4E8C)
        "야" -> Color(0xFF9A5400)
        "당" -> Color(0xFF8A3E00)
        "비" -> Color(0xFF4F6375)
        "휴" -> Color(0xFF5B6670)
        else -> Color(0xFF4D6B5C)
    }
}


