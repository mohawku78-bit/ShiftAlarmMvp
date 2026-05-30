package com.example.shiftalarmmvp.wear

import android.view.KeyEvent

object WatchAlarmHardwareKeys {
    fun controlPathFor(keyCode: Int, canSnooze: Boolean): String? {
        return when (keyCode) {
            KeyEvent.KEYCODE_HOME,
            KeyEvent.KEYCODE_ASSIST,
            KeyEvent.KEYCODE_STEM_PRIMARY,
            KeyEvent.KEYCODE_STEM_1,
            KeyEvent.KEYCODE_STEM_3,
            KeyEvent.KEYCODE_VOLUME_UP -> WatchAlarmProtocol.PATH_ALARM_STOP

            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_STEM_2,
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (canSnooze) WatchAlarmProtocol.PATH_ALARM_SNOOZE else null
            }

            else -> null
        }
    }

    fun shouldConsume(keyCode: Int): Boolean {
        return keyCode == KeyEvent.KEYCODE_HOME ||
            keyCode == KeyEvent.KEYCODE_ASSIST ||
            keyCode == KeyEvent.KEYCODE_STEM_PRIMARY ||
            keyCode == KeyEvent.KEYCODE_STEM_1 ||
            keyCode == KeyEvent.KEYCODE_STEM_2 ||
            keyCode == KeyEvent.KEYCODE_STEM_3 ||
            keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
            keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
            keyCode == KeyEvent.KEYCODE_BACK
    }
}
