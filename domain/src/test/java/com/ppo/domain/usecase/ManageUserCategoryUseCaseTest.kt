package com.ppo.domain.usecase

import com.ppo.domain.model.Category
import com.ppo.domain.model.CategoryId
import com.ppo.domain.model.UserId
import com.ppo.domain.repository.ICategoryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class ManageUserCategoryUseCaseTest {

    private lateinit var repository: ICategoryRepository
    private lateinit var useCase: ManageUserCategoryUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = ManageUserCategoryUseCase(repository)
    }

    @Test
    fun `when name is blank, should return failure`() = runTest {
        val category = Category(CategoryId(UUID.randomUUID()), UserId(UUID.randomUUID()), "   ", "#FFF", false)
        val result = useCase(category)
        assertTrue(result.isFailure)
        assertEquals("Имя категории не может быть пустым", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { repository.saveCategory(any()) }
    }

    @Test
    fun `when color format is invalid, should return failure`() = runTest {
        val category = Category(CategoryId(UUID.randomUUID()), UserId(UUID.randomUUID()), "Work", "invalid", false)
        val result = useCase(category)
        assertTrue(result.isFailure)
        assertEquals("Неверный формат цвета. Используйте #RRGGBB или #RGB", result.exceptionOrNull()?.message)
    }

    @Test
    fun `when color format is valid short, should return success`() = runTest {
        val userId = UserId(UUID.randomUUID())
        val category = Category(CategoryId(UUID.randomUUID()), userId, "Work", "#FFF", false)
        coEvery { repository.findCategoryByName(userId, "Work") } returns null
        coEvery { repository.saveCategory(any()) } returns Result.success(Unit)
        val result = useCase(category)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `when color format is valid long, should return success`() = runTest {
        val userId = UserId(UUID.randomUUID())
        val category = Category(CategoryId(UUID.randomUUID()), userId, "Work", "#FF0000", false)
        coEvery { repository.findCategoryByName(userId, "Work") } returns null
        coEvery { repository.saveCategory(any()) } returns Result.success(Unit)
        val result = useCase(category)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `when isDefault is true, should return failure`() = runTest {
        val category = Category(CategoryId(UUID.randomUUID()), UserId(UUID.randomUUID()), "Work", "#FFF", true)
        val result = useCase(category)
        assertTrue(result.isFailure)
        assertEquals("Пользователь не может создавать системные категории", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { repository.saveCategory(any()) }
    }

    @Test
    fun `when name already exists, should return failure`() = runTest {
        val userId = UserId(UUID.randomUUID())
        val category = Category(CategoryId(UUID.randomUUID()), userId, "Work", "#FFF", false)
        val existing = Category(CategoryId(UUID.randomUUID()), userId, "Work", "#000", false)

        coEvery { repository.findCategoryByName(userId, "Work") } returns existing

        val result = useCase(category)
        assertTrue(result.isFailure)
        assertEquals("Категория с таким именем уже существует", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { repository.saveCategory(any()) }
    }

    @Test
    fun `when validation passes, should save category and return success`() = runTest {
        val userId = UserId(UUID.randomUUID())
        val category = Category(CategoryId(UUID.randomUUID()), userId, "Work", "#FFF", false)

        coEvery { repository.findCategoryByName(userId, "Work") } returns null
        coEvery { repository.saveCategory(category) } returns Result.success(Unit)

        val result = useCase(category)
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.saveCategory(category) }
    }
}
