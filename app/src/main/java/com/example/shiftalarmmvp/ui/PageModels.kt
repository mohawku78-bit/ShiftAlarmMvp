package com.example.shiftalarmmvp.ui

import androidx.compose.ui.graphics.Color
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class AlarmPage(val label: String) {
    TODAY("오늘"),
    PATTERN("패턴"),
    EXCEPTION("예외"),
    MANAGE("관리"),
    EDITOR("편집"),
    PRESET("프리셋")
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
    val label: String,
    val sequence: List<String>,
    val category: ShiftCategory
)

val STANDARD_WORK_TYPES: List<String> = listOf("당직", "비번", "주간", "야간", "휴무", "휴가", "휴일")

private val WORK_TYPE_ALIAS_MAP: Map<String, String> = mapOf(
    "주" to "주간",
    "야" to "야간",
    "당" to "당직",
    "비" to "비번",
    "휴" to "휴무",
    "공휴일" to "휴일",
    "근무" to "당직",
    "석간" to "야간",
    "석" to "야간"
)

private val WORK_TYPE_MATCH_ORDER: List<String> =
    (STANDARD_WORK_TYPES + WORK_TYPE_ALIAS_MAP.keys)
        .distinct()
        .sortedByDescending { it.length }

fun normalizeWorkType(raw: String): String {
    val text = raw.trim()
    if (text.isBlank()) return ""
    val firstToken = text.substringBefore(" ").trim()
    val matched = WORK_TYPE_MATCH_ORDER.firstOrNull { firstToken.startsWith(it) } ?: firstToken
    return WORK_TYPE_ALIAS_MAP[matched] ?: matched
}

fun extractWorkTypeFromLabel(label: String): String = normalizeWorkType(label)

val QUICK_SHIFT_TEMPLATES: List<QuickShiftTemplate> = listOf(
    QuickShiftTemplate(label = "주간/야간", sequence = listOf("주간", "야간"), category = ShiftCategory.TWO_SHIFT),
    QuickShiftTemplate(label = "당직/비번", sequence = listOf("당직", "비번"), category = ShiftCategory.TWO_SHIFT),
    QuickShiftTemplate(label = "격일", sequence = listOf("당직", "휴무"), category = ShiftCategory.TWO_SHIFT),
    QuickShiftTemplate(label = "2교대 직접 구성", sequence = listOf("주간", "야간"), category = ShiftCategory.TWO_SHIFT),

    QuickShiftTemplate(label = "주간/당직/비번", sequence = listOf("주간", "당직", "비번"), category = ShiftCategory.THREE_SHIFT),
    QuickShiftTemplate(label = "주/야/비", sequence = listOf("주간", "야간", "비번"), category = ShiftCategory.THREE_SHIFT),
    QuickShiftTemplate(label = "주주야야비비", sequence = listOf("주간", "주간", "야간", "야간", "비번", "비번"), category = ShiftCategory.THREE_SHIFT),
    QuickShiftTemplate(label = "3교대 직접 구성", sequence = listOf("주간", "당직", "비번", "휴무"), category = ShiftCategory.THREE_SHIFT),

    QuickShiftTemplate(label = "직접 설정", sequence = listOf("주간", "당직", "비번"), category = ShiftCategory.CUSTOM)
)

fun templatesForCategory(category: ShiftCategory): List<QuickShiftTemplate> {
    return QUICK_SHIFT_TEMPLATES.filter { it.category == category }
}

fun inferPresetCategory(preset: RotationPreset): ShiftCategory {
    val badge = inferShiftPatternBadge(preset)
    return when {
        badge.contains("2교대") || badge.contains("당비당비") -> ShiftCategory.TWO_SHIFT
        badge.contains("3교대") || badge.contains("주당비") || badge.contains("주야비") -> ShiftCategory.THREE_SHIFT
        else -> ShiftCategory.CUSTOM
    }
}


fun defaultWorkTypeConfigs(types: List<String>): List<WorkTypeAlarmConfig> {
    return types.map(::normalizeWorkType).filter { it.isNotBlank() }.distinct().map { type ->
        val defaultEnabled = when {
            type.contains("휴가") -> true
            type.contains("비") || type.contains("휴") -> false
            else -> true
        }
        WorkTypeAlarmConfig(
            type = type,
            enabled = defaultEnabled,
            primaryTime = defaultPrimaryTime(type),
            secondaryTime = ""
        )
    }
}

fun defaultPrimaryTime(type: String): String {
    return when (normalizeWorkType(type)) {
        "주간" -> "06:30"
        "야간" -> "20:30"
        "당직" -> "08:30"
        "휴가" -> "09:30"
        "휴무", "휴일", "비번" -> "08:00"
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
    val seq = sequence.ifEmpty { listOf("주간") }
    val period = seq.size.coerceAtLeast(1)
    val start = todayIndex.coerceIn(0, period - 1)
    val intervalWeeks = (lcm(period, 7) / 7).coerceIn(1, 4)
    return BuiltWorkRotation(intervalWeeks = intervalWeeks, sequence = seq, todayIndex = start)
}

fun buildWeeklyPatternForType(
    rotation: BuiltWorkRotation,
    targetType: String,
    anchor: LocalDate = LocalDate.now()
): List<Set<DayOfWeek>> {
    val weeks = MutableList(rotation.intervalWeeks) { mutableSetOf<DayOfWeek>() }
    val period = rotation.sequence.size.coerceAtLeast(1)
    repeat(rotation.intervalWeeks * 7) { offset ->
        val step = rotation.sequence[(rotation.todayIndex + offset) % period]
        if (step == targetType) {
            val date = anchor.plusDays(offset.toLong())
            weeks[offset / 7].add(date.dayOfWeek)
        }
    }
    return weeks.map { it.toSet() }
}

fun buildWorkPreview(
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
        "${date.format(DateTimeFormatter.ofPattern("MM/dd"))}  $type"
    }
}

