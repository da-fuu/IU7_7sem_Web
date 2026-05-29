package com.ppo.domain.usecase

import com.ppo.domain.model.*
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

class EditTimeRecordUseCaseTest {

    private lateinit var repository: ITimeRecordRepository
    private lateinit var useCase: EditTimeRecordUseCase
    private val config = BusinessConfig(minDurationMinutes = 1)

    @Before
    fun setup() {
        repository = mockk()
        useCase = EditTimeRecordUseCase(repository, config)
    }

    @Test
    fun `when endTime is null, should return failure`() = runTest {
        val record = TimeRecord(
            RecordId(UUID.randomUUID()),
            UserId(UUID.randomUUID()),
            CategoryId(UUID.randomUUID()),
            Instant.now(),
            null
        )
        val result = useCase(record)
        assertTrue(result.isFailure)
        assertEquals("Время окончания обязательно", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { repository.saveRecord(any()) }
    }

    @Test
    fun `when validation passes, should save record and exclude self from overlapping check`() = runTest {
        val startTime = Instant.parse("2024-01-01T10:00:00Z")
        val endTime = Instant.parse("2024-01-01T11:00:00Z")
        val recordId = RecordId(UUID.randomUUID())
        val userId = UserId(UUID.randomUUID())
        val record = TimeRecord(recordId, userId, CategoryId(UUID.randomUUID()), startTime, endTime)

        coEvery { repository.getOverlappingRecords(userId, startTime, endTime, recordId) } returns emptyList()
        coEvery { repository.saveRecord(record) } returns Result.success(Unit)

        val result = useCase(record)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.saveRecord(record) }
        coVerify(exactly = 1) { repository.getOverlappingRecords(userId, startTime, endTime, recordId) }
    }
}
