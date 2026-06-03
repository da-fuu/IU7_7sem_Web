package com.ppo.data_objectbox.entity

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import io.objectbox.annotation.Unique

@Entity
data class UserBoxEntity(
    @Id var boxId: Long = 0,
    @Index @Unique var uid: String,
    var login: String,
    var name: String,
    var passwordHash: String,
    var role: String
)
