package com.oqba26.prayertimes

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import com.oqba26.prayertimes.models.MultiDate
import com.oqba26.prayertimes.screens.CalendarScreen
import com.oqba26.prayertimes.ui.theme.PrayerTimesTheme
import com.oqba26.prayertimes.utils.DateUtils
import com.oqba26.prayertimes.utils.PermissionHandler
import com.oqba26.prayertimes.services.PrayerForegroundService
import com.oqba26.prayertimes.viewmodels.PrayerViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: PrayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // مجوز نمایش نوتیف – اندروید 13+
        if (!PermissionHandler.checkNotificationPermission(this)) {
            PermissionHandler.requestNotificationPermission(this)
        }

        // فورگراند سرویس برای نوتیف دائمی استارت می‌شه
        val serviceIntent = Intent(this, PrayerForegroundService::class.java).apply {
            action = "START"
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        // بارگذاری دیتا
        viewModel.loadData(applicationContext)

        // نمایش UI اصلی
        setContent {
            var isDarkMode by remember { mutableStateOf(false) }

            PrayerTimesTheme(darkTheme = isDarkMode) {
                CalendarScreenEntryPoint(
                    viewModel = viewModel,
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = { isDarkMode = !isDarkMode }
                )
            }
        }
    }
}

@Composable
fun CalendarScreenEntryPoint(
    viewModel: PrayerViewModel,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit
) {
    var currentDate by remember { mutableStateOf<MultiDate>(DateUtils.getCurrentDate()) }
    val uiState = viewModel.uiState.collectAsState().value

    LaunchedEffect(currentDate) {
        viewModel.updateDate(currentDate)
    }

    CalendarScreen(
        currentDate = currentDate,
        uiState = uiState,
        onDateChange = { currentDate = it },
        onRetry = { viewModel.updateDate(currentDate) },
        viewModel = viewModel,
        onToggleDarkMode = onToggleDarkMode
    )
}