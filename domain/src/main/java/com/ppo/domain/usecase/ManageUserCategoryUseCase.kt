package com.ppo.domain.usecase

import com.ppo.domain.model.Category
import com.ppo.domain.repository.ICategoryRepository

class ManageUserCategoryUseCase(
    private val repository: ICategoryRepository
) {
    suspend operator fun invoke(category: Category): Result<Unit> {
        if (category.name.isBlank()) {
            return Result.failure(IllegalArgumentException("Имя категории не может быть пустым"))
        }

        if (category.isDefault) {
            return Result.failure(IllegalStateException("Пользователь не может создавать системные категории"))
        }

        val existing = repository.findCategoryByName(category.userId, category.name)
        if (existing != null && existing.id != category.id) {
            return Result.failure(IllegalStateException("Категория с таким именем уже существует"))
        }

        return repository.saveCategory(category)
    }
}
