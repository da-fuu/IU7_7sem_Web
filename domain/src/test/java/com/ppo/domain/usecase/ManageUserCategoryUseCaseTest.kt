package com.ppo.domain.usecase

import com.ppo.domain.builders.CategoryBuilder
import com.ppo.domain.fixtures.TestUserMother
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

@RunWith(RandomOrderRunner::class)
class ManageUserCategoryUseCaseTest {
    private val categoryRepository = mockk<ICategoryRepository>()
    private lateinit var useCase: ManageUserCategoryUseCase

    @Before
    fun setUp() {
        useCase = ManageUserCategoryUseCase(categoryRepository)
    }

    @Test
    fun shouldSaveCategory_whenValidNameAndHexColor() = runBlocking {
        val user = TestUserMother.regularUser()
        val category = CategoryBuilder().withUserId(user.id).withName("Health").withColorHex("#10B981").build()
        coEvery { categoryRepository.findCategoryByName(user.id, "Health") } returns null
        coEvery { categoryRepository.saveCategory(category) } returns Result.success(Unit)

        val result = useCase(category)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { categoryRepository.saveCategory(category) }
    }

    @Test
    fun shouldFailWithIllegalArgument_whenColorHexIsInvalid() = runBlocking {
        val user = TestUserMother.regularUser()
        val category = CategoryBuilder().withUserId(user.id).withColorHex("invalid-hex").build()

        val result = useCase(category)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun shouldFailWithIllegalState_whenUserAttemptsToCreateDefaultCategory() = runBlocking {
        val user = TestUserMother.regularUser()
        val category = CategoryBuilder().withUserId(user.id).asDefault().build()

        val result = useCase(category)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun shouldFailWithIllegalState_whenCategoryNameAlreadyExistsForUser() = runBlocking {
        val user = TestUserMother.regularUser()
        val category = CategoryBuilder().withUserId(user.id).withName("Work").build()
        val existing = CategoryBuilder().withUserId(user.id).withName("Work").build()
        coEvery { categoryRepository.findCategoryByName(user.id, "Work") } returns existing

        val result = useCase(category)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }
}
