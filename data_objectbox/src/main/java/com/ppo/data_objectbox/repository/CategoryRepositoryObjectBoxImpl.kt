package com.ppo.data_objectbox.repository

import com.ppo.data_objectbox.entity.CategoryBoxEntity
import com.ppo.data_objectbox.entity.CategoryBoxEntity_
import com.ppo.data_objectbox.mapper.toBoxEntity
import com.ppo.data_objectbox.mapper.toDomain
import com.ppo.domain.model.Category
import com.ppo.domain.model.CategoryId
import com.ppo.domain.model.UserId
import com.ppo.domain.repository.ICategoryRepository
import io.objectbox.BoxStore
import io.objectbox.kotlin.toFlow
import io.objectbox.query.QueryBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

class CategoryRepositoryObjectBoxImpl(
    boxStore: BoxStore
) : ICategoryRepository {
    private val categoryBox = boxStore.boxFor(CategoryBoxEntity::class.java)
    private val logger = LoggerFactory.getLogger(CategoryRepositoryObjectBoxImpl::class.java)

    override fun getCategoriesFlow(userId: UserId): Flow<List<Category>> {
        val query = categoryBox.query(
            CategoryBoxEntity_.userId.equal(userId.value.toString())
                .or(CategoryBoxEntity_.isDefault.equal(true))
        ).build()
            
        return query.subscribe().toFlow().map { entities: List<CategoryBoxEntity> -> entities.map { it.toDomain() } }
    }

    override suspend fun saveCategory(category: Category): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = categoryBox.query(CategoryBoxEntity_.uid.equal(category.id.value.toString())).build().findFirst()
            categoryBox.put(category.toBoxEntity(existing?.boxId ?: 0))
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error("Failed to save category", e)
            Result.failure(e)
        }
    }

    override suspend fun findCategoryByName(userId: UserId?, name: String): Category? = withContext(Dispatchers.IO) {
        val qBuilder = categoryBox.query(CategoryBoxEntity_.name.equal(name))
        if (userId != null) {
            qBuilder.and()
            qBuilder.equal(CategoryBoxEntity_.userId, userId.value.toString(), QueryBuilder.StringOrder.CASE_SENSITIVE)
            qBuilder.or()
            qBuilder.equal(CategoryBoxEntity_.isDefault, true)
        }
        qBuilder.build().findFirst()?.toDomain()
    }

    override suspend fun deleteCategory(categoryId: CategoryId): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entity = categoryBox.query(CategoryBoxEntity_.uid.equal(categoryId.value.toString())).build().findFirst()
            if (entity != null) {
                categoryBox.remove(entity)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error("Failed to delete category {}: {}", categoryId.value, e.message)
            Result.failure(e)
        }
    }

    override suspend fun getAllCategories(): List<Category> = withContext(Dispatchers.IO) {
        categoryBox.all.map { it.toDomain() }
    }
}
