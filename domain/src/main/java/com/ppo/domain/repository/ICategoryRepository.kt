package com.ppo.domain.repository

import com.ppo.domain.model.Category
import com.ppo.domain.model.CategoryId
import com.ppo.domain.model.UserId
import kotlinx.coroutines.flow.Flow

interface ICategoryRepository {
    fun getCategoriesFlow(userId: UserId): Flow<List<Category>>
    suspend fun saveCategory(category: Category): Result<Unit>
    suspend fun findCategoryByName(userId: UserId?, name: String): Category?
    suspend fun deleteCategory(categoryId: CategoryId): Result<Unit>
    suspend fun getAllCategories(): List<Category>
}
