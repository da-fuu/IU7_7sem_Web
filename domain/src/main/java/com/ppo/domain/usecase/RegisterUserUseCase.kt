package com.ppo.domain.usecase

import com.ppo.domain.model.User
import com.ppo.domain.model.UserId
import com.ppo.domain.model.UserRole
import com.ppo.domain.repository.IPasswordHasher
import com.ppo.domain.repository.IUserRepository
import java.util.UUID

class RegisterUserUseCase(
    private val userRepository: IUserRepository,
    private val passwordHasher: IPasswordHasher
) {
    suspend operator fun invoke(
        login: String,
        name: String,
        rawPassword: String,
        role: UserRole = UserRole.USER
    ): Result<Unit> {
        if (login.isBlank() || name.isBlank()) {
            return Result.failure(IllegalArgumentException("Логин и имя не должны быть пустыми"))
        }

        if (rawPassword.length < 6) {
            return Result.failure(IllegalArgumentException("Слишком короткий пароль"))
        }

        val existingUser = userRepository.findByLogin(login)
        if (existingUser != null) {
            return Result.failure(IllegalStateException("Пользователь с таким логином уже существует"))
        }

        val passwordHash = passwordHasher.hash(rawPassword)
        val user = User(
            id = UserId(UUID.randomUUID()),
            login = login,
            name = name,
            passwordHash = passwordHash,
            role = role
        )

        return userRepository.saveUser(user)
    }
}
