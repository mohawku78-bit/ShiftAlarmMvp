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
    fun `time changed is treated as reschedule action`() {
        assertTrue(shouldRescheduleOnAction(Intent.ACTION_TIME_CHANGED))
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
    fun `package replaced is treated as reschedule action`() {
        assertTrue(shouldRescheduleOnAction(Intent.ACTION_MY_PACKAGE_REPLACED))
    }

    @Test
    fun `random actions are not reschedule actions`() {
        assertFalse(shouldRescheduleOnAction("android.intent.action.AIRPLANE_MODE"))
    }
}
