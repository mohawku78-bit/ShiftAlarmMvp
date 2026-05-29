package com.example.shiftalarmmvp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun ShiftTypeIllustrationForType(
    type: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    ShiftTypeIllustration(
        badge = shiftTypeToBadge(type),
        modifier = modifier,
        selected = selected
    )
}

@Composable
internal fun ShiftTypeIllustration(
    badge: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    val backgroundColor = shiftBadgeBackgroundColor(badge)
    val accentColor = shiftBadgeColor(badge)
    val shape = RoundedCornerShape(22.dp)

    Box(
        modifier = modifier
            .background(
                color = backgroundColor.copy(alpha = if (selected) 1f else 0.88f),
                shape = shape
            )
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = if (selected) 0.34f else 0.16f),
                shape = shape
            )
            .padding(7.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawBackgroundBubble(selected)
            when (badge) {
                SHIFT_BADGE_DAY -> drawSun(accentColor, Offset(size.width * 0.52f, size.height * 0.50f), size.minDimension * 0.23f)
                SHIFT_BADGE_NIGHT -> drawMoon(accentColor, backgroundColor, Offset(size.width * 0.52f, size.height * 0.50f), size.minDimension * 0.28f)
                SHIFT_BADGE_DUTY -> drawDutyBuilding(accentColor, Offset(size.width * 0.50f, size.height * 0.52f), size.minDimension * 0.72f)
                SHIFT_BADGE_OFF, SHIFT_BADGE_REST -> drawRestHouse(accentColor, Offset(size.width * 0.50f, size.height * 0.54f), size.minDimension * 0.72f)
                SHIFT_BADGE_DAY_NIGHT -> {
                    drawSun(ShiftDesign.Day, Offset(size.width * 0.34f, size.height * 0.43f), size.minDimension * 0.15f)
                    drawMoon(ShiftDesign.Night, backgroundColor, Offset(size.width * 0.66f, size.height * 0.58f), size.minDimension * 0.19f)
                }
                SHIFT_BADGE_DAY_DUTY -> {
                    drawSun(ShiftDesign.Day, Offset(size.width * 0.34f, size.height * 0.38f), size.minDimension * 0.14f)
                    drawDutyBuilding(ShiftDesign.Duty, Offset(size.width * 0.63f, size.height * 0.58f), size.minDimension * 0.50f)
                }
                SHIFT_BADGE_NIGHT_DUTY -> {
                    drawMoon(ShiftDesign.Night, backgroundColor, Offset(size.width * 0.34f, size.height * 0.40f), size.minDimension * 0.18f)
                    drawDutyBuilding(ShiftDesign.Duty, Offset(size.width * 0.63f, size.height * 0.60f), size.minDimension * 0.50f)
                }
                else -> drawWorkClipboard(accentColor, Offset(size.width * 0.50f, size.height * 0.52f), size.minDimension * 0.70f)
            }
        }
    }
}

@Composable
internal fun ShiftTypeWatermark(
    badge: String,
    modifier: Modifier = Modifier,
    alpha: Float = 0.22f
) {
    val backgroundColor = shiftBadgeBackgroundColor(badge)
    val accentColor = shiftBadgeColor(badge)

    Canvas(modifier = modifier.alpha(alpha)) {
        when (badge) {
            SHIFT_BADGE_DAY -> drawSun(accentColor, Offset(size.width * 0.54f, size.height * 0.52f), size.minDimension * 0.30f)
            SHIFT_BADGE_NIGHT -> drawMoon(accentColor, backgroundColor.copy(alpha = 0.54f), Offset(size.width * 0.56f, size.height * 0.50f), size.minDimension * 0.34f)
            SHIFT_BADGE_DUTY -> drawDutyBuilding(accentColor, Offset(size.width * 0.52f, size.height * 0.55f), size.minDimension * 0.96f)
            SHIFT_BADGE_OFF, SHIFT_BADGE_REST -> drawRestHouse(accentColor, Offset(size.width * 0.52f, size.height * 0.58f), size.minDimension * 0.96f)
            SHIFT_BADGE_DAY_NIGHT -> {
                drawSun(ShiftDesign.Day, Offset(size.width * 0.36f, size.height * 0.42f), size.minDimension * 0.22f)
                drawMoon(ShiftDesign.Night, backgroundColor.copy(alpha = 0.54f), Offset(size.width * 0.66f, size.height * 0.62f), size.minDimension * 0.25f)
            }
            SHIFT_BADGE_DAY_DUTY -> {
                drawSun(ShiftDesign.Day, Offset(size.width * 0.34f, size.height * 0.38f), size.minDimension * 0.20f)
                drawDutyBuilding(ShiftDesign.Duty, Offset(size.width * 0.64f, size.height * 0.60f), size.minDimension * 0.70f)
            }
            SHIFT_BADGE_NIGHT_DUTY -> {
                drawMoon(ShiftDesign.Night, backgroundColor.copy(alpha = 0.54f), Offset(size.width * 0.34f, size.height * 0.40f), size.minDimension * 0.24f)
                drawDutyBuilding(ShiftDesign.Duty, Offset(size.width * 0.64f, size.height * 0.62f), size.minDimension * 0.70f)
            }
            else -> drawWorkClipboard(accentColor, Offset(size.width * 0.52f, size.height * 0.56f), size.minDimension * 0.92f)
        }
    }
}

private fun DrawScope.drawBackgroundBubble(selected: Boolean) {
    drawCircle(
        color = Color.White.copy(alpha = if (selected) 0.64f else 0.50f),
        radius = size.minDimension * 0.42f,
        center = Offset(size.width * 0.64f, size.height * 0.34f)
    )
    drawCircle(
        color = Color.White.copy(alpha = if (selected) 0.34f else 0.24f),
        radius = size.minDimension * 0.18f,
        center = Offset(size.width * 0.22f, size.height * 0.76f)
    )
}

