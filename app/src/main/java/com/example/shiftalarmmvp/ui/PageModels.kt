package com.example.shiftalarmmvp.ui

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.example.shiftalarmmvp.R
import com.example.shiftalarmmvp.data.normalizeIntervalWeeks
import com.example.shiftalarmmvp.data.normalizeWeekPatterns
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

enum class AlarmPage(@StringRes val labelResId: Int) {
    TODAY(R.string.main_nav_today),
    PATTERN(R.string.main_nav_pattern),
    EXCEPTION(R.string.main_nav_exception),
    MANAGE(R.string.main_nav_manage),
    EDITOR(R.string.main_nav_editor),
    PRESET(R.string.main_nav_preset)
}

data class WorkTypeAlarmConfig(
    val type: String,
    val enabled: Boolean,
    val primaryTime: String,
    val secondaryTime: String
)

data class BuiltWorkRotation(
    val intervalWeeks: Int,
    val sequence: List<String>,
    val todayIndex: Int
)

enum class ShiftCategory {
    TWO_SHIFT,
    THREE_SHIFT,
    CUSTOM
}

data class QuickShiftTemplate(
    val id: String,
    @StringRes val labelResId: Int,
    val sequence: List<String>,
    val category: ShiftCategory
)

const val WORK_TYPE_DUTY = "당직"
const val WORK_TYPE_OFF = "비번"
const val WORK_TYPE_DAY = "주간"
const val WORK_TYPE_NIGHT = "야간"
const val WORK_TYPE_REST = "휴무"
const val WORK_TYPE_VACATION = "휴가"
const val WORK_TYPE_HOLIDAY = "휴일"

const val WORK_TYPE_TOKEN_DAY = "주"
const val WORK_TYPE_TOKEN_NIGHT = "야"
const val WORK_TYPE_TOKEN_DUTY = "당"
const val WORK_TYPE_TOKEN_OFF = "비"
const val WORK_TYPE_TOKEN_REST = "휴"
const val WORK_TYPE_TOKEN_HOLIDAY = "공휴일"
const val WORK_TYPE_TOKEN_WORK = "근무"
const val WORK_TYPE_TOKEN_EVENING = "석간"
const val WORK_TYPE_TOKEN_EVENING_SHORT = "석"

const val QUICK_TEMPLATE_ID_DAY_NIGHT = "day_night"
const val QUICK_TEMPLATE_ID_DUTY_OFF = "duty_off"
const val QUICK_TEMPLATE_ID_EVERY_OTHER_DAY = "every_other_day"
const val QUICK_TEMPLATE_ID_TWO_SHIFT_CUSTOM = "two_shift_custom"
const val QUICK_TEMPLATE_ID_DAY_DUTY_OFF = "day_duty_off"
const val QUICK_TEMPLATE_ID_DAY_NIGHT_OFF = "day_night_off"
const val QUICK_TEMPLATE_ID_DOUBLE_DAY_DOUBLE_NIGHT_OFF_OFF = "double_day_double_night_off_off"
const val QUICK_TEMPLATE_ID_THREE_SHIFT_CUSTOM = "three_shift_custom"
const val QUICK_TEMPLATE_ID_CUSTOM = "custom"

val STANDARD_WORK_TYPES: List<String> = listOf(
    WORK_TYPE_DUTY,
    WORK_TYPE_OFF,
    WORK_TYPE_DAY,
    WORK_TYPE_NIGHT,
    WORK_TYPE_REST,
    WORK_TYPE_VACATION,
    WORK_TYPE_HOLIDAY
)

private val WORK_TYPE_ALIAS_MAP: Map<String, String> = mapOf(
    WORK_TYPE_TOKEN_DAY to WORK_TYPE_DAY,
    WORK_TYPE_TOKEN_NIGHT to WORK_TYPE_NIGHT,
    WORK_TYPE_TOKEN_DUTY to WORK_TYPE_DUTY,
    WORK_TYPE_TOKEN_OFF to WORK_TYPE_OFF,
    WORK_TYPE_TOKEN_REST to WORK_TYPE_REST,
    WORK_TYPE_TOKEN_HOLIDAY to WORK_TYPE_HOLIDAY,
    WORK_TYPE_TOKEN_WORK to WORK_TYPE_DUTY,
    WORK_TYPE_TOKEN_EVENING to WORK_TYPE_NIGHT,
    WORK_TYPE_TOKEN_EVENING_SHORT to WORK_TYPE_NIGHT
)

