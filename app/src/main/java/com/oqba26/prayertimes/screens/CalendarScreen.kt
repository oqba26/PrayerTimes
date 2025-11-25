@file:Suppress("AssignedValueIsNeverRead", "RemoveRedundantQualifierName")

package com.oqba26.prayertimes.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.oqba26.prayertimes.activities.AlarmActivity
import com.oqba26.prayertimes.activities.NoteEditorActivity
import com.oqba26.prayertimes.models.MultiDate
import com.oqba26.prayertimes.models.UserNote
import com.oqba26.prayertimes.screens.widgets.BottomBar
import com.oqba26.prayertimes.screens.widgets.DayHeaderBar
import com.oqba26.prayertimes.screens.widgets.MonthCalendarView
import com.oqba26.prayertimes.screens.widgets.NotesOverviewScreen
import com.oqba26.prayertimes.screens.widgets.PrayerTimesList
import com.oqba26.prayertimes.screens.widgets.ShamsiDatePicker
import com.oqba26.prayertimes.utils.DateUtils
import com.oqba26.prayertimes.utils.HolidayUtils
import com.oqba26.prayertimes.utils.NoteUtils
import com.oqba26.prayertimes.viewmodels.PrayerViewModel
import java.time.LocalDate
import java.time.Period
import java.time.LocalTime

val DarkThemePurpleBackground = Color(0xFF4F378B)
val DarkThemeOnPurpleText = Color(0xFFEADDFF)

enum class ViewMode { PRAYER_TIMES, NOTES }




