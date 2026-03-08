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
    primary = Color(0xFF2D6CE8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE7FF),
    onPrimaryContainer = Color(0xFF0A2C75),
    secondary = Color(0xFF5B6C8F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE3E9F7),
    onSecondaryContainer = Color(0xFF1F2A42),
    tertiary = Color(0xFF4D7FAF),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD8EBFF),
    onTertiaryContainer = Color(0xFF032B52),
    background = Color(0xFFF2F4F8),
    onBackground = Color(0xFF161B24),
    surface = Color(0xFFFCFCFF),
    onSurface = Color(0xFF161B24),
    surfaceVariant = Color(0xFFE8ECF4),
    onSurfaceVariant = Color(0xFF464E5E),
    outline = Color(0xFF7A8395)
)

private val ShiftTypography = Typography(
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 34.sp,
        lineHeight = 40.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp
    )
)

private val ShiftShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(28.dp)
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
