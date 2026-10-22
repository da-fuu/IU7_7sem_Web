package com.ppo.web.data.repository

import com.ppo.domain.model.Category
import com.ppo.domain.model.CategoryId
import com.ppo.domain.model.DescriptionVersion
import com.ppo.domain.model.RecordHistoryEntry
import com.ppo.domain.model.RecordId
import com.ppo.domain.model.TimeRecord
import com.ppo.domain.model.UserId
import com.ppo.domain.model.VersionId
import com.ppo.domain.repository.ITimeRecordRepository
import com.ppo.web.data.orm.CategoryEntity
import com.ppo.web.data.orm.RecordHistoryEntity
import com.ppo.web.data.orm.RecordHistoryTable
import com.ppo.web.data.orm.TimeRecordEntity
import com.ppo.web.data.orm.TimeRecordsTable
import com.ppo.web.data.orm.UserEntity
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

class PostgresTimeRecordRepository(private val dataSource: DataSource) : ITimeRecordRepository {
    override suspend fun saveRecord(record: TimeRecord): Result<Unit> = runCatching {
        transaction {
            val existing = TimeRecordEntity.findById(record.id.value.toString())
            val userEntity = UserEntity[record.userId.value.toString()]
            val categoryEntity = CategoryEntity[record.categoryId.value.toString()]
            if (existing != null) {
                existing.user = userEntity
                existing.category = categoryEntity
                existing.startTime = record.startTime
                existing.endTime = record.endTime
                existing.description = record.description
            } else {
                TimeRecordEntity.new(record.id.value.toString()) {
                    this.user = userEntity
                    this.category = categoryEntity
                    this.startTime = record.startTime
                    this.endTime = record.endTime
                    this.description = record.description
                }
            }
        }
    }

    override suspend fun getRecordById(recordId: RecordId): TimeRecord? = transaction {
        TimeRecordEntity.findById(recordId.value.toString())?.toDomain()
    }

    override suspend fun getOverlappingRecords(
        userId: UserId,
        start: Instant,
        end: Instant?,
        excludeRecordId: RecordId?
    ): List<TimeRecord> = transaction {
        val records = TimeRecordEntity.find {
            TimeRecordsTable.user eq userId.value.toString()
        }.map { it.toDomain() }

        records.filter { rec ->
            if (excludeRecordId != null && rec.id == excludeRecordId) {
                false
            } else {
                val recStart = rec.startTime
                val recEnd = rec.endTime
                if (end != null) {
                    if (recEnd != null) {
                        recStart < end && recEnd > start
                    } else {
                        recStart < end
                    }
                } else {
                    recEnd == null || recEnd > start
                }
            }
        }
    }

    override suspend fun getActiveSession(userId: UserId): TimeRecord? = transaction {
        TimeRecordEntity.find {
            (TimeRecordsTable.user eq userId.value.toString()) and TimeRecordsTable.endTime.isNull()
        }.firstOrNull()?.toDomain()
    }

    override suspend fun getTimeRecords(userId: UserId): List<TimeRecord> = transaction {
        TimeRecordEntity.find {
            TimeRecordsTable.user eq userId.value.toString()
        }.orderBy(TimeRecordsTable.startTime to SortOrder.DESC).map { it.toDomain() }
    }

    fun findRecords(
        userId: UserId,
        categoryId: CategoryId? = null,
        status: String? = null,
        from: Instant? = null,
        to: Instant? = null,
        limit: Int = 20,
        offset: Int = 0
    ): Pair<List<TimeRecord>, Int> = transaction {
        var query = TimeRecordsTable.selectAll().where { TimeRecordsTable.user eq userId.value.toString() }
        if (categoryId != null) {
            query = query.andWhere { TimeRecordsTable.category eq categoryId.value.toString() }
        }
        if (status != null) {
            when (status.lowercase()) {
                "active" -> query = query.andWhere { TimeRecordsTable.endTime.isNull() }
                "completed" -> query = query.andWhere { TimeRecordsTable.endTime.isNotNull() }
            }
        }
        if (from != null) {
            query = query.andWhere { TimeRecordsTable.startTime greaterEq from }
        }
        if (to != null) {
            query = query.andWhere { TimeRecordsTable.startTime lessEq to }
        }

        val total = query.count().toInt()
        val records = TimeRecordEntity.wrapRows(
            query.orderBy(TimeRecordsTable.startTime to SortOrder.DESC)
                .limit(limit)
                .offset(offset.toLong())
        ).map { it.toDomain() }

        Pair(records, total)
    }

    override suspend fun getDescriptionHistory(recordId: RecordId): List<DescriptionVersion> = transaction {
        RecordHistoryEntity.find {
            RecordHistoryTable.record eq recordId.value.toString()
        }.orderBy(RecordHistoryTable.modifiedAt to SortOrder.DESC).map {
            DescriptionVersion(
                id = VersionId(UUID.fromString(it.id.value)),
                recordId = recordId,
                description = it.description,
                modifiedAt = it.modifiedAt
            )
        }
    }

    override suspend fun addDescriptionVersion(version: DescriptionVersion): Result<Unit> = runCatching {
        transaction {
            val recordEntity = TimeRecordEntity[version.recordId.value.toString()]
            RecordHistoryEntity.new(version.id.value.toString()) {
                this.record = recordEntity
                this.categoryId = recordEntity.category.id.value
                this.startTime = recordEntity.startTime
                this.endTime = recordEntity.endTime
                this.description = version.description
                this.modifiedAt = version.modifiedAt
            }
        }
    }

    override suspend fun deleteRecord(recordId: RecordId): Result<Unit> = runCatching {
        transaction {
            val existing = TimeRecordEntity.findById(recordId.value.toString())
            if (existing != null) {
                existing.delete()
            } else {
                throw NoSuchElementException("Record with id $recordId not found")
            }
        }
    }

    override suspend fun getAllTimeRecords(): List<TimeRecord> = transaction {
        TimeRecordEntity.all().map { it.toDomain() }
    }

    override suspend fun getActiveRecords(userId: UserId?): List<TimeRecord> = transaction {
        val query = if (userId != null) {
            TimeRecordEntity.find {
                (TimeRecordsTable.user eq userId.value.toString()) and TimeRecordsTable.endTime.isNull()
            }
        } else {
            TimeRecordEntity.find {
                TimeRecordsTable.endTime.isNull()
            }
        }
        query.map { it.toDomain() }
    }
}
