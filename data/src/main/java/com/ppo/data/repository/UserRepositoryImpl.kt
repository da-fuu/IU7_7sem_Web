package com.ppo.data.repository

import com.ppo.data.db.dao.UserDao
import com.ppo.data.mapper.toDomain
import com.ppo.data.mapper.toEntity
import com.ppo.domain.model.User
import com.ppo.domain.model.UserId
import com.ppo.domain.model.UserRole
import com.ppo.domain.repository.IUserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepositoryImpl(
    private val userDao: UserDao
) : IUserRepository {
    override suspend fun findByLogin(login: String): User? = withContext(Dispatchers.IO) {
        userDao.findByLogin(login)?.toDomain()
    }

    override suspend fun saveUser(user: User): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            userDao.insert(user.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserRole(userId: UserId): UserRole? = withContext(Dispatchers.IO) {
        userDao.getUserRole(userId.value.toString())
    }
}
