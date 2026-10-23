package com.ppo.domain.fixtures

import com.ppo.domain.model.*
import java.time.Instant
import java.util.UUID

object TestUserMother {
    fun regularUser(): User = User(
        id = UserId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
        login = "alex_dev",
        name = "Alex Smirnov",
        passwordHash = "hash123456",
        role = UserRole.USER
    )

    fun adminUser(): User = User(
        id = UserId(UUID.fromString("00000000-0000-0000-0000-000000000002")),
        login = "admin_boss",
        name = "Ivan Petrov",
        passwordHash = "adminhash123",
        role = UserRole.ADMIN
    )

    fun otherUser(): User = User(
        id = UserId(UUID.fromString("00000000-0000-0000-0000-000000000003")),
        login = "bob_tester",
        name = "Bob Wilson",
        passwordHash = "bobhash123",
        role = UserRole.USER
    )
}

object TestUserSettingsMother {
    fun defaultSettings(userId: UserId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000001"))): UserSettings =
        UserSettings(
            userId = userId,
            autoStopEnabled = false,
            maxActiveDurationValue = 120,
            maxActiveDurationUnit = DurationUnit.minutes,
            minDurationEnabled = false,
            minDurationValue = 60,
            minDurationUnit = DurationUnit.seconds
        )

    fun strictSettings(userId: UserId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000001"))): UserSettings =
        UserSettings(
            userId = userId,
            autoStopEnabled = true,
            maxActiveDurationValue = 120,
            maxActiveDurationUnit = DurationUnit.minutes,
            minDurationEnabled = true,
            minDurationValue = 300,
            minDurationUnit = DurationUnit.seconds
        )
}

object TestCategoryMother {
    fun baseWorkCategory(): Category = Category(
        id = CategoryId(UUID.fromString("10000000-0000-0000-0000-000000000001")),
        userId = null,
        name = "Work",
        colorHex = "#3B82F6",
        isDefault = true
    )

    fun userCustomCategory(userId: UserId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000001"))): Category = Category(
        id = CategoryId(UUID.fromString("10000000-0000-0000-0000-000000000002")),
        userId = userId,
        name = "Sport",
        colorHex = "#10B981",
        isDefault = false
    )
}

object TestTimeRecordMother {
    fun activeTimerRecord(
        userId: UserId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
        categoryId: CategoryId = CategoryId(UUID.fromString("10000000-0000-0000-0000-000000000001")),
        startTime: Instant = Instant.parse("2026-10-15T10:00:00Z")
    ): TimeRecord = TimeRecord(
        id = RecordId(UUID.fromString("20000000-0000-0000-0000-000000000001")),
        userId = userId,
        categoryId = categoryId,
        startTime = startTime,
        endTime = null,
        description = "Active Task"
    )

    fun completedRecord(
        userId: UserId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
        categoryId: CategoryId = CategoryId(UUID.fromString("10000000-0000-0000-0000-000000000001")),
        startTime: Instant = Instant.parse("2026-10-15T10:00:00Z"),
        endTime: Instant = Instant.parse("2026-10-15T11:00:00Z")
    ): TimeRecord = TimeRecord(
        id = RecordId(UUID.fromString("20000000-0000-0000-0000-000000000002")),
        userId = userId,
        categoryId = categoryId,
        startTime = startTime,
        endTime = endTime,
        description = "Completed Task"
    )
}
