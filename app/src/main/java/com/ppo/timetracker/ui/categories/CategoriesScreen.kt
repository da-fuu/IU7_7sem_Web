package com.ppo.timetracker.ui.categories

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ppo.timetracker.ui.theme.ColorBox
import org.koin.androidx.compose.koinViewModel
import androidx.core.graphics.toColorInt

@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel = koinViewModel()
) {
    val categories by viewModel.categories.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
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
            items(categories) { category ->
                ListItem(
                    headlineContent = { Text(category.name) },
                    supportingContent = { if (category.isDefault) Text("[Базовая]") else null },
                    leadingContent = { ColorBox(category.colorHex) },
                    trailingContent = {
                        IconButton(onClick = { viewModel.deleteCategory(category) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить")
                        }
                    }
                )
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Добавить")
        }

        if (showAddDialog) {
            AddCategoryDialog(
                isAdmin = isAdmin,
                onDismiss = { showAddDialog = false },
                onConfirm = { name, color, isBase ->
                    viewModel.addCategory(name, color, isBase)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun AddCategoryDialog(
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val colorPresets = listOf(
        "#F44336", "#E91E63", "#9C27B0", "#673AB7",
        "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4",
        "#009688", "#4CAF50", "#8BC34A", "#CDDC39",
        "#FFEB3B", "#FFC107", "#FF9800", "#FF5722",
        "#795548", "#9E9E9E", "#607D8B", "#000000"
    )
    var selectedColor by remember { mutableStateOf(colorPresets[0]) }
    var isBase by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить категорию") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Выберите цвет:", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))

                colorPresets.chunked(5).forEach { rowColors ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        rowColors.forEach { colorHex ->
                            val color = Color(colorHex.toColorInt())
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .padding(4.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (selectedColor == colorHex) 3.dp else 0.dp,
                                        color = if (selectedColor == colorHex) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = colorHex }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (isAdmin) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isBase = !isBase }
                    ) {
                        Checkbox(checked = isBase, onCheckedChange = { isBase = it })
                        Text("Базовая категория")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, selectedColor, isBase) }, enabled = name.isNotBlank()) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
