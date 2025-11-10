package com.oqba26.prayertimes.screens

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oqba26.prayertimes.ui.AppFonts
import com.oqba26.prayertimes.viewmodels.SettingsViewModel

private val topBarLightColor = Color(0xFF0E7490)
private val topBarDarkColor = Color(0xFF4F378B)
private val onTopBarDarkColor = Color(0xFFEADDFF)
private val onTopBarLightColor = Color.White

enum class PrayerTime(val id: String, val displayName: String) {
    Fajr("fajr", "نماز صبح"),
    Dhuhr("dhuhr", "نماز ظهر"),
    Asr("asr", "نماز عصر"),
    Maghrib("maghrib", "نماز مغرب"),
    Isha("isha", "نماز عشاء")
}

@Composable
fun SettingsRow(
    text: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
        content()
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinutesDropdown(
    label: String,
    selectedValue: Int,
    onValueChange: (Int) -> Unit,
    options: List<Int> = listOf(5, 10, 15, 20, 30)
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            readOnly = true,
            value = "$selectedValue دقیقه",
            onValueChange = {},
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            shape = RoundedCornerShape(8.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { selectionOption ->
                DropdownMenuItem(
                    text = { Text("$selectionOption دقیقه") },
                    onClick = {
                        onValueChange(selectionOption)
                        expanded = false
                    }
                )
            }
        }
    }
}


@Composable
fun ExpandableSettingCard(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "بستن" else "باز کردن"
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
                    content()
                }
            }
        }
    }
}


