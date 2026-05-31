package com.ppo.domain.usecase

import com.ppo.domain.model.User
import com.ppo.domain.model.UserId
import com.ppo.domain.model.UserRole
import com.ppo.domain.repository.IUserRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.UUID

class GetUserInfoUseCaseTest {

    private val userRepository = mockk<IUserRepository>()
    private val useCase = GetUserInfoUseCase(userRepository)

    @Test
    fun `should return user when found`() = runBlocking {
        val userId = UserId(UUID.randomUUID())
        val user = User(userId, "test", "Test User", "hash", UserRole.USER)
        coEvery { userRepository.getUserById(userId) } returns user

        val result = useCase(userId)

        assertEquals(user, result)
    }

    @Test
    fun `should return null when user not found`() = runBlocking {
        val userId = UserId(UUID.randomUUID())
        coEvery { userRepository.getUserById(userId) } returns null

        val result = useCase(userId)

        assertNull(result)
    }
}
