package com.oqba26.prayertimes

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import com.oqba26.prayertimes.adhan.AdhanScheduler
import com.oqba26.prayertimes.screens.CalendarScreenEntryPoint
import com.oqba26.prayertimes.screens.SettingsScreen
import com.oqba26.prayertimes.services.AdhanPlayerService
import com.oqba26.prayertimes.services.PrayerForegroundService
import com.oqba26.prayertimes.theme.PrayerTimesTheme
import com.oqba26.prayertimes.ui.AppFonts
import com.oqba26.prayertimes.utils.DateUtils
import com.oqba26.prayertimes.viewmodels.PrayerViewModel
import com.oqba26.prayertimes.viewmodels.SettingsViewModel
import com.oqba26.prayertimes.widget.ModernWidgetProvider

@Suppress("DEPRECATION")
class MainActivity : ComponentActivity() {

    private val prayerViewModel: PrayerViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startPrayerServiceIfNeeded()

        // اعمال تنظیم نوار وضعیت و نوار پایین
        applySystemBars(isDarkThemeEffective())

        setContent {
            val ctx = LocalContext.current
            val prefs = remember { ctx.getSharedPreferences("settings", MODE_PRIVATE) }

            val initialThemeId = remember { prefs.getString("themeId", "system") ?: "system" }
            val initialFontId = remember { prefs.getString("fontId", "default") ?: "default" }
            val initialUsePersianNumbers = remember { prefs.getBoolean("use_persian_numbers", true) }
            val initialUse24HourFormat = remember { prefs.getBoolean("use_24_hour_format", true) }
            val initialAdhanEnabled = remember { prefs.getBoolean("adhan_enabled", true) }

            var themeId by remember { mutableStateOf(initialThemeId) }
            var fontId by remember { mutableStateOf(initialFontId) }
            var showSettings by remember { mutableStateOf(false) }
            var showExitDialog by remember { mutableStateOf(false) }
            var showWidgetPrompt by remember { mutableStateOf(false) }
            var usePersianNumbers by remember { mutableStateOf(initialUsePersianNumbers) }
            var use24HourFormat by remember { mutableStateOf(initialUse24HourFormat) }
            var adhanEnabled by remember { mutableStateOf(initialAdhanEnabled) }

            val prayerSilentSettings by settingsViewModel.prayerSilentSettings.collectAsState()

            val appFontFamily = remember(fontId) { AppFonts.familyFor(fontId) }

            val isDarkTheme = when (themeId) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            // هر بار که تم تغییر می‌کند، رنگ نوارها هم هماهنگ شود
            LaunchedEffect(isDarkTheme) {
                applySystemBars(isDarkTheme)
            }

            LaunchedEffect(usePersianNumbers) {
                DateUtils.setDefaultUsePersianNumbers(usePersianNumbers)
            }

            // تابع راه‌اندازی مجدد سرویس‌ها
            val ctxRestart = remember(ctx) {
                {
                    val svcIntent = Intent(ctx, PrayerForegroundService::class.java)
                        .apply { action = "RESTART" }
                    ContextCompat.startForegroundService(ctx, svcIntent)
                }
            }

            val onToggleTheme: () -> Unit = {
                val newThemeId = if (isDarkTheme) "light" else "dark"
                themeId = newThemeId
                prefs.edit(commit = true) {
                    putString("themeId", newThemeId)
                    putBoolean("is_dark_theme", newThemeId == "dark")
                }
                applySystemBars(newThemeId == "dark")
                ctxRestart()
                notifyAllWidgetsExplicit(ctx, ModernWidgetProvider.ACTION_THEME_CHANGED)
                notifyAllWidgetsExplicit(ctx, ModernWidgetProvider.ACTION_CLOCK_TICK_UPDATE)
                updateAllWidgetsNow(ctx)
            }

            androidx.activity.compose.BackHandler {
                if (showSettings) showSettings = false else showExitDialog = true
            }

            PrayerTimesTheme(
                darkTheme = isDarkTheme,
                appFontFamily = appFontFamily
            ) {
                Box(Modifier.fillMaxSize()) {
                    CalendarScreenEntryPoint(
                        viewModel = prayerViewModel,
                        onOpenSettings = { showSettings = true },
                        isDarkThemeActive = isDarkTheme,
                        onToggleTheme = onToggleTheme,
                        usePersianNumbers = usePersianNumbers,
                        use24HourFormat = use24HourFormat
                    )

                    if (showSettings) {
                        SettingsScreen(
                            currentFontId = fontId,
                            onSelectFont = { newId ->
                                fontId = newId
                                prefs.edit(commit = true) { putString("fontId", newId) }
                                ctxRestart()
                                notifyAllWidgetsExplicit(ctx, ModernWidgetProvider.ACTION_CLOCK_TICK_UPDATE)
                                updateAllWidgetsNow(ctx)
                            },
                            usePersianNumbers = usePersianNumbers,
                            onUsePersianNumbersChange = { newValue ->
                                usePersianNumbers = newValue
                                prefs.edit(commit = true) { putBoolean("use_persian_numbers", newValue) }
                                DateUtils.setDefaultUsePersianNumbers(newValue)
                                prayerViewModel.loadData(applicationContext)
                                ctxRestart()
                                notifyAllWidgetsExplicit(ctx, ModernWidgetProvider.ACTION_CLOCK_TICK_UPDATE)
                                updateAllWidgetsNow(ctx)
                            },
                            use24HourFormat = use24HourFormat,
                            onUse24HourFormatChange = { newValue ->
                                use24HourFormat = newValue
                                prefs.edit(commit = true) { putBoolean("use_24_hour_format", newValue) }
                                prayerViewModel.loadData(applicationContext)
                                ctxRestart()
                                notifyAllWidgetsExplicit(ctx, ModernWidgetProvider.ACTION_CLOCK_TICK_UPDATE)
                                updateAllWidgetsNow(ctx)
                            },
                            adhanEnabled = adhanEnabled,
                            onAdhanEnabledChange = { enabled ->
                                adhanEnabled = enabled
                                prefs.edit(commit = true) { putBoolean("adhan_enabled", enabled) }

                                if (!enabled) {
                                    AdhanScheduler.cancelAll(ctx)
                                    AdhanPlayerService.stop(ctx)
                                }
                                ctxRestart()
                            },
                            _onStopAdhan = { AdhanPlayerService.stop(ctx) },
                            onClose = { showSettings = false },
                            isDarkThemeActive = isDarkTheme,
                            prayerSilentSettings = prayerSilentSettings,
                            onPrayerSilentSettingChange = { prayerTime, isEnabled ->
                                settingsViewModel.updatePrayerSilentSetting(prayerTime, isEnabled)
                                ctxRestart()
                            }
                        )
                    }

                    if (showExitDialog) {
                        ExitConfirmDialog(
                            isDark = isDarkTheme,
                            onCancel = { showExitDialog = false },
                            onConfirm = {
                                showExitDialog = false
                                finish()
                            }
                        )
                    }

                    if (showWidgetPrompt) {
                        WidgetPromptDialog(
                            isDark = isDarkTheme,
                            onNotNow = {
                                showWidgetPrompt = false
                                prefs.edit { putBoolean("widget_prompt_shown", true) }
                            },
                            onAdd = {
                                showWidgetPrompt = false
                                prefs.edit { putBoolean("widget_prompt_shown", true) }
                                requestPinHomeWidget(ctx)
                            }
                        )
                    }
                }
            }
        }

