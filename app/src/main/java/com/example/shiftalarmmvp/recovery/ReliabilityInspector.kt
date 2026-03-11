package com.example.shiftalarmmvp.recovery

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

data class ReliabilityInspectionResult(
    val issueCount: Int,
    val summary: String
)

object ReliabilityInspector {
    fun inspect(context: Context): ReliabilityInspectionResult {
        val texts = recoveryStrings(context.resources).inspector
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val powerManager = context.getSystemService(PowerManager::class.java)
        val recovery = RescheduleRecoveryStore(context).load()
        val selfTest = SelfTestStatusStore(context).load()

        val exactReady = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager?.canScheduleExactAlarms() == true
        val notificationPermissionReady = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val notificationChannelReady = NotificationManagerCompat.from(context).areNotificationsEnabled()
        val notificationReady = notificationPermissionReady && notificationChannelReady
        val batteryReady = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
        val recoveryNeedsAttention = recovery?.outcome == RescheduleRecoveryState.Outcome.PARTIAL_RECOVERY

        val selfTestEvent = selfTest?.lastEvent
        val selfTestNeedsFollowUp = ReliabilityPolicy.isSelfTestFollowUpNeeded(selfTest)
        val selfTestStale = ReliabilityPolicy.isSelfTestStale(selfTest)

        val issues = mutableListOf<String>()
        if (!exactReady && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) issues += texts.issueExactAlarmPermission
        if (!notificationReady) issues += texts.issueNotificationPermission
        if (selfTestEvent == SelfTestStatus.Event.FAILED) {
            issues += texts.issueSelfTestFailed
        } else if (selfTestNeedsFollowUp) {
            issues += if (selfTestStale) texts.issueSelfTestRecheckRecommended else texts.issueSelfTestCheckNeeded
        }
        if (!batteryReady && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) issues += texts.issueBatteryNotExempt
        if (recoveryNeedsAttention) issues += texts.issueRecoveryPartial

        if (issues.isEmpty()) {
            return ReliabilityInspectionResult(
                issueCount = 0,
                summary = texts.summaryReady
            )
        }

        val summary = if (issues.size <= 2) {
            issues.joinToString(", ")
        } else {
            texts.summaryOverflowFormat.format(issues.take(2).joinToString(", "), issues.size - 2)
        }

        return ReliabilityInspectionResult(
            issueCount = issues.size,
            summary = summary
        )
    }
}