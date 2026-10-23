package com.ppo.domain.builders

import com.ppo.domain.model.*
import java.time.Instant
import java.util.UUID

class TimeRecordBuilder {
    private var id: RecordId = RecordId(UUID.randomUUID())
    private var userId: UserId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
    private var categoryId: CategoryId = CategoryId(UUID.fromString("10000000-0000-0000-0000-000000000001"))
    private var startTime: Instant = Instant.parse("2026-10-15T10:00:00Z")
    private var endTime: Instant? = Instant.parse("2026-10-15T11:00:00Z")
    private var description: String = "Test Task"

    fun withId(id: RecordId) = apply { this.id = id }
    fun withUserId(userId: UserId) = apply { this.userId = userId }
    fun withCategoryId(categoryId: CategoryId) = apply { this.categoryId = categoryId }
    fun withStartTime(startTime: Instant) = apply { this.startTime = startTime }
    fun withEndTime(endTime: Instant?) = apply { this.endTime = endTime }
    fun asActive() = apply { this.endTime = null }
    fun withDurationMinutes(minutes: Long) = apply { this.endTime = this.startTime.plusSeconds(minutes * 60) }
    fun withDurationSeconds(seconds: Long) = apply { this.endTime = this.startTime.plusSeconds(seconds) }
    fun withDescription(description: String) = apply { this.description = description }

    fun build(): TimeRecord = TimeRecord(
        id = id,
        userId = userId,
        categoryId = categoryId,
        startTime = startTime,
        endTime = endTime,
        description = description
    )
}

class UserSettingsBuilder {
    private var userId: UserId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
    private var autoStopEnabled: Boolean = false
    private var maxActiveDurationValue: Long = 120
    private var maxActiveDurationUnit: DurationUnit = DurationUnit.minutes
    private var minDurationEnabled: Boolean = false
    private var minDurationValue: Long = 60
    private var minDurationUnit: DurationUnit = DurationUnit.seconds

    fun withUserId(userId: UserId) = apply { this.userId = userId }
    fun withAutoStop(enabled: Boolean, value: Long = 120, unit: DurationUnit = DurationUnit.minutes) = apply {
        this.autoStopEnabled = enabled
        this.maxActiveDurationValue = value
        this.maxActiveDurationUnit = unit
    }
    fun withMinDuration(enabled: Boolean, value: Long = 60, unit: DurationUnit = DurationUnit.seconds) = apply {
        this.minDurationEnabled = enabled
        this.minDurationValue = value
        this.minDurationUnit = unit
    }

    fun build(): UserSettings = UserSettings(
        userId = userId,
        autoStopEnabled = autoStopEnabled,
        maxActiveDurationValue = maxActiveDurationValue,
        maxActiveDurationUnit = maxActiveDurationUnit,
        minDurationEnabled = minDurationEnabled,
        minDurationValue = minDurationValue,
        minDurationUnit = minDurationUnit
    )
}

class CategoryBuilder {
    private var id: CategoryId = CategoryId(UUID.randomUUID())
    private var userId: UserId? = UserId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
    private var name: String = "Test Category"
    private var colorHex: String = "#3B82F6"
    private var isDefault: Boolean = false

    fun withId(id: CategoryId) = apply { this.id = id }
    fun withUserId(userId: UserId?) = apply { this.userId = userId }
    fun withName(name: String) = apply { this.name = name }
    fun withColorHex(colorHex: String) = apply { this.colorHex = colorHex }
    fun asDefault() = apply {
        this.isDefault = true
        this.userId = null
    }

    fun build(): Category = Category(
        id = id,
        userId = userId,
        name = name,
        colorHex = colorHex,
        isDefault = isDefault
    )
}

class UserBuilder {
    private var id: UserId = UserId(UUID.randomUUID())
    private var login: String = "test_user"
    private var name: String = "Test User"
    private var passwordHash: String = "hashed_pass"
    private var role: UserRole = UserRole.USER

    fun withId(id: UserId) = apply { this.id = id }
    fun withLogin(login: String) = apply { this.login = login }
    fun withName(name: String) = apply { this.name = name }
    fun withPasswordHash(passwordHash: String) = apply { this.passwordHash = passwordHash }
    fun withRole(role: UserRole) = apply { this.role = role }

    fun build(): User = User(
        id = id,
        login = login,
        name = name,
        passwordHash = passwordHash,
        role = role
    )
}
