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
import androidx.compose.ui.text.style.TextOverflow
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
    usePersianNumbers: Boolean, // Added usePersianNumbers parameter
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
            Card(
                modifier = Modifier
                    .fillMaxSize(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            // Apply usePersianNumbers to selectedDate.shamsi
                            text = "یادداشت برای: ${DateUtils.convertToPersianNumbers(selectedDate.shamsi, usePersianNumbers)}",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            // Content itself might or might not need number conversion based on its nature
                            // If userNote.content can contain numbers that should be Persian, apply conversion.
                            // For now, assuming content is primarily text.
                            text = DateUtils.convertToPersianNumbers(userNote.content, usePersianNumbers),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            maxLines = 10,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (userNote.notificationEnabled && userNote.reminderTimeMillis != null) {
                            Text(
                                // Apply usePersianNumbers to reminder time
                                text = "یادآوری: ${DateUtils.convertToPersianNumbers(DateUtils.formatTimeMillis(userNote.reminderTimeMillis), usePersianNumbers)}",
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
                                Icon(Icons.Filled.Delete, contentDescription = DateUtils.convertToPersianNumbers("حذف یادداشت", usePersianNumbers), modifier = Modifier.size(ButtonDefaults.IconSize))
                                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                Text(DateUtils.convertToPersianNumbers("حذف", usePersianNumbers))
                            }
                            Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                            TextButton(
                                onClick = onEditNoteClick,
                                colors = ButtonDefaults.textButtonColors(contentColor = editButtonColor)
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = DateUtils.convertToPersianNumbers("ویرایش یادداشت", usePersianNumbers), modifier = Modifier.size(ButtonDefaults.IconSize))
                                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                Text(DateUtils.convertToPersianNumbers("ویرایش", usePersianNumbers))
                            }
                        }
                    }
                }
            }
        } else {
            Log.d("NotesOverviewScreen", "UserNote is null, showing FAB for date: ${DateUtils.convertToPersianNumbers(selectedDate.shamsi, usePersianNumbers)}")
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = DateUtils.convertToPersianNumbers("برای افزودن یادداشت روی دکمه + کلیک کنید.", usePersianNumbers),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(16.dp))
                FloatingActionButton(
                    onClick = {
                        Log.d("NotesOverviewScreen", "FAB onClick triggered")
                        onAddNoteClick()
                    },
                    modifier = Modifier.size(56.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Filled.Add, contentDescription = DateUtils.convertToPersianNumbers("افزودن یادداشت جدید", usePersianNumbers), modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}
