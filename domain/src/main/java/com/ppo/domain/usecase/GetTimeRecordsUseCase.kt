package com.ppo.domain.usecase

import com.ppo.domain.model.TimeRecord
import com.ppo.domain.model.UserId
import com.ppo.domain.repository.ITimeRecordRepository

class GetTimeRecordsUseCase(
    private val repository: ITimeRecordRepository
) {
    suspend operator fun invoke(userId: UserId): List<TimeRecord> {
        return repository.getTimeRecords(userId)
    }
}
