package com.ppo.timetracker.ui.migration

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ppo.timetracker.migration.MigrationState

@Composable
fun MigrationScreen(
    viewModel: MigrationViewModel,
    onMigrationFinished: () -> Unit
) {
    val state by viewModel.migrationState.collectAsState()
    var showSuccessDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        when (state) {
            MigrationState.SUCCESS -> showSuccessDialog = true
            MigrationState.NOT_NEEDED -> onMigrationFinished()
            else -> {}
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { },
            confirmButton = {
                Button(onClick = {
                    showSuccessDialog = false
                    onMigrationFinished()
                }) {
                    Text("Продолжить")
                }
            },
            title = { Text("Обновление завершено") },
            text = { Text("Ваши данные успешно перенесены на новую базу данных ObjectBox.") }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (state == MigrationState.ERROR) {
                Text("Ошибка при миграции данных", color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onMigrationFinished) {
                    Text("Пропустить")
                }
            } else {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Оптимизируем хранилище данных...")
                Text("Пожалуйста, подождите", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
