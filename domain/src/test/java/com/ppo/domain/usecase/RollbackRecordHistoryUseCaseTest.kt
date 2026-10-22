package com.ppo.domain.usecase

import com.ppo.domain.model.*
import com.ppo.domain.repository.IRecordHistoryRepository
import com.ppo.domain.repository.ITimeRecordRepository
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

class RollbackRecordHistoryUseCaseTest {
    private lateinit var timeRecordRepository: ITimeRecordRepository
    private lateinit var recordHistoryRepository: IRecordHistoryRepository
    private lateinit var useCase: RollbackRecordHistoryUseCase

    @Before
    fun setup() {
        timeRecordRepository = mockk()
        recordHistoryRepository = mockk()
        useCase = RollbackRecordHistoryUseCase(timeRecordRepository, recordHistoryRepository)
    }

    @Test
    fun `should create audit snapshot and rollback all record fields to version values`() = runTest {
        val userId = UserId(UUID.randomUUID())
        val catOld = CategoryId(UUID.randomUUID())
        val catCurrent = CategoryId(UUID.randomUUID())
        val recordId = RecordId(UUID.randomUUID())
        val versionId = VersionId(UUID.randomUUID())

        val currentRecord = TimeRecord(
            id = recordId,
            userId = userId,
            categoryId = catCurrent,
            startTime = Instant.parse("2026-10-15T12:00:00Z"),
            endTime = Instant.parse("2026-10-15T13:00:00Z"),
            description = "Current Description"
        )

        val targetVersion = RecordHistoryEntry(
            id = versionId,
            recordId = recordId,
            categoryId = catOld,
            startTime = Instant.parse("2026-10-15T08:00:00Z"),
            endTime = Instant.parse("2026-10-15T09:00:00Z"),
            description = "Original Description",
            modifiedAt = Instant.parse("2026-10-15T10:00:00Z")
        )

        coEvery { timeRecordRepository.getRecordById(recordId) } returns currentRecord
        coEvery { recordHistoryRepository.getHistoryEntry(versionId) } returns targetVersion
        coEvery { recordHistoryRepository.saveHistoryEntry(any()) } returns Result.success(Unit)
        coEvery { timeRecordRepository.saveRecord(any()) } returns Result.success(Unit)

        val result = useCase(recordId, versionId, userId)
        assertTrue(result.isSuccess)
        val rolledBack = result.getOrNull()
        assertEquals(catOld, rolledBack?.categoryId)
        assertEquals(targetVersion.startTime, rolledBack?.startTime)
        assertEquals(targetVersion.endTime, rolledBack?.endTime)
        assertEquals("Original Description", rolledBack?.description)

        coVerify(exactly = 1) {
            recordHistoryRepository.saveHistoryEntry(match {
                it.recordId == recordId &&
                it.categoryId == catCurrent &&
                it.description == "Current Description"
            })
        }
    }
}
