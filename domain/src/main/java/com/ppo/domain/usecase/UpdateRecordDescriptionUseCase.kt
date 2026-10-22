package com.ppo.domain.usecase

import com.ppo.domain.model.RecordHistoryEntry
import com.ppo.domain.model.RecordId
import com.ppo.domain.model.TimeRecord
import com.ppo.domain.model.VersionId
import com.ppo.domain.repository.ITimeRecordRepository
import java.time.Instant
import java.util.UUID

class UpdateRecordDescriptionUseCase(
    private val timeRecordRepository: ITimeRecordRepository
) {
    suspend operator fun invoke(recordId: RecordId, newDescription: String): Result<TimeRecord> {
        val record = timeRecordRepository.getRecordById(recordId)
            ?: return Result.failure(NoSuchElementException("Record not found"))

        if (record.description == newDescription) {
            return Result.success(record)
        }

        val version = RecordHistoryEntry(
            id = VersionId(UUID.randomUUID()),
            recordId = recordId,
            categoryId = record.categoryId,
            startTime = record.startTime,
            endTime = record.endTime,
            description = record.description,
            modifiedAt = Instant.now()
        )

        val historyResult = timeRecordRepository.addDescriptionVersion(version)
        if (historyResult.isFailure) {
            return Result.failure(historyResult.exceptionOrNull()!!)
        }

        val updatedRecord = record.copy(description = newDescription)
        return timeRecordRepository.saveRecord(updatedRecord).map { updatedRecord }
    }
}
