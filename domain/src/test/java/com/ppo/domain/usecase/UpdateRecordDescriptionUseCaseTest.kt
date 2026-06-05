package com.ppo.domain.usecase

import com.ppo.domain.model.*
import com.ppo.domain.repository.ITimeRecordRepository
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.UUID

class UpdateRecordDescriptionUseCaseTest {
    private val repo = mockk<ITimeRecordRepository>()
    private val useCase = UpdateRecordDescriptionUseCase(repo)

    @Test
    fun `should save old description to history and update record`() = runBlocking {
        val recordId = RecordId(UUID.randomUUID())
        val oldRecord = TimeRecord(
            id = recordId,
            userId = UserId(UUID.randomUUID()),
            categoryId = CategoryId(UUID.randomUUID()),
            startTime = Instant.now(),
            description = "Old"
        )
        val newDesc = "New"

        coEvery { repo.getRecordById(recordId) } returns oldRecord
        coEvery { repo.addDescriptionVersion(any()) } returns Result.success(Unit)
        coEvery { repo.saveRecord(any()) } returns Result.success(Unit)

        val result = useCase(recordId, newDesc)

        assertTrue(result.isSuccess)
        coVerify {
            repo.addDescriptionVersion(match { it.description == "Old" && it.recordId == recordId })
            repo.saveRecord(match { it.description == "New" && it.id == recordId })
        }
    }

    @Test
    fun `should return success if description is same`() = runBlocking {
        val recordId = RecordId(UUID.randomUUID())
        val record = TimeRecord(
            id = recordId,
            userId = UserId(UUID.randomUUID()),
            categoryId = CategoryId(UUID.randomUUID()),
            startTime = Instant.now(),
            description = "Same"
        )

        coEvery { repo.getRecordById(recordId) } returns record

        val result = useCase(recordId, "Same")

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { repo.addDescriptionVersion(any()) }
        coVerify(exactly = 0) { repo.saveRecord(any()) }
    }
}
