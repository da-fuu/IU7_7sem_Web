package com.ppo.web.service

import com.ppo.domain.model.UserId
import com.ppo.domain.repository.IRecordHistoryRepository
import com.ppo.domain.repository.ITimeRecordRepository
import com.ppo.domain.repository.IUserSettingsRepository
import com.ppo.domain.usecase.AutoStopExpiredRecordsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant

class AutoStopWorker(
    private val autoStopExpiredRecordsUseCase: AutoStopExpiredRecordsUseCase
) {
    constructor(
        timeRecordRepository: ITimeRecordRepository,
        userSettingsRepository: IUserSettingsRepository,
        recordHistoryRepository: IRecordHistoryRepository
    ) : this(AutoStopExpiredRecordsUseCase(timeRecordRepository, userSettingsRepository, recordHistoryRepository))
    suspend fun processExpiredRecords(now: Instant = Instant.now()): Int {
        return autoStopExpiredRecordsUseCase(now = now)
    }

    suspend fun processExpiredRecordsForUser(userId: UserId, now: Instant = Instant.now()): Int {
        return autoStopExpiredRecordsUseCase(userId = userId, now = now)
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
