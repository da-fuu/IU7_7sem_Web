package com.ppo.timetracker.ui.migration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ppo.timetracker.migration.MigrationManager
import com.ppo.timetracker.migration.MigrationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MigrationViewModel(
    private val migrationManager: MigrationManager
) : ViewModel() {

    private val _migrationState = MutableStateFlow(MigrationState.IN_PROGRESS)
    val migrationState: StateFlow<MigrationState> = _migrationState

    init {
        viewModelScope.launch {
            _migrationState.value = migrationManager.checkAndMigrate()
        }
    }
}
