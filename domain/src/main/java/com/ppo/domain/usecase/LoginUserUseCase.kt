package com.ppo.domain.usecase

import com.ppo.domain.model.User
import com.ppo.domain.repository.IPasswordHasher
import com.ppo.domain.repository.IUserRepository

class LoginUserUseCase(
    private val userRepository: IUserRepository,
    private val passwordHasher: IPasswordHasher
) {
    suspend operator fun invoke(login: String, rawPassword: String): Result<User> {
        val user = userRepository.findByLogin(login)
            ?: return Result.failure(IllegalStateException("Неверный логин или пароль"))

        if (!passwordHasher.verify(rawPassword, user.passwordHash)) {
            return Result.failure(IllegalStateException("Неверный логин или пароль"))
        }

        return Result.success(user)
    }
}