@Composable
fun CalendarScreen(
    currentDate: MultiDate,
    uiState: PrayerViewModel.Result<Map<String, String>>,
    onDateChange: (MultiDate) -> Unit,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAlarms: () -> Unit,
    isDarkThemeActive: Boolean,
    onToggleTheme: () -> Unit,
    currentViewMode: ViewMode,
    onViewModeChange: (ViewMode) -> Unit,
    userNotesMap: Map<String, UserNote>,
    onAddNoteRequest: (selectedDateShamsi: String) -> Unit,
    onEditNoteRequest: (noteId: String, noteToEdit: UserNote) -> Unit,
    onDeleteNoteRequest: (noteId: String) -> Unit,
    usePersianNumbers: Boolean,
    use24HourFormat: Boolean
) {
    val context = LocalContext.current

    // به‌روزرسانی هر دقیقه برای هایلایت کردن وقت بعدی
    val currentTime = remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60_000)
            currentTime.value = LocalTime.now()
        }
    }

    val today = remember { DateUtils.getCurrentDate() }
    val diffLabel = remember(currentDate.shamsi, today.shamsi, usePersianNumbers) {
        buildRelativeDiffLabel(today, currentDate, usePersianNumbers)
    }

    val (shYear, shMonth, _) = currentDate.getShamsiParts()
    var monthHolidays by remember(shYear, shMonth) { mutableStateOf<Map<Int, List<String>>>(emptyMap()) }
    LaunchedEffect(shYear, shMonth) {
        monthHolidays = HolidayUtils.getMonthHolidays(context, shYear, shMonth)
    }

    var showShamsiDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            BottomBar(
                onOpenSettings = onOpenSettings,
                onOpenAlarms = onOpenAlarms,
                onOpenQibla = {
                    val intent = android.content.Intent(
                        context,
                        com.oqba26.prayertimes.activities.QiblaActivity::class.java
                    ).apply {
                        putExtra("IS_DARK", isDarkThemeActive)
                    }
                    context.startActivity(intent)
                },
                onToggleTheme = onToggleTheme,
                isDark = isDarkThemeActive,
                currentViewMode = currentViewMode,
                onViewModeChange = onViewModeChange
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // تاپ‌بار
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDarkThemeActive) DarkThemePurpleBackground else Color(0xFF0E7490))
                    .statusBarsPadding()
            ) {
                val topColor = if (isDarkThemeActive) DarkThemeOnPurpleText else Color.White

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // تقویم + اشتراک‌گذاری سمت چپ
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            if (currentDate.shamsi != today.shamsi) onDateChange(today) else showShamsiDatePicker = true
                        }) {
                            Icon(
                                imageVector = Icons.Filled.CalendarToday,
                                contentDescription = "انتخاب تاریخ",
                                tint = topColor
                            )
                        }

                        IconButton(
                            onClick = {
                                if (uiState is PrayerViewModel.Result.Success) {
                                    val textToShare = com.oqba26.prayertimes.utils.ShareUtils.buildShareText(
                                        currentDate,
                                        uiState.data,
                                        usePersianNumbers,
                                        use24HourFormat
                                    )
                                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(android.content.Intent.EXTRA_TEXT, textToShare)
                                    }
                                    context.startActivity(
                                        android.content.Intent.createChooser(shareIntent, "اشتراک گذاری با")
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = "اشتراک‌گذاری",
                                tint = topColor
                            )
                        }
                    }

                    // عنوان سمت راست
                    Text(
                        text = "تقویم و اوقات نماز",
                        color = topColor,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.End
                    )
                }

                if (diffLabel.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = diffLabel,
                            color = if (isDarkThemeActive) DarkThemeOnPurpleText else Color.White,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // تقویم ماه
            MonthCalendarView(
                selectedDate = currentDate,
                onDateChange = onDateChange,
                holidays = monthHolidays,
                isDark = isDarkThemeActive,
                usePersianNumbers = usePersianNumbers
            )

            // هدر روز
            DayHeaderBar(
                date = currentDate,
                onPreviousDay = { onDateChange(DateUtils.getPreviousDate(currentDate)) },
                onNextDay = { onDateChange(DateUtils.getNextDate(currentDate)) },
                isDark = isDarkThemeActive,
                usePersianNumbers = usePersianNumbers,
                use24HourFormat = use24HourFormat
            )

            // محتوای اصلی
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (currentViewMode) {
                    ViewMode.PRAYER_TIMES -> {
                        when (uiState) {
                            is PrayerViewModel.Result.Loading -> Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) { CircularProgressIndicator() }

                            is PrayerViewModel.Result.Error -> Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(uiState.message)
                                    TextButton(onClick = onRetry) { Text("تلاش مجدد") }
                                }
                            }

                            is PrayerViewModel.Result.Success -> PrayerTimesList(
                                prayerTimes = uiState.data,
                                modifier = Modifier.fillMaxSize(),
                                isDark = isDarkThemeActive,
                                usePersianNumbers = usePersianNumbers,
                                use24HourFormat = use24HourFormat,
                                contentPadding = innerPadding,
                                currentTime = currentTime.value
                            )
                        }
                    }

                    ViewMode.NOTES -> {
                        val noteKey = NoteUtils.formatDateToKey(currentDate)
                        val noteForSelectedDate = userNotesMap[noteKey]
                        NotesOverviewScreen(
                            selectedDate = currentDate,
                            userNote = noteForSelectedDate,
                            onAddNoteClick = { onAddNoteRequest(currentDate.shamsi) },
                            onEditNoteClick = {
                                if (noteForSelectedDate != null) {
                                    onEditNoteRequest(noteForSelectedDate.id, noteForSelectedDate)
                                }
                            },
                            onDeleteClick = {
                                if (noteForSelectedDate != null) {
                                    onDeleteNoteRequest(noteForSelectedDate.id)
                                }
                            },
                            isDarkThemeActive = isDarkThemeActive,
                            usePersianNumbers = usePersianNumbers,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = innerPadding.calculateBottomPadding())
                        )
                    }
                }
            }
        }
    }

    if (showShamsiDatePicker) {
        ShamsiDatePicker(
            initialDate = currentDate,
            onDateSelected = { onDateChange(it); showShamsiDatePicker = false },
            onDismiss = { showShamsiDatePicker = false },
            isDarkTheme = isDarkThemeActive,
            usePersianNumbers = usePersianNumbers,
            use24HourFormat = use24HourFormat
        )
    }
}

