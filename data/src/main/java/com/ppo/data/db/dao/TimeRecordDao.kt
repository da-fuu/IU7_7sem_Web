package com.ppo.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ppo.data.entity.TimeRecordEntity
import java.time.Instant

@Dao
interface TimeRecordDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(record: TimeRecordEntity): Long

    @Update
    suspend fun update(record: TimeRecordEntity): Int

    @Query(
        """
        SELECT * FROM time_records
        WHERE user_id = :userId
          AND (:excludeRecordId IS NULL OR uid != :excludeRecordId)
          AND (
            (:end IS NOT NULL AND start_time < :end AND (end_time > :start OR end_time IS NULL))
            OR
            (:end IS NULL AND (end_time > :start OR end_time IS NULL))
          )
    """
    )
    suspend fun findOverlapping(
        userId: String,
        start: Instant,
        end: Instant?,
        excludeRecordId: String?
    ): List<TimeRecordEntity>

    @Query("SELECT * FROM time_records WHERE user_id = :userId AND end_time IS NULL LIMIT 1")
    suspend fun getActiveSession(userId: String): TimeRecordEntity?

    @Query("SELECT * FROM time_records WHERE uid = :recordId")
    suspend fun getById(recordId: String): TimeRecordEntity?

    @Query("SELECT * FROM time_records WHERE user_id = :userId ORDER BY start_time DESC")
    suspend fun getAllByUserId(userId: String): List<TimeRecordEntity>

    @Query("DELETE FROM time_records WHERE uid = :recordId")
    suspend fun deleteById(recordId: String): Int
}
