package com.ppo.domain.usecase

import com.ppo.domain.builders.TimeRecordBuilder
import com.ppo.domain.model.RecordHistoryEntry
import com.ppo.domain.model.RecordId
import com.ppo.domain.model.VersionId
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
class RollbackRecordDescriptionUseCaseTest {
    private val timeRecordRepository = mockk<ITimeRecordRepository>()
    private lateinit var useCase: RollbackRecordDescriptionUseCase

    @Before
    fun setUp() {
        useCase = RollbackRecordDescriptionUseCase(timeRecordRepository)
    }

    @Test
    fun shouldRollbackDescription_whenVersionExists() = runBlocking {
        val record = TimeRecordBuilder().withDescription("v2").build()
        val versionId = VersionId(UUID.randomUUID())
        val historyEntry = RecordHistoryEntry(
            id = versionId,
            recordId = record.id,
            categoryId = record.categoryId,
            startTime = record.startTime,
            endTime = record.endTime,
            description = "v1",
            modifiedAt = Instant.now()
        )
        coEvery { timeRecordRepository.getRecordById(record.id) } returns record
        coEvery { timeRecordRepository.getDescriptionHistory(record.id) } returns listOf(historyEntry)
        coEvery { timeRecordRepository.addDescriptionVersion(any()) } returns Result.success(Unit)
        coEvery { timeRecordRepository.saveRecord(any()) } returns Result.success(Unit)

        val result = useCase(record.id, versionId)

        assertTrue(result.isSuccess)
        assertEquals("v1", result.getOrNull()?.description)
        coVerify(exactly = 1) { timeRecordRepository.addDescriptionVersion(match { it.description == "v2" }) }
        coVerify(exactly = 1) { timeRecordRepository.saveRecord(match { it.description == "v1" }) }
    }

    @Test
    fun shouldFailWithNoSuchElement_whenRecordNotFound() = runBlocking {
        val missingId = RecordId(UUID.randomUUID())
        val versionId = VersionId(UUID.randomUUID())
        coEvery { timeRecordRepository.getRecordById(missingId) } returns null

        val result = useCase(missingId, versionId)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NoSuchElementException)
    }

    @Test
    fun shouldFailWithNoSuchElement_whenVersionNotFound() = runBlocking {
        val record = TimeRecordBuilder().build()
        val missingVersionId = VersionId(UUID.randomUUID())
        coEvery { timeRecordRepository.getRecordById(record.id) } returns record
        coEvery { timeRecordRepository.getDescriptionHistory(record.id) } returns emptyList()

        val result = useCase(record.id, missingVersionId)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NoSuchElementException)
    }
}
