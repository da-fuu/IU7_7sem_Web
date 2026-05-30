package com.ppo.timetracker.ui.dashboard

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ppo.timetracker.ui.theme.ColorBox
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onLogout: () -> Unit,
    viewModel: DashboardViewModel = koinViewModel()
) {
    val categories by viewModel.categories.collectAsState()
    val activeRecord by viewModel.activeRecord.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Панель управления") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Выйти")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            if (activeRecord != null) {
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Активный таймер", style = MaterialTheme.typography.titleLarge)
                        Text("Начало: ${activeRecord!!.startTime}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.stopTimer() }) {
                            Text("Остановить")
                        }
                    }
                }
            }

            Text("Выберите категорию для начала", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                items(categories) { category ->
                    ListItem(
                        headlineContent = { Text(category.name) },
                        leadingContent = { ColorBox(category.colorHex) },
                        trailingContent = {
                            Button(
                                onClick = { viewModel.startTimer(category.id) },
                                enabled = activeRecord == null
                            ) {
                                Text("Старт")
                            }
                        }
                    )
                }
            }
        }
    }
}
