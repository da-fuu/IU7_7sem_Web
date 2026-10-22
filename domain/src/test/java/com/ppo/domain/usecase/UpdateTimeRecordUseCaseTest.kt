package com.ppo.domain.usecase

import com.ppo.domain.model.*
import com.ppo.domain.repository.ICategoryRepository
import com.ppo.domain.repository.IRecordHistoryRepository
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

class UpdateTimeRecordUseCaseTest {
    private lateinit var timeRecordRepository: ITimeRecordRepository
    private lateinit var categoryRepository: ICategoryRepository
    private lateinit var userSettingsRepository: IUserSettingsRepository
    private lateinit var recordHistoryRepository: IRecordHistoryRepository
    private lateinit var useCase: UpdateTimeRecordUseCase

    @Before
    fun setup() {
        timeRecordRepository = mockk()
        categoryRepository = mockk()
        userSettingsRepository = mockk()
        recordHistoryRepository = mockk()
        useCase = UpdateTimeRecordUseCase(
            timeRecordRepository,
            categoryRepository,
            userSettingsRepository,
            recordHistoryRepository
        )
    }

    @Test
    fun `should create history snapshot with all previous fields and update record`() = runTest {
        val userId = UserId(UUID.randomUUID())
        val catId1 = CategoryId(UUID.randomUUID())
        val catId2 = CategoryId(UUID.randomUUID())
        val recordId = RecordId(UUID.randomUUID())
        val startOld = Instant.parse("2026-10-15T10:00:00Z")
        val endOld = Instant.parse("2026-10-15T11:00:00Z")

        val existing = TimeRecord(recordId, userId, catId1, startOld, endOld, "Old Desc")
        val cat2 = Category(catId2, userId, "New Cat", "#10B981", false)

        coEvery { timeRecordRepository.getRecordById(recordId) } returns existing
        coEvery { categoryRepository.findCategoryById(catId2) } returns cat2
        coEvery { userSettingsRepository.getSettings(userId) } returns UserSettings(userId, minDurationEnabled = false)
        coEvery { recordHistoryRepository.saveHistoryEntry(any()) } returns Result.success(Unit)
        coEvery { timeRecordRepository.saveRecord(any()) } returns Result.success(Unit)

        val newStart = Instant.parse("2026-10-15T09:00:00Z")
        val newEnd = Instant.parse("2026-10-15T10:30:00Z")
        val result = useCase(
            recordId = recordId,
            requestUserId = userId,
            categoryId = catId2,
            startTime = newStart,
            endTime = newEnd,
            description = "New Desc"
        )

        assertTrue(result.isSuccess)
        val updated = result.getOrNull()
        assertEquals(catId2, updated?.categoryId)
        assertEquals(newStart, updated?.startTime)
        assertEquals(newEnd, updated?.endTime)
        assertEquals("New Desc", updated?.description)

        coVerify(exactly = 1) {
            recordHistoryRepository.saveHistoryEntry(match {
                it.recordId == recordId &&
                it.categoryId == catId1 &&
                it.startTime == startOld &&
                it.endTime == endOld &&
                it.description == "Old Desc"
            })
        }
    }
}
