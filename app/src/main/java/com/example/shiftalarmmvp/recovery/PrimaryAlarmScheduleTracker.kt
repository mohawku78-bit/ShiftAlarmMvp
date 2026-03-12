package com.example.shiftalarmmvp.recovery

import android.content.Context
import com.example.shiftalarmmvp.data.AlarmRule
import com.example.shiftalarmmvp.scheduler.AlarmScheduleResult

internal const val SELF_TEST_ALARM_ID = 999_999L

internal class PrimaryAlarmScheduleTracker(context: Context) {
    private val appContext = context.applicationContext
    private val triggerHistoryStore = TriggerHistoryStore(appContext)

    fun recordSchedule(
        rule: AlarmRule,
        result: AlarmScheduleResult,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        if (rule.id <= 0L || rule.id == SELF_TEST_ALARM_ID) return
        if (!rule.enabled || !result.scheduled || result.scheduledTriggerMillis == null || result.scheduledTriggerMillis <= 0L) {
            clearAlarm(rule.id, nowMillis)
            return
        }
        triggerHistoryStore.recordSchedule(
            registration = TrackedAlarmRegistration(
                alarmId = rule.id,
                label = rule.label,
                expectedTriggerMillis = result.scheduledTriggerMillis,
                scheduledAtMillis = nowMillis,
                scheduleMode = result.mode
            ),
            eventAtMillis = nowMillis
        )
        AlarmWatchdogScheduler.schedule(appContext)
    }

    fun clearAlarm(
        alarmId: Long,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        if (alarmId <= 0L || alarmId == SELF_TEST_ALARM_ID) return
        triggerHistoryStore.clearAlarm(alarmId, nowMillis)
        AlarmWatchdogScheduler.schedule(appContext)
    }

    fun recordTrigger(
        alarmId: Long,
        label: String,
        expectedTriggerMillis: Long?,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        if (alarmId <= 0L || alarmId == SELF_TEST_ALARM_ID) return
        triggerHistoryStore.recordTriggered(alarmId, label, expectedTriggerMillis, nowMillis)
        AlarmWatchdogScheduler.schedule(appContext)
    }

    fun ensureScheduled() {
        AlarmWatchdogScheduler.schedule(appContext)
    }
}
