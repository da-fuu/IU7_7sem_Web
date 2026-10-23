package com.ppo.domain.usecase

import com.ppo.domain.builders.TimeRecordBuilder
import com.ppo.domain.model.RecordId
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
import java.util.UUID

@RunWith(RandomOrderRunner::class)
class UpdateRecordDescriptionUseCaseTest {
    private val timeRecordRepository = mockk<ITimeRecordRepository>()
    private lateinit var useCase: UpdateRecordDescriptionUseCase

    @Before
    fun setUp() {
        useCase = UpdateRecordDescriptionUseCase(timeRecordRepository)
    }

    @Test
    fun shouldUpdateDescriptionAndSaveVersion_whenDescriptionChanged() = runBlocking {
        val original = TimeRecordBuilder().withDescription("Initial description").build()
        coEvery { timeRecordRepository.getRecordById(original.id) } returns original
        coEvery { timeRecordRepository.addDescriptionVersion(any()) } returns Result.success(Unit)
        coEvery { timeRecordRepository.saveRecord(any()) } returns Result.success(Unit)

        val result = useCase(original.id, "Updated description")

        assertTrue(result.isSuccess)
        assertEquals("Updated description", result.getOrNull()?.description)
        coVerify(exactly = 1) { timeRecordRepository.addDescriptionVersion(any()) }
        coVerify(exactly = 1) { timeRecordRepository.saveRecord(match { it.description == "Updated description" }) }
    }

    @Test
    fun shouldReturnUnchangedRecord_whenDescriptionIsIdentical() = runBlocking {
        val original = TimeRecordBuilder().withDescription("Same description").build()
        coEvery { timeRecordRepository.getRecordById(original.id) } returns original

        val result = useCase(original.id, "Same description")

        assertTrue(result.isSuccess)
        assertEquals(original, result.getOrNull())
        coVerify(exactly = 0) { timeRecordRepository.saveRecord(any()) }
    }

    @Test
    fun shouldFailWithNoSuchElement_whenRecordNotFound() = runBlocking {
        val missingId = RecordId(UUID.randomUUID())
        coEvery { timeRecordRepository.getRecordById(missingId) } returns null

        val result = useCase(missingId, "New text")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NoSuchElementException)
    }
}
