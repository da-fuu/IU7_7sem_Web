package com.ppo.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "time_record_history",
    foreignKeys = [
        ForeignKey(
            entity = TimeRecordEntity::class,
            parentColumns = ["uid"],
            childColumns = ["record_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["record_id"])
    ]
)
data class TimeRecordHistoryEntity(
    @PrimaryKey val uid: String,
    @ColumnInfo(name = "record_id") val recordId: String,
    val description: String,
    @ColumnInfo(name = "modified_at") val modifiedAt: Instant
)
