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
    private val USE_NUMERIC_DATE_FORMAT_MAIN = booleanPreferencesKey("use_numeric_date_format_main")
    private val USE_NUMERIC_DATE_FORMAT_WIDGET = booleanPreferencesKey("use_numeric_date_format_widget")
    private val USE_NUMERIC_DATE_FORMAT_NOTIFICATION = booleanPreferencesKey("use_numeric_date_format_notification")


    // --- State Flows ---
    private val _dndEnabled = MutableStateFlow(false)
    val dndEnabled = _dndEnabled.asStateFlow()

    private val _dndStartTime = MutableStateFlow("22:00")
    val dndStartTime = _dndStartTime.asStateFlow()

    private val _dndEndTime = MutableStateFlow("07:00")
    val dndEndTime = _dndEndTime.asStateFlow()

    private val _showGeneralTimes = MutableStateFlow(true)
    val showGeneralTimes = _showGeneralTimes.asStateFlow()

    private val _showSpecificTimes = MutableStateFlow(true)
    val showSpecificTimes = _showSpecificTimes.asStateFlow()

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

    private val _prayerIqamaEnabled = MutableStateFlow<Map<PrayerTime, Boolean>>(emptyMap())
    val prayerIqamaEnabled = _prayerIqamaEnabled.asStateFlow()

    private val _minutesBeforeIqama = MutableStateFlow(10)
    val minutesBeforeIqama = _minutesBeforeIqama.asStateFlow()

    private val _iqamaNotificationText = MutableStateFlow(DEFAULT_IQAMA_TEXT)
    val iqamaNotificationText = _iqamaNotificationText.asStateFlow()

    private val _is24HourFormat = MutableStateFlow(true)
    val is24HourFormat = _is24HourFormat.asStateFlow()

    private val _usePersianNumbers = MutableStateFlow(true)
    val usePersianNumbers = _usePersianNumbers.asStateFlow()

    private val _useNumericDateFormatMain = MutableStateFlow(false)
    val useNumericDateFormatMain = _useNumericDateFormatMain.asStateFlow()

    private val _useNumericDateFormatWidget = MutableStateFlow(false)
    val useNumericDateFormatWidget = _useNumericDateFormatWidget.asStateFlow()

    private val _useNumericDateFormatNotification = MutableStateFlow(false)
    val useNumericDateFormatNotification = _useNumericDateFormatNotification.asStateFlow()

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

            _dndEnabled.value = settings[DND_ENABLED] ?: false
            _dndStartTime.value = settings[DND_START_TIME] ?: "22:00"
            _dndEndTime.value = settings[DND_END_TIME] ?: "07:00"
            _showGeneralTimes.value = settings[SHOW_GENERAL_TIMES] ?: true
            _showSpecificTimes.value = settings[SHOW_SPECIFIC_TIMES] ?: true
            _themeId.value = settings[THEME_ID] ?: "system"
            _fontId.value = settings[FONT_ID] ?: "estedad"
            _autoSilentEnabled.value = settings[AUTO_SILENT_ENABLED] ?: false

            val enabledMap = mutableMapOf<PrayerTime, Boolean>()
            val beforeMap = mutableMapOf<PrayerTime, Int>()
            val afterMap = mutableMapOf<PrayerTime, Int>()
            val beforeAdhanMap = mutableMapOf<PrayerTime, Int>()
            val iqamaEnabledMap = mutableMapOf<PrayerTime, Boolean>()

            PrayerTime.entries.forEach { prayer ->
                val enabledKey = booleanPreferencesKey("silent_enabled_${prayer.id}")
                val beforeKey = intPreferencesKey("minutes_before_silent_${prayer.id}")
                val afterKey = intPreferencesKey("minutes_after_silent_${prayer.id}")
                val beforeAdhanKey = intPreferencesKey("minutes_before_adhan_${prayer.id}")
                val iqamaEnabledKey = booleanPreferencesKey("iqama_enabled_${prayer.id}")

                enabledMap[prayer] = settings[enabledKey] ?: false
                beforeMap[prayer] = settings[beforeKey] ?: 10
                afterMap[prayer] = settings[afterKey] ?: 10
                beforeAdhanMap[prayer] = settings[beforeAdhanKey] ?: 0
                iqamaEnabledMap[prayer] = settings[iqamaEnabledKey] ?: false
            }

            _prayerSilentEnabled.value = enabledMap
            _prayerMinutesBefore.value = beforeMap
            _prayerMinutesAfter.value = afterMap
            _prayerMinutesBeforeAdhan.value = beforeAdhanMap
            _prayerIqamaEnabled.value = iqamaEnabledMap

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
            _useNumericDateFormatMain.value = settings[USE_NUMERIC_DATE_FORMAT_MAIN] ?: false
            _useNumericDateFormatWidget.value = settings[USE_NUMERIC_DATE_FORMAT_WIDGET] ?: false
            _useNumericDateFormatNotification.value = settings[USE_NUMERIC_DATE_FORMAT_NOTIFICATION] ?: false
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

    // --- Update Functions ---
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

    fun updateDndEnabled(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { it[DND_ENABLED] = enabled }
            _dndEnabled.value = enabled
            PrayerForegroundService.scheduleDnd(context)
        }
    }

    fun updateDndStartTime(time: String) {
        viewModelScope.launch {
            context.dataStore.edit { it[DND_START_TIME] = time }
            _dndStartTime.value = time
            PrayerForegroundService.scheduleDnd(context)
        }
    }

    fun updateDndEndTime(time: String) {
        viewModelScope.launch {
            context.dataStore.edit { it[DND_END_TIME] = time }
            _dndEndTime.value = time
            PrayerForegroundService.scheduleDnd(context)
        }
    }

    fun updateShowGeneralTimes(show: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { it[SHOW_GENERAL_TIMES] = show }
            _showGeneralTimes.value = show
            notifyWidgetsAndNotification()
        }
    }

    fun updateShowSpecificTimes(show: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { it[SHOW_SPECIFIC_TIMES] = show }
            _showSpecificTimes.value = show
            notifyWidgetsAndNotification()
        }
    }

    fun updateThemeId(newThemeId: String) {
        viewModelScope.launch {
            context.dataStore.edit {
                it[THEME_ID] = newThemeId
                it[IS_DARK_THEME] = newThemeId == "dark"
            }
            _themeId.value = newThemeId
            notifyWidgetsAndNotification()
        }
    }

    fun updateFontId(newFontId: String) {
        viewModelScope.launch {
            context.dataStore.edit { it[FONT_ID] = newFontId }
            _fontId.value = newFontId
            notifyWidgetsAndNotification()
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

    fun updatePrayerIqamaEnabled(prayer: PrayerTime, isEnabled: Boolean) {
        viewModelScope.launch {
            val key = booleanPreferencesKey("iqama_enabled_${prayer.id}")
            context.dataStore.edit { settings ->
                settings[key] = isEnabled
            }
            _prayerIqamaEnabled.value = _prayerIqamaEnabled.value.toMutableMap().apply {
                this[prayer] = isEnabled
            }
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
            notifyWidgetsAndNotification()
        }
    }

    fun updateUsePersianNumbers(usePersian: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { settings ->
                settings[USE_PERSIAN_NUMBERS] = usePersian
            }
            _usePersianNumbers.value = usePersian
            notifyWidgetsAndNotification()
        }
    }

    fun updateUseNumericDateFormatMain(useNumeric: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { settings ->
                settings[USE_NUMERIC_DATE_FORMAT_MAIN] = useNumeric
            }
            _useNumericDateFormatMain.value = useNumeric
        }
    }

    fun updateUseNumericDateFormatWidget(useNumeric: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { settings ->
                settings[USE_NUMERIC_DATE_FORMAT_WIDGET] = useNumeric
            }
            _useNumericDateFormatWidget.value = useNumeric
            notifyWidgetsAndNotification()
        }
    }

    fun updateUseNumericDateFormatNotification(useNumeric: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { settings ->
                settings[USE_NUMERIC_DATE_FORMAT_NOTIFICATION] = useNumeric
            }
            _useNumericDateFormatNotification.value = useNumeric
            PrayerForegroundService.update(context)
        }
    }
}
