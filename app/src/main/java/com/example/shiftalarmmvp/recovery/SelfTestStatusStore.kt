package com.example.shiftalarmmvp.recovery

import android.content.Context
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val SELF_TEST_PREF_NAME = "alarm_self_test_state"
private const val KEY_LAST_EVENT = "last_event"
private const val KEY_SCHEDULED_AT_MILLIS = "scheduled_at_millis"
private const val KEY_TRIGGER_AT_MILLIS = "trigger_at_millis"
private const val KEY_TRIGGERED_AT_MILLIS = "triggered_at_millis"
private const val KEY_FEEDBACK_AT_MILLIS = "feedback_at_millis"

private val SELF_TEST_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")

data class SelfTestStatus(
    val lastEvent: Event,
    val scheduledAtMillis: Long,
    val triggerAtMillis: Long,
    val triggeredAtMillis: Long,
    val feedbackAtMillis: Long
) {
    enum class Event {
        NONE,
        SCHEDULED,
        TRIGGERED,
        PASSED,
        UNCERTAIN,
        FAILED,
        CANCELED
    }

    enum class Feedback {
        PASSED,
        UNCERTAIN,
        FAILED
    }

    fun statusLabel(texts: SelfTestStatusStrings): String {
        return when (lastEvent) {
            Event.NONE -> texts.statusNone
            Event.SCHEDULED -> texts.statusScheduled
            Event.TRIGGERED -> texts.statusTriggered
            Event.PASSED -> texts.statusPassed
            Event.UNCERTAIN -> texts.statusUncertain
            Event.FAILED -> texts.statusFailed
            Event.CANCELED -> texts.statusCanceled
        }
    }

    fun detailText(texts: SelfTestStatusStrings): String {
        val target = when (lastEvent) {
            Event.SCHEDULED -> if (triggerAtMillis > 0L) triggerAtMillis else scheduledAtMillis
            Event.TRIGGERED -> if (triggeredAtMillis > 0L) triggeredAtMillis else triggerAtMillis
            Event.PASSED, Event.UNCERTAIN, Event.FAILED -> {
                if (feedbackAtMillis > 0L) feedbackAtMillis else triggeredAtMillis
            }
            Event.CANCELED -> if (feedbackAtMillis > 0L) feedbackAtMillis else scheduledAtMillis
            Event.NONE -> 0L
        }
        return if (target > 0L) {
            Instant.ofEpochMilli(target)
                .atZone(ZoneId.systemDefault())
                .format(SELF_TEST_TIME_FORMATTER)
        } else {
            texts.detailNone
        }
    }

    fun homeSummary(texts: SelfTestStatusStrings): String {
        return texts.homeSummaryFormat.format(statusLabel(texts), detailText(texts))
    }
}

class SelfTestStatusStore(context: Context) {
    private val prefs = context.getSharedPreferences(SELF_TEST_PREF_NAME, Context.MODE_PRIVATE)

    fun recordScheduled(
        triggerAtMillis: Long,
        scheduledAtMillis: Long = System.currentTimeMillis()
    ) {
        prefs.edit()
            .putString(KEY_LAST_EVENT, SelfTestStatus.Event.SCHEDULED.name)
            .putLong(KEY_SCHEDULED_AT_MILLIS, scheduledAtMillis)
            .putLong(KEY_TRIGGER_AT_MILLIS, triggerAtMillis.coerceAtLeast(0L))
            .putLong(KEY_TRIGGERED_AT_MILLIS, 0L)
            .putLong(KEY_FEEDBACK_AT_MILLIS, 0L)
            .apply()
    }

    fun recordTriggered(triggeredAtMillis: Long = System.currentTimeMillis()) {
        prefs.edit()
            .putString(KEY_LAST_EVENT, SelfTestStatus.Event.TRIGGERED.name)
            .putLong(KEY_TRIGGERED_AT_MILLIS, triggeredAtMillis)
            .apply()
    }

    fun recordCanceled(canceledAtMillis: Long = System.currentTimeMillis()) {
        prefs.edit()
            .putString(KEY_LAST_EVENT, SelfTestStatus.Event.CANCELED.name)
            .putLong(KEY_FEEDBACK_AT_MILLIS, canceledAtMillis)
            .apply()
    }

    fun recordFeedback(
        feedback: SelfTestStatus.Feedback,
        feedbackAtMillis: Long = System.currentTimeMillis()
    ) {
        val event = when (feedback) {
            SelfTestStatus.Feedback.PASSED -> SelfTestStatus.Event.PASSED
            SelfTestStatus.Feedback.UNCERTAIN -> SelfTestStatus.Event.UNCERTAIN
            SelfTestStatus.Feedback.FAILED -> SelfTestStatus.Event.FAILED
        }
        prefs.edit()
            .putString(KEY_LAST_EVENT, event.name)
            .putLong(KEY_FEEDBACK_AT_MILLIS, feedbackAtMillis)
            .apply()
    }

    fun load(): SelfTestStatus? {
        val eventName = prefs.getString(KEY_LAST_EVENT, SelfTestStatus.Event.NONE.name).orEmpty()
        val event = runCatching { SelfTestStatus.Event.valueOf(eventName) }
            .getOrDefault(SelfTestStatus.Event.NONE)
        if (event == SelfTestStatus.Event.NONE) return null

        return SelfTestStatus(
            lastEvent = event,
            scheduledAtMillis = prefs.getLong(KEY_SCHEDULED_AT_MILLIS, 0L),
            triggerAtMillis = prefs.getLong(KEY_TRIGGER_AT_MILLIS, 0L),
            triggeredAtMillis = prefs.getLong(KEY_TRIGGERED_AT_MILLIS, 0L),
            feedbackAtMillis = prefs.getLong(KEY_FEEDBACK_AT_MILLIS, 0L)
        )
    }
}