package com.ppo.domain.usecase

import com.ppo.domain.fixtures.TestCategoryMother
import com.ppo.domain.fixtures.TestUserMother
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
    fun shouldReturnCategoriesList_whenInvoked() = runBlocking {
        val user = TestUserMother.regularUser()
        val list = listOf(TestCategoryMother.baseWorkCategory(), TestCategoryMother.userCustomCategory(user.id))
        every { categoryRepository.getCategoriesFlow(user.id) } returns flowOf(list)

        val result = useCase(user.id)

        assertEquals(2, result.size)
    }

    @Test
    fun shouldReturnEmptyList_whenUserHasNoCategories() = runBlocking {
        val user = TestUserMother.regularUser()
        every { categoryRepository.getCategoriesFlow(user.id) } returns flowOf(emptyList())

        val result = useCase(user.id)

        assertEquals(0, result.size)
    }
}
