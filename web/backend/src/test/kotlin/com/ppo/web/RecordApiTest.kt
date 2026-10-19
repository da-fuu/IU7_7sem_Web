package com.ppo.web

import com.ppo.domain.model.Category
import com.ppo.domain.model.CategoryId
import com.ppo.domain.model.UserId
import com.ppo.domain.model.UserSettings
import com.ppo.web.dto.ErrorResponseDto
import com.ppo.web.dto.PaginatedRecordsResponseDto
import com.ppo.web.dto.TimeRecordDto
import com.ppo.web.routes.recordRoutes
import com.ppo.web.security.SecurityUtils
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.util.UUID

class RecordApiTest {
    @Test
    fun testStartActiveTaskSuccess() = testApplication {
        val recordRepo = MockTimeRecordRepository()
        val catRepo = MockCategoryRepository()
        val settingsRepo = MockUserSettingsRepository()
        val historyRepo = MockRecordHistoryRepository()

        val userId = UserId(UUID.randomUUID())
        val catId = CategoryId(UUID.randomUUID())
        runBlocking {
            catRepo.saveCategory(Category(catId, userId, "Разработка", "#3B82F6", false))
            settingsRepo.updateSettings(UserSettings(userId = userId))
        }
        val token = SecurityUtils.createToken(userId.value.toString(), "alex", "USER")

        val client = setupTestApp {
            recordRoutes(recordRepo, catRepo, settingsRepo, historyRepo)
        }

        val res = client.post("/api/v1/records") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"categoryId":"${catId.value}","description":"Разработка REST API"}""")
        }
        assertEquals(HttpStatusCode.Created, res.status)
        val record: TimeRecordDto = res.body()
        assertNotNull(record.id)
        assertNull(record.endTime)
        assertEquals("Разработка REST API", record.description)
    }

    @Test
    fun testParallelActiveTasksAllowed() = testApplication {
        val recordRepo = MockTimeRecordRepository()
        val catRepo = MockCategoryRepository()
        val settingsRepo = MockUserSettingsRepository()
        val historyRepo = MockRecordHistoryRepository()

        val userId = UserId(UUID.randomUUID())
        val catId = CategoryId(UUID.randomUUID())
        runBlocking {
            catRepo.saveCategory(Category(catId, userId, "Разработка", "#3B82F6", false))
            settingsRepo.updateSettings(UserSettings(userId = userId))
        }
        val token = SecurityUtils.createToken(userId.value.toString(), "alex", "USER")

        val client = setupTestApp {
            recordRoutes(recordRepo, catRepo, settingsRepo, historyRepo)
        }

        val res1 = client.post("/api/v1/records") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"categoryId":"${catId.value}","description":"Задача 1"}""")
        }
        assertEquals(HttpStatusCode.Created, res1.status)

        val res2 = client.post("/api/v1/records") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"categoryId":"${catId.value}","description":"Задача 2"}""")
        }
        assertEquals(HttpStatusCode.Created, res2.status)
    }

    @Test
    fun testStopActiveTaskByPatch() = testApplication {
        val recordRepo = MockTimeRecordRepository()
        val catRepo = MockCategoryRepository()
        val settingsRepo = MockUserSettingsRepository()
        val historyRepo = MockRecordHistoryRepository()

        val userId = UserId(UUID.randomUUID())
        val catId = CategoryId(UUID.randomUUID())
        runBlocking {
            catRepo.saveCategory(Category(catId, userId, "Разработка", "#3B82F6", false))
            settingsRepo.updateSettings(UserSettings(userId = userId))
        }
        val token = SecurityUtils.createToken(userId.value.toString(), "alex", "USER")

        val client = setupTestApp {
            recordRoutes(recordRepo, catRepo, settingsRepo, historyRepo)
        }

        val start = Instant.now().minusSeconds(1800).toString()
        val createRes = client.post("/api/v1/records") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"categoryId":"${catId.value}","startTime":"$start","description":"Активная"}""")
        }
        val created: TimeRecordDto = createRes.body()

        val end = Instant.now().toString()
        val patchRes = client.patch("/api/v1/records/${created.id}") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"endTime":"$end"}""")
        }
        assertEquals(HttpStatusCode.OK, patchRes.status)
        val patched: TimeRecordDto = patchRes.body()
        assertNotNull(patched.endTime)
    }

    @Test
    fun testInvalidTimeRangeReturns422() = testApplication {
        val recordRepo = MockTimeRecordRepository()
        val catRepo = MockCategoryRepository()
        val settingsRepo = MockUserSettingsRepository()
        val historyRepo = MockRecordHistoryRepository()

        val userId = UserId(UUID.randomUUID())
        val catId = CategoryId(UUID.randomUUID())
        runBlocking {
            catRepo.saveCategory(Category(catId, userId, "Разработка", "#3B82F6", false))
            settingsRepo.updateSettings(UserSettings(userId = userId))
        }
        val token = SecurityUtils.createToken(userId.value.toString(), "alex", "USER")

        val client = setupTestApp {
            recordRoutes(recordRepo, catRepo, settingsRepo, historyRepo)
        }

        val start = "2026-10-15T12:00:00Z"
        val end = "2026-10-15T11:00:00Z"
        val res = client.post("/api/v1/records") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"categoryId":"${catId.value}","startTime":"$start","endTime":"$end"}""")
        }
        assertEquals(HttpStatusCode.UnprocessableEntity, res.status)
        val err: ErrorResponseDto = res.body()
        assertEquals("INVALID_TIME_RANGE", err.code)
    }

    @Test
    fun testDurationTooShortReturns422() = testApplication {
        val recordRepo = MockTimeRecordRepository()
        val catRepo = MockCategoryRepository()
        val settingsRepo = MockUserSettingsRepository()
        val historyRepo = MockRecordHistoryRepository()

        val userId = UserId(UUID.randomUUID())
        val catId = CategoryId(UUID.randomUUID())
        runBlocking {
            catRepo.saveCategory(Category(catId, userId, "Разработка", "#3B82F6", false))
            settingsRepo.updateSettings(UserSettings(userId = userId, minDurationEnabled = true, minDurationValue = 1))
        }
        val token = SecurityUtils.createToken(userId.value.toString(), "alex", "USER")

        val client = setupTestApp {
            recordRoutes(recordRepo, catRepo, settingsRepo, historyRepo)
        }

        val start = "2026-10-15T12:00:00Z"
        val end = "2026-10-15T12:00:20Z"
        val res = client.post("/api/v1/records") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"categoryId":"${catId.value}","startTime":"$start","endTime":"$end"}""")
        }
        assertEquals(HttpStatusCode.UnprocessableEntity, res.status)
        val err: ErrorResponseDto = res.body()
        assertEquals("DURATION_TOO_SHORT", err.code)
    }

    @Test
    fun testListRecordsPagination() = testApplication {
        val recordRepo = MockTimeRecordRepository()
        val catRepo = MockCategoryRepository()
        val settingsRepo = MockUserSettingsRepository()
        val historyRepo = MockRecordHistoryRepository()

        val userId = UserId(UUID.randomUUID())
        val catId = CategoryId(UUID.randomUUID())
        runBlocking {
            catRepo.saveCategory(Category(catId, userId, "Разработка", "#3B82F6", false))
            settingsRepo.updateSettings(UserSettings(userId = userId, minDurationEnabled = false))
        }
        val token = SecurityUtils.createToken(userId.value.toString(), "alex", "USER")

        val client = setupTestApp {
            recordRoutes(recordRepo, catRepo, settingsRepo, historyRepo)
        }

        client.post("/api/v1/records") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"categoryId":"${catId.value}","description":"R1"}""")
        }
        client.post("/api/v1/records") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"categoryId":"${catId.value}","description":"R2"}""")
        }

        val res = client.get("/api/v1/records?limit=1&offset=0") {
            header("Authorization", "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, res.status)
        val page: PaginatedRecordsResponseDto = res.body()
        assertEquals(1, page.items.size)
        assertEquals(2, page.total)
    }
}
