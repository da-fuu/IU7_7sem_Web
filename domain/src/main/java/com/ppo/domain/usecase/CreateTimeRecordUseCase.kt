package com.ppo.domain.usecase

import com.ppo.domain.model.CategoryId
import com.ppo.domain.model.RecordId
import com.ppo.domain.model.TimeRecord
import com.ppo.domain.model.UserId
import com.ppo.domain.repository.ICategoryRepository
import com.ppo.domain.repository.ITimeRecordRepository
import com.ppo.domain.repository.IUserSettingsRepository
import java.time.Duration
import java.time.Instant
import java.util.UUID

class CreateTimeRecordUseCase(
    private val timeRecordRepository: ITimeRecordRepository,
    private val categoryRepository: ICategoryRepository,
    private val userSettingsRepository: IUserSettingsRepository
) {
    suspend operator fun invoke(
        userId: UserId,
        categoryId: CategoryId,
        startTime: Instant = Instant.now(),
        endTime: Instant? = null,
        description: String = ""
    ): Result<TimeRecord> {
        val category = categoryRepository.findCategoryById(categoryId)
            ?: return Result.failure(NoSuchElementException("Category not found"))

        if (!category.isDefault && category.userId != userId) {
            return Result.failure(IllegalAccessException("Category does not belong to user"))
        }

        if (endTime != null) {
            if (!endTime.isAfter(startTime)) {
                return Result.failure(IllegalArgumentException("End time must be after start time"))
            }

            val settings = userSettingsRepository.getSettings(userId)
            if (settings.minDurationEnabled) {
                val minSec = settings.minDurationUnit.toSeconds(settings.minDurationValue)
                val actualSec = Duration.between(startTime, endTime).seconds
                if (actualSec < minSec) {
                    return Result.failure(IllegalStateException("Duration is shorter than configured threshold ($minSec sec)"))
                }
            }
        }

        val record = TimeRecord(
            id = RecordId(UUID.randomUUID()),
            userId = userId,
            categoryId = categoryId,
            startTime = startTime,
            endTime = endTime,
            description = description
        )

        return timeRecordRepository.saveRecord(record).map { record }
    }
}
