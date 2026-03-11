package com.example.shiftalarmmvp.ui

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

enum class AlarmLogType {
    RING_START,
    STOP,
    SNOOZE_SCHEDULED,
    SNOOZE_BLOCKED,
    ONE_MORE_SCHEDULED,
    ONE_MORE_SKIPPED,
    MANUAL_VACATION_SET,
    MANUAL_VACATION_CLEAR,
    MANUAL_SKIP_SET,
    MANUAL_SKIP_CLEAR,
    MANUAL_SHIFT_CHANGE,
    MANUAL_UNDO,
    MANUAL_RECOVERY_ACTION
}

data class AlarmLogEntry(
    val timestampMillis: Long,
    val alarmId: Long,
    val label: String,
    val type: AlarmLogType,
    val detail: String = ""
) {
    fun toLocalDateTime(zoneId: ZoneId = ZoneId.systemDefault()): LocalDateTime {
        return Instant.ofEpochMilli(timestampMillis).atZone(zoneId).toLocalDateTime()
    }
}

class AlarmLogStore(context: Context) {
    private val prefs = context.getSharedPreferences("alarm_log_store", Context.MODE_PRIVATE)
    private val key = "alarm_logs_json"

    fun append(
        alarmId: Long,
        label: String,
        type: AlarmLogType,
        detail: String = ""
    ) {
        val current = loadMutable()
        current.add(
            AlarmLogEntry(
                timestampMillis = System.currentTimeMillis(),
                alarmId = alarmId,
                label = label,
                type = type,
                detail = detail
            )
        )
        save(current.takeLast(MAX_ENTRIES))
    }

    fun recent(limit: Int = 50): List<AlarmLogEntry> {
        return loadMutable().takeLast(limit.coerceIn(1, MAX_ENTRIES)).asReversed()
    }

    fun allEntries(): List<AlarmLogEntry> {
        return loadMutable().takeLast(MAX_ENTRIES)
    }

    fun replaceAll(entries: List<AlarmLogEntry>) {
        save(entries.sortedBy { it.timestampMillis }.takeLast(MAX_ENTRIES))
    }

    fun clear() {
        prefs.edit().remove(key).apply()
    }

    private fun loadMutable(): MutableList<AlarmLogEntry> {
        val raw = prefs.getString(key, null) ?: return mutableListOf()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                repeat(array.length()) { index ->
                    val obj = array.optJSONObject(index) ?: return@repeat
                    val type = runCatching { AlarmLogType.valueOf(obj.optString("type")) }.getOrNull() ?: return@repeat
                    add(
                        AlarmLogEntry(
                            timestampMillis = obj.optLong("timestampMillis", 0L),
                            alarmId = obj.optLong("alarmId", -1L),
                            label = obj.optString("label", ""),
                            type = type,
                            detail = obj.optString("detail", "")
                        )
                    )
                }
            }.toMutableList()
        }.getOrDefault(mutableListOf())
    }

    private fun save(entries: List<AlarmLogEntry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject()
                    .put("timestampMillis", entry.timestampMillis)
                    .put("alarmId", entry.alarmId)
                    .put("label", entry.label)
                    .put("type", entry.type.name)
                    .put("detail", entry.detail)
            )
        }
        prefs.edit().putString(key, array.toString()).apply()
    }

    companion object {
        private const val MAX_ENTRIES = 200
    }
}