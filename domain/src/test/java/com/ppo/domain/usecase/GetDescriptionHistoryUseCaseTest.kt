package com.ppo.domain.usecase

import com.ppo.domain.builders.TimeRecordBuilder
import com.ppo.domain.model.RecordHistoryEntry
import com.ppo.domain.model.RecordId
import com.ppo.domain.model.VersionId
import com.ppo.domain.repository.ITimeRecordRepository
import com.ppo.domain.runner.RandomOrderRunner
import io.mockk.coEvery
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
class GetDescriptionHistoryUseCaseTest {
    private val timeRecordRepository = mockk<ITimeRecordRepository>()
    private lateinit var useCase: GetDescriptionHistoryUseCase

    @Before
    fun setUp() {
        useCase = GetDescriptionHistoryUseCase(timeRecordRepository)
    }

    @Test
    fun shouldReturnHistoryList_whenRecordExists() = runBlocking {
        val record = TimeRecordBuilder().build()
        val history = listOf(
            RecordHistoryEntry(VersionId(UUID.randomUUID()), record.id, record.categoryId, record.startTime, record.endTime, "v1", Instant.now())
        )
        coEvery { timeRecordRepository.getRecordById(record.id) } returns record
        coEvery { timeRecordRepository.getDescriptionHistory(record.id) } returns history

        val result = useCase(record.id)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
    }

    @Test
    fun shouldFailWithNoSuchElement_whenRecordNotFound() = runBlocking {
        val missingId = RecordId(UUID.randomUUID())
        coEvery { timeRecordRepository.getRecordById(missingId) } returns null

        val result = useCase(missingId)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NoSuchElementException)
    }
}
