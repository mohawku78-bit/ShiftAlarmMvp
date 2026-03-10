package com.example.shiftalarmmvp.ui

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
    reliabilityUi: HomeReliabilityUiModel,
    panelExpanded: Boolean
): HomeReliabilityBannerState {
    val panelLockedOpen = reliabilityUi.level != HomeReliabilityLevel.SAFE
    val showPanel = panelExpanded || panelLockedOpen
    val toggleLabel = when {
        !showPanel -> "점검 보기"
        panelLockedOpen -> "점검 확인"
        else -> "접기"
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
