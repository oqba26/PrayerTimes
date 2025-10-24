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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
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

        // اعمال اولیه سیستم‌بارها مطابق تم موثر فعلی
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

            // هماهنگی رنگ استاتوس‌بار و نویگیشن‌بار با تغییر تم
            LaunchedEffect(isDarkTheme) {
                applySystemBars(isDarkTheme)
            }

            LaunchedEffect(usePersianNumbers) {
                DateUtils.setDefaultUsePersianNumbers(usePersianNumbers)
            }

            val ctxRestart = remember(ctx) {
                {
                    val svcIntent = Intent(ctx, PrayerForegroundService::class.java).apply { action = "RESTART" }
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
                // اعمال فوری تغییرات سیستم‌بار
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
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = { showExitDialog = false },
                            title = {
                                Text(text = ": هشدار", textAlign = TextAlign.Right)
                            },
                            text = {
                                Text(text = "شما در حال خروج از برنامه هستید. مطمئنید؟", textAlign = TextAlign.Right)
                            },
                            confirmButton = {
                                androidx.compose.foundation.layout.Row(
                                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                                ) {
                                    Button(
                                        onClick = { showExitDialog = false },
                                    ) { Text("خیر") }

                                    Button(
                                        onClick = {
                                            showExitDialog = false
                                            finish()
                                        },
                                    ) { Text("بله") }
                                }
                            },
                            dismissButton = {}
                        )
                    }

                    if (showWidgetPrompt) {
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = {
                                showWidgetPrompt = false
                                prefs.edit { putBoolean("widget_prompt_shown", true) }
                            },
                            title = { Text(text = "افزودن ویجت") },
                            text = { Text(text = "مایلید ویجت برنامه به صفحهٔ اصلی اضافه شود؟") },
                            confirmButton = {
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    androidx.compose.foundation.layout.Row(
                                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                                    ) {
                                        Button(onClick = {
                                            showWidgetPrompt = false
                                            prefs.edit { putBoolean("widget_prompt_shown", true) }
                                        }) { Text("الان نه") }

                                        Button(onClick = {
                                            showWidgetPrompt = false
                                            prefs.edit { putBoolean("widget_prompt_shown", true) }
                                            requestPinHomeWidget(ctx)
                                        }) { Text("بله، اضافه کن") }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // اطمینان از اعمال تنظیمات سیستم‌بار پس از بازگشت
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

    // تعیین تم موثر فعلی: dark/light یا system
    private fun isDarkThemeEffective(): Boolean {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        return when (prefs.getString("themeId", "system")) {
            "dark" -> true
            "light" -> false
            else -> {
                (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            }
        }
    }

    // سیستم‌بارها:
    // - StatusBar هم‌رنگ تاپ‌بار و تا لبهٔ بالا کشیده می‌شود
    // - NavigationBar همیشه سفید و بدون تاثیر از تم برنامه
    private fun applySystemBars(isDarkTheme: Boolean) {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // رنگ استاتوس‌بار مطابق تاپ‌بار (روشن/تیره)
        val statusColor = if (isDarkTheme) 0xFF4F378B.toInt() else 0xFF0E7490.toInt()
        window.statusBarColor = statusColor

        // نویگیشن‌بار: همیشه سفید، آیکن‌ها تیره (Light Navigation Bars)
        window.navigationBarColor = android.graphics.Color.WHITE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = true
        }

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        // روی رنگ‌های ما، آیکن‌های استاتوس‌بار باید روشن باشند
        controller.isAppearanceLightStatusBars = false
        // روی نویگیشن‌بار سفید، آیکن‌ها باید تیره باشند
        controller.isAppearanceLightNavigationBars = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val lp = window.attributes
            lp.layoutInDisplayCutoutMode =
                android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = lp
        }
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