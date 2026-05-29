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
    primary = Color(0xFF245FD6),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD8E5FF),
    onPrimaryContainer = Color(0xFF082867),
    secondary = Color(0xFF526174),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE6EBF2),
    onSecondaryContainer = Color(0xFF1D2734),
    tertiary = Color(0xFF0F766E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD7F2EF),
    onTertiaryContainer = Color(0xFF073C38),
    background = Color(0xFFF5F7FB),
    onBackground = Color(0xFF141923),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF141923),
    surfaceVariant = Color(0xFFE8EDF5),
    onSurfaceVariant = Color(0xFF445061),
    outline = Color(0xFF7D8796),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

private val ShiftTypography = Typography(
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 38.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 25.sp,
        lineHeight = 31.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 21.sp,
        lineHeight = 27.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)

private val ShiftShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
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
