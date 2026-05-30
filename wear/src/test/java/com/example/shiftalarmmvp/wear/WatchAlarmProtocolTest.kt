package com.example.shiftalarmmvp.wear

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchAlarmProtocolTest {
    @Test
    fun payloadRoundTrip_preservesAlarmFields() {
        val payload = WatchAlarmPayload(
            alarmId = 99L,
            label = "주간 근무",
            snoozeMinutes = 15,
            snoozeMaxCount = 4,
            currentSnoozeCount = 2,
            soundType = "ALARM",
            customSoundUri = "content://shift-alarm",
            volumePercent = 70,
            vibrationEnabled = true,
            snoozeAllowed = true,
            triggeredAtMillis = 987_654L
        )

        val parsed = WatchAlarmProtocol.parsePayload(WatchAlarmProtocol.toJson(payload))

        assertEquals(payload, parsed)
        assertTrue(parsed?.snoozeAllowed == true)
        assertTrue(parsed?.canSnooze == true)
    }

    @Test
    fun ackJson_preservesPayloadAndAddsDisplayMode() {
        val payload = WatchAlarmPayload(
            alarmId = 99L,
            label = "주간 근무",
            snoozeMinutes = 15,
            snoozeMaxCount = 4,
            currentSnoozeCount = 2,
            soundType = "ALARM",
            customSoundUri = null,
            volumePercent = 70,
            vibrationEnabled = true,
            snoozeAllowed = true,
            triggeredAtMillis = 987_654L
        )

        val ackJson = WatchAlarmProtocol.toAckJson(
            payload,
            WatchAlarmProtocol.ACK_DISPLAY_MODE_FOREGROUND_SERVICE
        )
        val parsed = WatchAlarmProtocol.parsePayload(ackJson)

        assertEquals(payload, parsed)
        assertEquals(
            WatchAlarmProtocol.ACK_DISPLAY_MODE_FOREGROUND_SERVICE,
            JSONObject(ackJson).getString(WatchAlarmProtocol.KEY_ACK_DISPLAY_MODE)
        )
    }

    @Test
    fun parsePayload_clampsUnsafeNumbersAndRejectsInvalidAlarmId() {
        val parsed = WatchAlarmProtocol.parsePayload(
            JSONObject()
                .put("alarmId", 11L)
                .put("snoozeMinutes", 0)
                .put("snoozeMaxCount", 999)
                .put("currentSnoozeCount", -3)
                .put("volumePercent", -30)
                .toString()
        )

        assertEquals(1, parsed?.snoozeMinutes)
        assertEquals(99, parsed?.snoozeMaxCount)
        assertEquals(0, parsed?.currentSnoozeCount)
        assertEquals(0, parsed?.volumePercent)

        val invalid = WatchAlarmProtocol.parsePayload(JSONObject().put("alarmId", 0L).toString())

        assertNull(invalid)
    }

    @Test
    fun parseCancellation_readsAlarmIdAndEventTime() {
        val cancellation = WatchAlarmProtocol.parseCancellation(
            JSONObject()
                .put("alarmId", 31L)
                .put("triggeredAtMillis", 555L)
                .toString()
                .toByteArray(Charsets.UTF_8)
        )

        assertEquals(31L, cancellation?.alarmId)
        assertEquals(555L, cancellation?.eventTimeMillis)
    }

    @Test
    fun canSnooze_respectsLimitedAndUnlimitedSnooze() {
        val limitedPayload = WatchAlarmPayload(
            alarmId = 1L,
            label = "",
            snoozeMinutes = 5,
            snoozeMaxCount = 1,
            currentSnoozeCount = 1,
            soundType = null,
            customSoundUri = null,
            volumePercent = 100,
            vibrationEnabled = true,
            snoozeAllowed = true,
            triggeredAtMillis = 1L
        )

        val unlimitedPayload = limitedPayload.copy(snoozeMaxCount = 0, currentSnoozeCount = 99)
        val blockedPayload = unlimitedPayload.copy(snoozeAllowed = false)

        assertFalse(limitedPayload.canSnooze)
        assertTrue(unlimitedPayload.canSnooze)
        assertFalse(blockedPayload.canSnooze)
    }

    @Test
    fun eventGateKey_separatesStartAndCancelEvents() {
        val startKey = WatchAlarmEventGate.eventKey(WatchAlarmEventGate.EVENT_START, 42L)
        val cancelKey = WatchAlarmEventGate.eventKey(WatchAlarmEventGate.EVENT_CANCEL, 42L)
        val nextStartKey = WatchAlarmEventGate.eventKey(WatchAlarmEventGate.EVENT_START, 43L)

        assertTrue(startKey != cancelKey)
        assertTrue(startKey != nextStartKey)
        assertNull(WatchAlarmEventGate.eventKey("", 42L))
        assertNull(WatchAlarmEventGate.eventKey(WatchAlarmEventGate.EVENT_START, 0L))
    }
}
