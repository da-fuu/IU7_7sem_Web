package com.ppo.data.repository

import com.ppo.data.db.dao.CategoryDao
import com.ppo.data.mapper.toDomain
import com.ppo.data.mapper.toEntity
import com.ppo.domain.model.Category
import com.ppo.domain.model.CategoryId
import com.ppo.domain.model.UserId
import com.ppo.domain.repository.ICategoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

class CategoryRepositoryImpl(
    private val categoryDao: CategoryDao
) : ICategoryRepository {
    private val logger = LoggerFactory.getLogger(CategoryRepositoryImpl::class.java)

    override fun getCategoriesFlow(userId: UserId): Flow<List<Category>> {
        return categoryDao.getCategoriesFlow(userId.value.toString())
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun saveCategory(category: Category): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            categoryDao.insert(category.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error("Failed to save category {}: {}", category.name, e.message)
            Result.failure(e)
        }
    }

    override suspend fun findCategoryByName(userId: UserId?, name: String): Category? = withContext(Dispatchers.IO) {
        categoryDao.findByName(userId?.value?.toString(), name)?.toDomain()
    }

    override suspend fun deleteCategory(categoryId: CategoryId): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val deletedCount = categoryDao.deleteById(categoryId.value.toString())
            if (deletedCount == 0) {
                logger.warn("No category found to delete with id: {}", categoryId.value)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error("Failed to delete category {}: {}", categoryId.value, e.message)
            Result.failure(e)
        }
    }

    override suspend fun getAllCategories(): List<Category> = withContext(Dispatchers.IO) {
        categoryDao.getAll().map { it.toDomain() }
    }
}
