package com.oqba26.prayertimes.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oqba26.prayertimes.ui.AppFonts

@Composable
private fun SettingsTopBar(
    title: String,
    onClose: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت")
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SettingsScreen(
    currentFontId: String,
    onSelectFont: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val fonts = remember { AppFonts.catalog(context) }

    Scaffold(
        topBar = { SettingsTopBar(title = "تنظیمات", onClose = onClose) }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            Text(
                text = "فونت",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color.Gray
            )

            if (fonts.isEmpty()) {
                Text(
                    text = "فونتی یافت نشد. فایل‌های فونت را در مسیر res/font اضافه کنید.",
                    modifier = Modifier.padding(16.dp),
                    color = Color(0xFF555555)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(fonts) { entry ->
                        val selected = entry.id == currentFontId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectFont(entry.id) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = { onSelectFont(entry.id) }
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(entry.label)
                                Text(
                                    text = "نمونه پیش‌نمایش ۱۲۳۴۵ ابجد ابجد",
                                    fontSize = 14.sp,
                                    color = Color(0xFF555555),
                                    fontFamily = entry.family,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Divider()
                    }
                }
            }
        }
    }
}