package com.example.shiftalarmmvp.wear

internal object WatchVibrationPatterns {
    val NOTIFICATION_RAMP_PATTERN =
        longArrayOf(0, 25, 1_400, 35, 1_250, 45, 1_100, 60, 950, 80, 800, 105, 700)

    val ACTIVITY_RAMP_TIMINGS =
        longArrayOf(0, 25, 1_200, 35, 1_100, 45, 1_000, 60, 900, 80, 800, 110, 700, 150, 600)
    val ACTIVITY_RAMP_AMPLITUDES =
        intArrayOf(0, 6, 0, 9, 0, 14, 0, 22, 0, 32, 0, 46, 0, 64, 0)
    const val ACTIVITY_RAMP_REPEAT_INDEX = 9
}
