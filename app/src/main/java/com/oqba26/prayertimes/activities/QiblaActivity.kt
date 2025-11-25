@file:Suppress("DEPRECATION")

package com.oqba26.prayertimes.activities

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.oqba26.prayertimes.screens.QiblaScreen
import com.oqba26.prayertimes.theme.PrayerTimesTheme

class QiblaActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // وضعیت تم انتخابی (از CalendarScreen پاس داده می‌شود)
        val isDark = intent.getBooleanExtra("IS_DARK", false)

        // رنگ استاتوس‌بار = همان رنگ هدر قبله‌نما
        val statusBarColor = if (isDark) 0xFF4F378B.toInt() else 0xFF0E7490.toInt()
        window.statusBarColor = statusBarColor

        // همیشه آیکون‌ها و متن استاتوس‌بار سفید باشند (LIGHT_STATUS_BAR را غیرفعال می‌کنیم)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val decor = window.decorView
            decor.systemUiVisibility =
                decor.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        }

        setContent {
            // همین تمی که در MainActivity استفاده می‌کنی
            PrayerTimesTheme(darkTheme = isDark) {
                QiblaScreen(
                    isDarkThemeActive = isDark,
                    onBack = { finish() }
                )
            }
        }
    }
}