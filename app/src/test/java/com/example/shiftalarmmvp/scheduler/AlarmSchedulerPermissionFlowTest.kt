package com.example.shiftalarmmvp.scheduler

import android.os.Build
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmSchedulerPermissionFlowTest {
    @Test
    fun `pre-S devices always allow exact path`() {
        assertTrue(shouldUseExactAlarm(Build.VERSION_CODES.R, canScheduleExact = false))
    }

    @Test
    fun `S plus uses exact path when permission granted`() {
        assertTrue(shouldUseExactAlarm(Build.VERSION_CODES.S, canScheduleExact = true))
        assertTrue(shouldUseExactAlarm(Build.VERSION_CODES.UPSIDE_DOWN_CAKE, canScheduleExact = true))
    }

    @Test
    fun `S plus disables exact path when permission denied`() {
        assertFalse(shouldUseExactAlarm(Build.VERSION_CODES.S, canScheduleExact = false))
        assertFalse(shouldUseExactAlarm(Build.VERSION_CODES.UPSIDE_DOWN_CAKE, canScheduleExact = false))
    }
}
