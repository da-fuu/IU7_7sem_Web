package com.ppo.domain.usecase

import com.ppo.domain.model.User
import com.ppo.domain.model.UserId
import com.ppo.domain.model.UserRole
import com.ppo.domain.repository.IPasswordHasher
import com.ppo.domain.repository.IUserRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class LoginUserUseCaseTest {

    private lateinit var userRepository: IUserRepository
    private lateinit var passwordHasher: IPasswordHasher
    private lateinit var useCase: LoginUserUseCase

    @Before
    fun setup() {
        userRepository = mockk()
        passwordHasher = mockk()
        useCase = LoginUserUseCase(userRepository, passwordHasher)
    }

    @Test
    fun `when user not found, should return failure`() = runTest {
        coEvery { userRepository.findByLogin("unknown") } returns null

        val result = useCase("unknown", "password")

        assertTrue(result.isFailure)
        assertEquals("Неверный логин или пароль", result.exceptionOrNull()?.message)
    }

    @Test
    fun `when passwords do not match, should return failure`() = runTest {
        val user = User(UserId(UUID.randomUUID()), "login", "Name", "hash", UserRole.USER)
        coEvery { userRepository.findByLogin("login") } returns user
        every { passwordHasher.verify("wrong", "hash") } returns false

        val result = useCase("login", "wrong")

        assertTrue(result.isFailure)
        assertEquals("Неверный логин или пароль", result.exceptionOrNull()?.message)
    }

    @Test
    fun `when credentials are valid, should return user`() = runTest {
        val user = User(UserId(UUID.randomUUID()), "login", "Name", "hash", UserRole.USER)
        coEvery { userRepository.findByLogin("login") } returns user
        every { passwordHasher.verify("password", "hash") } returns true

        val result = useCase("login", "password")

        assertTrue(result.isSuccess)
        assertEquals(user, result.getOrNull())
    }
}
