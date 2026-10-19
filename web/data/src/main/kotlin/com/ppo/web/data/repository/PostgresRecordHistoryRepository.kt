package com.ppo.web.data.repository

import com.ppo.domain.model.RecordHistoryEntry
import com.ppo.domain.model.RecordId
import com.ppo.domain.model.VersionId
import com.ppo.domain.repository.IRecordHistoryRepository
import com.ppo.web.data.orm.RecordHistoryEntity
import com.ppo.web.data.orm.RecordHistoryTable
import com.ppo.web.data.orm.TimeRecordEntity
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.transaction
import javax.sql.DataSource

class PostgresRecordHistoryRepository(private val dataSource: DataSource) : IRecordHistoryRepository {
    override suspend fun saveHistoryEntry(entry: RecordHistoryEntry): Result<Unit> = runCatching {
        transaction {
            val existing = RecordHistoryEntity.findById(entry.id.value.toString())
            val recordEntity = TimeRecordEntity[entry.recordId.value.toString()]
            if (existing != null) {
                existing.record = recordEntity
                existing.categoryId = entry.categoryId.value.toString()
                existing.startTime = entry.startTime
                existing.endTime = entry.endTime
                existing.description = entry.description
                existing.modifiedAt = entry.modifiedAt
            } else {
                RecordHistoryEntity.new(entry.id.value.toString()) {
                    this.record = recordEntity
                    this.categoryId = entry.categoryId.value.toString()
                    this.startTime = entry.startTime
                    this.endTime = entry.endTime
                    this.description = entry.description
                    this.modifiedAt = entry.modifiedAt
                }
            }
        }
    }

    override suspend fun getHistoryForRecord(recordId: RecordId): List<RecordHistoryEntry> = transaction {
        RecordHistoryEntity.find {
            RecordHistoryTable.record eq recordId.value.toString()
        }.orderBy(RecordHistoryTable.modifiedAt to SortOrder.DESC).map { it.toDomain() }
    }

    override suspend fun getHistoryEntry(versionId: VersionId): RecordHistoryEntry? = transaction {
        RecordHistoryEntity.findById(versionId.value.toString())?.toDomain()
    }
}
