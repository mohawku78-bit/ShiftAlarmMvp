package com.example.shiftalarmmvp.recovery

import android.content.Context

data class ReliabilityStateSnapshot(
    val selfTestStatus: SelfTestStatus?,
    val nightlyCheckStatus: NightlyReliabilityCheckStatus?
)

object ReliabilityPolicy {
    private const val SELF_TEST_STALE_AFTER_MILLIS = 7L * 24L * 60L * 60L * 1000L

    fun isSelfTestFollowUpNeeded(event: SelfTestStatus.Event?): Boolean {
        return when (event) {
            null,
            SelfTestStatus.Event.NONE,
            SelfTestStatus.Event.SCHEDULED,
            SelfTestStatus.Event.TRIGGERED,
            SelfTestStatus.Event.UNCERTAIN,
            SelfTestStatus.Event.CANCELED -> true
            SelfTestStatus.Event.PASSED,
            SelfTestStatus.Event.FAILED -> false
        }
    }

    fun isSelfTestFollowUpNeeded(
        status: SelfTestStatus?,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        val event = status?.lastEvent
        return when (event) {
            null,
            SelfTestStatus.Event.NONE,
            SelfTestStatus.Event.SCHEDULED,
            SelfTestStatus.Event.TRIGGERED,
            SelfTestStatus.Event.UNCERTAIN,
            SelfTestStatus.Event.CANCELED -> true
            SelfTestStatus.Event.PASSED -> isSelfTestStale(status, nowMillis)
            SelfTestStatus.Event.FAILED -> false
        }
    }

    fun isSelfTestStale(
        status: SelfTestStatus?,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (status?.lastEvent != SelfTestStatus.Event.PASSED) return false

        val confirmedAt = when {
            status.feedbackAtMillis > 0L -> status.feedbackAtMillis
            status.triggeredAtMillis > 0L -> status.triggeredAtMillis
            status.triggerAtMillis > 0L -> status.triggerAtMillis
            else -> 0L
        }
        if (confirmedAt <= 0L) return true

        return nowMillis - confirmedAt >= SELF_TEST_STALE_AFTER_MILLIS
    }
}

class ReliabilityStateCoordinator(context: Context) {
    private val appContext = context.applicationContext
    private val selfTestStore = SelfTestStatusStore(appContext)
    private val nightlyCheckStore = NightlyReliabilityCheckStore(appContext)

    fun snapshot(): ReliabilityStateSnapshot {
        return ReliabilityStateSnapshot(
            selfTestStatus = selfTestStore.load(),
            nightlyCheckStatus = nightlyCheckStore.load()
        )
    }

    fun recordSelfTestScheduled(
        triggerAtMillis: Long,
        scheduledAtMillis: Long = System.currentTimeMillis()
    ): ReliabilityStateSnapshot {
        selfTestStore.recordScheduled(triggerAtMillis, scheduledAtMillis)
        return recalculateAndSnapshot()
    }

    fun recordSelfTestCanceled(
        canceledAtMillis: Long = System.currentTimeMillis()
    ): ReliabilityStateSnapshot {
        selfTestStore.recordCanceled(canceledAtMillis)
        return recalculateAndSnapshot()
    }

    fun recordSelfTestFeedback(
        feedback: SelfTestStatus.Feedback,
        feedbackAtMillis: Long = System.currentTimeMillis()
    ): ReliabilityStateSnapshot {
        selfTestStore.recordFeedback(feedback, feedbackAtMillis)
        return recalculateAndSnapshot()
    }

    fun recalculateAndSnapshot(): ReliabilityStateSnapshot {
        val inspection = ReliabilityInspector.inspect(appContext)
        nightlyCheckStore.record(
            issueCount = inspection.issueCount,
            summary = inspection.summary
        )
        return snapshot()
    }
}