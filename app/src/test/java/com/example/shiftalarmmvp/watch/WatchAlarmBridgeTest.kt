package com.example.shiftalarmmvp.watch

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchAlarmBridgeTest {
    @Test
    fun parsePayload_readsPhoneAlarmPayload() {
        val rawJson = JSONObject()
            .put("alarmId", 42L)
            .put("label", "야간 근무")
            .put("snoozeMinutes", 10)
            .put("snoozeMaxCount", 3)
            .put("currentSnoozeCount", 1)
            .put("soundType", "ALARM")
            .put("customSoundUri", "content://alarm")
            .put("volumePercent", 85)
            .put("vibrationEnabled", true)
            .put("snoozeAllowed", true)
            .put("triggeredAtMillis", 123_456L)
            .toString()

        val payload = WatchAlarmBridge.parsePayload(rawJson.toByteArray(Charsets.UTF_8))

        assertEquals(42L, payload?.alarmId)
        assertEquals("야간 근무", payload?.label)
        assertEquals(10, payload?.snoozeMinutes)
        assertEquals(3, payload?.snoozeMaxCount)
        assertEquals(1, payload?.currentSnoozeCount)
        assertEquals("ALARM", payload?.soundType)
        assertEquals("content://alarm", payload?.customSoundUri)
        assertEquals(85, payload?.volumePercent)
        assertEquals(123_456L, payload?.triggeredAtMillis)
        assertTrue(payload?.snoozeAllowed == true)
        assertTrue(payload?.canSnooze == true)
    }

    @Test
    fun parseAck_readsPayloadAndDisplayMode() {
        val rawJson = JSONObject()
            .put("alarmId", 42L)
            .put("label", "야간 근무")
            .put("snoozeMinutes", 10)
            .put("snoozeMaxCount", 3)
            .put("currentSnoozeCount", 1)
            .put("volumePercent", 85)
            .put("vibrationEnabled", true)
            .put("snoozeAllowed", true)
            .put("triggeredAtMillis", 123_456L)
            .put("ackDisplayMode", WatchAlarmBridge.ACK_DISPLAY_MODE_NOTIFICATION)
            .toString()

        val ack = WatchAlarmBridge.parseAck(rawJson.toByteArray(Charsets.UTF_8))

        assertEquals(42L, ack?.payload?.alarmId)
        assertEquals("야간 근무", ack?.payload?.label)
        assertEquals(WatchAlarmBridge.ACK_DISPLAY_MODE_NOTIFICATION, ack?.displayMode)
    }

    @Test
    fun parsePayload_clampsUnsafeNumbersAndRejectsInvalidAlarmId() {
        val clamped = WatchAlarmBridge.parsePayload(
            JSONObject()
                .put("alarmId", 7L)
                .put("triggeredAtMillis", 20_000L)
                .put("snoozeMinutes", 999)
                .put("snoozeMaxCount", 999)
                .put("currentSnoozeCount", -2)
                .put("volumePercent", 999)
                .toString()
        )

        assertEquals(60, clamped?.snoozeMinutes)
        assertEquals(99, clamped?.snoozeMaxCount)
        assertEquals(0, clamped?.currentSnoozeCount)
        assertEquals(100, clamped?.volumePercent)

        val invalid = WatchAlarmBridge.parsePayload(JSONObject().put("alarmId", -1L).toString())
        val missingOccurrence = WatchAlarmBridge.parsePayload(JSONObject().put("alarmId", 7L).toString())

        assertNull(invalid)
        assertNull(missingOccurrence)
    }

    @Test
    fun canSnooze_respectsLimitedAndUnlimitedSnooze() {
        val limitedPayload = WatchAlarmPayload(
            alarmId = 1L,
            label = "",
            snoozeMinutes = 5,
            snoozeMaxCount = 2,
            currentSnoozeCount = 2,
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
    fun controlGateEventKey_deduplicatesAnyActionWithinSameAlarmCycle() {
        val payload = WatchAlarmPayload(
            alarmId = 77L,
            label = "야간 근무",
            snoozeMinutes = 5,
            snoozeMaxCount = 3,
            currentSnoozeCount = 0,
            soundType = null,
            customSoundUri = null,
            volumePercent = 100,
            vibrationEnabled = true,
            snoozeAllowed = true,
            triggeredAtMillis = 10_000L
        )

        val first = WatchAlarmControlGate.eventKey(payload)
        val duplicate = WatchAlarmControlGate.eventKey(payload)
        val nextCycle = WatchAlarmControlGate.eventKey(
            payload.copy(triggeredAtMillis = 20_000L, currentSnoozeCount = 1)
        )

        assertEquals(first, duplicate)
        assertTrue(first != nextCycle)
        assertNull(WatchAlarmControlGate.eventKey(payload.copy(alarmId = -1L)))
    }

    @Test
    fun acceptedControlReplay_matchesOnlySameRecentControl() {
        val payload = WatchAlarmPayload(
            alarmId = 77L,
            label = "?쇨컙 洹쇰Т",
            snoozeMinutes = 5,
            snoozeMaxCount = 3,
            currentSnoozeCount = 1,
            soundType = null,
            customSoundUri = null,
            volumePercent = 100,
            vibrationEnabled = true,
            snoozeAllowed = true,
            triggeredAtMillis = 10_000L
        )
        val acceptedAt = 50_000L

        assertTrue(
            WatchAlarmAcceptedControlStore.matchesRecent(
                action = WatchAlarmBridge.PATH_ALARM_SNOOZE,
                payload = payload,
                acceptedAction = WatchAlarmBridge.PATH_ALARM_SNOOZE,
                acceptedAlarmId = 77L,
                acceptedTriggeredAtMillis = 10_000L,
                acceptedCurrentSnoozeCount = 1,
                acceptedAtMillis = acceptedAt,
                nowMillis = acceptedAt + 5_000L
            )
        )
        assertFalse(
            WatchAlarmAcceptedControlStore.matchesRecent(
                action = WatchAlarmBridge.PATH_ALARM_STOP,
                payload = payload,
                acceptedAction = WatchAlarmBridge.PATH_ALARM_SNOOZE,
                acceptedAlarmId = 77L,
                acceptedTriggeredAtMillis = 10_000L,
                acceptedCurrentSnoozeCount = 1,
                acceptedAtMillis = acceptedAt,
                nowMillis = acceptedAt + 5_000L
            )
        )
        assertFalse(
            WatchAlarmAcceptedControlStore.matchesRecent(
                action = WatchAlarmBridge.PATH_ALARM_SNOOZE,
                payload = payload,
                acceptedAction = WatchAlarmBridge.PATH_ALARM_SNOOZE,
                acceptedAlarmId = 77L,
                acceptedTriggeredAtMillis = 10_000L,
                acceptedCurrentSnoozeCount = 1,
                acceptedAtMillis = acceptedAt,
                nowMillis = acceptedAt + 121_000L
            )
        )
    }
}
