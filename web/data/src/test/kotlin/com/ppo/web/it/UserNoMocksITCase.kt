package com.ppo.web.it

import com.ppo.domain.model.DurationUnit
import com.ppo.domain.model.UserId
import com.ppo.domain.model.UserSettings
import com.ppo.domain.usecase.ManageUserSettingsUseCase
import com.ppo.web.data.repository.PostgresUserSettingsRepository
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Rule
import org.junit.Before
import java.util.UUID

@RunWith(AllureRunner::class)
class UserNoMocksITCase {

    @get:Rule
    val rollbackRule = TestDbHelper.rollbackRule()

    private val rawUserId = UUID.randomUUID()
    private val userId = UserId(rawUserId)
    private val login = "user_nomock_${rawUserId.toString()}"

    @Before
    fun setUp() {
        transaction {
            exec("INSERT INTO users (id, login, name, password_hash, role) VALUES ('$rawUserId', '$login', 'NoMock User', 'hash', 'USER')")
        }
    }

    @Test
    fun executeUserSettingsWithoutMocks() = runBlocking {
        val repository = PostgresUserSettingsRepository(TestDbHelper.getDataSource())
        val useCase = ManageUserSettingsUseCase(repository)

        val initialSettings = useCase.getSettings(userId)
        assertNotNull(initialSettings)

        val updatedCandidate = initialSettings.copy(
            autoStopEnabled = true,
            maxActiveDurationValue = 4,
            maxActiveDurationUnit = DurationUnit.hours,
            minDurationEnabled = true,
            minDurationValue = 15,
            minDurationUnit = DurationUnit.minutes
        )
        val updateResult = useCase.updateSettings(updatedCandidate)
        assertTrue(updateResult.isSuccess)

        val retrievedSettings = repository.getSettings(userId)
        assertEquals(true, retrievedSettings.autoStopEnabled)
        assertEquals(4L, retrievedSettings.maxActiveDurationValue)
        assertEquals(DurationUnit.hours, retrievedSettings.maxActiveDurationUnit)
        assertEquals(true, retrievedSettings.minDurationEnabled)
        assertEquals(15L, retrievedSettings.minDurationValue)
        assertEquals(DurationUnit.minutes, retrievedSettings.minDurationUnit)
    }
}
