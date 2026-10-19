package com.ppo.web.data.repository

import com.ppo.domain.model.Category
import com.ppo.domain.model.CategoryId
import com.ppo.domain.model.UserId
import com.ppo.domain.repository.ICategoryRepository
import com.ppo.web.data.orm.CategoriesTable
import com.ppo.web.data.orm.CategoryEntity
import com.ppo.web.data.orm.UserEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.transactions.transaction
import javax.sql.DataSource

class PostgresCategoryRepository(private val dataSource: DataSource) : ICategoryRepository {
    override fun getCategoriesFlow(userId: UserId): Flow<List<Category>> = flow {
        emit(getCategories(userId))
    }

    private fun getCategories(userId: UserId): List<Category> = transaction {
        CategoryEntity.find {
            (CategoriesTable.user eq userId.value.toString()) or (CategoriesTable.isDefault eq true)
        }.map { it.toDomain() }
    }

    override suspend fun saveCategory(category: Category): Result<Unit> = runCatching {
        transaction {
            val existing = CategoryEntity.findById(category.id.value.toString())
            val userEntity = category.userId?.let { UserEntity.findById(it.value.toString()) }
            if (existing != null) {
                existing.user = userEntity
                existing.name = category.name
                existing.colorHex = category.colorHex
                existing.isDefault = category.isDefault
            } else {
                CategoryEntity.new(category.id.value.toString()) {
                    this.user = userEntity
                    this.name = category.name
                    this.colorHex = category.colorHex
                    this.isDefault = category.isDefault
                }
            }
        }
    }

    override suspend fun findCategoryByName(userId: UserId?, name: String): Category? = transaction {
        if (userId != null) {
            CategoryEntity.find {
                (CategoriesTable.name eq name) and (
                    (CategoriesTable.user eq userId.value.toString()) or (CategoriesTable.isDefault eq true)
                )
            }.firstOrNull()?.toDomain()
        } else {
            CategoryEntity.find {
                (CategoriesTable.name eq name) and (CategoriesTable.isDefault eq true)
            }.firstOrNull()?.toDomain()
        }
    }

    override suspend fun findCategoryById(categoryId: CategoryId): Category? = transaction {
        CategoryEntity.findById(categoryId.value.toString())?.toDomain()
    }

    override suspend fun deleteCategory(categoryId: CategoryId): Result<Unit> = runCatching {
        transaction {
            val existing = CategoryEntity.findById(categoryId.value.toString())
            if (existing != null) {
                existing.delete()
            } else {
                throw NoSuchElementException("Category with id $categoryId not found")
            }
        }
    }

    override suspend fun getAllCategories(): List<Category> = transaction {
        CategoryEntity.all().map { it.toDomain() }
    }
}
