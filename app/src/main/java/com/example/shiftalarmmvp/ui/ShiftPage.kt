package com.example.shiftalarmmvp.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.shiftalarmmvp.R
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

private data class ProfessionPresetRecommendation(
    @StringRes val titleResId: Int,
    @StringRes val supportingResId: Int,
    @StringRes val badgeResId: Int,
    val accentColor: Color,
    val category: ShiftCategory,
    val templateId: String?,
    val previewTypes: List<String>
)

private val PROFESSION_PRESET_RECOMMENDATIONS = listOf(
    ProfessionPresetRecommendation(
        titleResId = R.string.shift_profession_hospital_title,
        supportingResId = R.string.shift_profession_hospital_support,
        badgeResId = R.string.shift_profession_hospital_badge,
        accentColor = ShiftDesign.Lagoon,
        category = ShiftCategory.THREE_SHIFT,
        templateId = QUICK_TEMPLATE_ID_DAY_NIGHT_OFF,
        previewTypes = listOf(WORK_TYPE_DAY, WORK_TYPE_NIGHT, WORK_TYPE_OFF)
    ),
    ProfessionPresetRecommendation(
        titleResId = R.string.shift_profession_fire_title,
        supportingResId = R.string.shift_profession_fire_support,
        badgeResId = R.string.shift_profession_fire_badge,
        accentColor = ShiftDesign.Coral,
        category = ShiftCategory.TWO_SHIFT,
        templateId = QUICK_TEMPLATE_ID_DUTY_OFF,
        previewTypes = listOf(WORK_TYPE_DUTY, WORK_TYPE_OFF)
    ),
    ProfessionPresetRecommendation(
        titleResId = R.string.shift_profession_factory_title,
        supportingResId = R.string.shift_profession_factory_support,
        badgeResId = R.string.shift_profession_factory_badge,
        accentColor = ShiftDesign.Sun,
        category = ShiftCategory.TWO_SHIFT,
        templateId = QUICK_TEMPLATE_ID_DAY_NIGHT,
        previewTypes = listOf(WORK_TYPE_DAY, WORK_TYPE_NIGHT)
    ),
    ProfessionPresetRecommendation(
        titleResId = R.string.shift_profession_custom_title,
        supportingResId = R.string.shift_profession_custom_support,
        badgeResId = R.string.shift_profession_custom_badge,
        accentColor = ShiftDesign.Harbor,
        category = ShiftCategory.CUSTOM,
        templateId = null,
        previewTypes = listOf(WORK_TYPE_DAY, WORK_TYPE_DUTY, WORK_TYPE_OFF)
    )
)

private val ProfessionPresetCardHeight = 256.dp
private val PatternTemplateCardMinHeight = 150.dp
private val WorkTypeAlarmCardMinHeight = 210.dp

