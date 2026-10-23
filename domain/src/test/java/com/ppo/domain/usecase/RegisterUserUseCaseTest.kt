package com.ppo.domain.usecase

import com.ppo.domain.builders.UserBuilder
import com.ppo.domain.model.UserRole
import com.ppo.domain.repository.IPasswordHasher
import com.ppo.domain.repository.IUserRepository
import com.ppo.domain.runner.RandomOrderRunner
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(RandomOrderRunner::class)
class RegisterUserUseCaseTest {
    private val userRepository = mockk<IUserRepository>()
    private val passwordHasher = mockk<IPasswordHasher>()
    private lateinit var useCase: RegisterUserUseCase

    @Before
    fun setUp() {
        useCase = RegisterUserUseCase(userRepository, passwordHasher)
    }

    @Test
    fun shouldRegisterUser_whenValidCredentials() = runBlocking {
        coEvery { userRepository.findByLogin("new_user") } returns null
        every { passwordHasher.hash("secure123") } returns "hashed_secure123"
        coEvery { userRepository.saveUser(any()) } returns Result.success(Unit)

        val result = useCase("new_user", "New User", "secure123", UserRole.USER)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { userRepository.saveUser(match { it.login == "new_user" && it.passwordHash == "hashed_secure123" }) }
    }

    @Test
    fun shouldFailWithIllegalArgument_whenPasswordTooShort() = runBlocking {
        val result = useCase("new_user", "New User", "12345", UserRole.USER)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun shouldFailWithIllegalState_whenLoginAlreadyExists() = runBlocking {
        val existing = UserBuilder().withLogin("existing_user").build()
        coEvery { userRepository.findByLogin("existing_user") } returns existing

        val result = useCase("existing_user", "Name", "password123", UserRole.USER)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }
}
