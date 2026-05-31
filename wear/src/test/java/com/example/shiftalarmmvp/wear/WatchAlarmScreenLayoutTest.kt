package com.example.shiftalarmmvp.wear

import org.junit.Assert.assertTrue
import org.junit.Test

class WatchAlarmScreenLayoutTest {
    @Test
    fun roundScreensUseNarrowCenteredSafeWidthForActionButtons() {
        val spec = WatchAlarmScreenLayout.forScreen(
            isRound = true,
            screenWidthPx = 450,
            screenHeightPx = 450,
            density = 1f
        )

        assertTrue("round content should stay inside the circle's safer center column", spec.contentWidthPx <= 315)
        assertTrue("round horizontal padding should avoid clipped side buttons", spec.outerHorizontalPaddingPx >= 44)
        assertTrue("round vertical padding should keep snooze above the bottom curve", spec.outerVerticalPaddingPx >= 30)
        assertTrue("round layout should leave extra bottom breathing room", spec.bottomBreathingRoomPx >= 16)
    }

    @Test
    fun compactRoundScreensShrinkTextAndButtonsToKeepSnoozeVisible() {
        val spec = WatchAlarmScreenLayout.forScreen(
            isRound = true,
            screenWidthPx = 320,
            screenHeightPx = 320,
            density = 1f
        )

        val actionBlockHeight = spec.stopTopMarginPx +
            spec.buttonHeightPx +
            spec.buttonGapPx +
            spec.buttonHeightPx +
            spec.bottomBreathingRoomPx

        assertTrue("compact round content should be narrower than 70 percent of the dial", spec.contentWidthPx <= 212)
        assertTrue("compact round button height should reduce vertical pressure", spec.buttonHeightPx <= 40)
        assertTrue("compact labels should not push snooze below the curve", spec.maxLabelLines == 1)
        assertTrue("button block should leave room for alarm text above it", actionBlockHeight <= 115)
    }

    @Test
    fun squareScreensCanUseMoreWidthThanRoundScreens() {
        val square = WatchAlarmScreenLayout.forScreen(
            isRound = false,
            screenWidthPx = 450,
            screenHeightPx = 450,
            density = 1f
        )
        val round = WatchAlarmScreenLayout.forScreen(
            isRound = true,
            screenWidthPx = 450,
            screenHeightPx = 450,
            density = 1f
        )

        assertTrue(square.contentWidthPx > round.contentWidthPx)
        assertTrue(square.bottomBreathingRoomPx < round.bottomBreathingRoomPx)
    }
}
