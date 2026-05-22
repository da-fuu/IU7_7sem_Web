package com.ppo.domain.usecase

import com.ppo.domain.model.CategoryId
import com.ppo.domain.model.UserId
import com.ppo.domain.model.UserRole
import com.ppo.domain.repository.ICategoryRepository
import com.ppo.domain.repository.IUserRepository

class AdminDeleteBaseCategoryUseCase(
    private val categoryRepository: ICategoryRepository,
    private val userRepository: IUserRepository
) {
    suspend operator fun invoke(categoryId: CategoryId, adminId: UserId): Result<Unit> {
        val role = userRepository.getUserRole(adminId)
        if (role != UserRole.ADMIN) {
            return Result.failure(IllegalStateException("Нет прав администратора"))
        }
        return categoryRepository.deleteCategory(categoryId)
    }
}
