package com.ppo.domain.usecase

import com.ppo.domain.builders.TimeRecordBuilder
import com.ppo.domain.fixtures.TestUserMother
import com.ppo.domain.model.RecordHistoryEntry
import com.ppo.domain.model.RecordId
import com.ppo.domain.model.VersionId
import com.ppo.domain.repository.IRecordHistoryRepository
import com.ppo.domain.repository.ITimeRecordRepository
import com.ppo.domain.runner.RandomOrderRunner
import io.mockk.coEvery
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
class GetRecordHistoryUseCaseTest {
    private val timeRecordRepository = mockk<ITimeRecordRepository>()
    private val recordHistoryRepository = mockk<IRecordHistoryRepository>()
    private lateinit var useCase: GetRecordHistoryUseCase

    @Before
    fun setUp() {
        useCase = GetRecordHistoryUseCase(timeRecordRepository, recordHistoryRepository)
    }

    @Test
    fun shouldReturnHistoryList_whenOwnerRequestsHistory() = runBlocking {
        val user = TestUserMother.regularUser()
        val record = TimeRecordBuilder().withUserId(user.id).build()
        val entries = listOf(
            RecordHistoryEntry(VersionId(UUID.randomUUID()), record.id, record.categoryId, record.startTime, record.endTime, "v1", Instant.now())
        )
        coEvery { timeRecordRepository.getRecordById(record.id) } returns record
        coEvery { recordHistoryRepository.getHistoryForRecord(record.id) } returns entries

        val result = useCase(record.id, user.id, false)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
    }

    @Test
    fun shouldFailWithIllegalAccess_whenNonOwnerRequestsHistory() = runBlocking {
        val user = TestUserMother.regularUser()
        val other = TestUserMother.otherUser()
        val record = TimeRecordBuilder().withUserId(user.id).build()
        coEvery { timeRecordRepository.getRecordById(record.id) } returns record

        val result = useCase(record.id, other.id, false)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalAccessException)
    }

    @Test
    fun shouldFailWithNoSuchElement_whenRecordNotFound() = runBlocking {
        val user = TestUserMother.regularUser()
        val missingId = RecordId(UUID.randomUUID())
        coEvery { timeRecordRepository.getRecordById(missingId) } returns null

        val result = useCase(missingId, user.id, false)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NoSuchElementException)
    }
}
