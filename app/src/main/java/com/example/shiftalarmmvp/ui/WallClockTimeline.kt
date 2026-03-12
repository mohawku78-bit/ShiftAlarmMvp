package com.example.shiftalarmmvp.ui

import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

internal data class WallClockTimelineSnapshot(
    val now: LocalDateTime,
    val zoneId: ZoneId
) {
    val localMinute: LocalDateTime
        get() = now.truncatedTo(ChronoUnit.MINUTES)
}

internal fun captureWallClockTimelineSnapshot(
    now: LocalDateTime = LocalDateTime.now(),
    zoneId: ZoneId = ZoneId.systemDefault()
): WallClockTimelineSnapshot {
    return WallClockTimelineSnapshot(now = now, zoneId = zoneId)
}

internal fun shouldRefreshWallClockTimeline(
    previous: WallClockTimelineSnapshot?,
    current: WallClockTimelineSnapshot
): Boolean {
    return previous == null || previous.zoneId != current.zoneId || previous.localMinute != current.localMinute
}

internal fun nextWallClockTimelineDelayMillis(now: LocalDateTime = LocalDateTime.now()): Long {
    val nextMinute = now.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1)
    return maxOf(250L, Duration.between(now, nextMinute).toMillis())
}