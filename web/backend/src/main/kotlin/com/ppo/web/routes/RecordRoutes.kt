package com.ppo.web.routes

import com.ppo.domain.model.*
import com.ppo.domain.repository.ICategoryRepository
import com.ppo.domain.repository.IRecordHistoryRepository
import com.ppo.domain.repository.ITimeRecordRepository
import com.ppo.domain.repository.IUserSettingsRepository
import com.ppo.domain.usecase.*
import com.ppo.web.data.repository.PostgresTimeRecordRepository
import com.ppo.web.dto.*
import com.ppo.web.service.AutoStopWorker
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import java.time.Instant
import java.util.UUID

fun Route.recordRoutes(
    timeRecordRepository: ITimeRecordRepository,
    categoryRepository: ICategoryRepository,
    userSettingsRepository: IUserSettingsRepository,
    recordHistoryRepository: IRecordHistoryRepository,
    createTimeRecordUseCase: CreateTimeRecordUseCase = CreateTimeRecordUseCase(timeRecordRepository, categoryRepository, userSettingsRepository),
    updateTimeRecordUseCase: UpdateTimeRecordUseCase = UpdateTimeRecordUseCase(timeRecordRepository, categoryRepository, userSettingsRepository, recordHistoryRepository),
    deleteRecordUseCase: DeleteRecordUseCase = DeleteRecordUseCase(timeRecordRepository),
    autoStopWorker: AutoStopWorker = AutoStopWorker(AutoStopExpiredRecordsUseCase(timeRecordRepository, userSettingsRepository, recordHistoryRepository))
) {
    route("/api/v1/records") {
        get {
            val authUser = call.getAuthUserOrRespond() ?: return@get
            autoStopWorker.processExpiredRecordsForUser(authUser.userId)
            val categoryIdParam = call.request.queryParameters["categoryId"]?.let {
                try { CategoryId(UUID.fromString(it)) } catch (e: Exception) { null }
            }
            val statusParam = call.request.queryParameters["status"]
            val fromParam = call.request.queryParameters["from"]?.let {
                try { Instant.parse(it) } catch (e: Exception) { null }
            }
            val toParam = call.request.queryParameters["to"]?.let {
                try { Instant.parse(it) } catch (e: Exception) { null }
            }
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
            val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0

            val (records, total) = if (timeRecordRepository is PostgresTimeRecordRepository) {
                timeRecordRepository.findRecords(
                    userId = authUser.userId,
                    categoryId = categoryIdParam,
                    status = statusParam,
                    from = fromParam,
                    to = toParam,
                    limit = limit,
                    offset = offset
                )
            } else {
                var all = timeRecordRepository.getTimeRecords(authUser.userId)
                if (categoryIdParam != null) all = all.filter { it.categoryId == categoryIdParam }
                if (statusParam != null) {
                    when (statusParam.lowercase()) {
                        "active" -> all = all.filter { it.endTime == null }
                        "completed" -> all = all.filter { it.endTime != null }
                    }
                }
                if (fromParam != null) all = all.filter { it.startTime >= fromParam }
                if (toParam != null) all = all.filter { it.startTime <= toParam }
                Pair(all.drop(offset).take(limit), all.size)
            }

            call.respond(
                HttpStatusCode.OK,
                PaginatedRecordsResponseDto(
                    items = records.map { it.toDto() },
                    total = total,
                    limit = limit,
                    offset = offset
                )
            )
        }

        post {
            val authUser = call.getAuthUserOrRespond() ?: return@post
            val req = try {
                call.receive<TimeRecordCreateRequestDto>()
            } catch (e: Exception) {
                return@post call.respondError(HttpStatusCode.BadRequest, "BAD_REQUEST", "Некорректный JSON")
            }

            val categoryId = try {
                CategoryId(UUID.fromString(req.categoryId))
            } catch (e: Exception) {
                return@post call.respondError(HttpStatusCode.BadRequest, "BAD_REQUEST", "Некорректный UUID категории")
            }

            val startTime = if (req.startTime != null) {
                try { Instant.parse(req.startTime) } catch (e: Exception) {
                    return@post call.respondError(HttpStatusCode.BadRequest, "BAD_REQUEST", "Некорректный ISO-8601 формат startTime")
                }
            } else Instant.now()

            val endTime = if (req.endTime != null) {
                try { Instant.parse(req.endTime) } catch (e: Exception) {
                    return@post call.respondError(HttpStatusCode.BadRequest, "BAD_REQUEST", "Некорректный ISO-8601 формат endTime")
                }
            } else null

            autoStopWorker.processExpiredRecordsForUser(authUser.userId)

            val result = createTimeRecordUseCase(
                userId = authUser.userId,
                categoryId = categoryId,
                startTime = startTime,
                endTime = endTime,
                description = req.description ?: ""
            )

            result.fold(
                onSuccess = { call.respond(HttpStatusCode.Created, it.toDto()) },
                onFailure = { error ->
                    when (error) {
                        is NoSuchElementException -> call.respondError(HttpStatusCode.NotFound, "CATEGORY_NOT_FOUND", "Категория '${req.categoryId}' не найдена")
                        is IllegalAccessException -> call.respondError(HttpStatusCode.Forbidden, "FORBIDDEN", error.message ?: "Доступ запрещен")
                        is IllegalArgumentException -> call.respondError(HttpStatusCode.UnprocessableEntity, "INVALID_TIME_RANGE", "Время окончания должно быть строго позже времени начала")
                        is IllegalStateException -> call.respondError(HttpStatusCode.UnprocessableEntity, "DURATION_TOO_SHORT", error.message ?: "Длительность менее минимального порога")
                        else -> call.respondError(HttpStatusCode.InternalServerError, "INTERNAL_ERROR", error.message ?: "Внутренняя ошибка")
                    }
                }
            )
        }

        route("/{recordId}") {
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

                call.respond(HttpStatusCode.OK, record.toDto())
            }

            put {
                val authUser = call.getAuthUserOrRespond() ?: return@put
                val recIdStr = call.parameters["recordId"] ?: return@put call.respondError(
                    HttpStatusCode.BadRequest, "BAD_REQUEST", "Параметр recordId обязателен"
                )
                val recordId = try {
                    RecordId(UUID.fromString(recIdStr))
                } catch (e: Exception) {
                    return@put call.respondError(HttpStatusCode.BadRequest, "BAD_REQUEST", "Некорректный UUID записи")
                }

                val req = try {
                    call.receive<TimeRecordUpdateRequestDto>()
                } catch (e: Exception) {
                    return@put call.respondError(HttpStatusCode.BadRequest, "BAD_REQUEST", "Некорректный JSON")
                }

                val categoryId = try {
                    CategoryId(UUID.fromString(req.categoryId))
                } catch (e: Exception) {
                    return@put call.respondError(HttpStatusCode.BadRequest, "BAD_REQUEST", "Некорректный UUID категории")
                }

                val startTime = try { Instant.parse(req.startTime) } catch (e: Exception) {
                    return@put call.respondError(HttpStatusCode.BadRequest, "BAD_REQUEST", "Некорректный формат startTime")
                }

                val endTime = if (req.endTime != null) {
                    try { Instant.parse(req.endTime) } catch (e: Exception) {
                        return@put call.respondError(HttpStatusCode.BadRequest, "BAD_REQUEST", "Некорректный формат endTime")
                    }
                } else null

                val result = updateTimeRecordUseCase(
                    recordId = recordId,
                    requestUserId = authUser.userId,
                    isAdmin = authUser.role == UserRole.ADMIN,
                    categoryId = categoryId,
                    startTime = startTime,
                    endTime = endTime,
                    description = req.description
                )

                result.fold(
                    onSuccess = { call.respond(HttpStatusCode.OK, it.toDto()) },
                    onFailure = { error ->
                        when (error) {
                            is NoSuchElementException -> {
                                if (error.message?.contains("Category") == true) {
                                    call.respondError(HttpStatusCode.NotFound, "CATEGORY_NOT_FOUND", "Категория '${req.categoryId}' не найдена")
                                } else {
                                    call.respondError(HttpStatusCode.NotFound, "RECORD_NOT_FOUND", "Запись '$recIdStr' не найдена")
                                }
                            }
                            is IllegalAccessException -> call.respondError(HttpStatusCode.Forbidden, "FORBIDDEN", "Доступ к чужой записи запрещен")
                            is IllegalArgumentException -> call.respondError(HttpStatusCode.UnprocessableEntity, "INVALID_TIME_RANGE", "Время окончания должно быть строго позже времени начала")
                            is IllegalStateException -> call.respondError(HttpStatusCode.UnprocessableEntity, "DURATION_TOO_SHORT", error.message ?: "Длительность менее минимального порога")
                            else -> call.respondError(HttpStatusCode.InternalServerError, "INTERNAL_ERROR", error.message ?: "Внутренняя ошибка")
                        }
                    }
                )
            }

            patch {
                val authUser = call.getAuthUserOrRespond() ?: return@patch
                val recIdStr = call.parameters["recordId"] ?: return@patch call.respondError(
                    HttpStatusCode.BadRequest, "BAD_REQUEST", "Параметр recordId обязателен"
                )
                val recordId = try {
                    RecordId(UUID.fromString(recIdStr))
                } catch (e: Exception) {
                    return@patch call.respondError(HttpStatusCode.BadRequest, "BAD_REQUEST", "Некорректный UUID записи")
                }

                val req = try {
                    call.receive<TimeRecordPatchRequestDto>()
                } catch (e: Exception) {
                    return@patch call.respondError(HttpStatusCode.BadRequest, "BAD_REQUEST", "Некорректный JSON")
                }

                val newCategoryId = if (req.categoryId != null) {
                    try {
                        CategoryId(UUID.fromString(req.categoryId))
                    } catch (e: Exception) {
                        return@patch call.respondError(HttpStatusCode.BadRequest, "BAD_REQUEST", "Некорректный UUID категории")
                    }
                } else null

                val newStartTime = if (req.startTime != null) {
                    try { Instant.parse(req.startTime) } catch (e: Exception) {
                        return@patch call.respondError(HttpStatusCode.BadRequest, "BAD_REQUEST", "Некорректный формат startTime")
                    }
                } else null

                val newEndTime = if (req.endTime != null) {
                    try { Instant.parse(req.endTime) } catch (e: Exception) {
                        return@patch call.respondError(HttpStatusCode.BadRequest, "BAD_REQUEST", "Некорректный формат endTime")
                    }
                } else null

                val result = updateTimeRecordUseCase(
                    recordId = recordId,
                    requestUserId = authUser.userId,
                    isAdmin = authUser.role == UserRole.ADMIN,
                    categoryId = newCategoryId,
                    startTime = newStartTime,
                    endTime = newEndTime,
                    description = req.description
                )

                result.fold(
                    onSuccess = { call.respond(HttpStatusCode.OK, it.toDto()) },
                    onFailure = { error ->
                        when (error) {
                            is NoSuchElementException -> {
                                if (error.message?.contains("Category") == true) {
                                    call.respondError(HttpStatusCode.NotFound, "CATEGORY_NOT_FOUND", "Указанная категория не найдена")
                                } else {
                                    call.respondError(HttpStatusCode.NotFound, "RECORD_NOT_FOUND", "Запись '$recIdStr' не найдена")
                                }
                            }
                            is IllegalAccessException -> call.respondError(HttpStatusCode.Forbidden, "FORBIDDEN", "Доступ к чужой записи запрещен")
                            is IllegalArgumentException -> call.respondError(HttpStatusCode.UnprocessableEntity, "INVALID_TIME_RANGE", "Время окончания должно быть строго позже времени начала")
                            is IllegalStateException -> call.respondError(HttpStatusCode.UnprocessableEntity, "DURATION_TOO_SHORT", error.message ?: "Длительность менее минимального порога")
                            else -> call.respondError(HttpStatusCode.InternalServerError, "INTERNAL_ERROR", error.message ?: "Внутренняя ошибка")
                        }
                    }
                )
            }

            delete {
                val authUser = call.getAuthUserOrRespond() ?: return@delete
                val recIdStr = call.parameters["recordId"] ?: return@delete call.respondError(
                    HttpStatusCode.BadRequest, "BAD_REQUEST", "Параметр recordId обязателен"
                )
                val recordId = try {
                    RecordId(UUID.fromString(recIdStr))
                } catch (e: Exception) {
                    return@delete call.respondError(HttpStatusCode.BadRequest, "BAD_REQUEST", "Некорректный UUID записи")
                }

                val result = deleteRecordUseCase(
                    recordId = recordId,
                    requestUserId = authUser.userId,
                    isAdmin = authUser.role == UserRole.ADMIN
                )

                result.fold(
                    onSuccess = { call.respond(HttpStatusCode.NoContent, Unit) },
                    onFailure = { error ->
                        when (error) {
                            is NoSuchElementException -> call.respondError(HttpStatusCode.NotFound, "RECORD_NOT_FOUND", "Запись '$recIdStr' не найдена")
                            is IllegalAccessException -> call.respondError(HttpStatusCode.Forbidden, "FORBIDDEN", "Доступ к чужой записи запрещен")
                            else -> call.respondError(HttpStatusCode.InternalServerError, "INTERNAL_ERROR", error.message ?: "Внутренняя ошибка")
                        }
                    }
                )
            }
        }
    }
}