        // ✳️ بعد از ساخت UI۔
        // نوار پایین را همیشه سفید نگه دارد
        window.navigationBarColor = android.graphics.Color.WHITE
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightNavigationBars = true
    }

    override fun onResume() {
        super.onResume()
        applySystemBars(isDarkThemeEffective())
    }

    private fun startPrayerServiceIfNeeded() {
        val svc = Intent(this, PrayerForegroundService::class.java).apply { action = "START" }
        ContextCompat.startForegroundService(this, svc)
    }

    private fun requestPinHomeWidget(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Toast.makeText(ctx, "برای افزودن ویجت، از منوی ویجت‌های لانچر استفاده کنید.", Toast.LENGTH_LONG).show()
            return
        }
        val awm = AppWidgetManager.getInstance(ctx)
        if (!awm.isRequestPinAppWidgetSupported) {
            Toast.makeText(ctx, "لانچر شما از افزودن مستقیم ویجت پشتیبانی نمی‌کند.", Toast.LENGTH_LONG).show()
            return
        }
        val success = awm.requestPinAppWidget(
            ComponentName(ctx, ModernWidgetProvider::class.java),
            null,
            null
        )
        if (!success) {
            Toast.makeText(ctx, "متاسفانه افزودن ویجت با خطا مواجه شد.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isDarkThemeEffective(): Boolean {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        return when (prefs.getString("themeId", "system")) {
            "dark" -> true
            "light" -> false
            else -> {
                (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                        Configuration.UI_MODE_NIGHT_YES
            }
        }
    }

    private fun applySystemBars(isDarkTheme: Boolean) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val statusColor = if (isDarkTheme) 0xFF4F378B.toInt() else 0xFF0E7490.toInt()
        window.statusBarColor = statusColor
        window.navigationBarColor = android.graphics.Color.WHITE
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = true
    }
}

