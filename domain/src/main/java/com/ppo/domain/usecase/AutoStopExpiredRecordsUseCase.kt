package com.ppo.domain.usecase

import com.ppo.domain.model.RecordHistoryEntry
import com.ppo.domain.model.UserId
import com.ppo.domain.model.VersionId
import com.ppo.domain.repository.IRecordHistoryRepository
import com.ppo.domain.repository.ITimeRecordRepository
import com.ppo.domain.repository.IUserSettingsRepository
import java.time.Duration
import java.time.Instant
import java.util.UUID

class AutoStopExpiredRecordsUseCase(
    private val timeRecordRepository: ITimeRecordRepository,
    private val userSettingsRepository: IUserSettingsRepository,
    private val recordHistoryRepository: IRecordHistoryRepository
) {
    suspend operator fun invoke(userId: UserId? = null, now: Instant = Instant.now()): Int {
        val activeRecords = timeRecordRepository.getActiveRecords(userId)
        var stoppedCount = 0

        for (record in activeRecords) {
            val settings = userSettingsRepository.getSettings(record.userId)
            if (settings.autoStopEnabled) {
                val maxSec = settings.maxActiveDurationUnit.toSeconds(settings.maxActiveDurationValue)
                val durationSec = Duration.between(record.startTime, now).seconds
                if (durationSec >= maxSec) {
                    val autoEndTime = record.startTime.plusSeconds(maxSec)
                    val history = RecordHistoryEntry(
                        id = VersionId(UUID.randomUUID()),
                        recordId = record.id,
                        categoryId = record.categoryId,
                        startTime = record.startTime,
                        endTime = record.endTime,
                        description = record.description,
                        modifiedAt = now
                    )
                    recordHistoryRepository.saveHistoryEntry(history)
                    timeRecordRepository.saveRecord(record.copy(endTime = autoEndTime))
                    stoppedCount++
                }
            }
        }

        return stoppedCount
    }
}
