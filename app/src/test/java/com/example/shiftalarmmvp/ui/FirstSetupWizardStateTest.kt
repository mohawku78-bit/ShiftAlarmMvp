package com.example.shiftalarmmvp.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstSetupWizardStateTest {

    @Test
    fun `back moves to previous step and closes advanced pane`() {
        val state = FirstSetupWizardState(step = 2, showAdvanced = true, showStep1Advanced = true)

        val updated = state.back()

        assertEquals(1, updated.step)
        assertFalse(updated.showAdvanced)
        assertTrue(updated.showStep1Advanced)
    }

    @Test
    fun `next is capped at last wizard step`() {
        val updated = FirstSetupWizardState(step = FIRST_SETUP_WIZARD_LAST_STEP).next()

        assertEquals(FIRST_SETUP_WIZARD_LAST_STEP, updated.step)
        assertFalse(updated.showAdvanced)
    }

    @Test
    fun `back keeps step1 advanced open when secondary slot is selected`() {
        val updated = FirstSetupWizardState(
            step = 2,
            showStep1Advanced = false,
            selectedAlarmSlot = 1
        ).back()

        assertEquals(1, updated.step)
        assertTrue(updated.showStep1Advanced)
    }

    @Test
    fun `normalized clamps indexes but preserves unselected work type`() {
        val state = FirstSetupWizardState(
            step = 99,
            selectedStep1TypeIndex = 7,
            selectedAlarmSlot = 4,
            selectedTemplateId = QUICK_TEMPLATE_ID_DAY_DUTY_OFF
        )

        val normalized = state.normalized(workTypeCount = 2)

        assertEquals(FIRST_SETUP_WIZARD_LAST_STEP, normalized.step)
        assertEquals(-1, normalized.selectedStep1TypeIndex)
        assertEquals(1, normalized.selectedAlarmSlot)
    }

    @Test
    fun `normalized keeps selected work type index in range`() {
        val normalized = FirstSetupWizardState(selectedStep1TypeIndex = 1).normalized(workTypeCount = 2)

        assertEquals(1, normalized.selectedStep1TypeIndex)
    }

    @Test
    fun `saver restores template selection`() {
        val restored = FirstSetupWizardState.Saver.restore(
            listOf(1, false, true, 1, 1, QUICK_TEMPLATE_ID_DAY_DUTY_OFF)
        )

        assertEquals(QUICK_TEMPLATE_ID_DAY_DUTY_OFF, restored?.selectedTemplateId)
        assertTrue(restored?.showStep1Advanced == true)
        assertEquals(1, restored?.selectedAlarmSlot)
    }

    @Test
    fun `saver restores blank template as null`() {
        val restored = FirstSetupWizardState.Saver.restore(listOf(0, false, false, -1, 0, ""))

        assertNull(restored?.selectedTemplateId)
        assertEquals(-1, restored?.selectedStep1TypeIndex)
    }

    @Test
    fun `preset step0 requires template before advance`() {
        assertFalse(
            canAdvanceFirstSetupWizard(
                step = 0,
                usesCustomPattern = false,
                hasCustomRotation = false,
                hasPresetTemplate = false,
                hasSelectedWorkType = false
            )
        )
        assertTrue(
            canAdvanceFirstSetupWizard(
                step = 0,
                usesCustomPattern = false,
                hasCustomRotation = false,
                hasPresetTemplate = true,
                hasSelectedWorkType = false
            )
        )
        assertTrue(shouldApplyQuickTemplateOnAdvance(step = 0, usesCustomPattern = false))
    }

    @Test
    fun `custom step0 requires rotation before advance`() {
        assertFalse(
            canAdvanceFirstSetupWizard(
                step = 0,
                usesCustomPattern = true,
                hasCustomRotation = false,
                hasPresetTemplate = true,
                hasSelectedWorkType = false
            )
        )
        assertTrue(
            canAdvanceFirstSetupWizard(
                step = 0,
                usesCustomPattern = true,
                hasCustomRotation = true,
                hasPresetTemplate = false,
                hasSelectedWorkType = false
            )
        )
        assertFalse(shouldApplyQuickTemplateOnAdvance(step = 0, usesCustomPattern = true))
    }

    @Test
    fun `step1 requires an explicit work type selection`() {
        assertFalse(
            canAdvanceFirstSetupWizard(
                step = 1,
                usesCustomPattern = false,
                hasCustomRotation = false,
                hasPresetTemplate = true,
                hasSelectedWorkType = false
            )
        )
        assertTrue(
            canAdvanceFirstSetupWizard(
                step = 1,
                usesCustomPattern = false,
                hasCustomRotation = false,
                hasPresetTemplate = true,
                hasSelectedWorkType = true
            )
        )
    }

    @Test
    fun `later steps can advance without template gate`() {
        assertTrue(
            canAdvanceFirstSetupWizard(
                step = 2,
                usesCustomPattern = false,
                hasCustomRotation = false,
                hasPresetTemplate = false,
                hasSelectedWorkType = false
            )
        )
        assertFalse(shouldApplyQuickTemplateOnAdvance(step = 2, usesCustomPattern = false))
    }
}
