package com.ppo.domain.usecase

import com.ppo.domain.model.CategoryId
import com.ppo.domain.repository.ICategoryRepository
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
class DeleteCategoryUseCaseTest {
    private val categoryRepository = mockk<ICategoryRepository>()
    private lateinit var useCase: DeleteCategoryUseCase

    @Before
    fun setUp() {
        useCase = DeleteCategoryUseCase(categoryRepository)
    }

    @Test
    fun shouldDeleteCategory_whenCategoryExists() = runBlocking {
        val catId = CategoryId(UUID.randomUUID())
        coEvery { categoryRepository.deleteCategory(catId) } returns Result.success(Unit)

        val result = useCase(catId)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { categoryRepository.deleteCategory(catId) }
    }

    @Test
    fun shouldPropagateFailure_whenRepositoryFailsToDelete() = runBlocking {
        val catId = CategoryId(UUID.randomUUID())
        coEvery { categoryRepository.deleteCategory(catId) } returns Result.failure(IllegalStateException("DB Error"))

        val result = useCase(catId)

        assertTrue(result.isFailure)
    }
}
