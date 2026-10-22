package com.ppo.domain.usecase

import com.ppo.domain.model.RecordId
import com.ppo.domain.model.UserId
import com.ppo.domain.repository.ITimeRecordRepository

class DeleteRecordUseCase(
    private val timeRecordRepository: ITimeRecordRepository
) {
    suspend operator fun invoke(
        recordId: RecordId,
        requestUserId: UserId,
        isAdmin: Boolean = false
    ): Result<Unit> {
        val record = timeRecordRepository.getRecordById(recordId)
            ?: return Result.failure(NoSuchElementException("Record not found"))

        if (record.userId != requestUserId && !isAdmin) {
            return Result.failure(IllegalAccessException("Access denied"))
        }

        return timeRecordRepository.deleteRecord(recordId)
    }
}
