package com.ppo.domain.usecase

import com.ppo.domain.model.User
import com.ppo.domain.model.UserId
import com.ppo.domain.repository.IUserRepository

class GetUserInfoUseCase(
    private val userRepository: IUserRepository
) {
    suspend operator fun invoke(userId: UserId): User? {
        return userRepository.getUserById(userId)
    }
}
