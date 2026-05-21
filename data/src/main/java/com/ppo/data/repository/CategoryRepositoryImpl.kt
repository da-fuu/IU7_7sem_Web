package com.ppo.data.repository

import com.ppo.data.db.dao.CategoryDao
import com.ppo.data.mapper.toDomain
import com.ppo.data.mapper.toEntity
import com.ppo.domain.model.Category
import com.ppo.domain.model.UserId
import com.ppo.domain.repository.ICategoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class CategoryRepositoryImpl(
    private val categoryDao: CategoryDao
) : ICategoryRepository {
    override fun getCategoriesFlow(userId: UserId): Flow<List<Category>> {
        return categoryDao.getCategoriesFlow(userId.value.toString())
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun saveCategory(category: Category): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            categoryDao.insert(category.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun findCategoryByName(userId: UserId?, name: String): Category? = withContext(Dispatchers.IO) {
        categoryDao.findByName(userId?.value?.toString(), name)?.toDomain()
    }
}
