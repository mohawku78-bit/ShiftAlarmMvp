package com.example.shiftalarmmvp.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ShiftColorScheme = lightColorScheme(
    primary = ShiftDesign.Navy,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD9EEF1),
    onPrimaryContainer = ShiftDesign.Ink,
    secondary = ShiftDesign.Harbor,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = ShiftDesign.Mist,
    onSecondaryContainer = ShiftDesign.Ink,
    tertiary = ShiftDesign.Sun,
    onTertiary = ShiftDesign.Ink,
    tertiaryContainer = Color(0xFFFFEDC6),
    onTertiaryContainer = Color(0xFF523600),
    background = ShiftDesign.CanvasBottom,
    onBackground = ShiftDesign.Ink,
    surface = ShiftDesign.Paper,
    onSurface = ShiftDesign.Ink,
    surfaceVariant = ShiftDesign.Mist,
    onSurfaceVariant = ShiftDesign.InkSoft,
    outline = ShiftDesign.Line,
    error = ShiftDesign.Coral,
    errorContainer = Color(0xFFFFDED8),
    onErrorContainer = Color(0xFF4C130B)
)

private val ShiftDisplayFont = FontFamily.Serif
private val ShiftBodyFont = FontFamily.SansSerif
private val ShiftAccentFont = FontFamily.Monospace

private val ShiftTypography = Typography(
    headlineSmall = TextStyle(
        fontFamily = ShiftDisplayFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 34.sp,
        lineHeight = 39.sp
    ),
    titleLarge = TextStyle(
        fontFamily = ShiftDisplayFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 27.sp,
        lineHeight = 32.sp
    ),
    titleMedium = TextStyle(
        fontFamily = ShiftDisplayFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 21.sp,
        lineHeight = 27.sp
    ),
    titleSmall = TextStyle(
        fontFamily = ShiftBodyFont,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = ShiftBodyFont,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodySmall = TextStyle(
        fontFamily = ShiftBodyFont,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = ShiftBodyFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = ShiftAccentFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = ShiftAccentFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)

private val ShiftShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(30.dp)
)

@Composable
fun ShiftAlarmTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ShiftColorScheme,
        typography = ShiftTypography,
        shapes = ShiftShapes,
        content = content
    )
}