@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun CalendarScreenEntryPoint(
    viewModel: PrayerViewModel,
    onOpenSettings: () -> Unit,
    isDarkThemeActive: Boolean,
    onToggleTheme: () -> Unit,
    usePersianNumbers: Boolean,
    use24HourFormat: Boolean
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.loadData(context.applicationContext)
    }
    var currentDate by remember { mutableStateOf(DateUtils.getCurrentDate()) }
    val uiState = viewModel.uiState.collectAsState().value
    var currentViewMode by remember { mutableStateOf(ViewMode.PRAYER_TIMES) }
    var userNotesMapState by remember { mutableStateOf<Map<String, UserNote>>(emptyMap()) }

    var noteIdToDeleteDialog by remember { mutableStateOf<String?>(null) }
    var showExactAlarmPermissionDialog by remember { mutableStateOf(false) }
    var showXiaomiPermissionDialog by remember { mutableStateOf(false) }

    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }

    LaunchedEffect(Unit) {
        if (Build.MANUFACTURER.equals("xiaomi", ignoreCase = true)) {
            if (!prefs.getBoolean("shown_xiaomi_dialog", false)) {
                showXiaomiPermissionDialog = true
            }
        }
    }

    if (showXiaomiPermissionDialog) {
        XiaomiPermissionDialog(
            isDark = isDarkThemeActive,
            onDismiss = { showXiaomiPermissionDialog = false },
            onGoToSettings = {
                showXiaomiPermissionDialog = false
                prefs.edit { putBoolean("shown_xiaomi_dialog", true) }
                try {
                    // تلاش اول: مستقیم‌ترین Intent برای صفحه لیست مجوزها
                    val intent = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                        setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.AppPermissionsEditor")
                        putExtra("extra_pkgname", context.packageName)
                    }
                    context.startActivity(intent)
                } catch (e1: Exception) {
                    try {
                        // تلاش دوم: Fallback به Intent قبلی که صفحه «سایر مجوزها» را نشان می‌داد
                        val intent = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                             setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity")
                            putExtra("extra_pkgname", context.packageName)
                        }
                        context.startActivity(intent)
                    } catch (e2: Exception) {
                        // تلاش سوم: Fallback نهایی به صفحه استاندارد اندروید
                        try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        } catch (e3: Exception) {
                            // اگر هیچ‌کدام کار نکرد، حداقل یک Toast نشان بده
                            Toast.makeText(context, "لطفاً به صورت دستی به تنظیمات برنامه بروید", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        )
    }

    val noteEditorLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intentData = result.data ?: return@rememberLauncherForActivityResult
            val newNotesMap = userNotesMapState.toMutableMap()
            var noteModified = false
            var toastMessage: String? = null

            val deletedNoteId = intentData.getStringExtra(NoteEditorActivity.EXTRA_DELETED_NOTE_ID)
            if (deletedNoteId != null) {
                val noteToRemove = userNotesMapState.values.find { it.id == deletedNoteId }
                if(noteToRemove != null){
                    val dateParts = noteToRemove.id.split('-').map { it.toInt() }
                    val multiDate = DateUtils.createMultiDateFromShamsi(dateParts[0], dateParts[1], dateParts[2])
                    val noteKey = NoteUtils.formatDateToKey(multiDate)
                    NoteUtils.cancelNoteReminder(context, deletedNoteId)
                    newNotesMap.remove(noteKey)
                    noteModified = true
                    toastMessage = "یادداشت حذف شد"
                }
            }

            val savedNote: UserNote? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intentData.getParcelableExtra(NoteEditorActivity.EXTRA_SAVED_USER_NOTE, UserNote::class.java)
            } else {
                @Suppress("DEPRECATION")
                intentData.getParcelableExtra(NoteEditorActivity.EXTRA_SAVED_USER_NOTE)
            }

            if (savedNote != null) {
                NoteUtils.cancelNoteReminder(context, savedNote.id)
                val reminderScheduled = NoteUtils.scheduleNoteReminder(context, savedNote)

                if (!reminderScheduled) {
                    showExactAlarmPermissionDialog = true
                }
                val dateParts = savedNote.id.split('-').map { it.toInt() }
                val multiDate = DateUtils.createMultiDateFromShamsi(dateParts[0], dateParts[1], dateParts[2])
                val noteKey = NoteUtils.formatDateToKey(multiDate)
                newNotesMap[noteKey] = savedNote
                noteModified = true
                toastMessage = if (toastMessage == null) "یادداشت ذخیره شد" else "یادداشت منتقل و ذخیره شد"
            }

            if (noteModified) {
                userNotesMapState = newNotesMap.toMap()
                NoteUtils.saveNotesInternal(context, userNotesMapState)
                toastMessage?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
            }
        }
    }

    if (noteIdToDeleteDialog != null) {
        CustomConfirmDeleteDialog(
            isDark = isDarkThemeActive,
            onDismissRequest = { noteIdToDeleteDialog = null },
            onConfirmDelete = {
                val noteKey = noteIdToDeleteDialog!!
                val noteToRemove = userNotesMapState[noteKey]

                if (noteToRemove != null) {
                    NoteUtils.cancelNoteReminder(context, noteToRemove.id)
                    userNotesMapState = userNotesMapState.toMutableMap().apply { remove(noteKey) }.toMap()
                    NoteUtils.saveNotesInternal(context, userNotesMapState)
                    Toast.makeText(context, "یادداشت حذف شد", Toast.LENGTH_SHORT).show()
                }
                noteIdToDeleteDialog = null
            }
        )
    }

    if (showExactAlarmPermissionDialog) {
        ExactAlarmPermissionDialog(
            isDark = isDarkThemeActive,
            onDismiss = { showExactAlarmPermissionDialog = false },
            onGoToSettings = {
                showExactAlarmPermissionDialog = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    try {
                        context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        })
                    } catch (_: Exception) {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        })
                    }
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        userNotesMapState = NoteUtils.loadNotes(context)
    }

    LaunchedEffect(currentDate, usePersianNumbers, use24HourFormat) {
        viewModel.updateDate(currentDate)
    }

    CalendarScreen(
        currentDate = currentDate,
        uiState = uiState,
        onDateChange = { newDate -> currentDate = newDate },
        onRetry = { viewModel.updateDate(currentDate) },
        onOpenSettings = onOpenSettings,
        onOpenAlarms = { context.startActivity(Intent(context, AlarmActivity::class.java)) },
        isDarkThemeActive = isDarkThemeActive,
        onToggleTheme = onToggleTheme,
        currentViewMode = currentViewMode,
        onViewModeChange = { newMode -> currentViewMode = newMode },
        userNotesMap = userNotesMapState,
        onAddNoteRequest = { selectedDateShamsi ->
            noteEditorLauncher.launch(
                NoteEditorActivity.newIntent(
                    context,
                    selectedDateShamsi.replace('/', '-'),
                    null,
                    isDarkThemeActive,
                    usePersianNumbers,
                    use24HourFormat
                )
            )
        },
        onEditNoteRequest = { _, noteToEdit ->
            noteEditorLauncher.launch(
                NoteEditorActivity.newIntent(
                    context,
                    noteToEdit.id,
                    noteToEdit,
                    isDarkThemeActive,
                    usePersianNumbers,
                    use24HourFormat
                )
            )
        },
        onDeleteNoteRequest = {
            val noteKey = NoteUtils.formatDateToKey(currentDate)
            noteIdToDeleteDialog = noteKey
         },
        usePersianNumbers = usePersianNumbers,
        use24HourFormat = use24HourFormat
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomConfirmDeleteDialog(
    isDark: Boolean,
    onDismissRequest: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    val headerColor = if (isDark) Color(0xFF4F378B) else Color(0xFF0E7490)
    val headerTextColor = if (isDark) Color(0xFFEADDFF) else Color.White

    BasicAlertDialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(headerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text("تایید حذف", color = headerTextColor, style = MaterialTheme.typography.titleLarge)
                }

                Text(
                    "آیا از حذف این یادداشت اطمینان دارید؟",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Right,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
                    ) {
                        // انصراف (چپ) هم‌رنگ هدر
                        Button(
                            onClick = onDismissRequest,
                            modifier = Modifier.align(Alignment.CenterStart),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = headerColor,
                                contentColor = headerTextColor
                            )
                        ) { Text("انصراف") }

                        // تایید (راست) قرمز
                        Button(
                            onClick = onConfirmDelete,
                            modifier = Modifier.align(Alignment.CenterEnd),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) { Text("تایید") }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExactAlarmPermissionDialog(
    isDark: Boolean,
    onDismiss: () -> Unit,
    onGoToSettings: () -> Unit
) {
    val headerColor = if (isDark) Color(0xFF4F378B) else Color(0xFF0E7490)
    val headerTextColor = if (isDark) Color(0xFFEADDFF) else Color.White

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(headerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "نیاز به مجوز",
                        color = headerTextColor,
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Text(
                    "برای اینکه یادآوری‌ها به درستی کار کنند، برنامه به مجوز 'آلارم‌ها و یادآوری‌ها' نیاز دارد. لطفاً این مجوز را از تنظیمات برنامه فعال کنید.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textAlign = TextAlign.Right
                )

                androidx.compose.runtime.CompositionLocalProvider(
                    LocalLayoutDirection provides LayoutDirection.Ltr
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
                    ) {
                        // رد کردن (چپ) هم‌رنگ هدر
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.align(Alignment.CenterStart),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = headerColor,
                                contentColor = headerTextColor
                            )
                        ) { Text("رد کردن") }

                        // رفتن به تنظیمات (راست)
                        Button(
                            onClick = onGoToSettings,
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) { Text("رفتن به تنظیمات") }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XiaomiPermissionDialog(
    isDark: Boolean,
    onDismiss: () -> Unit,
    onGoToSettings: () -> Unit
) {
    val headerColor = if (isDark) Color(0xFF4F378B) else Color(0xFF0E7490)
    val headerTextColor = if (isDark) Color(0xFFEADDFF) else Color.White

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(headerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "مجوز مخصوص گوشی‌های شیائومی",
                        color = headerTextColor,
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Text(
                    text = "برای اینکه زنگ هشدار به درستی روی صفحه قفل نمایش داده شود، لطفاً در صفحه بعد، مجوز «نمایش روی صفحه قفل» (Display on Lock screen) را فعال کنید.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textAlign = TextAlign.Right
                )

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
                    ) {
                        // چپ: «باشه» با بک‌گراند هم‌رنگ هدر
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.align(Alignment.CenterStart),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = headerColor,
                                contentColor = headerTextColor
                            )
                        ) { Text("باشه") }

                        // راست: رفتن به تنظیمات
                        Button(
                            onClick = onGoToSettings,
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) { Text("رفتن به تنظیمات") }
                    }
                }
            }
        }
    }
}

