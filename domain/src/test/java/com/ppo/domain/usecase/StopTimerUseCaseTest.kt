package com.ppo.domain.usecase

import com.ppo.domain.builders.TimeRecordBuilder
import com.ppo.domain.fixtures.TestUserMother
import com.ppo.domain.model.BusinessConfig
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

@RunWith(RandomOrderRunner::class)
class StopTimerUseCaseTest {
    private val repository = mockk<ITimeRecordRepository>()
    private val config = BusinessConfig(minDurationMinutes = 5)
    private lateinit var useCase: StopTimerUseCase

    @Before
    fun setUp() {
        useCase = StopTimerUseCase(repository, config)
    }

    @Test
    fun shouldStopTimer_whenDurationAboveThreshold() = runBlocking {
        val user = TestUserMother.regularUser()
        val start = Instant.parse("2026-10-15T10:00:00Z")
        val stop = Instant.parse("2026-10-15T10:10:00Z")
        val active = TimeRecordBuilder().withUserId(user.id).withStartTime(start).asActive().build()
        coEvery { repository.getActiveSession(user.id) } returns active
        coEvery { repository.saveRecord(any()) } returns Result.success(Unit)

        val result = useCase(user.id, stop)

        assertTrue(result.isSuccess)
        assertEquals(stop, result.getOrNull()?.endTime)
        coVerify(exactly = 1) { repository.saveRecord(match { it.endTime == stop }) }
    }

    @Test
    fun shouldCancelAndRemoveTimer_whenDurationBelowThreshold() = runBlocking {
        val user = TestUserMother.regularUser()
        val start = Instant.parse("2026-10-15T10:00:00Z")
        val stop = Instant.parse("2026-10-15T10:02:00Z")
        val active = TimeRecordBuilder().withUserId(user.id).withStartTime(start).asActive().build()
        coEvery { repository.getActiveSession(user.id) } returns active
        coEvery { repository.deleteRecord(active.id) } returns Result.success(Unit)

        val result = useCase(user.id, stop)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        coVerify(exactly = 1) { repository.deleteRecord(active.id) }
    }

    @Test
    fun shouldFailToStop_whenNoActiveSessionExists() = runBlocking {
        val user = TestUserMother.regularUser()
        coEvery { repository.getActiveSession(user.id) } returns null

        val result = useCase(user.id, Instant.now())

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }
}
