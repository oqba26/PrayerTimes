package com.oqba26.prayertimes.screens.widgets

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.oqba26.prayertimes.R
import com.oqba26.prayertimes.screens.DarkThemeOnPurpleText
import com.oqba26.prayertimes.screens.DarkThemePurpleBackground
import com.oqba26.prayertimes.screens.ViewMode

@Composable
fun BottomBar(
    onOpenSettings: () -> Unit,
    onOpenAlarms: () -> Unit,
    onOpenQibla: () -> Unit,
    onToggleTheme: () -> Unit,
    isDark: Boolean,
    currentViewMode: ViewMode,
    onViewModeChange: (ViewMode) -> Unit
) {
    val containerColor: Color
    val selectedColor: Color
    val unselectedColor: Color
    val indicatorColor: Color

    if (isDark) {
        containerColor = DarkThemePurpleBackground
        selectedColor = DarkThemeOnPurpleText
        unselectedColor = DarkThemeOnPurpleText.copy(alpha = 0.7f)
        indicatorColor = DarkThemeOnPurpleText.copy(alpha = 0.3f)
    } else {
        containerColor = MaterialTheme.colorScheme.primary
        selectedColor = MaterialTheme.colorScheme.onPrimary
        unselectedColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
        indicatorColor = MaterialTheme.colorScheme.inversePrimary
    }

    // کمی کوتاه‌تر از قبل، تا جا برای ۵ آیکن باشد
    NavigationBar(
        modifier = Modifier
            .height(110.dp)          // مثل قبل
            .navigationBarsPadding(), // تا زیر نوار ناوبری اندروید نره
        containerColor = containerColor
    ) {
        // اوقات نماز
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Filled.AccessTime,
                    contentDescription = "اوقات شرعی",
                    tint = if (currentViewMode == ViewMode.PRAYER_TIMES) selectedColor else unselectedColor
                )
            },
            label = {
                Text(
                    "اوقات",
                    color = if (currentViewMode == ViewMode.PRAYER_TIMES) selectedColor else unselectedColor
                )
            },
            selected = currentViewMode == ViewMode.PRAYER_TIMES,
            onClick = { onViewModeChange(ViewMode.PRAYER_TIMES) },
            colors = NavigationBarItemDefaults.colors(indicatorColor = indicatorColor)
        )

        // یادداشت‌ها
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Filled.EditNote,
                    contentDescription = "یادداشت ها",
                    tint = if (currentViewMode == ViewMode.NOTES) selectedColor else unselectedColor
                )
            },
            label = {
                Text(
                    "یادداشت",
                    color = if (currentViewMode == ViewMode.NOTES) selectedColor else unselectedColor
                )
            },
            selected = currentViewMode == ViewMode.NOTES,
            onClick = { onViewModeChange(ViewMode.NOTES) },
            colors = NavigationBarItemDefaults.colors(indicatorColor = indicatorColor)
        )

        // قبله‌نما
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Filled.Explore,
                    contentDescription = "قبله‌نما",
                    tint = unselectedColor
                )
            },
            label = {
                Text(
                    "قبله‌نما",
                    color = unselectedColor
                )
            },
            selected = false,
            onClick = onOpenQibla,
            colors = NavigationBarItemDefaults.colors(indicatorColor = indicatorColor)
        )

        // آلارم‌ها
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Default.Alarm,
                    contentDescription = stringResource(id = R.string.alarm),
                    tint = unselectedColor
                )
            },
            label = {
                Text(
                    stringResource(id = R.string.alarm),
                    color = unselectedColor
                )
            },
            selected = false,
            onClick = onOpenAlarms,
            colors = NavigationBarItemDefaults.colors(indicatorColor = indicatorColor)
        )

        // تنظیمات
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "تنظیمات",
                    tint = unselectedColor
                )
            },
            label = {
                Text(
                    "تنظیمات",
                    color = unselectedColor
                )
            },
            selected = false,
            onClick = onOpenSettings,
            colors = NavigationBarItemDefaults.colors(indicatorColor = indicatorColor)
        )

        // تغییر تم
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Default.BrightnessMedium,
                    contentDescription = "تغییر تم",
                    tint = unselectedColor
                )
            },
            label = {
                Text(
                    "تم",
                    color = unselectedColor
                )
            },
            selected = false,
            onClick = onToggleTheme,
            colors = NavigationBarItemDefaults.colors(indicatorColor = indicatorColor)
        )
    }
}
