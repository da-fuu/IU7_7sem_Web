package com.ppo.timetracker.ui.records

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
import java.time.Instant

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
                    supportingContent = { Text("${record.startTime} - ${record.endTime ?: "Active"}\n${record.description}") },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddManualRecordDialog(
    categories: List<com.ppo.domain.model.Category>,
    onDismiss: () -> Unit,
    onConfirm: (CategoryId, Instant, Instant, String) -> Unit
) {
    var selectedCategoryId by remember { mutableStateOf<CategoryId?>(categories.firstOrNull()?.id) }
    var startStr by remember { mutableStateOf(Instant.now().minusSeconds(3600).toString()) }
    var endStr by remember { mutableStateOf(Instant.now().toString()) }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Manual Record") },
        text = {
            Column {
                Text("Category ID: ${selectedCategoryId?.value ?: "None"}")
                OutlinedTextField(value = startStr, onValueChange = { startStr = it }, label = { Text("Start ISO") })
                OutlinedTextField(value = endStr, onValueChange = { endStr = it }, label = { Text("End ISO") })
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") })
            }
        },
        confirmButton = {
            Button(onClick = {
                val catId = selectedCategoryId
                if (catId != null) {
                    try {
                        onConfirm(catId, Instant.parse(startStr), Instant.parse(endStr), description)
                    } catch (e: Exception) {
                    }
                }
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
