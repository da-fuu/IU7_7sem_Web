package com.ppo.domain.repository

import com.ppo.domain.model.RecordHistoryEntry
import com.ppo.domain.model.RecordId
import com.ppo.domain.model.TimeRecord
import com.ppo.domain.model.UserId
import java.time.Instant

interface ITimeRecordRepository {
    suspend fun saveRecord(record: TimeRecord): Result<Unit>
    suspend fun getRecordById(recordId: RecordId): TimeRecord?
    suspend fun getOverlappingRecords(
        userId: UserId,
        start: Instant,
        end: Instant?,
        excludeRecordId: RecordId? = null
    ): List<TimeRecord> = emptyList()

    suspend fun getActiveSession(userId: UserId): TimeRecord? =
        getActiveRecords(userId).firstOrNull()

    suspend fun getTimeRecords(userId: UserId): List<TimeRecord>
    suspend fun deleteRecord(recordId: RecordId): Result<Unit>
    suspend fun getAllTimeRecords(): List<TimeRecord>
    suspend fun getActiveRecords(userId: UserId? = null): List<TimeRecord> =
        if (userId != null) {
            getTimeRecords(userId).filter { it.endTime == null }
        } else {
            getAllTimeRecords().filter { it.endTime == null }
        }

    suspend fun getDescriptionHistory(recordId: RecordId): List<RecordHistoryEntry> = emptyList()
    suspend fun addDescriptionVersion(version: RecordHistoryEntry): Result<Unit> = Result.success(Unit)
}
