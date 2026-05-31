package com.example.shiftalarmmvp.watch

internal enum class WatchAlarmControlDecision {
    REPLAY_ACCEPTED_CONTROL_ACK,
    REJECT_STALE_ALARM,
    ACCEPT_STOP,
    ACCEPT_SNOOZE,
    REJECT_DUPLICATE_CONTROL,
    REJECT_SNOOZE_NOT_ALLOWED,
    REJECT_UNSUPPORTED_ACTION
}

internal object WatchAlarmControlPolicy {
    fun decide(
        action: String,
        payload: WatchAlarmPayload,
        hasRecentAcceptedControl: Boolean,
        isAlarmRinging: Boolean,
        acceptControlGate: () -> Boolean
    ): WatchAlarmControlDecision {
        if (hasRecentAcceptedControl) {
            return WatchAlarmControlDecision.REPLAY_ACCEPTED_CONTROL_ACK
        }
        if (!isAlarmRinging) {
            return WatchAlarmControlDecision.REJECT_STALE_ALARM
        }

        return when (action) {
            WatchAlarmBridge.PATH_ALARM_STOP -> {
                if (acceptControlGate()) {
                    WatchAlarmControlDecision.ACCEPT_STOP
                } else {
                    WatchAlarmControlDecision.REJECT_DUPLICATE_CONTROL
                }
            }

            WatchAlarmBridge.PATH_ALARM_SNOOZE -> {
                if (!payload.canSnooze) {
                    WatchAlarmControlDecision.REJECT_SNOOZE_NOT_ALLOWED
                } else if (acceptControlGate()) {
                    WatchAlarmControlDecision.ACCEPT_SNOOZE
                } else {
                    WatchAlarmControlDecision.REJECT_DUPLICATE_CONTROL
                }
            }

            else -> WatchAlarmControlDecision.REJECT_UNSUPPORTED_ACTION
        }
    }
}
