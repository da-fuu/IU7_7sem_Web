package com.ppo.domain.usecase

import com.ppo.domain.fixtures.TestUserMother
import com.ppo.domain.model.CategoryId
import com.ppo.domain.model.UserRole
import com.ppo.domain.repository.ICategoryRepository
import com.ppo.domain.repository.IUserRepository
import com.ppo.domain.runner.RandomOrderRunner
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(RandomOrderRunner::class)
class AdminDeleteBaseCategoryUseCaseTest {
    private val categoryRepository = mockk<ICategoryRepository>()
    private val userRepository = mockk<IUserRepository>()
    private lateinit var useCase: AdminDeleteBaseCategoryUseCase

    @Before
    fun setUp() {
        useCase = AdminDeleteBaseCategoryUseCase(categoryRepository, userRepository)
    }

    @Test
    fun shouldDeleteBaseCategory_whenRequesterIsAdmin() = runBlocking {
        val admin = TestUserMother.adminUser()
        val catId = CategoryId(UUID.randomUUID())
        coEvery { userRepository.getUserRole(admin.id) } returns UserRole.ADMIN
        coEvery { categoryRepository.deleteCategory(catId) } returns Result.success(Unit)

        val result = useCase(catId, admin.id)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { categoryRepository.deleteCategory(catId) }
    }

    @Test
    fun shouldFailWithIllegalState_whenRequesterIsNotAdmin() = runBlocking {
        val user = TestUserMother.regularUser()
        val catId = CategoryId(UUID.randomUUID())
        coEvery { userRepository.getUserRole(user.id) } returns UserRole.USER

        val result = useCase(catId, user.id)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }
}
