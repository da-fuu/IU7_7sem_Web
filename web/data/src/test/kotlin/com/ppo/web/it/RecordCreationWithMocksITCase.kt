package com.ppo.web.it

import com.ppo.domain.model.*
import com.ppo.domain.repository.IUserSettingsRepository
import com.ppo.domain.usecase.CreateTimeRecordUseCase
import com.ppo.web.data.repository.PostgresCategoryRepository
import com.ppo.web.data.repository.PostgresTimeRecordRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
class RecordCreationWithMocksITCase {

    @get:Rule
    val rollbackRule = TestDbHelper.rollbackRule()

    private val rawUserId = UUID.randomUUID()
    private val userId = UserId(rawUserId)
    private val baseCategoryId = CategoryId(UUID.fromString("11111111-1111-1111-1111-111111111111"))

    @Before
    fun setUp() {
        transaction {
            exec("INSERT INTO users (id, login, name, password_hash, role) VALUES ('$rawUserId', 'rec_user_${rawUserId.toString()}', 'Record User', 'hash', 'USER')")
        }
    }

    @Test
    fun executeRecordCreationWithMockedSettings() = runBlocking {
        val dataSource = TestDbHelper.getDataSource()
        val timeRecordRepository = PostgresTimeRecordRepository(dataSource)
        val categoryRepository = PostgresCategoryRepository(dataSource)
        val mockedSettingsRepository = mockk<IUserSettingsRepository>()

        coEvery { mockedSettingsRepository.getSettings(userId) } returns UserSettings(
            userId = userId,
            autoStopEnabled = false,
            maxActiveDurationValue = 4,
            maxActiveDurationUnit = DurationUnit.hours,
            minDurationEnabled = true,
            minDurationValue = 10,
            minDurationUnit = DurationUnit.minutes
        )

        val useCase = CreateTimeRecordUseCase(timeRecordRepository, categoryRepository, mockedSettingsRepository)

        val now = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val activeResult = useCase(
            userId = userId,
            categoryId = baseCategoryId,
            startTime = now,
            endTime = null,
            description = "Active Task with Mocks"
        )
        assertTrue(activeResult.isSuccess)
        val activeRecord = activeResult.getOrThrow()
        assertNotNull(activeRecord.id)
        assertNull(activeRecord.endTime)
        val persistedActive = timeRecordRepository.getRecordById(activeRecord.id)
        assertEquals("Active Task with Mocks", persistedActive?.description)

        val tooShortEnd = now.plus(2, ChronoUnit.MINUTES)
        val shortResult = useCase(
            userId = userId,
            categoryId = baseCategoryId,
            startTime = now,
            endTime = tooShortEnd,
            description = "Too Short"
        )
        assertTrue(shortResult.isFailure)
        assertTrue(shortResult.exceptionOrNull() is IllegalStateException)

        val validEnd = now.plus(30, ChronoUnit.MINUTES)
        val completedResult = useCase(
            userId = userId,
            categoryId = baseCategoryId,
            startTime = now,
            endTime = validEnd,
            description = "Valid Completed Task"
        )
        assertTrue(completedResult.isSuccess)
        val completedRecord = completedResult.getOrThrow()
        assertNotNull(completedRecord.endTime)
        val persistedCompleted = timeRecordRepository.getRecordById(completedRecord.id)
        assertTrue(persistedCompleted?.endTime != null)
    }
}
