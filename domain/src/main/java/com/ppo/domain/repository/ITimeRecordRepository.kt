package com.ppo.domain.repository

import com.ppo.domain.model.DescriptionVersion
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
    ): List<TimeRecord>

    suspend fun getActiveSession(userId: UserId): TimeRecord?
    suspend fun getTimeRecords(userId: UserId): List<TimeRecord>
    suspend fun getDescriptionHistory(recordId: RecordId): List<DescriptionVersion>
    suspend fun addDescriptionVersion(version: DescriptionVersion): Result<Unit>
    suspend fun deleteRecord(recordId: RecordId): Result<Unit>
}
