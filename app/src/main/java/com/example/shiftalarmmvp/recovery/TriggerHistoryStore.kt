package com.example.shiftalarmmvp.recovery

import android.content.Context
import com.example.shiftalarmmvp.scheduler.AlarmScheduleMode
import org.json.JSONArray
import org.json.JSONObject

private const val TRIGGER_HISTORY_PREF_NAME = "trigger_history_store"
private const val KEY_PENDING_REGISTRATIONS = "pending_registrations_json"
private const val KEY_RECENT_EVENTS = "recent_events_json"
private const val MAX_PENDING_REGISTRATIONS = 64
private const val MAX_TRIGGER_EVENTS = 120

internal data class TrackedAlarmRegistration(
    val alarmId: Long,
    val label: String,
    val expectedTriggerMillis: Long,
    val scheduledAtMillis: Long,
    val scheduleMode: AlarmScheduleMode
)

internal enum class TriggerHistoryEventType {
    SCHEDULED,
    TRIGGERED,
    MISSED,
    SELF_HEALED,
    CLEARED
}

internal data class TriggerHistoryEvent(
    val eventType: TriggerHistoryEventType,
    val eventAtMillis: Long,
    val alarmId: Long,
    val label: String,
    val expectedTriggerMillis: Long,
    val scheduleMode: AlarmScheduleMode?,
    val detail: String = ""
)

internal fun upsertTrackedRegistration(
    current: List<TrackedAlarmRegistration>,
    registration: TrackedAlarmRegistration
): List<TrackedAlarmRegistration> {
    return (current.filterNot { it.alarmId == registration.alarmId } + registration)
        .sortedBy { it.expectedTriggerMillis }
        .takeLast(MAX_PENDING_REGISTRATIONS)
}

internal fun removeTrackedRegistration(
    current: List<TrackedAlarmRegistration>,
    alarmId: Long,
    expectedTriggerMillis: Long? = null
): List<TrackedAlarmRegistration> {
    return current.filterNot { registration ->
        registration.alarmId == alarmId &&
            (expectedTriggerMillis == null || registration.expectedTriggerMillis == expectedTriggerMillis)
    }
}

internal fun closeTrackedRegistrationAsTriggered(
    current: List<TrackedAlarmRegistration>,
    alarmId: Long,
    expectedTriggerMillis: Long?
): Pair<List<TrackedAlarmRegistration>, TrackedAlarmRegistration?> {
    val match = current
        .filter { it.alarmId == alarmId }
        .let { registrations ->
            when {
                expectedTriggerMillis != null && expectedTriggerMillis > 0L -> {
                    registrations.firstOrNull { it.expectedTriggerMillis == expectedTriggerMillis }
                        ?: registrations.minByOrNull { kotlin.math.abs(it.expectedTriggerMillis - expectedTriggerMillis) }
                }
                else -> registrations.minByOrNull { it.expectedTriggerMillis }
            }
        }
    if (match == null) return current to null
    return removeTrackedRegistration(current, alarmId, match.expectedTriggerMillis) to match
}

internal fun hasTriggeredEventForRegistration(
    events: List<TriggerHistoryEvent>,
    registration: TrackedAlarmRegistration
): Boolean {
    return events.any { event ->
        event.eventType == TriggerHistoryEventType.TRIGGERED &&
            event.alarmId == registration.alarmId &&
            event.expectedTriggerMillis == registration.expectedTriggerMillis
    }
}

internal fun parseAlarmScheduleMode(raw: String?): AlarmScheduleMode? {
    return raw
        ?.takeIf { it.isNotBlank() }
        ?.let { value -> runCatching { AlarmScheduleMode.valueOf(value) }.getOrNull() }
}

