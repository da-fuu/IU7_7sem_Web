package com.ppo.web.routes

import com.ppo.domain.model.RecordId
import com.ppo.domain.model.UserRole
import com.ppo.domain.model.VersionId
import com.ppo.domain.repository.IRecordHistoryRepository
import com.ppo.domain.repository.ITimeRecordRepository
import com.ppo.domain.usecase.GetRecordHistoryUseCase
import com.ppo.domain.usecase.RollbackRecordHistoryUseCase
import com.ppo.web.dto.RecordHistoryListResponseDto
import com.ppo.web.dto.toDto
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID

fun Route.historyRoutes(
    timeRecordRepository: ITimeRecordRepository,
    recordHistoryRepository: IRecordHistoryRepository,
    getRecordHistoryUseCase: GetRecordHistoryUseCase = GetRecordHistoryUseCase(timeRecordRepository, recordHistoryRepository),
    rollbackRecordHistoryUseCase: RollbackRecordHistoryUseCase = RollbackRecordHistoryUseCase(timeRecordRepository, recordHistoryRepository)
) = historyRoutes(getRecordHistoryUseCase, rollbackRecordHistoryUseCase)

fun Route.historyRoutes(
    getRecordHistoryUseCase: GetRecordHistoryUseCase,
    rollbackRecordHistoryUseCase: RollbackRecordHistoryUseCase
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

            val result = getRecordHistoryUseCase(
                recordId = recordId,
                requestUserId = authUser.userId,
                isAdmin = authUser.role == UserRole.ADMIN
            )

            result.fold(
                onSuccess = { history ->
                    call.respond(HttpStatusCode.OK, RecordHistoryListResponseDto(items = history.map { it.toDto() }))
                },
                onFailure = { error ->
                    when (error) {
                        is NoSuchElementException -> call.respondError(HttpStatusCode.NotFound, "RECORD_NOT_FOUND", "Запись времени '$recIdStr' не найдена")
                        is IllegalAccessException -> call.respondError(HttpStatusCode.Forbidden, "FORBIDDEN", "Доступ к чужой записи запрещен")
                        else -> call.respondError(HttpStatusCode.InternalServerError, "INTERNAL_ERROR", error.message ?: "Ошибка")
                    }
                }
            )
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

            val result = rollbackRecordHistoryUseCase(
                recordId = recordId,
                versionId = versionId,
                requestUserId = authUser.userId,
                isAdmin = authUser.role == UserRole.ADMIN
            )

            result.fold(
                onSuccess = { rolledBack ->
                    call.respond(HttpStatusCode.OK, rolledBack.toDto())
                },
                onFailure = { error ->
                    when (error) {
                        is NoSuchElementException -> call.respondError(HttpStatusCode.NotFound, "VERSION_NOT_FOUND", "Историческая версия '$verIdStr' не найдена")
                        is IllegalAccessException -> call.respondError(HttpStatusCode.Forbidden, "FORBIDDEN", "Доступ к чужой записи запрещен")
                        is IllegalArgumentException -> call.respondError(HttpStatusCode.BadRequest, "BAD_REQUEST", error.message ?: "Некорректные параметры")
                        else -> call.respondError(HttpStatusCode.InternalServerError, "INTERNAL_ERROR", error.message ?: "Ошибка")
                    }
                }
            )
        }
    }
}
