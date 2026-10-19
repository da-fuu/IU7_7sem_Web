package com.ppo.web

import com.ppo.web.dto.AuthResponseDto
import com.ppo.web.dto.ErrorResponseDto
import com.ppo.web.routes.authRoutes
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AuthApiTest {
    @Test
    fun testRegisterSuccess() = testApplication {
        val userRepo = MockUserRepository()
        val settingsRepo = MockUserSettingsRepository()
        val client = setupTestApp {
            authRoutes(userRepo, settingsRepo)
        }

        val res = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"alex_dev","name":"Алексей","password":"securePassword123","role":"USER"}""")
        }
        assertEquals(HttpStatusCode.Created, res.status)
        val authRes: AuthResponseDto = res.body()
        assertNotNull(authRes.token)
        assertEquals("alex_dev", authRes.user.login)
        assertEquals("USER", authRes.user.role)
    }

    @Test
    fun testRegisterDuplicateLogin() = testApplication {
        val userRepo = MockUserRepository()
        val settingsRepo = MockUserSettingsRepository()
        val client = setupTestApp {
            authRoutes(userRepo, settingsRepo)
        }

        client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"alex_dev","name":"Алексей","password":"securePassword123","role":"USER"}""")
        }

        val res = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"alex_dev","name":"Алексей 2","password":"securePassword456","role":"USER"}""")
        }
        assertEquals(HttpStatusCode.Conflict, res.status)
        val err: ErrorResponseDto = res.body()
        assertEquals("USER_ALREADY_EXISTS", err.code)
    }

    @Test
    fun testRegisterInvalidLogin() = testApplication {
        val userRepo = MockUserRepository()
        val settingsRepo = MockUserSettingsRepository()
        val client = setupTestApp {
            authRoutes(userRepo, settingsRepo)
        }

        val res = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"al","name":"Алексей","password":"securePassword123"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, res.status)
        val err: ErrorResponseDto = res.body()
        assertEquals("VALIDATION_ERROR", err.code)
    }

    @Test
    fun testLoginSuccess() = testApplication {
        val userRepo = MockUserRepository()
        val settingsRepo = MockUserSettingsRepository()
        val client = setupTestApp {
            authRoutes(userRepo, settingsRepo)
        }

        client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"alex_dev","name":"Алексей","password":"securePassword123","role":"USER"}""")
        }

        val res = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"alex_dev","password":"securePassword123"}""")
        }
        assertEquals(HttpStatusCode.OK, res.status)
        val authRes: AuthResponseDto = res.body()
        assertNotNull(authRes.token)
        assertEquals("alex_dev", authRes.user.login)
    }

    @Test
    fun testLoginInvalidPassword() = testApplication {
        val userRepo = MockUserRepository()
        val settingsRepo = MockUserSettingsRepository()
        val client = setupTestApp {
            authRoutes(userRepo, settingsRepo)
        }

        client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"alex_dev","name":"Алексей","password":"securePassword123","role":"USER"}""")
        }

        val res = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"alex_dev","password":"wrongPassword"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, res.status)
        val err: ErrorResponseDto = res.body()
        assertEquals("INVALID_CREDENTIALS", err.code)
    }
}
