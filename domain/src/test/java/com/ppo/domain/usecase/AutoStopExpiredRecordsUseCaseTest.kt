package com.ppo.domain.usecase

import com.ppo.domain.builders.TimeRecordBuilder
import com.ppo.domain.builders.UserSettingsBuilder
import com.ppo.domain.fixtures.TestUserMother
import com.ppo.domain.model.DurationUnit
import com.ppo.domain.repository.IRecordHistoryRepository
import com.ppo.domain.repository.ITimeRecordRepository
import com.ppo.domain.repository.IUserSettingsRepository
import com.ppo.domain.runner.RandomOrderRunner
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(RandomOrderRunner::class)
class AutoStopExpiredRecordsUseCaseTest {
    private val timeRecordRepository = mockk<ITimeRecordRepository>()
    private val userSettingsRepository = mockk<IUserSettingsRepository>()
    private val recordHistoryRepository = mockk<IRecordHistoryRepository>()
    private lateinit var useCase: AutoStopExpiredRecordsUseCase

    @Before
    fun setUp() {
        useCase = AutoStopExpiredRecordsUseCase(timeRecordRepository, userSettingsRepository, recordHistoryRepository)
    }

    @Test
    fun shouldAutoStopOverdueRecordAndSaveHistory_whenDurationExceedsMaxLimit() = runBlocking {
        val user = TestUserMother.regularUser()
        val now = Instant.parse("2026-10-15T15:00:00Z")
        val record = TimeRecordBuilder()
            .withUserId(user.id)
            .withStartTime(Instant.parse("2026-10-15T10:00:00Z"))
            .asActive()
            .build()
        val settings = UserSettingsBuilder()
            .withUserId(user.id)
            .withAutoStop(true, 120, DurationUnit.minutes)
            .build()
        coEvery { timeRecordRepository.getActiveRecords(user.id) } returns listOf(record)
        coEvery { userSettingsRepository.getSettings(user.id) } returns settings
        coEvery { recordHistoryRepository.saveHistoryEntry(any()) } returns Result.success(Unit)
        coEvery { timeRecordRepository.saveRecord(any()) } returns Result.success(Unit)

        val stoppedCount = useCase(user.id, now)

        assertEquals(1, stoppedCount)
        coVerify(exactly = 1) { recordHistoryRepository.saveHistoryEntry(any()) }
        coVerify(exactly = 1) { timeRecordRepository.saveRecord(match { it.endTime != null }) }
    }

    @Test
    fun shouldNotStopRecord_whenDurationWithinAllowedLimit() = runBlocking {
        val user = TestUserMother.regularUser()
        val now = Instant.parse("2026-10-15T10:30:00Z")
        val record = TimeRecordBuilder()
            .withUserId(user.id)
            .withStartTime(Instant.parse("2026-10-15T10:00:00Z"))
            .asActive()
            .build()
        val settings = UserSettingsBuilder()
            .withUserId(user.id)
            .withAutoStop(true, 120, DurationUnit.minutes)
            .build()
        coEvery { timeRecordRepository.getActiveRecords(user.id) } returns listOf(record)
        coEvery { userSettingsRepository.getSettings(user.id) } returns settings

        val stoppedCount = useCase(user.id, now)

        assertEquals(0, stoppedCount)
        coVerify(exactly = 0) { timeRecordRepository.saveRecord(any()) }
    }

    @Test
    fun shouldNotStopRecord_whenAutoStopDisabledInUserSettings() = runBlocking {
        val user = TestUserMother.regularUser()
        val now = Instant.parse("2026-10-15T18:00:00Z")
        val record = TimeRecordBuilder()
            .withUserId(user.id)
            .withStartTime(Instant.parse("2026-10-15T10:00:00Z"))
            .asActive()
            .build()
        val settings = UserSettingsBuilder()
            .withUserId(user.id)
            .withAutoStop(false)
            .build()
        coEvery { timeRecordRepository.getActiveRecords(user.id) } returns listOf(record)
        coEvery { userSettingsRepository.getSettings(user.id) } returns settings

        val stoppedCount = useCase(user.id, now)

        assertEquals(0, stoppedCount)
        coVerify(exactly = 0) { timeRecordRepository.saveRecord(any()) }
    }
}
