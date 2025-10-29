package com.oqba26.prayertimes.screens.widgets

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.oqba26.prayertimes.models.MultiDate
import com.oqba26.prayertimes.models.UserNote
import com.oqba26.prayertimes.utils.DateUtils

@Composable
fun NotesOverviewScreen(
    selectedDate: MultiDate,
    userNote: UserNote?,
    onAddNoteClick: () -> Unit,
    onEditNoteClick: () -> Unit,
    onDeleteClick: () -> Unit,
    isDarkThemeActive: Boolean,
    usePersianNumbers: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (userNote != null) {
            // حالت نمایش یادداشت موجود
            Card(
                modifier = Modifier.fillMaxSize(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp)) { // Box اصلی برای چیدمان روی هم

                    // محتوای اسکرول شونده
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 80.dp), // فضا برای دکمه‌های پایین
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        Text(
                            text = "یادداشت برای: ${DateUtils.convertToPersianNumbers(selectedDate.shamsi, usePersianNumbers)}",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = DateUtils.convertToPersianNumbers(userNote.content, usePersianNumbers),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // دکمه‌های چسبیده به پایین
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (userNote.notificationEnabled && userNote.reminderTimeMillis != null) {
                            val formattedTime = DateUtils.formatTimeMillis(userNote.reminderTimeMillis)
                            val persianTime = DateUtils.convertToPersianNumbers(formattedTime, usePersianNumbers)
                            Text(
                                text = "یادآوری: $persianTime",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val deleteButtonColor = if (isDarkThemeActive) Color(0xFFF2B8B5) else MaterialTheme.colorScheme.error
                            val editButtonColor = if (isDarkThemeActive) Color(0xFFADC6FF) else MaterialTheme.colorScheme.primary

                            TextButton(
                                onClick = onDeleteClick,
                                colors = ButtonDefaults.textButtonColors(contentColor = deleteButtonColor)
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "حذف یادداشت", modifier = Modifier.size(ButtonDefaults.IconSize))
                                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                Text("حذف")
                            }
                            Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                            TextButton(
                                onClick = onEditNoteClick,
                                colors = ButtonDefaults.textButtonColors(contentColor = editButtonColor)
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = "ویرایش یادداشت", modifier = Modifier.size(ButtonDefaults.IconSize))
                                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                Text("ویرایش")
                            }
                        }
                    }
                }
            }
        } else {
            // حالت افزودن یادداشت جدید
            Log.d("NotesOverviewScreen", "یادداشتی برای ${DateUtils.convertToPersianNumbers(selectedDate.shamsi, usePersianNumbers)} وجود ندارد. نمایش دکمه افزودن.")
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "برای افزودن یادداشت روی دکمه + کلیک کنید.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(16.dp))
                FloatingActionButton(
                    onClick = {
                        Log.d("NotesOverviewScreen", "دکمه افزودن یادداشت کلیک شد.")
                        onAddNoteClick()
                    },
                    modifier = Modifier.size(56.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "افزودن یادداشت جدید", modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}
