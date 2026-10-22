package com.ppo.domain.usecase

import com.ppo.domain.model.RecordHistoryEntry
import com.ppo.domain.model.RecordId
import com.ppo.domain.repository.ITimeRecordRepository

class GetDescriptionHistoryUseCase(
    private val timeRecordRepository: ITimeRecordRepository
) {
    suspend operator fun invoke(recordId: RecordId): Result<List<RecordHistoryEntry>> {
        val record = timeRecordRepository.getRecordById(recordId)
            ?: return Result.failure(NoSuchElementException("Record not found"))
        return Result.success(timeRecordRepository.getDescriptionHistory(recordId))
    }
}
