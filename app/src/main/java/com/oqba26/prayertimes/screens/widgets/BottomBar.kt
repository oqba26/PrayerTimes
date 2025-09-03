package com.oqba26.prayertimes.screens.widgets

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.oqba26.prayertimes.models.MultiDate
import com.oqba26.prayertimes.utils.ShareUtils

@Composable
fun BottomBar(
    currentDate: MultiDate,
    prayers: Map<String, String>,
    onToggleDarkMode: () -> Unit,
    onOpenSettings: () -> Unit,
    height: Dp = 32.dp
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(Color(0xFF00ACC1))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {

            IconButton(onClick = onOpenSettings) {   // ← این
                Icon(Icons.Default.Settings, contentDescription = "تنظیمات", tint = Color.White)
            }

            // شب/روز
            IconButton(onClick = onToggleDarkMode) {
                Icon(
                    imageVector = Icons.Default.Nightlight,
                    contentDescription = "تغییر حالت شب/روز",
                    tint = Color.White
                )
            }

            // اشتراک‌گذاری
            IconButton(onClick = {
                val text = ShareUtils.buildShareText(currentDate, prayers)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری با"))
            }) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "اشتراک‌گذاری",
                    tint = Color.White
                )
            }
        }
    }
}