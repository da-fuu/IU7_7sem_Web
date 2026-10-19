package com.ppo.web.data.repository

import com.ppo.domain.model.UserId
import com.ppo.domain.model.UserSettings
import com.ppo.domain.repository.IUserSettingsRepository
import com.ppo.web.data.orm.UserEntity
import com.ppo.web.data.orm.UserSettingsEntity
import org.jetbrains.exposed.sql.transactions.transaction
import javax.sql.DataSource

class PostgresUserSettingsRepository(private val dataSource: DataSource) : IUserSettingsRepository {
    override suspend fun getSettings(userId: UserId): UserSettings = transaction {
        UserSettingsEntity.findById(userId.value.toString())?.toDomain() ?: UserSettings(userId = userId)
    }

    override suspend fun updateSettings(settings: UserSettings): Result<UserSettings> = runCatching {
        transaction {
            val existing = UserSettingsEntity.findById(settings.userId.value.toString())
            if (existing != null) {
                existing.autoStopEnabled = settings.autoStopEnabled
                existing.maxActiveDurationValue = settings.maxActiveDurationValue
                existing.maxActiveDurationUnit = settings.maxActiveDurationUnit.name
                existing.minDurationEnabled = settings.minDurationEnabled
                existing.minDurationValue = settings.minDurationValue
                existing.minDurationUnit = settings.minDurationUnit.name
                existing.toDomain()
            } else {
                val user = UserEntity.findById(settings.userId.value.toString())
                if (user != null) {
                    UserSettingsEntity.new(settings.userId.value.toString()) {
                        this.autoStopEnabled = settings.autoStopEnabled
                        this.maxActiveDurationValue = settings.maxActiveDurationValue
                        this.maxActiveDurationUnit = settings.maxActiveDurationUnit.name
                        this.minDurationEnabled = settings.minDurationEnabled
                        this.minDurationValue = settings.minDurationValue
                        this.minDurationUnit = settings.minDurationUnit.name
                    }.toDomain()
                } else {
                    settings
                }
            }
        }
    }
}
