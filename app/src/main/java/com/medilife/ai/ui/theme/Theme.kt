package com.medilife.ai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object Palette {
    val Cyan = Color(0xFF22D3EE)
    val Blue = Color(0xFF3B82F6)
    val Purple = Color(0xFF8B5CF6)
    val Teal = Color(0xFF14B8A6)
    val Pink = Color(0xFFEC4899)
    val Rose = Color(0xFFF43F5E)
    val Amber = Color(0xFFF59E0B)
    val Green = Color(0xFF22C55E)
    val BgDark = Color(0xFF070B1A)
    val BgDark2 = Color(0xFF0C1330)
    val BgLight = Color(0xFFF2F6FD)
    val BgLight2 = Color(0xFFE6EEFB)

    val primaryGradient = Brush.linearGradient(listOf(Cyan, Blue, Purple))
    val tealGradient = Brush.linearGradient(listOf(Teal, Cyan))
    val warmGradient = Brush.linearGradient(listOf(Pink, Purple))
    val dangerGradient = Brush.linearGradient(listOf(Rose, Color(0xFFFB7185)))
}

data class MediColors(val dark: Boolean, val glass: Color, val glassBorder: Color, val subtle: Color, val reduceMotion: Boolean)

val LocalMedi = staticCompositionLocalOf { MediColors(true, Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.6f), false) }

private val DarkScheme = darkColorScheme(
    primary = Palette.Cyan, onPrimary = Color(0xFF00222A),
    secondary = Palette.Purple, onSecondary = Color.White,
    tertiary = Palette.Teal,
    background = Palette.BgDark, onBackground = Color(0xFFE8EEFF),
    surface = Palette.BgDark2, onSurface = Color(0xFFE8EEFF),
    surfaceVariant = Color(0xFF16204A), onSurfaceVariant = Color(0xFFB4C0E0),
    surfaceContainer = Color(0xFF111A3D), surfaceContainerHigh = Color(0xFF18234D),
    outline = Color(0xFF3A4A7A),
    error = Palette.Rose
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF0891B2), onPrimary = Color.White,
    secondary = Color(0xFF7C3AED), onSecondary = Color.White,
    tertiary = Color(0xFF0D9488),
    background = Palette.BgLight, onBackground = Color(0xFF0B1530),
    surface = Color.White, onSurface = Color(0xFF0B1530),
    surfaceVariant = Color(0xFFE3EAF8), onSurfaceVariant = Color(0xFF4A5677),
    surfaceContainer = Color(0xFFF7F9FE), surfaceContainerHigh = Color(0xFFEFF3FC),
    outline = Color(0xFFB8C4DE),
    error = Color(0xFFE11D48)
)

private val Type = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 34.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 26.sp, letterSpacing = (-0.3).sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.4.sp)
)

@Composable
fun MediLifeTheme(themeMode: String, reduceMotion: Boolean, content: @Composable () -> Unit) {
    val dark = when (themeMode) { "LIGHT" -> false; "SYSTEM" -> isSystemInDarkTheme(); else -> true }
    val medi = if (dark) MediColors(true, Color.White.copy(alpha = 0.065f), Color.White.copy(alpha = 0.13f), Color.White.copy(alpha = 0.62f), reduceMotion)
    else MediColors(false, Color.White.copy(alpha = 0.78f), Color(0xFF0B1530).copy(alpha = 0.08f), Color(0xFF0B1530).copy(alpha = 0.6f), reduceMotion)
    CompositionLocalProvider(LocalMedi provides medi) {
        MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme, typography = Type, content = content)
    }
}
