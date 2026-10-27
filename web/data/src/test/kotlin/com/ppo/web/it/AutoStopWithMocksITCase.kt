package com.ppo.web.it

import com.ppo.domain.model.DurationUnit
import com.ppo.domain.model.RecordId
import com.ppo.domain.model.UserId
import com.ppo.domain.model.UserSettings
import com.ppo.domain.repository.IUserSettingsRepository
import com.ppo.domain.usecase.AutoStopExpiredRecordsUseCase
import com.ppo.web.data.repository.PostgresRecordHistoryRepository
import com.ppo.web.data.repository.PostgresTimeRecordRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class AutoStopWithMocksITCase {

    private val rawUserId = UUID.randomUUID()
    private val rawRecordId = UUID.randomUUID()
    private val userId = UserId(rawUserId)
    private val recordId = RecordId(rawRecordId)
    private val categoryId = "11111111-1111-1111-1111-111111111111"
    private val startTime = Instant.now().minus(3, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS)

    @Before
    fun setUp() {
        TestDbHelper.initTestSchema()
        transaction {
            exec("INSERT INTO users (id, login, name, password_hash, role) VALUES ('$rawUserId', 'autostop_${rawUserId.toString().take(8)}', 'AutoStop User', 'hash', 'USER')")
            exec("INSERT INTO time_records (id, user_id, category_id, start_time, end_time, description) VALUES ('$rawRecordId', '$rawUserId', '$categoryId', '$startTime', NULL, 'Long Active Task')")
        }
    }

    @After
    fun tearDown() {
        TestDbHelper.resetDatabase()
    }

    @Test
    fun executeAutoStopWithMockedSettings() = runBlocking {
        val dataSource = TestDbHelper.getDataSource()
        val timeRecordRepository = PostgresTimeRecordRepository(dataSource)
        val recordHistoryRepository = PostgresRecordHistoryRepository(dataSource)
        val mockedSettingsRepository = mockk<IUserSettingsRepository>()

        coEvery { mockedSettingsRepository.getSettings(userId) } returns UserSettings(
            userId = userId,
            autoStopEnabled = true,
            maxActiveDurationValue = 2,
            maxActiveDurationUnit = DurationUnit.hours
        )

        val useCase = AutoStopExpiredRecordsUseCase(timeRecordRepository, mockedSettingsRepository, recordHistoryRepository)

        val now = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val stoppedCount = useCase(userId, now)
        assertEquals(1, stoppedCount)

        val updatedRecord = timeRecordRepository.getRecordById(recordId)
        assertNotNull(updatedRecord?.endTime)
        val expectedEndTime = startTime.plus(2, ChronoUnit.HOURS)
        assertEquals(expectedEndTime, updatedRecord?.endTime)

        val historyEntries = recordHistoryRepository.getHistoryForRecord(recordId)
        assertTrue(historyEntries.isNotEmpty())

        val secondRunCount = useCase(userId, now)
        assertEquals(0, secondRunCount)
    }
}
