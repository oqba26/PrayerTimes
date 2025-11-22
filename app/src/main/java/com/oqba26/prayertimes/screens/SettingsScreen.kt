@file:Suppress("AssignedValueIsNeverRead")

package com.oqba26.prayertimes.screens

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oqba26.prayertimes.R
import com.oqba26.prayertimes.ui.AppFonts
import com.oqba26.prayertimes.utils.DateUtils
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

@Composable
fun ExpandableSettingCard(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(16.dp),
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
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 4.dp))
                    content()
                }
            }
        }
    }
}

@Composable
fun SwitchSettingRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = { onCheckedChange(!checked) })
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

@Composable
fun ClickableSettingRow(
    title: String,
    value: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

@SuppressLint("UnusedValue")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdhanSoundDropdown(
    label: String,
    selectedValue: String,
    onValueChange: (String) -> Unit,
    options: List<Pair<String, String>>
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = remember(selectedValue, options) {
        options.find { it.first == selectedValue }?.second ?: "خاموش"
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            readOnly = true,
            value = selectedLabel,
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
            options.forEach { (soundId, soundLabel) ->
                DropdownMenuItem(
                    text = { Text(soundLabel) },
                    onClick = {
                        onValueChange(soundId)
                        expanded = false
                    }
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.S)
@Composable
private fun PermissionCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("دسترسی آلارم دقیق", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
            Text(
                "برای عملکرد صحیح سکوت خودکار، نیاز به دسترسی آلارم‌های دقیق است.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
            )
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("اعطای دسترسی")
            }
        }
    }
}

@Composable
fun FontSelectorDialog(
    currentFontId: String,
    onDismiss: () -> Unit,
    onSelectFont: (String) -> Unit
) {
    val fonts = AppFonts.catalog()
    val (selected, onSelected) = remember { mutableStateOf(currentFontId) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("انتخاب فونت", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
                Column(Modifier.selectableGroup()) {
                    fonts.forEach { font ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .selectable(
                                    selected = (font.id == selected),
                                    onClick = { onSelected(font.id) },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (font.id == selected),
                                onClick = null
                            )
                            Text(
                                text = font.label,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("انصراف")
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        onSelectFont(selected)
                        onDismiss()
                    }) {
                        Text("تایید")
                    }
                }
            }
        }
    }
}


