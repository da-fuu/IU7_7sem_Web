package com.ppo.domain.usecase

import com.ppo.domain.builders.CategoryBuilder
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(RandomOrderRunner::class)
class AdminManageBaseCategoryUseCaseTest {
    private val categoryRepository = mockk<ICategoryRepository>()
    private val userRepository = mockk<IUserRepository>()
    private lateinit var useCase: AdminManageBaseCategoryUseCase

    @Before
    fun setUp() {
        useCase = AdminManageBaseCategoryUseCase(categoryRepository, userRepository)
    }

    @Test
    fun shouldSaveBaseCategory_whenAdminProvidesValidData() = runBlocking {
        val admin = TestUserMother.adminUser()
        val baseCategory = CategoryBuilder().asDefault().withName("Global Work").withColorHex("#3B82F6").build()
        coEvery { userRepository.getUserRole(admin.id) } returns UserRole.ADMIN
        coEvery { categoryRepository.findCategoryByName(null, "Global Work") } returns null
        coEvery { categoryRepository.saveCategory(baseCategory) } returns Result.success(Unit)

        val result = useCase(baseCategory, admin.id)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { categoryRepository.saveCategory(baseCategory) }
    }

    @Test
    fun shouldFailWithIllegalState_whenRequesterIsNotAdmin() = runBlocking {
        val regularUser = TestUserMother.regularUser()
        val baseCategory = CategoryBuilder().asDefault().build()
        coEvery { userRepository.getUserRole(regularUser.id) } returns UserRole.USER

        val result = useCase(baseCategory, regularUser.id)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun shouldFailWithIllegalState_whenBaseCategoryHasUserIdAttached() = runBlocking {
        val admin = TestUserMother.adminUser()
        val invalidBaseCategory = CategoryBuilder().asDefault().withUserId(admin.id).build()
        coEvery { userRepository.getUserRole(admin.id) } returns UserRole.ADMIN

        val result = useCase(invalidBaseCategory, admin.id)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun shouldFailWithIllegalState_whenDuplicateBaseCategoryNameExists() = runBlocking {
        val admin = TestUserMother.adminUser()
        val newCategory = CategoryBuilder().asDefault().withName("Спорт").build()
        val existingCategory = CategoryBuilder().asDefault().withName("Спорт").build()
        coEvery { userRepository.getUserRole(admin.id) } returns UserRole.ADMIN
        coEvery { categoryRepository.findCategoryByName(null, "Спорт") } returns existingCategory

        val result = useCase(newCategory, admin.id)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertEquals("Категория с таким именем уже существует", result.exceptionOrNull()?.message)
    }
}
