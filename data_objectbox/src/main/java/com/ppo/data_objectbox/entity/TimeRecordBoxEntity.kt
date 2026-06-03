package com.ppo.data_objectbox.entity

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import io.objectbox.annotation.Unique

@Entity
data class TimeRecordBoxEntity(
    @Id var boxId: Long = 0,
    @Index @Unique var uid: String,
    @Index var userId: String,
    @Index var categoryId: String,
    var startTime: Long,
    var endTime: Long?,
    var description: String
)
