package com.example.shiftalarmmvp.ui

import android.content.Context
import com.example.shiftalarmmvp.R
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.InputStreamReader
import java.util.Locale

private const val BATTERY_GUIDE_ASSET_NAME = "android-battery-guides.json"

data class BatteryGuideCatalog(
    val meta: BatteryGuideMeta = BatteryGuideMeta(),
    val fallback: BatteryGuideEntry? = null,
    val manufacturers: Map<String, BatteryGuideEntry> = emptyMap()
)

data class BatteryGuideMeta(
    val title: String = "",
    val status: String = "",
    val source: List<String> = emptyList(),
    @SerializedName("last_verified_at") val lastVerifiedAt: String = "",
    @SerializedName("created_from") val createdFrom: String = "",
    val note: String = ""
)

data class BatteryGuideEntry(
    val title: String = "",
    @SerializedName("manufacturer_description") val manufacturerDescription: String = "",
    @SerializedName("user_message") val userMessage: String = "",
    @SerializedName("short_description") val shortDescription: String = "",
    val source: List<String> = emptyList(),
    @SerializedName("last_verified_at") val lastVerifiedAt: String = "",
    val steps: List<String> = emptyList()
)

fun Context.loadBatteryGuideCatalog(): BatteryGuideCatalog? {
    return runCatching {
        assets.open(BATTERY_GUIDE_ASSET_NAME).use { inputStream ->
            InputStreamReader(inputStream, Charsets.UTF_8).use { reader ->
                Gson().fromJson(reader, BatteryGuideCatalog::class.java)
            }
        }
    }.getOrNull()?.takeIf { it.manufacturers.isNotEmpty() || it.fallback != null }
}

fun Context.buildFallbackBatteryGuide(): BatteryGuideEntry {
    return BatteryGuideEntry(
        title = getString(R.string.main_battery_guide_generic_title),
        manufacturerDescription = getString(R.string.main_battery_guide_generic_description),
        userMessage = getString(R.string.main_battery_guide_generic_user_message),
        shortDescription = getString(R.string.main_battery_guide_generic_short_description),
        steps = listOf(
            getString(R.string.main_battery_guide_generic_step_app_battery),
            getString(R.string.main_battery_guide_generic_step_disable_optimization),
            getString(R.string.main_battery_guide_generic_step_allow_background),
            getString(R.string.main_battery_guide_generic_step_exclude_sleep)
        )
    )
}

fun BatteryGuideCatalog.findGuideForManufacturer(
    manufacturer: String,
    brand: String = ""
): BatteryGuideEntry? {
    val key = batteryGuideManufacturerKey(manufacturer, brand)
    return key?.let { manufacturers[it] } ?: fallback
}

fun batteryGuideManufacturerKey(
    manufacturer: String,
    brand: String = ""
): String? {
    return batteryGuideManufacturerCandidates(manufacturer, brand).firstOrNull()
}

private fun batteryGuideManufacturerCandidates(
    manufacturer: String,
    brand: String = ""
): List<String> {
    val tokens = listOf(manufacturer, brand)
        .map { it.trim().lowercase(Locale.ROOT) }
        .filter { it.isNotBlank() }

    if (tokens.isEmpty()) {
        return emptyList()
    }

    return buildList {
        if (tokens.any { it.contains("samsung") }) add("samsung")
        if (tokens.any { it.contains("xiaomi") || it.contains("redmi") || it.contains("poco") }) add("xiaomi")
        if (tokens.any { it.contains("huawei") || it.contains("honor") }) add("huawei")
        if (tokens.any { it.contains("oppo") }) add("oppo")
        if (tokens.any { it.contains("oneplus") }) add("oneplus")
        if (tokens.any { it.contains("vivo") || it.contains("iqoo") }) add("vivo")
        if (tokens.any { it.contains("realme") }) add("realme")
        if (tokens.any { it.contains("google") || it.contains("pixel") }) add("google_pixel")
    }.distinct()
}