@Composable
private fun IqamaMinuteSelector(
    selectedValue: Int,
    onValueChange: (Int) -> Unit,
    usePersianNumbers: Boolean
) {
    val minutes = (0..20).toList()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val currentMinute = selectedValue.unaryMinus()
        val label = when (currentMinute) {
            0 -> "همزمان با نماز"
            else -> "${DateUtils.convertToPersianNumbers(currentMinute.toString(), usePersianNumbers)} دقیقه مانده به نماز"
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(minutes) { minute ->
                val isSelected = minute == currentMinute
                val backgroundColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest)
                val contentColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(backgroundColor)
                        .clickable { onValueChange(-minute) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = DateUtils.convertToPersianNumbers(minute.toString(), usePersianNumbers),
                        color = contentColor,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
fun MinuteChipSelector(
    label: String,
    selectedValue: Int,
    range: IntProgression,
    onValueChange: (Int) -> Unit,
    usePersianNumbers: Boolean
) {
    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(range.toList()) { minute ->
                val isSelected = minute == selectedValue
                val backgroundColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest)
                val contentColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(backgroundColor)
                        .clickable { onValueChange(minute) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = DateUtils.convertToPersianNumbers(minute.toString(), usePersianNumbers),
                        color = contentColor,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}


@SuppressLint("UnusedValue")
@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun SettingsScreen(
    onClose: () -> Unit,
    onAddWidget: () -> Unit,
    isDarkThemeActive: Boolean,
    currentFontId: String,
    onSelectFont: (String) -> Unit,
    usePersianNumbers: Boolean,
    onUsePersianNumbersChange: (Boolean) -> Unit,
    is24HourFormat: Boolean,
    onIs24HourFormatChange: (Boolean) -> Unit,
    settingsViewModel: SettingsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val alarmManager = ContextCompat.getSystemService(context, AlarmManager::class.java)

    var hasExactAlarmPermission by remember { mutableStateOf(alarmManager?.canScheduleExactAlarms() ?: true) }
    val exactAlarmSettingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        hasExactAlarmPermission = alarmManager?.canScheduleExactAlarms() ?: true
    }

    var generalExpanded by remember { mutableStateOf(false) }
    var displayExpanded by remember { mutableStateOf(false) }
    var adhanExpanded by remember { mutableStateOf(false) }
    var silentExpanded by remember { mutableStateOf(false) }
    var iqamaExpanded by remember { mutableStateOf(false) }
    var showFontDialog by remember { mutableStateOf(false) }

    val autoSilentEnabled by settingsViewModel.autoSilentEnabled.collectAsState()
    val prayerSilentEnabled by settingsViewModel.prayerSilentEnabled.collectAsState()
    val prayerMinutesBefore by settingsViewModel.prayerMinutesBefore.collectAsState()
    val prayerMinutesAfter by settingsViewModel.prayerMinutesAfter.collectAsState()
    val iqamaEnabled by settingsViewModel.iqamaEnabled.collectAsState()
    val minutesBeforeIqama by settingsViewModel.minutesBeforeIqama.collectAsState()

    if (showFontDialog) {
        FontSelectorDialog(
            currentFontId = currentFontId,
            onDismiss = { showFontDialog = false },
            onSelectFont = onSelectFont
        )
    }

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
                if (!hasExactAlarmPermission) {
                    PermissionCard(onClick = { exactAlarmSettingsLauncher.launch(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)) })
                }

                ExpandableSettingCard(
                    title = "عمومی",
                    expanded = generalExpanded,
                    onToggle = { generalExpanded = !generalExpanded }
                ) {
                    ClickableSettingRow(
                        title = "افزودن ویجت به صفحه اصلی",
                        onClick = onAddWidget
                    )
                }

                ExpandableSettingCard(
                    title = "نمایش",
                    expanded = displayExpanded,
                    onToggle = { displayExpanded = !displayExpanded }
                ) {
                    val fontName = AppFonts.catalog().find { it.id == currentFontId }?.label ?: ""
                    ClickableSettingRow(
                        title = "فونت برنامه",
                        value = fontName,
                        onClick = { showFontDialog = true }
                    )
                    HorizontalDivider()
                    SwitchSettingRow(
                        title = "اعداد فارسی",
                        subtitle = "نمایش تمام اعداد در برنامه با فرمت فارسی",
                        checked = usePersianNumbers,
                        onCheckedChange = onUsePersianNumbersChange
                    )
                    HorizontalDivider()
                    SwitchSettingRow(
                        title = "فرمت ۲۴ ساعته",
                        subtitle = "نمایش زمان با فرمت ۲۴ ساعته (مثال: ۱۷:۳۰)",
                        checked = is24HourFormat,
                        onCheckedChange = onIs24HourFormatChange
                    )
                }

                ExpandableSettingCard(
                    title = "اذان",
                    expanded = adhanExpanded,
                    onToggle = { adhanExpanded = !adhanExpanded }
                ) {
                    val adhanLabels = stringArrayResource(R.array.adhan_sound_labels)
                    val adhanIds = stringArrayResource(R.array.adhan_sound_ids)
                    val adhanSounds = remember(adhanLabels, adhanIds) {
                        adhanIds.zip(adhanLabels).toList()
                    }
                    Text(
                        "برای هر نماز، صدای اذان مورد نظر را انتخاب کنید. با انتخاب هر صدا، زمان اذان به صورت خودکار زمان‌بندی می‌شود.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    PrayerTime.entries.forEach { prayer ->
                        val currentSound by settingsViewModel.getAdhanSoundFlow(prayer.id).collectAsState("off")
                        AdhanSoundDropdown(
                            label = prayer.displayName,
                            selectedValue = currentSound,
                            onValueChange = { sound -> settingsViewModel.setAdhanSound(prayer.id, sound) },
                            options = adhanSounds
                        )
                    }
                }

                ExpandableSettingCard(
                    title = "اقامه",
                    expanded = iqamaExpanded,
                    onToggle = { iqamaExpanded = !iqamaExpanded }
                ) {
                    SwitchSettingRow(
                        title = "فعال کردن اعلان اقامه",
                        subtitle = "یک اعلان برای یادآوری اقامه نماز دریافت کنید",
                        checked = iqamaEnabled,
                        onCheckedChange = { settingsViewModel.updateIqamaEnabled(it) }
                    )
                    AnimatedVisibility(iqamaEnabled) {
                        IqamaMinuteSelector(
                            selectedValue = minutesBeforeIqama,
                            onValueChange = { settingsViewModel.updateMinutesBeforeIqama(it) },
                            usePersianNumbers = usePersianNumbers
                        )
                    }
                }


                ExpandableSettingCard(
                    title = "سکوت خودکار",
                    expanded = silentExpanded,
                    onToggle = { silentExpanded = !silentExpanded }
                ) {
                    SwitchSettingRow(
                        title = "فعال کردن سکوت خودکار",
                        subtitle = "گوشی در زمان‌های مشخص شده برای نمازها به حالت سکوت می‌رود",
                        checked = autoSilentEnabled,
                        onCheckedChange = { settingsViewModel.updateAutoSilentEnabled(it) },
                        enabled = hasExactAlarmPermission
                    )

                    AnimatedVisibility(visible = autoSilentEnabled && hasExactAlarmPermission) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            PrayerTime.entries.forEach { prayer ->
                                val isPrayerSilentEnabled = prayerSilentEnabled[prayer] ?: false
                                val before = prayerMinutesBefore[prayer] ?: 10
                                val after = prayerMinutesAfter[prayer] ?: 10

                                SwitchSettingRow(
                                    title = prayer.displayName,
                                    checked = isPrayerSilentEnabled,
                                    onCheckedChange = { settingsViewModel.updatePrayerSilentEnabled(prayer, it) }
                                )
                                AnimatedVisibility(isPrayerSilentEnabled) {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        MinuteChipSelector(
                                            label = "شروع سکوت (دقیقه قبل از نماز)",
                                            selectedValue = before,
                                            range = (0..30),
                                            onValueChange = { v -> settingsViewModel.updatePrayerMinutesBefore(prayer, v) },
                                            usePersianNumbers = usePersianNumbers
                                        )
                                        MinuteChipSelector(
                                            label = "پایان سکوت (دقیقه بعد از نماز)",
                                            selectedValue = after,
                                            range = (0..60 step 5),
                                            onValueChange = { v -> settingsViewModel.updatePrayerMinutesAfter(prayer, v) },
                                            usePersianNumbers = usePersianNumbers
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
}
