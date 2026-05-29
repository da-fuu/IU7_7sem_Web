package com.ppo.domain.usecase

import com.ppo.domain.model.CategoryId
import com.ppo.domain.repository.ICategoryRepository
import org.slf4j.LoggerFactory

class DeleteCategoryUseCase(
    private val repository: ICategoryRepository
) {
    private val logger = LoggerFactory.getLogger(DeleteCategoryUseCase::class.java)

    suspend operator fun invoke(categoryId: CategoryId): Result<Unit> {
        logger.info("Attempting to delete category: {}", categoryId.value)
        return repository.deleteCategory(categoryId).also {
            if (it.isSuccess) logger.info("Successfully deleted category {}", categoryId.value)
            else logger.error("Failed to delete category {}: {}", categoryId.value, it.exceptionOrNull()?.message)
        }
    }
}
