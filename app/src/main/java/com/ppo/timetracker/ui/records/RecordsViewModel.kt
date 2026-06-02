package com.ppo.timetracker.ui.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ppo.domain.model.Category
import com.ppo.domain.model.CategoryId
import com.ppo.domain.model.RecordId
import com.ppo.domain.model.TimeRecord
import com.ppo.domain.usecase.AddManualRecordUseCase
import com.ppo.domain.usecase.GetCategoriesUseCase
import com.ppo.domain.usecase.GetTimeRecordsUseCase
import com.ppo.timetracker.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

class RecordsViewModel(
    private val getTimeRecordsUseCase: GetTimeRecordsUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val addManualRecordUseCase: AddManualRecordUseCase,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _records = MutableStateFlow<List<TimeRecord>>(emptyList())
    val records: StateFlow<List<TimeRecord>> = _records.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun refresh() {
        val userId = sessionManager.currentUserId ?: return
        viewModelScope.launch {
            _records.value = getTimeRecordsUseCase(userId)
            _categories.value = getCategoriesUseCase(userId)
        }
    }

    fun addManualRecord(categoryId: CategoryId, start: Instant, end: Instant, desc: String) {
        val userId = sessionManager.currentUserId ?: return
        viewModelScope.launch {
            val record = TimeRecord(
                id = RecordId(UUID.randomUUID()),
                userId = userId,
                categoryId = categoryId,
                startTime = start,
                endTime = end,
                description = desc
            )
            val result = addManualRecordUseCase(record)
            result.onSuccess {
                refresh()
            }.onFailure {
                _error.value = it.message ?: "Ошибка при сохранении записи"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
