package com.ppo.domain.usecase

import com.ppo.domain.model.UserId
import com.ppo.domain.model.UserSettings
import com.ppo.domain.repository.IUserSettingsRepository

class ManageUserSettingsUseCase(
    private val userSettingsRepository: IUserSettingsRepository
) {
    suspend fun getSettings(userId: UserId): UserSettings {
        return userSettingsRepository.getSettings(userId)
    }

    suspend fun updateSettings(settings: UserSettings): Result<UserSettings> {
        if (settings.maxActiveDurationValue < 1) {
            return Result.failure(IllegalArgumentException("maxActiveDurationValue must be >= 1"))
        }
        if (settings.minDurationValue < 1) {
            return Result.failure(IllegalArgumentException("minDurationValue must be >= 1"))
        }
        return userSettingsRepository.updateSettings(settings).map { settings }
    }
}
