package com.oqba26.prayertimes.viewmodels

import android.annotation.SuppressLint
import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.oqba26.prayertimes.screens.PrayerTime
import com.oqba26.prayertimes.services.PrayerForegroundService
import com.oqba26.prayertimes.widget.LargeModernWidgetProvider
import com.oqba26.prayertimes.widget.ModernWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    @SuppressLint("StaticFieldLeak")
    private val context = getApplication<Application>().applicationContext

    // --- Preference Keys ---
    private val AUTO_SILENT_ENABLED = booleanPreferencesKey("auto_silent_enabled")
    private val IQAMA_ENABLED = booleanPreferencesKey("iqama_enabled")
    private val MINUTES_BEFORE_IQAMA = intPreferencesKey("minutes_before_iqama")
    private val USE_24_HOUR_FORMAT = booleanPreferencesKey("use_24_hour_format")
    private val USE_PERSIAN_NUMBERS = booleanPreferencesKey("use_persian_numbers")

    // --- State Flows ---

    // Master switch for the entire auto-silent feature
    private val _autoSilentEnabled = MutableStateFlow(false)
    val autoSilentEnabled = _autoSilentEnabled.asStateFlow()

    // Per-prayer enabled status
    private val _prayerSilentEnabled = MutableStateFlow<Map<PrayerTime, Boolean>>(emptyMap())
    val prayerSilentEnabled = _prayerSilentEnabled.asStateFlow()

    // Per-prayer minutes before
    private val _prayerMinutesBefore = MutableStateFlow<Map<PrayerTime, Int>>(emptyMap())
    val prayerMinutesBefore = _prayerMinutesBefore.asStateFlow()

    // Per-prayer minutes after
    private val _prayerMinutesAfter = MutableStateFlow<Map<PrayerTime, Int>>(emptyMap())
    val prayerMinutesAfter = _prayerMinutesAfter.asStateFlow()

    // Iqama State
    private val _iqamaEnabled = MutableStateFlow(false)
    val iqamaEnabled = _iqamaEnabled.asStateFlow()

    private val _minutesBeforeIqama = MutableStateFlow(10)
    val minutesBeforeIqama = _minutesBeforeIqama.asStateFlow()

    private val _is24HourFormat = MutableStateFlow(true)
    val is24HourFormat = _is24HourFormat.asStateFlow()

    private val _usePersianNumbers = MutableStateFlow(true)
    val usePersianNumbers = _usePersianNumbers.asStateFlow()


    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val settings = context.dataStore.data.first()
            _autoSilentEnabled.value = settings[AUTO_SILENT_ENABLED] ?: false

            // Load per-prayer settings
            val enabledMap = mutableMapOf<PrayerTime, Boolean>()
            val beforeMap = mutableMapOf<PrayerTime, Int>()
            val afterMap = mutableMapOf<PrayerTime, Int>()

            PrayerTime.entries.forEach { prayer ->
                val enabledKey = booleanPreferencesKey("silent_enabled_${prayer.id}")
                val beforeKey = intPreferencesKey("minutes_before_silent_${prayer.id}")
                val afterKey = intPreferencesKey("minutes_after_silent_${prayer.id}")

                enabledMap[prayer] = settings[enabledKey] ?: false
                beforeMap[prayer] = settings[beforeKey] ?: 10
                afterMap[prayer] = settings[afterKey] ?: 10
            }

            _prayerSilentEnabled.value = enabledMap
            _prayerMinutesBefore.value = beforeMap
            _prayerMinutesAfter.value = afterMap


            _iqamaEnabled.value = settings[IQAMA_ENABLED] ?: false
            _minutesBeforeIqama.value = settings[MINUTES_BEFORE_IQAMA] ?: 10

            _is24HourFormat.value = settings[USE_24_HOUR_FORMAT] ?: true
            _usePersianNumbers.value = settings[USE_PERSIAN_NUMBERS] ?: true

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

    private fun notifyService() {
        val serviceIntent = Intent(context, PrayerForegroundService::class.java).apply {
            action = "RESTART"
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    fun updateAutoSilentEnabled(isEnabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { settings ->
                settings[AUTO_SILENT_ENABLED] = isEnabled
            }
            _autoSilentEnabled.value = isEnabled
            notifyWidgets()
            notifyService()
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
            notifyWidgets()
            notifyService()
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
            notifyWidgets()
            notifyService()
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
            notifyWidgets()
            notifyService()
        }
    }


    fun updateIqamaEnabled(isEnabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { settings ->
                settings[IQAMA_ENABLED] = isEnabled
            }
            _iqamaEnabled.value = isEnabled
            notifyWidgets()
            notifyService()
        }
    }

    fun updateMinutesBeforeIqama(minutes: Int) {
        viewModelScope.launch {
            context.dataStore.edit { settings ->
                settings[MINUTES_BEFORE_IQAMA] = minutes
            }
            _minutesBeforeIqama.value = minutes
            notifyWidgets()
            notifyService()
        }
    }

    fun updateIs24HourFormat(is24Hour: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { settings ->
                settings[USE_24_HOUR_FORMAT] = is24Hour
            }
            _is24HourFormat.value = is24Hour
            notifyWidgets()
            notifyService()
        }
    }

    fun updateUsePersianNumbers(usePersian: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { settings ->
                settings[USE_PERSIAN_NUMBERS] = usePersian
            }
            _usePersianNumbers.value = usePersian
            notifyWidgets()
            notifyService()
        }
    }
}
