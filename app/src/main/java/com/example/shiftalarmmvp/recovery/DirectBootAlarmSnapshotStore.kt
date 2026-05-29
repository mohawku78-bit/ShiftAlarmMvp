package com.example.shiftalarmmvp.recovery

import android.content.Context
import android.os.Build
import com.example.shiftalarmmvp.data.AlarmRule
import org.json.JSONArray

private const val DIRECT_BOOT_PREF_NAME = "direct_boot_alarm_snapshots"
private const val KEY_SNAPSHOTS = "snapshots"

internal class DirectBootAlarmSnapshotStore(context: Context) {
    private val storageContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        context.applicationContext.createDeviceProtectedStorageContext()
    } else {
        context.applicationContext
    }
    private val prefs = storageContext.getSharedPreferences(DIRECT_BOOT_PREF_NAME, Context.MODE_PRIVATE)

    fun replaceAll(enabledAlarms: List<AlarmRule>) {
        val snapshots = enabledAlarms
            .filter { it.enabled && it.id > 0L }
            .map(DirectBootAlarmSnapshot::fromRule)
        save(snapshots)
    }

    fun upsert(rule: AlarmRule) {
        if (!rule.enabled || rule.id <= 0L) {
            remove(rule.id)
            return
        }

        val next = loadAll()
            .filterNot { it.id == rule.id }
            .plus(DirectBootAlarmSnapshot.fromRule(rule))
            .sortedBy { it.id }
        save(next)
    }

    fun remove(alarmId: Long) {
        if (alarmId <= 0L) return
        val next = loadAll().filterNot { it.id == alarmId }
        save(next)
    }

    fun clear() {
        prefs.edit().remove(KEY_SNAPSHOTS).commit()
    }

    fun loadAll(): List<DirectBootAlarmSnapshot> {
        val raw = prefs.getString(KEY_SNAPSHOTS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val obj = array.optJSONObject(index) ?: continue
                    DirectBootAlarmSnapshot.fromJson(obj)?.let(::add)
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun save(snapshots: List<DirectBootAlarmSnapshot>) {
        val array = JSONArray()
        snapshots
            .distinctBy { it.id }
            .sortedBy { it.id }
            .forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_SNAPSHOTS, array.toString()).commit()
    }
}
