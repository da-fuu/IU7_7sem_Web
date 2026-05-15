package com.ppo.domain.usecase

import com.ppo.domain.model.UserId
import com.ppo.domain.repository.ITimeRecordRepository
import java.time.Instant

class StopTimerUseCase(
    private val repository: ITimeRecordRepository
) {
    suspend operator fun invoke(userId: UserId, stopTime: Instant): Result<Unit> {
        val activeSession = repository.getActiveSession(userId)
            ?: return Result.failure(IllegalStateException("Нет активной сессии"))

        val updatedSession = activeSession.copy(endTime = stopTime)

        return if (updatedSession.getDurationMinutes() < 1) {
            repository.deleteRecord(activeSession.id)
            Result.failure(IllegalStateException("Сессия менее 1 минуты отменена"))
        } else {
            repository.saveRecord(updatedSession)
        }
    }
}
