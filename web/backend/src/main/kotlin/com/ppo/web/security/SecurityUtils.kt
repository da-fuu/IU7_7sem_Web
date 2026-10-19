package com.ppo.web.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import com.ppo.domain.model.UserId
import com.ppo.domain.model.UserRole
import java.security.MessageDigest
import java.util.Date
import java.util.UUID

data class AuthUser(
    val userId: UserId,
    val login: String,
    val role: UserRole
)

object SecurityUtils {
    private val jwtSecret = System.getenv("JWT_SECRET") ?: "timetracker_default_secret_key_2026_very_long"
    private val algorithm = Algorithm.HMAC256(jwtSecret)
    private const val issuer = "TimeTrackerBackend"
    private const val validityMs = 7 * 24 * 3600 * 1000L

    fun createToken(userId: String, login: String, role: String): String {
        val now = Date()
        val expiresAt = Date(now.time + validityMs)
        return JWT.create()
            .withIssuer(issuer)
            .withSubject(userId)
            .withClaim("login", login)
            .withClaim("role", role)
            .withIssuedAt(now)
            .withExpiresAt(expiresAt)
            .sign(algorithm)
    }

    fun verifyToken(token: String): AuthUser? {
        return try {
            val verifier = JWT.require(algorithm)
                .withIssuer(issuer)
                .build()
            val decoded: DecodedJWT = verifier.verify(token)
            val userIdStr = decoded.subject ?: return null
            val login = decoded.getClaim("login").asString() ?: return null
            val roleStr = decoded.getClaim("role").asString() ?: return null
            AuthUser(
                userId = UserId(UUID.fromString(userIdStr)),
                login = login,
                role = UserRole.valueOf(roleStr)
            )
        } catch (e: Exception) {
            null
        }
    }

    fun hashPassword(password: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(password.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPassword(password: String, hash: String): Boolean {
        return hashPassword(password) == hash
    }
}
