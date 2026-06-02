package com.ppo.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ppo.data.entity.TimeRecordHistoryEntity

@Dao
interface TimeRecordHistoryDao {
    @Insert
    suspend fun insert(entity: TimeRecordHistoryEntity)

    @Query("SELECT * FROM time_record_history WHERE record_id = :recordId ORDER BY modified_at DESC")
    suspend fun getByRecordId(recordId: String): List<TimeRecordHistoryEntity>
}
