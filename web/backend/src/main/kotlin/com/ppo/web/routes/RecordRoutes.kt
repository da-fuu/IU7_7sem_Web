package com.ppo.web.routes

import com.ppo.domain.model.*
import com.ppo.domain.repository.ICategoryRepository
import com.ppo.domain.repository.IRecordHistoryRepository
import com.ppo.domain.repository.ITimeRecordRepository
import com.ppo.domain.repository.IUserSettingsRepository
import com.ppo.web.data.repository.PostgresTimeRecordRepository
import com.ppo.web.dto.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import java.time.Duration
import java.time.Instant
import java.util.UUID

fun Route.recordRoutes(
    timeRecordRepository: ITimeRecordRepository,
    categoryRepository: ICategoryRepository,
    userSettingsRepository: IUserSettingsRepository,
    recordHistoryRepository: IRecordHistoryRepository
) {
    route("/api/v1/records") {
        get {
            val authUser = call.getAuthUserOrRespond() ?: return@get
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

            val category = categoryRepository.findCategoryById(categoryId)
            if (category == null) {
                return@post call.respondError(HttpStatusCode.NotFound, "CATEGORY_NOT_FOUND", "Категория '${req.categoryId}' не найдена")
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

            val settings = userSettingsRepository.getSettings(authUser.userId)

            if (endTime != null) {
                if (!endTime.isAfter(startTime)) {
                    return@post call.respondError(
                        HttpStatusCode.UnprocessableEntity, "INVALID_TIME_RANGE", "Время окончания должно быть строго позже времени начала"
                    )
                }

                if (settings.minDurationEnabled) {
                    val minSec = DurationConverter.toSeconds(settings.minDurationValue, settings.minDurationUnit)
                    val actualSec = Duration.between(startTime, endTime).seconds
                    if (actualSec < minSec) {
                        return@post call.respondError(
                            HttpStatusCode.UnprocessableEntity, "DURATION_TOO_SHORT", "Длительность записи менее минимального порога ($minSec сек)"
                        )
                    }
                }
            }

            if (settings.autoStopEnabled) {
                val activeSession = timeRecordRepository.getActiveSession(authUser.userId)
                if (activeSession != null) {
                    val maxSec = DurationConverter.toSeconds(settings.maxActiveDurationValue, settings.maxActiveDurationUnit)
                    val activeDuration = Duration.between(activeSession.startTime, Instant.now()).seconds
                    if (activeDuration >= maxSec) {
                        val autoEnd = activeSession.startTime.plusSeconds(maxSec)
                        timeRecordRepository.saveRecord(activeSession.copy(endTime = autoEnd))
                    }
                }
            }

            val record = TimeRecord(
                id = RecordId(UUID.randomUUID()),
                userId = authUser.userId,
                categoryId = categoryId,
                startTime = startTime,
                endTime = endTime,
                description = req.description ?: ""
            )

            timeRecordRepository.saveRecord(record)
            call.respond(HttpStatusCode.Created, record.toDto())
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

                val existing = timeRecordRepository.getRecordById(recordId)
                if (existing == null) {
                    return@put call.respondError(HttpStatusCode.NotFound, "RECORD_NOT_FOUND", "Запись '$recIdStr' не найдена")
                }

                if (existing.userId != authUser.userId && authUser.role != UserRole.ADMIN) {
                    return@put call.respondError(HttpStatusCode.Forbidden, "FORBIDDEN", "Доступ к чужой записи запрещен")
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

                if (categoryRepository.findCategoryById(categoryId) == null) {
                    return@put call.respondError(HttpStatusCode.NotFound, "CATEGORY_NOT_FOUND", "Категория '${req.categoryId}' не найдена")
                }

                val startTime = try { Instant.parse(req.startTime) } catch (e: Exception) {
                    return@put call.respondError(HttpStatusCode.BadRequest, "BAD_REQUEST", "Некорректный формат startTime")
                }

                val endTime = if (req.endTime != null) {
                    try { Instant.parse(req.endTime) } catch (e: Exception) {
                        return@put call.respondError(HttpStatusCode.BadRequest, "BAD_REQUEST", "Некорректный формат endTime")
                    }
                } else null

                val settings = userSettingsRepository.getSettings(existing.userId)

                if (endTime != null) {
                    if (!endTime.isAfter(startTime)) {
                        return@put call.respondError(
                            HttpStatusCode.UnprocessableEntity, "INVALID_TIME_RANGE", "Время окончания должно быть строго позже времени начала"
                        )
                    }

                    if (settings.minDurationEnabled) {
                        val minSec = DurationConverter.toSeconds(settings.minDurationValue, settings.minDurationUnit)
                        val actualSec = Duration.between(startTime, endTime).seconds
                        if (actualSec < minSec) {
                            return@put call.respondError(
                                HttpStatusCode.UnprocessableEntity, "DURATION_TOO_SHORT", "Длительность менее минимального порога ($minSec сек)"
                            )
                        }
                    }
                }

                val historyEntry = RecordHistoryEntry(
                    id = VersionId(UUID.randomUUID()),
                    recordId = existing.id,
                    categoryId = existing.categoryId,
                    startTime = existing.startTime,
                    endTime = existing.endTime,
                    description = existing.description,
                    modifiedAt = Instant.now()
                )
                recordHistoryRepository.saveHistoryEntry(historyEntry)

                val updated = existing.copy(
                    categoryId = categoryId,
                    startTime = startTime,
                    endTime = endTime,
                    description = req.description
                )
                timeRecordRepository.saveRecord(updated)
                call.respond(HttpStatusCode.OK, updated.toDto())
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

                val existing = timeRecordRepository.getRecordById(recordId)
                if (existing == null) {
                    return@patch call.respondError(HttpStatusCode.NotFound, "RECORD_NOT_FOUND", "Запись '$recIdStr' не найдена")
                }

                if (existing.userId != authUser.userId && authUser.role != UserRole.ADMIN) {
                    return@patch call.respondError(HttpStatusCode.Forbidden, "FORBIDDEN", "Доступ к чужой записи запрещен")
                }

                val req = try {
                    call.receive<TimeRecordPatchRequestDto>()
                } catch (e: Exception) {
                    return@patch call.respondError(HttpStatusCode.BadRequest, "BAD_REQUEST", "Некорректный JSON")
                }

                val newCategoryId = if (req.categoryId != null) {
                    val catId = try {
                        CategoryId(UUID.fromString(req.categoryId))
                    } catch (e: Exception) {
                        return@patch call.respondError(HttpStatusCode.BadRequest, "BAD_REQUEST", "Некорректный UUID категории")
                    }
                    if (categoryRepository.findCategoryById(catId) == null) {
                        return@patch call.respondError(HttpStatusCode.NotFound, "CATEGORY_NOT_FOUND", "Указанная категория не найдена")
                    }
                    catId
                } else existing.categoryId

                val newStartTime = if (req.startTime != null) {
                    try { Instant.parse(req.startTime) } catch (e: Exception) {
                        return@patch call.respondError(HttpStatusCode.BadRequest, "BAD_REQUEST", "Некорректный формат startTime")
                    }
                } else existing.startTime

                val newEndTime = if (req.endTime != null) {
                    try { Instant.parse(req.endTime) } catch (e: Exception) {
                        return@patch call.respondError(HttpStatusCode.BadRequest, "BAD_REQUEST", "Некорректный формат endTime")
                    }
                } else existing.endTime

                val settings = userSettingsRepository.getSettings(existing.userId)

                if (newEndTime != null) {
                    if (!newEndTime.isAfter(newStartTime)) {
                        return@patch call.respondError(
                            HttpStatusCode.UnprocessableEntity, "INVALID_TIME_RANGE", "Время окончания должно быть строго позже времени начала"
                        )
                    }

                    if (settings.minDurationEnabled) {
                        val minSec = DurationConverter.toSeconds(settings.minDurationValue, settings.minDurationUnit)
                        val actualSec = Duration.between(newStartTime, newEndTime).seconds
                        if (actualSec < minSec) {
                            return@patch call.respondError(
                                HttpStatusCode.UnprocessableEntity, "DURATION_TOO_SHORT", "Длительность менее минимального порога ($minSec сек)"
                            )
                        }
                    }
                }

                val historyEntry = RecordHistoryEntry(
                    id = VersionId(UUID.randomUUID()),
                    recordId = existing.id,
                    categoryId = existing.categoryId,
                    startTime = existing.startTime,
                    endTime = existing.endTime,
                    description = existing.description,
                    modifiedAt = Instant.now()
                )
                recordHistoryRepository.saveHistoryEntry(historyEntry)

                val updated = existing.copy(
                    categoryId = newCategoryId,
                    startTime = newStartTime,
                    endTime = newEndTime,
                    description = req.description ?: existing.description
                )
                timeRecordRepository.saveRecord(updated)
                call.respond(HttpStatusCode.OK, updated.toDto())
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

                val existing = timeRecordRepository.getRecordById(recordId)
                if (existing == null) {
                    return@delete call.respondError(HttpStatusCode.NotFound, "RECORD_NOT_FOUND", "Запись '$recIdStr' не найдена")
                }

                if (existing.userId != authUser.userId && authUser.role != UserRole.ADMIN) {
                    return@delete call.respondError(HttpStatusCode.Forbidden, "FORBIDDEN", "Доступ к чужой записи запрещен")
                }

                timeRecordRepository.deleteRecord(recordId)
                call.respond(HttpStatusCode.NoContent, Unit)
            }
        }
    }
}
