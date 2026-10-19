package com.ppo.domain.repository

import com.ppo.domain.model.UserId
import com.ppo.domain.model.UserSettings

interface IUserSettingsRepository {
    suspend fun getSettings(userId: UserId): UserSettings
    suspend fun updateSettings(settings: UserSettings): Result<UserSettings>
}
