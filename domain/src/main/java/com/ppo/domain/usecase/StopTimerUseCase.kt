package com.ppo.domain.usecase

import com.ppo.domain.model.BusinessConfig
import com.ppo.domain.model.TimeRecord
import com.ppo.domain.model.UserId
import com.ppo.domain.repository.ITimeRecordRepository
import java.time.Duration
import java.time.Instant

class StopTimerUseCase(
    private val repository: ITimeRecordRepository,
    private val config: BusinessConfig
) {
    suspend operator fun invoke(
        userId: UserId,
        stopTime: Instant = Instant.now()
    ): Result<TimeRecord> {
        val active = repository.getActiveSession(userId)
            ?: return Result.failure(IllegalStateException("Нет активной сессии"))

        if (Duration.between(active.startTime, stopTime).toMinutes() < config.minDurationMinutes) {
            repository.deleteRecord(active.id)
            return Result.failure(IllegalStateException("Сессия менее установленного лимита отменена"))
        }

        val updated = active.copy(endTime = stopTime)
        return repository.saveRecord(updated).map { updated }
    }
}
