package com.ppo.web

import com.ppo.domain.model.*
import com.ppo.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant

class MockUserRepository : IUserRepository {
    val users = mutableMapOf<UserId, User>()

    override suspend fun findByLogin(login: String): User? = users.values.find { it.login == login }
    override suspend fun saveUser(user: User): Result<Unit> {
        users[user.id] = user
        return Result.success(Unit)
    }
    override suspend fun getUserRole(userId: UserId): UserRole? = users[userId]?.role
    override suspend fun getUserById(userId: UserId): User? = users[userId]
    override suspend fun getAllUsers(): List<User> = users.values.toList()
}

class MockUserSettingsRepository : IUserSettingsRepository {
    val settings = mutableMapOf<UserId, UserSettings>()

    override suspend fun getSettings(userId: UserId): UserSettings {
        return settings.getOrPut(userId) { UserSettings(userId = userId) }
    }

    override suspend fun updateSettings(settings: UserSettings): Result<UserSettings> {
        this.settings[settings.userId] = settings
        return Result.success(settings)
    }
}

class MockCategoryRepository : ICategoryRepository {
    val categories = mutableMapOf<CategoryId, Category>()

    override fun getCategoriesFlow(userId: UserId): Flow<List<Category>> = flow {
        emit(categories.values.filter { it.isDefault || it.userId == userId })
    }

    override suspend fun saveCategory(category: Category): Result<Unit> {
        categories[category.id] = category
        return Result.success(Unit)
    }

    override suspend fun findCategoryByName(userId: UserId?, name: String): Category? {
        return categories.values.find {
            (it.isDefault || it.userId == userId) && it.name == name
        }
    }

    override suspend fun findCategoryById(categoryId: CategoryId): Category? = categories[categoryId]

    override suspend fun deleteCategory(categoryId: CategoryId): Result<Unit> {
        return if (categories.remove(categoryId) != null) Result.success(Unit)
        else Result.failure(NoSuchElementException())
    }

    override suspend fun getAllCategories(): List<Category> = categories.values.toList()
}

class MockTimeRecordRepository : ITimeRecordRepository {
    val records = mutableMapOf<RecordId, TimeRecord>()

    override suspend fun saveRecord(record: TimeRecord): Result<Unit> {
        records[record.id] = record
        return Result.success(Unit)
    }

    override suspend fun getRecordById(recordId: RecordId): TimeRecord? = records[recordId]

    override suspend fun getOverlappingRecords(
        userId: UserId,
        start: Instant,
        end: Instant?,
        excludeRecordId: RecordId?
    ): List<TimeRecord> = emptyList()

    override suspend fun getActiveSession(userId: UserId): TimeRecord? {
        return records.values.find { it.userId == userId && it.endTime == null }
    }

    override suspend fun getTimeRecords(userId: UserId): List<TimeRecord> {
        return records.values.filter { it.userId == userId }.sortedByDescending { it.startTime }
    }

    override suspend fun getDescriptionHistory(recordId: RecordId): List<DescriptionVersion> = emptyList()
    override suspend fun addDescriptionVersion(version: DescriptionVersion): Result<Unit> = Result.success(Unit)
    override suspend fun deleteRecord(recordId: RecordId): Result<Unit> {
        return if (records.remove(recordId) != null) Result.success(Unit)
        else Result.failure(NoSuchElementException())
    }
    override suspend fun getAllTimeRecords(): List<TimeRecord> = records.values.toList()
}

class MockRecordHistoryRepository : IRecordHistoryRepository {
    val history = mutableListOf<RecordHistoryEntry>()

    override suspend fun saveHistoryEntry(entry: RecordHistoryEntry): Result<Unit> {
        history.add(entry)
        return Result.success(Unit)
    }

    override suspend fun getHistoryForRecord(recordId: RecordId): List<RecordHistoryEntry> {
        return history.filter { it.recordId == recordId }.sortedByDescending { it.modifiedAt }
    }

    override suspend fun getHistoryEntry(versionId: VersionId): RecordHistoryEntry? {
        return history.find { it.id == versionId }
    }
}
