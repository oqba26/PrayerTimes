package com.oqba26.prayertimes.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
                currentDate = currentDate,
                prayers = if (uiState is PrayerViewModel.Result.Success) uiState.data else emptyMap(),
                onOpenSettings = onOpenSettings,
                onOpenAlarms = onOpenAlarms,
                onToggleTheme = onToggleTheme,
                isDark = isDarkThemeActive,
                currentViewMode = currentViewMode,
                onViewModeChange = onViewModeChange,
                usePersianNumbers = usePersianNumbers,
                use24HourFormat = use24HourFormat
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
                    IconButton(onClick = {
                        if (currentDate.shamsi != today.shamsi) onDateChange(today) else showShamsiDatePicker = true
                    }) {
                        Icon(
                            imageVector = Icons.Filled.CalendarToday,
                            contentDescription = "تقویم",
                            tint = topColor
                        )
                    }
                    Text(
                        text = "تقویم و اوقات نماز",
                        color = topColor,
                        style = MaterialTheme.typography.titleLarge
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

            // The padding is now passed to the PrayerTimesList directly
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
                                contentPadding = innerPadding
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
                            usePersianNumbers = usePersianNumbers
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
            onDismiss = { showXiaomiPermissionDialog = false },
            onGoToSettings = {
                showXiaomiPermissionDialog = false
                prefs.edit { putBoolean("shown_xiaomi_dialog", true) }
                try {
                    val intent = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                        setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.AppPermissionsEditor")
                        putExtra("extra_pkgname", context.packageName)
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        // ignore
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
                NoteUtils.cancelNoteReminder(context, deletedNoteId)
                newNotesMap.remove(deletedNoteId)
                noteModified = true
                toastMessage = "یادداشت حذف شد"
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

                newNotesMap[savedNote.id] = savedNote
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
            onDismissRequest = { noteIdToDeleteDialog = null },
            onConfirmDelete = {
                val noteId = noteIdToDeleteDialog!!
                NoteUtils.cancelNoteReminder(context, noteId)
                userNotesMapState = userNotesMapState.toMutableMap().apply { remove(noteId) }.toMap()
                NoteUtils.saveNotesInternal(context, userNotesMapState)
                Toast.makeText(context, "یادداشت حذف شد", Toast.LENGTH_SHORT).show()
                noteIdToDeleteDialog = null
            }
        )
    }

    if (showExactAlarmPermissionDialog) {
        ExactAlarmPermissionDialog(
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
                    isDarkThemeActive
                )
            )
        },
        onEditNoteRequest = { noteId, noteToEdit ->
            noteEditorLauncher.launch(
                NoteEditorActivity.newIntent(
                    context,
                    noteId,
                    noteToEdit,
                    isDarkThemeActive
                )
            )
        },
        onDeleteNoteRequest = { noteId -> noteIdToDeleteDialog = noteId },
        usePersianNumbers = usePersianNumbers,
        use24HourFormat = use24HourFormat
    )
}

@Composable
fun CustomConfirmDeleteDialog(onDismissRequest: () -> Unit, onConfirmDelete: () -> Unit) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "تایید حذف",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    textAlign = TextAlign.Right
                )
                Text(
                    "آیا از حذف این یادداشت اطمینان دارید؟",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    textAlign = TextAlign.Right
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(onClick = onDismissRequest) { Text("انصراف") }
                    Button(onClick = onConfirmDelete) { Text("تایید") }
                }
            }
        }
    }
}

@Composable
fun ExactAlarmPermissionDialog(onDismiss: () -> Unit, onGoToSettings: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "نیاز به مجوز",
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                "برای اینکه یادآوری‌ها به درستی کار کنند، برنامه به مجوز \"آلارم‌ها و یادآوری‌ها\" نیاز دارد. لطفاً این مجوز را از تنظیمات برنامه فعال کنید.",
                textAlign = TextAlign.Right
            )
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onDismiss) { Text("رد کردن") }
                TextButton(onClick = onGoToSettings) { Text("رفتن به تنظیمات") }
            }
        }
    )
}

@Composable
fun XiaomiPermissionDialog(onDismiss: () -> Unit, onGoToSettings: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "مجوز مخصوص گوشی‌های شیائومی",
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                "برای اینکه زنگ هشدار به درستی روی صفحه قفل نمایش داده شود، لطفاً در صفحه بعد، مجوز «نمایش روی صفحه قفل» (Display on Lock screen) را فعال کنید.",
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onDismiss) { Text("باشه") }
                TextButton(onClick = onGoToSettings) { Text("رفتن به تنظیمات") }
            }
        }
    )
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
        if (usePersianNumbers) DateUtils.convertToPersianNumbers(s) else s
    }

    val nNBSP = '\u202F'
    val parts = mutableListOf<String>()
    if (diff.years != 0) parts.add("${num(diff.years)}${nNBSP}سال")
    if (diff.months != 0) parts.add("${num(diff.months)}${nNBSP}ماه")
    if (diff.days != 0) parts.add("${num(diff.days)}${nNBSP}روز")
    if (parts.isEmpty()) return "امروز"

    val suffix = if (isAfter) "بعد" else "قبل"
    val rLI = '\u2067'
    val pDI = '\u2069'
    val nBSP = '\u00A0'
    return "${rLI}${parts.joinToString(" و ")}$nBSP$suffix$pDI"

}
