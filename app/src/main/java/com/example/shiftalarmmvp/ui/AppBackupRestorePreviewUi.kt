package com.example.shiftalarmmvp.ui

import android.content.res.Resources
import com.example.shiftalarmmvp.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal data class AppBackupCurrentCounts(
    val alarmCount: Int,
    val presetCount: Int,
    val alarmLogCount: Int
)

internal data class AppBackupRestorePreviewUi(
    val schemaVersion: Int,
    val exportedAtText: String,
    val incomingCounts: AppBackupCurrentCounts,
    val currentCounts: AppBackupCurrentCounts,
    val replaceWarningText: String
)

internal data class AppBackupRestorePreviewStrings(
    val exportedAtUnknown: String,
    val replaceWarningText: String
)

private val BACKUP_RESTORE_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

internal fun appBackupRestorePreviewStrings(resources: Resources): AppBackupRestorePreviewStrings {
    return AppBackupRestorePreviewStrings(
        exportedAtUnknown = resources.getString(R.string.backup_restore_unknown_exported_at),
        replaceWarningText = resources.getString(R.string.backup_restore_replace_warning)
    )
}

internal fun buildAppBackupRestorePreviewUi(
    snapshot: AppBackupSnapshot,
    currentCounts: AppBackupCurrentCounts,
    strings: AppBackupRestorePreviewStrings,
    zoneId: ZoneId = ZoneId.systemDefault()
): AppBackupRestorePreviewUi {
    val exportedAtText = if (snapshot.exportedAtEpochMillis > 0L) {
        Instant.ofEpochMilli(snapshot.exportedAtEpochMillis)
            .atZone(zoneId)
            .format(BACKUP_RESTORE_TIME_FORMATTER)
    } else {
        strings.exportedAtUnknown
    }

    return AppBackupRestorePreviewUi(
        schemaVersion = snapshot.schemaVersion,
        exportedAtText = exportedAtText,
        incomingCounts = AppBackupCurrentCounts(
            alarmCount = snapshot.alarms.size,
            presetCount = snapshot.presets.size,
            alarmLogCount = snapshot.alarmLogs.size
        ),
        currentCounts = currentCounts,
        replaceWarningText = strings.replaceWarningText
    )
}
