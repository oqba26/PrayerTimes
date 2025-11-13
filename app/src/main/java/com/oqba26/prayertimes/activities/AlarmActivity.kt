package com.oqba26.prayertimes.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.oqba26.prayertimes.screens.alarm.AlarmNavGraph
import com.oqba26.prayertimes.theme.PrayerTimesTheme
import com.oqba26.prayertimes.viewmodels.SettingsViewModel

@Suppress("DEPRECATION")
class AlarmActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🚀 تنظیم هماهنگ با MainActivity
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // رنگ استاتوس‌بار شفاف (زیر اپ‌بار)
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        // 🌤️ نویگیشن‌بار همیشه سفید با آیکن‌های تیره (مانند بقیه صفحات)
        window.navigationBarColor = android.graphics.Color.WHITE

        // کنترل وضعیت آیکن‌های نوارها
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = false   // آیکن‌های نوار بالا روشن
        controller.isAppearanceLightNavigationBars = true // آیکن‌های نوار پایین تیره (روی سفید واضح)

        setContent {
            // --- خواندن تمام تنظیمات از ViewModel به عنوان تنها منبع صحیح ---
            val usePersianNumbers by settingsViewModel.usePersianNumbers.collectAsState()
            val themeId by settingsViewModel.themeId.collectAsState()

            // --- تعیین تم بر اساس مقدار خوانده شده از ViewModel ---
            val isDarkThemeActive = when (themeId) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            PrayerTimesTheme(darkTheme = isDarkThemeActive) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AlarmNavGraph(navController = navController, usePersianNumbers = usePersianNumbers)
                }
            }
        }
    }
}