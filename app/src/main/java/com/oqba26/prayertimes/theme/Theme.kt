package com.oqba26.prayertimes.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.oqba26.prayertimes.R // ایمپورت برای دسترسی به منابع فونت

// فونت پیش فرض برنامه (وزیرمتن)
val DefaultAppFontFamily = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_bold, FontWeight.Bold)
)

// پالت روشن (پس‌زمینه و کارت‌ها روشن)
private val LightColors = lightColorScheme(
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF1F5F9),
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF111827),
    primary = Color(0xFF0E7490),
    onPrimary = Color.White
)

// پالت تاریک (تیره اما خوانا)
private val DarkColors = darkColorScheme(
    background = Color(0xFF0F1115),
    surface = Color(0xFF1A1D22),        // کمی روشن‌تر از مشکی
    surfaceVariant = Color(0xFF23272E),
    onBackground = Color(0xFFE5E7EB),
    onSurface = Color(0xFFE5E7EB),
    primary = Color(0xFF23272E),
    onPrimary = Color(0xFFE5E7EB)
)

@Immutable
data class DiffLabelColors(
    val gradientStart: Color,
    val gradientEnd: Color,
    val onGradient: Color
)

private val LightDiffLabelColors = DiffLabelColors(
    gradientStart = Color(0xFF0E7490),
    gradientEnd = Color(0xFF00ACC1),
    onGradient = Color(0xFFFFFDE7)
)

// Changed to align with the rest of the dark theme
private val DarkDiffLabelColors = DiffLabelColors(
    gradientStart = DarkColors.surface, // Was Color(0xFF00373F)
    gradientEnd = DarkColors.surfaceVariant, // Was Color(0xFF005F6B)
    onGradient = DarkColors.onSurface // Was Color(0xFFE0F7FA)
)

internal val LocalDiffLabelColors = staticCompositionLocalOf { LightDiffLabelColors }

// Helper: اعمال خانواده فونت روی کل تایپوگرافی
private fun Typography.withFontFamily(family: FontFamily): Typography = Typography(
    displayLarge = displayLarge.copy(fontFamily = family),
    displayMedium = displayMedium.copy(fontFamily = family),
    displaySmall = displaySmall.copy(fontFamily = family),
    headlineLarge = headlineLarge.copy(fontFamily = family),
    headlineMedium = headlineMedium.copy(fontFamily = family),
    headlineSmall = headlineSmall.copy(fontFamily = family),
    titleLarge = titleLarge.copy(fontFamily = family),
    titleMedium = titleMedium.copy(fontFamily = family),
    titleSmall = titleSmall.copy(fontFamily = family),
    bodyLarge = bodyLarge.copy(fontFamily = family),
    bodyMedium = bodyMedium.copy(fontFamily = family),
    bodySmall = bodySmall.copy(fontFamily = family),
    labelLarge = labelLarge.copy(fontFamily = family),
    labelMedium = labelMedium.copy(fontFamily = family),
    labelSmall = labelSmall.copy(fontFamily = family),
)

@Composable
fun PrayerTimesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    appFontFamily: FontFamily = DefaultAppFontFamily, // استفاده از فونت پیش فرض برنامه
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val diffLabelThemeColors = if (darkTheme) DarkDiffLabelColors else LightDiffLabelColors
    val baseTypography = Typography() // ایجاد یک Typography پایه با فونت های پیش فرض Material
    val typography = baseTypography.withFontFamily(appFontFamily) // اعمال فونت سفارشی به تمام سبک ها

    CompositionLocalProvider(LocalDiffLabelColors provides diffLabelThemeColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}
