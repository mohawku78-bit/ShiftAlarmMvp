package com.example.shiftalarmmvp.receiver

import android.content.Intent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BootReceiverTest {
    @Test
    fun `boot completed is treated as reschedule action`() {
        assertTrue(shouldRescheduleOnAction(Intent.ACTION_BOOT_COMPLETED))
    }

    @Test
    fun `time set is treated as reschedule action`() {
        assertTrue(shouldRescheduleOnAction("android.intent.action.TIME_SET"))
    }

    @Test
    fun `date changed is treated as reschedule action`() {
        assertTrue(shouldRescheduleOnAction(Intent.ACTION_DATE_CHANGED))
    }

    @Test
    fun `timezone changed is treated as reschedule action`() {
        assertTrue(shouldRescheduleOnAction(Intent.ACTION_TIMEZONE_CHANGED))
    }

    @Test
    fun `random actions are not reschedule actions`() {
        assertFalse(shouldRescheduleOnAction("android.intent.action.AIRPLANE_MODE"))
    }
}
