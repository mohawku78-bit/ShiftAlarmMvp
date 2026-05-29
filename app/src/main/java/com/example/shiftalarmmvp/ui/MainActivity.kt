package com.example.shiftalarmmvp.ui

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings


import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.shiftalarmmvp.BuildConfig
import com.example.shiftalarmmvp.R
import com.example.shiftalarmmvp.data.AlarmRule
import com.example.shiftalarmmvp.data.AlarmSoundType
import com.example.shiftalarmmvp.data.normalizeIntervalWeeks
import com.example.shiftalarmmvp.data.normalizeWeekPatterns
import com.example.shiftalarmmvp.receiver.AlarmReceiver
import com.example.shiftalarmmvp.recovery.HomeReliabilityAction
import com.example.shiftalarmmvp.recovery.HomeReliabilityLevel
import com.example.shiftalarmmvp.recovery.HomeReliabilityPolicy
import com.example.shiftalarmmvp.recovery.HomeReliabilitySignals
import com.example.shiftalarmmvp.recovery.AlarmWatchdogStatus
import com.example.shiftalarmmvp.recovery.ReliabilityCenterPolicy
import com.example.shiftalarmmvp.recovery.ReliabilityCenterSignals
import com.example.shiftalarmmvp.recovery.ReliabilityOverviewPolicy
import com.example.shiftalarmmvp.recovery.ReliabilityOverviewSignals
import com.example.shiftalarmmvp.recovery.ReliabilityOverviewTone
import com.example.shiftalarmmvp.recovery.ReliabilitySetupPolicy
import com.example.shiftalarmmvp.recovery.ReliabilitySetupSignals
import com.example.shiftalarmmvp.recovery.NightlyReliabilityCheckStatus
import com.example.shiftalarmmvp.recovery.ReliabilityPolicy
import com.example.shiftalarmmvp.recovery.ReliabilityStateCoordinator
import com.example.shiftalarmmvp.recovery.RescheduleRecoveryState
import com.example.shiftalarmmvp.recovery.RescheduleRecoveryStore
import com.example.shiftalarmmvp.recovery.RestorePostCheckStatus
import com.example.shiftalarmmvp.recovery.RestorePostCheckStore
import com.example.shiftalarmmvp.recovery.SelfTestStatus
import com.example.shiftalarmmvp.recovery.recoveryStrings
import com.example.shiftalarmmvp.scheduler.AlarmScheduler
import com.example.shiftalarmmvp.scheduler.AlarmTimeCalculator
import com.example.shiftalarmmvp.scheduler.NightlyReliabilityCheckScheduler
import com.example.shiftalarmmvp.service.AlarmRingingService
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val HOME_BANNER_VERSION = "088"
private const val SETTINGS_NAVIGATION_LOG_TAG = "ShiftAlarmSettings"

private fun reliabilityOverviewToneColor(tone: ReliabilityOverviewTone): Color {
    return when (tone) {
        ReliabilityOverviewTone.NEUTRAL -> Color.White.copy(alpha = 0.72f)
        ReliabilityOverviewTone.SAFE -> Color(0xFF4CD37B)
        ReliabilityOverviewTone.INFO -> Color(0xFF7EA8FF)
        ReliabilityOverviewTone.CHECK -> Color(0xFFFFC061)
        ReliabilityOverviewTone.ACTION -> Color(0xFFFF7B7B)
    }
}

private fun <T : Parcelable> Intent.parcelableExtraCompat(key: String, clazz: Class<T>): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(key, clazz)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(key) as? T
    }
}

