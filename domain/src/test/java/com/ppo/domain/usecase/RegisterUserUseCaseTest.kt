package com.ppo.domain.usecase

import com.ppo.domain.model.User
import com.ppo.domain.model.UserId
import com.ppo.domain.model.UserRole
import com.ppo.domain.repository.IPasswordHasher
import com.ppo.domain.repository.IUserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class RegisterUserUseCaseTest {

    private lateinit var userRepository: IUserRepository
    private lateinit var passwordHasher: IPasswordHasher
    private lateinit var useCase: RegisterUserUseCase

    @Before
    fun setup() {
        userRepository = mockk()
        passwordHasher = mockk()
        useCase = RegisterUserUseCase(userRepository, passwordHasher)
    }

    @Test
    fun `when password is too short, should return failure`() = runTest {
        val result = useCase("login", "name", "12345")

        assertTrue(result.isFailure)
        assertEquals("Слишком короткий пароль", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { userRepository.saveUser(any()) }
    }

    @Test
    fun `when login already exists, should return failure`() = runTest {
        val login = "existing"
        val existingUser = User(UserId(UUID.randomUUID()), login, "Name", "hash", UserRole.USER)
        
        coEvery { userRepository.findByLogin(login) } returns existingUser

        val result = useCase(login, "Name", "password123")

        assertTrue(result.isFailure)
        assertEquals("Пользователь с таким логином уже существует", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { userRepository.saveUser(any()) }
    }

    @Test
    fun `when validation passes, should hash password and save user`() = runTest {
        val login = "newuser"
        val password = "password123"
        val hashedPassword = "hashed_password"

        coEvery { userRepository.findByLogin(login) } returns null
        every { passwordHasher.hash(password) } returns hashedPassword
        coEvery { userRepository.saveUser(any()) } returns Result.success(Unit)

        val result = useCase(login, "Name", password)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { passwordHasher.hash(password) }
        coVerify(exactly = 1) { 
            userRepository.saveUser(match { 
                it.login == login && it.passwordHash == hashedPassword 
            }) 
        }
    }
}
