package com.ppo.web

import com.ppo.domain.model.*
import com.ppo.web.service.AutoStopWorker
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class AutoStopWorkerTest {

    @Test
    fun testAutoStopExpiredRecordsStopsOnlyOverdueTasks() = runBlocking {
        val userRepo = MockUserRepository()
        val settingsRepo = MockUserSettingsRepository()
        val recordRepo = MockTimeRecordRepository()
        val historyRepo = MockRecordHistoryRepository()

        val userId1 = UserId(UUID.randomUUID())
        val userId2 = UserId(UUID.randomUUID())

        userRepo.saveUser(User(userId1, "user1", "User One", "hash", UserRole.USER))
        userRepo.saveUser(User(userId2, "user2", "User Two", "hash", UserRole.USER))

        settingsRepo.updateSettings(
            UserSettings(
                userId = userId1,
                autoStopEnabled = true,
                maxActiveDurationValue = 2,
                maxActiveDurationUnit = DurationUnit.hours
            )
        )

        settingsRepo.updateSettings(
            UserSettings(
                userId = userId2,
                autoStopEnabled = false,
                maxActiveDurationValue = 1,
                maxActiveDurationUnit = DurationUnit.hours
            )
        )

        val catId = CategoryId(UUID.randomUUID())
        val now = Instant.now()

        val rec1Expired = TimeRecord(
            id = RecordId(UUID.randomUUID()),
            userId = userId1,
            categoryId = catId,
            startTime = now.minusSeconds(3 * 3600),
            endTime = null,
            description = "Long task user 1"
        )

        val rec2Active = TimeRecord(
            id = RecordId(UUID.randomUUID()),
            userId = userId1,
            categoryId = catId,
            startTime = now.minusSeconds(30 * 60),
            endTime = null,
            description = "Recent task user 1"
        )

        val rec3DisabledAutoStop = TimeRecord(
            id = RecordId(UUID.randomUUID()),
            userId = userId2,
            categoryId = catId,
            startTime = now.minusSeconds(5 * 3600),
            endTime = null,
            description = "Long task user 2"
        )

        recordRepo.saveRecord(rec1Expired)
        recordRepo.saveRecord(rec2Active)
        recordRepo.saveRecord(rec3DisabledAutoStop)

        val worker = AutoStopWorker(recordRepo, settingsRepo, historyRepo)
        val stoppedCount = worker.processExpiredRecords(now)

        assertEquals(1, stoppedCount)

        val updatedRec1 = recordRepo.getRecordById(rec1Expired.id)
        assertNotNull(updatedRec1)
        assertNotNull(updatedRec1!!.endTime)
        assertEquals(rec1Expired.startTime.plusSeconds(2 * 3600), updatedRec1.endTime)

        val updatedRec2 = recordRepo.getRecordById(rec2Active.id)
        assertNotNull(updatedRec2)
        assertNull(updatedRec2!!.endTime)

        val updatedRec3 = recordRepo.getRecordById(rec3DisabledAutoStop.id)
        assertNotNull(updatedRec3)
        assertNull(updatedRec3!!.endTime)

        val historyEntries = historyRepo.getHistoryForRecord(rec1Expired.id)
        assertEquals(1, historyEntries.size)
        assertEquals(rec1Expired.description, historyEntries[0].description)
        assertNull(historyEntries[0].endTime)
    }

    @Test
    fun testProcessExpiredRecordsForUserSpecific() = runBlocking {
        val settingsRepo = MockUserSettingsRepository()
        val recordRepo = MockTimeRecordRepository()
        val historyRepo = MockRecordHistoryRepository()

        val userId = UserId(UUID.randomUUID())
        val catId = CategoryId(UUID.randomUUID())
        val now = Instant.now()

        settingsRepo.updateSettings(
            UserSettings(
                userId = userId,
                autoStopEnabled = true,
                maxActiveDurationValue = 15,
                maxActiveDurationUnit = DurationUnit.minutes
            )
        )

        val expired = TimeRecord(
            id = RecordId(UUID.randomUUID()),
            userId = userId,
            categoryId = catId,
            startTime = now.minusSeconds(20 * 60),
            endTime = null,
            description = "Expired 20m ago"
        )
        recordRepo.saveRecord(expired)

        val worker = AutoStopWorker(recordRepo, settingsRepo, historyRepo)
        val stopped = worker.processExpiredRecordsForUser(userId, now)

        assertEquals(1, stopped)
        val stoppedRecord = recordRepo.getRecordById(expired.id)
        assertNotNull(stoppedRecord!!.endTime)
        assertEquals(expired.startTime.plusSeconds(15 * 60), stoppedRecord.endTime)
    }
}
