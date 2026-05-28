package com.ppo.domain.usecase

import com.ppo.domain.model.CategoryId
import com.ppo.domain.model.RecordId
import com.ppo.domain.model.TimeRecord
import com.ppo.domain.model.UserId
import com.ppo.domain.repository.ITimeRecordRepository
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

class StartTimerUseCase(
    private val repository: ITimeRecordRepository
) {
    private val logger = LoggerFactory.getLogger(StartTimerUseCase::class.java)

    suspend operator fun invoke(userId: UserId, categoryId: CategoryId, startTime: Instant): Result<Unit> {
        val activeSession = repository.getActiveSession(userId)
        if (activeSession != null) {
            logger.warn("Failed to start timer: user {} already has an active session", userId.value)
            return Result.failure(IllegalStateException("Сначала остановите текущий таймер"))
        }

        val record = TimeRecord(
            id = RecordId(UUID.randomUUID()),
            userId = userId,
            categoryId = categoryId,
            startTime = startTime,
            endTime = null
        )

        return repository.saveRecord(record).also {
            if (it.isSuccess) logger.info("Timer started for user {} in category {}", userId.value, categoryId.value)
            else logger.error("Failed to start timer for user {}: {}", userId.value, it.exceptionOrNull()?.message)
        }
    }
}
