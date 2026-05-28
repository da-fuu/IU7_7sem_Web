package com.ppo.domain.usecase

import com.ppo.domain.model.BusinessConfig
import com.ppo.domain.model.TimeRecord
import com.ppo.domain.repository.ITimeRecordRepository
import org.slf4j.LoggerFactory

class AddManualRecordUseCase(
    private val repository: ITimeRecordRepository,
    private val config: BusinessConfig
) {
    private val logger = LoggerFactory.getLogger(AddManualRecordUseCase::class.java)

    suspend operator fun invoke(record: TimeRecord): Result<Unit> {
        logger.info(
            "Attempting to add manual record for user: {}, category: {}",
            record.userId.value,
            record.categoryId.value
        )
        val endTime = record.endTime ?: return Result.failure(IllegalArgumentException("Время окончания обязательно"))

        if (endTime.isBefore(record.startTime)) {
            logger.warn("Invalid time range: end {} is before start {}", endTime, record.startTime)
            return Result.failure(IllegalArgumentException("Конец промежутка не может быть раньше начала"))
        }

        if (record.getDurationMinutes() < config.minDurationMinutes) {
            logger.warn(
                "Record duration {} is below minimum {}",
                record.getDurationMinutes(),
                config.minDurationMinutes
            )
            return Result.failure(IllegalArgumentException("Запись слишком короткая"))
        }

        val overlapping = repository.getOverlappingRecords(
            userId = record.userId,
            start = record.startTime,
            end = endTime,
            excludeRecordId = null
        )

        if (overlapping.isNotEmpty()) {
            logger.warn("Overlapping records found for user {}", record.userId.value)
            return Result.failure(IllegalStateException("Пересечение времени"))
        }

        return repository.saveRecord(record).also {
            if (it.isSuccess) logger.info("Successfully saved manual record")
            else logger.error("Failed to save record: {}", it.exceptionOrNull()?.message)
        }
    }
}
