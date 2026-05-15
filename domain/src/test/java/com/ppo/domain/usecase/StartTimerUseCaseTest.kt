package com.ppo.domain.usecase

import com.ppo.domain.model.CategoryId
import com.ppo.domain.model.RecordId
import com.ppo.domain.model.TimeRecord
import com.ppo.domain.model.UserId
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

class StartTimerUseCaseTest {

    private lateinit var repository: ITimeRecordRepository
    private lateinit var useCase: StartTimerUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = StartTimerUseCase(repository)
    }

    @Test
    fun `when active session exists, should return failure`() = runTest {
        val userId = UserId(UUID.randomUUID())
        val categoryId = CategoryId(UUID.randomUUID())
        val startTime = Instant.now()
        val activeSession = TimeRecord(RecordId(UUID.randomUUID()), userId, categoryId, startTime, null)

        coEvery { repository.getActiveSession(userId) } returns activeSession

        val result = useCase(userId, categoryId, startTime)

        assertTrue(result.isFailure)
        assertEquals("Сначала остановите текущий таймер", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { repository.saveRecord(any()) }
    }

    @Test
    fun `when no active session, should save new record and return success`() = runTest {
        val userId = UserId(UUID.randomUUID())
        val categoryId = CategoryId(UUID.randomUUID())
        val startTime = Instant.now()

        coEvery { repository.getActiveSession(userId) } returns null
        coEvery { repository.saveRecord(any()) } returns Result.success(Unit)

        val result = useCase(userId, categoryId, startTime)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { 
            repository.saveRecord(match { 
                it.userId == userId && it.categoryId == categoryId && it.startTime == startTime && it.endTime == null 
            }) 
        }
    }
}
