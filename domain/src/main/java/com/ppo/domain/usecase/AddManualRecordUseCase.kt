package com.ppo.domain.usecase

import com.ppo.domain.model.BusinessConfig
import com.ppo.domain.model.TimeRecord
import com.ppo.domain.repository.ITimeRecordRepository
import java.time.Duration

class AddManualRecordUseCase(
    private val repository: ITimeRecordRepository,
    private val config: BusinessConfig
) {
    suspend operator fun invoke(record: TimeRecord): Result<TimeRecord> {
        if (record.endTime == null) {
            return Result.failure(IllegalArgumentException("Время окончания обязательно"))
        }
        if (!record.endTime.isAfter(record.startTime)) {
            return Result.failure(IllegalArgumentException("Конец промежутка не может быть раньше начала"))
        }
        if (Duration.between(record.startTime, record.endTime).toMinutes() < config.minDurationMinutes) {
            return Result.failure(IllegalArgumentException("Запись слишком короткая"))
        }
        val overlapping = repository.getOverlappingRecords(
            record.userId,
            record.startTime,
            record.endTime,
            null
        )
        if (overlapping.isNotEmpty()) {
            return Result.failure(IllegalArgumentException("Пересечение времени"))
        }
        return repository.saveRecord(record).map { record }
    }
}
