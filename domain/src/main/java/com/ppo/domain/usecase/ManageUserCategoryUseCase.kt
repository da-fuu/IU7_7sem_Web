package com.ppo.domain.usecase

import com.ppo.domain.model.Category
import com.ppo.domain.repository.ICategoryRepository
import org.slf4j.LoggerFactory

class ManageUserCategoryUseCase(
    private val repository: ICategoryRepository
) {
    private val logger = LoggerFactory.getLogger(ManageUserCategoryUseCase::class.java)

    suspend operator fun invoke(category: Category): Result<Unit> {
        logger.info("Attempting to save/update user category: {}", category.name)
        if (category.name.isBlank()) {
            logger.warn("Category save failed: empty name")
            return Result.failure(IllegalArgumentException("Имя категории не может быть пустым"))
        }

        if (!category.colorHex.matches(Regex("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$"))) {
            logger.warn("Category save failed: invalid color format {}", category.colorHex)
            return Result.failure(IllegalArgumentException("Неверный формат цвета. Используйте #RRGGBB или #RGB"))
        }

        if (category.isDefault) {
            logger.warn("Category save failed: user tried to create a default category")
            return Result.failure(IllegalStateException("Пользователь не может создавать системные категории"))
        }

        val existing = repository.findCategoryByName(category.userId, category.name)
        if (existing != null && existing.id != category.id) {
            logger.warn("Category save failed: name {} already exists", category.name)
            return Result.failure(IllegalStateException("Категория с таким именем уже существует"))
        }

        return repository.saveCategory(category).also {
            if (it.isSuccess) logger.info("Successfully saved user category {}", category.name)
            else logger.error("Failed to save user category {}: {}", category.name, it.exceptionOrNull()?.message)
        }
    }
}
