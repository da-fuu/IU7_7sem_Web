package com.ppo.timetracker.ui.records

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ppo.domain.model.CategoryId
import com.ppo.timetracker.ui.theme.ColorBox
import org.koin.androidx.compose.koinViewModel
import java.time.*
import java.time.format.DateTimeFormatter

@Composable
fun RecordsScreen(
    viewModel: RecordsViewModel = koinViewModel()
) {
    val records by viewModel.records.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(records) { record ->
                val category = categories.find { it.id == record.categoryId }
                val categoryName = category?.name ?: "Неизвестно"
                
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ColorBox(category?.colorHex ?: "#808080")
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = categoryName,
                                style = MaterialTheme.typography.titleMedium
                            )
                            val timeRange = "${formatInstant(record.startTime)} - ${
                                record.endTime?.let { formatInstant(it) } ?: "Активно"
                            }"
                            Text(
                                text = if (record.description.isNotBlank()) {
                                    "$timeRange\n${record.description}"
                                } else {
                                    timeRange
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Добавить запись")
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
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить запись") },
        text = {
            Column(modifier = Modifier.verticalScroll(scrollState)) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.name ?: "Выберите категорию",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Категория") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.heightIn(max = 280.dp)
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

                Box(modifier = Modifier.fillMaxWidth().clickable { showStartPicker = true }) {
                    OutlinedTextField(
                        value = startDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Время начала") },
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

                Box(modifier = Modifier.fillMaxWidth().clickable { showEndPicker = true }) {
                    OutlinedTextField(
                        value = endDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Время окончания") },
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
                    label = { Text("Описание") },
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
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
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
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
    val timePickerState = rememberTimePickerState(
        initialHour = initialDateTime.hour,
        initialMinute = initialDateTime.minute
    )

    var showTimePicker by remember { mutableStateOf(false) }

    if (!showTimePicker) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = { showTimePicker = true }) {
                    Text("Далее")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Отмена")
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
                    Text("ОК")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Назад")
                }
            },
            title = { Text("Выберите время") },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }
}
