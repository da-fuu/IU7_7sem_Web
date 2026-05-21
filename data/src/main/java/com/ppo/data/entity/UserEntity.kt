package com.ppo.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ppo.domain.model.UserRole

@Entity(
    tableName = "users",
    indices = [Index(value = ["login"], unique = true)]
)
data class UserEntity(
    @PrimaryKey val uid: String,
    val login: String,
    val name: String,
    val passwordHash: String,
    val role: UserRole
)
