package com.oqba26.prayertimes.viewmodels
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oqba26.prayertimes.models.MultiDate
import com.oqba26.prayertimes.utils.DateUtils.getCurrentDate
import com.oqba26.prayertimes.utils.PrayerUtils.getCurrentPrayerNameFixed
import com.oqba26.prayertimes.utils.PrayerUtils.loadPrayerTimes
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PrayerViewModel : ViewModel() {
    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Error(val message: String, val throwable: Throwable? = null) : Result<Nothing>()
        object Loading : Result<Nothing>()
    }

    private val _currentDate = MutableStateFlow(getCurrentDate())
    val currentDate: StateFlow<MultiDate> = _currentDate.asStateFlow()

    private val _uiState = MutableStateFlow<Result<Map<String, String>>>(Result.Loading)
    val uiState: StateFlow<Result<Map<String, String>>> = _uiState.asStateFlow()

    private val _currentPrayer = MutableStateFlow("")
    val currentPrayer: StateFlow<String> = _currentPrayer.asStateFlow()

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

    fun refreshDate() {
        val today = getCurrentDate()
        updateDate(today)
    }

    fun retry() {
        context?.let {
            loadPrayerTimesForDate(_currentDate.value)
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
                val times = loadPrayerTimes(ctx, date)
                if (times.isEmpty()) {
                    _uiState.value = Result.Error("اوقات شرعی برای این تاریخ موجود نیست")
                } else {
                    prayerTimesData = times
                    _uiState.value = Result.Success(times)
                    updateCurrentPrayer() // به‌روزرسانی نماز فعلی پس از دریافت داده‌ها
                    Log.d("PrayerViewModel", "داده‌های نماز به‌روز شد: ${times.keys}")
                    // تست وجود عشاء
                    if (times.containsKey("عشاء")) {
                        Log.d("PrayerViewModel", "✅ عشاء موجود است: ${times["عشاء"]}")
                    } else {
                        Log.d("PrayerViewModel", "❌ عشاء موجود نیست!")
                        Log.d("PrayerViewModel", "کلیدهای دریافتی: ${times.keys.toList()}")
                    }
                }
            } catch (e: Exception) {
                val errorMessage = e.localizedMessage ?: "خطا در دریافت داده‌ها"
                _uiState.value = Result.Error(errorMessage, e)
                Log.e("PrayerViewModel", "خطا در دریافت داده‌ها", e)
            }
        }
    }

    private fun updateCurrentPrayer() {
        // استفاده از uiState به جای prayerTimesData
        val currentData = when (val state = _uiState.value) {
            is Result.Success -> state.data
            else -> {
                android.util.Log.w("PrayerViewModel", "uiState در حالت Success نیست: ${_uiState.value}")
                return
            }
        }

        if (currentData.isNotEmpty()) {
            val current = getCurrentPrayerNameFixed(currentData)

            // لاگ وضعیت قبل و بعد از به‌روزرسانی
            android.util.Log.d("PrayerViewModel", "به‌روزرسانی نماز فعلی:")
            android.util.Log.d("PrayerViewModel", "  مقدار قبلی: ${_currentPrayer.value}")
            android.util.Log.d("PrayerViewModel", "  مقدار جدید: $current")
            android.util.Log.d("PrayerViewModel", "  آیا تغییر کرده?: ${_currentPrayer.value != current}")

            _currentPrayer.value = current

            // لاگ وضعیت تمام نمازها برای مقایسه
            android.util.Log.d("PrayerViewModel", "وضعیت نمازها:")
            currentData.forEach { (prayer, time) ->
                val isCurrent = prayer == current
                android.util.Log.d("PrayerViewModel", "  $prayer: $time ${if (isCurrent) "(فعلی)" else ""}")
            }

            // به‌روزرسانی prayerTimesData نیز
            prayerTimesData = currentData
        } else {
            android.util.Log.w("PrayerViewModel", "داده‌های نماز خالی است، به‌روزرسانی نماز فعلی انجام نشد")
            android.util.Log.w("PrayerViewModel", "currentData size: ${currentData.size}")
            android.util.Log.w("PrayerViewModel", "uiState: ${_uiState.value}")
        }
    }

    private fun startPrayerTimeChecker() {
        viewModelScope.launch {
            while (isActive) {
                try {
                    updateCurrentPrayer()

                    // محاسبه زمان تا نماز بعدی
                    val delayUntilNext = calculateDelayUntilNextPrayer()

                    // محاسبه بازه چک کردن دینامیک
                    val checkInterval = if (delayUntilNext < 60000) 10000L else 60000L

                    android.util.Log.d(
                        "PrayerViewModel",
                        "چک خودکار نماز فعلی - تا نماز بعدی: ${delayUntilNext / 1000} ثانیه, بازه چک: ${checkInterval / 1000} ثانیه"
                    )

                    delay(checkInterval)
                } catch (e: Exception) {
                    android.util.Log.e("PrayerViewModel", "خطا در حلقه بررسی نماز", e)
                    delay(60000L) // در صورت خطا، 1 دقیقه صبر کن
                }
            }
            android.util.Log.d("PrayerViewModel", "حلقه بررسی نماز متوقف شد")
        }
    }

    private fun calculateDelayUntilNextPrayer(): Long {
        if (prayerTimesData.isEmpty()) return 60000L
        try {
            val currentTime = java.time.LocalTime.now()
            val formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
            val prayerOrder = listOf("طلوع بامداد", "طلوع خورشید", "ظهر", "عصر", "غروب", "عشاء")
            val prayerTimes = prayerOrder.mapNotNull { prayer ->
                prayerTimesData[prayer]?.let { timeStr ->
                    try {
                        prayer to java.time.LocalTime.parse(timeStr, formatter)
                    } catch (e: Exception) {
                        null
                    }
                }
            }

            // پیدا کردن نزدیک‌ترین نماز بعدی
            for ((_, time) in prayerTimes) {
                if (currentTime < time) {
                    val duration = java.time.Duration.between(currentTime, time)
                    return duration.toMillis()
                }
            }

            // اگر همه نمازهای امروز گذشت، تا فجر فردا
            prayerTimes.firstOrNull()?.second?.let { fajrTime ->
                val tomorrow = fajrTime.plusHours(24)
                val duration = java.time.Duration.between(currentTime, tomorrow)
                return duration.toMillis()
            }

            return 60000L // پیش‌فرض یک دقیقه
        } catch (e: Exception) {
            Log.e("PrayerViewModel", "خطا در محاسبه زمان تا نماز بعدی", e)
            return 60000L
        }
    }

    override fun onCleared() {
        super.onCleared()
        context = null
    }
}