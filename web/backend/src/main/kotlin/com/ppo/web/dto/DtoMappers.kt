package com.ppo.web.dto

import com.ppo.domain.model.*

fun User.toDto(): UserDto = UserDto(
    id = id.value.toString(),
    login = login,
    name = name,
    role = role.name
)

fun UserSettings.toDto(): UserSettingsDto = UserSettingsDto(
    autoStopEnabled = autoStopEnabled,
    maxActiveDurationValue = maxActiveDurationValue,
    maxActiveDurationUnit = maxActiveDurationUnit.name,
    minDurationEnabled = minDurationEnabled,
    minDurationValue = minDurationValue,
    minDurationUnit = minDurationUnit.name
)

fun Category.toDto(): CategoryDto = CategoryDto(
    id = id.value.toString(),
    userId = userId?.value?.toString(),
    name = name,
    colorHex = colorHex,
    isDefault = isDefault
)

fun TimeRecord.toDto(): TimeRecordDto = TimeRecordDto(
    id = id.value.toString(),
    userId = userId.value.toString(),
    categoryId = categoryId.value.toString(),
    startTime = startTime.toString(),
    endTime = endTime?.toString(),
    description = description
)

fun RecordHistoryEntry.toDto(): RecordHistoryEntryDto = RecordHistoryEntryDto(
    id = id.value.toString(),
    recordId = recordId.value.toString(),
    categoryId = categoryId.value.toString(),
    startTime = startTime.toString(),
    endTime = endTime?.toString(),
    description = description,
    modifiedAt = modifiedAt.toString()
)
