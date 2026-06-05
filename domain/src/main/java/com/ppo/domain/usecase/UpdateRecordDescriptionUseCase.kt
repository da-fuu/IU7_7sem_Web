package com.ppo.domain.usecase

import com.ppo.domain.model.DescriptionVersion
import com.ppo.domain.model.RecordId
import com.ppo.domain.model.VersionId
import com.ppo.domain.repository.ITimeRecordRepository
import java.time.Instant
import java.util.UUID

class UpdateRecordDescriptionUseCase(
    private val timeRecordRepository: ITimeRecordRepository
) {
    suspend operator fun invoke(recordId: RecordId, newDescription: String): Result<Unit> {
        val record = timeRecordRepository.getRecordById(recordId)
            ?: return Result.failure(Exception("Record not found"))

        if (record.description == newDescription) {
            return Result.success(Unit)
        }

        val version = DescriptionVersion(
            id = VersionId(UUID.randomUUID()),
            recordId = recordId,
            description = record.description,
            modifiedAt = Instant.now()
        )

        val historyResult = timeRecordRepository.addDescriptionVersion(version)
        if (historyResult.isFailure) {
            return historyResult
        }

        val updatedRecord = record.copy(description = newDescription)
        return timeRecordRepository.saveRecord(updatedRecord)
    }
}