private val WORK_TYPE_MATCH_ORDER: List<String> =
    (STANDARD_WORK_TYPES + WORK_TYPE_ALIAS_MAP.keys)
        .distinct()
        .sortedByDescending { it.length }

val QUICK_SHIFT_TEMPLATES: List<QuickShiftTemplate> = listOf(
    QuickShiftTemplate(
        id = QUICK_TEMPLATE_ID_DAY_NIGHT,
        labelResId = R.string.page_models_quick_template_day_night,
        sequence = listOf(WORK_TYPE_DAY, WORK_TYPE_NIGHT),
        category = ShiftCategory.TWO_SHIFT
    ),
    QuickShiftTemplate(
        id = QUICK_TEMPLATE_ID_DUTY_OFF,
        labelResId = R.string.page_models_quick_template_duty_off,
        sequence = listOf(WORK_TYPE_DUTY, WORK_TYPE_OFF),
        category = ShiftCategory.TWO_SHIFT
    ),
    QuickShiftTemplate(
        id = QUICK_TEMPLATE_ID_EVERY_OTHER_DAY,
        labelResId = R.string.page_models_quick_template_every_other_day,
        sequence = listOf(WORK_TYPE_DUTY, WORK_TYPE_REST),
        category = ShiftCategory.TWO_SHIFT
    ),
    QuickShiftTemplate(
        id = QUICK_TEMPLATE_ID_TWO_SHIFT_CUSTOM,
        labelResId = R.string.page_models_quick_template_two_shift_custom,
        sequence = listOf(WORK_TYPE_DAY, WORK_TYPE_NIGHT),
        category = ShiftCategory.TWO_SHIFT
    ),
    QuickShiftTemplate(
        id = QUICK_TEMPLATE_ID_DAY_DUTY_OFF,
        labelResId = R.string.page_models_quick_template_day_duty_off,
        sequence = listOf(WORK_TYPE_DAY, WORK_TYPE_DUTY, WORK_TYPE_OFF),
        category = ShiftCategory.THREE_SHIFT
    ),
    QuickShiftTemplate(
        id = QUICK_TEMPLATE_ID_DAY_NIGHT_OFF,
        labelResId = R.string.page_models_quick_template_day_night_off,
        sequence = listOf(WORK_TYPE_DAY, WORK_TYPE_NIGHT, WORK_TYPE_OFF),
        category = ShiftCategory.THREE_SHIFT
    ),
    QuickShiftTemplate(
        id = QUICK_TEMPLATE_ID_DOUBLE_DAY_DOUBLE_NIGHT_OFF_OFF,
        labelResId = R.string.page_models_quick_template_double_day_double_night_off_off,
        sequence = listOf(WORK_TYPE_DAY, WORK_TYPE_DAY, WORK_TYPE_NIGHT, WORK_TYPE_NIGHT, WORK_TYPE_OFF, WORK_TYPE_OFF),
        category = ShiftCategory.THREE_SHIFT
    ),
    QuickShiftTemplate(
        id = QUICK_TEMPLATE_ID_THREE_SHIFT_CUSTOM,
        labelResId = R.string.page_models_quick_template_three_shift_custom,
        sequence = listOf(WORK_TYPE_DAY, WORK_TYPE_DUTY, WORK_TYPE_OFF, WORK_TYPE_REST),
        category = ShiftCategory.THREE_SHIFT
    ),
    QuickShiftTemplate(
        id = QUICK_TEMPLATE_ID_CUSTOM,
        labelResId = R.string.page_models_quick_template_custom,
        sequence = listOf(WORK_TYPE_DAY, WORK_TYPE_DUTY, WORK_TYPE_OFF),
        category = ShiftCategory.CUSTOM
    )
)

