package com.example.shiftalarmmvp.wear

object WatchAlarmActions {
    const val ACTION_STOP = "com.example.shiftalarmmvp.wear.action.STOP"
    const val ACTION_SNOOZE = "com.example.shiftalarmmvp.wear.action.SNOOZE"

    fun controlPathFor(action: String?): String? {
        return when (action) {
            ACTION_STOP -> WatchAlarmProtocol.PATH_ALARM_STOP
            ACTION_SNOOZE -> WatchAlarmProtocol.PATH_ALARM_SNOOZE
            else -> null
        }
    }
}
