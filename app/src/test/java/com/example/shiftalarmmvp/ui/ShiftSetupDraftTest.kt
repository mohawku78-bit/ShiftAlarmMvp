package com.example.shiftalarmmvp.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ShiftSetupDraftTest {

    private val defaults = ShiftSetupDefaults(
        defaultRotationTypes = listOf(WORK_TYPE_DAY, WORK_TYPE_NIGHT, WORK_TYPE_OFF, WORK_TYPE_REST)
    )

    private val representativeTemplate = QUICK_SHIFT_TEMPLATES.first { it.id == QUICK_TEMPLATE_ID_DAY_DUTY_OFF }

    @Test
    fun `back action walks back steps without clearing selections`() {
        val configured = createInitialShiftSetupDraft(defaults)
            .reduce(ShiftSetupAction.SelectCategory(ShiftCategory.THREE_SHIFT, representativeTemplate.id), defaults)
            .reduce(ShiftSetupAction.ApplyQuickTemplate(representativeTemplate, "applied"), defaults)
            .reduce(ShiftSetupAction.Next, defaults)
            .reduce(ShiftSetupAction.SelectStep1Type(1), defaults)
            .reduce(ShiftSetupAction.ToggleStep1Advanced, defaults)
            .reduce(ShiftSetupAction.SelectAlarmSlot(1), defaults)
            .reduce(ShiftSetupAction.ChangeSecondaryTime(1, "22:30"), defaults)
            .reduce(ShiftSetupAction.Next, defaults)

        val backToStep1 = configured.reduce(ShiftSetupAction.Back, defaults)
        val backToStep0 = backToStep1.reduce(ShiftSetupAction.Back, defaults)

        assertEquals(1, backToStep1.wizardState.step)
        assertEquals(1, backToStep1.wizardState.selectedStep1TypeIndex)
        assertEquals(1, backToStep1.wizardState.selectedAlarmSlot)
        assertEquals("22:30", backToStep1.workTypeConfigs[1].secondaryTime)

        assertEquals(0, backToStep0.wizardState.step)
        assertEquals(representativeTemplate.id, backToStep0.wizardState.selectedTemplateId)
        assertEquals(representativeTemplate.sequence, backToStep0.rotationSequence)
    }

    @Test
    fun `custom rotation survives wizard round trip`() {
        val draft = createInitialShiftSetupDraft(defaults)
            .reduce(ShiftSetupAction.SelectCategory(ShiftCategory.CUSTOM, null), defaults)
            .reduce(ShiftSetupAction.ClearRotation, defaults)
            .reduce(ShiftSetupAction.AppendRotationType(WORK_TYPE_DAY, "day"), defaults)
            .reduce(ShiftSetupAction.AppendRotationType(WORK_TYPE_OFF, "off"), defaults)
            .reduce(ShiftSetupAction.Next, defaults)
            .reduce(ShiftSetupAction.Next, defaults)
            .reduce(ShiftSetupAction.Back, defaults)
            .reduce(ShiftSetupAction.Back, defaults)

        assertEquals(0, draft.wizardState.step)
        assertEquals(ShiftCategory.CUSTOM, draft.selectedCategory)
        assertEquals(listOf(WORK_TYPE_DAY, WORK_TYPE_OFF), draft.rotationSequence)
    }

    @Test
    fun `reopen resets draft while regular navigation keeps values`() {
        val changed = createInitialShiftSetupDraft(defaults)
            .reduce(ShiftSetupAction.SelectCategory(ShiftCategory.CUSTOM, null), defaults)
            .reduce(ShiftSetupAction.UpdateCustomWorkTypeInput("Late"), defaults)
            .reduce(ShiftSetupAction.ClearRotation, defaults)
            .reduce(ShiftSetupAction.AppendRotationType(WORK_TYPE_DAY, "added"), defaults)
            .reduce(ShiftSetupAction.Next, defaults)

        val kept = changed.reduce(ShiftSetupAction.Back, defaults)
        val reopened = changed.reduce(ShiftSetupAction.ReopenWizard, defaults)

        assertEquals("Late", kept.customWorkTypeInput)
        assertEquals(listOf(WORK_TYPE_DAY), kept.rotationSequence)
        assertEquals(0, kept.wizardState.step)

        assertEquals(defaults.initialCategory, reopened.selectedCategory)
        assertEquals(defaults.normalizedDefaultRotationTypes, reopened.rotationSequence)
        assertEquals("", reopened.customWorkTypeInput)
        assertEquals(0, reopened.wizardState.step)
    }

    @Test
    fun `auto build input reflects current draft state`() {
        val targetDate = LocalDate.of(2026, 3, 12)
        val draft = createInitialShiftSetupDraft(defaults)
            .reduce(ShiftSetupAction.SelectCategory(ShiftCategory.THREE_SHIFT, representativeTemplate.id), defaults)
            .reduce(ShiftSetupAction.ApplyQuickTemplate(representativeTemplate, "applied"), defaults)
            .reduce(ShiftSetupAction.ChangeTodayRotationIndex(2), defaults)
            .reduce(ShiftSetupAction.ChangeAnchorDate(targetDate), defaults)
            .reduce(ShiftSetupAction.SetInfiniteRotationEnabled(false), defaults)

        val input = draft.toAutoBuildInput()

        assertEquals(representativeTemplate.sequence, input.rotationSequence)
        assertEquals(2, input.todayRotationIndex)
        assertEquals(targetDate, input.anchorDate)
        assertFalse(input.infiniteRotationEnabled)
        assertTrue(input.workTypeConfigs.isNotEmpty())
    }
}

