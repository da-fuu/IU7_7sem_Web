package com.ppo.domain.usecase

import com.ppo.domain.model.BusinessConfig
import com.ppo.domain.model.UserId
import com.ppo.domain.repository.ITimeRecordRepository
import org.slf4j.LoggerFactory
import java.time.Instant

class StopTimerUseCase(
    private val repository: ITimeRecordRepository,
    private val config: BusinessConfig
) {
    private val logger = LoggerFactory.getLogger(StopTimerUseCase::class.java)

    suspend operator fun invoke(userId: UserId, stopTime: Instant): Result<Unit> {
        val activeSession = repository.getActiveSession(userId) ?: run {
            logger.warn("Failed to stop timer: no active session for user {}", userId.value)
            return Result.failure(IllegalStateException("Нет активной сессии"))
        }

        val updatedSession = activeSession.copy(endTime = stopTime)

        return if (updatedSession.getDurationMinutes() < config.minDurationMinutes) {
            logger.warn(
                "Session for user {} is too short ({} min), deleting.",
                userId.value,
                updatedSession.getDurationMinutes()
            )
            repository.deleteRecord(activeSession.id)
            Result.failure(IllegalStateException("Сессия менее установленного лимита отменена"))
        } else {
            repository.saveRecord(updatedSession).also {
                if (it.isSuccess) logger.info("Timer stopped for user {}", userId.value)
                else logger.error("Failed to stop timer for user {}: {}", userId.value, it.exceptionOrNull()?.message)
            }
        }
    }
}
