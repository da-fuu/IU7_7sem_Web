package com.ppo.data

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ppo.data.db.AppDatabase
import com.ppo.data.repository.CategoryRepositoryImpl
import com.ppo.data.repository.TimeRecordRepositoryImpl
import com.ppo.data.repository.UserRepositoryImpl
import com.ppo.domain.model.Category
import com.ppo.domain.model.CategoryId
import com.ppo.domain.model.RecordId
import com.ppo.domain.model.TimeRecord
import com.ppo.domain.model.User
import com.ppo.domain.model.UserId
import com.ppo.domain.model.UserRole
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.util.UUID

class DataIntegrationTest {

    private lateinit var db: AppDatabase
    private lateinit var userRepository: UserRepositoryImpl
    private lateinit var categoryRepository: CategoryRepositoryImpl
    private lateinit var timeRecordRepository: TimeRecordRepositoryImpl

    @Before
    fun setup() {
        // Mock Looper for Android classes used by Room
        io.mockk.mockkStatic(android.os.Looper::class)
        val mockLooper = io.mockk.mockk<android.os.Looper>(relaxed = true)
        io.mockk.every { android.os.Looper.getMainLooper() } returns mockLooper

        val mockContext = io.mockk.mockk<android.content.Context>(relaxed = true)
        db = Room.inMemoryDatabaseBuilder(mockContext, AppDatabase::class.java)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(kotlinx.coroutines.Dispatchers.Unconfined)
            .build()

        userRepository = UserRepositoryImpl(db.userDao())
        categoryRepository = CategoryRepositoryImpl(db.categoryDao())
        timeRecordRepository = TimeRecordRepositoryImpl(db.timeRecordDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `save and find user should succeed`() = runTest {
        val user = User(
            id = UserId(UUID.randomUUID()),
            login = "testuser",
            name = "Test User",
            passwordHash = "hash",
            role = UserRole.USER
        )

        val result = userRepository.saveUser(user)
        assertTrue(result.isSuccess)

        val found = userRepository.findByLogin("testuser")
        assertNotNull(found)
        assertEquals(user.id, found?.id)
        assertEquals(user.name, found?.name)
    }

    @Test
    fun `save duplicate user login should fail`() = runTest {
        val user1 = User(
            id = UserId(UUID.randomUUID()),
            login = "duplicate",
            name = "User 1",
            passwordHash = "hash1",
            role = UserRole.USER
        )
        val user2 = User(
            id = UserId(UUID.randomUUID()),
            login = "duplicate",
            name = "User 2",
            passwordHash = "hash2",
            role = UserRole.USER
        )

        userRepository.saveUser(user1)
        val result = userRepository.saveUser(user2)

        assertTrue(result.isFailure)
    }

    @Test
    fun `overlapping records detection`() = runTest {
        val userId = UserId(UUID.randomUUID())
        val categoryId = CategoryId(UUID.randomUUID())

        userRepository.saveUser(User(userId, "user", "User", "hash", UserRole.USER))
        categoryRepository.saveCategory(Category(categoryId, userId, "Work", "#FF0000", false))

        val baseTime = Instant.parse("2026-05-15T10:00:00Z")

        val record1 = TimeRecord(
            id = RecordId(UUID.randomUUID()),
            userId = userId,
            categoryId = categoryId,
            startTime = baseTime,
            endTime = baseTime.plusSeconds(3600)
        )
        timeRecordRepository.saveRecord(record1)

        val overlaps1 = timeRecordRepository.getOverlappingRecords(
            userId = userId,
            start = baseTime.plusSeconds(1800),
            end = baseTime.plusSeconds(5400)
        )
        assertEquals(1, overlaps1.size)
        assertEquals(record1.id, overlaps1[0].id)

        val overlaps2 = timeRecordRepository.getOverlappingRecords(
            userId = userId,
            start = baseTime.minusSeconds(3600),
            end = baseTime
        )
        assertEquals(0, overlaps2.size)

        val overlaps3 = timeRecordRepository.getOverlappingRecords(
            userId = userId,
            start = baseTime.plusSeconds(1800),
            end = null
        )
        assertEquals(1, overlaps3.size)
    }
}
