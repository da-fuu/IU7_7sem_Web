package com.ppo.domain.repository

import com.ppo.domain.model.User
import com.ppo.domain.model.UserId
import com.ppo.domain.model.UserRole

interface IUserRepository {
    suspend fun findByLogin(login: String): User?
    suspend fun saveUser(user: User): Result<Unit>
    suspend fun getUserRole(userId: UserId): UserRole?
    suspend fun getUserById(userId: UserId): User?
    suspend fun getAllUsers(): List<User>
}
