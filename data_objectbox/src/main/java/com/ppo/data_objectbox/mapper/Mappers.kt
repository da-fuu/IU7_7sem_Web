package com.ppo.data_objectbox.mapper

import com.ppo.data_objectbox.entity.CategoryBoxEntity
import com.ppo.data_objectbox.entity.DescriptionVersionBoxEntity
import com.ppo.data_objectbox.entity.TimeRecordBoxEntity
import com.ppo.data_objectbox.entity.UserBoxEntity
import com.ppo.domain.model.Category
import com.ppo.domain.model.CategoryId
import com.ppo.domain.model.DescriptionVersion
import com.ppo.domain.model.RecordId
import com.ppo.domain.model.TimeRecord
import com.ppo.domain.model.User
import com.ppo.domain.model.UserId
import com.ppo.domain.model.UserRole
import com.ppo.domain.model.VersionId
import java.time.Instant
import java.util.UUID

fun UserBoxEntity.toDomain() = User(
    id = UserId(UUID.fromString(uid)),
    login = login,
    name = name,
    passwordHash = passwordHash,
    role = UserRole.valueOf(role)
)

fun User.toBoxEntity(existingBoxId: Long = 0) = UserBoxEntity(
    boxId = existingBoxId,
    uid = id.value.toString(),
    login = login,
    name = name,
    passwordHash = passwordHash,
    role = role.name
)

fun CategoryBoxEntity.toDomain() = Category(
    id = CategoryId(UUID.fromString(uid)),
    userId = userId?.let { UserId(UUID.fromString(it)) },
    name = name,
    colorHex = colorHex,
    isDefault = isDefault
)

fun Category.toBoxEntity(existingBoxId: Long = 0) = CategoryBoxEntity(
    boxId = existingBoxId,
    uid = id.value.toString(),
    userId = userId?.value?.toString(),
    name = name,
    colorHex = colorHex,
    isDefault = isDefault
)

fun TimeRecordBoxEntity.toDomain() = TimeRecord(
    id = RecordId(UUID.fromString(uid)),
    userId = UserId(UUID.fromString(userId)),
    categoryId = CategoryId(UUID.fromString(categoryId)),
    startTime = Instant.ofEpochMilli(startTime),
    endTime = endTime?.let { Instant.ofEpochMilli(it) },
    description = description
)

fun TimeRecord.toBoxEntity(existingBoxId: Long = 0) = TimeRecordBoxEntity(
    boxId = existingBoxId,
    uid = id.value.toString(),
    userId = userId.value.toString(),
    categoryId = categoryId.value.toString(),
    startTime = startTime.toEpochMilli(),
    endTime = endTime?.toEpochMilli(),
    description = description
)

fun DescriptionVersionBoxEntity.toDomain() = DescriptionVersion(
    id = VersionId(UUID.fromString(uid)),
    recordId = RecordId(UUID.fromString(recordId)),
    description = description,
    modifiedAt = Instant.ofEpochMilli(modifiedAt)
)

fun DescriptionVersion.toBoxEntity(existingBoxId: Long = 0) = DescriptionVersionBoxEntity(
    boxId = existingBoxId,
    uid = id.value.toString(),
    recordId = recordId.value.toString(),
    description = description,
    modifiedAt = modifiedAt.toEpochMilli()
)
