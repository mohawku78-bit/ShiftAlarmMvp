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
        if (!exactReady && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) issues += "정확 알람 권한"
        if (!notificationReady) issues += "알림 권한"
        if (selfTestEvent == SelfTestStatus.Event.FAILED) {
            issues += "2분 테스트 실패"
        } else if (selfTestNeedsFollowUp) {
            issues += if (selfTestStale) "2분 테스트 재확인 권장" else "2분 테스트 확인 필요"
        }
        if (!batteryReady && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) issues += "배터리 예외 미설정"
        if (recoveryNeedsAttention) issues += "일부 알람 미복구"

        if (issues.isEmpty()) {
            return ReliabilityInspectionResult(
                issueCount = 0,
                summary = "문제 없이 준비됨"
            )
        }

        val summary = if (issues.size <= 2) {
            issues.joinToString(", ")
        } else {
            "${issues.take(2).joinToString(", ")} 외 ${issues.size - 2}건"
        }

        return ReliabilityInspectionResult(
            issueCount = issues.size,
            summary = summary
        )
    }
}