private sealed interface ShiftPatternBadgeKey {
    object None : ShiftPatternBadgeKey
    object Rest : ShiftPatternBadgeKey
    object Daily : ShiftPatternBadgeKey
    object DutyOff : ShiftPatternBadgeKey
    object TwoDayCycle : ShiftPatternBadgeKey
    object DayDutyOff : ShiftPatternBadgeKey
    object DayNightOff : ShiftPatternBadgeKey
    object ThreeDayCycle : ShiftPatternBadgeKey
    object DoubleDayDoubleNight : ShiftPatternBadgeKey
    object FourTeamThreeShift : ShiftPatternBadgeKey
    object DayNightTwoShift : ShiftPatternBadgeKey
    object Weekday : ShiftPatternBadgeKey
    object WeeklyFive : ShiftPatternBadgeKey
    object Weekend : ShiftPatternBadgeKey
    data class WeeklyCount(val activeCount: Int) : ShiftPatternBadgeKey
    data class HalfRotation(val period: Int) : ShiftPatternBadgeKey
    data class Rotation(val period: Int) : ShiftPatternBadgeKey
}

fun normalizeWorkType(raw: String): String {
    val text = raw.trim()
    if (text.isBlank()) return ""
    val firstToken = text.substringBefore(" ").trim()
    val matched = WORK_TYPE_MATCH_ORDER.firstOrNull { firstToken.startsWith(it) } ?: firstToken
    return WORK_TYPE_ALIAS_MAP[matched] ?: matched
}

fun extractWorkTypeFromLabel(label: String): String = normalizeWorkType(label)

fun templatesForCategory(category: ShiftCategory): List<QuickShiftTemplate> {
    return QUICK_SHIFT_TEMPLATES.filter { it.category == category }
}

fun inferPresetCategory(preset: RotationPreset): ShiftCategory {
    return when (inferShiftPatternBadgeKey(preset)) {
        ShiftPatternBadgeKey.DutyOff,
        ShiftPatternBadgeKey.TwoDayCycle,
        ShiftPatternBadgeKey.DoubleDayDoubleNight,
        ShiftPatternBadgeKey.DayNightTwoShift -> ShiftCategory.TWO_SHIFT

        ShiftPatternBadgeKey.DayDutyOff,
        ShiftPatternBadgeKey.DayNightOff,
        ShiftPatternBadgeKey.ThreeDayCycle,
        ShiftPatternBadgeKey.FourTeamThreeShift -> ShiftCategory.THREE_SHIFT

        else -> ShiftCategory.CUSTOM
    }
}

fun hasRestFamilyToken(type: String): Boolean = type.contains(WORK_TYPE_TOKEN_REST)

fun isDefaultEnabledWorkType(type: String): Boolean {
    return when {
        type.contains(WORK_TYPE_VACATION) -> true
        type.contains(WORK_TYPE_TOKEN_OFF) || hasRestFamilyToken(type) -> false
        else -> true
    }
}

fun defaultWorkTypeConfigs(types: List<String>): List<WorkTypeAlarmConfig> {
    return types.map(::normalizeWorkType).filter { it.isNotBlank() }.distinct().map { type ->
        WorkTypeAlarmConfig(
            type = type,
            enabled = isDefaultEnabledWorkType(type),
            primaryTime = defaultPrimaryTime(type),
            secondaryTime = ""
        )
    }
}

fun defaultPrimaryTime(type: String): String {
    return when (normalizeWorkType(type)) {
        WORK_TYPE_DAY -> "06:30"
        WORK_TYPE_NIGHT -> "20:30"
        WORK_TYPE_DUTY -> "08:30"
        WORK_TYPE_VACATION -> "09:30"
        WORK_TYPE_REST, WORK_TYPE_HOLIDAY, WORK_TYPE_OFF -> "08:00"
        else -> "07:00"
    }
}

fun parseHm(raw: String): LocalTime? {
    val match = Regex("^(\\d{1,2}):(\\d{2})$").matchEntire(raw.trim()) ?: return null
    val h = match.groupValues[1].toIntOrNull() ?: return null
    val m = match.groupValues[2].toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59) return null
    return LocalTime.of(h, m)
}

fun buildWorkTemplateRotation(
    sequence: List<String>,
    todayIndex: Int
): BuiltWorkRotation {
    val seq = sequence.ifEmpty { listOf(WORK_TYPE_DAY) }
    val period = seq.size.coerceAtLeast(1)
    val start = todayIndex.coerceIn(0, period - 1)
    val intervalWeeks = normalizeIntervalWeeks(lcm(period, 7) / 7)
    return BuiltWorkRotation(intervalWeeks = intervalWeeks, sequence = seq, todayIndex = start)
}

