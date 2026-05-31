package com.example.shiftalarmmvp.wear

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        setContentView(buildContent())
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
    }

    private fun buildContent(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(20.dp, 20.dp, 20.dp, 20.dp)
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(13, 39, 36), Color.rgb(28, 87, 78))
            )

            addView(
                TextView(this@MainActivity).apply {
                    text = getString(R.string.setup_title)
                    setTextColor(Color.WHITE)
                    textSize = 21f
                    typeface = Typeface.DEFAULT_BOLD
                    gravity = Gravity.CENTER
                },
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )

            addView(
                TextView(this@MainActivity).apply {
                    text = getString(R.string.setup_body)
                    setTextColor(Color.rgb(214, 236, 231))
                    textSize = 13f
                    gravity = Gravity.CENTER
                    setLineSpacing(2.dp.toFloat(), 1.0f)
                },
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).withTopMargin(12.dp)
            )

            addView(
                Button(this@MainActivity).apply {
                    text = getString(R.string.setup_preview_button)
                    isAllCaps = false
                    textSize = 14f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.rgb(12, 64, 55))
                    background = GradientDrawable().apply {
                        setColor(Color.rgb(178, 235, 219))
                        cornerRadius = 24.dp.toFloat()
                    }
                    setOnClickListener { showPreviewAlarm() }
                },
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    44.dp
                ).withTopMargin(18.dp)
            )
        }
    }

    private fun showPreviewAlarm() {
        val payload = WatchAlarmPayload(
            alarmId = System.currentTimeMillis().coerceAtMost(Int.MAX_VALUE.toLong()),
            label = getString(R.string.setup_preview_label),
            snoozeMinutes = 5,
            snoozeMaxCount = 3,
            currentSnoozeCount = 0,
            soundType = "ALARM",
            customSoundUri = null,
            volumePercent = 100,
            vibrationEnabled = true,
            snoozeAllowed = true,
            triggeredAtMillis = System.currentTimeMillis()
        )
        AlarmActivity.show(this, payload)
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    private fun LinearLayout.LayoutParams.withTopMargin(value: Int): LinearLayout.LayoutParams {
        topMargin = value
        return this
    }

    companion object {
        private const val REQUEST_NOTIFICATIONS = 2001
    }
}
