package com.ppo.domain.usecase

import com.ppo.domain.builders.CategoryBuilder
import com.ppo.domain.builders.TimeRecordBuilder
import com.ppo.domain.fixtures.TestCategoryMother
import com.ppo.domain.fixtures.TestUserMother
import com.ppo.domain.model.RecordId
import com.ppo.domain.repository.ICategoryRepository
import com.ppo.domain.repository.IRecordHistoryRepository
import com.ppo.domain.repository.ITimeRecordRepository
import com.ppo.domain.repository.IUserSettingsRepository
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
import java.util.UUID

@RunWith(RandomOrderRunner::class)
class UpdateTimeRecordUseCaseTest {
    private val timeRecordRepository = mockk<ITimeRecordRepository>()
    private val categoryRepository = mockk<ICategoryRepository>()
    private val userSettingsRepository = mockk<IUserSettingsRepository>()
    private val recordHistoryRepository = mockk<IRecordHistoryRepository>()
    private lateinit var useCase: UpdateTimeRecordUseCase

    @Before
    fun setUp() {
        useCase = UpdateTimeRecordUseCase(
            timeRecordRepository,
            categoryRepository,
            userSettingsRepository,
            recordHistoryRepository
        )
        coEvery { userSettingsRepository.getSettings(any()) } returns com.ppo.domain.model.UserSettings(
            userId = com.ppo.domain.fixtures.TestUserMother.regularUser().id,
            minDurationEnabled = false
        )
    }

    @Test
    fun shouldUpdateRecordAndSaveHistorySnapshot_whenOwnerUpdatesFields() = runBlocking {
        val user = TestUserMother.regularUser()
        val original = TimeRecordBuilder().withUserId(user.id).withDescription("Old description").build()
        val newEndTime = original.startTime.plusSeconds(3600)
        coEvery { timeRecordRepository.getRecordById(original.id) } returns original
        coEvery { recordHistoryRepository.saveHistoryEntry(any()) } returns Result.success(Unit)
        coEvery { timeRecordRepository.saveRecord(any()) } returns Result.success(Unit)

        val result = useCase(
            recordId = original.id,
            requestUserId = user.id,
            description = "New description",
            endTime = newEndTime
        )

        assertTrue(result.isSuccess)
        assertEquals("New description", result.getOrNull()?.description)
        coVerify(exactly = 1) {
            recordHistoryRepository.saveHistoryEntry(match {
                it.recordId == original.id && it.description == "Old description"
            })
        }
        coVerify(exactly = 1) { timeRecordRepository.saveRecord(match { it.description == "New description" }) }
    }

    @Test
    fun shouldUpdateRecord_whenAdminUpdatesAnotherUserRecord() = runBlocking {
        val owner = TestUserMother.regularUser()
        val admin = TestUserMother.adminUser()
        val record = TimeRecordBuilder().withUserId(owner.id).build()
        coEvery { timeRecordRepository.getRecordById(record.id) } returns record
        coEvery { recordHistoryRepository.saveHistoryEntry(any()) } returns Result.success(Unit)
        coEvery { timeRecordRepository.saveRecord(any()) } returns Result.success(Unit)

        val result = useCase(
            recordId = record.id,
            requestUserId = admin.id,
            isAdmin = true,
            description = "Admin override"
        )

        assertTrue(result.isSuccess)
    }

    @Test
    fun shouldFailWithIllegalAccess_whenNonOwnerAndNotAdminAttemptsUpdate() = runBlocking {
        val owner = TestUserMother.regularUser()
        val otherUser = TestUserMother.otherUser()
        val record = TimeRecordBuilder().withUserId(owner.id).build()
        coEvery { timeRecordRepository.getRecordById(record.id) } returns record

        val result = useCase(
            recordId = record.id,
            requestUserId = otherUser.id,
            isAdmin = false,
            description = "Hacked"
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalAccessException)
    }

    @Test
    fun shouldFailWithNoSuchElement_whenRecordNotFound() = runBlocking {
        val user = TestUserMother.regularUser()
        val missingId = RecordId(UUID.randomUUID())
        coEvery { timeRecordRepository.getRecordById(missingId) } returns null

        val result = useCase(recordId = missingId, requestUserId = user.id)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NoSuchElementException)
    }

    @Test
    fun shouldFailWithIllegalArgument_whenNewEndTimeIsBeforeStartTime() = runBlocking {
        val user = TestUserMother.regularUser()
        val record = TimeRecordBuilder().withUserId(user.id).build()
        val invalidEnd = record.startTime.minusSeconds(60)
        coEvery { timeRecordRepository.getRecordById(record.id) } returns record

        val result = useCase(
            recordId = record.id,
            requestUserId = user.id,
            endTime = invalidEnd
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }
}
