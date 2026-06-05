package com.ppo.domain.usecase

import com.ppo.domain.model.DescriptionVersion
import com.ppo.domain.model.RecordId
import com.ppo.domain.repository.ITimeRecordRepository

class GetDescriptionHistoryUseCase(
    private val timeRecordRepository: ITimeRecordRepository
) {
    suspend operator fun invoke(recordId: RecordId): Result<List<DescriptionVersion>> {
        return try {
            val history = timeRecordRepository.getDescriptionHistory(recordId)
            Result.success(history)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
