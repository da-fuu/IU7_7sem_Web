package com.ppo.web.data.orm

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.timestamp

object UsersTable : IdTable<String>("users") {
    override val id: Column<EntityID<String>> = varchar("id", 36).entityId()
    val login = varchar("login", 50).uniqueIndex()
    val name = varchar("name", 100)
    val passwordHash = varchar("password_hash", 256)
    val role = varchar("role", 20)

    override val primaryKey = PrimaryKey(id)
}

object UserSettingsTable : IdTable<String>("user_settings") {
    override val id: Column<EntityID<String>> = reference("user_id", UsersTable, onDelete = ReferenceOption.CASCADE)
    val autoStopEnabled = bool("auto_stop_enabled").default(true)
    val maxActiveDurationValue = long("max_active_duration_value").default(8)
    val maxActiveDurationUnit = varchar("max_active_duration_unit", 20).default("hours")
    val minDurationEnabled = bool("min_duration_enabled").default(true)
    val minDurationValue = long("min_duration_value").default(1)
    val minDurationUnit = varchar("min_duration_unit", 20).default("minutes")

    override val primaryKey = PrimaryKey(id)
}

object CategoriesTable : IdTable<String>("categories") {
    override val id: Column<EntityID<String>> = varchar("id", 36).entityId()
    val user = optReference("user_id", UsersTable, onDelete = ReferenceOption.CASCADE)
    val name = varchar("name", 100)
    val colorHex = varchar("color_hex", 7)
    val isDefault = bool("is_default").default(false)

    override val primaryKey = PrimaryKey(id)
}

object TimeRecordsTable : IdTable<String>("time_records") {
    override val id: Column<EntityID<String>> = varchar("id", 36).entityId()
    val user = reference("user_id", UsersTable, onDelete = ReferenceOption.CASCADE)
    val category = reference("category_id", CategoriesTable, onDelete = ReferenceOption.CASCADE)
    val startTime = timestamp("start_time")
    val endTime = timestamp("end_time").nullable()
    val description = varchar("description", 1000).default("")

    override val primaryKey = PrimaryKey(id)
}

object RecordHistoryTable : IdTable<String>("time_record_history") {
    override val id: Column<EntityID<String>> = varchar("id", 36).entityId()
    val record = reference("record_id", TimeRecordsTable, onDelete = ReferenceOption.CASCADE)
    val categoryId = varchar("category_id", 36)
    val startTime = timestamp("start_time")
    val endTime = timestamp("end_time").nullable()
    val description = varchar("description", 1000).default("")
    val modifiedAt = timestamp("modified_at")

    override val primaryKey = PrimaryKey(id)
}
