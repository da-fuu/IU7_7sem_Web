package com.ppo.domain.usecase

import com.ppo.domain.model.Category
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

class AdminManageBaseCategoryUseCaseTest {

    private lateinit var categoryRepository: ICategoryRepository
    private lateinit var userRepository: IUserRepository
    private lateinit var useCase: AdminManageBaseCategoryUseCase

    @Before
    fun setup() {
        categoryRepository = mockk()
        userRepository = mockk()
        useCase = AdminManageBaseCategoryUseCase(categoryRepository, userRepository)
    }

    @Test
    fun `when user is not admin, should return failure`() = runTest {
        val adminId = UserId(UUID.randomUUID())
        val category = Category(CategoryId(UUID.randomUUID()), null, "Work", "#FFF", true)

        coEvery { userRepository.getUserRole(adminId) } returns UserRole.USER

        val result = useCase(category, adminId)
        assertTrue(result.isFailure)
        assertEquals("Нет прав администратора", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { categoryRepository.saveCategory(any()) }
    }

    @Test
    fun `when category has userId, should return failure`() = runTest {
        val adminId = UserId(UUID.randomUUID())
        val category = Category(CategoryId(UUID.randomUUID()), UserId(UUID.randomUUID()), "Work", "#FFF", true)

        coEvery { userRepository.getUserRole(adminId) } returns UserRole.ADMIN

        val result = useCase(category, adminId)
        assertTrue(result.isFailure)
        assertEquals("Базовая категория должна быть isDefault=true и без привязки к пользователю", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { categoryRepository.saveCategory(any()) }
    }

    @Test
    fun `when category is not default, should return failure`() = runTest {
        val adminId = UserId(UUID.randomUUID())
        val category = Category(CategoryId(UUID.randomUUID()), null, "Work", "#FFF", false)

        coEvery { userRepository.getUserRole(adminId) } returns UserRole.ADMIN

        val result = useCase(category, adminId)
        assertTrue(result.isFailure)
        assertEquals("Базовая категория должна быть isDefault=true и без привязки к пользователю", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { categoryRepository.saveCategory(any()) }
    }

    @Test
    fun `when validation passes, should save category and return success`() = runTest {
        val adminId = UserId(UUID.randomUUID())
        val category = Category(CategoryId(UUID.randomUUID()), null, "Work", "#FFF", true)

        coEvery { userRepository.getUserRole(adminId) } returns UserRole.ADMIN
        coEvery { categoryRepository.findCategoryByName(null, "Work") } returns null
        coEvery { categoryRepository.saveCategory(category) } returns Result.success(Unit)

        val result = useCase(category, adminId)
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { categoryRepository.saveCategory(category) }
    }
}
