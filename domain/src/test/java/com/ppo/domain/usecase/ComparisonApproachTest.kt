package com.ppo.domain.usecase

import com.ppo.domain.builders.CategoryBuilder
import com.ppo.domain.fakes.InMemoryCategoryRepository
import com.ppo.domain.fakes.InMemoryTimeRecordRepository
import com.ppo.domain.fakes.InMemoryUserSettingsRepository
import com.ppo.domain.fixtures.TestCategoryMother
import com.ppo.domain.fixtures.TestUserMother
import com.ppo.domain.model.DurationUnit
import com.ppo.domain.model.UserSettings
import com.ppo.domain.repository.ICategoryRepository
import com.ppo.domain.repository.ITimeRecordRepository
import com.ppo.domain.repository.IUserSettingsRepository
import com.ppo.domain.runner.RandomOrderRunner
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(RandomOrderRunner::class)
class ComparisonApproachTest {

    @Test
    fun createRecord_londonSchool_verifiesCollaboratorInteractions() = runBlocking {
        val user = TestUserMother.regularUser()
        val category = TestCategoryMother.userCustomCategory(user.id)
        val startTime = Instant.parse("2026-10-15T09:00:00Z")
        val endTime = Instant.parse("2026-10-15T09:30:00Z")
        val mockRecordRepo = mockk<ITimeRecordRepository>()
        val mockCategoryRepo = mockk<ICategoryRepository>()
        val mockSettingsRepo = mockk<IUserSettingsRepository>()
        coEvery { mockCategoryRepo.findCategoryById(category.id) } returns category
        coEvery { mockSettingsRepo.getSettings(user.id) } returns UserSettings(user.id, minDurationEnabled = false)
        coEvery { mockRecordRepo.saveRecord(any()) } returns Result.success(Unit)
        val useCase = CreateTimeRecordUseCase(mockRecordRepo, mockCategoryRepo, mockSettingsRepo)

        val result = useCase(user.id, category.id, startTime, endTime, "London test")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            mockRecordRepo.saveRecord(match {
                it.userId == user.id && it.description == "London test" && it.endTime == endTime
            })
        }
    }

    @Test
    fun createRecord_classicalSchool_verifiesFinalStateInFakeStorage() = runBlocking {
        val user = TestUserMother.regularUser()
        val category = CategoryBuilder().withUserId(user.id).build()
        val startTime = Instant.parse("2026-10-15T09:00:00Z")
        val endTime = Instant.parse("2026-10-15T09:30:00Z")
        val fakeRecordRepo = InMemoryTimeRecordRepository()
        val fakeCategoryRepo = InMemoryCategoryRepository()
        val fakeSettingsRepo = InMemoryUserSettingsRepository()
        fakeCategoryRepo.saveCategory(category)
        fakeSettingsRepo.updateSettings(UserSettings(user.id, minDurationEnabled = false))
        val useCase = CreateTimeRecordUseCase(fakeRecordRepo, fakeCategoryRepo, fakeSettingsRepo)

        val result = useCase(user.id, category.id, startTime, endTime, "Detroit test")

        assertTrue(result.isSuccess)
        val saved = fakeRecordRepo.getRecordById(result.getOrNull()!!.id)
        assertNotNull(saved)
        assertEquals("Detroit test", saved?.description)
        assertEquals(endTime, saved?.endTime)
    }
}
