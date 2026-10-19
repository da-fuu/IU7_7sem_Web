package com.ppo.timetracker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ppo.domain.model.Category
import com.ppo.domain.model.CategoryId
import com.ppo.domain.model.TimeRecord
import com.ppo.domain.usecase.GetCategoriesUseCase
import com.ppo.domain.usecase.GetTimeRecordsUseCase
import com.ppo.domain.usecase.StartTimerUseCase
import com.ppo.domain.usecase.StopTimerUseCase
import com.ppo.timetracker.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

class DashboardViewModel(
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getTimeRecordsUseCase: GetTimeRecordsUseCase,
    private val startTimerUseCase: StartTimerUseCase,
    private val stopTimerUseCase: StopTimerUseCase,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _activeRecord = MutableStateFlow<TimeRecord?>(null)
    val activeRecord: StateFlow<TimeRecord?> = _activeRecord.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun refreshData() {
        val userId = sessionManager.currentUserId ?: return
        viewModelScope.launch {
            _categories.value = getCategoriesUseCase(userId)
            val records = getTimeRecordsUseCase(userId)
            _activeRecord.value = records.find { it.endTime == null }
        }
    }

    fun startTimer(categoryId: CategoryId) {
        val userId = sessionManager.currentUserId ?: return
        viewModelScope.launch {
            val result = startTimerUseCase(userId, categoryId, Instant.now())
            if (result.isSuccess) {
                refreshData()
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Ошибка при запуске таймера"
            }
        }
    }

    fun stopTimer() {
        val userId = sessionManager.currentUserId ?: return
        viewModelScope.launch {
            val result = stopTimerUseCase(userId, Instant.now())
            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message
            }
            refreshData()
        }
    }

    fun clearError() {
        _error.value = null
    }
}
