package com.example.shiftalarmmvp.wear

import android.view.KeyEvent
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
                .put("triggeredAtMillis", 20_000L)
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
        val missingOccurrence = WatchAlarmProtocol.parsePayload(JSONObject().put("alarmId", 11L).toString())

        assertNull(invalid)
        assertNull(missingOccurrence)
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
    fun parseControlAcknowledgement_readsActionAndPayload() {
        val payload = WatchAlarmPayload(
            alarmId = 99L,
            label = "二쇨컙 洹쇰Т",
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

        val ackJson = JSONObject()
            .put("action", WatchAlarmProtocol.PATH_ALARM_SNOOZE)
            .put("payloadJson", WatchAlarmProtocol.toJson(payload))
            .put("eventTimeMillis", 123L)
            .toString()

        val ack = WatchAlarmProtocol.parseControlAcknowledgement(ackJson.toByteArray(Charsets.UTF_8))

        assertEquals(WatchAlarmProtocol.PATH_ALARM_SNOOZE, ack?.action)
        assertEquals(payload, ack?.payload)
    }

    @Test
    fun notificationActionsMapToDataLayerControlPaths() {
        assertEquals(
            WatchAlarmProtocol.PATH_ALARM_STOP,
            WatchAlarmActions.controlPathFor(WatchAlarmActions.ACTION_STOP)
        )
        assertEquals(
            WatchAlarmProtocol.PATH_ALARM_SNOOZE,
            WatchAlarmActions.controlPathFor(WatchAlarmActions.ACTION_SNOOZE)
        )
        assertNull(WatchAlarmActions.controlPathFor("unknown"))
        assertNull(WatchAlarmActions.controlPathFor(null))
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
    fun eventGateKey_separatesStartCancelAndAlarmOccurrences() {
        val startKey = WatchAlarmEventGate.eventKey(WatchAlarmEventGate.EVENT_START, 42L, 10_000L)
        val cancelKey = WatchAlarmEventGate.eventKey(WatchAlarmEventGate.EVENT_CANCEL, 42L, 10_000L)
        val nextAlarmKey = WatchAlarmEventGate.eventKey(WatchAlarmEventGate.EVENT_START, 43L, 10_000L)
        val nextOccurrenceKey = WatchAlarmEventGate.eventKey(WatchAlarmEventGate.EVENT_START, 42L, 9_000L)

        assertTrue(startKey != cancelKey)
        assertTrue(startKey != nextAlarmKey)
        assertTrue(startKey != nextOccurrenceKey)
        assertNull(WatchAlarmEventGate.eventKey("", 42L, 10_000L))
        assertNull(WatchAlarmEventGate.eventKey(WatchAlarmEventGate.EVENT_START, 0L, 10_000L))
        assertNull(WatchAlarmEventGate.eventKey(WatchAlarmEventGate.EVENT_START, 42L, 0L))
    }

    @Test
    fun activeStoreMatchingRequiresSameAlarmOccurrence() {
        val active = WatchAlarmPayload(
            alarmId = 42L,
            label = "active",
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

        assertTrue(WatchAlarmActiveStore.matches(active, alarmId = 42L, triggeredAtMillis = 10_000L))
        assertFalse(WatchAlarmActiveStore.matches(active, alarmId = 42L, triggeredAtMillis = 20_000L))
        assertFalse(WatchAlarmActiveStore.matches(active, alarmId = 43L, triggeredAtMillis = 10_000L))
        assertFalse(WatchAlarmActiveStore.matches(null, alarmId = 42L, triggeredAtMillis = 10_000L))
        assertFalse(WatchAlarmActiveStore.matches(active, alarmId = 0L, triggeredAtMillis = 10_000L))
        assertFalse(WatchAlarmActiveStore.matches(active, alarmId = 42L, triggeredAtMillis = 0L))
    }

    @Test
    fun controlAckStoreMatchesOnlySameActionOccurrenceAndRequestWindow() {
        val payload = WatchAlarmPayload(
            alarmId = 42L,
            label = "active",
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

        assertTrue(
            WatchAlarmControlAckStore.matchesAcknowledgement(
                action = WatchAlarmProtocol.PATH_ALARM_STOP,
                payload = payload,
                storedAction = WatchAlarmProtocol.PATH_ALARM_STOP,
                storedAlarmId = 42L,
                storedTriggeredAtMillis = 10_000L,
                ackAtMillis = 50_000L,
                sinceMillis = 49_000L
            )
        )
        assertFalse(
            WatchAlarmControlAckStore.matchesAcknowledgement(
                action = WatchAlarmProtocol.PATH_ALARM_STOP,
                payload = payload,
                storedAction = WatchAlarmProtocol.PATH_ALARM_SNOOZE,
                storedAlarmId = 42L,
                storedTriggeredAtMillis = 10_000L,
                ackAtMillis = 50_000L,
                sinceMillis = 49_000L
            )
        )
        assertFalse(
            WatchAlarmControlAckStore.matchesAcknowledgement(
                action = WatchAlarmProtocol.PATH_ALARM_STOP,
                payload = payload,
                storedAction = WatchAlarmProtocol.PATH_ALARM_STOP,
                storedAlarmId = 42L,
                storedTriggeredAtMillis = 11_000L,
                ackAtMillis = 50_000L,
                sinceMillis = 49_000L
            )
        )
        assertFalse(
            WatchAlarmControlAckStore.matchesAcknowledgement(
                action = WatchAlarmProtocol.PATH_ALARM_STOP,
                payload = payload,
                storedAction = WatchAlarmProtocol.PATH_ALARM_STOP,
                storedAlarmId = 42L,
                storedTriggeredAtMillis = 10_000L,
                ackAtMillis = 48_000L,
                sinceMillis = 49_000L
            )
        )
    }

    @Test
    fun hardwareKeysMapToStopAndSnoozeControls() {
        assertEquals(
            WatchAlarmProtocol.PATH_ALARM_STOP,
            WatchAlarmHardwareKeys.controlPathFor(KeyEvent.KEYCODE_HOME, canSnooze = true)
        )
        assertEquals(
            WatchAlarmProtocol.PATH_ALARM_STOP,
            WatchAlarmHardwareKeys.controlPathFor(KeyEvent.KEYCODE_ASSIST, canSnooze = true)
        )
        assertEquals(
            WatchAlarmProtocol.PATH_ALARM_STOP,
            WatchAlarmHardwareKeys.controlPathFor(KeyEvent.KEYCODE_STEM_PRIMARY, canSnooze = true)
        )
        assertEquals(
            WatchAlarmProtocol.PATH_ALARM_STOP,
            WatchAlarmHardwareKeys.controlPathFor(KeyEvent.KEYCODE_STEM_1, canSnooze = true)
        )
        assertEquals(
            WatchAlarmProtocol.PATH_ALARM_STOP,
            WatchAlarmHardwareKeys.controlPathFor(KeyEvent.KEYCODE_STEM_3, canSnooze = true)
        )
        assertEquals(
            WatchAlarmProtocol.PATH_ALARM_STOP,
            WatchAlarmHardwareKeys.controlPathFor(KeyEvent.KEYCODE_VOLUME_UP, canSnooze = true)
        )
        assertEquals(
            WatchAlarmProtocol.PATH_ALARM_SNOOZE,
            WatchAlarmHardwareKeys.controlPathFor(KeyEvent.KEYCODE_BACK, canSnooze = true)
        )
        assertEquals(
            WatchAlarmProtocol.PATH_ALARM_SNOOZE,
            WatchAlarmHardwareKeys.controlPathFor(KeyEvent.KEYCODE_STEM_2, canSnooze = true)
        )
        assertEquals(
            WatchAlarmProtocol.PATH_ALARM_SNOOZE,
            WatchAlarmHardwareKeys.controlPathFor(KeyEvent.KEYCODE_VOLUME_DOWN, canSnooze = true)
        )
        assertNull(WatchAlarmHardwareKeys.controlPathFor(KeyEvent.KEYCODE_BACK, canSnooze = false))
        assertNull(WatchAlarmHardwareKeys.controlPathFor(KeyEvent.KEYCODE_DPAD_CENTER, canSnooze = true))
        assertTrue(WatchAlarmHardwareKeys.shouldConsume(KeyEvent.KEYCODE_HOME))
        assertTrue(WatchAlarmHardwareKeys.shouldConsume(KeyEvent.KEYCODE_BACK))
    }
}
