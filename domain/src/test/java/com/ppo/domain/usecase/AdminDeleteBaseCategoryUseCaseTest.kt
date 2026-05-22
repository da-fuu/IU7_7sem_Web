package com.ppo.domain.usecase

import com.ppo.domain.model.CategoryId
import com.ppo.domain.model.UserId
import com.ppo.domain.model.UserRole
import com.ppo.domain.repository.ICategoryRepository
import com.ppo.domain.repository.IUserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class AdminDeleteBaseCategoryUseCaseTest {

    private lateinit var categoryRepository: ICategoryRepository
    private lateinit var userRepository: IUserRepository
    private lateinit var useCase: AdminDeleteBaseCategoryUseCase

    @Before
    fun setup() {
        categoryRepository = mockk()
        userRepository = mockk()
        useCase = AdminDeleteBaseCategoryUseCase(categoryRepository, userRepository)
    }

    @Test
    fun `when not admin, should return failure`() = runTest {
        val adminId = UserId(UUID.randomUUID())
        val categoryId = CategoryId(UUID.randomUUID())

        coEvery { userRepository.getUserRole(adminId) } returns UserRole.USER

        val result = useCase(categoryId, adminId)

        assertTrue(result.isFailure)
        assertEquals("Нет прав администратора", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { categoryRepository.deleteCategory(any()) }
    }

    @Test
    fun `when admin, should delete category`() = runTest {
        val adminId = UserId(UUID.randomUUID())
        val categoryId = CategoryId(UUID.randomUUID())

        coEvery { userRepository.getUserRole(adminId) } returns UserRole.ADMIN
        coEvery { categoryRepository.deleteCategory(categoryId) } returns Result.success(Unit)

        val result = useCase(categoryId, adminId)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { categoryRepository.deleteCategory(categoryId) }
    }
}
