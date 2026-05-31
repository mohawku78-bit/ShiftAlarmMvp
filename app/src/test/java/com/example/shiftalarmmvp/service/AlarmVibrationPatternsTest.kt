package com.example.shiftalarmmvp.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmVibrationPatternsTest {
    @Test
    fun phoneAlarmVibrationStartsGentleAndRampsUp() {
        assertWaveformRamp(
            timings = AlarmVibrationPatterns.PHONE_RAMP_TIMINGS,
            amplitudes = AlarmVibrationPatterns.PHONE_RAMP_AMPLITUDES,
            repeatIndex = AlarmVibrationPatterns.PHONE_RAMP_REPEAT_INDEX,
            maxFirstPulseAmplitude = 10,
            maxPeakAmplitude = 150,
            minFirstPauseMillis = 1_500L
        )
    }

    @Test
    fun watchBridgeFallbackNotificationUsesSoftOneShotPattern() {
        assertNotificationRamp(
            pattern = AlarmVibrationPatterns.WATCH_BRIDGE_RAMP_PATTERN,
            maxFirstPulseMillis = 25L,
            maxPulseMillis = 105L,
            minFirstPauseMillis = 1_200L
        )
    }

    private fun assertWaveformRamp(
        timings: LongArray,
        amplitudes: IntArray,
        repeatIndex: Int,
        maxFirstPulseAmplitude: Int,
        maxPeakAmplitude: Int,
        minFirstPauseMillis: Long
    ) {
        assertEquals("timing and amplitude arrays must stay aligned", timings.size, amplitudes.size)
        assertEquals(0L, timings.first())
        assertEquals(0, amplitudes.first())
        assertTrue("repeat should restart on a vibration segment", repeatIndex > 0 && repeatIndex % 2 == 1)
        assertTrue("first phone pulse should stay gentle", amplitudes[1] <= maxFirstPulseAmplitude)
        assertTrue("first pause should prevent a harsh instant buzz", timings[2] >= minFirstPauseMillis)
        assertTrue("peak amplitude should stay below harsh full-strength vibration", amplitudes.max() <= maxPeakAmplitude)

        var previousPulseAmplitude = 0
        for (index in 1 until amplitudes.size step 2) {
            assertTrue("pulse amplitudes should ramp upward", amplitudes[index] >= previousPulseAmplitude)
            previousPulseAmplitude = amplitudes[index]
        }
    }

    private fun assertNotificationRamp(
        pattern: LongArray,
        maxFirstPulseMillis: Long,
        maxPulseMillis: Long,
        minFirstPauseMillis: Long
    ) {
        assertEquals(0L, pattern.first())
        assertTrue("notification pattern should end after a pulse", pattern.size % 2 == 1)
        assertTrue("first notification pulse should stay gentle", pattern[1] <= maxFirstPulseMillis)
        assertTrue("first pause should keep the alert calm", pattern[2] >= minFirstPauseMillis)

        var previousPulseMillis = 0L
        for (index in 1 until pattern.size step 2) {
            assertTrue("notification pulses should ramp upward", pattern[index] >= previousPulseMillis)
            assertTrue("notification pulse should stay short", pattern[index] <= maxPulseMillis)
            previousPulseMillis = pattern[index]
        }
    }
}