private fun buildRelativeDiffLabel(
    today: MultiDate,
    currentDate: MultiDate,
    usePersianNumbers: Boolean
): String {
    if (today.shamsi == currentDate.shamsi) return "امروز"

    val (tY, tM, tD) = today.getShamsiParts()
    val (cY, cM, cD) = currentDate.getShamsiParts()

    val isAfter = when {
        cY != tY -> cY > tY
        cM != tM -> cM > tM
        else -> cD > tD
    }

    val start = if (isAfter) LocalDate.of(tY, tM, tD) else LocalDate.of(cY, cM, cD)
    val end = if (isAfter) LocalDate.of(cY, cM, cD) else LocalDate.of(tY, tM, tD)
    val diff = Period.between(start, end)

    val num: (Int) -> String = { n ->
        val s = n.toString()
        DateUtils.convertToPersianNumbers(s, usePersianNumbers)
    }

    val nNBSP = ' '
    val parts = mutableListOf<String>()
    if (diff.years != 0) parts.add("${num(diff.years)}${nNBSP}سال")
    if (diff.months != 0) parts.add("${num(diff.months)}${nNBSP}ماه")
    if (diff.days != 0) parts.add("${num(diff.days)}${nNBSP}روز")
    if (parts.isEmpty()) return "امروز"

    val suffix = if (isAfter) "بعد" else "قبل"
    val rLI = '⁧'
    val pDI = '⁩'
    val nBSP = ' '
    return "${rLI}${parts.joinToString(" و ")}$nBSP$suffix$pDI"

}
