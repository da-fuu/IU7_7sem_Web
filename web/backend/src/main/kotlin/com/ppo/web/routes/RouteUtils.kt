package com.ppo.web.routes

import com.ppo.domain.model.DurationUnit
import com.ppo.web.dto.ErrorResponseDto
import com.ppo.web.security.AuthUser
import com.ppo.web.security.SecurityUtils
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.header
import io.ktor.server.response.respond
import java.time.Instant

object DurationConverter {
    fun toSeconds(value: Long, unit: DurationUnit): Long {
        return when (unit) {
            DurationUnit.seconds -> value
            DurationUnit.minutes -> value * 60L
            DurationUnit.hours -> value * 3600L
            DurationUnit.days -> value * 86400L
        }
    }
}

suspend fun ApplicationCall.respondError(
    status: HttpStatusCode,
    code: String,
    message: String,
    details: List<String>? = null
) {
    val errorDto = ErrorResponseDto(
        code = code,
        message = message,
        details = details,
        timestamp = Instant.now().toString()
    )
    respond(status, errorDto)
}

suspend fun ApplicationCall.getAuthUserOrRespond(): AuthUser? {
    val authHeader = request.header("Authorization")
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        respondError(HttpStatusCode.Unauthorized, "UNAUTHORIZED", "Требуется авторизация в системе")
        return null
    }
    val token = authHeader.removePrefix("Bearer ").trim()
    val authUser = SecurityUtils.verifyToken(token)
    if (authUser == null) {
        respondError(HttpStatusCode.Unauthorized, "UNAUTHORIZED", "Недействительный или истекший токен доступа")
        return null
    }
    return authUser
}
