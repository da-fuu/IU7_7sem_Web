package com.ppo.data.api

import com.ppo.domain.model.*
import com.ppo.domain.repository.ITimeRecordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID

class ApiTimeRecordRepository(
    private val client: RestApiClient
) : ITimeRecordRepository {
    override suspend fun saveRecord(record: TimeRecord): Result<Unit> = withContext(Dispatchers.IO) {
        val json = """{"categoryId":"${record.categoryId.value}","startTime":"${record.startTime}","endTime":${if (record.endTime != null) "\"${record.endTime}\"" else "null"},"description":"${record.description}"}"""
        val existing = getRecordById(record.id)
        val res = if (existing != null) {
            client.request("PUT", "/records/${record.id.value}", json)
        } else {
            client.request("POST", "/records", json)
        }
        if (res.statusCode in 200..299) Result.success(Unit) else Result.failure(Exception(res.body))
    }

    override suspend fun getRecordById(recordId: RecordId): TimeRecord? = withContext(Dispatchers.IO) {
        val res = client.request("GET", "/records/${recordId.value}")
        if (res.statusCode == 200) {
            parseRecordMap(JsonMini.parseObject(res.body))
        } else null
    }

    override suspend fun getTimeRecords(userId: UserId): List<TimeRecord> = withContext(Dispatchers.IO) {
        val res = client.request("GET", "/records?limit=100")
        if (res.statusCode == 200) {
            val root = JsonMini.parseObject(res.body)
            val items = root["items"] as? List<*> ?: return@withContext emptyList()
            items.mapNotNull { item ->
                val map = item as? Map<*, *> ?: return@mapNotNull null
                parseRecordMap(map)
            }
        } else emptyList()
    }

    override suspend fun getActiveSession(userId: UserId): TimeRecord? = withContext(Dispatchers.IO) {
        val res = client.request("GET", "/records?status=active")
        if (res.statusCode == 200) {
            val root = JsonMini.parseObject(res.body)
            val items = root["items"] as? List<*> ?: return@withContext null
            val first = items.firstOrNull() as? Map<*, *> ?: return@withContext null
            parseRecordMap(first)
        } else null
    }

    override suspend fun deleteRecord(recordId: RecordId): Result<Unit> = withContext(Dispatchers.IO) {
        val res = client.request("DELETE", "/records/${recordId.value}")
        if (res.statusCode in 200..299) Result.success(Unit) else Result.failure(Exception(res.body))
    }

    override suspend fun getDescriptionHistory(recordId: RecordId): List<DescriptionVersion> = withContext(Dispatchers.IO) {
        val res = client.request("GET", "/records/${recordId.value}/history")
        if (res.statusCode == 200) {
            val root = JsonMini.parseObject(res.body)
            val items = root["items"] as? List<*> ?: return@withContext emptyList()
            items.mapNotNull { item ->
                val map = item as? Map<*, *> ?: return@mapNotNull null
                val idStr = map["id"] as? String ?: return@mapNotNull null
                val desc = map["description"] as? String ?: ""
                val modAt = map["modifiedAt"] as? String ?: return@mapNotNull null
                DescriptionVersion(
                    id = VersionId(UUID.fromString(idStr)),
                    recordId = recordId,
                    description = desc,
                    modifiedAt = Instant.parse(modAt)
                )
            }
        } else emptyList()
    }

    override suspend fun addDescriptionVersion(version: DescriptionVersion): Result<Unit> = withContext(Dispatchers.IO) {
        Result.success(Unit)
    }

    override suspend fun getOverlappingRecords(
        userId: UserId,
        start: Instant,
        end: Instant?,
        excludeRecordId: RecordId?
    ): List<TimeRecord> = withContext(Dispatchers.IO) {
        emptyList()
    }

    override suspend fun getAllTimeRecords(): List<TimeRecord> = withContext(Dispatchers.IO) {
        emptyList()
    }

    private fun parseRecordMap(map: Map<*, *>): TimeRecord? {
        val idStr = map["id"] as? String ?: return null
        val userIdStr = map["userId"] as? String ?: return null
        val catIdStr = map["categoryId"] as? String ?: return null
        val startStr = map["startTime"] as? String ?: return null
        val endStr = map["endTime"] as? String
        val desc = map["description"] as? String ?: ""
        return TimeRecord(
            id = RecordId(UUID.fromString(idStr)),
            userId = UserId(UUID.fromString(userIdStr)),
            categoryId = CategoryId(UUID.fromString(catIdStr)),
            startTime = Instant.parse(startStr),
            endTime = endStr?.let { Instant.parse(it) },
            description = desc
        )
    }
}
