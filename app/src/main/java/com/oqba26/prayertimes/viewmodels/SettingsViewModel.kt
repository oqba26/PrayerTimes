package com.oqba26.prayertimes.viewmodels

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

    private val context = getApplication<Application>().applicationContext

    private val _prayerSilentSettings = MutableStateFlow<Map<PrayerTime, Boolean>>(emptyMap())
    val prayerSilentSettings = _prayerSilentSettings.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val settings = context.dataStore.data.first()
            val currentSettings = PrayerTime.values().associateWith { prayerTime ->
                val key = booleanPreferencesKey("${prayerTime.name}_silent_enabled")
                settings[key] ?: false
            }
            _prayerSilentSettings.value = currentSettings
        }
    }

    fun updatePrayerSilentSetting(prayerTime: PrayerTime, isEnabled: Boolean) {
        viewModelScope.launch {
            val key = booleanPreferencesKey("${prayerTime.name}_silent_enabled")
            context.dataStore.edit { settings ->
                settings[key] = isEnabled
            }
            loadSettings() // Reload settings to update the UI state
        }
    }
}
