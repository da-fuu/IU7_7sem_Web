package com.ppo.web

import com.ppo.domain.model.User
import com.ppo.domain.model.UserId
import com.ppo.domain.model.UserRole
import com.ppo.web.dto.ErrorResponseDto
import com.ppo.web.dto.UserDto
import com.ppo.web.dto.UserSettingsDto
import com.ppo.web.routes.userRoutes
import com.ppo.web.security.SecurityUtils
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class UserApiTest {
    @Test
    fun testUnauthorizedAccess() = testApplication {
        val userRepo = MockUserRepository()
        val settingsRepo = MockUserSettingsRepository()
        val client = setupTestApp {
            userRoutes(userRepo, settingsRepo)
        }

        val res = client.get("/api/v1/users/${UUID.randomUUID()}")
        assertEquals(HttpStatusCode.Unauthorized, res.status)
    }

    @Test
    fun testGetUserProfileSuccess() = testApplication {
        val userRepo = MockUserRepository()
        val settingsRepo = MockUserSettingsRepository()
        val userId = UserId(UUID.randomUUID())
        runBlocking {
            userRepo.saveUser(
                User(
                    id = userId,
                    login = "alex_dev",
                    name = "Алексей Смирнов",
                    passwordHash = "hash",
                    role = UserRole.USER
                )
            )
        }
        val token = SecurityUtils.createToken(userId.value.toString(), "alex_dev", "USER")

        val client = setupTestApp {
            userRoutes(userRepo, settingsRepo)
        }

        val res = client.get("/api/v1/users/${userId.value}") {
            header("Authorization", "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, res.status)
        val profile: UserDto = res.body()
        assertEquals(userId.value.toString(), profile.id)
        assertEquals("alex_dev", profile.login)
        assertEquals("Алексей Смирнов", profile.name)
    }

    @Test
    fun testAccessOtherUserProfileForbidden() = testApplication {
        val userRepo = MockUserRepository()
        val settingsRepo = MockUserSettingsRepository()
        val user1Id = UserId(UUID.randomUUID())
        val user2Id = UserId(UUID.randomUUID())
        runBlocking {
            userRepo.saveUser(User(user1Id, "alex", "Алексей", "h1", UserRole.USER))
            userRepo.saveUser(User(user2Id, "maria", "Мария", "h2", UserRole.USER))
        }
        val token = SecurityUtils.createToken(user1Id.value.toString(), "alex", "USER")

        val client = setupTestApp {
            userRoutes(userRepo, settingsRepo)
        }

        val res = client.get("/api/v1/users/${user2Id.value}") {
            header("Authorization", "Bearer $token")
        }
        assertEquals(HttpStatusCode.Forbidden, res.status)
        val err: ErrorResponseDto = res.body()
        assertEquals("FORBIDDEN", err.code)
    }

    @Test
    fun testGetAndPatchUserSettingsSuccess() = testApplication {
        val userRepo = MockUserRepository()
        val settingsRepo = MockUserSettingsRepository()
        val userId = UserId(UUID.randomUUID())
        runBlocking {
            userRepo.saveUser(User(userId, "alex", "Алексей", "h", UserRole.USER))
        }
        val token = SecurityUtils.createToken(userId.value.toString(), "alex", "USER")

        val client = setupTestApp {
            userRoutes(userRepo, settingsRepo)
        }

        val getRes = client.get("/api/v1/users/${userId.value}/settings") {
            header("Authorization", "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, getRes.status)
        val initialSettings: UserSettingsDto = getRes.body()
        assertTrue(initialSettings.autoStopEnabled)

        val patchRes = client.patch("/api/v1/users/${userId.value}/settings") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"maxActiveDurationValue":12,"maxActiveDurationUnit":"hours","minDurationEnabled":false}""")
        }
        assertEquals(HttpStatusCode.OK, patchRes.status)
        val updatedSettings: UserSettingsDto = patchRes.body()
        assertEquals(12L, updatedSettings.maxActiveDurationValue)
        assertEquals("hours", updatedSettings.maxActiveDurationUnit)
        assertFalse(updatedSettings.minDurationEnabled)
    }

    @Test
    fun testPatchUserSettingsInvalidValue() = testApplication {
        val userRepo = MockUserRepository()
        val settingsRepo = MockUserSettingsRepository()
        val userId = UserId(UUID.randomUUID())
        runBlocking {
            userRepo.saveUser(User(userId, "alex", "Алексей", "h", UserRole.USER))
        }
        val token = SecurityUtils.createToken(userId.value.toString(), "alex", "USER")

        val client = setupTestApp {
            userRoutes(userRepo, settingsRepo)
        }

        val patchRes = client.patch("/api/v1/users/${userId.value}/settings") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"minDurationValue":0}""")
        }
        assertEquals(HttpStatusCode.BadRequest, patchRes.status)
        val err: ErrorResponseDto = patchRes.body()
        assertEquals("VALIDATION_ERROR", err.code)
    }
}
