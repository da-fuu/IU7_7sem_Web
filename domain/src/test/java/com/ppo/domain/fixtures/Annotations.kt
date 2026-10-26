package com.ppo.domain.fixtures

import com.ppo.domain.model.DurationUnit

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class CustomFixture(val name: String = "")

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class WithRegularUser

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class WithAdminUser

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class WithStrictSettings(
    val minDurationValue: Long = 300,
    val minDurationUnit: DurationUnit = DurationUnit.seconds,
    val autoStopMinutes: Long = 120
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class WithDefaultSettings

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class WithBaseCategory

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class WithUserCategory

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class WithCustomCategory(
    val name: String = "Тестовая категория",
    val colorHex: String = "#3B82F6",
    val isDefault: Boolean = false
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class WithActiveTimeRecord(
    val description: String = "Активная задача"
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class WithCompletedTimeRecord(
    val durationMinutes: Long = 60,
    val description: String = "Завершенная задача"
)
