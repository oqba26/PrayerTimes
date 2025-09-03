package com.oqba26.prayertimes

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.oqba26.prayertimes.models.MultiDate
import com.oqba26.prayertimes.screens.CalendarScreen
import com.oqba26.prayertimes.screens.SettingsScreen
import com.oqba26.prayertimes.services.PrayerForegroundService
import com.oqba26.prayertimes.theme.PrayerTimesTheme
import com.oqba26.prayertimes.ui.AppFonts
import com.oqba26.prayertimes.utils.DateUtils
import com.oqba26.prayertimes.viewmodels.PrayerViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: PrayerViewModel by viewModels()

    private val requestPostNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startPrayerServiceIfNeeded()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // لود دیتا
        viewModel.loadData(applicationContext)

        // اجازه نوتیف + استارت سرویس
        if (hasPostNotificationPermission()) startPrayerServiceIfNeeded()
        else if (Build.VERSION.SDK_INT >= 33) {
            requestPostNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val isDarkModeInitially = prefs.getBoolean("darkMode", false)
        val initialFontId = prefs.getString("fontId", "system") ?: "system"

        setContent {
            var isDarkMode by remember { mutableStateOf(isDarkModeInitially) }
            var showSettings by remember { mutableStateOf(false) }
            var showExitDialog by remember { mutableStateOf(false) }
            var fontId by remember { mutableStateOf(initialFontId) }

            val ctx = LocalContext.current
            val appFontFamily = remember(fontId) { AppFonts.familyFor(ctx, fontId) }
            val activity = ctx as Activity

            // مدیریت Back گوشی:
            BackHandler {
                if (showSettings) {
                    showSettings = false
                } else {
                    showExitDialog = true
                }
            }

            PrayerTimesTheme(darkTheme = isDarkMode, appFontFamily = appFontFamily) {
                Box(Modifier.fillMaxSize()) {
                    CalendarScreenEntryPoint(
                        viewModel = viewModel,
                        isDarkMode = isDarkMode,
                        onToggleDarkMode = {
                            isDarkMode = !isDarkMode
                            prefs.edit().putBoolean("darkMode", isDarkMode).apply()
                        },
                        onOpenSettings = { showSettings = true }
                    )

                    if (showSettings) {
                        SettingsScreen(
                            currentFontId = fontId,
                            onSelectFont = {
                                // 1) ذخیره فونت انتخابی
                                fontId = it
                                prefs.edit().putString("fontId", it).apply()

                                // 2) ریفرش فوری نوتیف (Foreground Service) با اکشن RESTART
                                val i = Intent(ctx, PrayerForegroundService::class.java).apply {
                                    action = "RESTART"
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    ctx.startForegroundService(i)
                                } else {
                                    ctx.startService(i)
                                }

                                // 3) بستن تنظیمات
                                showSettings = false
                            },
                            onClose = { showSettings = false }
                        )
                    }

                    // دیالوگ تایید خروج
                    if (showExitDialog) {
                        AlertDialog(
                            onDismissRequest = { showExitDialog = false },
                            title = { Text("خروج از برنامه") },
                            text = { Text("می‌خوای از برنامه خارج بشی؟") },
                            confirmButton = {
                                TextButton(onClick = {
                                    showExitDialog = false
                                    activity.finish()
                                }) {
                                    Text("بله")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showExitDialog = false }) {
                                    Text("خیر")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun needsPostNotificationPermission(): Boolean = Build.VERSION.SDK_INT >= 33

    private fun hasPostNotificationPermission(): Boolean {
        return if (needsPostNotificationPermission()) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    private fun startPrayerServiceIfNeeded() {
        if (!isServiceRunning(PrayerForegroundService::class.java)) {
            val serviceIntent = Intent(this, PrayerForegroundService::class.java).apply { action = "START" }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(serviceIntent)
            else startService(serviceIntent)
        }
    }

    private fun Context.isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        return manager.getRunningServices(Int.MAX_VALUE).any {
            it.service.className == serviceClass.name
        }
    }
}

@Composable
fun CalendarScreenEntryPoint(
    viewModel: PrayerViewModel,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onOpenSettings: () -> Unit
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
        onToggleDarkMode = onToggleDarkMode,
        onOpenSettings = onOpenSettings
    )
}