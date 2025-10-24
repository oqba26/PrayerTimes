package com.oqba26.prayertimes.screens

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
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.unit.sp
import com.oqba26.prayertimes.ui.AppFonts

private val topBarLightColor = Color(0xFF0E7490)
private val topBarDarkColor = Color(0xFF4F378B)
private val onTopBarDarkColor = Color(0xFFEADDFF)
private val onTopBarLightColor = Color.White

enum class PrayerTime(val displayName: String) {
    Fajr("نماز صبح"),
    Dhuhr("نماز ظهر"),
    Asr("نماز عصر"),
    Maghrib("نماز مغرب"),
    Isha("نماز عشاء")
}

@Composable
fun PrayerTimeSettings(
    prayerTime: PrayerTime,
    isSilentEnabled: Boolean,
    onSilentEnabledChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = prayerTime.displayName, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = isSilentEnabled, onCheckedChange = onSilentEnabledChange)
    }
}

@Composable
private fun SettingsTopBar(
    onClose: () -> Unit,
    isDark: Boolean
) {
    val bg = if (isDark) topBarDarkColor else topBarLightColor
    val fg = if (isDark) onTopBarDarkColor else onTopBarLightColor

    // مهم: اول background سپس statusBarsPadding تا رنگ تا لبه‌ی بالا کشیده شود
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

@RequiresApi(Build.VERSION_CODES.M)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentFontId: String,
    onSelectFont: (String) -> Unit,
    usePersianNumbers: Boolean,
    onUsePersianNumbersChange: (Boolean) -> Unit,
    use24HourFormat: Boolean,
    onUse24HourFormatChange: (Boolean) -> Unit,
    adhanEnabled: Boolean,
    onAdhanEnabledChange: (Boolean) -> Unit,
    _onStopAdhan: () -> Unit,
    onClose: () -> Unit,
    isDarkThemeActive: Boolean,
    prayerSilentSettings: Map<PrayerTime, Boolean>,
    onPrayerSilentSettingChange: (PrayerTime, Boolean) -> Unit
) {
    val allFonts = remember { AppFonts.catalog() }
    val selectableFonts = remember(allFonts) { allFonts.filter { it.id != "system" } }
    var fontMenuExpanded by remember { mutableStateOf(false) }
    val selectedFontLabel = remember(currentFontId, selectableFonts) {
        (selectableFonts.find { it.id == currentFontId } ?: selectableFonts.firstOrNull())?.label ?: "انتخاب نشده"
    }

    val context = LocalContext.current
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    var hasDndAccess by remember { mutableStateOf(notificationManager.isNotificationPolicyAccessGranted) }

    val dndSettingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        hasDndAccess = notificationManager.isNotificationPolicyAccessGranted
    }

    // کنترل کامل Insets: بالا را خودمان با تاپ‌بار هندل می‌کنیم؛ پایین را با navigationBarsPadding
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
                    .navigationBarsPadding()  // جلوگیری از رفتن محتوا زیر Navigation Bar
                    .padding(16.dp)
            ) {
                // Display settings
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "قالب نمایش",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Right
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "نمایش اعداد فارسی", style = MaterialTheme.typography.bodyLarge)
                            Switch(checked = usePersianNumbers, onCheckedChange = onUsePersianNumbersChange)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "حالت ۲۴ ساعته", style = MaterialTheme.typography.bodyLarge)
                            Switch(checked = use24HourFormat, onCheckedChange = onUse24HourFormatChange)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // App Font
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "فونت برنامه",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Right
                        )
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
                                        },
                                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Adhan
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "اذان",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Right
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "پخش خودکار اذان سرِ وقت", style = MaterialTheme.typography.bodyLarge)
                            Switch(checked = adhanEnabled, onCheckedChange = onAdhanEnabledChange)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Auto silent during prayer
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "سکوت خودکار هنگام نماز",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Right
                        )

                        if (!hasDndAccess) {
                            Text(
                                text = "برای فعال‌سازی این قابلیت، نیاز به دسترسی \"مزاحم نشوید\" است.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Button(onClick = {
                                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                dndSettingsLauncher.launch(intent)
                            }) {
                                Text("اعطای دسترسی")
                            }
                        } else {
                            Text(
                                text = "دسترسی \"مزاحم نشوید\" فعال است.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        PrayerTime.entries.forEach { prayerTime ->
                            val isEnabled = prayerSilentSettings[prayerTime] ?: false
                            PrayerTimeSettings(
                                prayerTime = prayerTime,
                                isSilentEnabled = isEnabled,
                                onSilentEnabledChange = { newIsEnabled ->
                                    onPrayerSilentSettingChange(prayerTime, newIsEnabled)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // About section
                var aboutExpanded by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .clickable { aboutExpanded = !aboutExpanded }
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "درباره برنامه",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Icon(
                                imageVector = if (aboutExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = if (aboutExpanded) "بستن" else "باز کردن",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        AnimatedVisibility(visible = aboutExpanded) {
                            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                                val aboutText = "اوقات شرعی یک دستیار هوشمند و زیبا برای نیازهای روزانه شماست:\n\n" +
                                        "• تقویم و اوقات شرعی: نمایش دقیق اوقات شرعی، تقویم ماهانه، مناسبت‌ها و تعطیلات رسمی.\n" +
                                        "• اذان و نوتیفیکیشن: پخش خودکار اذان، نوتیفیکیشن دائمی برای دسترسی سریع و اطلاع از وقت بعدی.\n" +
                                        "• زنگ هشدار (آلارم): ساخت هشدارهای متعدد با قابلیت تکرار، انتخاب زنگ دلخواه، لرزش و تعویق.\n" +
                                        "• یادداشت‌ها: ثبت یادداشت‌های روزانه با قابلیت تنظیم یادآور و زمان‌بندی.\n" +
                                        "• شخصی‌سازی: ویجت‌های متنوع، تم روشن و تیره، فونت‌های زیبا، فرمت ۲۴ ساعته و اعداد فارسی.\n" +
                                        "• ناوبری آسان: امکان پرش به تاریخ دلخواه و بازگشت سریع به روز جاری."

                                Text(
                                    text = aboutText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Right,
                                    lineHeight = 24.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}