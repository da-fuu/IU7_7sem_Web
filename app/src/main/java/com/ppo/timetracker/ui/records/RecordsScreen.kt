package com.ppo.timetracker.ui.records

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ppo.domain.model.CategoryId
import com.ppo.timetracker.ui.theme.ColorBox
import org.koin.androidx.compose.koinViewModel
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordsScreen(
    viewModel: RecordsViewModel = koinViewModel()
) {
    val records by viewModel.records.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Manual")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(records) { record ->
                val category = categories.find { it.id == record.categoryId }
                val categoryName = category?.name ?: "Unknown"
                ListItem(
                    headlineContent = { Text(categoryName) },
                    supportingContent = {
                        Text(
                            "${formatInstant(record.startTime)} - ${
                                record.endTime?.let { formatInstant(it) } ?: "Active"
                            }\n${record.description}"
                        )
                    },
                    leadingContent = { ColorBox(category?.colorHex ?: "#808080") }
                )
            }
        }

        if (showAddDialog) {
            AddManualRecordDialog(
                categories = categories,
                onDismiss = { showAddDialog = false },
                onConfirm = { catId, start, end, desc ->
                    viewModel.addManualRecord(catId, start, end, desc)
                    showAddDialog = false
                }
            )
        }
    }
}

private fun formatInstant(instant: Instant): String {
    val formatter = DateTimeFormatter.ofPattern("dd.MM HH:mm")
        .withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddManualRecordDialog(
    categories: List<com.ppo.domain.model.Category>,
    onDismiss: () -> Unit,
    onConfirm: (CategoryId, Instant, Instant, String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()) }
    var description by remember { mutableStateOf("") }

    var startDateTime by remember { mutableStateOf(LocalDateTime.now().minusHours(1)) }
    var endDateTime by remember { mutableStateOf(LocalDateTime.now()) }

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Manual Record") },
        text = {
            Column {
                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.name ?: "Select Category",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = {
                                    Row {
                                        ColorBox(category.colorHex)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(category.name)
                                    }
                                },
                                onClick = {
                                    selectedCategory = category
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Start Time Picker
                Box(modifier = Modifier.fillMaxWidth().clickable { showStartPicker = true }) {
                    OutlinedTextField(
                        value = startDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Start Time") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // End Time Picker
                Box(modifier = Modifier.fillMaxWidth().clickable { showEndPicker = true }) {
                    OutlinedTextField(
                        value = endDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("End Time") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedCategory?.let {
                        val start = startDateTime.atZone(ZoneId.systemDefault()).toInstant()
                        val end = endDateTime.atZone(ZoneId.systemDefault()).toInstant()
                        onConfirm(it.id, start, end, description)
                    }
                },
                enabled = selectedCategory != null
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showStartPicker) {
        DateTimePickerDialog(
            initialDateTime = startDateTime,
            onDismiss = { showStartPicker = false },
            onConfirm = { startDateTime = it; showStartPicker = false }
        )
    }

    if (showEndPicker) {
        DateTimePickerDialog(
            initialDateTime = endDateTime,
            onDismiss = { showEndPicker = false },
            onConfirm = { endDateTime = it; showEndPicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerDialog(
    initialDateTime: LocalDateTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalDateTime) -> Unit
) {
    var datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
    var timePickerState = rememberTimePickerState(
        initialHour = initialDateTime.hour,
        initialMinute = initialDateTime.minute
    )

    var showTimePicker by remember { mutableStateOf(false) }

    if (!showTimePicker) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = { showTimePicker = true }) {
                    Text("Next")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    val date = Instant.ofEpochMilli(datePickerState.selectedDateMillis ?: 0)
                        .atZone(ZoneId.systemDefault()).toLocalDate()
                    val result = LocalDateTime.of(date, LocalTime.of(timePickerState.hour, timePickerState.minute))
                    onConfirm(result)
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Back")
                }
            },
            title = { Text("Select Time") },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }
}
