package com.ppo.web.it

import com.ppo.domain.model.*
import com.ppo.domain.usecase.CreateTimeRecordUseCase
import com.ppo.domain.usecase.DeleteRecordUseCase
import com.ppo.domain.usecase.GetTimeRecordsUseCase
import com.ppo.domain.usecase.UpdateTimeRecordUseCase
import com.ppo.web.data.repository.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Rule
import java.time.Instant
import java.util.UUID

@RunWith(AllureRunner::class)
class TimeTrackingLifecycleITCase {

    @get:Rule
    val rollbackRule = TestDbHelper.rollbackRule()

    private val dataSource = TestDbHelper.getDataSource()
    private val timeRecordRepository = PostgresTimeRecordRepository(dataSource)
    private val categoryRepository = PostgresCategoryRepository(dataSource)
    private val userSettingsRepository = PostgresUserSettingsRepository(dataSource)
    private val recordHistoryRepository = PostgresRecordHistoryRepository(dataSource)
    private val userRepository = PostgresUserRepository(dataSource)

    private val createRecordUseCase = CreateTimeRecordUseCase(timeRecordRepository, categoryRepository, userSettingsRepository)
    private val updateRecordUseCase = UpdateTimeRecordUseCase(timeRecordRepository, categoryRepository, userSettingsRepository, recordHistoryRepository)
    private val deleteRecordUseCase = DeleteRecordUseCase(timeRecordRepository)
    private val getTimeRecordsUseCase = GetTimeRecordsUseCase(timeRecordRepository)

    @Test
    fun executeTimeTrackingFunctionalityScenario(): Unit = runBlocking {
        val testUserId = UserId(UUID.randomUUID())
        userRepository.saveUser(
            User(testUserId, "tracking_${testUserId.value}", "Tracking Developer", "hash", UserRole.USER)
        )
        userSettingsRepository.updateSettings(
            UserSettings(
                userId = testUserId,
                autoStopEnabled = true,
                maxActiveDurationValue = 8,
                maxActiveDurationUnit = DurationUnit.hours,
                minDurationEnabled = true,
                minDurationValue = 60,
                minDurationUnit = DurationUnit.seconds
            )
        )

        val baseCategoryId = CategoryId(UUID.fromString("11111111-1111-1111-1111-111111111111"))

        val timerStartTime = Instant.now().minusSeconds(120)
        val startTimerResult = createRecordUseCase(
            userId = testUserId,
            categoryId = baseCategoryId,
            startTime = timerStartTime,
            endTime = null,
            description = "Разработка нового модуля"
        )
        assertTrue(startTimerResult.isSuccess)
        val activeRecord = startTimerResult.getOrNull()
        assertNotNull(activeRecord)
        assertNull(activeRecord?.endTime)
        assertEquals("Разработка нового модуля", activeRecord?.description)

        val userRecords = getTimeRecordsUseCase(testUserId)
        assertTrue(userRecords.any { it.id == activeRecord?.id && it.endTime == null })

        val invalidStopResult = updateRecordUseCase(
            recordId = activeRecord!!.id,
            requestUserId = testUserId,
            endTime = timerStartTime.plusSeconds(30)
        )
        assertTrue(invalidStopResult.isFailure)
        assertTrue(invalidStopResult.exceptionOrNull() is IllegalStateException)

        val validStopResult = updateRecordUseCase(
            recordId = activeRecord.id,
            requestUserId = testUserId,
            endTime = timerStartTime.plusSeconds(90)
        )
        assertTrue(validStopResult.isSuccess)
        val stoppedRecord = validStopResult.getOrNull()
        assertNotNull(stoppedRecord?.endTime)

        val manualStartTime = Instant.now().minusSeconds(7200)
        val manualEndTime = Instant.now().minusSeconds(3600)
        val manualRecordResult = createRecordUseCase(
            userId = testUserId,
            categoryId = baseCategoryId,
            startTime = manualStartTime,
            endTime = manualEndTime,
            description = "Ручное внесение задачи"
        )
        assertTrue(manualRecordResult.isSuccess)
        val manualRecord = manualRecordResult.getOrNull()
        assertNotNull(manualRecord)
        assertEquals(manualStartTime, manualRecord?.startTime)
        assertEquals(manualEndTime, manualRecord?.endTime)

        val invalidRangeResult = createRecordUseCase(
            userId = testUserId,
            categoryId = baseCategoryId,
            startTime = Instant.now(),
            endTime = Instant.now().minusSeconds(100),
            description = "Некорректный интервал"
        )
        assertTrue(invalidRangeResult.isFailure)
        assertTrue(invalidRangeResult.exceptionOrNull() is IllegalArgumentException)

        val deleteResult = deleteRecordUseCase(manualRecord!!.id, testUserId)
        assertTrue(deleteResult.isSuccess)

        val remainingRecords = getTimeRecordsUseCase(testUserId)
        assertFalse(remainingRecords.any { it.id == manualRecord.id })
    }
}
