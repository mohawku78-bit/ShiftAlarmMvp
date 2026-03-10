package com.example.shiftalarmmvp.receiver

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.shiftalarmmvp.R
import com.example.shiftalarmmvp.recovery.NightlyReliabilityCheckStore
import com.example.shiftalarmmvp.recovery.ReliabilityInspector
import com.example.shiftalarmmvp.scheduler.NightlyReliabilityCheckScheduler
import com.example.shiftalarmmvp.ui.MainActivity

private const val NIGHTLY_CHECK_NOTIFICATION_CHANNEL_ID = "nightly_reliability_check"
private const val NIGHTLY_CHECK_NOTIFICATION_ID = 770043

class NightlyReliabilityCheckReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != NightlyReliabilityCheckScheduler.ACTION_NIGHTLY_RELIABILITY_CHECK) return

        NightlyReliabilityCheckScheduler.schedule(context)

        val result = ReliabilityInspector.inspect(context)
        NightlyReliabilityCheckStore(context).record(
            issueCount = result.issueCount,
            summary = result.summary
        )

        if (result.issueCount <= 0) return
        if (!canNotify(context)) return

        showIssueNotification(context, result.summary)
    }

    private fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    @SuppressLint("MissingPermission")
    private fun showIssueNotification(context: Context, summary: String) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager != null) {
            val channel = NotificationChannel(
                NIGHTLY_CHECK_NOTIFICATION_CHANNEL_ID,
                "취침 전 자동 점검",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "취침 전에 알람 신뢰도를 자동 점검하고 문제가 있으면 알려줍니다."
            }
            manager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_OPEN_RELIABILITY_CENTER, true)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            NIGHTLY_CHECK_NOTIFICATION_ID,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NIGHTLY_CHECK_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_brand_badge)
            .setContentTitle("취침 전 자동 점검: 확인 필요")
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$summary\n앱에서 추천 버튼으로 바로 조치하세요."))
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(NIGHTLY_CHECK_NOTIFICATION_ID, notification)
    }
}
