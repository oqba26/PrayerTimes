package com.oqba26.prayertimes.screens.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.oqba26.prayertimes.utils.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinuteDropdown(
    label: String,
    subtitle: String? = null,
    selectedValue: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    usePersianNumbers: Boolean,
    onInteraction: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = if (selectedValue == 0) "همزمان با نماز" else "${DateUtils.convertToPersianNumbers(selectedValue.toString(), usePersianNumbers)} دقیقه قبل"

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = {
                expanded = it
                onInteraction()
            },
        ) {
            OutlinedTextField(
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                readOnly = true,
                value = selectedLabel,
                onValueChange = {},
                label = { Text(label) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                shape = MaterialTheme.shapes.medium
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                range.forEach { minute ->
                    DropdownMenuItem(
                        text = { Text(if (minute == 0) "همزمان با نماز" else "${DateUtils.convertToPersianNumbers(minute.toString(), usePersianNumbers)} دقیقه قبل") },
                        onClick = {
                            onValueChange(minute)
                            onInteraction()
                            expanded = false
                        }
                    )
                }
            }
        }
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp, end = 16.dp)
            )
        }
    }
}