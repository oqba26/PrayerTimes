package com.oqba26.prayertimes.viewmodels

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.oqba26.prayertimes.screens.PrayerTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    @SuppressLint("StaticFieldLeak")
    private val context = getApplication<Application>().applicationContext

    private val _prayerSilentSettings = MutableStateFlow<Map<PrayerTime, Boolean>>(emptyMap())
    val prayerSilentSettings = _prayerSilentSettings.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val settings = context.dataStore.data.first()
            val currentSettings = PrayerTime.entries.associateWith { prayerTime ->
                val key = booleanPreferencesKey("${prayerTime.name}_silent_enabled")
                settings[key] ?: false
            }
            _prayerSilentSettings.value = currentSettings
        }
    }

/**
 * Updates the silent setting for a specific prayer time in the data store.
 * This function launches a coroutine to persist the setting and then reloads all settings
 * to update the UI state accordingly.
 *
 * @param prayerTime The specific prayer time (e.g., FAJR, DHUHR) for which to update the setting
 * @param isEnabled Boolean value indicating whether the silent setting should be enabled or disabled
 */
    fun updatePrayerSilentSetting(prayerTime: PrayerTime, isEnabled: Boolean) {
        // Launch a coroutine in the viewModelScope to perform the update
        viewModelScope.launch {
            // Create a unique preference key based on the prayer time name
            val key = booleanPreferencesKey("${prayerTime.name}_silent_enabled")
            // Update the setting in the data store
            context.dataStore.edit { settings ->
                settings[key] = isEnabled
            }
            loadSettings() // Reload settings to update the UI state
        }
    }
}