private fun notifyAllWidgetsExplicit(ctx: Context, action: String) {
    val intent = Intent(action).apply {
        component = ComponentName(ctx, ModernWidgetProvider::class.java)
    }
    ctx.sendBroadcast(intent)
}

private fun updateAllWidgetsNow(ctx: Context) {
    val mgr = AppWidgetManager.getInstance(ctx)
    val component = ComponentName(ctx, ModernWidgetProvider::class.java)
    val ids = mgr.getAppWidgetIds(component)
    if (ids.isEmpty()) return

    val intent = Intent(ctx, ModernWidgetProvider::class.java).apply {
        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
    }
    ctx.sendBroadcast(intent)
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExitConfirmDialog(
    isDark: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    val headerColor = if (isDark) Color(0xFF4F378B) else Color(0xFF0E7490)
    val headerTextColor = if (isDark) Color(0xFFEADDFF) else Color.White

    BasicAlertDialog(onDismissRequest = onCancel) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(Modifier.fillMaxWidth()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(headerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text("هشدار", color = headerTextColor, style = MaterialTheme.typography.titleLarge)
                }

                Text(
                    "شما در حال خروج از برنامه هستید. مطمئنید؟",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textAlign = TextAlign.Right
                )

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = onCancel,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = headerColor,
                                contentColor = headerTextColor
                            )
                        ) { Text("خیر") }

                        Button(
                            onClick = onConfirm,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) { Text("بله") }
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetPromptDialog(
    isDark: Boolean,
    onNotNow: () -> Unit,
    onAdd: () -> Unit
) {
    val headerColor = if (isDark) Color(0xFF4F378B) else Color(0xFF0E7490)
    val headerTextColor = if (isDark) Color(0xFFEADDFF) else Color.White

    BasicAlertDialog(onDismissRequest = onNotNow) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(Modifier.fillMaxWidth()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(headerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text("افزودن ویجت", color = headerTextColor, style = MaterialTheme.typography.titleLarge)
                }

                Text(
                    "مایلید ویجت برنامه به صفحهٔ اصلی اضافه شود؟",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textAlign = TextAlign.Right
                )

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = onNotNow,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = headerColor,
                                contentColor = headerTextColor
                            )
                        ) { Text("الان نه") }

                        Button(
                            onClick = onAdd,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) { Text("بله، اضافه کن") }
                    }
                }
            }
        }
    }
}