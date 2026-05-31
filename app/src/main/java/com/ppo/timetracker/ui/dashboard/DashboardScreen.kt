package com.ppo.timetracker.ui.dashboard

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ppo.domain.model.Category
import com.ppo.domain.model.TimeRecord
import com.ppo.timetracker.ui.theme.ColorBox
import org.koin.androidx.compose.koinViewModel
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

@Composable
fun DashboardScreen(
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

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp)) {
            activeRecord?.let { record ->
                val activeCategory = categories.find { it.id == record.categoryId }
                ActiveTimerCard(
                    activeRecord = record,
                    category = activeCategory,
                    onStop = { viewModel.stopTimer() }
                )
            }

            Text("Выберите категорию для начала", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
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

@Composable
fun ActiveTimerCard(
    activeRecord: TimeRecord,
    category: Category?,
    onStop: () -> Unit
) {
    var currentTime by remember { mutableStateOf(Instant.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Instant.now()
            delay(1000)
        }
    }

    val duration = Duration.between(activeRecord.startTime, currentTime)
    val hours = duration.toHours()
    val minutes = (duration.toMinutes() % 60).toInt()
    val seconds = (duration.seconds % 60).toInt()
    val durationText = String.format("%02d:%02d:%02d", hours, minutes, seconds)

    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())
    val startTimeText = timeFormatter.format(activeRecord.startTime)

    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Активный таймер",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Начало в $startTimeText",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (category != null) {
                    ColorBox(category.colorHex)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    Text(
                        text = "Загрузка...",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = durationText,
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onStop,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Остановить")
            }
        }
    }
}
