package com.oqba26.prayertimes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.oqba26.prayertimes.screens.CalendarScreen
import com.oqba26.prayertimes.ui.theme.PrayerTimesTheme
import com.oqba26.prayertimes.utils.getCurrentDate
import com.oqba26.prayertimes.viewmodels.PrayerViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: PrayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 👇 این خط واجبه اگر ViewModel از LoadData استفاده می‌کنه
        viewModel.loadData(applicationContext)

        setContent {
            // 👇 مدیریت حالت شب
            var isDarkMode by remember {
                mutableStateOf(false)
            }

            PrayerTimesTheme(darkTheme = isDarkMode) {
                CalendarScreenEntryPoint(
                    viewModel = viewModel,
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = { isDarkMode = !isDarkMode }
                )
            }
        }
    }
}

@Composable
fun CalendarScreenEntryPoint(
    viewModel: PrayerViewModel,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit
) {
    var currentDate by remember { mutableStateOf(getCurrentDate()) }
    val uiState = viewModel.uiState.collectAsState().value

    LaunchedEffect(currentDate) {
        viewModel.updateDate(currentDate)
    }

    CalendarScreen(
        currentDate = currentDate,
        uiState = uiState,
        onDateChange = { currentDate = it },
        onRetry = { viewModel.updateDate(currentDate) },
        viewModel = viewModel,
        onToggleDarkMode = onToggleDarkMode
    )
}



