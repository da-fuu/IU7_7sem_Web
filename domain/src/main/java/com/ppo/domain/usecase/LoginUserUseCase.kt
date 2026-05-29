package com.ppo.domain.usecase

import com.ppo.domain.model.User
import com.ppo.domain.repository.IPasswordHasher
import com.ppo.domain.repository.IUserRepository
import org.slf4j.LoggerFactory

class LoginUserUseCase(
    private val userRepository: IUserRepository,
    private val passwordHasher: IPasswordHasher
) {
    private val logger = LoggerFactory.getLogger(LoginUserUseCase::class.java)

    suspend operator fun invoke(login: String, rawPassword: String): Result<User> {
        val user = userRepository.findByLogin(login) ?: run {
            logger.warn("Login failed: user {} not found", login)
            return Result.failure(IllegalStateException("Неверный логин или пароль"))
        }

        if (!passwordHasher.verify(rawPassword, user.passwordHash)) {
            logger.warn("Login failed: invalid password for user {}", login)
            return Result.failure(IllegalStateException("Неверный логин или пароль"))
        }

        logger.info("User {} logged in successfully", login)
        return Result.success(user)
    }
}
