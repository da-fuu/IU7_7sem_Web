package com.ppo.web.data.repository

import com.ppo.domain.model.Category
import com.ppo.domain.model.CategoryId
import com.ppo.domain.model.RecordHistoryEntry
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
class PostgresRecordHistoryRepositoryTest {

    private lateinit var tempDbFile: File
    private lateinit var userRepo: PostgresUserRepository
    private lateinit var categoryRepo: PostgresCategoryRepository
    private lateinit var timeRecordRepo: PostgresTimeRecordRepository
    private lateinit var repository: PostgresRecordHistoryRepository

    private lateinit var defaultRecord: TimeRecord

    @Before
    fun setUp() = runTest {
        val (dataSource, file) = TestDatabaseFactory.createTestDataSource()
        tempDbFile = file
        userRepo = PostgresUserRepository(dataSource)
        categoryRepo = PostgresCategoryRepository(dataSource)
        timeRecordRepo = PostgresTimeRecordRepository(dataSource)
        repository = PostgresRecordHistoryRepository(dataSource)

        val user = User(
            id = UserId(UUID.randomUUID()),
            login = "hist_user_${UUID.randomUUID()}",
            name = "History User",
            passwordHash = "hash",
            role = UserRole.USER
        )
        userRepo.saveUser(user)

        val category = Category(
            id = CategoryId(UUID.randomUUID()),
            userId = user.id,
            name = "Work",
            colorHex = "#10B981",
            isDefault = false
        )
        categoryRepo.saveCategory(category)

        defaultRecord = TimeRecord(
            id = RecordId(UUID.randomUUID()),
            userId = user.id,
            categoryId = category.id,
            startTime = Instant.now().minus(7200, ChronoUnit.SECONDS).truncatedTo(ChronoUnit.SECONDS),
            endTime = Instant.now().minus(3600, ChronoUnit.SECONDS).truncatedTo(ChronoUnit.SECONDS),
            description = "Initial description"
        )
        timeRecordRepo.saveRecord(defaultRecord)
    }

    @After
    fun tearDown() {
        tempDbFile.delete()
    }

    @Test
    fun saveHistoryEntry_andGetHistoryForRecord_orderedByModifiedAtDescending() = runTest {
        val t1 = Instant.now().minus(1800, ChronoUnit.SECONDS).truncatedTo(ChronoUnit.SECONDS)
        val t2 = Instant.now().minus(900, ChronoUnit.SECONDS).truncatedTo(ChronoUnit.SECONDS)
        val entry1 = RecordHistoryEntry(
            id = VersionId(UUID.randomUUID()),
            recordId = defaultRecord.id,
            categoryId = defaultRecord.categoryId,
            startTime = defaultRecord.startTime,
            endTime = defaultRecord.endTime,
            description = "Version 1",
            modifiedAt = t1
        )
        val entry2 = RecordHistoryEntry(
            id = VersionId(UUID.randomUUID()),
            recordId = defaultRecord.id,
            categoryId = defaultRecord.categoryId,
            startTime = defaultRecord.startTime,
            endTime = defaultRecord.endTime,
            description = "Version 2",
            modifiedAt = t2
        )
        repository.saveHistoryEntry(entry1)
        repository.saveHistoryEntry(entry2)

        val history = repository.getHistoryForRecord(defaultRecord.id)

        assertEquals(2, history.size)
        assertEquals(entry2.id, history[0].id)
        assertEquals("Version 2", history[0].description)
        assertEquals(entry1.id, history[1].id)
        assertEquals("Version 1", history[1].description)
    }

    @Test
    fun getHistoryEntry_existingVersionId_returnsSnapshot() = runTest {
        val entry = RecordHistoryEntry(
            id = VersionId(UUID.randomUUID()),
            recordId = defaultRecord.id,
            categoryId = defaultRecord.categoryId,
            startTime = defaultRecord.startTime,
            endTime = defaultRecord.endTime,
            description = "Snapshot description",
            modifiedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        )
        repository.saveHistoryEntry(entry)

        val loaded = repository.getHistoryEntry(entry.id)

        assertNotNull(loaded)
        assertEquals(entry.id, loaded?.id)
        assertEquals(entry.recordId, loaded?.recordId)
        assertEquals("Snapshot description", loaded?.description)
    }

    @Test
    fun getHistoryEntry_missingVersionId_returnsNull() = runTest {
        val missingVersionId = VersionId(UUID.randomUUID())

        val loaded = repository.getHistoryEntry(missingVersionId)

        assertNull(loaded)
    }

    @Test
    fun saveHistoryEntry_existingEntry_updatesSnapshot() = runTest {
        val entry = RecordHistoryEntry(
            id = VersionId(UUID.randomUUID()),
            recordId = defaultRecord.id,
            categoryId = defaultRecord.categoryId,
            startTime = defaultRecord.startTime,
            endTime = defaultRecord.endTime,
            description = "Initial entry",
            modifiedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        )
        repository.saveHistoryEntry(entry)
        val updatedEntry = entry.copy(description = "Updated entry text")

        val result = repository.saveHistoryEntry(updatedEntry)

        assertTrue(result.isSuccess)
        val loaded = repository.getHistoryEntry(entry.id)
        assertEquals("Updated entry text", loaded?.description)
    }
}
