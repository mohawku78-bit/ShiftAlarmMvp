package com.example.shiftalarmmvp.ui

import android.content.Context
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.shiftalarmmvp.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

@RunWith(AndroidJUnit4::class)
class ShiftSetupWizardTest {

    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: TestRule = RuleChain
        .outerRule(ClearFirstSetupPrefsRule())
        .around(composeRule)

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun representativeWizardBackNavigatesToPreviousSteps() {
        composeRule.onNodeWithTag(ShiftSetupTestTags.template(0)).performClick()
        composeRule.onNodeWithTag(ShiftSetupTestTags.NEXT_BUTTON).assertIsEnabled().performClick()
        composeRule.onNodeWithTag(ShiftSetupTestTags.workType(1)).performClick()
        composeRule.onNodeWithTag(ShiftSetupTestTags.NEXT_BUTTON).performClick()

        Espresso.pressBack()
        composeRule.assertTagExists(ShiftSetupTestTags.workType(1))

        Espresso.pressBack()
        composeRule.assertTagExists(ShiftSetupTestTags.template(0))
    }

    @Test
    fun customRotationRemainsAfterReturningToStepZero() {
        composeRule.onNodeWithTag(ShiftSetupTestTags.CATEGORY_CUSTOM).performClick()
        composeRule.onNodeWithTag(ShiftSetupTestTags.rotationType(0)).performClick()
        composeRule.onNodeWithTag(ShiftSetupTestTags.NEXT_BUTTON).assertIsEnabled().performClick()

        Espresso.pressBack()
        composeRule.onNodeWithTag(ShiftSetupTestTags.NEXT_BUTTON).assertIsEnabled()
    }

    @Test
    fun secondaryAlarmSelectionPersistsAfterStepRoundTrip() {
        val showSecondary = context.getString(R.string.shift_show_secondary_settings)
        val secondaryOn = context.getString(
            R.string.shift_secondary_alarm_status_format,
            context.getString(R.string.common_on)
        )

        composeRule.onNodeWithTag(ShiftSetupTestTags.template(0)).performClick()
        composeRule.onNodeWithTag(ShiftSetupTestTags.NEXT_BUTTON).assertIsEnabled().performClick()
        composeRule.onNodeWithTag(ShiftSetupTestTags.workType(1)).performClick()
        composeRule.onNodeWithText(showSecondary).performClick()
        composeRule.onNodeWithTag(ShiftSetupTestTags.ALARM_SLOT_SECONDARY).performClick()
        composeRule.onNodeWithTag(ShiftSetupTestTags.NEXT_BUTTON).performClick()

        Espresso.pressBack()
        composeRule.assertTagExists(ShiftSetupTestTags.ALARM_SLOT_SECONDARY)
        composeRule.assertTextExists(secondaryOn)
    }

    @Test
    fun completingWizardClosesWizardCard() {
        composeRule.onNodeWithTag(ShiftSetupTestTags.template(0)).performClick()
        composeRule.onNodeWithTag(ShiftSetupTestTags.NEXT_BUTTON).assertIsEnabled().performClick()
        composeRule.onNodeWithTag(ShiftSetupTestTags.NEXT_BUTTON).performClick()
        composeRule.onNodeWithTag(ShiftSetupTestTags.COMPLETE_BUTTON).performClick()

        composeRule.assertTagDoesNotExist(ShiftSetupTestTags.WIZARD_ROOT)
    }

    @Test
    fun stepZeroUsesActivityBackInsteadOfWizardBack() {
        composeRule.assertTagExists(ShiftSetupTestTags.WIZARD_ROOT)

        Espresso.pressBackUnconditionally()

        composeRule.waitUntil(5_000) {
            composeRule.activityRule.scenario.state == Lifecycle.State.DESTROYED
        }
        assertEquals(Lifecycle.State.DESTROYED, composeRule.activityRule.scenario.state)
    }
}

private class ClearFirstSetupPrefsRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                val context = ApplicationProvider.getApplicationContext<Context>()
                context.getSharedPreferences("first_setup_wizard", Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .commit()
                base.evaluate()
            }
        }
    }
}

private fun AndroidComposeTestRule<*, *>.assertTagExists(tag: String) {
    waitForIdle()
    assertTrue(onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
}

private fun AndroidComposeTestRule<*, *>.assertTagDoesNotExist(tag: String) {
    waitForIdle()
    assertTrue(onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
}

private fun AndroidComposeTestRule<*, *>.assertTextExists(text: String) {
    waitForIdle()
    assertTrue(onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
}
