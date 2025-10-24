package com.oqba26.prayertimes.screens.widgets

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.oqba26.prayertimes.R
import com.oqba26.prayertimes.models.MultiDate
import com.oqba26.prayertimes.screens.DarkThemeOnPurpleText
import com.oqba26.prayertimes.screens.DarkThemePurpleBackground
import com.oqba26.prayertimes.screens.ViewMode
import com.oqba26.prayertimes.utils.ShareUtils

@Composable
fun BottomBar(
    currentDate: MultiDate,
    prayers: Map<String, String>,
    onOpenSettings: () -> Unit,
    onOpenAlarms: () -> Unit,
    onToggleTheme: () -> Unit,
    isDark: Boolean,
    currentViewMode: ViewMode,
    onViewModeChange: (ViewMode) -> Unit,
    usePersianNumbers: Boolean,
    use24HourFormat: Boolean
) {
    val context = LocalContext.current
    var showMoreMenu by remember { mutableStateOf(false) }

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

    // You can change the height of the bottom bar from here
    NavigationBar(
        modifier = Modifier.height(110.dp),
        containerColor = containerColor
    ) {
        // Prayer Times
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

        // Notes
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

        // Alarms
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

        // More menu
        Box(
            Modifier
                .weight(1f)
                //.fillMaxHeight()
                .clickable { showMoreMenu = true },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val moreColor = if (showMoreMenu) selectedColor else unselectedColor
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = stringResource(id = R.string.more),
                    tint = moreColor
                )
                Text(
                    text = stringResource(id = R.string.more),
                    color = moreColor,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            DropdownMenu(
                expanded = showMoreMenu,
                onDismissRequest = { showMoreMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("تغییر تم") },
                    onClick = {
                        onToggleTheme()
                        showMoreMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.BrightnessMedium, "تغییر تم") }
                )
                DropdownMenuItem(
                    text = { Text("تنظیمات") },
                    onClick = {
                        onOpenSettings()
                        showMoreMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.Settings, "تنظیمات") }
                )
                DropdownMenuItem(
                    text = { Text("اشتراک گذاری") },
                    onClick = {
                        val text = ShareUtils.buildShareText(currentDate, prayers, usePersianNumbers, use24HourFormat)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(intent, "اشتراک گذاری با"))
                        showMoreMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.Share, "اشتراک گذاری") }
                )
            }
        }
    }
}