@Composable
fun ShiftPage(
    draft: ShiftSetupDraft,
    defaults: ShiftSetupDefaults,
    onDraftChange: ((ShiftSetupDraft) -> ShiftSetupDraft) -> Unit,
    quickTemplates: List<QuickShiftTemplate>,
    showFirstSetupWizard: Boolean,
    onAutoBuild: () -> Unit,
    onCompleteFirstSetup: () -> Unit,
    onHideFirstSetupWizard: () -> Unit,
    onReopenFirstSetupWizard: () -> Unit,
) {
    val context = LocalContext.current
    val normalizedDraft = draft.normalized()
    val normalizedWizardState = normalizedDraft.wizardState
    val customWorkTypeInput = normalizedDraft.customWorkTypeInput
    val workTypeConfigs = normalizedDraft.workTypeConfigs
    val rotationSequence = normalizedDraft.rotationSequence
    val todayRotationIndex = normalizedDraft.todayRotationIndex
    val infiniteRotationEnabled = normalizedDraft.infiniteRotationEnabled
    val autoBuildFeedback = normalizedDraft.autoBuildFeedback
    val selectedCategory = normalizedDraft.selectedCategory
    val anchorDate = normalizedDraft.anchorDate
    val wizardStep = normalizedWizardState.step
    val showAdvanced = normalizedWizardState.showAdvanced
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val professionPresetColumnCount = if (screenWidthDp >= 560) 2 else 1
    val selectedStep1TypeIndex = if (normalizedWizardState.selectedStep1TypeIndex in workTypeConfigs.indices) {
        normalizedWizardState.selectedStep1TypeIndex
    } else if (workTypeConfigs.isNotEmpty()) {
        0
    } else {
        -1
    }

    fun updateDraft(transform: (ShiftSetupDraft) -> ShiftSetupDraft) {
        onDraftChange(transform)
    }

    fun dispatch(action: ShiftSetupAction) {
        updateDraft { currentDraft -> currentDraft.reduce(action, defaults) }
    }

    fun updateWizardState(transform: (FirstSetupWizardState) -> FirstSetupWizardState) {
        updateDraft { currentDraft ->
            val currentWizardState = currentDraft.normalized().wizardState
            currentDraft.copy(wizardState = transform(currentWizardState)).normalized()
        }
    }

    val onCustomWorkTypeInputChange: (String) -> Unit = {
        dispatch(ShiftSetupAction.UpdateCustomWorkTypeInput(it))
    }
    val onAddType: () -> Unit = {
        updateDraft { currentDraft ->
            val normalizedName = normalizeWorkType(currentDraft.customWorkTypeInput)
            currentDraft.reduce(
                ShiftSetupAction.AddType(
                    emptyFeedback = context.getString(R.string.main_shift_type_name_empty),
                    existsFeedback = context.getString(R.string.main_shift_type_exists),
                    addedFeedback = context.getString(R.string.main_shift_type_added_format, normalizedName)
                ),
                defaults
            )
        }
    }
    val onResetDefaults: () -> Unit = {
        dispatch(ShiftSetupAction.ResetDefaults(context.getString(R.string.main_shift_defaults_reset)))
    }
    val onAppendRotationType: (String) -> Unit = { type ->
        dispatch(
            ShiftSetupAction.AppendRotationType(
                type = type,
                feedback = context.getString(R.string.main_shift_rotation_type_added_format, type)
            )
        )
    }
    val onDropLastRotation: () -> Unit = { dispatch(ShiftSetupAction.DropLastRotation) }
    val onClearRotation: () -> Unit = { dispatch(ShiftSetupAction.ClearRotation) }
    val onTodayRotationIndexChange: (Int) -> Unit = {
        dispatch(ShiftSetupAction.ChangeTodayRotationIndex(it))
    }
    val onToggleConfigEnabled: (Int, Boolean) -> Unit = { index, checked ->
        dispatch(ShiftSetupAction.ToggleConfigEnabled(index, checked))
    }
    val onDeleteConfigType: (String) -> Unit = {
        dispatch(ShiftSetupAction.DeleteConfigType(it))
    }
    val onConfigPrimaryChange: (Int, String) -> Unit = { index, value ->
        dispatch(ShiftSetupAction.ChangePrimaryTime(index, value))
    }
    val onConfigSecondaryChange: (Int, String) -> Unit = { index, value ->
        dispatch(ShiftSetupAction.ChangeSecondaryTime(index, value))
    }
    val onInfiniteRotationEnabledChange: (Boolean) -> Unit = {
        dispatch(ShiftSetupAction.SetInfiniteRotationEnabled(it))
    }
    val onAnchorDateChange: (LocalDate) -> Unit = {
        dispatch(ShiftSetupAction.ChangeAnchorDate(it))
    }

    BackHandler(enabled = showFirstSetupWizard) {
        if (wizardStep > 0) {
            dispatch(ShiftSetupAction.Back)
        } else {
            onHideFirstSetupWizard()
        }
    }

    val availableTemplates = quickTemplates.ifEmpty { QUICK_SHIFT_TEMPLATES }
    fun templatesForWizardCategory(category: ShiftCategory): List<QuickShiftTemplate> {
        return availableTemplates.filter { it.category == category }.ifEmpty { templatesForCategory(category) }
    }
    val categoryTemplates: List<QuickShiftTemplate> = if (selectedCategory == ShiftCategory.CUSTOM) {
        emptyList()
    } else {
        templatesForWizardCategory(selectedCategory)
    }
    fun onSelectedCategoryChange(category: ShiftCategory) {
        dispatch(
            ShiftSetupAction.SelectCategory(
                category = category,
                selectedTemplateId = if (category == ShiftCategory.CUSTOM) {
                    null
                } else {
                    templatesForWizardCategory(category).firstOrNull()?.id
                }
            )
        )
    }

        fun onProfessionPresetSelected(preset: ProfessionPresetRecommendation) {
        dispatch(
            ShiftSetupAction.SelectCategory(
                category = preset.category,
                selectedTemplateId = preset.templateId
            )
        )
    }

    fun isProfessionPresetSelected(preset: ProfessionPresetRecommendation): Boolean {
        return if (preset.templateId == null) {
            selectedCategory == preset.category
        } else {
            selectedCategory == preset.category && normalizedWizardState.selectedTemplateId == preset.templateId
        }
    }

    val selectedTemplate = categoryTemplates.firstOrNull { it.id == normalizedWizardState.selectedTemplateId }
    val usesCustomPattern = selectedCategory == ShiftCategory.CUSTOM
    val hasSelectedWorkType = selectedStep1TypeIndex in workTypeConfigs.indices
    val canAdvanceWizard = canAdvanceFirstSetupWizard(
        step = wizardStep,
        usesCustomPattern = usesCustomPattern,
        hasCustomRotation = rotationSequence.isNotEmpty(),
        hasPresetTemplate = selectedTemplate != null,
        hasSelectedWorkType = hasSelectedWorkType
    )
    val shouldApplyQuickTemplate = shouldApplyQuickTemplateOnAdvance(
        step = wizardStep,
        usesCustomPattern = usesCustomPattern
    )

    fun advanceWizard() {
        if (!canAdvanceWizard) return
        updateDraft { currentDraft ->
            var nextDraft = currentDraft
            if (shouldApplyQuickTemplate) {
                val chosen = selectedTemplate ?: categoryTemplates.firstOrNull() ?: return@updateDraft currentDraft
                nextDraft = nextDraft.reduce(
                    ShiftSetupAction.ApplyQuickTemplate(
                        template = chosen,
                        feedback = context.getString(
                            R.string.main_shift_quick_template_applied_format,
                            context.getString(chosen.labelResId)
                        )
                    ),
                    defaults
                )
            }
            nextDraft.reduce(ShiftSetupAction.Next, defaults)
        }
    }
    val wizardLastStep = 2
    val wizardTotalSteps = wizardLastStep + 1




    if (!showFirstSetupWizard) {
        ShiftPanel(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PrimaryActionButton(onClick = onReopenFirstSetupWizard, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.shift_reopen_setup))
                }
                NeutralActionButton(
                    onClick = { updateWizardState { it.copy(showAdvanced = !showAdvanced) } },
                    enabled = !showFirstSetupWizard,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        if (showAdvanced) {
                            stringResource(R.string.shift_hide_advanced)
                        } else {
                            stringResource(R.string.shift_show_advanced)
                        }
                    )
                }
            }
        }
    }

    if (showFirstSetupWizard) {
        val wizardHeroTitle = when (wizardStep) {
            0 -> stringResource(R.string.shift_step_pattern_title)
            1 -> stringResource(R.string.shift_wizard_title)
            else -> stringResource(R.string.shift_wizard_title)
        }
        val wizardHeroSupporting = when (wizardStep) {
            0 -> stringResource(R.string.shift_step_pattern_support)
            1 -> stringResource(R.string.shift_step_alarm_hint)
            else -> stringResource(R.string.shift_completion_message)
        }

        ShiftPanel(
            modifier = Modifier.fillMaxWidth().testTag(ShiftSetupTestTags.WIZARD_ROOT),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(30.dp),
            containerColor = ShiftDesign.Paper,
            borderColor = ShiftDesign.Line
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                PatternWizardHeroCard(
                    stepLabel = stringResource(R.string.shift_wizard_step_format, wizardStep + 1, wizardTotalSteps),
                    title = wizardHeroTitle,
                    supporting = wizardHeroSupporting,
                    currentStep = wizardStep,
                    totalSteps = wizardTotalSteps,
                    modifier = Modifier.testTag(ShiftSetupTestTags.STEP_LABEL)
                )

                when (wizardStep) {
                    0 -> {
                        PatternWizardSectionCard(title = stringResource(R.string.shift_profession_section_title)) {
                            Text(
                                text = stringResource(R.string.shift_profession_section_support),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            PROFESSION_PRESET_RECOMMENDATIONS.chunked(professionPresetColumnCount).forEach { rowItems ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    rowItems.forEach { preset ->
                                        ProfessionPresetCard(
                                            preset = preset,
                                            selected = isProfessionPresetSelected(preset),
                                            modifier = Modifier.weight(1f),
                                            onClick = { onProfessionPresetSelected(preset) }
                                        )
                                    }
                                    repeat(professionPresetColumnCount - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            CategoryButton(
                                label = stringResource(R.string.shift_category_two_shift),
                                selected = selectedCategory == ShiftCategory.TWO_SHIFT,
                                onClick = { onSelectedCategoryChange(ShiftCategory.TWO_SHIFT) }
                            )
                            CategoryButton(
                                label = stringResource(R.string.shift_category_three_shift),
                                selected = selectedCategory == ShiftCategory.THREE_SHIFT,
                                modifier = Modifier.testTag(ShiftSetupTestTags.CATEGORY_REPRESENTATIVE),
                                onClick = { onSelectedCategoryChange(ShiftCategory.THREE_SHIFT) }
                            )
                            CategoryButton(
                                label = stringResource(R.string.shift_category_custom),
                                selected = selectedCategory == ShiftCategory.CUSTOM,
                                modifier = Modifier.testTag(ShiftSetupTestTags.CATEGORY_CUSTOM),
                                onClick = { onSelectedCategoryChange(ShiftCategory.CUSTOM) }
                            )
                        }

                        if (selectedCategory != ShiftCategory.CUSTOM) {
                            PatternWizardSectionCard(title = stringResource(R.string.shift_template_select)) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                    categoryTemplates.forEachIndexed { index, template ->
                                        val selected = selectedTemplate?.id == template.id
                                        PatternTemplateCard(
                                            template = template,
                                            selected = selected,
                                            actionModifier = Modifier.testTag(ShiftSetupTestTags.template(index)),
                                            onClick = {
                                                if (selected) {
                                                    advanceWizard()
                                                } else {
                                                    dispatch(ShiftSetupAction.SelectTemplate(template.id))
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            Text(stringResource(R.string.shift_monthly_preview_hint), style = MaterialTheme.typography.bodySmall)
                        } else {
                            val appendableTypes = workTypeConfigs
                                .map { normalizeWorkType(it.type) }
                                .filter { it.isNotBlank() }
                                .distinct()
                                .ifEmpty { listOf(WORK_TYPE_DAY, WORK_TYPE_DUTY, WORK_TYPE_OFF, WORK_TYPE_REST) }

                            PatternWizardSectionCard(title = stringResource(R.string.shift_custom_pattern_input)) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                    appendableTypes.chunked(4).forEachIndexed { rowIndex, rowItems ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                            rowItems.forEachIndexed { itemIndex, type ->
                                                NeutralActionButton(
                                                    onClick = { onAppendRotationType(type) },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .testTag(ShiftSetupTestTags.rotationType(rowIndex * 4 + itemIndex))
                                                ) {
                                                    Text(type)
                                                }
                                            }
                                            repeat(4 - rowItems.size) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                                                shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp)
                                            )
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            stringResource(R.string.shift_input_order),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(rotationSequenceSummary(rotationSequence, R.string.shift_wizard_sequence_empty))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                            NeutralActionButton(onClick = onDropLastRotation, modifier = Modifier.weight(1f)) {
                                                Text(stringResource(R.string.shift_delete_one_step))
                                            }
                                            DangerActionButton(onClick = onClearRotation, modifier = Modifier.weight(1f)) {
                                                Text(stringResource(R.string.shift_clear_sequence))
                                            }
                                        }
                                    }
                                }
                            }

                            Text(stringResource(R.string.shift_monthly_preview_hint), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    1 -> {
                        Text(stringResource(R.string.shift_step_alarm_intro))
                        Text(
                            stringResource(R.string.shift_step_alarm_hint),
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (workTypeConfigs.isEmpty()) {
                            Text(stringResource(R.string.shift_no_work_types))
                        } else {
                            val selectedConfigIndex = if (hasSelectedWorkType) {
                                selectedStep1TypeIndex
                            } else {
                                0
                            }
                            Text(stringResource(R.string.shift_select_work_type))
                            workTypeConfigs.mapIndexed { index, config -> index to config.type }
                                .chunked(4)
                                .forEach { rowItems ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        rowItems.forEach { (index, type) ->
                                            val selected = selectedStep1TypeIndex == index
                                            Button(
                                                onClick = {
                                                    updateWizardState { it.copy(selectedStep1TypeIndex = index) }
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag(ShiftSetupTestTags.workType(index)),
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

                            Text(
                                stringResource(R.string.shift_work_type_alarm_list_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = ShiftDesign.InkSoft
                            )
                            workTypeConfigs.forEachIndexed { index, config ->
                                WorkTypeAlarmConfigCard(
                                    config = config,
                                    selected = selectedConfigIndex == index,
                                    onSelect = {
                                        updateWizardState { it.copy(selectedStep1TypeIndex = index) }
                                    },
                                    onToggleEnabled = { checked -> onToggleConfigEnabled(index, checked) },
                                    onPrimaryTimePicked = { picked ->
                                        onConfigPrimaryChange(index, String.format("%02d:%02d", picked.hour, picked.minute))
                                    },
                                    onSecondaryEnabledChange = { checked ->
                                        if (checked) {
                                            val primaryDisplay = parseHm(config.primaryTime)
                                                ?: parseHm(defaultPrimaryTime(config.type))
                                                ?: LocalTime.of(7, 0)
                                            val defaultSecond = config.secondaryTime.ifBlank {
                                                String.format("%02d:%02d", primaryDisplay.hour, primaryDisplay.minute)
                                            }
                                            onConfigSecondaryChange(index, defaultSecond)
                                        } else {
                                            onConfigSecondaryChange(index, "")
                                        }
                                    },
                                    onSecondaryTimePicked = { picked ->
                                        onConfigSecondaryChange(index, String.format("%02d:%02d", picked.hour, picked.minute))
                                    }
                                )
                            }
                        }
                    }
                    2 -> {
                        Text(stringResource(R.string.shift_step_anchor_intro))
                        DatePickerButton(
                            label = stringResource(R.string.shift_anchor_date),
                            date = anchorDate,
                            onDatePicked = onAnchorDateChange
                        )
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
                                            Text(stringResource(R.string.shift_rotation_position_format, index + 1, type))
                                        }
                                    }
                                    repeat(3 - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        Text(stringResource(R.string.shift_completion_message))
                        Text(stringResource(R.string.shift_existing_alarm_hint))
                        if (!showFirstSetupWizard && autoBuildFeedback.isNotBlank()) {
                            Text(autoBuildFeedback, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                if (!showAdvanced) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        if (wizardStep > 0) {
                            NeutralActionButton(
                                onClick = { updateWizardState { it.back() } },
                                modifier = Modifier.weight(1f).testTag(ShiftSetupTestTags.PREVIOUS_BUTTON)
                            ) {
                                Text(stringResource(R.string.shift_previous))
                            }
                        }

                        NeutralActionButton(
                            onClick = onHideFirstSetupWizard,
                            modifier = Modifier.weight(1f).testTag(ShiftSetupTestTags.CLOSE_BUTTON)
                        ) {
                            Text(stringResource(R.string.shift_close))
                        }

                        if (wizardStep < wizardLastStep) {
                            PrimaryActionButton(
                                onClick = { advanceWizard() },
                                enabled = canAdvanceWizard,
                                modifier = Modifier.weight(1f).testTag(ShiftSetupTestTags.NEXT_BUTTON)
                            ) {
                                Text(stringResource(R.string.shift_next))
                            }
                        } else {
                            PrimaryActionButton(
                                onClick = onCompleteFirstSetup,
                                modifier = Modifier.weight(1f).testTag(ShiftSetupTestTags.COMPLETE_BUTTON)
                            ) {
                                Text(stringResource(R.string.shift_confirm))
                            }
                        }
                    }
                }
            }
        }
    }

    if (!showAdvanced) return

    ShiftPanel(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Settings, contentDescription = null, tint = ShiftDesign.Navy)
                Text(
                    stringResource(R.string.shift_advanced_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = ShiftDesign.Ink
                )
            }

            OutlinedTextField(
                value = customWorkTypeInput,
                onValueChange = { onCustomWorkTypeInputChange(it.take(12)) },
                label = { Text(stringResource(R.string.shift_work_type_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                SecondaryActionButton(onClick = onAddType, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.shift_add_type))
                }
                NeutralActionButton(onClick = onResetDefaults, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.shift_defaults))
                }
            }

            if (workTypeConfigs.isNotEmpty()) {
                val rotationAppendableConfigs = workTypeConfigs

                Text(stringResource(R.string.shift_registered_work_types))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    rotationAppendableConfigs.forEach { cfg ->
                        NeutralActionButton(onClick = { onAppendRotationType(cfg.type) }) {
                            Text(cfg.type)
                        }
                    }
                }

                Text(stringResource(R.string.shift_manage_types))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    rotationAppendableConfigs.forEach { cfg ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            SecondaryActionButton(onClick = { onAppendRotationType(cfg.type) }, modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.shift_append_type_format, cfg.type))
                            }
                            DangerActionButton(onClick = { onDeleteConfigType(cfg.type) }, modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.shift_delete_type_format, cfg.type))
                            }
                        }
                    }
                }
            }

            Text(stringResource(R.string.shift_rotation_title))
            Text(rotationSequenceSummary(rotationSequence, R.string.shift_rotation_empty))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                NeutralActionButton(onClick = onDropLastRotation, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.shift_delete_last))
                }
                DangerActionButton(onClick = onClearRotation, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.shift_clear_rotation))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.shift_infinite_rotation))
                Switch(checked = infiniteRotationEnabled, onCheckedChange = onInfiniteRotationEnabledChange)
                Text(
                    if (infiniteRotationEnabled) {
                        stringResource(R.string.common_on)
                    } else {
                        stringResource(R.string.common_off)
                    }
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            PrimaryActionButton(onClick = onAutoBuild, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.shift_auto_build))
            }

            if (autoBuildFeedback.isNotBlank()) {
                Text(autoBuildFeedback, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun WorkTypeAlarmConfigCard(
    config: WorkTypeAlarmConfig,
    selected: Boolean,
    onSelect: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onPrimaryTimePicked: (LocalTime) -> Unit,
    onSecondaryEnabledChange: (Boolean) -> Unit,
    onSecondaryTimePicked: (LocalTime) -> Unit
) {
    val primaryDisplay = parseHm(config.primaryTime)
        ?: parseHm(defaultPrimaryTime(config.type))
        ?: LocalTime.of(7, 0)
    val secondaryEnabled = config.secondaryTime.isNotBlank()
    val secondaryDisplay = parseHm(config.secondaryTime) ?: primaryDisplay

    ShiftPanel(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = WorkTypeAlarmCardMinHeight),
        containerColor = ShiftDesign.Paper,
        borderColor = if (selected) ShiftDesign.Harbor.copy(alpha = 0.72f) else ShiftDesign.Line
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShiftTypeIllustrationForType(
                        type = config.type,
                        modifier = Modifier.size(56.dp),
                        selected = selected
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            config.type,
                            style = MaterialTheme.typography.titleLarge,
                            color = ShiftDesign.Ink,
                            fontWeight = FontWeight.Bold
                        )
                        ShiftPill(
                            text = if (config.enabled) {
                                stringResource(R.string.common_on)
                            } else {
                                stringResource(R.string.common_off)
                            },
                            containerColor = if (config.enabled) {
                                ShiftDesign.Mist
                            } else {
                                ShiftDesign.MistStrong.copy(alpha = 0.66f)
                            },
                            contentColor = if (config.enabled) ShiftDesign.Navy else ShiftDesign.InkSoft
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.shift_alarm_enabled), color = ShiftDesign.InkSoft)
                    Switch(
                        checked = config.enabled,
                        onCheckedChange = { checked ->
                            onSelect()
                            onToggleEnabled(checked)
                        }
                    )
                }
            }

            TimePickerButton(
                time = primaryDisplay,
                onTimePicked = { picked ->
                    onSelect()
                    onPrimaryTimePicked(picked)
                },
                label = stringResource(R.string.shift_primary_main_time)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(
                        R.string.shift_secondary_alarm_status_format,
                        if (secondaryEnabled) {
                            stringResource(R.string.common_on)
                        } else {
                            stringResource(R.string.common_off)
                        }
                    ),
                    color = ShiftDesign.InkSoft
                )
                Switch(
                    checked = secondaryEnabled,
                    onCheckedChange = { checked ->
                        onSelect()
                        onSecondaryEnabledChange(checked)
                    }
                )
            }

            if (secondaryEnabled) {
                TimePickerButton(
                    time = secondaryDisplay,
                    onTimePicked = { picked ->
                        onSelect()
                        onSecondaryTimePicked(picked)
                    },
                    label = stringResource(R.string.shift_secondary_main_time)
                )
            }
        }
    }
}

