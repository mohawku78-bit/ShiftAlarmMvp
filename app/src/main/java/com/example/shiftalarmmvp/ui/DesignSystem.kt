package com.example.shiftalarmmvp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal object ShiftDesign {
    val Ink = Color(0xFF172033)
    val InkSoft = Color(0xFF4D5A6C)
    val Navy = Color(0xFF113C5F)
    val Harbor = Color(0xFF176B87)
    val Lagoon = Color(0xFF20A39E)
    val Sun = Color(0xFFFFB84D)
    val Coral = Color(0xFFE8614D)
    val Paper = Color(0xFFFFFFFF)
    val CanvasTop = Color(0xFFFFF7E8)
    val CanvasMid = Color(0xFFEAF5F2)
    val CanvasBottom = Color(0xFFF7FAF5)
    val Mist = Color(0xFFEAF1EF)
    val MistStrong = Color(0xFFDDE8E5)
    val Line = Color(0xFFD4DDD9)
    val Night = Color(0xFF25345E)
    val Day = Color(0xFFC77700)
    val Duty = Color(0xFF087C72)
    val Rest = Color(0xFF6D7480)
}

@Composable
internal fun shiftAppBackgroundBrush(): Brush = Brush.verticalGradient(
    colors = listOf(
        ShiftDesign.CanvasTop,
        ShiftDesign.CanvasMid,
        ShiftDesign.CanvasBottom
    )
)

internal fun shiftHeroBrush(): Brush = Brush.linearGradient(
    colors = listOf(
        ShiftDesign.Navy,
        ShiftDesign.Harbor,
        ShiftDesign.Lagoon
    )
)

@Composable
internal fun ShiftPanel(
    modifier: Modifier = Modifier,
    containerColor: Color = ShiftDesign.Paper,
    borderColor: Color = ShiftDesign.Line,
    shape: Shape = MaterialTheme.shapes.large,
    elevation: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.border(1.dp, borderColor, shape),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        shape = shape
    ) {
        content()
    }
}

@Composable
internal fun ShiftPill(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = ShiftDesign.Mist,
    contentColor: Color = ShiftDesign.InkSoft
) {
    Box(
        modifier = modifier
            .background(containerColor, RoundedCornerShape(999.dp))
            .border(1.dp, contentColor.copy(alpha = 0.10f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            fontWeight = FontWeight.Bold
        )
    }
}
