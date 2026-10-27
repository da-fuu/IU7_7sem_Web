package com.ppo.web.it

import com.ppo.domain.model.*
import com.ppo.domain.usecase.CreateTimeRecordUseCase
import com.ppo.domain.usecase.GetRecordHistoryUseCase
import com.ppo.domain.usecase.RollbackRecordHistoryUseCase
import com.ppo.domain.usecase.UpdateTimeRecordUseCase
import com.ppo.web.data.repository.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Rule
import java.time.Instant
import java.util.UUID

class RecordHistoryAuditITCase {

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
    private val getRecordHistoryUseCase = GetRecordHistoryUseCase(timeRecordRepository, recordHistoryRepository)
    private val rollbackRecordHistoryUseCase = RollbackRecordHistoryUseCase(timeRecordRepository, recordHistoryRepository)

    @Test
    fun executeRecordHistoryAuditAndForwardOnlyRollbackScenario(): Unit = runBlocking {
        val testUserId = UserId(UUID.randomUUID())
        val strangerUserId = UserId(UUID.randomUUID())

        userRepository.saveUser(User(testUserId, "audit_${testUserId.value}", "Audit Author", "hash", UserRole.USER))
        userRepository.saveUser(User(strangerUserId, "stranger_${strangerUserId.value}", "Stranger", "hash", UserRole.USER))

        val baseCategoryId = CategoryId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
        val startTime = Instant.now().minusSeconds(3600)
        val endTime = Instant.now().minusSeconds(1800)

        val createResult = createRecordUseCase(
            userId = testUserId,
            categoryId = baseCategoryId,
            startTime = startTime,
            endTime = endTime,
            description = "Первоначальное описание задачи"
        )
        assertTrue(createResult.isSuccess)
        val record = createResult.getOrNull()
        assertNotNull(record)

        val firstUpdateResult = updateRecordUseCase(
            recordId = record!!.id,
            requestUserId = testUserId,
            description = "Вторая редакция описания"
        )
        assertTrue(firstUpdateResult.isSuccess)
        assertEquals("Вторая редакция описания", firstUpdateResult.getOrNull()?.description)

        val historyAfterFirstUpdate = getRecordHistoryUseCase(record.id, testUserId)
        assertTrue(historyAfterFirstUpdate.isSuccess)
        val historyList1 = historyAfterFirstUpdate.getOrNull()
        assertNotNull(historyList1)
        assertEquals(1, historyList1?.size)
        val initialVersion = historyList1!!.first()
        assertEquals("Первоначальное описание задачи", initialVersion.description)

        val secondUpdateResult = updateRecordUseCase(
            recordId = record.id,
            requestUserId = testUserId,
            description = "Третья редакция описания"
        )
        assertTrue(secondUpdateResult.isSuccess)
        assertEquals("Третья редакция описания", secondUpdateResult.getOrNull()?.description)

        val historyAfterSecondUpdate = getRecordHistoryUseCase(record.id, testUserId)
        val historyList2 = historyAfterSecondUpdate.getOrNull()
        assertNotNull(historyList2)
        assertEquals(2, historyList2?.size)

        val unauthorizedRollbackResult = rollbackRecordHistoryUseCase(
            recordId = record.id,
            versionId = initialVersion.id,
            requestUserId = strangerUserId
        )
        assertTrue(unauthorizedRollbackResult.isFailure)
        assertTrue(unauthorizedRollbackResult.exceptionOrNull() is IllegalAccessException)

        val rollbackResult = rollbackRecordHistoryUseCase(
            recordId = record.id,
            versionId = initialVersion.id,
            requestUserId = testUserId
        )
        assertTrue(rollbackResult.isSuccess)
        val rolledBackRecord = rollbackResult.getOrNull()
        assertNotNull(rolledBackRecord)
        assertEquals("Первоначальное описание задачи", rolledBackRecord?.description)

        val historyAfterRollback = getRecordHistoryUseCase(record.id, testUserId)
        val historyList3 = historyAfterRollback.getOrNull()
        assertNotNull(historyList3)
        assertEquals(3, historyList3?.size)
    }
}
