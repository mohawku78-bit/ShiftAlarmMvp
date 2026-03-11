package com.example.shiftalarmmvp.ui

import java.time.LocalDate

data class ShiftSetupDefaults(
    val defaultRotationTypes: List<String>,
    val initialCategory: ShiftCategory = ShiftCategory.THREE_SHIFT,
    val initialAnchorDate: LocalDate = LocalDate.now()
) {
    val normalizedDefaultRotationTypes: List<String> =
        defaultRotationTypes.map(::normalizeWorkType).filter { it.isNotBlank() }.ifEmpty {
            listOf(WORK_TYPE_DAY, WORK_TYPE_NIGHT, WORK_TYPE_OFF, WORK_TYPE_REST)
        }

    val defaultRestType: String = normalizedDefaultRotationTypes.getOrNull(3) ?: WORK_TYPE_REST
}

data class ShiftSetupDraft(
    val wizardState: FirstSetupWizardState,
    val selectedCategory: ShiftCategory,
    val customWorkTypeInput: String,
    val rotationSequence: List<String>,
    val todayRotationIndex: Int,
    val workTypeConfigs: List<WorkTypeAlarmConfig>,
    val infiniteRotationEnabled: Boolean,
    val anchorDate: LocalDate,
    val autoBuildFeedback: String
) {
    fun normalized(): ShiftSetupDraft {
        val normalizedRotation = rotationSequence.map(::normalizeWorkType).filter { it.isNotBlank() }
        val normalizedConfigs = workTypeConfigs.mapNotNull { config ->
            val normalizedType = normalizeWorkType(config.type)
            if (normalizedType.isBlank()) {
                null
            } else {
                config.copy(type = normalizedType)
            }
        }
        val maxRotationIndex = (normalizedRotation.size - 1).coerceAtLeast(0)
        return copy(
            wizardState = wizardState.normalized(normalizedConfigs.size),
            customWorkTypeInput = customWorkTypeInput.take(12),
            rotationSequence = normalizedRotation,
            todayRotationIndex = todayRotationIndex.coerceIn(0, maxRotationIndex),
            workTypeConfigs = normalizedConfigs
        )
    }

    fun toAutoBuildInput(): ShiftAutoBuildInput {
        val normalized = normalized()
        return ShiftAutoBuildInput(
            rotationSequence = normalized.rotationSequence,
            todayRotationIndex = normalized.todayRotationIndex,
            workTypeConfigs = normalized.workTypeConfigs,
            infiniteRotationEnabled = normalized.infiniteRotationEnabled,
            anchorDate = normalized.anchorDate
        )
    }
}

data class ShiftAutoBuildInput(
    val rotationSequence: List<String>,
    val todayRotationIndex: Int,
    val workTypeConfigs: List<WorkTypeAlarmConfig>,
    val infiniteRotationEnabled: Boolean,
    val anchorDate: LocalDate
)

sealed interface ShiftSetupAction {
    data object Back : ShiftSetupAction
    data object Next : ShiftSetupAction
    data object ReopenWizard : ShiftSetupAction
    data object ToggleAdvancedPanel : ShiftSetupAction
    data object ToggleStep1Advanced : ShiftSetupAction
    data object DropLastRotation : ShiftSetupAction
    data object ClearRotation : ShiftSetupAction
    data class SelectCategory(val category: ShiftCategory, val selectedTemplateId: String?) : ShiftSetupAction
    data class SelectTemplate(val templateId: String?) : ShiftSetupAction
    data class UpdateCustomWorkTypeInput(val value: String) : ShiftSetupAction
    data class AppendRotationType(val type: String, val feedback: String) : ShiftSetupAction
    data class ResetDefaults(val feedback: String) : ShiftSetupAction
    data class AddType(
        val emptyFeedback: String,
        val existsFeedback: String,
        val addedFeedback: String
    ) : ShiftSetupAction

