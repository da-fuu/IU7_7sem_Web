package com.ppo.domain.usecase

import com.ppo.domain.model.TimeRecord
import com.ppo.domain.repository.ITimeRecordRepository

class EditTimeRecordUseCase(
    private val repository: ITimeRecordRepository
) {
    suspend operator fun invoke(updatedRecord: TimeRecord): Result<Unit> {
        val endTime =
            updatedRecord.endTime ?: return Result.failure(IllegalArgumentException("Время окончания обязательно"))

        if (endTime.isBefore(updatedRecord.startTime)) {
            return Result.failure(IllegalArgumentException("Конец промежутка не может быть раньше начала"))
        }

        if (updatedRecord.getDurationMinutes() < 1) {
            return Result.failure(IllegalArgumentException("Запись слишком короткая"))
        }

        val overlapping = repository.getOverlappingRecords(
            userId = updatedRecord.userId,
            start = updatedRecord.startTime,
            end = endTime,
            excludeRecordId = updatedRecord.id
        )

        if (overlapping.isNotEmpty()) {
            return Result.failure(IllegalStateException("Пересечение времени"))
        }

        return repository.saveRecord(updatedRecord)
    }
}