fun buildWeeklyPatternForType(
    rotation: BuiltWorkRotation,
    targetType: String,
    anchor: LocalDate = LocalDate.now()
): List<Set<DayOfWeek>> {
    val interval = normalizeIntervalWeeks(rotation.intervalWeeks)
    val weeks = MutableList(interval) { mutableSetOf<DayOfWeek>() }
    val period = rotation.sequence.size.coerceAtLeast(1)
    val normalizedTarget = normalizeWorkType(targetType)
    val anchorWeekStart = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    repeat(interval * 7) { offset ->
        val step = normalizeWorkType(rotation.sequence[(rotation.todayIndex + offset) % period])
        if (step == normalizedTarget) {
            val date = anchor.plusDays(offset.toLong())
            val dateWeekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val weeksBetween = ChronoUnit.WEEKS.between(anchorWeekStart, dateWeekStart)
            if (weeksBetween >= 0) {
                val slot = (weeksBetween % interval.toLong()).toInt()
                weeks[slot].add(date.dayOfWeek)
            }
        }
    }
    return weeks.map { it.toSet() }
}

fun buildWorkPreview(
    resources: Resources,
    sequence: List<String>,
    todayIndex: Int,
    days: Int,
    anchor: LocalDate
): List<String> {
    if (sequence.isEmpty()) return emptyList()
    val start = todayIndex.coerceIn(0, sequence.size - 1)
    val period = sequence.size
    return (0 until days).map { d ->
        val date = anchor.plusDays(d.toLong())
        val type = sequence[(start + d) % period]
        resources.getString(
            R.string.page_models_work_preview_line_format,
            date.format(DateTimeFormatter.ofPattern("MM/dd")),
            type
        )
    }
}

fun workTypeColor(type: String): Color {
    return when (normalizeWorkType(type)) {
        WORK_TYPE_DAY -> Color(0xFF1565C0)
        WORK_TYPE_NIGHT -> Color(0xFFC62828)
        WORK_TYPE_DUTY -> Color(0xFFEF6C00)
        WORK_TYPE_OFF, WORK_TYPE_REST, WORK_TYPE_VACATION, WORK_TYPE_HOLIDAY -> Color(0xFF616161)
        else -> Color(0xFF2E7D32)
    }
}

fun workTypeColorName(resources: Resources, type: String): String {
    return when (normalizeWorkType(type)) {
        WORK_TYPE_DAY -> resources.getString(R.string.page_models_color_blue)
        WORK_TYPE_NIGHT -> resources.getString(R.string.page_models_color_red)
        WORK_TYPE_DUTY -> resources.getString(R.string.page_models_color_orange)
        WORK_TYPE_OFF, WORK_TYPE_REST, WORK_TYPE_VACATION, WORK_TYPE_HOLIDAY -> resources.getString(R.string.page_models_color_gray)
        else -> resources.getString(R.string.page_models_color_green)
    }
}

private fun gcd(a: Int, b: Int): Int {
    var x = kotlin.math.abs(a)
    var y = kotlin.math.abs(b)
    while (y != 0) {
        val t = x % y
        x = y
        y = t
    }
    return if (x == 0) 1 else x
}

private fun lcm(a: Int, b: Int): Int {
    return kotlin.math.abs(a / gcd(a, b) * b)
}

fun dayOfWeekLabel(resources: Resources, day: DayOfWeek): String {
    return when (day) {
        DayOfWeek.MONDAY -> resources.getString(R.string.shift_day_mon)
        DayOfWeek.TUESDAY -> resources.getString(R.string.shift_day_tue)
        DayOfWeek.WEDNESDAY -> resources.getString(R.string.shift_day_wed)
        DayOfWeek.THURSDAY -> resources.getString(R.string.shift_day_thu)
        DayOfWeek.FRIDAY -> resources.getString(R.string.shift_day_fri)
        DayOfWeek.SATURDAY -> resources.getString(R.string.shift_day_sat)
        DayOfWeek.SUNDAY -> resources.getString(R.string.shift_day_sun)
    }
}

