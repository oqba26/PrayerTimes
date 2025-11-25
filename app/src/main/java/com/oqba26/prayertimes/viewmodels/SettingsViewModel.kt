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
import com.oqba26.prayertimes.services.PrayerForegroundService
import com.oqba26.prayertimes.widget.LargeModernWidgetProvider
import com.oqba26.prayertimes.widget.ModernWidgetProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    @SuppressLint("StaticFieldLeak")
    private val context = getApplication<Application>().applicationContext

    // --- ثابت‌ها ---
    private val DEFAULT_IQAMA_TEXT = "اکنون زمان اقامه {prayer} است."

    // --- Preference Keys ---
    private val THEME_ID = stringPreferencesKey("themeId")
    private val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
    private val FONT_ID = stringPreferencesKey("fontId")
    private val AUTO_SILENT_ENABLED = booleanPreferencesKey("auto_silent_enabled")
    private val IQAMA_ENABLED = booleanPreferencesKey("iqama_enabled")
    private val MINUTES_BEFORE_IQAMA = intPreferencesKey("minutes_before_iqama")
    private val IQAMA_NOTIFICATION_TEXT = stringPreferencesKey("iqama_notification_text")
    private val USE_24_HOUR_FORMAT = booleanPreferencesKey("use_24_hour_format")
    private val USE_PERSIAN_NUMBERS = booleanPreferencesKey("use_persian_numbers")

    // --- State Flows ---
    private val _themeId = MutableStateFlow("system")
    val themeId = _themeId.asStateFlow()

    private val _fontId = MutableStateFlow("estedad")
    val fontId = _fontId.asStateFlow()

    private val _autoSilentEnabled = MutableStateFlow(false)
    val autoSilentEnabled = _autoSilentEnabled.asStateFlow()

    private val _prayerSilentEnabled = MutableStateFlow<Map<PrayerTime, Boolean>>(emptyMap())
    val prayerSilentEnabled = _prayerSilentEnabled.asStateFlow()

    private val _prayerMinutesBefore = MutableStateFlow<Map<PrayerTime, Int>>(emptyMap())
    val prayerMinutesBefore = _prayerMinutesBefore.asStateFlow()

    private val _prayerMinutesAfter = MutableStateFlow<Map<PrayerTime, Int>>(emptyMap())
    val prayerMinutesAfter = _prayerMinutesAfter.asStateFlow()

    private val _iqamaEnabled = MutableStateFlow(false)
    val iqamaEnabled = _iqamaEnabled.asStateFlow()

    private val _minutesBeforeIqama = MutableStateFlow(10)
    val minutesBeforeIqama = _minutesBeforeIqama.asStateFlow()

    private val _iqamaNotificationText = MutableStateFlow(DEFAULT_IQAMA_TEXT)
    val iqamaNotificationText = _iqamaNotificationText.asStateFlow()

    private val _is24HourFormat = MutableStateFlow(true)
    val is24HourFormat = _is24HourFormat.asStateFlow()

    private val _usePersianNumbers = MutableStateFlow(true)
    val usePersianNumbers = _usePersianNumbers.asStateFlow()

    private val _prayerMinutesBeforeAdhan = MutableStateFlow<Map<PrayerTime, Int>>(emptyMap())
    val prayerMinutesBeforeAdhan = _prayerMinutesBeforeAdhan.asStateFlow()

    val allAdhansSet: Flow<Boolean> = context.dataStore.data.map {
        PrayerTime.entries.all { prayer ->
            val key = stringPreferencesKey("adhan_sound_${prayer.id}")
            (it[key] ?: "off") != "off"
        }
    }

    val allSilentEnabled: Flow<Boolean> = context.dataStore.data.map {
        PrayerTime.entries.all { prayer ->
            val key = booleanPreferencesKey("silent_enabled_${prayer.id}")
            it[key] ?: false
        }
    }


    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val settings = context.dataStore.data.first()

            _themeId.value = settings[THEME_ID] ?: "system"
            _fontId.value = settings[FONT_ID] ?: "estedad"
            _autoSilentEnabled.value = settings[AUTO_SILENT_ENABLED] ?: false

            val enabledMap = mutableMapOf<PrayerTime, Boolean>()
            val beforeMap = mutableMapOf<PrayerTime, Int>()
            val afterMap = mutableMapOf<PrayerTime, Int>()
            val beforeAdhanMap = mutableMapOf<PrayerTime, Int>()

            PrayerTime.entries.forEach { prayer ->
                val enabledKey = booleanPreferencesKey("silent_enabled_${prayer.id}")
                val beforeKey = intPreferencesKey("minutes_before_silent_${prayer.id}")
                val afterKey = intPreferencesKey("minutes_after_silent_${prayer.id}")
                val beforeAdhanKey = intPreferencesKey("minutes_before_adhan_${prayer.id}")

                enabledMap[prayer] = settings[enabledKey] ?: false
                beforeMap[prayer] = settings[beforeKey] ?: 10
                afterMap[prayer] = settings[afterKey] ?: 10
                beforeAdhanMap[prayer] = settings[beforeAdhanKey] ?: 0
            }

            _prayerSilentEnabled.value = enabledMap
            _prayerMinutesBefore.value = beforeMap
            _prayerMinutesAfter.value = afterMap
            _prayerMinutesBeforeAdhan.value = beforeAdhanMap

            _iqamaEnabled.value = settings[IQAMA_ENABLED] ?: false
            _minutesBeforeIqama.value = settings[MINUTES_BEFORE_IQAMA] ?: 10

            // متن نوتیف اقامه
