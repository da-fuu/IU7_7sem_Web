package com.ppo.web

import com.ppo.domain.model.Category
import com.ppo.domain.model.CategoryId
import com.ppo.domain.model.UserId
import com.ppo.web.dto.CategoryDto
import com.ppo.web.dto.CategoryListResponseDto
import com.ppo.web.dto.ErrorResponseDto
import com.ppo.web.routes.categoryRoutes
import com.ppo.web.security.SecurityUtils
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class CategoryApiTest {
    @Test
    fun testListCategoriesReturnsItemsObject() = testApplication {
        val categoryRepo = MockCategoryRepository()
        val userId = UserId(UUID.randomUUID())
        val catId = CategoryId(UUID.randomUUID())
        runBlocking {
            categoryRepo.saveCategory(Category(catId, null, "Работа", "#3B82F6", true))
        }
        val token = SecurityUtils.createToken(userId.value.toString(), "alex", "USER")

        val client = setupTestApp {
            categoryRoutes(categoryRepo)
        }

        val res = client.get("/api/v1/categories") {
            header("Authorization", "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, res.status)
        val list: CategoryListResponseDto = res.body()
        assertNotNull(list.items)
        assertTrue(list.items.isNotEmpty())
        assertEquals("Работа", list.items[0].name)
    }

    @Test
    fun testCreateUserCategorySuccess() = testApplication {
        val categoryRepo = MockCategoryRepository()
        val userId = UserId(UUID.randomUUID())
        val token = SecurityUtils.createToken(userId.value.toString(), "alex", "USER")

        val client = setupTestApp {
            categoryRoutes(categoryRepo)
        }

        val res = client.post("/api/v1/categories") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Спорт","colorHex":"#10B981"}""")
        }
        assertEquals(HttpStatusCode.Created, res.status)
        val created: CategoryDto = res.body()
        assertEquals("Спорт", created.name)
        assertEquals("#10B981", created.colorHex)
        assertFalse(created.isDefault)
    }

    @Test
    fun testCreateBaseCategoryNonAdminForbidden() = testApplication {
        val categoryRepo = MockCategoryRepository()
        val userId = UserId(UUID.randomUUID())
        val token = SecurityUtils.createToken(userId.value.toString(), "alex", "USER")

        val client = setupTestApp {
            categoryRoutes(categoryRepo)
        }

        val res = client.post("/api/v1/categories") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Системная","colorHex":"#EF4444","isDefault":true}""")
        }
        assertEquals(HttpStatusCode.Forbidden, res.status)
        val err: ErrorResponseDto = res.body()
        assertEquals("FORBIDDEN", err.code)
    }

    @Test
    fun testCreateBaseCategoryAdminSuccess() = testApplication {
        val categoryRepo = MockCategoryRepository()
        val adminId = UserId(UUID.randomUUID())
        val token = SecurityUtils.createToken(adminId.value.toString(), "admin", "ADMIN")

        val client = setupTestApp {
            categoryRoutes(categoryRepo)
        }

        val res = client.post("/api/v1/categories") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Системная","colorHex":"#EF4444","isDefault":true}""")
        }
        assertEquals(HttpStatusCode.Created, res.status)
        val created: CategoryDto = res.body()
        assertTrue(created.isDefault)
    }

    @Test
    fun testPatchAndDeleteCategory() = testApplication {
        val categoryRepo = MockCategoryRepository()
        val userId = UserId(UUID.randomUUID())
        val catId = CategoryId(UUID.randomUUID())
        runBlocking {
            categoryRepo.saveCategory(Category(catId, userId, "Хобби", "#8B5CF6", false))
        }
        val token = SecurityUtils.createToken(userId.value.toString(), "alex", "USER")

        val client = setupTestApp {
            categoryRoutes(categoryRepo)
        }

        val patchRes = client.patch("/api/v1/categories/${catId.value}") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Хобби и книги"}""")
        }
        assertEquals(HttpStatusCode.OK, patchRes.status)
        val patched: CategoryDto = patchRes.body()
        assertEquals("Хобби и книги", patched.name)

        val deleteRes = client.delete("/api/v1/categories/${catId.value}") {
            header("Authorization", "Bearer $token")
        }
        assertEquals(HttpStatusCode.NoContent, deleteRes.status)

        val getRes = client.get("/api/v1/categories/${catId.value}") {
            header("Authorization", "Bearer $token")
        }
        assertEquals(HttpStatusCode.NotFound, getRes.status)
    }

    @Test
    fun testListCategoriesWithScopeFilter() = testApplication {
        val categoryRepo = MockCategoryRepository()
        val userId = UserId(UUID.randomUUID())
        val defaultId = CategoryId(UUID.randomUUID())
        val customId = CategoryId(UUID.randomUUID())
        runBlocking {
            categoryRepo.saveCategory(Category(defaultId, null, "DefaultCat", "#3B82F6", true))
            categoryRepo.saveCategory(Category(customId, userId, "CustomCat", "#8B5CF6", false))
        }
        val token = SecurityUtils.createToken(userId.value.toString(), "alex", "USER")

        val client = setupTestApp {
            categoryRoutes(categoryRepo)
        }

        val allRes = client.get("/api/v1/categories?scope=all") {
            header("Authorization", "Bearer $token")
        }
        val allList: CategoryListResponseDto = allRes.body()
        assertEquals(2, allList.items.size)

        val defaultRes = client.get("/api/v1/categories?scope=default") {
            header("Authorization", "Bearer $token")
        }
        val defaultList: CategoryListResponseDto = defaultRes.body()
        assertEquals(1, defaultList.items.size)
        assertTrue(defaultList.items[0].isDefault)

        val customRes = client.get("/api/v1/categories?scope=custom") {
            header("Authorization", "Bearer $token")
        }
        val customList: CategoryListResponseDto = customRes.body()
        assertEquals(1, customList.items.size)
        assertFalse(customList.items[0].isDefault)
    }
}
