package com.ppo.domain.usecase

import com.ppo.domain.model.CategoryId
import com.ppo.domain.repository.ICategoryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class DeleteCategoryUseCaseTest {

    private lateinit var repository: ICategoryRepository
    private lateinit var useCase: DeleteCategoryUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = DeleteCategoryUseCase(repository)
    }

    @Test
    fun `should delete category via repository`() = runTest {
        val categoryId = CategoryId(UUID.randomUUID())
        coEvery { repository.deleteCategory(categoryId) } returns Result.success(Unit)

        val result = useCase(categoryId)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.deleteCategory(categoryId) }
    }
}
