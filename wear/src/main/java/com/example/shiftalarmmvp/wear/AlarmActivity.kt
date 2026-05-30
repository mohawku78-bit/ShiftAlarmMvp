package com.example.shiftalarmmvp.wear

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import java.lang.ref.WeakReference
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class AlarmActivity : Activity() {
    private var payload: WatchAlarmPayload? = null
    private var vibrator: Vibrator? = null
    private val controlAckHandler = Handler(Looper.getMainLooper())
    private var controlAckTimeout: Runnable? = null
    private var pendingControlAction: String? = null
    private var pendingControlPayload: WatchAlarmPayload? = null
    private var actionStatusText: TextView? = null
    private var stopActionButton: Button? = null
    private var snoozeActionButton: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAlarmWindow()
        activeActivity = WeakReference(this)
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun configureAlarmWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )
    }

    private fun handleIntent(intent: Intent) {
        val nextPayload = WatchAlarmProtocol.parsePayload(intent)
        if (nextPayload == null) {
            finish()
            return
        }

        clearPendingControlState()
        payload = nextPayload
        if (intent.getBooleanExtra(EXTRA_USE_LOCAL_VIBRATION, true)) {
            startVibration(nextPayload)
        } else {
            stopVibration()
        }
        setContentView(buildContent(nextPayload))
    }

    private fun buildContent(payload: WatchAlarmPayload): View {
        val root = FrameLayout(this).apply {
            setPadding(18.dp, 16.dp, 18.dp, 16.dp)
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(12, 34, 32), Color.rgb(20, 70, 62), Color.rgb(243, 138, 106))
            )
        }

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        root.addView(
            column,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        column.addView(
            TextView(this).apply {
                text = getString(R.string.alarm_title)
                setTextColor(Color.rgb(183, 239, 225))
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                includeFontPadding = false
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        column.addView(
            TextView(this).apply {
                text = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                setTextColor(Color.WHITE)
                textSize = 42f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                includeFontPadding = false
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).withTopMargin(12.dp)
        )

        column.addView(
            TextView(this).apply {
                text = payload.label.ifBlank { getString(R.string.alarm_default_label) }
                setTextColor(Color.rgb(241, 248, 245))
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                maxLines = 2
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).withTopMargin(10.dp)
        )

        column.addView(
            TextView(this).apply {
                text = if (payload.canSnooze) {
                    getString(R.string.alarm_snooze_after_minutes, payload.snoozeMinutes)
                } else {
                    getString(R.string.alarm_snooze_disabled)
                }
                setTextColor(Color.rgb(209, 230, 224))
                textSize = 12f
                gravity = Gravity.CENTER
                includeFontPadding = false
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).withTopMargin(8.dp)
        )

        actionStatusText = TextView(this).apply {
            text = ""
            visibility = View.GONE
            setTextColor(Color.rgb(255, 238, 226))
            textSize = 12f
            gravity = Gravity.CENTER
            includeFontPadding = false
        }
        column.addView(
            actionStatusText,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).withTopMargin(8.dp)
        )

        stopActionButton = createActionButton(
            label = getString(R.string.alarm_stop),
            backgroundColor = Color.rgb(255, 238, 226),
            textColor = Color.rgb(100, 42, 32),
            enabled = true
        ) {
            sendActionAndAwaitAck(WatchAlarmProtocol.PATH_ALARM_STOP)
        }
        column.addView(
            stopActionButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                46.dp
            ).withTopMargin(18.dp)
        )

        snoozeActionButton = createActionButton(
            label = if (payload.canSnooze) getString(R.string.alarm_snooze) else getString(R.string.alarm_snooze_disabled),
            backgroundColor = if (payload.canSnooze) Color.rgb(178, 235, 219) else Color.rgb(88, 107, 101),
            textColor = if (payload.canSnooze) Color.rgb(12, 64, 55) else Color.rgb(195, 208, 203),
            enabled = payload.canSnooze
        ) {
            sendActionAndAwaitAck(WatchAlarmProtocol.PATH_ALARM_SNOOZE)
        }
        column.addView(
            snoozeActionButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                46.dp
            ).withTopMargin(8.dp)
        )

        return root
    }

    private fun createActionButton(
        label: String,
        backgroundColor: Int,
        textColor: Int,
        enabled: Boolean,
        onClick: () -> Unit
    ): Button {
        return Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textColor)
            isEnabled = enabled
            background = GradientDrawable().apply {
                setColor(backgroundColor)
                cornerRadius = 24.dp.toFloat()
            }
            setOnClickListener { onClick() }
        }
    }

    private fun sendActionAndAwaitAck(path: String) {
        val currentPayload = payload ?: return
        if (pendingControlAction != null) return
        if (path == WatchAlarmProtocol.PATH_ALARM_SNOOZE && !currentPayload.canSnooze) return
        pendingControlAction = path
        pendingControlPayload = currentPayload
        PhoneMessageBridge.send(this, path, currentPayload)
        stopVibration()
        WatchAlarmRingingService.stopKeepingNotification(this)
        WatchAlarmNotifier.showControlPending(this, currentPayload, path)
        showWaitingForControlAck(path)
        scheduleControlAckTimeout(path, currentPayload)
    }

    private fun showWaitingForControlAck(path: String) {
        actionStatusText?.apply {
            text = when (path) {
                WatchAlarmProtocol.PATH_ALARM_SNOOZE -> getString(R.string.alarm_waiting_snooze_ack)
                else -> getString(R.string.alarm_waiting_stop_ack)
            }
            visibility = View.VISIBLE
        }
        setActionButtonsEnabled(false)
    }

    private fun scheduleControlAckTimeout(path: String, payload: WatchAlarmPayload) {
        controlAckTimeout?.let(controlAckHandler::removeCallbacks)
        val timeout = Runnable {
            if (pendingControlAction == path && pendingControlPayload?.alarmId == payload.alarmId) {
                restoreControlRetryAfterMissingAck(payload, ringingAlreadyRestored = false)
            }
        }
        controlAckTimeout = timeout
        controlAckHandler.postDelayed(timeout, CONTROL_ACK_TIMEOUT_MILLIS)
    }

    private fun restoreControlRetryAfterMissingAck(payload: WatchAlarmPayload, ringingAlreadyRestored: Boolean) {
        val currentPayload = this.payload ?: return
        if (currentPayload.alarmId != payload.alarmId) return
        if (currentPayload.triggeredAtMillis != payload.triggeredAtMillis) return

        Log.w(TAG, "control ack timeout in activity alarmId=${payload.alarmId}")
        pendingControlAction = null
        pendingControlPayload = null
        if (!ringingAlreadyRestored) {
            val serviceStarted = WatchAlarmRingingService.start(this, payload)
            if (!serviceStarted) {
                WatchAlarmNotifier.show(this, payload)
                startVibration(payload)
            } else {
                stopVibration()
            }
        }
        actionStatusText?.apply {
            text = getString(R.string.alarm_missing_phone_ack)
            visibility = View.VISIBLE
        }
        setActionButtonsEnabled(true)
    }

    private fun setActionButtonsEnabled(enabled: Boolean) {
        stopActionButton?.apply {
            isEnabled = enabled
            alpha = if (enabled) 1f else 0.55f
        }
        snoozeActionButton?.apply {
            isEnabled = enabled && payload?.canSnooze == true
            alpha = if (isEnabled) 1f else 0.55f
        }
    }

    private fun dismissIfControlAckMatches(action: String, ackPayload: WatchAlarmPayload) {
        val currentPayload = payload ?: return
        if (action != WatchAlarmProtocol.PATH_ALARM_STOP && action != WatchAlarmProtocol.PATH_ALARM_SNOOZE) return
        if (currentPayload.alarmId != ackPayload.alarmId) return
        if (currentPayload.triggeredAtMillis != ackPayload.triggeredAtMillis) return
        dismissLocal()
    }

    private fun clearPendingControlState() {
        controlAckTimeout?.let(controlAckHandler::removeCallbacks)
        controlAckTimeout = null
        pendingControlAction = null
        pendingControlPayload = null
    }

    private fun startVibration(payload: WatchAlarmPayload) {
        stopVibration()
        if (!payload.vibrationEnabled) return

        val vibe = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        vibrator = vibe

        val pattern = longArrayOf(0, 650, 180, 650, 420)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibe.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibe.vibrate(pattern, 0)
        }
    }

    private fun stopVibration() {
        runCatching { vibrator?.cancel() }
        vibrator = null
    }

    private fun dismissLocal() {
        clearPendingControlState()
        stopVibration()
        WatchAlarmRingingService.stop(this)
        WatchAlarmNotifier.cancel(this)
        finishAndRemoveTask()
    }

    override fun onBackPressed() {
        // The alarm should be explicitly stopped or snoozed so the phone state stays correct.
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val currentPayload = payload
        val hardwareAction = currentPayload?.let {
            WatchAlarmHardwareKeys.controlPathFor(keyCode, it.canSnooze)
        }
        if (hardwareAction != null) {
            sendActionAndAwaitAck(hardwareAction)
            return true
        }
        return if (WatchAlarmHardwareKeys.shouldConsume(keyCode)) true else super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        if (activeActivity?.get() === this) {
            activeActivity = null
        }
        clearPendingControlState()
        stopVibration()
        super.onDestroy()
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    private fun LinearLayout.LayoutParams.withTopMargin(value: Int): LinearLayout.LayoutParams {
        topMargin = value
        return this
    }

    companion object {
        private var activeActivity: WeakReference<AlarmActivity>? = null

        private const val TAG = "ShiftWearAlarm"
        private const val EXTRA_USE_LOCAL_VIBRATION = "extra_use_local_vibration"
        private const val CONTROL_ACK_TIMEOUT_MILLIS = 8_000L

        fun createIntent(
            context: Context,
            payload: WatchAlarmPayload,
            useLocalVibration: Boolean = true
        ): Intent {
            return Intent(context, AlarmActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(WatchAlarmProtocol.EXTRA_PAYLOAD_JSON, WatchAlarmProtocol.toJson(payload))
                .putExtra(EXTRA_USE_LOCAL_VIBRATION, useLocalVibration)
        }

        fun show(
            context: Context,
            payload: WatchAlarmPayload,
            useLocalVibration: Boolean = true
        ) {
            runCatching { context.startActivity(createIntent(context, payload, useLocalVibration)) }
        }

        fun dismissIfMatching(alarmId: Long) {
            activeActivity?.get()?.let { activity ->
                activity.runOnUiThread {
                    if (activity.payload?.alarmId == alarmId) {
                        activity.dismissLocal()
                    }
                }
            }
        }

        fun dismissIfControlAcknowledged(action: String, payload: WatchAlarmPayload) {
            activeActivity?.get()?.let { activity ->
                activity.runOnUiThread {
                    activity.dismissIfControlAckMatches(action, payload)
                }
            }
        }

        fun awaitControlAcknowledgement(action: String, payload: WatchAlarmPayload) {
            activeActivity?.get()?.let { activity ->
                activity.runOnUiThread {
                    if (activity.payload?.alarmId == payload.alarmId &&
                        activity.payload?.triggeredAtMillis == payload.triggeredAtMillis
                    ) {
                        activity.pendingControlAction = action
                        activity.pendingControlPayload = payload
                        activity.stopVibration()
                        activity.showWaitingForControlAck(action)
                        activity.scheduleControlAckTimeout(action, payload)
                    }
                }
            }
        }

        fun restoreAfterMissingControlAck(payload: WatchAlarmPayload, ringingAlreadyRestored: Boolean = false) {
            activeActivity?.get()?.let { activity ->
                activity.runOnUiThread {
                    if (activity.payload?.alarmId == payload.alarmId &&
                        activity.payload?.triggeredAtMillis == payload.triggeredAtMillis
                    ) {
                        activity.restoreControlRetryAfterMissingAck(payload, ringingAlreadyRestored)
                    }
                }
            }
        }
    }
}