@Composable
private fun RowScope.CategoryButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .weight(1f)
            .height(52.dp),
        colors = segmentedActionButtonColors(selected)
    ) {
        Text(label)
    }
}
@Composable
private fun PatternWizardHeroCard(
    stepLabel: String,
    title: String,
    supporting: String,
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = ShiftDesign.Navy)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stepLabel.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.74f),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.86f)
            )
            Text(
                text = wizardProgressText(currentStep, totalSteps),
                style = MaterialTheme.typography.titleSmall,
                color = Color.White.copy(alpha = 0.82f),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PatternWizardSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    ShiftPanel(
        modifier = modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
        containerColor = ShiftDesign.Mist.copy(alpha = 0.58f),
        borderColor = ShiftDesign.Line
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            content()
        }
    }
}

@Composable
private fun ProfessionPresetCard(
    preset: ProfessionPresetRecommendation,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val containerColor = if (selected) {
        preset.accentColor.copy(alpha = 0.10f)
    } else {
        ShiftDesign.Paper
    }
    val borderColor = if (selected) {
        preset.accentColor.copy(alpha = 0.40f)
    } else {
        ShiftDesign.Line
    }

    Card(
        modifier = modifier.height(ProfessionPresetCardHeight),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, borderColor, androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShiftTypeIllustrationForType(
                        type = preset.previewTypes.firstOrNull().orEmpty(),
                        modifier = Modifier.size(56.dp),
                        selected = selected
                    )
                    Box(
                        modifier = Modifier
                            .background(
                                color = preset.accentColor.copy(alpha = if (selected) 0.20f else 0.12f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(preset.badgeResId),
                            color = preset.accentColor,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                Text(
                    text = stringResource(preset.titleResId),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ShiftDesign.Ink
                )
                Text(
                    text = stringResource(preset.supportingResId),
                    style = MaterialTheme.typography.bodySmall,
                    color = ShiftDesign.InkSoft
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    preset.previewTypes.forEach { type ->
                        PatternTemplateTypeChip(type = type)
                    }
                }
            }
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                colors = segmentedActionButtonColors(selected)
            ) {
                Text(
                    if (selected) {
                        stringResource(R.string.shift_template_selected)
                    } else {
                        stringResource(R.string.shift_template_select_action)
                    }
                )
            }
        }
    }
}

