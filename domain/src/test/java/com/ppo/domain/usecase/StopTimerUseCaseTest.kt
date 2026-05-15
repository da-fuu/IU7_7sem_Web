package com.ppo.domain.usecase

import com.ppo.domain.model.*
import com.ppo.domain.repository.ITimeRecordRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.UUID

class StopTimerUseCaseTest {

    private lateinit var repository: ITimeRecordRepository
    private lateinit var useCase: StopTimerUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = StopTimerUseCase(repository)
    }

    @Test
    fun `when no active session, should return failure`() = runTest {
        val userId = UserId(UUID.randomUUID())
        val stopTime = Instant.parse("2024-01-01T11:00:00Z")

        coEvery { repository.getActiveSession(userId) } returns null

        val result = useCase(userId, stopTime)

        assertTrue(result.isFailure)
        assertEquals("Нет активной сессии", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { repository.saveRecord(any()) }
    }

    @Test
    fun `when session duration is less than 1 minute, should delete record and return failure`() = runTest {
        val userId = UserId(UUID.randomUUID())
        val startTime = Instant.parse("2024-01-01T10:00:00Z")
        val stopTime = Instant.parse("2024-01-01T10:00:30Z") // 30 seconds

        val activeSession = TimeRecord(
            id = RecordId(UUID.randomUUID()),
            userId = userId,
            categoryId = CategoryId(UUID.randomUUID()),
            startTime = startTime,
            endTime = null
        )

        coEvery { repository.getActiveSession(userId) } returns activeSession
        coEvery { repository.deleteRecord(activeSession.id) } returns Result.success(Unit)

        val result = useCase(userId, stopTime)

        assertTrue(result.isFailure)
        assertEquals("Сессия менее 1 минуты отменена", result.exceptionOrNull()?.message)
        coVerify(exactly = 1) { repository.deleteRecord(activeSession.id) }
        coVerify(exactly = 0) { repository.saveRecord(any()) }
    }

    @Test
    fun `when session duration is valid, should save updated session and return success`() = runTest {
        val userId = UserId(UUID.randomUUID())
        val startTime = Instant.parse("2024-01-01T10:00:00Z")
        val stopTime = Instant.parse("2024-01-01T11:00:00Z") // 1 hour

        val activeSession = TimeRecord(
            id = RecordId(UUID.randomUUID()),
            userId = userId,
            categoryId = CategoryId(UUID.randomUUID()),
            startTime = startTime,
            endTime = null
        )

        val updatedSession = activeSession.copy(endTime = stopTime)

        coEvery { repository.getActiveSession(userId) } returns activeSession
        coEvery { repository.saveRecord(updatedSession) } returns Result.success(Unit)

        val result = useCase(userId, stopTime)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.saveRecord(updatedSession) }
        coVerify(exactly = 0) { repository.deleteRecord(any()) }
    }
}