private fun DrawScope.drawSun(color: Color, center: Offset, radius: Float) {
    val stroke = Stroke(width = radius * 0.16f, cap = StrokeCap.Round)
    repeat(8) { index ->
        val angle = (PI * 2.0 * index / 8.0).toFloat()
        val start = Offset(
            x = center.x + cos(angle) * radius * 1.42f,
            y = center.y + sin(angle) * radius * 1.42f
        )
        val end = Offset(
            x = center.x + cos(angle) * radius * 1.86f,
            y = center.y + sin(angle) * radius * 1.86f
        )
        drawLine(color = color.copy(alpha = 0.72f), start = start, end = end, strokeWidth = stroke.width, cap = stroke.cap)
    }
    drawCircle(color = color, radius = radius, center = center)
    drawCircle(
        color = Color.White.copy(alpha = 0.32f),
        radius = radius * 0.58f,
        center = Offset(center.x - radius * 0.18f, center.y - radius * 0.22f)
    )
}

private fun DrawScope.drawMoon(color: Color, maskColor: Color, center: Offset, radius: Float) {
    drawCircle(color = color, radius = radius, center = center)
    drawCircle(
        color = maskColor.copy(alpha = 0.98f),
        radius = radius * 0.88f,
        center = Offset(center.x + radius * 0.38f, center.y - radius * 0.12f)
    )
    drawTinyStar(color.copy(alpha = 0.70f), Offset(center.x - radius * 1.02f, center.y - radius * 0.98f), radius * 0.28f)
}

private fun DrawScope.drawTinyStar(color: Color, center: Offset, radius: Float) {
    drawLine(
        color = color,
        start = Offset(center.x - radius, center.y),
        end = Offset(center.x + radius, center.y),
        strokeWidth = radius * 0.28f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = color,
        start = Offset(center.x, center.y - radius),
        end = Offset(center.x, center.y + radius),
        strokeWidth = radius * 0.28f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawDutyBuilding(color: Color, center: Offset, extent: Float) {
    val width = extent * 0.58f
    val height = extent * 0.62f
    val left = center.x - width / 2f
    val top = center.y - height / 2f
    drawRoundRect(
        color = color.copy(alpha = 0.92f),
        topLeft = Offset(left, top + height * 0.08f),
        size = Size(width, height * 0.92f),
        cornerRadius = CornerRadius(width * 0.14f, width * 0.14f)
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.88f),
        topLeft = Offset(center.x - width * 0.20f, top - height * 0.03f),
        size = Size(width * 0.40f, height * 0.16f),
        cornerRadius = CornerRadius(width * 0.08f, width * 0.08f)
    )
    val windowSize = width * 0.14f
    val gap = width * 0.12f
    repeat(2) { row ->
        repeat(2) { col ->
            drawRoundRect(
                color = Color.White.copy(alpha = 0.72f),
                topLeft = Offset(
                    x = center.x - gap / 2f - windowSize + col * (windowSize + gap),
                    y = top + height * 0.30f + row * (windowSize + gap)
                ),
                size = Size(windowSize, windowSize),
                cornerRadius = CornerRadius(windowSize * 0.26f, windowSize * 0.26f)
            )
        }
    }
}

private fun DrawScope.drawRestHouse(color: Color, center: Offset, extent: Float) {
    val width = extent * 0.64f
    val bodyHeight = extent * 0.36f
    val bodyLeft = center.x - width / 2f
    val bodyTop = center.y - bodyHeight * 0.10f
    val roof = Path().apply {
        moveTo(center.x - width * 0.55f, bodyTop + bodyHeight * 0.10f)
        lineTo(center.x, bodyTop - bodyHeight * 0.58f)
        lineTo(center.x + width * 0.55f, bodyTop + bodyHeight * 0.10f)
        close()
    }
    drawPath(color = color.copy(alpha = 0.92f), path = roof)
    drawRoundRect(
        color = color.copy(alpha = 0.78f),
        topLeft = Offset(bodyLeft, bodyTop),
        size = Size(width, bodyHeight),
        cornerRadius = CornerRadius(width * 0.12f, width * 0.12f)
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.72f),
        topLeft = Offset(center.x - width * 0.08f, bodyTop + bodyHeight * 0.30f),
        size = Size(width * 0.16f, bodyHeight * 0.42f),
        cornerRadius = CornerRadius(width * 0.05f, width * 0.05f)
    )
}

private fun DrawScope.drawWorkClipboard(color: Color, center: Offset, extent: Float) {
    val width = extent * 0.52f
    val height = extent * 0.64f
    val left = center.x - width / 2f
    val top = center.y - height / 2f
    drawRoundRect(
        color = color.copy(alpha = 0.86f),
        topLeft = Offset(left, top),
        size = Size(width, height),
        cornerRadius = CornerRadius(width * 0.13f, width * 0.13f)
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.82f),
        topLeft = Offset(center.x - width * 0.22f, top - height * 0.04f),
        size = Size(width * 0.44f, height * 0.16f),
        cornerRadius = CornerRadius(width * 0.10f, width * 0.10f)
    )
    repeat(3) { index ->
        val y = top + height * (0.36f + index * 0.17f)
        drawLine(
            color = Color.White.copy(alpha = 0.62f),
            start = Offset(left + width * 0.24f, y),
            end = Offset(left + width * 0.76f, y),
            strokeWidth = width * 0.06f,
            cap = StrokeCap.Round
        )
    }
}
