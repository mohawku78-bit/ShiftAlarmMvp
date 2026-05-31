package com.example.shiftalarmmvp.service

internal object AlarmVibrationPatterns {
    val PHONE_RAMP_TIMINGS =
        longArrayOf(0, 35, 1_800, 45, 1_600, 60, 1_400, 80, 1_200, 105, 1_000, 140, 850, 180, 700, 230, 560, 280, 460)
    val PHONE_RAMP_AMPLITUDES =
        intArrayOf(0, 8, 0, 12, 0, 18, 0, 28, 0, 42, 0, 60, 0, 85, 0, 115, 0, 150, 0)
    const val PHONE_RAMP_REPEAT_INDEX = 9

    val WATCH_BRIDGE_RAMP_PATTERN =
        longArrayOf(
            0, 25, 1_100, 35, 1_000, 45, 950, 60, 900, 80, 850, 105,
            1_600, 45, 1_100, 65, 1_000, 85, 950, 110,
            1_800, 55, 1_100, 75, 1_000, 100, 900, 130
        )
}
