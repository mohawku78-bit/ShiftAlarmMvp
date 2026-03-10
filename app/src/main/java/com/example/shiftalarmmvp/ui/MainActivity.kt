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
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.shiftalarmmvp.R
import com.example.shiftalarmmvp.data.AlarmRule
import com.example.shiftalarmmvp.data.AlarmSoundType
import com.example.shiftalarmmvp.data.normalizeIntervalWeeks
import com.example.shiftalarmmvp.data.normalizeWeekPatterns
import com.example.shiftalarmmvp.receiver.AlarmReceiver
import com.example.shiftalarmmvp.recovery.HomeReliabilityAction
import com.example.shiftalarmmvp.recovery.HomeReliabilityPolicy
import com.example.shiftalarmmvp.recovery.HomeReliabilitySignals
import com.example.shiftalarmmvp.recovery.NightlyReliabilityCheckStatus
import com.example.shiftalarmmvp.recovery.ReliabilityPolicy
import com.example.shiftalarmmvp.recovery.ReliabilityStateCoordinator
import com.example.shiftalarmmvp.recovery.RescheduleRecoveryState
import com.example.shiftalarmmvp.recovery.RescheduleRecoveryStore
import com.example.shiftalarmmvp.recovery.SelfTestStatus
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

private const val HOME_BANNER_VERSION = "060"
private const val BATTERY_SETTINGS_LOG_TAG = "ShiftAlarmBattery"

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
    private var rescheduleRecoveryState by mutableStateOf<RescheduleRecoveryState?>(null)
    private var selfTestStatus by mutableStateOf<SelfTestStatus?>(null)
    private var nightlyCheckStatus by mutableStateOf<NightlyReliabilityCheckStatus?>(null)
    private var openReliabilityCenterRequestToken by mutableIntStateOf(0)
    private var pendingReliabilityFollowUpTarget by mutableStateOf<ReliabilityFollowUpTarget?>(null)
    private var reliabilityReceiverRegistered = false
    private val reliabilityStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != AlarmRingingService.ACTION_RELIABILITY_STATE_CHANGED) return
            refreshReliabilitySignals(recalculateSummary = true)
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
                    onRefreshReliabilityStatus = { refreshReliabilitySignals(showToast = true) },
                    isIgnoringBatteryOptimization = isIgnoringBatteryOptimizationState,
                    recoveryStatus = rescheduleRecoveryState,
                    selfTestStatus = selfTestStatus,
                    nightlyCheckStatus = nightlyCheckStatus,
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
    }

    override fun onStop() {
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

        val snapshot = if (showToast || recalculateSummary) {
            reliabilityCoordinator.recalculateAndSnapshot()
        } else {
            reliabilityCoordinator.snapshot()
        }
        applyReliabilitySnapshot(snapshot)

        if (showToast) {
            val summary = snapshot.nightlyCheckStatus?.summary ?: "점검 정보 없음"
            Toast.makeText(this, "점검 완료: $summary", Toast.LENGTH_SHORT).show()
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

    private fun applyReliabilitySnapshot(snapshot: com.example.shiftalarmmvp.recovery.ReliabilityStateSnapshot) {
        selfTestStatus = snapshot.selfTestStatus
        nightlyCheckStatus = snapshot.nightlyCheckStatus
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingReliabilityFollowUpTarget = ReliabilityFollowUpTarget.EXACT_ALARM
            startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
            )
        }
    }

    private fun openBatteryOptimizationSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Toast.makeText(this, "이 기기는 배터리 최적화 예외 설정이 필요하지 않습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        pendingReliabilityFollowUpTarget = ReliabilityFollowUpTarget.BATTERY_OPTIMIZATION
        val appPackage = packageName
        val packageUri = Uri.parse("package:$appPackage")
        val appLabel = applicationInfo.loadLabel(packageManager).toString()
        Log.i(BATTERY_SETTINGS_LOG_TAG, "battery settings button tapped")
        Toast.makeText(this, "배터리 설정 화면을 여는 중...", Toast.LENGTH_SHORT).show()

        val candidates = mutableListOf<Intent>()
        candidates += vendorBatteryIntents(appPackage, appLabel)
        candidates += Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = packageUri
        }

        val opened = tryStartActivityIntent(candidates)
        if (!opened) {
            pendingReliabilityFollowUpTarget = null
            Toast.makeText(this, "설정 화면을 열 수 없습니다. 직접 설정 앱에서 앱 정보를 열어 배터리 제한을 해제해 주세요.", Toast.LENGTH_LONG).show()
        }
    }


    private fun consumeReliabilityFollowUpFeedbackIfNeeded(fromPermissionCallback: Boolean = false) {
        val target = pendingReliabilityFollowUpTarget ?: return
        if (fromPermissionCallback && target != ReliabilityFollowUpTarget.NOTIFICATION_PERMISSION) return

        val (label, resolved) = when (target) {
            ReliabilityFollowUpTarget.EXACT_ALARM -> {
                val ok = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || canScheduleExact
                "정확 알람 권한" to ok
            }
            ReliabilityFollowUpTarget.BATTERY_OPTIMIZATION -> {
                val ok = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || isIgnoringBatteryOptimizationState
                "배터리 최적화 예외" to ok
            }
            ReliabilityFollowUpTarget.NOTIFICATION_PERMISSION -> {
                val ok = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || canPostNotifications
                "알림 권한" to ok
            }
        }

        val message = if (resolved) {
            "$label 해결됨"
        } else {
            "$label 아직 필요"
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        pendingReliabilityFollowUpTarget = null
    }
    private fun vendorBatteryIntents(appPackage: String, appLabel: String): List<Intent> {
        val manufacturer = Build.MANUFACTURER.lowercase(Locale.ROOT)
        return when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> listOf(
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
            manufacturer.contains("oppo") || manufacturer.contains("realme") || manufacturer.contains("oneplus") -> listOf(
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
            manufacturer.contains("vivo") || manufacturer.contains("iqoo") -> listOf(
                Intent().setClassName(
                    "com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                ).putExtra("packagename", appPackage),
                Intent().setClassName(
                    "com.iqoo.secure",
                    "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"
                ).putExtra("packagename", appPackage)
            )
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> listOf(
                Intent().setClassName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.optimize.process.ProtectActivity"
                ),
                Intent().setClassName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                )
            )
            manufacturer.contains("samsung") -> listOf(
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
                Log.i(BATTERY_SETTINGS_LOG_TAG, "opened action=$action component=$component data=$data")
            }
            .onFailure { error ->
                Log.w(BATTERY_SETTINGS_LOG_TAG, "failed action=$action component=$component data=$data", error)
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
    selfTestStatus: SelfTestStatus?,
    nightlyCheckStatus: NightlyReliabilityCheckStatus?,
    openReliabilityCenterRequestToken: Int
) {
    val alarms by vm.alarms.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val reliabilityCoordinator = remember(context) { ReliabilityStateCoordinator(context) }
    val selfTestActionHandler = remember(context, reliabilityCoordinator) { SelfTestActionHandler(context, reliabilityCoordinator) }
    var selfTestStatusState by remember { mutableStateOf(selfTestStatus) }
    var nightlyCheckStatusState by remember { mutableStateOf(nightlyCheckStatus) }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val recoveryFirstActionGuide = recoveryStatus
        ?.takeIf { it.outcome == RescheduleRecoveryState.Outcome.PARTIAL_RECOVERY }
        ?.let {
            when (it.action) {
                Intent.ACTION_BOOT_COMPLETED -> "부팅 후 알람 복구가 일부 완료되지 않았습니다. 먼저 '알람 재예약'을 눌러 전체 예약을 다시 맞춰 주세요."
                Intent.ACTION_TIME_CHANGED -> "시간 변경 후 알람 복구가 일부 완료되지 않았습니다. 먼저 '알람 재예약'으로 현재 시각 기준으로 다시 계산해 주세요."
                Intent.ACTION_TIMEZONE_CHANGED -> "시간대 변경 후 알람 복구가 일부 완료되지 않았습니다. 먼저 '알람 재예약'으로 시간대 반영을 다시 진행해 주세요."
                Intent.ACTION_DATE_CHANGED -> "날짜 변경 후 알람 복구가 일부 완료되지 않았습니다. 먼저 '알람 재예약'으로 날짜 기준을 갱신해 주세요."
                Intent.ACTION_MY_PACKAGE_REPLACED -> "앱 업데이트 후 알람 복구가 일부 완료되지 않았습니다. 먼저 '알람 재예약'으로 예약을 복구해 주세요."
                else -> "상태 변경 후 알람 복구가 일부 완료되지 않았습니다. 먼저 '알람 재예약'을 실행해 주세요."
            }
        }
    val recoverySummaryText = recoveryStatus?.homeOneLineSummary() ?: "복구 기록 없음"
    val recoverySummaryColor = when (recoveryStatus?.outcome) {
        RescheduleRecoveryState.Outcome.FULL_RECOVERY -> Color(0xFF4CD37B)
        RescheduleRecoveryState.Outcome.PARTIAL_RECOVERY -> Color(0xFFFFC061)
        RescheduleRecoveryState.Outcome.NO_ACTIVE_ALARMS,
        null -> Color.White.copy(alpha = 0.72f)
    }
    val recoveryActionLabel = when (recoveryStatus?.outcome) {
        RescheduleRecoveryState.Outcome.PARTIAL_RECOVERY -> "재예약"
        RescheduleRecoveryState.Outcome.FULL_RECOVERY,
        RescheduleRecoveryState.Outcome.NO_ACTIVE_ALARMS -> "점검 열기"
        null -> null
    }
    val recoverySummaryClickable = recoveryActionLabel != null
    var recoveryActionBlockedUntilMillis by remember { mutableStateOf(0L) }
    val selfTestSummaryText = selfTestStatusState?.homeSummary() ?: "2분 테스트 이력 없음"
    val selfTestSummaryColor = when (selfTestStatusState?.lastEvent) {
        SelfTestStatus.Event.PASSED -> Color(0xFF4CD37B)
        SelfTestStatus.Event.FAILED -> Color(0xFFFF7B7B)
        SelfTestStatus.Event.SCHEDULED, SelfTestStatus.Event.TRIGGERED -> Color(0xFF7EA8FF)
        SelfTestStatus.Event.UNCERTAIN, SelfTestStatus.Event.CANCELED -> Color.White.copy(alpha = 0.78f)
        else -> Color.White.copy(alpha = 0.72f)
    }
    val nightlyCheckSummaryText = nightlyCheckStatusState?.bannerText() ?: "최근 점검 없음 · 재점검으로 지금 확인"
    val nightlyCheckSummaryColor = when {
        nightlyCheckStatusState == null -> Color.White.copy(alpha = 0.72f)
        (nightlyCheckStatusState?.issueCount ?: 0) > 0 -> Color(0xFFFFC061)
        else -> Color(0xFF4CD37B)
    }
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val isCompactTodayBanner = screenWidthDp <= 380
    val isSamsungDevice = remember { Build.MANUFACTURER.lowercase(Locale.ROOT).contains("samsung") }
    val setupPrefs = remember(context) {
        context.getSharedPreferences("first_setup_wizard", android.content.Context.MODE_PRIVATE)
    }
    var setupWizardDismissed by remember { mutableStateOf(setupPrefs.getBoolean("dismissed", false)) }
    var shiftQuickSetupDone by remember { mutableStateOf(setupPrefs.getBoolean("shift_quick_setup_done", false)) }
    var shiftQuickSetupHidden by remember { mutableStateOf(setupPrefs.getBoolean("shift_quick_setup_hidden", false)) }
    var selectedShiftCategory by remember {
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
    var anchorDate by remember { mutableStateOf(LocalDate.now()) }
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
    var importMergeMode by remember { mutableStateOf(true) }
    var currentPage by remember { mutableStateOf(AlarmPage.TODAY) }
    var editorForcedStep by remember { mutableStateOf<Int?>(null) }
    var selfTestMessage by remember { mutableStateOf("") }
    val selfTestStatusSummary = selfTestStatusState?.homeSummary() ?: "2분 테스트 이력 없음"

    fun persistSelectedCategory(category: ShiftCategory) {
        selectedShiftCategory = category
        setupPrefs.edit().putString("selected_shift_category", category.name).apply()
    }

    fun reopenPatternSetup() {
        shiftQuickSetupDone = false
        shiftQuickSetupHidden = false
        setupPrefs.edit()
            .putBoolean("shift_quick_setup_done", false)
            .putBoolean("shift_quick_setup_hidden", false)
            .apply()
        currentPage = AlarmPage.PATTERN
    }

    fun openReliabilityCenter() {
        editorForcedStep = 3
        currentPage = AlarmPage.EDITOR
        scope.launch { scrollState.animateScrollTo(0) }
    }

    LaunchedEffect(openReliabilityCenterRequestToken) {
        if (openReliabilityCenterRequestToken > 0) {
            openReliabilityCenter()
        }
    }

    var customWorkTypeInput by remember { mutableStateOf("") }
    var rotationSequence by remember { mutableStateOf(listOf("주간", "야간", "비번", "휴무")) }
    var todayRotationIndex by remember { mutableIntStateOf(0) }
    var workTypeConfigs by remember { mutableStateOf(defaultWorkTypeConfigs(rotationSequence.distinct() + listOf("휴무"))) }
    var infiniteRotationEnabled by remember { mutableStateOf(true) }
    var autoBuildFeedback by remember { mutableStateOf("") }
    val presetStore = remember(context) { RotationPresetStore(context) }
    val savedPresets = remember { mutableStateListOf<RotationPreset>() }
    val alarmLogStore = remember(context) { AlarmLogStore(context) }
    val alarmLogs = remember { mutableStateListOf<AlarmLogEntry>() }
    val latestRecoveryActionEntry = alarmLogs.firstOrNull { it.type == AlarmLogType.MANUAL_RECOVERY_ACTION }
    val latestRecoveryActionText = latestRecoveryActionEntry?.let { entry ->
        val ts = entry.toLocalDateTime().format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
        "최근 복구 조치 $ts · ${entry.detail}"
    } ?: "최근 복구 조치 없음"
    val latestRecoveryActionColor = if (latestRecoveryActionEntry == null) {
        Color.White.copy(alpha = 0.62f)
    } else {
        Color.White.copy(alpha = 0.88f)
    }

    var pendingUndoMessage by remember { mutableStateOf<String?>(null) }
    var pendingUndoSnapshot by remember { mutableStateOf<Map<Long, Pair<Set<LocalDate>, Set<LocalDate>>>?>(null) }
    var pendingUndoToken by remember { mutableIntStateOf(0) }

    fun refreshAlarmLogs() {
        alarmLogs.clear()
        alarmLogs.addAll(alarmLogStore.recent(50))
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
            label = "홈 신뢰도",
            type = AlarmLogType.MANUAL_RECOVERY_ACTION,
            detail = detail
        )
        refreshAlarmLogs()
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
            label = "캘린더",
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
        anchorDate = draft.anchorDate
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
            val pickedUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            if (pickedUri != null) {
                maybePersistReadPermission(context, result.data, pickedUri)
                if (isUriPlayable(context, pickedUri)) {
                    selectedCustomSoundUri = pickedUri.toString()
                    selectedSoundType = AlarmSoundType.CUSTOM
                    customSoundMessage = "커스텀 소리 설정 완료"
                } else {
                    selectedCustomSoundUri = null
                    selectedSoundType = AlarmSoundType.ALARM
                    customSoundMessage = "선택한 소리를 사용할 수 없어 기본 알람음으로 전환했습니다."
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
                } ?: error("출력 스트림을 열 수 없습니다")
            }
            presetFeedbackMessage = if (exportResult.isSuccess) {
                "프리셋 내보내기 완료"
            } else {
                "프리셋 내보내기 실패: ${exportResult.exceptionOrNull()?.message ?: "원인 없음"}"
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
                    ?: error("입력 스트림을 열 수 없습니다")
                presetStore.importJson(content, merge = importMergeMode)
            }
            if (importResult.isSuccess) {
                savedPresets.clear()
                savedPresets.addAll(presetStore.load())
                val modeLabel = if (importMergeMode) "병합" else "덮어쓰기"
                presetFeedbackMessage = "프리셋 ${importResult.getOrDefault(0)}개 ${modeLabel} 가져오기 완료"
            } else {
                presetFeedbackMessage = "프리셋 가져오기 실패: ${importResult.exceptionOrNull()?.message ?: "원인 없음"}"
            }
        }
    }
    val visiblePatterns = normalizeWeekPatterns(intervalWeeks, weekPatterns)
    val canSave = visiblePatterns.any { it.isNotEmpty() }
    val exactReady = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || canScheduleExact
    val batteryReady = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || isIgnoringBatteryOptimization
    val notificationReady = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || canPostNotifications
    val selfTestEvent = selfTestStatusState?.lastEvent
    val recoveryNeedsAttention = recoveryStatus?.outcome == RescheduleRecoveryState.Outcome.PARTIAL_RECOVERY
    val selfTestNeedsFollowUp = ReliabilityPolicy.isSelfTestFollowUpNeeded(selfTestStatusState)
    val reliabilityUi = HomeReliabilityPolicy.evaluate(
        HomeReliabilitySignals(
            exactReady = exactReady,
            notificationReady = notificationReady,
            batteryReady = batteryReady,
            recoveryNeedsAttention = recoveryNeedsAttention,
            selfTestEvent = selfTestEvent,
            selfTestNeedsFollowUp = selfTestNeedsFollowUp,
            nightlyIssueCount = nightlyCheckStatusState?.issueCount ?: 0
        )
    )
    val samsungBatteryGuideText = if (
        isSamsungDevice && reliabilityUi.primaryAction == HomeReliabilityAction.OPEN_BATTERY_SETTINGS
    ) {
        "삼성은 앱 정보 > 배터리 > 제한 없음으로 변경 후 재점검해 주세요."
    } else {
        null
    }

    var reliabilityPanelExpanded by rememberSaveable { mutableStateOf(false) }
    val reliabilityBannerState = buildHomeReliabilityBannerState(
        reliabilityUi = reliabilityUi,
        panelExpanded = reliabilityPanelExpanded
    )

    val requiresExactPermission = !exactReady
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
    val homeNextTrigger = remember(alarms) {
        val now = LocalDateTime.now()
        alarms.asSequence()
            .filter { it.enabled }
            .mapNotNull { AlarmTimeCalculator.nextTrigger(it, now) }
            .minOrNull()
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
                if (showToast) Toast.makeText(context, "정확 알람 권한 설정 화면을 열었습니다.", Toast.LENGTH_SHORT).show()
            }
            HomeReliabilityAction.REQUEST_NOTIFICATION_PERMISSION -> {
                onRequestNotificationPermission()
                if (showToast) Toast.makeText(context, "알림 권한 요청을 시작했습니다.", Toast.LENGTH_SHORT).show()
            }
            HomeReliabilityAction.OPEN_BATTERY_SETTINGS -> {
                onOpenBatterySettings()
                if (showToast) {
                    val message = if (isSamsungDevice) {
                        "삼성: 앱 정보 > 배터리 > 제한 없음으로 설정해 주세요."
                    } else {
                        "배터리 설정 화면을 열었습니다."
                    }
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            }
            HomeReliabilityAction.RESCHEDULE_ALARMS -> {
                vm.rescheduleAllEnabled()
                if (showToast) Toast.makeText(context, "알람 재예약을 실행했습니다.", Toast.LENGTH_SHORT).show()
            }
            HomeReliabilityAction.OPEN_RELIABILITY_CENTER -> {
                openReliabilityCenter()
            }
            HomeReliabilityAction.RUN_SELF_TEST -> {
                scheduleSelfTest(showToast = showToast)
            }
            HomeReliabilityAction.REFRESH_STATUS -> {
                onRefreshReliabilityStatus()
                if (showToast) Toast.makeText(context, "신뢰도 상태를 다시 확인했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun runRecoverySummaryAction(showToast: Boolean = false) {
        when (recoveryStatus?.outcome) {
            RescheduleRecoveryState.Outcome.PARTIAL_RECOVERY -> {
                vm.rescheduleAllEnabled()
                onRefreshReliabilityStatus()
                appendRecoveryActionLog("부분 복구 상태에서 재예약 실행")
                if (showToast) Toast.makeText(context, "복구 조치로 알람 재예약을 실행했습니다.", Toast.LENGTH_SHORT).show()
            }
            RescheduleRecoveryState.Outcome.FULL_RECOVERY,
            RescheduleRecoveryState.Outcome.NO_ACTIVE_ALARMS -> {
                openReliabilityCenter()
                appendRecoveryActionLog("복구 상태 확인 후 신뢰도 화면 이동")
                if (showToast) Toast.makeText(context, "신뢰도 화면으로 이동했습니다.", Toast.LENGTH_SHORT).show()
            }
            null -> {
                if (showToast) Toast.makeText(context, "복구 기록이 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    fun runRecoverySummaryActionWithDebounce(showToast: Boolean = false) {
        val now = System.currentTimeMillis()
        if (now < recoveryActionBlockedUntilMillis) {
            if (showToast) Toast.makeText(context, "잠시 후 다시 눌러 주세요.", Toast.LENGTH_SHORT).show()
            return
        }
        recoveryActionBlockedUntilMillis = now + 1_500L
        runRecoverySummaryAction(showToast = showToast)
    }
    fun runAutoBuildFromShiftConfig(requireInfinite: Boolean): Boolean {
        if (rotationSequence.isEmpty()) {
            autoBuildFeedback = "근무 순서를 먼저 입력해 주세요"
            return false
        }
        if (requireInfinite && !infiniteRotationEnabled) {
            autoBuildFeedback = "무한 반복을 켠 뒤 알람을 생성해 주세요."
            return false
        }
        if (workTypeConfigs.none { it.enabled }) {
            autoBuildFeedback = "활성 근무 타입이 없어 알람을 생성할 수 없습니다."
            return false
        }

        val built = buildWorkTemplateRotation(rotationSequence, todayRotationIndex)
        val uniqueTypes = rotationSequence.distinct()
        val typeToPattern = uniqueTypes.associateWith { type ->
            buildWeeklyPatternForType(built, type, anchor = anchorDate)
        }

        var createdCount = 0
        var invalidCount = 0
        workTypeConfigs.forEach { cfg ->
            if (!cfg.enabled) return@forEach
            val pattern = typeToPattern[cfg.type].orEmpty()
            val isVacationType = cfg.type.contains("휴")
            if (pattern.all { it.isEmpty() } && !isVacationType) return@forEach

            val primary = parseHm(cfg.primaryTime)
            if (primary == null) {
                invalidCount += 1
            } else {
                vm.addAlarm(
                    label = "${cfg.type} 1차",
                    hour = primary.hour,
                    minute = primary.minute,
                    weeklyPattern = pattern,
                    intervalWeeks = built.intervalWeeks,
                    anchorDate = anchorDate,
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
                        label = "${cfg.type} 2차",
                        hour = second.hour,
                        minute = second.minute,
                        weeklyPattern = pattern,
                        intervalWeeks = built.intervalWeeks,
                        anchorDate = anchorDate,
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

        autoBuildFeedback = when {
            createdCount == 0 -> "생성된 알람이 없습니다. 시간 형식을 확인해 주세요."
            invalidCount > 0 -> "${createdCount}개 생성, ${invalidCount}개 시간 형식 오류"
            else -> "${createdCount}개 알람 자동 생성 완료"
        }
        return createdCount > 0
    }
    val primaryPages = listOf(AlarmPage.TODAY, AlarmPage.PATTERN, AlarmPage.MANAGE)
    val patternTabs = listOf("패턴 설정")
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
                        onClick = { currentPage = page },
                        icon = { Icon(icon, contentDescription = null) },
                        label = { Text(page.label) },
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
                colors = CardDefaults.cardColors(containerColor = Color(0xFF060C24))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = if (isCompactTodayBanner) Alignment.Top else Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_brand_badge),
                            contentDescription = "브랜드 로고",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(if (isCompactTodayBanner) 34.dp else 40.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            val titleText = remember {
                                buildAnnotatedString {
                                    append("교대근무")
                                    withStyle(SpanStyle(color = Color(0xFF6EA0FF))) {
                                        append("알람")
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = titleText,
                                    modifier = Modifier.weight(1f),
                                    style = if (isCompactTodayBanner) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "v$HOME_BANNER_VERSION",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.72f),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Text(
                                "패턴을 설정하면 자동 반복됩니다.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.86f)
                            )
                            AnimatedVisibility(
                                visible = reliabilityBannerState.showPanel,
                                enter = EnterTransition.None,
                                exit = ExitTransition.None
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = reliabilityUi.reasonText,
                                        maxLines = if (isCompactTodayBanner) 1 else 2,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.86f)
                                    )
                                    samsungBatteryGuideText?.let { guide ->
                                        Text(
                                            text = guide,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFFFC061)
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = recoverySummaryText,
                                            modifier = if (recoverySummaryClickable) {
                                                Modifier
                                                    .weight(1f)
                                                    .clickable { runRecoverySummaryActionWithDebounce(showToast = true) }
                                            } else {
                                                Modifier.weight(1f)
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = recoverySummaryColor
                                        )
                                        recoveryActionLabel?.let { actionLabel ->
                                            Card(
                                                modifier = Modifier.clickable { runRecoverySummaryActionWithDebounce(showToast = true) },
                                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.14f))
                                            ) {
                                                Text(
                                                    text = "조치: $actionLabel",
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White.copy(alpha = 0.9f),
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = latestRecoveryActionText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = latestRecoveryActionColor
                                    )
                                    Text(
                                        text = selfTestSummaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = selfTestSummaryColor
                                    )
                                    Text(
                                        text = nightlyCheckSummaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = nightlyCheckSummaryColor
                                    )
                                }
                            }
                        }
                    }
                    Column(
                        modifier = Modifier.widthIn(max = if (isCompactTodayBanner) 116.dp else 180.dp),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Card(
                            modifier = Modifier.clickable {
                                when {
                                    !reliabilityBannerState.showPanel -> reliabilityPanelExpanded = true
                                    reliabilityBannerState.panelLockedOpen -> openReliabilityCenter()
                                    else -> reliabilityPanelExpanded = false
                                }
                            },
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.16f))
                        ) {
                            Text(
                                text = reliabilityBannerState.toggleLabel,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        AnimatedVisibility(
                            visible = reliabilityBannerState.showPanel,
                            enter = EnterTransition.None,
                            exit = ExitTransition.None
                        ) {
                            Column(
                        modifier = Modifier.widthIn(max = if (isCompactTodayBanner) 116.dp else 180.dp),
                        horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                HomeReliabilityChip(
                                    label = reliabilityBannerState.chipLabel,
                                    tone = reliabilityBannerState.chipTone,
                                    onOpenReliabilityCenter = { openReliabilityCenter() }
                                )
                                Card(
                                    modifier = Modifier.clickable { runPrimaryReliabilityAction(showToast = true) },
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2F6EF1))
                                ) {
                                    Text(
                                        text = reliabilityUi.primaryActionLabel,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
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
                selectedLabel = selectedLabel,
                selectedTime = selectedTime,
                nextTrigger = homeNextTrigger,
                rotationPreview = buildWorkPreview(rotationSequence, todayRotationIndex, 7, LocalDate.now()),
                alarms = alarms,
                onReconfigurePattern = {
                    reopenPatternSetup()
                    scope.launch { scrollState.animateScrollTo(0) }
                },
                onOpenManage = {
                    currentPage = AlarmPage.MANAGE
                    scope.launch { scrollState.animateScrollTo(0) }
                },
                onSetVacationDate = { date ->
                    registerCalendarChange(
                        message = "휴가 처리 적용",
                        type = AlarmLogType.MANUAL_VACATION_SET,
                        detail = date.toString()
                    ) { vm.setVacationDateForAll(date) }
                },
                onClearVacationDate = { date ->
                    registerCalendarChange(
                        message = "휴가 해제 적용",
                        type = AlarmLogType.MANUAL_VACATION_CLEAR,
                        detail = date.toString()
                    ) { vm.clearVacationDateForAll(date) }
                },
                onSetVacationRange = { start, end ->
                    val from = minOf(start, end)
                    val to = maxOf(start, end)
                    registerCalendarChange(
                        message = "기간 휴가 적용",
                        type = AlarmLogType.MANUAL_VACATION_SET,
                        detail = "$from~$to"
                    ) { vm.setVacationRangeForAll(start, end) }
                },
                onClearVacationRange = { start, end ->
                    val from = minOf(start, end)
                    val to = maxOf(start, end)
                    registerCalendarChange(
                        message = "기간 휴가 해제",
                        type = AlarmLogType.MANUAL_VACATION_CLEAR,
                        detail = "$from~$to"
                    ) { vm.clearVacationRangeForAll(start, end) }
                },
                onSetSkipDateForIds = { date, ids ->
                    if (ids.isNotEmpty()) {
                        registerCalendarChange(
                            message = "스킵 처리",
                            type = AlarmLogType.MANUAL_SKIP_SET,
                            detail = "${date} (${ids.size}개 알람)"
                        ) { vm.setSkipDateForAlarmIds(date, ids) }
                    }
                },
                onClearSkipDateForIds = { date, ids ->
                    if (ids.isNotEmpty()) {
                        registerCalendarChange(
                            message = "스킵 해제",
                            type = AlarmLogType.MANUAL_SKIP_CLEAR,
                            detail = "${date} (${ids.size}개 알람)"
                        ) { vm.clearSkipDateForAlarmIds(date, ids) }
                    }
                },
                onApplyShiftChange = { date, type ->
                    registerCalendarChange(
                        message = "근무 변경 적용",
                        type = AlarmLogType.MANUAL_SHIFT_CHANGE,
                        detail = "${date} -> ${type}"
                    ) { vm.applyShiftTypeForDate(date, type) }
                },
                undoMessage = pendingUndoMessage,
                onUndoLastChange = {
                    val snapshot = pendingUndoSnapshot
                    if (snapshot != null) {
                        vm.restoreExceptionSnapshot(snapshot)
                        alarmLogStore.append(
                            alarmId = -1,
                            label = "캘린더",
                            type = AlarmLogType.MANUAL_UNDO,
                            detail = pendingUndoMessage ?: "직전 변경 취소"
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
                Text("편집 모드: 아래 값을 수정하고 저장 버튼을 눌러 주세요.")
            }

            EditorPage(
                editingAlarmId = editingAlarmId,
                initialStep = editorForcedStep,
                selectedLabel = selectedLabel,
                onSelectedLabelChange = { selectedLabel = it },
                canScheduleExact = canScheduleExact,
                isIgnoringBatteryOptimization = isIgnoringBatteryOptimization,
                onOpenExactAlarmSettings = onOpenExactAlarmSettings,
                onOpenBatterySettings = onOpenBatterySettings,
                onOpenAppDetailSettings = onOpenAppDetailSettings,
                onRescheduleAllEnabled = { vm.rescheduleAllEnabled() },
                canPostNotifications = canPostNotifications,
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
                        .putExtra(AlarmReceiver.EXTRA_LABEL, if (selectedLabel.isBlank()) "기본 알람" else selectedLabel)
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
                selectedTime = selectedTime,
                onSelectedTimeChange = { selectedTime = it },
                anchorDate = anchorDate,
                onAnchorDateChange = { anchorDate = it },
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
                onInfiniteRotationEnabledChange = { infiniteRotationEnabled = it },
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
                requiresExactPermission = requiresExactPermission
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
                customWorkTypeInput = customWorkTypeInput,
                onCustomWorkTypeInputChange = { customWorkTypeInput = it },
                onAddType = {
                    val name = normalizeWorkType(customWorkTypeInput)
                    if (name.isBlank()) {
                        autoBuildFeedback = "근무 타입 이름이 비어 있습니다."
                        return@ShiftPage
                    }
                    if (workTypeConfigs.any { normalizeWorkType(it.type) == name }) {
                        autoBuildFeedback = "이미 있는 근무 타입입니다."
                        return@ShiftPage
                    }
                    workTypeConfigs = workTypeConfigs + WorkTypeAlarmConfig(
                        type = name,
                        enabled = true,
                        primaryTime = defaultPrimaryTime(name),
                        secondaryTime = ""
                    )
                    customWorkTypeInput = ""
                    autoBuildFeedback = "근무 타입 '$name' 추가 완료"
                },
                onResetDefaults = {
                    val defaults = defaultWorkTypeConfigs(listOf("주간", "야간", "비번", "휴무"))
                    workTypeConfigs = defaults
                    rotationSequence = listOf("주간", "야간", "비번", "휴무")
                    todayRotationIndex = 0
                    persistSelectedCategory(ShiftCategory.THREE_SHIFT)
                    autoBuildFeedback = "기본값으로 초기화 완료"
                },
                quickTemplates = QUICK_SHIFT_TEMPLATES,
                onApplyQuickTemplate = { template ->
                    val normalized = template.sequence.map(::normalizeWorkType).filter { it.isNotBlank() }
                    val configTypes = (normalized + listOf("휴무")).distinct()
                    workTypeConfigs = defaultWorkTypeConfigs(configTypes)
                    rotationSequence = normalized
                    todayRotationIndex = 0
                    infiniteRotationEnabled = true
                    persistSelectedCategory(template.category)
                    autoBuildFeedback = "빠른 템플릿 '${template.label}' 적용 완료"
                },
                workTypeConfigs = workTypeConfigs,
                onAppendRotationType = { type ->
                    rotationSequence = rotationSequence + normalizeWorkType(type)
                    autoBuildFeedback = "${type} 추가됨"
                },
                rotationSequence = rotationSequence,
                onDropLastRotation = {
                    if (rotationSequence.isNotEmpty()) {
                        rotationSequence = rotationSequence.dropLast(1)
                        todayRotationIndex = todayRotationIndex.coerceAtMost((rotationSequence.size - 1).coerceAtLeast(0))
                    }
                },
                onClearRotation = {
                    rotationSequence = emptyList()
                    todayRotationIndex = 0
                },
                todayRotationIndex = todayRotationIndex,
                onTodayRotationIndexChange = { todayRotationIndex = it },
                onToggleConfigEnabled = { index, checked ->
                    val copy = workTypeConfigs.toMutableList()
                    copy[index] = copy[index].copy(enabled = checked)
                    workTypeConfigs = copy
                },
                onDeleteConfigType = { target ->
                    workTypeConfigs = workTypeConfigs.filterNot { it.type == target }
                    rotationSequence = rotationSequence.filterNot { it == target }
                    todayRotationIndex = todayRotationIndex.coerceAtMost((rotationSequence.size - 1).coerceAtLeast(0))
                },
                onConfigPrimaryChange = { index, value ->
                    val copy = workTypeConfigs.toMutableList()
                    copy[index] = copy[index].copy(primaryTime = value)
                    workTypeConfigs = copy
                },
                onConfigSecondaryChange = { index, value ->
                    val copy = workTypeConfigs.toMutableList()
                    copy[index] = copy[index].copy(secondaryTime = value)
                    workTypeConfigs = copy
                },
                infiniteRotationEnabled = infiniteRotationEnabled,
                onInfiniteRotationEnabledChange = { infiniteRotationEnabled = it },
                onAutoBuild = { runAutoBuildFromShiftConfig(requireInfinite = true) },
                showFirstSetupWizard = !shiftQuickSetupDone && !shiftQuickSetupHidden,
                onCompleteFirstSetup = {
                    val success = runAutoBuildFromShiftConfig(requireInfinite = false)
                    if (success) {
                        shiftQuickSetupDone = true
                        shiftQuickSetupHidden = true
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
                onReopenFirstSetupWizard = { reopenPatternSetup() },
                autoBuildFeedback = autoBuildFeedback,
                selectedCategory = selectedShiftCategory,
                onSelectedCategoryChange = { persistSelectedCategory(it) },
                anchorDate = anchorDate,
                onAnchorDateChange = { anchorDate = it },
                    )
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

        AnimatedVisibility(
            visible = currentPage == AlarmPage.PRESET,
            enter = EnterTransition.None,
            exit = ExitTransition.None
        ) {
            PresetPage(
                presetNameInput = presetNameInput,
                onPresetNameInputChange = { presetNameInput = it },
                onSaveCurrent = {
                    val normalized = normalizeWeekPatterns(intervalWeeks, weekPatterns)
                    val name = presetNameInput.trim().ifBlank {
                        "프리셋 ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))}"
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
                savedPresets = savedPresets.sortedBy { if (inferPresetCategory(it) == selectedShiftCategory) 0 else 1 },
                onApplyPreset = { preset ->
                    val presetInterval = normalizeIntervalWeeks(preset.intervalWeeks)
                    activeWeekIndex = 0
                    anchorDate = preset.anchorDate
                    applyIntervalWeeks(presetInterval, preset.weekPatterns)
                    presetNameInput = preset.name
                    infiniteRotationEnabled = preset.infiniteRotationEnabled
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
                        infiniteRotationEnabled = enabled
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
                    val copiedLabel = alarm.duplicateLabel()
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


