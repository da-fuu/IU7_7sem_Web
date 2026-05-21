package com.ppo.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ppo.data.entity.TimeRecordEntity
import java.time.Instant

@Dao
interface TimeRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(record: TimeRecordEntity): Long

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
    fun findOverlapping(
        userId: String,
        start: Instant,
        end: Instant?,
        excludeRecordId: String?
    ): List<TimeRecordEntity>

    @Query("SELECT * FROM time_records WHERE user_id = :userId AND end_time IS NULL LIMIT 1")
    fun getActiveSession(userId: String): TimeRecordEntity?

    @Query("DELETE FROM time_records WHERE uid = :recordId")
    fun deleteById(recordId: String): Int
}
