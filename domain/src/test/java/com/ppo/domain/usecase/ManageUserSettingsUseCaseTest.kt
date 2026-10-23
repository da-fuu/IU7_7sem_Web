package com.ppo.domain.usecase

import com.ppo.domain.builders.UserSettingsBuilder
import com.ppo.domain.fixtures.TestUserSettingsMother
import com.ppo.domain.model.UserId
import com.ppo.domain.repository.IUserSettingsRepository
import com.ppo.domain.runner.RandomOrderRunner
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(RandomOrderRunner::class)
class ManageUserSettingsUseCaseTest {
    private val userSettingsRepository = mockk<IUserSettingsRepository>()
    private lateinit var useCase: ManageUserSettingsUseCase

    @Before
    fun setUp() {
        useCase = ManageUserSettingsUseCase(userSettingsRepository)
    }

    @Test
    fun shouldReturnSettings_whenQueriedForUser() = runBlocking {
        val userId = UserId(UUID.randomUUID())
        val settings = TestUserSettingsMother.defaultSettings(userId)
        coEvery { userSettingsRepository.getSettings(userId) } returns settings

        val result = useCase.getSettings(userId)

        assertEquals(settings, result)
    }

    @Test
    fun shouldUpdateSettings_whenValuesAreValid() = runBlocking {
        val userId = UserId(UUID.randomUUID())
        val settings = UserSettingsBuilder().withUserId(userId).withAutoStop(true, 180).withMinDuration(true, 10).build()
        coEvery { userSettingsRepository.updateSettings(settings) } returns Result.success(settings)

        val result = useCase.updateSettings(settings)

        assertTrue(result.isSuccess)
        assertEquals(settings, result.getOrNull())
        coVerify(exactly = 1) { userSettingsRepository.updateSettings(settings) }
    }

    @Test
    fun shouldFailWithIllegalArgument_whenMaxDurationValueLessThanOne() = runBlocking {
        val userId = UserId(UUID.randomUUID())
        val invalidSettings = UserSettingsBuilder().withUserId(userId).withAutoStop(true, 0).build()

        val result = useCase.updateSettings(invalidSettings)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun shouldFailWithIllegalArgument_whenMinDurationValueLessThanOne() = runBlocking {
        val userId = UserId(UUID.randomUUID())
        val invalidSettings = UserSettingsBuilder().withUserId(userId).withMinDuration(true, 0).build()

        val result = useCase.updateSettings(invalidSettings)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }
}
