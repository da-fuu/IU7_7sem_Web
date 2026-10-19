package com.ppo.web

import com.ppo.domain.model.Category
import com.ppo.domain.model.CategoryId
import com.ppo.domain.model.UserId
import com.ppo.domain.model.UserSettings
import com.ppo.web.dto.RecordHistoryListResponseDto
import com.ppo.web.dto.TimeRecordDto
import com.ppo.web.routes.historyRoutes
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
import org.junit.Test
import java.util.UUID

class HistoryApiTest {
    @Test
    fun testHistorySnapshotAndRollback() = testApplication {
        val recordRepo = MockTimeRecordRepository()
        val catRepo = MockCategoryRepository()
        val settingsRepo = MockUserSettingsRepository()
        val historyRepo = MockRecordHistoryRepository()

        val userId = UserId(UUID.randomUUID())
        val catId = CategoryId(UUID.randomUUID())
        runBlocking {
            catRepo.saveCategory(Category(catId, userId, "Категория", "#3B82F6", false))
            settingsRepo.updateSettings(UserSettings(userId = userId, minDurationEnabled = false))
        }
        val token = SecurityUtils.createToken(userId.value.toString(), "alex", "USER")

        val client = setupTestApp {
            recordRoutes(recordRepo, catRepo, settingsRepo, historyRepo)
            historyRoutes(recordRepo, historyRepo)
        }

        val createRes = client.post("/api/v1/records") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"categoryId":"${catId.value}","description":"Оригинальное описание"}""")
        }
        val created: TimeRecordDto = createRes.body()

        client.patch("/api/v1/records/${created.id}") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"description":"Измененное описание 1"}""")
        }

        client.patch("/api/v1/records/${created.id}") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"description":"Измененное описание 2"}""")
        }

        val historyRes = client.get("/api/v1/records/${created.id}/history") {
            header("Authorization", "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, historyRes.status)
        val history: RecordHistoryListResponseDto = historyRes.body()
        assertEquals(2, history.items.size)
        assertEquals("Измененное описание 1", history.items[0].description)
        assertEquals("Оригинальное описание", history.items[1].description)

        val rollbackVerId = history.items[1].id
        val rollbackRes = client.post("/api/v1/records/${created.id}/history/$rollbackVerId/rollback") {
            header("Authorization", "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, rollbackRes.status)
        val rolledBack: TimeRecordDto = rollbackRes.body()
        assertEquals("Оригинальное описание", rolledBack.description)

        val afterRollbackHistory = client.get("/api/v1/records/${created.id}/history") {
            header("Authorization", "Bearer $token")
        }
        val historyAfter: RecordHistoryListResponseDto = afterRollbackHistory.body()
        assertEquals(3, historyAfter.items.size)
        assertEquals("Измененное описание 2", historyAfter.items[0].description)
    }
}
