package com.ppo.web.it

import com.ppo.domain.model.DurationUnit
import com.ppo.domain.model.UserRole
import com.ppo.domain.usecase.GetUserInfoUseCase
import com.ppo.domain.usecase.LoginUserUseCase
import com.ppo.domain.usecase.ManageUserSettingsUseCase
import com.ppo.domain.usecase.RegisterUserUseCase
import com.ppo.web.data.repository.PostgresUserRepository
import com.ppo.web.data.repository.PostgresUserSettingsRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class UserManagementITCase {

    private val dataSource = TestDbHelper.getDataSource()
    private val userRepository = PostgresUserRepository(dataSource)
    private val userSettingsRepository = PostgresUserSettingsRepository(dataSource)
    private val passwordHasher = TestPasswordHasher

    private val registerUserUseCase = RegisterUserUseCase(userRepository, passwordHasher)
    private val loginUserUseCase = LoginUserUseCase(userRepository, passwordHasher)
    private val getUserInfoUseCase = GetUserInfoUseCase(userRepository)
    private val manageUserSettingsUseCase = ManageUserSettingsUseCase(userSettingsRepository)

    @Before
    fun setUp() {
        TestDbHelper.initTestSchema()
        TestDbHelper.resetDatabase()
    }

    @After
    fun tearDown() {
        TestDbHelper.resetDatabase()
    }

    @Test
    fun executeUserManagementFunctionalityScenario(): Unit = runBlocking {
        val testSuffix = UUID.randomUUID().toString().take(8)
        val login = "dev_$testSuffix"
        val password = "StrongPassword123"
        val name = "Integration Developer"

        val registerResult = registerUserUseCase(
            login = login,
            name = name,
            rawPassword = password,
            role = UserRole.USER
        )
        assertTrue(registerResult.isSuccess)

        val createdUser = userRepository.findByLogin(login)
        assertNotNull(createdUser)
        assertEquals(login, createdUser?.login)
        assertEquals(name, createdUser?.name)
        assertEquals(UserRole.USER, createdUser?.role)
        assertTrue(createdUser?.passwordHash != password)
        assertTrue(passwordHasher.verify(password, createdUser?.passwordHash ?: ""))

        val duplicateResult = registerUserUseCase(
            login = login,
            name = "Duplicate Dev",
            rawPassword = password,
            role = UserRole.USER
        )
        assertTrue(duplicateResult.isFailure)
        assertTrue(duplicateResult.exceptionOrNull() is IllegalStateException)

        val shortPasswordResult = registerUserUseCase(
            login = "short_$testSuffix",
            name = "Short Pass Dev",
            rawPassword = "123",
            role = UserRole.USER
        )
        assertTrue(shortPasswordResult.isFailure)
        assertTrue(shortPasswordResult.exceptionOrNull() is IllegalArgumentException)

        val successfulLoginResult = loginUserUseCase(login, password)
        assertTrue(successfulLoginResult.isSuccess)
        val authenticatedUser = successfulLoginResult.getOrNull()
        assertNotNull(authenticatedUser)
        assertEquals(createdUser?.id, authenticatedUser?.id)

        val failedLoginResult = loginUserUseCase(login, "WrongPassword")
        assertTrue(failedLoginResult.isFailure)
        assertTrue(failedLoginResult.exceptionOrNull() is IllegalStateException)

        val profileUser = getUserInfoUseCase(createdUser!!.id)
        assertNotNull(profileUser)
        assertEquals(createdUser.id, profileUser?.id)

        val currentSettings = manageUserSettingsUseCase.getSettings(createdUser.id)
        val updatedSettingsCandidate = currentSettings.copy(
            autoStopEnabled = true,
            maxActiveDurationValue = 4,
            maxActiveDurationUnit = DurationUnit.hours,
            minDurationEnabled = true,
            minDurationValue = 2,
            minDurationUnit = DurationUnit.minutes
        )
        val updateResult = manageUserSettingsUseCase.updateSettings(updatedSettingsCandidate)
        assertTrue(updateResult.isSuccess)

        val persistedSettings = manageUserSettingsUseCase.getSettings(createdUser.id)
        assertEquals(4L, persistedSettings.maxActiveDurationValue)
        assertEquals(DurationUnit.hours, persistedSettings.maxActiveDurationUnit)
        assertTrue(persistedSettings.minDurationEnabled)
        assertEquals(2L, persistedSettings.minDurationValue)
        assertEquals(DurationUnit.minutes, persistedSettings.minDurationUnit)

        val invalidSettingsCandidate = currentSettings.copy(minDurationValue = 0)
        val invalidUpdateResult = manageUserSettingsUseCase.updateSettings(invalidSettingsCandidate)
        assertTrue(invalidUpdateResult.isFailure)
        assertTrue(invalidUpdateResult.exceptionOrNull() is IllegalArgumentException)
    }
}