fun workTypeColor(type: String): Color {
    return when (normalizeWorkType(type)) {
        "주간" -> Color(0xFF1565C0)
        "당직", "야간" -> Color(0xFFC62828)
        "비번", "휴무", "휴가", "휴일" -> Color(0xFF616161)
        else -> Color(0xFF2E7D32)
    }
}

fun workTypeColorName(type: String): String {
    return when (normalizeWorkType(type)) {
        "주간" -> "파랑"
        "당직", "야간" -> "빨강"
        "비번", "휴무", "휴가", "휴일" -> "회색"
        else -> "초록"
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

fun dayOfWeekLabel(day: DayOfWeek): String {
    return when (day) {
        DayOfWeek.MONDAY -> "월"
        DayOfWeek.TUESDAY -> "화"
        DayOfWeek.WEDNESDAY -> "수"
        DayOfWeek.THURSDAY -> "목"
        DayOfWeek.FRIDAY -> "금"
        DayOfWeek.SATURDAY -> "토"
        DayOfWeek.SUNDAY -> "일"
    }
}

fun inferShiftPatternBadge(preset: RotationPreset): String {
    val cycle = buildPresetCycle(preset)
    if (cycle.isEmpty()) return "패턴 없음"

    val period = findRepeatingPeriod(cycle)
    val base = cycle.take(period)
    val activeCount = base.count { it }

    if (activeCount == 0) return "휴무형"
    if (activeCount == period) return "매일형"

    return when (period) {
        2 -> if (activeCount == 1) "당비당비" else "2일 순환형"
        3 -> if (activeCount == 1) "주당비/주야비" else if (activeCount == 2) "주야비" else "3일 순환형"
        4 -> when {
            patternKey(base) in setOf("1100", "0011", "0110", "1001") -> "주주야야(2교대)"
            activeCount == 3 -> "4조3교대"
            activeCount == 2 -> "주야(2교대)"
            else -> "4일 순환형"
        }
        7 -> inferWeeklyBadge(preset, base)
        else -> {
            if (activeCount * 2 == period) "${period}일 반반 순환형" else "${period}일 순환형"
        }
    }
}

private fun inferWeeklyBadge(preset: RotationPreset, base: List<Boolean>): String {
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
            return "주중형(월~금)"
        }
        return "주5일형"
    }

    if (activeCount == 2) {
        val firstWeek = preset.weekPatterns.firstOrNull().orEmpty()
        if (firstWeek == setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) {
            return "주말형"
        }
    }

    return "주${activeCount}회형"
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
    val weeks = preset.intervalWeeks.coerceIn(1, 4)
    val patterns = (0 until weeks).map { preset.weekPatterns.getOrNull(it).orEmpty() }
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

fun formatAlarmLogType(type: AlarmLogType): String {
    return when (type) {
        AlarmLogType.RING_START -> "울림 시작"
        AlarmLogType.STOP -> "끄기"
        AlarmLogType.SNOOZE_SCHEDULED -> "스누즈 예약"
        AlarmLogType.SNOOZE_BLOCKED -> "스누즈 차단"
        AlarmLogType.ONE_MORE_SCHEDULED -> "마지막 1회 예약"
        AlarmLogType.ONE_MORE_SKIPPED -> "마지막 1회 미예약"
        AlarmLogType.MANUAL_VACATION_SET -> "휴가 처리"
        AlarmLogType.MANUAL_VACATION_CLEAR -> "휴가 해제"
        AlarmLogType.MANUAL_SKIP_SET -> "스킵 처리"
        AlarmLogType.MANUAL_SKIP_CLEAR -> "스킵 해제"
        AlarmLogType.MANUAL_SHIFT_CHANGE -> "근무 변경"
        AlarmLogType.MANUAL_UNDO -> "실행 취소"
    }
}

fun formatTimeUntil(target: LocalDateTime, now: LocalDateTime = LocalDateTime.now()): String {
    val totalMinutes = java.time.Duration.between(now, target).toMinutes().coerceAtLeast(0)
    if (totalMinutes == 0L) return "곧"

    val days = totalMinutes / (24 * 60)
    val hours = (totalMinutes % (24 * 60)) / 60
    val minutes = totalMinutes % 60
    val parts = mutableListOf<String>()

    if (days > 0) parts += "${days}일"
    if (hours > 0) parts += "${hours}시간"
    if (minutes > 0) parts += "${minutes}분"

    return parts.joinToString(" ")
}

