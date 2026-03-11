package com.example.shiftalarmmvp.ui

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver

internal const val FIRST_SETUP_WIZARD_LAST_STEP = 2

data class FirstSetupWizardState(
    val step: Int = 0,
    val showAdvanced: Boolean = false,
    val showStep1Advanced: Boolean = false,
    val selectedStep1TypeIndex: Int = 0,
    val selectedAlarmSlot: Int = 0,
    val selectedTemplateId: String? = null,
) {
    fun back(): FirstSetupWizardState {
        return copy(
            step = (step - 1).coerceAtLeast(0),
            showAdvanced = false
        )
    }

    fun next(): FirstSetupWizardState {
        return copy(
            step = (step + 1).coerceAtMost(FIRST_SETUP_WIZARD_LAST_STEP),
            showAdvanced = false
        )
    }

    fun normalized(workTypeCount: Int): FirstSetupWizardState {
        val normalizedIndex = if (workTypeCount == 0) {
            0
        } else {
            selectedStep1TypeIndex.coerceIn(0, workTypeCount - 1)
        }
        return copy(
            step = step.coerceIn(0, FIRST_SETUP_WIZARD_LAST_STEP),
            selectedStep1TypeIndex = normalizedIndex,
            selectedAlarmSlot = selectedAlarmSlot.coerceIn(0, 1)
        )
    }

    companion object {
        val Saver: Saver<FirstSetupWizardState, Any> = listSaver(
            save = {
                listOf(
                    it.step,
                    it.showAdvanced,
                    it.showStep1Advanced,
                    it.selectedStep1TypeIndex,
                    it.selectedAlarmSlot,
                    it.selectedTemplateId ?: ""
                )
            },
            restore = { values ->
                FirstSetupWizardState(
                    step = values[0] as Int,
                    showAdvanced = values[1] as Boolean,
                    showStep1Advanced = values[2] as Boolean,
                    selectedStep1TypeIndex = values[3] as Int,
                    selectedAlarmSlot = values[4] as Int,
                    selectedTemplateId = (values[5] as String).ifBlank { null }
                )
            }
        )
    }
}

internal fun canAdvanceFirstSetupWizard(
    step: Int,
    usesCustomPattern: Boolean,
    hasCustomRotation: Boolean,
    hasPresetTemplate: Boolean
): Boolean {
    return if (step != 0) {
        true
    } else if (usesCustomPattern) {
        hasCustomRotation
    } else {
        hasPresetTemplate
    }
}

internal fun shouldApplyQuickTemplateOnAdvance(
    step: Int,
    usesCustomPattern: Boolean
): Boolean = step == 0 && !usesCustomPattern