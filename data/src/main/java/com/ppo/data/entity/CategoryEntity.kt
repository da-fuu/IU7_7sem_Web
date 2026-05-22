package com.ppo.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["uid"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["user_id"])]
)
data class CategoryEntity(
    @PrimaryKey val uid: String,
    @ColumnInfo(name = "user_id") val userId: String?,
    val name: String,
    val colorHex: String,
    val isDefault: Boolean
)
