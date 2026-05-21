package com.ppo.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ppo.data.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE user_id = :userId OR isDefault = 1")
    fun getCategoriesFlow(userId: String): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity): Long

    @Query("SELECT * FROM categories WHERE (user_id = :userId OR (user_id IS NULL AND :userId IS NULL)) AND name = :name LIMIT 1")
    suspend fun findByName(userId: String?, name: String): CategoryEntity?

    @Query("DELETE FROM categories WHERE uid = :uid")
    suspend fun deleteById(uid: String): Int
}