fun inferShiftPatternBadge(resources: Resources, preset: RotationPreset): String {
    return when (val key = inferShiftPatternBadgeKey(preset)) {
        ShiftPatternBadgeKey.None -> resources.getString(R.string.page_models_shift_pattern_none)
        ShiftPatternBadgeKey.Rest -> resources.getString(R.string.page_models_shift_pattern_rest)
        ShiftPatternBadgeKey.Daily -> resources.getString(R.string.page_models_shift_pattern_daily)
        ShiftPatternBadgeKey.DutyOff -> resources.getString(R.string.page_models_shift_pattern_duty_off)
        ShiftPatternBadgeKey.TwoDayCycle -> resources.getString(R.string.page_models_shift_pattern_two_day_cycle)
        ShiftPatternBadgeKey.DayDutyOff -> resources.getString(R.string.page_models_shift_pattern_day_duty_off)
        ShiftPatternBadgeKey.DayNightOff -> resources.getString(R.string.page_models_shift_pattern_day_night_off)
        ShiftPatternBadgeKey.ThreeDayCycle -> resources.getString(R.string.page_models_shift_pattern_three_day_cycle)
        ShiftPatternBadgeKey.DoubleDayDoubleNight -> resources.getString(R.string.page_models_shift_pattern_double_day_double_night)
        ShiftPatternBadgeKey.FourTeamThreeShift -> resources.getString(R.string.page_models_shift_pattern_four_team_three_shift)
        ShiftPatternBadgeKey.DayNightTwoShift -> resources.getString(R.string.page_models_shift_pattern_day_night_two_shift)
        ShiftPatternBadgeKey.Weekday -> resources.getString(R.string.page_models_shift_pattern_weekday)
        ShiftPatternBadgeKey.WeeklyFive -> resources.getString(R.string.page_models_shift_pattern_weekly_five)
        ShiftPatternBadgeKey.Weekend -> resources.getString(R.string.page_models_shift_pattern_weekend)
        is ShiftPatternBadgeKey.WeeklyCount -> resources.getString(R.string.page_models_shift_pattern_weekly_count_format, key.activeCount)
        is ShiftPatternBadgeKey.HalfRotation -> resources.getString(R.string.page_models_shift_pattern_half_rotation_format, key.period)
        is ShiftPatternBadgeKey.Rotation -> resources.getString(R.string.page_models_shift_pattern_rotation_format, key.period)
    }
}

private fun inferShiftPatternBadgeKey(preset: RotationPreset): ShiftPatternBadgeKey {
    val cycle = buildPresetCycle(preset)
    if (cycle.isEmpty()) return ShiftPatternBadgeKey.None

    val period = findRepeatingPeriod(cycle)
    val base = cycle.take(period)
    val activeCount = base.count { it }

    if (activeCount == 0) return ShiftPatternBadgeKey.Rest
    if (activeCount == period) return ShiftPatternBadgeKey.Daily

    return when (period) {
        2 -> if (activeCount == 1) ShiftPatternBadgeKey.DutyOff else ShiftPatternBadgeKey.TwoDayCycle
        3 -> if (activeCount == 1) ShiftPatternBadgeKey.DayDutyOff else if (activeCount == 2) ShiftPatternBadgeKey.DayNightOff else ShiftPatternBadgeKey.ThreeDayCycle
        4 -> when {
            patternKey(base) in setOf("1100", "0011", "0110", "1001") -> ShiftPatternBadgeKey.DoubleDayDoubleNight
            activeCount == 3 -> ShiftPatternBadgeKey.FourTeamThreeShift
            activeCount == 2 -> ShiftPatternBadgeKey.DayNightTwoShift
            else -> ShiftPatternBadgeKey.Rotation(period)
        }
        7 -> inferWeeklyBadgeKey(preset, base)
        else -> {
            if (activeCount * 2 == period) ShiftPatternBadgeKey.HalfRotation(period) else ShiftPatternBadgeKey.Rotation(period)
        }
    }
}

private fun inferWeeklyBadgeKey(preset: RotationPreset, base: List<Boolean>): ShiftPatternBadgeKey {
    val activeCount = base.count { it }
    if (activeCount == 5 && hasConsecutiveOff(base, 2)) {
        val firstWeek = preset.weekPatterns.firstOrNull().orEmpty()
        if (firstWeek == setOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY
            )
        ) {
            return ShiftPatternBadgeKey.Weekday
        }
        return ShiftPatternBadgeKey.WeeklyFive
    }

    if (activeCount == 2) {
        val firstWeek = preset.weekPatterns.firstOrNull().orEmpty()
        if (firstWeek == setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) {
            return ShiftPatternBadgeKey.Weekend
        }
    }

    return ShiftPatternBadgeKey.WeeklyCount(activeCount)
}

