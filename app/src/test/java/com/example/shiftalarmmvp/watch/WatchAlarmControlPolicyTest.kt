package com.example.shiftalarmmvp.watch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchAlarmControlPolicyTest {
    @Test
    fun recentAcceptedControlReplaysAckWithoutCheckingRingingOrGate() {
        var gateChecked = false

        val decision = WatchAlarmControlPolicy.decide(
            action = WatchAlarmBridge.PATH_ALARM_STOP,
            payload = payload(),
            hasRecentAcceptedControl = true,
            isAlarmRinging = false,
            acceptControlGate = {
                gateChecked = true
                false
            }
        )

        assertEquals(WatchAlarmControlDecision.REPLAY_ACCEPTED_CONTROL_ACK, decision)
        assertFalse(gateChecked)
    }

    @Test
    fun staleControlRejectsAndDoesNotConsumeControlGate() {
        var gateChecked = false

        val decision = WatchAlarmControlPolicy.decide(
            action = WatchAlarmBridge.PATH_ALARM_STOP,
            payload = payload(),
            hasRecentAcceptedControl = false,
            isAlarmRinging = false,
            acceptControlGate = {
                gateChecked = true
                true
            }
        )

        assertEquals(WatchAlarmControlDecision.REJECT_STALE_ALARM, decision)
        assertFalse(gateChecked)
    }

    @Test
    fun stopRequiresControlGateAcceptance() {
        var gateChecks = 0

        val accepted = WatchAlarmControlPolicy.decide(
            action = WatchAlarmBridge.PATH_ALARM_STOP,
            payload = payload(),
            hasRecentAcceptedControl = false,
            isAlarmRinging = true,
            acceptControlGate = {
                gateChecks += 1
                true
            }
        )
        val duplicate = WatchAlarmControlPolicy.decide(
            action = WatchAlarmBridge.PATH_ALARM_STOP,
            payload = payload(),
            hasRecentAcceptedControl = false,
            isAlarmRinging = true,
            acceptControlGate = {
                gateChecks += 1
                false
            }
        )

        assertEquals(WatchAlarmControlDecision.ACCEPT_STOP, accepted)
        assertEquals(WatchAlarmControlDecision.REJECT_DUPLICATE_CONTROL, duplicate)
        assertEquals(2, gateChecks)
    }

    @Test
    fun snoozeRejectsWhenLimitReachedWithoutConsumingGate() {
        var gateChecked = false

        val decision = WatchAlarmControlPolicy.decide(
            action = WatchAlarmBridge.PATH_ALARM_SNOOZE,
            payload = payload(snoozeMaxCount = 2, currentSnoozeCount = 2),
            hasRecentAcceptedControl = false,
            isAlarmRinging = true,
            acceptControlGate = {
                gateChecked = true
                true
            }
        )

        assertEquals(WatchAlarmControlDecision.REJECT_SNOOZE_NOT_ALLOWED, decision)
        assertFalse(gateChecked)
    }

    @Test
    fun snoozeRequiresControlGateAfterSnoozeAllowed() {
        var gateChecked = false

        val decision = WatchAlarmControlPolicy.decide(
            action = WatchAlarmBridge.PATH_ALARM_SNOOZE,
            payload = payload(snoozeMaxCount = 2, currentSnoozeCount = 1),
            hasRecentAcceptedControl = false,
            isAlarmRinging = true,
            acceptControlGate = {
                gateChecked = true
                true
            }
        )

        assertEquals(WatchAlarmControlDecision.ACCEPT_SNOOZE, decision)
        assertTrue(gateChecked)
    }

    @Test
    fun unsupportedActionRejectsWithoutConsumingGate() {
        var gateChecked = false

        val decision = WatchAlarmControlPolicy.decide(
            action = "/shift_alarm/alarm/unknown",
            payload = payload(),
            hasRecentAcceptedControl = false,
            isAlarmRinging = true,
            acceptControlGate = {
                gateChecked = true
                true
            }
        )

        assertEquals(WatchAlarmControlDecision.REJECT_UNSUPPORTED_ACTION, decision)
        assertFalse(gateChecked)
    }

    private fun payload(
        snoozeMaxCount: Int = 3,
        currentSnoozeCount: Int = 0
    ): WatchAlarmPayload {
        return WatchAlarmPayload(
            alarmId = 77L,
            label = "watch control",
            snoozeMinutes = 5,
            snoozeMaxCount = snoozeMaxCount,
            currentSnoozeCount = currentSnoozeCount,
            soundType = null,
            customSoundUri = null,
            volumePercent = 100,
            vibrationEnabled = true,
            snoozeAllowed = true,
            triggeredAtMillis = 10_000L
        )
    }
}
