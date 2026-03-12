package com.example.shiftalarmmvp.recovery

import android.content.Context
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val RESTORE_POST_CHECK_PREF_NAME = "restore_post_check_state"
private const val KEY_RESTORED_AT_MILLIS = "restored_at_millis"
private const val KEY_VERIFIED_AT_MILLIS = "verified_at_millis"
private val RESTORE_POST_CHECK_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")

data class RestorePostCheckStatus(
    val restoredAtMillis: Long,
    val verifiedAtMillis: Long = 0L
) {
    val isPending: Boolean
        get() = restoredAtMillis > 0L && verifiedAtMillis <= 0L

    fun eventAtMillis(): Long {
        return when {
            isPending -> restoredAtMillis
            verifiedAtMillis > 0L -> verifiedAtMillis
            else -> restoredAtMillis
        }
    }

    fun eventAtText(): String {
        val millis = eventAtMillis()
        if (millis <= 0L) return "--"
        return Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .format(RESTORE_POST_CHECK_TIME_FORMATTER)
    }

    fun overviewText(strings: RestorePostCheckStrings): String {
        return if (isPending) {
            strings.linePendingFormat.format(eventAtText())
        } else {
            strings.lineCompletedFormat.format(eventAtText())
        }
    }
}

internal class RestorePostCheckStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(RESTORE_POST_CHECK_PREF_NAME, Context.MODE_PRIVATE)

    fun load(): RestorePostCheckStatus? {
        val restoredAtMillis = prefs.getLong(KEY_RESTORED_AT_MILLIS, 0L)
        if (restoredAtMillis <= 0L) return null

        return RestorePostCheckStatus(
            restoredAtMillis = restoredAtMillis,
            verifiedAtMillis = prefs.getLong(KEY_VERIFIED_AT_MILLIS, 0L)
        )
    }

    fun recordPending(restoredAtMillis: Long = System.currentTimeMillis()): RestorePostCheckStatus {
        val status = RestorePostCheckStatus(
            restoredAtMillis = restoredAtMillis,
            verifiedAtMillis = 0L
        )
        save(status)
        return status
    }

    fun completeIfPending(verifiedAtMillis: Long = System.currentTimeMillis()): RestorePostCheckStatus? {
        val current = load() ?: return null
        if (!current.isPending) return current

        val updated = current.copy(verifiedAtMillis = verifiedAtMillis)
        save(updated)
        return updated
    }

    private fun save(status: RestorePostCheckStatus) {
        prefs.edit()
            .putLong(KEY_RESTORED_AT_MILLIS, status.restoredAtMillis)
            .putLong(KEY_VERIFIED_AT_MILLIS, status.verifiedAtMillis)
            .commit()
    }
}
