package com.ppo.domain.usecase

import com.ppo.domain.model.RecordId
import com.ppo.domain.model.TimeRecord
import com.ppo.domain.model.UserId
import com.ppo.domain.model.CategoryId
import com.ppo.domain.repository.ITimeRecordRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.UUID

class GetTimeRecordsUseCaseTest {

    private lateinit var repository: ITimeRecordRepository
    private lateinit var useCase: GetTimeRecordsUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = GetTimeRecordsUseCase(repository)
    }

    @Test
    fun `should return records from repository`() = runTest {
        val userId = UserId(UUID.randomUUID())
        val records = listOf(
            TimeRecord(RecordId(UUID.randomUUID()), userId, CategoryId(UUID.randomUUID()), Instant.now(), null),
            TimeRecord(
                RecordId(UUID.randomUUID()),
                userId,
                CategoryId(UUID.randomUUID()),
                Instant.now().minusSeconds(3600),
                Instant.now()
            )
        )

        coEvery { repository.getTimeRecords(userId) } returns records

        val result = useCase(userId)

        assertEquals(records.size, result.size)
        assertEquals(records[0].id, result[0].id)
        assertEquals(records[1].startTime, result[1].startTime)
    }
}
