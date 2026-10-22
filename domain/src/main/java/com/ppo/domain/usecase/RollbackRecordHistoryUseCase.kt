package com.ppo.domain.usecase

import com.ppo.domain.model.RecordHistoryEntry
import com.ppo.domain.model.RecordId
import com.ppo.domain.model.TimeRecord
import com.ppo.domain.model.UserId
import com.ppo.domain.model.VersionId
import com.ppo.domain.repository.IRecordHistoryRepository
import com.ppo.domain.repository.ITimeRecordRepository
import java.time.Instant
import java.util.UUID

class RollbackRecordHistoryUseCase(
    private val timeRecordRepository: ITimeRecordRepository,
    private val recordHistoryRepository: IRecordHistoryRepository
) {
    suspend operator fun invoke(
        recordId: RecordId,
        versionId: VersionId,
        requestUserId: UserId,
        isAdmin: Boolean = false,
        now: Instant = Instant.now()
    ): Result<TimeRecord> {
        val record = timeRecordRepository.getRecordById(recordId)
            ?: return Result.failure(NoSuchElementException("Record not found"))

        if (record.userId != requestUserId && !isAdmin) {
            return Result.failure(IllegalAccessException("Access denied"))
        }

        val version = recordHistoryRepository.getHistoryEntry(versionId)
            ?: return Result.failure(NoSuchElementException("Version not found"))

        if (version.recordId != recordId) {
            return Result.failure(IllegalArgumentException("Version does not belong to this record"))
        }

        val currentSnapshot = RecordHistoryEntry(
            id = VersionId(UUID.randomUUID()),
            recordId = record.id,
            categoryId = record.categoryId,
            startTime = record.startTime,
            endTime = record.endTime,
            description = record.description,
            modifiedAt = now
        )
        recordHistoryRepository.saveHistoryEntry(currentSnapshot)

        val rolledBack = record.copy(
            categoryId = version.categoryId,
            startTime = version.startTime,
            endTime = version.endTime,
            description = version.description
        )

        return timeRecordRepository.saveRecord(rolledBack).map { rolledBack }
    }
}