class MainActivity : ComponentActivity() {
    private enum class ReliabilityFollowUpTarget {
        EXACT_ALARM,
        BATTERY_OPTIMIZATION,
        NOTIFICATION_PERMISSION
    }
    private val vm: AlarmViewModel by viewModels()
    private val scheduler by lazy { AlarmScheduler(this) }
    private var canScheduleExact by mutableStateOf(true)
    private var canPostNotifications by mutableStateOf(true)
    private var isIgnoringBatteryOptimizationState by mutableStateOf(true)
    private val reliabilityCoordinator by lazy { ReliabilityStateCoordinator(this) }
    private val recoveryStore by lazy { RescheduleRecoveryStore(this) }
    private val restorePostCheckStore by lazy { RestorePostCheckStore(this) }
    private var rescheduleRecoveryState by mutableStateOf<RescheduleRecoveryState?>(null)
    private var restorePostCheckStatus by mutableStateOf<RestorePostCheckStatus?>(null)
    private var selfTestStatus by mutableStateOf<SelfTestStatus?>(null)
    private var nightlyCheckStatus by mutableStateOf<NightlyReliabilityCheckStatus?>(null)
    private var watchdogStatus by mutableStateOf<AlarmWatchdogStatus?>(null)
    private var wallClockRefreshToken by mutableIntStateOf(0)
    private var openReliabilityCenterRequestToken by mutableIntStateOf(0)
    private var pendingReliabilityFollowUpTarget by mutableStateOf<ReliabilityFollowUpTarget?>(null)
    private var reliabilityReceiverRegistered = false
    private var wallClockReceiverRegistered = false
    private val reliabilityStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != AlarmRingingService.ACTION_RELIABILITY_STATE_CHANGED) return
            refreshReliabilitySignals(recalculateSummary = true)
        }
    }
    private val wallClockStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_DATE_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED -> handleWallClockStateChanged()
            }
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        canPostNotifications = granted
        consumeReliabilityFollowUpFeedbackIfNeeded(fromPermissionCallback = true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refreshReliabilitySignals()
        ensureRuntimePermissions()
        vm.refreshDirectBootAlarmSnapshots()
        NightlyReliabilityCheckScheduler.schedule(this)
        consumeLaunchIntent(intent)

        setContent {
            ShiftAlarmTheme {
                AlarmScreen(
                    vm = vm,
                    canScheduleExact = canScheduleExact,
                    canPostNotifications = canPostNotifications,
                    onOpenExactAlarmSettings = { openExactAlarmSettings() },
                    onOpenBatterySettings = { openBatteryOptimizationSettings() },
                    onOpenAppDetailSettings = { openAppDetailSettings() },
                    onRequestNotificationPermission = { requestNotificationPermission() },
                    onRefreshReliabilityStatus = { refreshReliabilityStatusFromUserAction() },
                    isIgnoringBatteryOptimization = isIgnoringBatteryOptimizationState,
                    recoveryStatus = rescheduleRecoveryState,
                    restorePostCheckStatus = restorePostCheckStatus,
                    selfTestStatus = selfTestStatus,
                    nightlyCheckStatus = nightlyCheckStatus,
                    watchdogStatus = watchdogStatus,
                    wallClockRefreshToken = wallClockRefreshToken,
                    openReliabilityCenterRequestToken = openReliabilityCenterRequestToken
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeLaunchIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        registerReliabilityStateReceiverIfNeeded()
        registerWallClockStateReceiverIfNeeded()
    }

    override fun onStop() {
        unregisterWallClockStateReceiverIfNeeded()
        unregisterReliabilityStateReceiverIfNeeded()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        val previous = canScheduleExact
        refreshReliabilitySignals()
        consumeReliabilityFollowUpFeedbackIfNeeded()
        if (!previous && canScheduleExact) {
            vm.rescheduleAllEnabled()
        }
    }

    private fun refreshReliabilitySignals(
        showToast: Boolean = false,
        recalculateSummary: Boolean = false
    ) {
        refreshExactAlarmPermissionState()
        refreshNotificationPermissionState()
        refreshBatteryOptimizationState()
        refreshRecoveryState()
        refreshRestorePostCheckState()

        val snapshot = if (showToast || recalculateSummary) {
            reliabilityCoordinator.recalculateAndSnapshot()
        } else {
            reliabilityCoordinator.snapshot()
        }
        applyReliabilitySnapshot(snapshot)

        if (showToast) {
            val summary = snapshot.nightlyCheckStatus?.summary ?: getString(R.string.main_reliability_summary_none)
            Toast.makeText(this, getString(R.string.main_reliability_summary_complete_format, summary), Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleWallClockStateChanged() {
        wallClockRefreshToken += 1
        refreshReliabilitySignals(recalculateSummary = true)
        lifecycleScope.launch {
            delay(750)
            if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return@launch
            wallClockRefreshToken += 1
            refreshReliabilitySignals(recalculateSummary = true)
        }
    }
    private fun registerReliabilityStateReceiverIfNeeded() {
        if (reliabilityReceiverRegistered) return
        val filter = IntentFilter(AlarmRingingService.ACTION_RELIABILITY_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(reliabilityStateReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(reliabilityStateReceiver, filter)
        }
        reliabilityReceiverRegistered = true
    }
    private fun unregisterReliabilityStateReceiverIfNeeded() {
        if (!reliabilityReceiverRegistered) return
        runCatching { unregisterReceiver(reliabilityStateReceiver) }
        reliabilityReceiverRegistered = false
    }
    private fun registerWallClockStateReceiverIfNeeded() {
        if (wallClockReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(wallClockStateReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(wallClockStateReceiver, filter)
        }
        wallClockReceiverRegistered = true
    }
    private fun unregisterWallClockStateReceiverIfNeeded() {
        if (!wallClockReceiverRegistered) return
        runCatching { unregisterReceiver(wallClockStateReceiver) }
        wallClockReceiverRegistered = false
    }
    private fun refreshExactAlarmPermissionState() {
        canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            scheduler.canScheduleExactAlarms()
        } else {
            true
        }
    }

    private fun ensureRuntimePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !canPostNotifications) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun refreshNotificationPermissionState() {
        canPostNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
    private fun refreshBatteryOptimizationState() {
        isIgnoringBatteryOptimizationState = isIgnoringBatteryOptimization()
    }

    private fun refreshRecoveryState() {
        rescheduleRecoveryState = recoveryStore.load()
    }

    private fun refreshRestorePostCheckState() {
        restorePostCheckStatus = restorePostCheckStore.load()
    }

    private fun applyReliabilitySnapshot(snapshot: com.example.shiftalarmmvp.recovery.ReliabilityStateSnapshot) {
        selfTestStatus = snapshot.selfTestStatus
        nightlyCheckStatus = snapshot.nightlyCheckStatus
        watchdogStatus = snapshot.watchdogStatus
    }

    private fun refreshReliabilityStatusFromUserAction() {
        restorePostCheckStore.completeIfPending()?.let { restorePostCheckStatus = it }
        refreshReliabilitySignals(showToast = true)
    }

    internal fun handleBackupRestoreApplied() {
        restorePostCheckStatus = restorePostCheckStore.recordPending()
        refreshReliabilitySignals()
        openReliabilityCenterRequestToken += 1
    }

    private fun consumeLaunchIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_RELIABILITY_CENTER, false) != true) return
        openReliabilityCenterRequestToken += 1
        intent.removeExtra(EXTRA_OPEN_RELIABILITY_CENTER)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pendingReliabilityFollowUpTarget = ReliabilityFollowUpTarget.NOTIFICATION_PERMISSION
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            Toast.makeText(
                this,
                getString(R.string.main_exact_alarm_not_required),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        refreshExactAlarmPermissionState()
        if (canScheduleExact) {
            Toast.makeText(this, getString(R.string.main_exact_alarm_ready), Toast.LENGTH_SHORT).show()
            vm.rescheduleAllEnabled()
            return
        }

        pendingReliabilityFollowUpTarget = ReliabilityFollowUpTarget.EXACT_ALARM
        val packageUri = Uri.parse("package:$packageName")
        val candidates = listOf(
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = packageUri
            },
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM),
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = packageUri
            }
        )

        val opened = tryStartActivityIntent(candidates)
        if (!opened) {
            pendingReliabilityFollowUpTarget = null
            Toast.makeText(
                this,
                getString(R.string.main_exact_alarm_settings_unavailable),
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val guidance = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getString(R.string.main_exact_alarm_settings_guidance)
        } else {
            getString(R.string.main_exact_alarm_settings_opening)
        }
        Toast.makeText(this, guidance, Toast.LENGTH_LONG).show()
    }

    private fun openBatteryOptimizationSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Toast.makeText(
                this,
                getString(R.string.main_battery_optimization_not_required),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        pendingReliabilityFollowUpTarget = ReliabilityFollowUpTarget.BATTERY_OPTIMIZATION
        val appPackage = packageName
        val packageUri = Uri.parse("package:$appPackage")
        val appLabel = applicationInfo.loadLabel(packageManager).toString()
        Log.i(SETTINGS_NAVIGATION_LOG_TAG, "battery settings button tapped")
        Toast.makeText(this, getString(R.string.main_battery_settings_opening), Toast.LENGTH_SHORT).show()

        val candidates = mutableListOf<Intent>()
        candidates += vendorBatteryIntents(appPackage, appLabel)
        candidates += Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = packageUri
        }

        val opened = tryStartActivityIntent(candidates)
        if (!opened) {
            pendingReliabilityFollowUpTarget = null
            Toast.makeText(
                this,
                getString(R.string.main_battery_settings_unavailable),
                Toast.LENGTH_LONG
            ).show()
        }
    }


    private fun consumeReliabilityFollowUpFeedbackIfNeeded(fromPermissionCallback: Boolean = false) {
        val target = pendingReliabilityFollowUpTarget ?: return
        if (fromPermissionCallback && target != ReliabilityFollowUpTarget.NOTIFICATION_PERMISSION) return

        val (label, resolved) = when (target) {
            ReliabilityFollowUpTarget.EXACT_ALARM -> {
                val ok = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || canScheduleExact
                getString(R.string.main_follow_up_exact_alarm_permission) to ok
            }
            ReliabilityFollowUpTarget.BATTERY_OPTIMIZATION -> {
                val ok = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || isIgnoringBatteryOptimizationState
                getString(R.string.main_follow_up_battery_optimization) to ok
            }
            ReliabilityFollowUpTarget.NOTIFICATION_PERMISSION -> {
                val ok = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || canPostNotifications
                getString(R.string.main_follow_up_notification_permission) to ok
            }
        }

        val recoveryTextSet = recoveryStrings(resources)
        val setupUi = ReliabilitySetupPolicy.build(
            signals = ReliabilitySetupSignals(
                exactReady = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || canScheduleExact,
                notificationReady = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || canPostNotifications,
                batteryReady = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || isIgnoringBatteryOptimizationState
            ),
            texts = recoveryTextSet.setup
        )
        val message = when {
            target == ReliabilityFollowUpTarget.EXACT_ALARM && !resolved ->
                getString(R.string.main_follow_up_exact_alarm_still_needed)
            resolved && setupUi.primaryStep != null -> getString(
                R.string.main_follow_up_resolved_next_format,
                label,
                setupUi.primaryStep.title
            )
            resolved -> getString(R.string.main_follow_up_ready)
            else -> getString(R.string.main_follow_up_still_needed_format, label)
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        pendingReliabilityFollowUpTarget = null
    }
    private fun vendorBatteryIntents(appPackage: String, appLabel: String): List<Intent> {
        return when (batteryGuideManufacturerKey(Build.MANUFACTURER, Build.BRAND)) {
            "xiaomi" -> listOf(
                Intent().setClassName(
                    "com.miui.powerkeeper",
                    "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"
                ).putExtra("package_name", appPackage)
                    .putExtra("package_label", appLabel),
                Intent("miui.intent.action.APP_PERM_EDITOR").setClassName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.permissions.PermissionsEditorActivity"
                ).putExtra("extra_pkgname", appPackage)
            )
            "oppo",
            "realme",
            "oneplus" -> listOf(
                Intent().setClassName(
                    "com.coloros.oppoguardelf",
                    "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity"
                ).putExtra("packageName", appPackage)
                    .putExtra("pkgName", appPackage),
                Intent().setClassName(
                    "com.oplus.battery",
                    "com.oplus.powermanager.fuelgaue.PowerUsageModelActivity"
                ).putExtra("packageName", appPackage)
                    .putExtra("pkgName", appPackage)
            )
            "vivo" -> listOf(
                Intent().setClassName(
                    "com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                ).putExtra("packagename", appPackage),
                Intent().setClassName(
                    "com.iqoo.secure",
                    "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"
                ).putExtra("packagename", appPackage)
            )
            "huawei" -> listOf(
                Intent().setClassName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.optimize.process.ProtectActivity"
                ),
                Intent().setClassName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                )
            )
            "samsung" -> listOf(
                Intent("com.samsung.android.sm.ACTION_BATTERY").putExtra("package_name", appPackage),
                Intent().setClassName(
                    "com.samsung.android.lool",
                    "com.samsung.android.sm.ui.battery.BatteryActivity"
                ),
                Intent().setClassName(
                    "com.samsung.android.sm",
                    "com.samsung.android.sm.app.dashboard.SmartManagerDashBoardActivity"
                )
            )
            else -> emptyList()
        }
    }
    private fun tryStartActivityIntent(intent: Intent): Boolean {
        val action = intent.action ?: "(none)"
        val component = intent.component?.flattenToShortString() ?: "(none)"
        val data = intent.dataString ?: "(none)"
        return runCatching { startActivity(intent) }
            .onSuccess {
                Log.i(SETTINGS_NAVIGATION_LOG_TAG, "opened action=$action component=$component data=$data")
            }
            .onFailure { error ->
                Log.w(SETTINGS_NAVIGATION_LOG_TAG, "failed action=$action component=$component data=$data", error)
            }
            .isSuccess
    }

    private fun tryStartActivityIntent(intents: List<Intent>): Boolean {
        for (intent in intents) {
            if (tryStartActivityIntent(intent)) return true
        }
        return false
    }
    private fun openAppDetailSettings() {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
        )
    }

    private fun isIgnoringBatteryOptimization(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(PowerManager::class.java)
            pm?.isIgnoringBatteryOptimizations(packageName) == true
        } else {
            true
        }
    }
    companion object {
        const val EXTRA_OPEN_RELIABILITY_CENTER = "extra_open_reliability_center"
    }
}

