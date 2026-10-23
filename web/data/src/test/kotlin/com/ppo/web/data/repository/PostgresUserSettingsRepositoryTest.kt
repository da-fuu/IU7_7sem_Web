package com.ppo.web.data.repository

import com.ppo.domain.model.DurationUnit
import com.ppo.domain.model.User
import com.ppo.domain.model.UserId
import com.ppo.domain.model.UserRole
import com.ppo.domain.model.UserSettings
import com.ppo.web.data.TestDatabaseFactory
import com.ppo.web.data.runner.RandomOrderRunner
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(RandomOrderRunner::class)
class PostgresUserSettingsRepositoryTest {

    private lateinit var tempDbFile: File
    private lateinit var userRepo: PostgresUserRepository
    private lateinit var repository: PostgresUserSettingsRepository

    @Before
    fun setUp() {
        val (dataSource, file) = TestDatabaseFactory.createTestDataSource()
        tempDbFile = file
        userRepo = PostgresUserRepository(dataSource)
        repository = PostgresUserSettingsRepository(dataSource)
    }

    @After
    fun tearDown() {
        tempDbFile.delete()
    }

    @Test
    fun getSettings_missingInDb_returnsDefaultFallbackSettings() = runTest {
        val userId = UserId(UUID.randomUUID())

        val settings = repository.getSettings(userId)

        assertEquals(userId, settings.userId)
        assertTrue(settings.autoStopEnabled)
        assertEquals(8L, settings.maxActiveDurationValue)
        assertEquals(DurationUnit.hours, settings.maxActiveDurationUnit)
        assertTrue(settings.minDurationEnabled)
        assertEquals(1L, settings.minDurationValue)
        assertEquals(DurationUnit.minutes, settings.minDurationUnit)
    }

    @Test
    fun updateSettings_validValues_persistsAndUpdatesExistingSettings() = runTest {
        val user = User(
            id = UserId(UUID.randomUUID()),
            login = "settings_user_${UUID.randomUUID()}",
            name = "Settings User",
            passwordHash = "hash",
            role = UserRole.USER
        )
        userRepo.saveUser(user)
        val customSettings = UserSettings(
            userId = user.id,
            autoStopEnabled = false,
            maxActiveDurationValue = 240L,
            maxActiveDurationUnit = DurationUnit.minutes,
            minDurationEnabled = false,
            minDurationValue = 30L,
            minDurationUnit = DurationUnit.seconds
        )

        val result = repository.updateSettings(customSettings)

        assertTrue(result.isSuccess)
        val loaded = repository.getSettings(user.id)
        assertFalse(loaded.autoStopEnabled)
        assertEquals(240L, loaded.maxActiveDurationValue)
        assertEquals(DurationUnit.minutes, loaded.maxActiveDurationUnit)
        assertFalse(loaded.minDurationEnabled)
        assertEquals(30L, loaded.minDurationValue)
        assertEquals(DurationUnit.seconds, loaded.minDurationUnit)
    }
}
