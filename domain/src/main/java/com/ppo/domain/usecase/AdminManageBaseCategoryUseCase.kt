package com.ppo.domain.usecase

import com.ppo.domain.model.Category
import com.ppo.domain.model.UserId
import com.ppo.domain.model.UserRole
import com.ppo.domain.repository.ICategoryRepository
import com.ppo.domain.repository.IUserRepository

class AdminManageBaseCategoryUseCase(
    private val categoryRepository: ICategoryRepository,
    private val userRepository: IUserRepository
) {
    suspend operator fun invoke(category: Category, adminId: UserId): Result<Unit> {
        val role = userRepository.getUserRole(adminId)
        if (role != UserRole.ADMIN) {
            return Result.failure(IllegalStateException("Нет прав администратора"))
        }

        if (category.userId != null || !category.isDefault) {
            return Result.failure(IllegalStateException("Базовая категория должна быть isDefault=true и без привязки к пользователю"))
        }

        if (category.name.isBlank()) {
            return Result.failure(IllegalArgumentException("Имя категории не может быть пустым"))
        }

        val existing = categoryRepository.findCategoryByName(null, category.name)
        if (existing != null && existing.id != category.id) {
            return Result.failure(IllegalStateException("Категория с таким именем уже существует"))
        }

        return categoryRepository.saveCategory(category)
    }
}
