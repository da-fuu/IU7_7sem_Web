package com.ppo.web.routes

import com.ppo.domain.model.RecordHistoryEntry
import com.ppo.domain.model.RecordId
import com.ppo.domain.model.UserRole
import com.ppo.domain.model.VersionId
import com.ppo.domain.repository.IRecordHistoryRepository
import com.ppo.domain.repository.ITimeRecordRepository
import com.ppo.web.dto.RecordHistoryListResponseDto
import com.ppo.web.dto.toDto
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.time.Instant
import java.util.UUID

fun Route.historyRoutes(
    timeRecordRepository: ITimeRecordRepository,
    recordHistoryRepository: IRecordHistoryRepository
) {
    route("/api/v1/records/{recordId}/history") {
        get {
            val authUser = call.getAuthUserOrRespond() ?: return@get
            val recIdStr = call.parameters["recordId"] ?: return@get call.respondError(
                HttpStatusCode.BadRequest, "BAD_REQUEST", "Параметр recordId обязателен"
            )
            val recordId = try {
                RecordId(UUID.fromString(recIdStr))
            } catch (e: Exception) {
                return@get call.respondError(HttpStatusCode.BadRequest, "BAD_REQUEST", "Некорректный UUID записи")
            }

            val record = timeRecordRepository.getRecordById(recordId)
            if (record == null) {
                return@get call.respondError(HttpStatusCode.NotFound, "RECORD_NOT_FOUND", "Запись времени '$recIdStr' не найдена")
            }

            if (record.userId != authUser.userId && authUser.role != UserRole.ADMIN) {
                return@get call.respondError(HttpStatusCode.Forbidden, "FORBIDDEN", "Доступ к чужой записи запрещен")
            }

            val history = recordHistoryRepository.getHistoryForRecord(recordId)
            call.respond(HttpStatusCode.OK, RecordHistoryListResponseDto(items = history.map { it.toDto() }))
        }

        post("/{versionId}/rollback") {
            val authUser = call.getAuthUserOrRespond() ?: return@post
            val recIdStr = call.parameters["recordId"] ?: return@post call.respondError(
                HttpStatusCode.BadRequest, "BAD_REQUEST", "Параметр recordId обязателен"
            )
            val verIdStr = call.parameters["versionId"] ?: return@post call.respondError(
                HttpStatusCode.BadRequest, "BAD_REQUEST", "Параметр versionId обязателен"
            )

            val recordId = try {
                RecordId(UUID.fromString(recIdStr))
            } catch (e: Exception) {
                return@post call.respondError(HttpStatusCode.BadRequest, "BAD_REQUEST", "Некорректный UUID записи")
            }
            val versionId = try {
                VersionId(UUID.fromString(verIdStr))
            } catch (e: Exception) {
                return@post call.respondError(HttpStatusCode.BadRequest, "BAD_REQUEST", "Некорректный UUID версии")
            }

            val record = timeRecordRepository.getRecordById(recordId)
            if (record == null) {
                return@post call.respondError(HttpStatusCode.NotFound, "RECORD_NOT_FOUND", "Запись '$recIdStr' не найдена")
            }

            if (record.userId != authUser.userId && authUser.role != UserRole.ADMIN) {
                return@post call.respondError(HttpStatusCode.Forbidden, "FORBIDDEN", "Доступ к чужой записи запрещен")
            }

            val version = recordHistoryRepository.getHistoryEntry(versionId)
            if (version == null || version.recordId != recordId) {
                return@post call.respondError(HttpStatusCode.NotFound, "VERSION_NOT_FOUND", "Историческая версия '$verIdStr' не найдена")
            }

            val currentSnapshot = RecordHistoryEntry(
                id = VersionId(UUID.randomUUID()),
                recordId = record.id,
                categoryId = record.categoryId,
                startTime = record.startTime,
                endTime = record.endTime,
                description = record.description,
                modifiedAt = Instant.now()
            )
            recordHistoryRepository.saveHistoryEntry(currentSnapshot)

            val rolledBack = record.copy(
                categoryId = version.categoryId,
                startTime = version.startTime,
                endTime = version.endTime,
                description = version.description
            )
            timeRecordRepository.saveRecord(rolledBack)
            call.respond(HttpStatusCode.OK, rolledBack.toDto())
        }
    }
}
