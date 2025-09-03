package com.oqba26.prayertimes.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

// پالت روشن (پس‌زمینه و کارت‌ها روشن)
private val LightColors = lightColorScheme(
    background = Color(0xFFF8FAFC),     // خیلی روشن
    surface = Color(0xFFFFFFFF),        // سفید برای کارت/دیالوگ
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
    primary = Color(0xFF81D4FA),
    onPrimary = Color(0xFF0B1B25)
)

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
    appFontFamily: FontFamily = FontFamily.Default,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val baseTypography = Typography()
    val typography = baseTypography.withFontFamily(appFontFamily)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}