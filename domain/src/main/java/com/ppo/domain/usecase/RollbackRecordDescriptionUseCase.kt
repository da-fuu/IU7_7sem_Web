package com.ppo.domain.usecase

import com.ppo.domain.model.RecordHistoryEntry
import com.ppo.domain.model.RecordId
import com.ppo.domain.model.TimeRecord
import com.ppo.domain.model.VersionId
import com.ppo.domain.repository.ITimeRecordRepository
import java.time.Instant
import java.util.UUID

class RollbackRecordDescriptionUseCase(
    private val timeRecordRepository: ITimeRecordRepository
) {
    suspend operator fun invoke(recordId: RecordId, versionId: VersionId): Result<TimeRecord> {
        val record = timeRecordRepository.getRecordById(recordId)
            ?: return Result.failure(NoSuchElementException("Record not found"))

        val versions = timeRecordRepository.getDescriptionHistory(recordId)
        val targetVersion = versions.find { it.id == versionId }
            ?: return Result.failure(NoSuchElementException("Version not found"))

        val currentToHistory = RecordHistoryEntry(
            id = VersionId(UUID.randomUUID()),
            recordId = record.id,
            categoryId = record.categoryId,
            startTime = record.startTime,
            endTime = record.endTime,
            description = record.description,
            modifiedAt = Instant.now()
        )

        val historyResult = timeRecordRepository.addDescriptionVersion(currentToHistory)
        if (historyResult.isFailure) {
            return Result.failure(historyResult.exceptionOrNull()!!)
        }

        val updatedRecord = record.copy(description = targetVersion.description)
        return timeRecordRepository.saveRecord(updatedRecord).map { updatedRecord }
    }
}
