package com.ppo.web.data.repository

import com.ppo.domain.model.Category
import com.ppo.domain.model.CategoryId
import com.ppo.domain.model.DescriptionVersion
import com.ppo.domain.model.RecordId
import com.ppo.domain.model.TimeRecord
import com.ppo.domain.model.User
import com.ppo.domain.model.UserId
import com.ppo.domain.model.UserRole
import com.ppo.domain.model.VersionId
import com.ppo.web.data.TestDatabaseFactory
import com.ppo.web.data.runner.RandomOrderRunner
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@RunWith(RandomOrderRunner::class)
class PostgresTimeRecordRepositoryTest {

    private lateinit var tempDbFile: File
    private lateinit var userRepo: PostgresUserRepository
    private lateinit var categoryRepo: PostgresCategoryRepository
    private lateinit var repository: PostgresTimeRecordRepository

    private lateinit var defaultUser: User
    private lateinit var defaultCategory: Category

    @Before
    fun setUp() = runTest {
        val (dataSource, file) = TestDatabaseFactory.createTestDataSource()
        tempDbFile = file
        userRepo = PostgresUserRepository(dataSource)
        categoryRepo = PostgresCategoryRepository(dataSource)
        repository = PostgresTimeRecordRepository(dataSource)

        defaultUser = User(
            id = UserId(UUID.randomUUID()),
            login = "rec_user_${UUID.randomUUID()}",
            name = "Record User",
            passwordHash = "hash",
            role = UserRole.USER
        )
        userRepo.saveUser(defaultUser)

        defaultCategory = Category(
            id = CategoryId(UUID.randomUUID()),
            userId = defaultUser.id,
            name = "Dev Category",
            colorHex = "#2563EB",
            isDefault = false
        )
        categoryRepo.saveCategory(defaultCategory)
    }

    @After
    fun tearDown() {
        tempDbFile.delete()
    }

    @Test
    fun saveRecord_activeTask_persistsWithNullEndTime() = runTest {
        val record = TimeRecord(
            id = RecordId(UUID.randomUUID()),
            userId = defaultUser.id,
            categoryId = defaultCategory.id,
            startTime = Instant.now().truncatedTo(ChronoUnit.SECONDS),
            endTime = null,
            description = "Active task"
        )

        val result = repository.saveRecord(record)

        assertTrue(result.isSuccess)
        val loaded = repository.getRecordById(record.id)
        assertNotNull(loaded)
        assertNull(loaded?.endTime)
        assertEquals("Active task", loaded?.description)
    }

    @Test
    fun saveRecord_completedTask_persistsWithEndTimestamp() = runTest {
        val now = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val record = TimeRecord(
            id = RecordId(UUID.randomUUID()),
            userId = defaultUser.id,
            categoryId = defaultCategory.id,
            startTime = now.minus(3600, ChronoUnit.SECONDS),
            endTime = now,
            description = "Completed task"
        )

        val result = repository.saveRecord(record)

        assertTrue(result.isSuccess)
        val loaded = repository.getRecordById(record.id)
        assertNotNull(loaded)
        assertEquals(now, loaded?.endTime)
    }

    @Test
    fun saveRecord_existingRecord_updatesFields() = runTest {
        val now = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val record = TimeRecord(
            id = RecordId(UUID.randomUUID()),
            userId = defaultUser.id,
            categoryId = defaultCategory.id,
            startTime = now.minus(3600, ChronoUnit.SECONDS),
            endTime = null,
            description = "Before update"
        )
        repository.saveRecord(record)
        val updated = record.copy(endTime = now, description = "After update")

        val result = repository.saveRecord(updated)

        assertTrue(result.isSuccess)
        val loaded = repository.getRecordById(record.id)
        assertEquals(now, loaded?.endTime)
        assertEquals("After update", loaded?.description)
    }

    @Test
    fun getActiveSession_whenActiveExists_returnsActiveRecord() = runTest {
        val now = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val completed = TimeRecord(
            id = RecordId(UUID.randomUUID()),
            userId = defaultUser.id,
            categoryId = defaultCategory.id,
            startTime = now.minus(7200, ChronoUnit.SECONDS),
            endTime = now.minus(3600, ChronoUnit.SECONDS),
            description = "Completed"
        )
        val active = TimeRecord(
            id = RecordId(UUID.randomUUID()),
            userId = defaultUser.id,
            categoryId = defaultCategory.id,
            startTime = now.minus(1800, ChronoUnit.SECONDS),
            endTime = null,
            description = "Active"
        )
        repository.saveRecord(completed)
        repository.saveRecord(active)

        val session = repository.getActiveSession(defaultUser.id)

        assertNotNull(session)
        assertEquals(active.id, session?.id)
    }

    @Test
    fun getActiveSession_whenNoActiveExists_returnsNull() = runTest {
        val now = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val completed = TimeRecord(
            id = RecordId(UUID.randomUUID()),
            userId = defaultUser.id,
            categoryId = defaultCategory.id,
            startTime = now.minus(7200, ChronoUnit.SECONDS),
            endTime = now.minus(3600, ChronoUnit.SECONDS),
            description = "Completed"
        )
        repository.saveRecord(completed)

        val session = repository.getActiveSession(defaultUser.id)

        assertNull(session)
    }

    @Test
    fun getOverlappingRecords_whenIntervalIntersects_returnsOverlappingList() = runTest {
        val baseTime = Instant.parse("2026-10-23T10:00:00Z")
        val record = TimeRecord(
            id = RecordId(UUID.randomUUID()),
            userId = defaultUser.id,
            categoryId = defaultCategory.id,
            startTime = baseTime,
            endTime = baseTime.plus(2, ChronoUnit.HOURS),
            description = "10:00 to 12:00"
        )
        repository.saveRecord(record)

        val overlapping = repository.getOverlappingRecords(
            userId = defaultUser.id,
            start = baseTime.plus(1, ChronoUnit.HOURS),
            end = baseTime.plus(3, ChronoUnit.HOURS),
            excludeRecordId = null
        )

        assertEquals(1, overlapping.size)
        assertEquals(record.id, overlapping.first().id)
    }

