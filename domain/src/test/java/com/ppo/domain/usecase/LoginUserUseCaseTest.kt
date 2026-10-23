package com.ppo.domain.usecase

import com.ppo.domain.builders.UserBuilder
import com.ppo.domain.repository.IPasswordHasher
import com.ppo.domain.repository.IUserRepository
import com.ppo.domain.runner.RandomOrderRunner
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(RandomOrderRunner::class)
class LoginUserUseCaseTest {
    private val userRepository = mockk<IUserRepository>()
    private val passwordHasher = mockk<IPasswordHasher>()
    private lateinit var useCase: LoginUserUseCase

    @Before
    fun setUp() {
        useCase = LoginUserUseCase(userRepository, passwordHasher)
    }

    @Test
    fun shouldReturnUser_whenPasswordMatches() = runBlocking {
        val user = UserBuilder().withLogin("alex").withPasswordHash("hashed_secret").build()
        coEvery { userRepository.findByLogin("alex") } returns user
        every { passwordHasher.verify("secret", "hashed_secret") } returns true

        val result = useCase("alex", "secret")

        assertTrue(result.isSuccess)
        assertEquals(user, result.getOrNull())
    }

    @Test
    fun shouldFailWithIllegalState_whenUserNotFound() = runBlocking {
        coEvery { userRepository.findByLogin("unknown") } returns null

        val result = useCase("unknown", "secret")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun shouldFailWithIllegalState_whenPasswordIncorrect() = runBlocking {
        val user = UserBuilder().withLogin("alex").withPasswordHash("hashed_secret").build()
        coEvery { userRepository.findByLogin("alex") } returns user
        every { passwordHasher.verify("wrong", "hashed_secret") } returns false

        val result = useCase("alex", "wrong")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }
}
