package com.ppo.domain.repository

import com.ppo.domain.model.RecordHistoryEntry
import com.ppo.domain.model.RecordId
import com.ppo.domain.model.VersionId

interface IRecordHistoryRepository {
    suspend fun saveHistoryEntry(entry: RecordHistoryEntry): Result<Unit>
    suspend fun getHistoryForRecord(recordId: RecordId): List<RecordHistoryEntry>
    suspend fun getHistoryEntry(versionId: VersionId): RecordHistoryEntry?
}