private fun hasConsecutiveOff(cycle: List<Boolean>, need: Int): Boolean {
    if (cycle.isEmpty() || need <= 0) return false
    var streak = 0
    repeat(cycle.size * 2) { i ->
        val on = cycle[i % cycle.size]
        if (!on) {
            streak += 1
            if (streak >= need) return true
        } else {
            streak = 0
        }
    }
    return false
}

private fun buildPresetCycle(preset: RotationPreset): List<Boolean> {
    val weeks = normalizeIntervalWeeks(preset.intervalWeeks)
    val patterns = normalizeWeekPatterns(weeks, preset.weekPatterns)
    val totalDays = weeks * 7

    return (0 until totalDays).map { offset ->
        val date = preset.anchorDate.plusDays(offset.toLong())
        val weekIndex = (offset / 7) % weeks
        patterns[weekIndex].contains(date.dayOfWeek)
    }
}

private fun findRepeatingPeriod(cycle: List<Boolean>): Int {
    val size = cycle.size
    for (period in 1..size) {
        if (size % period != 0) continue
        var valid = true
        for (index in cycle.indices) {
            if (cycle[index] != cycle[index % period]) {
                valid = false
                break
            }
        }
        if (valid) return period
    }
    return size
}

private fun patternKey(cycle: List<Boolean>): String {
    return cycle.joinToString(separator = "") { if (it) "1" else "0" }
}

fun formatAlarmLogType(resources: Resources, type: AlarmLogType): String {
    return when (type) {
        AlarmLogType.RING_START -> resources.getString(R.string.page_models_alarm_log_ring_start)
        AlarmLogType.STOP -> resources.getString(R.string.page_models_alarm_log_stop)
        AlarmLogType.SNOOZE_SCHEDULED -> resources.getString(R.string.page_models_alarm_log_snooze_scheduled)
        AlarmLogType.SNOOZE_BLOCKED -> resources.getString(R.string.page_models_alarm_log_snooze_blocked)
        AlarmLogType.ONE_MORE_SCHEDULED -> resources.getString(R.string.page_models_alarm_log_one_more_scheduled)
        AlarmLogType.ONE_MORE_SKIPPED -> resources.getString(R.string.page_models_alarm_log_one_more_skipped)
        AlarmLogType.MANUAL_VACATION_SET -> resources.getString(R.string.page_models_alarm_log_manual_vacation_set)
        AlarmLogType.MANUAL_VACATION_CLEAR -> resources.getString(R.string.page_models_alarm_log_manual_vacation_clear)
        AlarmLogType.MANUAL_SKIP_SET -> resources.getString(R.string.page_models_alarm_log_manual_skip_set)
        AlarmLogType.MANUAL_SKIP_CLEAR -> resources.getString(R.string.page_models_alarm_log_manual_skip_clear)
        AlarmLogType.MANUAL_SHIFT_CHANGE -> resources.getString(R.string.page_models_alarm_log_manual_shift_change)
        AlarmLogType.MANUAL_UNDO -> resources.getString(R.string.page_models_alarm_log_manual_undo)
        AlarmLogType.MANUAL_RECOVERY_ACTION -> resources.getString(R.string.page_models_alarm_log_manual_recovery_action)
    }
}

fun formatTimeUntil(
    resources: Resources,
    target: LocalDateTime,
    now: LocalDateTime = LocalDateTime.now()
): String {
    val totalMinutes = java.time.Duration.between(now, target).toMinutes().coerceAtLeast(0)
    if (totalMinutes == 0L) return resources.getString(R.string.page_models_time_until_soon)

    val days = totalMinutes / (24 * 60)
    val hours = (totalMinutes % (24 * 60)) / 60
    val minutes = totalMinutes % 60
    val parts = mutableListOf<String>()

    if (days > 0) parts += resources.getString(R.string.page_models_time_until_days_format, days)
    if (hours > 0) parts += resources.getString(R.string.page_models_time_until_hours_format, hours)
    if (minutes > 0) parts += resources.getString(R.string.page_models_time_until_minutes_format, minutes)

    return parts.joinToString(" ")
}