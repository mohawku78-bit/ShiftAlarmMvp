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
    private var pendingControlStartedAtMillis: Long = 0L
    private var actionStatusText: TextView? = null
    private var stopActionButton: Button? = null
    private var snoozeActionButton: Button? = null
    private var lastHardwareControlKeyCode: Int = KeyEvent.KEYCODE_UNKNOWN
    private var lastHardwareControlAtMillis: Long = 0L

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
        val pendingAction = intent.getStringExtra(EXTRA_PENDING_CONTROL_ACTION)
            ?.takeIf { isSupportedPendingControlAction(it, nextPayload) }
        val pendingStartedAtMillis = intent
            .getLongExtra(EXTRA_PENDING_CONTROL_STARTED_AT_MILLIS, 0L)
            .takeIf { it > 0L }
            ?: System.currentTimeMillis()

        clearPendingControlState()
        payload = nextPayload
        if (pendingAction != null) {
            stopVibration()
        } else if (intent.getBooleanExtra(EXTRA_USE_LOCAL_VIBRATION, true)) {
            startVibration(nextPayload)
        } else {
            stopVibration()
        }
        setContentView(buildContent(nextPayload))
        pendingAction?.let { restorePendingControlState(it, nextPayload, pendingStartedAtMillis) }
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
        val requestStartedAtMillis = System.currentTimeMillis()
        pendingControlAction = path
        pendingControlPayload = currentPayload
        pendingControlStartedAtMillis = requestStartedAtMillis
        PhoneMessageBridge.send(this, path, currentPayload)
        stopVibration()
        WatchAlarmNotifier.showControlPending(this, currentPayload, path, requestStartedAtMillis)
        showWaitingForControlAck(path)
        scheduleControlAckTimeout(path, currentPayload, requestStartedAtMillis)
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

    private fun scheduleControlAckTimeout(
        path: String,
        payload: WatchAlarmPayload,
        requestStartedAtMillis: Long
    ) {
        controlAckTimeout?.let(controlAckHandler::removeCallbacks)
        val timeout = Runnable {
            if (pendingControlAction == path &&
                pendingControlPayload?.alarmId == payload.alarmId &&
                pendingControlPayload?.triggeredAtMillis == payload.triggeredAtMillis
            ) {
                if (dismissIfStoredControlAck(path, payload, requestStartedAtMillis)) {
                    return@Runnable
                }
                restoreControlRetryAfterMissingAck(payload)
            }
        }
        controlAckTimeout = timeout
        controlAckHandler.postDelayed(timeout, CONTROL_ACK_TIMEOUT_MILLIS)
    }

    private fun restoreControlRetryAfterMissingAck(payload: WatchAlarmPayload) {
        val currentPayload = this.payload ?: return
        if (currentPayload.alarmId != payload.alarmId) return
        if (currentPayload.triggeredAtMillis != payload.triggeredAtMillis) return

        Log.w(TAG, "control ack timeout in activity powerSaverRetry alarmId=${payload.alarmId}")
        pendingControlAction = null
        pendingControlPayload = null
        stopVibration()
        WatchAlarmNotifier.show(this, payload)
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
        pendingControlStartedAtMillis = 0L
    }

    private fun restorePendingControlState(
        path: String,
        payload: WatchAlarmPayload,
        requestStartedAtMillis: Long
    ) {
        if (dismissIfStoredControlAck(path, payload, requestStartedAtMillis)) {
            return
        }
        pendingControlAction = path
        pendingControlPayload = payload
        pendingControlStartedAtMillis = requestStartedAtMillis
        showWaitingForControlAck(path)
        scheduleControlAckTimeout(path, payload, requestStartedAtMillis)
    }

    private fun dismissIfStoredControlAck(
        path: String,
        payload: WatchAlarmPayload,
        requestStartedAtMillis: Long
    ): Boolean {
        if (!WatchAlarmControlAckStore.hasAcknowledgementSince(this, path, payload, requestStartedAtMillis)) {
            return false
        }
        Log.i(TAG, "dismiss after stored control ack action=$path alarmId=${payload.alarmId}")
        dismissLocal()
        return true
    }

    private fun isSupportedPendingControlAction(path: String, payload: WatchAlarmPayload): Boolean {
        return path == WatchAlarmProtocol.PATH_ALARM_STOP ||
            (path == WatchAlarmProtocol.PATH_ALARM_SNOOZE && payload.canSnooze)
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibe.vibrate(
                VibrationEffect.createWaveform(
                    RAMP_VIBRATION_TIMINGS,
                    RAMP_VIBRATION_AMPLITUDES,
                    RAMP_VIBRATION_REPEAT_INDEX
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibe.vibrate(RAMP_VIBRATION_TIMINGS, RAMP_VIBRATION_REPEAT_INDEX)
        }
    }

    private fun stopVibration() {
        runCatching { vibrator?.cancel() }
        vibrator = null
    }

    private fun dismissLocal() {
        clearPendingControlState()
        stopVibration()
        WatchAlarmNotifier.cancel(this)
        finishAndRemoveTask()
    }

    override fun onBackPressed() {
        // The alarm should be explicitly stopped or snoozed so the phone state stays correct.
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return if (handleHardwareKey(keyCode, event, "down")) true else super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return if (handleHardwareKey(keyCode, event, "up")) true else super.onKeyUp(keyCode, event)
    }

    private fun handleHardwareKey(keyCode: Int, event: KeyEvent?, phase: String): Boolean {
        val currentPayload = payload
        val hardwareAction = currentPayload?.let {
            WatchAlarmHardwareKeys.controlPathFor(keyCode, it.canSnooze)
        }
        if (hardwareAction != null) {
            val now = System.currentTimeMillis()
            val isDuplicateUp = phase == "up" &&
                keyCode == lastHardwareControlKeyCode &&
                now - lastHardwareControlAtMillis < HARDWARE_KEY_UP_DEDUPE_MILLIS
            Log.i(
                TAG,
                "hardware key control phase=$phase keyCode=$keyCode action=$hardwareAction " +
                    "alarmId=${currentPayload.alarmId}"
            )
            if (!isDuplicateUp) {
                lastHardwareControlKeyCode = keyCode
                lastHardwareControlAtMillis = now
                sendActionAndAwaitAck(hardwareAction)
            }
            return true
        }
        if (currentPayload != null) {
            Log.i(
                TAG,
                "hardware key ignored phase=$phase keyCode=$keyCode repeat=${event?.repeatCount ?: 0} " +
                    "canSnooze=${currentPayload.canSnooze}"
            )
        }
        return WatchAlarmHardwareKeys.shouldConsume(keyCode)
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
        private const val HARDWARE_KEY_UP_DEDUPE_MILLIS = 1_000L
        private const val EXTRA_USE_LOCAL_VIBRATION = "extra_use_local_vibration"
        private const val EXTRA_PENDING_CONTROL_ACTION = "extra_pending_control_action"
        private const val EXTRA_PENDING_CONTROL_STARTED_AT_MILLIS = "extra_pending_control_started_at_millis"
        private const val CONTROL_ACK_TIMEOUT_MILLIS = 8_000L
        private val RAMP_VIBRATION_TIMINGS =
            longArrayOf(0, 45, 600, 60, 540, 85, 480, 115, 420, 155, 360, 210)
        private val RAMP_VIBRATION_AMPLITUDES =
            intArrayOf(0, 12, 0, 22, 0, 36, 0, 55, 0, 78, 0, 110)
        private const val RAMP_VIBRATION_REPEAT_INDEX = 1

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

        fun createPendingControlIntent(
            context: Context,
            payload: WatchAlarmPayload,
            action: String,
            requestStartedAtMillis: Long
        ): Intent {
            return createIntent(context, payload, useLocalVibration = false)
                .putExtra(EXTRA_PENDING_CONTROL_ACTION, action)
                .putExtra(EXTRA_PENDING_CONTROL_STARTED_AT_MILLIS, requestStartedAtMillis)
        }

        fun show(
            context: Context,
            payload: WatchAlarmPayload,
            useLocalVibration: Boolean = true
        ): Boolean {
            return runCatching {
                context.startActivity(createIntent(context, payload, useLocalVibration))
            }.onSuccess {
                Log.i(TAG, "show alarm activity alarmId=${payload.alarmId} useLocalVibration=$useLocalVibration")
            }.onFailure { error ->
                Log.w(TAG, "show alarm activity failed alarmId=${payload.alarmId}", error)
            }.isSuccess
        }

        fun dismissIfMatching(alarmId: Long, triggeredAtMillis: Long) {
            activeActivity?.get()?.let { activity ->
                activity.runOnUiThread {
                    if (activity.payload?.alarmId == alarmId &&
                        activity.payload?.triggeredAtMillis == triggeredAtMillis
                    ) {
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

        fun awaitControlAcknowledgement(
            action: String,
            payload: WatchAlarmPayload,
            requestStartedAtMillis: Long = System.currentTimeMillis()
        ) {
            activeActivity?.get()?.let { activity ->
                activity.runOnUiThread {
                    if (activity.payload?.alarmId == payload.alarmId &&
                        activity.payload?.triggeredAtMillis == payload.triggeredAtMillis
                    ) {
                        if (activity.dismissIfStoredControlAck(action, payload, requestStartedAtMillis)) {
                            return@runOnUiThread
                        }
                        activity.pendingControlAction = action
                        activity.pendingControlPayload = payload
                        activity.pendingControlStartedAtMillis = requestStartedAtMillis
                        activity.stopVibration()
                        activity.showWaitingForControlAck(action)
                        activity.scheduleControlAckTimeout(
                            action,
                            payload,
                            requestStartedAtMillis
                        )
                    }
                }
            }
        }

        fun restoreAfterMissingControlAck(payload: WatchAlarmPayload) {
            activeActivity?.get()?.let { activity ->
                activity.runOnUiThread {
                    if (activity.payload?.alarmId == payload.alarmId &&
                        activity.payload?.triggeredAtMillis == payload.triggeredAtMillis
                    ) {
                        activity.restoreControlRetryAfterMissingAck(payload)
                    }
                }
            }
        }
    }
}
