package com.ppo.domain.usecase

import com.ppo.domain.model.Category
import com.ppo.domain.model.UserId
import com.ppo.domain.repository.ICategoryRepository
import kotlinx.coroutines.flow.first

class GetCategoriesUseCase(
    private val repository: ICategoryRepository
) {
    suspend operator fun invoke(userId: UserId): List<Category> {
        return repository.getCategoriesFlow(userId).first()
    }
}
