package com.oqba26.prayertimes.activities

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.oqba26.prayertimes.models.MultiDate
import com.oqba26.prayertimes.models.UserNote
import com.oqba26.prayertimes.screens.widgets.CreateEditNoteScreen
import com.oqba26.prayertimes.theme.PrayerTimesTheme
import com.oqba26.prayertimes.ui.AppFonts
import com.oqba26.prayertimes.utils.DateUtils
import com.oqba26.prayertimes.utils.NoteUtils

class NoteEditorActivity : ComponentActivity() {

    private var initialShamsiDateString: String? = null
    private var existingUserNote: UserNote? = null
    private lateinit var initialNoteDate: MultiDate

    companion object {
        const val EXTRA_SHAMSI_DATE_STRING = "EXTRA_SHAMSI_DATE_STRING"
        const val EXTRA_EXISTING_USER_NOTE = "EXTRA_EXISTING_USER_NOTE"
        const val EXTRA_SAVED_USER_NOTE = "EXTRA_SAVED_USER_NOTE"
        const val EXTRA_DELETED_NOTE_ID = "EXTRA_DELETED_NOTE_ID"
        const val EXTRA_IS_DARK_THEME = "EXTRA_IS_DARK_THEME"

        fun newIntent(context: Context, shamsiDate: String, existingNote: UserNote?, isDark: Boolean): Intent {
            return Intent(context, NoteEditorActivity::class.java).apply {
                putExtra(EXTRA_SHAMSI_DATE_STRING, shamsiDate)
                putExtra(EXTRA_EXISTING_USER_NOTE, existingNote)
                putExtra(EXTRA_IS_DARK_THEME, isDark)
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Edge-to-edge مثل Settings
        window.apply {
            WindowCompat.setDecorFitsSystemWindows(this, false)

            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT

            val insets = WindowCompat.getInsetsController(window, window.decorView)
            // پس‌زمینه تاپ‌بار تیره است => آیکن‌های استاتوس‌بار روشن بمونه
            insets.isAppearanceLightStatusBars = false
            // پس‌زمینه نویگیشن سفید/روشنه => آیکن‌های نویگیشن تیره بشن
            insets.isAppearanceLightNavigationBars = true

            // ف allback برای API < 27 (قابلیت آیکن تیره ندارن)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
                // رنگ نویگیشن رو تیره کن تا آیکن‌های سفید دیده بشن
                window.navigationBarColor = 0xFF111111.toInt() // یا Color.BLACK
            }

            @Suppress("DEPRECATION")
            statusBarColor = android.graphics.Color.TRANSPARENT
        }
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false

        initialShamsiDateString = intent.getStringExtra(EXTRA_SHAMSI_DATE_STRING)
        existingUserNote = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_EXISTING_USER_NOTE, UserNote::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_EXISTING_USER_NOTE)
        }

        if (initialShamsiDateString == null) {
            finishWithResult(RESULT_CANCELED)
            return
        }
        try {
            val noteIdToParse = existingUserNote?.id ?: initialShamsiDateString!!
            val parts = noteIdToParse.split("-").map { it.toInt() }
            initialNoteDate = DateUtils.createMultiDateFromShamsi(parts[0], parts[1], parts[2])
        } catch (e: Exception) {
            Log.e("NoteEditorActivity", "Error parsing date string", e)
            finishWithResult(RESULT_CANCELED)
            return
        }

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val fontId = prefs.getString("fontId", "vazirmatn") ?: "vazirmatn"
        val appFontFamily = AppFonts.familyFor(fontId)
        val isDarkFromIntent = intent.getBooleanExtra(EXTRA_IS_DARK_THEME, false)
        val usePersianNumbers = prefs.getBoolean("use_persian_numbers", true)
        val use24HourFormat = prefs.getBoolean("use_24_hour_format", true)

        DateUtils.setDefaultUsePersianNumbers(usePersianNumbers)

        setContent {
            PrayerTimesTheme(darkTheme = isDarkFromIntent, appFontFamily = appFontFamily) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    CreateEditNoteScreen(
                        selectedDate = initialNoteDate,
                        initialNoteContent = existingUserNote?.content ?: "",
                        isNotificationEnabledInitial = existingUserNote?.notificationEnabled ?: false,
                        reminderTimeInitial = existingUserNote?.reminderTimeMillis,
                        isDark = isDarkFromIntent,
                        usePersianNumbers = usePersianNumbers,
                        use24HourFormat = use24HourFormat,
                        onBackClick = { finishWithResult(RESULT_CANCELED) },
                        onSaveClick = { noteDate, content, notificationEnabled, reminderTimeMillis ->
                            val noteId = NoteUtils.formatDateToKey(noteDate)
                            val resultIntent = Intent()

                            if (existingUserNote != null && existingUserNote!!.id != noteId) {
                                resultIntent.putExtra(EXTRA_DELETED_NOTE_ID, existingUserNote!!.id)
                            }

                            if (content.isBlank()) {
                                if (existingUserNote != null) {
                                    resultIntent.putExtra(EXTRA_DELETED_NOTE_ID, existingUserNote!!.id)
                                }
                                finishWithResult(RESULT_OK, resultIntent)
                            } else {
                                val savedUserNote = UserNote(
                                    id = noteId,
                                    content = content,
                                    notificationEnabled = notificationEnabled,
                                    reminderTimeMillis = reminderTimeMillis,
                                    createdAt = existingUserNote?.createdAt ?: System.currentTimeMillis(),
                                    updatedAt = System.currentTimeMillis()
                                )
                                resultIntent.putExtra(EXTRA_SAVED_USER_NOTE, savedUserNote)
                                finishWithResult(RESULT_OK, resultIntent)
                            }
                        },
                        onDeleteClick = {
                            if (existingUserNote != null) {
                                val resultIntent = Intent().apply {
                                    putExtra(EXTRA_DELETED_NOTE_ID, existingUserNote!!.id)
                                }
                                finishWithResult(RESULT_OK, resultIntent)
                            }
                        }
                    )
                }
            }
        }
    }

    private fun finishWithResult(resultCode: Int, data: Intent? = null) {
        setResult(resultCode, data)
        finish()
    }
}