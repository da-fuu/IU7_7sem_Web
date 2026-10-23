package com.ppo.domain.usecase

import com.ppo.domain.builders.TimeRecordBuilder
import com.ppo.domain.fixtures.TestUserMother
import com.ppo.domain.repository.ITimeRecordRepository
import com.ppo.domain.runner.RandomOrderRunner
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(RandomOrderRunner::class)
class GetTimeRecordsUseCaseTest {
    private val timeRecordRepository = mockk<ITimeRecordRepository>()
    private lateinit var useCase: GetTimeRecordsUseCase

    @Before
    fun setUp() {
        useCase = GetTimeRecordsUseCase(timeRecordRepository)
    }

    @Test
    fun shouldReturnTimeRecordsList_whenInvoked() = runBlocking {
        val user = TestUserMother.regularUser()
        val records = listOf(TimeRecordBuilder().withUserId(user.id).build())
        coEvery { timeRecordRepository.getTimeRecords(user.id) } returns records

        val result = useCase(user.id)

        assertEquals(1, result.size)
    }

    @Test
    fun shouldReturnEmptyList_whenUserHasNoRecords() = runBlocking {
        val user = TestUserMother.regularUser()
        coEvery { timeRecordRepository.getTimeRecords(user.id) } returns emptyList()

        val result = useCase(user.id)

        assertEquals(0, result.size)
    }
}
