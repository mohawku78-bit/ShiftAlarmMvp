package com.example.shiftalarmmvp.wear

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchVibrationPatternsTest {
    @Test
    fun notificationVibrationUsesSoftMultiPulseRamp() {
        assertNotificationMultiPulseRamp(
            pattern = WatchVibrationPatterns.NOTIFICATION_RAMP_PATTERN,
            amplitudes = WatchVibrationPatterns.NOTIFICATION_RAMP_AMPLITUDES,
            maxFirstPulseMillis = 25L,
            maxFirstPulseAmplitude = 6,
            maxPulseMillis = 130L,
            maxPeakAmplitude = 80,
            minFirstPauseMillis = 900L,
            minPulseCount = 10,
            minTotalDurationMillis = 14_000L
        )
    }

    @Test
    fun optionalAlarmScreenVibrationStartsGentleAndRampsUp() {
        assertWaveformRamp(
            timings = WatchVibrationPatterns.ACTIVITY_RAMP_TIMINGS,
            amplitudes = WatchVibrationPatterns.ACTIVITY_RAMP_AMPLITUDES,
            repeatIndex = WatchVibrationPatterns.ACTIVITY_RAMP_REPEAT_INDEX,
            maxFirstPulseAmplitude = 6,
            maxPeakAmplitude = 64,
            minFirstPauseMillis = 1_000L
        )
    }

    private fun assertNotificationMultiPulseRamp(
        pattern: LongArray,
        amplitudes: IntArray,
        maxFirstPulseMillis: Long,
        maxFirstPulseAmplitude: Int,
        maxPulseMillis: Long,
        maxPeakAmplitude: Int,
        minFirstPauseMillis: Long,
        minPulseCount: Int,
        minTotalDurationMillis: Long
    ) {
        assertEquals("notification timing and amplitude arrays must stay aligned", pattern.size, amplitudes.size)
        assertEquals(0L, pattern.first())
        assertEquals(0, amplitudes.first())
        assertTrue("notification pattern should contain wait/pulse pairs", pattern.size >= minPulseCount * 2)
        assertTrue("first notification pulse should stay gentle", pattern[1] <= maxFirstPulseMillis)
        assertTrue("first notification amplitude should stay gentle", amplitudes[1] <= maxFirstPulseAmplitude)
        assertTrue("first pause should keep the alert calm", pattern[2] >= minFirstPauseMillis)
        assertTrue("notification pattern should last long enough to feel like an alarm", pattern.sum() >= minTotalDurationMillis)

        var pulseCount = 0
        var strongestPulseMillis = 0L
        var previousPulseAmplitude = 0
        for (index in 1 until pattern.size step 2) {
            pulseCount += 1
            assertTrue("notification pulse should stay short", pattern[index] <= maxPulseMillis)
            strongestPulseMillis = maxOf(strongestPulseMillis, pattern[index])
            assertTrue("notification pulse amplitudes should ramp upward", amplitudes[index] >= previousPulseAmplitude)
            previousPulseAmplitude = amplitudes[index]
        }
        assertTrue("notification should vibrate several times", pulseCount >= minPulseCount)
        assertTrue("later pulses should grow beyond the first pulse", strongestPulseMillis > pattern[1])
        assertTrue("peak amplitude should stay below harsh full-strength vibration", amplitudes.max() <= maxPeakAmplitude)
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
        assertTrue("first watch pulse should stay gentle", amplitudes[1] <= maxFirstPulseAmplitude)
        assertTrue("first pause should prevent a harsh instant buzz", timings[2] >= minFirstPauseMillis)
        assertTrue("peak amplitude should stay below harsh full-strength vibration", amplitudes.max() <= maxPeakAmplitude)

        var previousPulseAmplitude = 0
        for (index in 1 until amplitudes.size step 2) {
            assertTrue("pulse amplitudes should ramp upward", amplitudes[index] >= previousPulseAmplitude)
            previousPulseAmplitude = amplitudes[index]
        }
    }
}
