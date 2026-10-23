package com.ppo.domain.usecase

import com.ppo.domain.builders.TimeRecordBuilder
import com.ppo.domain.fixtures.TestUserMother
import com.ppo.domain.model.RecordId
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
import java.util.UUID

@RunWith(RandomOrderRunner::class)
class DeleteRecordUseCaseTest {
    private val timeRecordRepository = mockk<ITimeRecordRepository>()
    private lateinit var useCase: DeleteRecordUseCase

    @Before
    fun setUp() {
        useCase = DeleteRecordUseCase(timeRecordRepository)
    }

    @Test
    fun shouldDeleteRecord_whenOwnerRequestsDeletion() = runBlocking {
        val user = TestUserMother.regularUser()
        val record = TimeRecordBuilder().withUserId(user.id).build()
        coEvery { timeRecordRepository.getRecordById(record.id) } returns record
        coEvery { timeRecordRepository.deleteRecord(record.id) } returns Result.success(Unit)

        val result = useCase(record.id, user.id, false)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { timeRecordRepository.deleteRecord(record.id) }
    }

    @Test
    fun shouldDeleteRecord_whenAdminRequestsDeletion() = runBlocking {
        val owner = TestUserMother.regularUser()
        val admin = TestUserMother.adminUser()
        val record = TimeRecordBuilder().withUserId(owner.id).build()
        coEvery { timeRecordRepository.getRecordById(record.id) } returns record
        coEvery { timeRecordRepository.deleteRecord(record.id) } returns Result.success(Unit)

        val result = useCase(record.id, admin.id, true)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { timeRecordRepository.deleteRecord(record.id) }
    }

    @Test
    fun shouldFailWithIllegalAccess_whenUserDeletesForeignRecord() = runBlocking {
        val owner = TestUserMother.regularUser()
        val other = TestUserMother.otherUser()
        val record = TimeRecordBuilder().withUserId(owner.id).build()
        coEvery { timeRecordRepository.getRecordById(record.id) } returns record

        val result = useCase(record.id, other.id, false)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalAccessException)
        coVerify(exactly = 0) { timeRecordRepository.deleteRecord(any()) }
    }

    @Test
    fun shouldFailWithNoSuchElement_whenRecordDoesNotExist() = runBlocking {
        val user = TestUserMother.regularUser()
        val missingId = RecordId(UUID.randomUUID())
        coEvery { timeRecordRepository.getRecordById(missingId) } returns null

        val result = useCase(missingId, user.id, false)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NoSuchElementException)
    }
}
