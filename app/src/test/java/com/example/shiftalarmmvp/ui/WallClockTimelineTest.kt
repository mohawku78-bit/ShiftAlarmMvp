package com.example.shiftalarmmvp.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class WallClockTimelineTest {
    @Test
    fun `timeline refreshes when local minute changes`() {
        val previous = captureWallClockTimelineSnapshot(
            now = LocalDateTime.of(2026, 3, 12, 9, 14, 0),
            zoneId = ZoneId.of("Asia/Seoul")
        )
        val current = captureWallClockTimelineSnapshot(
            now = LocalDateTime.of(2026, 3, 12, 9, 15, 0),
            zoneId = ZoneId.of("Asia/Seoul")
        )

        assertTrue(shouldRefreshWallClockTimeline(previous, current))
    }

    @Test
    fun `timeline refreshes when timezone changes within same local minute`() {
        val previous = captureWallClockTimelineSnapshot(
            now = LocalDateTime.of(2026, 3, 12, 9, 14, 0),
            zoneId = ZoneId.of("Asia/Seoul")
        )
        val current = captureWallClockTimelineSnapshot(
            now = LocalDateTime.of(2026, 3, 12, 9, 14, 0),
            zoneId = ZoneId.of("America/New_York")
        )

        assertTrue(shouldRefreshWallClockTimeline(previous, current))
    }

    @Test
    fun `timeline stays stable within same minute and timezone`() {
        val previous = captureWallClockTimelineSnapshot(
            now = LocalDateTime.of(2026, 3, 12, 9, 14, 5),
            zoneId = ZoneId.of("Asia/Seoul")
        )
        val current = captureWallClockTimelineSnapshot(
            now = LocalDateTime.of(2026, 3, 12, 9, 14, 59),
            zoneId = ZoneId.of("Asia/Seoul")
        )

        assertFalse(shouldRefreshWallClockTimeline(previous, current))
    }

    @Test
    fun `next timeline delay targets next minute boundary`() {
        val delayMillis = nextWallClockTimelineDelayMillis(
            LocalDateTime.of(2026, 3, 12, 9, 14, 10, 500_000_000)
        )

        assertEquals(49_500L, delayMillis)
    }
}