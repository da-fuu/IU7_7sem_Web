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

class AddManualRecordUseCaseTest {

    private lateinit var repository: ITimeRecordRepository
    private lateinit var useCase: AddManualRecordUseCase
    private val config = BusinessConfig(minDurationMinutes = 1)

    @Before
    fun setup() {
        repository = mockk()
        useCase = AddManualRecordUseCase(repository, config)
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
    fun `when endTime is before startTime, should return failure`() = runTest {
        val startTime = Instant.parse("2024-01-01T10:00:00Z")
        val endTime = Instant.parse("2024-01-01T09:00:00Z")
        val record = TimeRecord(
            RecordId(UUID.randomUUID()),
            UserId(UUID.randomUUID()),
            CategoryId(UUID.randomUUID()),
            startTime,
            endTime
        )

        val result = useCase(record)

        assertTrue(result.isFailure)
        assertEquals("Конец промежутка не может быть раньше начала", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { repository.saveRecord(any()) }
    }

    @Test
    fun `when duration is less than 1 minute, should return failure`() = runTest {
        val startTime = Instant.parse("2024-01-01T10:00:00Z")
        val endTime = Instant.parse("2024-01-01T10:00:30Z") // 30 seconds
        val record = TimeRecord(
            RecordId(UUID.randomUUID()),
            UserId(UUID.randomUUID()),
            CategoryId(UUID.randomUUID()),
            startTime,
            endTime
        )

        val result = useCase(record)

        assertTrue(result.isFailure)
        assertEquals("Запись слишком короткая", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { repository.saveRecord(any()) }
    }

    @Test
    fun `when records overlap, should return failure`() = runTest {
        val startTime = Instant.parse("2024-01-01T10:00:00Z")
        val endTime = Instant.parse("2024-01-01T11:00:00Z")
        val userId = UserId(UUID.randomUUID())
        val record = TimeRecord(RecordId(UUID.randomUUID()), userId, CategoryId(UUID.randomUUID()), startTime, endTime)

        coEvery {
            repository.getOverlappingRecords(
                userId,
                startTime,
                endTime,
                null
            )
        } returns listOf(record.copy(id = RecordId(UUID.randomUUID())))

        val result = useCase(record)

        assertTrue(result.isFailure)
        assertEquals("Пересечение времени", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { repository.saveRecord(any()) }
    }

    @Test
    fun `when validation passes, should save record and return success`() = runTest {
        val startTime = Instant.parse("2024-01-01T10:00:00Z")
        val endTime = Instant.parse("2024-01-01T11:00:00Z")
        val userId = UserId(UUID.randomUUID())
        val record = TimeRecord(RecordId(UUID.randomUUID()), userId, CategoryId(UUID.randomUUID()), startTime, endTime)

        coEvery { repository.getOverlappingRecords(userId, startTime, endTime, null) } returns emptyList()
        coEvery { repository.saveRecord(record) } returns Result.success(Unit)

        val result = useCase(record)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.saveRecord(record) }
    }
}