@Composable
private fun PatternTemplateCard(
    template: QuickShiftTemplate,
    selected: Boolean,
    actionModifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val containerColor = if (selected) {
        ShiftDesign.Navy.copy(alpha = 0.10f)
    } else {
        ShiftDesign.Paper
    }
    val borderColor = if (selected) {
        ShiftDesign.Navy.copy(alpha = 0.34f)
    } else {
        ShiftDesign.Line
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = PatternTemplateCardMinHeight),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .sizeIn(minHeight = PatternTemplateCardMinHeight)
                .border(1.dp, borderColor, androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val leadType = template.sequence.firstOrNull().orEmpty()
            Column(
                modifier = Modifier.width(72.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ShiftTypeIllustrationForType(
                    type = leadType,
                    modifier = Modifier.size(58.dp),
                    selected = selected
                )
                Text(
                    text = leadType.takeIf { it.isNotBlank() }?.let { step1TypeChipLabel(it) } ?: "?",
                    modifier = Modifier
                        .background(
                            color = if (selected) ShiftDesign.Navy else ShiftDesign.Navy.copy(alpha = 0.12f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp)
                        )
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else ShiftDesign.Navy,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(template.labelResId),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = template.sequence.joinToString(stringResource(R.string.shift_template_sequence_separator)),
                    style = MaterialTheme.typography.bodySmall,
                    color = ShiftDesign.InkSoft
                )
                template.sequence.chunked(4).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        rowItems.forEach { type ->
                            PatternTemplateTypeChip(type = type)
                        }
                    }
                }
            }
            Button(
                onClick = onClick,
                modifier = actionModifier,
                colors = segmentedActionButtonColors(selected)
            ) {
                Text(
                    if (selected) {
                        stringResource(R.string.shift_next)
                    } else {
                        stringResource(R.string.shift_template_select_action)
                    }
                )
            }
        }
    }
}

