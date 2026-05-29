package com.ppo.domain.usecase

import com.ppo.domain.model.Category
import com.ppo.domain.model.UserId
import com.ppo.domain.model.UserRole
import com.ppo.domain.repository.ICategoryRepository
import com.ppo.domain.repository.IUserRepository
import org.slf4j.LoggerFactory

class AdminManageBaseCategoryUseCase(
    private val categoryRepository: ICategoryRepository,
    private val userRepository: IUserRepository
) {
    private val logger = LoggerFactory.getLogger(AdminManageBaseCategoryUseCase::class.java)

    suspend operator fun invoke(category: Category, adminId: UserId): Result<Unit> {
        logger.info("Admin {} attempting to manage base category: {}", adminId.value, category.name)
        val role = userRepository.getUserRole(adminId)
        if (role != UserRole.ADMIN) {
            logger.warn("Access denied: User {} is not an admin", adminId.value)
            return Result.failure(IllegalStateException("Нет прав администратора"))
        }

        if (category.userId != null || !category.isDefault) {
            logger.warn(
                "Invalid base category parameters: isDefault={}, userId={}",
                category.isDefault,
                category.userId
            )
            return Result.failure(IllegalStateException("Базовая категория должна быть isDefault=true и без привязки к пользователю"))
        }

        if (category.name.isBlank()) {
            logger.warn("Base category save failed: empty name")
            return Result.failure(IllegalArgumentException("Имя категории не может быть пустым"))
        }

        if (!category.colorHex.matches(Regex("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$"))) {
            logger.warn("Base category save failed: invalid color format {}", category.colorHex)
            return Result.failure(IllegalArgumentException("Неверный формат цвета. Используйте #RRGGBB или #RGB"))
        }

        val existing = categoryRepository.findCategoryByName(null, category.name)
        if (existing != null && existing.id != category.id) {
            logger.warn("Base category save failed: name {} already exists", category.name)
            return Result.failure(IllegalStateException("Категория с таким именем уже существует"))
        }

        return categoryRepository.saveCategory(category).also {
            if (it.isSuccess) logger.info("Successfully saved base category {}", category.name)
            else logger.error("Failed to save base category {}: {}", category.name, it.exceptionOrNull()?.message)
        }
    }
}
