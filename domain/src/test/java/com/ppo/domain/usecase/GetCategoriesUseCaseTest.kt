package com.ppo.domain.usecase

import com.ppo.domain.fixtures.FixtureContext
import com.ppo.domain.fixtures.TestCategoryMother
import com.ppo.domain.fixtures.WithRegularUser
import com.ppo.domain.repository.ICategoryRepository
import com.ppo.domain.runner.RandomOrderRunner
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(RandomOrderRunner::class)
class GetCategoriesUseCaseTest {
    private val categoryRepository = mockk<ICategoryRepository>()
    private lateinit var useCase: GetCategoriesUseCase

    @Before
    fun setUp() {
        useCase = GetCategoriesUseCase(categoryRepository)
    }

    @Test
    @WithRegularUser
    fun shouldReturnCategoriesList_whenInvoked() = runBlocking {
        val user = FixtureContext.user
        val list = listOf(TestCategoryMother.baseWorkCategory(), TestCategoryMother.userCustomCategory(user.id))
        every { categoryRepository.getCategoriesFlow(user.id) } returns flowOf(list)

        val result = useCase(user.id)

        assertEquals(2, result.size)
    }

    @Test
    @WithRegularUser
    fun shouldReturnEmptyList_whenUserHasNoCategories() = runBlocking {
        val user = FixtureContext.user
        every { categoryRepository.getCategoriesFlow(user.id) } returns flowOf(emptyList())

        val result = useCase(user.id)

        assertEquals(0, result.size)
    }
}
