package com.ppo.domain.usecase

import com.ppo.domain.model.CategoryId
import com.ppo.domain.model.UserId
import com.ppo.domain.model.UserRole
import com.ppo.domain.repository.ICategoryRepository
import com.ppo.domain.repository.IUserRepository
import org.slf4j.LoggerFactory

class AdminDeleteBaseCategoryUseCase(
    private val categoryRepository: ICategoryRepository,
    private val userRepository: IUserRepository
) {
    private val logger = LoggerFactory.getLogger(AdminDeleteBaseCategoryUseCase::class.java)

    suspend operator fun invoke(categoryId: CategoryId, adminId: UserId): Result<Unit> {
        logger.info("Admin {} attempting to delete base category: {}", adminId.value, categoryId.value)
        val role = userRepository.getUserRole(adminId)
        if (role != UserRole.ADMIN) {
            logger.warn("Access denied: User {} is not an admin", adminId.value)
            return Result.failure(IllegalStateException("Нет прав администратора"))
        }
        return categoryRepository.deleteCategory(categoryId).also {
            if (it.isSuccess) logger.info("Successfully deleted base category {}", categoryId.value)
            else logger.error("Failed to delete base category {}: {}", categoryId.value, it.exceptionOrNull()?.message)
        }
    }
}
