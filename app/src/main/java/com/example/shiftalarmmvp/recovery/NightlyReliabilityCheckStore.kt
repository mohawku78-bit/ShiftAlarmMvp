package com.example.shiftalarmmvp.recovery

import android.content.Context
import com.example.shiftalarmmvp.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val NIGHTLY_CHECK_PREF_NAME = "nightly_reliability_check"
private const val KEY_CHECKED_AT_MILLIS = "checked_at_millis"
private const val KEY_ISSUE_COUNT = "issue_count"
private const val KEY_SUMMARY = "summary"

private val NIGHTLY_CHECK_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")

data class NightlyReliabilityCheckStatus(
    val checkedAtMillis: Long,
    val issueCount: Int,
    val summary: String
) {
    fun checkedAtText(): String {
        return Instant.ofEpochMilli(checkedAtMillis)
            .atZone(ZoneId.systemDefault())
            .format(NIGHTLY_CHECK_TIME_FORMATTER)
    }

    fun bannerText(texts: NightlyCheckStrings): String {
        val state = if (issueCount > 0) texts.stateCheckNeeded else texts.stateGood
        val resolvedSummary = summary.ifBlank { texts.summaryNone }
        return texts.bannerFormat.format(checkedAtText(), state, resolvedSummary)
    }
}

class NightlyReliabilityCheckStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(NIGHTLY_CHECK_PREF_NAME, Context.MODE_PRIVATE)

    fun record(
        issueCount: Int,
        summary: String,
        checkedAtMillis: Long = System.currentTimeMillis()
    ) {
        prefs.edit()
            .putLong(KEY_CHECKED_AT_MILLIS, checkedAtMillis)
            .putInt(KEY_ISSUE_COUNT, issueCount.coerceAtLeast(0))
            .putString(KEY_SUMMARY, summary)
            .apply()
    }

    fun load(): NightlyReliabilityCheckStatus? {
        val checkedAtMillis = prefs.getLong(KEY_CHECKED_AT_MILLIS, 0L)
        if (checkedAtMillis <= 0L) return null

        return NightlyReliabilityCheckStatus(
            checkedAtMillis = checkedAtMillis,
            issueCount = prefs.getInt(KEY_ISSUE_COUNT, 0),
            summary = prefs.getString(KEY_SUMMARY, appContext.getString(R.string.main_reliability_summary_none)).orEmpty()
        )
    }
}