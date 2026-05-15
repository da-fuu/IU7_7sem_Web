package com.ppo.domain.usecase

import com.ppo.domain.model.TimeRecord
import com.ppo.domain.repository.ITimeRecordRepository

class AddManualRecordUseCase(
    private val repository: ITimeRecordRepository
) {
    suspend operator fun invoke(record: TimeRecord): Result<Unit> {
        val endTime = record.endTime ?: return Result.failure(IllegalArgumentException("Время окончания обязательно"))

        if (endTime.isBefore(record.startTime)) {
            return Result.failure(IllegalArgumentException("Конец промежутка не может быть раньше начала"))
        }

        if (record.getDurationMinutes() < 1) {
            return Result.failure(IllegalArgumentException("Запись слишком короткая"))
        }

        val overlapping = repository.getOverlappingRecords(
            userId = record.userId,
            start = record.startTime,
            end = endTime,
            excludeRecordId = null
        )

        if (overlapping.isNotEmpty()) {
            return Result.failure(IllegalStateException("Пересечение времени"))
        }

        return repository.saveRecord(record)
    }
}
