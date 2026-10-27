package com.ppo.web.it

import com.ppo.domain.model.RecordId
import com.ppo.domain.model.UserId
import com.ppo.domain.usecase.DeleteRecordUseCase
import com.ppo.web.data.repository.PostgresTimeRecordRepository
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Rule
import org.junit.Before
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@RunWith(AllureRunner::class)
class RecordDeletionNoMocksITCase {

    @get:Rule
    val rollbackRule = TestDbHelper.rollbackRule()

    private val rawUserId = UUID.randomUUID()
    private val rawForeignUserId = UUID.randomUUID()
    private val rawRecordId = UUID.randomUUID()
    private val userId = UserId(rawUserId)
    private val foreignUserId = UserId(rawForeignUserId)
    private val recordId = RecordId(rawRecordId)
    private val categoryId = "11111111-1111-1111-1111-111111111111"

    @Before
    fun setUp() {
        val now = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val startTime = now.minus(1, ChronoUnit.HOURS)
        val endTime = now
        transaction {
            exec("INSERT INTO users (id, login, name, password_hash, role) VALUES ('$rawUserId', 'del_user_${rawUserId.toString()}', 'Delete User', 'hash', 'USER')")
            exec("INSERT INTO users (id, login, name, password_hash, role) VALUES ('$rawForeignUserId', 'foreign_${rawForeignUserId.toString()}', 'Foreign User', 'hash', 'USER')")
            exec("INSERT INTO time_records (id, user_id, category_id, start_time, end_time, description) VALUES ('$rawRecordId', '$rawUserId', '$categoryId', '$startTime', '$endTime', 'Record to delete')")
        }
    }

    @Test
    fun executeRecordDeletionWithoutMocks() = runBlocking {
        val repository = PostgresTimeRecordRepository(TestDbHelper.getDataSource())
        val useCase = DeleteRecordUseCase(repository)

        val nonExistingResult = useCase(RecordId(UUID.randomUUID()), userId)
        assertTrue(nonExistingResult.isFailure)

        val unauthorizedResult = useCase(recordId, foreignUserId)
        assertTrue(unauthorizedResult.isFailure)

        val existingRecord = repository.getRecordById(recordId)
        assertEquals("Record to delete", existingRecord?.description)

        val successfulResult = useCase(recordId, userId)
        assertTrue(successfulResult.isSuccess)

        val deletedRecord = repository.getRecordById(recordId)
        assertNull(deletedRecord)
    }
}
