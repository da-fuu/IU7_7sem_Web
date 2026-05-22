package com.ppo.data.mapper

import com.ppo.data.entity.CategoryEntity
import com.ppo.data.entity.TimeRecordEntity
import com.ppo.data.entity.UserEntity
import com.ppo.domain.model.Category
import com.ppo.domain.model.CategoryId
import com.ppo.domain.model.RecordId
import com.ppo.domain.model.TimeRecord
import com.ppo.domain.model.User
import com.ppo.domain.model.UserId
import java.util.UUID

fun UserEntity.toDomain(): User = User(
    id = UserId(UUID.fromString(uid)),
    login = login,
    name = name,
    passwordHash = passwordHash,
    role = role
)

fun User.toEntity(): UserEntity = UserEntity(
    uid = id.value.toString(),
    login = login,
    name = name,
    passwordHash = passwordHash,
    role = role
)

fun CategoryEntity.toDomain(): Category = Category(
    id = CategoryId(UUID.fromString(uid)),
    userId = userId?.let { UserId(UUID.fromString(it)) },
    name = name,
    colorHex = colorHex,
    isDefault = isDefault
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    uid = id.value.toString(),
    userId = userId?.value?.toString(),
    name = name,
    colorHex = colorHex,
    isDefault = isDefault
)

fun TimeRecordEntity.toDomain(): TimeRecord = TimeRecord(
    id = RecordId(UUID.fromString(uid)),
    userId = UserId(UUID.fromString(userId)),
    categoryId = CategoryId(UUID.fromString(categoryId)),
    startTime = startTime,
    endTime = endTime,
    description = description
)

fun TimeRecord.toEntity(): TimeRecordEntity = TimeRecordEntity(
    uid = id.value.toString(),
    userId = userId.value.toString(),
    categoryId = categoryId.value.toString(),
    startTime = startTime,
    endTime = endTime,
    description = description
)
