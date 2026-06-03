package com.ppo.data_objectbox.entity

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import io.objectbox.annotation.Unique

@Entity
data class CategoryBoxEntity(
    @Id var boxId: Long = 0,
    @Index @Unique var uid: String,
    @Index var userId: String?,
    var name: String,
    var colorHex: String,
    var isDefault: Boolean
)
