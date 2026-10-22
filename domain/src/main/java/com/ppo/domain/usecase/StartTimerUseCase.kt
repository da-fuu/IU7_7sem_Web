package com.ppo.domain.usecase

import com.ppo.domain.model.CategoryId
import com.ppo.domain.model.RecordId
import com.ppo.domain.model.TimeRecord
import com.ppo.domain.model.UserId
import com.ppo.domain.repository.ITimeRecordRepository
import java.time.Instant
import java.util.UUID

class StartTimerUseCase(
    private val repository: ITimeRecordRepository
) {
    suspend operator fun invoke(
        userId: UserId,
        categoryId: CategoryId,
        startTime: Instant = Instant.now()
    ): Result<TimeRecord> {
        val active = repository.getActiveSession(userId)
        if (active != null) {
            return Result.failure(IllegalStateException("Сначала остановите текущий таймер"))
        }
        val record = TimeRecord(
            id = RecordId(UUID.randomUUID()),
            userId = userId,
            categoryId = categoryId,
            startTime = startTime,
            endTime = null
        )
        return repository.saveRecord(record).map { record }
    }
}
