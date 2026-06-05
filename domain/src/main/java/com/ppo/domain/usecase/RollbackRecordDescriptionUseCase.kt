package com.ppo.domain.usecase

import com.ppo.domain.model.DescriptionVersion
import com.ppo.domain.model.RecordId
import com.ppo.domain.model.VersionId
import com.ppo.domain.repository.ITimeRecordRepository
import java.time.Instant
import java.util.UUID

class RollbackRecordDescriptionUseCase(
    private val timeRecordRepository: ITimeRecordRepository
) {
    suspend operator fun invoke(recordId: RecordId, versionId: VersionId): Result<Unit> {
        val history = timeRecordRepository.getDescriptionHistory(recordId)
        val targetVersion = history.find { it.id == versionId }
            ?: return Result.failure(Exception("Version not found"))

        val currentRecord = timeRecordRepository.getRecordById(recordId)
            ?: return Result.failure(Exception("Record not found"))

        val currentToHistory = DescriptionVersion(
            id = VersionId(UUID.randomUUID()),
            recordId = recordId,
            description = currentRecord.description,
            modifiedAt = Instant.now()
        )

        val historyResult = timeRecordRepository.addDescriptionVersion(currentToHistory)
        if (historyResult.isFailure) return historyResult

        val updatedRecord = currentRecord.copy(description = targetVersion.description)
        return timeRecordRepository.saveRecord(updatedRecord)
    }
}
