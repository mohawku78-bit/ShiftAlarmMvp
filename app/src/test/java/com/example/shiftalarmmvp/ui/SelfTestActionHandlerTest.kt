package com.example.shiftalarmmvp.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class SelfTestActionHandlerTest {

    @Test
    fun `formatSelfTestTriggerTime includes seconds for exact 2 minute trigger`() {
        val triggerAtMillis = ZonedDateTime.of(2026, 3, 12, 10, 2, 34, 0, ZoneId.of("Asia/Seoul"))
            .toInstant()
            .toEpochMilli()

        val formatted = formatSelfTestTriggerTime(
            triggerAtMillis = triggerAtMillis,
            zoneId = ZoneId.of("Asia/Seoul")
        )

        assertEquals("10:02:34", formatted)
    }
}
