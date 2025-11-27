package com.oqba26.prayertimes.viewmodels

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oqba26.prayertimes.models.MultiDate
import com.oqba26.prayertimes.models.UserNote // فرض بر اینکه این import لازم است
import com.oqba26.prayertimes.screens.ViewMode // فرض بر اینکه این import لازم است
import com.oqba26.prayertimes.utils.DateUtils.getCurrentDate
import com.oqba26.prayertimes.utils.PrayerUtils
import com.oqba26.prayertimes.utils.PrayerUtils.loadDetailedPrayerTimes
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalTime

class PrayerViewModel : ViewModel() {
    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Error(val message: String, val throwable: Throwable? = null) : Result<Nothing>()
        object Loading : Result<Nothing>()
    }
    // selectedDate
    private val _currentDate = MutableStateFlow(getCurrentDate())
    @Suppress("unused")
    val selectedDate: StateFlow<MultiDate> = _currentDate.asStateFlow()

    // uiState (prayer times data)
    private val _uiState = MutableStateFlow<Result<Map<String, String>>>(Result.Loading)
    val uiState: StateFlow<Result<Map<String, String>>> = _uiState.asStateFlow()

    // currentPrayer
    private val _currentPrayer = MutableStateFlow<String?>("")

    // userNotes
    private val _userNotes = MutableStateFlow<Map<String, UserNote>>(emptyMap())
    @Suppress("unused")
    val userNotes: StateFlow<Map<String, UserNote>> = _userNotes.asStateFlow()

    // currentViewMode
    private val _currentViewMode = MutableStateFlow(ViewMode.PRAYER_TIMES)
    @Suppress("unused")
    val currentViewMode: StateFlow<ViewMode> = _currentViewMode.asStateFlow()

    @SuppressLint("StaticFieldLeak")
    private var context: Context? = null
    private var prayerTimesData: Map<String, String> = emptyMap()

    fun loadData(context: Context) {
        this.context = context
        loadPrayerTimesForDate(_currentDate.value)
        startPrayerTimeChecker()
    }

    fun updateDate(newDate: MultiDate) {
        _currentDate.value = newDate
        context?.let {
            loadPrayerTimesForDate(newDate)
        }
    }

    private fun loadPrayerTimesForDate(date: MultiDate) {
        val ctx = context ?: run {
            _uiState.value = Result.Error("Context not available")
            return
        }

        viewModelScope.launch {
            _uiState.value = Result.Loading
            try {
                val times = loadDetailedPrayerTimes(ctx, date)
                if (times.isEmpty()) {
                    _uiState.value = Result.Error("اوقات شرعی برای این تاریخ موجود نیست")
                } else {
                    prayerTimesData = times
                    _uiState.value = Result.Success(times)
                    updateCurrentPrayer()
                }
            } catch (e: Exception) {
                val errorMessage = e.localizedMessage ?: "خطا در دریافت داده‌ها"
                _uiState.value = Result.Error(errorMessage, e)
                Log.e("PrayerViewModel", "خطا در دریافت داده‌ها", e)
            }
        }
    }

    private fun updateCurrentPrayer() {
        val currentData = when (val state = _uiState.value) {
            is Result.Success -> state.data
            else -> return
        }

        if (currentData.isNotEmpty()) {
            val order = listOf("طلوع بامداد", "طلوع خورشید", "ظهر", "عصر", "غروب", "عشاء", "صبح", "مغرب")
            val current = PrayerUtils.computeHighlightPrayer(LocalTime.now(), currentData, order)
            if (_currentPrayer.value != current) {
                _currentPrayer.value = current
            }
            prayerTimesData = currentData
        }
    }

    private fun startPrayerTimeChecker() {
        viewModelScope.launch {
            while (isActive) {
                try {
                    updateCurrentPrayer()
                    delay(60000L) // Check every minute
                } catch (e: Exception) {
                    Log.e("PrayerViewModel", "خطا در حلقه بررسی نماز", e)
                    delay(60000L) // Wait a minute on error
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        context = null
    }

    @Suppress("unused")
    fun loadUserNotes(@Suppress("UNUSED_PARAMETER") context: Context) {
        viewModelScope.launch {
            // Business logic to load notes
        }
    }

    @Suppress("unused")
    fun setViewMode(newMode: ViewMode) {
        _currentViewMode.value = newMode
    }
}