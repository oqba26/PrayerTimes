package com.oqba26.prayertimes.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oqba26.prayertimes.models.MultiDate
import com.oqba26.prayertimes.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

// New order: Shamsi, Hijri, Gregorian
enum class CalendarType {
    GREGORIAN,
    HIJRI,
    SHAMSI
}

data class DateConverterUiState(
    val inputDate: MultiDate = DateUtils.getCurrentDate(),
    val sourceCalendar: CalendarType = CalendarType.SHAMSI,
    val result: MultiDate? = null,
    val error: String? = null,
    val showDatePicker: Boolean = false
)

class DateConverterViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DateConverterUiState())
    val uiState: StateFlow<DateConverterUiState> = _uiState.asStateFlow()

    fun onDateSelected(newDate: MultiDate) {
        _uiState.value = _uiState.value.copy(inputDate = newDate, result = null, error = null, showDatePicker = false)
        convertDate()
    }

    fun setSourceCalendar(calendarType: CalendarType) {
        _uiState.value = _uiState.value.copy(sourceCalendar = calendarType, result = null, error = null)
        // Automatically convert the current date when tab is switched
        convertDate()
    }

    fun showDatePicker() {
        _uiState.value = _uiState.value.copy(showDatePicker = true)
    }

    fun hideDatePicker() {
        _uiState.value = _uiState.value.copy(showDatePicker = false)
    }

    private fun convertDate() {
        viewModelScope.launch {
            try {
                val validationPassedDate = validateAndCreateDate(_uiState.value.inputDate, _uiState.value.sourceCalendar)
                _uiState.value = _uiState.value.copy(result = validationPassedDate, error = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "خطا در تبدیل تاریخ", result = null)
            }
        }
    }

    // This function now primarily serves for validation during the creation process
    private fun validateAndCreateDate(date: MultiDate, source: CalendarType): MultiDate {
        return when (source) {
            CalendarType.SHAMSI -> {
                val (y, m, d) = date.getShamsiParts()
                val daysInMonth = DateUtils.daysInShamsiMonth(y, m)
                if (d > daysInMonth) throw IllegalArgumentException("روز نامعتبر برای این ماه شمسی: $d")
                DateUtils.createMultiDateFromShamsi(y, m, d)
            }
            CalendarType.GREGORIAN -> {
                val parts = date.gregorian.split("/").map { it.toInt() }
                val y = parts[0]; val m = parts[1]; val d = parts[2]
                val cal = Calendar.getInstance().apply { set(y, m - 1, 1) }
                val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                if (d > daysInMonth) throw IllegalArgumentException("روز نامعتبر برای این ماه میلادی: $d")
                DateUtils.createMultiDateFromGregorian(y, m, d)
            }
            CalendarType.HIJRI -> {
                val parts = date.hijri.split("/").map { it.toInt() }
                val y = parts[0]; val m = parts[1]; val d = parts[2]
                val isLeap = (11 * y + 14) % 30 < 11
                val daysInMonth = if (m == 12 && isLeap) 30 else if (m % 2 != 0) 30 else 29
                if (d > daysInMonth) throw IllegalArgumentException("روز نامعتبر برای این ماه قمری: $d")
                DateUtils.createMultiDateFromHijri(y, m, d)
            }
        }
    }
}
