package com.ppo.domain.usecase

import com.ppo.domain.builders.TimeRecordBuilder
import com.ppo.domain.fixtures.TestUserMother
import com.ppo.domain.model.RecordHistoryEntry
import com.ppo.domain.model.RecordId
import com.ppo.domain.model.VersionId
import com.ppo.domain.repository.IRecordHistoryRepository
import com.ppo.domain.repository.ITimeRecordRepository
import com.ppo.domain.runner.RandomOrderRunner
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.util.UUID

@RunWith(RandomOrderRunner::class)
class RollbackRecordHistoryUseCaseTest {
    private val timeRecordRepository = mockk<ITimeRecordRepository>()
    private val recordHistoryRepository = mockk<IRecordHistoryRepository>()
    private lateinit var useCase: RollbackRecordHistoryUseCase

    @Before
    fun setUp() {
        useCase = RollbackRecordHistoryUseCase(timeRecordRepository, recordHistoryRepository)
    }

    @Test
    fun shouldRollbackToSnapshotAndCreateNewAuditEntry_whenOwnerRequestsRollback() = runBlocking {
        val user = TestUserMother.regularUser()
        val record = TimeRecordBuilder().withUserId(user.id).withDescription("v2-current").build()
        val snapshot = RecordHistoryEntry(
            id = VersionId(UUID.randomUUID()),
            recordId = record.id,
            categoryId = record.categoryId,
            startTime = record.startTime,
            endTime = record.endTime,
            description = "v1-original",
            modifiedAt = Instant.now()
        )
        coEvery { timeRecordRepository.getRecordById(record.id) } returns record
        coEvery { recordHistoryRepository.getHistoryEntry(snapshot.id) } returns snapshot
        coEvery { recordHistoryRepository.saveHistoryEntry(any()) } returns Result.success(Unit)
        coEvery { timeRecordRepository.saveRecord(any()) } returns Result.success(Unit)

        val result = useCase(record.id, snapshot.id, user.id, false)

        assertTrue(result.isSuccess)
        assertEquals("v1-original", result.getOrNull()?.description)
        coVerify(exactly = 1) {
            recordHistoryRepository.saveHistoryEntry(match { it.description == "v2-current" })
        }
        coVerify(exactly = 1) {
            timeRecordRepository.saveRecord(match { it.description == "v1-original" })
        }
    }

    @Test
    fun shouldFailWithIllegalArgument_whenVersionBelongsToAnotherRecord() = runBlocking {
        val user = TestUserMother.regularUser()
        val record = TimeRecordBuilder().withUserId(user.id).build()
        val otherRecordId = RecordId(UUID.randomUUID())
        val foreignSnapshot = RecordHistoryEntry(
            id = VersionId(UUID.randomUUID()),
            recordId = otherRecordId,
            categoryId = record.categoryId,
            startTime = record.startTime,
            endTime = record.endTime,
            description = "foreign",
            modifiedAt = Instant.now()
        )
        coEvery { timeRecordRepository.getRecordById(record.id) } returns record
        coEvery { recordHistoryRepository.getHistoryEntry(foreignSnapshot.id) } returns foreignSnapshot

        val result = useCase(record.id, foreignSnapshot.id, user.id, false)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun shouldFailWithNoSuchElement_whenVersionNotFound() = runBlocking {
        val user = TestUserMother.regularUser()
        val record = TimeRecordBuilder().withUserId(user.id).build()
        val missingVersionId = VersionId(UUID.randomUUID())
        coEvery { timeRecordRepository.getRecordById(record.id) } returns record
        coEvery { recordHistoryRepository.getHistoryEntry(missingVersionId) } returns null

        val result = useCase(record.id, missingVersionId, user.id, false)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NoSuchElementException)
    }
}
