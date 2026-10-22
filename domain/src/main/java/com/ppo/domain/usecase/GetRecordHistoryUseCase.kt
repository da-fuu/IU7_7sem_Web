package com.ppo.domain.usecase

import com.ppo.domain.model.RecordHistoryEntry
import com.ppo.domain.model.RecordId
import com.ppo.domain.model.UserId
import com.ppo.domain.repository.IRecordHistoryRepository
import com.ppo.domain.repository.ITimeRecordRepository

class GetRecordHistoryUseCase(
    private val timeRecordRepository: ITimeRecordRepository,
    private val recordHistoryRepository: IRecordHistoryRepository
) {
    suspend operator fun invoke(
        recordId: RecordId,
        requestUserId: UserId,
        isAdmin: Boolean = false
    ): Result<List<RecordHistoryEntry>> {
        val record = timeRecordRepository.getRecordById(recordId)
            ?: return Result.failure(NoSuchElementException("Record not found"))

        if (record.userId != requestUserId && !isAdmin) {
            return Result.failure(IllegalAccessException("Access denied"))
        }

        return Result.success(recordHistoryRepository.getHistoryForRecord(recordId))
    }
}
