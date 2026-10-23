package com.ppo.domain.usecase

import com.ppo.domain.builders.CategoryBuilder
import com.ppo.domain.builders.TimeRecordBuilder
import com.ppo.domain.builders.UserSettingsBuilder
import com.ppo.domain.fixtures.TestCategoryMother
import com.ppo.domain.fixtures.TestUserMother
import com.ppo.domain.model.DurationUnit
import com.ppo.domain.repository.ICategoryRepository
import com.ppo.domain.repository.ITimeRecordRepository
import com.ppo.domain.repository.IUserSettingsRepository
import com.ppo.domain.runner.RandomOrderRunner
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(RandomOrderRunner::class)
class CreateTimeRecordUseCaseTest {
    private val timeRecordRepository = mockk<ITimeRecordRepository>()
    private val categoryRepository = mockk<ICategoryRepository>()
    private val userSettingsRepository = mockk<IUserSettingsRepository>()
    private lateinit var useCase: CreateTimeRecordUseCase

    @Before
    fun setUp() {
        useCase = CreateTimeRecordUseCase(timeRecordRepository, categoryRepository, userSettingsRepository)
    }

    @Test
    fun shouldCreateActiveRecord_whenEndTimeIsNull() = runBlocking {
        val user = TestUserMother.regularUser()
        val category = TestCategoryMother.userCustomCategory(user.id)
        val startTime = Instant.parse("2026-10-15T10:00:00Z")
        coEvery { categoryRepository.findCategoryById(category.id) } returns category
        coEvery { timeRecordRepository.saveRecord(any()) } returns Result.success(Unit)

        val result = useCase(user.id, category.id, startTime, null, "Active work")

        assertTrue(result.isSuccess)
        assertNull(result.getOrNull()?.endTime)
        coVerify(exactly = 1) { timeRecordRepository.saveRecord(match { it.endTime == null }) }
    }

    @Test
    fun shouldCreateCompletedRecord_whenValidTimeRangeAndAboveMinDuration() = runBlocking {
        val user = TestUserMother.regularUser()
        val category = TestCategoryMother.userCustomCategory(user.id)
        val startTime = Instant.parse("2026-10-15T10:00:00Z")
        val endTime = Instant.parse("2026-10-15T10:30:00Z")
        val settings = UserSettingsBuilder().withUserId(user.id).withMinDuration(true, 5, DurationUnit.minutes).build()
        coEvery { categoryRepository.findCategoryById(category.id) } returns category
        coEvery { userSettingsRepository.getSettings(user.id) } returns settings
        coEvery { timeRecordRepository.saveRecord(any()) } returns Result.success(Unit)

        val result = useCase(user.id, category.id, startTime, endTime, "Completed task")

        assertTrue(result.isSuccess)
        assertEquals(endTime, result.getOrNull()?.endTime)
        coVerify(exactly = 1) { timeRecordRepository.saveRecord(any()) }
    }

    @Test
    fun shouldFailWithNoSuchElement_whenCategoryDoesNotExist() = runBlocking {
        val user = TestUserMother.regularUser()
        val missingCategory = CategoryBuilder().build()
        coEvery { categoryRepository.findCategoryById(missingCategory.id) } returns null

        val result = useCase(user.id, missingCategory.id, Instant.now())

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NoSuchElementException)
    }

    @Test
    fun shouldFailWithIllegalAccess_whenCategoryBelongsToAnotherUser() = runBlocking {
        val user1 = TestUserMother.regularUser()
        val otherUser = TestUserMother.otherUser()
        val foreignCategory = TestCategoryMother.userCustomCategory(otherUser.id)
        coEvery { categoryRepository.findCategoryById(foreignCategory.id) } returns foreignCategory

        val result = useCase(user1.id, foreignCategory.id, Instant.now())

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalAccessException)
    }

    @Test
    fun shouldFailWithIllegalArgument_whenEndTimeBeforeStartTime() = runBlocking {
        val user = TestUserMother.regularUser()
        val category = TestCategoryMother.userCustomCategory(user.id)
        val startTime = Instant.parse("2026-10-15T12:00:00Z")
        val endTime = Instant.parse("2026-10-15T11:59:59Z")
        coEvery { categoryRepository.findCategoryById(category.id) } returns category

        val result = useCase(user.id, category.id, startTime, endTime)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun shouldFailWithIllegalState_whenDurationBelowConfiguredMinThreshold() = runBlocking {
        val user = TestUserMother.regularUser()
        val category = TestCategoryMother.userCustomCategory(user.id)
        val startTime = Instant.parse("2026-10-15T10:00:00Z")
        val endTime = Instant.parse("2026-10-15T10:04:59Z")
        val settings = UserSettingsBuilder().withUserId(user.id).withMinDuration(true, 5, DurationUnit.minutes).build()
        coEvery { categoryRepository.findCategoryById(category.id) } returns category
        coEvery { userSettingsRepository.getSettings(user.id) } returns settings

        val result = useCase(user.id, category.id, startTime, endTime)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun shouldSucceed_whenDurationEqualsMinThresholdExactly() = runBlocking {
        val user = TestUserMother.regularUser()
        val category = TestCategoryMother.userCustomCategory(user.id)
        val startTime = Instant.parse("2026-10-15T10:00:00Z")
        val endTime = Instant.parse("2026-10-15T10:05:00Z")
        val settings = UserSettingsBuilder().withUserId(user.id).withMinDuration(true, 5, DurationUnit.minutes).build()
        coEvery { categoryRepository.findCategoryById(category.id) } returns category
        coEvery { userSettingsRepository.getSettings(user.id) } returns settings
        coEvery { timeRecordRepository.saveRecord(any()) } returns Result.success(Unit)

        val result = useCase(user.id, category.id, startTime, endTime)

        assertTrue(result.isSuccess)
    }
}
