package com.ppo.web.it

import com.ppo.web.dto.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

class UiActionHelper(private val client: HttpClient) {

    suspend fun clickHealthCheck(): Pair<HttpStatusCode, Long> {
        val start = System.currentTimeMillis()
        val response = client.get("/health")
        val latency = System.currentTimeMillis() - start
        return Pair(response.status, latency)
    }

    suspend fun clickRegisterButton(
        login: String,
        name: String,
        pass: String,
        role: String = "USER"
    ): Pair<HttpStatusCode, AuthResponseDto> {
        val response = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(UserRegistrationRequestDto(login, name, pass, role))
        }
        val body: AuthResponseDto = response.body()
        return Pair(response.status, body)
    }

    suspend fun clickSaveSettingsButton(
        token: String,
        userId: String,
        request: UserSettingsUpdateRequestDto
    ): Pair<HttpStatusCode, UserSettingsDto> {
        val response = client.patch("/api/v1/users/$userId/settings") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        val body: UserSettingsDto = response.body()
        return Pair(response.status, body)
    }

    suspend fun clickCreateCategoryButton(
        token: String,
        name: String,
        colorHex: String
    ): Pair<HttpStatusCode, CategoryDto> {
        val response = client.post("/api/v1/categories") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(CategoryCreateRequestDto(name = name, colorHex = colorHex))
        }
        val body: CategoryDto = response.body()
        return Pair(response.status, body)
    }

    suspend fun clickStartTimerButton(
        token: String,
        categoryId: String,
        description: String
    ): Triple<HttpStatusCode, TimeRecordDto, Long> {
        val start = System.currentTimeMillis()
        val response = client.post("/api/v1/records") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(
                TimeRecordCreateRequestDto(
                    categoryId = categoryId,
                    startTime = java.time.Instant.now().toString(),
                    endTime = null,
                    description = description
                )
            )
        }
        val latency = System.currentTimeMillis() - start
        val body: TimeRecordDto = response.body()
        return Triple(response.status, body, latency)
    }

    suspend fun clickQueryActiveRecords(token: String): Pair<HttpStatusCode, PaginatedRecordsResponseDto> {
        val response = client.get("/api/v1/records?status=active") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        val body: PaginatedRecordsResponseDto = response.body()
        return Pair(response.status, body)
    }

    suspend fun clickStopTimerButton(
        token: String,
        recordId: String,
        endTime: String
    ): Pair<HttpStatusCode, TimeRecordDto> {
        val response = client.patch("/api/v1/records/$recordId") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(TimeRecordPatchRequestDto(endTime = endTime))
        }
        val body: TimeRecordDto = response.body()
        return Pair(response.status, body)
    }

    suspend fun clickUpdateRecordButton(
        token: String,
        recordId: String,
        categoryId: String,
        startTime: String,
        endTime: String?,
        description: String
    ): Pair<HttpStatusCode, TimeRecordDto> {
        val response = client.put("/api/v1/records/$recordId") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(
                TimeRecordUpdateRequestDto(
                    categoryId = categoryId,
                    startTime = startTime,
                    endTime = endTime,
                    description = description
                )
            )
        }
        val body: TimeRecordDto = response.body()
        return Pair(response.status, body)
    }

    suspend fun clickInspectHistoryButton(token: String, recordId: String): Pair<HttpStatusCode, RecordHistoryListResponseDto> {
        val response = client.get("/api/v1/records/$recordId/history") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        val body: RecordHistoryListResponseDto = response.body()
        return Pair(response.status, body)
    }

    suspend fun clickRollbackHistoryButton(
        token: String,
        recordId: String,
        historyId: String
    ): Pair<HttpStatusCode, TimeRecordDto> {
        val response = client.post("/api/v1/records/$recordId/history/$historyId/rollback") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        val body: TimeRecordDto = response.body()
        return Pair(response.status, body)
    }

    suspend fun clickDeleteRecordButton(token: String, recordId: String): HttpStatusCode {
        val response = client.delete("/api/v1/records/$recordId") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        return response.status
    }
}
