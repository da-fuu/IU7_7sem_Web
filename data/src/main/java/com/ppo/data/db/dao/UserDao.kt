package com.ppo.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ppo.data.entity.UserEntity
import com.ppo.domain.model.UserRole

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE login = :login LIMIT 1")
    suspend fun findByLogin(login: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity): Long

    @Query("SELECT role FROM users WHERE uid = :userId")
    suspend fun getUserRole(userId: String): UserRole?

    @Query("SELECT * FROM users WHERE uid = :userId LIMIT 1")
    suspend fun findById(userId: String): UserEntity?

    @Query("SELECT * FROM users")
    suspend fun getAll(): List<UserEntity>
}
