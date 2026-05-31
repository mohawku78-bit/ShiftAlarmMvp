package com.example.shiftalarmmvp.wear

internal object WatchVibrationPatterns {
    val NOTIFICATION_RAMP_PATTERN =
        longArrayOf(
            0, 25, 1_100, 35, 1_000, 45, 950, 60, 900, 80, 850, 105,
            1_600, 45, 1_100, 65, 1_000, 85, 950, 110,
            1_800, 55, 1_100, 75, 1_000, 100, 900, 130
        )
    val NOTIFICATION_RAMP_AMPLITUDES =
        intArrayOf(
            0, 6, 0, 8, 0, 10, 0, 14, 0, 18, 0, 24,
            0, 28, 0, 34, 0, 40, 0, 48,
            0, 56, 0, 64, 0, 72, 0, 80
        )

    val ACTIVITY_RAMP_TIMINGS =
        longArrayOf(0, 25, 1_200, 35, 1_100, 45, 1_000, 60, 900, 80, 800, 110, 700, 150, 600)
    val ACTIVITY_RAMP_AMPLITUDES =
        intArrayOf(0, 6, 0, 9, 0, 14, 0, 22, 0, 32, 0, 46, 0, 64, 0)
    const val ACTIVITY_RAMP_REPEAT_INDEX = 9
}
