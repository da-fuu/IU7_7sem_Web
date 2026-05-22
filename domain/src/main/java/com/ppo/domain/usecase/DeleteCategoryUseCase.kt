package com.ppo.domain.usecase

import com.ppo.domain.model.CategoryId
import com.ppo.domain.repository.ICategoryRepository

class DeleteCategoryUseCase(
    private val repository: ICategoryRepository
) {
    suspend operator fun invoke(categoryId: CategoryId): Result<Unit> {
        return repository.deleteCategory(categoryId)
    }
}
