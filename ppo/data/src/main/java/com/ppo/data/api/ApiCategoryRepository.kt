package com.ppo.data.api

import com.ppo.domain.model.Category
import com.ppo.domain.model.CategoryId
import com.ppo.domain.model.UserId
import com.ppo.domain.repository.ICategoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.util.UUID

class ApiCategoryRepository(
    private val client: RestApiClient
) : ICategoryRepository {
    override fun getCategoriesFlow(userId: UserId): Flow<List<Category>> = flow {
        emit(getAllCategories())
    }

    override suspend fun getAllCategories(): List<Category> = withContext(Dispatchers.IO) {
        val res = client.request("GET", "/categories")
        if (res.statusCode == 200) {
            val root = JsonMini.parseObject(res.body)
            val items = root["items"] as? List<*> ?: return@withContext emptyList()
            items.mapNotNull { item ->
                val map = item as? Map<*, *> ?: return@mapNotNull null
                val idStr = map["id"] as? String ?: return@mapNotNull null
                val userIdStr = map["userId"] as? String
                val name = map["name"] as? String ?: ""
                val colorHex = map["colorHex"] as? String ?: "#000000"
                val isDefault = (map["isDefault"] as? Boolean) ?: false
                Category(
                    id = CategoryId(UUID.fromString(idStr)),
                    userId = userIdStr?.let { UserId(UUID.fromString(it)) },
                    name = name,
                    colorHex = colorHex,
                    isDefault = isDefault
                )
            }
        } else emptyList()
    }

    override suspend fun saveCategory(category: Category): Result<Unit> = withContext(Dispatchers.IO) {
        val json = """{"name":"${category.name}","colorHex":"${category.colorHex}","isDefault":${category.isDefault}}"""
        val res = client.request("POST", "/categories", json)
        if (res.statusCode in 200..299) Result.success(Unit) else Result.failure(Exception(res.body))
    }

    override suspend fun findCategoryByName(userId: UserId?, name: String): Category? = withContext(Dispatchers.IO) {
        getAllCategories().find { it.name == name }
    }

    override suspend fun findCategoryById(categoryId: CategoryId): Category? = withContext(Dispatchers.IO) {
        val res = client.request("GET", "/categories/${categoryId.value}")
        if (res.statusCode == 200) {
            val map = JsonMini.parseObject(res.body)
            val idStr = map["id"] as? String ?: return@withContext null
            val userIdStr = map["userId"] as? String
            val name = map["name"] as? String ?: ""
            val colorHex = map["colorHex"] as? String ?: "#000000"
            val isDefault = (map["isDefault"] as? Boolean) ?: false
            Category(
                id = CategoryId(UUID.fromString(idStr)),
                userId = userIdStr?.let { UserId(UUID.fromString(it)) },
                name = name,
                colorHex = colorHex,
                isDefault = isDefault
            )
        } else null
    }

    override suspend fun deleteCategory(categoryId: CategoryId): Result<Unit> = withContext(Dispatchers.IO) {
        val res = client.request("DELETE", "/categories/${categoryId.value}")
        if (res.statusCode in 200..299) Result.success(Unit) else Result.failure(Exception(res.body))
    }
}
