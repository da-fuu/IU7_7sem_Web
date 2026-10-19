package com.ppo.domain.model

data class UserSettings(
    val userId: UserId,
    val autoStopEnabled: Boolean = true,
    val maxActiveDurationValue: Long = 8L,
    val maxActiveDurationUnit: DurationUnit = DurationUnit.hours,
    val minDurationEnabled: Boolean = true,
    val minDurationValue: Long = 1L,
    val minDurationUnit: DurationUnit = DurationUnit.minutes
)
