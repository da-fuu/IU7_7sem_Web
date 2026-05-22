package com.ppo.domain.repository

import com.ppo.domain.model.RecordId
import com.ppo.domain.model.TimeRecord
import com.ppo.domain.model.UserId
import java.time.Instant

interface ITimeRecordRepository {
    suspend fun saveRecord(record: TimeRecord): Result<Unit>
    suspend fun getOverlappingRecords(
        userId: UserId,
        start: Instant,
        end: Instant,
        excludeRecordId: RecordId? = null
    ): List<TimeRecord>
    suspend fun getActiveSession(userId: UserId): TimeRecord?
    suspend fun deleteRecord(recordId: RecordId): Result<Unit>
}