@Composable
private fun SettingsTopBar(
    onClose: () -> Unit,
    isDark: Boolean
) {
    val bg = if (isDark) topBarDarkColor else topBarLightColor
    val fg = if (isDark) onTopBarDarkColor else onTopBarLightColor

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .statusBarsPadding()
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.offset(y = 2.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت", tint = fg)
                }
                Text(
                    text = "تنظیمات",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f),
                    color = fg
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.S)
@Composable
private fun PermissionCard(
    permissionName: String,
    rationale: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(permissionName, style = MaterialTheme.typography.titleMedium)
            Text(rationale, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
            OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
                Text("اعطای دسترسی")
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.M)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onClose: () -> Unit,
    isDarkThemeActive: Boolean,
    // Display
    currentFontId: String,
    onSelectFont: (String) -> Unit,
    usePersianNumbers: Boolean,
    onUsePersianNumbersChange: (Boolean) -> Unit,
    is24HourFormat: Boolean,
    onIs24HourFormatChange: (Boolean) -> Unit,
    // Adhan
    adhanEnabled: Boolean,
    onAdhanEnabledChange: (Boolean) -> Unit,
    // Widget
    onAddWidget: () -> Unit,
    settingsViewModel: SettingsViewModel = viewModel(),
) {
    val allFonts = remember { AppFonts.catalog() }
    val selectableFonts = remember(allFonts) { allFonts.filter { it.id != "system" } }
    var fontMenuExpanded by remember { mutableStateOf(false) }
    val selectedFontLabel = remember(currentFontId, selectableFonts) {
        (selectableFonts.find { it.id == currentFontId } ?: selectableFonts.firstOrNull())?.label ?: "انتخاب نشده"
    }

    val context = LocalContext.current
    val alarmManager = ContextCompat.getSystemService(context, AlarmManager::class.java)
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    var hasDndAccess by remember { mutableStateOf(notificationManager.isNotificationPolicyAccessGranted) }
    val dndSettingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        hasDndAccess = notificationManager.isNotificationPolicyAccessGranted
    }

    var hasExactAlarmPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) alarmManager?.canScheduleExactAlarms() ?: false
            else true
        )
    }
    val exactAlarmSettingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasExactAlarmPermission = alarmManager?.canScheduleExactAlarms() ?: false
        }
    }

    var displaySectionExpanded by remember { mutableStateOf(false) }
    var fontSectionExpanded by remember { mutableStateOf(false) }
    var adhanSectionExpanded by remember { mutableStateOf(false) }
    var silentSectionExpanded by remember { mutableStateOf(false) }
    var iqamaSectionExpanded by remember { mutableStateOf(false) }
    var widgetSectionExpanded by remember { mutableStateOf(false) }

    val autoSilentEnabled by settingsViewModel.autoSilentEnabled.collectAsState()
    val prayerSilentEnabled by settingsViewModel.prayerSilentEnabled.collectAsState()
    val prayerMinutesBefore by settingsViewModel.prayerMinutesBefore.collectAsState()
    val prayerMinutesAfter by settingsViewModel.prayerMinutesAfter.collectAsState()
    val iqamaEnabled by settingsViewModel.iqamaEnabled.collectAsState()
    val minutesBeforeIqama by settingsViewModel.minutesBeforeIqama.collectAsState()

    Scaffold(
        topBar = { SettingsTopBar(onClose = onClose, isDark = isDarkThemeActive) },
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Adhan Settings
                ExpandableSettingCard(
                    title = "اذان",
                    expanded = adhanSectionExpanded,
                    onToggle = { adhanSectionExpanded = !adhanSectionExpanded }
                ) {
                    SettingsRow(text = "پخش اذان") {
                        Switch(checked = adhanEnabled, onCheckedChange = onAdhanEnabledChange)
                    }
                }

                // Auto Silent Settings
                ExpandableSettingCard(
                    title = "سایلنت خودکار",
                    expanded = silentSectionExpanded,
                    onToggle = { silentSectionExpanded = !silentSectionExpanded }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasExactAlarmPermission) {
                            PermissionCard(
                                permissionName = "دسترسی آلارم دقیق",
                                rationale = "برای عملکرد صحیح سایلنت خودکار، نیاز به دسترسی آلارم‌های دقیق است.",
                                onClick = { exactAlarmSettingsLauncher.launch(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)) }
                            )
                        }

                        SettingsRow(text = "فعال‌سازی کلی") {
                            Switch(checked = autoSilentEnabled, onCheckedChange = settingsViewModel::updateAutoSilentEnabled)
                        }
                        AnimatedVisibility(visible = autoSilentEnabled) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (!hasDndAccess) {
                                    PermissionCard(
                                        permissionName = "دسترسی 'مزاحم نشوید'",
                                        rationale = "برای عملکرد صحیح این قابلیت، نیاز به دسترسی 'مزاحم نشوید' (Do Not Disturb) است.",
                                        onClick = { dndSettingsLauncher.launch(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) }
                                    )
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                PrayerTime.entries.forEach { prayer ->
                                    val isEnabled = prayerSilentEnabled[prayer] ?: false
                                    val before = prayerMinutesBefore[prayer] ?: 10
                                    val after = prayerMinutesAfter[prayer] ?: 10

                                    Card(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            SettingsRow(text = prayer.displayName) {
                                                Switch(
                                                    checked = isEnabled,
                                                    onCheckedChange = { settingsViewModel.updatePrayerSilentEnabled(prayer, it) }
                                                )
                                            }
                                            AnimatedVisibility(visible = isEnabled) {
                                                Column {
                                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                                    MinutesDropdown(
                                                        label = "دقایق قبل",
                                                        selectedValue = before,
                                                        onValueChange = { settingsViewModel.updatePrayerMinutesBefore(prayer, it) }
                                                    )
                                                    MinutesDropdown(
                                                        label = "دقایق بعد",
                                                        selectedValue = after,
                                                        onValueChange = { settingsViewModel.updatePrayerMinutesAfter(prayer, it) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Iqama Settings
                ExpandableSettingCard(
                    title = "اعلان اقامه",
                    expanded = iqamaSectionExpanded,
                    onToggle = { iqamaSectionExpanded = !iqamaSectionExpanded }
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasExactAlarmPermission) {
                        PermissionCard(
                            permissionName = "دسترسی آلارم دقیق",
                            rationale = "برای دریافت اعلان اقامه، برنامه نیاز به دسترسی آلارم‌های دقیق دارد.",
                            onClick = { exactAlarmSettingsLauncher.launch(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)) }
                        )
                    }

                    SettingsRow(text = "فعال‌سازی اعلان اقامه") {
                        Switch(checked = iqamaEnabled, onCheckedChange = settingsViewModel::updateIqamaEnabled)
                    }
                    AnimatedVisibility(visible = iqamaEnabled) {
                        MinutesDropdown(
                            label = "زمان اعلان قبل از اقامه",
                            selectedValue = minutesBeforeIqama,
                            onValueChange = settingsViewModel::updateMinutesBeforeIqama
                        )
                    }
                }

                // Display settings
                ExpandableSettingCard(
                    title = "قالب نمایش",
                    expanded = displaySectionExpanded,
                    onToggle = { displaySectionExpanded = !displaySectionExpanded }
                ) {
                    SettingsRow(text = "نمایش اعداد فارسی") {
                        Switch(checked = usePersianNumbers, onCheckedChange = onUsePersianNumbersChange)
                    }
                    SettingsRow(text = "حالت ۲۴ ساعته") {
                        Switch(checked = is24HourFormat, onCheckedChange = onIs24HourFormatChange)
                    }
                }

                // App Font
                ExpandableSettingCard(
                    title = "فونت برنامه",
                    expanded = fontSectionExpanded,
                    onToggle = { fontSectionExpanded = !fontSectionExpanded }
                ) {
                    ExposedDropdownMenuBox(
                        expanded = fontMenuExpanded,
                        onExpandedChange = { fontMenuExpanded = !fontMenuExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            readOnly = true,
                            value = selectedFontLabel,
                            onValueChange = { },
                            label = { Text("فونت فعلی") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fontMenuExpanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            shape = RoundedCornerShape(8.dp),
                            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
                        )
                        ExposedDropdownMenu(
                            expanded = fontMenuExpanded,
                            onDismissRequest = { fontMenuExpanded = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            selectableFonts.forEach { fontEntry ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = fontEntry.label,
                                            fontFamily = fontEntry.family,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center
                                        )
                                    },
                                    onClick = {
                                        onSelectFont(fontEntry.id)
                                        fontMenuExpanded = false
                                    }

                                )
                            }
                        }
                    }
                }
                
                // Widget Settings
                ExpandableSettingCard(
                    title = "ویجت",
                    expanded = widgetSectionExpanded,
                    onToggle = { widgetSectionExpanded = !widgetSectionExpanded }
                ) {
                    OutlinedButton(
                        onClick = onAddWidget,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("افزودن ویجت به صفحه اصلی")
                    }
                }
            }
        }
    }
}
