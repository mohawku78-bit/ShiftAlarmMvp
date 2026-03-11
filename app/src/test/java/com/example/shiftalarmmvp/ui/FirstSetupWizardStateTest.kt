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
    fun `normalized clamps indexes`() {
        val state = FirstSetupWizardState(
            step = 99,
            selectedStep1TypeIndex = 7,
            selectedAlarmSlot = 4,
            selectedTemplateId = QUICK_TEMPLATE_ID_DAY_DUTY_OFF
        )

        val normalized = state.normalized(workTypeCount = 2)

        assertEquals(FIRST_SETUP_WIZARD_LAST_STEP, normalized.step)
        assertEquals(1, normalized.selectedStep1TypeIndex)
        assertEquals(1, normalized.selectedAlarmSlot)
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
        val restored = FirstSetupWizardState.Saver.restore(listOf(0, false, false, 0, 0, ""))

        assertNull(restored?.selectedTemplateId)
    }

    @Test
    fun `preset step0 requires template before advance`() {
        assertFalse(
            canAdvanceFirstSetupWizard(
                step = 0,
                usesCustomPattern = false,
                hasCustomRotation = false,
                hasPresetTemplate = false
            )
        )
        assertTrue(
            canAdvanceFirstSetupWizard(
                step = 0,
                usesCustomPattern = false,
                hasCustomRotation = false,
                hasPresetTemplate = true
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
                hasPresetTemplate = true
            )
        )
        assertTrue(
            canAdvanceFirstSetupWizard(
                step = 0,
                usesCustomPattern = true,
                hasCustomRotation = true,
                hasPresetTemplate = false
            )
        )
        assertFalse(shouldApplyQuickTemplateOnAdvance(step = 0, usesCustomPattern = true))
    }

    @Test
    fun `later steps can advance without template gate`() {
        assertTrue(
            canAdvanceFirstSetupWizard(
                step = 1,
                usesCustomPattern = false,
                hasCustomRotation = false,
                hasPresetTemplate = false
            )
        )
        assertFalse(shouldApplyQuickTemplateOnAdvance(step = 2, usesCustomPattern = false))
    }
}