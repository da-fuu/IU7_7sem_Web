package com.ppo.domain.usecase

import com.ppo.domain.builders.TimeRecordBuilder
import com.ppo.domain.fixtures.TestCategoryMother
import com.ppo.domain.fixtures.TestUserMother
import com.ppo.domain.repository.ITimeRecordRepository
import com.ppo.domain.runner.RandomOrderRunner
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(RandomOrderRunner::class)
class StartTimerUseCaseTest {
    private val repository = mockk<ITimeRecordRepository>()
    private lateinit var useCase: StartTimerUseCase

    @Before
    fun setUp() {
        useCase = StartTimerUseCase(repository)
    }

    @Test
    fun shouldStartTimer_whenNoActiveSessionExists() = runBlocking {
        val user = TestUserMother.regularUser()
        val category = TestCategoryMother.baseWorkCategory()
        val startTime = Instant.parse("2026-10-15T10:00:00Z")
        coEvery { repository.getActiveSession(user.id) } returns null
        coEvery { repository.saveRecord(any()) } returns Result.success(Unit)

        val result = useCase(user.id, category.id, startTime)

        assertTrue(result.isSuccess)
        assertNull(result.getOrNull()?.endTime)
        coVerify(exactly = 1) { repository.saveRecord(match { it.endTime == null }) }
    }

    @Test
    fun shouldFailToStartTimer_whenActiveSessionAlreadyRunning() = runBlocking {
        val user = TestUserMother.regularUser()
        val category = TestCategoryMother.baseWorkCategory()
        val active = TimeRecordBuilder().withUserId(user.id).asActive().build()
        coEvery { repository.getActiveSession(user.id) } returns active

        val result = useCase(user.id, category.id)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }
}