    data class DeleteConfigType(val type: String) : ShiftSetupAction
    data class ToggleConfigEnabled(val index: Int, val enabled: Boolean) : ShiftSetupAction
    data class ChangePrimaryTime(val index: Int, val value: String) : ShiftSetupAction
    data class ChangeSecondaryTime(val index: Int, val value: String) : ShiftSetupAction
    data class SetInfiniteRotationEnabled(val enabled: Boolean) : ShiftSetupAction
    data class SelectStep1Type(val index: Int) : ShiftSetupAction
    data class SelectAlarmSlot(val slot: Int) : ShiftSetupAction
    data class ChangeAnchorDate(val date: LocalDate) : ShiftSetupAction
    data class ChangeTodayRotationIndex(val index: Int) : ShiftSetupAction
    data class ApplyQuickTemplate(val template: QuickShiftTemplate, val feedback: String) : ShiftSetupAction
    data class SetAutoBuildFeedback(val feedback: String) : ShiftSetupAction
}

fun createInitialShiftSetupDraft(defaults: ShiftSetupDefaults): ShiftSetupDraft {
    return ShiftSetupDraft(
        wizardState = FirstSetupWizardState(),
        selectedCategory = defaults.initialCategory,
        customWorkTypeInput = "",
        rotationSequence = defaults.normalizedDefaultRotationTypes,
        todayRotationIndex = 0,
        workTypeConfigs = defaultWorkTypeConfigs(defaults.normalizedDefaultRotationTypes),
        infiniteRotationEnabled = true,
        anchorDate = defaults.initialAnchorDate,
        autoBuildFeedback = ""
    ).normalized()
}

fun ShiftSetupDraft.reduce(action: ShiftSetupAction, defaults: ShiftSetupDefaults): ShiftSetupDraft {
    val normalized = normalized()
    return when (action) {
        ShiftSetupAction.Back -> normalized.copy(wizardState = normalized.wizardState.back())
        ShiftSetupAction.Next -> normalized.copy(wizardState = normalized.wizardState.next())
        ShiftSetupAction.ReopenWizard -> createInitialShiftSetupDraft(defaults)
        ShiftSetupAction.ToggleAdvancedPanel -> normalized.copy(
            wizardState = normalized.wizardState.copy(showAdvanced = !normalized.wizardState.showAdvanced)
        )

        ShiftSetupAction.ToggleStep1Advanced -> normalized.copy(
            wizardState = normalized.wizardState.copy(
                showStep1Advanced = !normalized.wizardState.showStep1Advanced
            )
        )

        ShiftSetupAction.DropLastRotation -> normalized.copy(
            rotationSequence = normalized.rotationSequence.dropLast(1)
        ).normalized()

        ShiftSetupAction.ClearRotation -> normalized.copy(
            rotationSequence = emptyList(),
            todayRotationIndex = 0
        )

        is ShiftSetupAction.SelectCategory -> normalized.copy(
            selectedCategory = action.category,
            wizardState = normalized.wizardState.copy(
                selectedTemplateId = if (action.category == ShiftCategory.CUSTOM) {
                    null
                } else {
                    action.selectedTemplateId
                }
            )
        )

        is ShiftSetupAction.SelectTemplate -> normalized.copy(
            wizardState = normalized.wizardState.copy(selectedTemplateId = action.templateId)
        )

        is ShiftSetupAction.UpdateCustomWorkTypeInput -> normalized.copy(
            customWorkTypeInput = action.value.take(12)
        )

        is ShiftSetupAction.AppendRotationType -> {
            val type = normalizeWorkType(action.type)
            if (type.isBlank()) {
                normalized
            } else {
                normalized.copy(
                    rotationSequence = normalized.rotationSequence + type,
                    autoBuildFeedback = action.feedback
                )
            }
        }

        is ShiftSetupAction.ResetDefaults -> normalized.copy(
            selectedCategory = ShiftCategory.THREE_SHIFT,
            customWorkTypeInput = "",
            rotationSequence = defaults.normalizedDefaultRotationTypes,
            todayRotationIndex = 0,
            workTypeConfigs = defaultWorkTypeConfigs(defaults.normalizedDefaultRotationTypes),
            infiniteRotationEnabled = true,
            autoBuildFeedback = action.feedback,
            wizardState = normalized.wizardState.copy(selectedTemplateId = null)
        ).normalized()

        is ShiftSetupAction.AddType -> {
            val name = normalizeWorkType(normalized.customWorkTypeInput)
            when {
                name.isBlank() -> normalized.copy(autoBuildFeedback = action.emptyFeedback)
                normalized.workTypeConfigs.any { normalizeWorkType(it.type) == name } -> {
                    normalized.copy(autoBuildFeedback = action.existsFeedback)
                }

                else -> normalized.copy(
                    workTypeConfigs = normalized.workTypeConfigs + WorkTypeAlarmConfig(
                        type = name,
                        enabled = true,
                        primaryTime = defaultPrimaryTime(name),
                        secondaryTime = ""
                    ),
                    customWorkTypeInput = "",
                    autoBuildFeedback = action.addedFeedback
                )
            }
        }

        is ShiftSetupAction.DeleteConfigType -> normalized.copy(
            workTypeConfigs = normalized.workTypeConfigs.filterNot { it.type == action.type },
            rotationSequence = normalized.rotationSequence.filterNot { it == action.type }
        ).normalized()

        is ShiftSetupAction.ToggleConfigEnabled -> normalized.updateConfig(action.index) {
            it.copy(enabled = action.enabled)
        }

        is ShiftSetupAction.ChangePrimaryTime -> normalized.updateConfig(action.index) {
            it.copy(primaryTime = action.value)
        }

        is ShiftSetupAction.ChangeSecondaryTime -> normalized.updateConfig(action.index) {
            it.copy(secondaryTime = action.value)
        }

        is ShiftSetupAction.SetInfiniteRotationEnabled -> normalized.copy(
            infiniteRotationEnabled = action.enabled
        )

        is ShiftSetupAction.SelectStep1Type -> normalized.copy(
            wizardState = normalized.wizardState.copy(selectedStep1TypeIndex = action.index)
        ).normalized()

        is ShiftSetupAction.SelectAlarmSlot -> normalized.copy(
            wizardState = normalized.wizardState.copy(selectedAlarmSlot = action.slot)
        ).normalized()

        is ShiftSetupAction.ChangeAnchorDate -> normalized.copy(anchorDate = action.date)
        is ShiftSetupAction.ChangeTodayRotationIndex -> normalized.copy(todayRotationIndex = action.index).normalized()
        is ShiftSetupAction.ApplyQuickTemplate -> {
            val normalizedSequence = action.template.sequence.map(::normalizeWorkType).filter { it.isNotBlank() }
            val configTypes = (normalizedSequence + listOf(defaults.defaultRestType)).distinct()
            normalized.copy(
                selectedCategory = action.template.category,
                rotationSequence = normalizedSequence,
                todayRotationIndex = 0,
                workTypeConfigs = defaultWorkTypeConfigs(configTypes),
                infiniteRotationEnabled = true,
                autoBuildFeedback = action.feedback,
                wizardState = normalized.wizardState.copy(selectedTemplateId = action.template.id)
            ).normalized()
        }

        is ShiftSetupAction.SetAutoBuildFeedback -> normalized.copy(autoBuildFeedback = action.feedback)
    }
}

