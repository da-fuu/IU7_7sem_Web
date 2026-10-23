package com.ppo.domain.usecase

import com.ppo.domain.builders.UserBuilder
import com.ppo.domain.model.UserId
import com.ppo.domain.repository.IUserRepository
import com.ppo.domain.runner.RandomOrderRunner
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(RandomOrderRunner::class)
class GetUserInfoUseCaseTest {
    private val userRepository = mockk<IUserRepository>()
    private lateinit var useCase: GetUserInfoUseCase

    @Before
    fun setUp() {
        useCase = GetUserInfoUseCase(userRepository)
    }

    @Test
    fun shouldReturnUserInfo_whenUserExists() = runBlocking {
        val user = UserBuilder().build()
        coEvery { userRepository.getUserById(user.id) } returns user

        val result = useCase(user.id)

        assertEquals(user, result)
    }

    @Test
    fun shouldReturnNull_whenUserNotFound() = runBlocking {
        val missingId = UserId(UUID.randomUUID())
        coEvery { userRepository.getUserById(missingId) } returns null

        val result = useCase(missingId)

        assertNull(result)
    }
}
