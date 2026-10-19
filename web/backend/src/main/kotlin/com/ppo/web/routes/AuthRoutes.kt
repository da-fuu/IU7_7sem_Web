package com.ppo.web.routes

import com.ppo.domain.model.User
import com.ppo.domain.model.UserId
import com.ppo.domain.model.UserRole
import com.ppo.domain.model.UserSettings
import com.ppo.domain.repository.IUserRepository
import com.ppo.domain.repository.IUserSettingsRepository
import com.ppo.web.dto.AuthResponseDto
import com.ppo.web.dto.UserLoginRequestDto
import com.ppo.web.dto.UserRegistrationRequestDto
import com.ppo.web.dto.toDto
import com.ppo.web.security.SecurityUtils
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID

fun Route.authRoutes(
    userRepository: IUserRepository,
    userSettingsRepository: IUserSettingsRepository
) {
    route("/api/v1/auth") {
        post("/register") {
            val req = try {
                call.receive<UserRegistrationRequestDto>()
            } catch (e: Exception) {
                call.respondError(HttpStatusCode.BadRequest, "BAD_REQUEST", "Некорректный JSON в теле запроса")
                return@post
            }

            if (req.login.length < 3 || req.login.length > 50) {
                call.respondError(HttpStatusCode.BadRequest, "VALIDATION_ERROR", "Длина логина должна быть от 3 до 50 символов")
                return@post
            }
            if (!Regex("^[a-zA-Z0-9_.-]+$").matches(req.login)) {
                call.respondError(HttpStatusCode.BadRequest, "VALIDATION_ERROR", "Логин может содержать только латиницу, цифры, дефис и точку")
                return@post
            }
            if (req.name.isBlank()) {
                call.respondError(HttpStatusCode.BadRequest, "VALIDATION_ERROR", "Имя пользователя обязательно")
                return@post
            }
            if (req.password.length < 6) {
                call.respondError(HttpStatusCode.BadRequest, "VALIDATION_ERROR", "Пароль должен содержать не менее 6 символов")
                return@post
            }

            val existing = userRepository.findByLogin(req.login)
            if (existing != null) {
                call.respondError(HttpStatusCode.Conflict, "USER_ALREADY_EXISTS", "Пользователь с логином '${req.login}' уже зарегистрирован")
                return@post
            }

            val role = if (req.role?.uppercase() == "ADMIN") UserRole.ADMIN else UserRole.USER
            val userId = UserId(UUID.randomUUID())
            val user = User(
                id = userId,
                login = req.login,
                name = req.name,
                passwordHash = SecurityUtils.hashPassword(req.password),
                role = role
            )

            val saveResult = userRepository.saveUser(user)
            if (saveResult.isFailure) {
                call.respondError(HttpStatusCode.InternalServerError, "INTERNAL_ERROR", "Не удалось сохранить пользователя")
                return@post
            }

            userSettingsRepository.updateSettings(UserSettings(userId = userId))

            val token = SecurityUtils.createToken(userId.value.toString(), user.login, user.role.name)
            call.respond(HttpStatusCode.Created, AuthResponseDto(token = token, user = user.toDto()))
        }

        post("/login") {
            val req = try {
                call.receive<UserLoginRequestDto>()
            } catch (e: Exception) {
                call.respondError(HttpStatusCode.BadRequest, "BAD_REQUEST", "Некорректный JSON в теле запроса")
                return@post
            }

            val user = userRepository.findByLogin(req.login)
            if (user == null || !SecurityUtils.verifyPassword(req.password, user.passwordHash)) {
                call.respondError(HttpStatusCode.Unauthorized, "INVALID_CREDENTIALS", "Неверный логин или пароль")
                return@post
            }

            val token = SecurityUtils.createToken(user.id.value.toString(), user.login, user.role.name)
            call.respond(HttpStatusCode.OK, AuthResponseDto(token = token, user = user.toDto()))
        }
    }
}
