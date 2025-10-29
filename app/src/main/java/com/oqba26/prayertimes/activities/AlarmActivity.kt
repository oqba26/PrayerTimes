package com.oqba26.prayertimes.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.oqba26.prayertimes.screens.alarm.AlarmNavGraph
import com.oqba26.prayertimes.theme.PrayerTimesTheme

@Suppress("DEPRECATION")
class AlarmActivity : ComponentActivity() {
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

        // 🔹 تنظیم تم کاربر از SharedPreferences
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val themeId = prefs.getString("themeId", "system") ?: "system"

        val isDarkThemeActive = when (themeId) {
            "dark" -> true
            "light" -> false
            else -> {
                (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                        android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
        }

        setContent {
            PrayerTimesTheme(darkTheme = isDarkThemeActive) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AlarmNavGraph(navController = navController)
                }
            }
        }
    }
}