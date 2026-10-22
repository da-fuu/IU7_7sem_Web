package com.ppo.web.service

import com.ppo.domain.model.RecordHistoryEntry
import com.ppo.domain.model.UserId
import com.ppo.domain.model.VersionId
import com.ppo.domain.repository.IRecordHistoryRepository
import com.ppo.domain.repository.ITimeRecordRepository
import com.ppo.domain.repository.IUserSettingsRepository
import com.ppo.web.routes.DurationConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.util.UUID

class AutoStopWorker(
    private val timeRecordRepository: ITimeRecordRepository,
    private val userSettingsRepository: IUserSettingsRepository,
    private val recordHistoryRepository: IRecordHistoryRepository
) {
    suspend fun processExpiredRecords(now: Instant = Instant.now()): Int {
        val activeRecords = timeRecordRepository.getActiveRecords()
        var stoppedCount = 0
        for (record in activeRecords) {
            val settings = userSettingsRepository.getSettings(record.userId)
            if (settings.autoStopEnabled) {
                val maxSec = DurationConverter.toSeconds(settings.maxActiveDurationValue, settings.maxActiveDurationUnit)
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

    suspend fun processExpiredRecordsForUser(userId: UserId, now: Instant = Instant.now()): Int {
        val activeRecords = timeRecordRepository.getActiveRecords(userId)
        val settings = userSettingsRepository.getSettings(userId)
        if (!settings.autoStopEnabled) return 0

        val maxSec = DurationConverter.toSeconds(settings.maxActiveDurationValue, settings.maxActiveDurationUnit)
        var stoppedCount = 0
        for (record in activeRecords) {
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
        return stoppedCount
    }

    fun start(coroutineScope: CoroutineScope, intervalMs: Long = 10 * 60 * 1000L): Job {
        return coroutineScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(intervalMs)
                try {
                    processExpiredRecords()
                } catch (e: Exception) {
                }
            }
        }
    }
}
