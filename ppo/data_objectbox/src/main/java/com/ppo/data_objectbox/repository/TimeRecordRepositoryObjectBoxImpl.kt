package com.ppo.data_objectbox.repository

import com.ppo.data_objectbox.entity.DescriptionVersionBoxEntity
import com.ppo.data_objectbox.entity.DescriptionVersionBoxEntity_
import com.ppo.data_objectbox.entity.TimeRecordBoxEntity
import com.ppo.data_objectbox.entity.TimeRecordBoxEntity_
import com.ppo.data_objectbox.mapper.toBoxEntity
import com.ppo.data_objectbox.mapper.toDomain
import com.ppo.domain.model.DescriptionVersion
import com.ppo.domain.model.RecordId
import com.ppo.domain.model.TimeRecord
import com.ppo.domain.model.UserId
import com.ppo.domain.repository.ITimeRecordRepository
import io.objectbox.BoxStore
import io.objectbox.query.QueryBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.time.Instant

class TimeRecordRepositoryObjectBoxImpl(
    boxStore: BoxStore
) : ITimeRecordRepository {
    private val timeRecordBox = boxStore.boxFor(TimeRecordBoxEntity::class.java)
    private val historyBox = boxStore.boxFor(DescriptionVersionBoxEntity::class.java)
    private val logger = LoggerFactory.getLogger(TimeRecordRepositoryObjectBoxImpl::class.java)

    override suspend fun saveRecord(record: TimeRecord): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = timeRecordBox.query(TimeRecordBoxEntity_.uid.equal(record.id.value.toString())).build().findFirst()
            timeRecordBox.put(record.toBoxEntity(existing?.boxId ?: 0))
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error("Failed to save time record", e)
            Result.failure(e)
        }
    }

    override suspend fun getRecordById(recordId: RecordId): TimeRecord? = withContext(Dispatchers.IO) {
        timeRecordBox.query(TimeRecordBoxEntity_.uid.equal(recordId.value.toString())).build().findFirst()?.toDomain()
    }

    override suspend fun getOverlappingRecords(
        userId: UserId,
        start: Instant,
        end: Instant?,
        excludeRecordId: RecordId?
    ): List<TimeRecord> = withContext(Dispatchers.IO) {
        val qBuilder = timeRecordBox.query(TimeRecordBoxEntity_.userId.equal(userId.value.toString()))
        if (excludeRecordId != null) {
            qBuilder.and().notEqual(TimeRecordBoxEntity_.uid, excludeRecordId.value.toString(), QueryBuilder.StringOrder.CASE_SENSITIVE)
        }
        val records = qBuilder.build().find()
        
        val startMs = start.toEpochMilli()
        val endMs = end?.toEpochMilli()

        records.filter { entity: TimeRecordBoxEntity ->
            val eStart = entity.startTime
            val eEnd = entity.endTime
            
            if (endMs != null) {
                eStart < endMs && (eEnd == null || eEnd > startMs)
            } else {
                eEnd == null || eEnd > startMs
            }
        }.map { it.toDomain() }
    }

    override suspend fun getActiveSession(userId: UserId): TimeRecord? = withContext(Dispatchers.IO) {
        timeRecordBox.query(TimeRecordBoxEntity_.userId.equal(userId.value.toString()))
            .and().isNull(TimeRecordBoxEntity_.endTime)
            .build().findFirst()?.toDomain()
    }

    override suspend fun getTimeRecords(userId: UserId): List<TimeRecord> = withContext(Dispatchers.IO) {
        timeRecordBox.query(TimeRecordBoxEntity_.userId.equal(userId.value.toString()))
            .orderDesc(TimeRecordBoxEntity_.startTime)
            .build()
            .find()
            .map { it.toDomain() }
    }

    override suspend fun getDescriptionHistory(recordId: RecordId): List<DescriptionVersion> = withContext(Dispatchers.IO) {
        historyBox.query(DescriptionVersionBoxEntity_.recordId.equal(recordId.value.toString()))
            .orderDesc(DescriptionVersionBoxEntity_.modifiedAt)
            .build()
            .find()
            .map { it.toDomain() }
    }

    override suspend fun addDescriptionVersion(version: DescriptionVersion): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = historyBox.query(DescriptionVersionBoxEntity_.uid.equal(version.id.value.toString())).build().findFirst()
            historyBox.put(version.toBoxEntity(existing?.boxId ?: 0))
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error("Failed to add description history", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteRecord(recordId: RecordId): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entity = timeRecordBox.query(TimeRecordBoxEntity_.uid.equal(recordId.value.toString())).build().findFirst()
            if (entity != null) {
                timeRecordBox.remove(entity)
            }
            val history = historyBox.query(DescriptionVersionBoxEntity_.recordId.equal(recordId.value.toString())).build().find()
            historyBox.remove(history)
            
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error("Failed to delete record", e)
            Result.failure(e)
        }
    }

    override suspend fun getAllTimeRecords(): List<TimeRecord> = withContext(Dispatchers.IO) {
        timeRecordBox.all.map { it.toDomain() }
    }
}
