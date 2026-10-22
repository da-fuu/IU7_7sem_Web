package com.ppo.domain.usecase

import com.ppo.domain.model.CategoryId
import com.ppo.domain.model.RecordHistoryEntry
import com.ppo.domain.model.RecordId
import com.ppo.domain.model.TimeRecord
import com.ppo.domain.model.UserId
import com.ppo.domain.model.VersionId
import com.ppo.domain.repository.ICategoryRepository
import com.ppo.domain.repository.IRecordHistoryRepository
import com.ppo.domain.repository.ITimeRecordRepository
import com.ppo.domain.repository.IUserSettingsRepository
import java.time.Duration
import java.time.Instant
import java.util.UUID

class UpdateTimeRecordUseCase(
    private val timeRecordRepository: ITimeRecordRepository,
    private val categoryRepository: ICategoryRepository,
    private val userSettingsRepository: IUserSettingsRepository,
    private val recordHistoryRepository: IRecordHistoryRepository
) {
    suspend operator fun invoke(
        recordId: RecordId,
        requestUserId: UserId,
        isAdmin: Boolean = false,
        categoryId: CategoryId? = null,
        startTime: Instant? = null,
        endTime: Instant? = null,
        description: String? = null,
        now: Instant = Instant.now()
    ): Result<TimeRecord> {
        val existing = timeRecordRepository.getRecordById(recordId)
            ?: return Result.failure(NoSuchElementException("Record not found"))

        if (existing.userId != requestUserId && !isAdmin) {
            return Result.failure(IllegalAccessException("Access denied"))
        }

        val targetCategoryId = categoryId ?: existing.categoryId
        if (categoryId != null && categoryRepository.findCategoryById(targetCategoryId) == null) {
            return Result.failure(NoSuchElementException("Category not found"))
        }

        val targetStartTime = startTime ?: existing.startTime
        val targetEndTime = if (endTime != null) endTime else existing.endTime

        if (targetEndTime != null) {
            if (!targetEndTime.isAfter(targetStartTime)) {
                return Result.failure(IllegalArgumentException("End time must be after start time"))
            }

            val settings = userSettingsRepository.getSettings(existing.userId)
            if (settings.minDurationEnabled) {
                val minSec = settings.minDurationUnit.toSeconds(settings.minDurationValue)
                val actualSec = Duration.between(targetStartTime, targetEndTime).seconds
                if (actualSec < minSec) {
                    return Result.failure(IllegalStateException("Duration is shorter than configured threshold ($minSec sec)"))
                }
            }
        }

        val targetDescription = description ?: existing.description

        val historyEntry = RecordHistoryEntry(
            id = VersionId(UUID.randomUUID()),
            recordId = existing.id,
            categoryId = existing.categoryId,
            startTime = existing.startTime,
            endTime = existing.endTime,
            description = existing.description,
            modifiedAt = now
        )
        recordHistoryRepository.saveHistoryEntry(historyEntry)

        val updated = existing.copy(
            categoryId = targetCategoryId,
            startTime = targetStartTime,
            endTime = targetEndTime,
            description = targetDescription
        )

        return timeRecordRepository.saveRecord(updated).map { updated }
    }
}
