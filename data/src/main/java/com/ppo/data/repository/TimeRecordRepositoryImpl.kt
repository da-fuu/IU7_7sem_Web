package com.ppo.data.repository

import com.ppo.data.db.dao.TimeRecordDao
import com.ppo.data.mapper.toDomain
import com.ppo.data.mapper.toEntity
import com.ppo.domain.model.RecordId
import com.ppo.domain.model.TimeRecord
import com.ppo.domain.model.UserId
import com.ppo.domain.repository.ITimeRecordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.time.Instant

class TimeRecordRepositoryImpl(
    private val timeRecordDao: TimeRecordDao
) : ITimeRecordRepository {
    private val logger = LoggerFactory.getLogger(TimeRecordRepositoryImpl::class.java)

    override suspend fun saveRecord(record: TimeRecord): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            timeRecordDao.insert(record.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error("Failed to save time record {}: {}", record.id.value, e.message)
            Result.failure(e)
        }
    }

    override suspend fun getOverlappingRecords(
        userId: UserId,
        start: Instant,
        end: Instant?,
        excludeRecordId: RecordId?
    ): List<TimeRecord> = withContext(Dispatchers.IO) {
        timeRecordDao.findOverlapping(
            userId = userId.value.toString(),
            start = start,
            end = end,
            excludeRecordId = excludeRecordId?.value?.toString()
        ).map { it.toDomain() }
    }

    override suspend fun getActiveSession(userId: UserId): TimeRecord? = withContext(Dispatchers.IO) {
        timeRecordDao.getActiveSession(userId.value.toString())?.toDomain()
    }

    override suspend fun getTimeRecords(userId: UserId): List<TimeRecord> = withContext(Dispatchers.IO) {
        timeRecordDao.getAllByUserId(userId.value.toString()).map { it.toDomain() }
    }

    override suspend fun deleteRecord(recordId: RecordId): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val deletedCount = timeRecordDao.deleteById(recordId.value.toString())
            if (deletedCount == 0) {
                logger.warn("No time record found to delete with id: {}", recordId.value)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error("Failed to delete time record {}: {}", recordId.value, e.message)
            Result.failure(e)
        }
    }
}
