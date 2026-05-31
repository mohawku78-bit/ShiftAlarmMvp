package com.example.shiftalarmmvp.wear

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal data class WatchAlarmScreenLayoutSpec(
    val contentWidthPx: Int,
    val outerHorizontalPaddingPx: Int,
    val outerVerticalPaddingPx: Int,
    val bottomBreathingRoomPx: Int,
    val titleTextSp: Float,
    val timeTextSp: Float,
    val labelTextSp: Float,
    val hintTextSp: Float,
    val statusTextSp: Float,
    val buttonTextSp: Float,
    val buttonHeightPx: Int,
    val timeTopMarginPx: Int,
    val labelTopMarginPx: Int,
    val hintTopMarginPx: Int,
    val statusTopMarginPx: Int,
    val stopTopMarginPx: Int,
    val buttonGapPx: Int,
    val maxLabelLines: Int
)

internal object WatchAlarmScreenLayout {
    fun forScreen(
        isRound: Boolean,
        screenWidthPx: Int,
        screenHeightPx: Int,
        density: Float
    ): WatchAlarmScreenLayoutSpec {
        val safeDensity = density.takeIf { it > 0f } ?: 1f
        fun dp(value: Int): Int = (value * safeDensity).roundToInt()

        val fallbackSizePx = dp(320)
        val widthPx = screenWidthPx.takeIf { it > 0 } ?: fallbackSizePx
        val heightPx = screenHeightPx.takeIf { it > 0 } ?: fallbackSizePx
        val minSizePx = min(widthPx, heightPx)
        val minSizeDp = minSizePx / safeDensity
        val compact = minSizeDp <= COMPACT_SCREEN_DP

        val horizontalPaddingPx = when {
            isRound && compact -> dp(ROUND_COMPACT_HORIZONTAL_PADDING_DP)
            isRound -> dp(ROUND_HORIZONTAL_PADDING_DP)
            compact -> dp(COMPACT_HORIZONTAL_PADDING_DP)
            else -> dp(SQUARE_HORIZONTAL_PADDING_DP)
        }
        val verticalPaddingPx = when {
            isRound && compact -> dp(ROUND_COMPACT_VERTICAL_PADDING_DP)
            isRound -> dp(ROUND_VERTICAL_PADDING_DP)
            compact -> dp(COMPACT_VERTICAL_PADDING_DP)
            else -> dp(SQUARE_VERTICAL_PADDING_DP)
        }

        val availableWidthPx = max(1, widthPx - horizontalPaddingPx * 2)
        val roundSafeWidthPx = (minSizePx * if (compact) ROUND_COMPACT_SAFE_WIDTH_RATIO else ROUND_SAFE_WIDTH_RATIO)
            .roundToInt()
        val desiredWidthPx = if (isRound) min(availableWidthPx, roundSafeWidthPx) else availableWidthPx
        val minimumWidthPx = min(dp(MIN_CONTENT_WIDTH_DP), availableWidthPx)
        val contentWidthPx = desiredWidthPx.coerceIn(minimumWidthPx, availableWidthPx)

        return WatchAlarmScreenLayoutSpec(
            contentWidthPx = contentWidthPx,
            outerHorizontalPaddingPx = horizontalPaddingPx,
            outerVerticalPaddingPx = verticalPaddingPx,
            bottomBreathingRoomPx = if (isRound) dp(ROUND_BOTTOM_BREATHING_ROOM_DP) else dp(SQUARE_BOTTOM_BREATHING_ROOM_DP),
            titleTextSp = if (compact) 12f else 14f,
            timeTextSp = if (compact) 36f else 40f,
            labelTextSp = if (compact) 14f else 15f,
            hintTextSp = if (compact) 11f else 12f,
            statusTextSp = if (compact) 11f else 12f,
            buttonTextSp = if (compact) 14f else 15f,
            buttonHeightPx = dp(if (compact) COMPACT_BUTTON_HEIGHT_DP else BUTTON_HEIGHT_DP),
            timeTopMarginPx = dp(if (compact) 8 else 10),
            labelTopMarginPx = dp(if (compact) 7 else 8),
            hintTopMarginPx = dp(6),
            statusTopMarginPx = dp(6),
            stopTopMarginPx = dp(if (compact) 12 else 14),
            buttonGapPx = dp(6),
            maxLabelLines = if (compact) 1 else 2
        )
    }

    private const val COMPACT_SCREEN_DP = 360
    private const val MIN_CONTENT_WIDTH_DP = 168
    private const val ROUND_SAFE_WIDTH_RATIO = 0.70f
    private const val ROUND_COMPACT_SAFE_WIDTH_RATIO = 0.66f
    private const val ROUND_HORIZONTAL_PADDING_DP = 44
    private const val ROUND_COMPACT_HORIZONTAL_PADDING_DP = 38
    private const val SQUARE_HORIZONTAL_PADDING_DP = 18
    private const val COMPACT_HORIZONTAL_PADDING_DP = 22
    private const val ROUND_VERTICAL_PADDING_DP = 30
    private const val ROUND_COMPACT_VERTICAL_PADDING_DP = 24
    private const val SQUARE_VERTICAL_PADDING_DP = 16
    private const val COMPACT_VERTICAL_PADDING_DP = 14
    private const val ROUND_BOTTOM_BREATHING_ROOM_DP = 16
    private const val SQUARE_BOTTOM_BREATHING_ROOM_DP = 4
    private const val BUTTON_HEIGHT_DP = 42
    private const val COMPACT_BUTTON_HEIGHT_DP = 40
}
