package com.ppo.timetracker.ui.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ppo.domain.model.Category
import com.ppo.domain.model.CategoryId
import com.ppo.domain.model.DescriptionVersion
import com.ppo.domain.model.RecordId
import com.ppo.domain.model.TimeRecord
import com.ppo.domain.model.VersionId
import com.ppo.domain.usecase.*
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
    private val updateRecordDescriptionUseCase: UpdateRecordDescriptionUseCase,
    private val rollbackRecordDescriptionUseCase: RollbackRecordDescriptionUseCase,
    private val getDescriptionHistoryUseCase: GetDescriptionHistoryUseCase,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _records = MutableStateFlow<List<TimeRecord>>(emptyList())
    val records: StateFlow<List<TimeRecord>> = _records.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _historyList = MutableStateFlow<List<DescriptionVersion>>(emptyList())
    val historyList: StateFlow<List<DescriptionVersion>> = _historyList.asStateFlow()

    private val _selectedRecordIdForHistory = MutableStateFlow<RecordId?>(null)
    val selectedRecordIdForHistory: StateFlow<RecordId?> = _selectedRecordIdForHistory.asStateFlow()

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

    fun loadHistory(recordId: RecordId) {
        viewModelScope.launch {
            val result = getDescriptionHistoryUseCase(recordId)
            result.onSuccess {
                _historyList.value = it
                _selectedRecordIdForHistory.value = recordId
            }.onFailure {
                _error.value = it.message ?: "Ошибка при загрузке истории"
            }
        }
    }

    fun closeHistory() {
        _selectedRecordIdForHistory.value = null
        _historyList.value = emptyList()
    }

    fun updateDescription(recordId: RecordId, newDesc: String) {
        viewModelScope.launch {
            val result = updateRecordDescriptionUseCase(recordId, newDesc)
            result.onSuccess {
                refresh()
                loadHistory(recordId)
            }.onFailure {
                _error.value = it.message ?: "Ошибка при обновлении описания"
            }
        }
    }

    fun rollbackDescription(recordId: RecordId, versionId: VersionId) {
        viewModelScope.launch {
            val result = rollbackRecordDescriptionUseCase(recordId, versionId)
            result.onSuccess {
                refresh()
                loadHistory(recordId)
            }.onFailure {
                _error.value = it.message ?: "Ошибка при откате описания"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