    @Test
    fun getOverlappingRecords_whenNoIntersection_returnsEmptyList() = runTest {
        val baseTime = Instant.parse("2026-10-23T10:00:00Z")
        val record = TimeRecord(
            id = RecordId(UUID.randomUUID()),
            userId = defaultUser.id,
            categoryId = defaultCategory.id,
            startTime = baseTime,
            endTime = baseTime.plus(2, ChronoUnit.HOURS),
            description = "10:00 to 12:00"
        )
        repository.saveRecord(record)

        val overlapping = repository.getOverlappingRecords(
            userId = defaultUser.id,
            start = baseTime.plus(3, ChronoUnit.HOURS),
            end = baseTime.plus(4, ChronoUnit.HOURS),
            excludeRecordId = null
        )

        assertTrue(overlapping.isEmpty())
    }

    @Test
    fun findRecords_withStatusAndPagination_returnsFilteredSliceAndTotal() = runTest {
        val now = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val rec1 = TimeRecord(RecordId(UUID.randomUUID()), defaultUser.id, defaultCategory.id, now.minus(3, ChronoUnit.HOURS), now.minus(2, ChronoUnit.HOURS), "Done 1")
        val rec2 = TimeRecord(RecordId(UUID.randomUUID()), defaultUser.id, defaultCategory.id, now.minus(2, ChronoUnit.HOURS), now.minus(1, ChronoUnit.HOURS), "Done 2")
        val rec3 = TimeRecord(RecordId(UUID.randomUUID()), defaultUser.id, defaultCategory.id, now.minus(30, ChronoUnit.MINUTES), null, "Active 3")
        repository.saveRecord(rec1)
        repository.saveRecord(rec2)
        repository.saveRecord(rec3)

        val (items, total) = repository.findRecords(
            userId = defaultUser.id,
            categoryId = null,
            status = "completed",
            from = null,
            to = null,
            limit = 1,
            offset = 0
        )

        assertEquals(2, total)
        assertEquals(1, items.size)
        assertNotNull(items.first().endTime)
    }

    @Test
    fun findRecords_withCategoryAndInterval_filtersCorrectly() = runTest {
        val baseTime = Instant.parse("2026-10-23T12:00:00Z")
        val rec1 = TimeRecord(RecordId(UUID.randomUUID()), defaultUser.id, defaultCategory.id, baseTime, baseTime.plusSeconds(3600), "Matching")
        repository.saveRecord(rec1)

        val (items, total) = repository.findRecords(
            userId = defaultUser.id,
            categoryId = defaultCategory.id,
            status = "completed",
            from = baseTime.minusSeconds(60),
            to = baseTime.plusSeconds(60),
            limit = 10,
            offset = 0
        )

        assertEquals(1, total)
        assertEquals(rec1.id, items.first().id)
    }

    @Test
    fun deleteRecord_existingRecord_removesFromDatabase() = runTest {
        val record = TimeRecord(
            id = RecordId(UUID.randomUUID()),
            userId = defaultUser.id,
            categoryId = defaultCategory.id,
            startTime = Instant.now().truncatedTo(ChronoUnit.SECONDS),
            endTime = null,
            description = "To delete"
        )
        repository.saveRecord(record)

        val result = repository.deleteRecord(record.id)

        assertTrue(result.isSuccess)
        val loaded = repository.getRecordById(record.id)
        assertNull(loaded)
    }

    @Test
    fun deleteRecord_missingRecord_returnsFailure() = runTest {
        val missingId = RecordId(UUID.randomUUID())

        val result = repository.deleteRecord(missingId)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NoSuchElementException)
    }

    @Test
    fun addDescriptionVersion_andGetDescriptionHistory_persistsAndReturnsSortedHistory() = runTest {
        val record = TimeRecord(
            id = RecordId(UUID.randomUUID()),
            userId = defaultUser.id,
            categoryId = defaultCategory.id,
            startTime = Instant.now().minus(3600, ChronoUnit.SECONDS).truncatedTo(ChronoUnit.SECONDS),
            endTime = Instant.now().truncatedTo(ChronoUnit.SECONDS),
            description = "Initial"
        )
        repository.saveRecord(record)
        val version = DescriptionVersion(
            id = VersionId(UUID.randomUUID()),
            recordId = record.id,
            description = "Updated description text",
            modifiedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        )

        val result = repository.addDescriptionVersion(version)

        assertTrue(result.isSuccess)
        val history = repository.getDescriptionHistory(record.id)
        assertEquals(1, history.size)
        assertEquals(version.id, history.first().id)
        assertEquals("Updated description text", history.first().description)
    }

    @Test
    fun getActiveRecords_andGetAllTimeRecords_returnsMatchingLists() = runTest {
        val activeRecord = TimeRecord(
            id = RecordId(UUID.randomUUID()),
            userId = defaultUser.id,
            categoryId = defaultCategory.id,
            startTime = Instant.now().minus(600, ChronoUnit.SECONDS).truncatedTo(ChronoUnit.SECONDS),
            endTime = null,
            description = "Active now"
        )
        repository.saveRecord(activeRecord)

        val activeList = repository.getActiveRecords(defaultUser.id)

        assertTrue(activeList.any { it.id == activeRecord.id })
        val allRecords = repository.getAllTimeRecords()
        assertTrue(allRecords.any { it.id == activeRecord.id })
    }
}
