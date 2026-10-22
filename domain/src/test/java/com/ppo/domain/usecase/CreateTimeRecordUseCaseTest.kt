package com.ppo.domain.usecase

import com.ppo.domain.model.*
import com.ppo.domain.repository.ICategoryRepository
import com.ppo.domain.repository.ITimeRecordRepository
import com.ppo.domain.repository.IUserSettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.UUID

class CreateTimeRecordUseCaseTest {
    private lateinit var timeRecordRepository: ITimeRecordRepository
    private lateinit var categoryRepository: ICategoryRepository
    private lateinit var userSettingsRepository: IUserSettingsRepository
    private lateinit var useCase: CreateTimeRecordUseCase

    @Before
    fun setup() {
        timeRecordRepository = mockk()
        categoryRepository = mockk()
        userSettingsRepository = mockk()
        useCase = CreateTimeRecordUseCase(timeRecordRepository, categoryRepository, userSettingsRepository)
    }

    @Test
    fun `when category not found should return failure`() = runTest {
        val userId = UserId(UUID.randomUUID())
        val catId = CategoryId(UUID.randomUUID())

        coEvery { categoryRepository.findCategoryById(catId) } returns null

        val result = useCase(userId, catId)
        assertTrue(result.isFailure)
        coVerify(exactly = 0) { timeRecordRepository.saveRecord(any()) }
    }

    @Test
    fun `when active task created should save record with null endTime`() = runTest {
        val userId = UserId(UUID.randomUUID())
        val catId = CategoryId(UUID.randomUUID())
        val category = Category(catId, userId, "Test", "#3B82F6", false)

        coEvery { categoryRepository.findCategoryById(catId) } returns category
        coEvery { timeRecordRepository.saveRecord(any()) } returns Result.success(Unit)

        val result = useCase(userId, catId, description = "Task")
        assertTrue(result.isSuccess)
        val record = result.getOrNull()
        assertEquals(null, record?.endTime)
        assertEquals("Task", record?.description)
        coVerify(exactly = 1) { timeRecordRepository.saveRecord(any()) }
    }

    @Test
    fun `when duration less than configured threshold should fail with IllegalStateException`() = runTest {
        val userId = UserId(UUID.randomUUID())
        val catId = CategoryId(UUID.randomUUID())
        val category = Category(catId, null, "Default", "#3B82F6", true)

        val start = Instant.parse("2026-10-15T10:00:00Z")
        val end = Instant.parse("2026-10-15T10:00:30Z")

        coEvery { categoryRepository.findCategoryById(catId) } returns category
        coEvery { userSettingsRepository.getSettings(userId) } returns UserSettings(
            userId = userId,
            minDurationEnabled = true,
            minDurationValue = 2,
            minDurationUnit = DurationUnit.minutes
        )

        val result = useCase(userId, catId, startTime = start, endTime = end)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        coVerify(exactly = 0) { timeRecordRepository.saveRecord(any()) }
    }
}
