package com.example.shiftalarmmvp.ui

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.shiftalarmmvp.R
import com.example.shiftalarmmvp.data.AlarmRule
import com.example.shiftalarmmvp.data.AlarmSoundType
import com.example.shiftalarmmvp.receiver.AlarmReceiver
import com.example.shiftalarmmvp.scheduler.AlarmScheduler
import com.example.shiftalarmmvp.scheduler.AlarmTimeCalculator
import com.example.shiftalarmmvp.service.AlarmRingingService
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val vm: AlarmViewModel by viewModels()
    private val scheduler by lazy { AlarmScheduler(this) }
    private var canScheduleExact by mutableStateOf(true)
    private var canPostNotifications by mutableStateOf(true)
    private var isIgnoringBatteryOptimizationState by mutableStateOf(true)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        canPostNotifications = granted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refreshExactAlarmPermissionState()
        refreshNotificationPermissionState()
        refreshBatteryOptimizationState()
        ensureRuntimePermissions()

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
                    isIgnoringBatteryOptimization = isIgnoringBatteryOptimizationState
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val previous = canScheduleExact
        refreshExactAlarmPermissionState()
        refreshNotificationPermissionState()
        refreshBatteryOptimizationState()
        if (!previous && canScheduleExact) {
            vm.rescheduleAllEnabled()
        }
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


    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
            )
        }
    }

    private fun openBatteryOptimizationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
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
    isIgnoringBatteryOptimization: Boolean
) {
    val alarms by vm.alarms.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

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
                emptySet()
            )
        )
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

    var customWorkTypeInput by remember { mutableStateOf("") }
    var rotationSequence by remember { mutableStateOf(listOf("주간", "당직", "비번", "휴무")) }
    var todayRotationIndex by remember { mutableIntStateOf(0) }
    var workTypeConfigs by remember { mutableStateOf(defaultWorkTypeConfigs(rotationSequence.distinct() + listOf("휴무"))) }
    var infiniteRotationEnabled by remember { mutableStateOf(true) }
    var autoBuildFeedback by remember { mutableStateOf("") }
    val presetStore = remember(context) { RotationPresetStore(context) }
    val savedPresets = remember { mutableStateListOf<RotationPreset>() }
    val alarmLogStore = remember(context) { AlarmLogStore(context) }
    val alarmLogs = remember { mutableStateListOf<AlarmLogEntry>() }

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
            label = "홈 캘린더",
            type = type,
            detail = detail
        )
        refreshAlarmLogs()
        pushUndo(message, snapshot)
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
                    customSoundMessage = "이 소리는 재생 불가하여 기본 알람음으로 전환됨"
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
                } ?: error("출력 스트림을 열 수 없음")
            }
            presetFeedbackMessage = if (exportResult.isSuccess) {
                "프리셋 내보내기 완료"
            } else {
                "프리셋 내보내기 실패: ${exportResult.exceptionOrNull()?.message ?: "알 수 없음"}"
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
                    ?: error("입력 스트림을 열 수 없음")
                presetStore.importJson(content, merge = importMergeMode)
            }
            if (importResult.isSuccess) {
                savedPresets.clear()
                savedPresets.addAll(presetStore.load())
                val modeLabel = if (importMergeMode) "병합" else "교체"
                presetFeedbackMessage = "프리셋 ${importResult.getOrDefault(0)}개 ${modeLabel} 가져오기 완료"
            } else {
                presetFeedbackMessage = "프리셋 가져오기 실패: ${importResult.exceptionOrNull()?.message ?: "형식 오류"}"
            }
        }
    }
    val visiblePatterns = weekPatterns.take(intervalWeeks)
    val canSave = visiblePatterns.any { it.isNotEmpty() }
    val exactReady = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || canScheduleExact
    val batteryReady = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || isIgnoringBatteryOptimization
    val notificationReady = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || canPostNotifications
    val reliabilityStatus = when {
        !exactReady || !notificationReady -> AlarmReliabilityStatus.ISSUE
        !batteryReady -> AlarmReliabilityStatus.WARNING
        else -> AlarmReliabilityStatus.OK
    }
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

    fun scheduleSelfTest(showToast: Boolean = false) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val triggerAtMillis = System.currentTimeMillis() + 2 * 60 * 1000L
        val testIntent = Intent(context, AlarmReceiver::class.java)
            .putExtra(AlarmReceiver.EXTRA_ALARM_ID, 999_999L)
            .putExtra(AlarmReceiver.EXTRA_LABEL, if (selectedLabel.isBlank()) "2분 테스트 알람" else "${selectedLabel} 테스트")
            .putExtra(AlarmReceiver.EXTRA_SOUND_TYPE, selectedSoundType.name)
            .putExtra(AlarmReceiver.EXTRA_CUSTOM_SOUND_URI, selectedCustomSoundUri)
            .putExtra(AlarmReceiver.EXTRA_VOLUME_PERCENT, selectedVolume.toInt())
            .putExtra(AlarmReceiver.EXTRA_VIBRATION_ENABLED, vibrationEnabled)
            .putExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, selectedSnoozeMinutes)
            .putExtra(AlarmReceiver.EXTRA_SNOOZE_MAX_COUNT, 0)
            .putExtra(AlarmReceiver.EXTRA_SNOOZE_CURRENT_COUNT, 0)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            999_999,
            testIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && canScheduleExact -> {
                alarmManager?.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                alarmManager?.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
            else -> {
                alarmManager?.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        }
        val triggerAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(triggerAtMillis), ZoneId.systemDefault())
        selfTestMessage = "2분 테스트 예약됨: ${triggerAt.format(DateTimeFormatter.ofPattern("HH:mm"))}"
        if (showToast) {
            Toast.makeText(context, selfTestMessage, Toast.LENGTH_SHORT).show()
        }
    }

    fun cancelSelfTest(showToast: Boolean = false) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            999_999,
            Intent(context, AlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        selfTestMessage = if (pendingIntent != null) {
            alarmManager?.cancel(pendingIntent)
            pendingIntent.cancel()
            "2분 테스트 예약을 취소했습니다."
        } else {
            "취소할 테스트 예약이 없습니다."
        }
        if (showToast) {
            Toast.makeText(context, selfTestMessage, Toast.LENGTH_SHORT).show()
        }
    }
    fun runAutoBuildFromShiftConfig(requireInfinite: Boolean): Boolean {
        if (rotationSequence.isEmpty()) {
            autoBuildFeedback = "로테이션을 먼저 입력하세요."
            return false
        }
        if (requireInfinite && !infiniteRotationEnabled) {
            autoBuildFeedback = "계속 순환 스위치를 켜면 자동 생성됩니다."
            return false
        }
        if (workTypeConfigs.none { it.enabled }) {
            autoBuildFeedback = "활성화된 근무 알람이 없습니다."
            return false
        }

        val built = buildWorkTemplateRotation(rotationSequence, todayRotationIndex)
        val uniqueTypes = rotationSequence.distinct()
        val typeToPattern = uniqueTypes.associateWith { type ->
            buildWeeklyPatternForType(built, type)
        }

        var createdCount = 0
        var invalidCount = 0
        workTypeConfigs.forEach { cfg ->
            if (!cfg.enabled) return@forEach
            val pattern = typeToPattern[cfg.type].orEmpty()
            val isVacationType = cfg.type.contains("휴가")
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
                    anchorDate = LocalDate.now(),
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
                        anchorDate = LocalDate.now(),
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
            createdCount == 0 -> "생성된 알람이 없습니다. 시간 형식을 확인하세요."
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
                    verticalAlignment = Alignment.CenterVertically
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
                            modifier = Modifier.size(40.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "교대근무",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White
                                )
                                Text(
                                    text = "알람",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color(0xFF6EA0FF)
                                )
                            }
                            Text(
                                "패턴을 설정하면 자동 반복됩니다.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.86f)
                            )
                        }
                    }
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AlarmReliabilityChip(
                            status = reliabilityStatus,
                            onOpenReliabilityCenter = {
                                editorForcedStep = 3
                                currentPage = AlarmPage.EDITOR
                                scope.launch { scrollState.animateScrollTo(0) }
                            }
                        )
                        Card(
                            modifier = Modifier.clickable {
                                editorForcedStep = 3
                                currentPage = AlarmPage.EDITOR
                                scope.launch { scrollState.animateScrollTo(0) }
                            },
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.16f))
                        ) {
                            Text(
                                text = "테스트 열기",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                        Card(
                            modifier = Modifier.clickable { scheduleSelfTest(showToast = true) },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2F6EF1))
                        ) {
                            Text(
                                text = "2분 테스트",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
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
                nextTrigger = next10Preview.firstOrNull(),
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
                        message = "휴가 처리 적용됨",
                        type = AlarmLogType.MANUAL_VACATION_SET,
                        detail = date.toString()
                    ) { vm.setVacationDateForAll(date) }
                },
                onClearVacationDate = { date ->
                    registerCalendarChange(
                        message = "휴가 해제 적용됨",
                        type = AlarmLogType.MANUAL_VACATION_CLEAR,
                        detail = date.toString()
                    ) { vm.clearVacationDateForAll(date) }
                },
                onSetVacationRange = { start, end ->
                    val from = minOf(start, end)
                    val to = maxOf(start, end)
                    registerCalendarChange(
                        message = "기간 휴가 적용됨",
                        type = AlarmLogType.MANUAL_VACATION_SET,
                        detail = "$from~$to"
                    ) { vm.setVacationRangeForAll(start, end) }
                },
                onClearVacationRange = { start, end ->
                    val from = minOf(start, end)
                    val to = maxOf(start, end)
                    registerCalendarChange(
                        message = "기간 휴가 해제됨",
                        type = AlarmLogType.MANUAL_VACATION_CLEAR,
                        detail = "$from~$to"
                    ) { vm.clearVacationRangeForAll(start, end) }
                },
                onSetSkipDateForIds = { date, ids ->
                    if (ids.isNotEmpty()) {
                        registerCalendarChange(
                            message = "스킵 처리됨",
                            type = AlarmLogType.MANUAL_SKIP_SET,
                            detail = "${date} (${ids.size}개 알람)"
                        ) { vm.setSkipDateForAlarmIds(date, ids) }
                    }
                },
                onClearSkipDateForIds = { date, ids ->
                    if (ids.isNotEmpty()) {
                        registerCalendarChange(
                            message = "스킵 해제됨",
                            type = AlarmLogType.MANUAL_SKIP_CLEAR,
                            detail = "${date} (${ids.size}개 알람)"
                        ) { vm.clearSkipDateForAlarmIds(date, ids) }
                    }
                },
                onApplyShiftChange = { date, type ->
                    registerCalendarChange(
                        message = "근무 변경 적용됨",
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
                            label = "홈 캘린더",
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
                Text("편집 모드: 아래 값 수정 후 '알람 수정 저장'을 누르세요.")
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
                        .putExtra(AlarmReceiver.EXTRA_LABEL, if (selectedLabel.isBlank()) "테스트 알람" else selectedLabel)
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
                onIntervalWeeksChange = { intervalWeeks = it },
                activeWeekIndex = activeWeekIndex,
                onActiveWeekIndexChange = { activeWeekIndex = it },
                weekPatterns = weekPatterns,
                onWeekPatternsChange = { weekPatterns = it },
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
                        autoBuildFeedback = "근무 유형 이름을 입력하세요."
                        return@ShiftPage
                    }
                    if (workTypeConfigs.any { normalizeWorkType(it.type) == name }) {
                        autoBuildFeedback = "이미 있는 근무 유형입니다."
                        return@ShiftPage
                    }
                    workTypeConfigs = workTypeConfigs + WorkTypeAlarmConfig(
                        type = name,
                        enabled = true,
                        primaryTime = defaultPrimaryTime(name),
                        secondaryTime = ""
                    )
                    customWorkTypeInput = ""
                    autoBuildFeedback = "근무 유형 '$name' 추가 완료"
                },
                onResetDefaults = {
                    val defaults = defaultWorkTypeConfigs(listOf("주간", "당직", "비번", "휴무"))
                    workTypeConfigs = defaults
                    rotationSequence = listOf("주간", "당직", "비번", "휴무")
                    todayRotationIndex = 0
                    persistSelectedCategory(ShiftCategory.THREE_SHIFT)
                    autoBuildFeedback = "기본 패턴 적용됨"
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
                    autoBuildFeedback = "대표 근무형 '${template.label}' 적용 완료"
                },
                workTypeConfigs = workTypeConfigs,
                onAppendRotationType = { type ->
                    rotationSequence = rotationSequence + normalizeWorkType(type)
                    autoBuildFeedback = "${type} 칸 추가"
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
                    val normalized = weekPatterns.take(intervalWeeks.coerceIn(1, 4))
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
                    intervalWeeks = preset.intervalWeeks.coerceIn(2, 4)
                    activeWeekIndex = 0
                    anchorDate = preset.anchorDate
                    weekPatterns = listOf(
                        preset.weekPatterns.getOrNull(0).orEmpty(),
                        preset.weekPatterns.getOrNull(1).orEmpty(),
                        preset.weekPatterns.getOrNull(2).orEmpty(),
                        preset.weekPatterns.getOrNull(3).orEmpty()
                    )
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
                    val copiedLabel = if (alarm.label.isBlank()) "알람 복사본" else "${alarm.label} 복사본".take(24)
                    val copiedPattern = listOf(
                        alarm.weeklyPattern.getOrNull(0).orEmpty(),
                        alarm.weeklyPattern.getOrNull(1).orEmpty(),
                        alarm.weeklyPattern.getOrNull(2).orEmpty(),
                        alarm.weeklyPattern.getOrNull(3).orEmpty()
                    )
                    if (autoSaveOnDuplicate) {
                        vm.addAlarm(
                            label = copiedLabel,
                            hour = alarm.hour,
                            minute = alarm.minute,
                            weeklyPattern = copiedPattern,
                            intervalWeeks = alarm.intervalWeeks.coerceIn(2, 4),
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
                        editorForcedStep = null
                        editingAlarmId = null
                        editingEnabled = true
                        selectedTime = LocalTime.of(alarm.hour, alarm.minute)
                        selectedLabel = copiedLabel
                        intervalWeeks = alarm.intervalWeeks.coerceIn(2, 4)
                        activeWeekIndex = 0
                        anchorDate = alarm.anchorDate
                        weekPatterns = copiedPattern
                        selectedSoundType = alarm.soundType
                        selectedCustomSoundUri = alarm.customSoundUri
                        selectedVolume = alarm.volumePercent.toFloat()
                        selectedSnoozeMinutes = alarm.snoozeMinutes
                        customSnoozeInput = alarm.snoozeMinutes.toString()
                        selectedSnoozeMaxCount = alarm.snoozeMaxCount
                        customMaxCountInput = alarm.snoozeMaxCount.toString()
                        vibrationEnabled = alarm.vibrationEnabled
                        skipDates = alarm.skipDateEpochDays
                        addDates = alarm.addDateEpochDays
                        exceptionDate = LocalDate.now()
                        currentPage = AlarmPage.EDITOR
                        scope.launch { scrollState.animateScrollTo(0) }
                    }
                },
                onEdit = { alarm ->
                    editorForcedStep = null
                    editingAlarmId = alarm.id
                    editingEnabled = alarm.enabled
                    selectedTime = LocalTime.of(alarm.hour, alarm.minute)
                    selectedLabel = alarm.label
                    intervalWeeks = alarm.intervalWeeks.coerceIn(2, 4)
                    activeWeekIndex = 0
                    anchorDate = alarm.anchorDate
                    weekPatterns = listOf(
                        alarm.weeklyPattern.getOrNull(0).orEmpty(),
                        alarm.weeklyPattern.getOrNull(1).orEmpty(),
                        alarm.weeklyPattern.getOrNull(2).orEmpty(),
                        alarm.weeklyPattern.getOrNull(3).orEmpty()
                    )
                    selectedSoundType = alarm.soundType
                    selectedCustomSoundUri = alarm.customSoundUri
                    selectedVolume = alarm.volumePercent.toFloat()
                    selectedSnoozeMinutes = alarm.snoozeMinutes
                    customSnoozeInput = alarm.snoozeMinutes.toString()
                    selectedSnoozeMaxCount = alarm.snoozeMaxCount
                    customMaxCountInput = alarm.snoozeMaxCount.toString()
                    vibrationEnabled = alarm.vibrationEnabled
                    skipDates = alarm.skipDateEpochDays
                    addDates = alarm.addDateEpochDays
                    exceptionDate = LocalDate.now()
                        currentPage = AlarmPage.EDITOR
                    scope.launch { scrollState.animateScrollTo(0) }
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
private enum class AlarmReliabilityStatus {
    OK,
    WARNING,
    ISSUE
}

@Composable
private fun AlarmReliabilityChip(
    status: AlarmReliabilityStatus,
    onOpenReliabilityCenter: () -> Unit
) {
    val ui = when (status) {
        AlarmReliabilityStatus.OK -> ReliabilityChipUi(
            label = "정상",
            containerColor = Color.White.copy(alpha = 0.14f),
            textColor = Color.White.copy(alpha = 0.82f),
            clickable = false
        )
        AlarmReliabilityStatus.WARNING -> ReliabilityChipUi(
            label = "⚠ 점검",
            containerColor = Color(0xFFFFD89E),
            textColor = Color(0xFF4A3000),
            clickable = true
        )
        AlarmReliabilityStatus.ISSUE -> ReliabilityChipUi(
            label = "⚠ 권한",
            containerColor = Color(0xFFFF6B6B),
            textColor = Color.White,
            clickable = true
        )
    }

    val chipModifier = if (ui.clickable) {
        Modifier.clickable(onClick = onOpenReliabilityCenter)
    } else {
        Modifier
    }

    Card(
        modifier = chipModifier,
        colors = CardDefaults.cardColors(containerColor = ui.containerColor)
    ) {
        Text(
            text = ui.label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = ui.textColor
        )
    }
}

private data class ReliabilityChipUi(
    val label: String,
    val containerColor: Color,
    val textColor: Color,
    val clickable: Boolean
)

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