@Composable
private fun AlarmScreen(
    vm: AlarmViewModel,
    canScheduleExact: Boolean,
    canPostNotifications: Boolean,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenAppDetailSettings: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRefreshReliabilityStatus: () -> Unit,
    isIgnoringBatteryOptimization: Boolean,
    recoveryStatus: RescheduleRecoveryState?,
    restorePostCheckStatus: RestorePostCheckStatus?,
    selfTestStatus: SelfTestStatus?,
    nightlyCheckStatus: NightlyReliabilityCheckStatus?,
    watchdogStatus: AlarmWatchdogStatus?,
    wallClockRefreshToken: Int,
    openReliabilityCenterRequestToken: Int
) {
    val alarms by vm.alarms.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val recoveryTextSet = remember(context) { recoveryStrings(context.resources) }
    val backupRestorePreviewTextSet = remember(context) { appBackupRestorePreviewStrings(context.resources) }
    val reliabilityCoordinator = remember(context) { ReliabilityStateCoordinator(context) }
    val uiScheduler = remember(context) { AlarmScheduler(context) }
    val selfTestActionHandler = remember(context, reliabilityCoordinator) { SelfTestActionHandler(context, reliabilityCoordinator) }
    var selfTestStatusState by remember { mutableStateOf(selfTestStatus) }
    var nightlyCheckStatusState by remember { mutableStateOf(nightlyCheckStatus) }
    var wallClockTimeline by remember { mutableStateOf(captureWallClockTimelineSnapshot()) }
    val scope = rememberCoroutineScope()
    val currentWallClockNow = wallClockTimeline.now
    val currentWallClockZoneId = wallClockTimeline.zoneId

    fun refreshWallClockTimeline(force: Boolean = false) {
        val current = captureWallClockTimelineSnapshot()
        if (force || shouldRefreshWallClockTimeline(wallClockTimeline, current)) {
            wallClockTimeline = current
        }
    }

    LaunchedEffect(wallClockRefreshToken) {
        refreshWallClockTimeline(force = true)
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            refreshWallClockTimeline()
        }
    }
    val recoveryActionLabel = when (recoveryStatus?.outcome) {
        RescheduleRecoveryState.Outcome.DEGRADED_RECOVERY,
        RescheduleRecoveryState.Outcome.PARTIAL_RECOVERY -> stringResource(R.string.main_recovery_action_reschedule)
        RescheduleRecoveryState.Outcome.FULL_RECOVERY,
        RescheduleRecoveryState.Outcome.NO_ACTIVE_ALARMS -> stringResource(R.string.main_recovery_action_open_check)
        null -> null
    }
    val recoverySummaryClickable = recoveryActionLabel != null
    var recoveryActionBlockedUntilMillis by remember { mutableStateOf(0L) }
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val isCompactTodayBanner = screenWidthDp <= 380
    val isSamsungDevice = remember { Build.MANUFACTURER.lowercase(Locale.ROOT).contains("samsung") }
    val setupPrefs = remember(context) {
        context.getSharedPreferences("first_setup_wizard", android.content.Context.MODE_PRIVATE)
    }
    var setupWizardDismissed by remember { mutableStateOf(setupPrefs.getBoolean("dismissed", false)) }
    var shiftQuickSetupDone by remember { mutableStateOf(setupPrefs.getBoolean("shift_quick_setup_done", false)) }
    var shiftQuickSetupHidden by remember { mutableStateOf(setupPrefs.getBoolean("shift_quick_setup_hidden", false)) }
    var persistedShiftCategory by remember {
        mutableStateOf(
            runCatching {
                ShiftCategory.valueOf(
                    setupPrefs.getString("selected_shift_category", ShiftCategory.THREE_SHIFT.name)
                        ?: ShiftCategory.THREE_SHIFT.name
                )
            }.getOrDefault(ShiftCategory.THREE_SHIFT)
        )
    }

    var selectedTime by remember { mutableStateOf(LocalTime.of(7, 0)) }
    var selectedLabel by remember { mutableStateOf("") }
    var intervalWeeks by remember { mutableIntStateOf(2) }
    var activeWeekIndex by remember { mutableIntStateOf(0) }
    var weekPatterns by remember {
        mutableStateOf(
            listOf(
                setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY, DayOfWeek.SUNDAY),
                setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY),
                emptySet(),
                emptySet(),
                emptySet(),
                emptySet()
            )
        )
    }

    fun applyIntervalWeeks(targetInterval: Int, patterns: List<Set<DayOfWeek>> = weekPatterns) {
        val normalizedInterval = normalizeIntervalWeeks(targetInterval)
        intervalWeeks = normalizedInterval
        weekPatterns = normalizeWeekPatterns(normalizedInterval, patterns)
        activeWeekIndex = activeWeekIndex.coerceIn(0, normalizedInterval - 1)
    }

    var selectedSoundType by remember { mutableStateOf(AlarmSoundType.ALARM) }
    var selectedCustomSoundUri by remember { mutableStateOf<String?>(null) }
    var customSoundMessage by remember { mutableStateOf("") }
    var selectedVolume by remember { mutableFloatStateOf(100f) }
    var selectedSnoozeMinutes by remember { mutableIntStateOf(5) }
    var customSnoozeInput by remember { mutableStateOf("5") }
    var selectedSnoozeMaxCount by remember { mutableIntStateOf(0) }
    var customMaxCountInput by remember { mutableStateOf("0") }
    var vibrationEnabled by remember { mutableStateOf(true) }
    var editingAlarmId by remember { mutableStateOf<Long?>(null) }
    var editingEnabled by remember { mutableStateOf(true) }
    var showNext10 by remember { mutableStateOf(false) }
    var autoSaveOnDuplicate by remember { mutableStateOf(false) }
    var exceptionDate by remember { mutableStateOf(LocalDate.now()) }
    var skipDates by remember { mutableStateOf<Set<LocalDate>>(emptySet()) }
    var addDates by remember { mutableStateOf<Set<LocalDate>>(emptySet()) }
    var presetNameInput by remember { mutableStateOf("") }
    var presetFeedbackMessage by remember { mutableStateOf("") }
    var backupFeedbackMessage by remember { mutableStateOf("") }
    var pendingBackupRestoreSnapshot by remember { mutableStateOf<AppBackupSnapshot?>(null) }
    var pendingBackupRestorePreview by remember { mutableStateOf<AppBackupRestorePreviewUi?>(null) }
    var backupRestoreInFlight by remember { mutableStateOf(false) }
    var manualRescheduleInFlight by remember { mutableStateOf(false) }
    var reliabilityStatusRefreshToken by remember { mutableIntStateOf(0) }
    var importMergeMode by remember { mutableStateOf(true) }
    val isFirstSetupWizardActive = !shiftQuickSetupDone && !shiftQuickSetupHidden
    var currentPage by remember { mutableStateOf(if (isFirstSetupWizardActive) AlarmPage.PATTERN else AlarmPage.TODAY) }
    val scrollState = remember(currentPage) { ScrollState(initial = 0) }
    var editorForcedStep by remember { mutableStateOf<Int?>(null) }
    var selfTestMessage by remember { mutableStateOf("") }


    fun openReliabilityCenter() {
        editorForcedStep = 3
        currentPage = AlarmPage.EDITOR
        scope.launch { scrollState.animateScrollTo(0) }
    }

    LaunchedEffect(isFirstSetupWizardActive, currentPage) {
        if (isFirstSetupWizardActive && currentPage != AlarmPage.PATTERN) {
            currentPage = AlarmPage.PATTERN
        }
    }

    BackHandler(enabled = isFirstSetupWizardActive && currentPage != AlarmPage.PATTERN) {
        currentPage = AlarmPage.PATTERN
    }

    BackHandler(enabled = currentPage == AlarmPage.PRESET) {
        currentPage = AlarmPage.PATTERN
    }

    LaunchedEffect(openReliabilityCenterRequestToken) {
        if (openReliabilityCenterRequestToken > 0) {
            openReliabilityCenter()
        }
    }

    val defaultDayType = stringResource(R.string.main_default_type_day)
    val defaultNightType = stringResource(R.string.main_default_type_night)
    val defaultOffType = stringResource(R.string.main_default_type_off)
    val defaultRestType = stringResource(R.string.main_default_type_rest)
    val defaultRotationTypes = remember(defaultDayType, defaultNightType, defaultOffType, defaultRestType) {
        listOf(defaultDayType, defaultNightType, defaultOffType, defaultRestType)
    }
    fun currentShiftSetupDefaults() = ShiftSetupDefaults(
        defaultRotationTypes = defaultRotationTypes,
        initialCategory = persistedShiftCategory
    )
    var shiftSetupDraft by remember(defaultRotationTypes) {
        mutableStateOf(createInitialShiftSetupDraft(currentShiftSetupDefaults()))
    }
    val selectedShiftCategory = shiftSetupDraft.selectedCategory
    val infiniteRotationEnabled = shiftSetupDraft.infiniteRotationEnabled
    val anchorDate = shiftSetupDraft.anchorDate

    fun updateShiftSetupDraft(transform: (ShiftSetupDraft) -> ShiftSetupDraft) {
        val updated = transform(shiftSetupDraft).normalized()
        if (updated.selectedCategory != persistedShiftCategory) {
            persistedShiftCategory = updated.selectedCategory
            setupPrefs.edit().putString("selected_shift_category", updated.selectedCategory.name).apply()
        }
        shiftSetupDraft = updated
    }

    fun reopenPatternSetup() {
        shiftQuickSetupDone = false
        shiftQuickSetupHidden = false
        updateShiftSetupDraft { it.reduce(ShiftSetupAction.ReopenWizard, currentShiftSetupDefaults()) }
        setupPrefs.edit()
            .putBoolean("shift_quick_setup_done", false)
            .putBoolean("shift_quick_setup_hidden", false)
            .apply()
        currentPage = AlarmPage.PATTERN
    }
    val presetStore = remember(context) { RotationPresetStore(context) }
    val savedPresets = remember { mutableStateListOf<RotationPreset>() }
    val alarmLogStore = remember(context) { AlarmLogStore(context) }
    val alarmLogs = remember { mutableStateListOf<AlarmLogEntry>() }
    val latestRecoveryActionEntry = alarmLogs.firstOrNull { it.type == AlarmLogType.MANUAL_RECOVERY_ACTION }
    val latestRecoveryActionText = latestRecoveryActionEntry?.let { entry ->
        val ts = entry.toLocalDateTime(currentWallClockZoneId).format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
        context.getString(R.string.main_latest_recovery_action_format, ts, entry.detail)
    }
    val reliabilityOverviewUi = ReliabilityOverviewPolicy.build(
        signals = ReliabilityOverviewSignals(
            watchdogStatus = watchdogStatus,
            restorePostCheckStatus = restorePostCheckStatus,
            recoveryStatus = recoveryStatus,
            latestRecoveryActionText = latestRecoveryActionText,
            selfTestStatus = selfTestStatusState,
            nightlyCheckStatus = nightlyCheckStatusState
        ),
        texts = recoveryTextSet
    )

    var pendingUndoMessage by remember { mutableStateOf<String?>(null) }
    var pendingUndoSnapshot by remember { mutableStateOf<Map<Long, Pair<Set<LocalDate>, Set<LocalDate>>>?>(null) }
    var pendingUndoToken by remember { mutableIntStateOf(0) }

    val hostActivity = context as? MainActivity

    fun refreshAlarmLogs() {
        alarmLogs.clear()
        alarmLogs.addAll(alarmLogStore.recent(50))
    }

    fun dismissPendingBackupRestore() {
        if (backupRestoreInFlight) return
        pendingBackupRestoreSnapshot = null
        pendingBackupRestorePreview = null
    }

    fun confirmPendingBackupRestore() {
        if (backupRestoreInFlight) return
        val snapshot = pendingBackupRestoreSnapshot ?: return
        backupRestoreInFlight = true
        scope.launch {
            val restoreResult = runCatching {
                vm.replaceAllAlarms(snapshot.alarms)
                presetStore.replaceAll(snapshot.presets.map { it.normalized() })
                alarmLogStore.replaceAll(snapshot.alarmLogs)
                savedPresets.clear()
                savedPresets.addAll(presetStore.load())
                refreshAlarmLogs()
                editingAlarmId = null
                editingEnabled = true
                selectedLabel = ""
                exceptionDate = LocalDate.now()
                skipDates = emptySet()
                addDates = emptySet()
                presetNameInput = ""
                pendingUndoMessage = null
                pendingUndoSnapshot = null
                pendingUndoToken += 1
                hostActivity?.handleBackupRestoreApplied()
                snapshot
            }
            backupFeedbackMessage = if (restoreResult.isSuccess) {
                context.getString(
                    R.string.main_backup_import_success_format,
                    snapshot.alarms.size,
                    snapshot.presets.size,
                    snapshot.alarmLogs.size
                )
            } else {
                context.getString(
                    R.string.main_backup_import_failure_format,
                    restoreResult.exceptionOrNull()?.message ?: context.getString(R.string.main_feedback_unknown_reason)
                )
            }
            if (restoreResult.isSuccess) {
                pendingBackupRestoreSnapshot = null
                pendingBackupRestorePreview = null
            }
            backupRestoreInFlight = false
        }
    }

    fun snapshotExceptions(): Map<Long, Pair<Set<LocalDate>, Set<LocalDate>>> {
        return alarms.associate { alarm ->
            alarm.id to (alarm.skipDateEpochDays.toSet() to alarm.addDateEpochDays.toSet())
        }
    }

    fun pushUndo(message: String, snapshot: Map<Long, Pair<Set<LocalDate>, Set<LocalDate>>>) {
        pendingUndoMessage = message
        pendingUndoSnapshot = snapshot
        pendingUndoToken += 1
    }

    fun appendRecoveryActionLog(detail: String) {
        alarmLogStore.append(
            alarmId = -1,
            label = context.getString(R.string.main_log_label_home_reliability),
            type = AlarmLogType.MANUAL_RECOVERY_ACTION,
            detail = detail
        )
        refreshAlarmLogs()
    }
    fun requestManualReschedule(
        startMessage: String? = null,
        logDetail: String? = null,
        showCompletionToast: Boolean = false
    ) {
        if (manualRescheduleInFlight) {
            Toast.makeText(
                context,
                context.getString(R.string.main_toast_retry_later),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        manualRescheduleInFlight = true
        startMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }

        vm.rescheduleAllEnabled {
            manualRescheduleInFlight = false
            reliabilityStatusRefreshToken += 1
            logDetail?.let { appendRecoveryActionLog(it) }
            if (showCompletionToast) {
                Toast.makeText(
                    context,
                    context.getString(R.string.main_toast_reliability_refreshed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    fun registerCalendarChange(
        message: String,
        type: AlarmLogType,
        detail: String,
        apply: () -> Unit
    ) {
        val snapshot = snapshotExceptions()
        apply()
        alarmLogStore.append(
            alarmId = -1,
            label = context.getString(R.string.main_log_label_calendar),
            type = type,
            detail = detail
        )
        refreshAlarmLogs()
        pushUndo(message, snapshot)
    }
    fun openEditorWithDraft(draft: AlarmEditorDraft) {
        editorForcedStep = null
        editingAlarmId = draft.editingAlarmId
        editingEnabled = draft.editingEnabled
        selectedTime = draft.selectedTime
        selectedLabel = draft.selectedLabel
        activeWeekIndex = 0
        applyIntervalWeeks(draft.intervalWeeks, draft.weekPatterns)
        updateShiftSetupDraft { it.copy(anchorDate = draft.anchorDate) }
        selectedSoundType = draft.selectedSoundType
        selectedCustomSoundUri = draft.selectedCustomSoundUri
        selectedVolume = draft.selectedVolume
        selectedSnoozeMinutes = draft.selectedSnoozeMinutes
        customSnoozeInput = draft.selectedSnoozeMinutes.toString()
        selectedSnoozeMaxCount = draft.selectedSnoozeMaxCount
        customMaxCountInput = draft.selectedSnoozeMaxCount.toString()
        vibrationEnabled = draft.vibrationEnabled
        skipDates = draft.skipDates
        addDates = draft.addDates
        exceptionDate = draft.exceptionDate
        currentPage = AlarmPage.EDITOR
        scope.launch { scrollState.animateScrollTo(0) }
    }

    LaunchedEffect(selfTestStatus) {
        selfTestStatusState = selfTestStatus
    }

    LaunchedEffect(nightlyCheckStatus) {
        nightlyCheckStatusState = nightlyCheckStatus
    }

    LaunchedEffect(Unit) {
        savedPresets.clear()
        savedPresets.addAll(presetStore.load())
        refreshAlarmLogs()
    }

    LaunchedEffect(pendingUndoToken) {
        if (pendingUndoMessage == null) return@LaunchedEffect
        val token = pendingUndoToken
        delay(10_000)
        if (token == pendingUndoToken) {
            pendingUndoMessage = null
            pendingUndoSnapshot = null
        }
    }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val pickedUri = result.data?.parcelableExtraCompat(
                RingtoneManager.EXTRA_RINGTONE_PICKED_URI,
                Uri::class.java
            )
            if (pickedUri != null) {
                maybePersistReadPermission(context, result.data, pickedUri)
                if (isUriPlayable(context, pickedUri)) {
                    selectedCustomSoundUri = pickedUri.toString()
                    selectedSoundType = AlarmSoundType.CUSTOM
                    customSoundMessage = context.getString(R.string.main_custom_sound_saved)
                } else {
                    selectedCustomSoundUri = null
                    selectedSoundType = AlarmSoundType.ALARM
                    customSoundMessage = context.getString(R.string.main_custom_sound_fallback)
                }
            }
        }
    }

    val exportPresetLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@rememberLauncherForActivityResult
            val exportResult = runCatching {
                val payload = presetStore.exportJson()
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(payload.toByteArray(Charsets.UTF_8))
                } ?: error(context.getString(R.string.main_output_stream_unavailable))
            }
            presetFeedbackMessage = if (exportResult.isSuccess) {
                context.getString(R.string.main_preset_export_success)
            } else {
                context.getString(R.string.main_preset_export_failure_format, exportResult.exceptionOrNull()?.message ?: context.getString(R.string.main_feedback_unknown_reason))
            }
        }
    }

    val importPresetLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@rememberLauncherForActivityResult
            maybePersistReadPermission(context, result.data, uri)
            val importResult = runCatching {
                val content = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText() }
                    ?: error(context.getString(R.string.main_input_stream_unavailable))
                presetStore.importJson(content, merge = importMergeMode)
            }
            if (importResult.isSuccess) {
                savedPresets.clear()
                savedPresets.addAll(presetStore.load())
                val modeLabel = if (importMergeMode) {
                    context.getString(R.string.main_preset_import_mode_merge)
                } else {
                    context.getString(R.string.main_preset_import_mode_replace)
                }
                presetFeedbackMessage = context.getString(
                    R.string.main_preset_import_success_format,
                    importResult.getOrDefault(0),
                    modeLabel
                )
            } else {
                presetFeedbackMessage = context.getString(R.string.main_preset_import_failure_format, importResult.exceptionOrNull()?.message ?: context.getString(R.string.main_feedback_unknown_reason))
            }
        }
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@rememberLauncherForActivityResult
            val exportResult = runCatching {
                val presets = presetStore.load()
                val alarmEntries = alarmLogStore.allEntries()
                val payload = AppBackupCodec.exportJson(
                    alarms = alarms,
                    presets = presets,
                    alarmLogs = alarmEntries
                )
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(payload.toByteArray(Charsets.UTF_8))
                } ?: error(context.getString(R.string.main_output_stream_unavailable))
                Triple(alarms.size, presets.size, alarmEntries.size)
            }
            backupFeedbackMessage = if (exportResult.isSuccess) {
                val counts = exportResult.getOrThrow()
                context.getString(R.string.main_backup_export_success_format, counts.first, counts.second, counts.third)
            } else {
                context.getString(R.string.main_backup_export_failure_format, exportResult.exceptionOrNull()?.message ?: context.getString(R.string.main_feedback_unknown_reason))
            }
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@rememberLauncherForActivityResult
            maybePersistReadPermission(context, result.data, uri)
            scope.launch {
                val importResult = runCatching {
                    val content = context.contentResolver.openInputStream(uri)
                        ?.bufferedReader(Charsets.UTF_8)
                        ?.use { it.readText() }
                        ?: error(context.getString(R.string.main_input_stream_unavailable))
                    val snapshot = AppBackupCodec.parseJson(content, appBackupParseMessages(context.resources))
                    val preview = buildAppBackupRestorePreviewUi(
                        snapshot = snapshot,
                        currentCounts = AppBackupCurrentCounts(
                            alarmCount = alarms.size,
                            presetCount = presetStore.load().size,
                            alarmLogCount = alarmLogStore.allEntries().size
                        ),
                        strings = backupRestorePreviewTextSet
                    )
                    snapshot to preview
                }
                if (importResult.isSuccess) {
                    val parsed = importResult.getOrThrow()
                    pendingBackupRestoreSnapshot = parsed.first
                    pendingBackupRestorePreview = parsed.second
                    backupFeedbackMessage = ""
                } else {
                    pendingBackupRestoreSnapshot = null
                    pendingBackupRestorePreview = null
                    backupFeedbackMessage = context.getString(R.string.main_backup_import_failure_format, importResult.exceptionOrNull()?.message ?: context.getString(R.string.main_feedback_unknown_reason))
                }
            }
        }
    }
    val visiblePatterns = normalizeWeekPatterns(intervalWeeks, weekPatterns)
    val canSave = visiblePatterns.any { it.isNotEmpty() }
    val exactReady = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || canScheduleExact
    val batteryReady = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || isIgnoringBatteryOptimization
    val notificationReady = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || canPostNotifications
    val selfTestEvent = selfTestStatusState?.lastEvent
    val recoveryNeedsAttention = recoveryStatus?.needsAttention == true
    val selfTestNeedsFollowUp = ReliabilityPolicy.isSelfTestFollowUpNeeded(selfTestStatusState)
    val homeNextTrigger = remember(alarms, wallClockTimeline.localMinute, currentWallClockZoneId) {
        val now = currentWallClockNow
        alarms.asSequence()
            .filter { it.enabled }
            .mapNotNull { AlarmTimeCalculator.nextTrigger(it, now) }
            .minOrNull()
    }
    val registeredNextAlarmReady = remember(homeNextTrigger, exactReady, reliabilityStatusRefreshToken, currentWallClockZoneId) {
        if (!exactReady || homeNextTrigger == null) {
            true
        } else {
            val expectedTriggerMillis = homeNextTrigger
                .atZone(currentWallClockZoneId)
                .toInstant()
                .toEpochMilli()
            val registeredTriggerMillis = uiScheduler.nextOwnedAlarmClockTriggerMillis() ?: return@remember false
            kotlin.math.abs(registeredTriggerMillis - expectedTriggerMillis) <= 60_000L
        }
    }
    val shouldCheckAlarmRegistration = exactReady && homeNextTrigger != null
    val reliabilityUi = HomeReliabilityPolicy.evaluate(
        signals = HomeReliabilitySignals(
            exactReady = exactReady,
            notificationReady = notificationReady,
            batteryReady = batteryReady,
            nextAlarmRegisteredReady = registeredNextAlarmReady,
            shouldCheckAlarmRegistration = shouldCheckAlarmRegistration,
            recoveryNeedsAttention = recoveryNeedsAttention,
            watchdogStatus = watchdogStatus,
            restorePostCheckStatus = restorePostCheckStatus,
            selfTestEvent = selfTestEvent,
            selfTestNeedsFollowUp = selfTestNeedsFollowUp,
            nightlyIssueCount = nightlyCheckStatusState?.issueCount ?: 0
        ),
        texts = recoveryTextSet
    )
    val batteryGuideCatalog = remember(context) { context.loadBatteryGuideCatalog() }
    val currentBatteryGuide = remember(context, batteryGuideCatalog) {
        batteryGuideCatalog?.findGuideForManufacturer(
            manufacturer = Build.MANUFACTURER,
            brand = Build.BRAND
        ) ?: context.buildFallbackBatteryGuide()
    }
    val batteryGuideSummaryText = if (!batteryReady) {
        currentBatteryGuide.shortDescription
    } else {
        null
    }
    val reliabilityCenterUi = ReliabilityCenterPolicy.build(
        signals = ReliabilityCenterSignals(
            exactReady = exactReady,
            notificationReady = notificationReady,
            batteryReady = batteryReady,
            nextAlarmRegisteredReady = registeredNextAlarmReady,
            shouldCheckAlarmRegistration = shouldCheckAlarmRegistration,
            recoveryNeedsAttention = recoveryNeedsAttention,
            watchdogStatus = watchdogStatus,
            restorePostCheckStatus = restorePostCheckStatus,
            recoveryStatus = recoveryStatus,
            latestRecoveryActionText = latestRecoveryActionText,
            selfTestStatus = selfTestStatusState,
            selfTestNeedsFollowUp = selfTestNeedsFollowUp,
            nightlyCheckStatus = nightlyCheckStatusState,
            batteryGuideHint = batteryGuideSummaryText
        ),
        texts = recoveryTextSet
    )

    var batteryGuideDialogVisible by rememberSaveable { mutableStateOf(false) }
    val activeBatteryGuide = currentBatteryGuide.takeIf { batteryGuideDialogVisible }
    var reliabilityPanelExpanded by rememberSaveable { mutableStateOf(false) }
    val reliabilityBannerState = buildHomeReliabilityBannerState(
        resources = context.resources,
        reliabilityUi = reliabilityUi,
        panelExpanded = reliabilityPanelExpanded
    )
    val homeHeaderVersion = BuildConfig.VERSION_NAME.ifBlank { "v$HOME_BANNER_VERSION" }
    val homeHeaderTitle = buildAnnotatedString {
        append(stringResource(R.string.main_banner_title_prefix))
        append(" ")
        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
            append(stringResource(R.string.main_banner_title_suffix))
        }
    }
    val showReliabilityAlert = reliabilityCenterUi.summary.level != HomeReliabilityLevel.SAFE
    val settingsButtonContentDescription = stringResource(R.string.main_header_settings_content_description)
    val reliabilityButtonContentDescription = stringResource(R.string.main_header_reliability_content_description)

    if (activeBatteryGuide != null) {
        AlertDialog(
            onDismissRequest = { batteryGuideDialogVisible = false },
            title = { Text(activeBatteryGuide.title) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = activeBatteryGuide.manufacturerDescription,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = activeBatteryGuide.userMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.main_battery_guide_steps_label),
                        style = MaterialTheme.typography.titleSmall
                    )
                    activeBatteryGuide.steps.forEachIndexed { index, step ->
                        Text(
                            text = stringResource(R.string.main_battery_guide_step_format, index + 1, step),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        text = stringResource(R.string.main_battery_guide_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        batteryGuideDialogVisible = false
                        onOpenBatterySettings()
                    }
                ) {
                    Text(stringResource(R.string.main_battery_guide_open_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { batteryGuideDialogVisible = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    val canSaveByPermission = canSave
    val previewRule = AlarmRule(
        id = editingAlarmId ?: 0,
        label = selectedLabel,
        hour = selectedTime.hour,
        minute = selectedTime.minute,
        weeklyPattern = visiblePatterns,
        intervalWeeks = intervalWeeks,
        anchorDate = anchorDate,
        snoozeMinutes = selectedSnoozeMinutes,
        snoozeMaxCount = selectedSnoozeMaxCount,
        soundType = selectedSoundType,
        customSoundUri = selectedCustomSoundUri,
        volumePercent = selectedVolume.toInt(),
        vibrationEnabled = vibrationEnabled,
        skipDateEpochDays = skipDates,
        addDateEpochDays = addDates,
        enabled = editingEnabled
    )
    val next10Preview = remember(selectedTime, intervalWeeks, anchorDate, weekPatterns, editingEnabled, skipDates, addDates) {
        AlarmTimeCalculator.nextTriggers(previewRule, 10, LocalDateTime.now())
    }

    fun applySelfTestActionResult(result: SelfTestActionResult, showToast: Boolean = false) {
        selfTestMessage = result.message
        result.snapshot?.let { snapshot ->
            selfTestStatusState = snapshot.selfTestStatus
            nightlyCheckStatusState = snapshot.nightlyCheckStatus
        }
        if (showToast) {
            Toast.makeText(context, selfTestMessage, Toast.LENGTH_SHORT).show()
        }
    }

    fun scheduleSelfTest(showToast: Boolean = false) {
        val result = selfTestActionHandler.schedule(
            config = SelfTestScheduleConfig(
                label = selectedLabel,
                soundType = selectedSoundType,
                customSoundUri = selectedCustomSoundUri,
                volumePercent = selectedVolume.toInt(),
                vibrationEnabled = vibrationEnabled,
                snoozeMinutes = selectedSnoozeMinutes
            ),
            canScheduleExact = canScheduleExact
        )
        applySelfTestActionResult(result, showToast = showToast)
    }

    fun cancelSelfTest(showToast: Boolean = false) {
        val result = selfTestActionHandler.cancel()
        applySelfTestActionResult(result, showToast = showToast)
    }

    fun markSelfTestFeedback(feedback: SelfTestStatus.Feedback, showToast: Boolean = false) {
        val result = selfTestActionHandler.markFeedback(feedback)
        applySelfTestActionResult(result, showToast = showToast)
    }

    fun runPrimaryReliabilityAction(showToast: Boolean = false) {
        when (reliabilityUi.primaryAction) {
            HomeReliabilityAction.OPEN_EXACT_ALARM_SETTINGS -> {
                onOpenExactAlarmSettings()
            }
            HomeReliabilityAction.REQUEST_NOTIFICATION_PERMISSION -> {
                onRequestNotificationPermission()
                if (showToast) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.main_toast_notification_permission_request_started),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            HomeReliabilityAction.OPEN_BATTERY_SETTINGS -> {
                onOpenBatterySettings()
                if (showToast) {
                    val message = if (isSamsungDevice) {
                        context.getString(R.string.main_toast_battery_settings_samsung)
                    } else {
                        context.getString(R.string.main_toast_battery_settings_opened)
                    }
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            }
            HomeReliabilityAction.RESCHEDULE_ALARMS -> {
                requestManualReschedule(
                    startMessage = context.getString(R.string.main_toast_reschedule_started).takeIf { showToast },
                    showCompletionToast = showToast
                )
            }
            HomeReliabilityAction.OPEN_RELIABILITY_CENTER -> {
                openReliabilityCenter()
            }
            HomeReliabilityAction.RUN_SELF_TEST -> {
                scheduleSelfTest(showToast = showToast)
            }
            HomeReliabilityAction.REFRESH_STATUS -> {
                reliabilityStatusRefreshToken += 1
                onRefreshReliabilityStatus()
                if (showToast) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.main_toast_reliability_refreshed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun runRecoverySummaryAction(showToast: Boolean = false) {
        when (recoveryStatus?.outcome) {
            RescheduleRecoveryState.Outcome.DEGRADED_RECOVERY,
            RescheduleRecoveryState.Outcome.PARTIAL_RECOVERY -> {
                requestManualReschedule(
                    startMessage = context.getString(R.string.main_toast_recovery_reschedule).takeIf { showToast },
                    logDetail = context.getString(R.string.main_recovery_log_reschedule_from_partial),
                    showCompletionToast = showToast
                )
            }
            RescheduleRecoveryState.Outcome.FULL_RECOVERY,
            RescheduleRecoveryState.Outcome.NO_ACTIVE_ALARMS -> {
                openReliabilityCenter()
                appendRecoveryActionLog(context.getString(R.string.main_recovery_log_open_center_after_check))
                if (showToast) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.main_toast_recovery_open_center),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            null -> {
                if (showToast) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.main_toast_recovery_none),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    fun runRecoverySummaryActionWithDebounce(showToast: Boolean = false) {
        val now = System.currentTimeMillis()
        if (now < recoveryActionBlockedUntilMillis) {
            if (showToast) {
                Toast.makeText(
                    context,
                    context.getString(R.string.main_toast_retry_later),
                    Toast.LENGTH_SHORT
                ).show()
            }
            return
        }
        recoveryActionBlockedUntilMillis = now + 1_500L
        runRecoverySummaryAction(showToast = showToast)
    }
    fun runAutoBuildFromShiftConfig(
        draft: ShiftSetupDraft = shiftSetupDraft,
        requireInfinite: Boolean
    ): Boolean {
        val autoBuildInput = draft.toAutoBuildInput()
        fun fail(message: String): Boolean {
            updateShiftSetupDraft { it.copy(autoBuildFeedback = message) }
            return false
        }

        if (autoBuildInput.rotationSequence.isEmpty()) {
            return fail(context.getString(R.string.main_auto_build_empty_rotation))
        }
        if (requireInfinite && !autoBuildInput.infiniteRotationEnabled) {
            return fail(context.getString(R.string.main_auto_build_requires_infinite))
        }
        if (autoBuildInput.workTypeConfigs.none { it.enabled }) {
            return fail(context.getString(R.string.main_auto_build_no_enabled_types))
        }

        val built = buildWorkTemplateRotation(autoBuildInput.rotationSequence, autoBuildInput.todayRotationIndex)
        val uniqueTypes = autoBuildInput.rotationSequence.distinct()
        val typeToPattern = uniqueTypes.associateWith { type ->
            buildWeeklyPatternForType(built, type, anchor = autoBuildInput.anchorDate)
        }

        var createdCount = 0
        var invalidCount = 0
        autoBuildInput.workTypeConfigs.forEach { cfg ->
            if (!cfg.enabled) return@forEach
            val pattern = typeToPattern[cfg.type].orEmpty()
            val isVacationType = hasRestFamilyToken(cfg.type)
            if (pattern.all { it.isEmpty() } && !isVacationType) return@forEach

            val primary = parseHm(cfg.primaryTime)
            if (primary == null) {
                invalidCount += 1
            } else {
                vm.addAlarm(
                    label = context.getString(R.string.main_alarm_primary_label_format, cfg.type),
                    hour = primary.hour,
                    minute = primary.minute,
                    weeklyPattern = pattern,
                    intervalWeeks = built.intervalWeeks,
                    anchorDate = autoBuildInput.anchorDate,
                    soundType = selectedSoundType,
                    customSoundUri = selectedCustomSoundUri,
                    volumePercent = selectedVolume.toInt(),
                    vibrationEnabled = vibrationEnabled,
                    snoozeMinutes = selectedSnoozeMinutes,
                    snoozeMaxCount = selectedSnoozeMaxCount,
                    skipDateEpochDays = emptySet(),
                    addDateEpochDays = emptySet()
                )
                createdCount += 1
            }

            if (cfg.secondaryTime.isNotBlank()) {
                val second = parseHm(cfg.secondaryTime)
                if (second == null) {
                    invalidCount += 1
                } else {
                    vm.addAlarm(
                        label = context.getString(R.string.main_alarm_secondary_label_format, cfg.type),
                        hour = second.hour,
                        minute = second.minute,
                        weeklyPattern = pattern,
                        intervalWeeks = built.intervalWeeks,
                        anchorDate = autoBuildInput.anchorDate,
                        soundType = selectedSoundType,
                        customSoundUri = selectedCustomSoundUri,
                        volumePercent = selectedVolume.toInt(),
                        vibrationEnabled = vibrationEnabled,
                        snoozeMinutes = selectedSnoozeMinutes,
                        snoozeMaxCount = selectedSnoozeMaxCount,
                        skipDateEpochDays = emptySet(),
                        addDateEpochDays = emptySet()
                    )
                    createdCount += 1
                }
            }
        }

        val feedback = when {
            createdCount == 0 -> context.getString(R.string.main_auto_build_none_created)
            invalidCount > 0 -> context.getString(
                R.string.main_auto_build_partial_invalid_format,
                createdCount,
                invalidCount
            )
            else -> context.getString(R.string.main_auto_build_success_format, createdCount)
        }
        updateShiftSetupDraft { it.copy(autoBuildFeedback = feedback) }
        return createdCount > 0
    }
    val primaryPages = listOf(AlarmPage.TODAY, AlarmPage.PATTERN, AlarmPage.MANAGE)
    val patternTabs = listOf(stringResource(R.string.main_pattern_tab_setup))
    var selectedPatternTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(currentPage) {
        scrollState.scrollTo(0)
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                tonalElevation = 2.dp
            ) {
                primaryPages.forEach { page ->
                    val selected = when (page) {
                        AlarmPage.PATTERN -> currentPage == AlarmPage.PATTERN || currentPage == AlarmPage.PRESET
                        AlarmPage.MANAGE -> currentPage == AlarmPage.MANAGE || currentPage == AlarmPage.EDITOR
                        else -> currentPage == page
                    }
                    val icon = when (page) {
                        AlarmPage.TODAY -> Icons.Filled.Home
                        AlarmPage.PATTERN -> Icons.Filled.Edit
                        AlarmPage.EXCEPTION -> Icons.Filled.Edit
                        AlarmPage.MANAGE -> Icons.Filled.Settings
                        AlarmPage.EDITOR -> Icons.Filled.Edit
                        AlarmPage.PRESET -> Icons.Filled.Edit
                    }

                    NavigationBarItem(
                        selected = selected,
                        enabled = !isFirstSetupWizardActive || page == AlarmPage.PATTERN,
                        onClick = { currentPage = page },
                        icon = { Icon(icon, contentDescription = null) },
                        label = { Text(stringResource(page.labelResId)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .verticalScroll(scrollState)
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        AnimatedVisibility(
            visible = currentPage == AlarmPage.TODAY,
            enter = EnterTransition.None,
            exit = ExitTransition.None
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_home_brand_logo),
                            contentDescription = stringResource(R.string.main_brand_logo_content_description),
                            tint = Color.Unspecified,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = homeHeaderTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                                    shape = MaterialTheme.shapes.small
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = homeHeaderVersion,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Card(
                            modifier = Modifier
                                .semantics {
                                    contentDescription = settingsButtonContentDescription
                                }
                                .clickable { currentPage = AlarmPage.MANAGE },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f)),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .padding(10.dp)
                                    .size(19.dp)
                            )
                        }
                        AnimatedVisibility(
                            visible = showReliabilityAlert,
                            enter = EnterTransition.None,
                            exit = ExitTransition.None
                        ) {
                            Card(
                                modifier = Modifier
                                    .semantics {
                                        contentDescription = reliabilityButtonContentDescription
                                    }
                                    .clickable { openReliabilityCenter() },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.94f)),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "!",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = currentPage == AlarmPage.TODAY,
            enter = EnterTransition.None,
            exit = ExitTransition.None
        ) {
            HomePage(
                nextTrigger = homeNextTrigger,
                alarms = alarms,
                onSetVacationDate = { date ->
                    registerCalendarChange(
                        message = context.getString(R.string.main_calendar_vacation_apply),
                        type = AlarmLogType.MANUAL_VACATION_SET,
                        detail = date.toString()
                    ) { vm.setVacationDateForAll(date) }
                },
                onClearVacationDate = { date ->
                    registerCalendarChange(
                        message = context.getString(R.string.main_calendar_vacation_clear),
                        type = AlarmLogType.MANUAL_VACATION_CLEAR,
                        detail = date.toString()
                    ) { vm.clearVacationDateForAll(date) }
                },
                onSetVacationRange = { start, end ->
                    val from = minOf(start, end)
                    val to = maxOf(start, end)
                    registerCalendarChange(
                        message = context.getString(R.string.main_calendar_vacation_range_apply),
                        type = AlarmLogType.MANUAL_VACATION_SET,
                        detail = context.getString(R.string.main_calendar_range_detail_format, from, to)
                    ) { vm.setVacationRangeForAll(start, end) }
                },
                onClearVacationRange = { start, end ->
                    val from = minOf(start, end)
                    val to = maxOf(start, end)
                    registerCalendarChange(
                        message = context.getString(R.string.main_calendar_vacation_range_clear),
                        type = AlarmLogType.MANUAL_VACATION_CLEAR,
                        detail = context.getString(R.string.main_calendar_range_detail_format, from, to)
                    ) { vm.clearVacationRangeForAll(start, end) }
                },
                onSetSkipDateForIds = { date, ids ->
                    if (ids.isNotEmpty()) {
                        registerCalendarChange(
                            message = context.getString(R.string.main_calendar_skip_apply),
                            type = AlarmLogType.MANUAL_SKIP_SET,
                            detail = context.getString(R.string.main_calendar_alarm_count_detail_format, date, ids.size)
                        ) { vm.setSkipDateForAlarmIds(date, ids) }
                    }
                },
                onClearSkipDateForIds = { date, ids ->
                    if (ids.isNotEmpty()) {
                        registerCalendarChange(
                            message = context.getString(R.string.main_calendar_skip_clear),
                            type = AlarmLogType.MANUAL_SKIP_CLEAR,
                            detail = context.getString(R.string.main_calendar_alarm_count_detail_format, date, ids.size)
                        ) { vm.clearSkipDateForAlarmIds(date, ids) }
                    }
                },
                onApplyShiftChange = { date, type ->
                    registerCalendarChange(
                        message = context.getString(R.string.main_calendar_shift_change_apply),
                        type = AlarmLogType.MANUAL_SHIFT_CHANGE,
                        detail = context.getString(R.string.main_calendar_shift_change_detail_format, date, type)
                    ) { vm.applyShiftTypeForDate(date, type) }
                },
                undoMessage = pendingUndoMessage,
                onUndoLastChange = {
                    val snapshot = pendingUndoSnapshot
                    if (snapshot != null) {
                        vm.restoreExceptionSnapshot(snapshot)
                        alarmLogStore.append(
                            alarmId = -1,
                            label = context.getString(R.string.main_log_label_calendar),
                            type = AlarmLogType.MANUAL_UNDO,
                            detail = pendingUndoMessage ?: context.getString(R.string.main_calendar_undo_default_detail)
                        )
                        refreshAlarmLogs()
                        pendingUndoMessage = null
                        pendingUndoSnapshot = null
                    }
                }
            )
        }

        AnimatedVisibility(
            visible = currentPage == AlarmPage.EXCEPTION,
            enter = EnterTransition.None,
            exit = ExitTransition.None
        ) {
            ExceptionPage(
                alarms = alarms,
                onToggleTodaySkip = { alarm ->
                    if (LocalDate.now() in alarm.skipDateEpochDays) vm.unskipToday(alarm) else vm.skipToday(alarm)
                },
                onToggleTomorrowAdd = { alarm ->
                    if (LocalDate.now().plusDays(1) in alarm.addDateEpochDays) vm.removeTomorrow(alarm) else vm.addTomorrow(alarm)
                },
                onOpenManage = {
                    currentPage = AlarmPage.MANAGE
                    scope.launch { scrollState.animateScrollTo(0) }
                },
                onOpenEditor = {
                    editorForcedStep = null
                    editingAlarmId = null
                    editingEnabled = true
                    selectedLabel = ""
                    selectedTime = LocalTime.of(7, 0)
                    skipDates = emptySet()
                    addDates = emptySet()
                    exceptionDate = LocalDate.now()
                    currentPage = AlarmPage.EDITOR
                    scope.launch { scrollState.animateScrollTo(0) }
                }
            )
        }

        AnimatedVisibility(
            visible = currentPage == AlarmPage.EDITOR,
            enter = EnterTransition.None,
            exit = ExitTransition.None
        ) {
            if (editingAlarmId != null) {
                Text(stringResource(R.string.editor_edit_mode_hint))
            }

            EditorPage(
                editingAlarmId = editingAlarmId,
                initialStep = editorForcedStep,
                selectedLabel = selectedLabel,
                onSelectedLabelChange = { selectedLabel = it },
                onOpenExactAlarmSettings = onOpenExactAlarmSettings,
                onOpenBatterySettings = onOpenBatterySettings,
                onOpenAppDetailSettings = onOpenAppDetailSettings,
                onRescheduleAllEnabled = {
                    requestManualReschedule(
                        startMessage = context.getString(R.string.main_toast_reschedule_started),
                        showCompletionToast = true
                    )
                },
                onRequestNotificationPermission = onRequestNotificationPermission,
                setupWizardDismissed = setupWizardDismissed,
                onSetupWizardDismissedChange = {
                    setupWizardDismissed = it
                    setupPrefs.edit().putBoolean("dismissed", it).apply()
                },
                autoSaveOnDuplicate = autoSaveOnDuplicate,
                onAutoSaveOnDuplicateChange = { autoSaveOnDuplicate = it },
                onPlayTestSound = {
                    val testIntent = Intent(context, AlarmRingingService::class.java)
                        .setAction(AlarmRingingService.ACTION_START)
                        .putExtra(AlarmReceiver.EXTRA_ALARM_ID, 999_999L)
                                                .putExtra(
                            AlarmReceiver.EXTRA_LABEL,
                            if (selectedLabel.isBlank()) {
                                context.getString(R.string.main_default_alarm_label)
                            } else {
                                selectedLabel
                            }
                        )
                        .putExtra(AlarmReceiver.EXTRA_SOUND_TYPE, selectedSoundType.name)
                        .putExtra(AlarmReceiver.EXTRA_CUSTOM_SOUND_URI, selectedCustomSoundUri)
                        .putExtra(AlarmReceiver.EXTRA_VOLUME_PERCENT, selectedVolume.toInt())
                        .putExtra(AlarmReceiver.EXTRA_VIBRATION_ENABLED, vibrationEnabled)
                        .putExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, selectedSnoozeMinutes)
                        .putExtra(AlarmReceiver.EXTRA_SNOOZE_MAX_COUNT, selectedSnoozeMaxCount)
                        .putExtra(AlarmReceiver.EXTRA_SNOOZE_CURRENT_COUNT, 0)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(testIntent)
                    } else {
                        context.startService(testIntent)
                    }
                },
                onStopTestSound = { AlarmRingingService.stop(context, 999_999L) },
                onScheduleSelfTest = { scheduleSelfTest() },
                onCancelSelfTest = { cancelSelfTest() },
                selfTestMessage = selfTestMessage,
                reliabilityCenterUi = reliabilityCenterUi,
                onOpenReliabilityCenter = { openReliabilityCenter() },
                onRefreshReliabilityStatus = { onRefreshReliabilityStatus() },
                selectedTime = selectedTime,
                onSelectedTimeChange = { selectedTime = it },
                anchorDate = anchorDate,
                onAnchorDateChange = { updateShiftSetupDraft { draft -> draft.copy(anchorDate = it) } },
                selectedSoundType = selectedSoundType,
                onSelectedSoundTypeChange = { selectedSoundType = it },
                onPickCustomSound = {
                    val pickIntent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        selectedCustomSoundUri?.let { putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(it)) }
                    }
                    ringtonePickerLauncher.launch(pickIntent)
                },
                selectedCustomSoundUri = selectedCustomSoundUri,
                customSoundMessage = customSoundMessage,
                selectedVolume = selectedVolume,
                onSelectedVolumeChange = { selectedVolume = it },
                vibrationEnabled = vibrationEnabled,
                onVibrationEnabledChange = { vibrationEnabled = it },
                selectedSnoozeMinutes = selectedSnoozeMinutes,
                onSelectedSnoozeMinutesChange = {
                    selectedSnoozeMinutes = it
                    customSnoozeInput = it.toString()
                },
                customSnoozeInput = customSnoozeInput,
                onCustomSnoozeInputChange = { value ->
                    val digits = value.filter { it.isDigit() }
                    customSnoozeInput = digits
                    digits.toIntOrNull()?.let { selectedSnoozeMinutes = it.coerceIn(1, 60) }
                },
                selectedSnoozeMaxCount = selectedSnoozeMaxCount,
                customMaxCountInput = customMaxCountInput,
                onCustomMaxCountInputChange = { value ->
                    val digits = value.filter { it.isDigit() }
                    customMaxCountInput = digits
                    selectedSnoozeMaxCount = if (digits.isEmpty()) 0 else digits.toIntOrNull()?.coerceAtMost(99) ?: 0
                },
                exceptionDate = exceptionDate,
                onExceptionDateChange = { exceptionDate = it },
                skipDates = skipDates,
                onSkipDatesChange = { skipDates = it },
                addDates = addDates,
                onAddDatesChange = { addDates = it },
                showNext10 = showNext10,
                onShowNext10Change = { showNext10 = it },
                next10Preview = next10Preview,
                intervalWeeks = intervalWeeks,
                onIntervalWeeksChange = { applyIntervalWeeks(it) },
                activeWeekIndex = activeWeekIndex,
                onActiveWeekIndexChange = { activeWeekIndex = it },
                weekPatterns = normalizeWeekPatterns(intervalWeeks, weekPatterns),
                onWeekPatternsChange = { weekPatterns = normalizeWeekPatterns(intervalWeeks, it) },
                infiniteRotationEnabled = infiniteRotationEnabled,
                onInfiniteRotationEnabledChange = { updateShiftSetupDraft { draft -> draft.copy(infiniteRotationEnabled = it) } },
                canSaveByPermission = canSaveByPermission,
                onSaveOrUpdate = {
                    val editId = editingAlarmId
                    if (editId == null) {
                        vm.addAlarm(
                            label = selectedLabel,
                            hour = selectedTime.hour,
                            minute = selectedTime.minute,
                            weeklyPattern = weekPatterns,
                            intervalWeeks = intervalWeeks,
                            anchorDate = anchorDate,
                            soundType = selectedSoundType,
                            customSoundUri = selectedCustomSoundUri,
                            volumePercent = selectedVolume.toInt(),
                            vibrationEnabled = vibrationEnabled,
                            snoozeMinutes = selectedSnoozeMinutes,
                            snoozeMaxCount = selectedSnoozeMaxCount,
                            skipDateEpochDays = skipDates,
                            addDateEpochDays = addDates
                        )
                    } else {
                        vm.updateAlarm(
                            id = editId,
                            label = selectedLabel,
                            hour = selectedTime.hour,
                            minute = selectedTime.minute,
                            weeklyPattern = weekPatterns,
                            intervalWeeks = intervalWeeks,
                            anchorDate = anchorDate,
                            soundType = selectedSoundType,
                            customSoundUri = selectedCustomSoundUri,
                            volumePercent = selectedVolume.toInt(),
                            vibrationEnabled = vibrationEnabled,
                            snoozeMinutes = selectedSnoozeMinutes,
                            snoozeMaxCount = selectedSnoozeMaxCount,
                            enabled = editingEnabled,
                            skipDateEpochDays = skipDates,
                            addDateEpochDays = addDates
                        )
                        editingAlarmId = null
                        editingEnabled = true
                        selectedLabel = ""
                    }
                },
                onCancelEdit = {
                    editingAlarmId = null
                    editingEnabled = true
                    selectedLabel = ""
                    skipDates = emptySet()
                    addDates = emptySet()
                },
            )
        }

        AnimatedVisibility(
            visible = currentPage == AlarmPage.PATTERN,
            enter = EnterTransition.None,
            exit = ExitTransition.None
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (patternTabs.size > 1) {
                    TabRow(selectedTabIndex = selectedPatternTab) {
                        patternTabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedPatternTab == index,
                                onClick = { selectedPatternTab = index },
                                text = { Text(title) }
                            )
                        }
                    }
                }
                if (selectedPatternTab == 0) {
                    ShiftPage(
                        draft = shiftSetupDraft,
                        defaults = currentShiftSetupDefaults(),
                        onDraftChange = { transform -> updateShiftSetupDraft(transform) },
                        quickTemplates = QUICK_SHIFT_TEMPLATES,
                        showFirstSetupWizard = isFirstSetupWizardActive,
                        onAutoBuild = { runAutoBuildFromShiftConfig(requireInfinite = true) },
                        onCompleteFirstSetup = {
                            val success = runAutoBuildFromShiftConfig(requireInfinite = false)
                            if (success) {
                                shiftQuickSetupDone = true
                                shiftQuickSetupHidden = true
                                updateShiftSetupDraft { it.copy(wizardState = FirstSetupWizardState()) }
                                setupPrefs.edit()
                                    .putBoolean("shift_quick_setup_done", true)
                                    .putBoolean("shift_quick_setup_hidden", true)
                                    .apply()
                                currentPage = AlarmPage.TODAY
                            }
                        },
                        onHideFirstSetupWizard = {
                            shiftQuickSetupHidden = true
                            setupPrefs.edit().putBoolean("shift_quick_setup_hidden", true).apply()
                        },
                        onReopenFirstSetupWizard = { reopenPatternSetup() }
                    )
                    if (!isFirstSetupWizardActive) {
                        SecondaryActionButton(
                            onClick = {
                                currentPage = AlarmPage.PRESET
                                scope.launch { scrollState.animateScrollTo(0) }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.preset_title))
                        }
                    }
                } else {
                    ExceptionPage(
                        alarms = alarms,
                        onToggleTodaySkip = { alarm ->
                            if (LocalDate.now() in alarm.skipDateEpochDays) vm.unskipToday(alarm) else vm.skipToday(alarm)
                        },
                        onToggleTomorrowAdd = { alarm ->
                            if (LocalDate.now().plusDays(1) in alarm.addDateEpochDays) vm.removeTomorrow(alarm) else vm.addTomorrow(alarm)
                        },
                        onOpenManage = {
                            currentPage = AlarmPage.MANAGE
                            scope.launch { scrollState.animateScrollTo(0) }
                        },
                        onOpenEditor = {
                            editorForcedStep = null
                            editingAlarmId = null
                            editingEnabled = true
                            selectedLabel = ""
                            selectedTime = LocalTime.of(7, 0)
                            skipDates = emptySet()
                            addDates = emptySet()
                            exceptionDate = LocalDate.now()
                            currentPage = AlarmPage.EDITOR
                            scope.launch { scrollState.animateScrollTo(0) }
                        }
                    )
                }
            }
        }

        if (currentPage == AlarmPage.PRESET) {
            PresetPage(
                presetNameInput = presetNameInput,
                onPresetNameInputChange = { presetNameInput = it },
                onSaveCurrent = {
                    val normalized = normalizeWeekPatterns(intervalWeeks, weekPatterns)
                    val name = presetNameInput.trim().ifBlank {
                        context.getString(
                            R.string.main_preset_default_name_format,
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
                        )
                    }
                    presetStore.upsert(
                        RotationPreset(
                            name = name,
                            intervalWeeks = intervalWeeks,
                            anchorDate = anchorDate,
                            weekPatterns = normalized,
                            infiniteRotationEnabled = infiniteRotationEnabled
                        )
                    )
                    savedPresets.clear()
                    savedPresets.addAll(presetStore.load())
                    presetNameInput = name
                },
                onRefreshList = {
                    savedPresets.clear()
                    savedPresets.addAll(presetStore.load())
                },
                onExport = {
                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "application/json"
                        putExtra(Intent.EXTRA_TITLE, "shift-alarm-presets.json")
                    }
                    exportPresetLauncher.launch(intent)
                },
                onImport = {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "application/json"
                    }
                    importPresetLauncher.launch(intent)
                },
                importMergeMode = importMergeMode,
                onImportMergeModeChange = { importMergeMode = it },
                presetFeedbackMessage = presetFeedbackMessage,
                onExportBackup = {
                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "application/json"
                        putExtra(Intent.EXTRA_TITLE, "shift-alarm-backup-v1.json")
                    }
                    exportBackupLauncher.launch(intent)
                },
                onImportBackup = {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "application/json"
                    }
                    importBackupLauncher.launch(intent)
                },
                backupFeedbackMessage = backupFeedbackMessage,
                pendingBackupRestorePreview = pendingBackupRestorePreview,
                backupRestoreInFlight = backupRestoreInFlight,
                onConfirmBackupRestore = { confirmPendingBackupRestore() },
                onDismissBackupRestore = { dismissPendingBackupRestore() },
                savedPresets = savedPresets.sortedBy { if (inferPresetCategory(it) == selectedShiftCategory) 0 else 1 },
                onApplyPreset = { preset ->
                    val presetInterval = normalizeIntervalWeeks(preset.intervalWeeks)
                    activeWeekIndex = 0
                    applyIntervalWeeks(presetInterval, preset.weekPatterns)
                    presetNameInput = preset.name
                    updateShiftSetupDraft {
                        it.copy(
                            anchorDate = preset.anchorDate,
                            infiniteRotationEnabled = preset.infiniteRotationEnabled
                        )
                    }
                },
                onDeletePreset = { name ->
                    presetStore.deleteByName(name)
                    savedPresets.clear()
                    savedPresets.addAll(presetStore.load())
                },
                onSetDefaultPreset = { name ->
                    presetStore.setDefault(name)
                    savedPresets.clear()
                    savedPresets.addAll(presetStore.load())
                },
                onSetPresetInfinite = { name, enabled ->
                    presetStore.setInfinite(name, enabled)
                    savedPresets.clear()
                    savedPresets.addAll(presetStore.load())
                    if (presetNameInput.equals(name, ignoreCase = true)) {
                        updateShiftSetupDraft { it.copy(infiniteRotationEnabled = enabled) }
                    }
                },
                onMoveUp = { name ->
                    presetStore.moveUp(name)
                    savedPresets.clear()
                    savedPresets.addAll(presetStore.load())
                },
                onMoveDown = { name ->
                    presetStore.moveDown(name)
                    savedPresets.clear()
                    savedPresets.addAll(presetStore.load())
                }
            )
        }


        AnimatedVisibility(
            visible = currentPage == AlarmPage.MANAGE,
            enter = EnterTransition.None,
            exit = ExitTransition.None
        ) {
            AlarmListPage(
                alarms = alarms,
                editingAlarmId = editingAlarmId,
                onToggle = { vm.toggleEnabled(it) },
                onDelete = { vm.delete(it) },
                onToggleTodaySkip = { alarm ->
                    if (LocalDate.now() in alarm.skipDateEpochDays) vm.unskipToday(alarm) else vm.skipToday(alarm)
                },
                onToggleTomorrowAdd = { alarm ->
                    if (LocalDate.now().plusDays(1) in alarm.addDateEpochDays) vm.removeTomorrow(alarm) else vm.addTomorrow(alarm)
                },
                onDuplicate = { alarm ->
                    val copiedLabel = alarm.duplicateLabel(context.resources)
                    val copiedPattern = alarm.normalizedWeeklyPattern()
                    if (autoSaveOnDuplicate) {
                        vm.addAlarm(
                            label = copiedLabel,
                            hour = alarm.hour,
                            minute = alarm.minute,
                            weeklyPattern = copiedPattern,
                            intervalWeeks = normalizeIntervalWeeks(alarm.intervalWeeks),
                            anchorDate = alarm.anchorDate,
                            soundType = alarm.soundType,
                            customSoundUri = alarm.customSoundUri,
                            volumePercent = alarm.volumePercent,
                            vibrationEnabled = alarm.vibrationEnabled,
                            snoozeMinutes = alarm.snoozeMinutes,
                            snoozeMaxCount = alarm.snoozeMaxCount,
                            skipDateEpochDays = alarm.skipDateEpochDays,
                            addDateEpochDays = alarm.addDateEpochDays
                        )
                    } else {
                        openEditorWithDraft(
                            alarm.toEditorDraft(
                                editingAlarmId = null,
                                editingEnabled = true,
                                selectedLabel = copiedLabel,
                                weekPatterns = copiedPattern
                            )
                        )
                    }
                },
                onEdit = { alarm ->
                    openEditorWithDraft(
                        alarm.toEditorDraft(
                            editingAlarmId = alarm.id,
                            editingEnabled = alarm.enabled
                        )
                    )
                },
                onReconfigurePattern = {
                    reopenPatternSetup()
                    scope.launch { scrollState.animateScrollTo(0) }
                },
                alarmLogs = alarmLogs,
                onRefreshLogs = { refreshAlarmLogs() },
                onClearLogs = {
                    alarmLogStore.clear()
                    alarmLogs.clear()
                }
            )
        }
    }
}
}

private fun maybePersistReadPermission(context: android.content.Context, data: Intent?, uri: Uri) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) return
    val flags = data?.flags ?: 0
    val readFlag = flags and Intent.FLAG_GRANT_READ_URI_PERMISSION
    val persistFlag = flags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
    if (readFlag != 0 && persistFlag != 0) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}

private fun isUriPlayable(context: android.content.Context, uri: Uri): Boolean {
    if (runCatching { context.contentResolver.openAssetFileDescriptor(uri, "r")?.close() }.isSuccess) {
        return true
    }
    return runCatching { RingtoneManager.getRingtone(context, uri) != null }.getOrDefault(false)
}

















