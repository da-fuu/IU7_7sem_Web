package com.ppo.web.data.orm

import com.ppo.domain.model.Category
import com.ppo.domain.model.CategoryId
import com.ppo.domain.model.DurationUnit
import com.ppo.domain.model.RecordHistoryEntry
import com.ppo.domain.model.RecordId
import com.ppo.domain.model.TimeRecord
import com.ppo.domain.model.User
import com.ppo.domain.model.UserId
import com.ppo.domain.model.UserRole
import com.ppo.domain.model.UserSettings
import com.ppo.domain.model.VersionId
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.util.UUID

class UserEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, UserEntity>(UsersTable)

    var login by UsersTable.login
    var name by UsersTable.name
    var passwordHash by UsersTable.passwordHash
    var role by UsersTable.role

    fun toDomain(): User = User(
        id = UserId(UUID.fromString(id.value)),
        login = login,
        name = name,
        passwordHash = passwordHash,
        role = UserRole.valueOf(role)
    )
}

class UserSettingsEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, UserSettingsEntity>(UserSettingsTable)

    var autoStopEnabled by UserSettingsTable.autoStopEnabled
    var maxActiveDurationValue by UserSettingsTable.maxActiveDurationValue
    var maxActiveDurationUnit by UserSettingsTable.maxActiveDurationUnit
    var minDurationEnabled by UserSettingsTable.minDurationEnabled
    var minDurationValue by UserSettingsTable.minDurationValue
    var minDurationUnit by UserSettingsTable.minDurationUnit

    fun toDomain(): UserSettings = UserSettings(
        userId = UserId(UUID.fromString(id.value)),
        autoStopEnabled = autoStopEnabled,
        maxActiveDurationValue = maxActiveDurationValue,
        maxActiveDurationUnit = DurationUnit.valueOf(maxActiveDurationUnit),
        minDurationEnabled = minDurationEnabled,
        minDurationValue = minDurationValue,
        minDurationUnit = DurationUnit.valueOf(minDurationUnit)
    )
}

class CategoryEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, CategoryEntity>(CategoriesTable)

    var user by UserEntity optionalReferencedOn CategoriesTable.user
    var name by CategoriesTable.name
    var colorHex by CategoriesTable.colorHex
    var isDefault by CategoriesTable.isDefault

    fun toDomain(): Category = Category(
        id = CategoryId(UUID.fromString(id.value)),
        userId = user?.id?.value?.let { UserId(UUID.fromString(it)) },
        name = name,
        colorHex = colorHex,
        isDefault = isDefault
    )
}

class TimeRecordEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, TimeRecordEntity>(TimeRecordsTable)

    var user by UserEntity referencedOn TimeRecordsTable.user
    var category by CategoryEntity referencedOn TimeRecordsTable.category
    var startTime by TimeRecordsTable.startTime
    var endTime by TimeRecordsTable.endTime
    var description by TimeRecordsTable.description

    fun toDomain(): TimeRecord = TimeRecord(
        id = RecordId(UUID.fromString(id.value)),
        userId = UserId(UUID.fromString(user.id.value)),
        categoryId = CategoryId(UUID.fromString(category.id.value)),
        startTime = startTime,
        endTime = endTime,
        description = description
    )
}

class RecordHistoryEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, RecordHistoryEntity>(RecordHistoryTable)

    var record by TimeRecordEntity referencedOn RecordHistoryTable.record
    var categoryId by RecordHistoryTable.categoryId
    var startTime by RecordHistoryTable.startTime
    var endTime by RecordHistoryTable.endTime
    var description by RecordHistoryTable.description
    var modifiedAt by RecordHistoryTable.modifiedAt

    fun toDomain(): RecordHistoryEntry = RecordHistoryEntry(
        id = VersionId(UUID.fromString(id.value)),
        recordId = RecordId(UUID.fromString(record.id.value)),
        categoryId = CategoryId(UUID.fromString(categoryId)),
        startTime = startTime,
        endTime = endTime,
        description = description,
        modifiedAt = modifiedAt
    )
}