internal class TriggerHistoryStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(TRIGGER_HISTORY_PREF_NAME, Context.MODE_PRIVATE)

    fun loadPendingRegistrations(): List<TrackedAlarmRegistration> {
        val raw = prefs.getString(KEY_PENDING_REGISTRATIONS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                repeat(array.length()) { index ->
                    val obj = array.optJSONObject(index) ?: return@repeat
                    val scheduleMode = parseAlarmScheduleMode(obj.optString("scheduleMode")) ?: return@repeat
                    val alarmId = obj.optLong("alarmId", -1L)
                    val expectedTriggerMillis = obj.optLong("expectedTriggerMillis", 0L)
                    if (alarmId <= 0L || expectedTriggerMillis <= 0L) return@repeat
                    add(
                        TrackedAlarmRegistration(
                            alarmId = alarmId,
                            label = obj.optString("label", ""),
                            expectedTriggerMillis = expectedTriggerMillis,
                            scheduledAtMillis = obj.optLong("scheduledAtMillis", 0L),
                            scheduleMode = scheduleMode
                        )
                    )
                }
            }.sortedBy { it.expectedTriggerMillis }
        }.getOrDefault(emptyList())
    }

    fun loadRecentEvents(): List<TriggerHistoryEvent> {
        val raw = prefs.getString(KEY_RECENT_EVENTS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                repeat(array.length()) { index ->
                    val obj = array.optJSONObject(index) ?: return@repeat
                    val eventType = runCatching {
                        TriggerHistoryEventType.valueOf(obj.optString("eventType"))
                    }.getOrNull() ?: return@repeat
                    val alarmId = obj.optLong("alarmId", -1L)
                    if (alarmId == -1L) return@repeat
                    add(
                        TriggerHistoryEvent(
                            eventType = eventType,
                            eventAtMillis = obj.optLong("eventAtMillis", 0L),
                            alarmId = alarmId,
                            label = obj.optString("label", ""),
                            expectedTriggerMillis = obj.optLong("expectedTriggerMillis", 0L),
                            scheduleMode = parseAlarmScheduleMode(obj.optString("scheduleMode")),
                            detail = obj.optString("detail", "")
                        )
                    )
                }
            }.sortedBy { it.eventAtMillis }.takeLast(MAX_TRIGGER_EVENTS)
        }.getOrDefault(emptyList())
    }

    fun recordSchedule(
        registration: TrackedAlarmRegistration,
        eventAtMillis: Long = System.currentTimeMillis()
    ) {
        savePendingRegistrations(upsertTrackedRegistration(loadPendingRegistrations(), registration))
        appendEvent(
            TriggerHistoryEvent(
                eventType = TriggerHistoryEventType.SCHEDULED,
                eventAtMillis = eventAtMillis,
                alarmId = registration.alarmId,
                label = registration.label,
                expectedTriggerMillis = registration.expectedTriggerMillis,
                scheduleMode = registration.scheduleMode
            )
        )
    }

    fun clearAlarm(alarmId: Long, eventAtMillis: Long = System.currentTimeMillis()) {
        val current = loadPendingRegistrations()
        val removed = current.filter { it.alarmId == alarmId }
        if (removed.isEmpty()) return
        savePendingRegistrations(removeTrackedRegistration(current, alarmId))
        removed.forEach { registration ->
            appendEvent(
                TriggerHistoryEvent(
                    eventType = TriggerHistoryEventType.CLEARED,
                    eventAtMillis = eventAtMillis,
                    alarmId = registration.alarmId,
                    label = registration.label,
                    expectedTriggerMillis = registration.expectedTriggerMillis,
                    scheduleMode = registration.scheduleMode
                )
            )
        }
    }

    fun removePendingRegistration(
        alarmId: Long,
        expectedTriggerMillis: Long? = null,
        eventAtMillis: Long = System.currentTimeMillis(),
        appendClearedEvent: Boolean = false
    ) {
        val current = loadPendingRegistrations()
        val removed = current.filter { registration ->
            registration.alarmId == alarmId &&
                (expectedTriggerMillis == null || registration.expectedTriggerMillis == expectedTriggerMillis)
        }
        if (removed.isEmpty()) return
        savePendingRegistrations(removeTrackedRegistration(current, alarmId, expectedTriggerMillis))
        if (appendClearedEvent) {
            removed.forEach { registration ->
                appendEvent(
                    TriggerHistoryEvent(
                        eventType = TriggerHistoryEventType.CLEARED,
                        eventAtMillis = eventAtMillis,
                        alarmId = registration.alarmId,
                        label = registration.label,
                        expectedTriggerMillis = registration.expectedTriggerMillis,
                        scheduleMode = registration.scheduleMode
                    )
                )
            }
        }
    }

    fun recordTriggered(
        alarmId: Long,
        label: String,
        expectedTriggerMillis: Long?,
        eventAtMillis: Long = System.currentTimeMillis()
    ): TrackedAlarmRegistration? {
        val (remaining, matched) = closeTrackedRegistrationAsTriggered(
            current = loadPendingRegistrations(),
            alarmId = alarmId,
            expectedTriggerMillis = expectedTriggerMillis
        )
        savePendingRegistrations(remaining)
        val resolved = matched ?: TrackedAlarmRegistration(
            alarmId = alarmId,
            label = label,
            expectedTriggerMillis = expectedTriggerMillis ?: 0L,
            scheduledAtMillis = 0L,
            scheduleMode = AlarmScheduleMode.EXACT
        )
        appendEvent(
            TriggerHistoryEvent(
                eventType = TriggerHistoryEventType.TRIGGERED,
                eventAtMillis = eventAtMillis,
                alarmId = resolved.alarmId,
                label = resolved.label.ifBlank { label },
                expectedTriggerMillis = resolved.expectedTriggerMillis,
                scheduleMode = matched?.scheduleMode,
                detail = if (matched == null) "unmatched_trigger" else ""
            )
        )
        return matched
    }

    fun recordMissed(
        registration: TrackedAlarmRegistration,
        detail: String,
        eventAtMillis: Long = System.currentTimeMillis()
    ) {
        appendEvent(
            TriggerHistoryEvent(
                eventType = TriggerHistoryEventType.MISSED,
                eventAtMillis = eventAtMillis,
                alarmId = registration.alarmId,
                label = registration.label,
                expectedTriggerMillis = registration.expectedTriggerMillis,
                scheduleMode = registration.scheduleMode,
                detail = detail
            )
        )
    }

    fun recordSelfHealed(
        registration: TrackedAlarmRegistration,
        detail: String,
        eventAtMillis: Long = System.currentTimeMillis()
    ) {
        appendEvent(
            TriggerHistoryEvent(
                eventType = TriggerHistoryEventType.SELF_HEALED,
                eventAtMillis = eventAtMillis,
                alarmId = registration.alarmId,
                label = registration.label,
                expectedTriggerMillis = registration.expectedTriggerMillis,
                scheduleMode = registration.scheduleMode,
                detail = detail
            )
        )
    }

    private fun savePendingRegistrations(registrations: List<TrackedAlarmRegistration>) {
        val array = JSONArray()
        registrations.takeLast(MAX_PENDING_REGISTRATIONS).forEach { registration ->
            array.put(
                JSONObject()
                    .put("alarmId", registration.alarmId)
                    .put("label", registration.label)
                    .put("expectedTriggerMillis", registration.expectedTriggerMillis)
                    .put("scheduledAtMillis", registration.scheduledAtMillis)
                    .put("scheduleMode", registration.scheduleMode.name)
            )
        }
        prefs.edit().putString(KEY_PENDING_REGISTRATIONS, array.toString()).apply()
    }

    private fun appendEvent(event: TriggerHistoryEvent) {
        val current = loadRecentEvents().toMutableList()
        current += event
        val array = JSONArray()
        current.sortedBy { it.eventAtMillis }.takeLast(MAX_TRIGGER_EVENTS).forEach { entry ->
            array.put(
                JSONObject()
                    .put("eventType", entry.eventType.name)
                    .put("eventAtMillis", entry.eventAtMillis)
                    .put("alarmId", entry.alarmId)
                    .put("label", entry.label)
                    .put("expectedTriggerMillis", entry.expectedTriggerMillis)
                    .put("scheduleMode", entry.scheduleMode?.name)
                    .put("detail", entry.detail)
            )
        }
        prefs.edit().putString(KEY_RECENT_EVENTS, array.toString()).apply()
    }
}
