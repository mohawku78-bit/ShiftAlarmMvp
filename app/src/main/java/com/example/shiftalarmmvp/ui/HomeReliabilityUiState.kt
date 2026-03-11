package com.example.shiftalarmmvp.ui

import android.content.res.Resources
import com.example.shiftalarmmvp.R
import com.example.shiftalarmmvp.recovery.HomeReliabilityLevel
import com.example.shiftalarmmvp.recovery.HomeReliabilityUiModel

internal enum class HomeReliabilityChipTone {
    SAFE,
    CHECK,
    ACTION
}

internal data class HomeReliabilityBannerState(
    val showPanel: Boolean,
    val panelLockedOpen: Boolean,
    val toggleLabel: String,
    val chipLabel: String,
    val chipTone: HomeReliabilityChipTone
)

internal fun buildHomeReliabilityBannerState(
    resources: Resources,
    reliabilityUi: HomeReliabilityUiModel,
    panelExpanded: Boolean
): HomeReliabilityBannerState {
    val panelLockedOpen = reliabilityUi.level != HomeReliabilityLevel.SAFE
    val showPanel = panelExpanded || panelLockedOpen
    val toggleLabel = when {
        !showPanel -> resources.getString(R.string.home_reliability_toggle_show)
        panelLockedOpen -> resources.getString(R.string.home_reliability_toggle_check)
        else -> resources.getString(R.string.home_reliability_toggle_hide)
    }
    val chipTone = when (reliabilityUi.level) {
        HomeReliabilityLevel.SAFE -> HomeReliabilityChipTone.SAFE
        HomeReliabilityLevel.CHECK -> HomeReliabilityChipTone.CHECK
        HomeReliabilityLevel.ACTION -> HomeReliabilityChipTone.ACTION
    }

    return HomeReliabilityBannerState(
        showPanel = showPanel,
        panelLockedOpen = panelLockedOpen,
        toggleLabel = toggleLabel,
        chipLabel = reliabilityUi.statusLabel,
        chipTone = chipTone
    )
}