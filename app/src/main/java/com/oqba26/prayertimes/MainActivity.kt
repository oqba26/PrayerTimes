package com.oqba26.prayertimes

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.style.TextAlign
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
import com.oqba26.prayertimes.widget.LargeModernWidgetProvider
import com.oqba26.prayertimes.widget.ModernWidgetProvider

private val WIDGET_PROVIDERS = listOf(
    ModernWidgetProvider::class.java,
    LargeModernWidgetProvider::class.java
)

@RequiresApi(Build.VERSION_CODES.M)
class MainActivity : ComponentActivity() {

    private val prayerViewModel: PrayerViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startPrayerServiceIfNeeded()

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)

        setContent {
            val ctx = this

            val initialThemeId = remember { prefs.getString("themeId", "system") ?: "system" }
            val initialFontId = remember { prefs.getString("fontId", "estedad") ?: "estedad" }
            val initialAdhanEnabled = remember { prefs.getBoolean("adhan_enabled", true) }

            var themeId by remember { mutableStateOf(initialThemeId) }
            var fontId by remember { mutableStateOf(initialFontId) }
            var showSettings by remember { mutableStateOf(false) }
            var showExitDialog by remember { mutableStateOf(false) }
            var adhanEnabled by remember { mutableStateOf(initialAdhanEnabled) }

            val is24HourFormat by settingsViewModel.is24HourFormat.collectAsState()
            val usePersianNumbers by settingsViewModel.usePersianNumbers.collectAsState()

            val appFontFamily = remember(fontId) { AppFonts.familyFor(fontId) }

            val isDarkTheme = when (themeId) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            val onSettingsChanged = remember(ctx) {
                {
                    val svcIntent = Intent(ctx, PrayerForegroundService::class.java).apply { action = "RESTART" }
                    ContextCompat.startForegroundService(ctx, svcIntent)
                    updateAllWidgetsNow(ctx)
                }
            }

            LaunchedEffect(isDarkTheme) {
                applySystemBars(isDarkTheme)
            }

            LaunchedEffect(usePersianNumbers) {
                DateUtils.setDefaultUsePersianNumbers(usePersianNumbers)
            }

            val onToggleTheme: () -> Unit = {
                val newThemeId = if (isDarkTheme) "light" else "dark"
                prefs.edit {
                    putString("themeId", newThemeId)
                    putBoolean("is_dark_theme", newThemeId == "dark")
                }
                themeId = newThemeId
                applySystemBars(newThemeId == "dark")
                onSettingsChanged()
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
                        use24HourFormat = is24HourFormat
                    )

                    if (showSettings) {
                        SettingsScreen(
                            onClose = { showSettings = false },
                            isDarkThemeActive = isDarkTheme,
                            currentFontId = fontId,
                            onSelectFont = { newFontId ->
                                fontId = newFontId
                                prefs.edit { putString("fontId", newFontId) }
                                onSettingsChanged()
                            },
                            adhanEnabled = adhanEnabled,
                            onAdhanEnabledChange = { enabled ->
                                adhanEnabled = enabled
                                prefs.edit { putBoolean("adhan_enabled", enabled) }
                                if (!enabled) {
                                    AdhanScheduler.cancelAll(ctx)
                                    AdhanPlayerService.stop(ctx)
                                }
                                onSettingsChanged()
                            },
                            onAddWidget = { requestPinHomeWidget(ctx) },
                            usePersianNumbers = usePersianNumbers,
                            onUsePersianNumbersChange = { settingsViewModel.updateUsePersianNumbers(it) },
                            is24HourFormat = is24HourFormat,
                            onIs24HourFormatChange = { settingsViewModel.updateIs24HourFormat(it) }
                        )
                    }

                    if (showExitDialog) {
                        ExitConfirmDialog(
                            isDark = isDarkTheme,
                            onCancel = { showExitDialog = false },
                            onConfirm = { finish() }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        applySystemBars(isDarkThemeEffective())
    }

    private fun startPrayerServiceIfNeeded() {
        val svc = Intent(this, PrayerForegroundService::class.java).apply { action = "START" }
        ContextCompat.startForegroundService(this, svc)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun requestPinHomeWidget(ctx: Context) {
        val awm = AppWidgetManager.getInstance(ctx)
        if (!awm.isRequestPinAppWidgetSupported) {
            Toast.makeText(ctx, "لانچر شما از افزودن مستقیم ویجت پشتیبانی نمی‌کند.", Toast.LENGTH_LONG).show()
            return
        }
        awm.requestPinAppWidget(ComponentName(ctx, LargeModernWidgetProvider::class.java), null, null)
    }

    private fun isDarkThemeEffective(): Boolean {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        return when (prefs.getString("themeId", "system")) {
            "dark" -> true
            "light" -> false
            else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }
    }

    @Suppress("DEPRECATION")
    private fun applySystemBars(isDarkTheme: Boolean) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val statusColor = if (isDarkTheme) 0xFF4F378B.toInt() else 0xFF0E7490.toInt()
        window.statusBarColor = statusColor
        window.navigationBarColor = Color.WHITE
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = true
    }
}

private fun updateAllWidgetsNow(ctx: Context) {
    val mgr = AppWidgetManager.getInstance(ctx)
    WIDGET_PROVIDERS.forEach { providerClass ->
        val component = ComponentName(ctx, providerClass)
        val ids = mgr.getAppWidgetIds(component)
        if (ids.isNotEmpty()) {
            val intent = Intent(ctx, providerClass).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            ctx.sendBroadcast(intent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExitConfirmDialog(
    isDark: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    val headerColor = if (isDark) ComposeColor(0xFF4F378B) else ComposeColor(0xFF0E7490)
    val headerTextColor = if (isDark) ComposeColor(0xFFEADDFF) else ComposeColor.White

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
                    Text("هشدار", color = headerTextColor)
                }
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "برای خروج از برنامه مطمئن هستید؟",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = onCancel,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) { Text("انصراف") }
                        Button(
                            onClick = onConfirm,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ComposeColor(0xFFD32F2F)
                            )
                        ) { Text("خروج") }
                    }
                }
            }
        }
    }
}
