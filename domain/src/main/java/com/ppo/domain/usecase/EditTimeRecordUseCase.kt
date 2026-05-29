package com.ppo.domain.usecase

import com.ppo.domain.model.BusinessConfig
import com.ppo.domain.model.TimeRecord
import com.ppo.domain.repository.ITimeRecordRepository
import org.slf4j.LoggerFactory

class EditTimeRecordUseCase(
    private val repository: ITimeRecordRepository,
    private val config: BusinessConfig
) {
    private val logger = LoggerFactory.getLogger(EditTimeRecordUseCase::class.java)

    suspend operator fun invoke(updatedRecord: TimeRecord): Result<Unit> {
        logger.info("Attempting to edit time record: {}", updatedRecord.id.value)
        val endTime =
            updatedRecord.endTime ?: run {
                logger.warn("Edit failed: missing end time for record {}", updatedRecord.id.value)
                return Result.failure(IllegalArgumentException("Время окончания обязательно"))
            }

        if (endTime.isBefore(updatedRecord.startTime)) {
            logger.warn("Edit failed: invalid time range for record {}", updatedRecord.id.value)
            return Result.failure(IllegalArgumentException("Конец промежутка не может быть раньше начала"))
        }

        if (updatedRecord.getDurationMinutes() < config.minDurationMinutes) {
            logger.warn(
                "Edit failed: record {} duration {} below minimum",
                updatedRecord.id.value,
                updatedRecord.getDurationMinutes()
            )
            return Result.failure(IllegalArgumentException("Запись слишком короткая"))
        }

        val overlapping = repository.getOverlappingRecords(
            userId = updatedRecord.userId,
            start = updatedRecord.startTime,
            end = endTime,
            excludeRecordId = updatedRecord.id
        )

        if (overlapping.isNotEmpty()) {
            logger.warn("Edit failed: record {} overlaps with existing records", updatedRecord.id.value)
            return Result.failure(IllegalStateException("Пересечение времени"))
        }

        return repository.saveRecord(updatedRecord).also {
            if (it.isSuccess) logger.info("Successfully updated record {}", updatedRecord.id.value)
            else logger.error("Failed to update record {}: {}", updatedRecord.id.value, it.exceptionOrNull()?.message)
        }
    }
}
