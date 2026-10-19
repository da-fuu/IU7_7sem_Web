package com.ppo.web.dto

data class UserDto(
    val id: String,
    val login: String,
    val name: String,
    val role: String
)

data class UserRegistrationRequestDto(
    val login: String,
    val name: String,
    val password: String,
    val role: String? = "USER"
)

data class UserLoginRequestDto(
    val login: String,
    val password: String
)

data class AuthResponseDto(
    val token: String,
    val user: UserDto
)

data class UserSettingsDto(
    val autoStopEnabled: Boolean,
    val maxActiveDurationValue: Long,
    val maxActiveDurationUnit: String,
    val minDurationEnabled: Boolean,
    val minDurationValue: Long,
    val minDurationUnit: String
)

data class UserSettingsUpdateRequestDto(
    val autoStopEnabled: Boolean? = null,
    val maxActiveDurationValue: Long? = null,
    val maxActiveDurationUnit: String? = null,
    val minDurationEnabled: Boolean? = null,
    val minDurationValue: Long? = null,
    val minDurationUnit: String? = null
)

data class CategoryDto(
    val id: String,
    val userId: String? = null,
    val name: String,
    val colorHex: String,
    val isDefault: Boolean
)

data class CategoryListResponseDto(
    val items: List<CategoryDto>
)

data class CategoryCreateRequestDto(
    val name: String,
    val colorHex: String,
    val isDefault: Boolean? = false
)

data class CategoryUpdateRequestDto(
    val name: String,
    val colorHex: String
)

data class CategoryPatchRequestDto(
    val name: String? = null,
    val colorHex: String? = null
)

data class TimeRecordDto(
    val id: String,
    val userId: String,
    val categoryId: String,
    val startTime: String,
    val endTime: String? = null,
    val description: String
)

data class TimeRecordCreateRequestDto(
    val categoryId: String,
    val startTime: String? = null,
    val endTime: String? = null,
    val description: String? = ""
)

data class TimeRecordUpdateRequestDto(
    val categoryId: String,
    val startTime: String,
    val endTime: String? = null,
    val description: String
)

data class TimeRecordPatchRequestDto(
    val categoryId: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val description: String? = null
)

data class PaginatedRecordsResponseDto(
    val items: List<TimeRecordDto>,
    val total: Int,
    val limit: Int,
    val offset: Int
)

data class RecordHistoryEntryDto(
    val id: String,
    val recordId: String,
    val categoryId: String,
    val startTime: String,
    val endTime: String? = null,
    val description: String,
    val modifiedAt: String
)

data class RecordHistoryListResponseDto(
    val items: List<RecordHistoryEntryDto>
)

data class ErrorResponseDto(
    val code: String,
    val message: String,
    val details: List<String>? = null,
    val timestamp: String
)
