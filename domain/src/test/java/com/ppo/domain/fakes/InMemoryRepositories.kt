package com.ppo.domain.fakes

import com.ppo.domain.model.*
import com.ppo.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class InMemoryUserRepository : IUserRepository {
    val users = ConcurrentHashMap<UserId, User>()

    override suspend fun findByLogin(login: String): User? =
        users.values.find { it.login == login }

    override suspend fun saveUser(user: User): Result<Unit> {
        users[user.id] = user
        return Result.success(Unit)
    }

    override suspend fun getUserRole(userId: UserId): UserRole? =
        users[userId]?.role

    override suspend fun getUserById(userId: UserId): User? =
        users[userId]

    override suspend fun getAllUsers(): List<User> =
        users.values.toList()
}

class InMemoryUserSettingsRepository : IUserSettingsRepository {
    val settingsMap = ConcurrentHashMap<UserId, UserSettings>()

    override suspend fun getSettings(userId: UserId): UserSettings =
        settingsMap[userId] ?: UserSettings(userId = userId)

    override suspend fun updateSettings(settings: UserSettings): Result<UserSettings> {
        settingsMap[settings.userId] = settings
        return Result.success(settings)
    }
}

class InMemoryCategoryRepository : ICategoryRepository {
    val categories = ConcurrentHashMap<CategoryId, Category>()

    override fun getCategoriesFlow(userId: UserId): Flow<List<Category>> =
        flowOf(categories.values.filter { it.isDefault || it.userId == userId })

    override suspend fun saveCategory(category: Category): Result<Unit> {
        categories[category.id] = category
        return Result.success(Unit)
    }

    override suspend fun findCategoryByName(userId: UserId?, name: String): Category? =
        categories.values.find { it.userId == userId && it.name == name }

    override suspend fun findCategoryById(categoryId: CategoryId): Category? =
        categories[categoryId]

    override suspend fun deleteCategory(categoryId: CategoryId): Result<Unit> {
        categories.remove(categoryId)
        return Result.success(Unit)
    }

    override suspend fun getAllCategories(): List<Category> =
        categories.values.toList()
}

class InMemoryTimeRecordRepository : ITimeRecordRepository {
    val records = ConcurrentHashMap<RecordId, TimeRecord>()
    val history = mutableListOf<RecordHistoryEntry>()

    override suspend fun saveRecord(record: TimeRecord): Result<Unit> {
        records[record.id] = record
        return Result.success(Unit)
    }

    override suspend fun getRecordById(recordId: RecordId): TimeRecord? =
        records[recordId]

    override suspend fun getTimeRecords(userId: UserId): List<TimeRecord> =
        records.values.filter { it.userId == userId }

    override suspend fun deleteRecord(recordId: RecordId): Result<Unit> {
        records.remove(recordId)
        return Result.success(Unit)
    }

    override suspend fun getAllTimeRecords(): List<TimeRecord> =
        records.values.toList()

    override suspend fun getOverlappingRecords(
        userId: UserId,
        start: Instant,
        end: Instant?,
        excludeRecordId: RecordId?
    ): List<TimeRecord> {
        return records.values.filter { r ->
            r.userId == userId &&
                    (excludeRecordId == null || r.id != excludeRecordId) &&
                    r.endTime != null &&
                    end != null &&
                    !(r.endTime.isBefore(start) || r.startTime.isAfter(end))
        }
    }

    override suspend fun getDescriptionHistory(recordId: RecordId): List<RecordHistoryEntry> =
        history.filter { it.recordId == recordId }

    override suspend fun addDescriptionVersion(version: RecordHistoryEntry): Result<Unit> {
        history.add(version)
        return Result.success(Unit)
    }
}

class InMemoryRecordHistoryRepository : IRecordHistoryRepository {
    val entries = ConcurrentHashMap<VersionId, RecordHistoryEntry>()

    override suspend fun saveHistoryEntry(entry: RecordHistoryEntry): Result<Unit> {
        entries[entry.id] = entry
        return Result.success(Unit)
    }

    override suspend fun getHistoryForRecord(recordId: RecordId): List<RecordHistoryEntry> =
        entries.values.filter { it.recordId == recordId }.sortedByDescending { it.modifiedAt }

    override suspend fun getHistoryEntry(versionId: VersionId): RecordHistoryEntry? =
        entries[versionId]
}

class SimplePasswordHasher : IPasswordHasher {
    override fun hash(rawPassword: String): String = "hashed_$rawPassword"
    override fun verify(rawPassword: String, hashed: String): Boolean =
        hashed == "hashed_$rawPassword"
}
