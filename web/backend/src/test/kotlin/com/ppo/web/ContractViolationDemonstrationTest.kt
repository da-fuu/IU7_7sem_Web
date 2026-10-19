package com.ppo.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.ppo.domain.model.Category
import com.ppo.domain.model.CategoryId
import com.ppo.domain.model.UserId
import com.ppo.web.routes.categoryRoutes
import com.ppo.web.security.SecurityUtils
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ContractViolationDemonstrationTest {
    private val mapper = ObjectMapper().apply {
        registerModule(JavaTimeModule())
        registerKotlinModule()
    }

    private fun validateItemsObjectContract(jsonResponse: String) {
        val rootNode = mapper.readTree(jsonResponse)
        if (!rootNode.isObject) {
            throw AssertionError("CONTRACT VIOLATION: Response root must be a JSON object, but was: ${rootNode.nodeType}")
        }
        if (!rootNode.has("items")) {
            throw AssertionError("CONTRACT VIOLATION: Response object must contain 'items' property per OpenAPI 3.1 contract")
        }
    }

    @Test
    fun testContractViolationDetectionOnBareArray() {
        val nonCompliantJson = """[{"id":"1111","name":"Работа"}]"""
        var violationDetected = false
        try {
            validateItemsObjectContract(nonCompliantJson)
        } catch (e: AssertionError) {
            if (e.message?.contains("CONTRACT VIOLATION") == true) {
                violationDetected = true
            }
        }
        assertTrue(violationDetected)
    }

    @Test
    fun testRealApiCompliesWithContract() = testApplication {
        val categoryRepo = MockCategoryRepository()
        val userId = UserId(UUID.randomUUID())
        runBlocking {
            categoryRepo.saveCategory(Category(CategoryId(UUID.randomUUID()), null, "Системная", "#10B981", true))
        }
        val token = SecurityUtils.createToken(userId.value.toString(), "alex", "USER")

        val client = setupTestApp {
            categoryRoutes(categoryRepo)
        }

        val res = client.get("/api/v1/categories") {
            header("Authorization", "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, res.status)

        val bodyText = res.bodyAsText()
        validateItemsObjectContract(bodyText)

        assertFalse(bodyText.startsWith("["))
        assertTrue(bodyText.contains("\"items\""))
    }
}
