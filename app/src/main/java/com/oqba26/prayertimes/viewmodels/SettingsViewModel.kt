@file:Suppress("unused", "PrivatePropertyName")

package com.oqba26.prayertimes.viewmodels

import android.annotation.SuppressLint
import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.oqba26.prayertimes.PrayerTime
import com.oqba26.prayertimes.adhan.AdhanScheduler
import com.oqba26.prayertimes.services.PrayerForegroundService
import com.oqba26.prayertimes.widget.LargeModernWidgetProvider
import com.oqba26.prayertimes.widget.ModernWidgetProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    @SuppressLint("StaticFieldLeak")
    private val context = getApplication<Application>().applicationContext

    // --- ثابت‌ها ---
    private val DEFAULT_IQAMA_TEXT = "اکنون زمان اقامه {prayer} است."

    // --- Preference Keys ---
    private val DND_ENABLED = booleanPreferencesKey("dnd_enabled")
    private val DND_START_TIME = stringPreferencesKey("dnd_start_time")
    private val DND_END_TIME = stringPreferencesKey("dnd_end_time")
    private val SHOW_GENERAL_TIMES = booleanPreferencesKey("show_general_times")
    private val SHOW_SPECIFIC_TIMES = booleanPreferencesKey("show_specific_times")
    private val THEME_ID = stringPreferencesKey("themeId")
    private val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
    private val FONT_ID = stringPreferencesKey("fontId")
    private val AUTO_SILENT_ENABLED = booleanPreferencesKey("auto_silent_enabled")
    private val MINUTES_BEFORE_IQAMA = intPreferencesKey("minutes_before_iqama")
    private val IQAMA_NOTIFICATION_TEXT = stringPreferencesKey("iqama_notification_text")
    private val USE_24_HOUR_FORMAT = booleanPreferencesKey("use_24_hour_format")
    private val USE_PERSIAN_NUMBERS = booleanPreferencesKey("use_persian_numbers")
    private val USE_NUMERIC_DATE_FORMAT_MAIN =
        booleanPreferencesKey("use_numeric_date_format_main")
    private val USE_NUMERIC_DATE_FORMAT_WIDGET =
        booleanPreferencesKey("use_numeric_date_format_widget")
    private val USE_NUMERIC_DATE_FORMAT_NOTIFICATION =
        booleanPreferencesKey("use_numeric_date_format_notification")


    // --- State Flows ---
    val dndEnabled: StateFlow<Boolean> =
        context.dataStore.data.map { it[DND_ENABLED] ?: false }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val dndStartTime: StateFlow<String> =
        context.dataStore.data.map { it[DND_START_TIME] ?: "22:00" }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "22:00")
    val dndEndTime: StateFlow<String> =
        context.dataStore.data.map { it[DND_END_TIME] ?: "07:00" }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "07:00")
    val showGeneralTimes: StateFlow<Boolean> =
        context.dataStore.data.map { it[SHOW_GENERAL_TIMES] ?: true }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val showSpecificTimes: StateFlow<Boolean> =
        context.dataStore.data.map { it[SHOW_SPECIFIC_TIMES] ?: true }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val themeId: StateFlow<String> =
        context.dataStore.data.map { it[THEME_ID] ?: "system" }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")
    val fontId: StateFlow<String> =
        context.dataStore.data.map { it[FONT_ID] ?: "estedad" }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "estedad")
    val autoSilentEnabled: StateFlow<Boolean> =
        context.dataStore.data.map { it[AUTO_SILENT_ENABLED] ?: false }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val prayerSilentEnabled: StateFlow<Map<PrayerTime, Boolean>> =
        context.dataStore.data.map {
            PrayerTime.entries.associateWith { prayer ->
                val key = booleanPreferencesKey("silent_enabled_${prayer.id}")
                it[key] ?: false
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val prayerMinutesBefore: StateFlow<Map<PrayerTime, Int>> =
        context.dataStore.data.map {
            PrayerTime.entries.associateWith { prayer ->
                val key = intPreferencesKey("minutes_before_silent_${prayer.id}")
                it[key] ?: 10
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val prayerMinutesAfter: StateFlow<Map<PrayerTime, Int>> =
        context.dataStore.data.map {
            PrayerTime.entries.associateWith { prayer ->
                val key = intPreferencesKey("minutes_after_silent_${prayer.id}")
                it[key] ?: 10
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val prayerIqamaEnabled: StateFlow<Map<PrayerTime, Boolean>> =
        context.dataStore.data.map {
            PrayerTime.entries.associateWith { prayer ->
                val key = booleanPreferencesKey("iqama_enabled_${prayer.id}")
                it[key] ?: false
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val minutesBeforeIqama: StateFlow<Int> =
        context.dataStore.data.map { it[MINUTES_BEFORE_IQAMA] ?: 10 }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10)

    val iqamaNotificationText: StateFlow<String> =
        context.dataStore.data.map {
            when (val storedTemplate = it[IQAMA_NOTIFICATION_TEXT]) {
                null -> ""
                DEFAULT_IQAMA_TEXT -> ""
                else -> storedTemplate
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val is24HourFormat: StateFlow<Boolean> =
        context.dataStore.data.map { it[USE_24_HOUR_FORMAT] ?: true }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val usePersianNumbers: StateFlow<Boolean> =
        context.dataStore.data.map { it[USE_PERSIAN_NUMBERS] ?: true }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val useNumericDateFormatMain: StateFlow<Boolean> =
        context.dataStore.data.map { it[USE_NUMERIC_DATE_FORMAT_MAIN] ?: false }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val useNumericDateFormatWidget: StateFlow<Boolean> =
        context.dataStore.data.map { it[USE_NUMERIC_DATE_FORMAT_WIDGET] ?: false }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val useNumericDateFormatNotification: StateFlow<Boolean> =
        context.dataStore.data.map { it[USE_NUMERIC_DATE_FORMAT_NOTIFICATION] ?: false }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val prayerMinutesBeforeAdhan: StateFlow<Map<PrayerTime, Int>> =
        context.dataStore.data.map {
            PrayerTime.entries.associateWith { prayer ->
                val key = intPreferencesKey("minutes_before_adhan_${prayer.id}")
                it[key] ?: 0
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val allAdhansSet: Flow<Boolean> =
        context.dataStore.data.map {
            PrayerTime.entries.all { prayer ->
                val key = stringPreferencesKey("adhan_sound_${prayer.id}")
                (it[key] ?: "off") != "off"
            }
        }

    val allSilentEnabled: Flow<Boolean> =
        context.dataStore.data.map {
            PrayerTime.entries.all { prayer ->
                val key = booleanPreferencesKey("silent_enabled_${prayer.id}")
                it[key] ?: false
            }
        }

    // --- Adhan Sound Methods ---
    fun getAdhanSoundFlow(prayerId: String): Flow<String> {
        val key = stringPreferencesKey("adhan_sound_$prayerId")
        return context.dataStore.data.map { preferences ->
            preferences[key] ?: "off" // Default to "off"
        }
    }

    fun setAdhanSound(prayerId: String, sound: String) {
        viewModelScope.launch {
            val key = stringPreferencesKey("adhan_sound_$prayerId")
            context.dataStore.edit { settings ->
                settings[key] = sound
            }
            PrayerForegroundService.scheduleAlarms(context)
        }
    }

    // --- Update Functions کمکى ---
    private fun notifyWidgetsAndNotification() {
        PrayerForegroundService.update(context)
        val widgetManager = AppWidgetManager.getInstance(context)
        listOf(
            ModernWidgetProvider::class.java,
            LargeModernWidgetProvider::class.java
        ).forEach { providerClass ->
            val componentName = ComponentName(context, providerClass)
            val widgetIds = widgetManager.getAppWidgetIds(componentName)
            if (widgetIds.isNotEmpty()) {
                val updateIntent = Intent(context, providerClass).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
                }
                context.sendBroadcast(updateIntent)
            }
        }
    }

    private fun <T> updateSetting(key: Preferences.Key<T>, value: T) {
        viewModelScope.launch {
            context.dataStore.edit { settings ->
                settings[key] = value
            }
        }
    }

    private fun <T> updateSettingAndNotify(key: Preferences.Key<T>, value: T) {
        viewModelScope.launch {
            context.dataStore.edit { settings ->
                settings[key] = value
            }
            notifyWidgetsAndNotification()
        }
    }

    // --- DND ---
    fun updateDndEnabled(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { it[DND_ENABLED] = enabled }
            PrayerForegroundService.scheduleDnd(context)
        }
    }

    fun updateDndStartTime(time: String) {
        viewModelScope.launch {
            context.dataStore.edit { it[DND_START_TIME] = time }
            PrayerForegroundService.scheduleDnd(context)
        }
    }

    fun updateDndEndTime(time: String) {
        viewModelScope.launch {
            context.dataStore.edit { it[DND_END_TIME] = time }
            PrayerForegroundService.scheduleDnd(context)
        }
    }

    fun updateShowGeneralTimes(show: Boolean) =
        updateSettingAndNotify(SHOW_GENERAL_TIMES, show)

    fun updateShowSpecificTimes(show: Boolean) =
        updateSettingAndNotify(SHOW_SPECIFIC_TIMES, show)

    // --- تم (شامل is_dark_theme) + رفرش آنی نوتیف اقامه ---
    fun updateThemeId(newThemeId: String) {
        viewModelScope.launch {
            context.dataStore.edit {
                it[THEME_ID] = newThemeId
                it[IS_DARK_THEME] = newThemeId == "dark"
            }
            notifyWidgetsAndNotification()
            // رفرش نوتیف اقامه با تم جدید
            AdhanScheduler.refreshIqamaNotifications(context)
        }
    }

    // --- فونت + رفرش آنی نوتیف اقامه ---
    fun updateFontId(newFontId: String) {
        viewModelScope.launch {
            context.dataStore.edit { settings ->
                settings[FONT_ID] = newFontId
            }
            notifyWidgetsAndNotification()
            // رفرش نوتیف اقامه با فونت جدید
            AdhanScheduler.refreshIqamaNotifications(context)
        }
    }

    fun updateAutoSilentEnabled(isEnabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { settings ->
                settings[AUTO_SILENT_ENABLED] = isEnabled
            }
            PrayerForegroundService.scheduleAlarms(context)
        }
    }

    fun updatePrayerSilentEnabled(prayer: PrayerTime, isEnabled: Boolean) {
        viewModelScope.launch {
            val key = booleanPreferencesKey("silent_enabled_${prayer.id}")
            context.dataStore.edit { settings ->
                settings[key] = isEnabled
            }
            PrayerForegroundService.scheduleAlarms(context)
        }
    }

    fun updatePrayerMinutesBefore(prayer: PrayerTime, minutes: Int) {
        viewModelScope.launch {
            val key = intPreferencesKey("minutes_before_silent_${prayer.id}")
            context.dataStore.edit { settings ->
                settings[key] = minutes
            }
            PrayerForegroundService.scheduleAlarms(context)
        }
    }

    fun updatePrayerMinutesAfter(prayer: PrayerTime, minutes: Int) {
        viewModelScope.launch {
            val key = intPreferencesKey("minutes_after_silent_${prayer.id}")
            context.dataStore.edit { settings ->
                settings[key] = minutes
            }
            PrayerForegroundService.scheduleAlarms(context)
        }
    }

    fun updatePrayerIqamaEnabled(prayer: PrayerTime, isEnabled: Boolean) {
        viewModelScope.launch {
            val key = booleanPreferencesKey("iqama_enabled_${prayer.id}")
            context.dataStore.edit { settings ->
                settings[key] = isEnabled
            }
            PrayerForegroundService.scheduleAlarms(context)
        }
    }

    fun updateMinutesBeforeIqama(minutes: Int) {
        viewModelScope.launch {
            context.dataStore.edit { settings ->
                settings[MINUTES_BEFORE_IQAMA] = minutes
            }
            PrayerForegroundService.scheduleAlarms(context)
        }
    }

    fun updatePrayerMinutesBeforeAdhan(prayer: PrayerTime, minutes: Int) {
        viewModelScope.launch {
            val key = intPreferencesKey("minutes_before_adhan_${prayer.id}")
            context.dataStore.edit { settings ->
                settings[key] = minutes
            }
            PrayerForegroundService.scheduleAlarms(context)
        }
    }

    fun updateIqamaNotificationText(text: String) =
        updateSetting(IQAMA_NOTIFICATION_TEXT, text)

    fun updateIs24HourFormat(is24Hour: Boolean) =
        updateSettingAndNotify(USE_24_HOUR_FORMAT, is24Hour)

    fun updateUsePersianNumbers(usePersian: Boolean) =
        updateSettingAndNotify(USE_PERSIAN_NUMBERS, usePersian)

    fun updateUseNumericDateFormatMain(useNumeric: Boolean) =
        updateSetting(USE_NUMERIC_DATE_FORMAT_MAIN, useNumeric)

    fun updateUseNumericDateFormatWidget(useNumeric: Boolean) =
        updateSettingAndNotify(USE_NUMERIC_DATE_FORMAT_WIDGET, useNumeric)

    fun updateUseNumericDateFormatNotification(useNumeric: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { settings ->
                settings[USE_NUMERIC_DATE_FORMAT_NOTIFICATION] = useNumeric
            }
            PrayerForegroundService.update(context)
        }
    }
}