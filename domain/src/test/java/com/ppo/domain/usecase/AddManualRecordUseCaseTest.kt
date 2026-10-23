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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(RandomOrderRunner::class)
class AddManualRecordUseCaseTest {
    private val repository = mockk<ITimeRecordRepository>()
    private val config = BusinessConfig(minDurationMinutes = 5)
    private lateinit var useCase: AddManualRecordUseCase

    @Before
    fun setUp() {
        useCase = AddManualRecordUseCase(repository, config)
    }

    @Test
    fun shouldAddManualRecord_whenIntervalValidAndNoOverlap() = runBlocking {
        val user = TestUserMother.regularUser()
        val record = TimeRecordBuilder()
            .withUserId(user.id)
            .withStartTime(Instant.parse("2026-10-15T10:00:00Z"))
            .withEndTime(Instant.parse("2026-10-15T10:30:00Z"))
            .build()
        coEvery { repository.getOverlappingRecords(user.id, record.startTime, record.endTime, null) } returns emptyList()
        coEvery { repository.saveRecord(record) } returns Result.success(Unit)

        val result = useCase(record)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.saveRecord(record) }
    }

    @Test
    fun shouldFailManualRecord_whenEndTimeBeforeStartTime() = runBlocking {
        val user = TestUserMother.regularUser()
        val invalidRecord = TimeRecordBuilder()
            .withUserId(user.id)
            .withStartTime(Instant.parse("2026-10-15T12:00:00Z"))
            .withEndTime(Instant.parse("2026-10-15T11:00:00Z"))
            .build()

        val result = useCase(invalidRecord)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun shouldFailManualRecord_whenRecordsOverlap() = runBlocking {
        val user = TestUserMother.regularUser()
        val record = TimeRecordBuilder()
            .withUserId(user.id)
            .withStartTime(Instant.parse("2026-10-15T10:00:00Z"))
            .withEndTime(Instant.parse("2026-10-15T10:30:00Z"))
            .build()
        val overlapping = listOf(TimeRecordBuilder().withUserId(user.id).build())
        coEvery { repository.getOverlappingRecords(user.id, record.startTime, record.endTime, null) } returns overlapping

        val result = useCase(record)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }
}