// - null  یا برابر متن پیش‌فرض  => فیلد تنظیمات را خالی نشان بده (placeholder بگوید متن پیش‌فرض چیه)
// - هر چیز دیگر => متن سفارشی کاربر
            val storedTemplate = settings[IQAMA_NOTIFICATION_TEXT]
            _iqamaNotificationText.value = when (storedTemplate) {
                null -> ""
                DEFAULT_IQAMA_TEXT -> ""   // مهاجرت از نسخه قدیمی که پیش‌فرض را ذخیره می‌کرد
                else -> storedTemplate
            }

            _is24HourFormat.value = settings[USE_24_HOUR_FORMAT] ?: true
            _usePersianNumbers.value = settings[USE_PERSIAN_NUMBERS] ?: true
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
            notifyWidgets()
            PrayerForegroundService.scheduleAlarms(context)
        }
    }

    // --- Update Functions ---
    private fun notifyWidgets() {
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

    fun updateThemeId(newThemeId: String) {
        viewModelScope.launch {
            context.dataStore.edit {
                it[THEME_ID] = newThemeId
                it[IS_DARK_THEME] = newThemeId == "dark"
            }
            _themeId.value = newThemeId
            notifyWidgets()
        }
    }

    fun updateFontId(newFontId: String) {
        viewModelScope.launch {
            context.dataStore.edit { it[FONT_ID] = newFontId }
            _fontId.value = newFontId
            notifyWidgets()
        }
    }

    fun updateAutoSilentEnabled(isEnabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { settings ->
                settings[AUTO_SILENT_ENABLED] = isEnabled
            }
            _autoSilentEnabled.value = isEnabled
            PrayerForegroundService.scheduleAlarms(context)
        }
    }

    fun updatePrayerSilentEnabled(prayer: PrayerTime, isEnabled: Boolean) {
        viewModelScope.launch {
            val key = booleanPreferencesKey("silent_enabled_${prayer.id}")
            context.dataStore.edit { settings ->
                settings[key] = isEnabled
            }
            _prayerSilentEnabled.value = _prayerSilentEnabled.value.toMutableMap().apply {
                this[prayer] = isEnabled
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
            _prayerMinutesBefore.value = _prayerMinutesBefore.value.toMutableMap().apply {
                this[prayer] = minutes
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
            _prayerMinutesAfter.value = _prayerMinutesAfter.value.toMutableMap().apply {
                this[prayer] = minutes
            }
            PrayerForegroundService.scheduleAlarms(context)
        }
    }

    fun updateIqamaEnabled(isEnabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { settings ->
                settings[IQAMA_ENABLED] = isEnabled
            }
            _iqamaEnabled.value = isEnabled
            PrayerForegroundService.scheduleAlarms(context)
        }
    }

    fun updateMinutesBeforeIqama(minutes: Int) {
        viewModelScope.launch {
            context.dataStore.edit { settings ->
                settings[MINUTES_BEFORE_IQAMA] = minutes
            }
            _minutesBeforeIqama.value = minutes
            PrayerForegroundService.scheduleAlarms(context)
        }
    }

    fun updatePrayerMinutesBeforeAdhan(prayer: PrayerTime, minutes: Int) {
        viewModelScope.launch {
            val key = intPreferencesKey("minutes_before_adhan_${prayer.id}")
            context.dataStore.edit { settings ->
                settings[key] = minutes
            }
            _prayerMinutesBeforeAdhan.value = _prayerMinutesBeforeAdhan.value.toMutableMap().apply {
                this[prayer] = minutes
            }
            PrayerForegroundService.scheduleAlarms(context)
        }
    }

    // ⬇️ جدید: بروزرسانی متن نوتیف اقامه
    fun updateIqamaNotificationText(text: String) {
        viewModelScope.launch {
            context.dataStore.edit { settings ->
                // رشته به همان شکلی که کاربر می‌نویسد ذخیره می‌شود
                // خالی = یعنی از متن پیش‌فرض استفاده کن
                settings[IQAMA_NOTIFICATION_TEXT] = text
            }
            _iqamaNotificationText.value = text
        }
    }

    fun updateIs24HourFormat(is24Hour: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { settings ->
                settings[USE_24_HOUR_FORMAT] = is24Hour
            }
            _is24HourFormat.value = is24Hour
            notifyWidgets()
        }
    }

    fun updateUsePersianNumbers(usePersian: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { settings ->
                settings[USE_PERSIAN_NUMBERS] = usePersian
            }
            _usePersianNumbers.value = usePersian
            notifyWidgets()
        }
    }
}
