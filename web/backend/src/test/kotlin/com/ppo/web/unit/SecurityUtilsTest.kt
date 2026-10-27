package com.ppo.web.unit

import com.ppo.domain.model.UserRole
import com.ppo.web.security.SecurityUtils
import com.ppo.web.it.AllureRunner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AllureRunner::class)
class SecurityUtilsTest {

    @Test
    fun createAndVerifyTokenSuccess() {
        val testUserId = UUID.randomUUID().toString()
        val testLogin = "dev_unit_tester"
        val testRole = "USER"

        val token = SecurityUtils.createToken(testUserId, testLogin, testRole)

        val authUser = SecurityUtils.verifyToken(token)
        assertNotNull(authUser)
        assertEquals(testUserId, authUser?.userId?.value?.toString())
        assertEquals(testLogin, authUser?.login)
        assertEquals(UserRole.USER, authUser?.role)
    }

    @Test
    fun hashPasswordProducesSha256Digest() {
        val rawPassword = "MySecurePassword123"

        val hash1 = SecurityUtils.hashPassword(rawPassword)
        val hash2 = SecurityUtils.hashPassword(rawPassword)

        assertEquals(64, hash1.length)
        assertEquals(hash1, hash2)
        assertTrue(hash1 != rawPassword)
    }

    @Test
    fun verifyTokenReturnsNullOnTamperedToken() {
        val rawToken = SecurityUtils.createToken(UUID.randomUUID().toString(), "tester", "ADMIN")
        val tamperedToken = rawToken.substring(0, rawToken.length - 5) + "abcde"

        val authUser = SecurityUtils.verifyToken(tamperedToken)

        assertNull(authUser)
    }
}
