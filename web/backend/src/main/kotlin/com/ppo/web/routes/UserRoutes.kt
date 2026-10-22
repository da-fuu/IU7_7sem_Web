package com.ppo.web.routes

import com.ppo.domain.model.DurationUnit
import com.ppo.domain.model.UserId
import com.ppo.domain.model.UserRole
import com.ppo.domain.repository.IUserRepository
import com.ppo.domain.repository.IUserSettingsRepository
import com.ppo.domain.usecase.ManageUserSettingsUseCase
import com.ppo.web.dto.UserSettingsUpdateRequestDto
import com.ppo.web.dto.toDto
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.route
import java.util.UUID

fun Route.userRoutes(
    userRepository: IUserRepository,
    userSettingsRepository: IUserSettingsRepository,
    manageUserSettingsUseCase: ManageUserSettingsUseCase = ManageUserSettingsUseCase(userSettingsRepository)
) {
    route("/api/v1/users/{userId}") {
        get {
            val authUser = call.getAuthUserOrRespond() ?: return@get
            val userIdStr = call.parameters["userId"] ?: return@get call.respondError(
                HttpStatusCode.BadRequest, "BAD_REQUEST", "Параметр userId обязателен"
            )
            val targetUserId = try {
                UserId(UUID.fromString(userIdStr))
            } catch (e: Exception) {
                return@get call.respondError(HttpStatusCode.BadRequest, "BAD_REQUEST", "Некорректный UUID пользователя")
            }

            if (authUser.userId != targetUserId && authUser.role != UserRole.ADMIN) {
                return@get call.respondError(HttpStatusCode.Forbidden, "FORBIDDEN", "Доступ к чужому профилю запрещен")
            }

            val user = userRepository.getUserById(targetUserId)
            if (user == null) {
                return@get call.respondError(
                    HttpStatusCode.NotFound, "USER_NOT_FOUND", "Пользователь '$userIdStr' не найден"
                )
            }

            call.respond(HttpStatusCode.OK, user.toDto())
        }

        route("/settings") {
            get {
                val authUser = call.getAuthUserOrRespond() ?: return@get
                val userIdStr = call.parameters["userId"] ?: return@get call.respondError(
                    HttpStatusCode.BadRequest, "BAD_REQUEST", "Параметр userId обязателен"
                )
                val targetUserId = try {
                    UserId(UUID.fromString(userIdStr))
                } catch (e: Exception) {
                    return@get call.respondError(HttpStatusCode.BadRequest, "BAD_REQUEST", "Некорректный UUID пользователя")
                }

                if (authUser.userId != targetUserId && authUser.role != UserRole.ADMIN) {
                    return@get call.respondError(HttpStatusCode.Forbidden, "FORBIDDEN", "Доступ к чужим настройкам запрещен")
                }

                val user = userRepository.getUserById(targetUserId)
                if (user == null) {
                    return@get call.respondError(
                        HttpStatusCode.NotFound, "USER_NOT_FOUND", "Пользователь '$userIdStr' не найден"
                    )
                }

                val settings = manageUserSettingsUseCase.getSettings(targetUserId)
                call.respond(HttpStatusCode.OK, settings.toDto())
            }

            patch {
                val authUser = call.getAuthUserOrRespond() ?: return@patch
                val userIdStr = call.parameters["userId"] ?: return@patch call.respondError(
                    HttpStatusCode.BadRequest, "BAD_REQUEST", "Параметр userId обязателен"
                )
                val targetUserId = try {
                    UserId(UUID.fromString(userIdStr))
                } catch (e: Exception) {
                    return@patch call.respondError(HttpStatusCode.BadRequest, "BAD_REQUEST", "Некорректный UUID пользователя")
                }

                if (authUser.userId != targetUserId && authUser.role != UserRole.ADMIN) {
                    return@patch call.respondError(HttpStatusCode.Forbidden, "FORBIDDEN", "Доступ к чужим настройкам запрещен")
                }

                val user = userRepository.getUserById(targetUserId)
                if (user == null) {
                    return@patch call.respondError(
                        HttpStatusCode.NotFound, "USER_NOT_FOUND", "Пользователь '$userIdStr' не найден"
                    )
                }

                val req = try {
                    call.receive<UserSettingsUpdateRequestDto>()
                } catch (e: Exception) {
                    return@patch call.respondError(HttpStatusCode.BadRequest, "BAD_REQUEST", "Некорректный JSON")
                }

                val current = manageUserSettingsUseCase.getSettings(targetUserId)

                val maxDurationVal = req.maxActiveDurationValue ?: current.maxActiveDurationValue
                if (maxDurationVal < 1) {
                    return@patch call.respondError(
                        HttpStatusCode.BadRequest, "VALIDATION_ERROR", "maxActiveDurationValue должно быть >= 1"
                    )
                }

                val minDurationVal = req.minDurationValue ?: current.minDurationValue
                if (minDurationVal < 1) {
                    return@patch call.respondError(
                        HttpStatusCode.BadRequest, "VALIDATION_ERROR", "minDurationValue должно быть >= 1"
                    )
                }

                val maxDurationUnit = if (req.maxActiveDurationUnit != null) {
                    try {
                        DurationUnit.valueOf(req.maxActiveDurationUnit)
                    } catch (e: Exception) {
                        return@patch call.respondError(
                            HttpStatusCode.BadRequest, "VALIDATION_ERROR", "Некорректная единица: ${req.maxActiveDurationUnit}"
                        )
                    }
                } else current.maxActiveDurationUnit

                val minDurationUnit = if (req.minDurationUnit != null) {
                    try {
                        DurationUnit.valueOf(req.minDurationUnit)
                    } catch (e: Exception) {
                        return@patch call.respondError(
                            HttpStatusCode.BadRequest, "VALIDATION_ERROR", "Некорректная единица: ${req.minDurationUnit}"
                        )
                    }
                } else current.minDurationUnit

                val updated = current.copy(
                    autoStopEnabled = req.autoStopEnabled ?: current.autoStopEnabled,
                    maxActiveDurationValue = maxDurationVal,
                    maxActiveDurationUnit = maxDurationUnit,
                    minDurationEnabled = req.minDurationEnabled ?: current.minDurationEnabled,
                    minDurationValue = minDurationVal,
                    minDurationUnit = minDurationUnit
                )

                val result = manageUserSettingsUseCase.updateSettings(updated)
                result.fold(
                    onSuccess = { call.respond(HttpStatusCode.OK, it.toDto()) },
                    onFailure = { call.respondError(HttpStatusCode.BadRequest, "VALIDATION_ERROR", it.message ?: "Ошибка") }
                )
            }
        }
    }
}
