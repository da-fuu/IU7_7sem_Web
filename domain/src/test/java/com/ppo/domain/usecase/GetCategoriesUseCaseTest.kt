package com.ppo.domain.usecase

import com.ppo.domain.model.Category
import com.ppo.domain.model.CategoryId
import com.ppo.domain.model.UserId
import com.ppo.domain.repository.ICategoryRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.UUID

class GetCategoriesUseCaseTest {

    private lateinit var repository: ICategoryRepository
    private lateinit var useCase: GetCategoriesUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = GetCategoriesUseCase(repository)
    }

    @Test
    fun `should return categories from repository`() = runTest {
        val userId = UserId(UUID.randomUUID())
        val categories = listOf(
            Category(CategoryId(UUID.randomUUID()), userId, "Work", "#FF0000", false),
            Category(CategoryId(UUID.randomUUID()), null, "Study", "#00FF00", true)
        )

        coEvery { repository.getCategoriesFlow(userId) } returns flowOf(categories)

        val result = useCase(userId)

        assertEquals(categories.size, result.size)
        assertEquals(categories[0].id, result[0].id)
        assertEquals(categories[1].name, result[1].name)
    }
}