private fun ShiftSetupDraft.updateConfig(
    index: Int,
    transform: (WorkTypeAlarmConfig) -> WorkTypeAlarmConfig
): ShiftSetupDraft {
    if (index !in workTypeConfigs.indices) return this
    val copy = workTypeConfigs.toMutableList()
    copy[index] = transform(copy[index])
    return copy(workTypeConfigs = copy).normalized()
}

object ShiftSetupTestTags {
    const val WIZARD_ROOT = "shift_setup_wizard_root"
    const val STEP_LABEL = "shift_setup_step_label"
    const val PREVIOUS_BUTTON = "shift_setup_previous"
    const val NEXT_BUTTON = "shift_setup_next"
    const val CLOSE_BUTTON = "shift_setup_close"
    const val COMPLETE_BUTTON = "shift_setup_complete"
    const val CATEGORY_REPRESENTATIVE = "shift_setup_category_representative"
    const val CATEGORY_CUSTOM = "shift_setup_category_custom"
    const val ALARM_SLOT_PRIMARY = "shift_setup_alarm_slot_primary"
    const val ALARM_SLOT_SECONDARY = "shift_setup_alarm_slot_secondary"

    fun template(index: Int): String = "shift_setup_template_$index"

    fun rotationType(index: Int): String = "shift_setup_rotation_type_$index"

    fun workType(index: Int): String = "shift_setup_work_type_$index"
}
