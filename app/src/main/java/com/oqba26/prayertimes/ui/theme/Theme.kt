package com.oqba26.prayertimes.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun PrayerTimesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // 👈 این خط اضافـه شد
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme() // یا تعریف رنگ سفارشی‌شده
    } else {
        lightColorScheme()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // اگه Typography سفارشی‌سازی کردی
        content = content
    )
}