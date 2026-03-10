package com.example.shiftalarmmvp.ui

import java.time.DayOfWeek
import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject

data class RotationPreset(
    val name: String,
    val intervalWeeks: Int,
    val anchorDate: LocalDate,
    val weekPatterns: List<Set<DayOfWeek>>,
    val infiniteRotationEnabled: Boolean = true,
    val isDefault: Boolean = false
)

class RotationPresetStore(context: android.content.Context) {
    private val prefs = context.getSharedPreferences("rotation_preset_store", android.content.Context.MODE_PRIVATE)
    private val key = "rotation_presets_json"

    fun load(): List<RotationPreset> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return parseFromJson(raw)
    }

    fun upsert(preset: RotationPreset) {
        val current = load().toMutableList()
        val index = current.indexOfFirst { it.name.equals(preset.name, ignoreCase = true) }
        if (index >= 0) {
            val preservedDefault = current[index].isDefault
            current[index] = normalizePreset(preset).copy(isDefault = preservedDefault || preset.isDefault)
        } else {
            current.add(normalizePreset(preset))
        }
        save(current)
    }

    fun deleteByName(name: String) {
        val target = name.trim()
        if (target.isBlank()) return
        save(load().filterNot { it.name.equals(target, ignoreCase = true) })
    }

    fun setDefault(name: String) {
        val target = name.trim()
        if (target.isBlank()) return
        val updated = load().map {
            it.copy(isDefault = it.name.equals(target, ignoreCase = true))
        }
        save(updated)
    }

    fun setInfinite(name: String, enabled: Boolean) {
        val target = name.trim()
        if (target.isBlank()) return
        val updated = load().map {
            if (it.name.equals(target, ignoreCase = true)) it.copy(infiniteRotationEnabled = enabled) else it
        }
        save(updated)
    }

    fun moveUp(name: String) {
        val current = load().toMutableList()
        val index = current.indexOfFirst { it.name.equals(name, ignoreCase = true) }
        if (index > 0) {
            val tmp = current[index - 1]
            current[index - 1] = current[index]
            current[index] = tmp
            save(current)
        }
    }

    fun moveDown(name: String) {
        val current = load().toMutableList()
        val index = current.indexOfFirst { it.name.equals(name, ignoreCase = true) }
        if (index >= 0 && index < current.lastIndex) {
            val tmp = current[index + 1]
            current[index + 1] = current[index]
            current[index] = tmp
            save(current)
        }
    }

    fun exportJson(): String {
        return serializeToJson(load())
    }

    fun importJson(raw: String, merge: Boolean): Int {
        val imported = parseFromJson(raw)
        if (!merge) {
            save(imported)
            return imported.size
        }

        val merged = load().toMutableList()
        val hasImportedDefault = imported.any { it.isDefault }
        if (hasImportedDefault) {
            for (index in merged.indices) {
                merged[index] = merged[index].copy(isDefault = false)
            }
        }

        imported.forEach { incoming ->
            val found = merged.indexOfFirst { it.name.equals(incoming.name, ignoreCase = true) }
            if (found >= 0) {
                merged[found] = incoming
            } else {
                merged.add(incoming)
            }
        }

        save(merged)
        return imported.size
    }

    private fun parseFromJson(raw: String): List<RotationPreset> {
        return runCatching {
            val array = JSONArray(raw)
            val parsed = buildList {
                repeat(array.length()) { index ->
                    val obj = array.optJSONObject(index) ?: return@repeat
                    val name = obj.optString("name", "").trim()
                    if (name.isBlank()) return@repeat
                    val interval = obj.optInt("intervalWeeks", 2).coerceIn(2, 6)
                    val anchor = LocalDate.ofEpochDay(obj.optLong("anchorEpochDay", LocalDate.now().toEpochDay()))
                    val patterns = parsePatterns(obj.optJSONArray("patterns"), interval)
                    val isDefault = obj.optBoolean("isDefault", false)
                    val infiniteRotationEnabled = obj.optBoolean("infiniteRotationEnabled", true)
                    add(
                        RotationPreset(
                            name = name,
                            intervalWeeks = interval,
                            anchorDate = anchor,
                            weekPatterns = patterns,
                            infiniteRotationEnabled = infiniteRotationEnabled,
                            isDefault = isDefault
                        )
                    )
                }
            }
            normalizeList(parsed)
        }.getOrDefault(emptyList())
    }

    private fun save(items: List<RotationPreset>) {
        prefs.edit().putString(key, serializeToJson(items)).apply()
    }

    private fun serializeToJson(items: List<RotationPreset>): String {
        val normalized = normalizeList(items)
        val array = JSONArray()
        normalized.forEach { preset ->
            val obj = JSONObject()
                .put("name", preset.name)
                .put("intervalWeeks", preset.intervalWeeks.coerceIn(2, 6))
                .put("anchorEpochDay", preset.anchorDate.toEpochDay())
                .put("infiniteRotationEnabled", preset.infiniteRotationEnabled)
                .put("isDefault", preset.isDefault)
            val patterns = JSONArray()
            (0 until preset.intervalWeeks.coerceIn(2, 6)).forEach { index ->
                val dayArray = JSONArray()
                preset.weekPatterns.getOrNull(index)
                    .orEmpty()
                    .sortedBy { it.value }
                    .forEach { day -> dayArray.put(day.name) }
                patterns.put(dayArray)
            }
            obj.put("patterns", patterns)
            array.put(obj)
        }
        return array.toString()
    }

    private fun normalizeList(items: List<RotationPreset>): List<RotationPreset> {
        val ordered = mutableListOf<RotationPreset>()
        val indexByName = mutableMapOf<String, Int>()

        items.forEach { preset ->
            val normalized = normalizePreset(preset)
            val key = normalized.name.lowercase()
            val existingIndex = indexByName[key]
            if (existingIndex == null) {
                indexByName[key] = ordered.size
                ordered.add(normalized)
            } else {
                ordered[existingIndex] = normalized
            }
        }

        var defaultSeen = false
        return ordered.map { preset ->
            if (preset.isDefault && !defaultSeen) {
                defaultSeen = true
                preset
            } else {
                preset.copy(isDefault = false)
            }
        }
    }

    private fun normalizePreset(preset: RotationPreset): RotationPreset {
        val interval = preset.intervalWeeks.coerceIn(2, 6)
        return preset.copy(
            name = preset.name.trim(),
            intervalWeeks = interval,
            weekPatterns = (0 until interval).map { preset.weekPatterns.getOrNull(it).orEmpty() }
        )
    }

    private fun parsePatterns(patterns: JSONArray?, interval: Int): List<Set<DayOfWeek>> {
        return (0 until interval).map { index ->
            val rawDays = patterns?.optJSONArray(index) ?: return@map emptySet()
            buildSet {
                repeat(rawDays.length()) { dIdx ->
                    val day = runCatching { DayOfWeek.valueOf(rawDays.optString(dIdx)) }.getOrNull()
                    if (day != null) add(day)
                }
            }
        }
    }
}