@Composable
private fun PatternTemplateTypeChip(type: String) {
    val badge = previewTypeBadge(type)
    Text(
        text = step1TypeChipLabel(type),
        modifier = Modifier
            .background(
                color = previewBadgeBackgroundColor(badge),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 9.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        color = previewBadgeColor(badge),
        fontWeight = FontWeight.Bold
    )
}

private fun wizardProgressText(currentStep: Int, totalSteps: Int): String {
    return (0 until totalSteps).joinToString(" ") { index ->
        if (index == currentStep) "●" else "○"
    }
}

private enum class ShiftBadge {
    DAY,
    NIGHT,
    DUTY,
    OFF,
    REST,
    WORK
}

@Composable
private fun rotationSequenceSummary(rotationSequence: List<String>, emptyTextResId: Int): String {
    if (rotationSequence.isEmpty()) return stringResource(emptyTextResId)
    val separator = stringResource(R.string.shift_rotation_separator)
    return rotationSequence.mapIndexed { index, type ->
        stringResource(R.string.shift_rotation_item_format, index + 1, type)
    }.joinToString(separator)
}

private fun knownShiftBadge(type: String): ShiftBadge? {
    return when (normalizeWorkType(type.trim())) {
        WORK_TYPE_DAY -> ShiftBadge.DAY
        WORK_TYPE_NIGHT -> ShiftBadge.NIGHT
        WORK_TYPE_DUTY -> ShiftBadge.DUTY
        WORK_TYPE_OFF -> ShiftBadge.OFF
        WORK_TYPE_REST, WORK_TYPE_HOLIDAY, WORK_TYPE_VACATION -> ShiftBadge.REST
        else -> null
    }
}

@Composable
private fun step1TypeChipLabel(type: String): String {
    val normalized = type.trim()
    return when (knownShiftBadge(normalized)) {
        ShiftBadge.DAY -> stringResource(R.string.shift_work_type_short_day)
        ShiftBadge.NIGHT -> stringResource(R.string.shift_work_type_short_night)
        ShiftBadge.DUTY -> stringResource(R.string.shift_work_type_short_duty)
        ShiftBadge.OFF -> stringResource(R.string.shift_work_type_short_off)
        ShiftBadge.REST -> stringResource(R.string.shift_work_type_short_rest)
        ShiftBadge.WORK, null -> normalized
    }
}

private fun previewTypeBadge(type: String): ShiftBadge {
    return knownShiftBadge(type) ?: ShiftBadge.WORK
}

@Composable
private fun previewBadgeLabel(badge: ShiftBadge): String {
    return when (badge) {
        ShiftBadge.DAY -> stringResource(R.string.shift_preview_badge_day)
        ShiftBadge.NIGHT -> stringResource(R.string.shift_preview_badge_night)
        ShiftBadge.DUTY -> stringResource(R.string.shift_preview_badge_duty)
        ShiftBadge.OFF -> stringResource(R.string.shift_preview_badge_off)
        ShiftBadge.REST -> stringResource(R.string.shift_preview_badge_rest)
        ShiftBadge.WORK -> stringResource(R.string.shift_preview_badge_work)
    }
}

@Composable
private fun previewDayLabels(): List<String> {
    return listOf(
        stringResource(R.string.shift_day_mon),
        stringResource(R.string.shift_day_tue),
        stringResource(R.string.shift_day_wed),
        stringResource(R.string.shift_day_thu),
        stringResource(R.string.shift_day_fri),
        stringResource(R.string.shift_day_sat),
        stringResource(R.string.shift_day_sun)
    )
}

@Composable
private fun WorkPreviewCalendar(previewDays: List<Pair<LocalDate, String>>) {
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

    monthList.forEach { month ->
        Column(verticalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.shift_preview_month_format, month.year, month.monthValue),
                style = MaterialTheme.typography.titleSmall
            )
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
    val dayLabels = previewDayLabels()
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

private fun previewBadgeBackgroundColor(badge: ShiftBadge): Color {
    return when (badge) {
        ShiftBadge.DAY -> Color(0xFFD9E8FA)
        ShiftBadge.NIGHT -> Color(0xFFFFE3C8)
        ShiftBadge.DUTY -> Color(0xFFFFE9D6)
        ShiftBadge.OFF -> Color(0xFFE3E8EE)
        ShiftBadge.REST -> Color(0xFFEEF1F4)
        ShiftBadge.WORK -> Color(0xFFE2F0EA)
    }
}

private fun previewBadgeColor(badge: ShiftBadge): Color {
    return when (badge) {
        ShiftBadge.DAY -> Color(0xFF1E4E8C)
        ShiftBadge.NIGHT -> Color(0xFF9A5400)
        ShiftBadge.DUTY -> Color(0xFF8A3E00)
        ShiftBadge.OFF -> Color(0xFF4F6375)
        ShiftBadge.REST -> Color(0xFF5B6670)
        ShiftBadge.WORK -> Color(0xFF4D6B5C)
    }
}




